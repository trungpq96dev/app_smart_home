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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.DemoData
import vn.smarthome.data.RoomUi
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.PagerDots
import vn.smarthome.ui.components.QuickActionTile
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatTile
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.rememberClock
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.vm.HomeState

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
    onSelectRoom: (Int) -> Unit,
    onOpenRoom: (String) -> Unit,
    onQuick: (String) -> Unit,
    onOpenStats: () -> Unit,
) {
    val clock = rememberClock()

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

        // ---------- Hang giua: the phong | o thao tac nhanh ----------
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            RoomPager(
                rooms = state.rooms,
                current = state.selectedRoom,
                onPage = onSelectRoom,
                onOpen = onOpenRoom,
                modifier = Modifier.weight(1.3f).fillMaxHeight(),
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
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---------- Dai trang thai duoi cung ----------
        SmartCard(Modifier.fillMaxWidth().height(40.dp), padding = 9.dp, onClick = onOpenStats) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle, null,
                    tint = SH.Green, modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    DemoData.systemStatus,
                    style = SHType.Body,
                    color = SH.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("Xem chi tiết", style = SHType.Button, color = SH.Blue, maxLines = 1)
                Spacer(Modifier.width(5.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward, null,
                    tint = SH.Blue, modifier = Modifier.size(15.dp),
                )
            }
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

@Preview(widthDp = Dim.DesignW - 164 - 24, heightDp = Dim.DesignH - 24, showBackground = true)
@Composable
private fun HomePreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(0.dp)) {
            HomeScreen(HomeState(), {}, {}, {}, {})
        }
    }
}
