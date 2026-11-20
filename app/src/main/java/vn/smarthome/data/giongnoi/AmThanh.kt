package vn.smarthome.data.giongnoi

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/**
 * Thu tieng tu mic cho Gemini Live: PCM 16-bit, mono, 16000 Hz.
 *
 * Nguon am phai la [MediaRecorder.AudioSource.VOICE_COMMUNICATION]. Day la
 * thiet lap quan trong nhat cua ca tinh nang: no bao cho Android biet day la
 * cuoc thoai, nho do chip DSP cua may moi bat duong khu vong phan cung. Dung
 * MIC thuong thi tieng loa lot thang vao mic va Gemini se nghe chinh no noi.
 */
class ThuAm(private val chunkNghe: (ByteArray, Int) -> Unit) {

    private var rec: AudioRecord? = null
    private var khuVong: AcousticEchoCanceler? = null
    private var khuOn: NoiseSuppressor? = null
    private var canhAm: AutomaticGainControl? = null
    private val dangChay = AtomicBoolean(false)

    /** Mic co bi khoa tam khong - dung cho che do ban song cong. */
    private val khoa = AtomicBoolean(false)

    /** May nay co bo khu vong phan cung hay khong. Biet sau khi [batDau]. */
    var coKhuVong: Boolean = false
        private set

    /** Mot mieng tieng bao nhieu byte. 3200 byte = 100ms o 16kHz mono 16-bit. */
    private val coMieng = 3200

    /**
     * @return null neu mo duoc mic, nguoc lai la cau bao loi cho nguoi doc.
     */
    @SuppressLint("MissingPermission")
    fun batDau(): String? {
        if (dangChay.get()) return null

        val coToiThieu = AudioRecord.getMinBufferSize(
            TAN_SO_VAO,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (coToiThieu <= 0) return "Máy không mở được micro ở 16kHz."

        val r = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                TAN_SO_VAO,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(coToiThieu, coMieng * 4),
            )
        } catch (e: Exception) {
            return "Không mở được micro: " + (e.message ?: e.javaClass.simpleName)
        }

        if (r.state != AudioRecord.STATE_INITIALIZED) {
            r.release()
            return "Micro đang bị ứng dụng khác giữ."
        }

        batBoLoc(r.audioSessionId)

        try {
            r.startRecording()
        } catch (e: IllegalStateException) {
            r.release()
            return "Không bắt đầu thu được - có thể chưa cấp quyền micro."
        }

        rec = r
        dangChay.set(true)

        thread(name = "ThuAmGemini") {
            val dem = ByteArray(coMieng)
            while (dangChay.get()) {
                val doc = try {
                    r.read(dem, 0, dem.size)
                } catch (e: Exception) {
                    break
                }
                if (doc <= 0) continue
                // Khoa mic thi VAN doc het dem (khong doc thi dem day va tieng
                // cu bi giu lai, luc mo khoa se gui mot cuc tieng qua khu) -
                // chi khong gui di.
                if (!khoa.get()) chunkNghe(dem, doc)
            }
        }
        return null
    }

    fun khoaMic(khoa: Boolean) = this.khoa.set(khoa)

    fun dangKhoa(): Boolean = khoa.get()

    fun dung() {
        if (!dangChay.getAndSet(false)) return
        try {
            khuVong?.release(); khuVong = null
            khuOn?.release(); khuOn = null
            canhAm?.release(); canhAm = null
            rec?.stop()
            rec?.release()
        } catch (e: Exception) {
            // Dung mic hong thi khong con gi de cuu - nuot de khong che mat
            // ly do that su lam phien dut.
        }
        rec = null
    }

    /**
     * Bat ba bo loc thoai cua Android. Deu co the khong co tren may re -
     * `isAvailable` false thi bo qua, khong phai loi.
     */
    private fun batBoLoc(maPhien: Int) {
        coKhuVong = false
        try {
            if (AcousticEchoCanceler.isAvailable()) {
                khuVong = AcousticEchoCanceler.create(maPhien)?.apply { enabled = true }
                coKhuVong = khuVong?.enabled == true
            }
            if (NoiseSuppressor.isAvailable()) {
                khuOn = NoiseSuppressor.create(maPhien)?.apply { enabled = true }
            }
            if (AutomaticGainControl.isAvailable()) {
                canhAm = AutomaticGainControl.create(maPhien)?.apply { enabled = true }
            }
        } catch (e: Exception) {
            coKhuVong = false
        }
    }

    companion object {
        const val TAN_SO_VAO = 16000
    }
}

/**
 * Phat tieng Gemini tra ve: PCM 16-bit, mono, 24000 Hz.
 *
 * Viet vao [AudioTrack] tu mot luong rieng qua hang doi, de luong doc
 * WebSocket khong bi chan khi loa day.
 */
class PhatAm {

    private var track: AudioTrack? = null
    private val hangDoi = LinkedBlockingQueue<ByteArray>()
    private val dangChay = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Tong so khung da ghi vao AudioTrack. So sanh voi dau doc de biet con tieng chua. */
    private val khungDaGhi = AtomicLong(0)

    fun batDau(): String? {
        if (dangChay.get()) return null

        val coToiThieu = AudioTrack.getMinBufferSize(
            TAN_SO_RA,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (coToiThieu <= 0) return "Máy không phát được âm 24kHz."

        val t = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        // VOICE_COMMUNICATION o CA HAI dau (thu va phat) thi
                        // Android moi ghep duoc cap tin hieu cho bo khu vong.
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(TAN_SO_RA)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(coToiThieu * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            return "Không mở được loa: " + (e.message ?: e.javaClass.simpleName)
        }

        track = t
        khungDaGhi.set(0)
        dangChay.set(true)
        t.play()

        thread(name = "PhatAmGemini") {
            while (dangChay.get()) {
                val mieng = try {
                    hangDoi.take()
                } catch (e: InterruptedException) {
                    break
                }
                if (mieng.isEmpty()) continue
                try {
                    val ghi = t.write(mieng, 0, mieng.size)
                    if (ghi > 0) khungDaGhi.addAndGet((ghi / 2).toLong())
                } catch (e: Exception) {
                    break
                }
            }
        }
        return null
    }

    fun phat(pcm: ByteArray) {
        if (dangChay.get()) hangDoi.offer(pcm)
    }

    /**
     * Bo het tieng chua phat.
     *
     * Goi khi Gemini bao `interrupted`: nguoi dung da chen ngang, phan tieng
     * con lai la cau tra loi cho cau hoi CU - phat tiep la noi lac de.
     */
    fun xoaHangDoi() {
        hangDoi.clear()
        val t = track ?: return
        try {
            t.pause()
            t.flush()
            khungDaGhi.set(t.playbackHeadPosition.toLong())
            t.play()
        } catch (e: Exception) {
            // Track da hong thi phien cung sap dut, khong co gi de lam them.
        }
    }

    /**
     * Loa con tieng chua phat het khong.
     *
     * Dung cho che do ban song cong: con tieng thi khoa mic. So sanh so khung
     * da ghi voi dau doc cua phan cung chu khong phai hang doi - hang doi rong
     * khong co nghia la loa da im, phan cung van con mot doan dem.
     */
    fun dangPhat(): Boolean {
        if (!dangChay.get()) return false
        if (hangDoi.isNotEmpty()) return true
        val t = track ?: return false
        return try {
            khungDaGhi.get() - t.playbackHeadPosition.toLong() > KHUNG_COI_NHU_IM
        } catch (e: Exception) {
            false
        }
    }

    fun dung() {
        if (!dangChay.getAndSet(false)) return
        hangDoi.clear()
        // Mot mieng rong de luong dang nam trong take() tinh day ma thoat.
        hangDoi.offer(ByteArray(0))
        try {
            track?.pause()
            track?.flush()
            track?.stop()
            track?.release()
        } catch (e: Exception) {
            // Nhu tren.
        }
        track = null
    }

    companion object {
        const val TAN_SO_RA = 24000

        /**
         * Duoi nguong nay thi coi nhu loa da im.
         *
         * 1200 khung o 24kHz = 50ms. Khong de 0 vi `playbackHeadPosition` nhay
         * theo tung buoc cua phan cung, de 0 thi mic bat/tat lien tuc o cuoi
         * moi cau.
         */
        const val KHUNG_COI_NHU_IM = 1200L
    }
}

/**
 * Dat may o che do thoai trong luc noi chuyen.
 *
 * Tra may ve che do cu khi xong: de nguyen MODE_IN_COMMUNICATION thi am luong
 * cua ca may bi keo theo duong thoai, nhac va tieng camera nho han han.
 */
class CheDoThoai(ctx: Context) {

    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var cheDoCu: Int? = null

    // isSpeakerphoneOn bi danh dau deprecated tu API 31 (thay bang
    // setCommunicationDevice), nhung ban thay the chi co tu 31 trong khi
    // minSdk la 26 - va ham cu van chay dung tren API 35.
    @Suppress("DEPRECATION")
    fun bat() {
        val a = am ?: return
        try {
            cheDoCu = a.mode
            a.mode = AudioManager.MODE_IN_COMMUNICATION
            a.isSpeakerphoneOn = true
        } catch (e: Exception) {
            cheDoCu = null
        }
    }

    fun tat() {
        val a = am ?: return
        val cu = cheDoCu ?: return
        try {
            a.mode = cu
        } catch (e: Exception) {
            // Khong tra ve duoc thi thoi, khong dang de dut phien.
        }
        cheDoCu = null
    }
}
