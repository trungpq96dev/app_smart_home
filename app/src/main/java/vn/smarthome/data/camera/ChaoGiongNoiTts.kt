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
import kotlinx.coroutines.withContext
import vn.smarthome.data.baomat.ManChoLogic
import java.io.File
import java.io.FileInputStream
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
 * 1. Tải MP3 giọng nữ tiếng Việt chuẩn (`tl=vi`) từ `/api/tts` trên serverubuntu (có cache ổ đĩa).
 * 2. Phát qua `MediaPlayer` trên luồng `USAGE_MEDIA` (`STREAM_MUSIC`) bằng `FileDescriptor`
 *    (không dùng `USAGE_ASSISTANCE_SONIFICATION` vốn bị tắt tiếng mặc định trên tablet Samsung,
 *    và không truyền path trực tiếp tránh lỗi SELinux `MediaServer` với `cacheDir`).
 * 3. Dự phòng offline: Android `TextToSpeech` trên luồng Main, ưu tiên `com.google.android.tts` + `vi-VN`.
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
        onLoi: ((String) -> Unit)? = null,
    ) {
        if (!quyetDinh.hopLe) return
        val cau = quyetDinh.docTts
        scope.launch {
            if (quyetDinh.phatChuongTingTong) {
                withContext(Dispatchers.IO) { phatTingTong() }
                delay(280L)
            }
            if (!cau.isNullOrBlank()) {
                phatCauNoi(cau, cauHinhCamera, congApi, onLoi)
            }
        }
    }

    fun docThu(
        cau: String,
        cauHinhCamera: CauHinhCamera?,
        congApi: Int?,
        onKetQua: ((String) -> Unit)? = null,
    ) {
        val noiDung = cau.trim().ifEmpty { "Chào mừng bạn đã về nhà!" }
        scope.launch {
            phatCauNoi(noiDung, cauHinhCamera, congApi, onKetQua)
        }
    }

    private suspend fun phatCauNoi(
        noiDung: String,
        cauHinhCamera: CauHinhCamera?,
        congApi: Int?,
        onThongBao: ((String) -> Unit)?,
    ) {
        // Kiem tra am luong Media (STREAM_MUSIC) cua may tinh bang
        val amLuongZero = runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am != null && am.getStreamVolume(AudioManager.STREAM_MUSIC) == 0
        }.getOrDefault(false)
        if (amLuongZero) {
            onThongBao?.invoke("Âm lượng Media trên máy tính bảng đang bằng 0 — hãy tăng âm lượng loa!")
        }

        // 1. Tai MP3 giong Viet chuan tu serverubuntu (tren Dispatchers.IO)
        val mp3File = cauHinhCamera?.takeIf { it.dayDu() }?.let { cfg ->
            withContext(Dispatchers.IO) { taiMp3TuServer(cfg, noiDung, congApi) }
        }

        // 2. Phat MediaPlayer tren Dispatchers.Main bang FileDescriptor + USAGE_MEDIA (STREAM_MUSIC)
        if (mp3File != null) {
            val okPhat = withContext(Dispatchers.Main) { phatFileMp3(mp3File) }
            if (okPhat) return
        }

        // 3. Du phong offline: Android TextToSpeech tren Dispatchers.Main
        withContext(Dispatchers.Main) {
            docTiengVietOffline(noiDung)
        }
    }

    private fun taiMp3TuServer(cfg: CauHinhCamera, text: String, congApi: Int?): File? {
        var conn: HttpURLConnection? = null
        return try {
            val url = cfg.urlTts(text, congApi)
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 10000
                cfg.headerBasicAuth().forEach { (k, v) -> setRequestProperty(k, v) }
            }
            if (conn.responseCode != 200) return null
            val bytes = conn.inputStream.use { it.readBytes() }
            if (bytes.size < 512) return null

            val outFile = File(context.cacheDir, "tts_greet_${System.currentTimeMillis() % 10000}.mp3")
            // Don file tam cu neu con sot
            context.cacheDir.listFiles { f -> f.name.startsWith("tts_greet_") }?.forEach {
                runCatching { it.delete() }
            }
            outFile.writeBytes(bytes)
            outFile
        } catch (_: Throwable) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun phatFileMp3(file: File): Boolean {
        return try {
            runCatching {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                FileInputStream(file).use { fis ->
                    setDataSource(fis.fd)
                }
                setVolume(1.0f, 1.0f)
                setOnCompletionListener { player ->
                    runCatching { player.release() }
                    runCatching { file.delete() }
                    if (mediaPlayer === player) mediaPlayer = null
                }
                setOnErrorListener { player, _, _ ->
                    runCatching { player.release() }
                    runCatching { file.delete() }
                    if (mediaPlayer === player) mediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = mp
            true
        } catch (_: Throwable) {
            runCatching { file.delete() }
            false
        }
    }

    private fun phatTingTong() {
        runCatching {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
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
