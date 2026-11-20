package vn.smarthome.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.DemoData
import vn.smarthome.data.RoomUi
import vn.smarthome.data.lunar.AmLich
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.PagerDots
import vn.smarthome.ui.components.QuickActionTile
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatTile
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.components.WeatherCard
import vn.smarthome.ui.shell.ClockNow
import vn.smarthome.ui.shell.rememberClock
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.vm.HomeState
import java.time.LocalDate

/**
 * Trang chu.
 *
 * Ngan sach chieu cao (vung noi dung ~509dp sau khi tru padding):
 *   header 64 + 10 + o so lieu 84 + 10 + hang giua (con lai) + 10 + dai duoi 40
 * Hang giua an theo weight nen khong bi tran du chieu cao that co lech chut.
 */
@Composable
fun HomeScreen(
    state: HomeState,
    weather: ThoiTietUi,
    wifi: WifiState = WifiState(),
    onSelectRoom: (Int) -> Unit,
    onOpenRoom: (String) -> Unit,
    onQuick: (String) -> Unit,
    onOpenStats: () -> Unit,
    onRefreshWeather: () -> Unit,
    onOpenWifi: () -> Unit = {},
    onOpenLich: () -> Unit = {},
) {
    val clock = rememberClock(seconds = true)

    Column(Modifier.fillMaxSize()) {

        // ---------- Header ----------
        SmartCard(Modifier.fillMaxWidth().height(64.dp), padding = 10.dp) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.Home, SH.Blue, size = 40.dp, iconSize = 22.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Nhà thông minh của bạn",
                        style = SHType.PageTitle,
                        color = SH.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(clock.greeting, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                }
                DongHoLich(
                    clock = clock,
                    onOpenLich = onOpenLich,
                    modifier = Modifier.fillMaxHeight(),
                )
                Spacer(Modifier.width(10.dp))
                if (DemoData.IS_DEMO) {
                    DemoBadge()
                    Spacer(Modifier.width(8.dp))
                }
                StatusPill("Trực tuyến", SH.Green)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Wifi, null, tint = SH.TextDim, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---------- Bon o so lieu ----------
        Row(
            Modifier.fillMaxWidth().height(88.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            StatTile(
                Icons.Filled.Thermostat, DemoData.indoorTemp, "°C",
                "Nhiệt độ trong nhà", SH.Red, Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                Icons.Filled.WaterDrop, DemoData.humidity, "%",
                "Độ ẩm", SH.Cyan, Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                Icons.Filled.Bolt, DemoData.powerNow, "kW",
                "Điện năng hiện tại", SH.Amber, Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                Icons.Filled.Shield, DemoData.safetyLabel, "",
                "Tổng quan", SH.Green, Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---------- Hang giua: the phong | o thao tac nhanh | thoi tiet ----------
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            RoomPager(
                rooms = state.rooms,
                current = state.selectedRoom,
                onPage = onSelectRoom,
                onOpen = onOpenRoom,
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
            )

            Column(
                Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                state.quick.chunked(2).forEach { pair ->
                    Row(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
                    ) {
                        pair.forEach { q ->
                            QuickActionTile(
                                icon = q.icon,
                                label = q.label,
                                color = q.color,
                                on = q.on,
                                onClick = { onQuick(q.id) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }

            WeatherCard(
                ui = weather,
                onRefresh = onRefreshWeather,
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---------- Dai duoi: WiFi gia dinh + Thiet bi da ket noi ----------
        Row(
            Modifier.fillMaxWidth().height(110.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---- Left card: WiFi gia dinh ----
            SmartCard(
                Modifier.weight(0.45f).fillMaxHeight(),
                padding = 10.dp,
            ) {
                // Top row: icon + title + pill
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Wifi, SH.Purple, size = 28.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("WiFi gia đình", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f), maxLines = 1)
                    val online = wifi.info.isConnected
                    StatusPill(if (online) "Đã kết nối" else "Mất kết nối", if (online) SH.Green else SH.Red)
                }

                Spacer(Modifier.height(6.dp))

                // Middle: SSID + Security
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = SH.TextDim, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SSID: ${wifi.info.ssid}", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    Text("Bảo mật: ${wifi.info.security}", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                }

                Spacer(Modifier.height(6.dp))

                // Bottom: status left + button right
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val online = wifi.info.isConnected
                    Icon(
                        if (online) Icons.Filled.CheckCircle else Icons.Filled.CheckCircle,
                        null,
                        tint = if (online) SH.Green else SH.Red,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (online) "Đang hoạt động ổn định" else "Mất kết nối Internet",
                        style = SHType.Caption,
                        color = if (online) SH.Green else SH.Red,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Tốc độ: ${wifi.info.speedMbps} Mbps", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Filled.SignalCellularAlt, null, tint = SH.Green, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onOpenWifi,
                        modifier = Modifier.height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                        contentPadding = ButtonDefaults.ContentPadding.let {
                            androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        },
                    ) {
                        Text("Cài đặt WiFi", style = SHType.Caption, color = SH.Text, maxLines = 1)
                    }
                }
            }

            // ---- Right card: Thiet bi da ket noi ----
            SmartCard(
                Modifier.weight(0.55f).fillMaxHeight(),
                padding = 10.dp,
            ) {
                // Header row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Devices, null, tint = SH.Blue, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Thiết bị đã kết nối (${wifi.devices.size})",
                        style = SHType.CardTitle,
                        color = SH.Text,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Text(
                        "Xem tất cả →",
                        style = SHType.Caption,
                        color = SH.Blue,
                        maxLines = 1,
                        modifier = Modifier.clickable(onClick = onOpenWifi),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Device icons row
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val hienThi = wifi.devices.take(7)
                    val mauIcon = listOf(SH.Blue, SH.Cyan, SH.Purple, SH.Green, SH.Amber, SH.Amber, SH.Teal)
                    hienThi.forEachIndexed { idx, dev ->
                        WifiDeviceIcon(dev.icon, dev.name.take(10), mauIcon[idx % mauIcon.size])
                    }
                    if (wifi.devices.size > 7) {
                        WifiDeviceIcon(Icons.Filled.Add, "Khác (${wifi.devices.size - 7})", SH.TextDim)
                    }
                }
            }
        }
    }
}

/**
 * Dong ho that + ngay am lich, nam ben phai thanh tieu de Trang chu.
 *
 * Bam vao la mo man Tien ich de xem full lich van nien (am lich ca thang).
 *
 * Ngay am chi tinh lai khi chuoi ngay duong doi chu khong phai moi giay: dong
 * ho nhay tung giay, con am lich thi mot ngay moi doi mot lan.
 */
@Composable
private fun DongHoLich(
    clock: ClockNow,
    onOpenLich: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val am = remember(clock.date) {
        val d = LocalDate.now()
        AmLich.tuDuongLich(d.dayOfMonth, d.monthValue, d.year)
    }
    val canChi = remember(am) { AmLich.canChi(am) }

    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card2)
            .clickable(onClick = onOpenLich)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(clock.time, style = SHType.Value, color = SH.Text, maxLines = 1)
            Text(clock.date, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
        }

        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(1.dp).height(30.dp).background(SH.Border))
        Spacer(Modifier.width(10.dp))

        Icon(Icons.Filled.CalendarMonth, null, tint = SH.Amber, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                "Âm " + am.ngay + "/" + am.thang + (if (am.nhuan) " (nhuận)" else ""),
                style = SHType.BodyMed,
                color = SH.Amber,
                maxLines = 1,
            )
            Text("Năm " + canChi.nam, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
        }
    }
}

/** Bang xoay the phong - vuot ngang de doi phong, bam de mo man chi tiet. */
@Composable
private fun RoomPager(
    rooms: List<RoomUi>,
    current: Int,
    onPage: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pager = rememberPagerState(initialPage = current) { rooms.size }

    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }

    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val room = rooms[page]
            ImagePlaceholder(
                icon = room.icon,
                label = room.name,
                accent = room.accent,
                showLabel = false,
                iconSize = 58.dp,
                cornerRadius = Dim.CardRadius,
                modifier = Modifier.fillMaxSize(),
            ) {
                // Lop chu de len anh giu cho
                Column(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        room.name,
                        style = SHType.PageTitle,
                        color = SH.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Lightbulb, null,
                        tint = if (room.lightOn) SH.Amber else SH.TextOff,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (room.lightOn) "Đèn: Bật" else "Đèn: Tắt",
                        style = SHType.BodyMed,
                        color = if (room.lightOn) SH.Amber else SH.TextDim,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    androidx.compose.material3.TextButton(onClick = { onOpen(room.id) }) {
                        Text("Điều khiển", style = SHType.Button, color = SH.Blue)
                    }
                }
            }
        }

        PagerDots(
            count = rooms.size,
            current = pager.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
        )
    }
}

/** Icon thiet bi nho trong card "Thiet bi da ket noi". */
@Composable
private fun WifiDeviceIcon(icon: ImageVector, label: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = SHType.Caption, color = SH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(widthDp = Dim.DesignW - 164 - 24, heightDp = Dim.DesignH - 24, showBackground = true)
@Composable
private fun HomePreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(0.dp)) {
            HomeScreen(HomeState(), ThoiTietUi.DangTai, WifiState(), {}, {}, {}, {}, {})
        }
    }
}
