"""Ghi hình camera 24/24 tự động bằng `ffmpeg` theo từng đoạn (mặc định 10 phút)
và hỗ trợ chụp ảnh nhanh từ `go2rtc`.

Điểm sống còn với camera Tuya (xem CAU-HINH.txt mục 4):
- Không mở kết nối thẳng tới camera, chỉ đọc RTSP nội bộ từ `go2rtc`.
- Khi mất luồng, tự nối lại với độ trễ tăng dần (15s -> 45s -> 120s -> 270s)
  để tuyệt đối không dính đòn khóa tạm 4 phút của máy chủ Tuya.
- Dùng MP4 phân mảnh (`frag_keyframe+empty_moov+default_base_moof`) để lỡ mất
  điện giữa chừng thì đoạn đang ghi dở vẫn phát lại được bình thường.
"""

from __future__ import annotations

import base64
import logging
import re
import shutil
import subprocess
import threading
import time
import urllib.parse
import urllib.request
from datetime import datetime
from pathlib import Path

from cau_hinh import GIO_VN, CauHinhRecorder, ngay_vn_str
from db import CameraDb

log = logging.getLogger("camera_recorder.recorder")

_MAU_TEN_SEGMENT = re.compile(r"^(\d{8})_(\d{6})\.mp4$")


class BackoffTuya:
    """Bộ tính thời gian nghỉ tăng dần chống Tuya chặn tạm 4 phút."""

    BUOC_GIAY = (15.0, 45.0, 120.0, 270.0)
    NGUONG_CHAY_ON_GIAY = 120.0

    def __init__(self) -> None:
        self.lan_loi = 0

    def ghi_nhan_ket_thuc(self, thoi_gian_chay_giay: float) -> float:
        """Trả về số giây cần nghỉ trước lần nối lại tiếp theo."""
        if thoi_gian_chay_giay >= self.NGUONG_CHAY_ON_GIAY:
            self.lan_loi = 0
        idx = min(self.lan_loi, len(self.BUOC_GIAY) - 1)
        cho = self.BUOC_GIAY[idx]
        self.lan_loi += 1
        return cho

    def dat_lai(self) -> None:
        self.lan_loi = 0


def phan_tich_moc_tu_ten_file(ten_file: str) -> int | None:
    """Đọc mốc Unix timestamp từ tên file staging `YYYYMMDD_HHMMSS.mp4` (giờ VN +7)."""
    m = _MAU_TEN_SEGMENT.match(ten_file)
    if not m:
        return None
    try:
        dt = datetime.strptime(m.group(1) + m.group(2), "%Y%m%d%H%M%S").replace(tzinfo=GIO_VN)
        return int(dt.timestamp())
    except ValueError:
        return None


def do_thoi_luong_ffprobe(file_path: Path, timeout: float = 10.0) -> float | None:
    """Hỏi ffprobe thời lượng thật của file MP4 (nếu có ffprobe)."""
    if shutil.which("ffprobe") is None:
        return None
    try:
        proc = subprocess.run(
            [
                "ffprobe",
                "-v",
                "error",
                "-show_entries",
                "format=duration",
                "-of",
                "default=noprint_wrappers=1:nokey=1",
                str(file_path),
            ],
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
        if proc.returncode == 0 and proc.stdout.strip():
            val = float(proc.stdout.strip())
            if val > 0:
                return val
    except Exception:
        pass
    return None


def thu_hoach_staging_cam(
    camera: str,
    staging_cam_dir: Path,
    hot_dir: Path,
    db: CameraDb,
    chot_tat_ca: bool = False,
    segment_seconds: int = 600,
) -> list[int]:
    """Chuyển các file segment đã đóng xong từ `staging/<cam>/` sang `hot/<cam>/<YYYY-MM-DD>/` và ghi SQLite.

    Khi `ffmpeg` đang chạy (`chot_tat_ca=False`), file mới nhất là file đang mở ghi dở ->
    giữ nguyên file mới nhất lại, chỉ thu hoạch tất cả các file đứng trước nó!
    """
    if not staging_cam_dir.exists():
        return []
    ds_file = sorted(p for p in staging_cam_dir.glob("*.mp4") if p.is_file())
    if not ds_file:
        return []

    ung_vien = ds_file if chot_tat_ca else ds_file[:-1]
    da_nhap: list[int] = []

    for idx, src in enumerate(ung_vien):
        try:
            sz = src.stat().st_size
        except OSError:
            continue
        if sz < 4096:
            # File rỗng do luồng ngắt ngay lúc mở -> bỏ
            src.unlink(missing_ok=True)
            continue

        start_ts = phan_tich_moc_tu_ten_file(src.name)
        if start_ts is None:
            start_ts = max(0, int(src.stat().st_mtime) - segment_seconds)

        # Ưu tiên ffprobe; nếu không có thì suy từ mốc bắt đầu của file kế tiếp hoặc mtime
        thoi_luong = do_thoi_luong_ffprobe(src)
        if thoi_luong is not None:
            end_ts = start_ts + max(1, int(round(thoi_luong)))
        else:
            next_start = None
            if idx + 1 < len(ds_file):
                next_start = phan_tich_moc_tu_ten_file(ds_file[idx + 1].name)
            if next_start and next_start > start_ts:
                end_ts = next_start
            else:
                end_ts = max(start_ts + 1, int(src.stat().st_mtime))

        ngay = ngay_vn_str(start_ts)
        gio_ten = datetime.fromtimestamp(start_ts, tz=GIO_VN).strftime("%H-%M-%S") + ".mp4"
        dich_dir = hot_dir / camera / ngay
        dich_dir.mkdir(parents=True, exist_ok=True)
        dich = dich_dir / gio_ten
        if dich.exists():
            dich = dich_dir / f"{gio_ten[:-4]}_{int(time.time()) % 1000}.mp4"

        try:
            shutil.move(str(src), str(dich))
        except OSError as e:
            log.warning("Không chuyển được segment %s -> %s: %s", src, dich, e)
            continue

        seg_id = db.them_segment(
            camera=camera,
            start_ts=start_ts,
            end_ts=end_ts,
            size_bytes=sz,
            local_path=str(dich),
            status="local",
        )
        da_nhap.append(seg_id)
        log.info(
            "[%s] Đã chốt đoạn #%d (%s, %.1f MB, %ds)",
            camera,
            seg_id,
            dich.name,
            sz / (1024 * 1024),
            end_ts - start_ts,
        )

    return da_nhap


class LuongGhiCamera:
    """Quản lý tiến trình `ffmpeg` ghi 24/24 cho đúng 1 camera."""

    def __init__(self, camera: str, cfg: CauHinhRecorder, db: CameraDb, hot_dir: Path, staging_dir: Path) -> None:
        self.camera = camera
        self.cfg = cfg
        self.db = db
        self.hot_dir = hot_dir
        self.staging_cam_dir = staging_dir / camera
        self.staging_cam_dir.mkdir(parents=True, exist_ok=True)

        self.backoff = BackoffTuya()
        self.dang_ghi = False
        self.last_segment_ts: int | None = None
        self.loi_gan_nhat: str | None = None
        self.mat_luong_tu_ts: float | None = None

        self._chay = False
        self._proc: subprocess.Popen | None = None
        self._luong: threading.Thread | None = None

    def bat_dau(self) -> None:
        if self._chay:
            return
        # Thu hoạch các đoạn còn sót lại từ phiên trước (ví dụ sau khi mất điện khởi động lại)
        thu_hoach_staging_cam(
            self.camera,
            self.staging_cam_dir,
            self.hot_dir,
            self.db,
            chot_tat_ca=True,
            segment_seconds=self.cfg.segment_seconds,
        )
        self._chay = True
        self._luong = threading.Thread(
            target=self._vong_ghi, name=f"rec-{self.camera}", daemon=True
        )
        self._luong.start()

    def dung(self) -> None:
        self._chay = False
        self._dung_ffmpeg()
        if self._luong:
            self._luong.join(timeout=8.0)
        thu_hoach_staging_cam(
            self.camera,
            self.staging_cam_dir,
            self.hot_dir,
            self.db,
            chot_tat_ca=True,
            segment_seconds=self.cfg.segment_seconds,
        )
        self.dang_ghi = False

    def _dung_ffmpeg(self) -> None:
        proc = self._proc
        if proc and proc.poll() is None:
            try:
                proc.terminate()
                proc.wait(timeout=5.0)
            except Exception:
                try:
                    proc.kill()
                except Exception:
                    pass

    def tao_lenh_ffmpeg(self) -> list[str]:
        mau_out = str(self.staging_cam_dir / "%Y%m%d_%H%M%S.mp4")
        cmd = [
            "ffmpeg",
            "-hide_banner",
            "-loglevel",
            "warning",
            "-rtsp_transport",
            "tcp",
            "-i",
            self.cfg.rtsp_url(self.camera),
            "-map",
            "0:v:0",
            "-c:v",
            "copy",
        ]
        if self.cfg.audio_codec == "none":
            cmd.append("-an")
        elif self.cfg.audio_codec == "copy":
            cmd.extend(["-map", "0:a?", "-c:a", "copy"])
        else:
            # Camera Tuya thường phát G.711 PCMA -> đổi nhẹ sang AAC 64k để xem có tiếng trên cả Drive & Android
            cmd.extend(["-map", "0:a?", "-c:a", "aac", "-b:a", "64k", "-ar", "16000"])

        cmd.extend(
            [
                "-f",
                "segment",
                "-segment_time",
                str(self.cfg.segment_seconds),
                "-reset_timestamps",
                "1",
                "-strftime",
                "1",
                "-segment_format_options",
                "movflags=+frag_keyframe+empty_moov+default_base_moof",
                mau_out,
            ]
        )
        return cmd

    def _vong_ghi(self) -> None:
        while self._chay:
            if not self.cfg.recording_enabled:
                self.dang_ghi = False
                self._dung_ffmpeg()
                time.sleep(2.0)
                continue

            if shutil.which("ffmpeg") is None:
                self.dang_ghi = False
                self.loi_gan_nhat = "Thiếu lệnh ffmpeg trong môi trường chạy."
                time.sleep(15.0)
                continue

            cmd = self.tao_lenh_ffmpeg()
            bat_dau = time.monotonic()
            log.info("[%s] Bắt đầu kéo luồng ghi 24/24 từ go2rtc...", self.camera)
            try:
                self._proc = subprocess.Popen(
                    cmd,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.PIPE,
                    text=True,
                )
            except Exception as e:
                self.dang_ghi = False
                self.loi_gan_nhat = str(e)
                time.sleep(15.0)
                continue

            lan_thay_du_lieu = time.monotonic()
            tong_kich_thuoc_cu = -1
            while self._chay and self.cfg.recording_enabled and self._proc.poll() is None:
                ds_mp4 = list(self.staging_cam_dir.glob("*.mp4"))
                tong_hien_tai = sum(p.stat().st_size for p in ds_mp4 if p.exists())
                if tong_hien_tai != tong_kich_thuoc_cu and tong_hien_tai > 0:
                    tong_kich_thuoc_cu = tong_hien_tai
                    lan_thay_du_lieu = time.monotonic()

                co_file = any(p.exists() and p.stat().st_size > 4096 for p in ds_mp4)
                if co_file:
                    self.dang_ghi = True
                    self.loi_gan_nhat = None
                    self.mat_luong_tu_ts = None
                    self.backoff.dat_lai()

                # Canh treo: go2rtc còn mở cổng TCP nhưng camera Tuya ngừng đẩy khung hình quá 45s
                if time.monotonic() - lan_thay_du_lieu > 45.0:
                    log.warning("[%s] Không thấy dữ liệu mới sau 45s, ngắt ffmpeg để nối lại.", self.camera)
                    self._dung_ffmpeg()
                    break

                da_chot = thu_hoach_staging_cam(
                    self.camera,
                    self.staging_cam_dir,
                    self.hot_dir,
                    self.db,
                    chot_tat_ca=False,
                    segment_seconds=self.cfg.segment_seconds,
                )
                if da_chot:
                    self.last_segment_ts = int(time.time())
                time.sleep(3.0)

            if not self._chay or not self.cfg.recording_enabled:
                self._dung_ffmpeg()
                thu_hoach_staging_cam(
                    self.camera,
                    self.staging_cam_dir,
                    self.hot_dir,
                    self.db,
                    chot_tat_ca=True,
                    segment_seconds=self.cfg.segment_seconds,
                )
                self.dang_ghi = False
                continue

            # Tiến trình ffmpeg tự thoát do đứt luồng
            self.dang_ghi = False
            if self.mat_luong_tu_ts is None:
                self.mat_luong_tu_ts = time.monotonic()

            stderr_cuoi = ""
            if self._proc.stderr:
                try:
                    stderr_cuoi = self._proc.stderr.read()[-300:].strip()
                except Exception:
                    pass
            thu_hoach_staging_cam(
                self.camera,
                self.staging_cam_dir,
                self.hot_dir,
                self.db,
                chot_tat_ca=True,
                segment_seconds=self.cfg.segment_seconds,
            )

            chay_duoc = time.monotonic() - bat_dau
            cho_giay = self.backoff.ghi_nhan_ket_thuc(chay_duoc)
            self.loi_gan_nhat = stderr_cuoi.splitlines()[-1] if stderr_cuoi else "Luồng RTSP từ go2rtc bị ngắt"
            log.warning(
                "[%s] Luồng ghi dừng sau %.1fs (%s). Đợi %.0fs trước khi thử lại (tránh Tuya khóa).",
                self.camera,
                chay_duoc,
                self.loi_gan_nhat,
                cho_giay,
            )

            ket_thuc_cho = time.monotonic() + cho_giay
            while self._chay and self.cfg.recording_enabled and time.monotonic() < ket_thuc_cho:
                time.sleep(1.0)


def chup_anh_ngay(camera: str, cfg: CauHinhRecorder, thumbs_dir: Path) -> Path | None:
    """Chụp 1 khung hình JPEG tức thì cho nút 'Chụp ảnh' trên SmartHomeApp.

    Ưu tiên hỏi `/api/frame.jpeg?src=<cam>` của go2rtc; nếu lỗi thì fallback sang `ffmpeg -frames:v 1`.
    """
    now_ts = int(time.time())
    ngay = ngay_vn_str(now_ts)
    gio_str = datetime.fromtimestamp(now_ts, tz=GIO_VN).strftime("%H-%M-%S")
    dich_dir = thumbs_dir / camera / ngay
    dich_dir.mkdir(parents=True, exist_ok=True)
    dich = dich_dir / f"{gio_str}_snap.jpg"

    url = f"{cfg.go2rtc_http_base()}/api/frame.jpeg?src={urllib.parse.quote(camera, safe='')}"
    req = urllib.request.Request(url, method="GET")
    if cfg.go2rtc_user:
        tho = f"{cfg.go2rtc_user}:{cfg.go2rtc_pass}".encode("utf-8")
        req.add_header("Authorization", "Basic " + base64.b64encode(tho).decode("ascii"))
    try:
        with urllib.request.urlopen(req, timeout=10.0) as resp:
            du_lieu = resp.read()
            if resp.status == 200 and len(du_lieu) > 1024:
                dich.write_bytes(du_lieu)
                return dich
    except Exception as e:
        log.debug("go2rtc frame.jpeg không trả về (%s), chuyển sang ffmpeg: %s", camera, e)

    if shutil.which("ffmpeg") is None:
        return None
    try:
        proc = subprocess.run(
            [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-rtsp_transport",
                "tcp",
                "-i",
                cfg.rtsp_url(camera),
                "-frames:v",
                "1",
                "-q:v",
                "3",
                "-y",
                str(dich),
            ],
            capture_output=True,
            timeout=15.0,
            check=False,
        )
        if proc.returncode == 0 and dich.exists() and dich.stat().st_size > 1024:
            return dich
    except Exception as e:
        log.warning("Chụp ảnh bằng ffmpeg thất bại (%s): %s", camera, e)

    dich.unlink(missing_ok=True)
    return None
