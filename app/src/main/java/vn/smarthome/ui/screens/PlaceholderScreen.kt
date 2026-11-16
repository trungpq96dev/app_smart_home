package vn.smarthome.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.components.EmptyState
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Man chua lam.
 *
 * Co y de trong va noi thang la chua lam, thay vi bia noi dung gia cho day
 * man hinh - nguoi xem phai phan biet duoc cai gi da co va cai gi chua.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    icon: ImageVector,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title = title, onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = icon,
                title = title,
                detail = "Màn hình này sẽ làm ở giai đoạn sau",
            )
        }
    }
}

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
@Composable
private fun PlaceholderPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            PlaceholderScreen("Thiết bị", Icons.Filled.Devices) {}
        }
    }
}
