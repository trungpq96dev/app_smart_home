#!/usr/bin/env python3
"""Giai doan 0 - noi GATT va liet ke service/characteristic. CHI DOC.

  python do_gatt.py --mac 3C:CD:73:2B:58:8A [--doc]

Khong ghi gi vao thiet bi. --doc thi doc them gia tri cua nhung characteristic
co co 'read' (van la chi doc).

Dung de tra loi cau hoi cua muc 6.2 docs/KE-HOACH-CUKTECH.md: neu quang ba
khong cho so lieu thi co duong GATT nao chay notify khong. Co characteristic
notify cua hang -> phuong an B kha thi. Khong noi duoc, hoac chi co cac service
chuan (Device Information, Battery) -> khong con duong nao.
"""

from __future__ import annotations

import argparse
import asyncio
import sys

try:
    from bleak import BleakClient, BleakScanner
except ImportError:
    sys.exit("Thieu bleak. Xem README.md.")

# Service/characteristic chuan cua Bluetooth SIG - thay la biet khong phai cua hang.
CHUAN = {
    "1800": "Generic Access",
    "1801": "Generic Attribute",
    "180a": "Device Information",
    "180f": "Battery Service",
    "fe95": "Xiaomi MiBeacon (cau hinh)",
}


def _chu(b: bytes) -> str:
    """Doc phan chu trong mot mang byte (bo byte 0 dem o duoi). Rong thi tra ve ''."""
    t = b.split(b"\x00")[0].decode("utf-8", "ignore").strip()
    t = "".join(c for c in t if c.isprintable())
    return '("%s")' % t if t else ""


async def lenh_nghe(mac: str, char: list[str], giay: int) -> int:
    """Bam notify va NGHE. Khong ghi mot byte nao vao thiet bi.

    Chay lenh nay roi cam/rut tai: thiet bi co tu day so lieu len khong.
    """
    import datetime

    dev = await BleakScanner.find_device_by_address(mac, timeout=15)
    if dev is None:
        print("Khong thay thiet bi.")
        return 1

    dem = 0
    truoc: dict[str, bytes] = {}

    def cb(ch, data: bytearray):
        nonlocal dem
        dem += 1
        b = bytes(data)
        gio = datetime.datetime.now().strftime("%H:%M:%S.%f")[:-3]
        cu = truoc.get(ch.uuid)
        if cu is None:
            doi = "(goi dau)"
        elif cu == b:
            doi = "khong doi"
        elif len(cu) != len(b):
            doi = "DO DAI DOI %d -> %d" % (len(cu), len(b))
        else:
            doi = "byte doi: " + ",".join(str(i) for i, (x, y) in enumerate(zip(cu, b)) if x != y)
        truoc[ch.uuid] = b
        print("%s  %s  %s  %s %s" % (gio, ch.uuid.split("-")[0][-4:], b.hex(), doi, _chu(b)))

    print("Noi toi %s ..." % mac)
    async with BleakClient(dev, timeout=25) as c:
        print("DA NOI. Bam notify: %s" % ", ".join(char))
        bam = []
        for u in char:
            try:
                await c.start_notify(u, cb)
                bam.append(u)
            except Exception as e:
                print("  khong bam duoc %s: %s" % (u, type(e).__name__))
        if not bam:
            print("Khong bam duoc characteristic nao.")
            return 1
        print("Nghe %d giay. TRONG LUC NAY: cam tai vao cu sac, de ~20s, rut ra.\n" % giay)
        await asyncio.sleep(giay)
        for u in bam:
            try:
                await c.stop_notify(u)
            except Exception:
                pass

    print("\n--- %d goi notify ---" % dem)
    if dem == 0:
        print("Thiet bi KHONG tu day du lieu len. Muon co so thi phai GHI mot lenh")
        print("vao af07 truoc - ma lenh do chua biet, phai dich nguoc tu app CUKTECH.")
        print("Xem muc 6.2 docs/KE-HOACH-CUKTECH.md truoc khi bo cong vao viec do.")
    return 0


async def chay(mac: str, doc_gia_tri: bool) -> int:
    print("Tim thiet bi %s ..." % mac)
    dev = await BleakScanner.find_device_by_address(mac, timeout=15)
    if dev is None:
        print("Khong thay. Thiet bi co the dang ngu hoac da rut dien.")
        return 1

    print("Thay. Dang noi (co the mat 10-20 giay)...")
    try:
        async with BleakClient(dev, timeout=25) as c:
            print("DA NOI.\n")
            co_rieng = False
            for s in c.services:
                ngan = s.uuid.split("-")[0][-4:].lower()
                nhan = CHUAN.get(ngan)
                if nhan is None:
                    co_rieng = True
                print("service %s  %s" % (s.uuid, nhan or "<-- CUA HANG, khong phai SIG"))
                for ch in s.characteristics:
                    quyen = ",".join(ch.properties)
                    print("   char %s  [%s]" % (ch.uuid, quyen))
                    if doc_gia_tri and "read" in ch.properties:
                        try:
                            v = await c.read_gatt_char(ch)
                            print("      = %s %s" % (v.hex(), _chu(v)))
                        except Exception as e:
                            print("      doc khong duoc: %s" % type(e).__name__)
            print()
            if co_rieng:
                print("Co service rieng cua hang -> phuong an B co cua: bam notify")
                print("cua characteristic do roi cam/rut tai xem byte nao doi.")
            else:
                print("Chi co service chuan -> khong co duong nao cho so lieu cong sac.")
            return 0
    except Exception as e:
        print("Khong noi duoc: %s: %s" % (type(e).__name__, e))
        print("\nThuong gap voi thiet bi Xiaomi: chi cho noi khi dang o che do ghep doi,")
        print("hoac da ghep voi dien thoai nen tu choi may khac.")
        return 1


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--mac", required=True)
    p.add_argument("--doc", action="store_true", help="doc them gia tri cac char co quyen read")
    p.add_argument("--nghe", type=int, default=0, metavar="GIAY",
                   help="bam notify va nghe (khong ghi gi vao thiet bi)")
    p.add_argument("--char", action="append", default=None,
                   help="UUID char de bam notify; lap lai duoc. Mac dinh af08")
    ns = p.parse_args()
    if ns.nghe:
        chars = ns.char or ["0000af08-0000-1000-8000-00805f9b34fb"]
        return asyncio.run(lenh_nghe(ns.mac, chars, ns.nghe))
    return asyncio.run(chay(ns.mac, ns.doc))


if __name__ == "__main__":
    sys.exit(main())
