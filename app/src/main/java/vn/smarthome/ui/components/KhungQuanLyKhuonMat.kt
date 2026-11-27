package vn.smarthome.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.smarthome.data.camera.AnhNguoiLa
import vn.smarthome.data.camera.CameraPlaybackApi
import vn.smarthome.data.camera.CameraRecorderState
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.NguoiQuenMat
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint
import java.io.File

/**
 * Tab 'Khuon mat' trong man Camera:
 * - Cach 1: Dung vao truoc camera an ninh (`cam1`...) de chup ngay hoac dem nguoc 3s.
 * - Cach 2: Chup truc tiep bang camera tren chinh may tinh bang Tab A7 Lite.
 * - Cach 3: Gan ten 1 cham tu anh nguoi la vua chup (7 ngay gan nhat).
 */
@Composable
fun KhungQuanLyKhuonMat(
    luongThat: String?,
    cauHinhCamera: CauHinhCamera,
    cameraRecorder: CameraRecorderState,
    lanTai: Int,
    onDocThuChao: (String) -> Unit = {},
    onNotice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val congApi = cameraRecorder.apiPort

    var tenNguoi by remember { mutableStateOf("") }
    var dsNguoiQuen by remember { mutableStateOf<List<NguoiQuenMat>>(emptyList()) }
    var dsNguoiLa by remember { mutableStateOf<List<AnhNguoiLa>>(emptyList()) }
    var dangTai by remember { mutableStateOf(false) }
    var dangXuLy by remember { mutableStateOf(false) }
    var demNguoc by remember { mutableIntStateOf(0) }
    var ketQua by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var nhipTai by remember { mutableIntStateOf(0) }
    var choXoaTen by remember { mutableStateOf<String?>(null) }

    val fileChupTam = remember(ctx) { File(ctx.cacheDir, "face_enroll_capture.jpg") }

    fun taiLaiDanhSach() {
        nhipTai++
    }

    LaunchedEffect(cauHinhCamera, congApi, lanTai, nhipTai) {
        if (!cauHinhCamera.dayDu()) return@LaunchedEffect
        dangTai = true
        CameraPlaybackApi.taiDanhSachNguoiQuen(cauHinhCamera, congApi)
            .onSuccess { dsNguoiQuen = it }
        CameraPlaybackApi.taiDanhSachNguoiLa(cauHinhCamera, congApi)
            .onSuccess { dsNguoiLa = it }
        dangTai = false
    }

    // Launcher chup anh truc tiep bang camera cua Tablet
    val moMayAnhTablet = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { daChup ->
        if (!daChup) return@rememberLauncherForActivityResult
        val ten = tenNguoi.trim()
        if (ten.isEmpty()) {
            ketQua = false to "Hãy nhập tên người quen trước khi chụp."
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            dangXuLy = true
            ketQua = null
            val bytes = CameraPlaybackApi.nenAnhChupTablet(fileChupTam)
            fileChupTam.delete()
            if (bytes == null) {
                ketQua = false to "Không đọc được ảnh vừa chụp từ tablet."
                dangXuLy = false
                return@launch
            }
            CameraPlaybackApi.themMatTuAnh(cauHinhCamera, ten, bytes, congApi)
                .onSuccess { msg ->
                    ketQua = true to msg
                    onNotice(msg)
                    taiLaiDanhSach()
                }
                .onFailure { e ->
                    val err = CameraPlaybackApi.dienGiaiLoi(e, congApi ?: cauHinhCamera.congGhiHinh)
                    ketQua = false to err
                    onNotice(err)
                }
            dangXuLy = false
        }
    }

    fun kichHoatChupTablet() {
        val ten = tenNguoi.trim()
        if (ten.isEmpty()) {
            ketQua = false to "Hãy nhập tên người quen (ví dụ: Trung, Mẹ...) trước khi chụp."
            return
        }
        runCatching {
            fileChupTam.delete()
            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", fileChupTam)
            moMayAnhTablet.launch(uri)
        }.onFailure { e ->
            ketQua = false to "Không mở được camera trên tablet: ${e.message ?: "lỗi thiết bị"}"
        }
    }

    val xinQuyenCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { duoc ->
        if (duoc) kichHoatChupTablet()
        else ketQua = false to "Chưa cấp quyền Camera cho ứng dụng."
    }

    fun chupTuCameraAnNinh(coDemNguoc: Boolean) {
        val ten = tenNguoi.trim()
        if (ten.isEmpty()) {
            ketQua = false to "Hãy nhập tên người quen (ví dụ: Trung, Mẹ...) trước khi chụp."
            return
        }
        val cam = luongThat
        if (cam.isNullOrBlank()) {
            ketQua = false to "Chưa chọn camera an ninh nào."
            return
        }
        if (dangXuLy || demNguoc > 0) return

        scope.launch {
            ketQua = null
            if (coDemNguoc) {
                for (giay in 3 downTo 1) {
                    demNguoc = giay
                    delay(1000L)
                }
                demNguoc = 0
            }
            dangXuLy = true
            CameraPlaybackApi.themMatTuCamera(cauHinhCamera, ten, cam, congApi)
                .onSuccess { msg ->
                    ketQua = true to msg
                    onNotice(msg)
                    taiLaiDanhSach()
                }
                .onFailure { e ->
                    val err = CameraPlaybackApi.dienGiaiLoi(e, congApi ?: cauHinhCamera.congGhiHinh)
                    ketQua = false to err
                    onNotice(err)
                }
            dangXuLy = false
        }
    }

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Dim.CardGap)) {
        // ---------- Cot trai: Form them nguoi quen (Cam an ninh / Cam Tablet) ----------
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Dim.CardRadius))
                .background(SH.Card)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = SH.Blue, modifier = Modifier.size(19.dp))
                    Text("Thêm khuôn mặt người quen", style = SHType.CardTitle, color = SH.Text)
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Green.tint(0.16f))
                        .border(Dim.BorderWidth, SH.Green, RoundedCornerShape(8.dp))
                        .clickable { onDocThuChao(tenNguoi.ifBlank { dsNguoiQuen.firstOrNull()?.name ?: "Trung" }) }
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = SH.Green, modifier = Modifier.size(14.dp))
                    Text("Nghe thử giọng", style = SHType.Caption, color = SH.Green, fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Nhập tên rồi đứng nhìn thẳng vào camera an ninh hoặc chụp trực tiếp bằng camera tablet. Nên lưu 2–3 ảnh (cả ban ngày và ban đêm) để AI nhận chuẩn nhất.",
                style = SHType.Caption,
                color = SH.TextDim,
            )

            OutlinedTextField(
                value = tenNguoi,
                onValueChange = { tenNguoi = it },
                label = { Text("Tên gọi người quen (vd: Trung, Mẹ, Vợ)", style = SHType.Caption) },
                singleLine = true,
                textStyle = SHType.Body,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = SH.Text,
                    unfocusedTextColor = SH.Text,
                    focusedBorderColor = SH.Blue,
                    unfocusedBorderColor = SH.Border,
                    focusedLabelColor = SH.Blue,
                    unfocusedLabelColor = SH.TextDim,
                    cursorColor = SH.Blue,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (dsNguoiQuen.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Chọn nhanh:", style = SHType.Caption, color = SH.TextOff)
                    dsNguoiQuen.forEach { p ->
                        val chon = tenNguoi.trim().equals(p.name, ignoreCase = true)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (chon) SH.Blue else SH.Card2)
                                .border(Dim.BorderWidth, if (chon) SH.Blue else SH.Border, RoundedCornerShape(7.dp))
                                .clickable { tenNguoi = p.name }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text(
                                "${p.name} (${p.count})",
                                style = SHType.Caption,
                                color = if (chon) Color.White else SH.Text,
                            )
                        }
                    }
                }
            }

            // Cum nut 1: Chup tu Camera an ninh
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Card2)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Videocam, contentDescription = null, tint = SH.Green, modifier = Modifier.size(16.dp))
                    Text(
                        "Cách 1: Đứng vào Camera an ninh (${luongThat ?: "chưa chọn"})",
                        style = SHType.BodyMed,
                        color = SH.Text,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { chupTuCameraAnNinh(coDemNguoc = true) },
                        enabled = !dangXuLy && demNguoc == 0 && luongThat != null,
                        colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) {
                        Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (demNguoc > 0) "Đứng vào cam: ${demNguoc}s..." else "Đếm 3s & Chụp",
                            style = SHType.Caption,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OutlinedButton(
                        onClick = { chupTuCameraAnNinh(coDemNguoc = false) },
                        enabled = !dangXuLy && demNguoc == 0 && luongThat != null,
                        modifier = Modifier.weight(0.85f).height(40.dp),
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = SH.Text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Chụp ngay", style = SHType.Caption, color = SH.Text)
                    }
                }
            }

            // Cum nut 2: Chup truc tiep bang Camera cua Tablet
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Card2)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = SH.Amber, modifier = Modifier.size(16.dp))
                    Text("Cách 2: Chụp trực tiếp bằng Camera Tablet", style = SHType.BodyMed, color = SH.Text)
                }
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            kichHoatChupTablet()
                        } else {
                            xinQuyenCamera.launch(Manifest.permission.CAMERA)
                        }
                    },
                    enabled = !dangXuLy && demNguoc == 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SH.Amber),
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                ) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = SH.Bg, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Mở máy ảnh Tablet chụp người quen",
                        style = SHType.Caption,
                        color = SH.Bg,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (dangXuLy) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(color = SH.Blue, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text("Đang kiểm tra khuôn mặt YuNet & nạp vào AI...", style = SHType.Caption, color = SH.Blue)
                }
            }

            ketQua?.let { (ok, msg) ->
                val mau = if (ok) SH.Green else SH.Red
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(mau.tint(0.14f))
                        .border(Dim.BorderWidth, mau, RoundedCornerShape(9.dp))
                        .padding(10.dp),
                ) {
                    Text(msg, style = SHType.Caption, color = mau)
                }
            }
        }

        // ---------- Cot phai: Danh sach nguoi quen & Nguoi la gan day ----------
        Column(
            Modifier
                .width(290.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Dim.CardRadius))
                .background(SH.Card)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(Dim.CardRadius))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.Face, contentDescription = null, tint = SH.Green, modifier = Modifier.size(16.dp))
                    Text("Người quen (${dsNguoiQuen.size})", style = SHType.CardTitle, color = SH.Text)
                }
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(SH.Card2)
                        .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(7.dp))
                        .clickable { taiLaiDanhSach() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Tải lại", tint = SH.Text, modifier = Modifier.size(15.dp))
                }
            }

            if (dangTai && dsNguoiQuen.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SH.Blue, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (dsNguoiQuen.isEmpty()) {
                        item {
                            Text(
                                "Chưa có khuôn mặt người nhà nào. Hãy thêm ở khung bên trái.",
                                style = SHType.Caption,
                                color = SH.TextOff,
                            )
                        }
                    } else {
                        items(dsNguoiQuen, key = { "known_${it.name}" }) { p ->
                            TheNguoiQuen(
                                nguoi = p,
                                dangChoXoa = choXoaTen == p.name,
                                cauHinhCamera = cauHinhCamera,
                                congApi = congApi,
                                onChonThem = { tenNguoi = p.name },
                                onXoa = {
                                    if (choXoaTen != p.name) {
                                        choXoaTen = p.name
                                    } else {
                                        choXoaTen = null
                                        scope.launch {
                                            CameraPlaybackApi.xoaNguoiQuen(cauHinhCamera, p.name, congApi)
                                                .onSuccess { msg ->
                                                    ketQua = true to msg
                                                    onNotice(msg)
                                                    taiLaiDanhSach()
                                                }
                                                .onFailure { e ->
                                                    ketQua = false to CameraPlaybackApi.dienGiaiLoi(e)
                                                }
                                        }
                                    }
                                },
                            )
                        }
                    }

                    if (dsNguoiLa.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Ảnh người lạ vừa chụp (${dsNguoiLa.size}) — chạm Gán tên",
                                style = SHType.Caption,
                                color = SH.Orange,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(dsNguoiLa.take(10), key = { "unk_${it.filename}" }) { u ->
                            TheAnhNguoiLa(
                                anh = u,
                                tenDangNhap = tenNguoi.trim(),
                                cauHinhCamera = cauHinhCamera,
                                congApi = congApi,
                                onGanTen = {
                                    val ten = tenNguoi.trim()
                                    if (ten.isEmpty()) {
                                        ketQua = false to "Nhập tên vào ô bên trái rồi bấm 'Gán tên'."
                                    } else {
                                        scope.launch {
                                            dangXuLy = true
                                            CameraPlaybackApi.chuyenNguoiLaThanhQuen(cauHinhCamera, u.filename, ten, congApi)
                                                .onSuccess { msg ->
                                                    ketQua = true to msg
                                                    onNotice(msg)
                                                    taiLaiDanhSach()
                                                }
                                                .onFailure { e ->
                                                    ketQua = false to CameraPlaybackApi.dienGiaiLoi(e)
                                                }
                                            dangXuLy = false
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TheNguoiQuen(
    nguoi: NguoiQuenMat,
    dangChoXoa: Boolean,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onChonThem: () -> Unit,
    onXoa: () -> Unit,
) {
    var bmp by remember(nguoi.sampleUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(nguoi.sampleUrl) {
        val u = nguoi.sampleUrl
        if (!u.isNullOrBlank()) {
            bmp = CameraPlaybackApi.taiAnhUrl(cauHinhCamera, cauHinhCamera.urlAnhMat(u, congApi))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(SH.Card2)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(9.dp))
            .clickable(onClick = onChonThem)
            .padding(7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SH.Bg),
            contentAlignment = Alignment.Center,
        ) {
            val h = bmp
            if (h != null) {
                Image(
                    bitmap = h.asImageBitmap(),
                    contentDescription = nguoi.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.Face, contentDescription = null, tint = SH.Green, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(nguoi.name, style = SHType.BodyMed, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${nguoi.count} ảnh mẫu", style = SHType.Caption, color = SH.TextDim)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (dangChoXoa) SH.Red else SH.Bg)
                .clickable(onClick = onXoa)
                .padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            if (dangChoXoa) {
                Text("Xóa?", style = SHType.Caption, color = Color.White, fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Xóa", tint = SH.TextOff, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TheAnhNguoiLa(
    anh: AnhNguoiLa,
    tenDangNhap: String,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onGanTen: () -> Unit,
) {
    var bmp by remember(anh.url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(anh.url) {
        bmp = CameraPlaybackApi.taiAnhUrl(cauHinhCamera, cauHinhCamera.urlAnhMat(anh.url, congApi))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(SH.Card2)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(9.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SH.Bg),
            contentAlignment = Alignment.Center,
        ) {
            val h = bmp
            if (h != null) {
                Image(
                    bitmap = h.asImageBitmap(),
                    contentDescription = "Người lạ",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(Icons.Filled.Face, contentDescription = null, tint = SH.Orange, modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text("Chụp lúc ${anh.gioStr()}", style = SHType.Caption, color = SH.Text, maxLines = 1)
            Text(anh.filename, style = SHType.Caption, color = SH.TextOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(7.dp))
                .background(SH.Blue.tint(0.22f))
                .border(Dim.BorderWidth, SH.Blue, RoundedCornerShape(7.dp))
                .clickable(onClick = onGanTen)
                .padding(horizontal = 8.dp, vertical = 5.dp),
        ) {
            Text(
                if (tenDangNhap.isNotBlank()) "Gán $tenDangNhap" else "Gán tên",
                style = SHType.Caption,
                color = SH.Blue,
                maxLines = 1,
            )
        }
    }
}
