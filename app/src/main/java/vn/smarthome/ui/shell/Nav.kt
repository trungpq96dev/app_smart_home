package vn.smarthome.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/** Hai nhom menu ben trai: tach lop su dung hang ngay cho gia dinh va quan tri he thong. */
enum class NavGroup(val title: String) {
    DAILY("HẰNG NGÀY"),
    SYSTEM("HỆ THỐNG"),
}

/** Danh sach muc menu ben trai theo nhom Gia dinh & Ky thuat. */
enum class Dest(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val built: Boolean,
    val group: NavGroup,
) {
    // Cum 1: Su dung hang ngay
    HOME("home", "Trang chủ", Icons.Filled.Home, true, NavGroup.DAILY),
    DEVICES("devices", "Thiết bị", Icons.Filled.Devices, true, NavGroup.DAILY),
    CAMERA("camera", "Camera", Icons.Filled.Videocam, true, NavGroup.DAILY),
    UTILITIES("utilities", "Tiện ích", Icons.Filled.GridView, true, NavGroup.DAILY),

    // Cum 2: Quan tri & He thong
    AUTOMATION("automation", "Tự động hóa", Icons.Filled.AutoAwesome, true, NavGroup.SYSTEM),
    SENSORS("sensors", "Cảm biến", Icons.Filled.Sensors, true, NavGroup.SYSTEM),
    STATS("stats", "Thống kê", Icons.Filled.BarChart, true, NavGroup.SYSTEM),
    SERVER("server", "Server", Icons.Filled.Dns, true, NavGroup.SYSTEM),
    SETTINGS("settings", "Cài đặt", Icons.Filled.Settings, true, NavGroup.SYSTEM);

    companion object {
        val daily: List<Dest> = entries.filter { it.group == NavGroup.DAILY }
        val system: List<Dest> = entries.filter { it.group == NavGroup.SYSTEM }

        fun from(route: String?) = entries.firstOrNull { it.route == route } ?: HOME
    }
}
