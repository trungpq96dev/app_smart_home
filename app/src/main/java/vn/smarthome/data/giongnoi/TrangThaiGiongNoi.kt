package vn.smarthome.data.giongnoi

/**
 * Mot dong tren bang thoai.
 *
 * [cuaMinh] = cau nguoi dung noi; nguoc lai la cau Gemini noi. Lay tu
 * `inputTranscription` / `outputTranscription` cua Live API chu khong phai
 * app tu doan - nen cau hien len dung la cau Gemini DA NGHE duoc, rat co ich
 * khi no hieu sai ten phong.
 */
data class DongThoai(
    val cuaMinh: Boolean,
    val cau: String,
)

/** Mot lenh dieu khien da chay xong, hien thanh mot dong nho duoi bang thoai. */
data class ViecDaLam(
    val cau: String,
    val thanhCong: Boolean,
    /** True khi thiet bi that chua ton tai - de bang thoai noi ro, khong gia vo. */
    val chuaCoThietBiThat: Boolean,
)

/** Trang thai phien noi chuyen. */
sealed interface TrangThaiGiongNoi {

    /** Chua bat. Trang thai duy nhat luc app vua mo. */
    data object Tat : TrangThaiGiongNoi

    /** Dang bat tay WebSocket va cho `setupComplete`. */
    data object DangNoi : TrangThaiGiongNoi

    /** Da san sang, mic dang mo, chua ai noi gi. */
    data object DangNghe : TrangThaiGiongNoi

    /** Gemini dang noi - luc nay mic co the bi ngung neu dang ban song cong. */
    data object DangTraLoi : TrangThaiGiongNoi

    /**
     * Hong. [cau] viet cho nguoi doc, khong phai ma loi - giong cach
     * [vn.smarthome.data.dien.MqttDien] va [vn.smarthome.data.camera.Go2rtc]
     * dich loi truoc khi dua len man hinh.
     */
    data class Loi(val cau: String) : TrangThaiGiongNoi

    val dangChay: Boolean
        get() = this is DangNoi || this is DangNghe || this is DangTraLoi
}

/** Toan bo thu bang thoai can ve. Gom mot cuc de AppShell chi phai doc mot flow. */
data class PhienGiongNoi(
    val trangThai: TrangThaiGiongNoi = TrangThaiGiongNoi.Tat,
    val thoai: List<DongThoai> = emptyList(),
    val daLam: List<ViecDaLam> = emptyList(),
    /** Giay da troi cua phien, de hien dong ho va ap tran. */
    val giayDaChay: Int = 0,
    /** Dang chay o che do ban song cong hay khong - hien mot chu nho tren bang. */
    val banSongCong: Boolean = false,
) {
    val dangChay: Boolean get() = trangThai.dangChay

    companion object {
        /** Giu bao nhieu dong thoai tren man. Man cao 601dp, khong hien duoc nhieu. */
        const val TOI_DA_DONG_THOAI = 8
    }
}
