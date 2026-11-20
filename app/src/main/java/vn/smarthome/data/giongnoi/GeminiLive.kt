package vn.smarthome.data.giongnoi

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/**
 * Noi chuyen voi Gemini Live API bang WebSocket.
 *
 * Giao thuc doi chieu tai lieu Google ngay 20/11/2026 (ban v1beta):
 *
 *  1. Client gui `setup` -> doi `setupComplete`. CHUA duoc gui tieng truoc do.
 *  2. Client gui `realtimeInput.audio` = PCM 16-bit 16kHz mono, base64.
 *  3. Server gui `serverContent.modelTurn.parts[].inlineData` = PCM 24kHz, base64.
 *  4. Server gui `toolCall.functionCalls[]` -> client tra `toolResponse`.
 *
 * Lop nay CHI lo phan giao thuc: dong goi/mo goi JSON, base64, va dich loi.
 * Phan mic/loa nam o [AmThanh], phan ghep thanh phien nam o
 * [vn.smarthome.vm.TroLyGiongNoi].
 */
class GeminiLive(private val nghe: Nghe) {

    /** Nhung gi lop tren can biet. Goi tu luong cua OkHttp, khong phai luong chinh. */
    interface Nghe {
        /** Bat tay xong, da nhan `setupComplete`. Tu luc nay moi duoc gui tieng. */
        fun sanSang()

        /** Mot mieng tieng Gemini noi, PCM 16-bit 24kHz mono. */
        fun tiengNoi(pcm: ByteArray)

        /** Nguoi dung chen ngang - phai bo ngay phan tieng chua phat het. */
        fun biNgatLoi()

        /** Gemini noi xong mot luot. */
        fun hetLuot()

        /** Loi thoai da nhan dang duoc. [cuaMinh] = cau nguoi dung noi. */
        fun loiThoai(cau: String, cuaMinh: Boolean)

        /** Gemini goi ham. Phai tra ket qua bang [traKetQuaCongCu]. */
        fun goiCongCu(id: String?, ten: String, args: JSONObject)

        /**
         * Server bao sap dong duong (`goAway`) - thuong vi phien da dai.
         * Day la bao truoc, khong phai da dut.
         */
        fun sapHetPhien(conLaiMs: Long?)

        /** Dut han. [cau] da duoc dich sang tieng Viet cho nguoi doc. */
        fun dut(cau: String?)
    }

    private val client = OkHttpClient.Builder()
        // Live API la duong dai, khong duoc dat timeout doc: de mac dinh 10
        // giay thi dang nghe mot cau dai la bi cat ngang.
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        // Ping de thay duong chet som khi may tinh bang roi Wi-Fi.
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var ws: WebSocket? = null

    @Volatile
    private var daSanSang = false

    /**
     * So thu tu duong noi. Tang moi lan [noi] hoac [dong].
     *
     * `close()` la bat dong bo: bam tat roi bam bat lai ngay thi `onClosing`
     * cua duong CU van con dang tren duong ve. Khong chan thi no bao "dut" va
     * giet dung phien MOI vua mo - bieu hien la tro ly tu tat sau khi bat
     * chung mot giay, khong thay ly do o dau.
     *
     * Dem the he chu khong so sanh voi bien `ws`: `ws` chi duoc gan SAU khi
     * `newWebSocket` tra ve, con the he thi tang TRUOC khi mo duong, nen
     * khong co khe nao de goi den dau tien bi bo nham.
     */
    private val theHe = java.util.concurrent.atomic.AtomicInteger(0)

    /** Mo duong va gui `setup`. Khong chan luong goi. */
    fun noi(cauHinh: CauHinhGiongNoi, loiDan: String, congCu: JSONArray) {
        dong()
        daSanSang = false
        val heNay = theHe.incrementAndGet()
        val req = Request.Builder().url(cauHinh.duongWs()).build()
        ws = client.newWebSocket(req, object : WebSocketListener() {

            private fun conDung() = theHe.get() == heNay

            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(dungSetup(cauHinh, loiDan, congCu).toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (conDung()) doc(text)
            }

            // Live API gui JSON duoi dang khung NHI PHAN, khong phai khung chu.
            // Phai bat ca hai: cac ban server khac nhau gui khac nhau, va bo
            // sot mot ben thi bieu hien la "noi duoc ma khong nghe thay gi".
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (conDung()) doc(bytes.utf8())
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                if (!conDung()) return
                nghe.dut(dichMaDong(code, reason))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!conDung()) return
                nghe.dut(dienGiaiLoi(t, response))
            }
        })
    }

    /**
     * Gui mot mieng tieng tu mic.
     *
     * Bo qua khi chua nhan `setupComplete`: gui truoc do thi server dong duong
     * ngay voi mot ma loi kho hieu.
     */
    fun guiTieng(pcm: ByteArray, soByte: Int) {
        if (!daSanSang) return
        val w = ws ?: return
        val b64 = Base64.encodeToString(pcm, 0, soByte, Base64.NO_WRAP)
        w.send(
            JSONObject().put(
                "realtimeInput",
                JSONObject().put(
                    "audio",
                    JSONObject()
                        .put("data", b64)
                        .put("mimeType", "audio/pcm;rate=16000"),
                ),
            ).toString(),
        )
    }

    /** Bao cho server biet luong tieng tam dung - dung khi ban song cong khoa mic. */
    fun bangTiengDungLai() {
        if (!daSanSang) return
        ws?.send(JSONObject().put("realtimeInput", JSONObject().put("audioStreamEnd", true)).toString())
    }

    fun traKetQuaCongCu(id: String?, ten: String, ketQua: JSONObject) {
        val w = ws ?: return
        val mot = JSONObject().put("name", ten).put("response", ketQua)
        if (id != null) mot.put("id", id)
        w.send(
            JSONObject().put(
                "toolResponse",
                JSONObject().put("functionResponses", JSONArray().put(mot)),
            ).toString(),
        )
    }

    fun dong() {
        daSanSang = false
        // Tang the he TRUOC khi close: tu giay nay moi goi den cua duong cu
        // deu bi bo, ke ca cai dang tren duong ve.
        theHe.incrementAndGet()
        ws?.close(1000, "nguoi dung dong phien")
        ws = null
    }

    // ---------------------------------------------------------------

    private fun doc(json: String) {
        val o = try {
            JSONObject(json)
        } catch (e: Exception) {
            return
        }

        if (o.has("setupComplete")) {
            daSanSang = true
            nghe.sanSang()
            return
        }

        o.optJSONObject("serverContent")?.let { sc ->
            if (sc.optBoolean("interrupted")) nghe.biNgatLoi()

            sc.optJSONObject("inputTranscription")?.optString("text")
                ?.takeIf { it.isNotBlank() }?.let { nghe.loiThoai(it, cuaMinh = true) }
            sc.optJSONObject("outputTranscription")?.optString("text")
                ?.takeIf { it.isNotBlank() }?.let { nghe.loiThoai(it, cuaMinh = false) }

            sc.optJSONObject("modelTurn")?.optJSONArray("parts")?.let { parts ->
                for (i in 0 until parts.length()) {
                    val inline = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
                    val data = inline.optString("data")
                    if (data.isNotEmpty()) {
                        val pcm = try {
                            Base64.decode(data, Base64.DEFAULT)
                        } catch (e: IllegalArgumentException) {
                            continue
                        }
                        nghe.tiengNoi(pcm)
                    }
                }
            }

            if (sc.optBoolean("turnComplete")) nghe.hetLuot()
        }

        o.optJSONObject("toolCall")?.optJSONArray("functionCalls")?.let { calls ->
            for (i in 0 until calls.length()) {
                val c = calls.optJSONObject(i) ?: continue
                val ten = c.optString("name")
                if (ten.isEmpty()) continue
                nghe.goiCongCu(
                    id = c.optString("id").ifEmpty { null },
                    ten = ten,
                    args = c.optJSONObject("args") ?: JSONObject(),
                )
            }
        }

        o.optJSONObject("goAway")?.let { ga ->
            // `timeLeft` ve duoi dang chuoi thoi gian kieu "10s".
            val s = ga.optString("timeLeft").removeSuffix("s").toDoubleOrNull()
            nghe.sapHetPhien(s?.let { (it * 1000).toLong() })
        }
    }

    companion object {

        /**
         * Dung goi `setup`.
         *
         * Dat `responseModalities` va `speechConfig` o CAP NGOAI cua `setup`,
         * dung nhu vi du trong tai lieu Google ngay 20/11/2026. Model phai co
         * tien to `models/`.
         */
        fun dungSetup(
            cauHinh: CauHinhGiongNoi,
            loiDan: String,
            congCu: JSONArray,
        ): JSONObject {
            val setup = JSONObject()
                .put("model", "models/" + cauHinh.model.trim().removePrefix("models/"))
                .put("responseModalities", JSONArray().put("AUDIO"))
                .put(
                    "speechConfig",
                    JSONObject().put(
                        "voiceConfig",
                        JSONObject().put(
                            "prebuiltVoiceConfig",
                            JSONObject().put("voiceName", cauHinh.giongDoc),
                        ),
                    ),
                )
                .put(
                    "systemInstruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", loiDan))),
                )
                .put("tools", congCu)
                // Hai dong nay la thu cho bang thoai hien duoc cau DA NGHE duoc.
                // Khong co chung thi khi Gemini nghe nham ten phong, nguoi dung
                // khong co cach nao biet no nham o dau.
                .put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject())
                .put(
                    "realtimeInputConfig",
                    JSONObject().put(
                        "automaticActivityDetection",
                        // 800ms la mac dinh cua server. Dat 600 cho tieng Viet:
                        // noi co dau ngat giua cau nhieu hon, de 800 thi cam
                        // giac tro ly cham chap.
                        JSONObject().put("silenceDurationMs", 600),
                    ),
                )
            return JSONObject().put("setup", setup)
        }

        /**
         * Dich loi mang sang cau nguoi doc duoc.
         *
         * Giong [vn.smarthome.data.camera.Go2rtc.dienGiaiLoi]: nguoi dung phai
         * biet di sua o dau, khong phai doc ten lop Java.
         */
        fun dienGiaiLoi(t: Throwable, response: Response?): String {
            val ma = response?.code
            return when {
                ma == 400 -> "Gemini từ chối: sai tên model hoặc sai cấu hình. " +
                    "Kiểm tra ô Model trong Cài đặt."
                ma == 401 || ma == 403 -> "Khóa API Gemini không dùng được. " +
                    "Kiểm tra lại khóa trong Cài đặt."
                ma == 404 -> "Không có model này. Kiểm tra ô Model trong Cài đặt."
                ma == 429 -> "Gemini báo vượt hạn mức. Đợi một lúc rồi thử lại."
                ma != null && ma >= 500 -> "Máy chủ Gemini đang lỗi ($ma). Thử lại sau."
                t is UnknownHostException ->
                    "Không phân giải được generativelanguage.googleapis.com - máy tính bảng mất mạng."
                t is SocketTimeoutException -> "Gemini không trả lời kịp."
                t is SSLException -> "Bắt tay TLS với Gemini hỏng."
                t is EOFException -> "Gemini đóng đường đột ngột."
                else -> "Mất kết nối tới Gemini: " + (t.message ?: t.javaClass.simpleName)
            }
        }

        /** Null nghia la dong binh thuong, khong can bao gi len man hinh. */
        fun dichMaDong(code: Int, reason: String): String? = when {
            code == 1000 -> null
            // 1011 thuong di kem cau giai thich cua server, giu nguyen vi no
            // noi dung cho nao sai (vd ten model).
            reason.isNotBlank() -> "Gemini đóng kết nối: $reason"
            else -> "Gemini đóng kết nối (mã $code)."
        }
    }
}
