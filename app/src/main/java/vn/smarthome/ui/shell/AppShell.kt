package vn.smarthome.ui.shell

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import vn.smarthome.data.baomat.ManChoLogic
import vn.smarthome.data.bomnuoc.CheDoBon
import vn.smarthome.ui.components.BangCanhBaoKhanCap
import vn.smarthome.ui.components.HopThoaiPinAdmin
import vn.smarthome.ui.components.HopThoaiXacNhan
import vn.smarthome.ui.components.ManHinhCho
import vn.smarthome.ui.components.PopupChaoKhuonMat
import vn.smarthome.ui.components.YeuCauXacNhan
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.smarthome.data.giongnoi.TrangThaiGiongNoi
import vn.smarthome.ui.components.BangThoaiGiongNoi
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
    val cameraRecorder by vm.cameraRecorder.collectAsStateWithLifecycle()
    val danhSachLuongGo2rtc by vm.danhSachLuongGo2rtc.collectAsStateWithLifecycle()
    val chaoMatHienTai by vm.chaoMatHienTai.collectAsStateWithLifecycle()
    val dangChupAnh by vm.dangChupAnh.collectAsStateWithLifecycle()
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
    val cauHinhBomNuoc by vm.cauHinhBomNuoc.collectAsStateWithLifecycle()
    val bomNuoc by vm.bomNuoc.collectAsStateWithLifecycle()
    val luongNuocThang by vm.luongNuocThang.collectAsStateWithLifecycle()
    val ketNoiBom by vm.ketNoiBom.collectAsStateWithLifecycle()
    val kenhBom by vm.kenhBom.collectAsStateWithLifecycle()
    val choBomNuoc by vm.choBomNuoc.collectAsStateWithLifecycle()
    val nhatKyBomNuoc by vm.nhatKyBomNuoc.collectAsStateWithLifecycle()
    val cauHinhBaoMat by vm.cauHinhBaoMat.collectAsStateWithLifecycle()
    val gasMqtt by vm.gasMqtt.collectAsStateWithLifecycle()
    val adminUnlocked by vm.adminUnlocked.collectAsStateWithLifecycle()

    var current by rememberSaveable { mutableStateOf(Dest.HOME.route) }
    var openRoom by rememberSaveable { mutableStateOf<String?>(null) }
    var showChiTietDien by rememberSaveable { mutableStateOf(false) }
    var showWifi by rememberSaveable { mutableStateOf(false) }
    var showManCho by rememberSaveable { mutableStateOf(false) }
    var lastTouchMs by remember { mutableStateOf(System.currentTimeMillis()) }
    var dismissedAlertKey by remember { mutableStateOf<String?>(null) }
    var pendingPinAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pendingConfirm by remember { mutableStateOf<YeuCauXacNhan?>(null) }

    val dest = Dest.from(current)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }

    LaunchedEffect(dest) {
        vm.datDangXemManBom(dest == Dest.AUTOMATION)
    }

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

    val canhBaoMuc = remember(bomNuoc, ketNoiBom, ketNoi, server) {
        tinhCanhBaoSidebar(
            bomNuoc = bomNuoc,
            ketNoiBom = ketNoiBom,
            ketNoiMqtt = ketNoi,
            server = server,
        )
    }

    val kitchenGasAlert = state.rooms.firstOrNull { it.id == "kitchen" }?.gasAlert == true
    val rawSuCo = remember(kitchenGasAlert, gasMqtt, bomNuoc, ketNoiBom, ketNoi) {
        ManChoLogic.kiemTraSuCoKhanCap(
            kitchenGasAlert = kitchenGasAlert,
            gasMqtt = gasMqtt,
            bomNuoc = bomNuoc,
            ketNoiBom = ketNoiBom,
            ketNoiMqtt = ketNoi,
        )
    }
    val suCoKhanCap = rawSuCo?.takeIf { it.key != dismissedAlertKey }

    LaunchedEffect(rawSuCo) {
        if (rawSuCo == null) dismissedAlertKey = null
    }

    LaunchedEffect(suCoKhanCap) {
        if (suCoKhanCap != null && showManCho) {
            showManCho = false
        }
    }

    LaunchedEffect(chaoMatHienTai) {
        if (chaoMatHienTai != null && showManCho) {
            lastTouchMs = System.currentTimeMillis()
            showManCho = false
        }
    }

    // Tu dong vao man hinh cho ban dem sau N phut khong cham
    LaunchedEffect(cauHinhBaoMat.manChoBat, cauHinhBaoMat.phutChoTuDong, showManCho, suCoKhanCap) {
        if (!cauHinhBaoMat.manChoBat || showManCho || suCoKhanCap != null) return@LaunchedEffect
        val idleLimitMs = cauHinhBaoMat.phutChoTuDong.coerceAtLeast(1) * 60_000L
        while (true) {
            delay(5_000L)
            if (System.currentTimeMillis() - lastTouchMs >= idleLimitMs) {
                showManCho = true
                break
            }
        }
    }

    // Danh thuc thong minh tu cam bien PIR hanh lang
    LaunchedEffect(Unit) {
        vm.pirMotion.collect {
            if (cauHinhBaoMat.danhThucPir) {
                lastTouchMs = System.currentTimeMillis()
                if (showManCho) {
                    showManCho = false
                    openRoom = null
                    showChiTietDien = false
                    showWifi = false
                    current = Dest.HOME.route
                }
            }
        }
    }

    val danhThucManHinh: () -> Unit = {
        lastTouchMs = System.currentTimeMillis()
        showManCho = false
        openRoom = null
        showChiTietDien = false
        showWifi = false
        current = Dest.HOME.route
    }

    val yeuCauPinAdmin: (String, () -> Unit) -> Unit = { moTa, hanhDong ->
        if (!cauHinhBaoMat.khoaPinBat || adminUnlocked) {
            hanhDong()
        } else {
            pinError = null
            pendingPinAction = moTa to hanhDong
        }
    }

    val yeuCauXacNhan: (String, String, String, Boolean, () -> Unit) -> Unit =
        { tieuDe, noiDung, nhanXacNhan, nguyHiem, hanhDong ->
            pendingConfirm = YeuCauXacNhan(
                tieuDe = tieuDe,
                noiDung = noiDung,
                nhanXacNhan = nhanXacNhan,
                nguyHiem = nguyHiem,
                hanhDong = hanhDong,
            )
        }

    BackHandler(enabled = showManCho) {
        danhThucManHinh()
    }

    val onBamTroLy: () -> Unit = {
        when {
            phienGiongNoi.trangThai is TrangThaiGiongNoi.DangTraLoi -> vm.ngatLoiGiongNoi()
            phienGiongNoi.dangChay -> vm.dongGiongNoi()
            !cauHinhGiongNoi.coKhoa() -> {
                toast("Chưa khai báo khóa Gemini. Vào Cài đặt → Trợ lý giọng nói.")
                openRoom = null
                showChiTietDien = false
                showWifi = false
                current = Dest.SETTINGS.route
            }
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED -> vm.batTatGiongNoi()

            else -> xinQuyenMic.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SH.Bg)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        if (ev.changes.any { it.pressed }) {
                            lastTouchMs = System.currentTimeMillis()
                        }
                    }
                }
            },
    ) {
        Row(Modifier.fillMaxSize()) {
            SideNav(
                current = dest,
                onSelect = { d ->
                    current = d.route
                    openRoom = null
                    showChiTietDien = false
                    showWifi = false
                },
                phienGiongNoi = phienGiongNoi,
                onBamTroLy = onBamTroLy,
                canhBaoMuc = canhBaoMuc,
                onBamManCho = { showManCho = true },
            )

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(Dim.MainPadding),
            ) {
                if (suCoKhanCap != null) {
                    BangCanhBaoKhanCap(
                        suCo = suCoKhanCap,
                        onXem = {
                            openRoom = null
                            showChiTietDien = false
                            showWifi = false
                            current = when (suCoKhanCap.routeDich) {
                                "sensors" -> Dest.SENSORS.route
                                "automation" -> Dest.AUTOMATION.route
                                "server" -> Dest.SERVER.route
                                else -> Dest.HOME.route
                            }
                        },
                        onAn = { dismissedAlertKey = suCoKhanCap.key },
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Box(Modifier.weight(1f).fillMaxSize()) {
                val room = state.rooms.firstOrNull { it.id == openRoom }

                when {
                    room != null -> RoomDetailScreen(
                        room = room,
                        onBack = { openRoom = null },
                        onHome = { openRoom = null; current = Dest.HOME.route },
                        onToggleLight = {
                            if (congTac.laThat(room.id, "den")) {
                                val tb = congTac.tim(room.id, "den")
                                val dangBat = congTac.trangThaiCua(room.id, "den")?.bat == true
                                val dongTu = if (dangBat) "Tắt" else "Bật"
                                yeuCauXacNhan(
                                    "$dongTu ${tb?.ten ?: "đèn thật"}?",
                                    "Thao tác này sẽ gửi lệnh MQTT điều khiển công tắc vật lý trong nhà.",
                                    dongTu,
                                    false,
                                ) { vm.toggleLight(room.id) }
                            } else {
                                vm.toggleLight(room.id)
                            }
                        },
                        onToggleFan = {
                            if (congTac.laThat(room.id, "quat")) {
                                val tb = congTac.tim(room.id, "quat")
                                val dangBat = congTac.trangThaiCua(room.id, "quat")?.bat == true
                                val dongTu = if (dangBat) "Tắt" else "Bật"
                                yeuCauXacNhan(
                                    "$dongTu ${tb?.ten ?: "quạt thật"}?",
                                    "Thao tác này sẽ gửi lệnh MQTT điều khiển công tắc vật lý trong nhà.",
                                    dongTu,
                                    false,
                                ) { vm.toggleFan(room.id) }
                            } else {
                                vm.toggleFan(room.id)
                            }
                        },
                        onAcTemp = { d -> vm.changeAcTemp(room.id, d) },
                        onMode = { m -> vm.setMode(room.id, m) },
                        onToggleSchedule = { vm.toggleSchedule(room.id) },
                        oDen = vm.oThietBi(room.id, "den", congTac, choCongTac),
                        oQuat = vm.oThietBi(room.id, "quat", congTac, choCongTac),
                        onToggleAc = { vm.toggleAc(room.id) },
                        onLightBrightness = { b -> vm.setLightBrightness(room.id, b) },
                        onFanSpeed = { s -> vm.setFanSpeed(room.id, s) },
                        onToggleTv = { vm.toggleTv(room.id) },
                        onToggleCurtain = { vm.toggleCurtain(room.id) },
                        onCurtainOpen = { p -> vm.setCurtain(room.id, p) },
                        onHoodSpeed = { s -> vm.setHoodSpeed(room.id, s) },
                        onToggleStove = {
                            val dangBat = room.stoveOn
                            yeuCauXacNhan(
                                if (dangBat) "Tắt bếp từ?" else "Bật bếp từ?",
                                if (dangBat) "Xác nhận ngắt nguồn bếp từ nhà bếp."
                                else "Cảnh báo an toàn nhiệt: xác nhận bật bếp từ nhà bếp.",
                                if (dangBat) "Tắt bếp" else "Bật bếp",
                                !dangBat,
                            ) { vm.toggleStove(room.id) }
                        },
                        onStoveLevel = { d -> vm.changeStoveLevel(room.id, d) },
                        onGasAlert = { a -> vm.setGasAlert(room.id, a) },
                        onHallView = { v -> vm.setHallView(room.id, v) },
                        onToggleHallFrontLight = { vm.toggleHallFrontLight(room.id) },
                        onHallFrontBrightness = { b -> vm.setHallFrontBrightness(room.id, b) },
                        onToggleHallSurroundingLight = { vm.toggleHallSurroundingLight(room.id) },
                        onHallSurroundingBrightness = { b -> vm.setHallSurroundingBrightness(room.id, b) },
                        onToggleHallAllLights = { vm.toggleHallAllLights(room.id) },
                        onToggleHallLock = {
                            val dangKhoa = room.hallDoorLocked
                            yeuCauXacNhan(
                                if (dangKhoa) "Mở khóa cửa chính?" else "Khóa cửa chính?",
                                if (dangKhoa) "Cảnh báo an ninh: xác nhận mở khóa cửa chính hành lang."
                                else "Xác nhận khóa cửa chính hành lang.",
                                if (dangKhoa) "Mở khóa" else "Khóa cửa",
                                dangKhoa,
                            ) { vm.toggleHallLock(room.id) }
                        },
                        onToggleHallCamera = { vm.toggleHallCamera(room.id) },
                        onToggleHallIr = { vm.toggleHallIr(room.id) },
                        onToggleHallMotionAuto = { vm.toggleHallMotionAuto(room.id) },
                        onTriggerHallMotion = { vm.triggerHallMotion(room.id) },
                        onDismissHallMotion = { vm.clearHallMotion(room.id) },
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
                        onYeuCauPinAdmin = yeuCauPinAdmin,
                    )

                    dest == Dest.SERVER -> ServerScreen(
                        server = server,
                        onBack = { current = Dest.HOME.route },
                    )

                    dest == Dest.SENSORS -> CamBienScreen(
                        cuktech = cuktech,
                        dangCho = choCuktech,
                        onDatCong = { cong, bat ->
                            val dongTu = if (bat) "Bật" else "Tắt"
                            yeuCauXacNhan(
                                "$dongTu nguồn cổng sạc ${cong.nhan}?",
                                if (bat) "Cấp điện trở lại cho cổng ${cong.nhan} trên củ sạc CUKTECH 10 Ultra."
                                else "Ngắt điện cổng ${cong.nhan} ngay lập tức — thiết bị đang cắm sẽ ngừng sạc.",
                                "$dongTu cổng",
                                !bat,
                            ) { vm.datCongSac(cong, bat, toast) }
                        },
                        dangNoiLai = dangNoiLaiCuktech,
                        onNoiLai = {
                            yeuCauXacNhan(
                                "Nối lại Bluetooth củ sạc?",
                                "Máy chủ sẽ ngắt và bắt tay lại Bluetooth với CUKTECH 10 Ultra (mất khoảng 40–90 giây).",
                                "Nối lại",
                                false,
                            ) { vm.noiLaiCuktech(toast) }
                        },
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
                        bomNuoc = bomNuoc,
                        luongNuocThang = luongNuocThang,
                        ketNoiBom = ketNoiBom,
                        kenhBom = kenhBom,
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
                        choCongTac = choCongTac,
                        onRoomQuickAction = { roomId, actionId ->
                            val r = state.rooms.firstOrNull { it.id == roomId }
                            when {
                                actionId == "light" && congTac.laThat(roomId, "den") -> {
                                    val tb = congTac.tim(roomId, "den")
                                    val dangBat = congTac.trangThaiCua(roomId, "den")?.bat == true
                                    val dongTu = if (dangBat) "Tắt" else "Bật"
                                    yeuCauXacNhan(
                                        "$dongTu ${tb?.ten ?: "đèn thật"}?",
                                        "Xác nhận gửi lệnh MQTT điều khiển công tắc vật lý.",
                                        dongTu,
                                        false,
                                    ) { vm.thaoTacNhanhPhong(roomId, actionId) }
                                }
                                (actionId == "fan" || actionId == "hood") && congTac.laThat(roomId, "quat") -> {
                                    val tb = congTac.tim(roomId, "quat")
                                    val dangBat = congTac.trangThaiCua(roomId, "quat")?.bat == true
                                    val dongTu = if (dangBat) "Tắt" else "Bật"
                                    yeuCauXacNhan(
                                        "$dongTu ${tb?.ten ?: "quạt thật"}?",
                                        "Xác nhận gửi lệnh MQTT điều khiển công tắc vật lý.",
                                        dongTu,
                                        false,
                                    ) { vm.thaoTacNhanhPhong(roomId, actionId) }
                                }
                                actionId == "hall_lock" && r != null -> {
                                    val dangKhoa = r.hallDoorLocked
                                    yeuCauXacNhan(
                                        if (dangKhoa) "Mở khóa cửa chính?" else "Khóa cửa chính?",
                                        if (dangKhoa) "Cảnh báo an ninh: xác nhận mở khóa cửa chính hành lang."
                                        else "Xác nhận khóa cửa chính hành lang.",
                                        if (dangKhoa) "Mở khóa" else "Khóa cửa",
                                        dangKhoa,
                                    ) { vm.thaoTacNhanhPhong(roomId, actionId) }
                                }
                                actionId == "stove" && r != null -> {
                                    val dangBat = r.stoveOn
                                    yeuCauXacNhan(
                                        if (dangBat) "Tắt bếp từ?" else "Bật bếp từ?",
                                        if (dangBat) "Xác nhận ngắt nguồn bếp từ nhà bếp."
                                        else "Cảnh báo an toàn nhiệt: xác nhận bật bếp từ nhà bếp.",
                                        if (dangBat) "Tắt bếp" else "Bật bếp",
                                        !dangBat,
                                    ) { vm.thaoTacNhanhPhong(roomId, actionId) }
                                }
                                else -> vm.thaoTacNhanhPhong(roomId, actionId)
                            }
                        },
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
                        onLuu = { c -> yeuCauPinAdmin("Lưu cấu hình Broker MQTT") { vm.luuCauHinh(c) } },
                        onLuuCamera = { c -> yeuCauPinAdmin("Lưu cấu hình Camera go2rtc") { vm.luuCauHinhCamera(c) } },
                        onLuuThoiTiet = vm::luuCauHinhThoiTiet,
                        onLuuGiongNoi = { c -> yeuCauPinAdmin("Lưu khóa API Trợ lý giọng nói") { vm.luuCauHinhGiongNoi(c) } },
                        onKiemTraCapNhat = { vm.kiemTraCapNhat(thuCong = true) },
                        onBatDauCapNhat = { r ->
                            yeuCauPinAdmin("Tải và cài đặt bản cập nhật OTA ${r.tagName}") {
                                vm.batDauCapNhat(r)
                            }
                        },
                        onBack = { current = Dest.HOME.route },
                        cauHinhBomNuoc = cauHinhBomNuoc,
                        ketNoiBom = ketNoiBom,
                        kenhBom = kenhBom,
                        bomNuoc = bomNuoc,
                        onLuuBomNuoc = { c -> yeuCauPinAdmin("Lưu cấu hình bo bơm nước") { vm.luuCauHinhBomNuoc(c) } },
                        cauHinhBaoMat = cauHinhBaoMat,
                        adminUnlocked = adminUnlocked,
                        onLuuBaoMat = { c ->
                            yeuCauPinAdmin("Lưu cài đặt màn hình chờ & cảm biến an ninh") {
                                vm.luuCauHinhBaoMat(c)
                            }
                        },
                        onDoiPinAdmin = vm::doiPinAdmin,
                        onDatBatTatPin = vm::datBatTatKhoaPin,
                        onKhoaAdminNgay = vm::lockAdminNow,
                        onXemThuManCho = { showManCho = true },
                        onYeuCauPinAdmin = yeuCauPinAdmin,
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
                        cameraRecorder = cameraRecorder,
                        danhSachLuongGo2rtc = danhSachLuongGo2rtc,
                        dangChupAnh = dangChupAnh,
                        onChupAnh = vm::chupAnhCamera,
                        onDatCanhNguoi = vm::datCanhNguoiCamera,
                        onPtzMove = vm::dieuKhienPtzCamera,
                        onPtzPreset = vm::goiPresetCamera,
                        onDatRiengTu = vm::datCheDoRiengTuCamera,
                        onDatCheDoNha = vm::datCheDoNhaCamera,
                        onDocThuChao = vm::docThuGiongChao,
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
                        bomNuoc = bomNuoc,
                        luongNuocThang = luongNuocThang,
                        ketNoiBom = ketNoiBom,
                        kenhBom = kenhBom,
                        choBomNuoc = choBomNuoc,
                        cauHinhBom = cauHinhBomNuoc,
                        nhatKyBom = nhatKyBomNuoc,
                        onDatCheDoBon = { mode ->
                            val cheDo = CheDoBon.tuChuoi(mode)
                            yeuCauXacNhan(
                                "Chuyển bơm lên bồn sang ${cheDo.nhan}?",
                                when (mode) {
                                    "on" -> "Bơm chính sẽ chạy liên tục bằng tay (phao đầy vẫn tự cắt chống tràn). Xác nhận gửi lệnh xuống bo AquaControl?"
                                    "off" -> "Ngừng bơm lên bồn cho tới khi đổi lại chế độ. Xác nhận gửi lệnh?"
                                    else -> "Chuyển bơm lên bồn về chế độ tự động theo phao bồn."
                                },
                                "Gửi lệnh",
                                mode == "on",
                            ) { vm.datCheDoBon(mode) }
                        },
                        onDatBomTangAp = { bat ->
                            yeuCauXacNhan(
                                if (bat) "Bật bơm tăng áp?" else "Tắt bơm tăng áp?",
                                if (bat) "Bơm tăng áp sẽ chạy ngay (tự ngắt sau 15 phút). Xác nhận bật?"
                                else "Ngừng hoạt động bơm tăng áp ngay lập tức.",
                                if (bat) "Bật bơm áp" else "Tắt bơm áp",
                                bat,
                            ) { vm.datBomTangAp(bat) }
                        },
                        onDatDenPhuBom = vm::datDenPhuBom,
                        onXoaLoiBom = {
                            yeuCauXacNhan(
                                "Xoá lỗi và mở khoá bảo vệ bơm?",
                                "Cảnh báo: chỉ mở khoá sau khi đã kiểm tra nguồn nước, lưu lượng và nhiệt độ bơm an toàn.",
                                "Xoá lỗi & Mở khoá",
                                true,
                            ) { vm.xoaLoiBom() }
                        },
                        onTatCoiBom = vm::tatCoiBom,
                        onThuCoiBom = vm::thuCoiBom,
                        onLuuBaoVeBom = vm::luuBaoVeBom,
                        onGhiMocCanBom = vm::ghiMocCanBom,
                        onGhiMocDayBom = vm::ghiMocDayBom,
                        onDatLaiDemLuuLuong = vm::datLaiDemLuuLuong,
                        onXoaLichSuNuocTrenBo = vm::xoaLichSuNuocTrenBo,
                        onXoaNhatKyBomCucBo = vm::xoaNhatKyBomCucBo,
                        onLuuCauHinhBom = vm::luuCauHinhBomNuoc,
                        onYeuCauPinAdmin = yeuCauPinAdmin,
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
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))

        // Bang thoai tro ly AI chi hien khi dang noi chuyen hoac co loi.
        // Nut bam co dinh da nam o day Sidebar ben trai, khong che noi dung ben phai.
        BangThoaiGiongNoi(
            phien = phienGiongNoi,
            onDong = { vm.dongGiongNoi(); vm.xoaLoiGiongNoi() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dim.SidebarWidth + Dim.MainPadding, bottom = Dim.MainPadding),
        )

        // Pop-up thong bao co ban cap nhat moi tren Git (yeu cau PIN truoc khi cai dat)
        if (showUpdateDialog) {
            OtaUpdateDialog(
                status = otaStatus,
                onDismiss = vm::dongHopThoaiCapNhat,
                onStartUpdate = { r ->
                    yeuCauPinAdmin("Tải và cài đặt bản cập nhật OTA ${r.tagName}") {
                        vm.batDauCapNhat(r)
                    }
                },
            )
        }

        // Hop thoai xac nhan thao tac nhay cam (bom AquaControl, cong tac that, khoa cua...)
        val confirmReq = pendingConfirm
        if (confirmReq != null) {
            HopThoaiXacNhan(
                yeuCau = confirmReq,
                onXacNhan = {
                    val action = confirmReq.hanhDong
                    pendingConfirm = null
                    action()
                },
                onHuy = { pendingConfirm = null },
            )
        }

        // Hop thoai nhap PIN 4 so cho khu vuc Ky thuat
        val pinReq = pendingPinAction
        if (pinReq != null) {
            HopThoaiPinAdmin(
                moTaHanhDong = pinReq.first,
                loi = pinError,
                onXacNhan = { pin ->
                    val err = vm.verifyAdminPin(pin)
                    if (err == null) {
                        pinError = null
                        val action = pinReq.second
                        pendingPinAction = null
                        action()
                    } else {
                        pinError = err
                    }
                },
                onDong = {
                    pendingPinAction = null
                    pinError = null
                },
            )
        }

        // Banner 5s chao nguoi quen / Popup 10s anh chup khach la (`camera/face/greet`)
        val chaoMat = chaoMatHienTai
        if (chaoMat != null && !showManCho) {
            PopupChaoKhuonMat(
                ev = chaoMat.first,
                cauHinhCamera = cauHinhCamera,
                congApi = cameraRecorder.apiPort,
                onMoCamera = {
                    vm.anPopupChaoMat()
                    openRoom = null
                    showChiTietDien = false
                    showWifi = false
                    current = Dest.CAMERA.route
                },
                onDong = vm::anPopupChaoMat,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = Dim.MainPadding, end = Dim.MainPadding),
            )
        }

        // Che do Man hinh cho ban dem (Ambient Night Clock)
        if (showManCho) {
            ManHinhCho(
                thoiTiet = thoiTiet,
                dien = dien,
                bomNuoc = bomNuoc,
                ketNoiBom = ketNoiBom,
                cauHinhBom = cauHinhBomNuoc,
                cauHinhBaoMat = cauHinhBaoMat,
                onDanhThuc = danhThucManHinh,
            )
        }
    }
}
