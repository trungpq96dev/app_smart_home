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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.TrangThaiCam
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
 * Man Cai dat - hai duong noi ra ngoai cua app, moi duong mot cot:
 *
 *  - Trai: broker EMQX, de doc so dien that.
 *  - Phai: go2rtc, de lay hinh camera.
 *
 * Hai cot chu khong xep chong nhau: man hinh chi cao 601dp, xep chong thi
 * phan camera nam duoi day va phai cuon mot doan dai moi thay.
 *
 * Mat khau go o day nam trong DataStore cua may tinh bang va da loai khoi
 * sao luu dam may. Ma nguon KHONG nhung san mat khau nao, vi no nam tren
 * GitHub.
 */
@Composable
fun SettingsScreen(
    cauHinh: CauHinhMqtt,
    ketNoi: KetNoi,
    cauHinhCamera: CauHinhCamera,
    trangThaiCam: TrangThaiCam,
    onLuu: (CauHinhMqtt) -> Unit,
    onLuuCamera: (CauHinhCamera) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Cài đặt", onBack = onBack) {
            TrangThaiKetNoi(ketNoi)
            Spacer(Modifier.width(8.dp))
            TrangThaiCameraPill(trangThaiCam)
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                FormBroker(cauHinh, onLuu)
            }
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                FormCamera(cauHinhCamera, trangThaiCam, onLuuCamera)
            }
        }
    }
}

// ---------------------------------------------------------------- Broker

@Composable
private fun FormBroker(cauHinh: CauHinhMqtt, onLuu: (CauHinhMqtt) -> Unit) {
    var host by remember(cauHinh.host) { mutableStateOf(cauHinh.host) }
    var cong by remember(cauHinh.cong) { mutableStateOf(cauHinh.cong.toString()) }
    var tls by remember(cauHinh.tls) { mutableStateOf(cauHinh.tls) }
    var taiKhoan by remember(cauHinh.taiKhoan) { mutableStateOf(cauHinh.taiKhoan) }
    var matKhau by remember(cauHinh.matKhau) { mutableStateOf(cauHinh.matKhau) }
    var clientId by remember(cauHinh.clientId) { mutableStateOf(cauHinh.clientId) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Broker EMQX — số điện", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Nơi script dien_evn đẩy số điện lên.",
            style = SHType.Caption, color = SH.TextDim,
        )
        Spacer(Modifier.height(10.dp))

        O("Địa chỉ broker", host, { host = it }, "vd: emqx.trongnha.vn")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(110.dp)) {
                O("Cổng", cong, { cong = it.filter { c -> c.isDigit() }.take(5) }, "8883", soThoi = true)
            }
            CongTac(
                bat = tls,
                onDoi = { tls = it },
                ten = "TLS",
                phu = if (tls) "ssl:// — nên bật" else "tcp:// — không mã hoá",
                phuCanhBao = !tls,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))

        O("Tài khoản MQTT", taiKhoan, { taiKhoan = it }, "để trống nếu broker không yêu cầu")
        Spacer(Modifier.height(8.dp))

        OMatKhau("Mật khẩu MQTT", matKhau, { matKhau = it }, "để trống nếu broker không yêu cầu")
        Spacer(Modifier.height(8.dp))

        O("Client ID", clientId, { clientId = it }, "để trống thì app tự sinh")
        Text(
            "Phải khác client id của bo bơm và bo đèn — trùng là EMQX đá cả hai ra.",
            style = SHType.Caption, color = SH.Amber,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(14.dp))
        Ghi(
            "App chỉ đọc",
            "Đăng ký " + DienTopic.GOC + " trên broker. Mọi topic đều retained nên vừa " +
                "kết nối là có số ngay. App không publish vào dien/ — ghi vào là đè lên " +
                "số thật của đường ống.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Nối được mà vẫn trống?",
            "Thường là tài khoản MQTT chưa được cấp quyền đọc dien/# trên EMQX. " +
                "App báo rõ trường hợp đó ở góc trên bên phải.",
        )
    }
}

// ---------------------------------------------------------------- Camera

@Composable
private fun FormCamera(
    cauHinh: CauHinhCamera,
    trangThai: TrangThaiCam,
    onLuu: (CauHinhCamera) -> Unit,
) {
    var host by remember(cauHinh.host) { mutableStateOf(cauHinh.host) }
    var cong by remember(cauHinh.cong) { mutableStateOf(cauHinh.cong.toString()) }
    var tls by remember(cauHinh.tls) { mutableStateOf(cauHinh.tls) }
    var taiKhoan by remember(cauHinh.taiKhoan) { mutableStateOf(cauHinh.taiKhoan) }
    var matKhau by remember(cauHinh.matKhau) { mutableStateOf(cauHinh.matKhau) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("go2rtc — hình camera", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Máy chạy go2rtc trong nhà. Camera Tuya không nối thẳng với app.",
            style = SHType.Caption, color = SH.TextDim,
        )
        Spacer(Modifier.height(10.dp))

        O("Địa chỉ máy chạy go2rtc", host, { host = it }, "vd: 192.168.0.105")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(110.dp)) {
                O("Cổng", cong, { cong = it.filter { c -> c.isDigit() }.take(5) }, "1984", soThoi = true)
            }
            CongTac(
                bat = tls,
                onDoi = { tls = it },
                ten = "HTTPS",
                phu = if (tls) "khi đi qua tailscale" else "http — chỉ trong mạng nhà",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))

        O("Tài khoản go2rtc", taiKhoan, { taiKhoan = it }, "để trống nếu go2rtc không đặt")
        Spacer(Modifier.height(8.dp))
        OMatKhau("Mật khẩu go2rtc", matKhau, { matKhau = it }, "để trống nếu go2rtc không đặt")

        if (trangThai is TrangThaiCam.Loi) {
            Text(
                trangThai.moTa,
                style = SHType.Caption, color = SH.Amber,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                onLuu(
                    CauHinhCamera(
                        host = host,
                        cong = cong.toIntOrNull() ?: 1984,
                        tls = tls,
                        taiKhoan = taiKhoan,
                        matKhau = matKhau,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(Dim.ButtonH),
        ) {
            Icon(
                Icons.Filled.Refresh, contentDescription = null,
                tint = SH.Blue, modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Lưu và dò lại camera", style = SHType.Button, color = SH.Blue)
        }

        Spacer(Modifier.height(14.dp))
        Ghi(
            "Đường đi của hình",
            "Camera Tuya → go2rtc (đăng nhập tài khoản Tuya, kéo luồng về) → máy tính " +
                "bảng. App chỉ nói chuyện với go2rtc và không bao giờ đăng nhập Tuya, " +
                "nên tài khoản Tuya chỉ nằm trong go2rtc.yaml trên máy chạy go2rtc.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Khung hình đen?",
            "Ba lý do hay gặp: máy tính bảng và máy chạy go2rtc không cùng một Wi-Fi; " +
                "tường lửa Windows chưa mở cổng; hoặc go2rtc đã tắt. Địa chỉ localhost " +
                "thì không bao giờ chạy trên máy tính bảng — phải là địa chỉ của máy đó " +
                "trong mạng.",
        )
    }
}

// ---------------------------------------------------------------- Chung

@Composable
private fun Ghi(tieuDe: String, than: String) {
    Text(tieuDe, style = SHType.BodyMed, color = SH.Text)
    Spacer(Modifier.height(3.dp))
    Text(than, style = SHType.Caption, color = SH.TextDim)
}

@Composable
private fun CongTac(
    bat: Boolean,
    onDoi: (Boolean) -> Unit,
    ten: String,
    phu: String,
    modifier: Modifier = Modifier,
    phuCanhBao: Boolean = false,
) {
    Row(
        modifier.height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(
            checked = bat,
            onCheckedChange = onDoi,
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
            Text(ten, style = SHType.BodyMed, color = SH.Text)
            Text(
                phu,
                style = SHType.Caption,
                color = if (phuCanhBao) SH.Amber else SH.TextDim,
            )
        }
    }
}

/**
 * O nhap mat khau: che chu, co nut con mat de xem lai.
 *
 * Moi o giu rieng trang thai an/hien cua no. May tinh bang nay treo tuong,
 * ai di qua cung doc duoc man hinh - de mat khau hien chu thuong la hong.
 */
@Composable
private fun OMatKhau(
    nhan: String,
    giaTri: String,
    onDoi: (String) -> Unit,
    goiY: String,
) {
    var hien by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = giaTri,
        onValueChange = onDoi,
        label = { Text(nhan, style = SHType.Caption) },
        placeholder = { Text(goiY, style = SHType.Caption, color = SH.TextOff) },
        singleLine = true,
        visualTransformation = if (hien) VisualTransformation.None
        else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { hien = !hien }) {
                Icon(
                    if (hien) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (hien) "Ẩn mật khẩu" else "Hiện mật khẩu",
                    tint = SH.TextDim,
                    modifier = Modifier.size(19.dp),
                )
            }
        },
        textStyle = SHType.Body,
        colors = mauO(),
        modifier = Modifier.fillMaxWidth(),
    )
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
            SettingsScreen(
                CauHinhMqtt(), KetNoi.ChuaCauHinh,
                CauHinhCamera(), TrangThaiCam.ChuaCauHinh,
                {}, {}, {},
            )
        }
    }
}
