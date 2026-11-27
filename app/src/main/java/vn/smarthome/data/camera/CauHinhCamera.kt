package vn.smarthome.data.camera

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cho app biet tim go2rtc va dich vu dau ghi 24/24 (camera_recorder) o dau.
 *
 * go2rtc la cai cau: no dang nhap tai khoan Tuya, keo luong camera ve, roi
 * phat lai thanh WebRTC cho ai hoi. App chi noi toi go2rtc, KHONG bao gio
 * dang nhap Tuya truc tiep - tai khoan Tuya chi nam trong go2rtc.yaml tren
 * may chay go2rtc.
 *
 * Cung tren may serverubuntu do, dich vu `camera_recorder` mo cong 1985 de
 * phuc vu Xem lai Timeline 24/24 + danh sach su kien phat hien nguoi.
 */
data class CauHinhCamera(
    val host: String = "192.168.0.50",
    val cong: Int = 1984,
    val congGhiHinh: Int = 1985,
    val tls: Boolean = false,
    val taiKhoan: String = "",
    val matKhau: String = "",
) {
    fun dayDu(): Boolean = host.isNotBlank() && cong in 1..65535

    fun goc(): String =
        (if (tls) "https://" else "http://") + host.trim().trimEnd('/') + ":" + cong

    fun gocGhiHinh(congOverride: Int? = null): String {
        val p = (congOverride ?: congGhiHinh).takeIf { it in 1..65535 } ?: 1985
        return (if (tls) "https://" else "http://") + host.trim().trimEnd('/') + ":" + p
    }

    fun headerBasicAuth(): Map<String, String> {
        if (taiKhoan.isBlank()) return emptyMap()
        val tho = "$taiKhoan:$matKhau".toByteArray(Charsets.UTF_8)
        val ma = Base64.encodeToString(tho, Base64.NO_WRAP)
        return mapOf("Authorization" to "Basic $ma")
    }

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

    fun urlDanhSachNgay(tenLuong: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/days?cam=" + Uri.encode(tenLuong)

    fun urlDongThoiGian(tenLuong: String, ngay: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/timeline?cam=" + Uri.encode(tenLuong) + "&date=" + Uri.encode(ngay)

    fun urlVideoSegment(segId: Int, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/video/" + segId

    fun urlThumbnail(eventId: Int, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/thumb/" + eventId + ".jpg"

    fun urlChupAnh(tenLuong: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/snapshot?cam=" + Uri.encode(tenLuong)

    fun urlSuKien(tenLuong: String?, kind: String = "all", limit: Int = 60, congOverride: Int? = null): String {
        val params = buildList {
            if (!tenLuong.isNullOrBlank()) add("cam=" + Uri.encode(tenLuong))
            if (kind.isNotBlank() && kind != "all") add("kind=" + Uri.encode(kind))
            add("limit=$limit")
        }.joinToString("&")
        return gocGhiHinh(congOverride) + "/api/events?" + params
    }

    fun urlClipSuKien(eventId: Int, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/clip/" + eventId + ".mp4"

    fun urlAnhMat(snapshotUrl: String, congOverride: Int? = null): String {
        val s = snapshotUrl.trim()
        if (s.startsWith("http://") || s.startsWith("https://")) return s
        val duong = if (s.startsWith("/")) s else "/$s"
        return gocGhiHinh(congOverride) + duong
    }

    fun urlPtzPresets(tenLuong: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/ptz/presets?cam=" + Uri.encode(tenLuong)

    fun urlPtzMove(tenLuong: String, direction: String, speed: Float = 0.5f, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/ptz/move?cam=" + Uri.encode(tenLuong) +
            "&dir=" + Uri.encode(direction) + "&speed=$speed"

    fun urlPtzPreset(tenLuong: String, preset: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/ptz/preset?cam=" + Uri.encode(tenLuong) +
            "&preset=" + Uri.encode(preset)

    fun urlPrivacy(bat: Boolean, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/privacy?enabled=" + if (bat) "1" else "0"

    fun urlHomeMode(mode: String, congOverride: Int? = null): String =
        gocGhiHinh(congOverride) + "/api/home_mode?mode=" + Uri.encode(mode)

    /**
     * Chon luong phu `_sd` (H.264) cho WebView tren Tab A7 Lite neu go2rtc co khai bao song song
     * luong chinh (HD) va luong phu (`<ten>_sd`), tranh den hinh H.265 va tiet kiem RAM.
     */
    fun tenLuongWebView(tenLuong: String, dsTatCaLuong: Collection<String>): String {
        if (tenLuong.endsWith("_sd")) return tenLuong
        val sd = "${tenLuong}_sd"
        return if (sd in dsTatCaLuong) sd else tenLuong
    }
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
    private val kCongGhiHinh = intPreferencesKey("cam_cong_ghi_hinh")
    private val kTls = booleanPreferencesKey("cam_tls")
    private val kTaiKhoan = stringPreferencesKey("cam_tai_khoan")
    private val kMatKhau = stringPreferencesKey("cam_mat_khau")

    val cauHinh: Flow<CauHinhCamera> = ctx.dsCamera.data.map { p ->
        CauHinhCamera(
            host = p[kHost] ?: "192.168.0.50",
            cong = p[kCong] ?: 1984,
            congGhiHinh = p[kCongGhiHinh] ?: 1985,
            tls = p[kTls] ?: false,
            taiKhoan = p[kTaiKhoan] ?: "",
            matKhau = p[kMatKhau] ?: "",
        )
    }

    suspend fun luu(c: CauHinhCamera) {
        ctx.dsCamera.edit { p ->
            p[kHost] = c.host.trim()
            p[kCong] = c.cong
            p[kCongGhiHinh] = c.congGhiHinh
            p[kTls] = c.tls
            p[kTaiKhoan] = c.taiKhoan.trim()
            p[kMatKhau] = c.matKhau
        }
    }
}
