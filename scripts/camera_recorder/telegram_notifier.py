"""Gửi thông báo Telegram tức thì khi phát hiện người hoặc khi camera mất kết nối lâu.

Dùng thuần `urllib.request` (multipart/form-data) để không kéo thêm thư viện ngoài.
Chạy trên luồng nền có hàng đợi và giãn cách (cooldown) theo từng camera.
"""

from __future__ import annotations

import json
import logging
import queue
import threading
import time
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from typing import Callable

from cau_hinh import CauHinhRecorder, gio_vn_str

log = logging.getLogger("camera_recorder.telegram")


def tao_multipart_photo(chat_id: str, caption: str, ten_file: str, anh_bytes: bytes) -> tuple[bytes, str]:
    boundary = "----SmartHomeCam" + uuid.uuid4().hex
    dong: list[bytes] = []

    def _field(name: str, value: str) -> None:
        dong.append(f"--{boundary}\r\n".encode("utf-8"))
        dong.append(f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8"))
        dong.append(value.encode("utf-8") + b"\r\n")

    _field("chat_id", chat_id)
    _field("caption", caption)
    dong.append(f"--{boundary}\r\n".encode("utf-8"))
    dong.append(
        f'Content-Disposition: form-data; name="photo"; filename="{ten_file}"\r\n'.encode("utf-8")
    )
    dong.append(b"Content-Type: image/jpeg\r\n\r\n")
    dong.append(anh_bytes + b"\r\n")
    dong.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(dong), f"multipart/form-data; boundary={boundary}"


class TelegramNotifier:
    def __init__(
        self,
        cfg: CauHinhRecorder,
        sender: Callable[[str, bytes, dict[str, str]], bool] | None = None,
    ) -> None:
        self.cfg = cfg
        self._sender = sender or self._gui_http_that
        self._q: queue.Queue[dict | None] = queue.Queue(maxsize=64)
        self._lan_bao_nguoi_cuoi: dict[str, float] = {}
        self._dang_bao_mat_ket_noi: set[str] = set()
        self._chay = False
        self._luong: threading.Thread | None = None

    def bat_dau(self) -> None:
        if not self.cfg.tg_enabled or self._chay:
            return
        self._chay = True
        self._luong = threading.Thread(target=self._vong_gui, name="tg-worker", daemon=True)
        self._luong.start()

    def dung(self) -> None:
        self._chay = False
        try:
            self._q.put_nowait(None)
        except queue.Full:
            pass
        if self._luong:
            self._luong.join(timeout=3.0)

    def bao_phat_hien_nguoi(
        self,
        camera: str,
        start_ts: int,
        person_count: int,
        confidence: float,
        anh_path: Path | None,
        on_da_gui: Callable[[], None] | None = None,
        now_mono: float | None = None,
    ) -> bool:
        """Đưa cảnh báo có người vào hàng đợi nếu đã qua thời gian cooldown."""
        if not self.cfg.tg_enabled:
            return False
        now = now_mono if now_mono is not None else time.monotonic()
        cuoi = self._lan_bao_nguoi_cuoi.get(camera)
        if cuoi is not None and (now - cuoi) < self.cfg.tg_cooldown_sec:
            return False

        self._lan_bao_nguoi_cuoi[camera] = now
        caption = (
            f"🚶 Phát hiện người ({person_count}) · {int(round(confidence * 100))}%\n"
            f"📷 Camera: {camera}\n"
            f"🕒 Lúc: {gio_vn_str(start_ts)}"
        )
        muc = {
            "loai": "photo" if (anh_path and anh_path.exists()) else "text",
            "caption": caption,
            "anh_path": anh_path,
            "on_da_gui": on_da_gui,
        }
        try:
            self._q.put_nowait(muc)
            return True
        except queue.Full:
            log.warning("Hàng đợi Telegram đầy, bỏ qua 1 thông báo.")
            return False

    def gui_anh_chup_tay(self, camera: str, ts: int, anh_path: Path) -> bool:
        if not self.cfg.tg_enabled or not anh_path.exists():
            return False
        caption = f"📸 Ảnh chụp từ SmartHomeApp\n📷 Camera: {camera}\n🕒 Lúc: {gio_vn_str(ts)}"
        try:
            self._q.put_nowait({"loai": "photo", "caption": caption, "anh_path": anh_path, "on_da_gui": None})
            return True
        except queue.Full:
            return False

    def bao_trang_thai_ket_noi(self, camera: str, mat_ket_noi: bool, chi_tiet: str = "") -> None:
        if not self.cfg.tg_enabled:
            return
        if mat_ket_noi and camera not in self._dang_bao_mat_ket_noi:
            self._dang_bao_mat_ket_noi.add(camera)
            noi_dung = f"⚠️ Camera '{camera}' mất luồng ghi quá {int(self.cfg.offline_alert_min)} phút.\n{chi_tiet}".strip()
            try:
                self._q.put_nowait({"loai": "text", "caption": noi_dung, "anh_path": None, "on_da_gui": None})
            except queue.Full:
                pass
        elif not mat_ket_noi and camera in self._dang_bao_mat_ket_noi:
            self._dang_bao_mat_ket_noi.discard(camera)
            noi_dung = f"✅ Camera '{camera}' đã ghi hình 24/24 trở lại lúc {gio_vn_str()}."
            try:
                self._q.put_nowait({"loai": "text", "caption": noi_dung, "anh_path": None, "on_da_gui": None})
            except queue.Full:
                pass

    def xu_ly_mot_muc(self, muc: dict) -> bool:
        base = f"https://api.telegram.org/bot{self.cfg.tg_bot_token}"
        anh_path: Path | None = muc.get("anh_path")
        if muc.get("loai") == "photo" and anh_path and anh_path.exists():
            try:
                than, ctype = tao_multipart_photo(
                    self.cfg.tg_chat_id,
                    muc["caption"],
                    anh_path.name,
                    anh_path.read_bytes(),
                )
                ok = self._sender(f"{base}/sendPhoto", than, {"Content-Type": ctype})
            except OSError as e:
                log.warning("Không đọc được ảnh gửi Telegram %s: %s", anh_path, e)
                ok = False
        else:
            than = urllib.parse.urlencode(
                {"chat_id": self.cfg.tg_chat_id, "text": muc["caption"]}
            ).encode("utf-8")
            ok = self._sender(
                f"{base}/sendMessage",
                than,
                {"Content-Type": "application/x-www-form-urlencoded"},
            )

        if ok and callable(muc.get("on_da_gui")):
            try:
                muc["on_da_gui"]()
            except Exception:
                pass
        return ok

    def _vong_gui(self) -> None:
        while self._chay:
            try:
                muc = self._q.get(timeout=2.0)
            except queue.Empty:
                continue
            if muc is None:
                break
            self.xu_ly_mot_muc(muc)

    @staticmethod
    def _gui_http_that(url: str, body: bytes, headers: dict[str, str]) -> bool:
        req = urllib.request.Request(url, data=body, headers=headers, method="POST")
        try:
            with urllib.request.urlopen(req, timeout=20.0) as resp:
                if 200 <= resp.status < 300:
                    return True
                log.warning("Telegram trả mã %s", resp.status)
                return False
        except Exception as e:
            log.warning("Lỗi gửi Telegram: %s", e)
            return False
