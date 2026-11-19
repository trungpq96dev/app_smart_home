package vn.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.smarthome.data.ota.OtaStatus
import vn.smarthome.data.ota.ReleaseInfo
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

@Composable
fun OtaUpdateDialog(
    status: OtaStatus,
    onDismiss: () -> Unit,
    onStartUpdate: (ReleaseInfo) -> Unit,
) {
    val release = when (status) {
        is OtaStatus.UpdateAvailable -> status.info
        is OtaStatus.ReadyToInstall -> status.info
        else -> null
    }

    val downloadingState = status as? OtaStatus.Downloading

    Dialog(onDismissRequest = {
        // Neu dang tai thi khong cho dismiss bang click ngoai
        if (downloadingState == null) onDismiss()
    }) {
        Surface(
            modifier = Modifier
                .width(480.dp)
                .clip(RoundedCornerShape(Dim.CardRadius)),
            color = SH.Card,
            border = androidx.compose.foundation.BorderStroke(Dim.BorderWidth, SH.Border),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconBadge(
                        icon = if (downloadingState != null) Icons.Default.CloudDownload else Icons.Default.SystemUpdate,
                        color = SH.Blue,
                        size = 42.dp,
                        iconSize = 24.dp,
                    )
                    Column {
                        Text(
                            text = if (downloadingState != null) "Đang tải bản cập nhật..." else "Có phiên bản mới",
                            style = SHType.CardTitle,
                            color = SH.Text,
                        )
                        Text(
                            text = release?.tagName ?: "Phiên bản mới",
                            style = SHType.Caption,
                            color = SH.Blue,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Noi dung thay doi (Changelog)
                if (release != null && release.notes.isNotBlank()) {
                    Text(
                        text = "Nội dung cập nhật:",
                        style = SHType.Caption,
                        color = SH.TextDim,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Bg)
                            .border(1.dp, SH.Border, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = release.notes,
                            style = SHType.Body,
                            color = SH.Text,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Tien trinh tai (neu dang download)
                if (downloadingState != null) {
                    val percent = downloadingState.progressPercent
                    val downloadedMb = downloadingState.bytesDownloaded / (1024f * 1024f)
                    val totalMb = downloadingState.totalBytes / (1024f * 1024f)

                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Tiến độ: $percent%",
                                style = SHType.Caption,
                                color = SH.Blue,
                                fontWeight = FontWeight.Bold,
                            )
                            if (totalMb > 0) {
                                Text(
                                    text = String.format("%.1f / %.1f MB", downloadedMb, totalMb),
                                    style = SHType.Caption,
                                    color = SH.TextDim,
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SH.Blue,
                            trackColor = SH.Border,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Nut bam thao tac
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (downloadingState == null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SH.TextDim,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SH.Border),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("Để sau", style = SHType.Body)
                        }

                        Spacer(Modifier.width(12.dp))

                        if (release != null) {
                            Button(
                                onClick = { onStartUpdate(release) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SH.Blue,
                                    contentColor = Color.White,
                                ),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text("Cập nhật ngay", style = SHType.Body, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                        Text(
                            text = "Vui lòng giữ ứng dụng mở trong khi tải...",
                            style = SHType.Caption,
                            color = SH.TextDim,
                        )
                    }
                }
            }
        }
    }
}
