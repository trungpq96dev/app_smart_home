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
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
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
    val verifying = status is OtaStatus.Verifying
    val errorState = status as? OtaStatus.Error
    val busy = downloadingState != null || verifying

    Dialog(onDismissRequest = {
        // Neu dang tai hoac dang xac minh SHA-256 thi khong cho dismiss bang click ngoai
        if (!busy) onDismiss()
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
                        icon = when {
                            errorState != null -> Icons.Default.Error
                            verifying -> Icons.Default.Shield
                            downloadingState != null -> Icons.Default.CloudDownload
                            else -> Icons.Default.SystemUpdate
                        },
                        color = when {
                            errorState != null -> SH.Red
                            verifying -> SH.Green
                            else -> SH.Blue
                        },
                        size = 42.dp,
                        iconSize = 24.dp,
                    )
                    Column {
                        Text(
                            text = when {
                                errorState != null -> "Cập nhật bị hủy"
                                verifying -> "Đang kiểm tra toàn vẹn SHA-256..."
                                downloadingState != null -> "Đang tải bản cập nhật..."
                                else -> "Có phiên bản mới"
                            },
                            style = SHType.CardTitle,
                            color = if (errorState != null) SH.Red else SH.Text,
                        )
                        Text(
                            text = release?.tagName ?: if (verifying) "Xác minh chữ ký SHA-256" else "OTA SmartHome",
                            style = SHType.Caption,
                            color = if (verifying) SH.Green else SH.Blue,
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (release.coMaSha256) {
                            "Bảo mật OTA: Đã có mã SHA-256 để xác minh trước khi cài đặt."
                        } else {
                            "Cảnh báo: Bản phát hành chưa kèm file .sha256 — app sẽ chặn cài đặt."
                        },
                        style = SHType.Caption,
                        color = if (release.coMaSha256) SH.Green else SH.Amber,
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (errorState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Bg)
                            .border(1.dp, SH.Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                    ) {
                        Text(
                            text = errorState.message,
                            style = SHType.Body,
                            color = SH.Red,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }

                if (verifying) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            text = "Đang đối chiếu mã băm SHA-256 của file APK...",
                            style = SHType.Caption,
                            color = SH.Green,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SH.Green,
                            trackColor = SH.Border,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
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
                    if (!busy) {
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SH.TextDim,
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SH.Border),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(if (errorState != null) "Đóng" else "Để sau", style = SHType.Body)
                        }

                        if (release != null) {
                            Spacer(Modifier.width(12.dp))
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
                            text = if (verifying) "Đang xác minh file APK, vui lòng đợi..." else "Vui lòng giữ ứng dụng mở trong khi tải...",
                            style = SHType.Caption,
                            color = SH.TextDim,
                        )
                    }
                }
            }
        }
    }
}
