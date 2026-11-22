package vn.smarthome.data.bomnuoc

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cau hinh ket noi toi bo bom nuoc ESP32 (`bom_nuoc`).
 *
 * File DataStore RIENG (`cau_hinh_bom_nuoc`), tach biet voi `cau_hinh`
 * (Mosquitto noi bo) vi bo bom thuong dung EMQX Cloud TLS cong 8883 hoac
 * goi truc tiep qua HTTP trong LAN. Mat khau KHONG duoc nhung san trong ma
 * nguon va da duoc loai khoi sao luu dam may qua thu muc `datastore/`.
 */
data class CauHinhBomNuoc(
    val espHost: String = "",
    val uuTienLan: Boolean = true,
    val brokerHost: String = "",
    val brokerPort: Int = 8883,
    val brokerTls: Boolean = true,
    val brokerUser: String = "",
    val brokerPass: String = "",
) {
    val coLan: Boolean get() = espHost.isNotBlank()
    val coMqtt: Boolean get() = brokerHost.isNotBlank() && brokerPort in 1..65535
    val daCauHinh: Boolean get() = coLan || coMqtt

    fun mqttUri(): String =
        (if (brokerTls) "ssl://" else "tcp://") + brokerHost.trim() + ":" + brokerPort
}

private val Context.dsBomNuoc: DataStore<Preferences> by preferencesDataStore(name = "cau_hinh_bom_nuoc")

class CauHinhBomNuocStore(private val ctx: Context) {

    private object Kho {
        val ESP_HOST = stringPreferencesKey("bom_esp_host")
        val UU_TIEN_LAN = booleanPreferencesKey("bom_uu_tien_lan")
        val BROKER_HOST = stringPreferencesKey("bom_broker_host")
        val BROKER_PORT = intPreferencesKey("bom_broker_port")
        val BROKER_TLS = booleanPreferencesKey("bom_broker_tls")
        val BROKER_USER = stringPreferencesKey("bom_broker_user")
        val BROKER_PASS = stringPreferencesKey("bom_broker_pass")
    }

    val cauHinh: Flow<CauHinhBomNuoc> = ctx.dsBomNuoc.data.map { p ->
        CauHinhBomNuoc(
            espHost = p[Kho.ESP_HOST].orEmpty(),
            uuTienLan = p[Kho.UU_TIEN_LAN] ?: true,
            brokerHost = p[Kho.BROKER_HOST].orEmpty(),
            brokerPort = p[Kho.BROKER_PORT] ?: 8883,
            brokerTls = p[Kho.BROKER_TLS] ?: true,
            brokerUser = p[Kho.BROKER_USER].orEmpty(),
            brokerPass = p[Kho.BROKER_PASS].orEmpty(),
        )
    }

    suspend fun luu(c: CauHinhBomNuoc) {
        ctx.dsBomNuoc.edit { p ->
            p[Kho.ESP_HOST] = chuanHoaHost(c.espHost)
            p[Kho.UU_TIEN_LAN] = c.uuTienLan
            p[Kho.BROKER_HOST] = chuanHoaHost(c.brokerHost)
            p[Kho.BROKER_PORT] = if (c.brokerPort in 1..65535) c.brokerPort else 8883
            p[Kho.BROKER_TLS] = c.brokerTls
            p[Kho.BROKER_USER] = c.brokerUser.trim()
            p[Kho.BROKER_PASS] = c.brokerPass
        }
    }

    companion object {
        fun chuanHoaHost(raw: String): String = raw.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("wss://")
            .removePrefix("ws://")
            .substringBefore('/')
            .trim()
    }
}
