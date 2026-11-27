package vn.smarthome.data.camera

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.smarthome.data.baomat.ManChoLogic
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
 * - Tắt tự động đọc & chuông vào giờ ngủ (23:00–06:00) hoặc khi `silent=true` / `mode=away|sleep`.
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
        gioBatDauNgu: Int = 23,
        gioKetThucNgu: Int = 6,
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

        val dangNguHoacVang = ev.silent ||
            ev.mode.equals("away", ignoreCase = true) ||
            ev.mode.equals("sleep", ignoreCase = true) ||
            ManChoLogic.laKhungGioDem(gioHienTai, gioBatDauNgu, gioKetThucNgu)

        if (dangNguHoacVang) {
            // Ban đêm / Vắng nhà: im lặng hoàn toàn trên tablet (Telegram/ntfy đã báo điện thoại)
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
 * Bộ phát chuông ting-tong và đọc câu chào tiếng Việt bằng Android `TextToSpeech`.
 * Khởi tạo lười (chỉ gọi khi có sự kiện thật) để không làm nặng khởi động hoặc môi trường Robolectric.
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

    fun phat(quyetDinh: QuyetDinhChao) {
        if (!quyetDinh.hopLe) return
        val cau = quyetDinh.docTts
        if (quyetDinh.phatChuongTingTong) {
            scope.launch(Dispatchers.Default) {
                phatTingTong()
                if (!cau.isNullOrBlank()) {
                    delay(420L)
                    docTiengViet(cau)
                }
            }
        } else if (!cau.isNullOrBlank()) {
            docTiengViet(cau)
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

    private fun docTiengViet(noiDung: String) {
        val current = tts
        if (current == null) {
            cauCho = noiDung
            runCatching {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val engine = tts
                        if (engine != null) {
                            val vi = Locale("vi", "VN")
                            val r = engine.setLanguage(vi)
                            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                                engine.setLanguage(Locale.getDefault())
                            }
                            engine.setSpeechRate(0.98f)
                            sanSang = true
                            val pending = cauCho
                            cauCho = null
                            if (!pending.isNullOrBlank()) {
                                engine.speak(pending, TextToSpeech.QUEUE_FLUSH, null, "face_greet")
                            }
                        }
                    }
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
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        sanSang = false
        cauCho = null
    }
}
