package vn.smarthome.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import vn.smarthome.ui.screens.CameraScreen
import vn.smarthome.ui.screens.HomeScreen
import vn.smarthome.ui.screens.PlaceholderScreen
import vn.smarthome.ui.screens.RoomDetailScreen
import vn.smarthome.ui.screens.StatsScreen
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.vm.HomeViewModel

/**
 * Khung ngoai cung: cot menu co dinh ben trai + vung noi dung ben phai.
 *
 * Dieu huong lam bang mot bien `current` chu khong dung Navigation-Compose:
 * app chi co vai man phang, them thu vien dieu huong chi lam nang them.
 * `openRoom` khac null thi vung noi dung doi sang man chi tiet phong.
 */
@Composable
fun AppShell(vm: HomeViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()

    var current by rememberSaveable { mutableStateOf(Dest.HOME.route) }
    var openRoom by rememberSaveable { mutableStateOf<String?>(null) }
    var showStats by rememberSaveable { mutableStateOf(false) }

    val dest = Dest.from(current)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    // Nut Back cua may: thoat man con truoc, chua thoat app ngay.
    BackHandler(enabled = openRoom != null || showStats || dest != Dest.HOME) {
        when {
            openRoom != null -> openRoom = null
            showStats -> showStats = false
            else -> current = Dest.HOME.route
        }
    }

    Box(Modifier.fillMaxSize().background(SH.Bg)) {
        Row(Modifier.fillMaxSize()) {
            SideNav(
                current = dest,
                onSelect = { d ->
                    current = d.route
                    openRoom = null
                    showStats = false
                },
            )

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(Dim.MainPadding)
            ) {
                val room = state.rooms.firstOrNull { it.id == openRoom }

                when {
                    room != null -> RoomDetailScreen(
                        room = room,
                        onBack = { openRoom = null },
                        onHome = { openRoom = null; current = Dest.HOME.route },
                        onToggleLight = { vm.toggleLight(room.id) },
                        onToggleFan = { vm.toggleFan(room.id) },
                        onAcTemp = { d -> vm.changeAcTemp(room.id, d) },
                        onMode = { m -> vm.setMode(room.id, m) },
                        onToggleSchedule = { vm.toggleSchedule(room.id) },
                    )

                    showStats -> StatsScreen(
                        state = state,
                        onBack = { showStats = false },
                        onKind = vm::setStatKind,
                        onRange = vm::setStatRange,
                    )

                    dest == Dest.HOME -> HomeScreen(
                        state = state,
                        onSelectRoom = vm::selectRoom,
                        onOpenRoom = { id -> openRoom = id },
                        onQuick = vm::toggleQuick,
                        onOpenStats = { showStats = true },
                    )

                    dest == Dest.CAMERA -> CameraScreen(
                        state = state,
                        onBack = { current = Dest.HOME.route },
                        onSelectCamera = vm::selectCamera,
                        onNotice = toast,
                    )

                    else -> PlaceholderScreen(
                        title = dest.title,
                        icon = when (dest) {
                            Dest.DEVICES -> Icons.Filled.Devices
                            Dest.SENSORS -> Icons.Filled.Sensors
                            Dest.AUTOMATION -> Icons.Filled.AutoAwesome
                            else -> Icons.Filled.Settings
                        },
                        onBack = { current = Dest.HOME.route },
                    )
                }
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
