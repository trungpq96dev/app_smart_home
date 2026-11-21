/* Cau hinh tram BLE - CHEP RA cau_hinh.h ROI DIEN
 *
 *   cp main/cau_hinh.mau.h main/cau_hinh.h
 *
 * cau_hinh.h co MAT KHAU Wi-Fi va MQTT nen da nam trong .gitignore.
 * Repo nay cong khai - dung commit no.
 */
#pragma once

/* --- Wi-Fi 2.4 GHz (ESP32-C3 khong co 5 GHz) --- */
#define CH_WIFI_SSID      "TenWifiCuaBan"
#define CH_WIFI_PASS      "MatKhauWifi"

/* --- May chu se dung tram nay lam adapter BLE --- */
/* Cong TCP cho luong HCI. Chi nhan MOT client mot luc: hai host cung dieu
 * khien mot controller la hong ngay. */
#define CH_CONG_HCI       9000

/* --- Cu sac can theo doi --- */
#define CH_MAC_CU_SAC     "3C:CD:73:2B:58:8A"

/* --- MQTT de bao suc song ve app --- */
/* De rong CH_MQTT_URI thi tat han phan bao RSSI, tram chi lam cau HCI. */
#define CH_MQTT_URI       "mqtt://192.168.0.50:1883"
#define CH_MQTT_USER      "iot"
#define CH_MQTT_PASS      ""
#define CH_MQTT_CLIENT_ID "smarthome-esp-ble-proxy"
/* Topic se la `cuktech/rssi/<CH_NGUON_ID>`. Dat khac "server" de app hien
 * duoc hai dong canh nhau. */
#define CH_NGUON_ID       "esp"
#define CH_NGUON_TEN      "ESP32-C3 (cạnh củ sạc)"
/* Giay giua hai lan bao RSSI. */
#define CH_NHIP_RSSI      20
