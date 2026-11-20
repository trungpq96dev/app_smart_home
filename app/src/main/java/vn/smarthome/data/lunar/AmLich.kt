package vn.smarthome.data.lunar

import kotlin.math.floor
import kotlin.math.sin

/**
 * Doi duong lich sang am lich Viet Nam.
 *
 * Thuat toan cua Ho Ngoc Duc (ma nguon mo, dung cho lich van nien tieng Viet).
 * Chep nguyen cong thuc, KHONG tu "lam gon" mot he so nao: moi hang so trong
 * getNewMoonDay/getSunLongitude deu la he so thien van, doi mot chu so la lech
 * ngay Tet.
 *
 * Mui gio mac dinh +7: am lich Viet Nam chot ngay theo gio Ha Noi. Trung Quoc
 * dung +8 nen co nam lech nhau dung mot ngay - de nguyen 7.0 o day.
 *
 * Lop nay CHI tinh toan, khong dinh gi toi Compose hay Android, nen chay thang
 * duoc trong unit test JVM (xem app/src/test/.../AmLichTest.kt) - giong cach
 * [vn.smarthome.data.dien.DienState] tach khoi tang giao dien.
 */

/** Mot ngay am lich. [jd] la so ngay Julius, dung de tinh Can Chi. */
data class NgayAm(
    val ngay: Int,
    val thang: Int,
    val nam: Int,
    val nhuan: Boolean,
    val jd: Long,
)

/** Can Chi cua nam / thang / ngay, kem chi so Chi cua ngay de tra gio hoang dao. */
data class CanChi(
    val nam: String,
    val thang: String,
    val ngay: String,
    val chiNgay: Int,
)

object AmLich {

    val CAN = listOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    val CHI = listOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    /** Ten gio theo Chi + khung gio duong lich tuong ung. */
    val GIO = listOf(
        "Tý (23-1h)", "Sửu (1-3h)", "Dần (3-5h)", "Mão (5-7h)",
        "Thìn (7-9h)", "Tỵ (9-11h)", "Ngọ (11-13h)", "Mùi (13-15h)",
        "Thân (15-17h)", "Dậu (17-19h)", "Tuất (19-21h)", "Hợi (21-23h)",
    )

    /**
     * Bang gio hoang dao, xep theo CAP Chi cua ngay:
     * Ty/Ngo, Suu/Mui, Dan/Than, Mao/Dau, Thin/Tuat, Ty./Hoi.
     * Vi the tra bang chiNgay % 6.
     */
    private val GIO_HOANG_DAO = listOf(
        listOf(0, 1, 3, 6, 8, 9),
        listOf(2, 3, 5, 8, 10, 11),
        listOf(0, 1, 4, 5, 7, 10),
        listOf(0, 2, 3, 6, 7, 9),
        listOf(2, 4, 5, 8, 9, 11),
        listOf(1, 4, 6, 7, 10, 11),
    )

    private const val MUI_GIO_VN = 7.0

    /**
     * PHAI la floor chu khong duoc cat phan le.
     *
     * Hai thu khac nhau khi so am: getSunLongitude cho ra goc am voi cac ngay
     * truoc nam 1900, cat phan le o do la lech mot cung 30 do, keo theo sai
     * thang nhuan.
     */
    private fun int(d: Double): Long = floor(d).toLong()

    /** So ngay Julius cua mot ngay duong lich. */
    fun jdTuDuongLich(dd: Int, mm: Int, yy: Int): Long {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161L) {
            jd = dd + (153 * m + 2) / 5 + 365L * y + y / 4 - 32083
        }
        return jd
    }

    /** Ngay Julius cua lan trang moi thu [k] tinh tu 1/1/1900. */
    private fun ngayTrangMoi(k: Long, muiGio: Double): Long {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180.0
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        val c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m) -
            0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr) -
            0.0004 * sin(dr * 3 * mpr) + 0.0104 * sin(dr * 2 * f) -
            0.0051 * sin(dr * (m + mpr)) - 0.0074 * sin(dr * (m - mpr)) +
            0.0004 * sin(dr * (2 * f + m)) - 0.0004 * sin(dr * (2 * f - m)) -
            0.0006 * sin(dr * (2 * f + mpr)) + 0.0010 * sin(dr * (2 * f - mpr)) +
            0.0005 * sin(dr * (2 * mpr + m))
        val deltat = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return int(jd1 + c1 - deltat + 0.5 + muiGio / 24.0)
    }

    /** Kinh do mat troi, tra ve so cung 30 do (0..11). */
    private fun kinhDoMatTroi(jdn: Long, muiGio: Double): Int {
        val t = (jdn - 2451545.5 - muiGio / 24.0) / 36525.0
        val t2 = t * t
        val dr = Math.PI / 180.0
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        val dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m) +
            (0.019993 - 0.000101 * t) * sin(dr * 2 * m) +
            0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= Math.PI * 2 * int(l / (Math.PI * 2))
        return int(l / Math.PI * 6).toInt()
    }

    /** Ngay bat dau thang 11 am lich cua nam duong lich [yy]. */
    private fun thang11AmLich(yy: Int, muiGio: Double): Long {
        val off = jdTuDuongLich(31, 12, yy) - 2415021L
        val k = int(off / 29.530588853)
        var nm = ngayTrangMoi(k, muiGio)
        if (kinhDoMatTroi(nm, muiGio) >= 9) nm = ngayTrangMoi(k - 1, muiGio)
        return nm
    }

    /** Thang nhuan nam o vi tri thu may sau thang 11. */
    private fun viTriThangNhuan(a11: Long, muiGio: Double): Int {
        val k = int((a11 - 2415021.076998695) / 29.530588853 + 0.5)
        var last: Int
        var i = 1
        var arc = kinhDoMatTroi(ngayTrangMoi(k + i, muiGio), muiGio)
        do {
            last = arc
            i++
            arc = kinhDoMatTroi(ngayTrangMoi(k + i, muiGio), muiGio)
        } while (arc != last && i < 14)
        return i - 1
    }

    /** Doi mot ngay duong lich sang am lich. */
    fun tuDuongLich(dd: Int, mm: Int, yy: Int, muiGio: Double = MUI_GIO_VN): NgayAm {
        val soNgay = jdTuDuongLich(dd, mm, yy)
        val k = int((soNgay - 2415021.076998695) / 29.530588853)
        var dauThang = ngayTrangMoi(k + 1, muiGio)
        if (dauThang > soNgay) dauThang = ngayTrangMoi(k, muiGio)

        var a11 = thang11AmLich(yy, muiGio)
        var b11 = a11
        var namAm: Int
        if (a11 >= dauThang) {
            namAm = yy
            a11 = thang11AmLich(yy - 1, muiGio)
        } else {
            namAm = yy + 1
            b11 = thang11AmLich(yy + 1, muiGio)
        }

        val ngayAm = (soNgay - dauThang + 1).toInt()
        val chenh = int((dauThang - a11) / 29.0).toInt()
        var nhuan = false
        var thangAm = chenh + 11
        if (b11 - a11 > 365) {
            val viTri = viTriThangNhuan(a11, muiGio)
            if (chenh >= viTri) {
                thangAm = chenh + 10
                if (chenh == viTri) nhuan = true
            }
        }
        if (thangAm > 12) thangAm -= 12
        if (thangAm >= 11 && chenh < 4) namAm -= 1

        return NgayAm(ngay = ngayAm, thang = thangAm, nam = namAm, nhuan = nhuan, jd = soNgay)
    }

    /** Can Chi cua nam, thang va ngay. */
    fun canChi(am: NgayAm): CanChi {
        val chiNgay = ((am.jd + 1) % 12).toInt()
        return CanChi(
            nam = CAN[(am.nam + 6) % 10] + " " + CHI[(am.nam + 8) % 12],
            thang = CAN[(am.nam * 12 + am.thang + 3) % 10] + " " + CHI[(am.thang + 1) % 12],
            ngay = CAN[((am.jd + 9) % 10).toInt()] + " " + CHI[chiNgay],
            chiNgay = chiNgay,
        )
    }

    /** Sau khung gio hoang dao cua ngay. */
    fun gioHoangDao(chiNgay: Int): List<String> =
        GIO_HOANG_DAO[chiNgay % 6].map { GIO[it] }

    /** Le theo duong lich. */
    private val LE_DUONG = mapOf(
        "1/1" to "Tết Dương lịch",
        "14/2" to "Lễ tình nhân",
        "8/3" to "Quốc tế Phụ nữ",
        "30/4" to "Giải phóng miền Nam",
        "1/5" to "Quốc tế Lao động",
        "1/6" to "Quốc tế Thiếu nhi",
        "2/9" to "Quốc khánh",
        "20/10" to "Phụ nữ Việt Nam",
        "20/11" to "Nhà giáo Việt Nam",
        "22/12" to "Quân đội nhân dân VN",
        "24/12" to "Lễ Giáng sinh",
    )

    /** Le theo am lich. */
    private val LE_AM = mapOf(
        "1/1" to "Tết Nguyên Đán",
        "2/1" to "Mùng 2 Tết",
        "3/1" to "Mùng 3 Tết",
        "15/1" to "Tết Nguyên Tiêu",
        "3/3" to "Tết Hàn Thực",
        "10/3" to "Giỗ Tổ Hùng Vương",
        "15/4" to "Lễ Phật Đản",
        "5/5" to "Tết Đoan Ngọ",
        "15/7" to "Lễ Vu Lan",
        "15/8" to "Tết Trung Thu",
        "9/9" to "Tết Trùng Cửu",
        "23/12" to "Ông Công Ông Táo",
        "30/12" to "Giao thừa",
    )

    /**
     * Ten ngay le, hoac null khi la ngay thuong.
     *
     * Le am lich duoc uu tien hon le duong lich khi trung ngay. Thang nhuan
     * KHONG tinh le: Tet Doan Ngo 5/5 nhuan khong phai Tet Doan Ngo.
     */
    fun suKien(dd: Int, mm: Int, am: NgayAm): String? {
        if (!am.nhuan) LE_AM["${am.ngay}/${am.thang}"]?.let { return it }
        LE_DUONG["$dd/$mm"]?.let { return it }
        return null
    }

    /** Nhan ngan hien duoi so duong lich trong o lich: ngay mung 1 kem so thang. */
    fun nhanO(am: NgayAm): String =
        if (am.ngay == 1) "${am.ngay}/${am.thang}" else am.ngay.toString()

    /** Mung 1 va ngay Ram - hai moc duoc danh dau mau khac trong lich. */
    fun laMocAm(am: NgayAm): Boolean = am.ngay == 1 || am.ngay == 15
}
