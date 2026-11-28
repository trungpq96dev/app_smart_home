package vn.smarthome.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.server.DockerAction
import vn.smarthome.data.server.DockerControlStatus
import vn.smarthome.data.server.ServerContainer
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
 * Man xem & quan tri may chu serverubuntu tren may tinh bang.
 *
 * - So lieu he thong & CPU%/RAM tung container doc qua MQTT (`server/#`).
 * - Dieu khien Start/Stop/Restart di qua HTTP Gateway (`:3080`) + PIN 4 so Admin,
 *   KHONG bao gio ghi MQTT va chan cung 3 container loi (mosquitto, server-collector,
 *   smarthome-web).
 */
@Composable
fun ServerScreen(
    server: ServerState = ServerState(),
    onBack: () -> Unit,
    dockerControlStatus: DockerControlStatus = DockerControlStatus(),
    dockerBusyName: String? = null,
    dockerAdminUnlocked: Boolean = false,
    onRefreshDocker: () -> Unit = {},
    onToggleDockerLock: () -> Unit = {},
    onControlContainer: (ServerContainer, DockerAction) -> Unit = { _, _ -> },
) {
    val info = server.info
    val m = server.metric
    var sortByLoad by remember { mutableStateOf(false) }

    val runningCount = server.containers.count { it.state.equals("running", ignoreCase = true) }
    val totalContainerRamMb = remember(server.containers) {
        server.containers
            .filter { it.state.equals("running", ignoreCase = true) }
            .mapNotNull { it.memMb }
            .sum()
    }
    val hostRamMb = if (info.ramTotalGb > 0f) info.ramTotalGb * 1024f else 16384f

    val displayedContainers = remember(server.containers, sortByLoad) {
        if (!sortByLoad) {
            server.containers
        } else {
            server.containers.sortedWith(
                compareByDescending<ServerContainer> { it.state.equals("running", ignoreCase = true) }
                    .thenByDescending { (it.cpuPercent ?: 0f) * 10f + (it.memMb ?: 0f) / 10f }
                    .thenBy { it.name },
            )
        }
    }

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

            // Card: Docker containers (CPU%, RAM MB + Start/Stop/Restart co khoa PIN Admin)
            SmartCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Docker containers", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                        val ramSuffix = if (totalContainerRamMb > 0f) " · ${fmtMemMb(totalContainerRamMb)} RAM" else ""
                        Text(
                            "$runningCount/${server.containers.size} đang chạy$ramSuffix",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                        )
                    }

                    // Nut sap xep Nang nhat / Theo ten
                    SmallActionPill(
                        label = if (sortByLoad) "Nặng nhất" else "Theo tên",
                        icon = Icons.Filled.SwapVert,
                        color = if (sortByLoad) SH.Blue else SH.TextDim,
                        active = sortByLoad,
                        onClick = { sortByLoad = !sortByLoad },
                    )

                    if (dockerControlStatus.enabled) {
                        SmallActionPill(
                            label = if (dockerAdminUnlocked) "Đã mở PIN" else "Khóa PIN",
                            icon = if (dockerAdminUnlocked) Icons.Filled.LockOpen else Icons.Filled.Lock,
                            color = if (dockerAdminUnlocked) SH.Green else SH.Amber,
                            active = true,
                            onClick = onToggleDockerLock,
                        )
                        SmallActionPill(
                            label = "Làm mới",
                            icon = Icons.Filled.Refresh,
                            color = SH.Teal,
                            active = false,
                            onClick = onRefreshDocker,
                        )
                    }
                }

                if (dockerControlStatus.checked && !dockerControlStatus.enabled) {
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Bg)
                            .border(1.dp, SH.Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = dockerControlStatus.error
                                ?: "Chế độ chỉ xem · Chưa bật DOCKER_CONTROL_TOKEN trên Gateway (:3080).",
                            style = SHType.Caption,
                            color = SH.TextDim,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (displayedContainers.isEmpty()) {
                    Text(
                        "Chưa có container nào (hoặc chưa đọc được Docker).",
                        style = SHType.Body,
                        color = SH.TextOff,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        displayedContainers.forEach { c ->
                            val up = c.state.equals("running", ignoreCase = true)
                            val busy = dockerBusyName == c.name
                            val canControl = !c.isProtected &&
                                dockerControlStatus.enabled &&
                                (c.controllable || c.name.lowercase() in dockerControlStatus.allowedContainers)
                            val showLimit = c.memLimitMb != null && c.memLimitMb > 0f && c.memLimitMb < hostRamMb * 0.9f

                            ContainerRow(
                                c = c,
                                up = up,
                                busy = busy,
                                anyBusy = dockerBusyName != null,
                                canControl = canControl,
                                controlEnabled = dockerControlStatus.enabled,
                                showLimit = showLimit,
                                onAction = { act -> onControlContainer(c, act) },
                            )
                        }
                    }
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
private fun ContainerRow(
    c: ServerContainer,
    up: Boolean,
    busy: Boolean,
    anyBusy: Boolean,
    canControl: Boolean,
    controlEnabled: Boolean,
    showLimit: Boolean,
    onAction: (DockerAction) -> Unit,
) {
    val cpuColor = when {
        c.cpuPercent == null -> SH.TextDim
        c.cpuPercent >= 70f -> SH.Red
        c.cpuPercent >= 30f -> SH.Amber
        else -> SH.Green
    }
    val ramColor = when {
        c.memMb == null -> SH.TextDim
        c.memMb >= 512f -> SH.Red
        c.memMb >= 200f -> SH.Amber
        else -> SH.Purple
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Bg)
            .border(1.dp, SH.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(c.icon, if (up) SH.Green else SH.Red, size = 30.dp, iconSize = 16.dp)

        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    c.name,
                    style = SHType.BodyMed,
                    color = SH.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (c.isProtected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(SH.Amber.tint(0.16f))
                            .border(1.dp, SH.Amber.tint(0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = SH.Amber, modifier = Modifier.size(11.dp))
                        Text("Lõi", style = SHType.Caption, color = SH.Amber, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            val meta = if (c.status.isNotBlank()) "${c.image} · ${c.status}" else c.image
            Text(
                meta,
                style = SHType.Caption,
                color = SH.TextOff,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Chip CPU% & RAM MB
        if (up) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                MetricChip(
                    text = c.cpuPercent?.let { "CPU ${fmt(it)}%" } ?: "CPU —",
                    color = cpuColor,
                )
                val ramText = when {
                    c.memMb == null -> "RAM —"
                    showLimit && c.memLimitMb != null -> "${fmtMemMb(c.memMb)} / ${fmtMemMb(c.memLimitMb)}"
                    else -> fmtMemMb(c.memMb)
                }
                MetricChip(text = ramText, color = ramColor)
            }
        } else {
            Text("0% · 0 MB", style = SHType.Caption, color = SH.TextOff, maxLines = 1)
        }

        // Cham trang thai
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(46.dp)) {
            Dot(if (up) SH.Green else SH.Red, 7.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                if (up) "chạy" else "dừng",
                style = SHType.Caption,
                color = if (up) SH.Green else SH.TextOff,
                maxLines = 1,
            )
        }

        // Nut dieu khien Start / Stop / Restart (hoac khoa Bao ve)
        if (canControl) {
            if (busy) {
                Box(Modifier.width(124.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = SH.Blue,
                    )
                }
            } else if (up) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    ContainerActionButton(
                        label = "Dừng",
                        icon = Icons.Filled.Stop,
                        color = SH.Red,
                        enabled = !anyBusy,
                        onClick = { onAction(DockerAction.STOP) },
                    )
                    ContainerActionButton(
                        label = "Khởi động lại",
                        icon = Icons.Filled.Refresh,
                        color = SH.Blue,
                        enabled = !anyBusy,
                        compact = true,
                        onClick = { onAction(DockerAction.RESTART) },
                    )
                }
            } else {
                ContainerActionButton(
                    label = "Bật",
                    icon = Icons.Filled.PlayArrow,
                    color = SH.Green,
                    enabled = !anyBusy,
                    onClick = { onAction(DockerAction.START) },
                )
            }
        } else if (c.isProtected) {
            Text(
                "Bảo vệ",
                style = SHType.Caption,
                color = SH.TextOff,
                maxLines = 1,
            )
        } else if (controlEnabled) {
            Text(
                "Chỉ xem",
                style = SHType.Caption,
                color = SH.TextOff,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetricChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.tint(0.12f))
            .border(1.dp, color.tint(0.32f), RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = SHType.Caption, color = color, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun SmallActionPill(
    label: String,
    icon: ImageVector,
    color: Color,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (active) color.tint(0.16f) else SH.Bg,
        border = BorderStroke(1.dp, if (active) color.tint(0.5f) else SH.Border),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Text(label, style = SHType.Caption, color = if (active) color else SH.TextDim, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ContainerActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.tint(if (enabled) 0.16f else 0.06f),
        border = BorderStroke(1.dp, color.tint(if (enabled) 0.45f else 0.18f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 7.dp else 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) color else SH.TextOff, modifier = Modifier.size(14.dp))
            if (!compact) {
                Text(
                    label,
                    style = SHType.Caption,
                    color = if (enabled) color else SH.TextOff,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
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

private fun fmtMemMb(mb: Float): String =
    if (mb >= 1024f) "%.2f GB".format(mb / 1024f) else "${Math.round(mb)} MB"

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
