#!/usr/bin/env python3
"""Doc suc khoe serverubuntu -> publish MQTT.

Chay trong Docker tren chinh serverubuntu. Dung psutil doc CPU/RAM/o dia/nhiet
do/mang cua HOST (xem docker-compose.yml: pid=host, network=host, mount /sys va
host root). Liet ke container qua Docker socket (mount read-only).

DIEN NANG CPU THEO NGAY lay tu dich vu nen hwstats cua server
(/var/lib/hwstats/history.tsv + state) - DUNG nguon ma trang Cockpit dung, nen
so lieu khop het voi Cockpit. Khong tu tich luy rieng nua.

Hop dong topic (app chi doc, khong ghi):
  - server/info     JSON he thong (hostname, os, kernel, cpu, ram, disk, uptime)
  - server/metrics  JSON tuc thoi (cpu%, ram, disk, nhiet do chinh, load, mang)
  - server/docker   JSON list container
  - server/nhietdo  JSON tat ca cam bien nhiet + cao/thap hom nay (tu hwstats)
  - server/diennang JSON pin + RAPL CPU (ca goi / nhan / phu tro) - tuc thoi
  - server/diennang_ngay  JSON dien nang CPU (Wh) + khoang nhiet do theo ngay (hwstats)
  - server/capnhat  YYYY-MM-DD HH:MM:SS

KHONG BIA SO: truong nao host khong cap (vi du RAPL khong doc duoc, chua cai
hwstats) publish null / mang rong.
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

ROOTFS = os.getenv("ROOTFS", "/rootfs")
DOCKER_SOCK = os.getenv("DOCKER_SOCK", "/var/run/docker.sock")


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
    logger.info("Mosquitto: %s:%s | rootfs=%s | docker=%s | hwstats=%s",
                MQTT_BROKER, MQTT_PORT, ROOTFS, DOCKER_SOCK, HWSTATS)

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
            days, tmin, tmax = doc_hwstats()
            # Cao/thap hom nay lay tu hwstats (ca ngay) de khop Cockpit; khong co
            # thi suy tu cam bien hien tai.
            if tmin is None and temps:
                tmin = min(t["temp_c"] for t in temps)
                tmax = max(t["temp_c"] for t in temps)

            if lan % 12 == 0:  # ~moi 60s: info + docker
                _pub(cl, "server/info", dung_info())
                _pub(cl, "server/docker", dung_docker())
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


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("dung", file=sys.stderr)
        sys.exit(0)
