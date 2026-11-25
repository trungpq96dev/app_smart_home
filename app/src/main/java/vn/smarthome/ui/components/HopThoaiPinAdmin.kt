package vn.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

@Composable
fun HopThoaiPinAdmin(
    moTaHanhDong: String,
    loi: String?,
    onXacNhan: (String) -> Unit,
    onDong: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(loi) {
        if (loi != null) pin = ""
    }

    Dialog(onDismissRequest = onDong) {
        Surface(
            modifier = Modifier.width(340.dp),
            shape = RoundedCornerShape(Dim.CardRadius),
            color = SH.Card,
            border = BorderStroke(Dim.BorderWidth, SH.Border),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.Lock, SH.Amber, size = 34.dp, iconSize = 18.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Khóa PIN Kỹ thuật", style = SHType.CardTitle, color = SH.Text)
                        Text(
                            moTaHanhDong.ifBlank { "Nhập mã PIN 4 số để tiếp tục" },
                            style = SHType.Caption,
                            color = SH.TextDim,
                            maxLines = 2,
                        )
                    }
                    IconButton(onClick = onDong, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng", tint = SH.TextDim)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 4 cham PIN
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(4) { idx ->
                        val filled = idx < pin.length
                        Box(
                            Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) SH.Blue else SH.Bg)
                                .border(2.dp, if (filled) SH.Blue else SH.Border, CircleShape),
                        )
                    }
                }

                if (loi != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SH.Red.tint(0.14f))
                            .border(1.dp, SH.Red.tint(0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            loi,
                            style = SHType.Caption,
                            color = Color(0xFFFCA5A5),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "DEL"),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    rows.forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SH.Card2)
                                        .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
                                        .clickable {
                                            when (key) {
                                                "C" -> pin = ""
                                                "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                                else -> {
                                                    if (pin.length < 4) {
                                                        val next = pin + key
                                                        pin = next
                                                        if (next.length == 4) {
                                                            onXacNhan(next)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when (key) {
                                        "C" -> Text("Xóa", style = SHType.Caption, color = SH.TextDim)
                                        "DEL" -> Icon(
                                            Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Xóa một số",
                                            tint = SH.TextDim,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        else -> Text(
                                            key,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SH.Text,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "Mở khóa có hiệu lực 5 phút · Mặc định: 1234",
                    style = SHType.Caption,
                    color = SH.TextOff,
                )
            }
        }
    }
}

