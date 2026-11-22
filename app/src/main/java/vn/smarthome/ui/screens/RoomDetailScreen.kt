package vn.smarthome.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import vn.smarthome.data.DemoData
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.ui.components.ChoiceChip
import vn.smarthome.ui.components.DeviceSwitchRow
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.LivingRoomStage
import vn.smarthome.ui.components.MoPhongPhongNgu
import vn.smarthome.ui.components.RoundIconButton
import vn.smarthome.data.congtac.OThietBi
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.TempStepperRow
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint

/**
 * Man dieu khien mot phong.
 *
 * Moi cong tac o day deu doi trang thai that trong HomeViewModel, nen quay ve
 * Trang chu se thay the phong tuong ung doi theo.
 */
@Composable
fun RoomDetailScreen(
    room: RoomUi,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onToggleLight: () -> Unit,
    onToggleFan: () -> Unit,
    onAcTemp: (Int) -> Unit,
    onMode: (RoomMode) -> Unit,
    onToggleSchedule: () -> Unit,
    /** Mac dinh null = thiet bi mau, hanh vi y nhu truoc. */
    oDen: OThietBi? = null,
    oQuat: OThietBi? = null,
    onToggleAc: () -> Unit = {},
    onLightBrightness: (Int) -> Unit = {},
    onToggleTv: () -> Unit = {},
    onToggleCurtain: () -> Unit = {},
    onCurtainOpen: (Int) -> Unit = {},
) {
    val laPhongKhach = room.id == "living"
    val laPhongNgu = room.id in setOf("bed1", "bed2", "bed3")
    val denHienThi = (oDen as? OThietBi.That)?.let {
        val ro = it.dangCho || (it.bridgeSong && it.online && it.bat != null)
        ro && (it.mong ?: it.bat ?: false)
    } ?: room.lightOn
    val quatHienThi = (oQuat as? OThietBi.That)?.let {
        val ro = it.dangCho || (it.bridgeSong && it.online && it.bat != null)
        ro && (it.mong ?: it.bat ?: false)
    } ?: room.fanOn

    Column(Modifier.fillMaxSize()) {

        TopBar(
            title = "Điều khiển " + room.name.lowercase(),
            onBack = onBack,
        ) {
            RoundIconButton(Icons.Filled.Home, onHome, tint = SH.Blue)
        }

        // ---------- Anh phong | cac thiet bi ----------
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            if (laPhongKhach) {
                LivingRoomStage(
                    room = room.copy(lightOn = denHienThi, fanOn = quatHienThi),
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
            } else if (laPhongNgu) {
                MoPhongPhongNgu(
                    lightOn = denHienThi,
                    fanOn = quatHienThi,
                    acOn = room.acOn,
                    acTemp = room.acTemp,
                    mode = room.mode,
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
            } else {
                ImagePlaceholder(
                    icon = room.icon,
                    label = room.name,
                    accent = room.accent,
                    cornerRadius = Dim.CardRadius,
                    iconSize = 44.dp,
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
            }

            if (laPhongKhach) {
                LivingRoomControlsColumn(
                    room = room,
                    oDen = oDen,
                    oQuat = oQuat,
                    onToggleLight = onToggleLight,
                    onLightBrightness = onLightBrightness,
                    onToggleFan = onToggleFan,
                    onToggleTv = onToggleTv,
                    onToggleAc = onToggleAc,
                    onAcTemp = onAcTemp,
                    onToggleCurtain = onToggleCurtain,
                    onCurtainOpen = onCurtainOpen,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            } else {
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
                ) {
                    DeviceSwitchRow(
                        icon = Icons.Filled.Lightbulb,
                        label = "Đèn phòng",
                        color = SH.Amber,
                        checked = room.lightOn,
                        onToggle = onToggleLight,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        o = oDen,
                    )
                    DeviceSwitchRow(
                        icon = Icons.Filled.Air,
                        label = "Quạt trần",
                        color = SH.Blue,
                        checked = room.fanOn,
                        onToggle = onToggleFan,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        o = oQuat,
                    )
                    TempStepperRow(
                        icon = Icons.Filled.AcUnit,
                        label = "Điều hòa",
                        color = SH.Purple,
                        temp = room.acTemp,
                        onMinus = { onAcTemp(-1) },
                        onPlus = { onAcTemp(1) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        acOn = room.acOn,
                        onToggle = onToggleAc,
                    )
                }
            }
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---------- Che do hoat dong | Lich hen ----------
        Row(
            Modifier.fillMaxWidth().height(104.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            SmartCard(Modifier.weight(1.5f).fillMaxHeight(), padding = 10.dp) {
                Text("Chế độ hoạt động", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RoomMode.entries.forEach { m ->
                        ChoiceChip(
                            label = m.label,
                            icon = modeIcon(m),
                            selected = room.mode == m,
                            onClick = { onMode(m) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }

            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                Text("Lịch hẹn", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.Event, SH.Blue, size = 34.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            room.scheduleLabel,
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                        )
                        Text(
                            room.scheduleTime,
                            style = SHType.CardTitle,
                            color = if (room.scheduleOn) SH.Text else SH.TextOff,
                            maxLines = 1,
                        )
                    }
                    Switch(
                        checked = room.scheduleOn,
                        onCheckedChange = { onToggleSchedule() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SH.Blue,
                            checkedBorderColor = SH.Blue,
                            uncheckedThumbColor = SH.TextDim,
                            uncheckedTrackColor = SH.Card2,
                            uncheckedBorderColor = SH.Border,
                        ),
                    )
                }
            }
        }
    }
}

private fun modeIcon(m: RoomMode) = when (m) {
    RoomMode.SLEEP -> Icons.Filled.DarkMode
    RoomMode.HOME -> Icons.Filled.Home
    RoomMode.AWAY -> Icons.Filled.Work
    RoomMode.CUSTOM -> Icons.Filled.Star
}

@Composable
private fun LivingRoomControlsColumn(
    room: RoomUi,
    oDen: OThietBi?,
    oQuat: OThietBi?,
    onToggleLight: () -> Unit,
    onLightBrightness: (Int) -> Unit,
    onToggleFan: () -> Unit,
    onToggleTv: () -> Unit,
    onToggleAc: () -> Unit,
    onAcTemp: (Int) -> Unit,
    onToggleCurtain: () -> Unit,
    onCurtainOpen: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val denThat = oDen as? OThietBi.That
    val lightChecked = denThat?.let { it.mong ?: it.bat ?: false } ?: room.lightOn
    val lightRo = denThat == null || denThat.dangCho || (denThat.bridgeSong && denThat.online && denThat.bat != null)
    val lightEnabled = denThat == null || denThat.gatDuoc

    val quatThat = oQuat as? OThietBi.That
    val fanChecked = quatThat?.let { it.mong ?: it.bat ?: false } ?: room.fanOn
    val fanRo = quatThat == null || quatThat.dangCho || (quatThat.bridgeSong && quatThat.online && quatThat.bat != null)
    val fanEnabled = quatThat == null || quatThat.gatDuoc

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // 1. Den phong + thanh truot do sang
        SmartCard(Modifier.fillMaxWidth().weight(1.18f), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Filled.Lightbulb,
                        if (lightRo && lightChecked) SH.Amber else SH.TextOff,
                        size = 30.dp,
                        iconSize = 17.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Đèn phòng", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            denThat?.ghiChu
                                ?: if (!lightRo) "—"
                                else if (lightChecked) "Đang bật • Độ sáng ${room.lightBrightness}%"
                                else "Đang tắt",
                            style = SHType.Caption,
                            color = if (denThat?.ghiChu != null && !denThat.dangCho) SH.Amber else SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!lightRo) {
                        Text("—", style = SHType.CardTitle, color = SH.TextDim)
                    } else {
                        CompactSwitch(checked = lightChecked, enabled = lightEnabled, onCheckedChange = onToggleLight)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (lightRo && lightChecked) 1f else 0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.WbSunny, null, tint = SH.TextDim, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    CompactSlider(
                        value = room.lightBrightness,
                        valueRange = 10..100,
                        enabled = lightRo && lightChecked,
                        thumbColor = SH.Amber,
                        onValueChange = onLightBrightness,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${room.lightBrightness}%",
                        style = SHType.Label,
                        color = SH.Amber,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.width(34.dp),
                    )
                }
            }
        }

        // 2. Quat tran (giu lai theo yeu cau)
        SmartCard(Modifier.fillMaxWidth().weight(0.84f), padding = 8.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.Air,
                    if (fanRo && fanChecked) SH.Blue else SH.TextOff,
                    size = 30.dp,
                    iconSize = 17.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Quạt trần", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Text(
                        quatThat?.ghiChu
                            ?: if (!fanRo) "—"
                            else if (fanChecked) "Đang bật"
                            else "Đang tắt",
                        style = SHType.Caption,
                        color = if (quatThat?.ghiChu != null && !quatThat.dangCho) SH.Amber else SH.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!fanRo) {
                    Text("—", style = SHType.CardTitle, color = SH.TextDim)
                } else {
                    CompactSwitch(checked = fanChecked, enabled = fanEnabled, onCheckedChange = onToggleFan)
                }
            }
        }

        // 3. Smart TV
        SmartCard(Modifier.fillMaxWidth().weight(0.84f), padding = 8.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.Tv,
                    if (room.tvOn) SH.Blue else SH.TextOff,
                    size = 30.dp,
                    iconSize = 17.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Smart TV", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(if (room.tvOn) SH.Green else SH.TextOff, 6.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (room.tvOn) "Đang phát • 4K HDR" else "Đang tắt",
                            style = SHType.Caption,
                            color = if (room.tvOn) SH.Text else SH.TextDim,
                            maxLines = 1,
                        )
                    }
                }
                CompactSwitch(checked = room.tvOn, enabled = true, onCheckedChange = onToggleTv)
            }
        }

        // 4. Dieu hoa (nut - / nhiet do / + va cong tac bat/tat)
        SmartCard(Modifier.fillMaxWidth().weight(0.94f), padding = 8.dp) {
            val acLevel = when {
                !room.acOn -> "Tắt"
                room.acTemp <= 20 -> "Lạnh sâu"
                room.acTemp <= 25 -> "Mát dịu"
                else -> "Tiết kiệm"
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    Icons.Filled.AcUnit,
                    if (room.acOn) SH.Purple else SH.TextOff,
                    size = 30.dp,
                    iconSize = 17.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text("Điều hòa", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            acLevel,
                            style = SHType.Caption,
                            color = if (room.acOn) SH.Cyan else SH.TextOff,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (room.acOn) SH.Cyan.tint(0.14f) else SH.Card2)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                    Text(
                        if (room.acOn) "Đang làm lạnh • ${room.acTemp}°C" else "Đang tắt",
                        style = SHType.Caption,
                        color = SH.TextDim,
                        maxLines = 1,
                    )
                }
                Row(
                    modifier = Modifier.alpha(if (room.acOn) 1f else 0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactStepButton(Icons.Filled.Remove) { onAcTemp(-1) }
                    Text(
                        "${room.acTemp}°C",
                        style = SHType.BodyMed,
                        color = SH.Text,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.width(42.dp),
                    )
                    CompactStepButton(Icons.Filled.Add) { onAcTemp(1) }
                }
                Spacer(Modifier.width(6.dp))
                CompactSwitch(checked = room.acOn, enabled = true, onCheckedChange = onToggleAc)
            }
        }

        // 5. Rem cua tu dong + nut Dong/Mo + thanh truot %
        SmartCard(Modifier.fillMaxWidth().weight(1.20f), padding = 8.dp) {
            val curtainText = when (room.curtainOpen) {
                0 -> "Đang đóng kín • 0%"
                100 -> "Đang mở hoàn toàn • 100%"
                else -> "Đang mở một phần • ${room.curtainOpen}%"
            }
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Filled.Blinds,
                        if (room.curtainOpen > 0) SH.Teal else SH.TextOff,
                        size = 30.dp,
                        iconSize = 17.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Rèm cửa tự động", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(curtainText, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Card2)
                            .border(1.dp, SH.Border, RoundedCornerShape(8.dp))
                            .clickable(onClick = onToggleCurtain)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (room.curtainOpen > 0) "Đóng rèm" else "Mở rèm",
                            style = SHType.Label,
                            color = SH.Teal,
                            maxLines = 1,
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Đóng", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Spacer(Modifier.width(6.dp))
                    CompactSlider(
                        value = room.curtainOpen,
                        valueRange = 0..100,
                        enabled = true,
                        thumbColor = SH.Teal,
                        onValueChange = onCurtainOpen,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${room.curtainOpen}%",
                        style = SHType.Label,
                        color = SH.Teal,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.width(34.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.scale(0.82f),
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

@Composable
private fun CompactStepButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(SH.Card2)
            .border(1.dp, SH.Border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = SH.Text, modifier = Modifier.size(15.dp))
    }
}

@Composable
private fun CompactSlider(
    value: Int,
    valueRange: IntRange,
    enabled: Boolean,
    thumbColor: Color,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = (valueRange.last - valueRange.first).coerceAtLeast(1)
    val fraction = ((value - valueRange.first).toFloat() / span).coerceIn(0f, 1f)

    fun updateAt(xPx: Float, widthPx: Float) {
        if (!enabled || widthPx <= 1f) return
        val f = (xPx / widthPx).coerceIn(0f, 1f)
        val v = valueRange.first + (f * span).roundToInt()
        onValueChange(v.coerceIn(valueRange.first, valueRange.last))
    }

    Canvas(
        modifier = modifier
            .height(22.dp)
            .pointerInput(enabled, valueRange) {
                detectTapGestures { offset -> updateAt(offset.x, size.width.toFloat()) }
            }
            .pointerInput(enabled, valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    updateAt(change.position.x, size.width.toFloat())
                }
            },
    ) {
        val trackH = 5.dp.toPx()
        val thumbR = 7.dp.toPx()
        val usableW = (size.width - thumbR * 2f).coerceAtLeast(1f)
        val centerY = size.height / 2f
        val thumbX = thumbR + fraction * usableW

        drawRoundRect(
            color = Color(0xFF26324D),
            topLeft = Offset(thumbR, centerY - trackH / 2f),
            size = Size(usableW, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
        )
        drawRoundRect(
            color = thumbColor.copy(alpha = 0.75f),
            topLeft = Offset(thumbR, centerY - trackH / 2f),
            size = Size(fraction * usableW, trackH),
            cornerRadius = CornerRadius(trackH / 2f, trackH / 2f),
        )
        drawCircle(
            color = Color.White,
            radius = thumbR,
            center = Offset(thumbX, centerY),
        )
        drawCircle(
            color = thumbColor,
            radius = thumbR - 1.5.dp.toPx(),
            center = Offset(thumbX, centerY),
        )
    }
}

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
@Composable
private fun RoomDetailPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize()) {
            RoomDetailScreen(DemoData.rooms()[0], {}, {}, {}, {}, {}, {}, {})
        }
    }
}
