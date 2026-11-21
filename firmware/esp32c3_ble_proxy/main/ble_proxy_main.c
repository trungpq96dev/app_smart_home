/*
 * Tram trung chuyen BLE cho cu sac CUKTECH 10 Ultra.
 *
 * VAN DE
 * ------
 * Ngay 21/11/2026, `cuktech-ble-server` tren serverubuntu rot ket noi 37 lan
 * trong 2 gio (53 phien dong voi `reason=link_loss`). Do RSSI tai server:
 * -81 den -96 dBm. Cung cu sac do, do tu may dat ngay canh no: -43 dBm.
 * Khong phai loi phan mem - may chu dung qua xa.
 *
 * CACH CHUA
 * ---------
 * Dat con ESP32-C3 nay canh cu sac, va cho may chu muon RADIO cua no qua
 * Wi-Fi. Tram KHONG hieu mot chu nao trong giao thuc cua cu sac: no chi
 * chuyen tiep HCI - dung thu ngon ngu ma BlueZ va mot con dongle USB noi voi
 * nhau. Nho vay toan bo phan bat tay MiOT (HKDF + HMAC + AES-CCM), doc PIID,
 * bat tat cong... van nam nguyen trong `cuktech-ble-server`, khong phai chep
 * lai mot dong nao sang C.
 *
 * Phia may chu:  TCP  <->  pty  <->  btattach -P h4  <->  BlueZ (hci1)
 * Phia tram:     TCP  <->  task nay  <->  VHCI  <->  controller BLE
 *
 * HAI CHE DO
 * ----------
 * 1. CHUA ai noi vao cong TCP -> tram tu quet BLE va bao RSSI len MQTT. Dung
 *    de THU CHO DAT truoc khi dau noi: cam dien, mo app, nhin con so.
 * 2. Co client -> nhuong han controller cho may chu, chi con soi cac goi
 *    advertising di qua de van bao duoc RSSI.
 *
 * CHI MOT CLIENT mot luc. Hai host cung dieu khien mot controller thi trang
 * thai HCI (handle, buffer credit) lan nhau va hong ngay lap tuc.
 */

#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/semphr.h"
#include "freertos/event_groups.h"
#include "freertos/ringbuf.h"

#include "esp_log.h"
#include "esp_system.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "esp_timer.h"
#include "nvs_flash.h"
#include "esp_bt.h"
#include "mqtt_client.h"

#include "lwip/sockets.h"
#include "lwip/netdb.h"

#include "cau_hinh.h"

static const char *TAG = "tram_ble";

/* ---------------------------------------------------------------- HCI H4 */

#define H4_CMD   0x01
#define H4_ACL   0x02
#define H4_SCO   0x03
#define H4_EVT   0x04

/* Goi HCI lon nhat thuc te di qua day la ACL khoang 255 byte; 1024 la thua
 * du va van re so voi RAM cua C3. */
#define HCI_MAX  1024

/* Vong dem controller -> TCP. Callback nhan goi chay trong ngu canh cua
 * controller nen tuyet doi khong duoc block: day vao ring buffer roi tra ve
 * ngay. */
#define RB_SIZE  (8 * 1024)

static RingbufHandle_t s_rb;
static SemaphoreHandle_t s_vhci_sem;      /* controller bao "gui tiep duoc" */
static volatile int s_sock = -1;          /* socket cua host dang muon radio */

/* ------------------------------------------------------------- Suc song */

static uint8_t s_mac_can_tim[6];
static volatile int s_rssi = 0;           /* 0 = chua do duoc lan nao */
static volatile int64_t s_moc_rssi_us = 0;

/*
 * "3C:CD:73:2B:58:8A" -> {0x8A,0x58,0x2B,0x73,0xCD,0x3C}
 *
 * Dao nguoc co y: HCI chay dia chi theo thu tu little-endian, nguoc hoan toan
 * voi cach nguoi ta viet ra giay. Quen dao la so sanh khong bao gio khop ma
 * cung khong bao loi - chi im lang khong thay thiet bi nao.
 */
static bool doc_mac(const char *chu, uint8_t ra[6])
{
    unsigned b[6];
    if (sscanf(chu, "%x:%x:%x:%x:%x:%x", &b[0], &b[1], &b[2], &b[3], &b[4], &b[5]) != 6) {
        return false;
    }
    for (int i = 0; i < 6; i++) {
        ra[i] = (uint8_t) b[5 - i];
    }
    return true;
}

/*
 * Trich RSSI tu mot goi HCI Event neu do la LE Advertising Report cua dung
 * con cu sac.
 *
 * Khuon goi (Core Spec v5.x, 7.7.65.2):
 *   [0]=0x04 H4  [1]=0x3E evt  [2]=plen  [3]=0x02 subevent  [4]=so bao cao
 *   roi moi bao cao: event_type(1) addr_type(1) addr(6) len(1) data(len) rssi(1)
 *
 * Chi doc bao cao DAU TIEN. Gop nhieu bao cao vao mot goi la chuyen hiem o
 * mat do quang ba trong nha, va bo sot mot lan do thi 20 giay sau co lan khac.
 */
static void soi_adv(const uint8_t *p, int len)
{
    if (len < 13 || p[0] != H4_EVT || p[1] != 0x3E) return;
    if (p[3] != 0x02 || p[4] < 1) return;

    const uint8_t *bc = p + 5;
    int con_lai = len - 5;
    if (con_lai < 9) return;

    int dai_data = bc[8];
    if (con_lai < 9 + dai_data + 1) return;

    if (memcmp(bc + 2, s_mac_can_tim, 6) != 0) return;

    s_rssi = (int8_t) bc[9 + dai_data];
    s_moc_rssi_us = esp_timer_get_time();
}

/* ----------------------------------------------------------------- VHCI */

static void vhci_gui_duoc(void)
{
    xSemaphoreGive(s_vhci_sem);
}

/* Controller co goi cho host. KHONG block o day. */
static int vhci_nhan(uint8_t *data, uint16_t len)
{
    soi_adv(data, len);

    int sock = s_sock;
    if (sock >= 0 && len > 0) {
        /* Mat goi o day la mat mot event HCI - BlueZ se timeout roi reset.
         * Tha de no reset con hon lam treo task cua controller. */
        if (xRingbufferSend(s_rb, data, len, 0) != pdTRUE) {
            ESP_LOGW(TAG, "day vong dem, bo mot goi %u byte", (unsigned) len);
        }
    }
    return 0;
}

static esp_vhci_host_callback_t s_vhci_cb = {
    .notify_host_send_available = vhci_gui_duoc,
    .notify_host_recv = vhci_nhan,
};

static void gui_xuong_controller(const uint8_t *goi, size_t len)
{
    for (int i = 0; i < 200 && !esp_vhci_host_check_send_available(); i++) {
        xSemaphoreTake(s_vhci_sem, pdMS_TO_TICKS(10));
    }
    esp_vhci_host_send_packet((uint8_t *) goi, len);
}

/* -------------------------------------------------- Tu quet khi dang ranh */

/* HCI Reset */
static const uint8_t LENH_RESET[] = { H4_CMD, 0x03, 0x0C, 0x00 };

/*
 * LE Set Scan Parameters: passive, interval 60 ms, window 30 ms.
 *
 * Passive co y: quet chu dong con phai PHAT scan request, ma tram nay sinh ra
 * de NGHE cho tot hon chu khong phai de noi to hon. Cu sac cung khong cho
 * them du lieu nao trong scan response (do ngay 21/11/2026: khung quang ba
 * dai dung 11 byte, khong co object du lieu).
 */
static const uint8_t LENH_THAM_SO_QUET[] = {
    H4_CMD, 0x0B, 0x20, 0x07, 0x00, 0x60, 0x00, 0x30, 0x00, 0x00, 0x00,
};

/* LE Set Scan Enable: bat, KHONG loc trung (can RSSI moi nhat, khong phai
 * mot danh sach thiet bi). */
static const uint8_t LENH_BAT_QUET[] = { H4_CMD, 0x0C, 0x20, 0x02, 0x01, 0x00 };

static void bat_tu_quet(void)
{
    gui_xuong_controller(LENH_RESET, sizeof(LENH_RESET));
    vTaskDelay(pdMS_TO_TICKS(200));
    gui_xuong_controller(LENH_THAM_SO_QUET, sizeof(LENH_THAM_SO_QUET));
    vTaskDelay(pdMS_TO_TICKS(50));
    gui_xuong_controller(LENH_BAT_QUET, sizeof(LENH_BAT_QUET));
    ESP_LOGI(TAG, "dang tu quet de do suc song");
}

/* ----------------------------------------------------------------- MQTT */

static esp_mqtt_client_handle_t s_mqtt;

static const char *xep_hang(int rssi)
{
    if (rssi == 0) return "khong-thay";
    if (rssi >= -65) return "tot";
    if (rssi >= -80) return "kha";
    if (rssi >= -90) return "yeu";
    return "rat-yeu";
}

static void bao_rssi(void)
{
    if (!s_mqtt) return;

    char topic[96];
    snprintf(topic, sizeof(topic), "cuktech/rssi/%s", CH_NGUON_ID);

    char goi[320];
    if (s_rssi == 0) {
        /* Chua do duoc lan nao. Gui null chu khong gui 0: 0 dBm la mot muc
         * song RAT MANH, bia ra no la noi doi theo huong nguy hiem nhat. */
        snprintf(goi, sizeof(goi),
                 "{\"rssi\":null,\"xep_hang\":\"khong-thay\",\"tuoi_giay\":null,"
                 "\"dang_noi\":null,\"mac\":\"%s\",\"nguon\":\"%s\",\"adapter\":\"esp32c3\"}",
                 CH_MAC_CU_SAC, CH_NGUON_TEN);
    } else {
        int tuoi = (int) ((esp_timer_get_time() - s_moc_rssi_us) / 1000000);
        snprintf(goi, sizeof(goi),
                 "{\"rssi\":%d,\"xep_hang\":\"%s\",\"tuoi_giay\":%d,"
                 "\"dang_noi\":null,\"mac\":\"%s\",\"nguon\":\"%s\",\"adapter\":\"esp32c3\"}",
                 s_rssi, xep_hang(s_rssi), tuoi, CH_MAC_CU_SAC, CH_NGUON_TEN);
    }
    esp_mqtt_client_publish(s_mqtt, topic, goi, 0, 0, 1 /* retained */);
}

static void mqtt_khoi_dong(void)
{
    if (strlen(CH_MQTT_URI) == 0) {
        ESP_LOGI(TAG, "khong cau hinh MQTT - bo qua phan bao RSSI");
        return;
    }
    static char topic[96];
    snprintf(topic, sizeof(topic), "cuktech/rssi/%s", CH_NGUON_ID);

    esp_mqtt_client_config_t cfg = {
        .broker.address.uri = CH_MQTT_URI,
        .credentials.username = CH_MQTT_USER,
        .credentials.client_id = CH_MQTT_CLIENT_ID,
        .credentials.authentication.password = CH_MQTT_PASS,
        /* LWT: tram mat dien thi app phai thay dong nay BIEN MAT, chu khong
         * phai thay mot con so dung yen va tuong song van the. */
        .session.last_will.topic = topic,
        .session.last_will.msg = "",
        .session.last_will.msg_len = 0,
        .session.last_will.retain = true,
        .session.keepalive = 30,
    };
    s_mqtt = esp_mqtt_client_init(&cfg);
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
        ESP_LOGW(TAG, "mat Wi-Fi, dang noi lai");
        esp_wifi_connect();
    } else if (base == IP_EVENT && id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t *e = (ip_event_got_ip_t *) data;
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
    /* Khong de Wi-Fi ngu: HCI tre vai tram ms thi BlueZ coi la controller
     * chet. Doi lai la ton dien - tram nay cam dien lien tuc nen khong sao. */
    ESP_ERROR_CHECK(esp_wifi_set_ps(WIFI_PS_NONE));
    ESP_ERROR_CHECK(esp_wifi_start());

    xEventGroupWaitBits(s_wifi_eg, BIT_CO_IP, pdFALSE, pdTRUE, portMAX_DELAY);
}

/* ------------------------------------------------------- TCP <-> VHCI */

/* Do dai mot goi H4 theo chieu host -> controller. 0 = chua du header. */
static int dai_goi_h4(const uint8_t *b, int co)
{
    if (co < 1) return 0;
    switch (b[0]) {
    case H4_CMD: return co < 4 ? 0 : 4 + b[3];
    case H4_ACL: return co < 5 ? 0 : 5 + b[3] + (b[4] << 8);
    case H4_SCO: return co < 4 ? 0 : 4 + b[3];
    default:     return -1;   /* rac: khong con biet ranh goi o dau nua */
    }
}

/* Day cac goi tu controller ra socket. */
static void task_len_host(void *arg)
{
    for (;;) {
        size_t len = 0;
        uint8_t *goi = (uint8_t *) xRingbufferReceive(s_rb, &len, pdMS_TO_TICKS(500));
        if (!goi) continue;

        int sock = s_sock;
        if (sock >= 0) {
            int da = 0;
            while (da < (int) len) {
                int n = send(sock, goi + da, len - da, 0);
                if (n <= 0) break;
                da += n;
            }
        }
        vRingbufferReturnItem(s_rb, goi);
    }
}

static void phuc_vu_mot_host(int sock)
{
    static uint8_t dem[HCI_MAX * 2];
    int co = 0;

    ESP_LOGI(TAG, "may chu da muon radio - ngung tu quet");
    s_sock = sock;

    for (;;) {
        int n = recv(sock, dem + co, sizeof(dem) - co, 0);
        if (n <= 0) break;
        co += n;

        bool hong = false;
        for (;;) {
            int dai = dai_goi_h4(dem, co);
            if (dai < 0) {
                ESP_LOGE(TAG, "luong HCI hong (byte dau 0x%02x) - cat ket noi", dem[0]);
                hong = true;
                break;
            }
            if (dai == 0 || dai > co) break;          /* chua du mot goi */
            if (dai > HCI_MAX) {
                ESP_LOGE(TAG, "goi %d byte qua lon - cat ket noi", dai);
                hong = true;
                break;
            }
            gui_xuong_controller(dem, dai);
            memmove(dem, dem + dai, co - dai);
            co -= dai;
        }
        if (hong) break;
    }

    s_sock = -1;
    close(sock);
    ESP_LOGI(TAG, "may chu da nha radio - quay lai tu quet");
    /* Host co the da de controller o trang thai bat ky (dang noi, dang quang
     * ba). Reset roi quet lai tu dau la cach duy nhat biet chac dang o dau. */
    bat_tu_quet();
}

static void task_tcp(void *arg)
{
    int ls = socket(AF_INET, SOCK_STREAM, IPPROTO_IP);
    if (ls < 0) {
        ESP_LOGE(TAG, "khong mo duoc socket: %d", errno);
        vTaskDelete(NULL);
        return;
    }
    int mot = 1;
    setsockopt(ls, SOL_SOCKET, SO_REUSEADDR, &mot, sizeof(mot));

    struct sockaddr_in dc = {
        .sin_family = AF_INET,
        .sin_addr.s_addr = htonl(INADDR_ANY),
        .sin_port = htons(CH_CONG_HCI),
    };
    if (bind(ls, (struct sockaddr *) &dc, sizeof(dc)) != 0 || listen(ls, 1) != 0) {
        ESP_LOGE(TAG, "khong nghe duoc cong %d: %d", CH_CONG_HCI, errno);
        close(ls);
        vTaskDelete(NULL);
        return;
    }
    ESP_LOGI(TAG, "dang nghe HCI tren cong %d", CH_CONG_HCI);

    for (;;) {
        struct sockaddr_in tu;
        socklen_t dai = sizeof(tu);
        int sock = accept(ls, (struct sockaddr *) &tu, &dai);
        if (sock < 0) {
            vTaskDelay(pdMS_TO_TICKS(500));
            continue;
        }
        if (s_sock >= 0) {
            /* Mot host dang dung roi. Tu choi thang thay vi xen vao giua. */
            ESP_LOGW(TAG, "da co host khac - tu choi");
            close(sock);
            continue;
        }
        /* Nagle lam tre goi HCI nho toi 40 ms - du de BlueZ het kien nhan. */
        setsockopt(sock, IPPROTO_TCP, TCP_NODELAY, &mot, sizeof(mot));
        phuc_vu_mot_host(sock);
    }
}

/* ----------------------------------------------------------------- main */

static void task_bao_rssi(void *arg)
{
    for (;;) {
        bao_rssi();
        /* Chua co host nao muon radio ma cung lau roi khong do duoc gi -> co
         * the controller da bi reset tu lan truoc. Bat quet lai cho chac. */
        if (s_sock < 0 &&
            (s_rssi == 0 || esp_timer_get_time() - s_moc_rssi_us > 120LL * 1000000)) {
            bat_tu_quet();
        }
        vTaskDelay(pdMS_TO_TICKS(CH_NHIP_RSSI * 1000));
    }
}

void app_main(void)
{
    esp_err_t e = nvs_flash_init();
    if (e == ESP_ERR_NVS_NO_FREE_PAGES || e == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        e = nvs_flash_init();
    }
    ESP_ERROR_CHECK(e);

    if (!doc_mac(CH_MAC_CU_SAC, s_mac_can_tim)) {
        ESP_LOGE(TAG, "CH_MAC_CU_SAC khong doc duoc: %s", CH_MAC_CU_SAC);
    }

    s_rb = xRingbufferCreate(RB_SIZE, RINGBUF_TYPE_NOSPLIT);
    s_vhci_sem = xSemaphoreCreateBinary();

    wifi_khoi_dong();
    mqtt_khoi_dong();

    esp_bt_controller_config_t bt = BT_CONTROLLER_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_bt_controller_init(&bt));
    ESP_ERROR_CHECK(esp_bt_controller_enable(ESP_BT_MODE_BLE));
    ESP_ERROR_CHECK(esp_vhci_host_register_callback(&s_vhci_cb));

    xTaskCreate(task_len_host, "len_host", 4096, NULL, 6, NULL);
    xTaskCreate(task_tcp, "tcp_hci", 6144, NULL, 5, NULL);
    xTaskCreate(task_bao_rssi, "bao_rssi", 4096, NULL, 3, NULL);

    bat_tu_quet();
    ESP_LOGI(TAG, "tram san sang");
}
