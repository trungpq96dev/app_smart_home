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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
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
import vn.smarthome.data.HallwayView
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.data.stoveWatts
import vn.smarthome.ui.components.ChoiceChip
import vn.smarthome.ui.components.DeviceSwitchRow
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.HallwayStage
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.KitchenStage
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
    onHoodSpeed: (Int) -> Unit = {},
    onToggleStove: () -> Unit = {},
    onStoveLevel: (Int) -> Unit = {},
    onGasAlert: (Boolean) -> Unit = {},
    onFanSpeed: (Int) -> Unit = {},
    onHallView: (HallwayView) -> Unit = {},
    onToggleHallFrontLight: () -> Unit = {},
    onHallFrontBrightness: (Int) -> Unit = {},
    onToggleHallSurroundingLight: () -> Unit = {},
    onHallSurroundingBrightness: (Int) -> Unit = {},
    onToggleHallAllLights: () -> Unit = {},
    onToggleHallLock: () -> Unit = {},
    onToggleHallCamera: () -> Unit = {},
    onToggleHallIr: () -> Unit = {},
    onToggleHallMotionAuto: () -> Unit = {},
    onTriggerHallMotion: () -> Unit = {},
    onDismissHallMotion: () -> Unit = {},
) {
    val laPhongKhach = room.id == "living"
    val laNhaBep = room.id == "kitchen"
    val laHanhLang = room.id == "hall"
    val laPhongNgu = room.id in setOf("bed1", "bed2", "bed3")
    val denHienThi = (oDen as? OThietBi.That)?.let {
        when {
            it.dangCho -> it.mong ?: room.lightOn
            it.bridgeSong && it.online && it.bat != null -> it.bat
            else -> room.lightOn
        }
    } ?: room.lightOn
    val quatHienThi = (oQuat as? OThietBi.That)?.let {
        when {
            it.dangCho -> it.mong ?: room.fanOn
            it.bridgeSong && it.online && it.bat != null -> it.bat
            else -> room.fanOn
        }
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
            } else if (laNhaBep) {
                KitchenStage(
                    room = room.copy(lightOn = denHienThi, fanOn = quatHienThi),
                    onSilenceGasAlert = { onGasAlert(false) },
                    modifier = Modifier.weight(1.05f).fillMaxHeight(),
                )
            } else if (laHanhLang) {
                HallwayStage(
                    room = room.copy(lightOn = denHienThi),
                    onSelectView = onHallView,
                    onDismissMotion = onDismissHallMotion,
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
                    lightBrightness = room.lightBrightness,
                    fanSpeed = room.fanSpeed,
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
            } else if (laNhaBep) {
                KitchenControlsColumn(
                    room = room,
                    oDen = oDen,
                    oQuat = oQuat,
                    onToggleLight = onToggleLight,
                    onLightBrightness = onLightBrightness,
                    onToggleHood = onToggleFan,
                    onHoodSpeed = onHoodSpeed,
                    onToggleStove = onToggleStove,
                    onStoveLevel = onStoveLevel,
                    onGasAlert = onGasAlert,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            } else if (laHanhLang) {
                HallwayControlsColumn(
                    room = room,
                    oDen = oDen,
                    onToggleHallwayLight = onToggleLight,
                    onHallwayBrightness = onLightBrightness,
                    onToggleFrontLight = onToggleHallFrontLight,
                    onFrontBrightness = onHallFrontBrightness,
                    onToggleSurroundingLight = onToggleHallSurroundingLight,
                    onSurroundingBrightness = onHallSurroundingBrightness,
                    onToggleAllLights = onToggleHallAllLights,
                    onToggleLock = onToggleHallLock,
                    onToggleCamera = onToggleHallCamera,
                    onToggleIr = onToggleHallIr,
                    onToggleMotionAuto = onToggleHallMotionAuto,
                    onTriggerMotion = onTriggerHallMotion,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            } else if (laPhongNgu) {
                BedroomControlsColumn(
                    room = room,
                    denHienThi = denHienThi,
                    quatHienThi = quatHienThi,
                    oDen = oDen,
                    oQuat = oQuat,
                    onToggleLight = onToggleLight,
                    onLightBrightness = onLightBrightness,
                    onToggleFan = onToggleFan,
                    onFanSpeed = onFanSpeed,
                    onToggleAc = onToggleAc,
                    onAcTemp = onAcTemp,
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
                    val modeItems = if (laNhaBep) {
                        listOf(
                            Triple(RoomMode.HOME, "Nấu ăn", Icons.Filled.LocalFireDepartment),
                            Triple(RoomMode.CUSTOM, "Ăn tối", Icons.Filled.Restaurant),
                            Triple(RoomMode.SLEEP, "Dọn dẹp", Icons.Filled.AutoAwesome),
                            Triple(RoomMode.AWAY, "Vắng nhà", Icons.Filled.Work),
                        )
                    } else if (laHanhLang) {
                        listOf(
                            Triple(RoomMode.SLEEP, "Đêm an ninh", Icons.Filled.DarkMode),
                            Triple(RoomMode.HOME, "Về nhà", Icons.Filled.Home),
                            Triple(RoomMode.CUSTOM, "Tiếp khách", Icons.Filled.Groups),
                            Triple(RoomMode.AWAY, "Đi vắng", Icons.Filled.Work),
                        )
                    } else {
                        RoomMode.entries.map { Triple(it, it.label, modeIcon(it)) }
                    }
                    modeItems.forEach { (m, label, icon) ->
                        ChoiceChip(
                            label = label,
                            icon = icon,
                            selected = room.mode == m,
                            onClick = { onMode(m) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }

            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                if (laNhaBep) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Lịch hẹn & An toàn", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        val pillColor = if (room.gasAlert) SH.Red else SH.Green
                        Text(
                            text = if (room.gasAlert) "Cảnh báo rò rỉ!" else "An toàn 24/7",
                            style = SHType.Caption,
                            color = pillColor,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(pillColor.tint(0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(Icons.Filled.Schedule, SH.Blue, size = 34.dp, iconSize = 18.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Tự tắt hút mùi sau 15 phút",
                                style = SHType.Label,
                                color = SH.Text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                if (room.scheduleOn) "Hẹn giờ thông minh đang bật" else "Đã tạm tắt hẹn giờ",
                                style = SHType.Caption,
                                color = if (room.scheduleOn) SH.TextDim else SH.TextOff,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
                } else if (laHanhLang) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Lịch hẹn", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            text = if (room.scheduleOn) "Đang bật" else "Đã tắt",
                            style = SHType.Caption,
                            color = if (room.scheduleOn) SH.Blue else SH.TextDim,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (room.scheduleOn) SH.Blue.tint(0.16f) else SH.Card2)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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
                                overflow = TextOverflow.Ellipsis,
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
                } else {
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
}

private fun modeIcon(m: RoomMode) = when (m) {
    RoomMode.SLEEP -> Icons.Filled.DarkMode
    RoomMode.HOME -> Icons.Filled.Home
    RoomMode.AWAY -> Icons.Filled.Work
    RoomMode.CUSTOM -> Icons.Filled.Star
}

@Composable
private fun BedroomControlsColumn(
    room: RoomUi,
    denHienThi: Boolean,
    quatHienThi: Boolean,
    oDen: OThietBi?,
    oQuat: OThietBi?,
    onToggleLight: () -> Unit,
    onLightBrightness: (Int) -> Unit,
    onToggleFan: () -> Unit,
    onFanSpeed: (Int) -> Unit,
    onToggleAc: () -> Unit,
    onAcTemp: (Int) -> Unit,
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

    val speedLabels = when (room.fanSpeed) {
        1 -> "Nhẹ"
        2 -> "Vừa"
        else -> "Mạnh"
    }
    val acDesc = when {
        !room.acOn -> "Đang tắt"
        room.acTemp <= 20 -> "Đang làm lạnh • Lạnh sâu"
        room.acTemp <= 25 -> "Đang làm lạnh • Mát dịu"
        else -> "Đang làm lạnh • Tiết kiệm"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // 1. Den phong + thanh truot do sang
        SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 10.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !lightRo || lightEnabled, onClick = onToggleLight)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(
                            Icons.Filled.Lightbulb,
                            if (denHienThi) SH.Amber else SH.TextOff,
                            size = 36.dp,
                            iconSize = 19.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Đèn phòng", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                            Text(
                                denThat?.ghiChu
                                    ?: if (denHienThi) "Đang bật • Độ sáng ${room.lightBrightness}%"
                                    else "Đang tắt",
                                style = SHType.Label,
                                color = if (denThat?.ghiChu != null && !denThat.dangCho) SH.Amber else SH.TextDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!lightRo) {
                        Text("—", style = SHType.CardTitle, color = SH.TextDim)
                    } else {
                        Switch(
                            checked = lightChecked,
                            enabled = lightEnabled,
                            onCheckedChange = { onToggleLight() },
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
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (denHienThi) 1f else 0.35f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.WbSunny, null, tint = SH.TextDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    CompactSlider(
                        value = room.lightBrightness,
                        valueRange = 10..100,
                        enabled = denHienThi && (!lightRo || lightEnabled),
                        thumbColor = SH.Amber,
                        onValueChange = onLightBrightness,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${room.lightBrightness}%",
                        style = SHType.Label,
                        color = SH.Amber,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier.width(36.dp),
                    )
                }
            }
        }

        // 2. Quat tran + toc do gio 1 - 2 - 3
        SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 10.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !fanRo || fanEnabled, onClick = onToggleFan)
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconBadge(
                            Icons.Filled.Air,
                            if (quatHienThi) SH.Blue else SH.TextOff,
                            size = 36.dp,
                            iconSize = 19.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Quạt trần", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                            Text(
                                quatThat?.ghiChu
                                    ?: if (quatHienThi) "Đang quay • Tốc độ ${room.fanSpeed} ($speedLabels)"
                                    else "Đang tắt",
                                style = SHType.Label,
                                color = if (quatThat?.ghiChu != null && !quatThat.dangCho) SH.Amber else SH.TextDim,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (!fanRo) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (quatHienThi) SH.Blue.tint(0.18f) else SH.Card2)
                                    .border(
                                        1.dp,
                                        if (quatHienThi) SH.Blue else SH.Border,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable(onClick = onToggleFan)
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    if (quatHienThi) "Tắt quạt" else "Bật quạt",
                                    style = SHType.Label,
                                    color = if (quatHienThi) SH.Blue else SH.Text,
                                    maxLines = 1,
                                )
                            }
                            Text("—", style = SHType.CardTitle, color = SH.TextDim)
                        }
                    } else {
                        Switch(
                            checked = fanChecked,
                            enabled = fanEnabled,
                            onCheckedChange = { onToggleFan() },
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
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Tốc độ gió",
                        style = SHType.Label,
                        color = if (quatHienThi) SH.TextDim else SH.TextOff,
                        maxLines = 1,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..3).forEach { spd ->
                            val active = quatHienThi && room.fanSpeed == spd
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) SH.Blue else SH.Card2)
                                    .border(
                                        1.dp,
                                        if (active) SH.Blue else SH.Border,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable { onFanSpeed(spd) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    spd.toString(),
                                    style = SHType.Label,
                                    color = if (active) Color.White else SH.TextDim,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Dieu hoa + bat/tat + tang/giam nhiet do
        SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 10.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(
                        Icons.Filled.AcUnit,
                        if (room.acOn) SH.Purple else SH.TextOff,
                        size = 36.dp,
                        iconSize = 19.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Điều hòa", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            acDesc,
                            style = SHType.Label,
                            color = SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BedroomTogglePill(checked = room.acOn, onClick = onToggleAc)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (room.acOn) 1f else 0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Nhiệt độ mục tiêu", style = SHType.Label, color = SH.TextDim, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompactStepButton(Icons.Filled.Remove) { onAcTemp(-1) }
                        Text(
                            "${room.acTemp}°C",
                            style = SHType.BodyMed,
                            color = SH.Text,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.width(48.dp),
                        )
                        CompactStepButton(Icons.Filled.Add) { onAcTemp(1) }
                    }
                }
            }
        }
    }
}

/** Nut gat kieu iOS cho dieu hoa phong ngu (khong dung Switch de giu dung semantics test cong tac that). */
@Composable
private fun BedroomTogglePill(
    checked: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (checked) SH.Green else Color(0xFF2A354D)
    val borderColor = if (checked) SH.Green else Color(0x14FFFFFF)
    Box(
        modifier = Modifier
            .size(width = 50.dp, height = 28.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color(0xFFCBD5E1)),
        )
    }
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
    val (lightChecked, lightRo, lightEnabled) = resolveSwitchState(oDen, room.lightOn)
    val quatThat = oQuat as? OThietBi.That
    val (fanChecked, fanRo, fanEnabled) = resolveSwitchState(oQuat, room.fanOn)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // 1. Den phong + thanh truot do sang
        LightBrightnessCard(
            label = "Đèn phòng",
            brightness = room.lightBrightness,
            denThat = oDen as? OThietBi.That,
            lightChecked = lightChecked,
            lightRo = lightRo,
            lightEnabled = lightEnabled,
            onToggleLight = onToggleLight,
            onLightBrightness = onLightBrightness,
            modifier = Modifier.fillMaxWidth().weight(1.18f),
        )

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
private fun KitchenControlsColumn(
    room: RoomUi,
    oDen: OThietBi?,
    oQuat: OThietBi?,
    onToggleLight: () -> Unit,
    onLightBrightness: (Int) -> Unit,
    onToggleHood: () -> Unit,
    onHoodSpeed: (Int) -> Unit,
    onToggleStove: () -> Unit,
    onStoveLevel: (Int) -> Unit,
    onGasAlert: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (lightChecked, lightRo, lightEnabled) = resolveSwitchState(oDen, room.lightOn)
    val quatThat = oQuat as? OThietBi.That
    val (hoodChecked, hoodRo, hoodEnabled) = resolveSwitchState(oQuat, room.fanOn)

    val hoodSpeed = room.hoodSpeed.coerceIn(1, 3)
    val hoodLabel = when (hoodSpeed) {
        1 -> "Nhẹ"
        2 -> "Vừa"
        else -> "Mạnh"
    }
    val stoveLevel = room.stoveLevel.coerceIn(1, 9)
    val watts = if (room.stoveOn) stoveWatts(stoveLevel) else 0

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 1. Den bep + thanh truot do sang
        LightBrightnessCard(
            label = "Đèn bếp",
            brightness = room.lightBrightness,
            denThat = oDen as? OThietBi.That,
            lightChecked = lightChecked,
            lightRo = lightRo,
            lightEnabled = lightEnabled,
            onToggleLight = onToggleLight,
            onLightBrightness = onLightBrightness,
            modifier = Modifier.fillMaxWidth().weight(1.05f),
        )

        // 2. May hut mui + 3 cap do hut
        SmartCard(Modifier.fillMaxWidth().weight(0.95f), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Filled.Air,
                        if (hoodRo && hoodChecked) Color(0xFF38BDF8) else SH.TextOff,
                        size = 30.dp,
                        iconSize = 17.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Máy hút mùi", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            quatThat?.ghiChu
                                ?: if (!hoodRo) "—"
                                else if (hoodChecked) "Đang hút • Cấp $hoodSpeed ($hoodLabel)"
                                else "Đang tắt",
                            style = SHType.Caption,
                            color = if (quatThat?.ghiChu != null && !quatThat.dangCho) SH.Amber else SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!hoodRo) {
                        Text("—", style = SHType.CardTitle, color = SH.TextDim)
                    } else {
                        CompactSwitch(checked = hoodChecked, enabled = hoodEnabled, onCheckedChange = onToggleHood)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (hoodRo && hoodChecked) 1f else 0.4f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Cấp độ hút", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        listOf(1 to "1 - Nhẹ", 2 to "2 - Vừa", 3 to "3 - Mạnh").forEach { (spd, txt) ->
                            val selected = hoodRo && hoodChecked && hoodSpeed == spd
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) SH.Blue else SH.Card2)
                                    .border(1.dp, if (selected) SH.Blue else SH.Border, RoundedCornerShape(8.dp))
                                    .clickable(enabled = hoodRo && hoodChecked) { onHoodSpeed(spd) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = txt,
                                    style = SHType.Label,
                                    color = if (selected) Color.White else SH.TextDim,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Bep tu + cong suat Watt + nut - / Muc 1-9 / +
        SmartCard(Modifier.fillMaxWidth().weight(1.05f), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Filled.LocalFireDepartment,
                        if (room.stoveOn) SH.Orange else SH.TextOff,
                        size = 30.dp,
                        iconSize = 17.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Bếp từ", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                            Text(
                                text = "${watts}W",
                                style = SHType.Caption,
                                color = if (room.stoveOn) Color(0xFFFDBA74) else SH.TextDim,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(if (room.stoveOn) SH.Orange.tint(0.18f) else SH.Card2)
                                    .border(
                                        1.dp,
                                        if (room.stoveOn) SH.Orange.tint(0.4f) else SH.Border,
                                        RoundedCornerShape(5.dp),
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                        Text(
                            if (room.stoveOn) "Đang nấu • Công suất mức $stoveLevel" else "Đang tắt • Vùng nấu nguội",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    CompactSwitch(
                        checked = room.stoveOn,
                        enabled = true,
                        checkedColor = SH.Orange,
                        onCheckedChange = onToggleStove,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (room.stoveOn) 1f else 0.4f),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Mức công suất (1–9)", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CompactStepButton(Icons.Filled.Remove, enabled = room.stoveOn) { onStoveLevel(-1) }
                        Text(
                            text = "Mức $stoveLevel",
                            style = SHType.BodyMed,
                            color = SH.Orange,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.width(52.dp),
                        )
                        CompactStepButton(Icons.Filled.Add, enabled = room.stoveOn) { onStoveLevel(1) }
                    }
                }
            }
        }

        // 4. Cam bien Gas & Khoi + Test canh bao / Tat canh bao
        SmartCard(
            modifier = Modifier.fillMaxWidth().weight(1.05f),
            borderColor = if (room.gasAlert) SH.Red.copy(alpha = 0.7f) else SH.Border,
            padding = 8.dp,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        Icons.Filled.Shield,
                        if (room.gasAlert) SH.Red else SH.Green,
                        size = 30.dp,
                        iconSize = 17.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cảm biến Gas & Khói", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            if (room.gasAlert) "NGUY HIỂM • Phát hiện rò rỉ Gas/Khói!" else "An toàn • Không phát hiện rò rỉ",
                            style = SHType.Caption,
                            color = if (room.gasAlert) SH.Red else SH.Green,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${room.gasPpm} ppm",
                            style = SHType.BodyMed,
                            color = if (room.gasAlert) SH.Red else SH.Green,
                            maxLines = 1,
                        )
                        Text(
                            text = "Ngưỡng < 100",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Red.tint(0.15f))
                            .border(1.dp, SH.Red.tint(0.4f), RoundedCornerShape(8.dp))
                            .clickable { onGasAlert(true) }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Test cảnh báo", style = SHType.Label, color = Color(0xFFFCA5A5), maxLines = 1)
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (room.gasAlert) 1f else 0.4f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Card2)
                            .border(1.dp, SH.Border, RoundedCornerShape(8.dp))
                            .clickable(enabled = room.gasAlert) { onGasAlert(false) }
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Check, null, tint = SH.Text, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tắt cảnh báo", style = SHType.Label, color = SH.Text, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun HallwayControlsColumn(
    room: RoomUi,
    oDen: OThietBi?,
    onToggleHallwayLight: () -> Unit,
    onHallwayBrightness: (Int) -> Unit,
    onToggleFrontLight: () -> Unit,
    onFrontBrightness: (Int) -> Unit,
    onToggleSurroundingLight: () -> Unit,
    onSurroundingBrightness: (Int) -> Unit,
    onToggleAllLights: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleIr: () -> Unit,
    onToggleMotionAuto: () -> Unit,
    onTriggerMotion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val denThat = oDen as? OThietBi.That
    val (hallChecked, hallRo, hallEnabled) = resolveSwitchState(oDen, room.lightOn)
    val anyLightOn = (hallRo && hallChecked) || room.hallFrontLightOn || room.hallSurroundingLightOn

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // 1. He thong chieu sang (3 cum den doc lap + nut Bat/Tat tat ca)
        SmartCard(Modifier.fillMaxWidth().weight(2.04f), padding = 7.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(SH.Amber, 6.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Hệ thống chiếu sáng (3 cụm đèn)",
                            style = SHType.Caption,
                            color = SH.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(SH.Card2)
                            .border(1.dp, SH.Border, RoundedCornerShape(7.dp))
                            .clickable(onClick = onToggleAllLights)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = if (anyLightOn) "Tắt tất cả đèn" else "Bật tất cả đèn",
                            style = SHType.Caption,
                            color = SH.Amber,
                            maxLines = 1,
                        )
                    }
                }

                HallwayLightRow(
                    icon = Icons.Filled.Lightbulb,
                    name = "Đèn hành lang",
                    desc = "Sảnh hiên & tường hành lang",
                    checked = hallChecked,
                    roRang = hallRo,
                    enabled = hallEnabled,
                    brightness = room.lightBrightness,
                    note = denThat?.ghiChu,
                    notePending = denThat?.dangCho == true,
                    onToggle = onToggleHallwayLight,
                    onBrightness = onHallwayBrightness,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                HallwayLightRow(
                    icon = Icons.Filled.WbSunny,
                    name = "Đèn phía trước nhà",
                    desc = "Diềm mái Nhật & trụ cổng",
                    checked = room.hallFrontLightOn,
                    roRang = true,
                    enabled = true,
                    brightness = room.hallFrontBrightness,
                    note = null,
                    notePending = false,
                    onToggle = onToggleFrontLight,
                    onBrightness = onFrontBrightness,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                HallwayLightRow(
                    icon = Icons.Filled.AutoAwesome,
                    name = "Đèn xung quanh",
                    desc = "Sân vườn, đèn nấm & tường rào",
                    checked = room.hallSurroundingLightOn,
                    roRang = true,
                    enabled = true,
                    brightness = room.hallSurroundingBrightness,
                    note = null,
                    notePending = false,
                    onToggle = onToggleSurroundingLight,
                    onBrightness = onSurroundingBrightness,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }

        // 2. Khoa cua chinh
        SmartCard(Modifier.fillMaxWidth().weight(0.62f), padding = 7.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(
                    icon = if (room.hallDoorLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    color = if (room.hallDoorLocked) SH.Green else SH.Red,
                    size = 28.dp,
                    iconSize = 16.dp,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("Khóa cửa chính", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        val pillColor = if (room.hallDoorLocked) SH.Green else SH.Red
                        Text(
                            text = if (room.hallDoorLocked) "Đã khóa" else "Đang mở",
                            style = SHType.Caption,
                            color = pillColor,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(pillColor.tint(0.16f))
                                .border(1.dp, pillColor.tint(0.4f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                    Text(
                        text = if (room.hallDoorLocked) "Chốt an ninh đang kích hoạt" else "Cảnh báo: Cửa chính đang mở",
                        style = SHType.Caption,
                        color = SH.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                HallMiniSwitch(
                    checked = room.hallDoorLocked,
                    enabled = true,
                    activeColor = SH.Green,
                    onClick = onToggleLock,
                )
            }
        }

        // 3. Camera hanh lang + Hong ngoai (IR)
        SmartCard(Modifier.fillMaxWidth().weight(0.82f), padding = 7.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Filled.Videocam,
                        color = if (room.hallCameraOn) SH.Blue else SH.TextOff,
                        size = 28.dp,
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Camera hành lang", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            text = when {
                                !room.hallCameraOn -> "Camera đang tắt"
                                room.hallIrMode -> "Đang ghi hình • Hồng ngoại (IR)"
                                else -> "Đang ghi hình • 3 góc nhìn"
                            },
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HallMiniSwitch(
                        checked = room.hallCameraOn,
                        enabled = true,
                        activeColor = SH.Green,
                        onClick = onToggleCamera,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (room.hallCameraOn) 1f else 0.4f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Dot(SH.Green, 6.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Chế độ Hồng ngoại (IR)",
                        style = SHType.Caption,
                        color = SH.Text,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    HallMiniSwitch(
                        checked = room.hallIrMode,
                        enabled = room.hallCameraOn,
                        activeColor = SH.Blue,
                        onClick = onToggleIr,
                    )
                }
            }
        }

        // 4. Cam bien chuyen dong + Test phat hien
        SmartCard(
            modifier = Modifier.fillMaxWidth().weight(0.84f),
            borderColor = if (room.hallMotionDetected) SH.Red.copy(alpha = 0.7f) else SH.Border,
            padding = 7.dp,
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        icon = Icons.Filled.Sensors,
                        color = if (room.hallMotionDetected) SH.Red else SH.Purple,
                        size = 28.dp,
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Cảm biến chuyển động", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        Text(
                            text = "Tự bật Đèn hành lang & Xung quanh",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HallMiniSwitch(
                        checked = room.hallMotionAuto,
                        enabled = true,
                        activeColor = SH.Green,
                        onClick = onToggleMotionAuto,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Tự động bật đèn: ", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                        Text(
                            text = if (room.hallMotionAuto) "Đang bật" else "Đang tắt",
                            style = SHType.Caption,
                            color = if (room.hallMotionAuto) SH.Green else SH.TextOff,
                            maxLines = 1,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(SH.Red.tint(0.16f))
                            .border(1.dp, SH.Red.tint(0.4f), RoundedCornerShape(7.dp))
                            .clickable(onClick = onTriggerMotion)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "Test phát hiện",
                            style = SHType.Caption,
                            color = Color(0xFFFCA5A5),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HallwayLightRow(
    icon: ImageVector,
    name: String,
    desc: String,
    checked: Boolean,
    roRang: Boolean,
    enabled: Boolean,
    brightness: Int,
    note: String?,
    notePending: Boolean,
    onToggle: () -> Unit,
    onBrightness: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Card2.copy(alpha = 0.65f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (roRang && checked) SH.Amber else SH.TextOff,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(name, style = SHType.Label, color = SH.Text, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Text(
                text = note ?: if (!roRang) "—" else if (checked) "$desc • $brightness%" else "$desc • Đang tắt",
                style = SHType.Caption,
                color = if (note != null && !notePending) SH.Amber else SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            if (!roRang) {
                Text("—", style = SHType.Label, color = SH.TextDim)
            } else {
                HallMiniSwitch(
                    checked = checked,
                    enabled = enabled,
                    activeColor = SH.Green,
                    onClick = onToggle,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (roRang && checked) 1f else 0.35f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Độ sáng", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            CompactSlider(
                value = brightness,
                valueRange = 10..100,
                enabled = roRang && checked,
                thumbColor = SH.Amber,
                onValueChange = onBrightness,
                sliderHeight = 15.dp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$brightness%",
                style = SHType.Caption,
                color = SH.Amber,
                textAlign = TextAlign.End,
                maxLines = 1,
                modifier = Modifier.width(32.dp),
            )
        }
    }
}

@Composable
private fun HallMiniSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    activeColor: Color = SH.Green,
    onClick: () -> Unit,
) {
    val bg = if (checked) activeColor else Color(0xFF26334D)
    val borderColor = if (checked) activeColor else Color(0x14FFFFFF)
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .size(width = 36.dp, height = 20.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(2.dp),
    ) {
        Box(
            modifier = Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .size(16.dp)
                .clip(CircleShape)
                .background(if (checked) Color.White else Color(0xFFCBD5E1)),
        )
    }
}

private fun resolveSwitchState(o: OThietBi?, fallback: Boolean): Triple<Boolean, Boolean, Boolean> {
    val that = o as? OThietBi.That
    val checked = that?.let { it.mong ?: it.bat ?: false } ?: fallback
    val clear = that == null || that.dangCho || (that.bridgeSong && that.online && that.bat != null)
    val enabled = that == null || that.gatDuoc
    return Triple(checked, clear, enabled)
}

@Composable
private fun LightBrightnessCard(
    label: String,
    brightness: Int,
    denThat: OThietBi.That?,
    lightChecked: Boolean,
    lightRo: Boolean,
    lightEnabled: Boolean,
    onToggleLight: () -> Unit,
    onLightBrightness: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier, padding = 8.dp) {
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
                    Text(label, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Text(
                        denThat?.ghiChu
                            ?: if (!lightRo) "—"
                            else if (lightChecked) "Đang bật • Độ sáng $brightness%"
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
                    value = brightness,
                    valueRange = 10..100,
                    enabled = lightRo && lightChecked,
                    thumbColor = SH.Amber,
                    onValueChange = onLightBrightness,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "$brightness%",
                    style = SHType.Label,
                    color = SH.Amber,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(34.dp),
                )
            }
        }
    }
}

@Composable
private fun CompactSwitch(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: () -> Unit,
    checkedColor: Color = SH.Green,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = { onCheckedChange() },
            modifier = Modifier.scale(0.82f),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = checkedColor,
                checkedBorderColor = checkedColor,
                uncheckedThumbColor = SH.TextDim,
                uncheckedTrackColor = SH.Card2,
                uncheckedBorderColor = SH.Border,
            ),
        )
    }
}

@Composable
private fun CompactStepButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(SH.Card2)
            .border(1.dp, SH.Border, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
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
    sliderHeight: Dp = 22.dp,
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
            .height(sliderHeight)
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
