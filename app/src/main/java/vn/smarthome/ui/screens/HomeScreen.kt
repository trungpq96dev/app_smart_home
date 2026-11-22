package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import vn.smarthome.data.DemoData
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.data.lunar.AmLich
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.HallwayStage
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.KitchenStage
import vn.smarthome.ui.components.LivingRoomStage
import vn.smarthome.ui.components.MoPhongPhongNgu
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
import vn.smarthome.ui.theme.tint
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
    congTac: CongTacState = CongTacState(),
    choCongTac: Map<String, Boolean> = emptyMap(),
    onRoomQuickAction: (roomId: String, actionId: String) -> Unit = { _, actionId -> onQuick(actionId) },
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

        // ---------- Hang giua: the phong 2.5D | o thao tac nhanh | thoi tiet ----------
        val selectedRoom = state.rooms.getOrNull(state.selectedRoom) ?: state.rooms.firstOrNull()
        val quickSlots = remember(selectedRoom, congTac, choCongTac, state.quick) {
            selectedRoom?.let { oThaoTacNhanhCuaPhong(it, congTac, choCongTac) }
                ?: state.quick.map {
                    HomeQuickSlot(it.id, it.label, null, it.icon, it.color, it.on)
                }
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            RoomPager(
                rooms = state.rooms,
                current = state.selectedRoom,
                onPage = onSelectRoom,
                onOpen = onOpenRoom,
                modifier = Modifier.weight(1.45f).fillMaxHeight(),
                congTac = congTac,
                choCongTac = choCongTac,
            )

            Column(
                Modifier.weight(0.85f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                quickSlots.chunked(2).forEach { pair ->
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
                                onClick = {
                                    if (selectedRoom != null) {
                                        onRoomQuickAction(selectedRoom.id, q.id)
                                    } else {
                                        onQuick(q.id)
                                    }
                                },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                subtitle = q.subtitle,
                            )
                        }
                    }
                }
            }

            WeatherCard(
                ui = weather,
                onRefresh = onRefreshWeather,
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
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

private data class TrangThaiCongTacHienThi(
    val laThat: Boolean,
    val roRang: Boolean,
    val dangCho: Boolean,
    val bat: Boolean,
    val batChoMoPhong: Boolean,
)

private fun docCongTacHienThi(
    roomId: String,
    loai: String,
    giaTriRoom: Boolean,
    ct: CongTacState,
    cho: Map<String, Boolean>,
): TrangThaiCongTacHienThi {
    val tb = ct.tim(roomId, loai)
        ?: return TrangThaiCongTacHienThi(
            laThat = false,
            roRang = true,
            dangCho = false,
            bat = giaTriRoom,
            batChoMoPhong = giaTriRoom,
        )
    val tt = ct.trangThai[tb.ma]
    val mong = cho[tb.ma]
    val dangCho = mong != null
    val roRang = dangCho || (ct.bridgeSong && tt?.online == true && tt.bat != null)
    val batThuc = when {
        dangCho -> mong ?: giaTriRoom
        ct.bridgeSong && tt?.online == true && tt.bat != null -> tt.bat
        else -> false
    }
    val batMoPhong = when {
        dangCho -> mong ?: giaTriRoom
        ct.bridgeSong && tt?.online == true && tt.bat != null -> tt.bat
        else -> giaTriRoom
    }
    return TrangThaiCongTacHienThi(
        laThat = true,
        roRang = roRang,
        dangCho = dangCho,
        bat = batThuc,
        batChoMoPhong = batMoPhong,
    )
}

private data class HomeQuickSlot(
    val id: String,
    val label: String,
    val subtitle: String?,
    val icon: ImageVector,
    val color: Color,
    val on: Boolean,
)

private fun oThaoTacNhanhCuaPhong(
    room: RoomUi,
    ct: CongTacState,
    cho: Map<String, Boolean>,
): List<HomeQuickSlot> {
    val den = docCongTacHienThi(room.id, "den", room.lightOn, ct, cho)
    val quat = docCongTacHienThi(room.id, "quat", room.fanOn, ct, cho)

    val oDen = HomeQuickSlot(
        id = "light",
        label = when (room.id) {
            "kitchen" -> "Đèn bếp"
            "hall" -> "Đèn sảnh"
            else -> "Đèn"
        },
        subtitle = when {
            den.dangCho -> "Đang gửi…"
            !den.roRang -> "—"
            den.bat -> "${room.lightBrightness}%"
            else -> "Tắt"
        },
        icon = Icons.Filled.Lightbulb,
        color = SH.Amber,
        on = den.roRang && den.bat,
    )

    return when (room.id) {
        "living" -> listOf(
            oDen,
            HomeQuickSlot(
                id = "fan",
                label = "Quạt",
                subtitle = when {
                    quat.dangCho -> "Đang gửi…"
                    !quat.roRang -> "—"
                    quat.bat -> "Đang bật"
                    else -> "Tắt"
                },
                icon = Icons.Filled.Air,
                color = SH.Blue,
                on = quat.roRang && quat.bat,
            ),
            HomeQuickSlot(
                id = "ac",
                label = "Điều hòa",
                subtitle = if (room.acOn) "${room.acTemp}°C" else "Tắt",
                icon = Icons.Filled.AcUnit,
                color = SH.Purple,
                on = room.acOn,
            ),
            HomeQuickSlot(
                id = "curtain",
                label = "Rèm",
                subtitle = if (room.curtainOpen > 0) "Mở ${room.curtainOpen}%" else "Đóng",
                icon = Icons.Filled.Blinds,
                color = SH.Teal,
                on = room.curtainOpen > 0,
            ),
        )
        "kitchen" -> listOf(
            oDen,
            HomeQuickSlot(
                id = "hood",
                label = "Hút mùi",
                subtitle = when {
                    quat.dangCho -> "Đang gửi…"
                    !quat.roRang -> "—"
                    quat.bat -> "Cấp ${room.hoodSpeed}"
                    else -> "Tắt"
                },
                icon = Icons.Filled.Air,
                color = SH.Blue,
                on = quat.roRang && quat.bat,
            ),
            HomeQuickSlot(
                id = "stove",
                label = "Bếp từ",
                subtitle = if (room.stoveOn) "Mức ${room.stoveLevel}" else "Tắt",
                icon = Icons.Filled.LocalFireDepartment,
                color = SH.Orange,
                on = room.stoveOn,
            ),
            HomeQuickSlot(
                id = "schedule",
                label = "Hẹn giờ",
                subtitle = if (room.scheduleOn) "15 phút" else "Tắt",
                icon = Icons.Filled.Schedule,
                color = SH.Teal,
                on = room.scheduleOn,
            ),
        )
        "hall" -> listOf(
            oDen,
            HomeQuickSlot(
                id = "hall_front",
                label = "Đèn trước",
                subtitle = if (room.hallFrontLightOn) "${room.hallFrontBrightness}%" else "Tắt",
                icon = Icons.Filled.WbSunny,
                color = SH.Orange,
                on = room.hallFrontLightOn,
            ),
            HomeQuickSlot(
                id = "hall_lock",
                label = "Khóa cửa",
                subtitle = if (room.hallDoorLocked) "Đã khóa" else "Đang mở",
                icon = if (room.hallDoorLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                color = if (room.hallDoorLocked) SH.Green else SH.Red,
                on = room.hallDoorLocked,
            ),
            HomeQuickSlot(
                id = "hall_camera",
                label = "Camera",
                subtitle = when {
                    !room.hallCameraOn -> "Tắt"
                    room.hallIrMode -> "IR"
                    else -> "Đang ghi"
                },
                icon = Icons.Filled.Videocam,
                color = SH.Blue,
                on = room.hallCameraOn,
            ),
        )
        else -> listOf(
            oDen,
            HomeQuickSlot(
                id = "fan",
                label = "Quạt",
                subtitle = when {
                    quat.dangCho -> "Đang gửi…"
                    !quat.roRang -> "—"
                    quat.bat -> "Cấp ${room.fanSpeed}"
                    else -> "Tắt"
                },
                icon = Icons.Filled.Air,
                color = SH.Blue,
                on = quat.roRang && quat.bat,
            ),
            HomeQuickSlot(
                id = "ac",
                label = "Điều hòa",
                subtitle = if (room.acOn) "${room.acTemp}°C" else "Tắt",
                icon = Icons.Filled.AcUnit,
                color = SH.Purple,
                on = room.acOn,
            ),
            HomeQuickSlot(
                id = "sleep",
                label = "Chế độ ngủ",
                subtitle = if (room.mode == RoomMode.SLEEP) "Đang bật" else "Chạm bật",
                icon = Icons.Filled.DarkMode,
                color = SH.Teal,
                on = room.mode == RoomMode.SLEEP,
            ),
        )
    }
}

private data class ChipThongTinPhong(
    val icon: ImageVector,
    val label: String,
    val active: Boolean,
    val color: Color,
    val mutedText: Boolean = false,
)

private fun tenCheDoPhong(room: RoomUi): String = when (room.id) {
    "kitchen" -> when (room.mode) {
        RoomMode.HOME -> "Nấu ăn"
        RoomMode.CUSTOM -> "Ăn tối"
        RoomMode.SLEEP -> "Dọn dẹp"
        RoomMode.AWAY -> "Vắng nhà"
    }
    "hall" -> when (room.mode) {
        RoomMode.SLEEP -> "Đêm an ninh"
        RoomMode.HOME -> "Về nhà"
        RoomMode.CUSTOM -> "Tiếp khách"
        RoomMode.AWAY -> "Đi vắng"
    }
    else -> room.mode.label
}

private fun chipsTomTatPhong(
    room: RoomUi,
    den: TrangThaiCongTacHienThi,
    quat: TrangThaiCongTacHienThi,
): List<ChipThongTinPhong> {
    val denChip = ChipThongTinPhong(
        icon = Icons.Filled.Lightbulb,
        label = when {
            !den.roRang -> "Đèn: —"
            den.bat -> "Đèn: Bật"
            else -> "Đèn: Tắt"
        },
        active = den.roRang && den.bat,
        color = SH.Amber,
        mutedText = !den.roRang,
    )
    return when (room.id) {
        "living" -> listOf(
            denChip,
            ChipThongTinPhong(
                icon = Icons.Filled.AcUnit,
                label = if (room.acOn) "${room.acTemp}°C" else "ĐH tắt",
                active = room.acOn,
                color = SH.Purple,
            ),
            when {
                room.tvOn -> ChipThongTinPhong(Icons.Filled.Tv, "TV Bật", true, SH.Blue)
                quat.roRang && quat.bat -> ChipThongTinPhong(Icons.Filled.Air, "Quạt Bật", true, SH.Cyan)
                else -> ChipThongTinPhong(
                    Icons.Filled.Blinds,
                    if (room.curtainOpen > 0) "Rèm ${room.curtainOpen}%" else "Rèm đóng",
                    room.curtainOpen > 0,
                    SH.Teal,
                )
            },
        )
        "kitchen" -> listOf(
            denChip,
            ChipThongTinPhong(
                icon = Icons.Filled.Air,
                label = when {
                    !quat.roRang -> "Hút: —"
                    quat.bat -> "Hút C${room.hoodSpeed}"
                    else -> "Hút tắt"
                },
                active = quat.roRang && quat.bat,
                color = SH.Blue,
                mutedText = !quat.roRang,
            ),
            ChipThongTinPhong(
                icon = Icons.Filled.LocalFireDepartment,
                label = if (room.stoveOn) "Bếp P${room.stoveLevel}" else "Bếp tắt",
                active = room.stoveOn,
                color = SH.Orange,
            ),
        )
        "hall" -> {
            val soDenBat = listOf(den.roRang && den.bat, room.hallFrontLightOn, room.hallSurroundingLightOn).count { it }
            listOf(
                ChipThongTinPhong(
                    icon = Icons.Filled.Lightbulb,
                    label = if (!den.roRang) "Đèn: —" else "Đèn $soDenBat/3",
                    active = soDenBat > 0,
                    color = SH.Amber,
                    mutedText = !den.roRang,
                ),
                ChipThongTinPhong(
                    icon = if (room.hallDoorLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    label = if (room.hallDoorLocked) "Đã khóa" else "Đang mở",
                    active = true,
                    color = if (room.hallDoorLocked) SH.Green else SH.Red,
                ),
                ChipThongTinPhong(
                    icon = Icons.Filled.Videocam,
                    label = when {
                        !room.hallCameraOn -> "Cam tắt"
                        room.hallIrMode -> "Cam IR"
                        else -> "Cam"
                    },
                    active = room.hallCameraOn,
                    color = if (room.hallIrMode && room.hallCameraOn) SH.Green else SH.Blue,
                ),
            )
        }
        else -> listOf(
            denChip,
            ChipThongTinPhong(
                icon = Icons.Filled.Air,
                label = when {
                    !quat.roRang -> "Quạt: —"
                    quat.bat -> "Cấp ${room.fanSpeed}"
                    else -> "Quạt tắt"
                },
                active = quat.roRang && quat.bat,
                color = SH.Blue,
                mutedText = !quat.roRang,
            ),
            ChipThongTinPhong(
                icon = Icons.Filled.AcUnit,
                label = if (room.acOn) "${room.acTemp}°C" else "ĐH tắt",
                active = room.acOn,
                color = SH.Purple,
            ),
        )
    }
}

/** Bang xoay the phong mo phong 2.5D - vuot ngang hoac bam < > de doi phong, bam de dieu khien. */
@Composable
private fun RoomPager(
    rooms: List<RoomUi>,
    current: Int,
    onPage: (Int) -> Unit,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    congTac: CongTacState = CongTacState(),
    choCongTac: Map<String, Boolean> = emptyMap(),
) {
    val safeInitial = current.coerceIn(0, maxOf(rooms.lastIndex, 0))
    val pager = rememberPagerState(initialPage = safeInitial) { rooms.size }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pager.currentPage) { onPage(pager.currentPage) }

    Box(modifier) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val room = rooms[page]
            val den = docCongTacHienThi(room.id, "den", room.lightOn, congTac, choCongTac)
            val quat = docCongTacHienThi(room.id, "quat", room.fanOn, congTac, choCongTac)
            val coCanhBao = (room.id == "kitchen" && room.gasAlert) ||
                (room.id == "hall" && room.hallMotionDetected)
            val coThietBiBat = (den.roRang && den.bat) || (quat.roRang && quat.bat) ||
                room.acOn || room.tvOn || room.stoveOn ||
                (room.id == "hall" && (room.hallFrontLightOn || room.hallSurroundingLightOn))
            val shape = RoundedCornerShape(Dim.CardRadius)
            val borderColor = when {
                coCanhBao -> SH.Red
                coThietBiBat -> room.accent.tint(0.45f)
                else -> SH.Border
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(Color(0xFF0A101E))
                    .border(if (coCanhBao) 1.5.dp else Dim.BorderWidth, borderColor, shape)
                    .clickable { onOpen(room.id) },
            ) {
                when (room.id) {
                    "living" -> LivingRoomStage(
                        room = room.copy(lightOn = den.batChoMoPhong, fanOn = quat.batChoMoPhong),
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )
                    "kitchen" -> KitchenStage(
                        room = room.copy(lightOn = den.batChoMoPhong, fanOn = quat.batChoMoPhong),
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )
                    "hall" -> HallwayStage(
                        room = room.copy(lightOn = den.batChoMoPhong),
                        modifier = Modifier.fillMaxSize(),
                        compact = true,
                    )
                    "bed1", "bed2", "bed3" -> MoPhongPhongNgu(
                        lightOn = den.batChoMoPhong,
                        fanOn = quat.batChoMoPhong,
                        acOn = room.acOn,
                        acTemp = room.acTemp,
                        mode = room.mode,
                        modifier = Modifier.fillMaxSize(),
                        lightBrightness = room.lightBrightness,
                        fanSpeed = room.fanSpeed,
                        compact = true,
                    )
                    else -> ImagePlaceholder(
                        icon = room.icon,
                        label = room.name,
                        accent = room.accent,
                        showLabel = false,
                        iconSize = 58.dp,
                        cornerRadius = Dim.CardRadius,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Phu chuyen sac tren & duoi giup chu va chip luon ro du den sang hay tat
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xD9070B14),
                                    Color(0x66070B14),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(94.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0x88070B14),
                                    Color(0xEB070B14),
                                ),
                            ),
                        ),
                )

                // Hang tren: Ten phong + che do / chip THAT + nut chuyen phong < >
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = room.name,
                                style = SHType.PageTitle,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (den.laThat || quat.laThat) {
                                Text(
                                    text = "THẬT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0x3822C55E))
                                        .border(1.dp, Color(0x7022C55E), RoundedCornerShape(5.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            val sub = buildString {
                                append("Chế độ: ").append(tenCheDoPhong(room))
                                if (room.id == "hall") append(" • ").append(room.hallView.label)
                            }
                            Dot(if (coCanhBao) SH.Red else room.accent, 6.dp)
                            Text(
                                text = sub,
                                style = SHType.Caption,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (coCanhBao) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xE6DC2626))
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                ) {
                                    Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    Text(
                                        text = if (room.id == "kitchen") "RÒ RỈ GAS" else "CHUYỂN ĐỘNG",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    if (rooms.size > 1) {
                        Spacer(Modifier.width(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            PagerNavButton(
                                icon = Icons.Filled.ChevronLeft,
                                contentDescription = "Phòng trước",
                                onClick = {
                                    scope.launch {
                                        val target = (pager.currentPage - 1 + rooms.size) % rooms.size
                                        pager.animateScrollToPage(target)
                                    }
                                },
                            )
                            PagerNavButton(
                                icon = Icons.Filled.ChevronRight,
                                contentDescription = "Phòng tiếp theo",
                                onClick = {
                                    scope.launch {
                                        val target = (pager.currentPage + 1) % rooms.size
                                        pager.animateScrollToPage(target)
                                    }
                                },
                            )
                        }
                    }
                }

                // Hang duoi: chip trang thai thiet bi + nut Dieu khien + PagerDots
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val chips = chipsTomTatPhong(room, den, quat)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            chips.forEach { c ->
                                RoomStatusChip(chip = c)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SH.Blue)
                                .clickable { onOpen(room.id) }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Điều khiển",
                                style = SHType.Label,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        PagerDots(
                            count = rooms.size,
                            current = pager.currentPage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PagerNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xB80F172A))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun RoomStatusChip(chip: ChipThongTinPhong) {
    val bg = if (chip.active) Color(0xCC0F172A) else Color(0xB8090E1A)
    val border = if (chip.active) chip.color.tint(0.55f) else Color.White.copy(alpha = 0.14f)
    val tintColor = if (chip.active) chip.color else SH.TextOff
    val textColor = when {
        chip.mutedText -> SH.TextDim
        chip.active -> if (chip.color == SH.Red) Color(0xFFFCA5A5) else Color(0xFFF1F5F9)
        else -> SH.TextDim
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(7.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = chip.icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = chip.label,
            style = SHType.Caption,
            fontWeight = if (chip.active) FontWeight.Medium else FontWeight.Normal,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
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
