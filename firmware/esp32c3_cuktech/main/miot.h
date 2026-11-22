/* Khung lenh MiOT va cach giai so cua cu sac CUKTECH.
 *
 * Moi hang so trong file nay deu doi chieu tu `src/cuktech_ble/protocol.py`,
 * `controller.py` va `state_protocol_v2.py` cua `cuktech-ble-server` (MIT) -
 * ban dang chay that tren serverubuntu, khong phai doan tu tai lieu.
 */
#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

/* Service va characteristic duoi 0xFE95 (Xiaomi MiOT). Deu la UUID 16-bit. */
#define UUID_SV_FE95      0xFE95
#define UUID_CH_FW        0x0004   /* doc ban firmware, khong can xac thuc */
#define UUID_CH_AUTH_CTRL 0x0010
#define UUID_CH_AUTH_DATA 0x0019
#define UUID_CH_CMD_SEND  0x001A
#define UUID_CH_CMD_RECV  0x001B
#define UUID_CH_DEV_INFO  0x001C

#define MIOT_SIID_CU_SAC  2

/* PIID 1-4 la bon cong; 5-21 la cai dat. */
#define PIID_CONG_DAU     1
#define PIID_CONG_CUOI    4
#define PIID_KICH_BAN     5
#define PIID_PORT_MASK    16   /* bit0=C1 bit1=C2 bit2=C3 bit3=A */
#define PIID_HW_PROTO_12  17
#define PIID_HW_PROTO_3A  18

/* Cac PIID doc lai duoc bang lenh GET. Lay nguyen danh sach
 * READABLE_SETTINGS_PIIDS cua du an goc. */
extern const uint8_t MIOT_PIID_DOC_DUOC[];
extern const size_t MIOT_SO_PIID_DOC_DUOC;

/* Mot cong tai mot thoi diem, da giai xong. */
typedef struct {
    float   volt;
    float   ampe;
    float   oat;
    bool    dang_chay;
    uint8_t status_tho;
    uint8_t code_tho;
    const char *giao_thuc;   /* "PD", "PPS", "5V"... hoac "idle" */
} cong_sac_t;

/*
 * Dung plaintext cua mot lenh MiOT.
 *
 * Khuon (doi chieu `_build_miot_tlv`):
 *   [tong_dai:1][0x20:1][seq:1][0x00:1][opcode:1][cnt:1]
 *   [siid:1][piid:2 LE][tl:2 LE][value:N]
 *   tl = (kieu << 12) | so_byte;  kieu 1=UINT8, 5=UINT32
 *   opcode 0x00 = SET, 0x02 = GET
 *
 * `co_gia_tri` sai thi dung khung GET (van kem 1 byte 0x00 lam cho).
 * Tra ve so byte da ghi vao `ra`, hoac 0 neu `ra` qua nho.
 */
size_t miot_dung_tlv(uint8_t *ra, size_t suc_chua, uint8_t seq, uint8_t siid,
                     uint16_t piid, bool co_gia_tri, uint32_t gia_tri);

/*
 * Giai mot khung plaintext da giai ma.
 *
 * pt[4] la ma viec ("b4" trong du an goc): 0x03 = ket qua GET,
 * 0x04 = ket qua SET hoac THIET BI TU DAY len, 0x01 = ACK cua SET.
 * pt[6] la siid, pt[7] la piid (byte thap).
 */
uint8_t miot_ma_viec(const uint8_t *pt, size_t dai);
uint8_t miot_piid(const uint8_t *pt, size_t dai);

/*
 * Lay gia tri so tu mot khung GET/SET-result.
 *
 * Do dai gia tri nam o pt[11]: >= 4 thi doc u32 LE tai pt[13], con lai doc
 * mot byte tai pt[13]. Tra ve false khi khung ngan hoac thiet bi bao loi
 * (pt[9]/pt[10] khac 0 - luc do vung gia tri vo nghia).
 */
bool miot_doc_gia_tri(const uint8_t *pt, size_t dai, uint32_t *ra);

/*
 * Giai so mot cong tu khung day.
 *
 * Bon byte CUOI cua khung la [status][code][dong x10][ap x10]. Dong va ap
 * moi thu dung MOT byte nen tran o 25.5 A / 25.5 V - cu sac 165 W khong toi
 * nguong do.
 *
 * `hw_proto` la so giao thuc lay tu PIID 17/18 (0 = chua biet). Co no thi
 * dung no; khong co thi doan theo dien ap, tho hon nhung van hon bo trong.
 */
bool miot_giai_cong(uint8_t piid, const uint8_t *pt, size_t dai,
                    uint8_t hw_proto, cong_sac_t *ra);

/* "c1" "c2" "c3" "a" theo PIID 1-4, hoac NULL. */
const char *miot_ten_cong(uint8_t piid);

/* Nguoc lai: "c1" -> 1. Tra 0 neu khong khop. */
uint8_t miot_piid_tu_ten(const char *ten);
