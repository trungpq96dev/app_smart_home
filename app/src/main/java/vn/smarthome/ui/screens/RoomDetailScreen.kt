package vn.smarthome.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.DemoData
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.ui.components.ChoiceChip
import vn.smarthome.ui.components.DeviceSwitchRow
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.RoundIconButton
import vn.smarthome.data.congtac.OThietBi
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.TempStepperRow
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme

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
) {
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
            ImagePlaceholder(
                icon = room.icon,
                label = room.name,
                accent = room.accent,
                cornerRadius = Dim.CardRadius,
                iconSize = 44.dp,
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
            )

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
                )
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

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
@Composable
private fun RoomDetailPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize()) {
            RoomDetailScreen(DemoData.rooms()[1], {}, {}, {}, {}, {}, {}, {})
        }
    }
}
