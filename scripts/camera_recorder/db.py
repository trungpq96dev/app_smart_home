"""Chỉ mục SQLite lưu các đoạn video 24/24 (segments) và sự kiện có người (events).

Thiết kế để truy vấn dòng thời gian 24 giờ như Imou/EZVIZ trong vài mili-giây
mà không phải gọi API liệt kê thư mục trên Google Drive.
"""

from __future__ import annotations

import sqlite3
import threading
from pathlib import Path
from typing import Any

from cau_hinh import GIO_VN, khoang_ngay_vn, ngay_vn_str


SCHEMA_SQL = """
PRAGMA journal_mode=WAL;
PRAGMA synchronous=NORMAL;
PRAGMA foreign_keys=ON;

CREATE TABLE IF NOT EXISTS segments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    camera TEXT NOT NULL,
    start_ts INTEGER NOT NULL,
    end_ts INTEGER NOT NULL,
    duration_sec REAL NOT NULL,
    size_bytes INTEGER NOT NULL DEFAULT 0,
    local_path TEXT,
    drive_path TEXT,
    status TEXT NOT NULL DEFAULT 'local'
);

CREATE INDEX IF NOT EXISTS idx_segments_cam_time
    ON segments(camera, start_ts, end_ts);
CREATE INDEX IF NOT EXISTS idx_segments_status
    ON segments(status, start_ts);

CREATE TABLE IF NOT EXISTS events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    camera TEXT NOT NULL,
    kind TEXT NOT NULL DEFAULT 'person',
    start_ts INTEGER NOT NULL,
    end_ts INTEGER NOT NULL,
    max_conf REAL NOT NULL DEFAULT 0.0,
    person_count INTEGER NOT NULL DEFAULT 1,
    thumb_local TEXT,
    thumb_drive TEXT,
    notified_tg INTEGER NOT NULL DEFAULT 0,
    label TEXT,
    clip_local TEXT
);

CREATE INDEX IF NOT EXISTS idx_events_cam_time
    ON events(camera, start_ts, end_ts);
"""


class CameraDb:
    """Lớp truy cập SQLite an toàn đa luồng (WAL + Lock cho thao tác ghi)."""

    def __init__(self, db_path: Path) -> None:
        self.db_path = Path(db_path)
        self.db_path.parent.mkdir(parents=True, exist_ok=True)
        self._lock = threading.RLock()
        self._conn = sqlite3.connect(str(self.db_path), check_same_thread=False, timeout=15.0)
        self._conn.row_factory = sqlite3.Row
        with self._lock:
            self._conn.executescript(SCHEMA_SQL)
            self._nang_cap_cot_events()
            self._conn.commit()

    def _nang_cap_cot_events(self) -> None:
        """Tự thêm cột `label` và `clip_local` nếu đang nâng cấp từ index.db bản cũ."""
        cols = {
            row["name"]
            for row in self._conn.execute("PRAGMA table_info(events)").fetchall()
        }
        if "label" not in cols:
            self._conn.execute("ALTER TABLE events ADD COLUMN label TEXT")
        if "clip_local" not in cols:
            self._conn.execute("ALTER TABLE events ADD COLUMN clip_local TEXT")

    def dong(self) -> None:
        with self._lock:
            self._conn.close()

    # ------------------------------------------------------------- segments

    def them_segment(
        self,
        camera: str,
        start_ts: int,
        end_ts: int,
        size_bytes: int,
        local_path: str,
        status: str = "local",
        drive_path: str | None = None,
    ) -> int:
        duration = max(1.0, float(end_ts - start_ts))
        with self._lock:
            cur = self._conn.execute(
                """
                INSERT INTO segments(camera, start_ts, end_ts, duration_sec, size_bytes, local_path, drive_path, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (camera, int(start_ts), int(end_ts), duration, int(size_bytes), local_path, drive_path, status),
            )
            self._conn.commit()
            return int(cur.lastrowid)

    def danh_dau_da_upload(self, seg_id: int, drive_path: str) -> None:
        with self._lock:
            self._conn.execute(
                """
                UPDATE segments
                SET drive_path = ?,
                    status = CASE WHEN local_path IS NOT NULL THEN 'uploaded' ELSE 'cloud_only' END
                WHERE id = ?
                """,
                (drive_path, seg_id),
            )
            self._conn.commit()

    def danh_dau_xoa_local(self, seg_id: int) -> None:
        """Gỡ đường dẫn local khi file bị dọn khỏi Hot Cache 10 GB (vẫn còn trên Drive)."""
        with self._lock:
            self._conn.execute(
                """
                UPDATE segments
                SET local_path = NULL,
                    status = CASE WHEN drive_path IS NOT NULL THEN 'cloud_only' ELSE 'evicted' END
                WHERE id = ?
                """,
                (seg_id,),
            )
            self._conn.commit()

    def xoa_segment_han(self, seg_id: int) -> None:
        with self._lock:
            self._conn.execute("DELETE FROM segments WHERE id = ?", (seg_id,))
            self._conn.commit()

    def lay_segment(self, seg_id: int) -> dict[str, Any] | None:
        with self._lock:
            row = self._conn.execute("SELECT * FROM segments WHERE id = ?", (seg_id,)).fetchone()
            return dict(row) if row else None

    def danh_sach_cho_upload(self, gioi_han: int = 20) -> list[dict[str, Any]]:
        with self._lock:
            rows = self._conn.execute(
                """
                SELECT * FROM segments
                WHERE status = 'local' AND local_path IS NOT NULL
                ORDER BY start_ts ASC
                LIMIT ?
                """,
                (gioi_han,),
            ).fetchall()
            return [dict(r) for r in rows]

    def danh_sach_co_local(self, chi_da_upload: bool = True) -> list[dict[str, Any]]:
        """Lấy các segment đang chiếm dung lượng ở Hot Cache cục bộ (cũ nhất trước)."""
        dk = "AND status = 'uploaded'" if chi_da_upload else ""
        with self._lock:
            rows = self._conn.execute(
                f"""
                SELECT * FROM segments
                WHERE local_path IS NOT NULL {dk}
                ORDER BY start_ts ASC
                """
            ).fetchall()
            return [dict(r) for r in rows]

    def danh_sach_qua_han(self, truoc_ts: int) -> list[dict[str, Any]]:
        with self._lock:
            rows = self._conn.execute(
                "SELECT * FROM segments WHERE end_ts < ? ORDER BY start_ts ASC",
                (int(truoc_ts),),
            ).fetchall()
            return [dict(r) for r in rows]

    # --------------------------------------------------------------- events

    def them_event(
        self,
        camera: str,
        kind: str,
        start_ts: int,
        end_ts: int,
        max_conf: float = 0.0,
        person_count: int = 1,
        thumb_local: str | None = None,
        label: str | None = None,
        clip_local: str | None = None,
    ) -> int:
        with self._lock:
            cur = self._conn.execute(
                """
                INSERT INTO events(camera, kind, start_ts, end_ts, max_conf, person_count, thumb_local, label, clip_local)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    camera,
                    kind,
                    int(start_ts),
                    int(max(start_ts + 1, end_ts)),
                    float(max_conf),
                    int(person_count),
                    thumb_local,
                    label,
                    clip_local,
                ),
            )
            self._conn.commit()
            return int(cur.lastrowid)

    def cap_nhat_clip_event(self, event_id: int, clip_local: str) -> None:
        with self._lock:
            self._conn.execute(
                "UPDATE events SET clip_local = ? WHERE id = ?",
                (clip_local, event_id),
            )
            self._conn.commit()

    def danh_sach_su_kien_gan_nhat(
        self,
        camera: str | None = None,
        kind: str | None = None,
        gioi_han: int = 60,
    ) -> list[dict[str, Any]]:
        """Trả về danh sách sự kiện mới nhất phục vụ tab 'Sự kiện' Mini-NVR trên Tablet."""
        dk: list[str] = []
        tham_so: list[Any] = []
        if camera:
            dk.append("camera = ?")
            tham_so.append(camera)
        if kind and kind != "all":
            dk.append("kind = ?")
            tham_so.append(kind)
        where_sql = ("WHERE " + " AND ".join(dk)) if dk else ""
        tham_so.append(max(1, min(200, int(gioi_han))))
        with self._lock:
            rows = self._conn.execute(
                f"""
                SELECT id, camera, kind, start_ts, end_ts, max_conf, person_count,
                       thumb_local, thumb_drive, label, clip_local
                FROM events
                {where_sql}
                ORDER BY start_ts DESC, id DESC
                LIMIT ?
                """,
                tuple(tham_so),
            ).fetchall()
        ket_qua: list[dict[str, Any]] = []
        for e in rows:
            co_thumb = (bool(e["thumb_local"]) and Path(e["thumb_local"]).exists()) or bool(e["thumb_drive"])
            co_clip = bool(e["clip_local"]) and Path(e["clip_local"]).exists()
            ket_qua.append(
                {
                    "id": int(e["id"]),
                    "camera": str(e["camera"]),
                    "kind": str(e["kind"]),
                    "start_ts": int(e["start_ts"]),
                    "end_ts": int(e["end_ts"]),
                    "max_conf": round(float(e["max_conf"]), 3),
                    "person_count": int(e["person_count"]),
                    "has_thumb": co_thumb,
                    "has_clip": co_clip,
                    "label": e["label"],
                }
            )
        return ket_qua

    def cap_nhat_event(
        self,
        event_id: int,
        end_ts: int,
        max_conf: float | None = None,
        person_count: int | None = None,
        thumb_local: str | None = None,
    ) -> None:
        with self._lock:
            row = self._conn.execute("SELECT * FROM events WHERE id = ?", (event_id,)).fetchone()
            if not row:
                return
            new_end = max(int(row["start_ts"]) + 1, int(end_ts))
            new_conf = max(float(row["max_conf"]), float(max_conf)) if max_conf is not None else float(row["max_conf"])
            new_cnt = max(int(row["person_count"]), int(person_count)) if person_count is not None else int(row["person_count"])
            new_thumb = thumb_local if thumb_local is not None else row["thumb_local"]
            self._conn.execute(
                """
                UPDATE events
                SET end_ts = ?, max_conf = ?, person_count = ?, thumb_local = ?
                WHERE id = ?
                """,
                (new_end, new_conf, new_cnt, new_thumb, event_id),
            )
            self._conn.commit()

    def danh_dau_event_drive(self, event_id: int, thumb_drive: str) -> None:
        with self._lock:
            self._conn.execute(
                "UPDATE events SET thumb_drive = ? WHERE id = ?",
                (thumb_drive, event_id),
            )
            self._conn.commit()

    def danh_dau_event_telegram(self, event_id: int) -> None:
        with self._lock:
            self._conn.execute(
                "UPDATE events SET notified_tg = 1 WHERE id = ?",
                (event_id,),
            )
            self._conn.commit()

    def danh_sach_event_cho_upload_thumb(self, gioi_han: int = 30) -> list[dict[str, Any]]:
        with self._lock:
            rows = self._conn.execute(
                """
                SELECT * FROM events
                WHERE thumb_local IS NOT NULL AND thumb_drive IS NULL
                ORDER BY start_ts ASC
                LIMIT ?
                """,
                (gioi_han,),
            ).fetchall()
            return [dict(r) for r in rows]

    def lay_event(self, event_id: int) -> dict[str, Any] | None:
        with self._lock:
            row = self._conn.execute("SELECT * FROM events WHERE id = ?", (event_id,)).fetchone()
            return dict(row) if row else None

    def xoa_event_qua_han(self, truoc_ts: int) -> list[dict[str, Any]]:
        with self._lock:
            rows = self._conn.execute(
                "SELECT * FROM events WHERE end_ts < ?",
                (int(truoc_ts),),
            ).fetchall()
            ket_qua = [dict(r) for r in rows]
            if ket_qua:
                self._conn.execute("DELETE FROM events WHERE end_ts < ?", (int(truoc_ts),))
                self._conn.commit()
            return ket_qua

    # ---------------------------------------------------- truy van timeline

    def danh_sach_camera_da_luu(self) -> list[str]:
        with self._lock:
            r1 = self._conn.execute("SELECT DISTINCT camera FROM segments").fetchall()
            r2 = self._conn.execute("SELECT DISTINCT camera FROM events").fetchall()
            tap = {r[0] for r in r1} | {r[0] for r in r2}
            return sorted(tap)

    def danh_sach_ngay(self, camera: str, gioi_han: int = 120) -> list[dict[str, Any]]:
        """Trả về danh sách ngày (YYYY-MM-DD theo UTC+7) có bản ghi hoặc sự kiện người."""
        # SQLite shift +7 hours (+25200s) để gom đúng ngày giờ Việt Nam
        with self._lock:
            seg_rows = self._conn.execute(
                """
                SELECT date(start_ts + 25200, 'unixepoch') AS ngay,
                       COUNT(*) AS so_doan,
                       COALESCE(SUM(duration_sec), 0) AS tong_giay
                FROM segments
                WHERE camera = ? AND status != 'evicted'
                GROUP BY ngay
                """,
                (camera,),
            ).fetchall()
            ev_rows = self._conn.execute(
                """
                SELECT date(start_ts + 25200, 'unixepoch') AS ngay,
                       SUM(CASE WHEN kind = 'person' THEN 1 ELSE 0 END) AS so_nguoi
                FROM events
                WHERE camera = ?
                GROUP BY ngay
                """,
                (camera,),
            ).fetchall()

        gop: dict[str, dict[str, Any]] = {}
        for r in seg_rows:
            ngay = r["ngay"]
            if not ngay:
                continue
            gop[ngay] = {
                "date": ngay,
                "segment_count": int(r["so_doan"]),
                "recorded_sec": int(round(float(r["tong_giay"]))),
                "person_events": 0,
            }
        for r in ev_rows:
            ngay = r["ngay"]
            if not ngay:
                continue
            muc = gop.setdefault(
                ngay,
                {"date": ngay, "segment_count": 0, "recorded_sec": 0, "person_events": 0},
            )
            muc["person_events"] = int(r["so_nguoi"] or 0)

        return sorted(gop.values(), key=lambda x: x["date"], reverse=True)[:gioi_han]

    def dong_thoi_gian_ngay(self, camera: str, ngay_str: str) -> dict[str, Any]:
        """Trả toàn bộ đoạn video & sự kiện giao với ngày [ngay_str] (YYYY-MM-DD, UTC+7)."""
        dau_ngay, cuoi_ngay = khoang_ngay_vn(ngay_str)
        with self._lock:
            segs = self._conn.execute(
                """
                SELECT id, start_ts, end_ts, duration_sec, size_bytes, local_path, drive_path, status
                FROM segments
                WHERE camera = ?
                  AND status != 'evicted'
                  AND end_ts > ?
                  AND start_ts < ?
                ORDER BY start_ts ASC
                """,
                (camera, dau_ngay, cuoi_ngay),
            ).fetchall()
            evs = self._conn.execute(
                """
                SELECT id, kind, start_ts, end_ts, max_conf, person_count, thumb_local, thumb_drive, label, clip_local
                FROM events
                WHERE camera = ?
                  AND end_ts > ?
                  AND start_ts < ?
                ORDER BY start_ts ASC
                """,
                (camera, dau_ngay, cuoi_ngay),
            ).fetchall()

        ds_seg = []
        for s in segs:
            co_local = bool(s["local_path"]) and Path(s["local_path"]).exists()
            ds_seg.append(
                {
                    "id": int(s["id"]),
                    "start_ts": int(s["start_ts"]),
                    "end_ts": int(s["end_ts"]),
                    "duration_sec": round(float(s["duration_sec"]), 1),
                    "size_bytes": int(s["size_bytes"]),
                    "tier": "hot" if co_local else "drive",
                }
            )

        ds_ev = []
        for e in evs:
            co_thumb = (bool(e["thumb_local"]) and Path(e["thumb_local"]).exists()) or bool(e["thumb_drive"])
            co_clip = bool(e["clip_local"]) and Path(e["clip_local"]).exists()
            ds_ev.append(
                {
                    "id": int(e["id"]),
                    "kind": str(e["kind"]),
                    "start_ts": int(e["start_ts"]),
                    "end_ts": int(e["end_ts"]),
                    "max_conf": round(float(e["max_conf"]), 3),
                    "person_count": int(e["person_count"]),
                    "has_thumb": co_thumb,
                    "has_clip": co_clip,
                    "label": e["label"],
                }
            )

        return {
            "camera": camera,
            "date": ngay_str,
            "day_start_ts": dau_ngay,
            "day_end_ts": cuoi_ngay,
            "segments": ds_seg,
            "events": ds_ev,
        }

    def thong_ke_tong_hop(self, now_ts: float | None = None) -> dict[str, Any]:
        """Số liệu tổng hợp thật để bắn lên MQTT `camera/state`."""
        hom_nay = ngay_vn_str(now_ts)
        dau_ngay, cuoi_ngay = khoang_ngay_vn(hom_nay)
        with self._lock:
            cho_up = self._conn.execute(
                "SELECT COUNT(*) FROM segments WHERE status = 'local' AND local_path IS NOT NULL"
            ).fetchone()[0]
            da_up_hom_nay = self._conn.execute(
                """
                SELECT COUNT(*), COALESCE(SUM(size_bytes), 0)
                FROM segments
                WHERE drive_path IS NOT NULL AND start_ts >= ? AND start_ts < ?
                """,
                (dau_ngay, cuoi_ngay),
            ).fetchone()
            nguoi_hom_nay = self._conn.execute(
                """
                SELECT COUNT(*) FROM events
                WHERE kind = 'person' AND start_ts >= ? AND start_ts < ?
                """,
                (dau_ngay, cuoi_ngay),
            ).fetchone()[0]
            local_bytes = self._conn.execute(
                "SELECT COALESCE(SUM(size_bytes), 0) FROM segments WHERE local_path IS NOT NULL"
            ).fetchone()[0]

        return {
            "pending_uploads": int(cho_up),
            "uploaded_today": int(da_up_hom_nay[0]),
            "uploaded_today_bytes": int(da_up_hom_nay[1]),
            "person_today": int(nguoi_hom_nay),
            "cache_used_bytes": int(local_bytes),
        }
