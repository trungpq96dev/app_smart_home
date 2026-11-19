package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.ui.components.BarChart
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Man quan ly WiFi chi tiet.
 *
 * Cot tab trai (giong StatsScreen) + noi dung phai.
 * Ho tro hien thi ca so that tu MQTT (serverubuntu -> Mosquitto) va du lieu mau.
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

        // Tieu de phu ngay duoi TopBar (wifi icon + ten)
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
                    )
                }
            }

            // Noi dung phai
            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (selectedTab == 0) {
                    TongQuanContent(
                        wifi = wifi,
                        onRebootClick = {
                            daBamReboot = true
                            onReboot()
                        },
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconBadge(Icons.Filled.Wifi, SH.TextOff, size = 50.dp, iconSize = 26.dp, shape = CircleShape)
                            Spacer(Modifier.height(10.dp))
                            Text("Chức năng đang phát triển", style = SHType.CardTitle, color = SH.TextDim)
                            Text("Tính năng sẽ sớm được cập nhật", style = SHType.Caption, color = SH.TextOff)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  TAB TONG QUAN
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
        // Tin hieu
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
        // Download
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
        // Upload
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
        // Thiet bi
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
            // Legend
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
                    val ok = wifi.trangThai == "ok"
                    Icon(Icons.Filled.CheckCircle, null, tint = if (ok) SH.Green else SH.Amber, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(if (ok) "Không có cảnh báo nào" else "Cảnh báo kết nối", style = SHType.Body, color = SH.TextDim, maxLines = 1)
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

// =====================================================================
//  Phu tro
// =====================================================================

@Composable
private fun WifiTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
        )
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
        Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
