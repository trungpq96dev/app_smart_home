package vn.smarthome.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.camera.CameraPlaybackApi
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.FaceGreetEvent
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Lop phu noi hien thi su kien nhan dien khuon mat tu `camera/face/greet` (Muc 2.2):
 *  - Nguoi quen: banner gon 5 giay goc tren phai voi cau chao.
 *  - Nguoi la (che do Nha ban ngay): the popup 10 giay kem anh chup that + nut Mo Camera.
 */
@Composable
fun PopupChaoKhuonMat(
    ev: FaceGreetEvent,
    cauHinhCamera: CauHinhCamera,
    congApi: Int?,
    onMoCamera: () -> Unit,
    onDong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var anh by remember(ev.ts, ev.snapshotUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(ev.ts, ev.snapshotUrl) {
        if (ev.snapshotUrl.isNotBlank() && cauHinhCamera.dayDu()) {
            val url = cauHinhCamera.urlAnhMat(ev.snapshotUrl, congApi)
            anh = CameraPlaybackApi.taiAnhUrl(cauHinhCamera, url)
        }
    }

    val mauNhan = if (ev.isKnown) SH.Green else SH.Orange

    if (ev.isKnown) {
        // Banner 5 giay cho nguoi nha
        Row(
            modifier = modifier
                .width(360.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SH.Card)
                .border(1.5.dp, mauNhan, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(mauNhan.tint(0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Face, contentDescription = null, tint = mauNhan, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    ev.greeting,
                    style = SHType.CardTitle,
                    color = SH.Text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${ev.camera} · ${ev.gioStr()} · ${(ev.confidence * 100).toInt()}%",
                    style = SHType.Caption,
                    color = SH.TextDim,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = "Đóng",
                tint = SH.TextOff,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(onClick = onDong),
            )
        }
    } else {
        // Popup 10 giay kem anh chup cho khach / nguoi la o cua
        Column(
            modifier = modifier
                .width(340.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(SH.Card)
                .border(1.5.dp, mauNhan, RoundedCornerShape(14.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
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
                    Icon(
                        Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = mauNhan,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        ev.greeting.ifBlank { "Có khách ở cửa" },
                        style = SHType.CardTitle,
                        color = mauNhan,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Đóng",
                    tint = SH.TextOff,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onDong),
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Bg)
                    .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = anh
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Ảnh khách ở cửa",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Videocam,
                            contentDescription = null,
                            tint = SH.TextOff,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("Đang tải ảnh chụp...", style = SHType.Caption, color = SH.TextOff)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${ev.camera} · ${ev.gioStr()}",
                    style = SHType.Caption,
                    color = SH.TextDim,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Card2)
                            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(8.dp))
                            .clickable(onClick = onDong)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text("Đóng", style = SHType.Caption, color = SH.TextDim)
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Blue)
                            .clickable(onClick = onMoCamera)
                            .padding(horizontal = 11.dp, vertical = 6.dp),
                    ) {
                        Text("Mở Camera", style = SHType.Caption, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
