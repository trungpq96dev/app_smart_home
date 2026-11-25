"""Quản lý lưu trữ 2 tầng:
1) Hot Cache cục bộ (10 GB mặc định trên ổ serverubuntu) -> phát lại tức thì.
2) Kho lạnh Google Drive 5TB cá nhân qua `rclone` -> lưu trữ 24/24 dài hạn,
   tự động tải đệm về `cold_cache` khi người dùng tua xem lại ngày cũ trên tablet.
"""

from __future__ import annotations

import logging
import shutil
import subprocess
import threading
import time
from pathlib import Path
from typing import Callable

from cau_hinh import CauHinhRecorder, ngay_vn_str
from db import CameraDb

log = logging.getLogger("camera_recorder.storage")

BYTE_MOI_GB = 1024 ** 3


class QuanLyLuuTru:
    def __init__(
        self,
        cfg: CauHinhRecorder,
        db: CameraDb,
        rclone_runner: Callable[[list[str], float], tuple[int, str]] | None = None,
    ) -> None:
        self.cfg = cfg
        self.db = db
        self._rclone_runner = rclone_runner or self._chay_rclone_that

        self.hot_dir = cfg.data_dir / "hot"
        self.cold_dir = cfg.data_dir / "cold_cache"
        self.thumbs_dir = cfg.data_dir / "thumbs"
        self.staging_dir = cfg.data_dir / "staging"
        for d in (self.hot_dir, self.cold_dir, self.thumbs_dir, self.staging_dir):
            d.mkdir(parents=True, exist_ok=True)

        # Khóa theo từng segment_id để nhiều request HTTP Range cùng lúc không gọi rclone trùng
        self._khoa_tai_ve_map_lock = threading.Lock()
        self._khoa_tai_ve: dict[int, threading.Lock] = {}
        self.loi_drive_gan_nhat: str | None = None
        self.lan_upload_cuoi_ts: int | None = None

    # ------------------------------------------------------------ rclone CLI

    def rclone_san_sang(self) -> bool:
        if not self.cfg.drive_enabled:
            return False
        if not self.cfg.rclone_config.exists():
            return False
        return shutil.which("rclone") is not None or self._rclone_runner is not self._chay_rclone_that

    def _chay_rclone_that(self, args: list[str], timeout: float = 600.0) -> tuple[int, str]:
        cmd = ["rclone", "--config", str(self.cfg.rclone_config), *args]
        try:
            proc = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                check=False,
            )
            out = (proc.stderr or proc.stdout or "").strip()
            return proc.returncode, out
        except FileNotFoundError:
            return 127, "Chưa cài rclone trong môi trường chạy"
        except subprocess.TimeoutExpired:
            return 124, f"rclone quá hạn chờ {int(timeout)}s"
        except Exception as e:
            return 1, str(e)

    def duong_dan_drive_segment(self, camera: str, start_ts: int, ten_file: str) -> str:
        ngay = ngay_vn_str(start_ts)
        return f"{self.cfg.rclone_remote}:{self.cfg.drive_root}/{camera}/24x7/{ngay}/{ten_file}"

    def duong_dan_drive_event(self, camera: str, start_ts: int, ten_file: str) -> str:
        ngay = ngay_vn_str(start_ts)
        return f"{self.cfg.rclone_remote}:{self.cfg.drive_root}/{camera}/Events/{ngay}/{ten_file}"

    # ----------------------------------------------------- upload len Drive

    def day_hang_doi_len_drive(self) -> int:
        """Đẩy các đoạn video & ảnh sự kiện mới lên Google Drive 5TB. Trả về số file đã đẩy."""
        if not self.cfg.drive_enabled:
            return 0
        if not self.cfg.rclone_config.exists():
            self.loi_drive_gan_nhat = (
                f"Chưa thấy file {self.cfg.rclone_config}. Xem README để cấp quyền Google Drive."
            )
            return 0

        da_day = 0
        for seg in self.db.danh_sach_cho_upload(gioi_han=10):
            local_p = Path(seg["local_path"])
            if not local_p.exists():
                self.db.danh_dau_xoa_local(int(seg["id"]))
                continue

            remote_dest = self.duong_dan_drive_segment(
                seg["camera"], int(seg["start_ts"]), local_p.name
            )
            args = ["copyto", str(local_p), remote_dest]
            if self.cfg.drive_bwlimit:
                args.extend(["--bwlimit", self.cfg.drive_bwlimit])

            rc, msg = self._rclone_runner(args, 900.0)
            if rc == 0:
                self.db.danh_dau_da_upload(int(seg["id"]), remote_dest)
                self.loi_drive_gan_nhat = None
                self.lan_upload_cuoi_ts = int(time.time())
                da_day += 1
                log.info("Đã đẩy lên Drive: %s -> %s", local_p.name, remote_dest)
            else:
                self.loi_drive_gan_nhat = msg[:240] if msg else f"rclone trả mã {rc}"
                log.warning("Chưa đẩy được %s lên Drive: %s", local_p.name, self.loi_drive_gan_nhat)
                break

        # Đồng bộ ảnh thumbnail sự kiện lên thư mục Events/ trên Drive
        for ev in self.db.danh_sach_event_cho_upload_thumb(gioi_han=20):
            thumb_p = Path(ev["thumb_local"])
            if not thumb_p.exists():
                continue
            remote_ev = self.duong_dan_drive_event(
                ev["camera"], int(ev["start_ts"]), thumb_p.name
            )
            rc, _ = self._rclone_runner(["copyto", str(thumb_p), remote_ev], 120.0)
            if rc == 0:
                self.db.danh_dau_event_drive(int(ev["id"]), remote_ev)

        return da_day

    # ------------------------------------------------- don Hot Cache 10 GB

    def don_hot_cache(self) -> int:
        """Giữ tổng dung lượng video ở Hot Cache <= LOCAL_CACHE_MAX_GB (mặc định 10 GB).

        Ưu tiên tuyệt đối: CHỈ xóa các đoạn đã đẩy thành công lên Google Drive (`uploaded`).
        Chỉ khi mất mạng kéo dài làm ổ vượt ngưỡng khẩn cấp `LOCAL_EMERGENCY_MAX_GB`
        thì mới buộc xóa đoạn cũ nhất để bảo vệ hệ điều hành serverubuntu.
        """
        gioi_han_bytes = int(self.cfg.local_cache_max_gb * BYTE_MOI_GB)
        khan_cap_bytes = int(self.cfg.local_emergency_max_gb * BYTE_MOI_GB)
        da_xoa = 0

        tat_ca = self.db.danh_sach_co_local(chi_da_upload=False)
        tong_bytes = 0
        ton_tai: list[tuple[dict, Path, int]] = []
        for seg in tat_ca:
            p = Path(seg["local_path"])
            if not p.exists():
                self.db.danh_dau_xoa_local(int(seg["id"]))
                continue
            sz = p.stat().st_size
            tong_bytes += sz
            ton_tai.append((seg, p, sz))

        # Bước 1: Xóa các file cũ ĐÃ lên Drive nếu vượt 10 GB
        for seg, p, sz in ton_tai:
            if tong_bytes <= gioi_han_bytes:
                break
            if seg["status"] != "uploaded":
                continue
            try:
                p.unlink(missing_ok=True)
                self._xoa_thu_muc_rong(p.parent, self.hot_dir)
                self.db.danh_dau_xoa_local(int(seg["id"]))
                tong_bytes -= sz
                da_xoa += 1
                log.info("Dọn Hot Cache (đã có trên Drive): %s", p.name)
            except OSError as e:
                log.warning("Không xóa được file cache %s: %s", p, e)

        # Bước 2: Van an toàn khẩn cấp khi mất mạng quá lâu hoặc tắt Drive
        if tong_bytes > khan_cap_bytes:
            for seg, p, sz in ton_tai:
                if tong_bytes <= gioi_han_bytes:
                    break
                if not p.exists():
                    continue
                try:
                    p.unlink(missing_ok=True)
                    self._xoa_thu_muc_rong(p.parent, self.hot_dir)
                    self.db.danh_dau_xoa_local(int(seg["id"]))
                    tong_bytes -= sz
                    da_xoa += 1
                    log.warning(
                        "Dọn khẩn cấp vượt %.1f GB (chưa kịp lên Drive): %s",
                        self.cfg.local_emergency_max_gb,
                        p.name,
                    )
                except OSError:
                    pass

        return da_xoa

    def don_cold_cache(self) -> None:
        """Giữ thư mục đệm xem lại ngày cũ <= COLD_CACHE_MAX_GB (mặc định 1 GB, LRU theo mtime)."""
        gioi_han = int(self.cfg.cold_cache_max_gb * BYTE_MOI_GB)
        files = [p for p in self.cold_dir.glob("*.mp4") if p.is_file()]
        files.sort(key=lambda p: p.stat().st_mtime)
        tong = sum(p.stat().st_size for p in files)
        for p in files:
            if tong <= gioi_han:
                break
            sz = p.stat().st_size
            try:
                p.unlink(missing_ok=True)
                tong -= sz
            except OSError:
                pass

    # --------------------------------------- lay file phat cho Tablet (Hot/Cold)

    def _lay_khoa_segment(self, seg_id: int) -> threading.Lock:
        with self._khoa_tai_ve_map_lock:
            khoa = self._khoa_tai_ve.get(seg_id)
            if khoa is None:
                khoa = threading.Lock()
                self._khoa_tai_ve[seg_id] = khoa
            return khoa

    def lay_file_phat(self, seg_id: int) -> Path | None:
        """Trả về đường dẫn file MP4 trên đĩa để API cắt HTTP Range phát cho tablet.

        - Còn trong Hot Cache 10 GB -> trả về ngay (< 1 ms).
        - Đã chuyển lên Google Drive 5TB -> kiểm tra `cold_cache`; chưa có thì dùng
          `rclone copyto` kéo về `cold_cache` rồi trả về.
        """
        seg = self.db.lay_segment(seg_id)
        if not seg:
            return None

        if seg.get("local_path"):
            p = Path(seg["local_path"])
            if p.exists():
                return p

        drive_path = seg.get("drive_path")
        if not drive_path:
            return None

        cold_file = self.cold_dir / f"seg_{seg_id}.mp4"
        if cold_file.exists() and cold_file.stat().st_size > 0:
            cold_file.touch()
            return cold_file

        khoa = self._lay_khoa_segment(seg_id)
        with khoa:
            if cold_file.exists() and cold_file.stat().st_size > 0:
                return cold_file
            tam = self.cold_dir / f"seg_{seg_id}.part"
            tam.unlink(missing_ok=True)
            log.info("Đang kéo đoạn #%s từ Google Drive về bộ đệm xem lại...", seg_id)
            rc, msg = self._rclone_runner(["copyto", drive_path, str(tam)], 300.0)
            if rc != 0 or not tam.exists():
                log.warning("Không kéo được đoạn #%s từ Drive: %s", seg_id, msg)
                tam.unlink(missing_ok=True)
                return None
            tam.replace(cold_file)
            self.don_cold_cache()
            return cold_file

    def lay_file_thumbnail(self, event_id: int) -> Path | None:
        ev = self.db.lay_event(event_id)
        if not ev:
            return None
        if ev.get("thumb_local"):
            p = Path(ev["thumb_local"])
            if p.exists():
                return p
        if ev.get("thumb_drive"):
            dich = self.thumbs_dir / "restored" / f"ev_{event_id}.jpg"
            if dich.exists():
                return dich
            dich.parent.mkdir(parents=True, exist_ok=True)
            rc, _ = self._rclone_runner(["copyto", ev["thumb_drive"], str(dich)], 60.0)
            if rc == 0 and dich.exists():
                return dich
        return None

    # --------------------------------------------- don Drive & thumb qua han

    def don_du_lieu_qua_han(self, now_ts: float | None = None) -> int:
        """Xóa các bản ghi cũ hơn `DRIVE_RETENTION_DAYS` trên cả Drive và SQLite."""
        now = int(now_ts if now_ts is not None else time.time())
        moc_drive = now - self.cfg.drive_retention_days * 86400
        da_xoa = 0

        for seg in self.db.danh_sach_qua_han(moc_drive):
            if seg.get("local_path"):
                Path(seg["local_path"]).unlink(missing_ok=True)
            if seg.get("drive_path") and self.rclone_san_sang():
                self._rclone_runner(["deletefile", seg["drive_path"]], 120.0)
            self.db.xoa_segment_han(int(seg["id"]))
            da_xoa += 1

        moc_thumb = now - max(self.cfg.thumb_retention_days, self.cfg.drive_retention_days) * 86400
        for ev in self.db.xoa_event_qua_han(moc_thumb):
            if ev.get("thumb_local"):
                Path(ev["thumb_local"]).unlink(missing_ok=True)
            if ev.get("thumb_drive") and self.rclone_san_sang():
                self._rclone_runner(["deletefile", ev["thumb_drive"]], 60.0)

        if da_xoa > 0 and self.rclone_san_sang():
            self._rclone_runner(
                ["rmdirs", f"{self.cfg.rclone_remote}:{self.cfg.drive_root}", "--leave-root"],
                120.0,
            )
        return da_xoa

    @staticmethod
    def _xoa_thu_muc_rong(thu_muc: Path, goc_dung: Path) -> None:
        cur = thu_muc
        while cur != goc_dung and cur.exists():
            try:
                cur.rmdir()
                cur = cur.parent
            except OSError:
                break
