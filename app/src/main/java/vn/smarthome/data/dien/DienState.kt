package vn.smarthome.data.dien

import org.json.JSONArray
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Hop dong topic cua duong ong dien_evn (serverubuntu -> EMQX -> app).
 *
 * Lay nguyen van tu tai lieu ban giao dien_evn muc 5. KHONG tu doi ten topic:
 * ben publish la script Python chay bang systemd, doi mot ben la dut lien lac
 * ma khong bao loi gi ca.
 *
 * Moi topic deu RETAINED, nen vua subscribe la co so ngay, khong phai cho
 * cu chay ke tiep.
 */
object DienTopic {
    const val GOC = "dien/#"

    const val TRANG_THAI    = "dien/trangthai"       // "ok" | "loi"
    const val NGAY_SAN_LUONG = "dien/ngay/sanluong"  // kWh ngay gan nhat EVN da chot
    const val NGAY          = "dien/ngay/ngay"       // YYYY-MM-DD
    const val THANG_SAN_LUONG = "dien/thang/sanluong" // kWh luy ke tu ngay chot chi so
    const val THANG_TIEN    = "dien/thang/tien"      // tien uoc tinh, da gom VAT
    const val CONGTO_CHI_SO = "dien/congto/chiso"
    const val CONGNO_SO_TIEN = "dien/congno/sotien"
    const val LICH_SU       = "dien/lichsu"          // JSON [{"ngay","kwh"}, ...]
    const val CAP_NHAT      = "dien/capnhat"         // YYYY-MM-DD HH:MM:SS
    const val LOI           = "dien/loi"
}

/** Mot ngay trong dien/lichsu. */
data class NgayKwh(val ngay: String, val kwh: Double)

/**
 * Do tuoi cua so lieu.
 *
 * Timer systemd chay 4 cu/ngay, tuc moi 6 tieng. Nen:
 *   - duoi 8 tieng  : binh thuong (vua lo mot chut cung chua dang lo)
 *   - 8 den 24 tieng: da lo it nhat mot cu, canh bao
 *   - tren 24 tieng : chac chan hong (hoac laptop serverubuntu dang ngu)
 *
 * Tai lieu ban giao muc 5 ghi ro: khong co Last Will, nen CHI co cach nhin
 * tuoi cua dien/capnhat de biet so con moi hay khong.
 */
enum class DoTuoi { MOI, HOI_CU, QUA_CU, CHUA_CO }

/**
 * Toan bo so dien doc tu MQTT.
 *
 * Moi truong deu nullable va mac dinh null. Quy uoc du an: KHONG BIA SO -
 * chua co thi hien "--" chu khong dien so mac dinh.
 */
data class DienState(
    val trangThai: String? = null,
    val ngaySanLuong: Double? = null,
    val ngay: String? = null,
    val thangSanLuong: Double? = null,
    val thangTien: Double? = null,
    val congToChiSo: Double? = null,
    val congNoSoTien: Double? = null,
    val lichSu: List<NgayKwh> = emptyList(),
    val capNhat: String? = null,
    val loi: String? = null,
) {
    /** Da nhan duoc bat ky so lieu nao chua. */
    val coSoLieu: Boolean
        get() = lichSu.isNotEmpty() || ngaySanLuong != null || thangSanLuong != null

    fun doTuoi(bayGio: LocalDateTime = LocalDateTime.now()): DoTuoi {
        val t = capNhatLocalDateTime() ?: return DoTuoi.CHUA_CO
        val gio = Duration.between(t, bayGio).toHours()
        return when {
            gio < 8 -> DoTuoi.MOI
            gio < 24 -> DoTuoi.HOI_CU
            else -> DoTuoi.QUA_CU
        }
    }

    fun capNhatLocalDateTime(): LocalDateTime? = capNhat?.let {
        runCatching { LocalDateTime.parse(it.trim(), DINH_DANG_CAP_NHAT) }.getOrNull()
    }

    /**
     * Cong kWh cua n ngay gan nhat.
     * Tra null khi khong du du lieu - de man hinh hien "--" thay vi cong thieu
     * roi bao nhu that.
     */
    fun tongKwh(soNgay: Int): Double? {
        if (lichSu.isEmpty()) return null
        return lichSu.takeLast(soNgay).sumOf { it.kwh }
    }

    /**
     * Phan tram chenh giua n ngay gan nhat va n ngay lien truoc.
     * Tra null khi chua du 2n ngay - khong doan.
     */
    fun chenhPhanTram(soNgay: Int): Int? {
        if (lichSu.size < soNgay * 2) return null
        val cuoi = lichSu.takeLast(soNgay).sumOf { it.kwh }
        val truoc = lichSu.dropLast(soNgay).takeLast(soNgay).sumOf { it.kwh }
        if (truoc <= 0.0) return null
        return Math.round((cuoi - truoc) / truoc * 100.0).toInt()
    }

    companion object {
        val DINH_DANG_CAP_NHAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /**
         * Gop mot ban tin MQTT vao state.
         *
         * Payload rong duoc hieu la "khong co gia tri" chu khong phai 0 - dac
         * biet quan trong voi dien/loi (rong = khong sao) va dien/congno/sotien.
         */
        fun gop(cu: DienState, topic: String, payload: String): DienState {
            val s = payload.trim()
            val rong = s.isEmpty()
            return when (topic) {
                DienTopic.TRANG_THAI -> cu.copy(trangThai = s.ifEmpty { null })
                DienTopic.NGAY_SAN_LUONG -> cu.copy(ngaySanLuong = so(s))
                DienTopic.NGAY -> cu.copy(ngay = s.ifEmpty { null })
                DienTopic.THANG_SAN_LUONG -> cu.copy(thangSanLuong = so(s))
                DienTopic.THANG_TIEN -> cu.copy(thangTien = so(s))
                DienTopic.CONGTO_CHI_SO -> cu.copy(congToChiSo = so(s))
                DienTopic.CONGNO_SO_TIEN -> cu.copy(congNoSoTien = so(s))
                DienTopic.CAP_NHAT -> cu.copy(capNhat = s.ifEmpty { null })
                DienTopic.LOI -> cu.copy(loi = if (rong) null else s)
                DienTopic.LICH_SU -> cu.copy(lichSu = docLichSu(s))
                else -> cu
            }
        }

        private fun so(s: String): Double? = s.trim().toDoubleOrNull()

        /**
         * Doc dien/lichsu bang org.json cua Android - khong keo them thu vien
         * JSON nao vao APK chi de doc mot mang ~1 KB.
         *
         * Dong nao thieu khoa hoac sai kieu thi BO RIENG dong do, khong vut ca
         * mang: mat mot ngay con hon mat ca bieu do.
         */
        fun docLichSu(json: String): List<NgayKwh> {
            if (json.isBlank()) return emptyList()
            return runCatching {
                val arr = JSONArray(json)
                buildList {
                    for (i in 0 until arr.length()) {
                        val o = arr.optJSONObject(i) ?: continue
                        val ngay = o.optString("ngay").takeIf { it.isNotBlank() } ?: continue
                        if (!o.has("kwh")) continue
                        val kwh = o.optDouble("kwh", Double.NaN)
                        if (kwh.isNaN()) continue
                        add(NgayKwh(ngay, kwh))
                    }
                }
            }.getOrDefault(emptyList())
        }
    }
}
