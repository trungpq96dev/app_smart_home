package vn.smarthome.data.baomat

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.LoiBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.dien.KetNoi
import java.security.MessageDigest
import java.security.SecureRandom

object PinHasher {
    const val DEFAULT_SALT = "sh_kiosk_default_salt_v1"

    fun hopLe4So(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }

    fun taoMuoi(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun kiemTra(pin: String, salt: String, expectedHash: String): Boolean {
        if (!hopLe4So(pin) || salt.isBlank() || expectedHash.isBlank()) return false
        val actual = hash(pin, salt).toByteArray(Charsets.UTF_8)
        val expected = expectedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(actual, expected)
    }
}

data class CauHinhBaoMat(
    val manChoBat: Boolean = true,
    val phutChoTuDong: Int = 3,
    val giamSangDem: Boolean = true,
    val gioBatDauDem: Int = 22,
    val gioKetThucDem: Int = 6,
    val doSangDem: Float = 0.07f,
    val danhThucPir: Boolean = true,
    val topicPir: String = "smarthome/sensor/pir",
    val topicGas: String = "smarthome/sensor/gas",
    val nguongGasPpm: Int = 200,
    val khoaPinBat: Boolean = true,
    val pinSalt: String = PinHasher.DEFAULT_SALT,
    val pinHash: String = PinHasher.hash("1234", PinHasher.DEFAULT_SALT),
)

data class GasMqttState(
    val alert: Boolean = false,
    val ppm: Int? = null,
)

data class SuCoKhanCap(
    val key: String,
    val loai: String, // "gas" | "pump" | "server"
    val tieuDe: String,
    val chiTiet: String,
    val routeDich: String,
)

object ManChoLogic {
    fun laKhungGioDem(gioHienTai: Int, batDau: Int = 22, ketThuc: Int = 6): Boolean {
        val h = ((gioHienTai % 24) + 24) % 24
        val s = ((batDau % 24) + 24) % 24
        val e = ((ketThuc % 24) + 24) % 24
        if (s == e) return true
        return if (s > e) h >= s || h < e else h in s until e
    }

    fun parsePirMotion(payload: String): Boolean {
        val s = payload.trim()
        if (s.isEmpty()) return false
        return when (s.lowercase()) {
            "1", "true", "on", "motion", "detected", "occupied" -> true
            "0", "false", "off", "clear", "none" -> false
            else -> runCatching {
                val o = JSONObject(s)
                o.optBoolean("motion", false) ||
                    o.optBoolean("detected", false) ||
                    o.optBoolean("occupancy", false) ||
                    o.optBoolean("pir", false) ||
                    o.optInt("pir", 0) == 1 ||
                    o.optString("state", "").equals("motion", ignoreCase = true) ||
                    o.optString("state", "").equals("ON", ignoreCase = true)
            }.getOrDefault(false)
        }
    }

    fun parseGasAlert(payload: String, nguongPpm: Int = 200): GasMqttState {
        val s = payload.trim()
        if (s.isEmpty()) return GasMqttState()
        val lower = s.lowercase()
        if (lower in setOf("1", "true", "on", "leak", "danger", "alert")) {
            return GasMqttState(alert = true, ppm = null)
        }
        if (lower in setOf("0", "false", "off", "safe", "normal", "ok")) {
            return GasMqttState(alert = false, ppm = null)
        }
        s.toDoubleOrNull()?.let { n ->
            val ppm = n.toInt()
            return GasMqttState(alert = ppm >= nguongPpm, ppm = ppm)
        }
        return runCatching {
            val o = JSONObject(s)
            val ppm = when {
                o.has("ppm") && !o.isNull("ppm") -> o.optDouble("ppm").takeIf { it.isFinite() }?.toInt()
                o.has("gasPpm") && !o.isNull("gasPpm") -> o.optDouble("gasPpm").takeIf { it.isFinite() }?.toInt()
                o.has("gas_ppm") && !o.isNull("gas_ppm") -> o.optDouble("gas_ppm").takeIf { it.isFinite() }?.toInt()
                else -> null
            }
            val flag = o.optBoolean("alert", false) ||
                o.optBoolean("leak", false) ||
                o.optBoolean("gas_alert", false) ||
                o.optBoolean("gasAlert", false)
            GasMqttState(
                alert = flag || (ppm != null && ppm >= nguongPpm),
                ppm = ppm,
            )
        }.getOrDefault(GasMqttState())
    }

    fun kiemTraSuCoKhanCap(
        kitchenGasAlert: Boolean,
        gasMqtt: GasMqttState,
        bomNuoc: BomNuocState?,
        ketNoiBom: TrangThaiKetNoiBom,
        ketNoiMqtt: KetNoi,
    ): SuCoKhanCap? {
        if (gasMqtt.alert || kitchenGasAlert) {
            val ppmNote = gasMqtt.ppm?.let { " (${it} ppm)" }.orEmpty()
            return SuCoKhanCap(
                key = "gas:${gasMqtt.ppm ?: if (kitchenGasAlert) "kitchen" else "mqtt"}",
                loai = "gas",
                tieuDe = "Cảnh báo rò rỉ Gas!",
                chiTiet = "Phát hiện khí gas vượt ngưỡng an toàn$ppmNote. Mở cửa thông gió ngay.",
                routeDich = "sensors",
            )
        }

        if (bomNuoc != null) {
            val fault = LoiBomNuoc.phanTich(bomNuoc.fault)
            if (bomNuoc.dangKhoa || bomNuoc.bomApDangKhoa || fault?.mucDo == LoiBomNuoc.MucDo.NGUY_HIEM) {
                return SuCoKhanCap(
                    key = "pump:${bomNuoc.fault ?: "locked"}:${bomNuoc.locked}:${bomNuoc.bstLock}",
                    loai = "pump",
                    tieuDe = "Bo bơm nước đang khoá bảo vệ!",
                    chiTiet = fault?.noiDung ?: "Hệ thống bơm vừa gặp sự cố chạy cạn hoặc quá nhiệt.",
                    routeDich = "automation",
                )
            }
        }

        if (ketNoiBom is TrangThaiKetNoiBom.BoOffline) {
            return SuCoKhanCap(
                key = "pump:offline",
                loai = "pump",
                tieuDe = "Bo bơm nước mất kết nối!",
                chiTiet = "Không liên lạc được với bo bơm ESP32.",
                routeDich = "automation",
            )
        }

        if (ketNoiMqtt is KetNoi.Loi) {
            return SuCoKhanCap(
                key = "server:mqtt_loi:${ketNoiMqtt.moTa}",
                loai = "server",
                tieuDe = "Máy chủ MQTT ngoại tuyến!",
                chiTiet = ketNoiMqtt.moTa,
                routeDich = "server",
            )
        }

        return null
    }
}

private val Context.dsBaoMat: DataStore<Preferences> by preferencesDataStore(name = "cau_hinh_bao_mat")

class CauHinhBaoMatStore(private val ctx: Context) {

    private object Kho {
        val MAN_CHO_BAT = booleanPreferencesKey("man_cho_bat")
        val PHUT_CHO = intPreferencesKey("phut_cho")
        val GIAM_SANG_DEM = booleanPreferencesKey("giam_sang_dem")
        val GIO_BD_DEM = intPreferencesKey("gio_bd_dem")
        val GIO_KT_DEM = intPreferencesKey("gio_kt_dem")
        val DO_SANG_DEM = floatPreferencesKey("do_sang_dem")
        val DANH_THUC_PIR = booleanPreferencesKey("danh_thuc_pir")
        val TOPIC_PIR = stringPreferencesKey("topic_pir")
        val TOPIC_GAS = stringPreferencesKey("topic_gas")
        val NGUONG_GAS = intPreferencesKey("nguong_gas")
        val KHOA_PIN_BAT = booleanPreferencesKey("khoa_pin_bat")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val cauHinh: Flow<CauHinhBaoMat> = ctx.dsBaoMat.data.map { p ->
        val def = CauHinhBaoMat()
        CauHinhBaoMat(
            manChoBat = p[Kho.MAN_CHO_BAT] ?: def.manChoBat,
            phutChoTuDong = (p[Kho.PHUT_CHO] ?: def.phutChoTuDong).coerceIn(1, 60),
            giamSangDem = p[Kho.GIAM_SANG_DEM] ?: def.giamSangDem,
            gioBatDauDem = (p[Kho.GIO_BD_DEM] ?: def.gioBatDauDem).coerceIn(0, 23),
            gioKetThucDem = (p[Kho.GIO_KT_DEM] ?: def.gioKetThucDem).coerceIn(0, 23),
            doSangDem = (p[Kho.DO_SANG_DEM] ?: def.doSangDem).coerceIn(0.03f, 0.30f),
            danhThucPir = p[Kho.DANH_THUC_PIR] ?: def.danhThucPir,
            topicPir = p[Kho.TOPIC_PIR]?.takeIf { it.isNotBlank() } ?: def.topicPir,
            topicGas = p[Kho.TOPIC_GAS]?.takeIf { it.isNotBlank() } ?: def.topicGas,
            nguongGasPpm = (p[Kho.NGUONG_GAS] ?: def.nguongGasPpm).coerceIn(50, 5000),
            khoaPinBat = p[Kho.KHOA_PIN_BAT] ?: def.khoaPinBat,
            pinSalt = p[Kho.PIN_SALT]?.takeIf { it.isNotBlank() } ?: def.pinSalt,
            pinHash = p[Kho.PIN_HASH]?.takeIf { it.isNotBlank() } ?: def.pinHash,
        )
    }

    suspend fun luu(c: CauHinhBaoMat) {
        ctx.dsBaoMat.edit { p ->
            p[Kho.MAN_CHO_BAT] = c.manChoBat
            p[Kho.PHUT_CHO] = c.phutChoTuDong.coerceIn(1, 60)
            p[Kho.GIAM_SANG_DEM] = c.giamSangDem
            p[Kho.GIO_BD_DEM] = c.gioBatDauDem.coerceIn(0, 23)
            p[Kho.GIO_KT_DEM] = c.gioKetThucDem.coerceIn(0, 23)
            p[Kho.DO_SANG_DEM] = c.doSangDem.coerceIn(0.03f, 0.30f)
            p[Kho.DANH_THUC_PIR] = c.danhThucPir
            p[Kho.TOPIC_PIR] = c.topicPir.trim().ifBlank { "smarthome/sensor/pir" }
            p[Kho.TOPIC_GAS] = c.topicGas.trim().ifBlank { "smarthome/sensor/gas" }
            p[Kho.NGUONG_GAS] = c.nguongGasPpm.coerceIn(50, 5000)
            p[Kho.KHOA_PIN_BAT] = c.khoaPinBat
            p[Kho.PIN_SALT] = c.pinSalt
            p[Kho.PIN_HASH] = c.pinHash
        }
    }
}

