"""Nhận diện người 2 tầng nhẹ trên CPU (Hướng A):
1) Tầng 1 (`BoLocChuyenDong`): So sai khác khung hình nhỏ 160x90 -> phòng tĩnh nghỉ AI.
2) Tầng 2 (`BoNhanDienNguoiOnnx`): YOLOv8n/11n qua `onnxruntime` CPU, chỉ bắt class 0 (`person`).
3) Gom cụm sự kiện (`BoGomSuKien`): Xác nhận 2 khung liên tiếp mới mở sự kiện, gom liên tục
   khi khoảng trống < `event_gap_sec`, chọn khung hình rõ nhất vẽ khung cam/đỏ lưu thumbnail.
"""

from __future__ import annotations

import logging
import math
import shutil
import subprocess
import threading
import time
import urllib.request
from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Callable

from cau_hinh import GIO_VN, CauHinhRecorder, ngay_vn_str
from db import CameraDb
from recorder import BackoffTuya

log = logging.getLogger("camera_recorder.detector")

try:
    import numpy as np
except ImportError:  # pragma: no cover - máy dev chưa cài numpy vẫn chạy được unit test thuần
    np = None  # type: ignore

try:
    from PIL import Image, ImageDraw
except ImportError:  # pragma: no cover
    Image = None  # type: ignore
    ImageDraw = None  # type: ignore

try:
    import onnxruntime as ort
except ImportError:  # pragma: no cover
    ort = None  # type: ignore


@dataclass
class HopNguoi:
    """Khung chữ nhật khoanh 1 người phát hiện được (tọa độ pixel trên khung gốc)."""

    x1: float
    y1: float
    x2: float
    y2: float
    conf: float

    def dien_tich(self) -> float:
        return max(0.0, self.x2 - self.x1) * max(0.0, self.y2 - self.y1)

    def tam(self) -> tuple[float, float]:
        return ((self.x1 + self.x2) * 0.5, (self.y1 + self.y2) * 0.5)


def iou_hop(a: HopNguoi, b: HopNguoi) -> float:
    xx1 = max(a.x1, b.x1)
    yy1 = max(a.y1, b.y1)
    xx2 = min(a.x2, b.x2)
    yy2 = min(a.y2, b.y2)
    giao = max(0.0, xx2 - xx1) * max(0.0, yy2 - yy1)
    if giao <= 0.0:
        return 0.0
    hop = a.dien_tich() + b.dien_tich() - giao
    return giao / hop if hop > 0.0 else 0.0


def loc_nms(danh_sach: list[HopNguoi], iou_thresh: float = 0.45) -> list[HopNguoi]:
    """Loại bỏ các khung trùng lặp trên cùng một người (Non-Maximum Suppression)."""
    sap_xep = sorted(danh_sach, key=lambda h: h.conf, reverse=True)
    giu_lai: list[HopNguoi] = []
    for ung_vien in sap_xep:
        if all(iou_hop(ung_vien, da_giu) < iou_thresh for da_giu in giu_lai):
            giu_lai.append(ung_vien)
    return giu_lai


def loc_theo_roi(
    danh_sach: list[HopNguoi],
    width: int,
    height: int,
    roi: tuple[float, float, float, float],
) -> list[HopNguoi]:
    """Chỉ giữ người có điểm đứng (giữa cạnh đáy hộp) hoặc tâm nằm trong vùng ROI."""
    rx1, ry1, rx2, ry2 = roi
    if rx1 <= 0.0 and ry1 <= 0.0 and rx2 >= 1.0 and ry2 >= 1.0:
        return danh_sach
    px1, py1, px2, py2 = rx1 * width, ry1 * height, rx2 * width, ry2 * height
    ket_qua: list[HopNguoi] = []
    for h in danh_sach:
        cx, cy = h.tam()
        chan_y = h.y1 + 0.85 * (h.y2 - h.y1)
        if (px1 <= cx <= px2 and py1 <= cy <= py2) or (px1 <= cx <= px2 and py1 <= chan_y <= py2):
            ket_qua.append(h)
    return ket_qua


class BoLocChuyenDong:
    """Tầng 1: phát hiện chuyển động siêu nhẹ trên ảnh xám thu nhỏ."""

    def __init__(
        self,
        diff_thresh: int = 20,
        area_pct: float = 0.55,
        roi: tuple[float, float, float, float] = (0.0, 0.0, 1.0, 1.0),
        alpha: float = 0.15,
    ) -> None:
        self.diff_thresh = diff_thresh
        self.area_pct = area_pct
        self.roi = roi
        self.alpha = alpha
        self._nen: list[float] | object | None = None

    def co_chuyen_dong(self, rgb_bytes: bytes, width: int, height: int) -> bool:
        """Nhận khung RGB24, lấy mẫu lưới thưa ~160x90 để tính % điểm ảnh đổi màu."""
        buoc_x = max(1, width // 160)
        buoc_y = max(1, height // 90)
        rx1, ry1, rx2, ry2 = self.roi
        x_min, x_max = int(rx1 * width), int(rx2 * width)
        y_min, y_max = int(ry1 * height), int(ry2 * height)

        if np is not None:
            arr = np.frombuffer(rgb_bytes, dtype=np.uint8).reshape((height, width, 3))
            vung = arr[y_min:y_max:buoc_y, x_min:x_max:buoc_x]
            if vung.size == 0:
                return False
            xam = (
                0.299 * vung[:, :, 0].astype(np.float32)
                + 0.587 * vung[:, :, 1].astype(np.float32)
                + 0.114 * vung[:, :, 2].astype(np.float32)
            )
            if self._nen is None or getattr(self._nen, "shape", None) != xam.shape:
                self._nen = xam
                return False
            lech = np.abs(xam - self._nen)
            self._nen = (1.0 - self.alpha) * self._nen + self.alpha * xam
            pct = float(np.mean(lech > self.diff_thresh) * 100.0)
            return pct >= self.area_pct

        # Nhánh thuần Python (cho môi trường test không cài numpy)
        mau: list[float] = []
        stride = width * 3
        for y in range(y_min, y_max, buoc_y):
            row_off = y * stride
            for x in range(x_min, x_max, buoc_x):
                p = row_off + x * 3
                r, g, b = rgb_bytes[p], rgb_bytes[p + 1], rgb_bytes[p + 2]
                mau.append(0.299 * r + 0.587 * g + 0.114 * b)
        if not mau:
            return False
        if not isinstance(self._nen, list) or len(self._nen) != len(mau):
            self._nen = mau
            return False
        dem = 0
        nen_cu = self._nen
        for i, v in enumerate(mau):
            if abs(v - nen_cu[i]) > self.diff_thresh:
                dem += 1
            nen_cu[i] = (1.0 - self.alpha) * nen_cu[i] + self.alpha * v
        return (dem * 100.0 / len(mau)) >= self.area_pct


class BoGomSuKien:
    """Máy trạng thái xác nhận liên tiếp & gom cụm sự kiện phát hiện người giống Imou/EZVIZ."""

    def __init__(
        self,
        camera: str,
        db: CameraDb,
        thumbs_dir: Path,
        confirm_frames: int = 2,
        event_gap_sec: float = 15.0,
        on_su_kien_moi: Callable[[int, int, int, float, Path | None], None] | None = None,
        luu_anh_fn: Callable[[bytes, int, int, list[HopNguoi], Path, int], bool] | None = None,
    ) -> None:
        self.camera = camera
        self.db = db
        self.thumbs_dir = thumbs_dir
        self.confirm_frames = max(1, confirm_frames)
        self.event_gap_sec = event_gap_sec
        self.on_su_kien_moi = on_su_kien_moi
        self.luu_anh_fn = luu_anh_fn or ve_va_luu_thumbnail

        self._chuoi_lien_tiep = 0
        self._lan_thay_ung_vien_ts: float = 0.0

        # Phiên sự kiện đang mở
        self.active_event_id: int | None = None
        self.active_start_ts: int = 0
        self.last_person_ts: float = 0.0
        self.best_score: float = 0.0
        self.max_conf: float = 0.0
        self.max_count: int = 0
        self.thumb_path: Path | None = None
        self._lan_cap_nhat_db_ts: float = 0.0

    def xu_ly_khung(
        self,
        now_ts: float,
        ds_nguoi: list[HopNguoi],
        rgb_bytes: bytes | None = None,
        width: int = 0,
        height: int = 0,
    ) -> int | None:
        """Cập nhật trạng thái với kết quả nhận diện ở thời điểm `now_ts`.

        Trả về `event_id` đang hoạt động (hoặc `None` nếu không có sự kiện).
        """
        if ds_nguoi:
            if self._lan_thay_ung_vien_ts > 0 and (now_ts - self._lan_thay_ung_vien_ts) <= 4.0:
                self._chuoi_lien_tiep += 1
            else:
                self._chuoi_lien_tiep = 1
            self._lan_thay_ung_vien_ts = now_ts

            top_conf = max(h.conf for h in ds_nguoi)
            score = sum(h.conf * math.sqrt(max(1.0, h.dien_tich())) for h in ds_nguoi)

            if self.active_event_id is None:
                if self._chuoi_lien_tiep < self.confirm_frames:
                    return None
                # Đủ khung xác nhận -> mở sự kiện mới ngay để báo Telegram/MQTT tức thì!
                start_int = int(now_ts)
                self.active_start_ts = start_int
                self.last_person_ts = now_ts
                self.best_score = score
                self.max_conf = top_conf
                self.max_count = len(ds_nguoi)
                self.thumb_path = self._tao_duong_dan_thumb(start_int)
                co_anh = False
                if rgb_bytes and width > 0 and height > 0 and self.thumb_path:
                    co_anh = self.luu_anh_fn(rgb_bytes, width, height, ds_nguoi, self.thumb_path, start_int)

                self.active_event_id = self.db.them_event(
                    camera=self.camera,
                    kind="person",
                    start_ts=start_int,
                    end_ts=start_int + 2,
                    max_conf=top_conf,
                    person_count=len(ds_nguoi),
                    thumb_local=str(self.thumb_path) if co_anh else None,
                )
                self._lan_cap_nhat_db_ts = now_ts
                if self.on_su_kien_moi:
                    self.on_su_kien_moi(
                        self.active_event_id,
                        start_int,
                        len(ds_nguoi),
                        top_conf,
                        self.thumb_path if co_anh else None,
                    )
                return self.active_event_id

            # Đang trong phiên sự kiện -> kéo dài thời lượng & cập nhật ảnh đẹp hơn nếu rõ hơn
            self.last_person_ts = now_ts
            self.max_conf = max(self.max_conf, top_conf)
            self.max_count = max(self.max_count, len(ds_nguoi))
            if score > self.best_score * 1.08 and rgb_bytes and width > 0 and height > 0 and self.thumb_path:
                if self.luu_anh_fn(rgb_bytes, width, height, ds_nguoi, self.thumb_path, int(now_ts)):
                    self.best_score = score

            if (now_ts - self._lan_cap_nhat_db_ts) >= 8.0:
                self.db.cap_nhat_event(
                    self.active_event_id,
                    end_ts=int(now_ts),
                    max_conf=self.max_conf,
                    person_count=self.max_count,
                    thumb_local=str(self.thumb_path) if (self.thumb_path and self.thumb_path.exists()) else None,
                )
                self._lan_cap_nhat_db_ts = now_ts
            return self.active_event_id

        # Không thấy người ở khung này
        if (now_ts - self._lan_thay_ung_vien_ts) > 3.0:
            self._chuoi_lien_tiep = 0

        if self.active_event_id is not None and (now_ts - self.last_person_ts) >= self.event_gap_sec:
            self.dong_phien_hien_tai()
        return self.active_event_id

    def dong_phien_hien_tai(self) -> None:
        if self.active_event_id is None:
            return
        end_int = max(self.active_start_ts + 2, int(round(self.last_person_ts)))
        self.db.cap_nhat_event(
            self.active_event_id,
            end_ts=end_int,
            max_conf=self.max_conf,
            person_count=self.max_count,
            thumb_local=str(self.thumb_path) if (self.thumb_path and self.thumb_path.exists()) else None,
        )
        self.active_event_id = None
        self._chuoi_lien_tiep = 0
        self.best_score = 0.0

    def _tao_duong_dan_thumb(self, ts: int) -> Path:
        ngay = ngay_vn_str(ts)
        gio_str = datetime.fromtimestamp(ts, tz=GIO_VN).strftime("%H-%M-%S")
        thu_muc = self.thumbs_dir / self.camera / ngay
        thu_muc.mkdir(parents=True, exist_ok=True)
        return thu_muc / f"{gio_str}_person.jpg"


def ve_va_luu_thumbnail(
    rgb_bytes: bytes,
    width: int,
    height: int,
    ds_nguoi: list[HopNguoi],
    dich: Path,
    ts: int,
) -> bool:
    """Vẽ khung chữ nhật khoanh người + độ tin cậy lên ảnh và lưu JPEG (~30 KB)."""
    if Image is None or ImageDraw is None:
        return False
    try:
        img = Image.frombytes("RGB", (width, height), rgb_bytes)
        draw = ImageDraw.Draw(img)
        for h in ds_nguoi:
            x1 = max(0, min(width - 1, int(round(h.x1))))
            y1 = max(0, min(height - 1, int(round(h.y1))))
            x2 = max(0, min(width - 1, int(round(h.x2))))
            y2 = max(0, min(height - 1, int(round(h.y2))))
            draw.rectangle([x1, y1, x2, y2], outline=(249, 115, 22), width=3)
            nhan = f"Nguoi {int(round(h.conf * 100))}%"
            ty = max(0, y1 - 16)
            draw.rectangle([x1, ty, min(width - 1, x1 + 82), ty + 16], fill=(249, 115, 22))
            draw.text((x1 + 4, ty + 2), nhan, fill=(255, 255, 255))

        gio_goc = datetime.fromtimestamp(ts, tz=GIO_VN).strftime("%H:%M:%S")
        draw.rectangle([6, 6, 74, 22], fill=(11, 18, 32))
        draw.text((10, 8), gio_goc, fill=(241, 245, 249))
        dich.parent.mkdir(parents=True, exist_ok=True)
        img.save(str(dich), format="JPEG", quality=82, optimize=True)
        return True
    except Exception as e:
        log.warning("Lỗi vẽ thumbnail %s: %s", dich, e)
        return False


class BoNhanDienNguoiOnnx:
    """Tầng 2: chạy mô hình YOLOv8n/YOLO11n ONNX trên CPU để tìm nhãn `person` (class 0)."""

    INPUT_SIZE = 640

    def __init__(self, cfg: CauHinhRecorder) -> None:
        self.cfg = cfg
        self._session = None
        self._input_name = ""
        self._lock = threading.Lock()

    def dam_bao_san_sang(self) -> bool:
        if ort is None or np is None:
            log.warning("Chưa cài onnxruntime/numpy — tắt nhận diện AI.")
            return False
        with self._lock:
            if self._session is not None:
                return True
            if not self.cfg.onnx_model_path.exists():
                if not self._tai_model():
                    return False
            try:
                opts = ort.SessionOptions()
                opts.intra_op_num_threads = self.cfg.onnx_threads
                opts.inter_op_num_threads = 1
                self._session = ort.InferenceSession(
                    str(self.cfg.onnx_model_path),
                    sess_options=opts,
                    providers=["CPUExecutionProvider"],
                )
                self._input_name = self._session.get_inputs()[0].name
                log.info("Đã nạp mô hình nhận diện người ONNX: %s", self.cfg.onnx_model_path)
                return True
            except Exception as e:
                log.error("Không nạp được mô hình ONNX %s: %s", self.cfg.onnx_model_path, e)
                return False

    def _tai_model(self) -> bool:
        dich = self.cfg.onnx_model_path
        dich.parent.mkdir(parents=True, exist_ok=True)
        tam = dich.with_suffix(".part")
        log.info("Đang tải mô hình nhẹ yolov8n.onnx lần đầu từ %s ...", self.cfg.onnx_model_url)
        try:
            urllib.request.urlretrieve(self.cfg.onnx_model_url, str(tam))
            if tam.exists() and tam.stat().st_size > 1_000_000:
                tam.replace(dich)
                return True
        except Exception as e:
            log.warning("Không tải được mô hình ONNX tự động: %s", e)
        tam.unlink(missing_ok=True)
        return False

    def nhan_dien(self, rgb_bytes: bytes, width: int, height: int) -> list[HopNguoi]:
        if not self.dam_bao_san_sang():
            return []
        assert np is not None and self._session is not None

        sz = self.INPUT_SIZE
        img = np.frombuffer(rgb_bytes, dtype=np.uint8).reshape((height, width, 3))
        scale = min(sz / width, sz / height)
        nw, nh = int(round(width * scale)), int(round(height * scale))
        pad_x, pad_y = (sz - nw) // 2, (sz - nh) // 2

        if Image is not None:
            resized = np.asarray(Image.fromarray(img).resize((nw, nh)), dtype=np.uint8)
        else:
            ys = (np.linspace(0, height - 1, nh)).astype(np.int32)
            xs = (np.linspace(0, width - 1, nw)).astype(np.int32)
            resized = img[ys][:, xs]

        canvas = np.full((sz, sz, 3), 114, dtype=np.uint8)
        canvas[pad_y : pad_y + nh, pad_x : pad_x + nw] = resized
        blob = canvas.transpose((2, 0, 1)).astype(np.float32) / 255.0
        blob = np.expand_dims(blob, axis=0)

        with self._lock:
            out = self._session.run(None, {self._input_name: blob})[0]

        ket_qua = giai_ma_yolo_person(
            out,
            conf_thresh=self.cfg.person_conf_thresh,
            scale=scale,
            pad_x=pad_x,
            pad_y=pad_y,
            orig_w=width,
            orig_h=height,
        )
        ket_qua = loc_theo_roi(ket_qua, width, height, self.cfg.roi_box)
        return loc_nms(ket_qua, iou_thresh=0.45)


def giai_ma_yolo_person(
    pred,
    conf_thresh: float,
    scale: float,
    pad_x: int,
    pad_y: int,
    orig_w: int,
    orig_h: int,
) -> list[HopNguoi]:
    """Giải mã tensor đầu ra của YOLOv8/YOLO11 (hỗ trợ cả dạng [1,84,8400] lẫn [1,N,6])."""
    if np is None:
        return []
    arr = np.squeeze(pred)
    ket_qua: list[HopNguoi] = []

    # Dạng end2end NMS sẵn: (N, 6) = [x1, y1, x2, y2, conf, cls]
    if arr.ndim == 2 and arr.shape[1] == 6:
        chon = arr[(arr[:, 5] == 0) & (arr[:, 4] >= conf_thresh)]
        for r in chon:
            x1 = max(0.0, min(float(orig_w), (float(r[0]) - pad_x) / scale))
            y1 = max(0.0, min(float(orig_h), (float(r[1]) - pad_y) / scale))
            x2 = max(0.0, min(float(orig_w), (float(r[2]) - pad_x) / scale))
            y2 = max(0.0, min(float(orig_h), (float(r[3]) - pad_y) / scale))
            if x2 - x1 >= 8 and y2 - y1 >= 12:
                ket_qua.append(HopNguoi(x1, y1, x2, y2, float(r[4])))
        return ket_qua

    # Dạng chuẩn YOLOv8/v11: (84, 8400) -> chuyển vị thành (8400, 84), cột 4 là điểm class 0 (person)
    if arr.ndim == 2 and arr.shape[0] < arr.shape[1]:
        arr = arr.T
    if arr.ndim != 2 or arr.shape[1] < 5:
        return []

    diem_nguoi = arr[:, 4]
    chi_so = np.where(diem_nguoi >= conf_thresh)[0]
    for idx in chi_so:
        cx, cy, w, h = float(arr[idx, 0]), float(arr[idx, 1]), float(arr[idx, 2]), float(arr[idx, 3])
        conf = float(diem_nguoi[idx])
        x1 = max(0.0, min(float(orig_w), (cx - w * 0.5 - pad_x) / scale))
        y1 = max(0.0, min(float(orig_h), (cy - h * 0.5 - pad_y) / scale))
        x2 = max(0.0, min(float(orig_w), (cx + w * 0.5 - pad_x) / scale))
        y2 = max(0.0, min(float(orig_h), (cy + h * 0.5 - pad_y) / scale))
        if x2 - x1 >= 8 and y2 - y1 >= 12:
            ket_qua.append(HopNguoi(x1, y1, x2, y2, conf))
    return ket_qua


class LuongNhanDienCamera:
    """Luồng đọc 2 khung hình/giây từ `go2rtc` và chạy đường ống Motion -> YOLO -> Gom sự kiện."""

    def __init__(
        self,
        camera: str,
        cfg: CauHinhRecorder,
        db: CameraDb,
        thumbs_dir: Path,
        yolo: BoNhanDienNguoiOnnx,
        on_su_kien_moi: Callable[[str, int, int, int, float, Path | None], None] | None = None,
    ) -> None:
        self.camera = camera
        self.cfg = cfg
        self.motion = BoLocChuyenDong(
            diff_thresh=cfg.motion_diff_thresh,
            area_pct=cfg.motion_area_pct,
            roi=cfg.roi_box,
        )
        self.yolo = yolo
        self.gom = BoGomSuKien(
            camera=camera,
            db=db,
            thumbs_dir=thumbs_dir,
            confirm_frames=cfg.confirm_frames,
            event_gap_sec=cfg.event_gap_sec,
            on_su_kien_moi=lambda eid, ts, cnt, conf, p: (
                on_su_kien_moi(camera, eid, ts, cnt, conf, p) if on_su_kien_moi else None
            ),
        )
        self.backoff = BackoffTuya()
        self._chay = False
        self._proc: subprocess.Popen | None = None
        self._luong: threading.Thread | None = None

    def bat_dau(self) -> None:
        if self._chay:
            return
        self._chay = True
        self._luong = threading.Thread(
            target=self._vong_nhan_dien, name=f"ai-{self.camera}", daemon=True
        )
        self._luong.start()

    def dung(self) -> None:
        self._chay = False
        self._dung_proc()
        if self._luong:
            self._luong.join(timeout=5.0)
        self.gom.dong_phien_hien_tai()

    def _dung_proc(self) -> None:
        p = self._proc
        if p and p.poll() is None:
            try:
                p.terminate()
                p.wait(timeout=3.0)
            except Exception:
                try:
                    p.kill()
                except Exception:
                    pass

    def _vong_nhan_dien(self) -> None:
        w, h = self.cfg.detect_width, self.cfg.detect_height
        kich_thuoc_khung = w * h * 3

        while self._chay:
            if not self.cfg.detect_person_enabled:
                self._dung_proc()
                self.gom.dong_phien_hien_tai()
                time.sleep(2.0)
                continue

            if shutil.which("ffmpeg") is None or not self.yolo.dam_bao_san_sang():
                time.sleep(15.0)
                continue

            cmd = [
                "ffmpeg",
                "-hide_banner",
                "-loglevel",
                "error",
                "-rtsp_transport",
                "tcp",
                "-i",
                self.cfg.rtsp_url(self.camera),
                "-vf",
                f"fps={self.cfg.detect_fps},scale={w}:{h}",
                "-pix_fmt",
                "rgb24",
                "-f",
                "rawvideo",
                "pipe:1",
            ]
            bat_dau = time.monotonic()
            try:
                self._proc = subprocess.Popen(
                    cmd, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL
                )
            except Exception as e:
                log.warning("[%s] Không mở được ống đọc khung AI: %s", self.camera, e)
                time.sleep(20.0)
                continue

            assert self._proc.stdout is not None
            while self._chay and self.cfg.detect_person_enabled:
                buf = self._doc_du_bytes(self._proc.stdout, kich_thuoc_khung)
                if len(buf) != kich_thuoc_khung:
                    break
                self.backoff.dat_lai()
                now_ts = time.time()
                if self.motion.co_chuyen_dong(buf, w, h):
                    ds_nguoi = self.yolo.nhan_dien(buf, w, h)
                else:
                    ds_nguoi = []
                self.gom.xu_ly_khung(now_ts, ds_nguoi, buf, w, h)

            self._dung_proc()
            self.gom.dong_phien_hien_tai()
            if not self._chay or not self.cfg.detect_person_enabled:
                continue
            cho = self.backoff.ghi_nhan_ket_thuc(time.monotonic() - bat_dau)
            ket_thuc_cho = time.monotonic() + cho
            while self._chay and self.cfg.detect_person_enabled and time.monotonic() < ket_thuc_cho:
                time.sleep(1.0)

    @staticmethod
    def _doc_du_bytes(stream, so_byte: int) -> bytes:
        manh: list[bytes] = []
        con_thieu = so_byte
        while con_thieu > 0:
            phan = stream.read(con_thieu)
            if not phan:
                break
            manh.append(phan)
            con_thieu -= len(phan)
        return b"".join(manh)
