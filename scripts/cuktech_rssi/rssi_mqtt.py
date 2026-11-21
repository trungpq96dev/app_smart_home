#!/usr/bin/env python3
"""Do suc song BLE tu cu sac CUKTECH roi publish len MQTT.

Vi sao can mot script rieng thay vi sua `cuktech-ble-server`: du an do la ma
nguon ngoai (MIT) chay nguyen ban tren serverubuntu, va no KHONG publish RSSI
o dau ca (da doi chieu toan bo `mqtt_publish` ngay 21/11/2026). Sua vao do la
phai nuoi mot ban va rieng mai mai - khong dang cho mot con so.

Cach lay so: doc thuoc tinh `RSSI` ma BlueZ giu san cho thiet bi tren D-Bus.
`cuktech-ble-server` quet lai moi lan no noi lai, nen khi song yeu (rot lien
tuc) so nay tu tuoi ma khong ai phai lam gi.

Cho chinh la luc NOI DUOC: BlueZ XOA thuoc tinh RSSI khi thiet bi dang trong
mot ket noi (do thuc te 21/11/2026 - dang_noi=true thi RSSI bien mat han).
Nen script tu bat mot phien quet NGAN khi so da qua cu. Quet 6 giay moi 60
giay la du re de khong tranh song cua chinh cai dongle dang giu ket noi; do
thu truc tiep thi mot phien quet 8 giay khong lam rot ket noi dang co.

RSSI tren D-Bus khong kem moc thoi gian, nen script tu giu moc: chi khi GIA
TRI doi thi moi coi la vua do. `tuoi_giay` vi the la "bao lau roi chua thay
so moi" - app dung no de biet co nen tin con so hay khong. Gia tri cu VAN
duoc giu lai thay vi tra null: mat song khong phai la song bang 0, va mot so
cu kem tuoi doc duoc nhieu hon mot o trong.

Thang do (dBm, cang gan 0 cang manh):

    >= -65   tot      noi on dinh
    -66..-80 kha      thinh thoang rot
    -81..-90 yeu      rot lien tuc  <- serverubuntu do duoc -81..-96
    <  -90   rat yeu  gan nhu khong giu noi lien ket
"""

from __future__ import annotations

import json
import logging
import os
import signal
import subprocess
import sys
import time
from datetime import datetime, timedelta, timezone

import paho.mqtt.client as mqtt

GIO_VN = timezone(timedelta(hours=7))

log = logging.getLogger("cuktech_rssi")


def _env(ten: str, mac_dinh: str = "") -> str:
    return os.environ.get(ten, mac_dinh).strip()


def _doc_env_file(duong_dan: str) -> None:
    """Nap .env canh script. Khong dung thu vien ngoai cho mot viec nho the nay."""
    if not os.path.isfile(duong_dan):
        return
    with open(duong_dan, encoding="utf-8") as f:
        for dong in f:
            dong = dong.strip()
            if not dong or dong.startswith("#") or "=" not in dong:
                continue
            khoa, _, gia_tri = dong.partition("=")
            os.environ.setdefault(khoa.strip(), gia_tri.strip())


def duong_dan_dbus(adapter: str, mac: str) -> str:
    """`3C:CD:73:2B:58:8A` -> `/org/bluez/hci0/dev_3C_CD_73_2B_58_8A`."""
    return f"/org/bluez/{adapter}/dev_{mac.upper().replace(':', '_')}"


def doc_thuoc_tinh(duong_dan: str, ten: str) -> str | None:
    """Doc mot thuoc tinh org.bluez.Device1 qua `busctl`.

    Dung `busctl` chu khong phai python-dbus: may chi can python3 tran, khong
    them goi he thong nao. Thuoc tinh vang mat (BlueZ da quen thiet bi) tra ve
    None - KHONG phai 0, vi 0 dBm la mot gia tri RSSI hop le va rat manh.
    """
    try:
        ket_qua = subprocess.run(
            ["busctl", "get-property", "org.bluez", duong_dan, "org.bluez.Device1", ten],
            capture_output=True, text=True, timeout=10,
        )
    except (OSError, subprocess.TimeoutExpired) as e:
        log.warning("busctl %s loi: %s", ten, e)
        return None
    if ket_qua.returncode != 0:
        return None
    # Dinh dang: "n -81" (int16), "b true" (bool), "s abc" (string).
    phan = ket_qua.stdout.strip().split(None, 1)
    return phan[1] if len(phan) == 2 else None


def xep_hang(rssi: int | None) -> str:
    if rssi is None:
        return "khong-thay"
    if rssi >= -65:
        return "tot"
    if rssi >= -80:
        return "kha"
    if rssi >= -90:
        return "yeu"
    return "rat-yeu"


def quet_ngan(giay: int) -> None:
    """Bat discovery mot lat de BlueZ cap nhat lai RSSI.

    `bluetoothctl --timeout N scan on` tu tat sau N giay ke ca khi bi giet -
    khong de lai mot phien discovery treo lam ton song mai mai.
    """
    try:
        subprocess.run(
            ["bluetoothctl", "--timeout", str(giay), "scan", "on"],
            capture_output=True, text=True, timeout=giay + 10,
        )
    except (OSError, subprocess.TimeoutExpired) as e:
        log.warning("quet ngan loi: %s", e)


class BoDo:
    def __init__(self, adapter: str, mac: str, quet_giay: int, quet_cach: int) -> None:
        self.duong_dan = duong_dan_dbus(adapter, mac)
        self.quet_giay = quet_giay
        self.quet_cach = quet_cach
        self.rssi: int | None = None
        self.moc_doi: float = 0.0
        self.moc_quet: float = 0.0

    def _doc_rssi(self) -> int | None:
        tho = doc_thuoc_tinh(self.duong_dan, "RSSI")
        if tho is None:
            return None
        try:
            return int(tho)
        except ValueError:
            return None

    def _ghi_nhan(self, rssi: int | None, vua_quet: bool = False) -> None:
        """Cap nhat gia tri va moc "lan cuoi biet chac con song".

        Moc chi duoc lam moi khi co BANG CHUNG thiet bi con phat: hoac gia tri
        doi (ai do vua quet duoc so khac), hoac chinh ta vua quet xong ma van
        doc ra so. Doc duoc mot gia tri DUNG YEN ma khong quet thi khong tinh:
        BlueZ giu thuoc tinh RSSI trong cache rat lau sau khi thiet bi da im,
        nen tin vao no la bao "song tot" cho mot cu sac da rut dien.
        """
        if rssi is None:
            return
        if vua_quet or rssi != self.rssi or self.moc_doi == 0.0:
            self.moc_doi = time.time()
        self.rssi = rssi

    def do(self) -> dict:
        self._ghi_nhan(self._doc_rssi())

        bay_gio = time.time()
        qua_cu = self.moc_doi == 0.0 or bay_gio - self.moc_doi > self.quet_cach
        du_gian = bay_gio - self.moc_quet > self.quet_cach
        if self.quet_giay > 0 and qua_cu and du_gian:
            self.moc_quet = bay_gio
            quet_ngan(self.quet_giay)
            self._ghi_nhan(self._doc_rssi(), vua_quet=True)

        noi_tho = doc_thuoc_tinh(self.duong_dan, "Connected")
        dang_noi = None if noi_tho is None else noi_tho == "true"

        return {
            "rssi": self.rssi,
            "xep_hang": xep_hang(self.rssi),
            "tuoi_giay": None if self.moc_doi == 0 else int(time.time() - self.moc_doi),
            "dang_noi": dang_noi,
            "luc": datetime.now(GIO_VN).isoformat(timespec="seconds"),
        }


def chay() -> int:
    _doc_env_file(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env"))
    logging.basicConfig(
        level=_env("LOG_LEVEL", "INFO"),
        format="%(asctime)s %(levelname)s %(message)s",
    )

    mac = _env("CUKTECH_MAC")
    if not mac:
        log.error("Thieu CUKTECH_MAC trong .env")
        return 2
    adapter = _env("BT_ADAPTER", "hci0")
    nguon_id = _env("NGUON_ID", "server")
    nguon_ten = _env("NGUON_TEN", "serverubuntu")
    nhip = max(5, int(_env("NHIP", "20")))
    topic = f"{_env('TOPIC_GOC', 'cuktech')}/rssi/{nguon_id}"

    bo_do = BoDo(
        adapter, mac,
        quet_giay=int(_env("QUET_GIAY", "6")),
        quet_cach=max(30, int(_env("QUET_CACH", "60"))),
    )

    c = mqtt.Client(
        mqtt.CallbackAPIVersion.VERSION2,
        client_id=_env("MQTT_CLIENT_ID", "smarthome-cuktech-rssi"),
    )
    mat_khau = _env("MQTT_PASS")
    if mat_khau:
        c.username_pw_set(_env("MQTT_USER", "iot"), mat_khau)
    # LWT: script chet thi app phai thay so bien mat, khong phai thay mot con
    # so dung yen va tuong song van the.
    c.will_set(topic, "", retain=True)
    c.connect(_env("MQTT_BROKER", "127.0.0.1"), int(_env("MQTT_PORT", "1883")), 60)
    c.loop_start()

    dang_chay = True

    def dung(*_a: object) -> None:
        nonlocal dang_chay
        dang_chay = False

    signal.signal(signal.SIGTERM, dung)
    signal.signal(signal.SIGINT, dung)

    log.info("Do RSSI %s qua %s, publish len %s moi %ds", mac, adapter, topic, nhip)
    while dang_chay:
        goi = bo_do.do()
        goi["mac"] = mac
        goi["nguon"] = nguon_ten
        goi["adapter"] = adapter
        c.publish(topic, json.dumps(goi, ensure_ascii=False), retain=True)
        log.debug("%s", goi)
        for _ in range(nhip * 2):
            if not dang_chay:
                break
            time.sleep(0.5)

    c.publish(topic, "", retain=True)
    c.loop_stop()
    c.disconnect()
    return 0


if __name__ == "__main__":
    sys.exit(chay())
