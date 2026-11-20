package vn.smarthome.ui.screens

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Man Tu dong hoa.
 *
 * Tich hop the he thong bom nuoc AquaControl (mo app ngoai vn.aquacontrol)
 * va cac kich ban tu dong hoa thong minh trong nha.
 */
@Composable
fun AutomationScreen(
    onBack: () -> Unit,
    onNotice: (String) -> Unit,
) {
    val context = LocalContext.current
    val installed = remember { isAquaControlInstalled(context) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tự động hóa", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ================== COT TRAI: THE AQUACONTROL ==================
            SmartCard(
                modifier = Modifier
                    .weight(1.08f)
                    .fillMaxHeight(),
                borderColor = if (installed) SH.Cyan.tint(0.45f) else SH.Border,
                padding = 16.dp,
            ) {
                // Header the
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.WaterDrop, SH.Cyan, size = 38.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "AquaControl",
                            style = SHType.PageTitle,
                            color = SH.Text,
                            maxLines = 1,
                        )
                        Text(
                            text = "Hệ thống Bơm & Cấp Nước Tự Động",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                        )
                    }
                    StatusPill(
                        text = if (installed) "Đã cài đặt" else "Chưa phát hiện",
                        color = if (installed) SH.Green else SH.TextOff,
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Noi dung gioi thieu he thong bom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SH.Card2)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AquaFeatureRow(
                        icon = Icons.Filled.WaterDrop,
                        iconColor = SH.Cyan,
                        title = "Bồn chứa 1500L & Phao cảm biến",
                        desc = "Tự động bơm khi nước cạn, tự động ngắt khi đầy bồn chống tràn.",
                    )
                    AquaFeatureRow(
                        icon = Icons.Filled.Bolt,
                        iconColor = SH.Amber,
                        title = "Bảo vệ động cơ máy bơm ESP32",
                        desc = "Tự ngắt khi quá nhiệt, còi hú báo động khi kẹt bơm hoặc mất nước nguồn.",
                    )
                    AquaFeatureRow(
                        icon = Icons.Filled.CheckCircle,
                        iconColor = SH.Green,
                        title = "Đồng bộ đa kênh LAN & MQTT",
                        desc = "Nhịp số liệu 1s/lần qua mạng nội bộ và 30s/lần qua EMQX broker từ xa.",
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Nut bam khoi chay app AquaControl
                Button(
                    onClick = {
                        launchAquaControl(context, onNotice)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dim.ButtonH),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (installed) SH.Blue else SH.BorderHi,
                    ),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Mở ứng dụng AquaControl",
                        style = SHType.Body.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Gói ứng dụng: vn.aquacontrol • Bấm để mở giao diện điều khiển chi tiết bồn & bơm",
                    style = SHType.Caption,
                    color = SH.TextOff,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }

            // ================== COT PHAI: KICH BAN TU DONG HOA ==================
            SmartCard(
                modifier = Modifier
                    .weight(0.92f)
                    .fillMaxHeight(),
                padding = 16.dp,
            ) {
                // Header kich ban
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.AutoAwesome, SH.Amber, size = 38.dp, iconSize = 22.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Kịch bản tự động",
                            style = SHType.PageTitle,
                            color = SH.Text,
                            maxLines = 1,
                        )
                        Text(
                            text = "Thiết lập tự động hóa trong nhà",
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Danh sach cac kich ban
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AutomationScenarioCard(
                        icon = Icons.Filled.WaterDrop,
                        color = SH.Cyan,
                        title = "Bơm nước thông minh",
                        desc = "Tự động kích hoạt chu trình bơm theo phao mực nước",
                        status = "AquaControl",
                        onClick = { launchAquaControl(context, onNotice) },
                    )

                    AutomationScenarioCard(
                        icon = Icons.Filled.Bolt,
                        color = SH.Amber,
                        title = "Cảnh báo quá tải điện EVN",
                        desc = "Cảnh báo khi công suất phụ tải toàn nhà vượt 5000W",
                        status = "Đang chạy",
                        onClick = { onNotice("Hệ thống giám sát điện EVN đang hoạt động tự động.") },
                    )

                    AutomationScenarioCard(
                        icon = Icons.Filled.Nightlight,
                        color = SH.Purple,
                        title = "Chiếu sáng ban đêm",
                        desc = "Tự động bật đèn ban công & sân khi trời tối sau 18:00",
                        status = "Đã lên lịch",
                        onClick = { onNotice("Kịch bản chiếu sáng đã được kích hoạt theo giờ.") },
                    )

                    AutomationScenarioCard(
                        icon = Icons.Filled.MeetingRoom,
                        color = SH.Teal,
                        title = "Chế độ vắng nhà (Away)",
                        desc = "Tắt toàn bộ máy lạnh, quạt và đèn khi rời khỏi nhà",
                        status = "Sẵn sàng",
                        onClick = { onNotice("Chế độ vắng nhà đã sẵn sàng.") },
                    )
                }
            }
        }
    }
}

@Composable
private fun AquaFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = SHType.Body.copy(fontWeight = FontWeight.Medium),
                color = SH.Text,
                maxLines = 1,
            )
            Text(
                desc,
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun AutomationScenarioCard(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String,
    status: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card2)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, color, size = 32.dp, iconSize = 16.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = SHType.Body.copy(fontWeight = FontWeight.Medium),
                color = SH.Text,
                maxLines = 1,
            )
            Text(
                desc,
                style = SHType.Caption,
                color = SH.TextDim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        StatusPill(status, color)
    }
}

/**
 * Kiem tra ung dung AquaControl da duoc cai tren thiet bi hay chua.
 */
fun isAquaControlInstalled(context: Context): Boolean {
    val pm = context.packageManager
    return pm.getLaunchIntentForPackage("vn.aquacontrol") != null ||
            pm.getLaunchIntentForPackage("vn.aquacontrol.debug") != null
}

/**
 * Khoi chay ung dung AquaControl thong qua Intent.
 */
fun launchAquaControl(context: Context, onNotice: (String) -> Unit) {
    val pm = context.packageManager
    val packages = listOf("vn.aquacontrol", "vn.aquacontrol.debug")
    for (pkg in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                return
            } catch (e: Exception) {
                onNotice("Lỗi khi mở AquaControl: ${e.message}")
                return
            }
        }
    }
    onNotice("Không tìm thấy ứng dụng AquaControl (vn.aquacontrol) trên máy tính bảng.")
}
