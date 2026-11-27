"""Bộ kiểm thử tự động cho dịch vụ `camera_recorder` — chạy không cần camera thật, không cần mạng."""

from __future__ import annotations

import base64
import json
import sys
import tempfile
import unittest
import urllib.error
import urllib.request
from pathlib import Path

# Đảm bảo import được các module cùng thư mục khi chạy từ gốc repo
THU_MUC_NAY = Path(__file__).resolve().parent
if str(THU_MUC_NAY) not in sys.path:
    sys.path.insert(0, str(THU_MUC_NAY))

from api import MayChuPlayback, phan_tich_http_range
from cau_hinh import CauHinhRecorder, khoang_ngay_vn, phan_tich_roi
from db import CameraDb
from detector import BoGomSuKien, BoLocChuyenDong, HopNguoi, loc_nms, loc_theo_roi
from face_recognizer import (
    BoQuyetDinhChaoMat,
    KetQuaMat,
    chuyen_nguoi_la_thanh_nguoi_quen,
    danh_sach_nguoi_quen,
    don_anh_nguoi_la_qua_han,
    la_gio_ngu,
    luu_anh_mau_nguoi_quen,
    xoa_nguoi_quen,
)
from mqtt_bridge import CauNoiMqttCamera, tao_payload_state
from onvif_ptz import (
    CauHinhOnvifCam,
    QuanLyPtzOnvif,
    doc_danh_sach_onvif,
    huong_sang_vector,
    phan_tich_presets_xml,
    tao_wsse_header,
)
from recorder import BackoffTuya, MiniNvrClipper, phan_tich_moc_tu_ten_file, thu_hoach_staging_cam
from storage import BYTE_MOI_GB, QuanLyLuuTru
from telegram_notifier import TelegramNotifier, tao_multipart_photo


class TestCauHinhVaBackoff(unittest.TestCase):
    def test_phan_tich_roi_hop_le_va_loi(self) -> None:
        self.assertEqual(phan_tich_roi(""), (0.0, 0.0, 1.0, 1.0))
        self.assertEqual(phan_tich_roi("0.1, 0.2, 0.8, 0.9"), (0.1, 0.2, 0.8, 0.9))
        self.assertEqual(phan_tich_roi("0.9, 0.2, 0.1, 0.9"), (0.0, 0.0, 1.0, 1.0))
        self.assertEqual(phan_tich_roi("abc"), (0.0, 0.0, 1.0, 1.0))

    def test_backoff_tuya_tang_dan_va_dat_lai_khi_chay_on(self) -> None:
        b = BackoffTuya()
        self.assertEqual(b.ghi_nhan_ket_thuc(3.0), 15.0)
        self.assertEqual(b.ghi_nhan_ket_thuc(5.0), 45.0)
        self.assertEqual(b.ghi_nhan_ket_thuc(2.0), 120.0)
        self.assertEqual(b.ghi_nhan_ket_thuc(1.0), 270.0)
        self.assertEqual(b.ghi_nhan_ket_thuc(1.0), 270.0)
        # Sau khi luồng chạy ổn >= 120s rồi mới đứt -> quay về mức 15s đầu tiên
        self.assertEqual(b.ghi_nhan_ket_thuc(180.0), 15.0)


class TestDbVaThuHoachSegment(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.db = CameraDb(self.root / "index.db")

    def tearDown(self) -> None:
        self.db.dong()
        self.tmp.cleanup()

    def test_thu_hoach_staging_giu_lai_file_dang_ghi_moi_nhat(self) -> None:
        staging = self.root / "staging" / "cua_chinh"
        hot = self.root / "hot"
        staging.mkdir(parents=True)

        f1 = staging / "20261125_080000.mp4"
        f2 = staging / "20261125_081000.mp4"
        f3 = staging / "20261125_082000.mp4"
        for f in (f1, f2, f3):
            f.write_bytes(b"0" * 8192)

        da_chot = thu_hoach_staging_cam("cua_chinh", staging, hot, self.db, chot_tat_ca=False)
        self.assertEqual(len(da_chot), 2)
        self.assertFalse(f1.exists())
        self.assertFalse(f2.exists())
        self.assertTrue(f3.exists())  # File thứ 3 đang mở ghi -> phải giữ nguyên

        tl = self.db.dong_thoi_gian_ngay("cua_chinh", "2026-11-25")
        self.assertEqual(len(tl["segments"]), 2)
        self.assertEqual(tl["segments"][0]["duration_sec"], 600.0)
        self.assertEqual(tl["segments"][0]["tier"], "hot")

        ngay_ds = self.db.danh_sach_ngay("cua_chinh")
        self.assertEqual(len(ngay_ds), 1)
        self.assertEqual(ngay_ds[0]["date"], "2026-11-25")
        self.assertEqual(ngay_ds[0]["segment_count"], 2)


class TestStorageHotCacheVaDrive(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.rclone_conf = self.root / "rclone.conf"
        self.rclone_conf.write_text("[gdrive]\ntype = drive\n", encoding="utf-8")
        self.cfg = CauHinhRecorder(
            data_dir=self.root / "data",
            rclone_config=self.rclone_conf,
            local_cache_max_gb=0.001,       # ~1 MB để kiểm thử dọn cache
            local_emergency_max_gb=0.005,
        )
        self.db = CameraDb(self.cfg.data_dir / "index.db")
        self.remote_store: dict[str, bytes] = {}

        def gia_lap_rclone(args: list[str], timeout: float) -> tuple[int, str]:
            lenh = args[0]
            if lenh == "copyto":
                src, dst = args[1], args[2]
                if src.startswith("gdrive:"):
                    if src not in self.remote_store:
                        return 1, "not found on drive"
                    Path(dst).parent.mkdir(parents=True, exist_ok=True)
                    Path(dst).write_bytes(self.remote_store[src])
                    return 0, ""
                else:
                    self.remote_store[dst] = Path(src).read_bytes()
                    return 0, ""
            if lenh == "deletefile":
                self.remote_store.pop(args[1], None)
                return 0, ""
            return 0, ""

        self.storage = QuanLyLuuTru(self.cfg, self.db, rclone_runner=gia_lap_rclone)

    def tearDown(self) -> None:
        self.db.dong()
        self.tmp.cleanup()

    def test_day_len_drive_don_hot_cache_va_keo_lai_khi_xem_ngay_cu(self) -> None:
        dau, _ = khoang_ngay_vn("2026-11-20")
        p1 = self.storage.hot_dir / "cam1" / "2026-11-20" / "08-00-00.mp4"
        p2 = self.storage.hot_dir / "cam1" / "2026-11-20" / "08-10-00.mp4"
        p1.parent.mkdir(parents=True, exist_ok=True)
        du_lieu_1 = b"A" * 800_000
        du_lieu_2 = b"B" * 800_000
        p1.write_bytes(du_lieu_1)
        p2.write_bytes(du_lieu_2)

        id1 = self.db.them_segment("cam1", dau, dau + 600, len(du_lieu_1), str(p1))
        id2 = self.db.them_segment("cam1", dau + 600, dau + 1200, len(du_lieu_2), str(p2))

        so_up = self.storage.day_hang_doi_len_drive()
        self.assertEqual(so_up, 2)

        # Tổng 1.6 MB > ngưỡng 1 MB -> dọn file cũ nhất (id1) khỏi Hot Cache, giữ id2
        so_xoa = self.storage.don_hot_cache()
        self.assertEqual(so_xoa, 1)
        self.assertFalse(p1.exists())
        self.assertTrue(p2.exists())

        seg1 = self.db.lay_segment(id1)
        self.assertIsNotNone(seg1)
        self.assertEqual(seg1["status"], "cloud_only")

        # Khi tablet bấm tua về đoạn id1 (đã xóa local), storage tự kéo từ Drive về cold_cache!
        phat_p1 = self.storage.lay_file_phat(id1)
        self.assertIsNotNone(phat_p1)
        self.assertEqual(phat_p1.read_bytes(), du_lieu_1)
        self.assertEqual(self.storage.lay_file_phat(id2), p2)


class TestAiPhatHienNguoiVaTelegram(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.db = CameraDb(self.root / "index.db")

    def tearDown(self) -> None:
        self.db.dong()
        self.tmp.cleanup()

    def test_motion_gate_nms_roi_va_gom_cum_su_kien(self) -> None:
        w, h = 64, 36
        khung_den = bytes([10] * (w * h * 3))
        khung_sang = bytes([200] * (w * h * 3))
        bo_loc = BoLocChuyenDong(diff_thresh=20, area_pct=1.0)
        self.assertFalse(bo_loc.co_chuyen_dong(khung_den, w, h))  # Khung đầu khởi tạo nền
        self.assertFalse(bo_loc.co_chuyen_dong(khung_den, w, h))  # Đứng im -> không chuyển động
        self.assertTrue(bo_loc.co_chuyen_dong(khung_sang, w, h))  # Thay đổi lớn -> có chuyển động

        # NMS loại bỏ 2 khung đè lên nhau trên cùng 1 người
        h1 = HopNguoi(10, 10, 100, 200, 0.88)
        h2 = HopNguoi(14, 12, 102, 198, 0.65)
        h3 = HopNguoi(300, 20, 380, 210, 0.79)
        sau_nms = loc_nms([h2, h1, h3], iou_thresh=0.45)
        self.assertEqual(len(sau_nms), 2)
        self.assertAlmostEqual(sau_nms[0].conf, 0.88)

        # Lọc ROI nửa trái khung hình (0..0.5)
        trong_roi = loc_theo_roi(sau_nms, 640, 360, (0.0, 0.0, 0.5, 1.0))
        self.assertEqual(len(trong_roi), 1)

        # Gom cụm sự kiện: đòi đủ 2 khung liên tiếp mới báo, và gom khi cách nhau < 15s
        da_bao: list[tuple] = []

        def luu_gia(rgb, ww, hh, ds, dich: Path, ts: int) -> bool:
            dich.parent.mkdir(parents=True, exist_ok=True)
            dich.write_bytes(b"\xff\xd8\xff\xd9" * 64)
            return True

        gom = BoGomSuKien(
            camera="san_vuon",
            db=self.db,
            thumbs_dir=self.root / "thumbs",
            confirm_frames=2,
            event_gap_sec=15.0,
            on_su_kien_moi=lambda eid, ts, cnt, conf, p: da_bao.append((eid, cnt, conf, p)),
            luu_anh_fn=luu_gia,
        )
        t0 = 1764050000.0
        # Khung 1 thấy người -> chưa báo (chống nhiễu 1 frame)
        self.assertIsNone(gom.xu_ly_khung(t0, [h1], khung_sang, w, h))
        self.assertEqual(len(da_bao), 0)

        # Khung 2 thấy người (sau 0.5s) -> xác nhận mở sự kiện & bắn callback ngay!
        eid = gom.xu_ly_khung(t0 + 0.5, [h1], khung_sang, w, h)
        self.assertIsNotNone(eid)
        self.assertEqual(len(da_bao), 1)

        # Đi lại thêm ở t0 + 10s -> vẫn thuộc CÙNG 1 sự kiện eid, không đẻ sự kiện rác
        self.assertEqual(gom.xu_ly_khung(t0 + 10.0, [h1, h3], khung_sang, w, h), eid)
        self.assertEqual(len(da_bao), 1)

        # Sau 20s không còn ai -> tự đóng phiên với thời lượng ~10s và số người = 2
        self.assertIsNone(gom.xu_ly_khung(t0 + 26.0, [], khung_den, w, h))
        ev = self.db.lay_event(eid)
        self.assertEqual(ev["person_count"], 2)
        self.assertEqual(ev["end_ts"], int(t0 + 10))

    def test_telegram_cooldown_va_multipart(self) -> None:
        body, ctype = tao_multipart_photo("12345", "Có người", "a.jpg", b"JPEGDATA")
        self.assertIn("multipart/form-data; boundary=", ctype)
        self.assertIn(b"JPEGDATA", body)

        da_gui: list[str] = []
        cfg = CauHinhRecorder(
            data_dir=self.root,
            tg_enabled=True,
            tg_bot_token="token_gia",
            tg_chat_id="999",
            tg_cooldown_sec=45.0,
        )
        anh = self.root / "p.jpg"
        anh.write_bytes(b"JPG")
        tg = TelegramNotifier(cfg, sender=lambda url, b, h: (da_gui.append(url) or True))

        self.assertTrue(tg.bao_phat_hien_nguoi("cam1", 1764050000, 1, 0.85, anh, now_mono=100.0))
        # Lần 2 sau 10s (< 45s cooldown) -> phải chặn chống spam
        self.assertFalse(tg.bao_phat_hien_nguoi("cam1", 1764050010, 1, 0.90, anh, now_mono=110.0))
        # Lần 3 sau 50s -> cho phép gửi lại
        self.assertTrue(tg.bao_phat_hien_nguoi("cam1", 1764050050, 1, 0.91, anh, now_mono=150.0))

        while not tg._q.empty():
            tg.xu_ly_mot_muc(tg._q.get_nowait())
        self.assertEqual(len(da_gui), 2)
        self.assertTrue(all(u.endswith("/sendPhoto") for u in da_gui))


class TestHttpPlaybackApiVaMqtt(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.cfg = CauHinhRecorder(
            data_dir=self.root / "data",
            api_host="127.0.0.1",
            api_port=0,
            api_user="admin",
            api_pass="matkhau123",
            drive_enabled=False,
        )
        self.db = CameraDb(self.cfg.data_dir / "index.db")
        self.storage = QuanLyLuuTru(self.cfg, self.db)

        dau, _ = khoang_ngay_vn("2026-11-25")
        self.mp4_file = self.storage.hot_dir / "cua_chinh" / "2026-11-25" / "10-00-00.mp4"
        self.mp4_file.parent.mkdir(parents=True, exist_ok=True)
        self.mp4_bytes = bytes(range(256)) * 40  # 10,240 bytes
        self.mp4_file.write_bytes(self.mp4_bytes)
        self.seg_id = self.db.them_segment(
            "cua_chinh", dau + 36000, dau + 36600, len(self.mp4_bytes), str(self.mp4_file)
        )

        self.thumb_file = self.storage.thumbs_dir / "cua_chinh" / "2026-11-25" / "10-02-00.jpg"
        self.thumb_file.parent.mkdir(parents=True, exist_ok=True)
        self.thumb_file.write_bytes(b"\xff\xd8FAKEJPEG\xff\xd9")
        self.ev_id = self.db.them_event(
            "cua_chinh", "person", dau + 36120, dau + 36165, 0.89, 1, str(self.thumb_file)
        )

        self.api = MayChuPlayback(
            self.cfg,
            self.db,
            self.storage,
            chup_anh_cb=lambda cam: {"ok": True, "event_id": 77, "message": f"Đã chụp {cam}"},
        )
        self.port = self.api.bat_dau()
        tho = base64.b64encode(b"admin:matkhau123").decode("ascii")
        self.auth_hdr = {"Authorization": f"Basic {tho}"}

    def tearDown(self) -> None:
        self.api.dung()
        self.db.dong()
        self.tmp.cleanup()

    def test_api_chan_khong_mat_khau_va_tra_timeline_va_range_206(self) -> None:
        base = f"http://127.0.0.1:{self.port}"
        with self.assertRaises(urllib.error.HTTPError) as ctx:
            urllib.request.urlopen(f"{base}/api/health", timeout=5)
        self.assertEqual(ctx.exception.code, 401)

        # Timeline trả đúng đoạn video & mốc người
        req_tl = urllib.request.Request(
            f"{base}/api/timeline?cam=cua_chinh&date=2026-11-25", headers=self.auth_hdr
        )
        with urllib.request.urlopen(req_tl, timeout=5) as r:
            tl = json.loads(r.read().decode("utf-8"))
        self.assertEqual(len(tl["segments"]), 1)
        self.assertEqual(len(tl["events"]), 1)
        self.assertEqual(tl["events"][0]["id"], self.ev_id)

        # HTTP Range 206 Partial Content cho MediaPlayer tua video
        req_vid = urllib.request.Request(
            f"{base}/api/video/{self.seg_id}",
            headers={**self.auth_hdr, "Range": "bytes=100-299"},
        )
        with urllib.request.urlopen(req_vid, timeout=5) as r:
            self.assertEqual(r.status, 206)
            self.assertEqual(r.headers.get("Content-Range"), f"bytes 100-299/{len(self.mp4_bytes)}")
            manh = r.read()
        self.assertEqual(manh, self.mp4_bytes[100:300])

        # Thumbnail sự kiện người
        req_th = urllib.request.Request(f"{base}/api/thumb/{self.ev_id}.jpg", headers=self.auth_hdr)
        with urllib.request.urlopen(req_th, timeout=5) as r:
            self.assertEqual(r.status, 200)
            self.assertEqual(r.read(), b"\xff\xd8FAKEJPEG\xff\xd9")

    def test_mqtt_xu_ly_lenh_doi_trang_thai_va_tra_loi_id(self) -> None:
        mqtt_br = CauNoiMqttCamera(
            self.cfg,
            lay_state_cb=lambda tid: tao_payload_state(self.cfg, self.db.thong_ke_tong_hop(), {}, None, tid),
            chup_anh_cb=lambda cam: {"ok": True, "event_id": 88, "message": "OK"},
        )
        kq = mqtt_br.xu_ly_mot_lenh({"action": "SET_DETECT_PERSON", "value": False, "id": "lenh_01"})
        self.assertTrue(kq["ok"])
        self.assertEqual(kq["tra_loi_id"], "lenh_01")
        self.assertFalse(self.cfg.detect_person_enabled)

        kq_snap = mqtt_br.xu_ly_mot_lenh({"action": "SNAPSHOT", "camera": "cua_chinh", "id": "lenh_02"})
        self.assertTrue(kq_snap["ok"])
        self.assertEqual(kq_snap["event_id"], 88)


class TestNhanDienKhuonMatVaChongBaoNham(unittest.TestCase):
    def test_chao_nguoi_quen_cooldown_va_uu_tien_nguoi_quen_5_phut(self) -> None:
        bo = BoQuyetDinhChaoMat(
            known_cooldown_sec=1200.0,
            unknown_confirm_sec=2.5,
            unknown_confirm_frames=2,
            known_grace_sec=300.0,
        )
        # 10:00 giờ VN (ban ngày)
        dau_ngay, _ = khoang_ngay_vn("2026-11-27")
        t = float(dau_ngay + 10 * 3600)

        # Khung hình có CẢ người nhà ("Trung") và người lạ -> chỉ chào Trung, KHÔNG báo người lạ
        kq1 = bo.xu_ly_khung(
            "cua_chinh",
            [KetQuaMat("Trung", True, 0.72), KetQuaMat("unknown", False, 0.85)],
            now_ts=t,
            home_mode="home",
        )
        self.assertEqual(len(kq1), 1)
        self.assertTrue(kq1[0]["is_known"])
        self.assertEqual(kq1[0]["name"], "Trung")
        self.assertFalse(kq1[0]["silent"])

        # 5 phút sau vẫn trong cooldown 20 phút của Trung -> không chào lặp lại
        kq2 = bo.xu_ly_khung("cua_chinh", [KetQuaMat("Trung", True, 0.75)], now_ts=t + 200.0, home_mode="home")
        self.assertEqual(kq2, [])

        # Trong 5 phút ưu tiên người quen (grace=300s), chỉ có người lạ đứng đó -> vẫn bỏ qua
        for dt in (210.0, 212.0, 214.0):
            self.assertEqual(
                bo.xu_ly_khung("cua_chinh", [KetQuaMat("unknown", False, 0.9)], now_ts=t + dt, home_mode="home"),
                [],
            )

        # Sau 5 phút kể từ lần cuối thấy người quen (t + 200 + 305 = t + 505):
        # Xác nhận đa khung hình 2.5s cho người lạ: khung đầu chưa báo, khung sau >= 2.5s mới báo!
        self.assertEqual(
            bo.xu_ly_khung("cua_chinh", [KetQuaMat("unknown", False, 0.88)], now_ts=t + 505.0, home_mode="home"),
            [],
        )
        kq_la = bo.xu_ly_khung("cua_chinh", [KetQuaMat("unknown", False, 0.91)], now_ts=t + 508.0, home_mode="home")
        self.assertEqual(len(kq_la), 1)
        self.assertFalse(kq_la[0]["is_known"])
        self.assertEqual(kq_la[0]["greeting"], "Có khách ở cửa")
        self.assertFalse(kq_la[0]["silent"])
        self.assertFalse(kq_la[0]["send_alert"])

    def test_nguoi_la_vang_nha_va_gio_ngu_im_lang_tablet_gui_telegram(self) -> None:
        bo = BoQuyetDinhChaoMat(unknown_confirm_sec=2.0, unknown_confirm_frames=2)
        dau_ngay, _ = khoang_ngay_vn("2026-11-27")
        # 23:30 giờ VN (giờ ngủ)
        t_dem = float(dau_ngay + 23 * 3600 + 1800)
        self.assertTrue(la_gio_ngu(t_dem, 23, 6))

        bo.xu_ly_khung("cua_chinh", [KetQuaMat("unknown", False, 0.8)], now_ts=t_dem, home_mode="home")
        kq = bo.xu_ly_khung("cua_chinh", [KetQuaMat("unknown", False, 0.85)], now_ts=t_dem + 2.5, home_mode="home")
        self.assertEqual(len(kq), 1)
        self.assertEqual(kq[0]["mode"], "sleep")
        self.assertTrue(kq[0]["silent"])
        self.assertTrue(kq[0]["send_alert"])

    def test_don_anh_nguoi_la_7_ngay_va_chuyen_thanh_nguoi_quen(self) -> None:
        import os

        with tempfile.TemporaryDirectory() as tmp:
            faces = Path(tmp) / "faces"
            unk = faces / "unknown"
            unk.mkdir(parents=True)
            cu = unk / "old.jpg"
            moi = unk / "new.jpg"
            cu.write_bytes(b"OLD")
            moi.write_bytes(b"NEW")
            now = 1764250000.0
            os.utime(cu, (now - 8 * 86400, now - 8 * 86400))
            os.utime(moi, (now - 1 * 86400, now - 1 * 86400))

            da_xoa = don_anh_nguoi_la_qua_han(unk, retention_days=7, now_ts=now)
            self.assertEqual(da_xoa, 1)
            self.assertFalse(cu.exists())
            self.assertTrue(moi.exists())

            ok, _ = chuyen_nguoi_la_thanh_nguoi_quen(faces, "new.jpg", "Trung")
            self.assertTrue(ok)
            self.assertFalse(moi.exists())
            self.assertTrue((faces / "Trung" / "new.jpg").exists())


class TestOnvifPtzPrivacyVaMiniNvrApi(unittest.TestCase):
    def test_wsse_va_ptz_move_preset_privacy_mode(self) -> None:
        wsse = tao_wsse_header("admin", "123456", nonce=b"0123456789abcdef", created="2026-11-27T15:00:00Z")
        self.assertIn("PasswordDigest", wsse)
        self.assertIn("<Username>admin</Username>", wsse)
        self.assertEqual(huong_sang_vector("left", 0.6), (-0.6, 0.0, 0.0))

        xml_resp = b"""<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
            <s:Body><GetPresetsResponse xmlns="http://www.onvif.org/ver20/ptz/wsdl">
                <Preset token="1"><Name>Cua chinh</Name></Preset>
                <Preset token="2"><Name>Ban an</Name></Preset>
            </GetPresetsResponse></s:Body></s:Envelope>"""
        presets = phan_tich_presets_xml(xml_resp)
        self.assertEqual(len(presets), 2)
        self.assertEqual(presets[0]["label"], "Cua chinh")

        da_gui: list[tuple[str, str]] = []

        def fake_soap(url: str, payload: bytes, action: str) -> tuple[int, bytes]:
            da_gui.append((url, action))
            return 200, b"<ok/>"

        cams = doc_danh_sach_onvif(
            json.dumps(
                {
                    "cua_chinh": {"host": "192.168.0.101", "indoor": False},
                    "phong_khach": {"host": "192.168.0.102", "indoor": True, "privacy_preset": "99"},
                }
            )
        )
        ptz = QuanLyPtzOnvif(cams, soap_sender=fake_soap, step_duration_sec=0.2)

        r_move = ptz.di_chuyen("phong_khach", "right", 0.5, auto_stop=False)
        self.assertTrue(r_move["ok"])
        r_pre = ptz.goi_preset("phong_khach", "ban_an")
        self.assertTrue(r_pre["ok"])

        # Privacy Mode chỉ xoay camera trong nhà (`phong_khach`), không đụng camera ngoài cửa (`cua_chinh`)
        r_priv = ptz.dat_che_do_rieng_tu(True)
        self.assertTrue(r_priv["privacy_mode"])
        self.assertEqual(r_priv["affected_cameras"], ["phong_khach"])

        r_priv_off = ptz.dat_che_do_rieng_tu(False)
        self.assertFalse(r_priv_off["privacy_mode"])
        self.assertEqual(r_priv_off["affected_cameras"], ["phong_khach"])

    def test_mini_nvr_clip_api_events_va_ptz_endpoints(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            cfg = CauHinhRecorder(
                data_dir=root / "data",
                face_dir=root / "data" / "faces",
                api_host="127.0.0.1",
                api_port=0,
                api_user="u",
                api_pass="p",
                drive_enabled=False,
                clip_duration_sec=25,
            )
            db = CameraDb(cfg.data_dir / "index.db")
            storage = QuanLyLuuTru(cfg, db)
            clipper = MiniNvrClipper(cfg, db)
            cmd = clipper.tao_lenh_clip("cua_chinh", root / "c.mp4")
            self.assertIn("-c:v", cmd)
            self.assertIn("copy", cmd)
            self.assertIn("25", cmd)

            clip_file = root / "clip_ev.mp4"
            clip_file.write_bytes(b"MP4CLIPDATA" * 100)
            ev_id = db.them_event(
                "cua_chinh",
                "face_unknown",
                1764250000,
                1764250025,
                0.92,
                1,
                label="Người lạ",
                clip_local=str(clip_file),
            )
            snap_dir = cfg.face_dir / "unknown"
            snap_dir.mkdir(parents=True, exist_ok=True)
            (snap_dir / "test_la.jpg").write_bytes(b"\xff\xd8SNAP\xff\xd9")

            ptz_calls: list[tuple] = []
            api = MayChuPlayback(
                cfg,
                db,
                storage,
                ptz_move_cb=lambda c, d, s: (ptz_calls.append(("move", c, d)) or {"ok": True, "message": "OK"}),
                ptz_preset_cb=lambda c, p: (ptz_calls.append(("preset", c, p)) or {"ok": True, "message": "OK"}),
                ptz_presets_list_cb=lambda c: [{"id": "cua_chinh", "label": "Cửa chính", "token": "1"}],
                privacy_cb=lambda b: {"ok": True, "privacy_mode": b, "message": "OK"},
                home_mode_cb=lambda m: {"ok": True, "home_mode": m, "message": "OK"},
            )
            port = api.bat_dau()
            try:
                hdr = {"Authorization": "Basic " + base64.b64encode(b"u:p").decode("ascii")}
                base = f"http://127.0.0.1:{port}"

                with urllib.request.urlopen(urllib.request.Request(f"{base}/api/events?kind=face_unknown", headers=hdr)) as r:
                    evs = json.loads(r.read().decode("utf-8"))["events"]
                self.assertEqual(len(evs), 1)
                self.assertEqual(evs[0]["label"], "Người lạ")
                self.assertTrue(evs[0]["has_clip"])

                with urllib.request.urlopen(urllib.request.Request(f"{base}/api/clip/{ev_id}.mp4", headers=hdr)) as r:
                    self.assertEqual(r.read(), b"MP4CLIPDATA" * 100)

                with urllib.request.urlopen(urllib.request.Request(f"{base}/api/face/snapshot/test_la.jpg", headers=hdr)) as r:
                    self.assertEqual(r.read(), b"\xff\xd8SNAP\xff\xd9")

                req_ptz = urllib.request.Request(
                    f"{base}/api/ptz/move?cam=cua_chinh&dir=left",
                    data=b"{}",
                    headers={**hdr, "Content-Type": "application/json"},
                    method="POST",
                )
                with urllib.request.urlopen(req_ptz) as r:
                    self.assertTrue(json.loads(r.read().decode("utf-8"))["ok"])
                self.assertEqual(ptz_calls[0], ("move", "cua_chinh", "left"))
            finally:
                api.dung()
                db.dong()

    def test_face_enroll_upload_camera_people_va_delete_api(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            cfg = CauHinhRecorder(
                data_dir=root / "data",
                face_dir=root / "data" / "faces",
                api_host="127.0.0.1",
                api_port=0,
                api_user="u",
                api_pass="p",
                drive_enabled=False,
            )
            db = CameraDb(cfg.data_dir / "index.db")
            storage = QuanLyLuuTru(cfg, db)
            fake_jpeg = b"\xff\xd8\xff\xe0" + b"X" * 1024 + b"\xff\xd9"

            def enroll_upload(name: str, data: bytes) -> dict:
                ok, msg, _, cnt = luu_anh_mau_nguoi_quen(cfg.face_dir, name, data, nguon="tablet")
                return {"ok": ok, "message": msg, "count": cnt}

            def enroll_cam(name: str, cam: str) -> dict:
                ok, msg, _, cnt = luu_anh_mau_nguoi_quen(cfg.face_dir, name, fake_jpeg, nguon=f"cam_{cam}")
                return {"ok": ok, "message": msg, "count": cnt}

            api = MayChuPlayback(
                cfg,
                db,
                storage,
                face_people_cb=lambda: danh_sach_nguoi_quen(cfg.face_dir),
                face_enroll_camera_cb=enroll_cam,
                face_enroll_upload_cb=enroll_upload,
                face_delete_cb=lambda n: dict(zip(("ok", "message"), xoa_nguoi_quen(cfg.face_dir, n))),
            )
            port = api.bat_dau()
            try:
                hdr = {
                    "Authorization": "Basic " + base64.b64encode(b"u:p").decode("ascii"),
                    "Content-Type": "application/json",
                }
                base = f"http://127.0.0.1:{port}"

                # 1. Đăng ký bằng ảnh chụp từ Tablet (upload base64)
                body_up = json.dumps(
                    {"name": "Trung", "image_base64": base64.b64encode(fake_jpeg).decode("ascii")}
                ).encode("utf-8")
                with urllib.request.urlopen(
                    urllib.request.Request(f"{base}/api/face/enroll/upload", data=body_up, headers=hdr, method="POST")
                ) as r:
                    res = json.loads(r.read().decode("utf-8"))
                self.assertTrue(res["ok"])
                self.assertEqual(res["count"], 1)

                # 2. Đăng ký thêm bằng chụp từ Camera an ninh
                body_cam = json.dumps({"name": "Trung", "camera": "cam1"}).encode("utf-8")
                with urllib.request.urlopen(
                    urllib.request.Request(f"{base}/api/face/enroll/camera", data=body_cam, headers=hdr, method="POST")
                ) as r:
                    res2 = json.loads(r.read().decode("utf-8"))
                self.assertTrue(res2["ok"])
                self.assertEqual(res2["count"], 2)

                # 3. Lấy danh sách người quen & tải ảnh đại diện mẫu
                with urllib.request.urlopen(urllib.request.Request(f"{base}/api/face/people", headers=hdr)) as r:
                    people = json.loads(r.read().decode("utf-8"))["people"]
                self.assertEqual(len(people), 1)
                self.assertEqual(people[0]["name"], "Trung")
                self.assertEqual(people[0]["count"], 2)
                sample_url = people[0]["sample_url"]
                with urllib.request.urlopen(urllib.request.Request(f"{base}{sample_url}", headers=hdr)) as r:
                    self.assertEqual(r.read(), fake_jpeg)

                # 4. Xóa người quen
                body_del = json.dumps({"name": "Trung"}).encode("utf-8")
                with urllib.request.urlopen(
                    urllib.request.Request(f"{base}/api/face/delete", data=body_del, headers=hdr, method="POST")
                ) as r:
                    self.assertTrue(json.loads(r.read().decode("utf-8"))["ok"])
                self.assertEqual(danh_sach_nguoi_quen(cfg.face_dir), [])
            finally:
                api.dung()
                db.dong()


if __name__ == "__main__":
    unittest.main()
