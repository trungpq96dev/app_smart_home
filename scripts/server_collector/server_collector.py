#!/usr/bin/env python3
"""Doc so suc khoe cua serverubuntu -> publish MQTT.

Chay trong Docker tren chinh serverubuntu. Dung psutil doc CPU/RAM/o dia/
nhiet do/mang cua HOST (xem docker-compose.yml: pid=host, network=host, mount
/proc /sys va host root). Liet ke container qua Docker socket (mount read-only).

Hop dong topic (app chi doc, khong ghi):
  - server/info     JSON: hostname, os, kernel, cpu, ram tong, disk tong, uptime
  - server/metrics  JSON: cpu%, ram, disk, nhiet do, load, mang (tuc thoi)
  - server/docker   JSON list container: name, image, state, status
  - server/capnhat  YYYY-MM-DD HH:MM:SS

KHONG BIA SO: truong nao host khong cap (vi du nhiet do khi may khong co cam
bien) publish null, app hien "—".
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

# Host root mount (xem compose). Lay OS + dung luong o dia cua HOST, khong phai
# cua container python:slim.
ROOTFS = os.getenv("ROOTFS", "/rootfs")
DOCKER_SOCK = os.getenv("DOCKER_SOCK", "/var/run/docker.sock")

GIO_VN = timezone(timedelta(hours=7))
GB = 1024 ** 3


# --------------------------------------------------------------- tinh (info)

def _doc_os() -> str:
    """PRETTY_NAME trong os-release cua host (uu tien) hoac container."""
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
        try:
            with open(p, "r", encoding="utf-8") as f:
                ten = f.read().strip()
                if ten:
                    return ten
        except OSError:
            continue
    return socket.gethostname()


def _doc_cpu_model() -> str:
    for p in (os.path.join(ROOTFS, "proc/cpuinfo"), "/proc/cpuinfo"):
        try:
            with open(p, "r", encoding="utf-8") as f:
                for dong in f:
                    if dong.lower().startswith("model name"):
                        return dong.split(":", 1)[1].strip()
                    # ARM (Raspberry Pi) khong co "model name"
                    if dong.lower().startswith("hardware"):
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


# ------------------------------------------------------------ tuc thoi (metrics)

def _nhiet_do() -> float | None:
    """Nhiet do CPU neu co cam bien; None neu may khong bao."""
    fn = getattr(psutil, "sensors_temperatures", None)
    if fn is None:
        return None
    try:
        temps = fn()
    except Exception:
        return None
    if not temps:
        return None
    # Uu tien cac chip CPU pho bien.
    for ten in ("coretemp", "k10temp", "cpu_thermal", "zenpower", "acpitz"):
        if ten in temps and temps[ten]:
            for t in temps[ten]:
                if t.current:
                    return round(float(t.current), 1)
    # Khong khop ten quen -> lay cam bien dau tien co so.
    for ds in temps.values():
        for t in ds:
            if t.current:
                return round(float(t.current), 1)
    return None


_net_truoc = None  # (bytes_recv, bytes_sent, mono_time)


def _mang() -> tuple[float, float]:
    """Toc do mang (Mb/s) tinh tu delta giua 2 lan doc. Lan dau tra 0."""
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


def dung_metrics() -> dict:
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
        "cpu_temp_c": _nhiet_do(),  # None -> JSON null
        "load1": round(load1, 2),
        "load5": round(load5, 2),
        "load15": round(load15, 2),
        "net_rx_mbps": rx,
        "net_tx_mbps": tx,
    }


# --------------------------------------------------------------- docker

def dung_docker() -> list:
    """Liet ke container qua Docker Engine API tren unix socket (read-only).

    Khong co socket / khong doc duoc -> tra list rong (app hien thong bao, app
    van chay). Khong dung thu vien ngoai, chi http.client.
    """
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
            "state": c.get("State", ""),      # running | exited | ...
            "status": c.get("Status", ""),    # "Up 3 days" ...
        })
    ds.sort(key=lambda x: (x["state"] != "running", x["name"]))
    return ds


# ------------------------------------------------------------- MQTT

def on_connect(client, userdata, flags, rc, props=None):
    if rc == 0:
        logger.info("Da ket noi Mosquitto tai %s:%s", MQTT_BROKER, MQTT_PORT)
    else:
        logger.error("Khong ket noi duoc Mosquitto, ma %s", rc)


def main() -> int:
    if psutil is None:
        logger.error("Thieu psutil. pip install psutil")
        return 1
    if mqtt is None:
        logger.error("Thieu paho-mqtt. pip install paho-mqtt")
        return 1

    logger.info("Khoi dong Server Collector")
    logger.info("Mosquitto: %s:%s | rootfs=%s | docker=%s",
                MQTT_BROKER, MQTT_PORT, ROOTFS, DOCKER_SOCK)

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

    # Info it doi -> gui mot lan dau, roi lap lai thua thot.
    lan = 0
    while True:
        now = datetime.now(GIO_VN).strftime("%Y-%m-%d %H:%M:%S")
        try:
            if lan % 12 == 0:  # ~moi 60s (POLL=5) cap nhat info + docker
                cl.publish("server/info", json.dumps(dung_info(), ensure_ascii=False),
                           retain=True, qos=1)
                cl.publish("server/docker", json.dumps(dung_docker(), ensure_ascii=False),
                           retain=True, qos=1)
            m = dung_metrics()
            cl.publish("server/metrics", json.dumps(m, ensure_ascii=False),
                       retain=True, qos=1)
            cl.publish("server/capnhat", now, retain=True, qos=1)
            logger.info("OK: CPU=%.0f%% RAM=%.0f%% Disk=%.0f%% Temp=%s",
                        m["cpu_percent"], m["ram_percent"], m["disk_percent"],
                        m["cpu_temp_c"])
        except Exception as e:
            logger.error("Loi doc so lieu: %s", e)
        lan += 1
        # dung_metrics da ngu 1s (cpu_percent interval=1); bu phan con lai.
        time.sleep(max(POLL_INTERVAL - 1, 0))


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("dung", file=sys.stderr)
        sys.exit(0)
