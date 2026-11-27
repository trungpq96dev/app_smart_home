"""Nhận diện khuôn mặt & Chào bằng giọng nói siêu nhẹ cho CPU i5-3210M (Mục 2.2).

Kiến trúc tối ưu tài nguyên:
1. Lấy ảnh tĩnh JPEG từ go2rtc (`/api/frame.jpeg?src=<cam>`) mỗi 1-2 giây (luồng HD).
2. Motion Gate (OpenCV / diff ảnh xám thu nhỏ): chỉ chạy AI khi có chuyển động thực sự.
3. YuNet (phát hiện mặt) + SFace (trích vector 128D) qua OpenCV (`cv2.FaceDetectorYN` /
   `cv2.FaceRecognizerSF`), so khớp với thư mục ảnh mẫu `faces/<Ten_Nguoi>/`
   (hỗ trợ cả ảnh ban ngày lẫn ảnh hồng ngoại ban đêm, tự hot-reload khi thêm ảnh).
4. Chống spam & báo nhầm:
   - Cooldown 15-30 phút / người quen để không chào lặp lại.
   - Xác nhận đa khung hình cho người lạ (`unknown`): chỉ báo khi không khớp liên tiếp 2-3 giây.
   - Ưu tiên người quen: nếu cùng khung hình có người nhà và người lạ, hoặc vừa chào người nhà
     trong 5 phút gần nhất thì tạm bỏ qua cảnh báo người lạ.
   - Chế độ Nhà (ban ngày): publish MQTT `camera/face/greet` (`retain=False`) để Tablet
     phát *ting-tong* + đọc *"Có khách ở cửa"* + hiện popup ảnh chụp 10 giây.
   - Chế độ Vắng nhà hoặc Ngủ (23:00-06:00): im lặng trên tablet (`silent=True`), tự gửi
     ảnh chụp + thời gian về Telegram / ntfy.
   - Lưu ảnh người lạ vào `faces/unknown/` và tự dọn file quá 7 ngày.
"""

from __future__ import annotations

import base64
import logging
import math
import shutil
import threading
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any, Callable

from cau_hinh import GIO_VN, CauHinhRecorder, gio_vn_str

log = logging.getLogger("camera_recorder.face")

try:
    import numpy as np
except ImportError:  # pragma: no cover
    np = None  # type: ignore

try:
    import cv2
except ImportError:  # pragma: no cover
    cv2 = None  # type: ignore


YUNET_DEFAULT_URL = (
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_detection_yunet/face_detection_yunet_2023mar.onnx"
)
SFACE_DEFAULT_URL = (
    "https://github.com/opencv/opencv_zoo/raw/main/models/"
    "face_recognition_sface/face_recognition_sface_2021dec.onnx"
)

ANH_DUOI_HOP_LE = {".jpg", ".jpeg", ".png", ".webp", ".bmp"}


@dataclass
class KetQuaMat:
    """Một khuôn mặt đã nhận diện được trên khung hình."""

    name: str
    is_known: bool
    confidence: float
    bbox: tuple[int, int, int, int] = (0, 0, 0, 0)


@dataclass
class SuKienChaoMat:
    """Payload sự kiện chuẩn gửi lên MQTT `camera/face/greet` (`retain=False`)."""

    name: str
    is_known: bool
    greeting: str
    camera: str
    confidence: float
    snapshot_url: str
    ts: int
    mode: str = "home"
    silent: bool = False
    snapshot_path: Path | None = None

    def thanh_mqtt_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "is_known": self.is_known,
            "greeting": self.greeting,
            "camera": self.camera,
            "confidence": round(float(self.confidence), 3),
            "snapshot_url": self.snapshot_url,
            "ts": int(self.ts),
            "mode": self.mode,
            "silent": bool(self.silent),
        }


def la_gio_ngu(ts: float | None = None, gio_bat_dau: int = 23, gio_ket_thuc: int = 6) -> bool:
    """Kiểm tra giờ hiện tại (UTC+7) có nằm trong khung giờ ngủ (mặc định 23:00-06:00) không."""
    dt = datetime.fromtimestamp(ts, tz=GIO_VN) if ts is not None else datetime.now(GIO_VN)
    h = dt.hour
    s = gio_bat_dau % 24
    e = gio_ket_thuc % 24
    if s == e:
        return True
    if s > e:
        return h >= s or h < e
    return s <= h < e


def tao_cau_chao(name: str, is_known: bool, ts: float | None = None) -> str:
    """Sinh câu chào tiếng Việt tự nhiên theo buổi trong ngày."""
    if not is_known:
        return "Có khách ở cửa"
    dt = datetime.fromtimestamp(ts, tz=GIO_VN) if ts is not None else datetime.now(GIO_VN)
    h = dt.hour
    if 5 <= h < 11:
        buoi = "Chào buổi sáng"
    elif 11 <= h < 14:
        buoi = "Chào buổi trưa"
    elif 14 <= h < 18:
        buoi = "Chào buổi chiều"
    else:
        buoi = "Chào mừng về nhà"
    return f"{buoi}, {name}!"


def cosine_similarity(a: list[float] | Any, b: list[float] | Any) -> float:
    """Độ tương đồng cosine chuẩn hóa [-1..1] giữa 2 vector đặc trưng khuôn mặt SFace."""
    if np is not None:
        va = np.asarray(a, dtype=np.float32).ravel()
        vb = np.asarray(b, dtype=np.float32).ravel()
        na = float(np.linalg.norm(va))
        nb = float(np.linalg.norm(vb))
        if na <= 1e-8 or nb <= 1e-8:
            return 0.0
        return float(np.dot(va, vb) / (na * nb))

    dot = sum(float(x) * float(y) for x, y in zip(a, b))
    na = math.sqrt(sum(float(x) * float(x) for x in a))
    nb = math.sqrt(sum(float(y) * float(y) for y in b))
    if na <= 1e-8 or nb <= 1e-8:
        return 0.0
    return dot / (na * nb)


class BoQuyetDinhChaoMat:
    """Máy trạng thái chống chào lặp & chống báo nhầm người lạ theo đúng đặc tả 2.2.

    Tách riêng hoàn toàn khỏi OpenCV để unit test deterministic chạy 100% không cần camera/GPU.
    """

    def __init__(
        self,
        known_cooldown_sec: float = 1200.0,       # 20 phút (trong khoảng 15-30 phút)
        unknown_confirm_sec: float = 2.5,         # Liên tiếp 2-3 giây mới báo người lạ
        unknown_confirm_frames: int = 2,          # Tối thiểu 2 khung hình liên tiếp
        unknown_cooldown_sec: float = 60.0,       # Tránh bắn ting-tong dồn dập khi khách đứng chờ
        known_grace_sec: float = 300.0,           # Vừa thấy người quen -> tạm bỏ qua người lạ 5 phút
        sleep_start_hour: int = 23,
        sleep_end_hour: int = 6,
    ) -> None:
        self.known_cooldown_sec = max(10.0, float(known_cooldown_sec))
        self.unknown_confirm_sec = max(0.5, float(unknown_confirm_sec))
        self.unknown_confirm_frames = max(1, int(unknown_confirm_frames))
        self.unknown_cooldown_sec = max(5.0, float(unknown_cooldown_sec))
        self.known_grace_sec = max(0.0, float(known_grace_sec))
        self.sleep_start_hour = sleep_start_hour
        self.sleep_end_hour = sleep_end_hour

        # Trạng thái thời gian
        self._lan_chao_nguoi_quen: dict[str, float] = {}
        self._moc_thay_nguoi_quen_cuoi: dict[str, float] = {}
        self._chuoi_la_bat_dau: dict[str, float] = {}
        self._chuoi_la_so_khung: dict[str, int] = {}
        self._chuoi_la_lan_cuoi: dict[str, float] = {}
        self._lan_bao_nguoi_la: dict[str, float] = {}

    def che_do_hieu_luc(self, home_mode: str, now_ts: float) -> str:
        """Trả về chế độ hiệu lực: `away` | `sleep` | `home`."""
        m = home_mode.strip().lower()
        if m == "away":
            return "away"
        if m == "sleep" or la_gio_ngu(now_ts, self.sleep_start_hour, self.sleep_end_hour):
            return "sleep"
        return "home"

    def xu_ly_khung(
        self,
        camera: str,
        faces: list[KetQuaMat],
        now_ts: float,
        home_mode: str = "home",
    ) -> list[dict[str, Any]]:
        """Nhận danh sách khuôn mặt ở 1 khung hình, trả về danh sách quyết định phát sự kiện.

        Mỗi phần tử trả về có khóa:
        `name`, `is_known`, `confidence`, `greeting`, `mode`, `silent`, `send_alert`, `save_unknown`.
        """
        if not faces:
            # Quá 4 giây không thấy người lạ thì xóa chuỗi xác nhận đa khung hình
            cuoi = self._chuoi_la_lan_cuoi.get(camera)
            if cuoi is not None and (now_ts - cuoi) > 4.0:
                self._xoa_chuoi_la(camera)
            return []

        mode = self.che_do_hieu_luc(home_mode, now_ts)
        silent = mode in ("away", "sleep")

        known_faces = [f for f in faces if f.is_known]
        unknown_faces = [f for f in faces if not f.is_known]
        su_kien: list[dict[str, Any]] = []

        # 1. Ưu tiên người quen: nếu có người nhà trong khung hình -> ghi nhận mốc ưu tiên 5 phút
        #    và xóa chuỗi nghi ngờ người lạ để không báo oan khi đi cùng người thân/bạn bè.
        if known_faces:
            self._moc_thay_nguoi_quen_cuoi[camera] = now_ts
            self._xoa_chuoi_la(camera)

            # Chọn điểm cao nhất cho mỗi người quen xuất hiện trong khung
            tot_nhat: dict[str, KetQuaMat] = {}
            for f in known_faces:
                cu = tot_nhat.get(f.name)
                if cu is None or f.confidence > cu.confidence:
                    tot_nhat[f.name] = f

            for ten, f in tot_nhat.items():
                lan_truoc = self._lan_chao_nguoi_quen.get(ten)
                if lan_truoc is not None and (now_ts - lan_truoc) < self.known_cooldown_sec:
                    continue
                self._lan_chao_nguoi_quen[ten] = now_ts
                su_kien.append(
                    {
                        "name": ten,
                        "is_known": True,
                        "confidence": f.confidence,
                        "greeting": tao_cau_chao(ten, True, now_ts),
                        "mode": mode,
                        "silent": silent,
                        "send_alert": False,
                        "save_unknown": False,
                    }
                )
            return su_kien

        # 2. Chỉ có người lạ trong khung hình -> kiểm tra ngưỡng ưu tiên người quen 5 phút
        moc_quen = self._moc_thay_nguoi_quen_cuoi.get(camera)
        if moc_quen is not None and (now_ts - moc_quen) < self.known_grace_sec:
            self._xoa_chuoi_la(camera)
            return []

        # 3. Xác nhận đa khung hình liên tiếp 2-3 giây chống báo nhầm
        lan_cuoi = self._chuoi_la_lan_cuoi.get(camera)
        if lan_cuoi is None or (now_ts - lan_cuoi) > 4.0:
            self._chuoi_la_bat_dau[camera] = now_ts
            self._chuoi_la_so_khung[camera] = 1
        else:
            self._chuoi_la_so_khung[camera] = self._chuoi_la_so_khung.get(camera, 0) + 1
        self._chuoi_la_lan_cuoi[camera] = now_ts

        bat_dau = self._chuoi_la_bat_dau[camera]
        so_khung = self._chuoi_la_so_khung[camera]
        du_khung = so_khung >= self.unknown_confirm_frames
        du_thoi_gian = (now_ts - bat_dau) >= self.unknown_confirm_sec or (
            self.unknown_confirm_frames == 1 and self.unknown_confirm_sec <= 0.5
        )
        if not (du_khung and du_thoi_gian):
            return []

        # Kiểm tra cooldown giữa 2 lần báo người lạ trên cùng 1 camera
        lan_bao_cuoi = self._lan_bao_nguoi_la.get(camera)
        if lan_bao_cuoi is not None and (now_ts - lan_bao_cuoi) < self.unknown_cooldown_sec:
            return []

        self._lan_bao_nguoi_la[camera] = now_ts
        self._xoa_chuoi_la(camera)
        best = max(unknown_faces, key=lambda x: x.confidence)
        su_kien.append(
            {
                "name": "unknown",
                "is_known": False,
                "confidence": best.confidence,
                "greeting": tao_cau_chao("unknown", False, now_ts),
                "mode": mode,
                "silent": silent,
                "send_alert": silent,  # Vắng nhà / Ngủ -> im lặng tablet, gửi Telegram/ntfy
                "save_unknown": True,
            }
        )
        return su_kien

    def _xoa_chuoi_la(self, camera: str) -> None:
        self._chuoi_la_bat_dau.pop(camera, None)
        self._chuoi_la_so_khung.pop(camera, None)
        self._chuoi_la_lan_cuoi.pop(camera, None)


def don_anh_nguoi_la_qua_han(unknown_dir: Path, retention_days: int = 7, now_ts: float | None = None) -> int:
    """Tự xóa ảnh trong `faces/unknown/` cũ hơn `retention_days` (mặc định 7 ngày)."""
    if not unknown_dir.exists():
        return 0
    moc = (now_ts if now_ts is not None else time.time()) - max(1, retention_days) * 86400.0
    da_xoa = 0
    for f in unknown_dir.iterdir():
        if not f.is_file() or f.suffix.lower() not in ANH_DUOI_HOP_LE:
            continue
        try:
            if f.stat().st_mtime < moc:
                f.unlink(missing_ok=True)
                da_xoa += 1
        except OSError:
            pass
    return da_xoa


def chuyen_nguoi_la_thanh_nguoi_quen(
    faces_dir: Path,
    filename: str,
    ten_nguoi: str,
) -> tuple[bool, str]:
    """Chuyển 1 ảnh từ `faces/unknown/<filename>` sang `faces/<Ten_Nguoi>/` để học mặt mới."""
    ten_sach = Path(filename).name
    nguon = faces_dir / "unknown" / ten_sach
    if not nguon.exists() or not nguon.is_file():
        return False, f"Không tìm thấy ảnh người lạ: {ten_sach}"
    ten_thu_muc = "".join(ch for ch in ten_nguoi.strip() if ch not in '/\\:*?"<>|')
    if not ten_thu_muc or ten_thu_muc.lower() == "unknown":
        return False, "Tên người không hợp lệ."
    dich_dir = faces_dir / ten_thu_muc
    dich_dir.mkdir(parents=True, exist_ok=True)
    dich = dich_dir / ten_sach
    shutil.move(str(nguon), str(dich))
    return True, f"Đã chuyển ảnh sang thư mục '{ten_thu_muc}'."


def gui_ntfy_canh_bao(
    ntfy_url: str,
    title: str,
    message: str,
    anh_path: Path | None = None,
    timeout: float = 12.0,
) -> bool:
    """Gửi thông báo dự phòng tới máy chủ ntfy (nếu người dùng khai báo `NTFY_URL`)."""
    url = ntfy_url.strip()
    if not url:
        return False
    try:
        headers: dict[str, str] = {"Title": title.encode("ascii", errors="ignore").decode("ascii") or "SmartHome"}
        if anh_path and anh_path.exists():
            headers["Filename"] = anh_path.name
            headers["Message"] = message.encode("ascii", errors="ignore").decode("ascii")
            body = anh_path.read_bytes()
        else:
            body = message.encode("utf-8")
        req = urllib.request.Request(url, data=body, headers=headers, method="POST")
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return 200 <= resp.status < 300
    except Exception as e:
        log.warning("Gửi ntfy thất bại (%s): %s", url, e)
        return False


class BoNhanDienKhuonMatYuNetSFace:
    """Bọc YuNet + SFace (OpenCV) chạy CPU rất nhẹ (~15-30ms/khung hình trên i5-3210M)."""

    def __init__(
        self,
        faces_dir: Path,
        models_dir: Path,
        yunet_url: str = YUNET_DEFAULT_URL,
        sface_url: str = SFACE_DEFAULT_URL,
        score_thresh: float = 0.72,
        match_thresh: float = 0.363,
    ) -> None:
        self.faces_dir = faces_dir
        self.models_dir = models_dir
        self.yunet_path = models_dir / "face_detection_yunet_2023mar.onnx"
        self.sface_path = models_dir / "face_recognition_sface_2021dec.onnx"
        self.yunet_url = yunet_url
        self.sface_url = sface_url
        self.score_thresh = score_thresh
        self.match_thresh = match_thresh

        self._lock = threading.Lock()
        self._detector = None
        self._recognizer = None
        self._gallery: dict[str, list[Any]] = {}
        self._gallery_sig: str = ""

    def dam_bao_san_sang(self) -> bool:
        if cv2 is None or np is None:
            log.warning("Chưa cài opencv-python-headless/numpy — tạm tắt nhận diện khuôn mặt.")
            return False
        if not hasattr(cv2, "FaceDetectorYN") or not hasattr(cv2, "FaceRecognizerSF"):
            log.warning("Phiên bản OpenCV thiếu FaceDetectorYN/FaceRecognizerSF (cần opencv >= 4.7).")
            return False

        with self._lock:
            if self._detector is not None and self._recognizer is not None:
                self._nap_thu_vien_neu_doi()
                return True
            if not self.yunet_path.exists():
                if not self._tai_file(self.yunet_url, self.yunet_path, "YuNet"):
                    return False
            if not self.sface_path.exists():
                if not self._tai_file(self.sface_url, self.sface_path, "SFace"):
                    return False
            try:
                self._detector = cv2.FaceDetectorYN.create(
                    str(self.yunet_path),
                    "",
                    (320, 320),
                    self.score_thresh,
                    0.3,
                    5000,
                )
                self._recognizer = cv2.FaceRecognizerSF.create(str(self.sface_path), "")
                log.info("Đã nạp bộ nhận diện khuôn mặt YuNet + SFace.")
                self._nap_thu_vien_neu_doi(force=True)
                return True
            except Exception as e:
                log.error("Không nạp được YuNet/SFace: %s", e)
                return False

    @staticmethod
    def _tai_file(url: str, dich: Path, ten: str) -> bool:
        dich.parent.mkdir(parents=True, exist_ok=True)
        tam = dich.with_suffix(".part")
        log.info("Đang tải mô hình %s lần đầu từ %s ...", ten, url)
        try:
            urllib.request.urlretrieve(url, str(tam))
            if tam.exists() and tam.stat().st_size > 100_000:
                tam.replace(dich)
                return True
        except Exception as e:
            log.warning("Không tải được mô hình %s: %s", ten, e)
        tam.unlink(missing_ok=True)
        return False

    def _tinh_chu_ky_gallery(self) -> str:
        if not self.faces_dir.exists():
            return ""
        muc: list[str] = []
        for sub in sorted(self.faces_dir.iterdir()):
            if not sub.is_dir() or sub.name.lower() == "unknown":
                continue
            for img_p in sorted(sub.iterdir()):
                if img_p.is_file() and img_p.suffix.lower() in ANH_DUOI_HOP_LE:
                    try:
                        st = img_p.stat()
                        muc.append(f"{sub.name}/{img_p.name}:{int(st.st_mtime)}:{st.st_size}")
                    except OSError:
                        pass
        return "|".join(muc)

    def _nap_thu_vien_neu_doi(self, force: bool = False) -> None:
        self.faces_dir.mkdir(parents=True, exist_ok=True)
        (self.faces_dir / "unknown").mkdir(parents=True, exist_ok=True)
        sig = self._tinh_chu_ky_gallery()
        if not force and sig == self._gallery_sig:
            return
        self._gallery_sig = sig
        moi: dict[str, list[Any]] = {}
        if self._detector is None or self._recognizer is None or cv2 is None:
            self._gallery = moi
            return

        for sub in sorted(self.faces_dir.iterdir()):
            if not sub.is_dir() or sub.name.lower() == "unknown":
                continue
            vecs: list[Any] = []
            for img_p in sorted(sub.iterdir()):
                if not img_p.is_file() or img_p.suffix.lower() not in ANH_DUOI_HOP_LE:
                    continue
                img = cv2.imread(str(img_p))
                if img is None:
                    continue
                h, w = img.shape[:2]
                self._detector.setInputSize((w, h))
                _, faces = self._detector.detect(img)
                if faces is None or len(faces) == 0:
                    continue
                # Lấy khuôn mặt rõ nhất trên ảnh mẫu
                mat = max(faces, key=lambda r: float(r[14]))
                aligned = self._recognizer.alignCrop(img, mat)
                feat = self._recognizer.feature(aligned)
                vecs.append(feat.copy())
            if vecs:
                moi[sub.name] = vecs
        self._gallery = moi
        log.info(
            "Đã nạp thư viện khuôn mặt: %d người (%s)",
            len(moi),
            ", ".join(f"{k}:{len(v)}" for k, v in moi.items()) or "trống",
        )

    def lam_moi_thu_vien(self) -> None:
        with self._lock:
            self._gallery_sig = ""
            if self._detector is not None:
                self._nap_thu_vien_neu_doi(force=True)

    def nhan_dien_tu_jpeg(self, jpeg_bytes: bytes) -> list[KetQuaMat]:
        if not self.dam_bao_san_sang() or cv2 is None or np is None:
            return []
        arr = np.frombuffer(jpeg_bytes, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
        if img is None:
            return []
        h, w = img.shape[:2]
        # Giới hạn cạnh dài tối đa 1280px để CPU i5-3210M xử lý cực nhanh mà vẫn đủ nét
        scale = 1.0
        if max(w, h) > 1280:
            scale = 1280.0 / max(w, h)
            img = cv2.resize(img, (int(w * scale), int(h * scale)), interpolation=cv2.INTER_AREA)
            h, w = img.shape[:2]

        with self._lock:
            self._nap_thu_vien_neu_doi()
            assert self._detector is not None and self._recognizer is not None
            self._detector.setInputSize((w, h))
            _, faces = self._detector.detect(img)
            if faces is None or len(faces) == 0:
                return []

            ket_qua: list[KetQuaMat] = []
            for mat in faces:
                det_score = float(mat[14])
                if det_score < self.score_thresh:
                    continue
                aligned = self._recognizer.alignCrop(img, mat)
                feat = self._recognizer.feature(aligned)

                best_name = "unknown"
                best_sim = 0.0
                for ten, mau_list in self._gallery.items():
                    for mau in mau_list:
                        sim = float(self._recognizer.match(feat, mau, cv2.FaceRecognizerSF_FR_COSINE))
                        if sim > best_sim:
                            best_sim = sim
                            best_name = ten

                la_quen = best_sim >= self.match_thresh and best_name != "unknown"
                x, y, bw, bh = int(mat[0]), int(mat[1]), int(mat[2]), int(mat[3])
                ket_qua.append(
                    KetQuaMat(
                        name=best_name if la_quen else "unknown",
                        is_known=la_quen,
                        confidence=best_sim if la_quen else det_score,
                        bbox=(x, y, bw, bh),
                    )
                )
            return ket_qua


class BoLocChuyenDongJpeg:
    """Motion Gate siêu nhẹ trên ảnh JPEG chụp từ go2rtc `/api/frame.jpeg`."""

    def __init__(self, diff_thresh: int = 18, area_pct: float = 0.6) -> None:
        self.diff_thresh = diff_thresh
        self.area_pct = area_pct
        self._nen: dict[str, Any] = {}

    def co_chuyen_dong(self, camera: str, jpeg_bytes: bytes) -> bool:
        if cv2 is None or np is None:
            return True
        arr = np.frombuffer(jpeg_bytes, dtype=np.uint8)
        img = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
        if img is None:
            return False
        nho = cv2.resize(img, (160, 90), interpolation=cv2.INTER_AREA).astype(np.float32)
        nen = self._nen.get(camera)
        if nen is None or nen.shape != nho.shape:
            self._nen[camera] = nho
            return False
        lech = np.abs(nho - nen)
        self._nen[camera] = 0.85 * nen + 0.15 * nho
        pct = float(np.mean(lech > self.diff_thresh) * 100.0)
        return pct >= self.area_pct


class LuongNhanDienKhuonMat:
    """Vòng lặp nền lấy ảnh từ go2rtc mỗi 1-2s -> Motion Gate -> YuNet+SFace -> MQTT/Telegram."""

    def __init__(
        self,
        cfg: CauHinhRecorder,
        lay_danh_sach_cam_cb: Callable[[], list[str]],
        lay_home_mode_cb: Callable[[], str],
        la_rieng_tu_cb: Callable[[str], bool],
        on_su_kien_mat: Callable[[SuKienChaoMat, bool], None],
        engine: BoNhanDienKhuonMatYuNetSFace | None = None,
    ) -> None:
        self.cfg = cfg
        self.lay_danh_sach_cam_cb = lay_danh_sach_cam_cb
        self.lay_home_mode_cb = lay_home_mode_cb
        self.la_rieng_tu_cb = la_rieng_tu_cb
        self.on_su_kien_mat = on_su_kien_mat

        self.faces_dir = cfg.face_dir
        self.unknown_dir = self.faces_dir / "unknown"
        self.snapshots_dir = self.faces_dir / "_snapshots"
        self.unknown_dir.mkdir(parents=True, exist_ok=True)
        self.snapshots_dir.mkdir(parents=True, exist_ok=True)

        self.engine = engine or BoNhanDienKhuonMatYuNetSFace(
            faces_dir=self.faces_dir,
            models_dir=cfg.data_dir / "models",
            match_thresh=cfg.face_match_thresh,
        )
        self.motion = BoLocChuyenDongJpeg(
            diff_thresh=cfg.motion_diff_thresh,
            area_pct=cfg.motion_area_pct,
        )
        self.quyet_dinh = BoQuyetDinhChaoMat(
            known_cooldown_sec=cfg.face_greet_cooldown_sec,
            unknown_confirm_sec=cfg.face_unknown_confirm_sec,
            unknown_confirm_frames=cfg.face_unknown_confirm_frames,
            known_grace_sec=cfg.face_known_grace_sec,
            sleep_start_hour=cfg.face_sleep_start_hour,
            sleep_end_hour=cfg.face_sleep_end_hour,
        )
        self._chay = False
        self._luong: threading.Thread | None = None
        self._lan_don_unknown = 0.0

    def bat_dau(self) -> None:
        if self._chay:
            return
        self._chay = True
        self._luong = threading.Thread(target=self._vong_chay, name="face-recognizer", daemon=True)
        self._luong.start()

    def dung(self) -> None:
        self._chay = False
        if self._luong:
            self._luong.join(timeout=4.0)

    def _lay_anh_go2rtc(self, camera: str) -> bytes | None:
        url = f"{self.cfg.go2rtc_http_base()}/api/frame.jpeg?src={urllib.parse.quote(camera, safe='')}"
        req = urllib.request.Request(url, method="GET")
        if self.cfg.go2rtc_user:
            tho = f"{self.cfg.go2rtc_user}:{self.cfg.go2rtc_pass}".encode("utf-8")
            req.add_header("Authorization", "Basic " + base64.b64encode(tho).decode("ascii"))
        try:
            with urllib.request.urlopen(req, timeout=4.0) as resp:
                if resp.status == 200:
                    data = resp.read()
                    if len(data) > 1024:
                        return data
        except Exception as e:
            log.debug("Chưa lấy được frame.jpeg cho face (%s): %s", camera, e)
        return None

    def _luu_anh_su_kien(self, camera: str, is_known: bool, jpeg_bytes: bytes, ts: int) -> tuple[Path, str]:
        stamp = datetime.fromtimestamp(ts, tz=GIO_VN).strftime("%Y%m%d_%H%M%S")
        ten_file = f"{stamp}_{camera}_{'known' if is_known else 'unknown'}.jpg"
        dich = (self.snapshots_dir if is_known else self.unknown_dir) / ten_file
        dich.parent.mkdir(parents=True, exist_ok=True)
        dich.write_bytes(jpeg_bytes)
        return dich, f"/api/face/snapshot/{urllib.parse.quote(ten_file, safe='')}"

    def _vong_chay(self) -> None:
        while self._chay:
            if not self.cfg.face_recognition_enabled:
                time.sleep(2.0)
                continue

            now_mono = time.monotonic()
            # Dọn ảnh người lạ quá 7 ngày & ảnh snapshot tạm quá 2 ngày mỗi 6 giờ
            if now_mono - self._lan_don_unknown >= 6 * 3600.0:
                don_anh_nguoi_la_qua_han(self.unknown_dir, self.cfg.face_unknown_retention_days)
                don_anh_nguoi_la_qua_han(self.snapshots_dir, 2)
                self._lan_don_unknown = now_mono

            cams = self.cfg.face_cameras or self.lay_danh_sach_cam_cb()
            # Ưu tiên luồng chính (bỏ qua các luồng phụ `_sd` nếu đã có luồng chính cùng tên)
            cam_set = set(cams)
            hd_cams = [c for c in cams if not (c.endswith("_sd") and c[:-3] in cam_set)]

            home_mode = self.lay_home_mode_cb()
            for cam in hd_cams:
                if not self._chay or not self.cfg.face_recognition_enabled:
                    break
                if self.la_rieng_tu_cb(cam):
                    continue
                jpeg = self._lay_anh_go2rtc(cam)
                if not jpeg:
                    continue
                if not self.motion.co_chuyen_dong(cam, jpeg):
                    continue

                faces = self.engine.nhan_dien_tu_jpeg(jpeg)
                now_ts = time.time()
                cac_quyet_dinh = self.quyet_dinh.xu_ly_khung(cam, faces, now_ts, home_mode=home_mode)
                for qd in cac_quyet_dinh:
                    ts_int = int(now_ts)
                    anh_path, snap_url = self._luu_anh_su_kien(cam, qd["is_known"], jpeg, ts_int)
                    ev = SuKienChaoMat(
                        name=qd["name"],
                        is_known=qd["is_known"],
                        greeting=qd["greeting"],
                        camera=cam,
                        confidence=qd["confidence"],
                        snapshot_url=snap_url,
                        ts=ts_int,
                        mode=qd["mode"],
                        silent=qd["silent"],
                        snapshot_path=anh_path,
                    )
                    self.on_su_kien_mat(ev, bool(qd["send_alert"]))

            time.sleep(self.cfg.face_poll_sec)
