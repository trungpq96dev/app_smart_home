/*
 * Tram CUKTECH tren ESP32-C3.
 *
 * ESP32-C3 dat canh cu sac, TU NOI BLE toi no, tu bat tay MiOT, roi day so
 * len MQTT. May chu Ubuntu khong con dinh gi toi Bluetooth nua - no chi la
 * mot thue bao MQTT nhu moi thu khac trong nha.
 *
 * Vi sao doi sang kien truc nay: do ngay 21/11/2026, RSSI tai may chu la
 * -81 den -96 dBm va cu sac rot ket noi 37 lan trong 2 gio. May chu sau nay
 * con de tren cao, cang xa hon. Dat radio canh cu sac la chua dung cho; con
 * doan Wi-Fi tu tram ve may chu thi mat goi bao nhieu cung chi tre so vai
 * giay, khong lam dut phien BLE.
 *
 * Hop dong topic giu NGUYEN nhu `cuktech-ble-server` dang publish, nen app
 * khong phai sua mot dong nao. Doi lai: PHAI tat dich vu kia truoc khi cam
 * tram nay - hai ben cung ghi mot topic retained thi man hinh nhay qua lai,
 * va quan trong hon la cu sac chi cho MOT ket noi BLE mot luc.
 */

#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/event_groups.h"

#include "esp_log.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "esp_timer.h"
#include "nvs_flash.h"
#include "mqtt_client.h"
#include "cJSON.h"

#include "ble_cuktech.h"
#include "cuktech.h"
#include "mi_crypto.h"
#include "miot.h"
#include "cau_hinh.h"

static const char *TAG = "tram";

/* ------------------------------------------------------------ trang thai */

#define SO_PIID_TOI_DA 24

static struct {
    cong_sac_t cong[4];
    bool       co_cong[4];
    uint8_t    hw_proto[5];        /* chi so 1..4 theo PIID cong */
    uint32_t   cai_dat[SO_PIID_TOI_DA];
    bool       co_cai_dat[SO_PIID_TOI_DA];
    char       firmware[32];
    bool       xac_thuc;
    int8_t     rssi;
    bool       co_rssi;
} g;

static mi_phien_t s_phien;
static uint8_t s_token[12];
static uint8_t s_mac[6];
static esp_mqtt_client_handle_t s_mqtt;
static volatile bool s_mqtt_noi;
static volatile bool s_cho_phep_ble = true;

/* Lenh tu MQTT, xu ly trong tac vu BLE chu khong tai cho: ca hai deu dung
 * chung mot kenh lenh, chen ngang la lac phan hoi cua nhau. */
typedef enum { LENH_SET_PIID, LENH_CONG, LENH_BLE } loai_lenh_t;

typedef struct {
    loai_lenh_t loai;
    uint8_t     piid;
    uint8_t     cong;      /* 1..4 cho LENH_CONG */
    bool        bat;
    uint32_t    gia_tri;
} lenh_t;

static QueueHandle_t s_hang_lenh;

/* ---------------------------------------------------------------- MQTT */

/*
 * Noi chuoi an toan: tra ve do dai moi, va KHONG BAO GIO cho `n` vuot
 * `suc_chua`.
 *
 * Viet thang `snprintf(ra + n, suc_chua - n, ...)` thi khi `n` da bang
 * `suc_chua`, phep tru tren size_t quan vong thanh mot so khong lo va
 * snprintf ghi tran ra ngoai bo dem. Ca ham dung JSON duoi day deu noi
 * chuoi nhieu lan nen phai di qua day.
 */
static size_t noi(char *ra, size_t suc_chua, size_t n, const char *dinh_dang, ...)
{
    if (n >= suc_chua) {
        return n;
    }
    va_list ap;
    va_start(ap, dinh_dang);
    int m = vsnprintf(ra + n, suc_chua - n, dinh_dang, ap);
    va_end(ap);
    if (m < 0) {
        return n;
    }
    n += (size_t) m;
    return n > suc_chua - 1 ? suc_chua - 1 : n;
}

static void publish(const char *duoi, const char *tai, bool giu)
{
    if (!s_mqtt || !s_mqtt_noi) {
        return;
    }
    char topic[128];
    if (duoi && duoi[0]) {
        snprintf(topic, sizeof(topic), "%s/%s", CH_TOPIC_GOC, duoi);
    } else {
        snprintf(topic, sizeof(topic), "%s", CH_TOPIC_GOC);
    }
    esp_mqtt_client_publish(s_mqtt, topic, tai, 0, 0, giu ? 1 : 0);
}

/* `enabled` cua mot cong lay tu mat na PIID 16. Chua biet mat na thi KHONG
 * ghi truong nay ra - app doc thieu `enabled` la hieu "chua biet", con ghi
 * bua mot gia tri la noi doi. */
static bool cong_duoc_bat(uint8_t piid, bool *ra)
{
    if (!g.co_cai_dat[PIID_PORT_MASK]) {
        return false;
    }
    *ra = ((g.cai_dat[PIID_PORT_MASK] >> (piid - 1)) & 1) != 0;
    return true;
}

static size_t viet_json_cong(char *ra, size_t suc_chua, uint8_t piid)
{
    const cong_sac_t *c = &g.cong[piid - 1];
    bool bat;
    if (cong_duoc_bat(piid, &bat)) {
        return noi(ra, suc_chua, 0,
            "{\"voltage\":%.1f,\"current\":%.1f,\"power\":%.1f,"
            "\"active\":%s,\"protocol\":\"%s\",\"status_raw\":%u,"
            "\"enabled\":%s}",
            c->volt, c->ampe, c->oat, c->dang_chay ? "true" : "false",
            c->giao_thuc, c->status_tho, bat ? "true" : "false");
    }
    return noi(ra, suc_chua, 0,
        "{\"voltage\":%.1f,\"current\":%.1f,\"power\":%.1f,"
        "\"active\":%s,\"protocol\":\"%s\",\"status_raw\":%u}",
        c->volt, c->ampe, c->oat, c->dang_chay ? "true" : "false",
        c->giao_thuc, c->status_tho);
}

static void publish_cong(uint8_t piid)
{
    const char *ten = miot_ten_cong(piid);
    if (!ten) {
        return;
    }
    char tai[256];
    viet_json_cong(tai, sizeof(tai), piid);

    char duoi[32];
    snprintf(duoi, sizeof(duoi), "port/%s", ten);
    publish(duoi, tai, true);
}

static size_t viet_json_cai_dat(char *ra, size_t suc_chua)
{
    size_t n = noi(ra, suc_chua, 0, "{");
    bool dau = true;
    for (int piid = 1; piid < SO_PIID_TOI_DA; piid++) {
        if (!g.co_cai_dat[piid]) {
            continue;
        }
        n = noi(ra, suc_chua, n, "%s\"%d\":%lu", dau ? "" : ",", piid,
                (unsigned long) g.cai_dat[piid]);
        dau = false;
    }
    return noi(ra, suc_chua, n, "}");
}

static void publish_cai_dat(void)
{
    char tai[512];
    viet_json_cai_dat(tai, sizeof(tai));
    publish("settings", tai, true);
}

static void publish_trang_thai(void)
{
    char tai[1600];
    size_t n = noi(tai, sizeof(tai), 0,
        "{\"connected\":%s,\"authenticated\":%s",
        ble_dang_noi() ? "true" : "false", g.xac_thuc ? "true" : "false");

    if (g.firmware[0]) {
        n = noi(tai, sizeof(tai), n, ",\"firmware_version\":\"%s\"", g.firmware);
    }

    n = noi(tai, sizeof(tai), n, ",\"ports\":{");
    bool dau = true;
    for (uint8_t piid = 1; piid <= 4; piid++) {
        if (!g.co_cong[piid - 1]) {
            continue;
        }
        n = noi(tai, sizeof(tai), n, "%s\"%u\":", dau ? "" : ",", piid);
        n += viet_json_cong(tai + n, sizeof(tai) - n, piid);
        dau = false;
    }
    n = noi(tai, sizeof(tai), n, "},\"settings\":");
    n += viet_json_cai_dat(tai + n, sizeof(tai) - n);
    noi(tai, sizeof(tai), n, "}");

    publish("status", tai, true);
}

static void publish_rssi(void)
{
    if (!s_mqtt || !s_mqtt_noi) {
        return;
    }
    char topic[96];
    snprintf(topic, sizeof(topic), "cuktech/rssi/%s", CH_RSSI_NGUON_ID);

    char tai[320];
    if (g.co_rssi) {
        snprintf(tai, sizeof(tai),
            "{\"rssi\":%d,\"xep_hang\":\"%s\",\"tuoi_giay\":0,\"dang_noi\":%s,"
            "\"mac\":\"%s\",\"nguon\":\"%s\",\"adapter\":\"esp32c3\"}",
            g.rssi,
            g.rssi >= -65 ? "tot" : g.rssi >= -80 ? "kha" :
            g.rssi >= -90 ? "yeu" : "rat-yeu",
            ble_dang_noi() ? "true" : "false",
            CH_MAC_CU_SAC, CH_RSSI_NGUON_TEN);
    } else {
        /* Chua do duoc lan nao -> null, KHONG phai 0: 0 dBm la muc rat manh. */
        snprintf(tai, sizeof(tai),
            "{\"rssi\":null,\"xep_hang\":\"khong-thay\",\"tuoi_giay\":null,"
            "\"dang_noi\":false,\"mac\":\"%s\",\"nguon\":\"%s\","
            "\"adapter\":\"esp32c3\"}",
            CH_MAC_CU_SAC, CH_RSSI_NGUON_TEN);
    }
    esp_mqtt_client_publish(s_mqtt, topic, tai, 0, 0, 1);
}

/* ------------------------------------------------------- lenh tu app */

static void nhan_lenh_mqtt(const char *topic, int dai_topic,
                           const char *tai, int dai_tai)
{
    char t[128];
    int n = dai_topic < (int) sizeof(t) - 1 ? dai_topic : (int) sizeof(t) - 1;
    memcpy(t, topic, n);
    t[n] = 0;

    cJSON *j = cJSON_ParseWithLength(tai, dai_tai);
    if (!j) {
        ESP_LOGW(TAG, "lenh khong phai JSON: %.*s", dai_tai, tai);
        return;
    }

    lenh_t l = { 0 };
    bool co = false;

    if (strstr(t, "/port")) {
        const cJSON *p = cJSON_GetObjectItem(j, "port");
        const cJSON *a = cJSON_GetObjectItem(j, "action");
        if (cJSON_IsString(p) && cJSON_IsString(a)) {
            uint8_t piid = miot_piid_tu_ten(p->valuestring);
            if (piid) {
                l.loai = LENH_CONG;
                l.cong = piid;
                l.bat = strcmp(a->valuestring, "on") == 0;
                co = true;
            }
        }
    } else if (strstr(t, "/set")) {
        const cJSON *p = cJSON_GetObjectItem(j, "piid");
        const cJSON *v = cJSON_GetObjectItem(j, "value");
        if (cJSON_IsNumber(p) && cJSON_IsNumber(v)) {
            l.loai = LENH_SET_PIID;
            l.piid = (uint8_t) p->valuedouble;
            l.gia_tri = (uint32_t) v->valuedouble;
            co = true;
        }
    } else if (strstr(t, "/ble")) {
        const cJSON *e = cJSON_GetObjectItem(j, "enabled");
        if (cJSON_IsBool(e)) {
            l.loai = LENH_BLE;
            l.bat = cJSON_IsTrue(e);
            co = true;
        }
    }

    cJSON_Delete(j);

    if (co && s_hang_lenh) {
        xQueueSend(s_hang_lenh, &l, 0);
    }
}

static void mqtt_su_kien(void *arg, esp_event_base_t base, int32_t id, void *data)
{
    esp_mqtt_event_handle_t e = data;
    switch ((esp_mqtt_event_id_t) id) {
    case MQTT_EVENT_CONNECTED: {
        s_mqtt_noi = true;
        ESP_LOGI(TAG, "MQTT da noi");
        char t[128];
        snprintf(t, sizeof(t), "%s/port", CH_TOPIC_GOC);
        esp_mqtt_client_subscribe(s_mqtt, t, 0);
        snprintf(t, sizeof(t), "%s/set", CH_TOPIC_GOC);
        esp_mqtt_client_subscribe(s_mqtt, t, 0);
        snprintf(t, sizeof(t), "%s/ble", CH_TOPIC_GOC);
        esp_mqtt_client_subscribe(s_mqtt, t, 0);
        publish_trang_thai();
        break;
    }
    case MQTT_EVENT_DISCONNECTED:
        s_mqtt_noi = false;
        break;
    case MQTT_EVENT_DATA:
        nhan_lenh_mqtt(e->topic, e->topic_len, e->data, e->data_len);
        break;
    default:
        break;
    }
}

static void mqtt_khoi_dong(void)
{
    static char lwt[128];
    snprintf(lwt, sizeof(lwt), "%s/status", CH_TOPIC_GOC);

    esp_mqtt_client_config_t cfg = {
        .broker.address.uri = CH_MQTT_URI,
        .credentials.username = CH_MQTT_USER,
        .credentials.client_id = CH_MQTT_CLIENT_ID,
        .credentials.authentication.password = CH_MQTT_PASS,
        /* Tram mat dien thi app phai thay "mat ket noi" chu khong phai mot
         * ban tin retained cu noi rang moi thu van tot. */
        .session.last_will.topic = lwt,
        .session.last_will.msg = "{\"connected\":false,\"authenticated\":false}",
        .session.last_will.retain = true,
        .session.keepalive = 30,
    };
    s_mqtt = esp_mqtt_client_init(&cfg);
    esp_mqtt_client_register_event(s_mqtt, ESP_EVENT_ANY_ID, mqtt_su_kien, NULL);
    esp_mqtt_client_start(s_mqtt);
}

/* ---------------------------------------------------------------- Wi-Fi */

static EventGroupHandle_t s_wifi_eg;
#define BIT_CO_IP BIT0

static void wifi_su_kien(void *arg, esp_event_base_t base, int32_t id, void *data)
{
    if (base == WIFI_EVENT && id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (base == WIFI_EVENT && id == WIFI_EVENT_STA_DISCONNECTED) {
        xEventGroupClearBits(s_wifi_eg, BIT_CO_IP);
        esp_wifi_connect();
    } else if (base == IP_EVENT && id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t *e = data;
        ESP_LOGI(TAG, "co IP " IPSTR, IP2STR(&e->ip_info.ip));
        xEventGroupSetBits(s_wifi_eg, BIT_CO_IP);
    }
}

static void wifi_khoi_dong(void)
{
    s_wifi_eg = xEventGroupCreate();
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(
        WIFI_EVENT, ESP_EVENT_ANY_ID, wifi_su_kien, NULL, NULL));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(
        IP_EVENT, IP_EVENT_STA_GOT_IP, wifi_su_kien, NULL, NULL));

    wifi_config_t wc = { 0 };
    strncpy((char *) wc.sta.ssid, CH_WIFI_SSID, sizeof(wc.sta.ssid) - 1);
    strncpy((char *) wc.sta.password, CH_WIFI_PASS, sizeof(wc.sta.password) - 1);

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wc));
    /* Khong de Wi-Fi ngu. Ngu thi tiet kiem dien nhung cung chia khe song
     * voi BLE kieu khac, va tram nay cam dien lien tuc. */
    ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
    ESP_ERROR_CHECK(esp_wifi_start());

    xEventGroupWaitBits(s_wifi_eg, BIT_CO_IP, pdFALSE, pdTRUE, portMAX_DELAY);
}

/* ------------------------------------------------------- xu ly khung BLE */

static void ghi_nhan_cai_dat(uint8_t piid, uint32_t gia_tri)
{
    if (piid == 0 || piid >= SO_PIID_TOI_DA) {
        return;
    }
    g.cai_dat[piid] = gia_tri;
    g.co_cai_dat[piid] = true;

    /* PIID 17/18 chua so giao thuc cua tung cong, xep MSB -> LSB:
     *   PIID 17: [C1_proto][C1_oat][C2_proto][C2_oat]
     *   PIID 18: [C3_proto][C3_oat][A_proto ][A_oat ]
     */
    if (piid == PIID_HW_PROTO_12) {
        g.hw_proto[1] = (uint8_t) ((gia_tri >> 24) & 0xFF);
        g.hw_proto[2] = (uint8_t) ((gia_tri >> 8) & 0xFF);
    } else if (piid == PIID_HW_PROTO_3A) {
        g.hw_proto[3] = (uint8_t) ((gia_tri >> 24) & 0xFF);
        g.hw_proto[4] = (uint8_t) ((gia_tri >> 8) & 0xFF);
    }
}

static void xu_ly_khung(const uint8_t *pt, size_t n)
{
    uint8_t b4 = miot_ma_viec(pt, n);
    uint8_t piid = miot_piid(pt, n);

    if (b4 != 0x04) {
        return;
    }

    /* Khung thiet bi TU DAY: gia tri u32 nam o pt[12..15]. Khac mot byte so
     * voi phan hoi GET (pt[13..16]) - day la cua du an goc, hai duong di
     * khac nhau that, khong phai nham. */
    if ((piid == PIID_HW_PROTO_12 || piid == PIID_HW_PROTO_3A) && n >= 16) {
        uint32_t v = (uint32_t) pt[12] | ((uint32_t) pt[13] << 8) |
                     ((uint32_t) pt[14] << 16) | ((uint32_t) pt[15] << 24);
        ghi_nhan_cai_dat(piid, v);
        return;
    }

    if (piid >= PIID_CONG_DAU && piid <= PIID_CONG_CUOI) {
        cong_sac_t c;
        if (!miot_giai_cong(piid, pt, n, g.hw_proto[piid], &c)) {
            return;
        }
        cong_sac_t *cu = &g.cong[piid - 1];
        bool doi = !g.co_cong[piid - 1] ||
                   cu->volt != c.volt || cu->ampe != c.ampe ||
                   cu->dang_chay != c.dang_chay ||
                   strcmp(cu->giao_thuc, c.giao_thuc) != 0;

        *cu = c;
        g.co_cong[piid - 1] = true;
        if (doi) {
            publish_cong(piid);
        }
        return;
    }

    /* Cai dat cung duoc day len khi doi tren man hinh cu sac. */
    uint32_t v;
    if (miot_doc_gia_tri(pt, n, &v)) {
        ghi_nhan_cai_dat(piid, v);
    }
}

static void doc_lai_cai_dat(void)
{
    uint8_t pt[CT_PT_MAX];
    size_t dai;
    int doc_duoc = 0;

    for (size_t i = 0; i < MIOT_SO_PIID_DOC_DUOC; i++) {
        if (!ble_dang_noi()) {
            return;
        }
        uint8_t piid = MIOT_PIID_DOC_DUOC[i];
        if (ct_gui_lenh(&s_phien, MIOT_SIID_CU_SAC, piid, false, 0,
                        pt, &dai, xu_ly_khung)) {
            uint32_t v;
            if (miot_doc_gia_tri(pt, dai, &v)) {
                ghi_nhan_cai_dat(piid, v);
                doc_duoc++;
            }
        }
        vTaskDelay(pdMS_TO_TICKS(120));
    }

    ESP_LOGI(TAG, "doc lai cai dat: %d/%u PIID", doc_duoc,
             (unsigned) MIOT_SO_PIID_DOC_DUOC);
    publish_cai_dat();
    publish_trang_thai();
}

static void lam_lenh(const lenh_t *l)
{
    uint8_t pt[CT_PT_MAX];
    size_t dai;

    switch (l->loai) {
    case LENH_BLE:
        s_cho_phep_ble = l->bat;
        ESP_LOGI(TAG, "app bao %s BLE", l->bat ? "bat" : "tat");
        if (!l->bat) {
            ble_ngat();
        }
        break;

    case LENH_SET_PIID:
        ESP_LOGI(TAG, "SET piid=%u gia tri=%lu", l->piid,
                 (unsigned long) l->gia_tri);
        if (ct_gui_lenh(&s_phien, MIOT_SIID_CU_SAC, l->piid, true, l->gia_tri,
                        pt, &dai, xu_ly_khung)) {
            /* Doc lai DUNG cai vua ghi: cu sac lang le bo qua gia tri ngoai
             * khoang, nen "gui xong" khong co nghia la "da doi". */
            if (ct_gui_lenh(&s_phien, MIOT_SIID_CU_SAC, l->piid, false, 0,
                            pt, &dai, xu_ly_khung)) {
                uint32_t v;
                if (miot_doc_gia_tri(pt, dai, &v)) {
                    ghi_nhan_cai_dat(l->piid, v);
                }
            }
            publish_cai_dat();
            publish_trang_thai();
        }
        break;

    case LENH_CONG: {
        if (!g.co_cai_dat[PIID_PORT_MASK]) {
            ESP_LOGW(TAG, "chua biet mat na cong, bo qua lenh bat/tat");
            break;
        }
        uint32_t mask = g.cai_dat[PIID_PORT_MASK];
        uint32_t moi = l->bat ? (mask | (1u << (l->cong - 1)))
                              : (mask & ~(1u << (l->cong - 1)));
        if (moi == mask) {
            break;
        }
        ESP_LOGI(TAG, "cong %s -> %s", miot_ten_cong(l->cong),
                 l->bat ? "bat" : "tat");
        if (ct_gui_lenh(&s_phien, MIOT_SIID_CU_SAC, PIID_PORT_MASK, true, moi,
                        pt, &dai, xu_ly_khung)) {
            if (ct_gui_lenh(&s_phien, MIOT_SIID_CU_SAC, PIID_PORT_MASK, false, 0,
                            pt, &dai, xu_ly_khung)) {
                uint32_t v;
                if (miot_doc_gia_tri(pt, dai, &v)) {
                    ghi_nhan_cai_dat(PIID_PORT_MASK, v);
                }
            }
            for (uint8_t p = 1; p <= 4; p++) {
                if (g.co_cong[p - 1]) {
                    publish_cong(p);
                }
            }
            publish_cai_dat();
            publish_trang_thai();
        }
        break;
    }
    }
}

/* ------------------------------------------------------------ tac vu BLE */

static void tac_vu_ble(void *arg)
{
    int lan_hong = 0;

    for (;;) {
        if (!s_cho_phep_ble) {
            lenh_t l;
            if (xQueueReceive(s_hang_lenh, &l, pdMS_TO_TICKS(500)) == pdTRUE &&
                l.loai == LENH_BLE) {
                lam_lenh(&l);
            }
            continue;
        }

        g.xac_thuc = false;
        publish_trang_thai();

        ESP_LOGI(TAG, "dang tim cu sac...");
        if (!ble_noi(s_mac, 20)) {
            /* Cho lau dan nhung chan tren 30 giay. Cu sac mat dien thi thu
             * lai moi giay chi ton pin cua tram; cham qua thi man hinh treo
             * tuong bao mat ket noi lau hon ban than su co. */
            lan_hong++;
            int cho = lan_hong * 3;
            if (cho > 30) cho = 30;
            vTaskDelay(pdMS_TO_TICKS(cho * 1000));
            continue;
        }

        if (ble_doc_firmware(g.firmware, sizeof(g.firmware))) {
            ESP_LOGI(TAG, "firmware cu sac: %s", g.firmware);
        }

        if (!ct_xac_thuc(&s_phien, s_token)) {
            ble_ngat();
            lan_hong++;
            vTaskDelay(pdMS_TO_TICKS(5000));
            continue;
        }

        lan_hong = 0;
        g.xac_thuc = true;
        publish_trang_thai();

        doc_lai_cai_dat();

        int64_t moc_cai_dat = esp_timer_get_time();
        int64_t moc_rssi = 0;
        int64_t moc_trang_thai = 0;

        while (ble_dang_noi() && s_cho_phep_ble) {
            uint8_t pt[CT_PT_MAX];
            size_t n = ct_doc_khung(&s_phien, 300, pt);
            if (n) {
                xu_ly_khung(pt, n);
            }

            lenh_t l;
            if (xQueueReceive(s_hang_lenh, &l, 0) == pdTRUE) {
                lam_lenh(&l);
            }

            int64_t gio = esp_timer_get_time();

            if (gio - moc_rssi > (int64_t) CH_NHIP_RSSI * 1000000) {
                moc_rssi = gio;
                int8_t r;
                if (ble_rssi(&r)) {
                    g.rssi = r;
                    g.co_rssi = true;
                }
                publish_rssi();
            }

            if (gio - moc_cai_dat > (int64_t) CH_NHIP_CAI_DAT * 1000000) {
                moc_cai_dat = gio;
                doc_lai_cai_dat();
                moc_trang_thai = gio;
            } else if (gio - moc_trang_thai > 10LL * 1000000) {
                /* Publish lai dinh ky du khong co gi doi, de ban tin retained
                 * luon tuoi - app do tuoi cua no de biet so con dung khong. */
                moc_trang_thai = gio;
                publish_trang_thai();
            }
        }

        ESP_LOGW(TAG, "phien ket thuc, se noi lai");
        g.xac_thuc = false;
        g.co_rssi = false;
        ble_ngat();
        publish_trang_thai();
        publish_rssi();
        vTaskDelay(pdMS_TO_TICKS(2000));
    }
}

/* ----------------------------------------------------------------- main */

void app_main(void)
{
    esp_err_t e = nvs_flash_init();
    if (e == ESP_ERR_NVS_NO_FREE_PAGES || e == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        e = nvs_flash_init();
    }
    ESP_ERROR_CHECK(e);

    if (!mi_doc_hex(CH_MAC_CU_SAC, s_mac, 6)) {
        ESP_LOGE(TAG, "CH_MAC_CU_SAC sai dinh dang: %s", CH_MAC_CU_SAC);
        return;
    }
    if (!mi_doc_hex(CH_TOKEN_HEX, s_token, 12)) {
        ESP_LOGE(TAG, "CH_TOKEN_HEX phai la 24 ky tu hex");
        return;
    }

    for (int i = 0; i < 4; i++) {
        g.cong[i].giao_thuc = "idle";
    }

    s_hang_lenh = xQueueCreate(8, sizeof(lenh_t));

    wifi_khoi_dong();
    mqtt_khoi_dong();

    if (!ble_khoi_dong()) {
        ESP_LOGE(TAG, "khong khoi dong duoc BLE");
        return;
    }

    xTaskCreate(tac_vu_ble, "ble", 8192, NULL, 5, NULL);
    ESP_LOGI(TAG, "tram san sang");
}
