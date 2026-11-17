package vn.smarthome.data.camera

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/** Trang thai duong toi go2rtc. */
sealed interface TrangThaiCam {
    data object ChuaCauHinh : TrangThaiCam
    data object DangDo : TrangThaiCam
    data class Co(val so: Int) : TrangThaiCam
    data class Loi(val moTa: String) : TrangThaiCam
}

/**
 * Hoi go2rtc xem no dang giu nhung luong nao.
 *
 * Khong dung thu vien HTTP nao: `HttpURLConnection` va `org.json` co san
 * trong Android. Them Retrofit/OkHttp chi de goi dung mot dia chi la khong
 * dang.
 */
object Go2rtc {

    /** Qua con so nay thi coi nhu go2rtc khong tra loi. */
    private const val CHO_MS = 6000

    /**
     * Tra ve TEN cac luong, da sap xep.
     *
     * CO Y chi doc KHOA cua JSON.
     *
     * `/api/streams` tra ve ca `producers[].url`, ma trong url do co ca email
     * va MAT KHAU tai khoan Tuya (go2rtc luu nguyen chuoi
     * `tuya://...&password=...`). App khong doc, khong luu, khong hien phan
     * do o bat cu dau - no khong can thiet de xem hinh.
     */
    suspend fun danhSachLuong(c: CauHinhCamera): Result<List<String>> =
        withContext(Dispatchers.IO) {
            if (!c.dayDu()) {
                return@withContext Result.failure(IllegalStateException("Chưa khai báo go2rtc"))
            }
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(c.urlDanhSach()).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = CHO_MS
                    readTimeout = CHO_MS
                    if (c.taiKhoan.isNotBlank()) {
                        val tho = c.taiKhoan + ":" + c.matKhau
                        val ma = Base64.encodeToString(
                            tho.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
                        )
                        setRequestProperty("Authorization", "Basic " + ma)
                    }
                }
                val ma = conn.responseCode
                if (ma == 401 || ma == 403) {
                    return@withContext Result.failure(
                        IOException("go2rtc đòi tài khoản. Điền tài khoản và mật khẩu go2rtc ở dưới.")
                    )
                }
                if (ma != 200) {
                    return@withContext Result.failure(
                        IOException("go2rtc trả mã " + ma + ". Kiểm tra lại cổng.")
                    )
                }
                val than = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val goc = JSONObject(than)
                // keys() - chi lay ten luong, khong dong toi gia tri.
                val ten = goc.keys().asSequence().toList().sorted()
                Result.success(ten)
            } catch (e: Throwable) {
                Result.failure(e)
            } finally {
                conn?.disconnect()
            }
        }

    /**
     * Doi loi thanh cau noi ro CHO PHAI SUA.
     *
     * Cung loi nghi nhu [vn.smarthome.data.dien.MqttDien]: nguoi dung khong
     * doc duoc stack trace, va "khong ket noi duoc" thi khong biet di sua o
     * dau.
     */
    fun dienGiaiLoi(e: Throwable?): String = when {
        e == null -> "Lỗi không rõ"
        e is UnknownHostException ->
            "Không tìm thấy máy chạy go2rtc. Sai địa chỉ, hoặc máy tính bảng đang ở mạng Wi-Fi khác."
        e is SocketTimeoutException ->
            "Máy chạy go2rtc không trả lời. Thường là tường lửa Windows chặn cổng, hoặc go2rtc đã tắt."
        e is ConnectException ->
            "Máy có đó nhưng từ chối cổng này. Kiểm tra go2rtc còn chạy không và cổng có đúng không."
        else -> e.message ?: e.javaClass.simpleName
    }
}
