#!/usr/bin/env python3
"""Kiem thu phan DOC/PHAN TICH cua driver - cho de sai nhat.

Chay khong can broker, khong can phan cung:

    python -m unittest discover -s scripts/congtac_bridge -p "test_*.py"

Fixture la goi THAT bat duoc tu cong tac CozyLife 192.168.0.103 ngay
21/11/2026 (xem scripts/congtac_g0/README.md), khong phai goi tu nghi ra.
Day la cho de tu lua minh nhat: cho chay mot lan, chep ket qua ra lam moc,
roi test nao cung xanh - nhung chi chung minh duoc code KHONG DOI, khong
chung minh duoc code DUNG.
"""

from __future__ import annotations

import json
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from driver import (  # noqa: E402
    CozyLifeDriver, KetQuaDoc, LoiDoc, TuyaDriver, doc_bat, doc_tra_loi, dung_driver, sn_moi,
)

# Goi that, chep nguyen van tu phien do 21/11/2026.
TRA_LOI_HOI = (b'{"cmd":2,"pv":0,"sn":"1795195188798","msg":{"attr":[1,2],'
               b'"data":{"1":0,"2":0}},"res":0}\r\n')
TRA_LOI_LY_LICH = (b'{"cmd":0,"pv":0,"sn":"1795195188798","msg":{"attr":[0],'
                   b'"did":"727700990050c2ecbf27","dtp":"02","pid":"osP4vT",'
                   b'"mac":"0050c2ecbf27","ip":"192.168.0.103","rssi":-53,'
                   b'"sv":"1.0.0","hv":"0.0.1"},"res":0}\r\n')
SN = "1795195188798"


class TestSn(unittest.TestCase):
    def test_sn_la_chuoi_13_chu_so(self):
        """Doi mot chu la thiet bi CAM - khong tra loi, khong bao loi."""
        sn = sn_moi()
        self.assertIsInstance(sn, str)
        self.assertEqual(len(sn), 13)
        self.assertTrue(sn.isdigit())

    def test_sn_tang_dan(self):
        self.assertLessEqual(sn_moi(), sn_moi())


class TestDocTraLoi(unittest.TestCase):
    def test_doc_duoc_goi_that(self):
        o = doc_tra_loi(TRA_LOI_HOI, SN)
        self.assertIsNotNone(o)
        self.assertEqual(o["res"], 0)
        self.assertEqual(o["msg"]["data"], {"1": 0, "2": 0})

    def test_bo_qua_goi_sai_sn(self):
        """Mot so firmware tu day goi 'report' khong moi. Lay nham la doc
        trang thai cua lenh TRUOC, khong phai lenh vua gui."""
        self.assertIsNone(doc_tra_loi(TRA_LOI_HOI, "9999999999999"))

    def test_nhat_dung_goi_trong_dem_co_nhieu_dong(self):
        rac = b'{"cmd":2,"pv":0,"sn":"0000000000000","msg":{},"res":0}\r\n'
        self.assertIsNotNone(doc_tra_loi(rac + TRA_LOI_HOI, SN))

    def test_dem_vo_nua_chung_khong_nem(self):
        self.assertIsNone(doc_tra_loi(b'{"cmd":2,"pv":0,"sn":"179', SN))

    def test_rac_khong_phai_json_khong_nem(self):
        self.assertIsNone(doc_tra_loi(b"khong phai json\r\n", SN))


class TestDocBat(unittest.TestCase):
    def test_0_la_tat(self):
        self.assertIs(doc_bat({"data": {"1": 0}}, "1"), False)

    def test_255_la_bat(self):
        self.assertIs(doc_bat({"data": {"1": 255}}, "1"), True)

    def test_gia_tri_khac_0_cung_la_bat(self):
        """Mot so firmware tra 1 thay vi 255, va cong tac co muc do thi tra
        gia tri giua chung. So bang dung 255 se bao 'tat' trong khi den
        dang sang mo."""
        self.assertIs(doc_bat({"data": {"1": 1}}, "1"), True)
        self.assertIs(doc_bat({"data": {"1": 128}}, "1"), True)

    def test_thieu_dpid_tra_None_chu_khong_phai_False(self):
        """Day la noi 'khong bia so' song hoac chet. Tra False o day thi man
        hinh ve cong tac o the TAT trong khi khong ai biet den the nao."""
        self.assertIsNone(doc_bat({"data": {"2": 255}}, "1"))

    def test_khong_co_data_tra_None(self):
        self.assertIsNone(doc_bat({}, "1"))
        self.assertIsNone(doc_bat({"data": None}, "1"))

    def test_gia_tri_khong_phai_so_tra_None(self):
        self.assertIsNone(doc_bat({"data": {"1": "bat"}}, "1"))

    def test_kenh_2_doc_duoc_rieng(self):
        """Thiet bi that khai hai kenh [1, 2] - phai doc duoc tung kenh."""
        msg = json.loads(TRA_LOI_HOI.decode())["msg"]
        self.assertIs(doc_bat(msg, "1"), False)
        self.assertIs(doc_bat(msg, "2"), False)


class TestDungDriver(unittest.TestCase):
    CHUNG = {"id": "x", "ten": "X", "phong": "bed1", "loai": "den", "ip": "192.168.0.1"}

    def test_cozylife(self):
        d = dung_driver({**self.CHUNG, "he": "cozylife"})
        self.assertIsInstance(d, CozyLifeDriver)
        self.assertEqual(d.mo_ta()["driver"], "cozylife")

    def test_tuya(self):
        d = dung_driver({**self.CHUNG, "he": "tuya", "device_id": "a", "local_key": "b"})
        self.assertIsInstance(d, TuyaDriver)
        self.assertEqual(d.mo_ta()["driver"], "tuya")

    def test_he_la_thi_nem(self):
        with self.assertRaises(ValueError):
            dung_driver({**self.CHUNG, "he": "zigbee"})

    def test_mo_ta_du_truong_cho_congtac_info(self):
        d = dung_driver({**self.CHUNG, "he": "cozylife"})
        for k in ("id", "ten", "phong", "loai", "driver", "ip", "nang_luc"):
            self.assertIn(k, d.mo_ta())


class DriverGia(CozyLifeDriver):
    """Driver gia de thu `dao()` ma khong can mang."""

    def __init__(self, kq: KetQuaDoc) -> None:
        super().__init__({"id": "g", "ten": "G", "phong": "bed1", "loai": "quat",
                          "ip": "0.0.0.0"})
        self._kq = kq
        self.da_dat: list[bool] = []

    def doc(self) -> KetQuaDoc:
        return self._kq

    def dat(self, bat: bool) -> KetQuaDoc:
        self.da_dat.append(bat)
        return KetQuaDoc(True, bat, None)


class TestDao(unittest.TestCase):
    def test_dao_doc_truoc_roi_moi_dao(self):
        d = DriverGia(KetQuaDoc(True, True, None))
        self.assertIs(d.dao().is_on, False)
        self.assertEqual(d.da_dat, [False])

    def test_dao_khi_offline_thi_NEM_chu_khong_doan(self):
        """Doan o day la 'bam bat thi no tat' - rat kho chiu va rat kho tai hien."""
        d = DriverGia(KetQuaDoc(False, None, "mat dien"))
        with self.assertRaises(LoiDoc):
            d.dao()
        self.assertEqual(d.da_dat, [])

    def test_dao_khi_online_ma_khong_ro_cung_NEM(self):
        d = DriverGia(KetQuaDoc(True, None, "khong co dpid"))
        with self.assertRaises(LoiDoc):
            d.dao()
        self.assertEqual(d.da_dat, [])


class TestDienGiaiLoiTuya(unittest.TestCase):
    """Ba nguyen nhan cho ra trieu chung gan giong nhau - phai goi ten tung
    cai, khong thi nguoi ta di sua dia chi IP oan."""

    def test_giai_ma_hong_noi_den_local_key(self):
        s = TuyaDriver.dien_giai({"Error": "Unexpected Payload from Device"})
        self.assertIn("local_key", s)

    def test_timeout_noi_den_mat_dien(self):
        s = TuyaDriver.dien_giai({"Error": "Timeout waiting for device"})
        self.assertIn("mat dien", s)

    def test_tu_choi_noi_den_khe_local(self):
        s = TuyaDriver.dien_giai({"Error": "Connection refused"})
        self.assertIn("khe local", s)

    def test_unreachable_noi_den_mat_dien_hoac_doi_ip(self):
        s = TuyaDriver.dien_giai({"Error": "Network Error: Device Unreachable", "Err": "905"})
        self.assertIn("mat dien", s)
        self.assertIn("doi IP", s)

    def test_loi_la_van_noi_duoc_thanh_cau(self):
        self.assertIn("gi do moi", TuyaDriver.dien_giai({"Error": "gi do moi"}))


class _FakeTuyaDevice:
    def __init__(self, responses):
        self._responses = list(responses)
        self.closed = False

    def status(self):
        r = self._responses.pop(0)
        if isinstance(r, Exception):
            raise r
        return r

    def close(self):
        self.closed = True


class TestTuyaAutoRediscover(unittest.TestCase):
    def _make_driver(self, ip="192.168.0.100", cho_quet=0):
        return TuyaDriver({
            "id": "bed1_den", "ten": "Den", "phong": "bed1", "loai": "den",
            "ip": ip, "device_id": "eb74553feb16595327lzam",
            "local_key": "0123456789abcdef", "version": "3.3", "dp": "1",
            "mang": "192.168.0.0/24", "cho_quet": cho_quet,
        })

    def test_tuya_doc_tu_tim_lai_ip_khi_status_tra_dict_loi_905(self):
        import driver as mod
        d = self._make_driver(ip="192.168.0.100")
        old_tb = _FakeTuyaDevice([{"Error": "Network Error: Device Unreachable", "Err": "905"}])
        new_tb = _FakeTuyaDevice([{"dps": {"1": True}}])
        d._tb = old_tb
        d._thiet_bi = lambda: new_tb if d.ip == "192.168.0.107" else old_tb
        d.xac_minh = lambda ip: ip == "192.168.0.107"

        goc_quet = mod.quet_cong
        mod.quet_cong = lambda mang, cong, **kw: ["192.168.0.104", "192.168.0.107"]
        try:
            kq = d.doc()
        finally:
            mod.quet_cong = goc_quet

        self.assertTrue(kq.online)
        self.assertIs(kq.is_on, True)
        self.assertEqual(d.ip, "192.168.0.107")
        self.assertTrue(old_tb.closed)

    def test_tuya_doc_khi_ip_rong_tu_tim_ra_ip(self):
        import driver as mod
        d = self._make_driver(ip="")
        new_tb = _FakeTuyaDevice([{"dps": {"1": False}}])
        d._thiet_bi = lambda: new_tb
        d.xac_minh = lambda ip: ip == "192.168.0.107"

        goc_quet = mod.quet_cong
        mod.quet_cong = lambda mang, cong, **kw: ["192.168.0.107"]
        try:
            kq = d.doc()
        finally:
            mod.quet_cong = goc_quet

        self.assertTrue(kq.online)
        self.assertIs(kq.is_on, False)
        self.assertEqual(d.ip, "192.168.0.107")

    def test_tim_lai_ip_chay_ngay_lan_dau_khi_uptime_nho_hon_cho_quet(self):
        import driver as mod
        d = DriverTim("abc", "192.168.0.77", ["192.168.0.77"])
        d.cho_quet = 120
        goc_quet = mod.quet_cong
        goc_mono = mod.time.monotonic
        mod.quet_cong = lambda mang, cong, **kw: d._quet()
        mod.time.monotonic = lambda: 10.0
        try:
            self.assertEqual("192.168.0.77", d.tim_lai_ip())
        finally:
            mod.quet_cong = goc_quet
            mod.time.monotonic = goc_mono

    def test_cozylife_dat_thu_lai_sau_khi_tim_lai_ip(self):
        d = CozyLifeDriver({
            "id": "bed1_quat", "ten": "Quat", "phong": "bed1", "loai": "quat",
            "ip": "192.168.0.103", "did": "727700990050c2ecbf27",
            "mang": "192.168.0.0/24", "cho_quet": 0,
        })
        calls = []

        def fake_goi(cmd, msg):
            calls.append((d.ip, cmd))
            if d.ip == "192.168.0.103":
                raise LoiDoc("timeout")
            if cmd == 3:
                return {"cmd": 3, "res": 0, "msg": {}}
            return {"cmd": 2, "res": 0, "msg": {"data": {"1": 255}}}

        def fake_tim():
            d.ip = "192.168.0.108"
            return d.ip

        d._goi = fake_goi
        d.tim_lai_ip = fake_tim
        kq = d.dat(True)
        self.assertTrue(kq.online)
        self.assertIs(kq.is_on, True)
        self.assertEqual(calls, [("192.168.0.103", 3), ("192.168.0.108", 3), ("192.168.0.108", 2)])


class DriverTim(CozyLifeDriver):
    """Driver gia de thu `tim_lai_ip` ma khong can mang that."""

    def __init__(self, did: str, dung_ip: str, ung_vien: list) -> None:
        super().__init__({"id": "g", "ten": "G", "phong": "bed1", "loai": "quat",
                          "ip": "192.168.0.250", "did": did,
                          "mang": "192.168.0.0/24", "cho_quet": 0})
        self._dung_ip = dung_ip
        self._ung_vien = ung_vien
        self.da_hoi = []

    def _quet(self):
        return list(self._ung_vien)

    def xac_minh(self, ip):
        self.da_hoi.append(ip)
        return ip == self._dung_ip


class TestTimLaiIp(unittest.TestCase):
    """DHCP doi IP thi bridge phai tu tim lai - day la thu thay cho viec
    dat IP tinh tren router."""

    def _chay(self, d):
        import driver as mod
        goc = mod.quet_cong
        mod.quet_cong = lambda mang, cong, **kw: d._quet()
        try:
            return d.tim_lai_ip()
        finally:
            mod.quet_cong = goc

    def test_tim_duoc_khi_thiet_bi_doi_ip(self):
        d = DriverTim("abc", "192.168.0.77", ["192.168.0.10", "192.168.0.77"])
        self.assertEqual("192.168.0.77", self._chay(d))
        self.assertEqual("192.168.0.77", d.ip)

    def test_thu_dia_chi_cu_TRUOC_TIEN(self):
        # Truong hop thuong gap nhat: thiet bi chi vua khoi dong lai, IP cu.
        d = DriverTim("abc", "192.168.0.250", ["192.168.0.10", "192.168.0.250"])
        self.assertEqual("192.168.0.250", self._chay(d))
        self.assertEqual(["192.168.0.250"], d.da_hoi)

    def test_khong_may_nao_dung_ma_thi_tra_None_va_GIU_NGUYEN_ip(self):
        d = DriverTim("abc", "192.168.0.99", ["192.168.0.10", "192.168.0.11"])
        self.assertIsNone(self._chay(d))
        self.assertEqual("192.168.0.250", d.ip)

    def test_khong_khai_mang_thi_khong_quet(self):
        d = DriverTim("abc", "192.168.0.77", ["192.168.0.77"])
        d.mang = ""
        self.assertIsNone(self._chay(d))
        self.assertEqual([], d.da_hoi)

    def test_cho_quet_chan_quet_lien_tuc(self):
        # Thiet bi mat dien that thi khong duoc quet moi vong poll.
        d = DriverTim("abc", "192.168.0.77", ["192.168.0.77"])
        d.cho_quet = 9999
        self.assertEqual("192.168.0.77", self._chay(d))
        d2_hoi = len(d.da_hoi)
        self.assertIsNone(self._chay(d))
        self.assertEqual(d2_hoi, len(d.da_hoi), "lan hai phai bi chan")

    def test_khong_khai_did_thi_khong_nhan_bua_may_nao(self):
        # Cong 5555 cung la cong ADB cua Android - thay cong mo khong du.
        d = CozyLifeDriver({"id": "g", "ten": "G", "phong": "bed1", "loai": "quat",
                            "ip": "1.2.3.4", "mang": "192.168.0.0/24"})
        self.assertFalse(d.xac_minh("192.168.0.50"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
