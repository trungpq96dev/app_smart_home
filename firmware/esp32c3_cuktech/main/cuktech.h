/* Bat tay MiOT va kenh lenh cua cu sac CUKTECH.
 *
 * Trinh tu duoi day chep tung buoc tu `_try_authenticate()` trong
 * `src/cuktech_ble/controller.py` cua `cuktech-ble-server`. KHONG rut gon
 * buoc nao, ke ca nhung buoc trong vo nghia (gui lai 240 byte 0xf2, hai lan
 * keepalive o vong hai): du an goc lay chung tu ban ghi Frida cua app Mi
 * Home that, va thiet bi tu choi dang nhap neu thieu.
 */
#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

#include "mi_crypto.h"

/* Plaintext lon nhat can giu lai: khung GET/SET tra ve chi vai chuc byte,
 * nhung khung thiet bi tu day luc moi noi co the dai hon. */
#define CT_PT_MAX 256

/* Ham duoc goi cho moi khung thiet bi TU DAY len trong luc dang cho mot
 * phan hoi khac. Bo qua chung la mat so cong: thiet bi da nhan ACK roi nen
 * no khong day lai. */
typedef void (*ct_nhan_day_t)(const uint8_t *pt, size_t dai);

/* Chay tron bo bat tay. Tra ve true khi thiet bi bao dang nhap thanh cong. */
bool ct_xac_thuc(mi_phien_t *p, const uint8_t token[12]);

/*
 * Gui mot lenh MiOT roi cho phan hoi.
 *
 * `co_gia_tri` = false la GET, true la SET. Phan hoi duoc chep vao `pt_ra`
 * (CT_PT_MAX byte) va do dai vao `*dai_ra`. Khung thiet bi tu day gap trong
 * luc cho se di qua `tren_day` chu khong bi vut.
 */
bool ct_gui_lenh(mi_phien_t *p, uint8_t siid, uint16_t piid,
                 bool co_gia_tri, uint32_t gia_tri,
                 uint8_t *pt_ra, size_t *dai_ra, ct_nhan_day_t tren_day);

/*
 * Doc mot khung tu kenh `cmd_recv` (giai ma san).
 *
 * Dung cho vong chinh luc khong gui lenh gi: cu sac tu day so cong len moi
 * khi co thay doi, va do la nguon so chinh - khong phai do vong.
 * Tra ve 0 khi het han.
 */
size_t ct_doc_khung(mi_phien_t *p, int ms, uint8_t *pt_ra);
