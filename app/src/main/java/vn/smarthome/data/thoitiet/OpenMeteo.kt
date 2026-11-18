package vn.smarthome.data.thoitiet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Lay thoi tiet that tu Open-Meteo (api.open-meteo.com).
 *
 * Vi sao Open-Meteo: mien phi, KHONG can API key, dung hop cho dung ca nhan -
 * giong cach app WeatherMaster (repo tham khao) dung nguon nay. Khong co key
 * nghia la khong co bi mat nao phai nhung vao ma nguon (ma nguon nay o GitHub).
 *
 * Khong them thu vien HTTP/JSON nao: `HttpURLConnection` + `org.json` co san
 * trong Android, giong [vn.smarthome.data.camera.Go2rtc]. java.time dung duoc
 * tu minSdk 26 (xem build.gradle) nen khong can desugar.
 */
object OpenMeteo {

    /**
     * Toa do mac dinh: Ha Noi. DOI O DAY neu dat may o tinh khac.
     *
     * (Chua lam phan chon dia diem trong Cai dat vi muc nay chi can mot cho co
     * dinh; neu sau nay can nhieu noi thi dua ba hang so nay vao DataStore giong
     * cau hinh MQTT/camera.)
     */
    const val LAT = 21.0278
    const val LON = 105.8342
    const val TEN_DIA_DIEM = "Hà Nội"

    /** Qua con so nay thi coi nhu Open-Meteo khong tra loi. */
    private const val CHO_MS = 8000

    /** Bao nhieu ngay du bao (gom hom nay). The chi hien 4 o nen lay 4. */
    private const val SO_NGAY = 4

    private val DINH_DANG_GIO = DateTimeFormatter.ofPattern("HH:mm")

    private fun url(lat: Double, lon: Double): String =
        "https://api.open-meteo.com/v1/forecast" +
            "?latitude=" + lat +
            "&longitude=" + lon +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto" +
            "&forecast_days=" + SO_NGAY

    suspend fun layThoiTiet(
        lat: Double = LAT,
        lon: Double = LON,
        tenDiaDiem: String = TEN_DIA_DIEM,
    ): Result<ThoiTietData> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url(lat, lon)).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CHO_MS
                readTimeout = CHO_MS
            }
            val ma = conn.responseCode
            if (ma != 200) {
                return@withContext Result.failure(
                    IOException("Open-Meteo trả mã " + ma + ".")
                )
            }
            val than = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            Result.success(phanTich(JSONObject(than), tenDiaDiem))
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            conn?.disconnect()
        }
    }

    private fun phanTich(goc: JSONObject, tenDiaDiem: String): ThoiTietData {
        val cur = goc.getJSONObject("current")
        val hienTai = ThoiTietHienTai(
            nhietDo = Math.round(cur.getDouble("temperature_2m")).toInt(),
            camGiac = Math.round(cur.getDouble("apparent_temperature")).toInt(),
            doAm = cur.getInt("relative_humidity_2m"),
            gio = Math.round(cur.getDouble("wind_speed_10m")).toInt(),
            maWmo = cur.getInt("weather_code"),
            banNgay = cur.optInt("is_day", 1) == 1,
        )

        val daily = goc.getJSONObject("daily")
        val ngayList = daily.getJSONArray("time")
        val maList = daily.getJSONArray("weather_code")
        val caoList = daily.getJSONArray("temperature_2m_max")
        val thapList = daily.getJSONArray("temperature_2m_min")

        val cacNgay = ArrayList<NgayThoiTiet>(ngayList.length())
        for (i in 0 until ngayList.length()) {
            cacNgay.add(
                NgayThoiTiet(
                    thu = nhanThu(ngayList.getString(i), i),
                    maWmo = maList.getInt(i),
                    caoNhat = Math.round(caoList.getDouble(i)).toInt(),
                    thapNhat = Math.round(thapList.getDouble(i)).toInt(),
                )
            )
        }

        return ThoiTietData(
            diaDiem = tenDiaDiem,
            hienTai = hienTai,
            cacNgay = cacNgay,
            gioCapNhat = gioCapNhat(cur.optString("time", "")),
        )
    }

    /** Ngay dau tien la "Hôm nay"; con lai doi ra thu kieu Viet. */
    private fun nhanThu(ngayIso: String, chiSo: Int): String {
        if (chiSo == 0) return "Hôm nay"
        return try {
            when (LocalDate.parse(ngayIso).dayOfWeek.value) {
                1 -> "Th 2"; 2 -> "Th 3"; 3 -> "Th 4"; 4 -> "Th 5"
                5 -> "Th 6"; 6 -> "Th 7"; else -> "CN"
            }
        } catch (e: Exception) {
            ngayIso
        }
    }

    /** Lay gio cap nhat tu truong current.time ("2026-11-18T14:00"). */
    private fun gioCapNhat(gioIso: String): String = try {
        LocalDateTime.parse(gioIso).format(DINH_DANG_GIO)
    } catch (e: Exception) {
        LocalTime.now().format(DINH_DANG_GIO)
    }

    /**
     * Doi loi thanh cau noi ro CHO PHAI SUA, giong cach
     * [vn.smarthome.data.camera.Go2rtc] lam: nguoi dung khong doc stack trace.
     */
    fun dienGiaiLoi(e: Throwable?): String = when {
        e == null -> "Lỗi không rõ"
        e is UnknownHostException ->
            "Không vào được Open-Meteo. Máy tính bảng chưa có mạng, hoặc đang ở Wi-Fi không ra được Internet."
        e is SocketTimeoutException ->
            "Open-Meteo không trả lời kịp. Mạng chậm hoặc đang chập chờn, sẽ tự thử lại sau."
        else -> e.message ?: e.javaClass.simpleName
    }
}
