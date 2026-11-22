package vn.smarthome.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Che do hoat dong cua mot phong. */
enum class RoomMode(val label: String) {
    SLEEP("Ngủ"),
    HOME("Nhà"),
    AWAY("Vắng nhà"),
    CUSTOM("Tùy chỉnh"),
}

/**
 * Mot phong + trang thai thiet bi trong phong.
 * Khung giao dien: trang thai nay chi song trong bo nho, khong gui di dau.
 */
data class RoomUi(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val accent: Color,
    val lightOn: Boolean,
    val fanOn: Boolean,
    val acOn: Boolean,
    val acTemp: Int,
    val mode: RoomMode,
    val scheduleOn: Boolean,
    val scheduleLabel: String,
    val scheduleTime: String,
    val lightBrightness: Int = 100, // 10..100 (%)
    val tvOn: Boolean = false,
    val curtainOpen: Int = 100,     // 0..100 (%)
)

/** Mot o thao tac nhanh o Trang chu. */
data class QuickItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val on: Boolean,
)

/**
 * Mot camera trong danh sach ben trai man Camera.
 *
 * [luong] la ten luong trong go2rtc (vd "cam1"). Null nghia la o giu cho -
 * chua co hinh that, van ve bang [vn.smarthome.ui.components.ImagePlaceholder].
 */
data class CameraFeed(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val accent: Color,
    val luong: String? = null,
)

/** Nhom so lieu o man Thong ke - moi nhom mot tab ben trai. */
enum class StatKind(
    val label: String,
    val title: String,
    val unit: String,
    val color: Color,
) {
    POWER("Điện năng", "Điện năng tiêu thụ", "kWh", Color(0xFF3B82F6)),
    TEMP("Nhiệt độ", "Nhiệt độ trung bình", "°C", Color(0xFFEF4444)),
    HUMID("Độ ẩm", "Độ ẩm trung bình", "%", Color(0xFF22D3EE)),
    WATER("Nước", "Nước tiêu thụ", "m³", Color(0xFF14B8A6)),
    GAS("Gas", "Mức gas còn lại", "%", Color(0xFFFBBF24)),
}

/** Khoang thoi gian cua bieu do. */
enum class StatRange(val label: String, val days: Int) {
    WEEK("7 ngày", 7),
    FORTNIGHT("14 ngày", 14),
    MONTH("30 ngày", 30),
}

/** Mot bo so lieu da san sang ve len bieu do. */
data class StatSeries(
    val kind: StatKind,
    val total: String,
    val unit: String,
    val deltaPercent: Int,
    val bars: List<Float>,
    val labels: List<String>,
    val gaugeLabel: String,
    val gaugeValue: String,
    val gaugeFraction: Float,
    val costLabel: String,
    val costValue: String,
    val statusLabel: String,
    val statusOk: Boolean,
)
