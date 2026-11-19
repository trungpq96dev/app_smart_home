package vn.smarthome.data.thoitiet

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dia diem xem thoi tiet.
 *
 * Mac dinh la Ha Noi (lay tu [OpenMeteo]). Nguoi dung doi duoc trong Cai dat
 * khi dat may o tinh khac - khong can sua ma nguon nua. Open-Meteo nhan thang
 * toa do nen chi can lat/lon; truong [ten] chi de hien tren the.
 */
data class CauHinhThoiTiet(
    val ten: String = OpenMeteo.TEN_DIA_DIEM,
    val lat: Double = OpenMeteo.LAT,
    val lon: Double = OpenMeteo.LON,
) {
    fun hopLe(): Boolean =
        ten.isNotBlank() && lat in -90.0..90.0 && lon in -180.0..180.0
}

private val Context.dsThoiTiet by preferencesDataStore(name = "cau_hinh_thoitiet")

/**
 * Luu dia diem thoi tiet tren may.
 *
 * File DataStore RIENG, giong cach cau hinh camera tach khoi cau hinh MQTT:
 * ba thu khong lien quan nhau, mot file hong khong keo theo file kia. O day
 * khong co gi bi mat (chi la toa do) nen khong dong toi quy tac sao luu.
 */
class CauHinhThoiTietStore(private val ctx: Context) {

    private val kTen = stringPreferencesKey("tt_ten")
    private val kLat = doublePreferencesKey("tt_lat")
    private val kLon = doublePreferencesKey("tt_lon")

    val cauHinh: Flow<CauHinhThoiTiet> = ctx.dsThoiTiet.data.map { p ->
        CauHinhThoiTiet(
            ten = p[kTen] ?: OpenMeteo.TEN_DIA_DIEM,
            lat = p[kLat] ?: OpenMeteo.LAT,
            lon = p[kLon] ?: OpenMeteo.LON,
        )
    }

    suspend fun luu(c: CauHinhThoiTiet) {
        ctx.dsThoiTiet.edit { p ->
            p[kTen] = c.ten.trim()
            p[kLat] = c.lat
            p[kLon] = c.lon
        }
    }
}
