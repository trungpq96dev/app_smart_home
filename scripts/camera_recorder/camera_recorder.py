#!/usr/bin/env python3
"""Điểm khởi chạy chính của dịch vụ `camera_recorder` trên serverubuntu.

Tự động:
1) Hỏi danh sách camera từ `go2rtc` (hoặc lấy từ biến `CAMERAS` trong `.env`).
2) Ghi hình 24/24 hoàn toàn tự động (10 phút/file) vào Hot Cache 10 GB.
3) Chạy worker `rclone` đẩy lên Google Drive 5TB cá nhân & dọn dẹp theo kỳ hạn.
4) Chạy AI 2 tầng nhận diện người (Motion -> YOLOv8n ONNX CPU) + bắn cảnh báo Telegram.
5) Mở cổng HTTP Playback API (:1985) & MQTT (`camera/#`) cho Tablet SmartHomeApp.
"""

from __future__ import annotations

import logging
import signal
import sys
import threading
import time
from pathlib import Path
from typing import Any

from api import MayChuPlayback
from cau_hinh import CauHinhRecorder
from db import CameraDb
from detector import BoNhanDienNguoiOnnx, LuongNhanDienCamera
from face_recognizer import (
    LuongNhanDienKhuonMat,
    SuKienChaoMat,
    chuyen_nguoi_la_thanh_nguoi_quen,
    danh_sach_nguoi_quen,
    gui_ntfy_canh_bao,
    luu_anh_mau_nguoi_quen,
    xoa_nguoi_quen,
)
from mqtt_bridge import CauNoiMqttCamera, tao_payload_state
from onvif_ptz import QuanLyPtzOnvif, doc_danh_sach_onvif
from recorder import LuongGhiCamera, MiniNvrClipper, chup_anh_ngay
from storage import QuanLyLuuTru
from telegram_notifier import TelegramNotifier
from tts_viet import BoTtsTiengViet

log = logging.getLogger("camera_recorder")


class DichVuCameraRecorder:
    def __init__(self, cfg: CauHinhRecorder) -> None:
        self.cfg = cfg
        self.db = CameraDb(cfg.data_dir / "index.db")
        self.storage = QuanLyLuuTru(cfg, self.db)
        self.tg = TelegramNotifier(cfg)
        self.tts = BoTtsTiengViet(cfg.data_dir / "tts_cache")
        self.yolo = BoNhanDienNguoiOnnx(cfg)
        self.clipper = MiniNvrClipper(cfg, self.db)
        self.ptz = QuanLyPtzOnvif(
            cameras=doc_danh_sach_onvif(cfg.onvif_cameras_json, cfg.onvif_config_path)
        )
        self.home_mode = cfg.home_mode if cfg.home_mode in ("home", "away", "sleep") else "home"

        self._lock_cam = threading.Lock()
        self.recorders: dict[str, LuongGhiCamera] = {}
        self.detectors: dict[str, LuongNhanDienCamera] = {}

        self.face_worker = LuongNhanDienKhuonMat(
            cfg=cfg,
            lay_danh_sach_cam_cb=self._danh_sach_ten_cam,
            lay_home_mode_cb=lambda: self.home_mode,
            la_rieng_tu_cb=lambda cam: self.ptz.privacy_mode and self.ptz.la_cam_trong_nha(cam),
            on_su_kien_mat=self._khi_nhan_dien_mat,
        )

        self.mqtt = CauNoiMqttCamera(
            cfg=cfg,
            lay_state_cb=self.tao_state_hien_tai,
            chup_anh_cb=self.chup_anh_va_luu,
            ptz_move_cb=self.ptz.di_chuyen,
            ptz_preset_cb=self.ptz.goi_preset,
            ptz_set_preset_cb=self.ptz.luu_preset,
            privacy_cb=self.dat_che_do_rieng_tu,
            home_mode_cb=self.dat_che_do_nha,
        )
        self.api = MayChuPlayback(
            cfg=cfg,
            db=self.db,
            storage=self.storage,
            chup_anh_cb=self.chup_anh_va_luu,
            trang_thai_cb=lambda: {
                "cameras": self._anh_chup_trang_thai_cam(),
                "privacy_mode": self.ptz.privacy_mode,
                "home_mode": self.home_mode,
            },
            ptz_move_cb=self.ptz.di_chuyen,
            ptz_preset_cb=self.ptz.goi_preset,
            ptz_presets_list_cb=self.ptz.danh_sach_preset,
            privacy_cb=self.dat_che_do_rieng_tu,
            home_mode_cb=self.dat_che_do_nha,
            face_promote_cb=self.chuyen_anh_nguoi_la,
            face_people_cb=self.lay_danh_sach_nguoi_quen,
            face_enroll_camera_cb=self.them_mat_tu_camera,
            face_enroll_upload_cb=self.them_mat_tu_anh,
            face_delete_cb=self.xoa_mat_nguoi_quen,
            tts_cb=self.tts.tao_mp3,
        )
        self._chay = True

    def _danh_sach_ten_cam(self) -> list[str]:
        with self._lock_cam:
            return sorted(self.recorders.keys())

    def dat_che_do_rieng_tu(self, bat: bool) -> dict[str, Any]:
        kq = self.ptz.dat_che_do_rieng_tu(bat)
        self.mqtt.phat_state()
        return kq

    def dat_che_do_nha(self, mode: str) -> dict[str, Any]:
        m = mode.strip().lower()
        if m not in ("home", "away", "sleep"):
            return {"ok": False, "message": f"Chế độ không hợp lệ: {mode} (chọn home|away|sleep)"}
        self.home_mode = m
        self.mqtt.phat_state()
        nhan = {"home": "Ở nhà", "away": "Vắng nhà", "sleep": "Đi ngủ"}[m]
        return {"ok": True, "home_mode": m, "message": f"Đã chuyển chế độ camera sang: {nhan}."}

    def chuyen_anh_nguoi_la(self, filename: str, ten_nguoi: str) -> dict[str, Any]:
        ok, msg = chuyen_nguoi_la_thanh_nguoi_quen(self.cfg.face_dir, filename, ten_nguoi)
        if ok:
            self.face_worker.engine.lam_moi_thu_vien()
            self.face_worker.quyet_dinh.xoa_cooldown_nguoi(ten_nguoi.strip())
        return {"ok": ok, "message": msg}

    def lay_danh_sach_nguoi_quen(self) -> list[dict[str, Any]]:
        return danh_sach_nguoi_quen(self.cfg.face_dir)

    def them_mat_tu_camera(self, ten_nguoi: str, camera: str) -> dict[str, Any]:
        """Chụp khung hình HD trực tiếp từ camera an ninh (go2rtc) để thêm người quen."""
        cam_hd = camera.strip().removesuffix("_sd")
        jpeg = self.face_worker._lay_anh_go2rtc(cam_hd)
        if not jpeg:
            anh_tam = chup_anh_ngay(cam_hd, self.cfg, self.storage.thumbs_dir)
            if anh_tam and anh_tam.exists():
                jpeg = anh_tam.read_bytes()
                anh_tam.unlink(missing_ok=True)
        if not jpeg:
            return {"ok": False, "message": f"Không chụp được ảnh từ camera '{cam_hd}'. Kiểm tra go2rtc."}
        return self.them_mat_tu_anh(ten_nguoi, jpeg, nguon=f"cam_{cam_hd}")

    def them_mat_tu_anh(self, ten_nguoi: str, jpeg_bytes: bytes, nguon: str = "tablet") -> dict[str, Any]:
        """Kiểm tra & tự xoay ảnh khuôn mặt bằng YuNet, lưu vào `faces/<Ten_Nguoi>/` và nạp nóng AI."""
        co_mat, so_mat, anh_chuan = self.face_worker.engine.chuan_hoa_anh_khuon_mat(jpeg_bytes)
        if not co_mat:
            return {
                "ok": False,
                "message": "Không phát hiện khuôn mặt rõ trong ảnh — hãy đứng gần, đủ sáng và nhìn thẳng vào ống kính rồi thử lại.",
            }
        ok, msg, _, tong = luu_anh_mau_nguoi_quen(self.cfg.face_dir, ten_nguoi, anh_chuan, nguon=nguon)
        if ok:
            self.face_worker.engine.lam_moi_thu_vien()
            self.face_worker.quyet_dinh.xoa_cooldown_nguoi(ten_nguoi.strip())
        return {"ok": ok, "message": msg, "count": tong, "faces_detected": so_mat}

    def xoa_mat_nguoi_quen(self, ten_nguoi: str) -> dict[str, Any]:
        ok, msg = xoa_nguoi_quen(self.cfg.face_dir, ten_nguoi)
        if ok:
            self.face_worker.engine.lam_moi_thu_vien()
        return {"ok": ok, "message": msg}

    def _anh_chup_trang_thai_cam(self) -> dict[str, dict[str, Any]]:
        with self._lock_cam:
            return {
                ten: {
                    "recording": rec.dang_ghi,
                    "last_segment_ts": rec.last_segment_ts,
                    "error": rec.loi_gan_nhat,
                }
                for ten, rec in self.recorders.items()
            }

    def tao_state_hien_tai(self, tra_loi_id: str | None = None) -> dict[str, Any]:
        tk = self.db.thong_ke_tong_hop()
        ptz_info = self.ptz.trang_thai_tom_tat()
        return tao_payload_state(
            cfg=self.cfg,
            thong_ke_db=tk,
            trang_thai_cam=self._anh_chup_trang_thai_cam(),
            loi_drive=self.storage.loi_drive_gan_nhat,
            tra_loi_id=tra_loi_id,
            privacy_mode=ptz_info["privacy_mode"],
            home_mode=self.home_mode,
            ptz_cameras=ptz_info["ptz_cameras"],
            indoor_cameras=ptz_info["indoor_cameras"],
        )

    def _khi_nhan_dien_mat(self, ev: SuKienChaoMat, gui_canh_bao_tu_xa: bool) -> None:
        log.info(
            "[%s] Nhận diện khuôn mặt: %s (quen=%s, %.0f%%, mode=%s, silent=%s)",
            ev.camera,
            ev.name,
            ev.is_known,
            ev.confidence * 100,
            ev.mode,
            ev.silent,
        )
        kind = "face_known" if ev.is_known else "face_unknown"
        label = ev.name if ev.is_known else "Người lạ"
        ev_id = self.db.them_event(
            camera=ev.camera,
            kind=kind,
            start_ts=ev.ts,
            end_ts=ev.ts + 3,
            max_conf=ev.confidence,
            person_count=1,
            thumb_local=str(ev.snapshot_path) if ev.snapshot_path else None,
            label=label,
        )
        # Người lạ -> ghi ngay Mini-NVR clip 20-30s bằng ffmpeg -c copy (~0% CPU)
        if not ev.is_known:
            self.clipper.ghi_clip_su_kien(ev.camera, ev_id, kind, ev.ts)

        # Publish MQTT camera/face/greet (retain=False) cho Tablet chào / popup
        self.mqtt.phat_chao_khuon_mat(ev.thanh_mqtt_dict())

        # Chế độ Vắng nhà / Ngủ: gửi ảnh chụp người lạ về Telegram / ntfy
        if gui_canh_bao_tu_xa and not ev.is_known:
            self.tg.bao_nguoi_la(
                camera=ev.camera,
                start_ts=ev.ts,
                mode=ev.mode,
                anh_path=ev.snapshot_path,
                on_da_gui=lambda: self.db.danh_dau_event_telegram(ev_id),
            )
            if self.cfg.ntfy_url:
                gui_ntfy_canh_bao(
                    self.cfg.ntfy_url,
                    f"Nguoi la tai {ev.camera}",
                    f"Phat hien nguoi la ({ev.mode}) tai {ev.camera}",
                    ev.snapshot_path,
                )

    def _khi_phat_hien_nguoi(
        self,
        camera: str,
        event_id: int,
        start_ts: int,
        person_count: int,
        confidence: float,
        thumb_path: Path | None,
    ) -> None:
        if self.ptz.privacy_mode and self.ptz.la_cam_trong_nha(camera):
            return
        log.info(
            "[%s] Phát hiện người #%d (%d người, %.0f%%)",
            camera,
            event_id,
            person_count,
            confidence * 100,
        )
        # Ghi Mini-NVR clip 20-30s bằng ffmpeg -c copy để tab 'Sự kiện' phát lại tức thì
        self.clipper.ghi_clip_su_kien(camera, event_id, "person", start_ts)
        self.mqtt.phat_su_kien_nguoi(event_id, camera, start_ts, person_count, confidence)
        self.tg.bao_phat_hien_nguoi(
            camera=camera,
            start_ts=start_ts,
            person_count=person_count,
            confidence=confidence,
            anh_path=thumb_path,
            on_da_gui=lambda: self.db.danh_dau_event_telegram(event_id),
        )

    def chup_anh_va_luu(self, camera: str) -> dict[str, Any]:
        anh = chup_anh_ngay(camera, self.cfg, self.storage.thumbs_dir)
        if anh is None:
            return {
                "ok": False,
                "message": f"Không chụp được ảnh từ luồng '{camera}'. Kiểm tra go2rtc và ffmpeg.",
            }
        now_ts = int(time.time())
        ev_id = self.db.them_event(
            camera=camera,
            kind="snapshot",
            start_ts=now_ts,
            end_ts=now_ts + 1,
            max_conf=1.0,
            person_count=0,
            thumb_local=str(anh),
        )
        da_gui_tg = self.tg.gui_anh_chup_tay(camera, now_ts, anh)
        if da_gui_tg:
            self.db.danh_dau_event_telegram(ev_id)
        duoi = " & gửi Telegram" if da_gui_tg else ""
        return {
            "ok": True,
            "event_id": ev_id,
            "message": f"Đã chụp ảnh '{camera}' (lưu lịch sử/Drive{duoi}).",
        }

    def dong_bo_danh_sach_camera(self) -> None:
        ds = self.cfg.do_danh_sach_cam_go2rtc()
        if not ds:
            return
        with self._lock_cam:
            for cam in ds:
                if cam in self.recorders:
                    continue
                log.info("Kích hoạt ghi 24/24 & AI canh người cho luồng: %s", cam)
                rec = LuongGhiCamera(
                    camera=cam,
                    cfg=self.cfg,
                    db=self.db,
                    hot_dir=self.storage.hot_dir,
                    staging_dir=self.storage.staging_dir,
                )
                det = LuongNhanDienCamera(
                    camera=cam,
                    cfg=self.cfg,
                    db=self.db,
                    thumbs_dir=self.storage.thumbs_dir,
                    yolo=self.yolo,
                    on_su_kien_moi=self._khi_phat_hien_nguoi,
                )
                self.recorders[cam] = rec
                self.detectors[cam] = det
                rec.bat_dau()
                det.bat_dau()

    def chay_mai(self) -> None:
        self.tg.bat_dau()
        self.api.bat_dau()
        self.mqtt.bat_dau()
        self.face_worker.bat_dau()

        lan_do_cam = 0.0
        lan_upload_drive = 0.0
        lan_don_qua_han = 0.0
        lan_phat_mqtt = 0.0

        while self._chay:
            now = time.monotonic()

            # 1. Dò lại danh sách camera từ go2rtc mỗi 60s (nếu người dùng thêm cam mới trong go2rtc.yaml)
            if now - lan_do_cam >= 60.0 or not self.recorders:
                self.dong_bo_danh_sach_camera()
                lan_do_cam = now

            # 2. Đẩy nền lên Google Drive 5TB + dọn Hot Cache 10 GB mỗi 3s
            if now - lan_upload_drive >= 3.0:
                try:
                    self.storage.day_hang_doi_len_drive()
                    self.storage.don_hot_cache()
                except Exception as e:
                    log.warning("Lỗi vòng đồng bộ Drive/Cache: %s", e)
                lan_upload_drive = now

            # 3. Dọn file quá hạn lưu trữ trên Drive & Mini-NVR clips mỗi 6 giờ
            if now - lan_don_qua_han >= 6 * 3600.0:
                try:
                    self.storage.don_du_lieu_qua_han()
                    self.clipper.don_clip_qua_han()
                except Exception as e:
                    log.warning("Lỗi dọn dữ liệu quá hạn: %s", e)
                lan_don_qua_han = now

            # 4. Kiểm tra cảnh báo mất luồng quá lâu & cập nhật MQTT mỗi 15s
            if now - lan_phat_mqtt >= 15.0:
                with self._lock_cam:
                    for ten, rec in self.recorders.items():
                        if rec.mat_luong_tu_ts is not None:
                            phut_mat = (now - rec.mat_luong_tu_ts) / 60.0
                            if phut_mat >= self.cfg.offline_alert_min:
                                self.tg.bao_trang_thai_ket_noi(ten, True, rec.loi_gan_nhat or "")
                        elif rec.dang_ghi:
                            self.tg.bao_trang_thai_ket_noi(ten, False)
                self.mqtt.phat_state()
                lan_phat_mqtt = now

            time.sleep(2.0)

    def dung(self) -> None:
        if not self._chay:
            return
        log.info("Đang dừng camera_recorder an toàn...")
        self._chay = False
        self.face_worker.dung()
        with self._lock_cam:
            ds_rec = list(self.recorders.values())
            ds_det = list(self.detectors.values())
        for det in ds_det:
            det.dung()
        for rec in ds_rec:
            rec.dung()
        self.api.dung()
        self.mqtt.dung()
        self.tg.dung()
        self.db.dong()


def main() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )
    cfg = CauHinhRecorder.tu_moi_truong()
    dv = DichVuCameraRecorder(cfg)

    def _bat_tin_hieu(signum, frame) -> None:
        dv.dung()
        sys.exit(0)

    signal.signal(signal.SIGINT, _bat_tin_hieu)
    signal.signal(signal.SIGTERM, _bat_tin_hieu)
    dv.chay_mai()


if __name__ == "__main__":
    main()
