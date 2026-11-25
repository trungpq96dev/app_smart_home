"""HTTP Playback API phục vụ màn Xem lại Timeline kiểu Imou/EZVIZ trên Tablet.

Tính năng cốt lõi:
- `GET /api/health`                   -> Trạng thái dịch vụ, dung lượng cache, Drive.
- `GET /api/days?cam=<ten>`           -> Các ngày có bản ghi 24/24 & số sự kiện người.
- `GET /api/timeline?cam=<ten>&date=YYYY-MM-DD` -> Toàn bộ đoạn video + mốc có người trong ngày.
- `GET /api/video/<seg_id>`           -> Stream MP4 chuẩn HTTP `Range: bytes=...` (206 Partial Content)
                                         để tua mượt từng giây; tự kéo từ Google Drive về nếu ngày cũ.
- `GET /api/thumb/<event_id>.jpg`     -> Ảnh thumbnail đã khoanh khung người.
- `POST /api/snapshot?cam=<ten>`      -> Chụp ảnh tức thì từ go2rtc, lưu SQLite/Drive/Telegram.
"""

from __future__ import annotations

import base64
import json
import logging
import re
import threading
import urllib.parse
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any, Callable

from cau_hinh import CauHinhRecorder, ngay_vn_str
from db import CameraDb
from storage import BYTE_MOI_GB, QuanLyLuuTru

log = logging.getLogger("camera_recorder.api")

_MAU_RANGE = re.compile(r"^bytes=(\d*)-(\d*)$")
_MAU_NGAY = re.compile(r"^\d{4}-\d{2}-\d{2}$")


def phan_tich_http_range(range_header: str | None, file_size: int) -> tuple[int, int] | None:
    """Đổi header `Range: bytes=start-end` sang cặp chỉ số byte bao gồm `(start, end)`.

    Trả `None` nếu không có header Range; ném `ValueError` nếu vượt ngoài kích thước file (416).
    """
    if not range_header or file_size <= 0:
        return None
    m = _MAU_RANGE.match(range_header.strip())
    if not m:
        return None
    dau_s, cuoi_s = m.group(1), m.group(2)
    if not dau_s and not cuoi_s:
        return None
    if not dau_s:
        # bytes=-500 -> 500 byte cuối
        hau_to = int(cuoi_s)
        if hau_to <= 0:
            raise ValueError("Invalid suffix range")
        start = max(0, file_size - hau_to)
        end = file_size - 1
        return (start, end)

    start = int(dau_s)
    end = int(cuoi_s) if cuoi_s else (file_size - 1)
    if start >= file_size or start > end:
        raise ValueError("Range Not Satisfiable")
    end = min(end, file_size - 1)
    return (start, end)


class MayChuPlayback:
    def __init__(
        self,
        cfg: CauHinhRecorder,
        db: CameraDb,
        storage: QuanLyLuuTru,
        chup_anh_cb: Callable[[str], dict[str, Any]] | None = None,
        trang_thai_cb: Callable[[], dict[str, Any]] | None = None,
    ) -> None:
        self.cfg = cfg
        self.db = db
        self.storage = storage
        self.chup_anh_cb = chup_anh_cb
        self.trang_thai_cb = trang_thai_cb
        self._httpd: ThreadingHTTPServer | None = None
        self._luong: threading.Thread | None = None

    def bat_dau(self) -> int:
        handler_cls = self._tao_handler()
        self._httpd = ThreadingHTTPServer((self.cfg.api_host, self.cfg.api_port), handler_cls)
        self._httpd.daemon_threads = True
        cong_that = int(self._httpd.server_address[1])
        self._luong = threading.Thread(target=self._httpd.serve_forever, name="playback-http", daemon=True)
        self._luong.start()
        log.info("HTTP Playback API đang lắng nghe tại %s:%d", self.cfg.api_host, cong_that)
        return cong_that

    def dung(self) -> None:
        if self._httpd:
            self._httpd.shutdown()
            self._httpd.server_close()
        if self._luong:
            self._luong.join(timeout=3.0)

    def kiem_tra_xac_thuc(self, auth_header: str | None, query_params: dict[str, list[str]]) -> bool:
        if not self.cfg.api_user:
            return True
        mong_doi = base64.b64encode(
            f"{self.cfg.api_user}:{self.cfg.api_pass}".encode("utf-8")
        ).decode("ascii")
        if auth_header and auth_header.startswith("Basic "):
            if auth_header[6:].strip() == mong_doi:
                return True
        token_q = query_params.get("auth", [""])[0].strip()
        return bool(token_q) and token_q == mong_doi

    def _tao_handler(self):
        parent = self

        class Handler(BaseHTTPRequestHandler):
            protocol_version = "HTTP/1.1"

            def log_message(self, fmt: str, *args) -> None:
                log.debug("HTTP %s - " + fmt, self.address_string(), *args)

            def _them_cors(self) -> None:
                self.send_header("Access-Control-Allow-Origin", "*")
                self.send_header("Access-Control-Allow-Headers", "Authorization, Range, Content-Type")
                self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")

            def _tra_json(self, status: int, obj: Any) -> None:
                than = json.dumps(obj, ensure_ascii=False).encode("utf-8")
                self.send_response(status)
                self._them_cors()
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(than)))
                self.end_headers()
                self.wfile.write(than)

            def do_OPTIONS(self) -> None:
                self.send_response(HTTPStatus.NO_CONTENT)
                self._them_cors()
                self.send_header("Content-Length", "0")
                self.end_headers()

            def do_GET(self) -> None:
                parsed = urllib.parse.urlparse(self.path)
                qs = urllib.parse.parse_qs(parsed.query)
                if not parent.kiem_tra_xac_thuc(self.headers.get("Authorization"), qs):
                    self._tra_json(HTTPStatus.UNAUTHORIZED, {"error": "Sai hoặc thiếu tài khoản Playback API"})
                    return

                path = parsed.path.rstrip("/") or "/"

                if path == "/api/health":
                    tk = parent.db.thong_ke_tong_hop()
                    extra = parent.trang_thai_cb() if parent.trang_thai_cb else {}
                    self._tra_json(
                        HTTPStatus.OK,
                        {
                            "ok": True,
                            "cache_used_gb": round(tk["cache_used_bytes"] / BYTE_MOI_GB, 2),
                            "cache_max_gb": parent.cfg.local_cache_max_gb,
                            "pending_uploads": tk["pending_uploads"],
                            "uploaded_today": tk["uploaded_today"],
                            "person_today": tk["person_today"],
                            **extra,
                        },
                    )
                    return

                if path == "/api/days":
                    cam = qs.get("cam", [""])[0].strip()
                    if not cam:
                        ds_cam = parent.db.danh_sach_camera_da_luu()
                        cam = ds_cam[0] if ds_cam else ""
                    ngay_ds = parent.db.danh_sach_ngay(cam) if cam else []
                    self._tra_json(HTTPStatus.OK, {"camera": cam, "days": ngay_ds})
                    return

                if path == "/api/timeline":
                    cam = qs.get("cam", [""])[0].strip()
                    ngay = qs.get("date", [ngay_vn_str()])[0].strip()
                    if not cam:
                        self._tra_json(HTTPStatus.BAD_REQUEST, {"error": "Thiếu tham số cam"})
                        return
                    if not _MAU_NGAY.match(ngay):
                        self._tra_json(HTTPStatus.BAD_REQUEST, {"error": "Định dạng date phải là YYYY-MM-DD"})
                        return
                    tl = parent.db.dong_thoi_gian_ngay(cam, ngay)
                    self._tra_json(HTTPStatus.OK, tl)
                    return

                if path.startswith("/api/video/"):
                    duoi = path[len("/api/video/") :].removesuffix(".mp4")
                    if not duoi.isdigit():
                        self._tra_json(HTTPStatus.BAD_REQUEST, {"error": "Mã đoạn video không hợp lệ"})
                        return
                    seg_id = int(duoi)
                    file_p = parent.storage.lay_file_phat(seg_id)
                    if not file_p or not file_p.exists():
                        self._tra_json(
                            HTTPStatus.NOT_FOUND,
                            {"error": "Không tìm thấy đoạn video này trên máy chủ hoặc Google Drive."},
                        )
                        return
                    self._phat_file_range(file_p, "video/mp4")
                    return

                if path.startswith("/api/thumb/"):
                    duoi = path[len("/api/thumb/") :].removesuffix(".jpg")
                    if not duoi.isdigit():
                        self._tra_json(HTTPStatus.BAD_REQUEST, {"error": "Mã sự kiện không hợp lệ"})
                        return
                    ev_id = int(duoi)
                    thumb_p = parent.storage.lay_file_thumbnail(ev_id)
                    if not thumb_p or not thumb_p.exists():
                        self._tra_json(HTTPStatus.NOT_FOUND, {"error": "Không có ảnh thu nhỏ"})
                        return
                    du_lieu = thumb_p.read_bytes()
                    self.send_response(HTTPStatus.OK)
                    self._them_cors()
                    self.send_header("Content-Type", "image/jpeg")
                    self.send_header("Cache-Control", "public, max-age=86400")
                    self.send_header("Content-Length", str(len(du_lieu)))
                    self.end_headers()
                    self.wfile.write(du_lieu)
                    return

                self._tra_json(HTTPStatus.NOT_FOUND, {"error": "Không có đường dẫn này"})

            def do_POST(self) -> None:
                parsed = urllib.parse.urlparse(self.path)
                qs = urllib.parse.parse_qs(parsed.query)
                if not parent.kiem_tra_xac_thuc(self.headers.get("Authorization"), qs):
                    self._tra_json(HTTPStatus.UNAUTHORIZED, {"error": "Sai hoặc thiếu tài khoản Playback API"})
                    return

                path = parsed.path.rstrip("/")
                if path == "/api/snapshot":
                    cam = qs.get("cam", [""])[0].strip()
                    if not cam:
                        self._tra_json(HTTPStatus.BAD_REQUEST, {"error": "Thiếu tham số cam"})
                        return
                    if not parent.chup_anh_cb:
                        self._tra_json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": "Chưa nối hàm chụp ảnh"})
                        return
                    kq = parent.chup_anh_cb(cam)
                    ma = HTTPStatus.OK if kq.get("ok") else HTTPStatus.BAD_GATEWAY
                    self._tra_json(ma, kq)
                    return

                self._tra_json(HTTPStatus.NOT_FOUND, {"error": "Không có đường dẫn này"})

            def _phat_file_range(self, file_p: Path, mime: str) -> None:
                size = file_p.stat().st_size
                range_hdr = self.headers.get("Range")
                try:
                    khoang = phan_tich_http_range(range_hdr, size)
                except ValueError:
                    self.send_response(HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    self._them_cors()
                    self.send_header("Content-Range", f"bytes */{size}")
                    self.send_header("Content-Length", "0")
                    self.end_headers()
                    return

                try:
                    with file_p.open("rb") as f:
                        if khoang is None:
                            self.send_response(HTTPStatus.OK)
                            self._them_cors()
                            self.send_header("Content-Type", mime)
                            self.send_header("Accept-Ranges", "bytes")
                            self.send_header("Content-Length", str(size))
                            self.end_headers()
                            con_lai = size
                        else:
                            start, end = khoang
                            con_lai = end - start + 1
                            f.seek(start)
                            self.send_response(HTTPStatus.PARTIAL_CONTENT)
                            self._them_cors()
                            self.send_header("Content-Type", mime)
                            self.send_header("Accept-Ranges", "bytes")
                            self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
                            self.send_header("Content-Length", str(con_lai))
                            self.end_headers()

                        while con_lai > 0:
                            mieng = f.read(min(65536, con_lai))
                            if not mieng:
                                break
                            self.wfile.write(mieng)
                            con_lai -= len(mieng)
                except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                    # MediaPlayer hủy kết nối cũ khi người dùng kéo thanh timeline sang điểm khác — bình thường.
                    pass

        return Handler
