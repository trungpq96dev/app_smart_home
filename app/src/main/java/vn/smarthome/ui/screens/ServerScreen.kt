package vn.smarthome.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.server.ServerState
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Man xem thong tin may chu serverubuntu.
 *
 * CHI XEM (read-only): so that lay tu MQTT (server/# do script server_collector
 * tren serverubuntu publish). Khong co nut dieu khien o pha nay.
 *
 * Truong nao may chu khong cap (vi du nhiet do CPU may khong co cam bien) hien
 * "—", khong bia so.
 */
@Composable
fun ServerScreen(
    server: ServerState = ServerState(),
    onBack: () -> Unit,
) {
    val info = server.info
    val m = server.metric

    Column(Modifier.fillMaxSize()) {

        // ---- Header ----
        Row(
            Modifier.fillMaxWidth().height(Dim.HeaderHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(Icons.Filled.Dns, SH.Blue, size = 34.dp, iconSize = 19.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Máy chủ serverubuntu",
                        style = SHType.PageTitle,
                        color = SH.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    server.capNhat?.let {
                        Text(
                            "Cập nhật: $it",
                            style = SHType.Caption,
                            color = SH.TextOff,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!server.isRealData) {
                    DemoBadge()
                }
                StatusPill(info.hostname, SH.Green)
                Text(
                    info.os,
                    style = SHType.Caption,
                    color = SH.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ---- Noi dung cuon doc ----
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {

            // Hang stat tile: CPU / RAM / O dia / Nhiet do
            Row(
                Modifier.fillMaxWidth().height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                StatTile(Modifier.weight(1f), Icons.Filled.Speed, SH.Blue,
                    pct(m.cpuPercent), "CPU", "%")
                StatTile(Modifier.weight(1f), Icons.Filled.Memory, SH.Purple,
                    pct(m.ramPercent), "RAM", "%")
                StatTile(Modifier.weight(1f), Icons.Filled.Storage, SH.Teal,
                    pct(m.diskPercent), "Ổ đĩa", "%")
                StatTile(Modifier.weight(1f), Icons.Filled.Thermostat,
                    if ((m.cpuTempC ?: 0f) >= 75f) SH.Red else SH.Amber,
                    m.cpuTempC?.let { pct(it) } ?: "—",
                    "Nhiệt độ", if (m.cpuTempC != null) "°C" else "")
            }

            // Card: Tai nguyen (thanh tien trinh)
            SmartCard(Modifier.fillMaxWidth()) {
                Text("Tài nguyên", style = SHType.CardTitle, color = SH.Text)
                Spacer(Modifier.height(8.dp))
                MeterRow("CPU", m.cpuPercent, SH.Blue,
                    "${fmt(m.cpuPercent)}%")
                MeterRow("RAM", m.ramPercent, SH.Purple,
                    "${fmt(m.ramUsedGb)} / ${fmt(info.ramTotalGb)} GB")
                MeterRow("Ổ đĩa", m.diskPercent, SH.Teal,
                    "${fmt(m.diskUsedGb)} / ${fmt(info.diskTotalGb)} GB")
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.NetworkCheck, null, tint = SH.TextDim, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Mạng", style = SHType.Body, color = SH.TextDim, modifier = Modifier.width(70.dp))
                    Text("↓ ${fmt(m.netRxMbps)} Mb/s", style = SHType.Body, color = SH.Blue, maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    Text("↑ ${fmt(m.netTxMbps)} Mb/s", style = SHType.Body, color = SH.Green, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Text("Load: ${fmt(m.load1)} / ${fmt(m.load5)} / ${fmt(m.load15)}",
                        style = SHType.Caption, color = SH.TextOff, maxLines = 1)
                }
            }

            // Card: Thong tin he thong
            SmartCard(Modifier.fillMaxWidth()) {
                Text("Thông tin hệ thống", style = SHType.CardTitle, color = SH.Text)
                Spacer(Modifier.height(6.dp))
                InfoRow("Tên máy", info.hostname)
                InfoRow("Hệ điều hành", info.os)
                InfoRow("Kernel", info.kernel)
                InfoRow("CPU", "${info.cpuModel} (${info.cpuCores} nhân)")
                InfoRow("Thời gian hoạt động", "${info.uptimeDays} ngày ${info.uptimeHours} giờ")
            }

            // Card: Docker containers
            SmartCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Docker containers", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    val running = server.containers.count { it.state == "running" }
                    Text("$running/${server.containers.size} đang chạy",
                        style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))
                if (server.containers.isEmpty()) {
                    Text("Chưa có container nào (hoặc chưa đọc được Docker).",
                        style = SHType.Body, color = SH.TextOff)
                } else {
                    server.containers.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(c.icon, SH.Blue, size = 28.dp, iconSize = 16.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.name, style = SHType.BodyMed, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(c.image, style = SHType.Caption, color = SH.TextOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            val up = c.state == "running"
                            if (c.status.isNotBlank()) {
                                Text(c.status, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                                Spacer(Modifier.width(8.dp))
                            }
                            Dot(if (up) SH.Green else SH.TextOff, 8.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(if (up) "chạy" else "dừng",
                                style = SHType.Caption, color = if (up) SH.Green else SH.TextOff, maxLines = 1)
                        }
                    }
                }
            }

            // Card: Nhiet do (nhieu cam bien)
            if (server.temps.isNotEmpty()) {
                SmartCard(Modifier.fillMaxWidth()) {
                    Text("Nhiệt độ", style = SHType.CardTitle, color = SH.Text)
                    Spacer(Modifier.height(8.dp))
                    server.temps.chunked(4).forEach { hang ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
                        ) {
                            hang.forEach { t ->
                                TempCell(Modifier.weight(1f), t.label, t.tempC)
                            }
                            // Lap o trong cho du 4 cot de khong bi gian.
                            repeat(4 - hang.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (server.tempMinToday != null && server.tempMaxToday != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Trong ngày hôm nay: thấp nhất ${pct(server.tempMinToday)}°C, " +
                                "cao nhất ${pct(server.tempMaxToday)}°C.",
                            style = SHType.Caption, color = SH.TextDim,
                        )
                    }
                }
            }

            // Card: Dien nang (pin + RAPL CPU)
            SmartCard(Modifier.fillMaxWidth()) {
                Text("Điện năng", style = SHType.CardTitle, color = SH.Text)
                Spacer(Modifier.height(6.dp))
                val p = server.power
                KvRow("Điện áp pin", p.batVoltageV?.let { "${fmt(it)} V" } ?: "—")
                KvRow("Dòng điện", p.batCurrentA?.let { "${fmt3(it)} A" } ?: "—")
                KvRow("Công suất pin", p.batPowerW?.let { "${fmt(it)} W" } ?: "—")
                KvRow("CPU, cả gói", p.cpuPackageW?.let { "${fmt(it)} W" } ?: "—")
                KvRow("— phần nhân tính toán", p.cpuCoreW?.let { "${fmt(it)} W" } ?: "—", dim = true)
                KvRow("— phần phụ trợ", p.cpuUncoreW?.let { "${fmt(it)} W" } ?: "—", dim = true)
            }

            // Card: Dien nang CPU theo ngay
            SmartCard(Modifier.fillMaxWidth()) {
                Text("Điện năng CPU theo ngày", style = SHType.CardTitle, color = SH.Text)
                Spacer(Modifier.height(6.dp))
                if (server.days.isEmpty()) {
                    Text("Đang tích lũy số liệu, quay lại sau.",
                        style = SHType.Body, color = SH.TextOff)
                } else {
                    val maxWh = server.days.maxOf { it.wh }.coerceAtLeast(0.1f)
                    server.days.forEach { d ->
                        DayRow(d.date, d.wh, maxWh, d.tempMin, d.tempMax)
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun TempCell(modifier: Modifier, label: String, tempC: Float) {
    Column(modifier) {
        Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(pct(tempC), style = SHType.Value, color = SH.Green, maxLines = 1)
            Text("°C", style = SHType.Label, color = SH.TextDim,
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(SH.Green.tint(0.15f))) {
            val frac = (tempC / 100f).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(SH.Green))
        }
    }
}

@Composable
private fun KvRow(label: String, value: String, dim: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = SHType.Body, color = if (dim) SH.TextOff else SH.TextDim,
            maxLines = 1, modifier = Modifier.weight(1f))
        Text(value, style = SHType.BodyMed, color = SH.Text, maxLines = 1)
    }
}

@Composable
private fun DayRow(date: String, wh: Float, maxWh: Float, tempMin: Float?, tempMax: Float?) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(date, style = SHType.Body, color = SH.Text, maxLines = 1, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(SH.Green.tint(0.15f))) {
            val frac = (wh / maxWh).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(SH.Green))
        }
        Spacer(Modifier.width(8.dp))
        val khoang = if (tempMin != null && tempMax != null) " · ${pct(tempMin)}–${pct(tempMax)} °C" else ""
        Text("${fmt(wh)} Wh$khoang", style = SHType.Caption, color = SH.TextDim,
            maxLines = 1, modifier = Modifier.width(150.dp))
    }
}

// =====================================================================
//  Phu tro
// =====================================================================

@Composable
private fun StatTile(
    modifier: Modifier,
    icon: ImageVector,
    color: Color,
    value: String,
    label: String,
    unit: String,
) {
    SmartCard(modifier.fillMaxHeight(), padding = 8.dp) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            IconBadge(icon, color, size = 26.dp, iconSize = 15.dp)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = SHType.Value, color = SH.Text, maxLines = 1)
                if (unit.isNotBlank()) {
                    Text(unit, style = SHType.Label, color = SH.TextDim,
                        modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
            }
            Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
        }
    }
}

@Composable
private fun MeterRow(
    label: String,
    percent: Float,
    color: Color,
    trailing: String,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = SHType.Body, color = SH.TextDim, modifier = Modifier.width(70.dp), maxLines = 1)
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color.tint(0.15f)),
        ) {
            val frac = (percent / 100f).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Text(trailing, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.width(110.dp))
    }
}

@Composable
private fun InfoRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(key, style = SHType.Body, color = SH.TextDim, modifier = Modifier.width(150.dp))
        Text(value, style = SHType.Body, color = SH.Text, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

/** Lam tron 1 chu so thap phan, bo ".0" cho gon. */
private fun fmt(v: Float): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}

/** So nguyen (cho stat tile phan tram / nhiet do). */
private fun pct(v: Float): String = Math.round(v).toString()

/** Lam tron 3 chu so thap phan (dong dien pin, vi du 0.000 A). */
private fun fmt3(v: Float): String {
    val r = Math.round(v * 1000) / 1000.0
    return if (r == Math.floor(r)) "%.3f".format(r) else r.toString()
}
