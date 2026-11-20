package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.giongnoi.MucDung
import vn.smarthome.data.giongnoi.MucDungNgay

/**
 * Kiem thu phan DEM MUC DUNG cua tro ly giong noi.
 *
 * Day la cai chan "noi toi luc mat tien thi dung", nen cong sai mot cho la
 * hoac chan oan (dang noi thi bi cat), hoac khong chan (hoa don chay tiep).
 *
 * Chay bang Robolectric vi phan doc/ghi dung `org.json`.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MucDungTest {

    // ---------- gop phien ----------

    @Test
    fun phien_dau_tien_trong_ngay_tao_ban_ghi_moi() {
        val ds = MucDung.gopPhien(emptyList(), "2026-11-21", 120, 10, 20, 30)
        assertEquals(1, ds.size)
        assertEquals("2026-11-21", ds[0].ngay)
        assertEquals(120, ds[0].giay)
        assertEquals(1, ds[0].soPhien)
        assertEquals(30L, ds[0].tokenTong)
    }

    @Test
    fun phien_thu_hai_cung_ngay_thi_cong_don_vao_ban_ghi_cu() {
        var ds = MucDung.gopPhien(emptyList(), "2026-11-21", 120, 10, 20, 30)
        ds = MucDung.gopPhien(ds, "2026-11-21", 60, 5, 5, 10)
        assertEquals(1, ds.size)
        assertEquals(180, ds[0].giay)
        assertEquals(2, ds[0].soPhien)
        assertEquals(40L, ds[0].tokenTong)
    }

    /**
     * Qua nua dem thi phai sang ban ghi MOI, va ngay hom qua phai con nguyen.
     *
     * Day la cho de sai nhat: cong nham sang ngay cu thi tran ngay khong bao
     * gio nha ra, nguoi dung sang hom sau van bi chan.
     */
    @Test
    fun qua_ngay_moi_thi_tach_ban_ghi_va_giu_ngay_cu() {
        var ds = MucDung.gopPhien(emptyList(), "2026-11-20", 600, 0, 0, 0)
        ds = MucDung.gopPhien(ds, "2026-11-21", 60, 0, 0, 0)
        assertEquals(2, ds.size)
        assertEquals(600, MucDung.cuaNgay(ds, "2026-11-20").giay)
        assertEquals(60, MucDung.cuaNgay(ds, "2026-11-21").giay)
    }

    @Test
    fun ngay_chua_dung_gi_thi_tra_ve_ban_ghi_rong_chu_khong_phai_null() {
        val ds = MucDung.gopPhien(emptyList(), "2026-11-20", 600, 0, 0, 0)
        val homNay = MucDung.cuaNgay(ds, "2026-11-21")
        assertEquals(0, homNay.giay)
        assertEquals(0, homNay.soPhien)
    }

    /** Khong cat thi sau mot nam DataStore giu 365 ban ghi khong ai doc toi. */
    @Test
    fun chi_giu_30_ngay_gan_nhat() {
        var ds = emptyList<MucDungNgay>()
        for (d in 1..40) {
            ds = MucDung.gopPhien(ds, "2026-10-%02d".format(d), 60, 0, 0, 0)
        }
        assertEquals(MucDung.SO_NGAY_GIU, ds.size)
        // Giu phan DUOI (ngay moi), bo phan tren (ngay cu).
        assertEquals("2026-10-11", ds.first().ngay)
        assertEquals("2026-10-40", ds.last().ngay)
    }

    @Test
    fun danh_sach_luon_sap_theo_ngay_du_gop_lon_xon() {
        var ds = MucDung.gopPhien(emptyList(), "2026-11-21", 60, 0, 0, 0)
        ds = MucDung.gopPhien(ds, "2026-11-19", 60, 0, 0, 0)
        ds = MucDung.gopPhien(ds, "2026-11-20", 60, 0, 0, 0)
        assertEquals(listOf("2026-11-19", "2026-11-20", "2026-11-21"), ds.map { it.ngay })
    }

    // ---------- cong nhieu ngay ----------

    @Test
    fun cong_7_ngay_gan_nhat() {
        var ds = emptyList<MucDungNgay>()
        for (d in 14..21) ds = MucDung.gopPhien(ds, "2026-11-%02d".format(d), 60, 0, 0, 0)
        // 8 ngay x 60 giay, lay 7 ngay cuoi = 420.
        assertEquals(420, MucDung.giayNgayGanDay(ds, 7))
    }

    // ---------- doc/ghi JSON ----------

    @Test
    fun ghi_ra_doc_lai_khong_mat_gi() {
        var ds = MucDung.gopPhien(emptyList(), "2026-11-20", 600, 11, 22, 33)
        ds = MucDung.gopPhien(ds, "2026-11-21", 120, 1, 2, 3)
        val lai = MucDung.tuJson(MucDung.sangJson(ds))
        assertEquals(ds, lai)
    }

    /**
     * Hong thi tra danh sach RONG chu khong nem ngoai le.
     *
     * Mat so thong ke la phien toai; chet app luc mo man Cai dat la hong.
     */
    @Test
    fun json_rac_thi_tra_rong_chu_khong_chet() {
        assertTrue(MucDung.tuJson("{khong phai json}").isEmpty())
        assertTrue(MucDung.tuJson("").isEmpty())
        assertTrue(MucDung.tuJson(null).isEmpty())
        // Mang dung dinh dang nhung phan tu thieu ngay thi bo qua phan tu do.
        assertTrue(MucDung.tuJson("""[{"giay":60}]""").isEmpty())
    }

    @Test
    fun doc_duoc_thi_sap_lai_theo_ngay() {
        val ds = MucDung.tuJson(
            """[{"ngay":"2026-11-21","giay":60},{"ngay":"2026-11-19","giay":30}]""",
        )
        assertEquals(listOf("2026-11-19", "2026-11-21"), ds.map { it.ngay })
    }

    // ---------- tran ngay ----------

    @Test
    fun tinh_duoc_da_cham_tran_hay_chua() {
        val ds = MucDung.gopPhien(emptyList(), "2026-11-21", 29 * 60, 0, 0, 0)
        val daDung = MucDung.cuaNgay(ds, "2026-11-21").giay
        assertTrue("29 phut chua cham tran 30", daDung < 30 * 60)

        val ds2 = MucDung.gopPhien(ds, "2026-11-21", 2 * 60, 0, 0, 0)
        assertTrue("31 phut la qua tran 30", MucDung.cuaNgay(ds2, "2026-11-21").giay >= 30 * 60)
    }
}
