package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.giongnoi.CongCuNha
import vn.smarthome.data.giongnoi.GeminiLive

/**
 * Kiem thu phan GIAO THUC noi voi Gemini Live.
 *
 * Khong noi mang that: chi kiem goi `setup` dung hinh dang ma tai lieu Google
 * mo ta (doi chieu ngay 20/11/2026). Sai mot ten truong o day thi bieu hien
 * tren may that chi la "noi duoc ma tro ly cam nhu hen" - rat kho lan ra.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GeminiLiveTest {

    private fun setup(c: CauHinhGiongNoi = CauHinhGiongNoi(khoaApi = "k")) =
        GeminiLive.dungSetup(c, "lời dặn", CongCuNha.khaiBao(listOf("living")))
            .getJSONObject("setup")

    @Test
    fun model_phai_co_tien_to_models() {
        val s = setup(CauHinhGiongNoi(khoaApi = "k", model = "gemini-3.8-live"))
        assertEquals("models/gemini-3.8-live", s.getString("model"))
    }

    /** Nguoi dung go ca "models/..." vao o Cai dat thi khong duoc thanh "models/models/...". */
    @Test
    fun go_san_tien_to_models_thi_khong_nhan_doi() {
        val s = setup(CauHinhGiongNoi(khoaApi = "k", model = "models/gemini-3.8-live"))
        assertEquals("models/gemini-3.8-live", s.getString("model"))
    }

    @Test
    fun xin_tieng_chu_khong_phai_chu() {
        val m = setup().getJSONObject("generationConfig").getJSONArray("responseModalities")
        assertEquals(1, m.length())
        assertEquals("AUDIO", m.getString(0))
    }

    @Test
    fun giong_doc_di_dung_cho() {
        val s = setup(CauHinhGiongNoi(khoaApi = "k", giongDoc = "Kore"))
        assertEquals(
            "Kore",
            s.getJSONObject("generationConfig")
                .getJSONObject("speechConfig")
                .getJSONObject("voiceConfig")
                .getJSONObject("prebuiltVoiceConfig")
                .getString("voiceName"),
        )
    }

    /**
     * Chan dung cai loi da lam hong ban v0.5.0.
     *
     * `responseModalities` va `speechConfig` dat o cap ngoai cua `setup` thi
     * server dong duong ngay voi ma 1007:
     *
     *     Invalid JSON payload received. Unknown name "speechConfig"
     *     at 'setup': Cannot find field.
     *
     * Bai hoc that su: test cu CO chay va CO xanh, nhung no khang dinh dung
     * cai cau truc sai - vi ma nguon va test deu viet tu cung mot gia dinh
     * sai. Test chi chot duoc hinh dang, khong chot duoc hinh dang DUNG.
     * Cho nay phai doi chieu trang API reference, khong phai trang huong dan.
     */
    @Test
    fun setup_chi_duoc_chua_dung_cac_truong_server_nhan() {
        val s = setup()
        val choPhep = setOf(
            "model", "generationConfig", "systemInstruction", "tools",
            "realtimeInputConfig", "sessionResumption", "contextWindowCompression",
            "inputAudioTranscription", "outputAudioTranscription",
            "proactivity", "historyConfig", "labels",
        )
        val dangGui = s.keys().asSequence().toList()
        val la = dangGui.filterNot { it in choPhep }
        assertTrue("Truong server khong nhan: $la", la.isEmpty())

        // Hai cai nay la thu phong o cap ngoai - phai nam trong generationConfig.
        assertFalse(s.has("responseModalities"))
        assertFalse(s.has("speechConfig"))
    }

    @Test
    fun loi_dan_nam_trong_systemInstruction() {
        val s = setup()
        assertEquals(
            "lời dặn",
            s.getJSONObject("systemInstruction").getJSONArray("parts")
                .getJSONObject(0).getString("text"),
        )
    }

    /**
     * Thieu hai truong nay thi bang thoai trong tron: nguoi dung khong co cach
     * nao thay Gemini DA NGHE duoc gi, nen khi no hieu sai ten phong thi khong
     * biet no sai o dau.
     */
    @Test
    fun co_xin_loi_thoai_ca_hai_chieu() {
        val s = setup()
        assertTrue(s.has("inputAudioTranscription"))
        assertTrue(s.has("outputAudioTranscription"))
    }

    @Test
    fun cong_cu_di_kem_setup() {
        val tools: JSONArray = setup().getJSONArray("tools")
        assertEquals(3, tools.getJSONObject(0).getJSONArray("functionDeclarations").length())
    }

    @Test
    fun khoa_api_KHONG_duoc_nam_trong_goi_setup() {
        // Khoa di bang tham so truy van cua URL. Nhet vao goi setup la mot cach
        // de no lot vao log.
        assertFalse(setup().toString().contains("k\""))
    }

    // ---------- duong WebSocket ----------

    @Test
    fun duong_ws_dung_ban_v1beta_va_mang_khoa() {
        val c = CauHinhGiongNoi(khoaApi = "ABC123")
        val u = c.duongWs()
        assertTrue(u.startsWith("wss://generativelanguage.googleapis.com/ws/"))
        assertTrue(u.contains("v1beta.GenerativeService.BidiGenerateContent"))
        assertTrue(u.endsWith("?key=ABC123"))
    }

    @Test
    fun duong_cho_nguoi_xem_thi_che_khoa() {
        val u = CauHinhGiongNoi(khoaApi = "ABC123").choThayTrongLoi()
        assertFalse(u.contains("ABC123"))
        assertTrue(u.endsWith("?key=***"))
    }

    // ---------- dich loi ----------

    @Test
    fun dong_binh_thuong_thi_khong_bao_gi_len_man() {
        assertNull(GeminiLive.dichMaDong(1000, ""))
    }

    @Test
    fun dong_bat_thuong_thi_giu_nguyen_cau_cua_server() {
        // Cau cua server thuong noi dung cho nao sai, vd ten model - cat di la
        // mat manh moi quan trong nhat.
        val cau = GeminiLive.dichMaDong(1011, "model not found") ?: ""
        assertTrue(cau.contains("model not found"))
    }
}
