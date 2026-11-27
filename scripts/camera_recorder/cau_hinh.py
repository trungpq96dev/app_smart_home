"""Cấu hình chung cho dịch vụ camera_recorder đọc từ biến môi trường (.env)."""

from __future__ import annotations

import base64
import json
import logging
import os
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
from pathlib import Path

GIO_VN = timezone(timedelta(hours=7))

log = logging.getLogger("camera_recorder.config")


def gio_vn_str(ts: float | None = None) -> str:
    dt = datetime.fromtimestamp(ts, tz=GIO_VN) if ts is not None else datetime.now(GIO_VN)
    return dt.strftime("%Y-%m-%d %H:%M:%S")


def ngay_vn_str(ts: float | None = None) -> str:
    dt = datetime.fromtimestamp(ts, tz=GIO_VN) if ts is not None else datetime.now(GIO_VN)
    return dt.strftime("%Y-%m-%d")


def khoang_ngay_vn(ngay_str: str) -> tuple[int, int]:
    """Đổi chuỗi YYYY-MM-DD (giờ Việt Nam +7) sang cặp Unix timestamp [đầu_ngày, cuối_ngày)."""
    dt = datetime.strptime(ngay_str, "%Y-%m-%d").replace(tzinfo=GIO_VN)
    dau = int(dt.timestamp())
    return dau, dau + 86400


def _str(ten: str, mac_dinh: str = "") -> str:
    v = os.environ.get(ten, "").strip()
    return v if v else mac_dinh


def _float(ten: str, mac_dinh: float) -> float:
    v = _str(ten)
    if not v:
        return mac_dinh
    try:
        return float(v)
    except ValueError:
        return mac_dinh


def _int(ten: str, mac_dinh: int) -> int:
    v = _str(ten)
    if not v:
        return mac_dinh
    try:
        return int(float(v))
    except ValueError:
        return mac_dinh


def _bool(ten: str, mac_dinh: bool = True) -> bool:
    v = _str(ten).lower()
    if not v:
        return mac_dinh
    return v not in ("0", "false", "no", "off", "khong")


def phan_tich_roi(chuoi: str) -> tuple[float, float, float, float]:
    """Đọc vùng canh ROI chuẩn hóa x1,y1,x2,y2 trong đoạn [0..1]."""
    if not chuoi.strip():
        return (0.0, 0.0, 1.0, 1.0)
    try:
        phan = [float(x.strip()) for x in chuoi.split(",")]
        if len(phan) != 4:
            return (0.0, 0.0, 1.0, 1.0)
        x1 = max(0.0, min(1.0, phan[0]))
        y1 = max(0.0, min(1.0, phan[1]))
        x2 = max(0.0, min(1.0, phan[2]))
        y2 = max(0.0, min(1.0, phan[3]))
        if x2 <= x1 or y2 <= y1:
            return (0.0, 0.0, 1.0, 1.0)
        return (x1, y1, x2, y2)
    except ValueError:
        return (0.0, 0.0, 1.0, 1.0)


@dataclass
class CauHinhRecorder:
    # Thư mục dữ liệu cục bộ (mount volume Docker)
    data_dir: Path = Path("/data")

    # go2rtc (nguồn kéo camera Tuya nội bộ)
    go2rtc_host: str = "127.0.0.1"
    go2rtc_api_port: int = 1984
    go2rtc_rtsp_port: int = 8554
    go2rtc_tls: bool = False
    go2rtc_user: str = ""
    go2rtc_pass: str = ""
    cameras: list[str] = field(default_factory=list)

    # Ghi 24/24
    recording_enabled: bool = True
    segment_seconds: int = 600          # 10 phút / file
    audio_codec: str = "aac"            # "aac" | "copy" | "none"

    # Bộ nhớ đệm cục bộ (Hot Cache 10 GB theo xác nhận của người dùng)
    local_cache_max_gb: float = 10.0
    local_emergency_max_gb: float = 13.5
    cold_cache_max_gb: float = 1.0
    thumb_retention_days: int = 90

    # Google Drive 5TB (qua rclone)
    drive_enabled: bool = True
    rclone_config: Path = Path("/config/rclone.conf")
    rclone_remote: str = "gdrive"
    drive_root: str = "SmartHomeCamera"
    drive_bwlimit: str = "4M"           # Giới hạn băng thông đẩy lên để không nghẽn Wi-Fi nhà
    drive_retention_days: int = 90

    # AI phát hiện người (Hướng A: Motion Gate + YOLO ONNX CPU)
    detect_person_enabled: bool = True
    detect_fps: float = 2.0
    detect_width: int = 640
    detect_height: int = 360
    motion_diff_thresh: int = 20
    motion_area_pct: float = 0.55
    person_conf_thresh: float = 0.50
    confirm_frames: int = 2
    event_gap_sec: float = 15.0
    roi_box: tuple[float, float, float, float] = (0.0, 0.0, 1.0, 1.0)
    onnx_model_path: Path = Path("/data/models/yolov8n.onnx")
    onnx_model_url: str = (
        "https://huggingface.co/Kalray/yolov8/resolve/main/yolov8n.onnx"
    )
    onnx_threads: int = 2

    # Telegram cảnh báo khi phát hiện người
    tg_enabled: bool = True
    tg_bot_token: str = ""
    tg_chat_id: str = ""
    tg_cooldown_sec: float = 45.0
    offline_alert_min: float = 10.0

    # HTTP Playback API cho Tablet SmartHomeApp
    api_host: str = "0.0.0.0"
    api_port: int = 1985
    api_user: str = ""
    api_pass: str = ""

    # MQTT Mosquitto
    mqtt_broker: str = "127.0.0.1"
    mqtt_port: int = 1883
    mqtt_user: str = "iot"
    mqtt_pass: str = ""
    mqtt_topic_goc: str = "camera"

    @classmethod
    def tu_moi_truong(cls) -> "CauHinhRecorder":
        data_dir = Path(_str("DATA_DIR", "/data"))
        cams_raw = _str("CAMERAS", "")
        cams = [c.strip() for c in cams_raw.split(",") if c.strip()]
        go2rtc_user = _str("GO2RTC_USER", "")
        go2rtc_pass = _str("GO2RTC_PASS", "")
        # Mặc định dùng lại tài khoản go2rtc cho cổng Playback API để trên tablet chỉ điền 1 lần
        api_user = _str("API_USER", go2rtc_user)
        api_pass = _str("API_PASS", go2rtc_pass)
        tg_token = _str("TG_BOT_TOKEN", "")
        tg_chat = _str("TG_CHAT_ID", "")
        tg_on = _bool("TG_ENABLED", True) and bool(tg_token and tg_chat)

        return cls(
            data_dir=data_dir,
            go2rtc_host=_str("GO2RTC_HOST", "127.0.0.1"),
            go2rtc_api_port=_int("GO2RTC_API_PORT", 1984),
            go2rtc_rtsp_port=_int("GO2RTC_RTSP_PORT", 8554),
            go2rtc_tls=_bool("GO2RTC_TLS", False),
            go2rtc_user=go2rtc_user,
            go2rtc_pass=go2rtc_pass,
            cameras=cams,
            recording_enabled=_bool("RECORDING_ENABLED", True),
            segment_seconds=max(60, _int("SEGMENT_SECONDS", 600)),
            audio_codec=_str("AUDIO_CODEC", "aac").lower(),
            local_cache_max_gb=max(1.0, _float("LOCAL_CACHE_MAX_GB", 10.0)),
            local_emergency_max_gb=max(2.0, _float("LOCAL_EMERGENCY_MAX_GB", 13.5)),
            cold_cache_max_gb=max(0.3, _float("COLD_CACHE_MAX_GB", 1.0)),
            thumb_retention_days=max(7, _int("THUMB_RETENTION_DAYS", 90)),
            drive_enabled=_bool("DRIVE_ENABLED", True),
            rclone_config=Path(_str("RCLONE_CONFIG", "/config/rclone.conf")),
            rclone_remote=_str("RCLONE_REMOTE", "gdrive").rstrip(":"),
            drive_root=_str("DRIVE_ROOT", "SmartHomeCamera").strip("/"),
            drive_bwlimit=_str("DRIVE_BWLIMIT", "4M"),
            drive_retention_days=max(7, _int("DRIVE_RETENTION_DAYS", 90)),
            detect_person_enabled=_bool("DETECT_PERSON_ENABLED", True),
            detect_fps=max(0.5, min(5.0, _float("DETECT_FPS", 2.0))),
            detect_width=_int("DETECT_WIDTH", 640),
            detect_height=_int("DETECT_HEIGHT", 360),
            motion_diff_thresh=_int("MOTION_DIFF_THRESH", 20),
            motion_area_pct=_float("MOTION_AREA_PCT", 0.55),
            person_conf_thresh=_float("PERSON_CONF_THRESH", 0.50),
            confirm_frames=max(1, _int("CONFIRM_FRAMES", 2)),
            event_gap_sec=max(3.0, _float("EVENT_GAP_SEC", 15.0)),
            roi_box=phan_tich_roi(_str("ROI_BOX", "")),
            onnx_model_path=Path(_str("ONNX_MODEL_PATH", str(data_dir / "models" / "yolov8n.onnx"))),
            onnx_model_url=_str(
                "ONNX_MODEL_URL",
                "https://huggingface.co/Kalray/yolov8/resolve/main/yolov8n.onnx",
            ),
            onnx_threads=max(1, _int("ONNX_THREADS", 2)),
            tg_enabled=tg_on,
            tg_bot_token=tg_token,
            tg_chat_id=tg_chat,
            tg_cooldown_sec=max(5.0, _float("TG_COOLDOWN_SEC", 45.0)),
            offline_alert_min=max(2.0, _float("OFFLINE_ALERT_MIN", 10.0)),
            api_host=_str("API_HOST", "0.0.0.0"),
            api_port=_int("API_PORT", 1985),
            api_user=api_user,
            api_pass=api_pass,
            mqtt_broker=_str("MQTT_BROKER", "127.0.0.1"),
            mqtt_port=_int("MQTT_PORT", 1883),
            mqtt_user=_str("MQTT_USER", "iot"),
            mqtt_pass=_str("MQTT_PASS", ""),
            mqtt_topic_goc=_str("MQTT_TOPIC_GOC", "camera").rstrip("/"),
        )

    def go2rtc_http_base(self) -> str:
        proto = "https" if self.go2rtc_tls else "http"
        return f"{proto}://{self.go2rtc_host}:{self.go2rtc_api_port}"

    def rtsp_url(self, ten_cam: str) -> str:
        """Tạo URL RTSP nội bộ tới go2rtc."""
        from urllib.parse import quote

        auth = ""
        if self.go2rtc_user:
            auth = f"{quote(self.go2rtc_user, safe='')}:{quote(self.go2rtc_pass, safe='')}@"
        return f"rtsp://{auth}{self.go2rtc_host}:{self.go2rtc_rtsp_port}/{quote(ten_cam, safe='')}"

    def do_danh_sach_cam_go2rtc(self, timeout: float = 6.0) -> list[str]:
        """Hỏi /api/streams của go2rtc nếu người dùng không khai cứng CAMERAS trong .env.

        Giống Go2rtc.kt trên Android: CHỈ đọc khóa (tên luồng), tuyệt đối không
        đụng tới hay in ra giá trị bên trong vì có mật khẩu Tuya.
        """
        if self.cameras:
            return list(self.cameras)

        url = f"{self.go2rtc_http_base()}/api/streams"
        req = urllib.request.Request(url, method="GET")
        if self.go2rtc_user:
            tho = f"{self.go2rtc_user}:{self.go2rtc_pass}".encode("utf-8")
            req.add_header("Authorization", "Basic " + base64.b64encode(tho).decode("ascii"))
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                if resp.status != 200:
                    log.warning("go2rtc trả mã %s khi hỏi danh sách luồng", resp.status)
                    return []
                data = json.loads(resp.read().decode("utf-8"))
                if isinstance(data, dict):
                    return sorted(str(k) for k in data.keys())
        except Exception as e:
            log.warning("Chưa lấy được danh sách luồng từ go2rtc (%s): %s", url, e)
        return []
