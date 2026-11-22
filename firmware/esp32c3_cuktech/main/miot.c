#include "miot.h"

#include <string.h>
#include <math.h>

const uint8_t MIOT_PIID_DOC_DUOC[] = {
    5, 6, 8, 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 20, 21,
};
const size_t MIOT_SO_PIID_DOC_DUOC =
    sizeof(MIOT_PIID_DOC_DUOC) / sizeof(MIOT_PIID_DOC_DUOC[0]);

size_t miot_dung_tlv(uint8_t *ra, size_t suc_chua, uint8_t seq, uint8_t siid,
                     uint16_t piid, bool co_gia_tri, uint32_t gia_tri)
{
    uint8_t gt[4];
    uint8_t so_byte;
    uint8_t kieu;
    uint8_t opcode;

    if (co_gia_tri) {
        opcode = 0x00;              /* SET */
        if (gia_tri <= 0xFF) {
            kieu = 1;               /* UINT8 */
            so_byte = 1;
            gt[0] = (uint8_t) gia_tri;
        } else {
            kieu = 5;               /* UINT32 */
            so_byte = 4;
            gt[0] = (uint8_t) (gia_tri & 0xFF);
            gt[1] = (uint8_t) ((gia_tri >> 8) & 0xFF);
            gt[2] = (uint8_t) ((gia_tri >> 16) & 0xFF);
            gt[3] = (uint8_t) ((gia_tri >> 24) & 0xFF);
        }
    } else {
        opcode = 0x02;              /* GET */
        kieu = 1;
        so_byte = 1;
        gt[0] = 0x00;               /* cho giu cho, thiet bi khong doc toi */
    }

    size_t tong = 11 + so_byte;
    if (suc_chua < tong) {
        return 0;
    }

    uint16_t tl = (uint16_t) ((kieu << 12) | so_byte);

    ra[0]  = (uint8_t) (tong & 0xFF);
    ra[1]  = 0x20;
    ra[2]  = seq;
    ra[3]  = 0x00;
    ra[4]  = opcode;
    ra[5]  = 0x01;                  /* so TLV trong khung */
    ra[6]  = siid;
    ra[7]  = (uint8_t) (piid & 0xFF);
    ra[8]  = (uint8_t) ((piid >> 8) & 0xFF);
    ra[9]  = (uint8_t) (tl & 0xFF);
    ra[10] = (uint8_t) ((tl >> 8) & 0xFF);
    memcpy(ra + 11, gt, so_byte);

    return tong;
}

uint8_t miot_ma_viec(const uint8_t *pt, size_t dai)
{
    return (pt && dai > 4) ? pt[4] : 0xFF;
}

uint8_t miot_piid(const uint8_t *pt, size_t dai)
{
    return (pt && dai > 7) ? pt[7] : 0;
}

bool miot_doc_gia_tri(const uint8_t *pt, size_t dai, uint32_t *ra)
{
    if (!pt || !ra || dai < 14) {
        return false;
    }
    /* pt[9]/pt[10] la ma loi cua thiet bi. Khac 0 nghia la no tu choi doc
     * PIID nay - vung gia tri phia sau khong co nghia gi, tra ve false chu
     * khong tra ve so 0. */
    if (pt[9] || pt[10]) {
        return false;
    }
    uint8_t dai_gt = pt[11];
    if (dai_gt >= 4) {
        if (dai < 17) {
            return false;
        }
        *ra = (uint32_t) pt[13] | ((uint32_t) pt[14] << 8) |
              ((uint32_t) pt[15] << 16) | ((uint32_t) pt[16] << 24);
    } else {
        *ra = pt[13];
    }
    return true;
}

/*
 * So giao thuc theo cach danh so cua Mi Home.
 *
 * Bang nay lay tu MIJIA_PROTOCOLS trong `state_protocol_v2.py`. 8 va 9 deu
 * la PPS - khong phai nham, firmware dung ca hai ma.
 */
static const char *ten_giao_thuc(uint8_t so)
{
    switch (so) {
    case 1: case 2: return "5V";
    case 3:         return "QC";
    case 4:         return "AFC";
    case 5:         return "FCP";
    case 6:         return "SCP";
    case 7:         return "PD";
    case 8: case 9: return "PPS";
    case 10:        return "UFCS";
    default:        return "PD";
    }
}

/*
 * Doan giao thuc chi bang dien ap - CHI dung khi PIID 17/18 chua ve lan nao.
 *
 * Khong dung byte `code` cua khung day de doan: du an goc da dich nguoc
 * firmware va chung minh byte do la chi so nac PDO chu khong phai so giao
 * thuc (dang sac PD 20 V no nhay giua 0x0B va 0x04). Luat duoi day chi dua
 * tren dac tinh cua chuan USB-PD: nac co dinh la 5/9/12/15/20 V, con PPS
 * chinh duoc lien tuc nen dien ap le.
 */
static uint8_t doan_theo_dien_ap(uint8_t piid, float volt)
{
    static const float NAC_CO_DINH[] = { 5.0f, 9.0f, 12.0f, 15.0f, 20.0f };

    if (piid == 4) {
        return volt > 5.5f ? 3 : 1;
    }
    if (piid == 3) {
        if (volt >= 15.0f) return 7;
        return volt >= 8.5f ? 3 : 1;
    }
    if (volt <= 5.5f) {
        return 1;
    }
    for (size_t i = 0; i < sizeof(NAC_CO_DINH) / sizeof(NAC_CO_DINH[0]); i++) {
        if (fabsf(volt - NAC_CO_DINH[i]) < 0.25f) {
            return 7;
        }
    }
    return 8;
}

bool miot_giai_cong(uint8_t piid, const uint8_t *pt, size_t dai,
                    uint8_t hw_proto, cong_sac_t *ra)
{
    if (!pt || !ra || dai < 12) {
        return false;
    }

    const uint8_t *b = pt + dai - 4;

    ra->status_tho = b[0];
    ra->code_tho   = b[1];
    ra->ampe = b[2] / 10.0f;
    ra->volt = b[3] / 10.0f;
    ra->oat  = ra->volt * ra->ampe;

    /* Dung `status` cua thiet bi, KHONG lay oat > 0 lam chuan: sac gan day
     * pin thi dong tut ve gan 0 ma cong van dang sac that. */
    ra->dang_chay = (b[0] != 0) || ra->volt > 0.0f || ra->ampe > 0.0f;

    if (!ra->dang_chay) {
        ra->giao_thuc = "idle";
        return true;
    }

    uint8_t so = 0;
    if (hw_proto > 0 && hw_proto <= 10) {
        so = hw_proto;
    } else if (hw_proto == 0x70) {
        so = 3;                     /* ma MiOT rieng cua cong C3/A */
    } else if (hw_proto == 0x80) {
        so = 7;
    } else {
        so = doan_theo_dien_ap(piid, ra->volt);
    }
    ra->giao_thuc = ten_giao_thuc(so);
    return true;
}

const char *miot_ten_cong(uint8_t piid)
{
    switch (piid) {
    case 1: return "c1";
    case 2: return "c2";
    case 3: return "c3";
    case 4: return "a";
    default: return NULL;
    }
}

uint8_t miot_piid_tu_ten(const char *ten)
{
    if (!ten) return 0;
    if (!strcmp(ten, "c1")) return 1;
    if (!strcmp(ten, "c2")) return 2;
    if (!strcmp(ten, "c3")) return 3;
    if (!strcmp(ten, "a"))  return 4;
    return 0;
}
