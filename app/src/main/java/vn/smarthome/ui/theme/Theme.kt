package vn.smarthome.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SmartHomeColors = darkColorScheme(
    primary          = SH.Blue,
    onPrimary        = SH.Text,
    secondary        = SH.Green,
    onSecondary      = SH.Text,
    background       = SH.Bg,
    onBackground     = SH.Text,
    surface          = SH.Card,
    onSurface        = SH.Text,
    surfaceVariant   = SH.Card2,
    onSurfaceVariant = SH.TextDim,
    error            = SH.Red,
    outline          = SH.Border,
)

/** Chi co che do toi: may tinh bang treo tuong dung nen toi la dung muc dich. */
@Composable
fun SmartHomeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SmartHomeColors,
        typography = SHTypography,
        content = content,
    )
}
