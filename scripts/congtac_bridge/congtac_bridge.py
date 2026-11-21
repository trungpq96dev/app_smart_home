#!/usr/bin/env python3
"""Cau noi 2 cong tac Wi-Fi phong ngu 1 <-> Mosquitto.

Nghe lenh o `smarthome/control` (topic app da dung san cho tro ly giong noi),
thuc thi qua driver LAN, roi publish trang thai that len `congtac/#`.

Hop dong topic - xem docs/KE-HOACH-CONGTAC.md muc 4:

  congtac/info            retained  danh sach thiet bi bridge dang quan
  congtac/thietbi/<id>    retained  trang thai mot cong tac
  congtac/trangthai       retained  "online" / "offline"  <- LWT
  congtac/capnhat         retained  moc thoi gian
  congtac/cmd/result      khong     ket qua mot lenh vua thuc thi
  smarthome/control       (nghe)    lenh tu app

KHONG BIA TRANG THAI: khong doc duoc thi `is_on` la null, khong phai false.
Mot cong tac gat duoc, nam o the TAT, trong khi khong ai biet den dang sang
hay khong, la loi noi doi ton it pixel nhat trong ca du an nay.
"""

from __future__ import annotations

import json
import logging
import os
import queue
import random
import signal
import string
import sys
import time
from datetime import datetime, timedelta, timezone

import paho.mqtt.client as mqtt

from driver import Driver, KetQuaDoc, LoiDoc, dung_driver

# Image slim mac dinh UTC. Hai collector kia cung chot cung +7 nhu nay.
GIO_VN = timezone(timedelta(hours=7))

log = logging.getLogger("congtac")


def gio() -> str:
    return datetime.now(GIO_VN).strftime("%Y-%m-%d %H:%M:%S")


def moi_truong(ten: str, mac_dinh: str = "") -> str:
    return os.environ.get(ten, mac_dinh).strip()


def so(ten: str, mac_dinh: float) -> float:
    try:
        return float(moi_truong(ten) or mac_dinh)
    except ValueError:
        return mac_dinh


def bat_khong(ten: str, mac_dinh: bool = True) -> bool:
    v = moi_truong(ten).lower()
    if v == "":
        return mac_dinh
    return v not in ("0", "false", "no", "khong")


# --------------------------------------------------------------- cau hinh

def nap_cau_hinh() -> list[Driver]:
    """Doc .env thanh danh sach driver.

    Moi thiet bi mot tien to (DEN_, QUAT_). Co <TIEN_TO>_BAT=0 thi bo qua han
    thiet bi do - day la duong de len quat truoc khi con dang tac local_key
    cua den, ma khong phai sua mot dong Kotlin nao: congtac/info chi liet ke
    thiet bi co that, va app khong hard-code ten thiet bi nao ca.
    """
    timeout = so("POLL_TIMEOUT", 3.0)
    # Dai mang de di tim lai thiet bi khi DHCP doi IP. Rong = tat tu dong tim
    # (luc do IP trong .env phai dung, va phai dat IP tinh tren router).
    mang = moi_truong("MANG", "192.168.0.0/24")
    cho_quet = so("CHO_QUET", 120)
    ds: list[Driver] = []

    if bat_khong("DEN_BAT", True):
        # DEN_IP khong con bat buoc: co DEN_DEVICE_ID + DEN_LOCAL_KEY la du
        # de di tim thiet bi tren mang.
        thieu = [k for k in ("DEN_DEVICE_ID", "DEN_LOCAL_KEY") if not moi_truong(k)]
        if thieu:
            log.error("Bo qua den: thieu %s trong .env. "
                      "Chua co local_key thi dat DEN_BAT=0 cho tuong minh.", ", ".join(thieu))
        else:
            ds.append(dung_driver({
                "he": "tuya",
                "id": moi_truong("DEN_ID", "bed1_den"),
                "ten": moi_truong("DEN_TEN", "Den"),
                "phong": moi_truong("DEN_PHONG", "bed1"),
                "loai": moi_truong("DEN_LOAI", "den"),
                "ip": moi_truong("DEN_IP"),
                "device_id": moi_truong("DEN_DEVICE_ID"),
                "local_key": moi_truong("DEN_LOCAL_KEY"),
                "version": moi_truong("DEN_VERSION", "3.3"),
                "dp": moi_truong("DEN_DP", "1"),
                "timeout": timeout,
                "mang": mang,
                "cho_quet": cho_quet,
            }))

    if bat_khong("QUAT_BAT", True):
        # Can IT NHAT mot trong hai: QUAT_DID (de tu tim) hoac QUAT_IP.
        if not moi_truong("QUAT_DID") and not moi_truong("QUAT_IP"):
            log.error("Bo qua quat: phai co QUAT_DID hoac QUAT_IP trong .env.")
        else:
            ds.append(dung_driver({
                "he": "cozylife",
                "id": moi_truong("QUAT_ID", "bed1_quat"),
                "ten": moi_truong("QUAT_TEN", "Quat"),
                "phong": moi_truong("QUAT_PHONG", "bed1"),
                "loai": moi_truong("QUAT_LOAI", "quat"),
                "ip": moi_truong("QUAT_IP"),
                "port": int(so("QUAT_PORT", 5555)),
                "dpid": moi_truong("QUAT_DPID", "1"),
                "gia_tri_bat": int(so("QUAT_GIA_TRI_BAT", 255)),
                "timeout": timeout,
                "did": moi_truong("QUAT_DID"),
                "mang": mang,
                "cho_quet": cho_quet,
            }))

    return ds


# ------------------------------------------------------------------ payload

def payload_info(ds: list[Driver]) -> dict:
    return {"bridge": "congtac_bridge 1.0", "cap_nhat": gio(),
            "thiet_bi": [d.mo_ta() for d in ds]}


class TrangThai:
    """Nho trang thai lan truoc de biet khi nao DOI va khi nao can publish lai."""

    def __init__(self, d: Driver) -> None:
        self.d = d
        self.is_on: bool | None = None
        self.online = False
        self.doi_luc: str | None = None
        self.lan_publish = 0.0
        self.loi_lien_tiep = 0
        self.tra_loi_id: str | None = None
        self.dau_tien = True

    def payload(self, kq: KetQuaDoc, nguon: str) -> dict:
        return {
            "id": self.d.id,
            "phong": self.d.phong,
            "loai": self.d.loai,
            "online": self.online,
            # online=False thi is_on BAT BUOC la null. Khong ngoai le.
            "is_on": self.is_on if self.online else None,
            "nguon": nguon,
            "doc_luc": gio(),
            "doi_luc": self.doi_luc,
            "tra_loi_id": self.tra_loi_id,
            "ly_do_loi": kq.ly_do_loi,
        }


class Bridge:
    def __init__(self, ds: list[Driver]) -> None:
        self.ds = ds
        self.tt = {d.id: TrangThai(d) for d in ds}
        self.goc = moi_truong("TOPIC_GOC", "congtac").rstrip("/")
        self.topic_lenh = moi_truong("TOPIC_LENH", "smarthome/control")
        self.poll = so("POLL_INTERVAL", 10.0)
        self.lam_moi = so("LAM_MOI_RETAIN", 60.0)
        self.nguong_offline = int(so("SO_LAN_LOI_OFFLINE", 3))
        self.hang_doi: queue.Queue[dict] = queue.Queue(maxsize=64)
        self.chay = True
        self.cl: mqtt.Client | None = None
        #: IP cua tung thiet bi tai lan publish `congtac/info` gan nhat.
        #: Thiet bi doi IP ma khong publish lai thi `info` noi sai - app doc
        #: no de hien trong man chan doan.
        self._ip_da_bao: dict[str, str] = {}

    # ---- MQTT

    def t(self, duoi: str) -> str:
        return "%s/%s" % (self.goc, duoi)

    def pub(self, topic: str, obj, retain: bool = True) -> None:
        than = obj if isinstance(obj, str) else json.dumps(obj, ensure_ascii=False)
        assert self.cl is not None
        self.cl.publish(topic, than, retain=retain, qos=1)

    def on_connect(self, cl, userdata, flags, rc, properties=None) -> None:
        if rc != 0:
            log.error("Broker tu choi dang nhap, ma %s. Kiem MQTT_USER/MQTT_PASS.", rc)
            return
        log.info("Da noi broker. Nghe lenh o %s", self.topic_lenh)
        cl.subscribe(self.topic_lenh, qos=1)
        self.pub(self.t("trangthai"), "online")
        self.pub_info()

    def pub_info(self) -> None:
        self.pub(self.t("info"), payload_info(self.ds))
        self._ip_da_bao = {d.id: d.ip for d in self.ds}

    def pub_info_neu_doi(self) -> None:
        """Publish lai `info` khi co thiet bi doi IP.

        Phai goi sau MOI lan doc, khong chi trong vong poll: lan doc luc khoi
        dong cung co the kich hoat tu tim va doi IP, ma luc do `info` vua
        duoc publish o on_connect voi dia chi cu.
        """
        if any(self._ip_da_bao.get(d.id) != d.ip for d in self.ds):
            self.pub_info()

    def on_message(self, cl, userdata, msg) -> None:
        """CHI parse roi bo vao hang doi.

        Tuyet doi khong cham thiet bi o day: callback nay chay tren luong mang
        cua Paho. Block vai giay la rot keepalive -> broker da client ra ->
        reconnect quay vong, trieu chung giong het mat song Wi-Fi.
        """
        try:
            o = json.loads(msg.payload.decode("utf8"))
        except Exception:
            log.warning("Lenh khong phai JSON, bo qua: %r", msg.payload[:120])
            return
        if not isinstance(o, dict) or "device" not in o:
            log.warning("Lenh thieu truong 'device', bo qua: %r", msg.payload[:120])
            return
        try:
            self.hang_doi.put_nowait(o)
        except queue.Full:
            log.warning("Hang doi lenh day, bo qua mot lenh.")

    # ---- thuc thi

    def xu_ly_lenh(self, o: dict) -> None:
        ma = str(o.get("device", ""))
        st = self.tt.get(ma)
        if st is None:
            # IM LANG co chu dinh. smarthome/control la bus dung chung; mai mot
            # ESP32 so huu kitchen_den. Ban "khong biet thiet bi" cho moi lenh
            # khong phai cua minh thi log va cmd/result day bao dong gia, va
            # app khong phan biet duoc "khong ai nhan" voi "co nguoi nhan".
            log.debug("Lenh cho thiet bi khong phai cua minh: %s", ma)
            return

        hanh_dong = str(o.get("action", "")).strip().upper()
        nguon = str(o.get("source", "?"))
        ma_lenh = str(o.get("id", "")) or None
        bat_dau = time.monotonic()

        def ket(ket_qua: str, ly_do: str | None = None, kq: KetQuaDoc | None = None) -> None:
            self.pub(self.t("cmd/result"), {
                "id": ma, "action": hanh_dong, "source": nguon, "lenh_id": ma_lenh,
                "nhan_luc": gio(), "ket_qua": ket_qua, "ly_do": ly_do,
                "is_on": kq.is_on if kq else None,
                "ms": int((time.monotonic() - bat_dau) * 1000),
            }, retain=False)

        try:
            if hanh_dong in ("ON", "OFF"):
                kq = st.d.dat(hanh_dong == "ON")
            elif hanh_dong == "DAT":
                gt = str(o.get("value", "")).strip().lower()
                if gt in ("on", "true", "1", "bat"):
                    kq = st.d.dat(True)
                elif gt in ("off", "false", "0", "tat"):
                    kq = st.d.dat(False)
                else:
                    ket("loi", 'khong hieu value "%s"' % o.get("value"))
                    return
            elif hanh_dong == "DAO":
                kq = st.d.dao()
            elif hanh_dong in ("TANG", "GIAM"):
                # Khong bao gio coi TANG la ON. Lam the la tu bia ra y nguoi dung.
                ket("bo_qua", "cong tac nay chi co bat/tat")
                return
            else:
                ket("bo_qua", 'khong hieu lenh "%s"' % hanh_dong)
                return
        except LoiDoc as e:
            ket("loi", str(e))
            self.ghi_nhan(st, KetQuaDoc(False, None, str(e)), "lenh", ma_lenh)
            return

        st.tra_loi_id = ma_lenh
        self.ghi_nhan(st, kq, "lenh", ma_lenh, ep_publish=True)
        ket("ok" if kq.online and kq.is_on is not None else "loi", kq.ly_do_loi, kq)

    def ghi_nhan(self, st: TrangThai, kq: KetQuaDoc, nguon: str,
                 ma_lenh: str | None = None, ep_publish: bool = False) -> None:
        """Gop ket qua doc vao trang thai, roi publish neu dang publish.

        Publish khi: lan dau, trang thai DOI, vua thuc thi mot lenh, hoac den
        han lam moi retained. Khong publish moi vong poll - giu broker sach,
        ma retained van luon tuoi.
        """
        truoc = (st.online, st.is_on)

        if kq.online:
            st.loi_lien_tiep = 0
            st.online = True
            st.is_on = kq.is_on
        else:
            st.loi_lien_tiep += 1
            if st.loi_lien_tiep < self.nguong_offline and not st.dau_tien:
                # Chua du nguong: giu trang thai cu, chua tuyen bo mat ket noi.
                # Mot vong loi le chi la Wi-Fi chop; tuyen bo offline ngay thi
                # man hinh nhap nhay "mat ket noi" suot ngay.
                return
            st.online = False
            st.is_on = None        # offline thi khong bao gio giu gia tri cu

        doi = (st.online, st.is_on) != truoc
        if doi:
            st.doi_luc = gio()
        if ma_lenh is not None:
            st.tra_loi_id = ma_lenh

        bay_gio = time.monotonic()
        if doi or ep_publish or st.dau_tien or bay_gio - st.lan_publish >= self.lam_moi:
            self.pub(self.t("thietbi/%s" % st.d.id), st.payload(kq, nguon))
            st.lan_publish = bay_gio
            st.dau_tien = False

    # ---- vong chinh

    def vong(self) -> None:
        # Doc mot lan ngay khi khoi dong de retained co so that tu dau.
        for d in self.ds:
            self.ghi_nhan(self.tt[d.id], d.doc(), "khoi_dong", ep_publish=True)
        self.pub_info_neu_doi()
        self.pub(self.t("capnhat"), gio())

        while self.chay:
            han = time.monotonic() + self.poll
            while self.chay and time.monotonic() < han:
                try:
                    lenh = self.hang_doi.get(timeout=0.2)
                except queue.Empty:
                    continue
                try:
                    self.xu_ly_lenh(lenh)
                except Exception:
                    log.exception("Loi khi xu ly lenh, bridge van chay tiep.")
            if not self.chay:
                break
            for d in self.ds:
                try:
                    self.ghi_nhan(self.tt[d.id], d.doc(), "thiet_bi")
                except Exception:
                    log.exception("Loi khi doc %s, bo qua vong nay.", d.id)
            self.pub_info_neu_doi()
            self.pub(self.t("capnhat"), gio())

    def dung(self, *_) -> None:
        self.chay = False


def main() -> int:
    logging.basicConfig(
        level=getattr(logging, moi_truong("LOG_LEVEL", "INFO").upper(), logging.INFO),
        format="%(asctime)s %(levelname)s %(message)s",
    )

    ds = nap_cau_hinh()
    if not ds:
        log.error("Khong co thiet bi nao duoc bat. Kiem DEN_BAT / QUAT_BAT va .env.")
        return 1
    log.info("Quan %d thiet bi: %s", len(ds), ", ".join("%s(%s)" % (d.id, d.he) for d in ds))

    b = Bridge(ds)
    cid = moi_truong("MQTT_CLIENT_ID") or "smarthome-congtac-" + "".join(
        random.choices(string.digits, k=6))
    # PHAI khac client id cua cac collector khac - trung id la broker da ca hai
    # ra, va trieu chung nhin giong mat song chu khong giong trung id.
    cl = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=cid)
    b.cl = cl

    tk, mk = moi_truong("MQTT_USER"), moi_truong("MQTT_PASS")
    if tk:
        cl.username_pw_set(tk, mk)
    cl.will_set(b.t("trangthai"), "offline", qos=1, retain=True)
    cl.on_connect = b.on_connect
    cl.on_message = b.on_message

    host = moi_truong("MQTT_BROKER", "192.168.0.50")
    cong = int(so("MQTT_PORT", 1883))
    try:
        cl.connect(host, cong, keepalive=45)
    except OSError as e:
        log.error("Khong noi duoc broker %s:%d (%s). Trong container KHONG dung "
                  "network host thi 127.0.0.1 la chinh container, khong phai broker.",
                  host, cong, e)
        return 1

    signal.signal(signal.SIGTERM, b.dung)
    signal.signal(signal.SIGINT, b.dung)

    cl.loop_start()
    try:
        b.vong()
    finally:
        log.info("Dang dung...")
        # Noi that truoc khi di: LWT chi ban khi CHET dot ngot.
        try:
            b.pub(b.t("trangthai"), "offline")
            time.sleep(0.3)
        except Exception:
            pass
        cl.loop_stop()
        try:
            cl.disconnect()
        except Exception:
            pass
        for d in ds:
            d.dong()
    return 0


if __name__ == "__main__":
    sys.exit(main())
