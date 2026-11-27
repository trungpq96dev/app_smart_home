package vn.smarthome.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.CameraFeed
import vn.smarthome.data.camera.CameraPlaybackApi
import vn.smarthome.data.camera.CameraRecorderState
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.DoanVideo
import vn.smarthome.data.camera.DongThoiGianNgay
import vn.smarthome.data.camera.NgayCoVideo
import vn.smarthome.data.camera.PtzPreset
import vn.smarthome.data.camera.SuKienCamera
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.camera.dinhDangGioVn
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.FlatButton
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.KhungQuanLyKhuonMat
import vn.smarthome.ui.components.LuongCamera
import vn.smarthome.ui.components.MucZoomTimeline
import vn.smarthome.ui.components.RoundIconButton
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.components.ThanhThoiGianCamera
import vn.smarthome.ui.components.TrinhPhatXemLai
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.shell.rememberCameraStamp
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

private val ZONA_VN: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

private enum class CheDoManCam { TRUC_TIEP, SU_KIEN, XEM_LAI, KHUON_MAT }

/**
 * Man camera an ninh & Dau ghi thong minh 24/24 (Imou / EZVIZ style):
 *
 *  - Che do `Trực tiếp`: xem luong WebRTC tu go2rtc (uu tien `_sd` H.264 nhe RAM),
 *    dieu khien xoay 360° PTZ + 4 Preset goc quay, chup anh that, bat/tat AI canh nguoi
 *    va Che do Rieng tu 1 cham (xoay up ong kinh + ngat luong).
 *  - Che do `Sự kiện`: tab Mini-NVR xem lai clip 25s (`ffmpeg -c copy`) theo bo loc
 *    Tat ca / Nguoi la / Nguoi quen / Co nguoi.
 *  - Che do `Xem lại`: xem lai dong thoi gian 24/24 (Hot Cache 10 GB + Google Drive 5TB),
 *    keo thuoc thoi gian co vach cam phat hien nguoi, va bam nhay thang toi tung su kien.
 */
@Composable
fun CameraScreen(
    state: HomeState,
    cauHinhCamera: CauHinhCamera,
    trangThaiCam: TrangThaiCam,
    onBack: () -> Unit,
    onSelectCamera: (Int) -> Unit,
    onNotice: (String) -> Unit,
    onDoLai: () -> Unit,
    cameraRecorder: CameraRecorderState = CameraRecorderState(),
    danhSachLuongGo2rtc: List<String> = emptyList(),
    dangChupAnh: Boolean = false,
    onChupAnh: (String) -> Unit = {},
    onDatCanhNguoi: (Boolean) -> Unit = {},
    onPtzMove: (String, String) -> Unit = { _, _ -> },
    onPtzPreset: (String, String) -> Unit = { _, _ -> },
    onDatRiengTu: (Boolean) -> Unit = {},
    onDatCheDoNha: (String) -> Unit = {},
) {
    val cam = state.cameras.getOrNull(state.selectedCamera) ?: state.cameras.firstOrNull()
    val stamp = rememberCameraStamp()
    val luongThat = cam?.luong?.takeIf { cauHinhCamera.dayDu() }
    val luongWebView = luongThat?.let { cauHinhCamera.tenLuongWebView(it, danhSachLuongGo2rtc) }

    var lanTai by remember { mutableIntStateOf(0) }
    var cheDo by rememberSaveable { mutableStateOf(CheDoManCam.TRUC_TIEP) }
    var hienBangPtz by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Camera an ninh", onBack = null) {
            // Cong tac chuyen nhanh Trực tiếp | Sự kiện | Xem lại
            NutChuyenCheDoCam(
                cheDo = cheDo,
                onChon = { cheDo = it },
            )
            Spacer(Modifier.width(6.dp))
            // Nut chuyen che do Ở nhà / Vắng nhà (dieu khien canh bao nguoi la)
            val dangVangNha = cameraRecorder.homeMode.equals("away", ignoreCase = true)
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (dangVangNha) SH.Orange.tint(0.18f) else SH.Card)
                    .border(
                        Dim.BorderWidth,
                        if (dangVangNha) SH.Orange else SH.Border,
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { onDatCheDoNha(if (dangVangNha) "home" else "away") }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    if (dangVangNha) Icons.Filled.Lock else Icons.Filled.Home,
                    contentDescription = null,
                    tint = if (dangVangNha) SH.Orange else SH.Green,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    if (dangVangNha) "Vắng nhà" else "Ở nhà",
                    style = SHType.Caption,
                    color = if (dangVangNha) SH.Orange else SH.Text,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(6.dp))
            Rec247StatusPill(cameraRecorder, luongThat, trangThaiCam)
            Spacer(Modifier.width(6.dp))
            RoundIconButton(Icons.Filled.Refresh, { onDoLai(); lanTai++ })
            RoundIconButton(
                Icons.Filled.Settings,
                { onNotice("Khai báo go2rtc và cổng đầu ghi ở màn Cài đặt.") },
            )
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------- Cot trai: Danh sach camera + Thong ke dau ghi 24/24 ----------
            Column(
                Modifier.width(158.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.cameras.isEmpty()) {
                    KhongCoCamera(trangThaiCam, Modifier.fillMaxWidth())
                } else {
                    state.cameras.forEachIndexed { i, c ->
                        CameraRow(
                            feed = c,
                            selected = i == state.selectedCamera,
                            dangGhi = cameraRecorder.camDangGhi(c.luong),
                            biAnRiengTu = cameraRecorder.camBiAnRiengTu(c.luong),
                            onClick = { onSelectCamera(i) },
                            modifier = Modifier.fillMaxWidth().height(58.dp),
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                TheThongKeDauGhi(
                    rec = cameraRecorder,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ---------- Vung chinh ben phai ----------
            when (cheDo) {
                CheDoManCam.TRUC_TIEP -> KhungCheDoTrucTiep(
                    cam = cam,
                    luongThat = luongThat,
                    luongWebView = luongWebView,
                    cauHinhCamera = cauHinhCamera,
                    trangThaiCam = trangThaiCam,
                    cameraRecorder = cameraRecorder,
                    stamp = stamp,
                    lanTai = lanTai,
                    dangChupAnh = dangChupAnh,
                    hienBangPtz = hienBangPtz,
                    onDaoBangPtz = { hienBangPtz = !hienBangPtz },
                    onPtzMove = { dir ->
                        if (luongThat != null) onPtzMove(luongThat, dir)
                        else onNotice(CHUA_NOI)
                    },
                    onPtzPreset = { preset ->
                        if (luongThat != null) onPtzPreset(luongThat, preset)
                        else onNotice(CHUA_NOI)
                    },
                    onDaoRiengTu = { onDatRiengTu(!cameraRecorder.privacyMode) },
                    onTaiLai = { lanTai++ },
                    onChupAnh = {
                        if (luongThat != null) onChupAnh(luongThat)
                        else onNotice(CHUA_NOI)
                    },
                    onDaoCanhNguoi = {
                        val hienTai = cameraRecorder.detectPersonEnabled ?: true
                        onDatCanhNguoi(!hienTai)
                    },
                    onNotice = onNotice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                CheDoManCam.SU_KIEN -> KhungCheDoSuKien(
                    luongThat = luongThat,
                    cauHinhCamera = cauHinhCamera,
                    cameraRecorder = cameraRecorder,
                    lanTai = lanTai,
                    onNotice = onNotice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                CheDoManCam.XEM_LAI -> KhungCheDoXemLai(
                    tenCamHienThi = cam?.name ?: "Camera",
                    luongThat = luongThat,
                    cauHinhCamera = cauHinhCamera,
                    cameraRecorder = cameraRecorder,
                    lanTai = lanTai,
                    onNotice = onNotice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )

                CheDoManCam.KHUON_MAT -> KhungQuanLyKhuonMat(
                    luongThat = luongThat,
                    cauHinhCamera = cauHinhCamera,
                    cameraRecorder = cameraRecorder,
                    lanTai = lanTai,
                    onNotice = onNotice,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
    }
}

private const val CHUA_NOI =
    "Chưa khai báo go2rtc — vào màn Cài đặt điền địa chỉ máy chạy go2rtc."

// ============================================================ CHE DO TRUC TIEP

@Composable
private fun KhungCheDoTrucTiep(
    cam: CameraFeed?,
    luongThat: String?,
    luongWebView: String?,
    cauHinhCamera: CauHinhCamera,
    trangThaiCam: TrangThaiCam,
    cameraRecorder: CameraRecorderState,
    stamp: String,
    lanTai: Int,
    dangChupAnh: Boolean,
    hienBangPtz: Boolean,
    onDaoBangPtz: () -> Unit,
    onPtzMove: (String) -> Unit,
    onPtzPreset: (String) -> Unit,
    onDaoRiengTu: () -> Unit,
    onTaiLai: () -> Unit,
    onChupAnh: () -> Unit,
    onDaoCanhNguoi: () -> Unit,
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dangCanhNguoi = cameraRecorder.detectPersonEnabled
    val nhanCanhNguoi = when (dangCanhNguoi) {
        true -> "Canh người: Bật"
        false -> "Canh người: Tắt"
        null -> "Canh người"
    }
    val biAnRiengTu = cameraRecorder.camBiAnRiengTu(luongThat)
    var dsPreset by remember(luongThat) { mutableStateOf(PtzPreset.MAC_DINH) }

    LaunchedEffect(luongThat, hienBangPtz, cameraRecorder.apiPort) {
        if (hienBangPtz && luongThat != null && cauHinhCamera.dayDu()) {
            CameraPlaybackApi.taiPtzPresets(cauHinhCamera, luongThat, cameraRecorder.apiPort)
                .onSuccess { dsPreset = it }
        }
    }

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when {
                    biAnRiengTu -> {
                        // Che do Rieng tu: NGAT HAN WebView luong camera va hien man chan bao ve
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(Dim.CardRadius))
                                .background(SH.Card)
                                .border(1.5.dp, SH.Purple, RoundedCornerShape(Dim.CardRadius)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Icon(
                                    Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = SH.Purple,
                                    modifier = Modifier.size(40.dp),
                                )
                                Text(
                                    "Chế độ Riêng tư đang bật",
                                    style = SHType.CardTitle,
                                    color = SH.Text,
                                )
                                Text(
                                    "Ống kính camera trong nhà đã xoay úp vào tường và tạm ngắt luồng hiển thị.",
                                    style = SHType.Caption,
                                    color = SH.TextDim,
                                    textAlign = TextAlign.Center,
                                )
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(SH.Purple)
                                        .clickable(onClick = onDaoRiengTu)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                ) {
                                    Text(
                                        "Tắt Chế độ Riêng tư",
                                        style = SHType.Label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    luongThat != null && luongWebView != null -> {
                        key(luongWebView, lanTai) {
                            LuongCamera(
                                url = cauHinhCamera.urlTrangPhat(luongWebView),
                                taiKhoan = cauHinhCamera.taiKhoan,
                                matKhau = cauHinhCamera.matKhau,
                                onLoi = onNotice,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(Dim.CardRadius)),
                            )
                        }
                        val phuText = buildString {
                            append("go2rtc · ").append(cauHinhCamera.host)
                            if (luongWebView != luongThat) append(" · SD")
                            if (cameraRecorder.camDangGhi(luongThat)) {
                                append(" · Đang ghi 24/24")
                            }
                        }
                        PhuDe(
                            ten = cam?.name ?: luongThat,
                            phu = phuText,
                            stamp = stamp,
                            trucTiep = true,
                            suKienGanNhat = cameraRecorder.nguoiGanNhat
                                ?.takeIf { it.camera == luongThat }
                                ?.let { "Người gần nhất: ${it.thoiGian.takeLast(8)}" },
                        )
                    }

                    cam != null -> {
                        ImagePlaceholder(
                            icon = cam.icon,
                            label = cam.name,
                            accent = cam.accent,
                            iconSize = 54.dp,
                            cornerRadius = Dim.CardRadius,
                            showLabel = false,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            PhuDe(
                                ten = cam.name,
                                phu = "chưa nối luồng hình",
                                stamp = stamp,
                                trucTiep = false,
                            )
                        }
                    }

                    else -> KhungTrong(trangThaiCam, Modifier.fillMaxSize())
                }
            }

            if (hienBangPtz && !biAnRiengTu) {
                BangDieuKhienPtz(
                    presets = dsPreset,
                    onMove = onPtzMove,
                    onPreset = onPtzPreset,
                    modifier = Modifier.width(178.dp).fillMaxHeight(),
                )
            }
        }

        Spacer(Modifier.height(Dim.CardGap))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlatButton(
                Icons.Filled.PhotoCamera,
                if (dangChupAnh) "Đang chụp..." else "Chụp ảnh",
                onChupAnh,
                Modifier.weight(1f),
            )
            FlatButton(
                if (dangCanhNguoi == false) Icons.Filled.NotificationsOff else Icons.Filled.NotificationsActive,
                nhanCanhNguoi,
                onDaoCanhNguoi,
                Modifier.weight(1f),
            )
            FlatButton(
                Icons.Filled.ControlCamera,
                if (hienBangPtz) "Ẩn PTZ" else "Xoay PTZ",
                onDaoBangPtz,
                Modifier.weight(1f),
            )
            FlatButton(
                Icons.Filled.VisibilityOff,
                if (cameraRecorder.privacyMode) "Tắt Riêng tư" else "Riêng tư",
                onDaoRiengTu,
                Modifier.weight(1f),
            )
            FlatButton(
                Icons.Filled.Refresh,
                "Tải lại",
                onTaiLai,
                Modifier.weight(0.85f),
            )
        }
    }
}

/** Bang dieu khien xoay 360° ONVIF (4 huong + Dung) & 4 nut Preset goc quay. */
@Composable
private fun BangDieuKhienPtz(
    presets: List<PtzPreset>,
    onMove: (String) -> Unit,
    onPreset: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(Dim.CardRadius))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Xoay 360° (PTZ)", style = SHType.Label, color = SH.Text)

        // D-pad 4 huong + Nut Dung o tam
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            NutPtzHuong(Icons.Filled.KeyboardArrowUp, "Xoay lên") { onMove("up") }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                NutPtzHuong(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Xoay trái") { onMove("left") }
                NutPtzHuong(Icons.Filled.Stop, "Dừng xoay", noiBat = true) { onMove("stop") }
                NutPtzHuong(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Xoay phải") { onMove("right") }
            }
            NutPtzHuong(Icons.Filled.KeyboardArrowDown, "Xoay xuống") { onMove("down") }
        }

        Spacer(Modifier.height(2.dp))
        Text("Góc quay nhanh", style = SHType.Caption, color = SH.TextDim)

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            presets.take(4).forEach { p ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Card2)
                        .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(8.dp))
                        .clickable { onPreset(p.id) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        p.label,
                        style = SHType.Caption,
                        color = SH.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun NutPtzHuong(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    moTa: String,
    noiBat: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (noiBat) SH.Blue.tint(0.22f) else SH.Card2)
            .border(Dim.BorderWidth, if (noiBat) SH.Blue else SH.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = moTa, tint = if (noiBat) SH.Blue else SH.Text, modifier = Modifier.size(20.dp))
    }
}

// ============================================================ CHE DO SU KIEN (MINI-NVR)

@Composable
private fun KhungCheDoSuKien(
    luongThat: String?,
    cauHinhCamera: CauHinhCamera,
    cameraRecorder: CameraRecorderState,
    lanTai: Int,
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val congApi = cameraRecorder.apiPort
    var boLoc by rememberSaveable { mutableStateOf("all") }
    var dsSuKien by remember { mutableStateOf<List<SuKienCamera>>(emptyList()) }
    var dangTai by remember { mutableStateOf(false) }
    var loi by remember { mutableStateOf<String?>(null) }
    var suKienChon by remember { mutableStateOf<SuKienCamera?>(null) }
    var nhipTai by remember { mutableIntStateOf(0) }
    var dangPhat by remember { mutableStateOf(true) }
    var seekVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(luongThat, boLoc, lanTai, nhipTai, congApi) {
        if (!cauHinhCamera.dayDu()) {
            loi = CHUA_NOI
            dsSuKien = emptyList()
            return@LaunchedEffect
        }
        dangTai = true
        loi = null
        CameraPlaybackApi.taiDanhSachSuKien(cauHinhCamera, luongThat, boLoc, congApi)
            .onSuccess { ds ->
                dsSuKien = ds
                if (suKienChon == null || ds.none { it.id == suKienChon?.id }) {
                    suKienChon = ds.firstOrNull()
                }
            }
            .onFailure { e ->
                loi = CameraPlaybackApi.dienGiaiLoi(e, congApi ?: cauHinhCamera.congGhiHinh)
                dsSuKien = emptyList()
            }
        dangTai = false
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)) {
        // Cot giua: Bo loc + Trinh phat clip 25s Mini-NVR
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(
                        "all" to "Tất cả",
                        "face_unknown" to "Người lạ",
                        "face_known" to "Người quen",
                        "person" to "Có người",
                    ).forEach { (k, nhan) ->
                        val chon = boLoc == k
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (chon) SH.Blue else SH.Card)
                                .border(Dim.BorderWidth, if (chon) SH.Blue else SH.Border, RoundedCornerShape(7.dp))
                                .clickable { boLoc = k }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text(
                                nhan,
                                style = SHType.Caption,
                                color = if (chon) Color.White else SH.TextDim,
                                fontWeight = if (chon) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
                NutNho(Icons.Filled.Refresh, "Tải lại sự kiện") { nhipTai++ }
            }

            val ev = suKienChon
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(Dim.CardRadius))
                    .background(SH.Card)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    dangTai && dsSuKien.isEmpty() ->
                        CircularProgressIndicator(color = SH.Blue, modifier = Modifier.size(28.dp))

                    loi != null -> KhungThongBaoXemLai("Chưa tải được danh sách sự kiện", loi ?: "")

                    ev == null -> KhungThongBaoXemLai(
                        "Chưa có sự kiện nào",
                        "Mini-NVR sẽ tự ghi clip 25 giây (-c copy) khi phát hiện người lạ hoặc chuyển động có người.",
                    )

                    ev.hasClip -> {
                        TrinhPhatXemLai(
                            url = cauHinhCamera.urlClipSuKien(ev.id, congApi),
                            headers = cauHinhCamera.headerBasicAuth(),
                            seekOffsetMs = 0L,
                            seekVersion = seekVersion,
                            dangPhat = dangPhat,
                            tocDo = 1.0f,
                            laTuDrive = false,
                            onTienTrinh = {},
                            onHetDoan = { dangPhat = false },
                            onLoi = onNotice,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Row(
                            Modifier.align(Alignment.TopStart).padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusPill(
                                text = "${ev.nhanHienThi()} · ${ev.gioBatDau()}",
                                color = when {
                                    ev.laNguoiLa -> SH.Red
                                    ev.laNguoiQuen -> SH.Green
                                    else -> SH.Orange
                                },
                            )
                            StatusPill("Mini-NVR Clip", SH.Blue, showDot = false)
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SH.Bg.copy(alpha = 0.78f))
                                .clickable {
                                    if (!dangPhat) seekVersion++
                                    dangPhat = !dangPhat
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                if (dangPhat) "Tạm dừng" else "Phát lại clip",
                                style = SHType.Caption,
                                color = SH.Text,
                            )
                        }
                    }

                    else -> {
                        var bmp by remember(ev.id) { mutableStateOf<Bitmap?>(null) }
                        LaunchedEffect(ev.id) {
                            if (ev.hasThumb) {
                                bmp = CameraPlaybackApi.taiThumbnail(cauHinhCamera, ev.id, congApi)
                            }
                        }
                        val hinh = bmp
                        if (hinh != null) {
                            Image(
                                bitmap = hinh.asImageBitmap(),
                                contentDescription = ev.nhanHienThi(),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            KhungThongBaoXemLai(
                                "${ev.nhanHienThi()} lúc ${ev.gioBatDau()}",
                                "Sự kiện này chỉ lưu ảnh chụp (chưa có clip ngắn Mini-NVR). Bạn có thể xem đầy đủ ở tab Xem lại 24/24.",
                            )
                        }
                    }
                }
            }
        }

        // Cot phai: Danh sach su kien Mini-NVR
        Column(
            Modifier
                .width(232.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Dim.CardRadius))
                .background(SH.Card)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
                .padding(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = SH.Blue, modifier = Modifier.size(16.dp))
                    Text("Sự kiện", style = SHType.CardTitle, color = SH.Text)
                }
                Text("${dsSuKien.size} mục", style = SHType.Caption, color = SH.TextDim)
            }
            Spacer(Modifier.height(8.dp))
            if (dsSuKien.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có sự kiện phù hợp.", style = SHType.Caption, color = SH.TextOff)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(dsSuKien, key = { it.id }) { ev ->
                        val chon = suKienChon?.id == ev.id
                        TheSuKienMiniNvr(
                            ev = ev,
                            dangXem = chon,
                            cauHinhCamera = cauHinhCamera,
                            congApi = congApi,
                            onClick = {
                                suKienChon = ev
                                dangPhat = true
                                seekVersion++
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TheSuKienMiniNvr(
    ev: SuKienCamera,
    dangXem: Boolean,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onClick: () -> Unit,
) {
    var bmp by remember(ev.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(ev.id, ev.hasThumb) {
        if (ev.hasThumb) bmp = CameraPlaybackApi.taiThumbnail(cauHinhCamera, ev.id, congApi)
    }
    val mau = when {
        ev.laNguoiLa -> SH.Red
        ev.laNguoiQuen -> SH.Green
        else -> SH.Orange
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (dangXem) mau.tint(0.16f) else SH.Card2)
            .border(Dim.BorderWidth, if (dangXem) mau else SH.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 62.dp, height = 40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SH.Bg),
            contentAlignment = Alignment.Center,
        ) {
            val hinh = bmp
            if (hinh != null) {
                Image(
                    bitmap = hinh.asImageBitmap(),
                    contentDescription = ev.nhanHienThi(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    when {
                        ev.laNguoiLa -> Icons.Filled.PersonOff
                        ev.laNguoiQuen -> Icons.Filled.Face
                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                    },
                    contentDescription = null,
                    tint = mau,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                ev.nhanHienThi(),
                style = SHType.BodyMed,
                color = if (dangXem) mau else SH.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(ev.gioBatDau())
                    if (ev.hasClip) append(" · Clip")
                    if (ev.camera.isNotBlank()) append(" · ").append(ev.camera)
                },
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ============================================================ CHE DO XEM LAI (IMOU / EZVIZ)

@Composable
private fun KhungCheDoXemLai(
    tenCamHienThi: String,
    luongThat: String?,
    cauHinhCamera: CauHinhCamera,
    cameraRecorder: CameraRecorderState,
    lanTai: Int,
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val congApi = cameraRecorder.apiPort
    val homNayStr = remember { LocalDate.now(ZONA_VN).toString() }

    var ngayChon by rememberSaveable(luongThat) { mutableStateOf(homNayStr) }
    var dsNgay by remember(luongThat) { mutableStateOf<List<NgayCoVideo>>(emptyList()) }
    var timeline by remember(luongThat, ngayChon) { mutableStateOf<DongThoiGianNgay?>(null) }
    var dangTaiTl by remember(luongThat, ngayChon) { mutableStateOf(false) }
    var loiTl by remember(luongThat, ngayChon) { mutableStateOf<String?>(null) }
    var nhipTaiLai by remember { mutableIntStateOf(0) }

    var doanDangPhat by remember(luongThat, ngayChon) { mutableStateOf<DoanVideo?>(null) }
    var seekOffsetMs by remember { mutableLongStateOf(0L) }
    var seekVersion by remember { mutableIntStateOf(0) }
    var currentTs by remember(luongThat, ngayChon) { mutableLongStateOf(0L) }

    var dangPhat by remember { mutableStateOf(true) }
    var tocDo by remember { mutableFloatStateOf(1.0f) }
    var zoom by remember { mutableStateOf(MucZoomTimeline.GIO_6) }
    var chiTuaNguoi by remember { mutableStateOf(false) }

    fun nhayToiMoc(tl: DongThoiGianNgay, dichTs: Long) {
        val viTri = tl.timViTriPhat(dichTs)
        if (viTri == null) {
            onNotice("Chưa có đoạn ghi hình nào tại mốc này.")
            return
        }
        currentTs = viTri.targetEpochSec
        seekOffsetMs = viTri.offsetMs
        if (doanDangPhat?.id != viTri.segment.id) {
            doanDangPhat = viTri.segment
        }
        seekVersion++
        dangPhat = true
    }

    // Tai danh sach ngay + dong thoi gian tu serverubuntu
    LaunchedEffect(luongThat, ngayChon, lanTai, nhipTaiLai, congApi) {
        if (luongThat == null) {
            timeline = null
            loiTl = CHUA_NOI
            return@LaunchedEffect
        }
        dangTaiTl = true
        loiTl = null

        CameraPlaybackApi.danhSachNgay(cauHinhCamera, luongThat, congApi)
            .onSuccess { dsNgay = it }

        CameraPlaybackApi.taiDongThoiGian(cauHinhCamera, luongThat, ngayChon, congApi)
            .onSuccess { tl ->
                timeline = tl
                if (tl.segments.isNotEmpty()) {
                    // Mac dinh mo su kien nguoi moi nhat, neu khong co thi mo doan ghi moi nhat
                    val suKienCuoi = tl.suKienNguoi.lastOrNull()
                    val mocMoDau = suKienCuoi?.let { tl.mocNhaySuKien(it) }
                        ?: maxOf(tl.segments.last().startTs, tl.segments.last().endTs - 30L)
                    if (doanDangPhat == null) {
                        nhayToiMoc(tl, mocMoDau)
                    }
                } else {
                    doanDangPhat = null
                    currentTs = tl.dayStartTs + 12 * 3600L
                }
            }
            .onFailure { e ->
                timeline = null
                doanDangPhat = null
                loiTl = CameraPlaybackApi.dienGiaiLoi(e, congApi ?: cauHinhCamera.congGhiHinh)
            }
        dangTaiTl = false
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // ---------- Cot giua: Thanh chon ngay + Video MP4 + Dieu khien + Timeline ----------
        Column(Modifier.weight(1f).fillMaxHeight()) {
            // 1. Thanh chon ngay & muc zoom Timeline
            ThanhChonNgayVaZoom(
                ngayChon = ngayChon,
                homNay = homNayStr,
                dsNgay = dsNgay,
                zoom = zoom,
                onDoiNgay = { ngayMoi ->
                    doanDangPhat = null
                    ngayChon = ngayMoi
                },
                onDoiZoom = { zoom = it },
                onLamMoi = { nhipTaiLai++ },
            )

            Spacer(Modifier.height(6.dp))

            // 2. Khung phat video MP4
            val seg = doanDangPhat
            val tl = timeline
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(Dim.CardRadius))
                    .background(SH.Card)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
            ) {
                when {
                    dangTaiTl && tl == null -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SH.Blue, modifier = Modifier.size(28.dp))
                        }
                    }

                    loiTl != null -> {
                        KhungThongBaoXemLai(
                            tieuDe = "Chưa tải được lịch sử ghi hình",
                            chiTiet = loiTl ?: "",
                        )
                    }

                    tl == null || tl.segments.isEmpty() || seg == null -> {
                        KhungThongBaoXemLai(
                            tieuDe = "Chưa có video ngày $ngayChon",
                            chiTiet = "Đầu ghi trên serverubuntu tự lưu 24/24 theo từng đoạn 10 phút — đoạn đầu tiên sẽ xuất hiện ngay khi chốt xong.",
                        )
                    }

                    else -> {
                        TrinhPhatXemLai(
                            url = cauHinhCamera.urlVideoSegment(seg.id, congApi),
                            headers = cauHinhCamera.headerBasicAuth(),
                            seekOffsetMs = seekOffsetMs,
                            seekVersion = seekVersion,
                            dangPhat = dangPhat,
                            tocDo = tocDo,
                            laTuDrive = !seg.laCucBo,
                            onTienTrinh = { posMs ->
                                val mocMoi = seg.startTs + posMs / 1000L
                                currentTs = mocMoi
                                // Neu bat che do "Chi tua moc co nguoi": het vung co nguoi thi nhay sang su kien tiep!
                                if (chiTuaNguoi) {
                                    val dangTrongSuKien = tl.suKienNguoi.any {
                                        mocMoi >= (it.startTs - 4L) && mocMoi <= (it.endTs + 3L)
                                    }
                                    if (!dangTrongSuKien) {
                                        tl.suKienNguoiTiep(mocMoi)?.let { ke ->
                                            nhayToiMoc(tl, tl.mocNhaySuKien(ke))
                                        }
                                    }
                                }
                            },
                            onHetDoan = {
                                val tiep = tl.doanTiepTheo(seg.id)
                                if (tiep != null) {
                                    seekOffsetMs = 0L
                                    currentTs = tiep.startTs
                                    doanDangPhat = tiep
                                    seekVersion++
                                } else {
                                    dangPhat = false
                                }
                            },
                            onLoi = onNotice,
                            modifier = Modifier.fillMaxSize(),
                        )

                        // Nhan goc tren trai: Xem lai + Nguon luu tru (Hot Cache 10GB vs Google Drive 5TB)
                        Row(
                            Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusPill(
                                text = "Xem lại · ${dinhDangGioVn(currentTs)}",
                                color = SH.Amber,
                            )
                            StatusPill(
                                text = if (seg.laCucBo) "Bộ đệm 10GB" else "Google Drive 5TB",
                                color = if (seg.laCucBo) SH.Green else SH.Blue,
                                showDot = false,
                            )
                        }

                        Text(
                            "$tenCamHienThi · $ngayChon",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(SH.Bg.copy(alpha = 0.7f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 3. Hang nut dieu khien tua video + nhay su kien nguoi + toc do 1x/2x/4x
            ThanhDieuKhienXemLai(
                dangPhat = dangPhat,
                tocDo = tocDo,
                chiTuaNguoi = chiTuaNguoi,
                coDuLieu = timeline?.segments?.isNotEmpty() == true,
                onPlayPause = { dangPhat = !dangPhat },
                onLui10s = { timeline?.let { nhayToiMoc(it, currentTs - 10L) } },
                onTien10s = { timeline?.let { nhayToiMoc(it, currentTs + 10L) } },
                onNguoiTruoc = {
                    val t = timeline ?: return@ThanhDieuKhienXemLai
                    val ev = t.suKienNguoiTruoc(currentTs)
                    if (ev != null) nhayToiMoc(t, t.mocNhaySuKien(ev))
                    else onNotice("Không còn sự kiện có người nào trước mốc này trong ngày.")
                },
                onNguoiTiep = {
                    val t = timeline ?: return@ThanhDieuKhienXemLai
                    val ev = t.suKienNguoiTiep(currentTs)
                    if (ev != null) nhayToiMoc(t, t.mocNhaySuKien(ev))
                    else onNotice("Đã ở sự kiện có người cuối cùng trong ngày.")
                },
                onDoiTocDo = {
                    tocDo = when (tocDo) {
                        1.0f -> 2.0f
                        2.0f -> 4.0f
                        else -> 1.0f
                    }
                },
                onDaoChiTuaNguoi = { chiTuaNguoi = !chiTuaNguoi },
            )

            Spacer(Modifier.height(6.dp))

            // 4. Thanh thuoc dong thoi gian 24/24 keo muot bang ngon tay
            val dayStart = timeline?.dayStartTs ?: 0L
            val dayEnd = timeline?.dayEndTs ?: 86_400L
            ThanhThoiGianCamera(
                dayStartTs = dayStart,
                dayEndTs = dayEnd,
                currentTs = currentTs.takeIf { it > 0L } ?: (dayStart + 43_200L),
                zoom = zoom,
                segments = timeline?.segments.orEmpty(),
                events = timeline?.events.orEmpty(),
                onChonMoc = { moc -> timeline?.let { nhayToiMoc(it, moc) } },
                modifier = Modifier.fillMaxWidth().height(60.dp),
            )
        }

        // ---------- Cot phai: Danh sach su kien "Phat hien nguoi" kem thumbnail ----------
        CotSuKienNguoi(
            suKienNguoi = timeline?.suKienNguoi.orEmpty(),
            currentTs = currentTs,
            cauHinhCamera = cauHinhCamera,
            congApi = congApi,
            onChonSuKien = { ev ->
                timeline?.let { tl -> nhayToiMoc(tl, tl.mocNhaySuKien(ev)) }
            },
            modifier = Modifier.width(214.dp).fillMaxHeight(),
        )
    }
}

@Composable
private fun ThanhChonNgayVaZoom(
    ngayChon: String,
    homNay: String,
    dsNgay: List<NgayCoVideo>,
    zoom: MucZoomTimeline,
    onDoiNgay: (String) -> Unit,
    onDoiZoom: (MucZoomTimeline) -> Unit,
    onLamMoi: () -> Unit,
) {
    val ngayParsed = remember(ngayChon) { runCatching { LocalDate.parse(ngayChon) }.getOrNull() }
    val coNguoiHomDo = dsNgay.firstOrNull { it.ngay == ngayChon }?.soSuKienNguoi ?: 0

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Cụm lịch ngày bên trái
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NutNho(Icons.Filled.ChevronLeft, "Ngày trước") {
                ngayParsed?.minusDays(1)?.toString()?.let(onDoiNgay)
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Card)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(8.dp))
                    .clickable { onDoiNgay(homNay) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    if (ngayChon == homNay) "Hôm nay ($ngayChon)" else ngayChon,
                    style = SHType.Label,
                    color = SH.Text,
                )
                if (coNguoiHomDo > 0) {
                    Dot(SH.Orange, 6.dp)
                    Text("$coNguoiHomDo", style = SHType.Caption, color = SH.Orange)
                }
            }
            NutNho(
                icon = Icons.Filled.ChevronRight,
                moTa = "Ngày sau",
                bat = ngayChon < homNay,
            ) {
                ngayParsed?.plusDays(1)?.toString()?.takeIf { it <= homNay }?.let(onDoiNgay)
            }
            NutNho(Icons.Filled.Refresh, "Làm mới dòng thời gian", onClick = onLamMoi)
        }

        // Cụm chọn mức phóng to thước thời gian (24h / 6h / 1h)
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SH.Card)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(8.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MucZoomTimeline.entries.forEach { muc ->
                val chon = muc == zoom
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (chon) SH.Blue else Color.Transparent)
                        .clickable { onDoiZoom(muc) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        muc.nhan,
                        style = SHType.Caption,
                        color = if (chon) Color.White else SH.TextDim,
                        fontWeight = if (chon) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThanhDieuKhienXemLai(
    dangPhat: Boolean,
    tocDo: Float,
    chiTuaNguoi: Boolean,
    coDuLieu: Boolean,
    onPlayPause: () -> Unit,
    onLui10s: () -> Unit,
    onTien10s: () -> Unit,
    onNguoiTruoc: () -> Unit,
    onNguoiTiep: () -> Unit,
    onDoiTocDo: () -> Unit,
    onDaoChiTuaNguoi: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            NutNho(Icons.Filled.SkipPrevious, "Sự kiện người trước", bat = coDuLieu, onClick = onNguoiTruoc)
            NutNho(Icons.Filled.FastRewind, "Lùi 10 giây", bat = coDuLieu, onClick = onLui10s)
            NutNho(
                icon = if (dangPhat) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                moTa = if (dangPhat) "Tạm dừng" else "Phát",
                bat = coDuLieu,
                noiBat = coDuLieu,
                onClick = onPlayPause,
            )
            NutNho(Icons.Filled.FastForward, "Tiến 10 giây", bat = coDuLieu, onClick = onTien10s)
            NutNho(Icons.Filled.SkipNext, "Sự kiện người tiếp", bat = coDuLieu, onClick = onNguoiTiep)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Chip tốc độ 1x / 2x / 4x
            Box(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(SH.Card)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(7.dp))
                    .clickable(enabled = coDuLieu, onClick = onDoiTocDo)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Text(
                    "${tocDo.toInt()}x",
                    style = SHType.Label,
                    color = if (tocDo > 1f) SH.Amber else SH.Text,
                )
            }

            // Công tắc tự nhảy qua khoảng trống không có người
            Row(
                Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (chiTuaNguoi) SH.Orange.tint(0.18f) else SH.Card)
                    .border(
                        Dim.BorderWidth,
                        if (chiTuaNguoi) SH.Orange else SH.Border,
                        RoundedCornerShape(7.dp),
                    )
                    .clickable(onClick = onDaoChiTuaNguoi)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = if (chiTuaNguoi) SH.Orange else SH.TextDim,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    "Chỉ mốc người",
                    style = SHType.Caption,
                    color = if (chiTuaNguoi) SH.Orange else SH.TextDim,
                )
            }
        }
    }
}

@Composable
private fun CotSuKienNguoi(
    suKienNguoi: List<SuKienCamera>,
    currentTs: Long,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onChonSuKien: (SuKienCamera) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(Dim.CardRadius))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
            .padding(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = SH.Orange,
                    modifier = Modifier.size(16.dp),
                )
                Text("Có người", style = SHType.CardTitle, color = SH.Text)
            }
            Text(
                "${suKienNguoi.size} mốc",
                style = SHType.Caption,
                color = SH.Orange,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (suKienNguoi.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Chưa ghi nhận người nào trong ngày này.",
                    style = SHType.Caption,
                    color = SH.TextOff,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        } else {
            val daoNguoc = remember(suKienNguoi) { suKienNguoi.asReversed() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                items(daoNguoc, key = { it.id }) { ev ->
                    val dangXem = currentTs >= (ev.startTs - 4L) && currentTs <= (ev.endTs + 2L)
                    TheSuKienNguoi(
                        ev = ev,
                        dangXem = dangXem,
                        cauHinhCamera = cauHinhCamera,
                        congApi = congApi,
                        onClick = { onChonSuKien(ev) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TheSuKienNguoi(
    ev: SuKienCamera,
    dangXem: Boolean,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onClick: () -> Unit,
) {
    var bmp by remember(ev.id) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(ev.id, ev.hasThumb) {
        if (ev.hasThumb) {
            bmp = CameraPlaybackApi.taiThumbnail(cauHinhCamera, ev.id, congApi)
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (dangXem) SH.Orange.tint(0.16f) else SH.Card2)
            .border(
                Dim.BorderWidth,
                if (dangXem) SH.Orange else SH.Border,
                RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SH.Bg),
            contentAlignment = Alignment.Center,
        ) {
            val hinh = bmp
            if (hinh != null) {
                Image(
                    bitmap = hinh.asImageBitmap(),
                    contentDescription = "Ảnh phát hiện người",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsWalk,
                    contentDescription = null,
                    tint = SH.Orange,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                ev.gioBatDau(),
                style = SHType.BodyMed,
                color = if (dangXem) SH.Orange else SH.Text,
                maxLines = 1,
            )
            Text(
                "${ev.personCount} người · ${ev.thoiLuongGiay}s · ${(ev.maxConf * 100).toInt()}%",
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ============================================================ THANH PHAN PHU

@Composable
private fun NutChuyenCheDoCam(
    cheDo: CheDoManCam,
    onChon: (CheDoManCam) -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Bg)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        NutTabNho("Trực tiếp", cheDo == CheDoManCam.TRUC_TIEP) { onChon(CheDoManCam.TRUC_TIEP) }
        NutTabNho("Sự kiện", cheDo == CheDoManCam.SU_KIEN) { onChon(CheDoManCam.SU_KIEN) }
        NutTabNho("Xem lại", cheDo == CheDoManCam.XEM_LAI) { onChon(CheDoManCam.XEM_LAI) }
        NutTabNho("Khuôn mặt", cheDo == CheDoManCam.KHUON_MAT) { onChon(CheDoManCam.KHUON_MAT) }
    }
}

@Composable
private fun NutTabNho(nhan: String, chon: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (chon) SH.Blue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            nhan,
            style = SHType.Caption,
            color = if (chon) Color.White else SH.TextDim,
            fontWeight = if (chon) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun NutNho(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    moTa: String,
    bat: Boolean = true,
    noiBat: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (noiBat) SH.Blue else SH.Card)
            .border(Dim.BorderWidth, if (noiBat) SH.Blue else SH.Border, RoundedCornerShape(7.dp))
            .alpha(if (bat) 1f else 0.4f)
            .clickable(enabled = bat, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = moTa,
            tint = if (noiBat) Color.White else SH.Text,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TheThongKeDauGhi(
    rec: CameraRecorderState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(11.dp))
            .padding(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                if (rec.driveEnabled) Icons.Filled.CloudDone else Icons.Filled.Storage,
                contentDescription = null,
                tint = if (rec.dangSong) SH.Green else SH.TextOff,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "Đầu ghi 24/24",
                style = SHType.Label,
                color = SH.Text,
                maxLines = 1,
            )
        }

        if (rec.online == null) {
            Text(
                "Chưa nhận MQTT camera/#",
                style = SHType.Caption,
                color = SH.TextOff,
            )
        } else if (!rec.dangSong) {
            Text(
                "Đầu ghi đang tắt (offline)",
                style = SHType.Caption,
                color = SH.Red,
            )
        } else {
            val cacheStr = String.format(Locale.US, "%.1f/%.0f GB", rec.cacheUsedGb, rec.cacheMaxGb)
            Text("Đệm: $cacheStr", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            Text(
                "Drive hôm nay: ${rec.uploadedToday} đoạn",
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 1,
            )
            Text(
                "Người hôm nay: ${rec.personToday} lần",
                style = SHType.Caption,
                color = if (rec.personToday > 0) SH.Orange else SH.TextDim,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Rec247StatusPill(
    rec: CameraRecorderState,
    luongDangChon: String?,
    trangThaiCam: TrangThaiCam,
) {
    when {
        rec.camDangGhi(luongDangChon) -> StatusPill("REC 24/24 · Drive", SH.Green)
        rec.online == false -> StatusPill("Đầu ghi tắt", SH.Red)
        else -> TrangThaiCameraPill(trangThaiCam)
    }
}

@Composable
private fun KhungThongBaoXemLai(tieuDe: String, chiTiet: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                tint = SH.TextOff,
                modifier = Modifier.size(30.dp),
            )
            Text(tieuDe, style = SHType.CardTitle, color = SH.TextDim, textAlign = TextAlign.Center)
            Text(chiTiet, style = SHType.Caption, color = SH.TextOff, textAlign = TextAlign.Center)
        }
    }
}

/** Lop chu de len khung hinh: huy hieu Truc tiep, gio, ten camera. */
@Composable
private fun BoxScope.PhuDe(
    ten: String,
    phu: String,
    stamp: String,
    trucTiep: Boolean,
    suKienGanNhat: String? = null,
) {
    if (trucTiep) {
        LiveBadge(Modifier.align(Alignment.TopStart).padding(10.dp))
    }
    Text(
        stamp,
        style = SHType.Label,
        color = SH.Text,
        maxLines = 1,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SH.Bg.copy(alpha = 0.72f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
    if (suKienGanNhat != null) {
        Text(
            suKienGanNhat,
            style = SHType.Caption,
            color = SH.Orange,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SH.Bg.copy(alpha = 0.75f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
    Column(
        Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SH.Bg.copy(alpha = 0.55f))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(
            ten,
            style = SHType.CardTitle,
            color = SH.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(phu, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
    }
}

/** Khung xem khi da khai bao go2rtc nhung khong lay duoc luong nao. */
@Composable
private fun KhungTrong(trangThai: TrangThaiCam, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(Dim.CardRadius))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.VideocamOff,
                contentDescription = null,
                tint = SH.TextOff,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when (trangThai) {
                    is TrangThaiCam.DangDo -> "Đang hỏi go2rtc..."
                    is TrangThaiCam.Loi -> trangThai.moTa
                    else -> "Chưa có luồng nào"
                },
                style = SHType.Body,
                color = SH.TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
            )
        }
    }
}

/** Cot trai khi khong co camera nao de liet ke. */
@Composable
private fun KhongCoCamera(trangThai: TrangThaiCam, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(11.dp))
            .padding(10.dp),
    ) {
        Icon(
            Icons.Filled.VideocamOff,
            contentDescription = null,
            tint = SH.TextOff,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (trangThai is TrangThaiCam.DangDo) "Đang hỏi go2rtc" else "Không có camera",
            style = SHType.BodyMed,
            color = SH.TextDim,
        )
    }
}

/** Mot dong trong danh sach camera ben trai. */
@Composable
private fun CameraRow(
    feed: CameraFeed,
    selected: Boolean,
    dangGhi: Boolean,
    biAnRiengTu: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) SH.Blue.tint(0.14f) else SH.Card)
            .border(
                Dim.BorderWidth,
                if (selected) SH.BorderHi else SH.Border,
                RoundedCornerShape(11.dp),
            )
            .clickable(onClick = onClick)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImagePlaceholder(
            icon = if (biAnRiengTu) Icons.Filled.VisibilityOff else feed.icon,
            label = feed.name,
            accent = if (biAnRiengTu) SH.Purple else feed.accent,
            showLabel = false,
            iconSize = 16.dp,
            cornerRadius = 8.dp,
            modifier = Modifier.size(width = 40.dp, height = 32.dp),
        )
        Spacer(Modifier.width(7.dp))
        Column(Modifier.weight(1f)) {
            Text(
                feed.name,
                style = SHType.BodyMed,
                color = if (selected) SH.Text else SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (biAnRiengTu) {
                Text("● Riêng tư", style = SHType.Caption, color = SH.Purple, maxLines = 1)
            } else if (dangGhi) {
                Text("● 24/24", style = SHType.Caption, color = SH.Green, maxLines = 1)
            }
        }
    }
}

/** Huy hieu "Truc tiep" - cham do nhap nhay cho giong dau ghi. */
@Composable
private fun LiveBadge(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "live")
    val a by t.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha",
    )
    Row(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SH.Bg.copy(alpha = 0.72f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.alpha(a)) { Dot(SH.Red, 7.dp) }
        Spacer(Modifier.width(5.dp))
        Text("Trực tiếp", style = SHType.Label, color = SH.Text, maxLines = 1)
    }
}

@Composable
fun TrangThaiCameraPill(trangThai: TrangThaiCam) {
    when (trangThai) {
        is TrangThaiCam.Co -> StatusPill(trangThai.so.toString() + " luồng", SH.Green)
        is TrangThaiCam.DangDo -> StatusPill("Đang hỏi...", SH.Amber)
        is TrangThaiCam.ChuaCauHinh -> StatusPill("Chưa khai báo go2rtc", SH.TextOff)
        is TrangThaiCam.Loi -> StatusPill("Không lấy được luồng", SH.Red)
    }
}

@Preview(widthDp = 818, heightDp = 577, showBackground = true)
@Composable
private fun CameraPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize()) {
            CameraScreen(
                HomeState(), CauHinhCamera(), TrangThaiCam.ChuaCauHinh,
                {}, {}, {}, {},
            )
        }
    }
}
