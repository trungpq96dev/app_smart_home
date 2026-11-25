package vn.smarthome.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Goi HTTP Playback API (`:1985` mac dinh) cua dich vu `camera_recorder` tren serverubuntu.
 *
 * Dung thuan `HttpURLConnection` + `org.json` + `LruCache` co san trong Android
 * de khong keo them thu vien nang vao APK.
 */
object CameraPlaybackApi {

    private const val TIMEOUT_NHANH_MS = 8000
    private const val TIMEOUT_CHUP_MS = 18000

    /** Cache RAM ~8 MB cho anh thumbnail su kien nguoi de cuon danh sach 60fps. */
    private val cacheThumb = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = maxOf(1, value.byteCount / 1024)
    }

    suspend fun danhSachNgay(
        c: CauHinhCamera,
        tenLuong: String,
        congOverride: Int? = null,
    ): Result<List<NgayCoVideo>> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) {
            return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        }
        goiJson(c.urlDanhSachNgay(tenLuong, congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> phanTichDanhSachNgay(goc) }
    }

    suspend fun taiDongThoiGian(
        c: CauHinhCamera,
        tenLuong: String,
        ngay: String,
        congOverride: Int? = null,
    ): Result<DongThoiGianNgay> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) {
            return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        }
        goiJson(c.urlDongThoiGian(tenLuong, ngay, congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> phanTichDongThoiGian(goc) }
    }

    suspend fun chupAnhNgay(
        c: CauHinhCamera,
        tenLuong: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) {
            return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        }
        goiJson(c.urlChupAnh(tenLuong, congOverride), "POST", c.headerBasicAuth(), TIMEOUT_CHUP_MS)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", "").ifBlank {
                    if (ok) "Đã chụp ảnh lưu vào đầu ghi." else "Không chụp được ảnh."
                }
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun taiThumbnail(
        c: CauHinhCamera,
        eventId: Int,
        congOverride: Int? = null,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val url = c.urlThumbnail(eventId, congOverride)
        cacheThumb.get(url)?.let { return@withContext it }

        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_NHANH_MS
                readTimeout = TIMEOUT_NHANH_MS
                c.headerBasicAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode != 200) return@withContext null
            val bytes = conn.inputStream.use { it.readBytes() }
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) cacheThumb.put(url, bmp)
            bmp
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun dienGiaiLoi(e: Throwable?, cong: Int = 1985): String = when {
        e == null -> "Lỗi không rõ"
        e is UnknownHostException ->
            "Không tìm thấy máy chủ đầu ghi. Kiểm tra địa chỉ Wi-Fi."
        e is SocketTimeoutException ->
            "Đầu ghi camera (cổng $cong) không trả lời kịp."
        e is ConnectException ->
            "Chưa nối được dịch vụ ghi hình 24/24 ở cổng $cong trên serverubuntu. Kiểm tra container camera-recorder đã chạy chưa."
        else -> e.message ?: e.javaClass.simpleName
    }

    // ------------------------------------------------- Tach JSON de kiem thu

    internal fun phanTichDanhSachNgay(goc: JSONObject): List<NgayCoVideo> {
        val arr = goc.optJSONArray("days") ?: JSONArray()
        val kq = ArrayList<NgayCoVideo>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val ngay = o.optString("date", "")
            if (ngay.isBlank()) continue
            kq.add(
                NgayCoVideo(
                    ngay = ngay,
                    soDoan = o.optInt("segment_count", 0),
                    tongGiay = o.optInt("recorded_sec", 0),
                    soSuKienNguoi = o.optInt("person_events", 0),
                )
            )
        }
        return kq
    }

    internal fun phanTichDongThoiGian(goc: JSONObject): DongThoiGianNgay {
        val segArr = goc.optJSONArray("segments") ?: JSONArray()
        val dsSeg = ArrayList<DoanVideo>(segArr.length())
        for (i in 0 until segArr.length()) {
            val s = segArr.optJSONObject(i) ?: continue
            dsSeg.add(
                DoanVideo(
                    id = s.optInt("id"),
                    startTs = s.optLong("start_ts"),
                    endTs = s.optLong("end_ts"),
                    durationSec = s.optDouble("duration_sec", 0.0).toFloat(),
                    sizeBytes = s.optLong("size_bytes", 0L),
                    tier = s.optString("tier", "hot"),
                )
            )
        }

        val evArr = goc.optJSONArray("events") ?: JSONArray()
        val dsEv = ArrayList<SuKienCamera>(evArr.length())
        for (i in 0 until evArr.length()) {
            val e = evArr.optJSONObject(i) ?: continue
            dsEv.add(
                SuKienCamera(
                    id = e.optInt("id"),
                    kind = e.optString("kind", "person"),
                    startTs = e.optLong("start_ts"),
                    endTs = e.optLong("end_ts"),
                    maxConf = e.optDouble("max_conf", 0.0).toFloat(),
                    personCount = e.optInt("person_count", 1),
                    hasThumb = e.optBoolean("has_thumb", false),
                )
            )
        }

        return DongThoiGianNgay(
            camera = goc.optString("camera", ""),
            ngay = goc.optString("date", ""),
            dayStartTs = goc.optLong("day_start_ts", 0L),
            dayEndTs = goc.optLong("day_end_ts", 0L),
            segments = dsSeg,
            events = dsEv,
        )
    }

    private fun goiJson(
        url: String,
        method: String,
        headers: Map<String, String>,
        timeoutMs: Int,
    ): Result<JSONObject> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (method == "POST") {
                    doOutput = true
                    setFixedLengthStreamingMode(0)
                }
            }
            val ma = conn.responseCode
            if (ma == 401 || ma == 403) {
                return Result.failure(
                    IOException("Đầu ghi đòi tài khoản xác thực — kiểm tra tài khoản/mật khẩu ở Cài đặt Camera.")
                )
            }
            val luongDoc = if (ma in 200..299) conn.inputStream else conn.errorStream
            val than = luongDoc?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (ma !in 200..299) {
                val tb = runCatching { JSONObject(than).optString("error") }.getOrNull()
                return Result.failure(IOException(tb?.ifBlank { null } ?: "Đầu ghi trả mã HTTP $ma"))
            }
            Result.success(JSONObject(than))
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }
}
