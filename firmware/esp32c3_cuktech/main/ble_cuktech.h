/* Lop BLE: quet, noi, bat notify, va bien bon kenh notify thanh bon hang doi.
 *
 * Vi sao co hang doi: giao thuc bat tay cua MiOT la mot chuoi "ghi roi cho
 * tra loi" dai muoi may buoc. Viet thang bang callback cua NimBLE thi thanh
 * mot may trang thai ba chuc nhanh, khong ai doc lai duoc. Voi hang doi thi
 * ham `ble_cho()` dong vai `await wait_notify(...)` trong ban Python goc, va
 * trinh tu bat tay doc tu tren xuong duoi dung nhu tai lieu.
 */
#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

typedef enum {
    KENH_AUTH_CTRL = 0,   /* char 0x0010 */
    KENH_AUTH_DATA,       /* char 0x0019 */
    KENH_CMD_SEND,        /* char 0x001A */
    KENH_CMD_RECV,        /* char 0x001B */
    KENH_SO_LUONG,
} ble_kenh_t;

/* Mot khung notify lon nhat ma thiet bi gui. Khung lon nhat thay trong giao
 * thuc la bo du lieu trao doi khoa (~240 byte); 512 la thua du va van vua
 * MTU 517 da thoa thuan. */
#define BLE_KHUNG_MAX 512

/* Khoi dong NimBLE. Goi mot lan luc may len. */
bool ble_khoi_dong(void);

/* Quet tim `mac` roi noi, thoa thuan MTU, bat notify ca bon kenh.
 * `giay` la han cho ca viec quet lan viec noi. */
bool ble_noi(const uint8_t mac[6], int giay);

void ble_ngat(void);
bool ble_dang_noi(void);

/* Ghi khong can tra loi (write-without-response) vao mot characteristic,
 * goi theo UUID 16-bit (UUID_CH_* trong miot.h). */
bool ble_ghi(uint16_t uuid_char, const uint8_t *d, size_t n);

/* Doc ban firmware (char 0x0004, doc duoc khi chua xac thuc). */
bool ble_doc_firmware(char *ra, size_t suc_chua);

/*
 * Cho mot khung tren mot kenh.
 *
 * Tra ve so byte nhan duoc, 0 khi het han. `ra` phai chua duoc
 * BLE_KHUNG_MAX byte.
 */
size_t ble_cho(ble_kenh_t kenh, uint8_t *ra, size_t suc_chua, int ms);

/* Vut het khung con ton trong mot kenh. Phai goi truoc moi buoc "ghi roi cho
 * tra loi", neu khong se doc phai tra loi cu cua buoc truoc. */
void ble_xoa_hang(ble_kenh_t kenh);

/* RSSI cua lien ket dang mo. Tra ve false khi chua noi. */
bool ble_rssi(int8_t *ra);

/* Tang moi lan ngat ket noi - de vong ngoai biet phien da chet. */
uint32_t ble_so_lan_ngat(void);
