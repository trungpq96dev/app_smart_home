"""Điều khiển xoay 360° (PTZ), Preset góc quay và Chế độ Riêng tư 1 chạm qua ONVIF Profile S.

Thiết kế thuần thư viện chuẩn Python (`urllib.request`, `hashlib`, `xml.etree.ElementTree`)
để không phải kéo thêm `zeep`/`onvif-zeep` nặng nề vào container trên serverubuntu.
Tương thích tốt với Imou Ranger 2 (A32), Ezviz C6N, Ezviz H6C khi bật ONVIF nội bộ.
"""

from __future__ import annotations

import base64
import hashlib
import json
import logging
import os
import threading
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable
from xml.sax.saxutils import escape as xml_escape

log = logging.getLogger("camera_recorder.onvif_ptz")

SOAP_ENV_NS = "http://www.w3.org/2003/05/soap-envelope"
PTZ_NS = "http://www.onvif.org/ver20/ptz/wsdl"
TT_NS = "http://www.onvif.org/ver10/schema"

# 4 góc quay mẫu hiển thị ngay trên màn hình Camera của Tablet
PRESET_MAC_DINH: list[dict[str, str]] = [
    {"id": "cua_chinh", "label": "Cửa chính", "token": "1"},
    {"id": "ban_an", "label": "Bàn ăn", "token": "2"},
    {"id": "cau_thang", "label": "Cầu thang", "token": "3"},
    {"id": "toan_canh", "label": "Toàn cảnh", "token": "4"},
]


@dataclass
class CauHinhOnvifCam:
    """Thông số ONVIF của 1 camera vật lý (Imou / Ezviz)."""

    camera: str
    host: str
    port: int = 80
    user: str = "admin"
    password: str = ""
    ptz_path: str = "/onvif/ptz_service"
    profile_token: str = "Profile_1"
    indoor: bool = True
    privacy_preset: str = "privacy"
    home_preset: str = "1"
    presets: list[dict[str, str]] = field(default_factory=lambda: [dict(p) for p in PRESET_MAC_DINH])

    @property
    def ptz_url(self) -> str:
        path = self.ptz_path if self.ptz_path.startswith("/") else f"/{self.ptz_path}"
        return f"http://{self.host}:{self.port}{path}"

    def tim_token_preset(self, preset_key: str) -> str:
        """Tìm ONVIF PresetToken theo id (`cua_chinh`), nhãn (`Cửa chính`) hoặc token thô (`1`)."""
        k = preset_key.strip().lower()
        for p in self.presets:
            if (
                p.get("id", "").lower() == k
                or p.get("token", "").lower() == k
                or p.get("label", "").lower() == k
            ):
                return str(p.get("token", preset_key))
        return preset_key.strip()


def tao_wsse_header(user: str, password: str, nonce: bytes | None = None, created: str | None = None) -> str:
    """Tạo SOAP Security Header theo chuẩn WS-UsernameToken (PasswordDigest = Base64(SHA1(nonce+created+pass)))."""
    if not user:
        return ""
    raw_nonce = nonce if nonce is not None else os.urandom(16)
    ts = created if created is not None else datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    digest_bytes = hashlib.sha1(raw_nonce + ts.encode("utf-8") + password.encode("utf-8")).digest()
    digest_b64 = base64.b64encode(digest_bytes).decode("ascii")
    nonce_b64 = base64.b64encode(raw_nonce).decode("ascii")
    return (
        "<s:Header>"
        '<Security s:mustUnderstand="1" xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">'
        "<UsernameToken>"
        f"<Username>{xml_escape(user)}</Username>"
        '<Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest">'
        f"{digest_b64}</Password>"
        '<Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">'
        f"{nonce_b64}</Nonce>"
        '<Created xmlns="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd">'
        f"{ts}</Created>"
        "</UsernameToken>"
        "</Security>"
        "</s:Header>"
    )


def boc_soap_envelope(body_xml: str, user: str = "", password: str = "") -> bytes:
    header = tao_wsse_header(user, password) if user else ""
    xml = (
        '<?xml version="1.0" encoding="UTF-8"?>'
        f'<s:Envelope xmlns:s="{SOAP_ENV_NS}" xmlns:tptz="{PTZ_NS}" xmlns:tt="{TT_NS}">'
        f"{header}"
        f"<s:Body>{body_xml}</s:Body>"
        "</s:Envelope>"
    )
    return xml.encode("utf-8")


def tao_body_continuous_move(profile_token: str, pan: float, tilt: float, zoom: float = 0.0) -> str:
    pan_clamped = max(-1.0, min(1.0, float(pan)))
    tilt_clamped = max(-1.0, min(1.0, float(tilt)))
    zoom_clamped = max(-1.0, min(1.0, float(zoom)))
    zoom_xml = f'<tt:Zoom x="{zoom_clamped:.2f}"/>' if abs(zoom_clamped) > 0.001 else ""
    return (
        "<tptz:ContinuousMove>"
        f"<tptz:ProfileToken>{xml_escape(profile_token)}</tptz:ProfileToken>"
        "<tptz:Velocity>"
        f'<tt:PanTilt x="{pan_clamped:.2f}" y="{tilt_clamped:.2f}"/>'
        f"{zoom_xml}"
        "</tptz:Velocity>"
        "</tptz:ContinuousMove>"
    )


def tao_body_stop(profile_token: str) -> str:
    return (
        "<tptz:Stop>"
        f"<tptz:ProfileToken>{xml_escape(profile_token)}</tptz:ProfileToken>"
        "<tptz:PanTilt>true</tptz:PanTilt>"
        "<tptz:Zoom>true</tptz:Zoom>"
        "</tptz:Stop>"
    )


def tao_body_goto_preset(profile_token: str, preset_token: str, speed: float = 0.7) -> str:
    sp = max(0.1, min(1.0, float(speed)))
    return (
        "<tptz:GotoPreset>"
        f"<tptz:ProfileToken>{xml_escape(profile_token)}</tptz:ProfileToken>"
        f"<tptz:PresetToken>{xml_escape(preset_token)}</tptz:PresetToken>"
        "<tptz:Speed>"
        f'<tt:PanTilt x="{sp:.2f}" y="{sp:.2f}"/>'
        "</tptz:Speed>"
        "</tptz:GotoPreset>"
    )


def tao_body_set_preset(profile_token: str, preset_name: str, preset_token: str | None = None) -> str:
    tok_xml = f"<tptz:PresetToken>{xml_escape(preset_token)}</tptz:PresetToken>" if preset_token else ""
    return (
        "<tptz:SetPreset>"
        f"<tptz:ProfileToken>{xml_escape(profile_token)}</tptz:ProfileToken>"
        f"<tptz:PresetName>{xml_escape(preset_name)}</tptz:PresetName>"
        f"{tok_xml}"
        "</tptz:SetPreset>"
    )


def tao_body_get_presets(profile_token: str) -> str:
    return (
        "<tptz:GetPresets>"
        f"<tptz:ProfileToken>{xml_escape(profile_token)}</tptz:ProfileToken>"
        "</tptz:GetPresets>"
    )


def phan_tich_presets_xml(xml_bytes: bytes) -> list[dict[str, str]]:
    """Đọc danh sách Preset từ phản hồi SOAP `GetPresetsResponse`."""
    ket_qua: list[dict[str, str]] = []
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError:
        return ket_qua
    for elem in root.iter():
        if elem.tag.endswith("Preset"):
            tok = elem.attrib.get("token", "").strip()
            name = ""
            for child in elem:
                if child.tag.endswith("Name") and child.text:
                    name = child.text.strip()
                    break
            if tok:
                ket_qua.append({"id": tok, "token": tok, "label": name or f"Góc {tok}"})
    return ket_qua


def huong_sang_vector(direction: str, speed: float = 0.5) -> tuple[float, float, float]:
    """Đổi tên hướng (`left`, `right`, `up`, `down`, `zoom_in`, `zoom_out`, `stop`) sang (pan, tilt, zoom)."""
    sp = max(0.1, min(1.0, float(speed)))
    d = direction.strip().lower()
    if d == "left":
        return (-sp, 0.0, 0.0)
    if d == "right":
        return (sp, 0.0, 0.0)
    if d == "up":
        return (0.0, sp, 0.0)
    if d == "down":
        return (0.0, -sp, 0.0)
    if d == "up_left":
        return (-sp, sp, 0.0)
    if d == "up_right":
        return (sp, sp, 0.0)
    if d == "down_left":
        return (-sp, -sp, 0.0)
    if d == "down_right":
        return (sp, -sp, 0.0)
    if d == "zoom_in":
        return (0.0, 0.0, sp)
    if d == "zoom_out":
        return (0.0, 0.0, -sp)
    return (0.0, 0.0, 0.0)


def doc_danh_sach_onvif(json_raw: str = "", file_path: Path | None = None) -> dict[str, CauHinhOnvifCam]:
    """Nạp cấu hình ONVIF từ chuỗi JSON (`.env`) hoặc file `/data/onvif_cameras.json`."""
    du_lieu: Any = None
    if json_raw.strip():
        try:
            du_lieu = json.loads(json_raw)
        except Exception as e:
            log.warning("ONVIF_CAMERAS_JSON không hợp lệ: %s", e)
    elif file_path and file_path.exists():
        try:
            du_lieu = json.loads(file_path.read_text(encoding="utf-8"))
        except Exception as e:
            log.warning("Không đọc được file cấu hình ONVIF %s: %s", file_path, e)

    ket_qua: dict[str, CauHinhOnvifCam] = {}
    if isinstance(du_lieu, dict):
        items = [{"camera": k, **v} for k, v in du_lieu.items() if isinstance(v, dict)]
    elif isinstance(du_lieu, list):
        items = [x for x in du_lieu if isinstance(x, dict)]
    else:
        return ket_qua

    for item in items:
        cam = str(item.get("camera", "")).strip()
        host = str(item.get("host", "")).strip()
        if not cam or not host:
            continue
        presets_raw = item.get("presets")
        presets = [dict(p) for p in PRESET_MAC_DINH]
        if isinstance(presets_raw, list) and presets_raw:
            presets = []
            for p in presets_raw:
                if isinstance(p, dict) and p.get("token"):
                    pid = str(p.get("id") or p["token"]).strip()
                    presets.append(
                        {
                            "id": pid,
                            "token": str(p["token"]).strip(),
                            "label": str(p.get("label") or pid).strip(),
                        }
                    )
        ket_qua[cam] = CauHinhOnvifCam(
            camera=cam,
            host=host,
            port=int(item.get("port", 80)),
            user=str(item.get("user", "admin")),
            password=str(item.get("password", "")),
            ptz_path=str(item.get("ptz_path", "/onvif/ptz_service")),
            profile_token=str(item.get("profile_token", "Profile_1")),
            indoor=bool(item.get("indoor", True)),
            privacy_preset=str(item.get("privacy_preset", "privacy")),
            home_preset=str(item.get("home_preset", "1")),
            presets=presets,
        )
    return ket_qua


class QuanLyPtzOnvif:
    """Quản lý xoay 360°, Preset và Chế độ Riêng tư 1 chạm cho tất cả camera."""

    def __init__(
        self,
        cameras: dict[str, CauHinhOnvifCam] | None = None,
        soap_sender: Callable[[str, bytes, str], tuple[int, bytes]] | None = None,
        step_duration_sec: float = 0.55,
    ) -> None:
        self.cameras: dict[str, CauHinhOnvifCam] = dict(cameras or {})
        self._sender = soap_sender or self._gui_soap_that
        self.step_duration_sec = max(0.15, min(2.0, float(step_duration_sec)))
        self._lock = threading.Lock()
        self.privacy_mode: bool = False
        self._stop_timers: dict[str, threading.Timer] = {}

    def _tim_cam(self, camera: str) -> CauHinhOnvifCam | None:
        c = camera.strip()
        if c in self.cameras:
            return self.cameras[c]
        # Cho phép khớp tên luồng phụ `_sd` về camera gốc
        if c.endswith("_sd") and c[:-3] in self.cameras:
            return self.cameras[c[:-3]]
        return None

    def danh_sach_preset(self, camera: str) -> list[dict[str, str]]:
        cfg = self._tim_cam(camera)
        if cfg is None:
            return [dict(p) for p in PRESET_MAC_DINH]
        return [dict(p) for p in cfg.presets]

    def la_cam_trong_nha(self, camera: str) -> bool:
        cfg = self._tim_cam(camera)
        if cfg is None:
            return True
        return cfg.indoor

    def trang_thai_tom_tat(self) -> dict[str, Any]:
        return {
            "privacy_mode": self.privacy_mode,
            "ptz_cameras": sorted(self.cameras.keys()),
            "indoor_cameras": sorted(k for k, v in self.cameras.items() if v.indoor),
        }

    def di_chuyen(
        self,
        camera: str,
        direction: str,
        speed: float = 0.5,
        auto_stop: bool = True,
    ) -> dict[str, Any]:
        cfg = self._tim_cam(camera)
        if cfg is None:
            return {
                "ok": False,
                "message": f"Camera '{camera}' chưa khai báo thông số ONVIF PTZ (ONVIF_CAMERAS_JSON / onvif_cameras.json).",
            }

        d = direction.strip().lower()
        if d == "stop":
            self._huy_hen_dung(cfg.camera)
            ok, err = self._gui_lenh(cfg, tao_body_stop(cfg.profile_token), "Stop")
            return {"ok": ok, "message": "Đã dừng xoay camera." if ok else err}

        pan, tilt, zoom = huong_sang_vector(d, speed)
        if abs(pan) < 0.001 and abs(tilt) < 0.001 and abs(zoom) < 0.001:
            return {"ok": False, "message": f"Hướng PTZ không hợp lệ: {direction}"}

        body = tao_body_continuous_move(cfg.profile_token, pan, tilt, zoom)
        ok, err = self._gui_lenh(cfg, body, "ContinuousMove")
        if not ok:
            return {"ok": False, "message": err}

        if auto_stop:
            self._hen_tu_dung(cfg)

        nhan_huong = {
            "left": "trái",
            "right": "phải",
            "up": "lên",
            "down": "xuống",
            "zoom_in": "phóng to",
            "zoom_out": "thu nhỏ",
        }.get(d, d)
        return {"ok": True, "message": f"Đã xoay '{camera}' {nhan_huong}."}

    def goi_preset(self, camera: str, preset_key: str) -> dict[str, Any]:
        cfg = self._tim_cam(camera)
        if cfg is None:
            return {
                "ok": False,
                "message": f"Camera '{camera}' chưa khai báo thông số ONVIF PTZ.",
            }
        token = cfg.tim_token_preset(preset_key)
        self._huy_hen_dung(cfg.camera)
        ok, err = self._gui_lenh(cfg, tao_body_goto_preset(cfg.profile_token, token), "GotoPreset")
        if not ok:
            return {"ok": False, "message": err}
        nhan = next((p["label"] for p in cfg.presets if p["token"] == token or p["id"] == preset_key), preset_key)
        return {"ok": True, "message": f"Đã xoay '{camera}' tới góc {nhan}."}

    def luu_preset(self, camera: str, preset_key: str, label: str = "") -> dict[str, Any]:
        cfg = self._tim_cam(camera)
        if cfg is None:
            return {"ok": False, "message": f"Camera '{camera}' chưa khai báo ONVIF PTZ."}
        token = cfg.tim_token_preset(preset_key)
        ten = label.strip() or preset_key
        ok, err = self._gui_lenh(cfg, tao_body_set_preset(cfg.profile_token, ten, token), "SetPreset")
        if not ok:
            return {"ok": False, "message": err}
        return {"ok": True, "message": f"Đã lưu vị trí hiện tại vào góc '{ten}'."}

    def dat_che_do_rieng_tu(self, bat: bool) -> dict[str, Any]:
        """Chế độ Riêng tư 1 chạm: xoay toàn bộ camera trong nhà úp vào tường & đánh dấu ẩn luồng."""
        with self._lock:
            self.privacy_mode = bool(bat)

        indoor_cams = [c for c in self.cameras.values() if c.indoor]
        ket_qua_cam: list[str] = []
        loi_cam: list[str] = []

        for cfg in indoor_cams:
            if bat:
                # Ưu tiên gọi preset privacy; nếu không có thì xoay úp tối đa (pan=1.0, tilt=-1.0)
                ok, err = self._gui_lenh(
                    cfg,
                    tao_body_goto_preset(cfg.profile_token, cfg.privacy_preset),
                    "GotoPreset",
                )
                if not ok:
                    ok2, err2 = self._gui_lenh(
                        cfg,
                        tao_body_continuous_move(cfg.profile_token, pan=0.8, tilt=-0.8),
                        "ContinuousMove",
                    )
                    if ok2:
                        self._hen_tu_dung(cfg, delay_sec=1.6)
                        ket_qua_cam.append(cfg.camera)
                    else:
                        loi_cam.append(f"{cfg.camera}: {err2 or err}")
                else:
                    ket_qua_cam.append(cfg.camera)
            else:
                ok, err = self._gui_lenh(
                    cfg,
                    tao_body_goto_preset(cfg.profile_token, cfg.home_preset),
                    "GotoPreset",
                )
                if ok:
                    ket_qua_cam.append(cfg.camera)
                else:
                    loi_cam.append(f"{cfg.camera}: {err}")

        if bat:
            msg = "Đã bật Chế độ Riêng tư (ẩn luồng camera trong nhà"
            if ket_qua_cam:
                msg += f" và xoay úp ống kính: {', '.join(ket_qua_cam)}"
            msg += ")."
        else:
            msg = "Đã tắt Chế độ Riêng tư (khôi phục luồng hiển thị"
            if ket_qua_cam:
                msg += f" và đưa về góc mặc định: {', '.join(ket_qua_cam)}"
            msg += ")."

        if loi_cam:
            msg += f" Lưu ý ONVIF: {'; '.join(loi_cam)}"

        return {
            "ok": True,
            "privacy_mode": self.privacy_mode,
            "affected_cameras": ket_qua_cam,
            "errors": loi_cam,
            "message": msg,
        }

    def _hen_tu_dung(self, cfg: CauHinhOnvifCam, delay_sec: float | None = None) -> None:
        self._huy_hen_dung(cfg.camera)
        cho = delay_sec if delay_sec is not None else self.step_duration_sec

        def _dung_sau() -> None:
            self._gui_lenh(cfg, tao_body_stop(cfg.profile_token), "Stop")

        t = threading.Timer(cho, _dung_sau)
        t.daemon = True
        with self._lock:
            self._stop_timers[cfg.camera] = t
        t.start()

    def _huy_hen_dung(self, camera: str) -> None:
        with self._lock:
            cu = self._stop_timers.pop(camera, None)
        if cu is not None:
            cu.cancel()

    def _gui_lenh(self, cfg: CauHinhOnvifCam, body_xml: str, soap_action: str) -> tuple[bool, str]:
        payload = boc_soap_envelope(body_xml, cfg.user, cfg.password)
        status, resp = self._sender(cfg.ptz_url, payload, f"{PTZ_NS}/{soap_action}")
        if 200 <= status < 300:
            return True, ""
        chi_tiet = resp.decode("utf-8", errors="ignore")[:180].strip()
        return False, f"ONVIF {soap_action} thất bại (HTTP {status}): {chi_tiet}"

    @staticmethod
    def _gui_soap_that(url: str, payload: bytes, action: str) -> tuple[int, bytes]:
        req = urllib.request.Request(
            url,
            data=payload,
            method="POST",
            headers={
                "Content-Type": f'application/soap+xml; charset=utf-8; action="{action}"',
                "Content-Length": str(len(payload)),
            },
        )
        try:
            with urllib.request.urlopen(req, timeout=5.0) as resp:
                return int(resp.status), resp.read()
        except urllib.error.HTTPError as e:
            return int(e.code), e.read()
        except Exception as e:
            return 0, str(e).encode("utf-8")
