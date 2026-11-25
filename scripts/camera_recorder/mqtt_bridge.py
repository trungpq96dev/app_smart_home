"""Cầu nối MQTT Mosquitto cho `camera_recorder` trên nhánh `camera/#`.

Hợp đồng topic:
- `camera/trangthai`   (retained, LWT = "offline"): "online" | "offline"
- `camera/state`       (retained JSON): trạng thái ghi 24/24, AI canh người,
                        dung lượng Hot Cache /10 GB, số file đã đẩy Drive, lỗi từng cam.
- `camera/person`      (retained JSON): sự kiện phát hiện người gần nhất.
- `camera/cmd`         (app publish, không retained):
                        {"action": "SET_DETECT_PERSON"|"SET_RECORDING"|"SNAPSHOT",
                         "value": true/false, "camera": "...", "id": "<mã lệnh>"}
- `camera/cmd/result`  (không retained): kết quả thực thi kèm `tra_loi_id`.
"""

from __future__ import annotations

import json
import logging
import queue
import random
import threading
from typing import Any, Callable

from cau_hinh import CauHinhRecorder, gio_vn_str
from storage import BYTE_MOI_GB

log = logging.getLogger("camera_recorder.mqtt")

try:
    import paho.mqtt.client as mqtt
except ImportError:  # pragma: no cover
    mqtt = None  # type: ignore


def tao_payload_state(
    cfg: CauHinhRecorder,
    thong_ke_db: dict[str, Any],
    trang_thai_cam: dict[str, dict[str, Any]],
    loi_drive: str | None,
    tra_loi_id: str | None = None,
) -> dict[str, Any]:
    return {
        "recording_enabled": bool(cfg.recording_enabled),
        "detect_person_enabled": bool(cfg.detect_person_enabled),
        "telegram_enabled": bool(cfg.tg_enabled),
        "drive_enabled": bool(cfg.drive_enabled),
        "api_port": int(cfg.api_port),
        "cache_used_gb": round(float(thong_ke_db.get("cache_used_bytes", 0)) / BYTE_MOI_GB, 2),
        "cache_max_gb": round(float(cfg.local_cache_max_gb), 1),
        "pending_uploads": int(thong_ke_db.get("pending_uploads", 0)),
        "uploaded_today": int(thong_ke_db.get("uploaded_today", 0)),
        "uploaded_today_mb": round(float(thong_ke_db.get("uploaded_today_bytes", 0)) / (1024 * 1024), 1),
        "person_today": int(thong_ke_db.get("person_today", 0)),
        "drive_error": loi_drive,
        "cameras": trang_thai_cam,
        "cap_nhat": gio_vn_str(),
        "tra_loi_id": tra_loi_id,
    }


def tao_payload_person(
    event_id: int,
    camera: str,
    start_ts: int,
    person_count: int,
    confidence: float,
) -> dict[str, Any]:
    return {
        "event_id": int(event_id),
        "camera": camera,
        "start_ts": int(start_ts),
        "thoi_gian": gio_vn_str(start_ts),
        "person_count": int(person_count),
        "confidence": round(float(confidence), 3),
    }


class CauNoiMqttCamera:
    def __init__(
        self,
        cfg: CauHinhRecorder,
        lay_state_cb: Callable[[str | None], dict[str, Any]],
        chup_anh_cb: Callable[[str], dict[str, Any]],
    ) -> None:
        self.cfg = cfg
        self.lay_state_cb = lay_state_cb
        self.chup_anh_cb = chup_anh_cb
        self.goc = cfg.mqtt_topic_goc.rstrip("/")
        self._hang_lenh: queue.Queue[dict[str, Any] | None] = queue.Queue(maxsize=64)
        self._cl = None
        self._chay = False
        self._luong: threading.Thread | None = None

    def t(self, duoi: str) -> str:
        return f"{self.goc}/{duoi}"

    def bat_dau(self) -> None:
        if mqtt is None or not self.cfg.mqtt_broker:
            log.warning("Bỏ qua MQTT (thiếu paho-mqtt hoặc MQTT_BROKER rỗng).")
            return
        if self._chay:
            return

        cid = f"camera-recorder-{random.randint(10000, 99999)}"
        if hasattr(mqtt, "CallbackAPIVersion"):
            cl = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=cid)
        else:
            cl = mqtt.Client(client_id=cid)

        if self.cfg.mqtt_user:
            cl.username_pw_set(self.cfg.mqtt_user, self.cfg.mqtt_pass)

        cl.will_set(self.t("trangthai"), payload="offline", qos=1, retain=True)
        cl.on_connect = self._on_connect
        cl.on_message = self._on_message
        cl.reconnect_delay_set(min_delay=2, max_delay=30)

        self._cl = cl
        self._chay = True
        self._luong = threading.Thread(target=self._vong_xu_ly_lenh, name="mqtt-cmd", daemon=True)
        self._luong.start()

        try:
            cl.connect_async(self.cfg.mqtt_broker, self.cfg.mqtt_port, keepalive=45)
            cl.loop_start()
        except Exception as e:
            log.error("Không khởi động được kết nối MQTT: %s", e)

    def dung(self) -> None:
        self._chay = False
        try:
            self._hang_lenh.put_nowait(None)
        except queue.Full:
            pass
        if self._luong:
            self._luong.join(timeout=3.0)
        if self._cl is not None:
            try:
                self.pub(self.t("trangthai"), "offline", retain=True)
                self._cl.loop_stop()
                self._cl.disconnect()
            except Exception:
                pass

    def pub(self, topic: str, obj: Any, retain: bool = True) -> None:
        if self._cl is None:
            return
        than = obj if isinstance(obj, str) else json.dumps(obj, ensure_ascii=False)
        try:
            self._cl.publish(topic, than, qos=1, retain=retain)
        except Exception as e:
            log.debug("Lỗi publish %s: %s", topic, e)

    def phat_state(self, tra_loi_id: str | None = None) -> None:
        self.pub(self.t("state"), self.lay_state_cb(tra_loi_id), retain=True)

    def phat_su_kien_nguoi(
        self,
        event_id: int,
        camera: str,
        start_ts: int,
        person_count: int,
        confidence: float,
    ) -> None:
        self.pub(
            self.t("person"),
            tao_payload_person(event_id, camera, start_ts, person_count, confidence),
            retain=True,
        )
        self.phat_state()

    def _on_connect(self, cl, userdata, flags, rc, properties=None) -> None:
        ma = getattr(rc, "value", rc)
        if ma != 0:
            log.error("Mosquitto từ chối đăng nhập (mã %s). Kiểm tra MQTT_USER/MQTT_PASS.", ma)
            return
        log.info("Đã nối Mosquitto tại %s:%d", self.cfg.mqtt_broker, self.cfg.mqtt_port)
        cl.subscribe(self.t("cmd"), qos=1)
        self.pub(self.t("trangthai"), "online", retain=True)
        self.phat_state()

    def _on_message(self, cl, userdata, msg) -> None:
        """Chỉ parse JSON rồi đưa vào hàng đợi — không bao giờ chặn luồng mạng của Paho."""
        try:
            o = json.loads(msg.payload.decode("utf-8"))
        except Exception:
            log.warning("Lệnh camera/cmd không phải JSON: %r", msg.payload[:120])
            return
        if not isinstance(o, dict) or "action" not in o:
            return
        try:
            self._hang_lenh.put_nowait(o)
        except queue.Full:
            log.warning("Hàng đợi lệnh camera/cmd đầy.")

    def xu_ly_mot_lenh(self, o: dict[str, Any]) -> dict[str, Any]:
        """Xử lý 1 lệnh điều khiển từ app, trả về payload `camera/cmd/result`."""
        action = str(o.get("action", "")).strip().upper()
        ma_id = str(o.get("id", "")).strip() or None
        cam = str(o.get("camera", "")).strip()

        if action == "SET_DETECT_PERSON":
            bat = bool(o.get("value", True))
            self.cfg.detect_person_enabled = bat
            self.phat_state(tra_loi_id=ma_id)
            return {
                "tra_loi_id": ma_id,
                "action": action,
                "ok": True,
                "message": "Đã bật phát hiện người" if bat else "Đã tắt phát hiện người",
            }

        if action == "SET_RECORDING":
            bat = bool(o.get("value", True))
            self.cfg.recording_enabled = bat
            self.phat_state(tra_loi_id=ma_id)
            return {
                "tra_loi_id": ma_id,
                "action": action,
                "ok": True,
                "message": "Đang ghi hình 24/24" if bat else "Đã tạm dừng ghi hình",
            }

        if action == "SNAPSHOT":
            if not cam:
                return {"tra_loi_id": ma_id, "action": action, "ok": False, "message": "Thiếu tên camera"}
            kq = self.chup_anh_cb(cam)
            self.phat_state(tra_loi_id=ma_id)
            return {
                "tra_loi_id": ma_id,
                "action": action,
                "ok": bool(kq.get("ok")),
                "event_id": kq.get("event_id"),
                "message": str(kq.get("message", "")),
            }

        return {"tra_loi_id": ma_id, "action": action, "ok": False, "message": f"Lệnh không hỗ trợ: {action}"}

    def _vong_xu_ly_lenh(self) -> None:
        while self._chay:
            try:
                lenh = self._hang_lenh.get(timeout=2.0)
            except queue.Empty:
                continue
            if lenh is None:
                break
            ket_qua = self.xu_ly_mot_lenh(lenh)
            self.pub(self.t("cmd/result"), ket_qua, retain=False)
