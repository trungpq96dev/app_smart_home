package vn.smarthome.data.thoitiet

/**
 * So lieu thoi tiet THAT lay tu Open-Meteo.
 *
 * Lop nay CHI giu con so (nhiet do, ma thoi tiet WMO...), khong dinh gi toi
 * icon hay mau - phan do la viec cua tang giao dien (WeatherCard). Giu tang
 * du lieu khong phu thuoc Compose de de kiem thu va de doi nguon sau nay.
 */

/** Thoi tiet hien tai. Nhiet do da lam tron ve so nguyen do C. */
data class ThoiTietHienTai(
    val nhietDo: Int,
    val camGiac: Int,
    val doAm: Int,
    val gio: Int,          // km/h
    val maWmo: Int,        // ma thoi tiet WMO (xem maThoiTiet o WeatherCard)
    val banNgay: Boolean,  // is_day == 1
)

/** Mot ngay trong du bao ngan. */
data class NgayThoiTiet(
    val thu: String,       // "Hôm nay", "Th 4", "CN"...
    val maWmo: Int,
    val caoNhat: Int,
    val thapNhat: Int,
)

/** Goi so lieu day du cho the thoi tiet. */
data class ThoiTietData(
    val diaDiem: String,
    val hienTai: ThoiTietHienTai,
    val cacNgay: List<NgayThoiTiet>,
    val gioCapNhat: String,   // "14:05"
)

/**
 * Trang thai the thoi tiet tren Trang chu.
 *
 * Cung loi nghi nhu [vn.smarthome.data.camera.TrangThaiCam]: khong bia so mau
 * khi goi hong. Hong thi bao loi ro rang, con khi tai lai ma hong thi giu lai
 * so cu (xem HomeViewModel.taiThoiTiet) de man khong nhay ve trang.
 */
sealed interface ThoiTietUi {
    data object DangTai : ThoiTietUi
    data class Co(val data: ThoiTietData) : ThoiTietUi
    data class Loi(val moTa: String) : ThoiTietUi
}
