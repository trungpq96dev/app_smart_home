#!/usr/bin/env python3
"""Quet LAN tim cong tac Tuya (TCP 6668) va CozyLife (TCP 5555).

Phep do dau tien cua Giai doan 0. CHI DOC: mo socket roi dong ngay, khong
gui mot byte nao vao thiet bi.

    python quet_lan.py
    python quet_lan.py --dai 192.168.0 --cong 6668,5555 --timeout 0.6

Cong mo chi la GOI Y, khong phai bang chung:
  - 5555 cung la cong ADB-over-TCP cua Android.
  - Co firmware CozyLife dung chip Tuya va mo ca 6668 nhung khong noi
    duoc giao thuc Tuya local.
Bang chung that nam o `nghe_tuya.py` (bat tay quang ba) va
`thu_cozylife.py` (bat tay TCP).
"""

from __future__ import annotations

import argparse
import re
import socket
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

DOAN = {6668: "Tuya / Smart Life", 5555: "CozyLife (hoac ADB Android)"}


def cong_mo(ip: str, cong: int, timeout: float) -> bool:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(timeout)
    try:
        return s.connect_ex((ip, cong)) == 0
    except OSError:
        return False
    finally:
        s.close()


def bang_arp() -> dict[str, str]:
    """Doc cache ARP de lay MAC. Chi thay may vua lien lac, nen goi SAU khi quet."""
    try:
        ra = subprocess.run(["arp", "-a"], capture_output=True, text=True, timeout=10).stdout
    except Exception:
        return {}
    kq: dict[str, str] = {}
    for d in ra.splitlines():
        m = re.search(r"(\d+\.\d+\.\d+\.\d+)\s+([0-9a-fA-F]{2}[-:][0-9a-fA-F:-]{14,})", d)
        if m:
            kq[m.group(1)] = m.group(2).lower().replace("-", ":")
    return kq


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--dai", default="192.168.0", help="ba so dau cua mang")
    p.add_argument("--cong", default="6668,5555")
    p.add_argument("--timeout", type=float, default=0.6)
    p.add_argument("--luong", type=int, default=128)
    a = p.parse_args()

    congs = [int(c) for c in a.cong.split(",") if c.strip()]
    viec = [(f"{a.dai}.{i}", c) for i in range(2, 255) for c in congs]

    print(f"Quet {a.dai}.2-254 o cong {congs} ...")
    thay: dict[str, list[int]] = {}
    with ThreadPoolExecutor(max_workers=a.luong) as ex:
        for (ip, c), mo in zip(viec, ex.map(lambda v: cong_mo(v[0], v[1], a.timeout), viec)):
            if mo:
                thay.setdefault(ip, []).append(c)

    if not thay:
        print("\nKhong thay may nao mo cac cong tren.")
        print("Kiem: may nay co dang o dung mang do khong (ipconfig), cong tac co dien khong.")
        return 1

    arp = bang_arp()
    print(f"\n{'IP':<16}{'MAC':<20}{'CONG':<8}DOAN")
    print("-" * 68)
    for ip in sorted(thay, key=lambda x: int(x.split(".")[-1])):
        cs = thay[ip]
        print(f"{ip:<16}{arp.get(ip, '?'):<20}{','.join(map(str, cs)):<8}"
              f"{' + '.join(DOAN.get(c, str(c)) for c in cs)}")
    print("\nBuoc tiep: nghe_tuya.py (xac nhan Tuya) va thu_cozylife.py (xac nhan CozyLife).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
