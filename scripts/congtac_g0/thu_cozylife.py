#!/usr/bin/env python3
"""Noi chuyen voi cong tac CozyLife qua TCP 5555. CHI HOI, khong dieu khien.

    python thu_cozylife.py --ip 192.168.0.103 lyLich
    python thu_cozylife.py --ip 192.168.0.103 hoi
    python thu_cozylife.py --ip 192.168.0.103 theo --giay 120

Lenh `theo` la phep do G0-c: no hoi lai moi 2 giay va in ra khi co kenh nao
DOI. Trong luc no chay, bat/tat quat bang nut tren tuong (hoac bang app
CozyLife) - kenh nao nhay la kenh do.

--- BA CHO TAI LIEU GOC GHI SAI, da do ngay 21/11/2026 ---

1. `sn` PHAI la chuoi 13 chu so mili-giay: str(int(time.time()*1000)).
   Tai lieu goc viet "sn": 1700000000 (so nguyen 10 chu so). Gui nhu the
   thi thiet bi IM LANG TUYET DOI - khong tra loi, khong bao loi, chi
   timeout. Trieu chung giong het "sai IP" va "thiet bi hong".
2. Thieu "pv": 0 cung im lang y nhu vay. Khong phai truong tuy chon.
3. `res` nam o cap NGOAI CUNG, khong nam trong `msg`. res=0 ok, res=1 loi.

Va thiet bi do duoc khai HAI kenh attr [1, 2], khong phai mot.
"""

from __future__ import annotations

import argparse
import json
import socket
import sys
import time

CMD_LY_LICH = 0
CMD_HOI = 2
# CMD_DAT = 3 - co y KHONG dat hang so o day. File nay chi duoc phep hoi.


def sn_moi() -> str:
    """13 chu so mili-giay, dang CHUOI. Doi mot chu la thiet bi cam."""
    return str(int(time.time() * 1000))


def goi(ip: str, cong: int, cmd: int, msg: dict, timeout: float = 5.0) -> dict | None:
    """Mo -> gui -> doc -> DONG. Dong socket moi lan la co y: CozyLife chi co
    vai connection slot va don rat cham."""
    sn = sn_moi()
    goi_tin = json.dumps({"cmd": cmd, "pv": 0, "sn": sn, "msg": msg}) + "\r\n"
    s = None
    try:
        s = socket.create_connection((ip, cong), timeout=timeout)
        s.settimeout(timeout)
        s.sendall(goi_tin.encode())
        dem = b""
        het = time.time() + timeout
        while time.time() < het:
            try:
                phan = s.recv(4096)
            except socket.timeout:
                break
            if not phan:
                break
            dem += phan
            for dong in dem.split(b"\r\n"):
                if not dong.strip():
                    continue
                try:
                    o = json.loads(dong.decode("utf8"))
                except Exception:
                    continue
                # Khop dung sn: mot so firmware tu day goi "report" khong moi.
                if o.get("sn") == sn:
                    return o
        return None
    except OSError as e:
        print(f"  loi mang: {e}")
        return None
    finally:
        if s is not None:
            s.close()


def in_trang_thai(o: dict | None) -> dict | None:
    if o is None:
        print("  IM LANG - khong tra loi trong thoi gian cho.")
        print("  Kiem: sn co dung 13 chu so chuoi khong, co 'pv':0 khong, IP dung chua.")
        return None
    if o.get("res") != 0:
        print(f"  thiet bi tra loi LOI: res={o.get('res')}  {json.dumps(o, ensure_ascii=False)}")
        return None
    return o.get("msg", {})


def main() -> int:
    p = argparse.ArgumentParser()
    p.add_argument("viec", choices=["lyLich", "hoi", "theo"])
    p.add_argument("--ip", required=True)
    p.add_argument("--cong", type=int, default=5555)
    p.add_argument("--giay", type=int, default=120)
    p.add_argument("--nhip", type=float, default=2.0)
    a = p.parse_args()

    if a.viec == "lyLich":
        msg = in_trang_thai(goi(a.ip, a.cong, CMD_LY_LICH, {"attr": [0]}))
        if msg is None:
            return 1
        for k in ("did", "dtp", "pid", "mac", "ip", "rssi", "sv", "hv"):
            if k in msg:
                print(f"  {k:<5}: {msg[k]}")
        return 0

    if a.viec == "hoi":
        msg = in_trang_thai(goi(a.ip, a.cong, CMD_HOI, {"attr": [0]}))
        if msg is None:
            return 1
        print(f"  attr: {msg.get('attr')}")
        print(f"  data: {msg.get('data')}")
        return 0

    # theo - phep do G0-c
    print(f"Hoi {a.ip}:{a.cong} moi {a.nhip}s trong {a.giay}s.")
    print("BAY GIO: bat roi tat quat bang nut tren tuong. Kenh nao doi la kenh cua quat.\n")
    truoc: dict | None = None
    doi_o: set[str] = set()
    het = time.time() + a.giay
    try:
        while time.time() < het:
            msg = goi(a.ip, a.cong, CMD_HOI, {"attr": [0]})
            if msg is None or msg.get("res") != 0:
                print(f"  [{time.strftime('%H:%M:%S')}] khong doc duoc")
                time.sleep(a.nhip)
                continue
            data = msg.get("msg", {}).get("data", {})
            if truoc is None:
                print(f"  [{time.strftime('%H:%M:%S')}] moc dau: {data}")
            elif data != truoc:
                kenh = [k for k in set(data) | set(truoc) if data.get(k) != truoc.get(k)]
                doi_o.update(kenh)
                print(f"  [{time.strftime('%H:%M:%S')}] DOI o kenh {kenh}: "
                      f"{ {k: (truoc.get(k), data.get(k)) for k in kenh} }")
            truoc = data
            time.sleep(a.nhip)
    except KeyboardInterrupt:
        print("\n(dung bang Ctrl+C)")

    print()
    if not doi_o:
        print("KHONG kenh nao doi. Hoac chua ai bam cong tac, hoac cong tac co")
        print("khong bao ve thiet bi - neu vay thi ca co che 'thay duoc khi nguoi")
        print("bam tay' sup, va thiet ke phai doi.")
        return 1
    print(f"Cac kenh da doi: {sorted(doi_o)}")
    if len(doi_o) == 1:
        k = sorted(doi_o)[0]
        print(f"=> dpid cua quat la \"{k}\". Dien QUAT_DPID={k} vao .env.")
    else:
        print("Nhieu hon mot kenh doi - bam lai tung nut mot de tach ra.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
