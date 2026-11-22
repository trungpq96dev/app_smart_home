package vn.smarthome.ui.shell

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.smarthome.ui.components.BangGiongNoi
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import vn.smarthome.ui.components.OtaUpdateDialog
import vn.smarthome.ui.screens.AutomationScreen
import vn.smarthome.ui.screens.CamBienScreen
import vn.smarthome.ui.screens.CameraScreen
import vn.smarthome.ui.screens.DevicesScreen
import vn.smarthome.ui.screens.ChiTietDienScreen
import vn.smarthome.ui.screens.HomeScreen
import vn.smarthome.ui.screens.PlaceholderScreen
import vn.smarthome.ui.screens.RoomDetailScreen
import vn.smarthome.ui.screens.ServerScreen
import vn.smarthome.ui.screens.SettingsScreen
import vn.smarthome.ui.screens.StatsScreen
import vn.smarthome.ui.screens.TienIchScreen
import vn.smarthome.ui.screens.WifiScreen
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
    val dien by vm.dien.collectAsStateWithLifecycle()
    val ketNoi by vm.ketNoi.collectAsStateWithLifecycle()
    val cauHinh by vm.cauHinh.collectAsStateWithLifecycle()
    val cauHinhCamera by vm.cauHinhCamera.collectAsStateWithLifecycle()
    val trangThaiCam by vm.trangThaiCam.collectAsStateWithLifecycle()
    val thoiTiet by vm.thoiTiet.collectAsStateWithLifecycle()
    val thoiTietCauHinh by vm.cauHinhThoiTiet.collectAsStateWithLifecycle()
    val wifi by vm.wifi.collectAsStateWithLifecycle()
    val server by vm.server.collectAsStateWithLifecycle()
    val cuktech by vm.cuktech.collectAsStateWithLifecycle()
    val choCuktech by vm.choCuktech.collectAsStateWithLifecycle()
    val dangNoiLaiCuktech by vm.dangNoiLaiCuktech.collectAsStateWithLifecycle()
    val choCaiDatCuktech by vm.choCaiDatCuktech.collectAsStateWithLifecycle()
    val congTac by vm.congTac.collectAsStateWithLifecycle()
    val choCongTac by vm.choCongTac.collectAsStateWithLifecycle()
    val otaStatus by vm.otaStatus.collectAsStateWithLifecycle()
    val showUpdateDialog by vm.showUpdateDialog.collectAsStateWithLifecycle()
    val phienGiongNoi by vm.phienGiongNoi.collectAsStateWithLifecycle()
    val cauHinhGiongNoi by vm.cauHinhGiongNoi.collectAsStateWithLifecycle()
    val mucDungGiongNoi by vm.mucDungGiongNoi.collectAsStateWithLifecycle()

    var current by rememberSaveable { mutableStateOf(Dest.HOME.route) }
    var openRoom by rememberSaveable { mutableStateOf<String?>(null) }
    var showChiTietDien by rememberSaveable { mutableStateOf(false) }
    var showWifi by rememberSaveable { mutableStateOf(false) }

    val dest = Dest.from(current)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    // ---- Tro ly giong noi ----

    val ctx = LocalContext.current
    val xinQuyenMic = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { duoc ->
        if (duoc) vm.batTatGiongNoi()
        else toast("Chưa cấp quyền micro nên chưa nói chuyện được.")
    }

    // Giong noi doi man hinh. Dieu huong van nam o day chu khong chuyen vao
    // ViewModel: `current`/`openRoom` la chuyen cua khung hinh, va
    // rememberSaveable giu chung qua luc xoay may.
    LaunchedEffect(Unit) {
        vm.lenhDieuHuong.collect { lenh ->
            if (lenh.startsWith("phong:")) {
                openRoom = lenh.removePrefix("phong:")
                return@collect
            }
            openRoom = null
            showChiTietDien = false
            showWifi = false
            when (lenh) {
                "chi_tiet_dien" -> { current = Dest.STATS.route; showChiTietDien = true }
                "wifi" -> showWifi = true
                "trang_chu" -> current = Dest.HOME.route
                "thiet_bi" -> current = Dest.DEVICES.route
                "camera" -> current = Dest.CAMERA.route
                "cam_bien" -> current = Dest.SENSORS.route
                "tu_dong_hoa" -> current = Dest.AUTOMATION.route
                "cai_dat" -> current = Dest.SETTINGS.route
                "server" -> current = Dest.SERVER.route
                "thong_ke" -> current = Dest.STATS.route
                "tien_ich" -> current = Dest.UTILITIES.route
            }
        }
    }

    // Thong bao loi / qua han 6s cua cong tac that (tap hoac giong noi).
    LaunchedEffect(Unit) {
        vm.thongBao.collect { msg -> snackbar.showSnackbar(msg) }
    }

    // Nut Back cua may: thoat man con truoc, chua thoat app ngay.
    BackHandler(enabled = openRoom != null || showChiTietDien || showWifi || dest != Dest.HOME) {
        when {
            openRoom != null -> openRoom = null
            showChiTietDien -> showChiTietDien = false
            showWifi -> showWifi = false
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
                    showChiTietDien = false
                    showWifi = false
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
                        oDen = vm.oThietBi(room.id, "den", congTac, choCongTac),
                        oQuat = vm.oThietBi(room.id, "quat", congTac, choCongTac),
                        onToggleAc = { vm.toggleAc(room.id) },
                    )

                    showChiTietDien -> ChiTietDienScreen(
                        dien = dien,
                        onBack = { showChiTietDien = false },
                    )

                    showWifi -> WifiScreen(
                        wifi = wifi,
                        onBack = { showWifi = false },
                        onReboot = {
                            vm.rebootWifi()
                            toast("Đã gửi lệnh khởi động lại modem TP-Link!")
                        },
                    )

                    dest == Dest.SERVER -> ServerScreen(
                        server = server,
                        onBack = { current = Dest.HOME.route },
                    )

                    dest == Dest.SENSORS -> CamBienScreen(
                        cuktech = cuktech,
                        dangCho = choCuktech,
                        onDatCong = { cong, bat -> vm.datCongSac(cong, bat, toast) },
                        dangNoiLai = dangNoiLaiCuktech,
                        onNoiLai = { vm.noiLaiCuktech(toast) },
                        choCaiDat = choCaiDatCuktech,
                        onDatCaiDat = { piid, giaTri -> vm.datCaiDatCuktech(piid, giaTri, toast) },
                        onBack = { current = Dest.HOME.route },
                    )

                    dest == Dest.STATS -> StatsScreen(
                        state = state,
                        dien = dien,
                        ketNoi = ketNoi,
                        onBack = { current = Dest.HOME.route },
                        onKind = vm::setStatKind,
                        onRange = vm::setStatRange,
                        onXemChiTiet = { showChiTietDien = true },
                    )

                    dest == Dest.HOME -> HomeScreen(
                        state = state,
                        weather = thoiTiet,
                        wifi = wifi,
                        onSelectRoom = vm::selectRoom,
                        onOpenRoom = { id -> openRoom = id },
                        onQuick = vm::toggleQuick,
                        onOpenStats = { current = Dest.STATS.route },
                        onRefreshWeather = vm::taiLaiThoiTiet,
                        onOpenWifi = { showWifi = true },
                        onOpenLich = { current = Dest.UTILITIES.route },
                        congTac = congTac,
                    )

                    dest == Dest.SETTINGS -> SettingsScreen(
                        cauHinh = cauHinh,
                        ketNoi = ketNoi,
                        cauHinhCamera = cauHinhCamera,
                        trangThaiCam = trangThaiCam,
                        cauHinhThoiTiet = thoiTietCauHinh,
                        cauHinhGiongNoi = cauHinhGiongNoi,
                        mucDungGiongNoi = mucDungGiongNoi,
                        otaStatus = otaStatus,
                        currentVersion = vm.currentVersionName,
                        onLuu = vm::luuCauHinh,
                        onLuuCamera = vm::luuCauHinhCamera,
                        onLuuThoiTiet = vm::luuCauHinhThoiTiet,
                        onLuuGiongNoi = vm::luuCauHinhGiongNoi,
                        onKiemTraCapNhat = { vm.kiemTraCapNhat(thuCong = true) },
                        onBatDauCapNhat = vm::batDauCapNhat,
                        onBack = { current = Dest.HOME.route },
                    )

                    dest == Dest.UTILITIES -> TienIchScreen(
                        onBack = { current = Dest.HOME.route },
                    )

                    dest == Dest.CAMERA -> CameraScreen(
                        state = state,
                        cauHinhCamera = cauHinhCamera,
                        trangThaiCam = trangThaiCam,
                        onBack = { current = Dest.HOME.route },
                        onSelectCamera = vm::selectCamera,
                        onNotice = toast,
                        onDoLai = vm::doLaiCamera,
                    )

                    dest == Dest.DEVICES -> DevicesScreen(
                        state = state,
                        onOpenRoom = { id -> openRoom = id },
                        onBack = { current = Dest.HOME.route },
                        congTac = congTac,
                    )

                    dest == Dest.AUTOMATION -> AutomationScreen(
                        onBack = { current = Dest.HOME.route },
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

        // Nut mic noi tren moi man. Goc duoi ben PHAI: canh trai la cot menu,
        // canh duoi giua la snackbar.
        BangGiongNoi(
            phien = phienGiongNoi,
            onBatTat = {
                when {
                    phienGiongNoi.dangChay -> vm.dongGiongNoi()
                    !cauHinhGiongNoi.coKhoa() -> {
                        toast("Chưa khai báo khóa Gemini. Vào Cài đặt → Trợ lý giọng nói.")
                        current = Dest.SETTINGS.route
                    }
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED -> vm.batTatGiongNoi()

                    else -> xinQuyenMic.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onDong = { vm.dongGiongNoi(); vm.xoaLoiGiongNoi() },
            modifier = Modifier.align(Alignment.BottomEnd),
        )

        // Pop-up thong bao co ban cap nhat moi tren Git
        if (showUpdateDialog) {
            OtaUpdateDialog(
                status = otaStatus,
                onDismiss = vm::dongHopThoaiCapNhat,
                onStartUpdate = vm::batDauCapNhat,
            )
        }
    }
}
