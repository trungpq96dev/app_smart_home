package vn.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/** The chuan: bo goc 14dp, vien 1dp, nen #16213A. */
@Composable
fun SmartCard(
    modifier: Modifier = Modifier,
    background: Color = SH.Card,
    borderColor: Color = SH.Border,
    padding: Dp = Dim.CardPadding,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(Dim.CardRadius),
        color = background,
        border = BorderStroke(Dim.BorderWidth, borderColor),
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

/** Icon tron co nen mo - dung o dau the va trong hang thiet bi. */
@Composable
fun IconBadge(
    icon: ImageVector,
    color: Color,
    size: Dp = 34.dp,
    iconSize: Dp = 18.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp),
    filled: Boolean = false,
) {
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (filled) color else color.tint(0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) Color.White else color,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Chip trang thai: cham tron + chu, nen mo theo mau. */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.tint(0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (showDot) Dot(color, 6.dp)
        Text(text, style = SHType.Label, color = color, maxLines = 1)
    }
}

@Composable
fun Dot(color: Color, size: Dp = 7.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}

/**
 * Chip canh bao rang moi con so tren man hinh deu la so mau.
 * Bat/tat bang DemoData.IS_DEMO - khong phai di tim trong tung man.
 */
@Composable
fun DemoBadge(modifier: Modifier = Modifier) {
    Text(
        "DỮ LIỆU MẪU",
        style = SHType.Caption,
        color = SH.Amber,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SH.Amber.tint(0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/** Trang thai rong cho cac man chua lam - khong bia noi dung de lap cho. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconBadge(icon, SH.TextOff, size = 56.dp, iconSize = 28.dp, shape = CircleShape)
        Text(title, style = SHType.PageTitle, color = SH.TextDim)
        Text(detail, style = SHType.Body, color = SH.TextOff)
    }
}
