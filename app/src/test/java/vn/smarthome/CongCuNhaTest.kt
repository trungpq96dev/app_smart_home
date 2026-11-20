package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.giongnoi.CongCuNha
import vn.smarthome.data.giongnoi.HanhDongNha

/**
 * Kiem thu phan DOC LENH cua Gemini - cho de sai nhat cua tinh nang giong noi.
 *
 * Gemini la mot cai hop den: no co the goi ham voi tham so thieu, sai chinh
 * ta, hoac ten phong khong ton tai. Moi truong hop do PHAI tra ve mot cau
 * tieng Viet de Gemini hoi lai nguoi dung, khong duoc nem ngoai le lam dut
 * ca phien noi chuyen.
 *
 * Chay bang Robolectric vi `org.json` la thu vien cua Android, khong co san
 * tren JVM thuan - giong [DienStateTest].
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CongCuNhaTest {

    /**
     * Ban gia cua [HanhDongNha]: ghi lai da bi goi gi, va giu mot trang thai
     * toi thieu de thu phep "dao".
     */
    private class NhaGia : HanhDongNha {
        val daGoi = mutableListOf<String>()
        val den = mutableMapOf("living" to true, "bed1" to false)
        val oNhanh = mutableMapOf("light" to false, "curtain" to false)
        var nhietDo = 26
        var manDangMo: String? = null
        var soLanGuiMqtt = 0

        override fun maPhong() = listOf("living", "bed1")

        override fun tenPhong(ma: String): String? = when (ma) {
            "living" -> "Phòng khách"
            "bed1" -> "Phòng ngủ 1"
            else -> null
        }

        override fun datDen(phong: String, bat: Boolean) {
            den[phong] = bat
            daGoi += "datDen($phong,$bat)"
        }

        override fun datQuat(phong: String, bat: Boolean) {
            daGoi += "datQuat($phong,$bat)"
        }

        override fun datDieuHoa(phong: String, bat: Boolean) {
            daGoi += "datDieuHoa($phong,$bat)"
        }

        override fun datLichHen(phong: String, bat: Boolean) {
            daGoi += "datLichHen($phong,$bat)"
        }

        override fun datNhietDo(phong: String, doC: Int): Int {
            nhietDo = doC.coerceIn(16, 30)
            daGoi += "datNhietDo($phong,$doC)"
            return nhietDo
        }

        override fun chinhNhietDo(phong: String, buoc: Int): Int {
            nhietDo = (nhietDo + buoc).coerceIn(16, 30)
            daGoi += "chinhNhietDo($phong,$buoc)"
            return nhietDo
        }

        override fun datCheDo(phong: String, cheDo: String) {
            daGoi += "datCheDo($phong,$cheDo)"
        }

        override fun datONhanh(id: String, bat: Boolean) {
            oNhanh[id] = bat
            daGoi += "datONhanh($id,$bat)"
        }

        override fun dangBat(phong: String, thietBi: String): Boolean? =
            if (phong == "nhanh") oNhanh[thietBi]
            else if (thietBi == "den") den[phong] else null

        override fun moManHinh(dich: String): Boolean {
            val co = dich in listOf("camera", "thong_ke") || dich == "phong:living"
            if (co) manDangMo = dich
            return co
        }

        override fun docSoLieu(muc: String): JSONObject =
            JSONObject().put("co_so", true).put("muc", muc)

        override fun chupTrangThai() = "- Phòng khách: đèn bật"

        override fun guiMqtt(
            phong: String,
            thietBi: String,
            hanhDong: String,
            giaTri: String?,
        ): Boolean {
            soLanGuiMqtt++
            return true
        }
    }

    private fun args(vararg cap: Pair<String, String>) =
        JSONObject().apply { cap.forEach { put(it.first, it.second) } }

    // ---------- duong binh thuong ----------

    @Test
    fun bat_den_mot_phong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "bed1", "thiet_bi" to "den", "hanh_dong" to "bat"),
        )
        assertTrue(r.getBoolean("ok"))
        assertTrue(nha.daGoi.contains("datDen(bed1,true)"))
        // Cau tra ve phai co ten tieng Viet de Gemini doc len, khong phai "bed1".
        assertTrue(r.getString("ket_qua").contains("Phòng ngủ 1"))
    }

    /**
     * Cho de sai nhat: "bat den" luc den DANG BAT khong duoc tat no di.
     *
     * Nut tren man hinh thi dao la dung, giong noi thi khong - nguoi ta khong
     * nhin man hinh khi noi.
     */
    @Test
    fun bat_den_dang_bat_thi_van_la_bat() {
        val nha = NhaGia()
        CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "den", "hanh_dong" to "bat"),
        )
        assertEquals(true, nha.den["living"])
    }

    @Test
    fun dao_den_thi_doi_theo_trang_thai_hien_tai() {
        val nha = NhaGia()
        val cc = CongCuNha(nha)
        cc.chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "den", "hanh_dong" to "dao"),
        )
        assertEquals(false, nha.den["living"])
        cc.chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "den", "hanh_dong" to "dao"),
        )
        assertEquals(true, nha.den["living"])
    }

    @Test
    fun tat_ca_phong_thi_chay_het_moi_phong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "tat_ca", "thiet_bi" to "den", "hanh_dong" to "tat"),
        )
        assertTrue(r.getBoolean("ok"))
        assertTrue(nha.daGoi.contains("datDen(living,false)"))
        assertTrue(nha.daGoi.contains("datDen(bed1,false)"))
    }

    @Test
    fun dat_nhiet_do_dieu_hoa() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "living", "thiet_bi" to "nhiet_do",
                "hanh_dong" to "dat", "gia_tri" to "24",
            ),
        )
        assertTrue(r.getBoolean("ok"))
        assertEquals(24, nha.nhietDo)
    }

    @Test
    fun bat_dieu_hoa_kem_nhiet_do_thi_lam_ca_hai() {
        val nha = NhaGia()
        CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "living", "thiet_bi" to "dieu_hoa",
                "hanh_dong" to "bat", "gia_tri" to "23",
            ),
        )
        assertTrue(nha.daGoi.contains("datDieuHoa(living,true)"))
        assertEquals(23, nha.nhietDo)
    }

    @Test
    fun tang_giam_nhiet_do() {
        val nha = NhaGia()
        val cc = CongCuNha(nha)
        cc.chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "nhiet_do", "hanh_dong" to "tang"),
        )
        assertEquals(27, nha.nhietDo)
        cc.chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "living", "thiet_bi" to "nhiet_do",
                "hanh_dong" to "giam", "gia_tri" to "3",
            ),
        )
        assertEquals(24, nha.nhietDo)
    }

    @Test
    fun dat_che_do_phong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "bed1", "thiet_bi" to "che_do",
                "hanh_dong" to "dat", "gia_tri" to "ngu",
            ),
        )
        assertTrue(r.getBoolean("ok"))
        assertTrue(nha.daGoi.contains("datCheDo(bed1,ngu)"))
    }

    @Test
    fun o_thao_tac_nhanh_khong_can_phong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "nhanh", "thiet_bi" to "rem", "hanh_dong" to "bat"),
        )
        assertTrue(r.getBoolean("ok"))
        assertEquals(true, nha.oNhanh["curtain"])
    }

    // ---------- duong xau: Gemini goi bay ----------

    @Test
    fun phong_khong_ton_tai_thi_bao_loi_kem_danh_sach_phong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "phong_tam", "thiet_bi" to "den", "hanh_dong" to "bat"),
        )
        assertFalse(r.getBoolean("ok"))
        // Phai liet ke phong dang co, de Gemini hoi lai cho dung thay vi noi
        // chung chung "khong lam duoc".
        assertTrue(r.getString("loi").contains("Phòng khách"))
        assertTrue(nha.daGoi.isEmpty())
    }

    @Test
    fun nhiet_do_ngoai_khoang_thi_kep_lai_va_noi_ro() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "living", "thiet_bi" to "nhiet_do",
                "hanh_dong" to "dat", "gia_tri" to "99",
            ),
        )
        assertEquals(30, nha.nhietDo)
        assertTrue(r.getString("ket_qua").contains("16"))
    }

    @Test
    fun thieu_tham_so_thi_bao_loi_chu_khong_nem_ngoai_le() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(CongCuNha.DIEU_KHIEN, JSONObject())
        assertFalse(r.getBoolean("ok"))
        assertTrue(r.getString("loi").isNotBlank())
    }

    /**
     * Gemini goi "dat nhiet do" ma quen so: phai bao HONG.
     *
     * Bao thanh cong kem cau giai thich thi Gemini thay `ok` va di xac nhan
     * voi nguoi dung la da chinh xong - trong khi dieu hoa khong doi gi.
     */
    @Test
    fun dat_nhiet_do_ma_thieu_so_thi_bao_hong() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "nhiet_do", "hanh_dong" to "dat"),
        )
        assertFalse(r.getBoolean("ok"))
        assertEquals(26, nha.nhietDo)
    }

    @Test
    fun che_do_khong_co_that_thi_bao_hong_kem_danh_sach() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args(
                "phong" to "bed1", "thiet_bi" to "che_do",
                "hanh_dong" to "dat", "gia_tri" to "tiec_tung",
            ),
        )
        assertFalse(r.getBoolean("ok"))
        assertTrue(r.getString("loi").contains("Vắng nhà"))
        assertTrue(nha.daGoi.isEmpty())
    }

    @Test
    fun ten_cong_cu_la_thi_bao_loi() {
        val r = CongCuNha(NhaGia()).chay("xoa_het_du_lieu", JSONObject())
        assertFalse(r.getBoolean("ok"))
    }

    @Test
    fun hanh_dong_khong_hop_le_thi_bao_loi() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "den", "hanh_dong" to "nhay_mua"),
        )
        assertFalse(r.getBoolean("ok"))
        assertTrue(nha.daGoi.isEmpty())
    }

    // ---------- mo man hinh ----------

    @Test
    fun mo_man_hinh_co_that() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(CongCuNha.MO_MAN_HINH, args("dich" to "camera"))
        assertTrue(r.getBoolean("ok"))
        assertEquals("camera", nha.manDangMo)
    }

    @Test
    fun mo_man_hinh_khong_co_thi_bao_loi() {
        val nha = NhaGia()
        val r = CongCuNha(nha).chay(CongCuNha.MO_MAN_HINH, args("dich" to "may_giat"))
        assertFalse(r.getBoolean("ok"))
        assertEquals(null, nha.manDangMo)
    }

    // ---------- doc so lieu ----------

    @Test
    fun doc_so_lieu_muc_hop_le() {
        val r = CongCuNha(NhaGia()).chay(CongCuNha.DOC_SO_LIEU, args("muc" to "dien"))
        assertTrue(r.getBoolean("ok"))
        assertEquals("dien", r.getJSONObject("so_lieu").getString("muc"))
    }

    @Test
    fun doc_so_lieu_muc_bia_ra_thi_bao_loi() {
        val r = CongCuNha(NhaGia()).chay(CongCuNha.DOC_SO_LIEU, args("muc" to "gia_vang"))
        assertFalse(r.getBoolean("ok"))
    }

    // ---------- MQTT ----------

    @Test
    fun khong_bat_gui_mqtt_thi_khong_ban_gi() {
        val nha = NhaGia()
        CongCuNha(nha, guiMqtt = false).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "living", "thiet_bi" to "den", "hanh_dong" to "bat"),
        )
        assertEquals(0, nha.soLanGuiMqtt)
    }

    @Test
    fun bat_gui_mqtt_thi_moi_phong_mot_lenh() {
        val nha = NhaGia()
        CongCuNha(nha, guiMqtt = true).chay(
            CongCuNha.DIEU_KHIEN,
            args("phong" to "tat_ca", "thiet_bi" to "den", "hanh_dong" to "tat"),
        )
        assertEquals(2, nha.soLanGuiMqtt)
    }

    // ---------- khai bao cong cu ----------

    @Test
    fun khai_bao_du_ba_cong_cu_va_co_du_ma_phong() {
        val ds = CongCuNha.khaiBao(listOf("living", "bed1"))
        val ham = ds.getJSONObject(0).getJSONArray("functionDeclarations")
        assertEquals(3, ham.length())

        val ten = (0 until ham.length()).map { ham.getJSONObject(it).getString("name") }
        assertTrue(ten.contains(CongCuNha.DIEU_KHIEN))
        assertTrue(ten.contains(CongCuNha.MO_MAN_HINH))
        assertTrue(ten.contains(CongCuNha.DOC_SO_LIEU))

        // Ma phong phai di vao enum, neu khong Gemini se tu bia ten phong.
        val enumPhong = ham.getJSONObject(0)
            .getJSONObject("parameters").getJSONObject("properties")
            .getJSONObject("phong").getJSONArray("enum")
        val dsPhong = (0 until enumPhong.length()).map { enumPhong.getString(it) }
        assertTrue(dsPhong.contains("living"))
        assertTrue(dsPhong.contains("bed1"))
        assertTrue(dsPhong.contains("tat_ca"))
        assertTrue(dsPhong.contains("nhanh"))
    }
}
