#!/usr/bin/env python3
"""Driver noi chuyen voi cong tac Wi-Fi trong LAN.

Tach rieng khoi congtac_bridge.py co chu dinh: day la phan de sai nhat, va
tach ra thi test duoc ma khong can broker lan phan cung (test_driver.py).

Hai he:
  TuyaDriver     - Smart Life / Tuya, TCP 6668, can device_id + local_key
  CozyLifeDriver - CozyLife, TCP 5555, JSON thuan ket thuc CRLF, khong can khoa

LUAT CHUNG, ca hai driver deu phai giu:
  Khong doc duoc trang thai thi `is_on` la None, KHONG phai False.
  Dat False o day la bia - man hinh se ve cong tac o the TAT trong khi den
  dang sang. Day la noi nguyen tac "khong bia so" cua du an song hoac chet.
"""

from __future__ import annotations

import json
import logging
import socket
import time
from abc import ABC, abstractmethod
from typing import NamedTuple

log = logging.getLogger(__name__)

CRLF = "\r\n"


class KetQuaDoc(NamedTuple):
    online: bool
    is_on: bool | None          # None = KHONG doc duoc. Khong bao gio doan.
    ly_do_loi: str | None       # cau tieng Viet doc duoc, hien thang len app


class LoiDoc(Exception):
    """Khong doc duoc trang thai. Nem ra de `dao()` khong dam dao mo."""


# ---------------------------------------------------------------- lop chung

class Driver(ABC):
    he = "?"

    def __init__(self, cau: dict) -> None:
        self.id: str = cau["id"]
        self.ten: str = cau["ten"]
        self.phong: str = cau["phong"]
        self.loai: str = cau["loai"]
        self.ip: str = cau["ip"]
        self.timeout: float = float(cau.get("timeout", 3.0))

    @abstractmethod
    def doc(self) -> KetQuaDoc:
        """Hoi thiet bi. Khong bao gio nem - loi thi tra online=False."""

    @abstractmethod
    def dat(self, bat: bool) -> KetQuaDoc:
        """Dat XONG doc lai, tra trang thai THAT chu khong tra cai vua gui."""

    def dao(self) -> KetQuaDoc:
        """Doc truoc roi moi dao.

        Khong duoc dua vao cache: cache cu 10 giay ma trong 10 giay do co
        nguoi bam cong tac co thi dao nguoc y nguoi dung - trieu chung
        "bam bat thi no tat", rat kho chiu va rat kho tai hien.
        """
        kq = self.doc()
        if not kq.online or kq.is_on is None:
            raise LoiDoc(kq.ly_do_loi or "khong doc duoc trang thai hien tai nen khong dam dao")
        return self.dat(not kq.is_on)

    def dong(self) -> None:
        """Tha socket luc tat bridge. Mac dinh khong giu gi."""

    def mo_ta(self) -> dict:
        """Mot phan tu cua congtac/info."""
        return {
            "id": self.id,
            "ten": self.ten,
            "phong": self.phong,
            "loai": self.loai,
            "driver": self.he,
            "ip": self.ip,
            "nang_luc": ["ON", "OFF", "DAO", "DAT"],
        }


# ------------------------------------------------------------------- CozyLife

CMD_LY_LICH = 0
CMD_HOI = 2
CMD_DAT = 3


def sn_moi() -> str:
    """13 chu so mili-giay, dang CHUOI.

    DO NGAY 21/11/2026: doi mot chu la thiet bi CAM. Gui so nguyen 10 chu so
    (nhu tai lieu goc viet: sn = 1700000000) thi thiet bi khong tra loi,
    khong bao loi, chi timeout - trieu chung giong het "sai IP".
    Thieu "pv": 0 cung im lang y nhu vay.
    """
    return str(int(time.time() * 1000))


def doc_tra_loi(raw: bytes, sn: str) -> dict | None:
    """Tach dong JSON co dung `sn` ra khoi dem.

    Khop dung sn chu khong lay goi dau tien vo duoc: mot so firmware tu day
    goi "report" khong moi, lay nham la doc trang thai cua lenh truoc.
    """
    for dong in raw.split(b"\r\n"):
        if not dong.strip():
            continue
        try:
            o = json.loads(dong.decode("utf8"))
        except (ValueError, UnicodeDecodeError):
            continue
        if isinstance(o, dict) and o.get("sn") == sn:
            return o
    return None


def doc_bat(msg: dict, dpid: str) -> bool | None:
    """Lay trang thai bat/tat tu `msg` cua tra loi cmd 2.

    Tra None khi thiet bi khong noi gi ve dpid nay - khong doan la tat.

    Coi moi gia tri khac 0 la BAT, thay vi so bang dung 255: mot so firmware
    tra 1, va cong tac co muc do thi tra gia tri giua chung. So bang 255 se
    bao "tat" trong khi den dang sang mo.
    """
    data = msg.get("data")
    if not isinstance(data, dict) or dpid not in data:
        return None
    try:
        return int(data[dpid]) != 0
    except (TypeError, ValueError):
        return None


class CozyLifeDriver(Driver):
    he = "cozylife"

    def __init__(self, cau: dict) -> None:
        super().__init__(cau)
        self.cong = int(cau.get("port", 5555))
        self.dpid = str(cau.get("dpid", "1"))
        self.gia_tri_bat = int(cau.get("gia_tri_bat", 255))

    def _goi(self, cmd: int, msg: dict) -> dict:
        """Mo -> gui -> doc -> DONG.

        Dong socket moi lan la BAT BUOC, khong phai cho gon: CozyLife chi co
        vai connection slot va don rat cham. Quen dong thi sau vai chuc lenh
        quat cam toi khi rut dien. Vi vay close() nam trong `finally`.
        """
        sn = sn_moi()
        goi_tin = json.dumps({"cmd": cmd, "pv": 0, "sn": sn, "msg": msg}) + CRLF
        s = None
        try:
            s = socket.create_connection((self.ip, self.cong), timeout=self.timeout)
            s.settimeout(self.timeout)
            s.sendall(goi_tin.encode())
            dem = b""
            het = time.time() + self.timeout
            while time.time() < het:
                try:
                    phan = s.recv(4096)
                except socket.timeout:
                    break
                if not phan:
                    break
                dem += phan
                o = doc_tra_loi(dem, sn)
                if o is not None:
                    return o
            raise LoiDoc("%s:%d khong tra loi trong %gs" % (self.ip, self.cong, self.timeout))
        except OSError as e:
            raise LoiDoc("khong noi duoc %s:%d (%s)" % (self.ip, self.cong, e)) from e
        finally:
            if s is not None:
                try:
                    s.close()
                except OSError:
                    pass

    def doc(self) -> KetQuaDoc:
        try:
            o = self._goi(CMD_HOI, {"attr": [0]})
        except LoiDoc as e:
            return KetQuaDoc(False, None, str(e))
        if o.get("res") != 0:
            return KetQuaDoc(True, None, "thiet bi tra loi loi res=%s" % o.get("res"))
        bat = doc_bat(o.get("msg", {}), self.dpid)
        if bat is None:
            return KetQuaDoc(True, None, 'tra loi khong co dpid "%s"' % self.dpid)
        return KetQuaDoc(True, bat, None)

    def dat(self, bat: bool) -> KetQuaDoc:
        gt = self.gia_tri_bat if bat else 0
        try:
            o = self._goi(CMD_DAT, {"attr": [int(self.dpid)], "data": {self.dpid: gt}})
        except LoiDoc as e:
            return KetQuaDoc(False, None, str(e))
        if o.get("res") != 0:
            return KetQuaDoc(True, None, "thiet bi tu choi lenh, res=%s" % o.get("res"))
        # Doc lai tu thiet bi. Tra ve cai vua gui la bao cao MONG MUON chu
        # khong phai su that - cong tac co the da tu choi lenh.
        return self.doc()


# ----------------------------------------------------------------------- Tuya

class TuyaDriver(Driver):
    he = "tuya"

    def __init__(self, cau: dict) -> None:
        super().__init__(cau)
        self.device_id = cau["device_id"]
        self.local_key = cau["local_key"]
        self.version = str(cau.get("version", "3.3"))
        self.dp = str(cau.get("dp", "1"))
        self._tb = None
        self._loi_lien_tiep = 0

    def _thiet_bi(self):
        """Giu MOT instance, socket persistent.

        Tuya chi cho MOT ket noi local tai mot thoi diem (TCP 6668). Giu khe
        do nghia la app Smart Life tren dien thoai chuyen sang dieu khien qua
        may - van chay, cham hon. Doi lai tablet treo tuong (client chinh)
        duoc nhanh va khong phai bat tay lai moi lan.
        """
        if self._tb is None:
            import tinytuya
            tb = tinytuya.OutletDevice(self.device_id, self.ip, self.local_key)
            tb.set_version(float(self.version))
            tb.set_socketPersistent(True)
            tb.set_socketTimeout(self.timeout)
            tb.set_socketRetryLimit(1)
            self._tb = tb
        return self._tb

    def _mo_lai(self) -> None:
        """Thiet bi reboot thi socket chet lang, loi mai khong tu khoi phuc."""
        self.dong()
        self._loi_lien_tiep = 0

    def _ghi_loi(self) -> None:
        self._loi_lien_tiep += 1
        if self._loi_lien_tiep >= 2:
            self._mo_lai()

    def dong(self) -> None:
        if self._tb is not None:
            try:
                self._tb.close()
            except Exception:
                pass
            self._tb = None

    @staticmethod
    def dien_giai(tt: dict) -> str:
        """Doi loi tho cua tinytuya sang cau noi ro CHO PHAI SUA.

        Ba nguyen nhan duoi day cho ra trieu chung gan giong nhau, nen phai
        goi ten tung cai - khong thi nguoi ta di sua dia chi IP oan.
        """
        loi = str(tt.get("Error") or tt.get("Err") or tt)
        thap = loi.lower()
        if any(k in thap for k in ("json", "payload", "decrypt", "55aa", "checksum")):
            return ("giai ma that bai - local_key co the da doi sau khi ghep lai thiet bi, "
                    "hoac sai protocol version")
        if any(k in thap for k in ("timeout", "no response", "timed out")):
            return ("khong tra loi trong thoi gian cho - thiet bi mat dien, sai IP, "
                    "hoac sai protocol version")
        if any(k in thap for k in ("refused", "connect")):
            return "tu choi ket noi - khe local dang bi client khac giu (app Smart Life?)"
        return "tinytuya bao loi: %s" % loi

    def doc(self) -> KetQuaDoc:
        try:
            tt = self._thiet_bi().status()
        except Exception as e:
            self._ghi_loi()
            return KetQuaDoc(False, None, "khong noi duoc %s:6668 (%s)" % (self.ip, e))

        if not isinstance(tt, dict) or "dps" not in tt:
            self._ghi_loi()
            return KetQuaDoc(False, None, self.dien_giai(tt if isinstance(tt, dict) else {}))

        self._loi_lien_tiep = 0
        dps = tt["dps"]
        if self.dp not in dps:
            return KetQuaDoc(True, None,
                             'thiet bi khong tra dps "%s" (co: %s)' % (self.dp, list(dps)))
        return KetQuaDoc(True, bool(dps[self.dp]), None)

    def dat(self, bat: bool) -> KetQuaDoc:
        try:
            self._thiet_bi().set_value(self.dp, bat)
        except Exception as e:
            self._ghi_loi()
            return KetQuaDoc(False, None, "khong gui duoc lenh toi %s:6668 (%s)" % (self.ip, e))
        return self.doc()


# --------------------------------------------------------------------- xuong

def dung_driver(cau: dict) -> Driver:
    he = str(cau.get("he", "")).lower()
    if he == "tuya":
        return TuyaDriver(cau)
    if he == "cozylife":
        return CozyLifeDriver(cau)
    raise ValueError('khong biet he "%s" (chi co tuya / cozylife)' % he)
