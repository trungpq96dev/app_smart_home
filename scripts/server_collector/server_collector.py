#!/usr/bin/env python3
"""Doc suc khoe serverubuntu -> publish MQTT.

Chay trong Docker tren chinh serverubuntu. Dung psutil doc CPU/RAM/o dia/nhiet
do/mang cua HOST (xem docker-compose.yml: pid=host, network=host, mount /sys va
host root). Liet ke container qua Docker socket (mount read-only).

Hop dong topic (app chi doc, khong ghi):
  - server/info     JSON he thong (hostname, os, kernel, cpu, ram, disk, uptime)
  - server/metrics  JSON tuc thoi (cpu%, ram, disk, nhiet do chinh, load, mang)
  - server/docker   JSON list container
  - server/nhietdo  JSON tat ca cam bien nhiet + cao/thap hom nay
  - server/diennang JSON pin + RAPL CPU (ca goi / nhan / phu tro)
  - server/diennang_ngay  JSON dien nang CPU (Wh) + khoang nhiet do theo ngay
  - server/capnhat  YYYY-MM-DD HH:MM:SS

KHONG BIA SO: truong nao host khong cap (vi du RAPL khong doc duoc, may khong co
pin) publish null; bieu do theo ngay tu tich luy, bat dau trong va day dan.
"""

from __future__ import annotations

import json
import logging
import os
import platform
import socket
import sys
import time
import http.client
from datetime import datetime, timedelta, timezone

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
DAYS_KEEP = int(os.getenv("DAYS_KEEP", "14"))

ROOTFS = os.getenv("ROOTFS", "/rootfs")
DOCKER_SOCK = os.getenv("DOCKER_SOCK", "/var/run/docker.sock")
STATE_DIR = os.getenv("STATE_DIR", "/data")
STATE_FILE = os.path.join(STATE_DIR, "state.json")

# Docker che (mask) /sys/devices/virtual/powercap bang tmpfs rong -> doc RAPL
# qua host rootfs (/rootfs/sys) khong bi che. Pin thi /sys thuong khong bi che,
# nhung dung chung rootfs cho chac.
def _chon_sys(sub: str) -> str:
    r = os.path.join(ROOTFS, "sys", sub)
    return r if os.path.isdir(r) else os.path.join("/sys", sub)


POWERCAP = _chon_sys("class/powercap")
BAT_DIR = _chon_sys("class/power_supply")

GIO_VN = timezone(timedelta(hours=7))
GB = 1024 ** 3


def _hom_nay() -> str:
    return datetime.now(GIO_VN).strftime("%Y-%m-%d")


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


# --------------------------------------------------------------- dien nang

# energy_uj cua moi mien RAPL: (gia tri uj lan truoc, mono_time lan truoc)
_rapl_last: dict[str, tuple[int, float]] = {}


def _rapl_path(mien: str) -> str:
    # package = intel-rapl:0 ; core = intel-rapl:0:0 ; uncore = intel-rapl:0:1
    return os.path.join(POWERCAP, mien)


def _rapl_watt(mien: str) -> tuple[float | None, int]:
    """Cong suat (W) cua mot mien RAPL + delta nang luong (uj).

    Tra (None, 0) khi chua doc duoc hoac lan dau (chua co moc truoc).
    """
    base = _rapl_path(mien)
    e = _doc_mot(os.path.join(base, "energy_uj"))
    if e is None:
        return None, 0
    try:
        e = int(e)
    except ValueError:
        return None, 0
    now = time.monotonic()
    truoc = _rapl_last.get(mien)
    _rapl_last[mien] = (e, now)
    if truoc is None:
        return None, 0
    e_truoc, t_truoc = truoc
    dt = now - t_truoc
    if dt <= 0:
        return None, 0
    delta = e - e_truoc
    if delta < 0:  # tran bo dem
        mx = _doc_mot(os.path.join(base, "max_energy_range_uj"))
        try:
            delta += int(mx)
        except (TypeError, ValueError):
            return None, 0
    watt = delta / 1e6 / dt
    return round(watt, 2), delta


def doc_pin() -> dict:
    """Pin: dien ap (V), dong dien (A), cong suat (W), trang thai. null neu thieu."""
    bat = None
    for name in os.listdir(BAT_DIR) if os.path.isdir(BAT_DIR) else []:
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

    vu = num("voltage_now")   # uV
    iu = num("current_now")   # uA
    pu = num("power_now")     # uW (mot so may co san)
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


def dung_diennang() -> tuple[dict, int]:
    """JSON dien nang + delta nang luong package (uj) de tich luy Wh."""
    pkg_w, pkg_delta = _rapl_watt("intel-rapl:0")
    core_w, _ = _rapl_watt("intel-rapl:0:0")
    uncore_w, _ = _rapl_watt("intel-rapl:0:1")
    d = doc_pin()
    d.update({
        "cpu_package_w": pkg_w,
        "cpu_core_w": core_w,
        "cpu_uncore_w": uncore_w,
    })
    return d, pkg_delta


# ----------------------------------------------------------- state theo ngay

def _nap_state() -> dict:
    try:
        with open(STATE_FILE, "r", encoding="utf-8") as f:
            s = json.load(f)
            if isinstance(s, dict) and "days" in s:
                return s
    except (OSError, ValueError):
        pass
    return {"days": {}}


def _luu_state(s: dict) -> None:
    try:
        os.makedirs(STATE_DIR, exist_ok=True)
        tmp = STATE_FILE + ".tmp"
        with open(tmp, "w", encoding="utf-8") as f:
            json.dump(s, f, ensure_ascii=False)
        os.replace(tmp, STATE_FILE)
    except OSError as e:
        logger.warning("Khong luu duoc state: %s", e)


def cap_nhat_ngay(state: dict, pkg_delta_uj: int, temps: list) -> dict:
    """Cong don Wh package + nhiet do cao/thap vao ngay hom nay. Tra state."""
    today = _hom_nay()
    days = state.setdefault("days", {})
    d = days.setdefault(today, {"wh": 0.0, "temp_min": None, "temp_max": None})

    if pkg_delta_uj > 0:
        d["wh"] = round(d["wh"] + pkg_delta_uj / 1e6 / 3600.0, 4)

    if temps:
        cur_min = min(t["temp_c"] for t in temps)
        cur_max = max(t["temp_c"] for t in temps)
        d["temp_min"] = cur_min if d["temp_min"] is None else min(d["temp_min"], cur_min)
        d["temp_max"] = cur_max if d["temp_max"] is None else max(d["temp_max"], cur_max)

    # Giu DAYS_KEEP ngay gan nhat.
    if len(days) > DAYS_KEEP:
        for k in sorted(days.keys())[:-DAYS_KEEP]:
            days.pop(k, None)
    return state


def ngay_ra_json(state: dict) -> list:
    days = state.get("days", {})
    out = []
    for date in sorted(days.keys(), reverse=True):
        d = days[date]
        out.append({
            "date": date,
            "wh": round(d.get("wh", 0.0), 2),
            "temp_min": d.get("temp_min"),
            "temp_max": d.get("temp_max"),
        })
    return out


def nhietdo_ra_json(state: dict, temps: list) -> dict:
    today = state.get("days", {}).get(_hom_nay(), {})
    return {
        "sensors": temps,
        "today_min": today.get("temp_min"),
        "today_max": today.get("temp_max"),
    }


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


# --------------------------------------------------------------- docker

def dung_docker() -> list:
    if not os.path.exists(DOCKER_SOCK):
        return []
    try:
        conn = http.client.HTTPConnection("localhost", timeout=5)
        conn.sock = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
        conn.sock.settimeout(5)
        conn.sock.connect(DOCKER_SOCK)
        conn.request("GET", "/v1.41/containers/json?all=1")
        resp = conn.getresponse()
        raw = resp.read()
        conn.close()
        if resp.status != 200:
            logger.warning("Docker API tra %s", resp.status)
            return []
        data = json.loads(raw.decode("utf-8", "replace"))
    except Exception as e:
        logger.warning("Khong doc duoc Docker: %s", e)
        return []
    ds = []
    for c in data:
        names = c.get("Names") or []
        ten = (names[0] if names else c.get("Id", "")[:12]).lstrip("/")
        ds.append({
            "name": ten,
            "image": c.get("Image", ""),
            "state": c.get("State", ""),
            "status": c.get("Status", ""),
        })
    ds.sort(key=lambda x: (x["state"] != "running", x["name"]))
    return ds


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
    logger.info("Mosquitto: %s:%s | rootfs=%s | docker=%s | state=%s",
                MQTT_BROKER, MQTT_PORT, ROOTFS, DOCKER_SOCK, STATE_FILE)

    state = _nap_state()

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

    lan = 0
    while True:
        now = datetime.now(GIO_VN).strftime("%Y-%m-%d %H:%M:%S")
        try:
            temps = doc_nhiet()
            dn, pkg_delta = dung_diennang()
            state = cap_nhat_ngay(state, pkg_delta, temps)
            _luu_state(state)

            if lan % 12 == 0:  # ~moi 60s (POLL=5): info + docker + lich su ngay
                _pub(cl, "server/info", dung_info())
                _pub(cl, "server/docker", dung_docker())
            _pub(cl, "server/metrics", dung_metrics(temps))
            _pub(cl, "server/nhietdo", nhietdo_ra_json(state, temps))
            _pub(cl, "server/diennang", dn)
            _pub(cl, "server/diennang_ngay", ngay_ra_json(state))
            cl.publish("server/capnhat", now, retain=True, qos=1)

            logger.info("OK: pkgW=%s coreW=%s pin=%sV cambien=%d",
                        dn.get("cpu_package_w"), dn.get("cpu_core_w"),
                        dn.get("bat_voltage_v"), len(temps))
        except Exception as e:
            logger.error("Loi doc so lieu: %s", e)
        lan += 1
        time.sleep(max(POLL_INTERVAL - 1, 0))  # dung_metrics da ngu 1s


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("dung", file=sys.stderr)
        sys.exit(0)
