package vn.smarthome.data.camera

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * Hop dong topic MQTT cua dich vu `scripts/camera_recorder/` tren serverubuntu.
 */
object CameraTopic {
    const val GOC = "camera/#"
    const val TRANG_THAI = "camera/trangthai"
    const val STATE = "camera/state"
    const val PERSON = "camera/person"
    const val CMD = "camera/cmd"
    const val KET_QUA = "camera/cmd/result"
}

private val MUI_GIO_VN: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
private val DINH_DANG_GIO: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun dinhDangGioVn(epochSec: Long): String =
    Instant.ofEpochSecond(epochSec).atZone(MUI_GIO_VN).format(DINH_DANG_GIO)

data class TrangThaiGhiTungCam(
    val ten: String,
    val dangGhi: Boolean,
    val lastSegmentTs: Long?,
    val loi: String?,
)

data class SuKienNguoiGanNhat(
    val eventId: Int,
    val camera: String,
    val startTs: Long,
    val thoiGian: String,
    val soNguoi: Int,
    val doTinCay: Float,
)

data class KetQuaLenhCam(
    val traLoiId: String?,
    val action: String,
    val ok: Boolean,
    val message: String,
)

/**
 * Trang thai dau ghi 24/24 + AI canh nguoi doc tu nhanh MQTT `camera/#`.
 *
 * Khoi tao null cho cac truong trang thai de khong bia so khi serverubuntu
 * chua bat `camera_recorder` hoac mat ket noi broker.
 */
data class CameraRecorderState(
    val online: Boolean? = null,
    val recordingEnabled: Boolean? = null,
    val detectPersonEnabled: Boolean? = null,
    val telegramEnabled: Boolean = false,
    val driveEnabled: Boolean = false,
    val apiPort: Int? = null,
    val cacheUsedGb: Double = 0.0,
    val cacheMaxGb: Double = 10.0,
    val pendingUploads: Int = 0,
    val uploadedToday: Int = 0,
    val uploadedTodayMb: Double = 0.0,
    val personToday: Int = 0,
    val driveError: String? = null,
    val cameras: Map<String, TrangThaiGhiTungCam> = emptyMap(),
    val capNhat: String? = null,
    val traLoiId: String? = null,
    val nguoiGanNhat: SuKienNguoiGanNhat? = null,
    val ketQuaLenh: KetQuaLenhCam? = null,
) {
    val dangSong: Boolean get() = online == true

    fun camDangGhi(tenLuong: String?): Boolean =
        dangSong && recordingEnabled == true &&
            (tenLuong == null || cameras[tenLuong]?.dangGhi == true)

    companion object {
        fun gop(cur: CameraRecorderState, topic: String, payload: String): CameraRecorderState {
            val txt = payload.trim()
            if (txt.isEmpty()) return cur

            if (topic == CameraTopic.TRANG_THAI) {
                return cur.copy(online = txt.equals("online", ignoreCase = true))
            }

            return try {
                val o = JSONObject(txt)
                when (topic) {
                    CameraTopic.STATE -> {
                        val camsMap = mutableMapOf<String, TrangThaiGhiTungCam>()
                        o.optJSONObject("cameras")?.let { co ->
                            val keys = co.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                val item = co.optJSONObject(k) ?: continue
                                camsMap[k] = TrangThaiGhiTungCam(
                                    ten = k,
                                    dangGhi = item.optBoolean("recording", false),
                                    lastSegmentTs = if (item.isNull("last_segment_ts")) null else item.optLong("last_segment_ts"),
                                    loi = item.optString("error", "").ifBlank { null }
                                        ?.takeIf { it != "null" },
                                )
                            }
                        }
                        cur.copy(
                            online = cur.online ?: true,
                            recordingEnabled = o.optBoolean("recording_enabled", true),
                            detectPersonEnabled = o.optBoolean("detect_person_enabled", true),
                            telegramEnabled = o.optBoolean("telegram_enabled", false),
                            driveEnabled = o.optBoolean("drive_enabled", false),
                            apiPort = o.optInt("api_port", 0).takeIf { it in 1..65535 },
                            cacheUsedGb = o.optDouble("cache_used_gb", 0.0),
                            cacheMaxGb = o.optDouble("cache_max_gb", 10.0),
                            pendingUploads = o.optInt("pending_uploads", 0),
                            uploadedToday = o.optInt("uploaded_today", 0),
                            uploadedTodayMb = o.optDouble("uploaded_today_mb", 0.0),
                            personToday = o.optInt("person_today", 0),
                            driveError = o.optString("drive_error", "").ifBlank { null }
                                ?.takeIf { it != "null" },
                            cameras = camsMap,
                            capNhat = o.optString("cap_nhat", "").ifBlank { null },
                            traLoiId = o.optString("tra_loi_id", "").ifBlank { null }
                                ?.takeIf { it != "null" },
                        )
                    }

                    CameraTopic.PERSON -> {
                        val suKien = SuKienNguoiGanNhat(
                            eventId = o.optInt("event_id", 0),
                            camera = o.optString("camera", ""),
                            startTs = o.optLong("start_ts", 0L),
                            thoiGian = o.optString("thoi_gian", ""),
                            soNguoi = o.optInt("person_count", 1),
                            doTinCay = o.optDouble("confidence", 0.0).toFloat(),
                        )
                        cur.copy(nguoiGanNhat = suKien)
                    }

                    CameraTopic.KET_QUA -> {
                        cur.copy(
                            ketQuaLenh = KetQuaLenhCam(
                                traLoiId = o.optString("tra_loi_id", "").ifBlank { null }
                                    ?.takeIf { it != "null" },
                                action = o.optString("action", ""),
                                ok = o.optBoolean("ok", false),
                                message = o.optString("message", ""),
                            )
                        )
                    }

                    else -> cur
                }
            } catch (_: Exception) {
                cur
            }
        }
    }
}

// ------------------------------------------------ Mo hinh Xem lai Timeline

data class NgayCoVideo(
    val ngay: String,
    val soDoan: Int,
    val tongGiay: Int,
    val soSuKienNguoi: Int,
)

data class DoanVideo(
    val id: Int,
    val startTs: Long,
    val endTs: Long,
    val durationSec: Float,
    val sizeBytes: Long,
    /** "hot" = nam san tren o serverubuntu (<0.5s); "drive" = keo tu Google Drive 5TB. */
    val tier: String,
) {
    val laCucBo: Boolean get() = tier == "hot"
}

data class SuKienCamera(
    val id: Int,
    val kind: String,
    val startTs: Long,
    val endTs: Long,
    val maxConf: Float,
    val personCount: Int,
    val hasThumb: Boolean,
) {
    val laNguoi: Boolean get() = kind == "person"
    val thoiLuongGiay: Int get() = max(1, (endTs - startTs).toInt())
    fun gioBatDau(): String = dinhDangGioVn(startTs)
}

data class ViTriPhat(
    val segment: DoanVideo,
    val offsetMs: Long,
    val targetEpochSec: Long,
)

data class DongThoiGianNgay(
    val camera: String,
    val ngay: String,
    val dayStartTs: Long,
    val dayEndTs: Long,
    val segments: List<DoanVideo>,
    val events: List<SuKienCamera>,
) {
    val suKienNguoi: List<SuKienCamera>
        get() = events.filter { it.laNguoi }

    /**
     * Tim doan video chua moc [epochSec]; neu roi vao khoang nghi ngan giua 2 file
     * thi nhay toi dau doan tiep theo gan nhat (hoac cuoi doan truoc neu o cuoi ngay).
     */
    fun timViTriPhat(epochSec: Long): ViTriPhat? {
        if (segments.isEmpty()) return null
        val trung = segments.firstOrNull { epochSec >= it.startTs && epochSec < it.endTs }
        if (trung != null) {
            val offset = max(0L, (epochSec - trung.startTs) * 1000L)
            return ViTriPhat(trung, offset, epochSec)
        }
        val keTiep = segments.firstOrNull { it.startTs >= epochSec }
        if (keTiep != null) {
            return ViTriPhat(keTiep, 0L, keTiep.startTs)
        }
        val cuoi = segments.last()
        val offCuoi = max(0L, (cuoi.endTs - cuoi.startTs - 2L) * 1000L)
        return ViTriPhat(cuoi, offCuoi, max(cuoi.startTs, cuoi.endTs - 2L))
    }

    /** Muc tieu nhay khi nguoi dung bam vao 1 su kien co nguoi: lui truoc 3 giay de thay luc buoc vao. */
    fun mocNhaySuKien(ev: SuKienCamera): Long = max(dayStartTs, ev.startTs - 3L)

    fun doanTiepTheo(sauSegId: Int): DoanVideo? {
        val idx = segments.indexOfFirst { it.id == sauSegId }
        return if (idx >= 0 && idx + 1 < segments.size) segments[idx + 1] else null
    }

    fun suKienNguoiTruoc(hienTaiTs: Long): SuKienCamera? =
        suKienNguoi.lastOrNull { it.startTs < hienTaiTs - 4L }

    fun suKienNguoiTiep(hienTaiTs: Long): SuKienCamera? =
        suKienNguoi.firstOrNull { it.startTs > hienTaiTs + 2L }
}
