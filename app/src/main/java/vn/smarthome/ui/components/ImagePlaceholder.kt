package vn.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * O giu cho anh - ve bang code, khong nhung file anh nao vao APK.
 *
 * DAY LA KHE CAM ANH THAT SAU NAY. Khi co anh phong hoac luong camera:
 *   - anh tinh  : doi than ham nay thanh Image(painterResource(...))
 *   - camera    : boc mot AndroidView chua PlayerView cua ExoPlayer/libVLC
 * Moi cho goi deu di qua day nen chi phai sua dung mot file.
 */
@Composable
fun ImagePlaceholder(
    icon: ImageVector,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    iconSize: Dp = 34.dp,
    showLabel: Boolean = true,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.tint(0.22f),
                        SH.Card2,
                        SH.Bg,
                    )
                )
            )
            .border(1.dp, SH.Border, RoundedCornerShape(cornerRadius)),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                // null chu khong phai label: o nao goi ham nay cung da co chu
                // ten phong/ten camera ngay canh, de contentDescription nua la
                // trinh doc man hinh doc lap hai lan.
                contentDescription = null,
                tint = accent.copy(alpha = 0.55f),
                modifier = Modifier.size(iconSize),
            )
            if (showLabel) {
                Spacer(Modifier.height(6.dp))
                Text(label, style = SHType.Label, color = SH.TextDim, maxLines = 1)
                Text("chưa gắn ảnh", style = SHType.Caption, color = SH.TextOff, maxLines = 1)
            }
        }
        overlay()
    }
}
