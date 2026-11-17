package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.DienTopic
import vn.smarthome.data.dien.DoTuoi
import vn.smarthome.data.dien.NgayKwh
import java.time.LocalDateTime

/**
 * Kiem thu phan DOC SO tu MQTT - cho de sai nhat cua ca tinh nang.
 *
 * Chay bang Robolectric vi `org.json` la thu vien cua Android, khong co san
 * tren JVM thuan.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class DienStateTest {

    // ---------- doc dien/lichsu ----------

    @Test
    fun doc_duoc_lich_su_binh_thuong() {
        val ds = DienState.docLichSu(
            """[{"ngay":"2026-11-01","kwh":12.4},{"ngay":"2026-11-02","kwh":9}]"""
        )
        assertEquals(listOf(NgayKwh("2026-11-01", 12.4), NgayKwh("2026-11-02", 9.0)), ds)
    }

    @Test
    fun bo_rieng_dong_hong_chu_khong_vut_ca_mang() {
        // Mat mot ngay con hon mat ca bieu do.
        val ds = DienState.docLichSu(
            """[{"ngay":"2026-11-01","kwh":12.4},{"kwh":5},{"ngay":"2026-11-03"},{"ngay":"2026-11-04","kwh":7.1}]"""
        )
        assertEquals(2, ds.size)
        assertEquals("2026-11-01", ds[0].ngay)
        assertEquals("2026-11-04", ds[1].ngay)
    }

    @Test
    fun json_rac_thi_tra_rong_chu_khong_nem_loi() {
        assertTrue(DienState.docLichSu("khong phai json").isEmpty())
        assertTrue(DienState.docLichSu("").isEmpty())
        assertTrue(DienState.docLichSu("<html>loi 500</html>").isEmpty())
    }

    // ---------- gop ban tin theo topic ----------

    @Test
    fun gop_dung_tung_topic() {
        var s = DienState()
        s = DienState.gop(s, DienTopic.TRANG_THAI, "ok")
        s = DienState.gop(s, DienTopic.NGAY_SAN_LUONG, "12.4")
        s = DienState.gop(s, DienTopic.NGAY, "2026-11-16")
        s = DienState.gop(s, DienTopic.THANG_SAN_LUONG, "209")
        s = DienState.gop(s, DienTopic.THANG_TIEN, "480650")
        s = DienState.gop(s, DienTopic.CONGTO_CHI_SO, "15234")
        s = DienState.gop(s, DienTopic.CONGNO_SO_TIEN, "0")
        s = DienState.gop(s, DienTopic.CAP_NHAT, "2026-11-16 18:00:00")

        assertEquals("ok", s.trangThai)
        assertEquals(12.4, s.ngaySanLuong!!, 0.001)
        assertEquals("2026-11-16", s.ngay)
        assertEquals(209.0, s.thangSanLuong!!, 0.001)
        assertEquals(480650.0, s.thangTien!!, 0.001)
        assertEquals(15234.0, s.congToChiSo!!, 0.001)
        assertEquals(0.0, s.congNoSoTien!!, 0.001)
    }

    @Test
    fun payload_rong_la_khong_co_gia_tri_chu_khong_phai_so_khong() {
        val s = DienState.gop(DienState(), DienTopic.NGAY_SAN_LUONG, "")
        assertNull(s.ngaySanLuong)
    }

    @Test
    fun dien_loi_rong_nghia_la_khong_sao() {
        var s = DienState.gop(DienState(), DienTopic.LOI, "EVN tra 500")
        assertEquals("EVN tra 500", s.loi)
        s = DienState.gop(s, DienTopic.LOI, "")
        assertNull(s.loi)
    }

    @Test
    fun topic_la_thi_bo_qua_khong_lam_hong_state() {
        val truoc = DienState(trangThai = "ok")
        val sau = DienState.gop(truoc, "dien/mot/topic/moi/nao/do", "123")
        assertEquals(truoc, sau)
    }

    // ---------- tinh toan ----------

    @Test
    fun tong_kwh_va_chenh_phan_tram() {
        val s = DienState(
            lichSu = (1..14).map { NgayKwh("2026-11-%02d".format(it), if (it <= 7) 10.0 else 5.0) }
        )
        assertEquals(35.0, s.tongKwh(7)!!, 0.001)   // 7 ngay cuoi, moi ngay 5
        assertEquals(-50, s.chenhPhanTram(7))       // 35 so voi 70 => giam 50%
    }

    @Test
    fun chua_du_du_lieu_thi_tra_null_chu_khong_doan() {
        val rong = DienState()
        assertNull(rong.tongKwh(7))
        assertNull(rong.chenhPhanTram(7))

        // Co 7 ngay nhung chua co 7 ngay lien truoc de so sanh.
        val motTuan = DienState(lichSu = (1..7).map { NgayKwh("2026-11-0$it", 10.0) })
        assertEquals(70.0, motTuan.tongKwh(7)!!, 0.001)
        assertNull(motTuan.chenhPhanTram(7))
    }

    // ---------- tuoi so lieu ----------

    @Test
    fun do_tuoi_theo_nhip_4_cu_mot_ngay() {
        val bayGio = LocalDateTime.of(2026, 11, 17, 12, 0, 0)
        fun tuoi(capNhat: String) =
            DienState(capNhat = capNhat).doTuoi(bayGio)

        assertEquals(DoTuoi.MOI, tuoi("2026-11-17 08:00:00"))      // 4 tieng
        assertEquals(DoTuoi.HOI_CU, tuoi("2026-11-17 01:00:00"))   // 11 tieng
        assertEquals(DoTuoi.QUA_CU, tuoi("2026-11-15 12:00:00"))   // 2 ngay
        assertEquals(DoTuoi.CHUA_CO, DienState().doTuoi(bayGio))
    }

    @Test
    fun cap_nhat_sai_dinh_dang_thi_coi_nhu_chua_co() {
        val s = DienState(capNhat = "hom qua")
        assertEquals(DoTuoi.CHUA_CO, s.doTuoi())
        assertNull(s.capNhatLocalDateTime())
    }

    @Test
    fun co_so_lieu_chi_bat_khi_that_su_co_so() {
        assertTrue(!DienState().coSoLieu)
        assertTrue(!DienState(trangThai = "ok", capNhat = "2026-11-17 08:00:00").coSoLieu)
        assertTrue(DienState(lichSu = listOf(NgayKwh("2026-11-01", 1.0))).coSoLieu)
        assertTrue(DienState(ngaySanLuong = 3.0).coSoLieu)
    }
}
