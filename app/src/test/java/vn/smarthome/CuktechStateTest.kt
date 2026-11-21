package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.cuktech.CongSacId
import vn.smarthome.data.cuktech.KichBan
import vn.smarthome.data.cuktech.CuktechPiid
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.data.cuktech.CuktechTopic

/**
 * Kiem bo giai ma `cuktech/charger/#` bang GOI THAT.
 *
 * Nhung chuoi JSON duoi day khong phai do nghi ra: chung duoc bat tu broker
 * Mosquitto tren serverubuntu ngay 21/11/2026, luc cong C1 dang cam tai. Chep
 * nguyen van. Doi ben kia doi dinh dang thi test nay phai do - do la muc dich.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CuktechStateTest {

    private val goiC1Dangsac = """
        {"voltage":20.1,"current":2.3,"power":46.2,"active":true,"status_raw":1,
         "protocol":"PD","_raw_code":"0x03","_proto_num":7,"enabled":true}
    """.trimIndent()

    private val goiCongTrong = """
        {"voltage":0.0,"current":0.0,"power":0.0,"active":false,"protocol":"idle"}
    """.trimIndent()

    private val goiStatus = """
        {"connected":true,"authenticated":true,
         "device_model":"njcuk.fitting.ad1204_HS6621C","firmware_version":"2.1.2_0073",
         "ports":{"1":{"voltage":20.1,"current":2.3,"power":46.2,"active":true,"protocol":"PD","enabled":true},
                  "2":{"voltage":0.0,"current":0.0,"power":0.0,"active":false,"protocol":"idle","enabled":true},
                  "3":{"voltage":0.0,"current":0.0,"power":0.0,"active":false,"protocol":"idle","enabled":true},
                  "4":{"voltage":5.1,"current":0.3,"power":1.5,"active":true,"protocol":"5V","enabled":true}}}
    """.trimIndent()

    @Test
    fun trang_thai_rong_khong_bia_so() {
        val s = CuktechState()
        assertFalse(s.coSoThat)
        assertEquals(0f, s.tongOat, 0.001f)
        assertEquals(0, s.soCongDangChay)
        // Van liet ke du 4 cong de man hinh ve duoc khung, nhung tat ca deu 0.
        assertEquals(4, s.danhSachCong().size)
        assertTrue(s.danhSachCong().none { it.dangChay })
    }

    @Test
    fun doc_duoc_cong_dang_sac_tu_topic_cong_roi() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.TIEN_TO_CONG + "c1", goiC1Dangsac)
        val c1 = s.cong[CongSacId.C1]!!

        assertTrue(s.coSoThat)
        assertEquals(20.1f, c1.volt, 0.01f)
        assertEquals(2.3f, c1.ampe, 0.01f)
        assertEquals(46.2f, c1.oat, 0.01f)
        assertTrue(c1.dangChay)
        assertEquals("PD", c1.giaoThuc)
        assertEquals(true, c1.choPhep)
    }

    @Test
    fun cong_trong_thieu_enabled_thi_giu_null_chu_khong_doan() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.TIEN_TO_CONG + "c2", goiCongTrong)
        val c2 = s.cong[CongSacId.C2]!!

        assertFalse(c2.dangChay)
        // Goi cong trong CHI CO 5 truong. Khai `Boolean = true` la bia ra mot
        // su that khong nam trong goi.
        assertNull(c2.choPhep)
        assertEquals("Trống", c2.nhanGiaoThuc())
    }

    @Test
    fun status_cho_luon_ca_bon_cong() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)

        assertTrue(s.noiDuoc)
        assertTrue(s.xacThuc)
        assertEquals("njcuk.fitting.ad1204_HS6621C", s.model)
        assertEquals("2.1.2_0073", s.firmware)
        assertEquals(4, s.cong.size)
        assertEquals(46.2f, s.cong[CongSacId.C1]!!.oat, 0.01f)
        // Khoa "4" trong status la cong USB-A.
        assertEquals("5V", s.cong[CongSacId.USB_A]!!.giaoThuc)
        assertTrue(s.cong[CongSacId.USB_A]!!.dangChay)
    }

    @Test
    fun tong_cong_suat_la_tong_cong_lai_tu_bon_cong() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        // Cu sac KHONG phat so tong. 46.2 + 0 + 0 + 1.5
        assertEquals(47.7f, s.tongOat, 0.01f)
        assertEquals(2, s.soCongDangChay)
    }

    /**
     * Goi `settings` THAT bat cung luc voi anh chup app CUKTECH tren dien
     * thoai. Tam gia tri duoi day deu doi chieu duoc voi anh do.
     */
    private val goiCaiDat = """
        {"21":50532111,"5":1,"6":2,"8":0,"9":0,"10":0,"11":0,"12":0,
         "13":0,"15":0,"16":15,"17":120391469,"18":522,"19":1,"20":0}
    """.trimIndent()

    @Test
    fun giai_duoc_bang_piid_thanh_cai_dat() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.SETTINGS, goiCaiDat)
        val cd = s.caiDat!!

        assertEquals(KichBan.AI, cd.kichBan)              // PIID 5 = 1  -> app hien "AI"
        assertEquals("10 phút", cd.thoiGianSangMan)       // PIID 6 = 2  -> app hien "10 phút"
        assertEquals(true, cd.tuTatManNhanRoi)            // PIID 19 = 1 -> app bat
        assertEquals(false, cd.khoaXoayMan)               // PIID 20 = 0 -> app tat
        assertEquals(false, cd.sacNhoGiotUsbA)            // PIID 15 = 0 -> app tat
    }

    @Test
    fun mat_na_bit_piid_16_cho_biet_cong_nao_bi_tat() {
        // 16 = 15 = 0b1111 -> ca bon cong deu bat, dung nhu anh chup.
        val s = CuktechState.gop(CuktechState(), CuktechTopic.SETTINGS, goiCaiDat)
        CongSacId.entries.forEach { assertEquals(false, s.congBiTat(it)) }

        // 16 = 13 = 0b1101 -> rieng C2 (bit1) bi tat.
        val tatC2 = CuktechState.gop(CuktechState(), CuktechTopic.SETTINGS, """{"16":13}""")
        assertEquals(false, tatC2.congBiTat(CongSacId.C1))
        assertEquals(true, tatC2.congBiTat(CongSacId.C2))
        assertEquals(false, tatC2.congBiTat(CongSacId.C3))
        assertEquals(false, tatC2.congBiTat(CongSacId.USB_A))
    }

    @Test
    fun chua_co_goi_settings_thi_khong_doan_trang_thai_cong() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        // Khong co `settings` -> khong biet cong bi tat hay khong. Man hinh se
        // KHONG ve nut bat/tat, vi ve ra la phai doan.
        assertNull(s.congBiTat(CongSacId.C1))
    }

    @Test
    fun ty_le_phan_bo_cong_suat() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        val t = s.tyLePhanBo()
        // 46.2 / 47.7 = 96.8% ; 1.5 / 47.7 = 3.1%
        assertEquals(96.8f, t[CongSacId.C1]!!, 0.2f)
        assertEquals(3.1f, t[CongSacId.USB_A]!!, 0.2f)
        // Cong khong sac thi KHONG co trong map - khac han voi "0%".
        assertNull(t[CongSacId.C2])
    }

    @Test
    fun khong_sac_gi_thi_ty_le_rong_chu_khong_phai_bon_cai_0_phan_tram() {
        assertTrue(CuktechState().tyLePhanBo().isEmpty())
    }

    // ---------- co `connected` cu vs so cong dang ve ----------

    /** Ban tin status THAT bi ket o connected:false toi 21/11/2026. */
    private val goiStatusCu = """
        {"connected": false, "device_model":"njcuk.fitting.ad1204_HS6621C",
         "firmware_version":"2.1.2_0073"}
    """.trimIndent()

    @Test
    fun co_bao_mat_nhung_so_cong_van_ve_thi_KHONG_keu_mat_ket_noi() {
        // Dung canh gap that: `cuktech/charger/status` la ban tin retained, no
        // ket o connected:false tu mot phien cu, trong khi bon topic cong van
        // doi so moi 10 giay. Keu "mat ket noi" luc do la noi sai.
        val t0 = 1_000_000L
        var s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatusCu, t0)
        s = CuktechState.gop(s, CuktechTopic.TIEN_TO_CONG + "c1", goiC1Dangsac, t0 + 5_000)

        assertFalse(s.noiDuoc)
        assertFalse("goi moi ve 5 giay truoc, khong the la mat ket noi",
            s.matKetNoiThat(t0 + 6_000))
        assertTrue("day la mau thuan: co bao mat ma so van ve",
            s.coMauThuan(t0 + 6_000))
    }

    @Test
    fun co_bao_mat_va_lau_khong_co_goi_nao_thi_la_mat_that() {
        val t0 = 1_000_000L
        var s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatusCu, t0)
        s = CuktechState.gop(s, CuktechTopic.TIEN_TO_CONG + "c1", goiC1Dangsac, t0)

        val sau = t0 + (CuktechState.GIAY_COI_LA_CU + 5) * 1000L
        assertTrue(s.laSoCu(sau))
        assertTrue(s.matKetNoiThat(sau))
        assertFalse(s.coMauThuan(sau))
    }

    @Test
    fun dang_noi_binh_thuong_thi_khong_canh_bao_gi() {
        val t0 = 1_000_000L
        val s = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus, t0)
        assertTrue(s.noiDuoc)
        assertFalse(s.matKetNoiThat(t0 + 1000))
        assertFalse(s.coMauThuan(t0 + 1000))
    }

    // ---------- bang PIID tho, dung de doi soat sau khi gui lenh ----------

    @Test
    fun giu_lai_bang_piid_tho_de_doi_soat() {
        val s = CuktechState.gop(CuktechState(), CuktechTopic.SETTINGS, goiCaiDat)
        val tho = s.caiDat!!.tho

        // Gui `{"piid":6,"value":3}` xong thi doc lai dung o nay de biet cu
        // sac da nghe chua. Khong doc qua truong da dich: "30 phút" co the
        // den tu nhieu ma khac nhau neu sau nay bang dich doi.
        assertEquals(2, tho[CuktechPiid.SANG_MAN])
        assertEquals(1, tho[CuktechPiid.KICH_BAN])
        assertEquals(0, tho[CuktechPiid.SAC_NHO_GIOT_A])
        assertEquals(1, tho[CuktechPiid.TU_TAT_MAN])
        assertEquals(0, tho[CuktechPiid.KHOA_XOAY])
        assertEquals(15, tho[16])
    }

    @Test
    fun moi_lua_chon_tren_man_hinh_deu_nam_trong_khoang_server_cho_phep() {
        // Khoang lay tu `state.py` cua cuktech-ble-server (PIID_RANGES).
        // Server lang le bo qua gia tri ngoai khoang, nen de nguoi dung bam ra
        // mot gia tri chac chan bi chan la bay nguoi ta.
        CuktechPiid.SANG_MAN_CHON.forEach { (ma, _) ->
            assertTrue("sang man $ma ngoai khoang 0..5", ma in 0..5)
        }
        CuktechPiid.HEN_GIO_CHON.forEach { (phut, _) ->
            assertTrue("hen gio $phut ngoai khoang 0..1440", phut in 0..1440)
        }
        KichBan.entries.forEach {
            assertTrue("kich ban ${it.ma} ngoai khoang 1..4", it.ma in 1..4)
        }
        // Hen gio: PIID 9,10,11,12 theo dung thu tu C1,C2,C3,USB-A.
        assertEquals(9, CuktechPiid.henGio(CongSacId.C1))
        assertEquals(10, CuktechPiid.henGio(CongSacId.C2))
        assertEquals(11, CuktechPiid.henGio(CongSacId.C3))
        assertEquals(12, CuktechPiid.henGio(CongSacId.USB_A))
    }

    @Test
    fun chua_nhan_goi_nao_thi_khong_coi_la_so_cu() {
        // mocNhanMs = 0: chua co gi de ma "cu". Man hinh hien "Chưa có số".
        assertFalse(CuktechState().laSoCu(9_999_999L))
    }

    @Test
    fun payload_hong_thi_giu_nguyen_trang_thai_cu() {
        val truoc = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        assertEquals(truoc, CuktechState.gop(truoc, CuktechTopic.STATUS, "khong phai json"))
        assertEquals(truoc, CuktechState.gop(truoc, CuktechTopic.STATUS, ""))
    }

    @Test
    fun topic_la_khong_lam_gi_ca() {
        val truoc = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        assertEquals(truoc, CuktechState.gop(truoc, "cuktech/charger/port/khong_co_that", goiC1Dangsac))
    }
}
