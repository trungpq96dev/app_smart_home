package vn.smarthome.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import vn.smarthome.data.congtac.OThietBi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/** O so lieu o hang tren Trang chu: icon, so lon, nhan nho. */
@Composable
fun StatTile(
    icon: ImageVector,
    value: String,
    unit: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier = modifier, padding = 6.dp) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon, color, size = 28.dp, iconSize = 16.dp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = SHType.Value, color = SH.Text, maxLines = 1)
                if (unit.isNotEmpty()) {
                    Text(
                        unit,
                        style = SHType.Label,
                        color = SH.TextDim,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                    )
                }
            }
            Text(
                label,
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** O thao tac nhanh: vong tron mau khi bat, xam khi tat. */
@Composable
fun QuickActionTile(
    icon: ImageVector,
    label: String,
    color: Color,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val circle by animateColorAsState(if (on) color else SH.Card2, label = "quickCircle")
    val fg by animateColorAsState(if (on) Color.White else SH.TextOff, label = "quickFg")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dim.CardRadius))
            .background(if (on) color.tint(0.10f) else SH.Card)
            .border(
                Dim.BorderWidth,
                if (on) color.tint(0.45f) else SH.Border,
                RoundedCornerShape(Dim.CardRadius),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.height(5.dp))
            Text(label, style = SHType.Label, color = if (on) SH.Text else SH.TextDim, maxLines = 1)
        }
    }
}

/**
 * Hang thiet bi co cong tac gat (Den phong, Quat tran...).
 *
 * [o] = null thi giu nguyen hanh vi cu (thiet bi mau, va moi preview/test san
 * co khong phai sua). Co [o] la thiet bi THAT: luc do man hinh khong duoc tu
 * bia trang thai - khong ro thi ve dau gach "—" va an han Switch di, chu
 * tuyet doi khong ve mot cai cong tac nam o the TAT.
 */
@Composable
fun DeviceSwitchRow(
    icon: ImageVector,
    label: String,
    color: Color,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    o: OThietBi? = null,
) {
    val that = o as? OThietBi.That
    // Dang cho thi ve o the nguoi dung vua gat toi, cho do mat tu nhien.
    val veBat = that?.let { it.mong ?: it.bat ?: false } ?: checked
    val roRang = that == null || that.dangCho || (that.online && that.bat != null)
    val ghiChu = that?.ghiChu

    SmartCard(modifier = modifier.defaultMinSize(minHeight = Dim.DeviceRowH), padding = 10.dp) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon,
                if (roRang && veBat) color else SH.TextOff,
                size = 36.dp,
                iconSize = 19.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = SHType.CardTitle,
                    color = SH.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ghiChu != null) {
                    Text(
                        ghiChu,
                        style = SHType.Label,
                        color = if (that?.dangCho == true) SH.TextDim else SH.Amber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!roRang) {
                // Khong ro thi noi thang la khong ro. Mot cai cong tac gat
                // duoc, nam o the TAT, trong khi app khong he biet den dang
                // sang hay khong, la loi noi doi ton it pixel nhat trong app.
                Text("—", style = SHType.CardTitle, color = SH.TextDim)
            } else {
                Switch(
                    checked = veBat,
                    enabled = that == null || that.gatDuoc,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = SH.Green,
                        checkedBorderColor = SH.Green,
                        uncheckedThumbColor = SH.TextDim,
                        uncheckedTrackColor = SH.Card2,
                        uncheckedBorderColor = SH.Border,
                    ),
                )
            }
        }
    }
}

/** Hang dieu hoa: nut tru / so do C / nut cong. */
@Composable
fun TempStepperRow(
    icon: ImageVector,
    label: String,
    color: Color,
    temp: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier = modifier.defaultMinSize(minHeight = Dim.DeviceRowH), padding = 10.dp) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon, color, size = 36.dp, iconSize = 19.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                label,
                style = SHType.CardTitle,
                color = SH.Text,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            StepButton(Icons.Filled.Remove, onMinus)
            Text(
                temp.toString() + "°C",
                style = SHType.BodyMed,
                color = SH.Text,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(52.dp),
            )
            StepButton(Icons.Filled.Add, onPlus)
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(SH.Card2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = SH.Text, modifier = Modifier.size(17.dp))
    }
}

/** Chip chon che do / chon khoang thoi gian. */
@Composable
fun ChoiceChip(
    label: String,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(if (selected) SH.Blue else SH.Card2, label = "chipBg")
    val fg = if (selected) Color.White else SH.TextDim

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
        }
        Text(label, style = SHType.Label, color = fg, maxLines = 1)
    }
}

/** Nut bam ngang dung o thanh duoi man Camera. */
@Composable
fun FlatButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(Dim.ButtonH)
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = SH.TextDim, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(label, style = SHType.Button, color = SH.Text, maxLines = 1)
    }
}

/** Nut icon vuong bo goc dung o thanh tieu de. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = SH.TextDim,
    background: Color = SH.Card2,
) {
    Box(
        modifier = modifier
            .size(Dim.TouchTarget)
            .clip(RoundedCornerShape(11.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** Cham chi so trang cua bang xoay the phong. */
@Composable
fun PagerDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { i ->
            val active = i == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (active) 16.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) SH.Blue else SH.TextOff.tint(0.6f))
            )
        }
    }
}
