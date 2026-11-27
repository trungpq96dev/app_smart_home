package vn.smarthome.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
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
    ): Bitmap? = taiAnhUrl(c, c.urlThumbnail(eventId, congOverride))

    suspend fun taiAnhUrl(
        c: CauHinhCamera,
        url: String,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
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

    suspend fun taiDanhSachSuKien(
        c: CauHinhCamera,
        tenLuong: String?,
        kind: String = "all",
        congOverride: Int? = null,
    ): Result<List<SuKienCamera>> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) {
            return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        }
        goiJson(c.urlSuKien(tenLuong, kind, 60, congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> phanTichDanhSachSuKien(goc) }
    }

    suspend fun taiPtzPresets(
        c: CauHinhCamera,
        tenLuong: String,
        congOverride: Int? = null,
    ): Result<List<PtzPreset>> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.success(PtzPreset.MAC_DINH)
        goiJson(c.urlPtzPresets(tenLuong, congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc ->
                val arr = goc.optJSONArray("presets") ?: return@mapCatching PtzPreset.MAC_DINH
                val kq = ArrayList<PtzPreset>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id", "").ifBlank { o.optString("token", "") }
                    val label = o.optString("label", id)
                    val token = o.optString("token", id)
                    if (id.isNotBlank()) kq.add(PtzPreset(id, label, token))
                }
                if (kq.isEmpty()) PtzPreset.MAC_DINH else kq
            }
    }

    suspend fun dieuKhienPtzMove(
        c: CauHinhCamera,
        tenLuong: String,
        direction: String,
        speed: Float = 0.5f,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlPtzMove(tenLuong, direction, speed, congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã xoay camera." else "Lỗi xoay camera.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun goiPtzPreset(
        c: CauHinhCamera,
        tenLuong: String,
        preset: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlPtzPreset(tenLuong, preset, congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã xoay tới góc lưu sẵn." else "Lỗi gọi preset.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun datCheDoRiengTu(
        c: CauHinhCamera,
        bat: Boolean,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlPrivacy(bat, congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc ->
                goc.optString("message", if (bat) "Đã bật Chế độ Riêng tư." else "Đã tắt Chế độ Riêng tư.")
            }
    }

    suspend fun datCheDoNha(
        c: CauHinhCamera,
        mode: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlHomeMode(mode, congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> goc.optString("message", "Đã đổi chế độ camera.") }
    }

    // ----------------------------------------- Quan ly khuon mat nguoi quen

    suspend fun taiDanhSachNguoiQuen(
        c: CauHinhCamera,
        congOverride: Int? = null,
    ): Result<List<NguoiQuenMat>> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlDanhSachNguoiQuen(congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> phanTichDanhSachNguoiQuen(goc) }
    }

    suspend fun taiDanhSachNguoiLa(
        c: CauHinhCamera,
        congOverride: Int? = null,
    ): Result<List<AnhNguoiLa>> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        goiJson(c.urlDanhSachNguoiLa(congOverride), "GET", c.headerBasicAuth(), TIMEOUT_NHANH_MS)
            .mapCatching { goc -> phanTichDanhSachNguoiLa(goc) }
    }

    suspend fun themMatTuCamera(
        c: CauHinhCamera,
        tenNguoi: String,
        tenCam: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        val body = JSONObject().put("name", tenNguoi.trim()).put("camera", tenCam.trim()).toString()
        goiJson(c.urlThemMatTuCam(congOverride), "POST", c.headerBasicAuth(), TIMEOUT_CHUP_MS, body)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã thêm ảnh khuôn mặt." else "Không thêm được ảnh.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun themMatTuAnh(
        c: CauHinhCamera,
        tenNguoi: String,
        jpegBytes: ByteArray,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        val b64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        val body = JSONObject().put("name", tenNguoi.trim()).put("image_base64", b64).toString()
        goiJson(c.urlThemMatUpload(congOverride), "POST", c.headerBasicAuth(), TIMEOUT_CHUP_MS, body)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã lưu ảnh từ tablet." else "Không lưu được ảnh.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun chuyenNguoiLaThanhQuen(
        c: CauHinhCamera,
        filename: String,
        tenNguoi: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        val body = JSONObject().put("filename", filename).put("name", tenNguoi.trim()).toString()
        goiJson(c.urlChuyenNguoiLa(congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS, body)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã gán tên người quen." else "Lỗi gán tên.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    suspend fun xoaNguoiQuen(
        c: CauHinhCamera,
        tenNguoi: String,
        congOverride: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!c.dayDu()) return@withContext Result.failure(IllegalStateException("Chưa khai báo địa chỉ camera"))
        val body = JSONObject().put("name", tenNguoi.trim()).toString()
        goiJson(c.urlXoaNguoiQuen(congOverride), "POST", c.headerBasicAuth(), TIMEOUT_NHANH_MS, body)
            .mapCatching { goc ->
                val ok = goc.optBoolean("ok", false)
                val msg = goc.optString("message", if (ok) "Đã xóa người quen." else "Không xóa được.")
                if (!ok) throw IOException(msg)
                msg
            }
    }

    /**
     * Thu gon anh chup tu camera tablet ve toi da 960px JPEG (~80-150KB) de gui nhanh len serverubuntu.
     * QUAN TRONG: Doc EXIF Orientation tu camera Samsung/Android va xoay dung dung chieu truoc khi nen,
     * neu khong anh bi xoay ngang 90/270 do khien YuNet khong nhan dien duoc khuon mat!
     */
    suspend fun nenAnhChupTablet(file: File): ByteArray? = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() <= 0L) return@withContext null
        try {
            val exifOrientation = runCatching {
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (maxDim / sample > 1400) sample *= 2
            val rawBmp = BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return@withContext null

            val matrix = Matrix()
            when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }

            val upright = if (!matrix.isIdentity) {
                Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true).also {
                    if (it !== rawBmp) rawBmp.recycle()
                }
            } else rawBmp

            val scaled = if (maxOf(upright.width, upright.height) > 960) {
                val scale = 960f / maxOf(upright.width, upright.height)
                val w = (upright.width * scale).toInt().coerceAtLeast(1)
                val h = (upright.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(upright, w, h, true).also {
                    if (it !== upright) upright.recycle()
                }
            } else upright

            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
            scaled.recycle()
            out.toByteArray()
        } catch (_: Throwable) {
            null
        }
    }

    internal fun phanTichDanhSachNguoiQuen(goc: JSONObject): List<NguoiQuenMat> {
        val arr = goc.optJSONArray("people") ?: JSONArray()
        val kq = ArrayList<NguoiQuenMat>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name", "").trim()
            if (name.isBlank()) continue
            kq.add(
                NguoiQuenMat(
                    name = name,
                    count = o.optInt("count", 0),
                    sampleUrl = o.optString("sample_url", "").ifBlank { null },
                    updatedTs = o.optLong("updated_ts", 0L),
                )
            )
        }
        return kq
    }

    internal fun phanTichDanhSachNguoiLa(goc: JSONObject): List<AnhNguoiLa> {
        val arr = goc.optJSONArray("items") ?: JSONArray()
        val kq = ArrayList<AnhNguoiLa>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val fn = o.optString("filename", "").trim()
            val url = o.optString("url", "").trim()
            if (fn.isBlank() || url.isBlank()) continue
            kq.add(AnhNguoiLa(fn, url, o.optLong("ts", 0L)))
        }
        return kq
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
        val dsEv = phanTichMangSuKien(evArr, goc.optString("camera", ""))

        return DongThoiGianNgay(
            camera = goc.optString("camera", ""),
            ngay = goc.optString("date", ""),
            dayStartTs = goc.optLong("day_start_ts", 0L),
            dayEndTs = goc.optLong("day_end_ts", 0L),
            segments = dsSeg,
            events = dsEv,
        )
    }

    internal fun phanTichDanhSachSuKien(goc: JSONObject): List<SuKienCamera> =
        phanTichMangSuKien(goc.optJSONArray("events") ?: JSONArray(), "")

    private fun phanTichMangSuKien(evArr: JSONArray, camMacDinh: String): List<SuKienCamera> {
        val dsEv = ArrayList<SuKienCamera>(evArr.length())
        for (i in 0 until evArr.length()) {
            val e = evArr.optJSONObject(i) ?: continue
            val labelRaw = if (e.isNull("label")) null else e.optString("label", "").ifBlank { null }
            dsEv.add(
                SuKienCamera(
                    id = e.optInt("id"),
                    kind = e.optString("kind", "person"),
                    startTs = e.optLong("start_ts"),
                    endTs = e.optLong("end_ts"),
                    maxConf = e.optDouble("max_conf", 0.0).toFloat(),
                    personCount = e.optInt("person_count", 1),
                    hasThumb = e.optBoolean("has_thumb", false),
                    camera = e.optString("camera", camMacDinh),
                    label = labelRaw,
                    hasClip = e.optBoolean("has_clip", false),
                )
            )
        }
        return dsEv
    }

    private fun goiJson(
        url: String,
        method: String,
        headers: Map<String, String>,
        timeoutMs: Int,
        jsonBody: String? = null,
    ): Result<JSONObject> {
        var conn: HttpURLConnection? = null
        return try {
            val bodyBytes = jsonBody?.toByteArray(Charsets.UTF_8)
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                headers.forEach { (k, v) -> setRequestProperty(k, v) }
                if (method == "POST") {
                    doOutput = true
                    if (bodyBytes != null) {
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        setFixedLengthStreamingMode(bodyBytes.size)
                    } else {
                        setFixedLengthStreamingMode(0)
                    }
                }
            }
            if (method == "POST" && bodyBytes != null) {
                conn.outputStream.use { it.write(bodyBytes) }
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
                val tb = runCatching {
                    val jo = JSONObject(than)
                    jo.optString("message").ifBlank { jo.optString("error") }
                }.getOrNull()
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
