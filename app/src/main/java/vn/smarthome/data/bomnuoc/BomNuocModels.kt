package vn.smarthome.data.bomnuoc

import org.json.JSONArray
import org.json.JSONObject

/** Topic MQTT co dinh trong firmware `bom_nuoc` (`config.h:144-147`). */
object BomNuocTopic {
    const val STATE = "bomnuoc/state"
    const val CMD = "bomnuoc/cmd"
    const val STATUS = "bomnuoc/status"
    const val USAGE = "bomnuoc/usage"
    const val SYS = "bomnuoc/sys"
}

enum class CheDoBon(val cmd: String, val nhan: String) {
    AUTO("auto", "tự động"),
    BAT("on", "bật tay"),
    TAT("off", "tắt tay"),
    KHONG_RO("", "—");

    companion object {
        fun tuChuoi(s: String?): CheDoBon = when (s?.trim()?.lowercase()) {
            "auto" -> AUTO
            "on" -> BAT
            "off" -> TAT
            else -> KHONG_RO
        }
    }
}

enum class CheDoTangAp(val cmd: String, val nhan: String) {
    BAT("on", "bật"),
    TAT("off", "tắt"),
    KHONG_RO("", "—");

    companion object {
        fun tuChuoi(s: String?): CheDoTangAp = when (s?.trim()?.lowercase()) {
            "on" -> BAT
            "off" -> TAT
            else -> KHONG_RO
        }
    }
}

data class CamBienNhiet(
    val ten: String = "",
    val giaTriTho: Double = -127.0,
) {
    /** Firmware gui -127.0 (DEVICE_DISCONNECTED_C) khi khong co cam bien. */
    val giaTri: Double? get() = if (giaTriTho > -100.0) giaTriTho else null
    val ketNoi: Boolean get() = giaTri != null
}

data class ChanIo(
    val ten: String = "",
    val gpio: Int = -1,
    val muc: Int = 0,
) {
    val cao: Boolean get() = muc == 1
}

/** Mot thang trong mang `/api/usage` hoac topic `bomnuoc/usage` (toi da 12 thang, moi nhat truoc). */
data class LuongNuocThang(
    val thang: String = "",
    val bomLit: Double = 0.0,
    val nuocMayLit: Double? = null,
) {
    val laThangChuaRo: Boolean get() = thang == "chua ro thang" || thang.isBlank()

    val nhanNgan: String
        get() = if (laThangChuaRo) "?" else
            thang.substringAfter('-', "").toIntOrNull()?.let { "T$it" } ?: thang

    val tongLit: Double get() = bomLit + (nuocMayLit ?: 0.0)

    companion object {
        fun tuJsonArray(raw: String): List<LuongNuocThang>? = runCatching {
            val arr = JSONArray(raw.trim())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val m = o.optStringOrNull("m").orEmpty()
                    val t = o.optDoubleOrNull("t") ?: 0.0
                    val c = o.optDoubleOrNull("c")
                    add(LuongNuocThang(thang = m, bomLit = t, nuocMayLit = c))
                }
            }
        }.getOrNull()
    }
}

/**
 * Trang thai day du cua bo `bom_nuoc` - khop voi `stateJson()` trong firmware.
 * Moi truong deu nullable de an toan khi firmware tat bot tinh nang luc bien dich.
 */
data class BomNuocState(
    val up: Long? = null,
    val fw: String? = null,
    val hb: Int? = null,

    val rssi: Int? = null,
    val ssid: String? = null,
    val ip: String? = null,

    val aboveLow: Int? = null,
    val isFull: Int? = null,
    val lvl: Double? = null,
    val litres: Double? = null,
    val lvlRaw: Long? = null,
    val lvlE: Long? = null,
    val lvlF: Long? = null,

    val tankOn: Int? = null,
    val tankMode: String? = null,
    val tankRun: Long? = null,
    val fTank: Double? = null,

    val boostOn: Int? = null,
    val boostRun: Long? = null,
    val bstMode: String? = null,
    val bstLock: Int? = null,

    val fCity: Double? = null,
    val useC: Double? = null,
    val cityPulses: Double? = null,

    val den: Int? = null,
    val locked: Int? = null,
    val fault: String? = null,
    val temp: Double? = null,

    val dryCheck: Long? = null,
    val minRest: Double? = null,
    val maxRun: Long? = null,
    val pulses: Double? = null,
    val minFlow: Double? = null,

    val calPT: Long? = null,
    val calLT: Double? = null,

    val useM: String? = null,
    val useT: Double? = null,

    val temps: List<CamBienNhiet>? = null,
    val io: List<ChanIo>? = null,
) {
    /** lvl = -1 nghia la chua hieu chuan hoac mat ADS1115. Tuyet doi khong doan theo phao. */
    val coCamBienMuc: Boolean get() = lvl != null && lvl >= 0.0

    val mucNuocPhanTram: Double? get() = if (coCamBienMuc) lvl!!.coerceIn(0.0, 100.0) else null

    val mucNuocLit: Double? get() = if (litres != null && litres >= 0.0) litres else null

    val nhietDoNuoc: Double? get() = if (temp != null && temp > -100.0) temp else null

    val nhietDoBomBon: Double? get() = temps?.getOrNull(1)?.giaTri

    val nhietDoBomAp: Double? get() = temps?.getOrNull(2)?.giaTri

    /** rssi = 0 nghia la mat WiFi, khong phai song cuc manh. */
    val wifiRssi: Int? get() = if (rssi != null && rssi != 0) rssi else null

    val coLoi: Boolean get() = !fault.isNullOrBlank()

    val bomChinhDangChay: Boolean get() = tankOn == 1
    val bomApDangChay: Boolean get() = boostOn == 1
    val dangKhoa: Boolean get() = locked == 1
    val bomApDangKhoa: Boolean get() = bstLock == 1
    val bonDay: Boolean get() = isFull == 1
    val denPhuBat: Boolean get() = den == 1

    val cheDoBon: CheDoBon get() = CheDoBon.tuChuoi(tankMode)
    val cheDoTangAp: CheDoTangAp get() = CheDoTangAp.tuChuoi(bstMode)

    val cheDoPhatSongAp: Boolean get() = ssid?.contains("phat song") == true

    /** Nguong bao im lang bat thuong: hb * 3 + 10 giay. */
    val nguongImLangGiay: Int get() = (hb ?: 5) * 3 + 10

    /** Doc trang thai coi that tu io[] (GPIO 5), khong suy dien theo fault. */
    val coiDangKeu: Boolean? get() = io?.firstOrNull { it.gpio == 5 }?.let { it.muc == 1 }

    val nuocMayDangChay: Boolean get() = (fCity ?: 0.0) > 0.3
    val bomDangCoDong: Boolean get() = (fTank ?: 0.0) > 0.1

    val coNhanhNuocMay: Boolean get() = fCity != null
    val coDenPhu: Boolean get() = den != null

    companion object {
        const val DUNG_TICH_BON_LIT = 1500

        fun tuJson(raw: String): BomNuocState? = runCatching {
            val o = JSONObject(raw.trim())
            BomNuocState(
                up = o.optLongOrNull("up"),
                fw = o.optStringOrNull("fw"),
                hb = o.optIntOrNull("hb"),
                rssi = o.optIntOrNull("rssi"),
                ssid = o.optStringOrNull("ssid"),
                ip = o.optStringOrNull("ip"),
                aboveLow = o.optIntOrNull("aboveLow"),
                isFull = o.optIntOrNull("isFull"),
                lvl = o.optDoubleOrNull("lvl"),
                litres = o.optDoubleOrNull("litres"),
                lvlRaw = o.optLongOrNull("lvlRaw"),
                lvlE = o.optLongOrNull("lvlE"),
                lvlF = o.optLongOrNull("lvlF"),
                tankOn = o.optIntOrNull("tankOn"),
                tankMode = o.optStringOrNull("tankMode"),
                tankRun = o.optLongOrNull("tankRun"),
                fTank = o.optDoubleOrNull("fTank"),
                boostOn = o.optIntOrNull("boostOn"),
                boostRun = o.optLongOrNull("boostRun"),
                bstMode = o.optStringOrNull("bstMode"),
                bstLock = o.optIntOrNull("bstLock"),
                fCity = o.optDoubleOrNull("fCity"),
                useC = o.optDoubleOrNull("useC"),
                cityPulses = o.optDoubleOrNull("cityPulses"),
                den = o.optIntOrNull("den"),
                locked = o.optIntOrNull("locked"),
                fault = o.optStringOrNull("fault"),
                temp = o.optDoubleOrNull("temp"),
                dryCheck = o.optLongOrNull("dryCheck"),
                minRest = o.optDoubleOrNull("minRest"),
                maxRun = o.optLongOrNull("maxRun"),
                pulses = o.optDoubleOrNull("pulses"),
                minFlow = o.optDoubleOrNull("minFlow"),
                calPT = o.optLongOrNull("calPT"),
                calLT = o.optDoubleOrNull("calLT"),
                useM = o.optStringOrNull("useM"),
                useT = o.optDoubleOrNull("useT"),
                temps = o.optJSONArray("temps")?.let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            val t = arr.optJSONObject(i) ?: continue
                            add(
                                CamBienNhiet(
                                    ten = t.optStringOrNull("n").orEmpty(),
                                    giaTriTho = t.optDoubleOrNull("v") ?: -127.0,
                                )
                            )
                        }
                    }
                },
                io = o.optJSONArray("io")?.let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            val pin = arr.optJSONObject(i) ?: continue
                            add(
                                ChanIo(
                                    ten = pin.optStringOrNull("n").orEmpty(),
                                    gpio = pin.optIntOrNull("g") ?: -1,
                                    muc = pin.optIntOrNull("v") ?: 0,
                                )
                            )
                        }
                    }
                },
            )
        }.getOrNull()
    }
}

enum class KenhBomNuoc(val nhanNgan: String) {
    LAN("LAN"),
    MQTT("MQTT"),
    KHONG("—"),
}

sealed interface TrangThaiKetNoiBom {
    val nhan: String

    data object ChuaCauHinh : TrangThaiKetNoiBom {
        override val nhan = "Chưa cấu hình"
    }

    data object DangNoi : TrangThaiKetNoiBom {
        override val nhan = "Đang kết nối…"
    }

    data object ChiCoBroker : TrangThaiKetNoiBom {
        override val nhan =
            "Nối được broker nhưng chưa thấy bo — kiểm tra nguồn bo hoặc tài khoản MQTT"
    }

    data class TrucTiep(val kenh: KenhBomNuoc, val tuoiGiay: Int) : TrangThaiKetNoiBom {
        override val nhan = if (kenh == KenhBomNuoc.LAN) "LAN · 1s" else "MQTT · 30s"
    }

    data class SoCu(val tuoiGiay: Int) : TrangThaiKetNoiBom {
        override val nhan = "Bo không gửi số liệu (${tuoiGiay}s)"
    }

    data object BoOffline : TrangThaiKetNoiBom {
        override val nhan = "Bo đang offline"
    }

    data object MatBroker : TrangThaiKetNoiBom {
        override val nhan = "Mất kết nối broker"
    }

    data object SaiTaiKhoan : TrangThaiKetNoiBom {
        override val nhan = "Sai tài khoản hoặc mật khẩu MQTT"
    }

    data class Loi(val lyDo: String) : TrangThaiKetNoiBom {
        override val nhan = "Lỗi: ${lyDo.ifBlank { "không nối được" }}"
    }
}

sealed interface KetQuaLenhBom {
    data object DaXacNhan : KetQuaLenhBom
    data object ChuaXacNhan : KetQuaLenhBom
    data class GuiThatBai(val lyDo: String) : KetQuaLenhBom
    data object BiKhoaBaoVe : KetQuaLenhBom
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (has(key) && !isNull(key)) opt(key)?.toString() else null

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val v = when (val raw = opt(key)) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDoubleOrNull()
        else -> null
    } ?: return null
    return if (v.isNaN() || v.isInfinite()) null else v
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (!has(key) || isNull(key)) return null
    return when (val raw = opt(key)) {
        is Number -> raw.toLong()
        is String -> raw.trim().toLongOrNull() ?: raw.trim().toDoubleOrNull()?.toLong()
        else -> null
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? = optLongOrNull(key)?.toInt()
