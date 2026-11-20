#!/usr/bin/env python3
"""Giai doan 0 - do xem cu sac CUKTECH 10 Ultra co phat so lieu qua BLE khong.

Cong cu TAM, khong phai mot phan cua app. Xong giai doan 0 thi hoac chuyen vao
scripts/cuktech_collector/ hoac xoa ca thu muc nay.

Ba lenh:

  python quet_ble.py quet  [--giay 20]
      Quet tat ca thiet bi BLE quanh day. In bang: dia chi, ten, RSSI, so goi,
      do dai payload. Danh dau thiet bi co service data FE95 (Xiaomi MiBeacon).

  python quet_ble.py theo  --loc <MAC hoac mot phan ten> [--giay 180] [--ra goi.jsonl]
      Bam theo MOT thiet bi. Moi goi in ra: gio, RSSI, payload hex, va CHI SO
      BYTE da doi so voi goi truoc. Day la phep do chinh:
      chay lenh nay, cam tai vao cu sac, rut ra, xem byte co doi khong.

  python quet_ble.py doi   goi.jsonl
      Doc lai file da ghi: bao nhieu payload khac nhau, byte nao tung doi.

Chay bang venv da cai bleak (xem README.md trong cung thu muc).
Output co y de thuan ASCII - console Windows hay vo dau tieng Viet.
"""

from __future__ import annotations

import argparse
import asyncio
import json
import sys
from collections import OrderedDict
from datetime import datetime

try:
    from bleak import BleakScanner
except ImportError:
    sys.exit("Thieu bleak. Xem README.md: phai chay bang python cua venv_ble.")

# Service data UUID cua Xiaomi MiBeacon. Co dong nay la co cua de di tiep;
# khong co thi xem muc 6.2 trong docs/KE-HOACH-CUKTECH.md (phuong an B).
UUID_XIAOMI = "0000fe95-0000-1000-8000-00805f9b34fb"


def _hex(b: bytes) -> str:
    return b.hex()


# --------------------------------------------------- doc khung MiBeacon (FE95)

# Y nghia tung bit cua Frame Control (2 byte dau, little endian) theo MiBeacon v5.
_CO = [
    (0, "factory new"),
    (1, "connected"),
    (2, "central"),
    (3, "MA HOA"),
    (4, "co MAC"),
    (5, "co capability"),
    (6, "CO OBJECT (so lieu)"),
    (7, "mesh"),
    (8, "registered"),
    (9, "solicited"),
]


def doc_khung_fe95(data: bytes) -> list[str]:
    """Tra ve vai dong giai thich mot khung FE95. Khong nem loi."""
    ra: list[str] = []
    if len(data) < 5:
        return ["khung qua ngan (%d byte)" % len(data)]

    fc = int.from_bytes(data[0:2], "little")
    pid = int.from_bytes(data[2:4], "little")
    dem = data[4]
    i = 5

    bat = [ten for bit, ten in _CO if fc >> bit & 1]
    ra.append("frame control 0x%04x  version %d  authmode %d" % (fc, fc >> 12 & 0xF, fc >> 10 & 0x3))
    ra.append("co bat: " + (", ".join(bat) if bat else "(khong)"))
    ra.append("product id 0x%04x (%d)   frame counter %d" % (pid, pid, dem))

    if fc >> 4 & 1:
        if len(data) >= i + 6:
            mac = data[i:i + 6][::-1]
            ra.append("MAC trong khung: " + ":".join("%02X" % x for x in mac))
            i += 6
        else:
            ra.append("co co MAC nhung khung het byte")
            return ra
    if fc >> 5 & 1 and len(data) > i:
        ra.append("capability 0x%02x" % data[i])
        i += 1

    con = data[i:]
    if fc >> 6 & 1:
        if fc >> 3 & 1:
            ra.append("PHAN SO LIEU DA MA HOA, %d byte: %s" % (len(con), con.hex()))
            ra.append("-> can bindkey de doc. Chay giai_ma_thu.py.")
        else:
            ra.append("phan so lieu KHONG ma hoa, %d byte: %s" % (len(con), con.hex()))
            if len(con) >= 3:
                oid = int.from_bytes(con[0:2], "little")
                dai = con[2]
                ra.append("   object id 0x%04x, dai %d byte, gia tri %s"
                          % (oid, dai, con[3:3 + dai].hex()))
    else:
        ra.append("KHONG CO OBJECT -> khung nay chi la quang ba nhan dang,")
        ra.append("   khong cho so lieu nao. Con %d byte du: %s" % (len(con), con.hex() or "-"))
    return ra


def _blobs(adv) -> "OrderedDict[str, bytes]":
    """Gom moi mang byte trong mot goi quang ba, dat ten de doi chieu."""
    out: "OrderedDict[str, bytes]" = OrderedDict()
    for uuid, data in sorted((adv.service_data or {}).items()):
        nhan = "svc:" + uuid.split("-")[0][-4:]
        out[nhan] = bytes(data)
    for cid, data in sorted((adv.manufacturer_data or {}).items()):
        out["mfr:%04x" % cid] = bytes(data)
    return out


def _byte_doi(cu: bytes, moi: bytes) -> str:
    """Chi so byte khac nhau giua hai goi. Do dai khac nhau cung la mot thay doi."""
    if cu is None:
        return "(goi dau)"
    if len(cu) != len(moi):
        return "DO DAI DOI %d -> %d" % (len(cu), len(moi))
    kh = [i for i, (a, b) in enumerate(zip(cu, moi)) if a != b]
    if not kh:
        return "khong doi"
    return "byte doi: " + ",".join(str(i) for i in kh)


# ----------------------------------------------------------------- lenh: quet

async def lenh_quet(giay: int) -> int:
    thay: dict[str, dict] = {}

    def cb(dev, adv):
        d = thay.setdefault(
            dev.address,
            {"ten": None, "rssi": None, "so_goi": 0, "blob": OrderedDict(), "xiaomi": False},
        )
        d["ten"] = adv.local_name or dev.name or d["ten"]
        d["rssi"] = adv.rssi
        d["so_goi"] += 1
        for nhan, data in _blobs(adv).items():
            d["blob"][nhan] = data
        if UUID_XIAOMI in (adv.service_data or {}):
            d["xiaomi"] = True

    print("Dang quet %d giay... (de may gan cu sac)" % giay)
    scanner = BleakScanner(detection_callback=cb)
    await scanner.start()
    await asyncio.sleep(giay)
    await scanner.stop()

    if not thay:
        print("\nKHONG THAY THIET BI BLE NAO.")
        print("Kiem tra: Bluetooth cua may da bat chua; may co adapter BLE khong.")
        return 1

    print("\n%d thiet bi, sap theo song manh dan:\n" % len(thay))
    print("%-18s %-26s %6s %5s  %s" % ("DIA CHI", "TEN", "RSSI", "GOI", "PAYLOAD"))
    print("-" * 100)
    for addr, d in sorted(thay.items(), key=lambda kv: kv[1]["rssi"] or -999, reverse=True):
        mo_ta = "; ".join("%s=%s" % (k, _hex(v)) for k, v in d["blob"].items()) or "(khong co)"
        if len(mo_ta) > 46:
            mo_ta = mo_ta[:43] + "..."
        print("%-18s %-26s %6s %5d  %s%s" % (
            addr,
            (d["ten"] or "(khong ten)")[:26],
            d["rssi"],
            d["so_goi"],
            mo_ta,
            "   <-- XIAOMI FE95" if d["xiaomi"] else "",
        ))

    co_xiaomi = [a for a, d in thay.items() if d["xiaomi"]]
    print()
    if co_xiaomi:
        print("Co %d thiet bi phat MiBeacon (FE95): %s" % (len(co_xiaomi), ", ".join(co_xiaomi)))
        print("Buoc tiep: python quet_ble.py theo --loc <DIA CHI> --ra goi.jsonl")
    else:
        print("KHONG thiet bi nao phat FE95.")
        print("Neu cu sac co trong bang tren (theo ten) thi no dung giao thuc rieng")
        print("-> xem muc 6.2 trong docs/KE-HOACH-CUKTECH.md.")
        print("Neu cu sac KHONG co trong bang: co the no chi phat khi dang sac,")
        print("hoac phai bat quang ba tu app CUKTECH/Mi Home. Cam tai roi quet lai.")
    return 0


# ----------------------------------------------------------------- lenh: theo

async def lenh_theo(loc: str, giay: int, ra: str | None) -> int:
    loc_l = loc.lower()
    f = open(ra, "a", encoding="utf-8") if ra else None
    truoc: dict[str, bytes] = {}
    dem = 0

    def cb(dev, adv):
        nonlocal dem
        ten = adv.local_name or dev.name or ""
        if loc_l not in dev.address.lower() and loc_l not in ten.lower():
            return
        dem += 1
        gio = datetime.now().strftime("%H:%M:%S.%f")[:-3]
        blobs = _blobs(adv)
        if not blobs:
            print("%s  rssi=%4d  (goi khong co payload - chi ten/co)" % (gio, adv.rssi))
        for nhan, data in blobs.items():
            doi = _byte_doi(truoc.get(nhan), data)
            truoc[nhan] = data
            print("%s  rssi=%4d  %-9s %-40s %s" % (gio, adv.rssi, nhan, _hex(data), doi))
        if f:
            f.write(json.dumps({
                "t": datetime.now().isoformat(timespec="milliseconds"),
                "addr": dev.address,
                "ten": ten,
                "rssi": adv.rssi,
                "blob": {k: _hex(v) for k, v in blobs.items()},
            }, ensure_ascii=False) + "\n")
            f.flush()

    print("Bam theo thiet bi khop '%s' trong %d giay." % (loc, giay))
    print("TRONG LUC NAY: cam mot tai vao cong thu nhat, de ~20s, rut ra, de ~20s.")
    print("Neu cot cuoi khong bao gio khac 'khong doi' thi cu sac KHONG phat so theo tai.\n")
    scanner = BleakScanner(detection_callback=cb)
    await scanner.start()
    try:
        await asyncio.sleep(giay)
    except KeyboardInterrupt:
        pass
    await scanner.stop()
    if f:
        f.close()
        print("\nDa ghi %d goi vao %s" % (dem, ra))
    if dem == 0:
        print("\nKHONG BAT DUOC GOI NAO khop '%s'." % loc)
        print("Chay 'quet' truoc de lay dung dia chi.")
        return 1
    return 0


# ------------------------------------------------------------------ lenh: doi

def lenh_doi(duong_dan: str) -> int:
    theo_nhan: dict[str, list[bytes]] = {}
    n = 0
    with open(duong_dan, encoding="utf-8") as f:
        for dong in f:
            dong = dong.strip()
            if not dong:
                continue
            g = json.loads(dong)
            n += 1
            for nhan, h in g["blob"].items():
                theo_nhan.setdefault(nhan, []).append(bytes.fromhex(h))

    if not n:
        print("File rong.")
        return 1

    print("Doc %d goi tu %s\n" % (n, duong_dan))
    for nhan, ds in theo_nhan.items():
        rieng = []
        for b in ds:
            if b not in rieng:
                rieng.append(b)
        do_dai = sorted({len(b) for b in ds})
        print("[%s] %d goi, %d payload khac nhau, do dai %s" % (
            nhan, len(ds), len(rieng), do_dai))

        if len(do_dai) == 1:
            n_byte = do_dai[0]
            doi = [i for i in range(n_byte) if len({b[i] for b in ds}) > 1]
            if doi:
                print("   byte tung doi: %s" % ",".join(str(i) for i in doi))
                print("   -> co du lieu thay doi. Doi chieu voi luc cam/rut tai.")
            else:
                print("   KHONG byte nao doi -> goi tinh, khong cho so lieu.")
        for b in rieng[:6]:
            print("   %s" % b.hex())
            if nhan == "svc:fe95":
                for dong in doc_khung_fe95(b):
                    print("      " + dong)
        if len(rieng) > 6:
            print("   ... con %d payload nua" % (len(rieng) - 6))
        print()
    return 0


def lenh_khung(hex_str: str) -> int:
    h = hex_str.strip().replace(" ", "").replace(":", "")
    try:
        data = bytes.fromhex(h)
    except ValueError:
        print("Khong phai chuoi hex.")
        return 1
    for dong in doc_khung_fe95(data):
        print(dong)
    return 0


def main() -> int:
    p = argparse.ArgumentParser(description="Giai doan 0 - do BLE cua CUKTECH 10 Ultra")
    sub = p.add_subparsers(dest="lenh", required=True)

    a = sub.add_parser("quet", help="quet tat ca thiet bi BLE quanh day")
    a.add_argument("--giay", type=int, default=20)

    b = sub.add_parser("theo", help="bam theo mot thiet bi, in byte doi")
    b.add_argument("--loc", required=True, help="MAC hoac mot phan ten")
    b.add_argument("--giay", type=int, default=180)
    b.add_argument("--ra", default=None, help="ghi JSONL de phan tich sau")

    c = sub.add_parser("doi", help="tong ket mot file JSONL da ghi")
    c.add_argument("duong_dan")

    d = sub.add_parser("khung", help="doc mot khung FE95 dang hex")
    d.add_argument("hex")

    ns = p.parse_args()
    if ns.lenh == "quet":
        return asyncio.run(lenh_quet(ns.giay))
    if ns.lenh == "theo":
        return asyncio.run(lenh_theo(ns.loc, ns.giay, ns.ra))
    if ns.lenh == "khung":
        return lenh_khung(ns.hex)
    return lenh_doi(ns.duong_dan)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("\nDung.")
        sys.exit(130)
