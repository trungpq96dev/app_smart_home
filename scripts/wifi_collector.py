#!/usr/bin/env python3
"""Doc thong tin WiFi tu router TP-Link TL-WR840N -> publish MQTT.

Viet lai ngay 19/11/2026 cho firmware v6 (Build 180906) dung giao thuc
"cgi" (POST /cgi?<flags> + cookie Basic), KHAC voi ban cu viet cho firmware
doi dau (userRpm/*.htm). Da khao sat truc tiep tren router that.

Hop dong topic (giu nguyen de app khong phai sua):
  - wifi/info     JSON thong tin mang
  - wifi/devices  JSON danh sach thiet bi DHCP
  - wifi/trangthai  "ok" | "loi"
  - wifi/capnhat    YYYY-MM-DD HH:MM:SS
  - wifi/cmd (subscribe)  "reboot" -> khoi dong lai router

KHONG BIA SO: nhung gi router khong cap (cuong do tin hieu, toc do tai)
publish 0, khong dat so gia.
"""

from __future__ import annotations

import base64
import json
import logging
import os
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timedelta, timezone

try:
    import paho.mqtt.client as mqtt
except ImportError:
    mqtt = None

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger("wifi")

ROUTER_IP = os.getenv("ROUTER_IP", "192.168.0.1")
ROUTER_USER = os.getenv("ROUTER_USER", "admin")
ROUTER_PASS = os.getenv("ROUTER_PASS", "admin")

MQTT_BROKER = os.getenv("MQTT_BROKER", "127.0.0.1")
MQTT_PORT = int(os.getenv("MQTT_PORT", "1883"))
MQTT_USER = os.getenv("MQTT_USER", "")
MQTT_PASS = os.getenv("MQTT_PASS", "")
MQTT_CLIENT_ID = os.getenv("MQTT_CLIENT_ID", "smarthome-wifi-collector")

POLL_INTERVAL = int(os.getenv("POLL_INTERVAL", "15"))
GIO_VN = timezone(timedelta(hours=7))

# Viet Nam khong co gio mua he -> cong thang 7 tieng.
CHO_HTTP = 8


# ------------------------------------------------------------ router

class TPLinkCgi:
    """Client giao thuc cgi cua TL-WR840N firmware v6.

    Auth: cookie `Authorization=Basic base64(user:pass)` - khong ma hoa,
    da xac nhan tren router that. Moi request kem Referer, neu khong router
    tra 403/404.
    """

    # Body lay trang thai - chep NGUYEN VAN tu request that cua giao dien web.
    # 10 khoi OID; cac so trong URL (1/5) la "get single" / "get list".
    STATUS_FLAGS = "1&1&1&5&5&5&5&5&5&5"
    STATUS_BODY = (
        "[SYS_MODE#0,0,0,0,0,0#0,0,0,0,0,0]0,1\r\nmode\r\n"
        "[IGD#0,0,0,0,0,0#0,0,0,0,0,0]1,1\r\nLANDeviceNumberOfEntries\r\n"
        "[IGD_DEV_INFO#0,0,0,0,0,0#0,0,0,0,0,0]2,3\r\n"
        "softwareVersion\r\nhardwareVersion\r\nupTime\r\n"
        "[WAN_COMMON_INTF_CFG#0,0,0,0,0,0#0,0,0,0,0,0]3,1\r\nWANAccessType\r\n"
        "[WAN_IP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]4,0\r\n"
        "[WAN_PPP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]5,0\r\n"
        "[WAN_L2TP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]6,0\r\n"
        "[WAN_PPTP_CONN#0,0,0,0,0,0#0,0,0,0,0,0]7,0\r\n"
        "[L2_BRIDGING_ENTRY#0,0,0,0,0,0#0,0,0,0,0,0]8,1\r\nbridgeName\r\n"
        "[LAN_WLAN#0,0,0,0,0,0#0,0,0,0,0,0]9,12\r\n"
        "status\r\nSSID\r\nBSSID\r\nchannel\r\nautoChannelEnable\r\nstandard\r\n"
        "beaconType\r\nbasicEncryptionModes\r\nX_TP_Bandwidth\r\n"
        "possibleDataTransmitRates\r\nWPAAuthenticationMode\r\n"
        "IEEE11iAuthenticationMode\r\n"
    )

    DEVICES_FLAGS = "5"
    DEVICES_BODY = (
        "[LAN_HOST_ENTRY#0,0,0,0,0,0#0,0,0,0,0,0]0,4\r\n"
        "leaseTimeRemaining\r\nMACAddress\r\nhostName\r\nIPAddress\r\n"
    )

    # Khoi dong lai - best effort. Chua xac nhan tren may that, chay thu khi
    # bam nut tren app. Neu router khong nhan OID nay thi se thay trong log.
    REBOOT_FLAGS = "4"
    REBOOT_BODY = "[ACT_REBOOT#0,0,0,0,0,0#0,0,0,0,0,0]0,0\r\n"

    def __init__(self, ip: str, user: str, mat_khau: str):
        self.base = "http://" + ip
        tho = (user + ":" + mat_khau).encode("utf-8")
        self.cookie = "Authorization=Basic " + base64.b64encode(tho).decode("ascii")

    def _cgi(self, flags: str, body: str) -> str:
        url = self.base + "/cgi?" + flags
        req = urllib.request.Request(
            url,
            data=body.encode("utf-8"),
            method="POST",
            headers={
                "Cookie": self.cookie,
                "Referer": self.base + "/",
                "Content-Type": "text/plain",
            },
        )
        with urllib.request.urlopen(req, timeout=CHO_HTTP) as r:
            return r.read().decode("utf-8", "replace")

    @staticmethod
    def _parse(text: str):
        """Doi response cgi thanh list (group_index, dict).

        Format:
            [a,b,c,d,e,f]G
            attr=value
            ...
            [error]0
        G la so nguyen ngay sau dau ']'. Moi khoi mot phan tu.
        """
        khoi = []
        hien = None
        for dong in text.splitlines():
            dong = dong.strip()
            if not dong:
                continue
            if dong.startswith("["):
                dong_dong = dong.rsplit("]", 1)
                if len(dong_dong) == 2 and dong_dong[1].isdigit():
                    g = int(dong_dong[1])
                    hien = {}
                    khoi.append((g, hien))
                else:
                    hien = None  # [error]0, [cgi]... - bo qua
            elif "=" in dong and hien is not None:
                k, v = dong.split("=", 1)
                hien[k] = v
        return khoi

    def trang_thai(self) -> dict:
        kq = self._parse(self._cgi(self.STATUS_FLAGS, self.STATUS_BODY))

        uptime = 0
        wan = {}
        wlan = {}
        for g, d in kq:
            if g == 2 and "upTime" in d:
                uptime = int(d.get("upTime", "0") or 0)
            elif g == 4:
                # WAN_IP_CONN: nhieu instance, lay cai dang ket noi.
                if d.get("connectionStatus") == "Connected":
                    wan = d
            elif g == 9:
                # LAN_WLAN: nhieu interface, uu tien cai dang bat.
                if not wlan or d.get("status") not in ("Disabled", "Down", ""):
                    if d.get("status") not in ("Disabled", "Down", "") or not wlan:
                        wlan = d
        return {"uptime": uptime, "wan": wan, "wlan": wlan}

    def thiet_bi(self) -> list:
        kq = self._parse(self._cgi(self.DEVICES_FLAGS, self.DEVICES_BODY))
        ds = []
        for _g, d in kq:
            ip = d.get("IPAddress", "").strip()
            mac = d.get("MACAddress", "").strip()
            if not ip or not mac:
                continue
            ten = d.get("hostName", "").strip() or "Unknown"
            con = int(d.get("leaseTimeRemaining", "0") or 0)
            ds.append({
                "id": mac,
                "name": ten,
                "ip": ip,
                "mac": mac,
                "online": con > 0,
                "download_mb": 0,   # router khong cap - khong bia
                "upload_mb": 0,
            })
        return ds

    def reboot(self) -> None:
        self._cgi(self.REBOOT_FLAGS, self.REBOOT_BODY)


# ------------------------------------------------------- map sang JSON

def _bao_mat(wlan: dict) -> str:
    """Doi beaconType cua router thanh nhan nguoi doc."""
    bt = (wlan.get("beaconType") or "").strip()
    return {
        "None": "Mở (không mật khẩu)",
        "Basic": "WEP",
        "WPA": "WPA",
        "11i": "WPA2",
        "WPAand11i": "WPA/WPA2",
    }.get(bt, bt or "—")


def dung_info(tt: dict) -> dict:
    wan = tt["wan"]
    wlan = tt["wlan"]
    uptime = tt["uptime"]
    dns = (wan.get("DNSServers") or "").split(",")[0].strip()
    return {
        "ssid": wlan.get("SSID", "").strip() or "—",
        "security": _bao_mat(wlan),
        "signal_dbm": 0,          # router khong bao cuong do cua chinh no
        "signal_label": "",
        "download_mbps": 0,       # router khong cap throughput
        "upload_mbps": 0,
        "speed_mbps": 0,
        "router_ip": ROUTER_IP,
        "dns": dns or "—",
        "gateway": (wan.get("defaultGateway") or "").strip() or "—",
        "wan_ip": (wan.get("externalIPAddress") or "").strip() or "—",
        "is_connected": wan.get("connectionStatus") == "Connected",
        "uptime_days": uptime // 86400,
        "uptime_hours": (uptime % 86400) // 3600,
    }


# ------------------------------------------------------------- MQTT

_router: TPLinkCgi | None = None


def on_connect(client, userdata, flags, rc, props=None):
    if rc == 0:
        logger.info("Da ket noi Mosquitto tai %s:%s", MQTT_BROKER, MQTT_PORT)
        client.subscribe("wifi/cmd", qos=1)
    else:
        logger.error("Khong ket noi duoc Mosquitto, ma %s", rc)


def on_message(client, userdata, msg):
    lenh = msg.payload.decode("utf-8", "replace").strip().lower()
    logger.info("Nhan lenh wifi/cmd: %s", lenh)
    if lenh == "reboot" and _router is not None:
        try:
            _router.reboot()
            logger.info("Da gui lenh reboot toi router")
            client.publish("wifi/cmd/result", "da gui reboot", qos=1)
        except Exception as e:
            logger.error("Reboot that bai: %s", e)
            client.publish("wifi/cmd/result", "reboot loi: " + str(e), qos=1)


def main() -> int:
    global _router
    if mqtt is None:
        logger.error("Thieu paho-mqtt. pip install paho-mqtt")
        return 1

    logger.info("Khoi dong WiFi Collector (firmware cgi)")
    logger.info("Router: %s user %s | Mosquitto: %s:%s",
                ROUTER_IP, ROUTER_USER, MQTT_BROKER, MQTT_PORT)
    _router = TPLinkCgi(ROUTER_IP, ROUTER_USER, ROUTER_PASS)

    try:
        cl = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2,
                         client_id=MQTT_CLIENT_ID, clean_session=True)
    except (AttributeError, TypeError):
        cl = mqtt.Client(client_id=MQTT_CLIENT_ID, clean_session=True)
    cl.on_connect = on_connect
    cl.on_message = on_message
    if MQTT_USER:
        cl.username_pw_set(MQTT_USER, MQTT_PASS)
    try:
        cl.connect(MQTT_BROKER, MQTT_PORT, keepalive=60)
    except Exception as e:
        logger.error("Khong noi duoc broker: %s", e)
        return 1
    cl.loop_start()

    while True:
        now = datetime.now(GIO_VN).strftime("%Y-%m-%d %H:%M:%S")
        try:
            tt = _router.trang_thai()
            devices = _router.thiet_bi()
            info = dung_info(tt)

            cl.publish("wifi/info", json.dumps(info, ensure_ascii=False),
                       retain=True, qos=1)
            cl.publish("wifi/devices", json.dumps(devices, ensure_ascii=False),
                       retain=True, qos=1)
            cl.publish("wifi/capnhat", now, retain=True, qos=1)
            cl.publish("wifi/trangthai", "ok", retain=True, qos=1)
            logger.info("OK: SSID=%s, WAN=%s, thiet bi=%d",
                        info["ssid"], info["wan_ip"], len(devices))
        except urllib.error.HTTPError as e:
            logger.error("Router tra loi HTTP %s - sai mat khau hoac sai "
                         "endpoint firmware?", e.code)
            cl.publish("wifi/trangthai", "loi", retain=True, qos=1)
        except Exception as e:
            logger.error("Loi doc router: %s", e)
            cl.publish("wifi/trangthai", "loi", retain=True, qos=1)

        time.sleep(POLL_INTERVAL)


if __name__ == "__main__":
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print("dung", file=sys.stderr)
        sys.exit(0)
