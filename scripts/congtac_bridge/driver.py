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
from concurrent.futures import ThreadPoolExecutor
from typing import NamedTuple

log = logging.getLogger(__name__)

CRLF = "\r\n"


class KetQuaDoc(NamedTuple):
    online: bool
    is_on: bool | None          # None = KHONG doc duoc. Khong bao gio doan.
    ly_do_loi: str | None       # cau tieng Viet doc duoc, hien thang len app


class LoiDoc(Exception):
    """Khong doc duoc trang thai. Nem ra de `dao()` khong dam dao mo."""


# --------------------------------------------------------------- tim thiet bi

def quet_cong(mang: str, cong: int, timeout: float = 0.6, luong: int = 128) -> list[str]:
    """Tra ve cac IP trong dai /24 dang mo `cong`.

    Chi mo socket roi dong ngay, khong gui byte nao.

    Vi sao quet cong chu khong nghe quang ba UDP: container o mang bridge
    cua Docker KHONG nhan duoc goi broadcast cua LAN. Nghe quang ba thi
    buoc phai doi sang `network_mode: host`, tuc cho container nhin thay
    toan bo mang cua may chu - gia dat hon nhieu so voi mot vong quet 253
    dia chi moi khi mat dau thiet bi.
    """
    goc = mang.split("/")[0].rsplit(".", 1)[0]
    ds = ["%s.%d" % (goc, i) for i in range(1, 255)]

    def mo(ip: str) -> str | None:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        try:
            return ip if s.connect_ex((ip, cong)) == 0 else None
        except OSError:
            return None
        finally:
            s.close()

    with ThreadPoolExecutor(max_workers=luong) as ex:
        return [ip for ip in ex.map(mo, ds) if ip]


# ---------------------------------------------------------------- lop chung

class Driver(ABC):
    he = "?"
    #: Cong dac trung cua he, dung de quet khi phai di tim lai thiet bi.
    cong_dac_trung = 0

    def __init__(self, cau: dict) -> None:
        self.id: str = cau["id"]
        self.ten: str = cau["ten"]
        self.phong: str = cau["phong"]
        self.loai: str = cau["loai"]
        self.ip: str = cau.get("ip", "")
        self.timeout: float = float(cau.get("timeout", 3.0))
        #: Dai mang de quet khi di tim lai. Rong = tat tu dong tim.
        self.mang: str = cau.get("mang", "")
        #: Khoang cach toi thieu giua hai lan quet (giay) - quet lien tuc
        #: moi vong poll thi vua cham vua lam phien ca mang.
        self.cho_quet: float = float(cau.get("cho_quet", 120))
        self._lan_quet = 0.0

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

    def xac_minh(self, ip: str) -> bool:
        """Dung thiet bi CUA MINH co dang o `ip` khong?

        Phai hoi thiet bi va doi chieu MA CO DINH cua no, khong duoc chi
        thay cong mo roi ket luan: trong nha co the co nhieu thiet bi cung
        he (camera Tuya cung mo 6668), va nhan nham thi bat nham do.
        """
        return False

    def tim_lai_ip(self) -> str | None:
        """Quet mang tim lai thiet bi theo ma co dinh cua no.

        Day la thu thay cho viec dat IP tinh tren router: DHCP doi IP bao
        nhieu lan cung duoc, ma khong ai phai sua cau hinh.
        """
        if not self.mang or not self.cong_dac_trung:
            return None
        bay_gio = time.monotonic()
        if bay_gio - self._lan_quet < self.cho_quet:
            return None
        self._lan_quet = bay_gio

        log.info("%s: mat dau o %s, quet %s cong %d de tim lai...",
                 self.id, self.ip or "?", self.mang, self.cong_dac_trung)
        ung_vien = quet_cong(self.mang, self.cong_dac_trung)
        # Thu dia chi cu truoc neu no van nam trong danh sach - do la truong
        # hop thuong gap nhat (thiet bi chi vua khoi dong lai).
        if self.ip in ung_vien:
            ung_vien.remove(self.ip)
            ung_vien.insert(0, self.ip)
        for ip in ung_vien:
            try:
                if self.xac_minh(ip):
                    if ip != self.ip:
                        log.warning("%s: DOI IP %s -> %s", self.id, self.ip or "?", ip)
                    self.ip = ip
                    return ip
            except Exception:
                continue
        log.error("%s: quet %d may mo cong %d, khong may nao dung ma thiet bi.",
                  self.id, len(ung_vien), self.cong_dac_trung)
        return None

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
    cong_dac_trung = 5555

    def __init__(self, cau: dict) -> None:
        super().__init__(cau)
        self.cong = int(cau.get("port", 5555))
        self.cong_dac_trung = self.cong
        self.dpid = str(cau.get("dpid", "1"))
        self.gia_tri_bat = int(cau.get("gia_tri_bat", 255))
        #: Ma co dinh cua thiet bi (`did` trong tra loi cmd 0). Day moi la
        #: danh tinh; IP chi la cho no dang dung.
        self.did = str(cau.get("did", "")).strip().lower()

    def xac_minh(self, ip: str) -> bool:
        """Hoi ly lich may o `ip` va doi chieu `did`.

        Khong khai `did` thi khong xac minh duoc - tra False de khong nhan
        bua mot may la. Cong 5555 cung la cong ADB cua Android, nen "mo
        cong" tuyet doi khong du de ket luan.
        """
        if not self.did:
            return False
        cu, self.ip = self.ip, ip
        try:
            o = self._goi(CMD_LY_LICH, {"attr": [0]})
            did = str(o.get("msg", {}).get("did", "")).strip().lower()
            if did == self.did:
                return True
        except Exception:
            pass
        finally:
            if self.ip == ip:
                self.ip = cu
        return False

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
            # Khong noi duoc: co the thiet bi mat dien, cung co the DHCP da
            # doi IP cua no. Di tim mot lan roi thu lai truoc khi ket luan.
            if self.tim_lai_ip() is None:
                return KetQuaDoc(False, None, str(e))
            try:
                o = self._goi(CMD_HOI, {"attr": [0]})
            except LoiDoc as e2:
                return KetQuaDoc(False, None, str(e2))
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
    cong_dac_trung = 6668

    def xac_minh(self, ip: str) -> bool:
        """Thu noi toi `ip` bang device_id + local_key cua minh.

        Giai ma duoc la dung may: local_key chi mo duoc dung mot thiet bi.
        Camera Tuya trong nha cung mo 6668 nhung khoa khac nen se truot.
        """
        try:
            import tinytuya
        except ImportError:
            return False
        tb = None
        try:
            tb = tinytuya.OutletDevice(self.device_id, ip, self.local_key)
            tb.set_version(float(self.version))
            tb.set_socketTimeout(self.timeout)
            tb.set_socketRetryLimit(1)
            tt = tb.status()
            return isinstance(tt, dict) and "dps" in tt
        except Exception:
            return False
        finally:
            if tb is not None:
                try:
                    tb.close()
                except Exception:
                    pass

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
            cu = self.ip
            if self.tim_lai_ip() is not None and self.ip != cu:
                # Doi IP that: phai vut instance cu di, no van om dia chi cu.
                self.dong()
                try:
                    tt = self._thiet_bi().status()
                except Exception as e2:
                    return KetQuaDoc(False, None,
                                     "khong noi duoc %s:6668 (%s)" % (self.ip, e2))
            else:
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
