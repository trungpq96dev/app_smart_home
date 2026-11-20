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

    @Test
    fun topic_settings_bi_bo_qua() {
        val truoc = CuktechState.gop(CuktechState(), CuktechTopic.STATUS, goiStatus)
        val sau = CuktechState.gop(
            truoc, CuktechTopic.SETTINGS, """{"5":1,"17":120391469,"21":50532111}""",
        )
        // Bang PIID tho chua ai giai nghia - doan y nghia roi hien len la bia.
        assertEquals(truoc, sau)
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
