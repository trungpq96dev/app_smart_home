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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.CameraFeed
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.FlatButton
import vn.smarthome.ui.components.ImagePlaceholder
import vn.smarthome.ui.components.RoundIconButton
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
 * KHONG co luong hinh that: khung xem la o giu cho. Ba nut duoi cung co bao
 * cho nguoi dung biet dieu do thay vi gia vo da chup / da quay - khung giao
 * dien khong duoc noi doi ve viec da lam duoc gi.
 */
@Composable
fun CameraScreen(
    state: HomeState,
    onBack: () -> Unit,
    onSelectCamera: (Int) -> Unit,
    onNotice: (String) -> Unit,
) {
    val cam = state.cameras.getOrNull(state.selectedCamera) ?: state.cameras.first()
    val stamp = rememberCameraStamp()

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Camera an ninh", onBack = onBack) {
            RoundIconButton(Icons.Filled.Fullscreen, { onNotice(NOT_CONNECTED) })
            RoundIconButton(Icons.Filled.Settings, { onNotice("Cài đặt camera sẽ làm ở giai đoạn sau.") })
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
                // Chieu cao co dinh 62dp cho tung dong - de weight(1f) thi 4 dong
                // se keo cao 104dp moi dong o man 533dp, nhin khong giong thiet ke.
                state.cameras.forEachIndexed { i, c ->
                    CameraRow(
                        feed = c,
                        selected = i == state.selectedCamera,
                        onClick = { onSelectCamera(i) },
                        modifier = Modifier.fillMaxWidth().height(62.dp),
                    )
                }
            }

            // ---------- Khung xem ----------
            Column(Modifier.weight(1f).fillMaxHeight()) {
                ImagePlaceholder(
                    icon = cam.icon,
                    label = cam.name,
                    accent = cam.accent,
                    iconSize = 54.dp,
                    cornerRadius = Dim.CardRadius,
                    // Ten camera da nam o goc duoi trai roi - bat showLabel nua
                    // thi ten hien hai lan tren cung mot khung.
                    showLabel = false,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                ) {
                    LiveBadge(
                        Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
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
                    ) {
                        Text(
                            cam.name,
                            style = SHType.CardTitle,
                            color = SH.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "chưa nối luồng hình",
                            style = SHType.Caption,
                            color = SH.TextOff,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(Dim.CardGap))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FlatButton(
                        Icons.Filled.PhotoCamera, "Chụp ảnh",
                        { onNotice(NOT_CONNECTED) }, Modifier.weight(1f),
                    )
                    FlatButton(
                        Icons.Filled.Videocam, "Quay video",
                        { onNotice(NOT_CONNECTED) }, Modifier.weight(1f),
                    )
                    FlatButton(
                        Icons.Filled.Fullscreen, "Toàn màn hình",
                        { onNotice(NOT_CONNECTED) }, Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val NOT_CONNECTED =
    "Chưa nối camera thật - đây mới là khung giao diện."

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

@Preview(widthDp = 705, heightDp = 509, showBackground = true)
@Composable
private fun CameraPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize()) {
            CameraScreen(HomeState(), {}, {}, {})
        }
    }
}
