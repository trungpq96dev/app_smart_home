package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.DemoData
import vn.smarthome.data.StatKind
import vn.smarthome.data.StatRange
import vn.smarthome.ui.components.ArcGauge
import vn.smarthome.ui.components.BarChart
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState

/**
 * Man thong ke va bao cao.
 *
 * Bieu do ve bang Canvas (xem ui/components/Charts.kt), khong dung thu vien
 * chart nao - gon va du dung o kich thuoc nay.
 */
@Composable
fun StatsScreen(
    state: HomeState,
    onBack: () -> Unit,
    onKind: (StatKind) -> Unit,
    onRange: (StatRange) -> Unit,
) {
    val s = DemoData.series(state.statKind, state.statRange)
    var menuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Thống kê & Báo cáo", onBack = onBack) {
            if (DemoData.IS_DEMO) DemoBadge()
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SH.Card2)
                        .clickable { menuOpen = true }
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.statRange.label, style = SHType.Button, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ExpandMore, null,
                        tint = SH.TextDim, modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    StatRange.entries.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.label, style = SHType.Body, color = SH.Text) },
                            onClick = { onRange(r); menuOpen = false },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------- Tab doc ben trai ----------
            Column(
                Modifier.width(136.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StatKind.entries.forEach { k ->
                    KindTab(
                        kind = k,
                        icon = kindIcon(k),
                        selected = k == state.statKind,
                        onClick = { onKind(k) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }

            // ---------- Noi dung ----------
            Column(Modifier.weight(1f).fillMaxHeight()) {

                SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 12.dp) {
                    Text(s.kind.title, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(s.total, style = SHType.ValueBig, color = SH.Text, maxLines = 1)
                        Text(
                            s.unit,
                            style = SHType.CardTitle,
                            color = SH.TextDim,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                        DeltaChip(s.deltaPercent, Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    BarChart(
                        values = s.bars,
                        labels = s.labels,
                        color = s.kind.color,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }

                Spacer(Modifier.height(Dim.CardGap))

                // ---------- Ba the duoi ----------
                Row(
                    Modifier.fillMaxWidth().height(116.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
                ) {
                    SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                        Text(s.gaugeLabel, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                        ArcGauge(
                            fraction = s.gaugeFraction,
                            value = s.gaugeValue,
                            color = s.kind.color,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }

                    SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                        Text(s.costLabel, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                        Spacer(Modifier.weight(1f))
                        IconBadge(Icons.Filled.Payments, SH.Amber, size = 30.dp, iconSize = 17.dp)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            s.costValue,
                            style = SHType.CardTitle,
                            color = SH.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.weight(1f))
                    }

                    SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                        Text("Trạng thái", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                        Spacer(Modifier.weight(1f))
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            IconBadge(
                                Icons.Filled.CheckCircle,
                                if (s.statusOk) SH.Green else SH.Red,
                                size = 34.dp, iconSize = 19.dp,
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                s.statusLabel,
                                style = SHType.BodyMed,
                                color = if (s.statusOk) SH.Green else SH.Red,
                                maxLines = 1,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun KindTab(
    kind: StatKind,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SH.Blue else SH.Card)
            .border(
                Dim.BorderWidth,
                if (selected) SH.Blue else SH.Border,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, null,
            tint = if (selected) Color.White else kind.color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            kind.label,
            style = SHType.BodyMed,
            color = if (selected) Color.White else SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Chip phan tram tang/giam so voi ky truoc. */
@Composable
private fun DeltaChip(percent: Int, modifier: Modifier = Modifier) {
    val up = percent >= 0
    val color = if (up) SH.Red else SH.Green
    Row(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.tint(0.15f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            null, tint = color, modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            (if (up) percent else -percent).toString() + "%",
            style = SHType.Label, color = color, maxLines = 1,
        )
    }
}

private fun kindIcon(k: StatKind): ImageVector = when (k) {
    StatKind.POWER -> Icons.Filled.Bolt
    StatKind.TEMP -> Icons.Filled.Thermostat
    StatKind.HUMID -> Icons.Filled.WaterDrop
    StatKind.WATER -> Icons.Filled.Water
    StatKind.GAS -> Icons.Filled.LocalFireDepartment
}

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
@Composable
private fun StatsPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize()) {
            StatsScreen(HomeState(), {}, {}, {})
        }
    }
}
