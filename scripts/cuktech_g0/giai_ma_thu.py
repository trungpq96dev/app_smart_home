#!/usr/bin/env python3
"""Giai doan 0 - thu giai ma cac goi da bat bang xiaomi-ble + bindkey.

  python giai_ma_thu.py goi.jsonl --mac AA:BB:CC:DD:EE:FF --bindkey <32 ky tu hex>

Khong co bindkey thi van chay duoc (bo --bindkey): goi MiBeacon khong ma hoa
van doc duoc, va it ra biet xiaomi-ble co NHAN RA thiet bi nay khong.

Ba ket qua co the gap:

  1. In ra sensor kem gia tri        -> giai duoc, di tiep giai doan 1.
  2. "xiaomi-ble khong nhan ra"      -> object ID rieng cua CUKTECH. Thu vien
                                        tra ve rong CHU KHONG bao loi, nen
                                        dung ket luan la cau hinh sai.
  3. "bindkey sai"                   -> lay lai token.
"""

from __future__ import annotations

import argparse
import json
import sys

try:
    from home_assistant_bluetooth import BluetoothServiceInfo
    from xiaomi_ble import XiaomiBluetoothDeviceData
except ImportError:
    sys.exit("Thieu xiaomi-ble. Xem README.md: phai chay bang python cua venv_ble.")

UUID_XIAOMI = "0000fe95-0000-1000-8000-00805f9b34fb"


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("duong_dan", help="file JSONL do quet_ble.py theo --ra ghi ra")
    p.add_argument("--mac", required=True)
    p.add_argument("--bindkey", default=None, help="32 ky tu hex")
    p.add_argument("--ten", default="CUKTECH 10 Ultra")
    ns = p.parse_args()

    bindkey = None
    if ns.bindkey:
        k = ns.bindkey.strip().replace(":", "").replace(" ", "")
        if len(k) != 32:
            sys.exit("bindkey phai dung 32 ky tu hex, dang co %d." % len(k))
        bindkey = bytes.fromhex(k)

    thiet_bi = XiaomiBluetoothDeviceData(bindkey=bindkey)

    so_goi = 0
    so_fe95 = 0
    so_ra_sensor = 0
    da_thay: set[str] = set()

    with open(ns.duong_dan, encoding="utf-8") as f:
        for dong in f:
            dong = dong.strip()
            if not dong:
                continue
            g = json.loads(dong)
            so_goi += 1

            hex_fe95 = None
            for nhan, h in g["blob"].items():
                if nhan.lower() in ("svc:fe95", "svc:FE95".lower()):
                    hex_fe95 = h
            if hex_fe95 is None:
                continue
            so_fe95 += 1

            info = BluetoothServiceInfo(
                name=g.get("ten") or ns.ten,
                address=ns.mac,
                rssi=g.get("rssi", -60),
                manufacturer_data={},
                service_data={UUID_XIAOMI: bytes.fromhex(hex_fe95)},
                service_uuids=[UUID_XIAOMI],
                source="local",
            )

            try:
                ket_qua = thiet_bi.update(info)
            except Exception as e:  # thu vien nem loi rieng cho bindkey sai
                print("goi #%d: loi giai ma: %s: %s" % (so_goi, type(e).__name__, e))
                continue

            gia_tri = getattr(ket_qua, "entity_values", {}) or {}
            if gia_tri:
                so_ra_sensor += 1
                for khoa, v in gia_tri.items():
                    ten = getattr(khoa, "key", khoa)
                    don_vi = getattr(v, "native_unit_of_measurement", "") or ""
                    dong_ra = "%s = %s %s" % (ten, getattr(v, "native_value", v), don_vi)
                    if dong_ra not in da_thay:
                        da_thay.add(dong_ra)
                    print("goi #%d: %s" % (so_goi, dong_ra.strip()))

    print("\n--- TONG KET ---")
    print("Goi doc duoc        : %d" % so_goi)
    print("Goi co FE95         : %d" % so_fe95)
    print("Goi ra duoc sensor  : %d" % so_ra_sensor)
    print("bindkey da xac thuc : %s" % getattr(thiet_bi, "bindkey_verified", "khong ro"))

    if so_fe95 == 0:
        print("\nKhong goi nao co service data FE95 -> khong phai MiBeacon.")
        print("Xem muc 6.2 trong docs/KE-HOACH-CUKTECH.md (phuong an B).")
        return 1
    if so_ra_sensor == 0:
        print("\nCo FE95 nhung xiaomi-ble KHONG ra sensor nao.")
        print("Hai kha nang: bindkey sai, hoac object ID rieng cua CUKTECH ma")
        print("thu vien chua biet. Truong hop sau thi thu vien tra ve RONG chu")
        print("khong bao loi - dung ket luan vi la cau hinh sai.")
        return 1

    print("\nGiai duoc. Doi chieu gia tri voi so hien tren man hinh cu sac,")
    print("roi chot hop dong topic o muc 4 docs/KE-HOACH-CUKTECH.md.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
