package vn.smarthome.ui.shell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector

/** Sau muc menu ben trai, dung thu tu nhu anh thiet ke. */
enum class Dest(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val built: Boolean,
) {
    HOME("home", "Trang chủ", Icons.Filled.Home, true),
    DEVICES("devices", "Thiết bị", Icons.Filled.Devices, true),
    CAMERA("camera", "Camera", Icons.Filled.Videocam, true),
    SENSORS("sensors", "Cảm biến", Icons.Filled.Sensors, false),
    AUTOMATION("automation", "Tự động hóa", Icons.Filled.AutoAwesome, false),
    SETTINGS("settings", "Cài đặt", Icons.Filled.Settings, false);

    companion object {
        fun from(route: String?) = entries.firstOrNull { it.route == route } ?: HOME
    }
}
