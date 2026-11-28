#!/usr/bin/env python3
"""Doc suc khoe serverubuntu -> publish MQTT + API dieu khien Docker an toan.

Chay trong Docker tren chinh serverubuntu. Dung psutil doc CPU/RAM/o dia/nhiet
do/mang cua HOST (xem docker-compose.yml: pid=host, network=host, mount /sys va
host root). Liet ke container + CPU%/RAM qua Docker socket.

BAO MAT DOCKER SOCKET:
  - TUYET DOI KHONG subscribe MQTT de nhan lenh Docker (vi Mosquitto chua co ACL).
  - Dieu khien container chi mo qua HTTP API rieng (mac dinh cong 1986) khi:
      1) DOCKER_CONTROL_ENABLED=true
      2) DOCKER_CONTROL_TOKEN da dat (>= 16 ky tu, so sanh constant-time)
      3) Container nam trong danh sach cho phep DOCKER_ALLOWED_CONTAINERS (fail-closed)
      4) Container KHONG thuoc danh sach bao ve cung (mosquitto, server-collector,
         smarthome-web va chinh container dang chay)
      5) Chi cho phep 3 hanh dong: start | stop | restart (cam exec/rm/create)

Hop dong topic MQTT (app chi doc, khong ghi):
  - server/info     JSON he thong (hostname, os, kernel, cpu, ram, disk, uptime)
  - server/metrics  JSON tuc thoi (cpu%, ram, disk, nhiet do chinh, load, mang)
  - server/docker   JSON list container (them cpu_percent, mem_mb, mem_limit_mb,
                    mem_percent, protected, controllable)
  - server/nhietdo  JSON tat ca cam bien nhiet + cao/thap hom nay (tu hwstats)
  - server/diennang JSON pin + RAPL CPU (ca goi / nhan / phu tro) - tuc thoi
  - server/diennang_ngay  JSON dien nang CPU (Wh) + khoang nhiet do theo ngay (hwstats)
  - server/capnhat  YYYY-MM-DD HH:MM:SS

KHONG BIA SO: truong nao host khong cap (vi du RAPL khong doc duoc, chua cai
hwstats, container dang dung) publish null / mang rong.
"""

from __future__ import annotations

import hmac
import http.client
import json
import logging
import os
import platform
import re
import socket
import sys
import threading
import time
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import quote

try:
    import psutil
except ImportError:
    psutil = None

try:
    import paho.mqtt.client as mqtt
except ImportError:
    mqtt = None

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("server")

MQTT_BROKER = os.getenv("MQTT_BROKER", "127.0.0.1")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
MQTT_USER = os.getenv("MQTT_USER", "")
MQTT_PASS = os.getenv("MQTT_PASS", "")
MQTT_CLIENT_ID = os.getenv("MQTT_CLIENT_ID", "smarthome-server-collector")

POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "5"))
DOCKER_POLL_EVERY = max(1, int(os.getenv("DOCKER_POLL_EVERY", "3")))  # ~15s khi POLL_INTERVAL=5

ROOTFS = os.getenv("ROOTFS", "/rootfs")
DOCKER_SOCK = os.getenv("DOCKER_SOCK", "/var/run/docker.sock")

# --- Cau hinh dieu khien Docker an toan (KHONG qua MQTT) ---

def _parse_bool(val: str | None, default: bool = False) -> bool:
    if val is None:
        return default
    s = val.strip().lower()
    if s in ("1", "true", "yes", "on"):
        return True
    if s in ("0", "false", "no", "off"):
        return False
    return default


def _parse_name_set(val: str | None) -> frozenset[str]:
    if not val:
        return frozenset()
    items = []
    for part in val.split(","):
        cleaned = part.strip().lstrip("/").lower()
        if cleaned:
            items.append(cleaned)
    return frozenset(items)


MIN_CONTROL_TOKEN_LEN = 16
DOCKER_CONTROL_ENABLED = _parse_bool(os.getenv("DOCKER_CONTROL_ENABLED"), False)
DOCKER_CONTROL_BIND = os.getenv("DOCKER_CONTROL_BIND", "0.0.0.0").strip() or "0.0.0.0"
DOCKER_CONTROL_PORT = int(os.getenv("DOCKER_CONTROL_PORT", "1986"))
DOCKER_CONTROL_TOKEN = os.getenv("DOCKER_CONTROL_TOKEN", "").strip()
DOCKER_ALLOWED_CONTAINERS = _parse_name_set(os.getenv("DOCKER_ALLOWED_CONTAINERS", ""))
DOCKER_EXTRA_PROTECTED = _parse_name_set(os.getenv("DOCKER_PROTECTED_CONTAINERS", ""))

# Danh sach bao ve CUNG - khong the bi ghi de qua .env hay API.
CORE_PROTECTED_EXACT = frozenset({
    "mosquitto",
    "server-collector",
    "server_collector",
    "smarthome-web",
    "smarthome_web",
})
CORE_PROTECTED_SUBSTRINGS = (
    "mosquitto",
    "server-collector",
    "server_collector",
    "smarthome-web",
    "smarthome_web",
)
ALLOWED_DOCKER_ACTIONS = frozenset({"start", "stop", "restart"})
CONTAINER_NAME_RE = re.compile(r"^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$")

AUTH_MAX_FAILS = 5
AUTH_LOCKOUT_SECONDS = 60.0


def _detect_self_container_id() -> str:
    """ID rut gon cua chinh container server-collector (de tu bao ve)."""
    hn = (socket.gethostname() or "").strip().lower()
    if re.fullmatch(r"[0-9a-f]{12,64}", hn):
        return hn[:12]
    return ""


SELF_CONTAINER_ID = _detect_self_container_id()


# Docker che (mask) /sys/devices/virtual/powercap bang tmpfs rong -> doc RAPL
# qua host rootfs (/rootfs/sys) khong bi che. Pin thi /sys thuong khong bi che,
# nhung dung chung rootfs cho chac.
def _chon_sys(sub: str) -> str:
    r = os.path.join(ROOTFS, "sys", sub)
    return r if os.path.isdir(r) else os.path.join("/sys", sub)


POWERCAP = _chon_sys("class/powercap")
BAT_DIR = _chon_sys("class/power_supply")


# Dich vu nen hwstats ghi dien nang/nhiet do CPU theo ngay (Cockpit cung doc day).
def _chon_hwstats() -> str:
    r = os.path.join(ROOTFS, "var/lib/hwstats")
    return r if os.path.isdir(r) else "/var/lib/hwstats"


HWSTATS = _chon_hwstats()

GIO_VN = timezone(timedelta(hours=7))
GB = 1024 ** 3
MB = 1024 ** 2


# --------------------------------------------------------------- tinh (info)

def _doc_mot(path: str) -> str | None:
    try:
        with open(path, "r", encoding="utf-8") as f:
            return f.read().strip()
    except OSError:
        return None


def _doc_os() -> str:
    for p in (os.path.join(ROOTFS, "etc/os-release"), "/etc/os-release"):
        try:
            with open(p, "r", encoding="utf-8") as f:
                for dong in f:
                    if dong.startswith("PRETTY_NAME="):
                        return dong.split("=", 1)[1].strip().strip('"')
        except OSError:
            continue
    return "Linux"


def _doc_hostname() -> str:
    for p in (os.path.join(ROOTFS, "etc/hostname"), "/etc/hostname"):
        ten = _doc_mot(p)
        if ten:
            return ten
    return socket.gethostname()


def _doc_cpu_model() -> str:
    for p in (os.path.join(ROOTFS, "proc/cpuinfo"), "/proc/cpuinfo"):
        try:
            with open(p, "r", encoding="utf-8") as f:
                for dong in f:
                    low = dong.lower()
                    if low.startswith("model name") or low.startswith("hardware"):
                        return dong.split(":", 1)[1].strip()
        except OSError:
            continue
    return platform.processor() or "CPU"


def dung_info() -> dict:
    uptime = int(time.time() - psutil.boot_time())
    disk = psutil.disk_usage(ROOTFS if os.path.isdir(ROOTFS) else "/")
    vm = psutil.virtual_memory()
    return {
        "hostname": _doc_hostname(),
        "os": _doc_os(),
        "kernel": platform.release(),
        "cpu_model": _doc_cpu_model(),
        "cpu_cores": psutil.cpu_count(logical=True) or 0,
        "ram_total_gb": round(vm.total / GB, 1),
        "disk_total_gb": round(disk.total / GB, 1),
        "uptime_days": uptime // 86400,
        "uptime_hours": (uptime % 86400) // 3600,
    }


# ------------------------------------------------------------------- nhiet do

def doc_nhiet() -> list:
    """Tat ca cam bien nhiet, nhan giong Cockpit. [] neu khong co."""
    fn = getattr(psutil, "sensors_temperatures", None)
    if fn is None:
        return []
    try:
        temps = fn()
    except Exception:
        return []
    ds = []
    for chip, ds_chip in temps.items():
        for i, t in enumerate(ds_chip):
            # Bo cam bien tat/khong dung (bao 0) - acpitz co vai cai nhu vay.
            if t.current is None or t.current <= 0:
                continue
            nhan = t.label.strip() if t.label else ""
            if not nhan:
                nhan = chip + " temp" + str(i + 1)
            ds.append({"label": nhan, "temp_c": round(float(t.current), 1)})
    return ds


def _nhiet_chinh(ds: list) -> float | None:
    """Nhiet do dai dien: uu tien Package, roi Core, roi cao nhat."""
    if not ds:
        return None
    for d in ds:
        if "package" in d["label"].lower():
            return d["temp_c"]
    for d in ds:
        if "core" in d["label"].lower():
            return d["temp_c"]
    return max(d["temp_c"] for d in ds)


# --------------------------------------------------------------- dien nang (RAPL)

_rapl_last: dict[str, tuple[int, float]] = {}


def _rapl_watt(mien: str) -> float | None:
    """Cong suat (W) cua mot mien RAPL. None khi chua doc duoc / lan dau."""
    base = os.path.join(POWERCAP, mien)
    e = _doc_mot(os.path.join(base, "energy_uj"))
    if e is None:
        return None
    try:
        e = int(e)
    except ValueError:
        return None
    now = time.monotonic()
    truoc = _rapl_last.get(mien)
    _rapl_last[mien] = (e, now)
    if truoc is None:
        return None
    e_truoc, t_truoc = truoc
    dt = now - t_truoc
    if dt <= 0:
        return None
    delta = e - e_truoc
    if delta < 0:  # tran bo dem
        mx = _doc_mot(os.path.join(base, "max_energy_range_uj"))
        try:
            delta += int(mx)
        except (TypeError, ValueError):
            return None
    return round(delta / 1e6 / dt, 2)


def doc_pin() -> dict:
    """Pin: dien ap (V), dong dien (A), cong suat (W), trang thai. null neu thieu."""
    bat = None
    for name in (os.listdir(BAT_DIR) if os.path.isdir(BAT_DIR) else []):
        d = os.path.join(BAT_DIR, name)
        if (_doc_mot(os.path.join(d, "type")) or "").lower() == "battery":
            bat = d
            break
    if bat is None:
        return {"bat_voltage_v": None, "bat_current_a": None,
                "bat_power_w": None, "bat_status": None}

    def num(f):
        v = _doc_mot(os.path.join(bat, f))
        try:
            return int(v)
        except (TypeError, ValueError):
            return None

    vu = num("voltage_now")
    iu = num("current_now")
    pu = num("power_now")
    v = round(vu / 1e6, 2) if vu is not None else None
    a = round(iu / 1e6, 3) if iu is not None else None
    if pu is not None:
        w = round(pu / 1e6, 2)
    elif v is not None and a is not None:
        w = round(v * a, 2)
    else:
        w = None
    return {
        "bat_voltage_v": v,
        "bat_current_a": a,
        "bat_power_w": w,
        "bat_status": _doc_mot(os.path.join(bat, "status")),
    }


def dung_diennang() -> dict:
    d = doc_pin()
    d.update({
        "cpu_package_w": _rapl_watt("intel-rapl:0"),
        "cpu_core_w": _rapl_watt("intel-rapl:0:0"),
        "cpu_uncore_w": _rapl_watt("intel-rapl:0:1"),
    })
    return d


# ----------------------------------------- dien nang theo ngay (tu hwstats)

def doc_hwstats() -> tuple[list, float | None, float | None]:
    """Doc /var/lib/hwstats do dich vu nen ghi (Cockpit cung doc day).

    - history.tsv: date<TAB>wh<TAB>tmin<TAB>tmax<TAB>samples  (cac ngay truoc)
    - state:       date last_energy_uj accumulated_uj tmin tmax samples  (hom nay)

    Tra (list ngay moi->cu, today_min, today_max). Chua cai hwstats -> ([], None, None).
    """
    days = []
    today_min = today_max = None

    try:
        with open(os.path.join(HWSTATS, "history.tsv"), "r", encoding="utf-8") as f:
            for line in f:
                p = line.rstrip("\n").split("\t")
                if len(p) >= 4:
                    try:
                        days.append({
                            "date": p[0],
                            "wh": round(float(p[1]), 2),
                            "temp_min": float(p[2]),
                            "temp_max": float(p[3]),
                        })
                    except ValueError:
                        continue
    except OSError:
        pass

    try:
        with open(os.path.join(HWSTATS, "state"), "r", encoding="utf-8") as f:
            s = f.read().split()
            if len(s) >= 6:
                acc = float(s[2])          # accumulated_uj
                today_min = float(s[3])
                today_max = float(s[4])
                days.append({
                    "date": s[0],
                    "wh": round(acc / 3.6e9, 2),  # uJ -> Wh
                    "temp_min": today_min,
                    "temp_max": today_max,
                })
    except (OSError, ValueError):
        pass

    days.sort(key=lambda x: x["date"], reverse=True)  # moi -> cu
    return days, today_min, today_max


# ------------------------------------------------------------ tuc thoi (metrics)

_net_truoc = None


def _mang() -> tuple[float, float]:
    global _net_truoc
    io = psutil.net_io_counters()
    now = time.monotonic()
    if _net_truoc is None:
        _net_truoc = (io.bytes_recv, io.bytes_sent, now)
        return 0.0, 0.0
    dt = now - _net_truoc[2]
    if dt <= 0:
        return 0.0, 0.0
    rx = (io.bytes_recv - _net_truoc[0]) * 8 / dt / 1e6
    tx = (io.bytes_sent - _net_truoc[1]) * 8 / dt / 1e6
    _net_truoc = (io.bytes_recv, io.bytes_sent, now)
    return round(max(rx, 0.0), 2), round(max(tx, 0.0), 2)


def dung_metrics(temps: list) -> dict:
    vm = psutil.virtual_memory()
    disk = psutil.disk_usage(ROOTFS if os.path.isdir(ROOTFS) else "/")
    try:
        load1, load5, load15 = psutil.getloadavg()
    except (OSError, AttributeError):
        load1 = load5 = load15 = 0.0
    rx, tx = _mang()
    return {
        "cpu_percent": round(psutil.cpu_percent(interval=1), 1),
        "ram_used_gb": round(vm.used / GB, 1),
        "ram_percent": round(vm.percent, 1),
        "disk_used_gb": round(disk.used / GB, 1),
        "disk_percent": round(disk.percent, 1),
        "cpu_temp_c": _nhiet_chinh(temps),
        "load1": round(load1, 2),
        "load5": round(load5, 2),
        "load15": round(load15, 2),
        "net_rx_mbps": rx,
        "net_tx_mbps": tx,
    }


# --------------------------------------------------------------- docker & policy

_docker_lock = threading.Lock()
_docker_cpu_last: dict[str, tuple[int, int]] = {}


def is_control_ready(
    enabled: bool | None = None,
    token: str | None = None,
) -> bool:
    en = DOCKER_CONTROL_ENABLED if enabled is None else enabled
    tk = DOCKER_CONTROL_TOKEN if token is None else token
    return bool(en and len(tk.strip()) >= MIN_CONTROL_TOKEN_LEN)


def la_container_bao_ve(
    name: str,
    container_id: str = "",
    extra_protected: frozenset[str] | None = None,
    self_id: str | None = None,
) -> bool:
    """Kiem tra container co nam trong danh sach bao ve cung hay khong."""
    low = (name or "").strip().lstrip("/").lower()
    if not low:
        return True
    if low in CORE_PROTECTED_EXACT:
        return True
    if any(sub in low for sub in CORE_PROTECTED_SUBSTRINGS):
        return True
    extras = DOCKER_EXTRA_PROTECTED if extra_protected is None else extra_protected
    if low in extras:
        return True
    sid = SELF_CONTAINER_ID if self_id is None else self_id
    cid = (container_id or "").strip().lower()
    if sid and cid and cid.startswith(sid):
        return True
    return False


def kiem_tra_quyen_dieu_khien(
    name: str,
    action: str = "stop",
    container_id: str = "",
    *,
    enabled: bool | None = None,
    token: str | None = None,
    allowed_containers: frozenset[str] | None = None,
    extra_protected: frozenset[str] | None = None,
    self_id: str | None = None,
) -> tuple[bool, int, str]:
    """Kiem tra chinh sach an toan truoc khi thuc thi lenh Docker.

    Tra ve (ok, http_status, ly_do_tieng_viet).
    """
    clean_name = (name or "").strip().lstrip("/")
    clean_action = (action or "").strip().lower()

    if not clean_name or not CONTAINER_NAME_RE.fullmatch(clean_name):
        return False, 400, "Tên container không hợp lệ."

    if clean_action not in ALLOWED_DOCKER_ACTIONS:
        return False, 400, "Hành động không hợp lệ (chỉ cho phép start, stop, restart)."

    if la_container_bao_ve(clean_name, container_id, extra_protected=extra_protected, self_id=self_id):
        return (
            False,
            403,
            f"Container lõi '{clean_name}' nằm trong danh sách bảo vệ cứng, không được phép điều khiển từ xa.",
        )

    if not is_control_ready(enabled=enabled, token=token):
        return (
            False,
            503,
            "Điều khiển Docker chưa được bật hoặc thiếu DOCKER_CONTROL_TOKEN (>= 16 ký tự).",
        )

    allowed = DOCKER_ALLOWED_CONTAINERS if allowed_containers is None else allowed_containers
    if clean_name.lower() not in allowed:
        return (
            False,
            403,
            f"Container '{clean_name}' chưa nằm trong danh sách được phép (DOCKER_ALLOWED_CONTAINERS).",
        )

    return True, 200, ""


def _docker_unix_request(method: str, path: str, timeout: float = 5.0) -> tuple[int, bytes]:
    if not os.path.exists(DOCKER_SOCK):
        raise FileNotFoundError(f"Khong thay Docker socket tai {DOCKER_SOCK}")
    conn = http.client.HTTPConnection("localhost", timeout=timeout)
    sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    sock.connect(DOCKER_SOCK)
    conn.sock = sock
    try:
        conn.request(method, path)
        resp = conn.getresponse()
        raw = resp.read()
        return resp.status, raw
    finally:
        conn.close()


def tinh_stats_container(
    cid: str,
    raw_stats: dict,
    prev_cpu_map: dict[str, tuple[int, int]],
) -> tuple[float | None, float | None, float | None, float | None]:
    """Tinh (cpu_percent, mem_mb, mem_limit_mb, mem_percent) tu Docker stats JSON.

    Ho tro ca che do `one-shot=true` (dung delta tu prev_cpu_map) lan `precpu_stats`.
    Cong thuc tinh RAM khop `docker stats` (tru cache inactive_file / cache).
    """
    cpu_pct: float | None = None
    cpu_stats = raw_stats.get("cpu_stats") or {}
    cpu_usage = cpu_stats.get("cpu_usage") or {}
    cur_total = cpu_usage.get("total_usage")
    cur_sys = cpu_stats.get("system_cpu_usage")

    if isinstance(cur_total, (int, float)) and isinstance(cur_sys, (int, float)):
        cur_total_i = int(cur_total)
        cur_sys_i = int(cur_sys)
        prev = prev_cpu_map.get(cid)
        if prev is None:
            precpu = raw_stats.get("precpu_stats") or {}
            pre_total = (precpu.get("cpu_usage") or {}).get("total_usage")
            pre_sys = precpu.get("system_cpu_usage")
            if isinstance(pre_total, (int, float)) and isinstance(pre_sys, (int, float)) and pre_sys > 0:
                prev = (int(pre_total), int(pre_sys))
        prev_cpu_map[cid] = (cur_total_i, cur_sys_i)

        if prev is not None:
            cpu_delta = cur_total_i - prev[0]
            sys_delta = cur_sys_i - prev[1]
            online_cpus = cpu_stats.get("online_cpus")
            if not isinstance(online_cpus, int) or online_cpus <= 0:
                percpu = cpu_usage.get("percpu_usage")
                online_cpus = len(percpu) if isinstance(percpu, list) and percpu else (os.cpu_count() or 1)
            if sys_delta > 0 and cpu_delta >= 0:
                cpu_pct = round((cpu_delta / sys_delta) * online_cpus * 100.0, 1)

    mem_mb: float | None = None
    mem_limit_mb: float | None = None
    mem_pct: float | None = None
    mem_stats = raw_stats.get("memory_stats") or {}
    usage = mem_stats.get("usage")
    limit = mem_stats.get("limit")
    if isinstance(usage, (int, float)) and usage >= 0:
        st = mem_stats.get("stats") or {}
        cache = st.get("inactive_file")
        if not isinstance(cache, (int, float)):
            cache = st.get("total_inactive_file")
        if not isinstance(cache, (int, float)):
            cache = st.get("cache", 0)
        used_bytes = max(0.0, float(usage) - float(cache or 0))
        mem_mb = round(used_bytes / MB, 1)
        if isinstance(limit, (int, float)) and limit > 0:
            mem_limit_mb = round(float(limit) / MB, 1)
            mem_pct = round((used_bytes / float(limit)) * 100.0, 1)

    return cpu_pct, mem_mb, mem_limit_mb, mem_pct


def _doc_stats_container(cid: str) -> dict | None:
    try:
        status, raw = _docker_unix_request(
            "GET",
            f"/v1.41/containers/{quote(cid, safe='')}/stats?stream=false&one-shot=true",
            timeout=3.0,
        )
        if status != 200:
            return None
        return json.loads(raw.decode("utf-8", "replace"))
    except Exception as e:
        logger.debug("Khong doc duoc stats container %s: %s", cid[:12], e)
        return None


def _liet_ke_docker_tho() -> list[dict]:
    if not os.path.exists(DOCKER_SOCK):
        return []
    status, raw = _docker_unix_request("GET", "/v1.41/containers/json?all=1", timeout=5.0)
    if status != 200:
        logger.warning("Docker API tra %s", status)
        return []
    data = json.loads(raw.decode("utf-8", "replace"))
    return data if isinstance(data, list) else []


def dung_docker(include_stats: bool = True) -> list[dict]:
    try:
        data = _liet_ke_docker_tho()
    except Exception as e:
        logger.warning("Khong doc duoc Docker: %s", e)
        return []

    control_ready = is_control_ready()
    running_ids: set[str] = set()
    ds: list[dict] = []

    with _docker_lock:
        for c in data:
            cid = str(c.get("Id", ""))
            names = c.get("Names") or []
            ten = (names[0] if names else cid[:12]).lstrip("/")
            state = str(c.get("State", ""))
            is_running = state.lower() == "running"
            protected = la_container_bao_ve(ten, cid)
            controllable = bool(
                control_ready and (not protected) and (ten.lower() in DOCKER_ALLOWED_CONTAINERS)
            )

            cpu_pct = None
            mem_mb = None
            mem_limit_mb = None
            mem_pct = None

            if is_running and cid:
                running_ids.add(cid)
                if include_stats:
                    raw_stats = _doc_stats_container(cid)
                    if raw_stats is not None:
                        cpu_pct, mem_mb, mem_limit_mb, mem_pct = tinh_stats_container(
                            cid, raw_stats, _docker_cpu_last
                        )

            ds.append({
                "name": ten,
                "image": c.get("Image", ""),
                "state": state,
                "status": c.get("Status", ""),
                "cpu_percent": cpu_pct,
                "mem_mb": mem_mb,
                "mem_limit_mb": mem_limit_mb,
                "mem_percent": mem_pct,
                "protected": protected,
                "controllable": controllable,
            })

        # Xoa cache CPU cua container da dung/xoa de lan bat lai tinh dung delta.
        for stale in list(_docker_cpu_last.keys()):
            if stale not in running_ids:
                _docker_cpu_last.pop(stale, None)

    ds.sort(key=lambda x: (x["state"] != "running", x["name"]))
    return ds


def thuc_thi_lenh_docker(name: str, action: str) -> tuple[bool, int, str, list[dict] | None]:
    """Thuc thi lenh start / stop / restart an toan qua Docker Unix socket."""
    clean_name = (name or "").strip().lstrip("/")
    clean_action = (action or "").strip().lower()

    ok, code, err = kiem_tra_quyen_dieu_khien(clean_name, clean_action)
    if not ok:
        logger.warning("DOCKER_CONTROL_DENIED action=%s container=%s reason=%s", clean_action, clean_name, err)
        return False, code, err, None

    try:
        raw_list = _liet_ke_docker_tho()
    except Exception as e:
        return False, 502, f"Không đọc được Docker daemon: {e}", None

    target = None
    for c in raw_list:
        names = [n.lstrip("/") for n in (c.get("Names") or [])]
        if clean_name in names or clean_name.lower() in [n.lower() for n in names]:
            target = c
            break

    if target is None:
        return False, 404, f"Không tìm thấy container '{clean_name}' trên server.", None

    cid = str(target.get("Id", ""))
    actual_name = ((target.get("Names") or [clean_name])[0]).lstrip("/")

    # Kiem tra lai voi container ID that (chan tu tat chinh minh).
    ok2, code2, err2 = kiem_tra_quyen_dieu_khien(actual_name, clean_action, container_id=cid)
    if not ok2:
        logger.warning("DOCKER_CONTROL_DENIED action=%s container=%s id=%s", clean_action, actual_name, cid[:12])
        return False, code2, err2, None

    endpoint_map = {
        "start": (f"/v1.41/containers/{quote(cid, safe='')}/start", 15.0),
        "stop": (f"/v1.41/containers/{quote(cid, safe='')}/stop?t=10", 25.0),
        "restart": (f"/v1.41/containers/{quote(cid, safe='')}/restart?t=10", 30.0),
    }
    ep, timeout = endpoint_map[clean_action]

    try:
        status, body = _docker_unix_request("POST", ep, timeout=timeout)
    except Exception as e:
        logger.error("DOCKER_CONTROL_ERROR action=%s container=%s err=%s", clean_action, actual_name, e)
        return False, 502, f"Lỗi gọi Docker Engine: {e}", None

    if status in (200, 204, 304):
        labels = {
            "start": ("đã bật" if status != 304 else "đang chạy sẵn"),
            "stop": ("đã dừng" if status != 304 else "đã dừng sẵn"),
            "restart": "đã khởi động lại",
        }
        msg = f"Container '{actual_name}' {labels[clean_action]}."
        logger.info("DOCKER_CONTROL_OK action=%s container=%s status=%s", clean_action, actual_name, status)
        containers = dung_docker(include_stats=True)
        return True, 200, msg, containers

    detail = body.decode("utf-8", "replace").strip()
    try:
        detail_json = json.loads(detail)
        if isinstance(detail_json, dict) and detail_json.get("message"):
            detail = str(detail_json["message"])
    except Exception:
        pass
    logger.warning("DOCKER_CONTROL_FAIL action=%s container=%s status=%s detail=%s", clean_action, actual_name, status, detail)
    return False, 500, f"Docker trả lỗi ({status}): {detail or 'Không rõ nguyên nhân'}", None


# ----------------------------------------- HTTP Control Server (NO MQTT write)

class _AuthLimiter:
    def __init__(self) -> None:
        self._lock = threading.Lock()
        self._fails: dict[str, tuple[int, float]] = {}

    def is_locked(self, key: str, now: float | None = None) -> int:
        t = time.monotonic() if now is None else now
        with self._lock:
            rec = self._fails.get(key)
            if rec and rec[1] > t:
                return max(1, int(rec[1] - t) + 1)
        return 0

    def record_fail(self, key: str, now: float | None = None) -> int:
        t = time.monotonic() if now is None else now
        with self._lock:
            count, _ = self._fails.get(key, (0, 0.0))
            count += 1
            if count >= AUTH_MAX_FAILS:
                self._fails[key] = (0, t + AUTH_LOCKOUT_SECONDS)
                return int(AUTH_LOCKOUT_SECONDS)
            self._fails[key] = (count, 0.0)
            return 0

    def clear(self, key: str) -> None:
        with self._lock:
            self._fails.pop(key, None)


def tao_docker_control_server(
    bind: str,
    port: int,
    on_containers_updated=None,
) -> ThreadingHTTPServer:
    limiter = _AuthLimiter()

    class Handler(BaseHTTPRequestHandler):
        def log_message(self, fmt, *args):
            logger.debug("control-http %s - %s", self.address_string(), fmt % args)

        def _send_json(self, status: int, payload: dict) -> None:
            raw = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)

        def _check_bearer(self) -> bool:
            client_ip = self.client_address[0] if self.client_address else "unknown"
            wait_sec = limiter.is_locked(client_ip)
            if wait_sec > 0:
                self._send_json(429, {"ok": False, "error": f"Sai token quá nhiều lần. Thử lại sau {wait_sec}s."})
                return False

            if not is_control_ready():
                self._send_json(503, {"ok": False, "error": "Điều khiển Docker đang tắt hoặc chưa cấu hình token."})
                return False

            auth = (self.headers.get("Authorization") or "").strip()
            expected = f"Bearer {DOCKER_CONTROL_TOKEN}"
            if not hmac.compare_digest(auth.encode("utf-8"), expected.encode("utf-8")):
                locked = limiter.record_fail(client_ip)
                msg = (
                    f"Sai token quá nhiều lần. Khóa tạm {locked}s."
                    if locked > 0
                    else "Thiếu hoặc sai DOCKER_CONTROL_TOKEN."
                )
                self._send_json(429 if locked > 0 else 401, {"ok": False, "error": msg})
                return False

            limiter.clear(client_ip)
            return True

        def do_GET(self):
            if not self._check_bearer():
                return
            if self.path == "/api/docker/status":
                self._send_json(200, {
                    "ok": True,
                    "enabled": is_control_ready(),
                    "protected_containers": sorted(CORE_PROTECTED_EXACT | DOCKER_EXTRA_PROTECTED),
                    "allowed_containers": sorted(
                        n for n in DOCKER_ALLOWED_CONTAINERS if not la_container_bao_ve(n)
                    ),
                })
                return
            if self.path == "/api/docker/containers":
                containers = dung_docker(include_stats=True)
                if on_containers_updated:
                    try:
                        on_containers_updated(containers)
                    except Exception:
                        pass
                self._send_json(200, {"ok": True, "containers": containers})
                return
            self._send_json(404, {"ok": False, "error": "Không tìm thấy endpoint."})

        def do_POST(self):
            if not self._check_bearer():
                return
            if self.path != "/api/docker/action":
                self._send_json(404, {"ok": False, "error": "Không tìm thấy endpoint."})
                return

            try:
                length = int(self.headers.get("Content-Length") or "0")
                if length <= 0 or length > 8192:
                    self._send_json(400, {"ok": False, "error": "Payload không hợp lệ."})
                    return
                body = json.loads(self.rfile.read(length).decode("utf-8", "replace"))
                if not isinstance(body, dict):
                    self._send_json(400, {"ok": False, "error": "Payload phải là JSON object."})
                    return
            except Exception:
                self._send_json(400, {"ok": False, "error": "JSON không hợp lệ."})
                return

            name = str(body.get("name") or "")
            action = str(body.get("action") or "")
            ok, status, msg, containers = thuc_thi_lenh_docker(name, action)
            if ok and containers is not None and on_containers_updated:
                try:
                    on_containers_updated(containers)
                except Exception:
                    pass
            resp = {"ok": ok, "message": msg}
            if not ok:
                resp["error"] = msg
            if containers is not None:
                resp["containers"] = containers
            self._send_json(status, resp)

    return ThreadingHTTPServer((bind, port), Handler)


# ------------------------------------------------------------- MQTT

def on_connect(client, userdata, flags, rc, props=None):
    if rc == 0:
        logger.info("Da ket noi Mosquitto tai %s:%s", MQTT_BROKER, MQTT_PORT)
    else:
        logger.error("Khong ket noi duoc Mosquitto, ma %s", rc)


def _pub(cl, topic, obj):
    cl.publish(topic, json.dumps(obj, ensure_ascii=False), retain=True, qos=1)


def main() -> int:
    if psutil is None:
        logger.error("Thieu psutil. pip install psutil")
        return 1
    if mqtt is None:
        logger.error("Thieu paho-mqtt. pip install paho-mqtt")
        return 1

    logger.info("Khoi dong Server Collector")
    logger.info(
        "Mosquitto: %s:%s | rootfs=%s | docker=%s | hwstats=%s | control=%s",
        MQTT_BROKER, MQTT_PORT, ROOTFS, DOCKER_SOCK, HWSTATS,
        f"ON ({DOCKER_CONTROL_BIND}:{DOCKER_CONTROL_PORT}, allowed={len(DOCKER_ALLOWED_CONTAINERS)})"
        if is_control_ready() else "OFF",
    )

    try:
        cl = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2,
                         client_id=MQTT_CLIENT_ID, clean_session=True)
    except (AttributeError, TypeError):
        cl = mqtt.Client(client_id=MQTT_CLIENT_ID, clean_session=True)
    cl.on_connect = on_connect
    if MQTT_USER:
        cl.username_pw_set(MQTT_USER, MQTT_PASS)
    try:
        cl.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
    except Exception as e:
        logger.error("Khong noi duoc broker: %s", e)
        return 1
    cl.loop_start()

    control_srv = None
    if is_control_ready():
        try:
            control_srv = tao_docker_control_server(
                DOCKER_CONTROL_BIND,
                DOCKER_CONTROL_PORT,
                on_containers_updated=lambda ds: _pub(cl, "server/docker", ds),
            )
            t = threading.Thread(target=control_srv.serve_forever, name="docker-control-http", daemon=True)
            t.start()
            logger.info(
                "Docker Control HTTP API dang lang nghe tai http://%s:%s (Bearer Auth, KHONG qua MQTT)",
                DOCKER_CONTROL_BIND,
                DOCKER_CONTROL_PORT,
            )
        except Exception as e:
            logger.error("Khong mo duoc Docker Control HTTP API: %s", e)
    elif DOCKER_CONTROL_ENABLED:
        logger.warning(
            "DOCKER_CONTROL_ENABLED=true nhung DOCKER_CONTROL_TOKEN chua du %d ky tu -> TAT dieu khien Docker.",
            MIN_CONTROL_TOKEN_LEN,
        )

    lan = 0
    try:
        while True:
            now = datetime.now(GIO_VN).strftime("%Y-%m-%d %H:%M:%S")
            try:
                temps = doc_nhiet()
                days, tmin, tmax = doc_hwstats()
                # Cao/thap hom nay lay tu hwstats (ca ngay) de khop Cockpit; khong co
                # thi suy tu cam bien hien tai.
                if tmin is None and temps:
                    tmin = min(t["temp_c"] for t in temps)
                    tmax = max(t["temp_c"] for t in temps)

                if lan % 12 == 0:  # ~moi 60s: info
                    _pub(cl, "server/info", dung_info())
                if lan % DOCKER_POLL_EVERY == 0:  # ~moi 15s: docker + CPU%/RAM tung container
                    _pub(cl, "server/docker", dung_docker(include_stats=True))
                _pub(cl, "server/metrics", dung_metrics(temps))
                _pub(cl, "server/nhietdo",
                     {"sensors": temps, "today_min": tmin, "today_max": tmax})
                _pub(cl, "server/diennang", dung_diennang())
                _pub(cl, "server/diennang_ngay", days)
                cl.publish("server/capnhat", now, retain=True, qos=1)

                hom_nay = days[0]["wh"] if days else None
                logger.info("OK: cambien=%d | hom nay=%s Wh | so ngay=%d",
                            len(temps), hom_nay, len(days))
            except Exception as e:
                logger.error("Loi doc so lieu: %s", e)
            lan += 1
            time.sleep(max(POLL_INTERVAL - 1, 0))  # dung_metrics da ngu 1s
    finally:
        if control_srv is not None:
            control_srv.shutdown()
            control_srv.server_close()


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("dung", file=sys.stderr)
        sys.exit(0)
