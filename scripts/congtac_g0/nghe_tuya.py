#!/usr/bin/env python3
"""Nghe quang ba UDP cua thiet bi Tuya -> lay device_id ma KHONG can local_key.

Thiet bi Tuya tu phat quang ba ra 255.255.255.255 vai giay mot lan:

    6666  JSON thuan            protocol 3.1 (doi cu)
    6667  AES-128-ECB, khoa co dinh   3.2 / 3.3 / 3.4
    7000  nhu tren, bien the moi      mot so 3.5

Khoa giai gói 6667 la MD5("yGAdlopoPVldABfn") - HANG SO CONG KHAI cua giao
thuc, in trong moi thu vien Tuya. No KHONG phai local_key va khong mo duoc
thiet bi nao; no chi de doc gói tu gioi thieu.

Gia tri cua phep do nay: lay duoc `gwId` (= device_id) va `version`
(= protocol) TRUOC khi ton cong mo tai khoan iot.tuya.com. Tai khoan cloud
chi con can de lay dung mot thu: local_key.

CHI NGHE, khong gui gi.

    python nghe_tuya.py --giay 120 --ra tuya_bc.jsonl

Windows: lan dau chay se hien hop thoai Firewall - PHAI chon "Private
networks". Khong hien hop thoai ma cung khong nhan goi nao = da bi chan im
lang tu truoc.
Docker: network bridge KHONG nhan duoc broadcast. Phai network_mode: host.
"""

from __future__ import annotations

import argparse
import binascii
import hashlib
import json
import socket
import sys
import time
from datetime import datetime

CONG = (6666, 6667, 7000)
KHOA = hashlib.md5(b"yGAdlopoPVldABfn").digest()
MAGIC = b"\x00\x00\x55\xaa"


def giai_aes_ecb(ct: bytes) -> bytes | None:
    try:
        from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
    except ImportError:
        return None
    if not ct or len(ct) % 16:
        return None
    d = Cipher(algorithms.AES(KHOA), modes.ECB()).decryptor()
    p = d.update(ct) + d.finalize()
    if p and 1 <= p[-1] <= 16:          # bo padding PKCS7
        p = p[: -p[-1]]
    return p


def doc_khung(data: bytes) -> dict | None:
    """Boc khung Tuya: 16 byte dau (magic+seq+cmd+len), 8 byte cuoi (crc+suffix)."""
    if not data.startswith(MAGIC) or len(data) < 24:
        return None
    than = data[20:-8]
    # 6666 la JSON thuan; 6667/7000 thi AES. Thu thang truoc.
    for thu in (than, giai_aes_ecb(than)):
        if not thu:
            continue
        try:
            o = json.loads(thu.decode("utf8").strip("\x00"))
            if isinstance(o, dict):
                return o
        except Exception:
            continue
    return None


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("--giay", type=int, default=120)
    p.add_argument("--ra", default=None, help="ghi goi tho ra file .jsonl")
    a = p.parse_args()

    socks = []
    for c in CONG:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            s.bind(("0.0.0.0", c))
            s.settimeout(0.4)
            socks.append((c, s))
        except OSError as e:
            print(f"  khong mo duoc UDP {c}: {e}")
    if not socks:
        print("Khong mo duoc cong nao. Co tien trinh khac dang giu (tinytuya scan?).")
        return 1

    print(f"Dang nghe UDP {[c for c, _ in socks]} trong {a.giay} giay. Ctrl+C de dung.\n")
    ra = open(a.ra, "a", encoding="utf8") if a.ra else None
    thay: dict[str, dict] = {}
    het = time.time() + a.giay
    try:
        while time.time() < het:
            for cong, s in socks:
                try:
                    data, addr = s.recvfrom(8192)
                except (socket.timeout, OSError):
                    continue
                luc = datetime.now().strftime("%H:%M:%S")
                if ra:
                    ra.write(json.dumps({"luc": luc, "cong": cong, "tu": addr[0],
                                         "hex": binascii.hexlify(data).decode()}) + "\n")
                    ra.flush()
                o = doc_khung(data)
                if o is None:
                    print(f"[{luc}] {addr[0]}:{cong} - khong giai duoc "
                          f"({len(data)} byte, {binascii.hexlify(data[:16]).decode()}...)")
                    continue
                gw = o.get("gwId", addr[0])
                if gw in thay:
                    continue
                thay[gw] = o
                print(f"[{luc}] {addr[0]}  cong {cong}")
                print(f"         device_id : {o.get('gwId')}")
                print(f"         version   : {o.get('version')}")
                print(f"         productKey: {o.get('productKey')}")
                print(f"         encrypt   : {o.get('encrypt')}\n")
    except KeyboardInterrupt:
        print("\n(dung bang Ctrl+C)")
    finally:
        if ra:
            ra.close()
        for _, s in socks:
            s.close()

    print(f"Xong. Tim duoc {len(thay)} thiet bi Tuya.")
    if not thay:
        print("Khong co goi nao. Kiem: Firewall Windows, dung mang chua, cong tac co dien chua.")
        return 1
    print("\nChep device_id va version vao .env cua congtac_bridge.")
    print("Con thieu local_key - xem muc G1 cua ke hoach.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
