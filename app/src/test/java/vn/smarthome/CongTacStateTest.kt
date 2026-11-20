package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.data.congtac.CongTacTopic
import vn.smarthome.data.congtac.HangChoCongTac
import vn.smarthome.data.congtac.LenhCongTac
import vn.smarthome.data.congtac.OThietBi

/**
 * Kiem thu phan DOC `congtac/#` tu MQTT.
 *
 * Chay bang Robolectric vi `org.json` la thu vien cua Android, khong co san
 * tren JVM thuan - cung ly do voi [DienStateTest].
 *
 * Payload trong file nay la payload THAT ma `scripts/congtac_bridge/` sinh ra,
 * khong phai payload tu nghi.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CongTacStateTest {

    private val INFO = """
        {"bridge":"congtac_bridge 1.0","cap_nhat":"2026-11-21 00:40:32","thiet_bi":[
          {"id":"bed1_den","ten":"Đèn Phòng ngủ 1","phong":"bed1","loai":"den",
           "driver":"tuya","ip":"192.168.0.100","nang_luc":["ON","OFF","DAO","DAT"]},
          {"id":"bed1_quat","ten":"Quạt Phòng ngủ 1","phong":"bed1","loai":"quat",
           "driver":"cozylife","ip":"192.168.0.103","nang_luc":["ON","OFF","DAO","DAT"]}]}
    """.trimIndent()

    private fun nenTang(): CongTacState {
        var s = CongTacState()
        s = CongTacState.gop(s, CongTacTopic.TRANG_THAI, "online")
        s = CongTacState.gop(s, CongTacTopic.INFO, INFO)
        return s
    }

    // ---------- congtac/info ----------

    @Test
    fun doc_duoc_danh_sach_thiet_bi() {
        val s = nenTang()
        assertEquals(2, s.danhSach.size)
        assertTrue(s.laThat("bed1", "den"))
        assertTrue(s.laThat("bed1", "quat"))
        assertEquals("Đèn Phòng ngủ 1", s.tim("bed1", "den")!!.ten)
        assertEquals("cozylife", s.tim("bed1", "quat")!!.he)
    }

    @Test
    fun thiet_bi_khong_duoc_khai_thi_van_la_mau() {
        val s = nenTang()
        assertFalse("Dieu hoa bed1 chua noi gi", s.laThat("bed1", "dieu_hoa"))
        assertFalse("Den phong khach chua noi gi", s.laThat("living", "den"))
    }

    @Test
    fun bo_rieng_phan_tu_hong_chu_khong_vut_ca_mang() {
        // Mat mot cong tac con hon mat ca hai.
        val s = CongTacState.gop(
            CongTacState(), CongTacTopic.INFO,
            """{"thiet_bi":[{"ten":"thieu id"},
               {"id":"bed1_quat","phong":"bed1","loai":"quat","ten":"Quạt"}]}""",
        )
        assertEquals(1, s.danhSach.size)
        assertTrue(s.laThat("bed1", "quat"))
    }

    @Test
    fun info_mang_rong_thi_xoa_het_thiet_bi() {
        val s = CongTacState.gop(nenTang(), CongTacTopic.INFO, """{"thiet_bi":[]}""")
        assertTrue(s.danhSach.isEmpty())
    }

    @Test
    fun info_payload_rong_thi_GIU_NGUYEN_danh_sach_cu() {
        // Broker khoi dong lai mat retained thi thiet bi that khong duoc bien
        // thanh mau trong mot nhay mat. Muon xoa thi bridge publish `[]`.
        val s = CongTacState.gop(nenTang(), CongTacTopic.INFO, "")
        assertEquals(2, s.danhSach.size)
    }

    @Test
    fun payload_rac_khong_nem_va_khong_lam_mat_du_lieu_cu() {
        val s = CongTacState.gop(nenTang(), CongTacTopic.INFO, "khong phai json")
        assertEquals(2, s.danhSach.size)
    }

    // ---------- bay tien to: congtac/trangthai vs congtac/thietbi/<ma> ----------

    @Test
    fun LWT_khong_bi_dem_di_parse_JSON() {
        // Day la ly do `congtac/thietbi/<ma>` duoc dat ten khac han `trangthai`.
        val s = CongTacState.gop(CongTacState(), CongTacTopic.TRANG_THAI, "online")
        assertEquals("online", s.bridge)
        assertTrue(s.bridgeSong)
        assertTrue("Khong duoc sinh ra thiet bi ten rong", s.trangThai.isEmpty())
    }

    @Test
    fun LWT_offline_thi_bridge_khong_song() {
        val s = CongTacState.gop(nenTang(), CongTacTopic.TRANG_THAI, "offline")
        assertFalse(s.bridgeSong)
    }

    // ---------- congtac/thietbi/<ma> ----------

    @Test
    fun doc_duoc_trang_thai_bat() {
        val s = CongTacState.gop(
            nenTang(), CongTacTopic.TIEN_TO_TB + "bed1_den",
            """{"online":true,"is_on":true,"doc_luc":"2026-11-21 00:40:32",
               "tra_loi_id":null,"ly_do_loi":null}""",
        )
        val tt = s.trangThaiCua("bed1", "den")!!
        assertTrue(tt.online)
        assertEquals(true, tt.bat)
        assertNull(tt.lyDoLoi)
    }

    @Test
    fun thieu_khoa_is_on_thi_null_CHU_KHONG_PHAI_false() {
        // Day la cho nguyen tac "khong bia so" song hoac chet: tra false thi
        // man hinh ve cong tac o the TAT trong khi den dang sang.
        val s = CongTacState.gop(
            nenTang(), CongTacTopic.TIEN_TO_TB + "bed1_den",
            """{"online":true,"doc_luc":"2026-11-21 00:40:32"}""",
        )
        assertNull(s.trangThaiCua("bed1", "den")!!.bat)
    }

    @Test
    fun is_on_null_tuong_minh_thi_cung_null() {
        val s = CongTacState.gop(
            nenTang(), CongTacTopic.TIEN_TO_TB + "bed1_den",
            """{"online":true,"is_on":null}""",
        )
        assertNull(s.trangThaiCua("bed1", "den")!!.bat)
    }

    @Test
    fun offline_thi_is_on_luon_null_du_payload_co_gi() {
        // Bridge da hua la khong gui, nhung app khong duoc tin vao loi hua do.
        val s = CongTacState.gop(
            nenTang(), CongTacTopic.TIEN_TO_TB + "bed1_den",
            """{"online":false,"is_on":true,"ly_do_loi":"mat dien"}""",
        )
        val tt = s.trangThaiCua("bed1", "den")!!
        assertFalse(tt.online)
        assertNull(tt.bat)
        assertEquals("mat dien", tt.lyDoLoi)
    }

    @Test
    fun trang_thai_cua_thiet_bi_mau_la_null() {
        assertNull(nenTang().trangThaiCua("living", "den"))
    }

    @Test
    fun capnhat_doc_duoc() {
        val s = CongTacState.gop(nenTang(), CongTacTopic.CAP_NHAT, "2026-11-21 00:40:32")
        assertEquals("2026-11-21 00:40:32", s.capNhat)
    }

    // ---------- LenhCongTac ----------

    @Test
    fun json_lenh_giu_dung_khuon_payload_cu() {
        val j = org.json.JSONObject(
            LenhCongTac.json("bed1", "den", "bat", null, LenhCongTac.NGUON_MAN_HINH, "abc123"),
        )
        assertEquals("bed1_den", j.getString("device"))
        assertEquals("bed1", j.getString("room"))
        assertEquals("BAT", j.getString("action"))
        assertEquals("man_hinh", j.getString("source"))
        assertEquals("abc123", j.getString("id"))
    }

    @Test
    fun o_nhanh_giu_tien_to_nhanh_nhu_truoc() {
        val j = org.json.JSONObject(
            LenhCongTac.json("nhanh", "light", "bat", null, "voice", "x"),
        )
        assertEquals("nhanh_light", j.getString("device"))
    }

    // ---------- HangChoCongTac ----------

    @Test
    fun go_hang_cho_khi_ma_lenh_khop() {
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", mong = true, id = "a1", bayGioMs = 1000)
        assertTrue(h.dangCho("bed1_den"))
        assertTrue(h.nhanTrangThai("bed1_den", "a1"))
        assertFalse(h.dangCho("bed1_den"))
    }

    @Test
    fun khong_go_khi_ma_lenh_la_cua_lenh_cu() {
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", true, "a2", 1000)
        assertFalse("Ban tin cu khong duoc go co cua lenh moi", h.nhanTrangThai("bed1_den", "a1"))
        assertTrue(h.dangCho("bed1_den"))
    }

    @Test
    fun khong_go_khi_ban_tin_khong_co_ma_lenh() {
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", true, "a1", 1000)
        assertFalse(h.nhanTrangThai("bed1_den", null))
        assertTrue(h.dangCho("bed1_den"))
    }

    @Test
    fun van_go_khi_cong_tac_tra_ve_the_NGUOC_voi_mong_muon() {
        // Do dung la cau tra loi. Man hinh phai hien su that chu khong quay vong.
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", mong = true, id = "a1", bayGioMs = 1000)
        assertTrue(h.nhanTrangThai("bed1_den", "a1"))
    }

    @Test
    fun qua_han_thi_go_va_bao_ten() {
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", true, "a1", 1000)
        assertTrue("Chua du 6 giay", h.quaHan(5000).isEmpty())
        assertEquals(listOf("bed1_den"), h.quaHan(7000))
        assertFalse(h.dangCho("bed1_den"))
    }

    @Test
    fun gat_lan_hai_thi_ma_moi_de_len_dong_ho_cu_khong_go_nham() {
        val h = HangChoCongTac(6)
        h.ganh("bed1_den", true, "a1", 1000)
        h.ganh("bed1_den", false, "a2", 3000)
        assertFalse("Tra loi cua lenh thu nhat khong duoc go lenh thu hai",
            h.nhanTrangThai("bed1_den", "a1"))
        assertTrue(h.nhanTrangThai("bed1_den", "a2"))
    }

    // ---------- OThietBi ----------

    @Test
    fun o_that_mat_ket_noi_thi_khong_gat_duoc_va_co_ghi_chu() {
        val o = OThietBi.That("Đèn", bridgeSong = true, online = false, bat = null,
            dangCho = false, mong = null, lyDoLoi = "mat dien")
        assertFalse(o.gatDuoc)
        assertEquals("mat dien", o.ghiChu)
    }

    @Test
    fun o_that_bridge_chet_thi_khong_gat_duoc() {
        val o = OThietBi.That("Đèn", bridgeSong = false, online = true, bat = true,
            dangCho = false, mong = null, lyDoLoi = null)
        assertFalse(o.gatDuoc)
        assertTrue(o.ghiChu!!.contains("Bộ nối"))
    }

    @Test
    fun o_that_online_va_ro_thi_gat_duoc_va_khong_can_ghi_chu() {
        val o = OThietBi.That("Đèn", bridgeSong = true, online = true, bat = false,
            dangCho = false, mong = null, lyDoLoi = null)
        assertTrue(o.gatDuoc)
        assertNull(o.ghiChu)
    }

    @Test
    fun o_that_dang_cho_thi_khoa_gat_lai() {
        val o = OThietBi.That("Đèn", bridgeSong = true, online = true, bat = false,
            dangCho = true, mong = true, lyDoLoi = null)
        assertFalse(o.gatDuoc)
        assertEquals("Đang gửi lệnh…", o.ghiChu)
    }

    @Test
    fun o_that_online_ma_khong_doc_duoc_cong_tac() {
        val o = OThietBi.That("Quạt", bridgeSong = true, online = true, bat = null,
            dangCho = false, mong = null, lyDoLoi = null)
        assertFalse(o.gatDuoc)
        assertEquals("Chưa đọc được công tắc", o.ghiChu)
    }
}
