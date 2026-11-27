package vn.smarthome.data.camera

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.smarthome.data.baomat.ManChoLogic
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class QuyetDinhChao(
    val hopLe: Boolean,
    val hienPopup: Boolean,
    val thoiLuongMs: Long,
    val phatChuongTingTong: Boolean,
    val docTts: String?,
)

/**
 * Logic thuần cho sự kiện `camera/face/greet` (Mục 2.2) — dễ kiểm thử đơn vị 100%:
 * - Chỉ nhận sự kiện còn mới (`ts` cách hiện tại < 30 giây).
 * - Tắt tự động đọc & chuông vào giờ ngủ (22:00–06:00) trừ khi bật công tắc `docChaoGioNgu`.
 * - Chế độ Vắng nhà (`away`) luôn giữ im lặng trên tablet.
 * - Người quen: đọc câu chào tiếng Việt + hiện banner 5 giây.
 * - Người lạ (chế độ Nhà ban ngày): phát tiếng ting-tong + đọc "Có khách ở cửa" + hiện popup ảnh 10 giây.
 */
object FaceGreetLogic {
    const val MAX_AGE_SEC = 30L
    const val BANNER_QUEN_MS = 5_000L
    const val POPUP_LA_MS = 10_000L

    fun quyetDinh(
        ev: FaceGreetEvent,
        nowEpochSec: Long,
        gioHienTai: Int,
        gioBatDauNgu: Int = 22,
        gioKetThucNgu: Int = 6,
        docChaoGioNgu: Boolean = false,
    ): QuyetDinhChao {
        if (!ev.conMoi(nowEpochSec, MAX_AGE_SEC)) {
            return QuyetDinhChao(
                hopLe = false,
                hienPopup = false,
                thoiLuongMs = 0L,
                phatChuongTingTong = false,
                docTts = null,
            )
        }

        val dangVangNha = ev.mode.equals("away", ignoreCase = true)
        val dangGioNgu = ev.mode.equals("sleep", ignoreCase = true) ||
            ManChoLogic.laKhungGioDem(gioHienTai, gioBatDauNgu, gioKetThucNgu) ||
            (ev.silent && !dangVangNha)

        val imLang = dangVangNha || (dangGioNgu && !docChaoGioNgu)

        if (imLang) {
            return QuyetDinhChao(
                hopLe = true,
                hienPopup = false,
                thoiLuongMs = 0L,
                phatChuongTingTong = false,
                docTts = null,
            )
        }

        return if (ev.isKnown) {
            QuyetDinhChao(
                hopLe = true,
                hienPopup = true,
                thoiLuongMs = BANNER_QUEN_MS,
                phatChuongTingTong = false,
                docTts = ev.greeting.ifBlank { "Chào mừng ${ev.name} về nhà!" },
            )
        } else {
            QuyetDinhChao(
                hopLe = true,
                hienPopup = true,
                thoiLuongMs = POPUP_LA_MS,
                phatChuongTingTong = true,
                docTts = ev.greeting.ifBlank { "Có khách ở cửa" },
            )
        }
    }
}

/**
 * Bộ phát chuông ting-tong và đọc câu chào tiếng Việt chuẩn tự nhiên:
 * 1. Ưu tiên tải MP3 giọng Việt Neural (`vi-VN-HoaiMyNeural`) từ `/api/tts` trên serverubuntu
 *    (có cache ổ đĩa trên server -> độ trễ LAN ~50ms, giọng người Việt tự nhiên 100%, không bị
 *    lỗi giọng Samsung TTS đọc lơ lớ kiểu tiếng Trung).
 * 2. Dự phòng offline: dùng Android `TextToSpeech` ưu tiên động cơ `com.google.android.tts`
 *    và chọn đúng giọng `vi-VN`.
 */
class ChaoGiongNoiTts(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    @Volatile
    private var tts: TextToSpeech? = null

    @Volatile
    private var sanSang = false

    @Volatile
    private var cauCho: String? = null

    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    fun phat(
        quyetDinh: QuyetDinhChao,
        cauHinhCamera: CauHinhCamera? = null,
        congApi: Int? = null,
    ) {
        if (!quyetDinh.hopLe) return
        val cau = quyetDinh.docTts
        scope.launch(Dispatchers.IO) {
            if (quyetDinh.phatChuongTingTong) {
                phatTingTong()
                delay(320L)
            }
            if (!cau.isNullOrBlank()) {
                val daPhatServer = cauHinhCamera?.takeIf { it.dayDu() }?.let { cfg ->
                    phatMp3TuServer(cfg, cau, congApi)
                } ?: false
                if (!daPhatServer) {
                    docTiengVietOffline(cau)
                }
            }
        }
    }

    fun docThu(cau: String, cauHinhCamera: CauHinhCamera?, congApi: Int?) {
        val noiDung = cau.trim().ifEmpty { "Chào mừng bạn đã về nhà!" }
        scope.launch(Dispatchers.IO) {
            val daPhatServer = cauHinhCamera?.takeIf { it.dayDu() }?.let { cfg ->
                phatMp3TuServer(cfg, noiDung, congApi)
            } ?: false
            if (!daPhatServer) {
                docTiengVietOffline(noiDung)
            }
        }
    }

    private fun phatMp3TuServer(cfg: CauHinhCamera, text: String, congApi: Int?): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            val url = cfg.urlTts(text, congApi)
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 10000
                cfg.headerBasicAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode != 200) return false
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size < 512) return false

            val outFile = File(context.cacheDir, "tts_greet_latest.mp3")
            outFile.writeBytes(bytes)

            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(outFile.absolutePath)
                setOnCompletionListener { player ->
                    runCatching { player.release() }
                    if (mediaPlayer === player) mediaPlayer = null
                }
                setOnErrorListener { player, _, _ ->
                    runCatching { player.release() }
                    if (mediaPlayer === player) mediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            true
        } catch (_: Throwable) {
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun phatTingTong() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            try {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                Thread.sleep(220L)
                tg.startTone(ToneGenerator.TONE_PROP_ACK, 220)
                Thread.sleep(240L)
            } finally {
                tg.release()
            }
        }
    }

    private fun docTiengVietOffline(noiDung: String) {
        val current = tts
        if (current == null) {
            cauCho = noiDung
            val listener = TextToSpeech.OnInitListener { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    if (engine != null) {
                        val vi = Locale("vi", "VN")
                        engine.setLanguage(vi)
                        // Chon dung giong tieng Viet (uu tien local/network cua Google TTS)
                        runCatching {
                            val giongViet = engine.voices
                                ?.filter { it.locale?.language.equals("vi", ignoreCase = true) }
                                ?.sortedBy { it.isNetworkConnectionRequired }
                                ?.firstOrNull()
                            if (giongViet != null) engine.voice = giongViet
                        }
                        engine.setSpeechRate(0.95f)
                        sanSang = true
                        val pending = cauCho
                        cauCho = null
                        if (!pending.isNullOrBlank()) {
                            engine.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "face_greet")
                        }
                    }
                }
            }
            // Uu tien Google TTS (com.google.android.tts) thay vi Samsung TTS de tranh giong lo lo
            val hasGoogleTts = runCatching {
                context.packageManager.getPackageInfo("com.google.android.tts", 0)
                true
            }.getOrDefault(false)
            runCatching {
                tts = if (hasGoogleTts) {
                    TextToSpeech(context.applicationContext, listener, "com.google.android.tts")
                } else {
                    TextToSpeech(context.applicationContext, listener)
                }
            }
            return
        }
        if (sanSang) {
            runCatching {
                current.speak(noiDung, TextToSpeech.QUEUE_FLUSH, null, "face_greet")
            }
        } else {
            cauCho = noiDung
        }
    }

    fun dong() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        sanSang = false
        cauCho = null
    }
}
