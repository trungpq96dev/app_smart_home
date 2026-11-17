package vn.smarthome.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.dien.CauHinhMqtt
import vn.smarthome.data.dien.DienTopic
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Man Cai dat - cho toi gio chi co mot viec: khai bao broker EMQX de doc
 * so dien that.
 *
 * Mat khau go o day duoc luu trong DataStore cua may tinh bang va da loai
 * khoi sao luu dam may. Ma nguon KHONG nhung san mat khau nao, vi no nam
 * tren GitHub.
 */
@Composable
fun SettingsScreen(
    cauHinh: CauHinhMqtt,
    ketNoi: KetNoi,
    onLuu: (CauHinhMqtt) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember(cauHinh.host) { mutableStateOf(cauHinh.host) }
    var cong by remember(cauHinh.cong) { mutableStateOf(cauHinh.cong.toString()) }
    var tls by remember(cauHinh.tls) { mutableStateOf(cauHinh.tls) }
    var taiKhoan by remember(cauHinh.taiKhoan) { mutableStateOf(cauHinh.taiKhoan) }
    var matKhau by remember(cauHinh.matKhau) { mutableStateOf(cauHinh.matKhau) }
    var clientId by remember(cauHinh.clientId) { mutableStateOf(cauHinh.clientId) }
    var hienMatKhau by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Cài đặt", onBack = onBack) {
            TrangThaiKetNoi(ketNoi)
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------- Form ----------
            SmartCard(Modifier.weight(1.1f).fillMaxHeight(), padding = 14.dp) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Broker EMQX", style = SHType.CardTitle, color = SH.Text)
                    Text(
                        "Nơi script dien_evn đẩy số điện lên.",
                        style = SHType.Caption, color = SH.TextDim,
                    )
                    Spacer(Modifier.height(10.dp))

                    O("Địa chỉ broker", host, { host = it }, "vd: emqx.trongnha.vn")
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(120.dp)) {
                            O("Cổng", cong, { cong = it.filter { c -> c.isDigit() }.take(5) }, "8883", soThoi = true)
                        }
                        Row(
                            Modifier.weight(1f).height(56.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Switch(
                                checked = tls,
                                onCheckedChange = { tls = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SH.Green,
                                    checkedBorderColor = SH.Green,
                                    uncheckedThumbColor = SH.TextDim,
                                    uncheckedTrackColor = SH.Card2,
                                    uncheckedBorderColor = SH.Border,
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("TLS", style = SHType.BodyMed, color = SH.Text)
                                Text(
                                    if (tls) "ssl:// — nên bật" else "tcp:// — không mã hoá",
                                    style = SHType.Caption,
                                    color = if (tls) SH.TextDim else SH.Amber,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    O("Tài khoản MQTT", taiKhoan, { taiKhoan = it }, "để trống nếu broker không yêu cầu")
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = matKhau,
                        onValueChange = { matKhau = it },
                        label = { Text("Mật khẩu MQTT", style = SHType.Caption) },
                        singleLine = true,
                        visualTransformation = if (hienMatKhau) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { hienMatKhau = !hienMatKhau }) {
                                Icon(
                                    if (hienMatKhau) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (hienMatKhau) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = SH.TextDim,
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        },
                        textStyle = SHType.Body,
                        colors = mauO(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))

                    O("Client ID", clientId, { clientId = it }, "để trống thì app tự sinh")
                    Text(
                        "Phải khác client id của bo bơm và bo đèn — trùng là EMQX đá cả hai ra.",
                        style = SHType.Caption, color = SH.Amber,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            onLuu(
                                CauHinhMqtt(
                                    host = host,
                                    cong = cong.toIntOrNull() ?: 8883,
                                    tls = tls,
                                    taiKhoan = taiKhoan,
                                    matKhau = matKhau,
                                    clientId = clientId,
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                        modifier = Modifier.fillMaxWidth().height(Dim.ButtonH),
                    ) {
                        Text("Lưu và kết nối lại", style = SHType.Button, color = Color.White)
                    }
                }
            }

            // ---------- Giai thich ----------
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("App đọc gì", style = SHType.CardTitle, color = SH.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Đăng ký " + DienTopic.GOC + " trên broker. Mọi topic đều retained " +
                            "nên vừa kết nối là có số ngay, không phải chờ cữ chạy kế tiếp.",
                        style = SHType.Body, color = SH.TextDim,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("App không ghi gì", style = SHType.CardTitle, color = SH.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Số điện do script dien_evn trên serverubuntu quyết định. App chỉ " +
                            "là màn hình xem, không publish vào dien/<topic> — ghi vào là đè lên " +
                            "số thật của đường ống.",
                        style = SHType.Body, color = SH.TextDim,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Chưa thấy số?", style = SHType.CardTitle, color = SH.Text)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kết nối được mà vẫn trống thì thường là tài khoản MQTT chưa được " +
                            "cấp quyền đọc dien/# trên EMQX. App báo rõ trường hợp đó ở góc " +
                            "trên bên phải.",
                        style = SHType.Body, color = SH.TextDim,
                    )
                }
            }
        }
    }
}

@Composable
private fun O(
    nhan: String,
    giaTri: String,
    onDoi: (String) -> Unit,
    goiY: String,
    soThoi: Boolean = false,
) {
    OutlinedTextField(
        value = giaTri,
        onValueChange = onDoi,
        label = { Text(nhan, style = SHType.Caption) },
        placeholder = { Text(goiY, style = SHType.Caption, color = SH.TextOff) },
        singleLine = true,
        keyboardOptions = if (soThoi) KeyboardOptions(keyboardType = KeyboardType.Number)
        else KeyboardOptions.Default,
        textStyle = SHType.Body,
        colors = mauO(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun mauO() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SH.Text,
    unfocusedTextColor = SH.Text,
    focusedBorderColor = SH.Blue,
    unfocusedBorderColor = SH.Border,
    focusedLabelColor = SH.Blue,
    unfocusedLabelColor = SH.TextDim,
    cursorColor = SH.Blue,
)

@Composable
fun TrangThaiKetNoi(ketNoi: KetNoi) {
    when (ketNoi) {
        is KetNoi.DaNoi -> StatusPill("Đã nối broker", SH.Green)
        is KetNoi.DangNoi -> StatusPill("Đang nối...", SH.Amber)
        is KetNoi.ChuaCauHinh -> StatusPill("Chưa khai báo broker", SH.TextOff)
        is KetNoi.Loi -> StatusPill("Lỗi kết nối", SH.Red)
    }
}

@Preview(widthDp = 830, heightDp = 601, showBackground = true)
@Composable
private fun SettingsPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            SettingsScreen(CauHinhMqtt(), KetNoi.ChuaCauHinh, {}, {})
        }
    }
}
