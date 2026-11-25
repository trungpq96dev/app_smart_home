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
from mqtt_bridge import CauNoiMqttCamera, tao_payload_state
from recorder import LuongGhiCamera, chup_anh_ngay
from storage import QuanLyLuuTru
from telegram_notifier import TelegramNotifier

log = logging.getLogger("camera_recorder")


class DichVuCameraRecorder:
    def __init__(self, cfg: CauHinhRecorder) -> None:
        self.cfg = cfg
        self.db = CameraDb(cfg.data_dir / "index.db")
        self.storage = QuanLyLuuTru(cfg, self.db)
        self.tg = TelegramNotifier(cfg)
        self.yolo = BoNhanDienNguoiOnnx(cfg)

        self._lock_cam = threading.Lock()
        self.recorders: dict[str, LuongGhiCamera] = {}
        self.detectors: dict[str, LuongNhanDienCamera] = {}

        self.mqtt = CauNoiMqttCamera(
            cfg=cfg,
            lay_state_cb=self.tao_state_hien_tai,
            chup_anh_cb=self.chup_anh_va_luu,
        )
        self.api = MayChuPlayback(
            cfg=cfg,
            db=self.db,
            storage=self.storage,
            chup_anh_cb=self.chup_anh_va_luu,
            trang_thai_cb=lambda: {"cameras": self._anh_chup_trang_thai_cam()},
        )
        self._chay = True

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
        return tao_payload_state(
            cfg=self.cfg,
            thong_ke_db=tk,
            trang_thai_cam=self._anh_chup_trang_thai_cam(),
            loi_drive=self.storage.loi_drive_gan_nhat,
            tra_loi_id=tra_loi_id,
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
        log.info(
            "[%s] Phát hiện người #%d (%d người, %.0f%%)",
            camera,
            event_id,
            person_count,
            confidence * 100,
        )
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

            # 2. Đẩy nền lên Google Drive 5TB + dọn Hot Cache 10 GB mỗi 15s
            if now - lan_upload_drive >= 15.0:
                try:
                    self.storage.day_hang_doi_len_drive()
                    self.storage.don_hot_cache()
                except Exception as e:
                    log.warning("Lỗi vòng đồng bộ Drive/Cache: %s", e)
                lan_upload_drive = now

            # 3. Dọn file quá hạn lưu trữ trên Drive mỗi 6 giờ
            if now - lan_don_qua_han >= 6 * 3600.0:
                try:
                    self.storage.don_du_lieu_qua_han()
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
