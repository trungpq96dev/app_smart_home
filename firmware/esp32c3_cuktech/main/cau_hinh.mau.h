/* Cau hinh tram CUKTECH - CHEP RA cau_hinh.h ROI DIEN
 *
 *   cp main/cau_hinh.mau.h main/cau_hinh.h
 *
 * cau_hinh.h co MAT KHAU Wi-Fi, MAT KHAU MQTT va TOKEN cua cu sac nen da nam
 * trong .gitignore. Repo nay cong khai tren GitHub - dung commit no.
 */
#pragma once

/* --- Wi-Fi 2.4 GHz (ESP32-C3 khong co 5 GHz) --- */
#define CH_WIFI_SSID      "TenWifiCuaBan"
#define CH_WIFI_PASS      "MatKhauWifi"

/* --- Cu sac --- */
#define CH_MAC_CU_SAC     "3C:CD:73:2B:58:8A"

/* TOKEN cua thiet bi: 24 ky tu hex = 12 byte.
 *
 * Lay o `ble.token` trong ~/cuktech-ble-server/config.yaml tren serverubuntu
 * (dung may truoc gio van dung chinh no de bat tay). KHONG phai `ble_key` -
 * ble_key 32 ky tu la de giai MiBeacon, khong dung cho bat tay nay.
 *
 * Sai token thi bat tay chay het 5 buoc roi thiet bi tra ve 0x23 (Login
 * Failed) - khong phai loi ket noi, dung di do song.
 */
#define CH_TOKEN_HEX      "000000000000000000000000"

/* --- MQTT --- */
#define CH_MQTT_URI       "mqtt://192.168.0.50:1883"
#define CH_MQTT_USER      "iot"
#define CH_MQTT_PASS      ""
#define CH_MQTT_CLIENT_ID "smarthome-esp-cuktech"

/* Tien to topic. PHAI trung voi `cuktech/charger` ma app dang doc, neu khong
 * thi man hinh Cam bien khong thay gi. */
#define CH_TOPIC_GOC      "cuktech/charger"

/* Topic rieng bao suc song BLE do TAI TRAM (app doc `cuktech/rssi/<id>`). */
#define CH_RSSI_NGUON_ID  "esp"
#define CH_RSSI_NGUON_TEN "ESP32-C3 (cạnh củ sạc)"

/* --- Nhip --- */
/* Giay giua hai lan doc lai bang cai dat (PIID). So cong thi khong can doc -
 * cu sac tu day ve moi khi co thay doi. */
#define CH_NHIP_CAI_DAT   30
/* Giay giua hai lan publish RSSI. */
#define CH_NHIP_RSSI      20
