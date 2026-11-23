package vn.smarthome.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.CameraFeed
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.FlatButton
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.LuongCamera
import vn.smarthome.ui.components.RoundIconButton
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.shell.rememberCameraStamp
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState

/**
 * Man camera an ninh.
 *
 * Hai che do, tuy theo da khai bao go2rtc chua:
 *
 *  - CHUA khai bao: khung xem la o giu cho, y nhu truoc. Ba nut noi thang
 *    rang chua co camera that thay vi gia vo da chup / da quay.
 *  - DA khai bao: khung xem la luong that tu go2rtc (xem [LuongCamera]).
 *    Camera Tuya khong noi thang voi app - go2rtc dang nhap Tuya, keo hinh
 *    ve, roi phat lai trong mang nha.
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
) {
    val cam = state.cameras.getOrNull(state.selectedCamera) ?: state.cameras.firstOrNull()
    val stamp = rememberCameraStamp()
    val luongThat = cam?.luong?.takeIf { cauHinhCamera.dayDu() }

    // Tang len mot la khung hinh duoc dung lai tu dau. Re hon nhieu so voi
    // tat mo lai app khi luong treo.
    var lanTai by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Camera an ninh", onBack = null) {
            TrangThaiCameraPill(trangThaiCam)
            Spacer(Modifier.width(8.dp))
            RoundIconButton(Icons.Filled.Refresh, { onDoLai(); lanTai++ })
            RoundIconButton(
                Icons.Filled.Settings,
                { onNotice("Khai báo go2rtc ở màn Cài đặt.") },
            )
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------- Danh sach camera ----------
            Column(
                Modifier.width(168.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.cameras.isEmpty()) {
                    KhongCoCamera(trangThaiCam, Modifier.fillMaxWidth())
                } else {
                    // Chieu cao co dinh 62dp cho tung dong - de weight(1f) thi
                    // mot camera se keo cao het cot, nhin khong giong thiet ke.
                    state.cameras.forEachIndexed { i, c ->
                        CameraRow(
                            feed = c,
                            selected = i == state.selectedCamera,
                            onClick = { onSelectCamera(i) },
                            modifier = Modifier.fillMaxWidth().height(62.dp),
                        )
                    }
                }
            }

            // ---------- Khung xem ----------
            Column(Modifier.weight(1f).fillMaxHeight()) {

                Box(Modifier.fillMaxWidth().weight(1f)) {
                    if (luongThat != null) {
                        // key(): doi camera hoac bam Tai lai thi dung khung moi
                        // han, khong co gang tai su dung WebView cu - WebRTC
                        // doi nguon giua chung hay treo o khung den.
                        key(luongThat, lanTai) {
                            LuongCamera(
                                url = cauHinhCamera.urlTrangPhat(luongThat),
                                taiKhoan = cauHinhCamera.taiKhoan,
                                matKhau = cauHinhCamera.matKhau,
                                onLoi = onNotice,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(Dim.CardRadius)),
                            )
                        }
                        PhuDe(
                            ten = cam?.name ?: luongThat,
                            phu = "go2rtc · " + cauHinhCamera.host,
                            stamp = stamp,
                            trucTiep = true,
                        )
                    } else if (cam != null) {
                        ImagePlaceholder(
                            icon = cam.icon,
                            label = cam.name,
                            accent = cam.accent,
                            iconSize = 54.dp,
                            cornerRadius = Dim.CardRadius,
                            // Ten camera da nam o goc duoi trai roi - bat
                            // showLabel nua thi ten hien hai lan tren cung
                            // mot khung.
                            showLabel = false,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            // trucTiep = false: o giu cho thi KHONG duoc deo
                            // huy hieu "Truc tiep". Khong co hinh nao dang
                            // chay ca - deo vao la noi doi ngay tren man hinh.
                            PhuDe(
                                ten = cam.name,
                                phu = "chưa nối luồng hình",
                                stamp = stamp,
                                trucTiep = false,
                            )
                        }
                    } else {
                        KhungTrong(trangThaiCam, Modifier.fillMaxSize())
                    }
                }

                Spacer(Modifier.height(Dim.CardGap))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlatButton(
                        Icons.Filled.PhotoCamera, "Chụp ảnh",
                        { onNotice(if (luongThat != null) CAN_FFMPEG else CHUA_NOI) },
                        Modifier.weight(1f),
                    )
                    FlatButton(
                        Icons.Filled.Videocam, "Quay video",
                        { onNotice(if (luongThat != null) CHUA_LAM_QUAY else CHUA_NOI) },
                        Modifier.weight(1f),
                    )
                    FlatButton(
                        Icons.Filled.Refresh, "Tải lại hình",
                        { lanTai++ },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val CHUA_NOI =
    "Chưa khai báo go2rtc - vào màn Cài đặt điền địa chỉ máy chạy go2rtc."

private const val CAN_FFMPEG =
    "Ảnh tĩnh cần ffmpeg trên máy chạy go2rtc. Hình trực tiếp thì không cần, " +
        "nên khung hình vẫn chạy bình thường."

private const val CHUA_LAM_QUAY = "Quay video chưa làm."

/** Lop chu de len khung hinh: huy hieu Truc tiep, gio, ten camera. */
@Composable
private fun BoxScope.PhuDe(
    ten: String,
    phu: String,
    stamp: String,
    trucTiep: Boolean,
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
            icon = feed.icon,
            label = feed.name,
            accent = feed.accent,
            showLabel = false,
            iconSize = 17.dp,
            cornerRadius = 8.dp,
            modifier = Modifier.size(width = 46.dp, height = 34.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            feed.name,
            style = SHType.BodyMed,
            color = if (selected) SH.Text else SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
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
