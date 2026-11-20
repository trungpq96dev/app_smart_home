package vn.smarthome.data.giongnoi

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cau hinh tro ly giong noi Gemini Live.
 *
 * Giong [vn.smarthome.data.dien.CauHinhMqtt] va
 * [vn.smarthome.data.camera.CauHinhCamera]: khoa API go o man Cai dat, nam
 * trong DataStore cua may tinh bang va da loai khoi sao luu dam may (xem
 * res/xml/backup_rules.xml). Ma nguon KHONG nhung san khoa nao - vi ma nguon
 * nay nam tren GitHub, va ban release duoc phat hanh cong khai o GitHub
 * Releases cho co che OTA tai ve.
 */
data class CauHinhGiongNoi(
    val khoaApi: String = "",
    val model: String = MODEL_MAC_DINH,
    val giongDoc: String = GIONG_MAC_DINH,
    /** Bat thi moi lenh dieu khien con ban ra MQTT `smarthome/control`. */
    val guiMqtt: Boolean = false,
    /**
     * Ban song cong: ngung gui mic trong luc loa dang phat tieng Gemini.
     *
     * De [BanSongCong.TU_DONG] thi app tu quyet dinh theo viec may co bo khu
     * vong phan cung hay khong. Dat tay khi may co bo khu vong nhung yeu.
     */
    val banSongCong: BanSongCong = BanSongCong.TU_DONG,
    /** Im lang bao nhieu giay thi tu dong dong phien. 0 = khong tu dong dong. */
    val giayImLangTuDong: Int = 45,
    /** Tran cung cho mot phien, tinh bang giay. */
    val giayToiDaPhien: Int = 600,
) {
    fun coKhoa(): Boolean = khoaApi.isNotBlank()

    fun dayDu(): Boolean = coKhoa() && model.isNotBlank()

    /**
     * Duong WebSocket toi Gemini Live.
     *
     * Doi chieu tai lieu Google ngay 20/11/2026: ban v1beta, khoa di bang
     * tham so truy van `key`. Khong ghi khoa ra log o bat cu dau - dung
     * [choThayTrongLoi] khi can hien duong nay cho nguoi dung xem.
     */
    fun duongWs(): String = GOC_WS + "?key=" + khoaApi.trim()

    /** Duong WebSocket da che khoa, dung de hien trong thong bao loi. */
    fun choThayTrongLoi(): String = GOC_WS + "?key=***"

    companion object {
        /**
         * Tai lieu Google ngay 20/11/2026 goi day la model Live API mac dinh.
         *
         * O "Model" trong Cai dat la o NHAP TAY co chu dinh: ho doi ten model
         * Live kha thuong xuyen (gemini-2.5-flash-native-audio-preview-*,
         * gemini-3.1-flash-live-preview, gemini-3.8-live...). Doi model ve sau
         * la go lai mot o, khong phai build lai APK.
         */
        const val MODEL_MAC_DINH = "gemini-3.8-live"

        /** Cac giong doc san co. Model native audio dung duoc giong cua TTS. */
        val GIONG_CO_SAN = listOf("Aoede", "Kore", "Puck", "Charon", "Fenrir", "Leda", "Orus", "Zephyr")

        const val GIONG_MAC_DINH = "Aoede"

        private const val GOC_WS =
            "wss://generativelanguage.googleapis.com/ws/" +
                "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }
}

/** Cach xu ly tieng loa lot vao mic. */
enum class BanSongCong(val nhan: String) {
    /** Theo kha nang cua may: co AcousticEchoCanceler thi song cong day du. */
    TU_DONG("Tự động"),

    /** Luon ngung mic khi loa dang phat. Khong ngat loi duoc, nhung khong bao gio vong. */
    LUON_BAT("Luôn bật"),

    /** Luon gui mic. Chi dung khi da do la bo khu vong chay tot. */
    LUON_TAT("Luôn tắt"),
}

private val Context.dataStoreGiongNoi by preferencesDataStore(name = "cau_hinh_giong_noi")

class CauHinhGiongNoiStore(private val ctx: Context) {

    private val kKhoa = stringPreferencesKey("gemini_khoa_api")
    private val kModel = stringPreferencesKey("gemini_model")
    private val kGiong = stringPreferencesKey("gemini_giong_doc")
    private val kGuiMqtt = booleanPreferencesKey("gemini_gui_mqtt")
    private val kBanSongCong = stringPreferencesKey("gemini_ban_song_cong")
    private val kGiayImLang = intPreferencesKey("gemini_giay_im_lang")
    private val kGiayToiDa = intPreferencesKey("gemini_giay_toi_da")

    val cauHinh: Flow<CauHinhGiongNoi> = ctx.dataStoreGiongNoi.data.map { p ->
        CauHinhGiongNoi(
            khoaApi = p[kKhoa] ?: "",
            model = p[kModel] ?: CauHinhGiongNoi.MODEL_MAC_DINH,
            giongDoc = p[kGiong] ?: CauHinhGiongNoi.GIONG_MAC_DINH,
            guiMqtt = p[kGuiMqtt] ?: false,
            banSongCong = docBanSongCong(p[kBanSongCong]),
            giayImLangTuDong = p[kGiayImLang] ?: 45,
            giayToiDaPhien = p[kGiayToiDa] ?: 600,
        )
    }

    suspend fun luu(c: CauHinhGiongNoi) {
        ctx.dataStoreGiongNoi.edit { p ->
            p[kKhoa] = c.khoaApi.trim()
            p[kModel] = c.model.trim()
            p[kGiong] = c.giongDoc.trim()
            p[kGuiMqtt] = c.guiMqtt
            p[kBanSongCong] = c.banSongCong.name
            p[kGiayImLang] = c.giayImLangTuDong
            p[kGiayToiDa] = c.giayToiDaPhien
        }
    }

    /**
     * Doc enum tu chuoi da luu.
     *
     * Khong dung `valueOf` thang: doi ten hang so ve sau thi may dang chay se
     * nem IllegalArgumentException ngay luc doc cau hinh, truoc khi app kip ve
     * man hinh nao.
     */
    private fun docBanSongCong(s: String?): BanSongCong =
        BanSongCong.entries.firstOrNull { it.name == s } ?: BanSongCong.TU_DONG
}
