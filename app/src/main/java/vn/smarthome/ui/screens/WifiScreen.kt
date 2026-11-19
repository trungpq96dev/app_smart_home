package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShieldMoon
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.smarthome.data.wifi.ChannelNoise
import vn.smarthome.data.wifi.ConnectionLog
import vn.smarthome.data.wifi.SecuritySetting
import vn.smarthome.data.wifi.StatsDataset
import vn.smarthome.data.wifi.UnknownDevice
import vn.smarthome.data.wifi.WifiDevice
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.ui.components.BarChart
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.components.ThinBar
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Man quan ly WiFi chi tiet voi 5 tab:
 *   0 - Tong quan
 *   1 - Thiet bi ket noi
 *   2 - Thong ke su dung
 *   3 - Bao mat
 *   4 - Quan ly nang cao
 */
@Composable
fun WifiScreen(
    wifi: WifiState = WifiState(),
    onBack: () -> Unit,
    onReboot: () -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var daBamReboot by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        // ---- Top bar ----
        TopBar(title = "", onBack = onBack) {
            if (!wifi.isRealData) {
                DemoBadge()
                Spacer(Modifier.width(6.dp))
            }
            val online = wifi.info.isConnected
            StatusPill(if (online) "Đã kết nối" else "Mất kết nối", if (online) SH.Green else SH.Red)
            Spacer(Modifier.width(6.dp))
            Text(
                "SSID: ${wifi.info.ssid}  |  Bảo mật: ${wifi.info.security}",
                style = SHType.Caption, color = SH.TextDim, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Tieu de phu ngay duoi TopBar
        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Filled.Wifi, SH.Purple, size = 34.dp, iconSize = 19.dp)
            Spacer(Modifier.width(8.dp))
            Text("WiFi gia đình", style = SHType.PageTitle, color = SH.Text, maxLines = 1)
            if (daBamReboot) {
                Spacer(Modifier.width(10.dp))
                StatusPill("Đã gửi lệnh khởi động lại modem...", SH.Amber)
            }
        }

        // ---- Body: tab trai + noi dung phai ----
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // Tab trai
            Column(
                Modifier.width(136.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                val tabs = listOf(
                    "Tổng quan" to Icons.Filled.Assessment,
                    "Thiết bị kết nối" to Icons.Filled.Devices,
                    "Thống kê sử dụng" to Icons.Filled.BarChart,
                    "Bảo mật" to Icons.Filled.Security,
                    "Quản lý nâng cao" to Icons.Filled.Settings,
                )
                tabs.forEachIndexed { i, (label, icon) ->
                    WifiTab(
                        label = label,
                        icon = icon,
                        selected = i == selectedTab,
                        onClick = { selectedTab = i },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        badge = if (i == 3 && wifi.unknownDevices.isNotEmpty()) wifi.unknownDevices.size else null,
                    )
                }
            }

            // Noi dung phai
            Column(Modifier.weight(1f).fillMaxHeight()) {
                when (selectedTab) {
                    0 -> TongQuanContent(
                        wifi = wifi,
                        onRebootClick = {
                            daBamReboot = true
                            onReboot()
                        },
                    )
                    1 -> ThietBiKetNoiContent(wifi = wifi)
                    2 -> ThongKeSuDungContent(wifi = wifi)
                    3 -> BaoMatContent(wifi = wifi)
                    4 -> QuanLyNangCaoContent(wifi = wifi, onRebootClick = {
                        daBamReboot = true
                        onReboot()
                    })
                }
            }
        }
    }
}

// =====================================================================
//  TAB 0: TONG QUAN
// =====================================================================

@Composable
private fun ColumnScope.TongQuanContent(
    wifi: WifiState,
    onRebootClick: () -> Unit,
) {
    // ---- Hang 1: 4 stat tile ----
    Row(
        Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.Wifi, SH.Purple, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(wifi.info.signalDbm.toString(), style = SHType.Value, color = SH.Text, maxLines = 1)
                    Text("dBm", style = SHType.Label, color = SH.TextDim, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
                Text("Chất lượng tín hiệu", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                StatusPill(wifi.info.signalLabel, SH.Green)
            }
        }
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.ArrowDownward, SH.Blue, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(wifi.info.downloadMbps.toString(), style = SHType.Value, color = SH.Text, maxLines = 1)
                    Text("Mb/s", style = SHType.Label, color = SH.TextDim, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
                Text("Tốc độ tải xuống", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
        }
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.ArrowUpward, SH.Green, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(wifi.info.uploadMbps.toString(), style = SHType.Value, color = SH.Text, maxLines = 1)
                    Text("Mb/s", style = SHType.Label, color = SH.TextDim, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
                }
                Text("Tốc độ tải lên", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
        }
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(Icons.Filled.People, SH.Purple, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.height(3.dp))
                Text(wifi.devices.size.toString(), style = SHType.Value, color = SH.Text, maxLines = 1)
                Text("Thiết bị kết nối", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
        }
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- Hang 2: Bieu do su dung mang ----
    SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BarChart, null, tint = SH.TextDim, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text("Biểu đồ sử dụng mạng (24h)", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(SH.Blue, 6.dp)
                Spacer(Modifier.width(3.dp))
                Text("Tải xuống", style = SHType.Caption, color = SH.TextDim)
                Spacer(Modifier.width(8.dp))
                Dot(SH.Green, 6.dp)
                Spacer(Modifier.width(3.dp))
                Text("Tải lên", style = SHType.Caption, color = SH.TextDim)
            }
        }
        Spacer(Modifier.height(6.dp))
        val chartData = if (wifi.usageChart24h.isNotEmpty()) wifi.usageChart24h else List(24) { 1f }
        BarChart(
            values = chartData,
            labels = List(chartData.size) { i -> if (i % 4 == 0) "%02d:00".format(i) else "" },
            color = SH.Blue,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- Hang 3: 3 cot duoi ----
    Row(
        Modifier.fillMaxWidth().height(145.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // Cot 1: Danh sach thiet bi
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách thiết bị kết nối (${wifi.devices.size})", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text("Xem tất cả →", style = SHType.Label, color = SH.Blue, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            val hienThi = wifi.devices.take(5)
            hienThi.forEach { dev ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(dev.icon, null, tint = SH.TextDim, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Column(Modifier.weight(1f)) {
                        Text(dev.name, style = SHType.Caption, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(dev.ip, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
                    }
                    StatusPill(if (dev.isOnline) "Online" else "Offline", if (dev.isOnline) SH.Green else SH.TextOff)
                }
            }
        }

        // Cot 2: Thong tin mang + Trang thai Internet
        Column(
            Modifier.weight(0.7f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 8.dp) {
                Text("Thông tin mạng", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                val info = listOf(
                    "SSID:" to wifi.info.ssid,
                    "Bảo mật:" to wifi.info.security,
                    "IP Router:" to wifi.info.ipRouter,
                    "DNS:" to wifi.info.dns,
                    "Gateway:" to wifi.info.gateway,
                )
                info.forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(k, style = SHType.Caption, color = SH.TextDim, modifier = Modifier.width(55.dp))
                        Text(v, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Filled.ContentCopy, null, tint = SH.TextOff, modifier = Modifier.size(11.dp))
                    }
                }
            }
            SmartCard(Modifier.fillMaxWidth().weight(0.6f), padding = 8.dp) {
                Text("Trạng thái Internet", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    val online = wifi.info.isConnected
                    Icon(Icons.Filled.CheckCircle, null, tint = if (online) SH.Green else SH.Red, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Column {
                        Text(if (online) "Kết nối ổn định" else "Mất kết nối", style = SHType.BodyMed, color = if (online) SH.Green else SH.Red, maxLines = 1)
                        Text("Thời gian hoạt động: ${wifi.info.uptimeDays} ngày ${wifi.info.uptimeHours} giờ", style = SHType.Caption, color = SH.TextOff, maxLines = 1)
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }

        // Cot 3: Cong cu nhanh + Canh bao
        Column(
            Modifier.weight(0.7f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 8.dp) {
                Text("Công cụ nhanh", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    WifiToolButton(Icons.Filled.Pause, "Tạm dừng\nWiFi", SH.Amber) {}
                    WifiToolButton(Icons.Filled.Refresh, "Khởi động\nlại", SH.Blue, onClick = onRebootClick)
                    WifiToolButton(Icons.Filled.Shield, "Kiểm tra\nbảo mật", SH.Green) {}
                }
                Spacer(Modifier.weight(1f))
            }
            SmartCard(Modifier.fillMaxWidth().weight(0.6f), padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Cảnh báo & Ghi chú", style = SHType.CardTitle, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("Xem thêm →", style = SHType.Label, color = SH.Blue, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ok = wifi.unknownDevices.isEmpty()
                    Icon(
                        if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        null,
                        tint = if (ok) SH.Green else SH.Amber,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (ok) "Không có cảnh báo nào" else "Có ${wifi.unknownDevices.size} thiết bị lạ cần kiểm tra",
                        style = SHType.Body,
                        color = if (ok) SH.TextDim else SH.Amber,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// =====================================================================
//  TAB 1: THIET BI KET NOI
// =====================================================================

@Composable
private fun ColumnScope.ThietBiKetNoiContent(wifi: WifiState) {
    var filter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }

    val allDevices = wifi.devices
    val onlineCount = allDevices.count { it.isOnline && !it.blocked }
    val offlineCount = allDevices.size - onlineCount
    val newCount = allDevices.count { it.isNew }

    // ---- Hang 1: 4 stat cards ----
    Row(
        Modifier.fillMaxWidth().height(68.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        MiniStatCard(Modifier.weight(1f), "Tổng thiết bị", allDevices.size.toString(), Icons.Filled.Devices, SH.Blue)
        MiniStatCard(Modifier.weight(1f), "Đang Online", onlineCount.toString(), Icons.Filled.Wifi, SH.Green)
        MiniStatCard(Modifier.weight(1f), "Offline", offlineCount.toString(), Icons.Filled.WifiOff, SH.TextDim)
        MiniStatCard(Modifier.weight(1f), "Thiết bị mới", newCount.toString(), Icons.Filled.Radar, SH.Teal)
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- Bang danh sach thiet bi ----
    SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 8.dp) {
        // Thanh cong cu
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Danh sách thiết bị kết nối", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
            StatusPill(allDevices.size.toString(), SH.Blue, showDot = false)
            Spacer(Modifier.weight(1f))

            // Filter buttons
            val filters = listOf("all" to "Tất cả", "online" to "Online", "offline" to "Offline", "new" to "Mới")
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Card2)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                filters.forEach { (key, label) ->
                    Text(
                        label,
                        style = SHType.Caption,
                        color = if (filter == key) Color.White else SH.TextDim,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (filter == key) SH.Blue else Color.Transparent)
                            .clickable { filter = key }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Header bang
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("THIẾT BỊ", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(2.2f))
            Text("ĐỊA CHỈ MAC", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(1.5f))
            Text("HÃNG", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(0.8f))
            Text("LOẠI", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(0.8f))
            Text("BĂNG TẦN", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(0.7f))
            Text("TRẠNG THÁI", style = SHType.Caption, color = SH.TextOff, modifier = Modifier.weight(1.0f))
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(SH.Border),
        )

        // Danh sach
        val filtered = allDevices.filter { d ->
            val matchFilter = when (filter) {
                "online" -> d.isOnline && !d.blocked
                "offline" -> !d.isOnline || d.blocked
                "new" -> d.isNew
                else -> true
            }
            matchFilter
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Không tìm thấy thiết bị phù hợp", style = SHType.Body, color = SH.TextDim)
                }
            } else {
                filtered.forEach { dev ->
                    DeviceRow(dev)
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(dev: WifiDevice) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ten + IP + icon
        Row(
            Modifier.weight(2.2f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Card2)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(dev.icon, null, tint = SH.Blue, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dev.name, style = SHType.Caption, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (dev.trusted) {
                        Spacer(Modifier.width(3.dp))
                        Icon(Icons.Filled.Verified, null, tint = SH.Green, modifier = Modifier.size(10.dp))
                    }
                    if (dev.isNew) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "MỚI",
                            style = SHType.Caption,
                            color = SH.Teal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SH.Teal.tint(0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(dev.ip, style = SHType.Caption, color = SH.TextOff, maxLines = 1, fontFamily = FontFamily.Monospace)
            }
        }

        // MAC
        Text(
            dev.mac.ifEmpty { "—" },
            style = SHType.Caption,
            color = SH.TextDim,
            maxLines = 1,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.5f),
        )

        // Hang
        Text(
            dev.brand.ifEmpty { "—" },
            style = SHType.Caption,
            color = SH.Text,
            maxLines = 1,
            modifier = Modifier.weight(0.8f),
        )

        // Loai
        Text(
            dev.type.ifEmpty { "—" },
            style = SHType.Caption,
            color = SH.TextDim,
            maxLines = 1,
            modifier = Modifier.weight(0.8f),
        )

        // Bang tan
        Box(Modifier.weight(0.7f)) {
            val is5g = dev.band == "5GHz"
            Text(
                dev.band,
                style = SHType.Caption,
                color = if (is5g) SH.Purple else SH.TextDim,
                maxLines = 1,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (is5g) SH.Purple.tint(0.12f) else SH.Card2)
                    .border(Dim.BorderWidth, if (is5g) SH.Purple.tint(0.3f) else SH.Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }

        // Trang thai
        Box(Modifier.weight(1.0f)) {
            when {
                dev.blocked -> StatusPill("Đã chặn", SH.Red)
                dev.isOnline -> StatusPill("Online", SH.Green)
                else -> StatusPill("Offline", SH.TextOff)
            }
        }
    }
}

// =====================================================================
//  TAB 2: THONG KE SU DUNG
// =====================================================================

@Composable
private fun ColumnScope.ThongKeSuDungContent(wifi: WifiState) {
    var range by remember { mutableStateOf("day") }

    val ds = when (range) {
        "week" -> wifi.statsWeek
        "month" -> wifi.statsMonth
        else -> wifi.statsDay
    }

    // ---- Header + range selector ----
    SmartCard(Modifier.fillMaxWidth(), padding = 8.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Thống kê lưu lượng sử dụng", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Text("Theo dõi băng thông tải xuống, tải lên theo từng thiết bị", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Card2)
                    .padding(2.dp),
            ) {
                listOf("day" to "Ngày", "week" to "Tuần", "month" to "Tháng").forEach { (key, label) ->
                    Text(
                        label,
                        style = SHType.Label,
                        color = if (range == key) Color.White else SH.TextDim,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (range == key) SH.Blue else Color.Transparent)
                            .clickable { range = key }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- 4 KPI cards ----
    Row(
        Modifier.fillMaxWidth().height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        KpiCard(Modifier.weight(1f), ds.downLabel, ds.downVal, ds.downSub, Icons.Filled.ArrowDownward, SH.Blue)
        KpiCard(Modifier.weight(1f), ds.upLabel, ds.upVal, ds.upSub, Icons.Filled.ArrowUpward, SH.Green)
        KpiCard(Modifier.weight(1f), ds.totalLabel, ds.totalVal, ds.totalSub, Icons.Filled.Assessment, SH.Purple)
        KpiCard(Modifier.weight(1f), "Tốc độ đỉnh", ds.peakVal, ds.peakSub, Icons.Filled.FlashOn, SH.Amber)
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- Bieu do ----
    SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BarChart, null, tint = SH.Blue, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(5.dp))
            Text(ds.title, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(SH.Blue, 6.dp); Spacer(Modifier.width(3.dp))
                Text("Tải xuống", style = SHType.Caption, color = SH.TextDim)
                Spacer(Modifier.width(8.dp))
                Dot(SH.Green, 6.dp); Spacer(Modifier.width(3.dp))
                Text("Tải lên", style = SHType.Caption, color = SH.TextDim)
            }
        }
        Spacer(Modifier.height(6.dp))
        BarChart(
            values = ds.down,
            labels = ds.labels,
            color = SH.Blue,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- Hang duoi: Top thiet bi + Phan bo ----
    Row(
        Modifier.fillMaxWidth().height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // Top thiet bi
        SmartCard(Modifier.weight(1.4f).fillMaxHeight(), padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Thống kê theo từng thiết bị", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text("Top tiêu thụ băng thông", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
            Spacer(Modifier.height(4.dp))
            val sorted = wifi.devices.sortedByDescending { it.downloadMB + it.uploadMB }.take(5)
            val maxTotal = sorted.firstOrNull()?.let { it.downloadMB + it.uploadMB } ?: 1f
            val barColors = listOf(SH.Blue, SH.Green, SH.Purple, SH.Amber, SH.Teal)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sorted.forEachIndexed { idx, dev ->
                    val total = dev.downloadMB + dev.uploadMB
                    val frac = (total / maxTotal).coerceIn(0.08f, 1f)
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(dev.icon, null, tint = SH.TextDim, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(dev.name, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                            Text("%.1f GB".format(total), style = SHType.Caption, color = SH.Text)
                            Text(" (↓%.1f / ↑%.1f)".format(dev.downloadMB, dev.uploadMB), style = SHType.Caption, color = SH.TextOff)
                        }
                        ThinBar(frac, barColors[idx % barColors.size], Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Phan bo luu luong theo nhom
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Text("Phân bố lưu lượng theo nhóm", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            val catLabels = listOf("Máy tính / Laptop", "Điện thoại & Tablet", "Smart TV", "Thiết bị IoT")
            val catColors = listOf(SH.Blue, SH.Green, SH.Purple, SH.Amber)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ds.categories.forEachIndexed { i, pct ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Card2)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(catColors[i], 7.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(catLabels[i], style = SHType.Caption, color = SH.TextDim, maxLines = 1, modifier = Modifier.weight(1f))
                        Text("$pct%", style = SHType.Label, color = SH.Text, maxLines = 1)
                    }
                }
            }
        }
    }
}

// =====================================================================
//  TAB 3: BAO MAT
// =====================================================================

@Composable
private fun ColumnScope.BaoMatContent(wifi: WifiState) {
    val hasUnknown = wifi.unknownDevices.isNotEmpty()

    // ---- Banner trang thai bao mat ----
    SmartCard(Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                if (hasUnknown) Icons.Filled.Warning else Icons.Filled.Shield,
                if (hasUnknown) SH.Amber else SH.Green,
                size = 38.dp,
                iconSize = 20.dp,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (hasUnknown) "Phát hiện ${wifi.unknownDevices.size} thiết bị lạ cần xử lý" else "Hệ thống an toàn",
                    style = SHType.CardTitle,
                    color = SH.Text,
                    maxLines = 1,
                )
                Text(
                    if (hasUnknown) "Vui lòng xác nhận Cho phép hoặc Chặn các thiết bị bên dưới" else "Tường lửa đang bật, không có mối đe dọa nghiêm trọng.",
                    style = SHType.Caption,
                    color = SH.TextDim,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Blue)
                    .clickable { }
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Shield, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Quét lỗ hổng bảo mật", style = SHType.Label, color = Color.White, maxLines = 1)
            }
        }
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- 2 cot: Thiet bi la & Lich su ket noi ----
    Row(
        Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // Cot 1: Thiet bi la & canh bao
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = SH.Amber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Thiết bị lạ & Cảnh báo", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                StatusPill(
                    "${wifi.unknownDevices.size} cảnh báo",
                    if (hasUnknown) SH.Red else SH.Green,
                )
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (wifi.unknownDevices.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.CheckCircle, null, tint = SH.Green, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Tất cả thiết bị lạ đã được xử lý an toàn.", style = SHType.Caption, color = SH.TextDim)
                        }
                    }
                } else {
                    wifi.unknownDevices.forEach { unk ->
                        SmartCard(
                            Modifier.fillMaxWidth(),
                            padding = 8.dp,
                            borderColor = SH.Amber.tint(0.3f),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Dot(SH.Amber, 6.dp)
                                Spacer(Modifier.width(5.dp))
                                Text(unk.name, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.Ellipsis)
                            }
                            Text("MAC: ${unk.mac} • IP: ${unk.ip}", style = SHType.Caption, color = SH.TextOff, maxLines = 1, fontFamily = FontFamily.Monospace)
                            Text("${unk.reason} (${unk.time})", style = SHType.Caption, color = SH.Amber)
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                MiniActionButton("Cho phép", SH.Green) {}
                                MiniActionButton("Chặn", SH.Red) {}
                            }
                        }
                    }
                }
            }
        }

        // Cot 2: Lich su ket noi
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.History, null, tint = SH.Blue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Lịch sử kết nối", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                Text("Xóa nhật ký", style = SHType.Caption, color = SH.TextOff, maxLines = 1,
                    modifier = Modifier.clickable { })
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (wifi.connectionLogs.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Chưa có nhật ký kết nối nào.", style = SHType.Caption, color = SH.TextDim)
                    }
                } else {
                    wifi.connectionLogs.forEach { log ->
                        ConnectionLogRow(log)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(Dim.CardGap))

    // ---- 2 cot: Tin cay & Cau hinh bao ve ----
    Row(
        Modifier.fillMaxWidth().weight(0.8f),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // Danh sach tin cay
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Danh sách thiết bị tin cậy", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                Spacer(Modifier.weight(1f))
                val trustedCount = wifi.devices.count { it.trusted }
                StatusPill("$trustedCount thiết bị", SH.Green)
            }
            Text("Các thiết bị luôn được phép truy cập", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                wifi.devices.filter { it.trusted }.forEach { dev ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Card2)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(dev.icon, null, tint = SH.Green, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(5.dp))
                        Column(Modifier.weight(1f)) {
                            Text(dev.name, style = SHType.Caption, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(dev.mac, style = SHType.Caption, color = SH.TextOff, maxLines = 1, fontFamily = FontFamily.Monospace)
                        }
                        Text("Gỡ bỏ", style = SHType.Caption, color = SH.TextOff,
                            modifier = Modifier.clickable { })
                    }
                }
            }
        }

        // Cau hinh bao ve (Toggle Switches)
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
            Text("Cấu hình bảo vệ", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
            Text("Bật/tắt các lớp bảo mật cho mạng WiFi", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            Spacer(Modifier.height(3.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                wifi.securitySettings.forEach { setting ->
                    SecurityToggleRow(setting)
                }
            }
        }
    }
}

@Composable
private fun ConnectionLogRow(log: ConnectionLog) {
    val logColor = when (log.type) {
        "success" -> SH.Green
        "warning" -> SH.Amber
        "danger" -> SH.Red
        else -> SH.TextDim
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Card2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(log.device, style = SHType.Caption, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(log.action, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
        }
        Text(
            "${log.time} • ${log.date}",
            style = SHType.Caption,
            color = logColor,
            maxLines = 1,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(logColor.tint(0.12f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SecurityToggleRow(setting: SecuritySetting) {
    var checked by remember(setting.key) { mutableStateOf(setting.enabled) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Card2)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(setting.title, style = SHType.Caption, color = SH.Text, maxLines = 1)
            Text(setting.desc, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
        }
        Switch(
            checked = checked,
            onCheckedChange = { checked = it },
            modifier = Modifier.height(24.dp),
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

// =====================================================================
//  TAB 4: QUAN LY NANG CAO
// =====================================================================

@Composable
private fun ColumnScope.QuanLyNangCaoContent(
    wifi: WifiState,
    onRebootClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // ---- Hang 1: Thong tin Router + Cau hinh LAN/DHCP ----
        Row(
            Modifier.fillMaxWidth().height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // The 1: Thong tin Router
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Router, SH.Blue, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Thông tin Router", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    StatusPill("Hoạt động tốt", SH.Green)
                }
                Spacer(Modifier.height(6.dp))

                val routerInfo = listOf(
                    "Tên model" to "Router AX3000",
                    "Phần cứng" to "HW-v3.2 (Dual-Core)",
                    "Firmware" to "v9.9.12-stable",
                    "Uptime" to "${wifi.info.uptimeDays} ngày, ${wifi.info.uptimeHours} giờ",
                )
                routerInfo.forEach { (k, v) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .padding(bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = SHType.Caption, color = SH.TextDim, modifier = Modifier.weight(0.4f))
                        Text(
                            v, style = SHType.Caption, color = SH.Text,
                            maxLines = 1, modifier = Modifier.weight(0.6f),
                            fontFamily = if (k == "Firmware") FontFamily.Monospace else FontFamily.SansSerif,
                        )
                    }
                    if (k != routerInfo.last().first) {
                        Box(Modifier.fillMaxWidth().height(0.5.dp).background(SH.Border.tint(0.5f)))
                    }
                }

                Spacer(Modifier.weight(1f))

                // CPU + RAM bars
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Tải CPU", style = SHType.Caption, color = SH.TextDim)
                            Text("28%", style = SHType.Caption, color = SH.Blue)
                        }
                        ThinBar(0.28f, SH.Blue, Modifier.fillMaxWidth())
                    }
                    Column(Modifier.weight(1f)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Bộ nhớ RAM", style = SHType.Caption, color = SH.TextDim)
                            Text("44%", style = SHType.Caption, color = SH.Green)
                        }
                        ThinBar(0.44f, SH.Green, Modifier.fillMaxWidth())
                    }
                }
            }

            // The 2: Cau hinh LAN / DHCP
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.NetworkCheck, SH.Purple, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cấu hình LAN / DHCP", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Text("DHCP: Bật", style = SHType.Caption, color = SH.Green, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))

                val lanInfo = listOf(
                    "IP Router (Gateway)" to wifi.info.ipRouter,
                    "Dải IP bắt đầu" to "192.168.0.100",
                    "Dải IP kết thúc" to "192.168.0.254",
                    "Lease Time" to "24 giờ (1440 phút)",
                    "Subnet Mask" to "255.255.255.0",
                )
                lanInfo.forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = SHType.Caption, color = SH.TextDim, modifier = Modifier.weight(0.5f))
                        Text(v, style = SHType.Caption, color = SH.Text, maxLines = 1, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.5f))
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    MiniActionButton("Lưu cấu hình LAN", SH.Blue) {}
                }
            }
        }

        // ---- Hang 2: DNS + WAN ----
        Row(
            Modifier.fillMaxWidth().height(185.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // The 3: Cau hinh DNS
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Lock, SH.Teal, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cấu hình DNS", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))

                val dnsOptions = listOf(
                    "DNS Nhà mạng (Tự động)" to "Auto ISP",
                    "Cloudflare DNS (Nhanh & Riêng tư)" to "1.1.1.1",
                    "Google Public DNS" to "8.8.8.8",
                    "Thủ công (Tùy chỉnh DNS)" to "Primary / Secondary",
                )
                var selectedDns by remember { mutableIntStateOf(1) } // default Cloudflare
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    dnsOptions.forEachIndexed { i, (label, hint) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SH.Card2)
                                .border(
                                    Dim.BorderWidth,
                                    if (selectedDns == i) SH.Blue.tint(0.5f) else SH.Border,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { selectedDns = i }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedDns == i) SH.Blue else SH.Card2)
                                    .border(Dim.BorderWidth, if (selectedDns == i) SH.Blue else SH.TextOff, CircleShape),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(label, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(hint, style = SHType.Caption, color = if (selectedDns == i) SH.Green else SH.TextOff, maxLines = 1, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    MiniActionButton("Áp dụng DNS", SH.Blue) {}
                }
            }

            // The 4: Cai dat WAN & Cau hinh mang
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Settings, SH.Purple, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cài đặt WAN & Cấu hình mạng", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                }
                Spacer(Modifier.height(6.dp))

                val wanInfo = listOf(
                    "Loại kết nối WAN" to "PPPoE (Tài khoản nhà mạng)",
                    "Tên mạng WiFi (SSID)" to wifi.info.ssid,
                    "Băng tần phát sóng" to "2.4GHz + 5GHz",
                    "Kênh phát (Channel)" to "Tự động (Tối ưu)",
                    "Độ rộng kênh" to "80 MHz (5GHz)",
                )
                wanInfo.forEach { (k, v) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(k, style = SHType.Caption, color = SH.TextDim, modifier = Modifier.weight(0.5f))
                        Text(v, style = SHType.Caption, color = SH.Text, maxLines = 1, modifier = Modifier.weight(0.5f))
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    MiniActionButton("Lưu cấu hình mạng", SH.Blue) {}
                }
            }
        }

        // ---- Hang 3: Quet nhieu kenh WiFi + Cong cu he thong ----
        Row(
            Modifier.fillMaxWidth().height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // The 5: Quet nhieu kenh WiFi
            var channelNoiseList by remember { mutableStateOf(wifi.channelNoise) }
            var isScanning by remember { mutableStateOf(false) }

            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.SignalWifi4Bar, SH.Amber, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Quét mạng & Cấu hình giám sát", style = SHType.CardTitle, color = SH.Text, maxLines = 1, modifier = Modifier.weight(1f))
                    MiniActionButton(if (isScanning) "Đang đo..." else "Quét kênh", SH.Blue) {
                        isScanning = true
                        val rnd = kotlin.random.Random(System.currentTimeMillis())
                        channelNoiseList = channelNoiseList.map { ch ->
                            ch.copy(noise = rnd.nextInt(12, 75))
                        }
                        isScanning = false
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Khung ket qua do kenh WiFi
                SmartCard(Modifier.fillMaxWidth(), padding = 8.dp, background = SH.Card2) {
                    val best = channelNoiseList.minByOrNull { it.noise }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Mức nhiễu kênh 5GHz xung quanh", style = SHType.Caption, color = SH.TextDim)
                        Text(
                            "Khuyên dùng: Kênh ${best?.channel ?: 149}",
                            style = SHType.Caption,
                            color = SH.Green,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        channelNoiseList.forEach { ch ->
                            ChannelBar(ch, Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Cac cong tac giam sat
                val monitorToggles = listOf(
                    "Port Forwarding" to "Cho phép truy cập Camera/NAS từ xa",
                    "Chế độ DMZ Host" to "Mở toàn bộ cổng cho 1 thiết bị",
                    "Cảnh báo mất mạng" to "Gửi thông báo khi mất kết nối WAN",
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    monitorToggles.forEach { (title, desc) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(title, style = SHType.Caption, color = SH.Text, maxLines = 1)
                                Text(desc, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
                            }
                            var on by remember(title) { mutableStateOf(title != "Chế độ DMZ Host") }
                            Switch(
                                checked = on,
                                onCheckedChange = { on = it },
                                modifier = Modifier.height(20.dp),
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

            // The 6: Cong cu he thong
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Settings, SH.Red, size = 26.dp, iconSize = 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Công cụ hệ thống", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
                }
                Spacer(Modifier.height(8.dp))

                val tools = listOf(
                    Triple(Icons.Filled.Refresh, "Khởi động lại Router", "Làm mới bộ nhớ & kết nối") to SH.Blue,
                    Triple(Icons.Filled.Save, "Sao lưu cấu hình", "Xuất file cấu hình .json") to SH.Green,
                    Triple(Icons.Filled.Download, "Cập nhật Firmware", "Hiện tại: v9.9.12-stable") to SH.Purple,
                    Triple(Icons.Filled.Delete, "Khôi phục mặc định", "Đặt lại cấu hình xuất xưởng") to SH.Red,
                )
                Column(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    tools.forEach { (triple, color) ->
                        val (icon, title, desc) = triple
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (color == SH.Red) SH.Red.tint(0.08f) else SH.Card2)
                                .border(
                                    Dim.BorderWidth,
                                    if (color == SH.Red) SH.Red.tint(0.2f) else SH.Border,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    if (title == "Khởi động lại Router") onRebootClick()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(icon, color, size = 28.dp, iconSize = 15.dp)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    title,
                                    style = SHType.Caption,
                                    color = if (color == SH.Red) SH.Red else SH.Text,
                                    maxLines = 1,
                                )
                                Text(desc, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  COMPOSABLE PHU TRO
// =====================================================================

@Composable
private fun WifiTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Int? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SH.Blue else SH.Card)
            .border(Dim.BorderWidth, if (selected) SH.Blue else SH.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) Color.White else SH.TextDim, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label, style = SHType.Body,
            color = if (selected) Color.White else SH.TextDim,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (badge != null && badge > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                badge.toString(),
                style = SHType.Caption,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Red)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun WifiToolButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.tint(0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun MiniStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
) {
    SmartCard(modifier.fillMaxHeight(), padding = 8.dp) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                Text(value, style = SHType.Value, color = if (color == SH.TextDim) SH.TextDim else SH.Text, maxLines = 1)
            }
            IconBadge(icon, color, size = 30.dp, iconSize = 16.dp)
        }
    }
}

@Composable
private fun KpiCard(
    modifier: Modifier,
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    color: Color,
) {
    SmartCard(modifier.fillMaxHeight(), padding = 8.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 1, modifier = Modifier.weight(1f))
            IconBadge(icon, color, size = 22.dp, iconSize = 12.dp)
        }
        Spacer(Modifier.weight(1f))
        Text(value, style = SHType.Value, color = SH.Text, maxLines = 1)
        Text(sub, style = SHType.Caption, color = if (sub.startsWith("+")) SH.Green else SH.TextDim, maxLines = 1)
    }
}

@Composable
private fun MiniActionButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        label,
        style = SHType.Caption,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.tint(0.12f))
            .border(Dim.BorderWidth, color.tint(0.3f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun ChannelBar(ch: ChannelNoise, modifier: Modifier = Modifier) {
    val color = when {
        ch.noise < 30 -> SH.Green
        ch.noise < 55 -> SH.Amber
        else -> SH.Red
    }
    val label = when {
        ch.noise < 30 -> "Rất thoáng"
        ch.noise < 55 -> "Trung bình"
        else -> "Nhiễu cao"
    }
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Khung cot do nhieu
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SH.Card)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(6.dp))
                .padding(2.5.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight((ch.noise / 100f).coerceIn(0.08f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(color),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("CH ${ch.channel}", style = SHType.Label, color = SH.Text, maxLines = 1)
        Text(label, style = SHType.Caption, color = color, maxLines = 1)
        Text("${ch.noise}%", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
    }
}
