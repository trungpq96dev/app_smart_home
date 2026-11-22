package vn.smarthome.data.bomnuoc

/**
 * Chuyen chuoi `fault` TIENG VIET KHONG DAU cua firmware `bom_nuoc` sang tieng Viet
 * co dau de hien thi, dong thoi giu nguyen cac tham so so (so phut, nhiet do).
 * Neu khong khop mau nao thi tra lai dung chuoi goc, khong nuot mat loi moi.
 */
object LoiBomNuoc {

    enum class MucDo { CANH_BAO, NGUY_HIEM }

    data class ChiTietLoi(
        val goc: String,
        val noiDung: String,
        val mucDo: MucDo,
        val xoaDuoc: Boolean,
    )

    private data class QuyTac(
        val khop: String,
        val mucDo: MucDo,
        val xoaDuoc: Boolean,
        val taoCau: (String) -> String,
    )

    private val quyTac = listOf(
        QuyTac("Bom len bon chay nhung khong co nuoc", MucDo.NGUY_HIEM, true) {
            "Bơm lên bồn chạy nhưng không có nước. Đã tắt và khoá."
        },
        QuyTac("Bom len bon chay qua", MucDo.NGUY_HIEM, true) { raw ->
            val phut = soGiua(raw, "qua ", " phut")
            "Bơm lên bồn chạy quá ${phut ?: "?"} phút mà chưa đầy. Đã tắt và khoá."
        },
        QuyTac("Bom len bon qua nong", MucDo.NGUY_HIEM, true) { raw ->
            val nhiet = soGiua(raw, "(", " do C")
            "Bơm lên bồn quá nóng (${nhiet ?: "?"} °C). Đã tắt và khoá."
        },
        QuyTac("Bom ap chay lien tuc qua", MucDo.NGUY_HIEM, true) { raw ->
            val phut = soGiua(raw, "qua ", " phut")
            "Bơm tăng áp chạy liên tục quá ${phut ?: "?"} phút. Đã cắt nguồn và khoá để kiểm tra."
        },
        QuyTac("Bom ap qua nong", MucDo.NGUY_HIEM, true) { raw ->
            val nhiet = soGiua(raw, "(", " do C")
            "Bơm tăng áp quá nóng (${nhiet ?: "?"} °C). Đã cắt nguồn và khoá."
        },
        QuyTac("Bom ap dang khoa vi su co lan truoc", MucDo.CANH_BAO, true) {
            "Bơm tăng áp đang khoá vì sự cố lần trước. Bấm Xoá lỗi để mở."
        },
        QuyTac("Dang khoa vi lan truoc bom chay can", MucDo.CANH_BAO, true) {
            "Đang khoá vì lần trước bơm chạy cạn. Bấm Xoá lỗi để mở."
        },
        QuyTac("Dang khoa vi lan truoc bom chay qua gio", MucDo.CANH_BAO, true) {
            "Đang khoá vì lần trước bơm chạy quá giờ. Bấm Xoá lỗi để mở."
        },
        QuyTac("Hai moc muc nuoc bi dao nguoc", MucDo.CANH_BAO, true) {
            "Hai mốc mức nước bị đảo ngược. Vào mục Bảo vệ, ghi lại mốc CẠN lúc bồn cạn và mốc ĐẦY lúc bồn đầy."
        },
        QuyTac("Nap firmware bi dut giua chung", MucDo.CANH_BAO, true) {
            "Nạp firmware bị đứt giữa chừng. Firmware cũ vẫn nguyên vẹn."
        },
    )

    fun phanTich(raw: String?): ChiTietLoi? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null

        val match = quyTac.firstOrNull { s.startsWith(it.khop) || s.contains(it.khop) }
        return if (match != null) {
            ChiTietLoi(s, match.taoCau(s), match.mucDo, match.xoaDuoc)
        } else {
            ChiTietLoi(s, s, MucDo.NGUY_HIEM, true)
        }
    }

    private fun soGiua(s: String, sau: String, truoc: String): String? {
        val i = s.indexOf(sau)
        if (i < 0) return null
        val start = i + sau.length
        val j = s.indexOf(truoc, start)
        if (j < 0) return null
        return s.substring(start, j).trim().ifBlank { null }
    }
}
