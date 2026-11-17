package vn.smarthome.data.camera

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cho app biet tim go2rtc o dau.
 *
 * go2rtc la cai cau: no dang nhap tai khoan Tuya, keo luong camera ve, roi
 * phat lai thanh WebRTC cho ai hoi. App chi noi toi go2rtc, KHONG bao gio
 * dang nhap Tuya truc tiep - tai khoan Tuya chi nam trong go2rtc.yaml tren
 * may chay go2rtc.
 *
 * Mac dinh cong 1984 (cong web + api cua go2rtc) va KHONG TLS, vi hien tai
 * go2rtc chay trong mang LAN. Khi chuyen sang duong `tailscale serve` thi
 * bat [tls] va doi cong sang 443.
 */
data class CauHinhCamera(
    val host: String = "",
    val cong: Int = 1984,
    val tls: Boolean = false,
    val taiKhoan: String = "",
    val matKhau: String = "",
) {
    fun dayDu(): Boolean = host.isNotBlank() && cong in 1..65535

    fun goc(): String =
        (if (tls) "https://" else "http://") + host.trim().trimEnd('/') + ":" + cong

    /**
     * Trang phat san co cua go2rtc. No tu chon duong truyen: thu WebRTC
     * truoc, khong duoc thi lui ve MSE - nen khong can app doan.
     *
     * `width=100%` phai viet la `100%25` trong URL, vi `%` trong query la ky
     * tu mo dau ma hoa. De nguyen `%` thi trinh duyet doc `%w` thanh rac va
     * khung hinh tut ve 320px.
     */
    fun urlTrangPhat(tenLuong: String): String =
        goc() + "/stream.html?src=" + Uri.encode(tenLuong) + "&width=100%25"

    fun urlDanhSach(): String = goc() + "/api/streams"
}

private val Context.dsCamera by preferencesDataStore(name = "cau_hinh_camera")

/**
 * Luu cau hinh camera tren may.
 *
 * Dung file DataStore RIENG voi cau hinh MQTT: hai thu khong lien quan gi
 * nhau, va mot file hong thi khong keo theo file kia.
 *
 * Mat khau go2rtc (neu co) da duoc loai khoi sao luu dam may - xem
 * res/xml/backup_rules.xml va data_extraction_rules.xml.
 */
class CauHinhCameraStore(private val ctx: Context) {

    private val kHost = stringPreferencesKey("cam_host")
    private val kCong = intPreferencesKey("cam_cong")
    private val kTls = booleanPreferencesKey("cam_tls")
    private val kTaiKhoan = stringPreferencesKey("cam_tai_khoan")
    private val kMatKhau = stringPreferencesKey("cam_mat_khau")

    val cauHinh: Flow<CauHinhCamera> = ctx.dsCamera.data.map { p ->
        CauHinhCamera(
            host = p[kHost] ?: "",
            cong = p[kCong] ?: 1984,
            tls = p[kTls] ?: false,
            taiKhoan = p[kTaiKhoan] ?: "",
            matKhau = p[kMatKhau] ?: "",
        )
    }

    suspend fun luu(c: CauHinhCamera) {
        ctx.dsCamera.edit { p ->
            p[kHost] = c.host.trim()
            p[kCong] = c.cong
            p[kTls] = c.tls
            p[kTaiKhoan] = c.taiKhoan.trim()
            p[kMatKhau] = c.matKhau
        }
    }
}
