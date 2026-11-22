#include "cuktech.h"
#include "ble_cuktech.h"
#include "miot.h"

#include <string.h>

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_log.h"
#include "esp_random.h"
#include "esp_timer.h"

static const char *TAG = "cuktech";

/* Cac khung dieu khien co dinh cua giao thuc. Dat ten de doc lai de biet
 * dang cho cai gi. */
static const uint8_t RCV_RDY[]   = { 0x00, 0x00, 0x01, 0x01 };
static const uint8_t RCV_OK[]    = { 0x00, 0x00, 0x01, 0x00 };
static const uint8_t KEEPALIVE[] = { 0x00, 0x00, 0x03, 0x00 };
static const uint8_t ACK_INLINE[]= { 0x00, 0x00, 0x03, 0x00 };
static const uint8_t CMD_LOGIN[] = { 0x24, 0x00, 0x00, 0x00 };
static const uint8_t SEND_KEY[]  = { 0x00, 0x00, 0x00, 0x0b, 0x01, 0x00 };
static const uint8_t SEND_INFO[] = { 0x00, 0x00, 0x00, 0x0a, 0x01, 0x00 };
static const uint8_t CMD_HEADER[]= { 0x00, 0x00, 0x00, 0x00, 0x01, 0x00 };

static uint8_t s_seq = 1;

static int64_t bay_gio_ms(void)
{
    return esp_timer_get_time() / 1000;
}

static bool bang(const uint8_t *a, size_t na, const uint8_t *b, size_t nb)
{
    return na == nb && memcmp(a, b, na) == 0;
}

/* ------------------------------------------------- nhan phan hoi bat tay */

/*
 * Nhan mot phan hoi tren kenh auth_data, tu xu ly ca hai dang khung.
 *
 *   noi tuyen:  00 00 02 XX [du lieu]        - du lieu nho, gui thang
 *   nhieu khung: 00 00 00 XX [so_lo] [so_hi] - roi tung khung [frm_lo][frm_hi][du lieu]
 */
static size_t nhan_auth(uint8_t *ra, size_t suc_chua, int ms)
{
    uint8_t buf[BLE_KHUNG_MAX];
    size_t n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), ms);
    if (n < 4) {
        return 0;
    }

    if (buf[2] == 0x02) {
        size_t dai = n - 4;
        if (dai > suc_chua) {
            dai = suc_chua;
        }
        memcpy(ra, buf + 4, dai);
        ble_ghi(UUID_CH_AUTH_DATA, ACK_INLINE, sizeof(ACK_INLINE));
        return dai;
    }

    if (buf[2] == 0x00 && n >= 6) {
        /* Chan tren 100 khung: thiet bi loi co the khai mot so khong lo va
         * ham nay se treo o day hang gio. */
        int so_khung = buf[4] + 0x100 * buf[5];
        if (so_khung > 100) {
            so_khung = 100;
        }
        ble_ghi(UUID_CH_AUTH_DATA, RCV_RDY, sizeof(RCV_RDY));

        size_t da = 0;
        for (int i = 0; i < so_khung; i++) {
            size_t m = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
            if (m < 2) {
                break;
            }
            size_t phan = m - 2;
            if (da + phan > suc_chua) {
                phan = suc_chua - da;
            }
            memcpy(ra + da, buf + 2, phan);
            da += phan;
            if (da >= suc_chua) {
                break;
            }
        }
        ble_ghi(UUID_CH_AUTH_DATA, RCV_OK, sizeof(RCV_OK));
        return da;
    }

    ESP_LOGW(TAG, "khung auth la: buf[2]=0x%02x dai=%u", buf[2], (unsigned) n);
    return 0;
}

/* ------------------------------------------------------------- bat tay */

bool ct_xac_thuc(mi_phien_t *p, const uint8_t token[12])
{
    uint8_t buf[BLE_KHUNG_MAX];
    size_t n;

    ESP_LOGI(TAG, "[1/5] khoi tao thiet bi (0xa4)...");
    ble_xoa_hang(KENH_AUTH_DATA);

    const uint8_t INIT[] = { 0xa4 };
    if (!ble_ghi(UUID_CH_AUTH_CTRL, INIT, sizeof(INIT))) {
        return false;
    }

    n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
    if (n < 3) {
        ESP_LOGW(TAG, "khong co phan hoi khoi tao");
        return false;
    }

    /* Thoa thuan phien ban: tra lai dung khung vua nhan, rieng byte[2] cong
     * them 1 (04 -> 05). */
    uint8_t ack[BLE_KHUNG_MAX];
    memcpy(ack, buf, n);
    ack[2] = (uint8_t) (ack[2] + 1);
    ble_ghi(UUID_CH_AUTH_DATA, ack, n);

    size_t dai_khoa = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 5000);

    /* May trang thai cua cu sac doi khi lech pha: no gui lai khung khoi tao
     * thay vi bo trao doi khoa. Dau hieu la byte[2]==0x04 nhung khung ngan.
     * Cho them toi 12 giay xem no co duoi kip khong. */
    if (dai_khoa >= 3 && dai_khoa < 20 && buf[2] == 0x04) {
        ESP_LOGW(TAG, "thiet bi lech pha, cho no duoi kip...");
        int64_t han = bay_gio_ms() + 12000;
        bool duoc = false;
        for (int i = 0; i < 3 && bay_gio_ms() < han; i++) {
            size_t m = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf),
                               (int) (han - bay_gio_ms()));
            if (m >= 20 && buf[2] == 0x04) {
                dai_khoa = m;
                duoc = true;
                break;
            }
        }
        if (!duoc) {
            ESP_LOGW(TAG, "thiet bi khong hoi phuc, bo lan bat tay nay");
            return false;
        }
    } else if (dai_khoa == 0) {
        ESP_LOGW(TAG, "khong nhan duoc bo trao doi khoa");
        return false;
    }

    /* Tra lai mot bo giu cho DUNG BANG do dai vua nhan. Noi dung khong quan
     * trong (0xf2 lay theo ban ghi cua app that), nhung do dai thi co. */
    size_t dai_cho = dai_khoa > 4 ? dai_khoa - 4 : 0;
    uint8_t giu_cho[BLE_KHUNG_MAX];
    giu_cho[0] = 0x00; giu_cho[1] = 0x00; giu_cho[2] = 0x05; giu_cho[3] = 0x01;
    memset(giu_cho + 4, 0xf2, dai_cho);
    ble_ghi(UUID_CH_AUTH_DATA, giu_cho, 4 + dai_cho);

    for (int i = 0; i < 2; i++) {
        vTaskDelay(pdMS_TO_TICKS(300));
        ble_xoa_hang(KENH_AUTH_DATA);
    }

    ESP_LOGI(TAG, "[2/5] gui lenh dang nhap (0x24)...");
    vTaskDelay(pdMS_TO_TICKS(50));
    ble_ghi(UUID_CH_AUTH_CTRL, CMD_LOGIN, sizeof(CMD_LOGIN));

    ESP_LOGI(TAG, "[3/5] gui khoa ngau nhien...");
    uint8_t khoa_ta[16];
    esp_fill_random(khoa_ta, sizeof(khoa_ta));

    ble_ghi(UUID_CH_AUTH_DATA, SEND_KEY, sizeof(SEND_KEY));

    /* Bo qua cac khung con ton lai cua buoc trao doi khoa cho den khi thay
     * RCV_RDY. */
    bool san_sang = false;
    for (int i = 0; i < 5; i++) {
        n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
        if (bang(buf, n, RCV_RDY, sizeof(RCV_RDY))) {
            san_sang = true;
            break;
        }
    }
    if (!san_sang) {
        ESP_LOGW(TAG, "khong thay RCV_RDY truoc khi gui khoa");
        return false;
    }

    uint8_t khung[2 + 32];
    khung[0] = 0x01; khung[1] = 0x00;
    memcpy(khung + 2, khoa_ta, 16);
    ble_ghi(UUID_CH_AUTH_DATA, khung, 2 + 16);

    n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
    if (!bang(buf, n, RCV_OK, sizeof(RCV_OK))) {
        ESP_LOGW(TAG, "khong thay RCV_OK sau khi gui khoa");
        return false;
    }

    ESP_LOGI(TAG, "[4/5] nhan phan hoi cua thiet bi...");
    uint8_t khoa_tb[64];
    size_t dai_tb = nhan_auth(khoa_tb, sizeof(khoa_tb), 3000);
    if (dai_tb < 16) {
        ESP_LOGW(TAG, "khoa thiet bi khong hop le (%u byte)", (unsigned) dai_tb);
        return false;
    }

    uint8_t hmac_tb[64];
    size_t dai_hmac = nhan_auth(hmac_tb, sizeof(hmac_tb), 3000);
    if (dai_hmac < 32) {
        ESP_LOGW(TAG, "HMAC thiet bi khong hop le (%u byte)", (unsigned) dai_hmac);
        return false;
    }

    /* salt     = khoa cua ta || khoa cua thiet bi
     * salt_dao = khoa cua thiet bi || khoa cua ta
     * Ta ky tren salt, thiet bi ky tren salt_dao - doi chieu cheo nhu vay
     * thi mot ben phat lai chu ky cua ben kia cung khong qua duoc. */
    uint8_t salt[32], salt_dao[32];
    memcpy(salt, khoa_ta, 16);
    memcpy(salt + 16, khoa_tb, 16);
    memcpy(salt_dao, khoa_tb, 16);
    memcpy(salt_dao + 16, khoa_ta, 16);

    if (!mi_dan_khoa(p, token, khoa_ta, khoa_tb)) {
        ESP_LOGE(TAG, "dan khoa phien that bai");
        return false;
    }
    ESP_LOGI(TAG, "da dan khoa phien");

    uint8_t mong_doi[32];
    if (!mi_hmac(p->dev_key, 16, salt_dao, sizeof(salt_dao), mong_doi)) {
        return false;
    }
    if (memcmp(mong_doi, hmac_tb, 32) != 0) {
        /* Gan nhu luon la token sai. Sai token thi moi buoc truoc do deu
         * chay tron, chi rieng cho nay lech - de nham la loi song. */
        ESP_LOGE(TAG, "HMAC cua thiet bi KHONG khop - gan nhu chac chan la "
                      "CH_TOKEN_HEX sai");
        return false;
    }
    ESP_LOGI(TAG, "HMAC thiet bi dung");

    uint8_t hmac_ta[32];
    if (!mi_hmac(p->app_key, 16, salt, sizeof(salt), hmac_ta)) {
        return false;
    }

    ESP_LOGI(TAG, "[5/5] gui xac nhan...");
    ble_ghi(UUID_CH_AUTH_DATA, SEND_INFO, sizeof(SEND_INFO));

    n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
    if (!bang(buf, n, RCV_RDY, sizeof(RCV_RDY))) {
        ESP_LOGW(TAG, "khong thay RCV_RDY truoc khi gui HMAC");
        return false;
    }

    khung[0] = 0x01; khung[1] = 0x00;
    memcpy(khung + 2, hmac_ta, 32);
    ble_ghi(UUID_CH_AUTH_DATA, khung, 2 + 32);
    ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);   /* ACK, khong bat buoc */

    /* ---- Vong hai: thach thuc - dap loi ----
     * Ban ghi tu app Mi Home:
     *   nhan challenge (0x0d) -> gui keepalive
     *   nhan response  (0x0c) -> gui keepalive -> gui ACK -> nhan RCV_RDY
     *   gui lai response -> nhan RCV_OK
     */
    uint8_t dap[BLE_KHUNG_MAX];
    size_t dai_dap = 0;
    int64_t han = bay_gio_ms() + 8000;
    while (bay_gio_ms() < han) {
        int con = (int) (han - bay_gio_ms());
        n = ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), con > 3000 ? 3000 : con);
        if (n == 0) {
            continue;
        }
        if (n >= 3 && buf[2] == 0x0d) {
            ble_ghi(UUID_CH_AUTH_DATA, KEEPALIVE, sizeof(KEEPALIVE));
        } else if (n >= 3 && buf[2] == 0x0c) {
            memcpy(dap, buf, n);
            dai_dap = n;
            ble_ghi(UUID_CH_AUTH_DATA, KEEPALIVE, sizeof(KEEPALIVE));
            ble_ghi(UUID_CH_AUTH_DATA, SEND_INFO, sizeof(SEND_INFO));
        } else if (bang(buf, n, RCV_RDY, sizeof(RCV_RDY)) ||
                   bang(buf, n, RCV_OK, sizeof(RCV_OK))) {
            break;
        }
    }

    if (dai_dap > 3) {
        uint8_t lai[BLE_KHUNG_MAX];
        lai[0] = 0x01; lai[1] = 0x00; lai[2] = 0x0c;
        memcpy(lai + 3, dap + 3, dai_dap - 3);
        ble_ghi(UUID_CH_AUTH_DATA, lai, dai_dap);
        ble_cho(KENH_AUTH_DATA, buf, sizeof(buf), 3000);
    }

    /* ---- Ket qua ----
     * 0x21 = dang nhap xong, 0x11 = kich hoat xong (lan dau ghep doi),
     * 0x23 / 0x12 = that bai.
     */
    n = ble_cho(KENH_AUTH_CTRL, buf, sizeof(buf), 5000);
    if (n == 0) {
        ESP_LOGW(TAG, "khong nhan duoc ket qua xac thuc");
        return false;
    }
    if (buf[0] == 0x21 || buf[0] == 0x11) {
        ESP_LOGI(TAG, "[+] xac thuc thanh cong (0x%02x)", buf[0]);
        s_seq = 1;
        p->it_gui = 0;
        p->it_nhan_cao = 0;
        p->it_nhan_thap_cuoi = 0;
        vTaskDelay(pdMS_TO_TICKS(500));
        ble_xoa_hang(KENH_CMD_SEND);
        return true;
    }

    ESP_LOGE(TAG, "xac thuc that bai (0x%02x)", buf[0]);
    return false;
}

/* --------------------------------------------------------- kenh lenh */

/* Giai mot khung tren cmd_recv: bo 4 byte dau (hoac gom nhieu khung) roi
 * giai ma. Tra ve do dai plaintext, 0 neu khong giai duoc. */
static size_t giai_khung_cmd(mi_phien_t *p, const uint8_t *raw, size_t n,
                             uint8_t *pt_ra)
{
    if (n < 4) {
        return 0;
    }

    if (raw[2] == 0x02) {
        ble_ghi(UUID_CH_CMD_RECV, ACK_INLINE, sizeof(ACK_INLINE));
        return mi_giai_ma(p, raw + 4, n - 4, pt_ra, CT_PT_MAX);
    }

    if (raw[2] == 0x00 && n >= 6) {
        int so_khung = raw[4] + 0x100 * raw[5];
        if (so_khung > 100) {
            so_khung = 100;
        }
        ble_ghi(UUID_CH_CMD_RECV, RCV_RDY, sizeof(RCV_RDY));

        uint8_t gom[BLE_KHUNG_MAX];
        size_t da = 0;
        uint8_t buf[BLE_KHUNG_MAX];
        for (int i = 0; i < so_khung; i++) {
            size_t m = ble_cho(KENH_CMD_RECV, buf, sizeof(buf), 3000);
            if (m < 2) {
                break;
            }
            size_t phan = m - 2;
            if (da + phan > sizeof(gom)) {
                phan = sizeof(gom) - da;
            }
            memcpy(gom + da, buf + 2, phan);
            da += phan;
            if (da >= sizeof(gom)) {
                break;
            }
        }
        ble_ghi(UUID_CH_CMD_RECV, RCV_OK, sizeof(RCV_OK));
        return da ? mi_giai_ma(p, gom, da, pt_ra, CT_PT_MAX) : 0;
    }

    return 0;
}

size_t ct_doc_khung(mi_phien_t *p, int ms, uint8_t *pt_ra)
{
    uint8_t raw[BLE_KHUNG_MAX];
    size_t n = ble_cho(KENH_CMD_RECV, raw, sizeof(raw), ms);
    if (n == 0) {
        return 0;
    }
    return giai_khung_cmd(p, raw, n, pt_ra);
}

static bool gui_ma_hoa(mi_phien_t *p, const uint8_t *pt, size_t dai_pt)
{
    /* Vut phan hoi cu cua lenh truoc, neu khong buoc cho RCV_RDY se doc
     * phai no va tuong thiet bi da san sang. */
    ble_xoa_hang(KENH_CMD_SEND);

    uint8_t ma[CT_PT_MAX + 8];
    size_t dai_ma = mi_ma_hoa(p, pt, dai_pt, ma, sizeof(ma));
    if (dai_ma == 0) {
        ESP_LOGE(TAG, "ma hoa that bai");
        return false;
    }

    uint8_t buf[BLE_KHUNG_MAX];
    ble_ghi(UUID_CH_CMD_SEND, CMD_HEADER, sizeof(CMD_HEADER));

    size_t n = ble_cho(KENH_CMD_SEND, buf, sizeof(buf), 3000);
    if (!bang(buf, n, RCV_RDY, sizeof(RCV_RDY))) {
        ESP_LOGW(TAG, "CMD_SEND khong co RCV_RDY");
        return false;
    }

    uint8_t khung[2 + CT_PT_MAX + 8];
    khung[0] = 0x01; khung[1] = 0x00;
    memcpy(khung + 2, ma, dai_ma);
    ble_ghi(UUID_CH_CMD_SEND, khung, 2 + dai_ma);

    n = ble_cho(KENH_CMD_SEND, buf, sizeof(buf), 3000);
    if (!bang(buf, n, RCV_OK, sizeof(RCV_OK))) {
        ESP_LOGW(TAG, "CMD_SEND khong co RCV_OK");
        return false;
    }
    return true;
}

bool ct_gui_lenh(mi_phien_t *p, uint8_t siid, uint16_t piid,
                 bool co_gia_tri, uint32_t gia_tri,
                 uint8_t *pt_ra, size_t *dai_ra, ct_nhan_day_t tren_day)
{
    uint8_t pt[32];
    size_t dai_pt = miot_dung_tlv(pt, sizeof(pt), s_seq, siid, piid,
                                  co_gia_tri, gia_tri);
    if (dai_pt == 0) {
        return false;
    }
    uint8_t seq = s_seq;
    s_seq = (uint8_t) (s_seq + 1);

    if (!gui_ma_hoa(p, pt, dai_pt)) {
        return false;
    }

    /* Cho phan hoi. GET tra ve ma viec 0x03, SET tra ve 0x01 (ACK) roi 0x04
     * (ket qua). Trong luc cho, thiet bi van day so cong len cung duong -
     * day chung ra ngoai thay vi vut. */
    const uint8_t mong = co_gia_tri ? 0x04 : 0x03;
    int64_t han = bay_gio_ms() + 8000;
    while (bay_gio_ms() < han) {
        int con = (int) (han - bay_gio_ms());
        size_t n = ct_doc_khung(p, con > 3000 ? 3000 : con, pt_ra);
        if (n == 0) {
            continue;
        }

        uint8_t b4 = miot_ma_viec(pt_ra, n);
        uint8_t pt_siid = n > 6 ? pt_ra[6] : 0;
        uint8_t pt_piid = miot_piid(pt_ra, n);

        if (b4 == mong && pt_siid == siid && pt_piid == (uint8_t) (piid & 0xFF)) {
            if (dai_ra) {
                *dai_ra = n;
            }
            return true;
        }
        /* SET: ACK den truoc ket qua, cu cho tiep. */
        if (co_gia_tri && b4 == 0x01) {
            continue;
        }
        if (b4 == 0x04 && tren_day) {
            tren_day(pt_ra, n);
        }
    }

    ESP_LOGW(TAG, "%s siid=%u piid=%u: khong co phan hoi (seq=%u)",
             co_gia_tri ? "SET" : "GET", siid, piid, seq);
    return false;
}
