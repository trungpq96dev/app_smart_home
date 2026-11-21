#!/usr/bin/env python3
"""Noi luong HCI cua tram ESP32-C3 vao BlueZ tren serverubuntu.

BlueZ chi biet nhan mot adapter qua **cong noi tiep** (`btattach -P h4`), con
tram thi phat HCI qua **TCP**. Script nay dung o giua: mo mot pty, chep byte
hai chieu giua pty do va socket TCP cua tram. `btattach` nhin vao pty va
tuong no dang cam mot con dongle UART.

    btattach  <->  /dev/pts/N  <->  script nay  <->  TCP  <->  ESP32-C3

Khong dung `socat` vi may chu khong co san no, va viec can lam thi `pty` +
`socket` cua thu vien chuan lam duoc het - them mot goi he thong nua chi de
chep byte la khong dang.

KHONG tu goi `btattach`: lenh do can quyen root, ma script nay khong nen chay
bang root chi de chep byte. Script in ra dung dong lenh can chay roi dung do.

Dung:

    python3 hci_tcp_pty.py 192.168.0.123          # in ra /dev/pts/N roi noi
    python3 hci_tcp_pty.py 192.168.0.123 --lien-ket /tmp/ttyESPBT

Roi o mot cua so khac:

    sudo btattach -B /tmp/ttyESPBT -P h4
"""

from __future__ import annotations

import argparse
import errno
import os
import pty
import select
import signal
import socket
import sys
import time

# Cong mac dinh phai khop CH_CONG_HCI trong main/cau_hinh.h.
CONG_MAC_DINH = 9000


def noi_toi_tram(dia_chi: str, cong: int, cho_toi_da: float) -> socket.socket:
    """Noi TCP toi tram, thu lai den khi duoc.

    Tram co the dang khoi dong lai (mat dien, nap lai firmware). Bo cuoc ngay
    lan dau co nghia la moi lan tram reset la phai vao tay chay lai script -
    dung thu de tu dong hoa mot nua.
    """
    bat_dau = time.monotonic()
    lan = 0
    while True:
        try:
            s = socket.create_connection((dia_chi, cong), timeout=10)
            s.settimeout(None)
            # Nagle gom cac goi HCI nho lai toi 40 ms - du de BlueZ het kien
            # nhan va reset ca adapter.
            s.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
            return s
        except OSError as e:
            lan += 1
            if time.monotonic() - bat_dau > cho_toi_da:
                raise
            cho = min(5.0, 0.5 * lan)
            print(f"chua noi duoc toi {dia_chi}:{cong} ({e}) - thu lai sau {cho:.1f}s",
                  file=sys.stderr)
            time.sleep(cho)


def chep_hai_chieu(fd_pty: int, s: socket.socket) -> None:
    """Chep byte giua pty va socket cho den khi mot ben dong.

    Chep THO, khong cat theo goi HCI: ca hai dau deu la luong byte H4 va ben
    kia (ESP32-C3 / BlueZ) tu biet ranh goi o dau. Cat goi o day chi them mot
    cho nua de sai.
    """
    sock_fd = s.fileno()
    while True:
        san_sang, _, loi = select.select([fd_pty, sock_fd], [], [fd_pty, sock_fd], 30)
        if loi:
            return
        for fd in san_sang:
            if fd == fd_pty:
                try:
                    data = os.read(fd_pty, 4096)
                except OSError as e:
                    # EIO = chua ai mo dau slave (btattach chua chay, hoac vua
                    # thoat). Khong phai loi - cho tiep.
                    if e.errno in (errno.EIO, errno.EAGAIN):
                        time.sleep(0.05)
                        continue
                    raise
                if data:
                    s.sendall(data)
            else:
                data = s.recv(4096)
                if not data:
                    print("tram da dong ket noi", file=sys.stderr)
                    return
                os.write(fd_pty, data)


def chay() -> int:
    p = argparse.ArgumentParser(description="Cau HCI giua BlueZ va tram ESP32-C3")
    p.add_argument("dia_chi", help="IP cua tram ESP32-C3")
    p.add_argument("--cong", type=int, default=CONG_MAC_DINH)
    p.add_argument("--lien-ket", default="",
                   help="tao symlink co ten co dinh toi pty (vd /tmp/ttyESPBT)")
    p.add_argument("--cho", type=float, default=3600.0,
                   help="giay toi da chiu kho thu noi lai tram (mac dinh 1 gio)")
    tham_so = p.parse_args()

    master, slave = pty.openpty()
    ten = os.ttyname(slave)
    # Khong dong `slave` o day: dong no la pty khong con dau nao mo, va moi
    # lan btattach thoat thi `os.read` tra EOF vinh vien thay vi EIO tam thoi.

    lien_ket = tham_so.lien_ket
    if lien_ket:
        try:
            if os.path.islink(lien_ket) or os.path.exists(lien_ket):
                os.unlink(lien_ket)
            os.symlink(ten, lien_ket)
        except OSError as e:
            print(f"khong tao duoc symlink {lien_ket}: {e}", file=sys.stderr)
            lien_ket = ""

    duong = lien_ket or ten
    print(f"pty: {ten}")
    if lien_ket:
        print(f"symlink: {lien_ket} -> {ten}")
    print()
    print("Mo mot cua so khac roi chay:")
    print(f"    sudo btattach -B {duong} -P h4")
    print()

    dang_chay = True

    def dung(*_a: object) -> None:
        nonlocal dang_chay
        dang_chay = False
        raise KeyboardInterrupt

    signal.signal(signal.SIGTERM, dung)

    try:
        while dang_chay:
            s = noi_toi_tram(tham_so.dia_chi, tham_so.cong, tham_so.cho)
            print(f"da noi toi tram {tham_so.dia_chi}:{tham_so.cong}", file=sys.stderr)
            try:
                chep_hai_chieu(master, s)
            finally:
                s.close()
            if dang_chay:
                print("mat ket noi toi tram - thu noi lai", file=sys.stderr)
                time.sleep(1)
    except KeyboardInterrupt:
        pass
    finally:
        if lien_ket and os.path.islink(lien_ket):
            try:
                os.unlink(lien_ket)
            except OSError:
                pass
        os.close(slave)
        os.close(master)
    return 0


if __name__ == "__main__":
    sys.exit(chay())
