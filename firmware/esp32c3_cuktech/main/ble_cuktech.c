#include "ble_cuktech.h"
#include "miot.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/queue.h"
#include "freertos/semphr.h"
#include "esp_log.h"

#include "nimble/nimble_port.h"
#include "nimble/nimble_port_freertos.h"
#include "host/ble_hs.h"
#include "host/util/util.h"

static const char *TAG = "ble";

/* ------------------------------------------------------------- trang thai */

typedef struct {
    uint16_t uuid;
    uint16_t val_handle;
} chr_t;

static chr_t s_chr[] = {
    { UUID_CH_FW,        0 },
    { UUID_CH_AUTH_CTRL, 0 },
    { UUID_CH_AUTH_DATA, 0 },
    { UUID_CH_CMD_SEND,  0 },
    { UUID_CH_CMD_RECV,  0 },
    { UUID_CH_DEV_INFO,  0 },
};
#define SO_CHR (sizeof(s_chr) / sizeof(s_chr[0]))

static uint16_t s_conn = BLE_HS_CONN_HANDLE_NONE;
static uint16_t s_svc_dau, s_svc_cuoi;
static QueueHandle_t s_hang[KENH_SO_LUONG];
static SemaphoreHandle_t s_xong;      /* bao mot thao tac bat dong bo da xong */
static volatile int s_rc;             /* ket qua cua thao tac do */
static volatile bool s_dang_quet;
static uint8_t s_mac_tim[6];
static ble_addr_t s_dia_chi;        /* giu nguyen kieu dia chi quet duoc */
static volatile bool s_thay_mac;
static volatile uint8_t s_da_log;
static volatile uint32_t s_so_lan_ngat;
static char s_firmware[32];

/* Mot khung notify dat trong hang doi. Cap phat dong roi day con tro vao
 * hang: khung to nhat 512 byte ma hang sau muc thi 4 KB nam cho mot kenh
 * gan nhu khong bao gio dung toi. */
typedef struct {
    uint16_t dai;
    uint8_t  d[BLE_KHUNG_MAX];
} khung_t;

/* ----------------------------------------------------------------- tien ich */

static uint16_t handle_cua(uint16_t uuid)
{
    for (size_t i = 0; i < SO_CHR; i++) {
        if (s_chr[i].uuid == uuid) {
            return s_chr[i].val_handle;
        }
    }
    return 0;
}

static int kenh_cua_handle(uint16_t h)
{
    if (h == handle_cua(UUID_CH_AUTH_CTRL)) return KENH_AUTH_CTRL;
    if (h == handle_cua(UUID_CH_AUTH_DATA)) return KENH_AUTH_DATA;
    if (h == handle_cua(UUID_CH_CMD_SEND))  return KENH_CMD_SEND;
    if (h == handle_cua(UUID_CH_CMD_RECV))  return KENH_CMD_RECV;
    return -1;
}

static void bao_xong(int rc)
{
    s_rc = rc;
    xSemaphoreGive(s_xong);
}

static bool cho_xong(int ms)
{
    return xSemaphoreTake(s_xong, pdMS_TO_TICKS(ms)) == pdTRUE && s_rc == 0;
}

/* ------------------------------------------------------- callback cua GATT */

static int cb_svc(uint16_t conn, const struct ble_gatt_error *loi,
                  const struct ble_gatt_svc *svc, void *arg)
{
    if (loi->status == 0 && svc) {
        s_svc_dau = svc->start_handle;
        s_svc_cuoi = svc->end_handle;
        return 0;
    }
    bao_xong(loi->status == BLE_HS_EDONE ? (s_svc_dau ? 0 : BLE_HS_ENOENT)
                                         : loi->status);
    return 0;
}

static int cb_chr(uint16_t conn, const struct ble_gatt_error *loi,
                  const struct ble_gatt_chr *chr, void *arg)
{
    if (loi->status == 0 && chr) {
        /* NimBLE khong co `ble_uuid_is16()`; kiem kieu truc tiep roi moi goi
         * `ble_uuid_u16()`, vi ham do doc 16 bit ma khong tu kiem gi ca. */
        if (chr->uuid.u.type == BLE_UUID_TYPE_16) {
            uint16_t u = ble_uuid_u16(&chr->uuid.u);
            for (size_t i = 0; i < SO_CHR; i++) {
                if (s_chr[i].uuid == u) {
                    s_chr[i].val_handle = chr->val_handle;
                }
            }
        }
        return 0;
    }
    bao_xong(loi->status == BLE_HS_EDONE ? 0 : loi->status);
    return 0;
}

static int cb_ghi(uint16_t conn, const struct ble_gatt_error *loi,
                  struct ble_gatt_attr *attr, void *arg)
{
    bao_xong(loi->status);
    return 0;
}

static int cb_doc(uint16_t conn, const struct ble_gatt_error *loi,
                  struct ble_gatt_attr *attr, void *arg)
{
    if (loi->status == 0 && attr && attr->om) {
        uint16_t n = OS_MBUF_PKTLEN(attr->om);
        if (n > sizeof(s_firmware) - 1) {
            n = sizeof(s_firmware) - 1;
        }
        ble_hs_mbuf_to_flat(attr->om, s_firmware, n, NULL);
        s_firmware[n] = 0;
    }
    bao_xong(loi->status);
    return 0;
}

/* --------------------------------------------------------- callback cua GAP */

static int cb_gap(struct ble_gap_event *ev, void *arg);

static void bat_dau_noi(void)
{
    /* Dung DUNG kieu dia chi ma ban quang ba khai, khong gia dinh public:
     * doan sai kieu thi ket noi het gio ma khong co loi nao ro rang. */
    /* Khoang ket noi 30-50 ms, va supervision timeout 6 giay thay vi 2.56
     * giay mac dinh. Tram nay sinh ra de chay o cho song yeu: mat vai goi
     * lien tiep la chuyen thuong, va cat lien ket vi the thi chi tu lam kho
     * minh - bat tay lai het 40 giay. */
    struct ble_gap_conn_params cp = {
        .scan_itvl = 0x0010,
        .scan_window = 0x0010,
        .itvl_min = 24,
        .itvl_max = 40,
        .latency = 0,
        .supervision_timeout = 600,
        .min_ce_len = 0,
        .max_ce_len = 0,
    };
    int rc = ble_gap_connect(BLE_OWN_ADDR_PUBLIC, &s_dia_chi, 20000, &cp,
                             cb_gap, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "ble_gap_connect loi %d", rc);
        bao_xong(rc);
    }
}

static int cb_gap(struct ble_gap_event *ev, void *arg)
{
    switch (ev->type) {

    case BLE_GAP_EVENT_DISC: {
        if (s_thay_mac || !s_dang_quet) {
            return 0;
        }
        /* Vai dong dau moi lan quet: in ra thay nhung ai. Khong co no thi
         * "khong tim thay" va "cu sac tat dien" trong y het nhau. */
        if (s_da_log < 8) {
            const uint8_t *v = ev->disc.addr.val;
            ESP_LOGI(TAG, "thay %02x:%02x:%02x:%02x:%02x:%02x rssi=%d",
                     v[5], v[4], v[3], v[2], v[1], v[0], ev->disc.rssi);
            s_da_log++;
        }
        if (memcmp(ev->disc.addr.val, s_mac_tim, 6) != 0) {
            return 0;
        }
        ESP_LOGI(TAG, "thay cu sac, rssi=%d", ev->disc.rssi);
        s_dia_chi = ev->disc.addr;
        s_thay_mac = true;
        s_dang_quet = false;
        ble_gap_disc_cancel();
        bat_dau_noi();
        return 0;
    }

    case BLE_GAP_EVENT_DISC_COMPLETE:
        if (s_dang_quet) {
            s_dang_quet = false;
            bao_xong(BLE_HS_ETIMEOUT);   /* quet het gio ma khong thay */
        }
        return 0;

    case BLE_GAP_EVENT_CONNECT:
        if (ev->connect.status == 0) {
            s_conn = ev->connect.conn_handle;
            ESP_LOGI(TAG, "da noi, conn=%d", s_conn);
        } else {
            ESP_LOGW(TAG, "noi that bai, status=%d", ev->connect.status);
        }
        bao_xong(ev->connect.status);
        return 0;

    case BLE_GAP_EVENT_DISCONNECT:
        ESP_LOGW(TAG, "mat ket noi, ly do=0x%04x", ev->disconnect.reason);
        s_conn = BLE_HS_CONN_HANDLE_NONE;
        for (size_t i = 0; i < SO_CHR; i++) {
            s_chr[i].val_handle = 0;
        }
        s_so_lan_ngat++;
        return 0;

    case BLE_GAP_EVENT_MTU:
        ESP_LOGI(TAG, "MTU = %d", ev->mtu.value);
        return 0;

    case BLE_GAP_EVENT_NOTIFY_RX: {
        int kenh = kenh_cua_handle(ev->notify_rx.attr_handle);
        if (kenh < 0 || !s_hang[kenh]) {
            return 0;
        }
        uint16_t n = OS_MBUF_PKTLEN(ev->notify_rx.om);
        if (n == 0 || n > BLE_KHUNG_MAX) {
            ESP_LOGW(TAG, "bo khung %u byte tren kenh %d", n, kenh);
            return 0;
        }
        khung_t *k = malloc(sizeof(khung_t));
        if (!k) {
            return 0;
        }
        k->dai = n;
        ble_hs_mbuf_to_flat(ev->notify_rx.om, k->d, n, NULL);
        /* Day la ngu canh cua NimBLE host - khong duoc cho. Hang day thi bo
         * khung moi nhat va keu len; mat mot khung o day chi lam buoc dang
         * cho het han roi thu lai. */
        if (xQueueSend(s_hang[kenh], &k, 0) != pdTRUE) {
            ESP_LOGW(TAG, "hang kenh %d day, bo mot khung", kenh);
            free(k);
        }
        return 0;
    }

    default:
        return 0;
    }
}

/* ------------------------------------------------------------- khoi dong */

static void tren_dong_bo(void)
{
    ble_hs_util_ensure_addr(0);
    bao_xong(0);
}

static void tac_vu_host(void *arg)
{
    nimble_port_run();
    nimble_port_freertos_deinit();
}

bool ble_khoi_dong(void)
{
    s_xong = xSemaphoreCreateBinary();
    if (!s_xong) {
        return false;
    }
    for (int i = 0; i < KENH_SO_LUONG; i++) {
        s_hang[i] = xQueueCreate(8, sizeof(khung_t *));
        if (!s_hang[i]) {
            return false;
        }
    }

    esp_err_t e = nimble_port_init();
    if (e != ESP_OK) {
        ESP_LOGE(TAG, "nimble_port_init loi %d", e);
        return false;
    }

    ble_hs_cfg.sync_cb = tren_dong_bo;
    /* KHONG goi `ble_svc_gap_init()` / `ble_svc_gap_device_name_set()`: do la
     * service cua GATT SERVER, chi duoc bien dich khi bat vai tro peripheral.
     * `sdkconfig.defaults` tat peripheral (tram chi di noi, khong bao gio
     * quang ba), nen goi chung vao thi link hong - va cung chang de lam gi:
     * khong ai doc duoc ten cua mot thiet bi khong quang ba. */
    /* Khung bat tay dai toi 240 byte - MTU 23 mac dinh chi cho 20 byte moi
     * goi, khong du cho mot khung nao trong so do. */
    ble_att_set_preferred_mtu(517);

    nimble_port_freertos_init(tac_vu_host);

    if (!cho_xong(10000)) {
        ESP_LOGE(TAG, "NimBLE khong dong bo duoc");
        return false;
    }
    ESP_LOGI(TAG, "NimBLE san sang");
    return true;
}

/* ----------------------------------------------------------------- ket noi */

void ble_xoa_hang(ble_kenh_t kenh)
{
    if (kenh < 0 || kenh >= KENH_SO_LUONG || !s_hang[kenh]) {
        return;
    }
    khung_t *k;
    while (xQueueReceive(s_hang[kenh], &k, 0) == pdTRUE) {
        free(k);
    }
}

static bool bat_notify(uint16_t uuid)
{
    uint16_t h = handle_cua(uuid);
    if (!h) {
        ESP_LOGE(TAG, "khong tim thay char 0x%04x", uuid);
        return false;
    }
    /* CCCD nam ngay sau o gia tri. Dung voi ca sau characteristic cua cu sac
     * nay - bang handle trong `protocol.py` cua du an goc liet ke du
     * (0x000d/0x000e, 0x0010/0x0011, 0x0019/0x001a, 0x001c/0x001d). Di tim
     * descriptor cho tung cai la them mot vong bat tay GATT nua ma khong doi
     * duoc gi. */
    uint8_t bat[2] = { 0x01, 0x00 };
    /* Ghi CO tra loi, khong phai write-without-response: theo chuan GATT,
     * CCCD chi chap nhan Write Request. Ghi kieu "ban roi quen" vao day thi
     * thiet bi am tham bo qua, notify khong bao gio bat, va trieu chung se
     * la "bat tay het gio o buoc 1" - rat de do nham sang song yeu. */
    xSemaphoreTake(s_xong, 0);
    int rc = ble_gattc_write_flat(s_conn, h + 1, bat, sizeof(bat), cb_ghi, NULL);
    if (rc != 0 || !cho_xong(3000)) {
        ESP_LOGE(TAG, "bat notify 0x%04x loi (rc=%d s_rc=%d)", uuid, rc, s_rc);
        return false;
    }
    return true;
}

bool ble_noi(const uint8_t mac[6], int giay)
{
    /* NimBLE giu dia chi BLE theo thu tu byte NGUOC: `addr.val[0]` la byte
     * THAP nhat, tuc la chu so cuoi cua "3C:CD:73:2B:58:8A". Con `mi_doc_hex`
     * doc chuoi tu trai sang phai nen tra ve thu tu thuan. So thang hai cai
     * voi nhau thi khong bao gio khop, va trieu chung la quet mai khong thay
     * gi - giong het cu sac mat dien. Dao lai o day, mot lan, cho xong. */
    for (int i = 0; i < 6; i++) {
        s_mac_tim[i] = mac[5 - i];
    }
    s_da_log = 0;
    s_thay_mac = false;
    s_dang_quet = true;
    s_svc_dau = s_svc_cuoi = 0;
    s_firmware[0] = 0;
    for (size_t i = 0; i < SO_CHR; i++) {
        s_chr[i].val_handle = 0;
    }
    for (int i = 0; i < KENH_SO_LUONG; i++) {
        ble_xoa_hang(i);
    }
    xSemaphoreTake(s_xong, 0);

    struct ble_gap_disc_params dp = {
        .itvl = 0x0060,
        .window = 0x0030,
        .filter_policy = 0,
        .limited = 0,
        /* Quet thu dong: khong phat scan request. Cu sac khong cho them du
         * lieu nao trong scan response (khung quang ba cua no dai dung 11
         * byte), nen hoi them chi ton song. */
        .passive = 1,
        .filter_duplicates = 0,
    };
    int rc = ble_gap_disc(BLE_OWN_ADDR_PUBLIC, giay * 1000, &dp, cb_gap, NULL);
    if (rc != 0) {
        ESP_LOGE(TAG, "ble_gap_disc loi %d", rc);
        s_dang_quet = false;
        return false;
    }

    /* Mot lan `bao_xong` cho ca quet lan noi: callback quet goi thang
     * `bat_dau_noi()`, va chi su kien CONNECT (hoac het gio) moi bao ve. */
    if (!cho_xong(giay * 1000 + 25000)) {
        ESP_LOGW(TAG, "khong noi duoc (rc=%d)", s_rc);
        s_dang_quet = false;
        ble_gap_disc_cancel();
        ble_ngat();
        return false;
    }

    /* Thoa thuan MTU truoc khi lam bat cu gi khac. */
    ble_gattc_exchange_mtu(s_conn, NULL, NULL);
    vTaskDelay(pdMS_TO_TICKS(300));

    ble_uuid16_t sv = BLE_UUID16_INIT(UUID_SV_FE95);
    xSemaphoreTake(s_xong, 0);
    rc = ble_gattc_disc_svc_by_uuid(s_conn, &sv.u, cb_svc, NULL);
    if (rc != 0 || !cho_xong(5000) || !s_svc_dau) {
        ESP_LOGE(TAG, "khong tim thay service FE95");
        ble_ngat();
        return false;
    }

    xSemaphoreTake(s_xong, 0);
    rc = ble_gattc_disc_all_chrs(s_conn, s_svc_dau, s_svc_cuoi, cb_chr, NULL);
    if (rc != 0 || !cho_xong(8000)) {
        ESP_LOGE(TAG, "khong liet ke duoc characteristic");
        ble_ngat();
        return false;
    }

    if (!handle_cua(UUID_CH_AUTH_CTRL) || !handle_cua(UUID_CH_AUTH_DATA) ||
        !handle_cua(UUID_CH_CMD_SEND) || !handle_cua(UUID_CH_CMD_RECV)) {
        ESP_LOGE(TAG, "thieu characteristic bat buoc");
        ble_ngat();
        return false;
    }

    /* Bat notify TRUOC khi bat tay. Rieng auth_ctrl, ban Python dang ky lai
     * mot lan nua giua chung bat tay; o day bat san ca bon tu dau cho gon -
     * notify bat hai lan khong hai gi. */
    if (!bat_notify(UUID_CH_AUTH_DATA) || !bat_notify(UUID_CH_AUTH_CTRL) ||
        !bat_notify(UUID_CH_CMD_SEND) || !bat_notify(UUID_CH_CMD_RECV)) {
        ble_ngat();
        return false;
    }

    ESP_LOGI(TAG, "da noi va bat notify du bon kenh");
    return true;
}

void ble_ngat(void)
{
    if (s_conn != BLE_HS_CONN_HANDLE_NONE) {
        ble_gap_terminate(s_conn, BLE_ERR_REM_USER_CONN_TERM);
        for (int i = 0; i < 30 && s_conn != BLE_HS_CONN_HANDLE_NONE; i++) {
            vTaskDelay(pdMS_TO_TICKS(100));
        }
    }
    s_conn = BLE_HS_CONN_HANDLE_NONE;
}

bool ble_dang_noi(void)
{
    return s_conn != BLE_HS_CONN_HANDLE_NONE;
}

bool ble_ghi(uint16_t uuid_char, const uint8_t *d, size_t n)
{
    if (!ble_dang_noi()) {
        return false;
    }
    uint16_t h = handle_cua(uuid_char);
    if (!h) {
        return false;
    }
    int rc = ble_gattc_write_no_rsp_flat(s_conn, h, d, n);
    if (rc != 0) {
        ESP_LOGW(TAG, "ghi 0x%04x loi %d", uuid_char, rc);
        return false;
    }
    return true;
}

bool ble_doc_firmware(char *ra, size_t suc_chua)
{
    if (!ble_dang_noi() || !handle_cua(UUID_CH_FW)) {
        return false;
    }
    xSemaphoreTake(s_xong, 0);
    s_firmware[0] = 0;
    if (ble_gattc_read(s_conn, handle_cua(UUID_CH_FW), cb_doc, NULL) != 0) {
        return false;
    }
    if (!cho_xong(3000) || !s_firmware[0]) {
        return false;
    }
    snprintf(ra, suc_chua, "%s", s_firmware);
    return true;
}

size_t ble_cho(ble_kenh_t kenh, uint8_t *ra, size_t suc_chua, int ms)
{
    if (kenh < 0 || kenh >= KENH_SO_LUONG || !s_hang[kenh] || !ra) {
        return 0;
    }
    khung_t *k = NULL;
    if (xQueueReceive(s_hang[kenh], &k, pdMS_TO_TICKS(ms)) != pdTRUE || !k) {
        return 0;
    }
    size_t n = k->dai;
    if (n > suc_chua) {
        n = suc_chua;
    }
    memcpy(ra, k->d, n);
    free(k);
    return n;
}

bool ble_rssi(int8_t *ra)
{
    if (!ble_dang_noi() || !ra) {
        return false;
    }
    return ble_gap_conn_rssi(s_conn, ra) == 0;
}

uint32_t ble_so_lan_ngat(void)
{
    return s_so_lan_ngat;
}
