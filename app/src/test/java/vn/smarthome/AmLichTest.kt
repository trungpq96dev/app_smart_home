package vn.smarthome

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import vn.smarthome.data.lunar.AmLich
import java.time.LocalDate

/**
 * Kiem thuat toan am lich bang NGAY TET THAT, khong phai bang ket qua do
 * chinh no in ra.
 *
 * Tu doi lai ket qua cua chinh minh thi test nao cung xanh - chi chung minh
 * duoc code khong doi, khong chung minh duoc code dung. Nen moc so sanh o day
 * la cac ngay ai cung tra duoc tren to lich giay.
 */
class AmLichTest {

    private fun am(d: Int, m: Int, y: Int) = AmLich.tuDuongLich(d, m, y)

    @Test
    fun `mung 1 Tet cac nam gan day roi dung ngay`() {
        // Tet Quy Mao, Giap Thin, At Ty, Binh Ngo, Dinh Mui.
        val tet = listOf(
            Triple(22, 1, 2023),
            Triple(10, 2, 2024),
            Triple(29, 1, 2025),
            Triple(17, 2, 2026),
            Triple(6, 2, 2027),
        )
        tet.forEach { (d, m, y) ->
            val a = am(d, m, y)
            assertEquals("ngay $d/$m/$y phai la mung 1", 1, a.ngay)
            assertEquals("ngay $d/$m/$y phai la thang Gieng", 1, a.thang)
            assertFalse("thang Gieng khong bao gio nhuan", a.nhuan)
        }
    }

    @Test
    fun `nam Can Chi dung ten`() {
        assertEquals("Quý Mão", AmLich.canChi(am(22, 1, 2023)).nam)
        assertEquals("Giáp Thìn", AmLich.canChi(am(10, 2, 2024)).nam)
        assertEquals("Ất Tỵ", AmLich.canChi(am(29, 1, 2025)).nam)
        assertEquals("Bính Ngọ", AmLich.canChi(am(17, 2, 2026)).nam)
    }

    @Test
    fun `ngay truoc Tet van thuoc nam cu`() {
        // 16/2/2026 la 30 Tet - van con nam At Ty.
        val a = am(16, 2, 2026)
        assertEquals(12, a.thang)
        assertEquals("Ất Tỵ", AmLich.canChi(a).nam)
    }

    @Test
    fun `nam 2025 co thang 6 nhuan`() {
        // At Ty 2025 nhuan thang 6 - quet ca nam duong lich 2025 phai thay.
        var thayNhuan = false
        var d = LocalDate.of(2025, 1, 1)
        while (d.year == 2025) {
            val a = am(d.dayOfMonth, d.monthValue, 2025)
            if (a.nhuan) {
                assertEquals("nam 2025 chi nhuan thang 6", 6, a.thang)
                thayNhuan = true
            }
            d = d.plusDays(1)
        }
        assertTrue("phai tim thay thang nhuan trong nam 2025", thayNhuan)
    }

    @Test
    fun `nam 2026 khong co thang nhuan`() {
        var d = LocalDate.of(2026, 3, 1)
        while (d.isBefore(LocalDate.of(2027, 1, 1))) {
            assertFalse(
                "2026 khong phai nam nhuan am lich, ${d} bao nhuan",
                am(d.dayOfMonth, d.monthValue, 2026).nhuan,
            )
            d = d.plusDays(1)
        }
    }

    @Test
    fun `ngay am lich chay lien tuc tu 1 den 29 hoac 30`() {
        // Thang am lich chi dai 29 hoac 30 ngay, va so ngay phai tang deu.
        var truoc = am(1, 1, 2026)
        var d = LocalDate.of(2026, 1, 2)
        while (d.isBefore(LocalDate.of(2027, 1, 1))) {
            val nay = am(d.dayOfMonth, d.monthValue, 2026)
            if (nay.ngay != 1) {
                assertEquals("ngay am phai tang 1 tai $d", truoc.ngay + 1, nay.ngay)
            } else {
                assertTrue("thang am phai dai 29 hoac 30 ngay, het o $d", truoc.ngay in 29..30)
            }
            assertTrue("ngay am ngoai khoang tai $d", nay.ngay in 1..30)
            assertTrue("thang am ngoai khoang tai $d", nay.thang in 1..12)
            truoc = nay
            d = d.plusDays(1)
        }
    }

    @Test
    fun `can chi ngay xoay vong dung 60 ngay`() {
        val moc = am(20, 11, 2026)
        val sau60 = am(19, 1, 2027) // 20/11/2026 + 60 ngay
        assertEquals(60L, sau60.jd - moc.jd)
        assertEquals(AmLich.canChi(moc).ngay, AmLich.canChi(sau60).ngay)
    }

    @Test
    fun `moi ngay co dung 6 gio hoang dao`() {
        for (chi in 0..11) {
            val gio = AmLich.gioHoangDao(chi)
            assertEquals(6, gio.size)
            assertEquals("khong duoc trung gio", 6, gio.toSet().size)
        }
    }

    @Test
    fun `su kien am lich duoc uu tien hon duong lich`() {
        // Tet Nguyen Dan 17/2/2026 phai ra "Tet Nguyen Dan".
        assertEquals("Tết Nguyên Đán", AmLich.suKien(17, 2, am(17, 2, 2026)))
        // 20/11 duong lich la ngay Nha giao, am lich hom do khong co le gi.
        assertEquals("Nhà giáo Việt Nam", AmLich.suKien(20, 11, am(20, 11, 2026)))
    }

    @Test
    fun `ngay thuong thi khong co su kien`() {
        val a = am(21, 11, 2026)
        if (a.ngay != 1 && a.ngay != 15) {
            assertEquals(null, AmLich.suKien(21, 11, a))
        }
    }

    @Test
    fun `nhan o lich kem thang khi la mung 1`() {
        val tet = am(17, 2, 2026)
        assertEquals("1/1", AmLich.nhanO(tet))
        assertTrue(AmLich.laMocAm(tet))

        val thuong = am(18, 2, 2026)
        assertEquals("2", AmLich.nhanO(thuong))
        assertFalse(AmLich.laMocAm(thuong))
    }

    @Test
    fun `jd khop voi so ngay Julius chuan`() {
        // 1/1/2000 co so ngay Julius 2451545 (moc J2000 ai cung tra duoc).
        assertEquals(2451545L, AmLich.jdTuDuongLich(1, 1, 2000))
    }

    @Test
    fun `doi duoc ca ngay truoc cai cach lich Gregory`() {
        // Khong dung o man hinh, nhung nhanh lich Julius phai chay chu khong no.
        assertNotNull(am(4, 10, 1582))
    }
}
