package vn.smarthome.data.dien

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cau hinh noi toi EMQX.
 *
 * Mac dinh cong 8883 + TLS theo tai lieu ban giao dien_evn muc 4 buoc 4.
 */
data class CauHinhMqtt(
    val host: String = "",
    val cong: Int = 8883,
    val tls: Boolean = true,
    val taiKhoan: String = "",
    val matKhau: String = "",
    val clientId: String = "",
) {
    fun dayDu(): Boolean = host.isNotBlank() && cong in 1..65535

    fun uri(): String = (if (tls) "ssl://" else "tcp://") + host.trim() + ":" + cong
}

private val Context.dataStore by preferencesDataStore(name = "cau_hinh")

/**
 * Luu cau hinh tren may.
 *
 * Mat khau broker nam trong DataStore cua app va da duoc loai khoi sao luu
 * dam may (xem res/xml/backup_rules.xml va data_extraction_rules.xml).
 * App KHONG nhung san mat khau nao trong ma nguon - vi ma nguon nay nam tren
 * GitHub.
 */
class CauHinhStore(private val ctx: Context) {

    private val kHost = stringPreferencesKey("mqtt_host")
    private val kCong = intPreferencesKey("mqtt_cong")
    private val kTls = booleanPreferencesKey("mqtt_tls")
    private val kTaiKhoan = stringPreferencesKey("mqtt_tai_khoan")
    private val kMatKhau = stringPreferencesKey("mqtt_mat_khau")
    private val kClientId = stringPreferencesKey("mqtt_client_id")

    val cauHinh: Flow<CauHinhMqtt> = ctx.dataStore.data.map { p ->
        CauHinhMqtt(
            host = p[kHost] ?: "",
            cong = p[kCong] ?: 8883,
            tls = p[kTls] ?: true,
            taiKhoan = p[kTaiKhoan] ?: "",
            matKhau = p[kMatKhau] ?: "",
            clientId = p[kClientId] ?: "",
        )
    }

    suspend fun luu(c: CauHinhMqtt) {
        ctx.dataStore.edit { p ->
            p[kHost] = c.host.trim()
            p[kCong] = c.cong
            p[kTls] = c.tls
            p[kTaiKhoan] = c.taiKhoan.trim()
            p[kMatKhau] = c.matKhau
            p[kClientId] = c.clientId.trim()
        }
    }
}
