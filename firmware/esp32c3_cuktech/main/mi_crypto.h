/* Phan mat ma cua phien MiOT: dan khoa, HMAC, AES-CCM.
 *
 * Tach rieng khoi phan BLE co y - day la phan DUY NHAT trong firmware co the
 * kiem tra bang vector co san (tu log cua `cuktech-ble-server`) ma khong can
 * cu sac o canh.
 */
#pragma once

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>

typedef struct {
    uint8_t dev_key[16];
    uint8_t app_key[16];
    uint8_t dev_iv[4];
    uint8_t app_iv[4];

    /* Bo dem khung gui di. Thiet bi theo doi no de chong phat lai. */
    uint32_t it_gui;

    /* Thiet bi CHI gui 16 bit thap cua bo dem cua no trong moi khung. Qua
     * 65535 thi 16 bit cao khac 0, ma khung khong cho biet - phai tu dem lay
     * bang cach bat luc 16 bit thap quay vong. Thieu cho nay thi sau vai gio
     * chay lien tuc la giai ma hong hang loat, trong khi ket noi van tot. */
    uint32_t it_nhan_cao;
    uint16_t it_nhan_thap_cuoi;
} mi_phien_t;

/*
 * Dan bon khoa phien tu token.
 *
 * HKDF-SHA256(ikm = token 12 byte, salt = khoa_ta || khoa_thiet_bi (32 byte),
 *             info = "mible-login-info", L = 64)
 * roi cat: dev_key[0:16] app_key[16:32] dev_iv[32:36] app_iv[36:40].
 */
bool mi_dan_khoa(mi_phien_t *p, const uint8_t token[12],
                 const uint8_t khoa_ta[16], const uint8_t khoa_tb[16]);

/* HMAC-SHA256. Dung de kiem HMAC cua thiet bi va dung HMAC cua minh. */
bool mi_hmac(const uint8_t *khoa, size_t dai_khoa,
             const uint8_t *vao, size_t dai_vao, uint8_t ra[32]);

/*
 * Ma hoa mot khung gui di.
 *
 * ra = [it_lo][it_hi][ciphertext][tag 4 byte]
 * Tra ve so byte da ghi, hoac 0 khi loi / `ra` qua nho.
 * Tang `it_gui` khi thanh cong.
 */
size_t mi_ma_hoa(mi_phien_t *p, const uint8_t *pt, size_t dai_pt,
                 uint8_t *ra, size_t suc_chua);

/*
 * Giai ma mot khung nhan duoc (dang [it_lo][it_hi][ct][tag]).
 *
 * Tra ve so byte plaintext, hoac 0 khi tag sai / khung qua ngan.
 */
size_t mi_giai_ma(mi_phien_t *p, const uint8_t *vao, size_t dai_vao,
                  uint8_t *ra, size_t suc_chua);

/* "3ccd732b588a" hoac "3C:CD:73:2B:58:8A" -> 6 byte. Tra ve false neu sai. */
bool mi_doc_hex(const char *chu, uint8_t *ra, size_t can);
