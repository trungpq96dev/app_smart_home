#include "mi_crypto.h"

#include <string.h>

#include "mbedtls/ccm.h"
#include "mbedtls/hkdf.h"
#include "mbedtls/md.h"

#define TAG_LEN 4   /* AES-CCM tag 4 byte - do du an goc chot, khong doi duoc */

bool mi_dan_khoa(mi_phien_t *p, const uint8_t token[12],
                 const uint8_t khoa_ta[16], const uint8_t khoa_tb[16])
{
    if (!p || !token || !khoa_ta || !khoa_tb) {
        return false;
    }

    uint8_t salt[32];
    memcpy(salt, khoa_ta, 16);
    memcpy(salt + 16, khoa_tb, 16);

    static const char INFO[] = "mible-login-info";
    uint8_t ra[64];

    const mbedtls_md_info_t *md = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    if (!md) {
        return false;
    }
    int rc = mbedtls_hkdf(md, salt, sizeof(salt), token, 12,
                          (const unsigned char *) INFO, sizeof(INFO) - 1,
                          ra, sizeof(ra));
    if (rc != 0) {
        return false;
    }

    memcpy(p->dev_key, ra + 0, 16);
    memcpy(p->app_key, ra + 16, 16);
    memcpy(p->dev_iv, ra + 32, 4);
    memcpy(p->app_iv, ra + 36, 4);

    /* Bo dem hai chieu deu bat dau lai tu 0 moi phien. */
    p->it_gui = 0;
    p->it_nhan_cao = 0;
    p->it_nhan_thap_cuoi = 0;

    memset(ra, 0, sizeof(ra));
    return true;
}

bool mi_hmac(const uint8_t *khoa, size_t dai_khoa,
             const uint8_t *vao, size_t dai_vao, uint8_t ra[32])
{
    const mbedtls_md_info_t *md = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    if (!md) {
        return false;
    }
    return mbedtls_md_hmac(md, khoa, dai_khoa, vao, dai_vao, ra) == 0;
}

/* nonce = iv(4) + 0x00 x4 + bo_dem(4, little-endian) */
static void dung_nonce(const uint8_t iv[4], uint32_t it, uint8_t ra[12])
{
    memcpy(ra, iv, 4);
    memset(ra + 4, 0, 4);
    ra[8]  = (uint8_t) (it & 0xFF);
    ra[9]  = (uint8_t) ((it >> 8) & 0xFF);
    ra[10] = (uint8_t) ((it >> 16) & 0xFF);
    ra[11] = (uint8_t) ((it >> 24) & 0xFF);
}

size_t mi_ma_hoa(mi_phien_t *p, const uint8_t *pt, size_t dai_pt,
                 uint8_t *ra, size_t suc_chua)
{
    if (!p || !pt || !ra || suc_chua < dai_pt + 2 + TAG_LEN) {
        return 0;
    }

    uint8_t nonce[12];
    dung_nonce(p->app_iv, p->it_gui, nonce);

    mbedtls_ccm_context ccm;
    mbedtls_ccm_init(&ccm);
    int rc = mbedtls_ccm_setkey(&ccm, MBEDTLS_CIPHER_ID_AES, p->app_key, 128);
    if (rc == 0) {
        /* mbedTLS tach ciphertext va tag ra hai cho; thu vien Python gan
         * lien nhau. Ghi tag ngay sau ciphertext de hai ben giong nhau. */
        rc = mbedtls_ccm_encrypt_and_tag(&ccm, dai_pt, nonce, sizeof(nonce),
                                         NULL, 0, pt, ra + 2,
                                         ra + 2 + dai_pt, TAG_LEN);
    }
    mbedtls_ccm_free(&ccm);
    if (rc != 0) {
        return 0;
    }

    ra[0] = (uint8_t) (p->it_gui & 0xFF);
    ra[1] = (uint8_t) ((p->it_gui >> 8) & 0xFF);
    p->it_gui++;

    return dai_pt + 2 + TAG_LEN;
}

size_t mi_giai_ma(mi_phien_t *p, const uint8_t *vao, size_t dai_vao,
                  uint8_t *ra, size_t suc_chua)
{
    if (!p || !vao || !ra || dai_vao < 2 + TAG_LEN) {
        return 0;
    }

    uint16_t it_thap = (uint16_t) (vao[0] | ((uint16_t) vao[1] << 8));
    /* 16 bit thap vua quay vong (tut xuong hon nua chu ky) -> cong 1 vao
     * phan cao. Nguong nua chu ky de mot vai khung ve khong dung thu tu
     * khong bi hieu nham la quay vong. */
    if (it_thap < p->it_nhan_thap_cuoi &&
        (uint16_t) (p->it_nhan_thap_cuoi - it_thap) > 32768u) {
        p->it_nhan_cao++;
    }
    p->it_nhan_thap_cuoi = it_thap;

    uint32_t it = (p->it_nhan_cao << 16) | it_thap;

    uint8_t nonce[12];
    dung_nonce(p->dev_iv, it, nonce);

    size_t dai_ct = dai_vao - 2 - TAG_LEN;
    if (dai_ct == 0 || suc_chua < dai_ct) {
        return 0;
    }

    mbedtls_ccm_context ccm;
    mbedtls_ccm_init(&ccm);
    int rc = mbedtls_ccm_setkey(&ccm, MBEDTLS_CIPHER_ID_AES, p->dev_key, 128);
    if (rc == 0) {
        rc = mbedtls_ccm_auth_decrypt(&ccm, dai_ct, nonce, sizeof(nonce),
                                      NULL, 0, vao + 2, ra,
                                      vao + 2 + dai_ct, TAG_LEN);
    }
    mbedtls_ccm_free(&ccm);

    return rc == 0 ? dai_ct : 0;
}

static int gia_tri_hex(char c)
{
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

bool mi_doc_hex(const char *chu, uint8_t *ra, size_t can)
{
    if (!chu || !ra) {
        return false;
    }
    size_t da = 0;
    int cao = -1;
    for (const char *c = chu; *c; c++) {
        if (*c == ':' || *c == '-' || *c == ' ') {
            continue;
        }
        int v = gia_tri_hex(*c);
        if (v < 0) {
            return false;
        }
        if (cao < 0) {
            cao = v;
        } else {
            if (da >= can) {
                return false;
            }
            ra[da++] = (uint8_t) ((cao << 4) | v);
            cao = -1;
        }
    }
    return cao < 0 && da == can;
}
