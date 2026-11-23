package vn.smarthome.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CauHinhBomNuoc
import vn.smarthome.data.bomnuoc.KenhBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.dien.CauHinhMqtt
import vn.smarthome.data.dien.DienTopic
import vn.smarthome.data.giongnoi.BanSongCong
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.giongnoi.MucDung
import vn.smarthome.data.giongnoi.MucDungNgay
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.ota.OtaStatus
import vn.smarthome.data.ota.ReleaseInfo
import vn.smarthome.data.thoitiet.CauHinhThoiTiet
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.components.TrangThaiKetNoiBomPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Man Cai dat - ba duong noi ra ngoai cua app, moi duong mot cot:
 *
 *  - Trai: broker EMQX, de doc so dien that.
 *  - Giua: go2rtc, de lay hinh camera.
 *  - Phai: Open-Meteo, de lay thoi tiet (chi can toa do, khong co gi bi mat).
 *
 * Ba cot chu khong xep chong nhau: man hinh chi cao 601dp, xep chong thi
 * phan duoi nam khuat va phai cuon mot doan dai moi thay.
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
    cauHinhThoiTiet: CauHinhThoiTiet,
    cauHinhGiongNoi: CauHinhGiongNoi,
    mucDungGiongNoi: List<MucDungNgay>,
    onLuu: (CauHinhMqtt) -> Unit,
    onLuuCamera: (CauHinhCamera) -> Unit,
    onLuuThoiTiet: (CauHinhThoiTiet) -> Unit,
    onLuuGiongNoi: (CauHinhGiongNoi) -> Unit,
    onBack: () -> Unit,
    otaStatus: OtaStatus = OtaStatus.Idling,
    currentVersion: String = "0.1",
    onKiemTraCapNhat: () -> Unit = {},
    onBatDauCapNhat: (ReleaseInfo) -> Unit = {},
    cauHinhBomNuoc: CauHinhBomNuoc = CauHinhBomNuoc(),
    ketNoiBom: TrangThaiKetNoiBom = TrangThaiKetNoiBom.ChuaCauHinh,
    kenhBom: KenhBomNuoc = KenhBomNuoc.KHONG,
    bomNuoc: BomNuocState? = null,
    onLuuBomNuoc: (CauHinhBomNuoc) -> Unit = {},
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val coBanMoi = otaStatus is OtaStatus.UpdateAvailable

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Cài đặt", onBack = null) {
            // Tab chuyen doi giua Dich vu va Cap nhat
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Bg)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TabButton(
                    text = "Dịch vụ & Kết nối",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                )
                TabButton(
                    text = "Trợ lý giọng nói",
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                )
                TabButton(
                    text = "Bơm nước",
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                )
                TabButton(
                    text = if (coBanMoi) "Cập nhật (Mới!)" else "Cập nhật",
                    selected = selectedTab == 1,
                    highlight = coBanMoi,
                    onClick = { selectedTab = 1 },
                )
            }

            Spacer(Modifier.width(8.dp))
            if (selectedTab == 3) {
                TrangThaiKetNoiBomPill(ketNoiBom, kenhBom)
            } else {
                TrangThaiKetNoi(ketNoi)
                Spacer(Modifier.width(6.dp))
                TrangThaiCameraPill(trangThaiCam)
            }
        }

        Spacer(Modifier.height(8.dp))

        if (selectedTab == 0) {
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
                SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                    FormThoiTiet(cauHinhThoiTiet, onLuuThoiTiet)
                }
            }
        } else if (selectedTab == 2) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                SmartCard(Modifier.weight(3f).fillMaxHeight(), padding = 14.dp) {
                    FormGiongNoi(cauHinhGiongNoi, onLuuGiongNoi)
                }
                SmartCard(Modifier.weight(2f).fillMaxHeight(), padding = 14.dp) {
                    ThongKeGiongNoi(mucDungGiongNoi, cauHinhGiongNoi)
                }
                SmartCard(Modifier.weight(2f).fillMaxHeight(), padding = 14.dp) {
                    GhiChuGiongNoi()
                }
            }
        } else if (selectedTab == 3) {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                SmartCard(Modifier.weight(1.3f).fillMaxHeight(), padding = 14.dp) {
                    FormBomNuoc(cauHinhBomNuoc, onLuuBomNuoc)
                }
                SmartCard(Modifier.weight(0.7f).fillMaxHeight(), padding = 14.dp) {
                    TrangThaiBomNuocCard(cauHinhBomNuoc, ketNoiBom, kenhBom, bomNuoc)
                }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                // Cot 1: Thong tin thiet bi & phien ban
                SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 16.dp) {
                    ThongTinHeThong(currentVersion)
                }

                // Cot 2: Trung tam OTA Update
                SmartCard(Modifier.weight(2f).fillMaxHeight(), padding = 16.dp) {
                    FormOtaUpdate(
                        currentVersion = currentVersion,
                        otaStatus = otaStatus,
                        onKiemTra = onKiemTraCapNhat,
                        onCapNhat = onBatDauCapNhat,
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    highlight: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    selected -> SH.Blue
                    highlight -> SH.Amber.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SHType.Caption,
            color = if (selected) Color.White else if (highlight) SH.Amber else SH.TextDim,
            fontWeight = if (selected || highlight) FontWeight.Bold else FontWeight.Normal,
        )
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

// ---------------------------------------------------------------- Thoi tiet

@Composable
private fun FormThoiTiet(cauHinh: CauHinhThoiTiet, onLuu: (CauHinhThoiTiet) -> Unit) {
    var ten by remember(cauHinh.ten) { mutableStateOf(cauHinh.ten) }
    var lat by remember(cauHinh.lat) { mutableStateOf(cauHinh.lat.toString()) }
    var lon by remember(cauHinh.lon) { mutableStateOf(cauHinh.lon.toString()) }

    // Toa do nhap vao doc duoc khong - de bao do (vuot khoang hop le).
    val la = lat.trim().toDoubleOrNull()
    val lo = lon.trim().toDoubleOrNull()
    val hopLe = ten.isNotBlank() &&
        la != null && la in -90.0..90.0 &&
        lo != null && lo in -180.0..180.0

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Thời tiết — Open-Meteo", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Lấy thời tiết thật cho Trang chủ. Nguồn miễn phí, không cần API key.",
            style = SHType.Caption, color = SH.TextDim,
        )
        Spacer(Modifier.height(10.dp))

        O("Tên địa điểm", ten, { ten = it }, "vd: Hà Nội")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                O("Vĩ độ (lat)", lat, { lat = it }, "21.0278")
            }
            Box(Modifier.weight(1f)) {
                O("Kinh độ (lon)", lon, { lon = it }, "105.8342")
            }
        }
        Text(
            "Toạ độ tra trên Google Maps: bấm giữ một điểm, cặp số hiện ra là vĩ độ, kinh độ.",
            style = SHType.Caption, color = SH.TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (!hopLe) {
            Text(
                "Vĩ độ trong khoảng -90..90, kinh độ -180..180. Sai thì giữ nguyên địa điểm cũ.",
                style = SHType.Caption, color = SH.Amber,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onLuu(
                    CauHinhThoiTiet(
                        ten = ten,
                        lat = la ?: cauHinh.lat,
                        lon = lo ?: cauHinh.lon,
                    )
                )
            },
            enabled = hopLe,
            colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
            modifier = Modifier.fillMaxWidth().height(Dim.ButtonH),
        ) {
            Text("Lưu và tải lại thời tiết", style = SHType.Button, color = Color.White)
        }

        Spacer(Modifier.height(14.dp))
        Ghi(
            "Vì sao chỉ cần toạ độ",
            "Open-Meteo nhận thẳng vĩ độ/kinh độ nên không phải khai tên thành phố. " +
                "Tên địa điểm ở trên chỉ để hiện trên thẻ, không ảnh hưởng số liệu.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Mặc định Hà Nội",
            "Để trống hoặc nhập sai thì app vẫn lấy thời tiết Hà Nội. Đổi địa điểm " +
                "xong là thẻ ở Trang chủ tự tải lại ngay.",
        )
    }
}

// ---------------------------------------------------------- Tro ly giong noi

@Composable
private fun FormGiongNoi(cauHinh: CauHinhGiongNoi, onLuu: (CauHinhGiongNoi) -> Unit) {
    var khoa by remember(cauHinh.khoaApi) { mutableStateOf(cauHinh.khoaApi) }
    var model by remember(cauHinh.model) { mutableStateOf(cauHinh.model) }
    var giong by remember(cauHinh.giongDoc) { mutableStateOf(cauHinh.giongDoc) }
    var guiMqtt by remember(cauHinh.guiMqtt) { mutableStateOf(cauHinh.guiMqtt) }
    var banSongCong by remember(cauHinh.banSongCong) { mutableStateOf(cauHinh.banSongCong) }
    var imLang by remember(cauHinh.giayImLangTuDong) {
        mutableStateOf(cauHinh.giayImLangTuDong.toString())
    }
    var toiDa by remember(cauHinh.giayToiDaPhien) {
        mutableStateOf(cauHinh.giayToiDaPhien.toString())
    }
    var phutNgay by remember(cauHinh.phutMoiNgay) {
        mutableStateOf(cauHinh.phutMoiNgay.toString())
    }

    val sIm = imLang.trim().toIntOrNull()
    val sToiDa = toiDa.trim().toIntOrNull()
    val sPhutNgay = phutNgay.trim().toIntOrNull()
    // Tran phien khong duoc vuot 15 phut: Live API tu cat phien audio o do.
    val hopLe = khoa.isNotBlank() && model.isNotBlank() &&
        sIm != null && sIm in 0..600 &&
        sToiDa != null && sToiDa in 60..900 &&
        sPhutNgay != null && sPhutNgay in 0..1440

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Trợ lý giọng nói — Gemini Live", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Nói chuyện tự nhiên và điều khiển thiết bị bằng giọng nói. " +
                "Máy tính bảng nối thẳng tới Gemini, không qua server nào.",
            style = SHType.Caption, color = SH.TextDim,
        )
        Spacer(Modifier.height(10.dp))

        OMatKhau("Khóa API Gemini", khoa, { khoa = it }, "lấy ở Google AI Studio")
        Spacer(Modifier.height(8.dp))

        O("Model", model, { model = it }, CauHinhGiongNoi.MODEL_MAC_DINH)
        Text(
            "Google đổi tên model Live khá thường xuyên nên ô này nhập tay. " +
                "Báo \"Không có model này\" thì tra lại tên trên trang model của Google.",
            style = SHType.Caption, color = SH.TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.height(8.dp))

        Text("Giọng đọc", style = SHType.Caption, color = SH.TextDim)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CauHinhGiongNoi.GIONG_CO_SAN.take(4).forEach { g ->
                ChipGiong(g, giong == g) { giong = g }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CauHinhGiongNoi.GIONG_CO_SAN.drop(4).forEach { g ->
                ChipGiong(g, giong == g) { giong = g }
            }
        }

        Spacer(Modifier.height(6.dp))
        CongTac(
            bat = guiMqtt,
            onDoi = { guiMqtt = it },
            ten = "Bắn cả lệnh của thiết bị mẫu ra MQTT",
            phu = "Để gỡ rối. Lệnh vào công tắc THẬT thì luôn được gửi, không cần bật ô này",
        )

        Spacer(Modifier.height(4.dp))
        Text("Khử vọng (tiếng loa lọt vào mic)", style = SHType.Caption, color = SH.TextDim)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BanSongCong.entries.forEach { b ->
                ChipGiong(b.nhan, banSongCong == b) { banSongCong = b }
            }
        }
        Text(
            "Tự động: máy không có bộ khử vọng phần cứng thì tạm ngừng mic lúc loa " +
                "đang nói. Trợ lý tự nói chuyện với chính nó thì chọn \"Luôn bật\".",
            style = SHType.Caption, color = SH.TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                O("Im lặng bao lâu thì đóng (giây)", imLang, { imLang = it }, "45", soThoi = true)
            }
            Box(Modifier.weight(1f)) {
                O("Trần một phiên (giây)", toiDa, { toiDa = it }, "600", soThoi = true)
            }
        }
        Spacer(Modifier.height(10.dp))
        O("Trần mỗi ngày (phút)", phutNgay, { phutNgay = it }, "30", soThoi = true)
        Text(
            "Nói hết số phút này là app không cho mở phiên nữa cho tới hôm sau. " +
                "Để 0 là không chặn gì — chỉ nên để 0 khi chắc chắn tài khoản chưa " +
                "gắn thanh toán.",
            style = SHType.Caption, color = SH.TextDim,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (!hopLe) {
            Text(
                "Cần khóa API và tên model. Im lặng 0-600 giây (0 là không tự đóng), " +
                    "trần phiên 60-900 giây, trần ngày 0-1440 phút.",
                style = SHType.Caption, color = SH.Amber,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onLuu(
                    CauHinhGiongNoi(
                        khoaApi = khoa,
                        model = model,
                        giongDoc = giong,
                        guiMqtt = guiMqtt,
                        banSongCong = banSongCong,
                        giayImLangTuDong = sIm ?: cauHinh.giayImLangTuDong,
                        giayToiDaPhien = sToiDa ?: cauHinh.giayToiDaPhien,
                        phutMoiNgay = sPhutNgay ?: cauHinh.phutMoiNgay,
                    )
                )
            },
            enabled = hopLe,
            colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
            modifier = Modifier.fillMaxWidth().height(Dim.ButtonH),
        ) {
            Text("Lưu cấu hình trợ lý", style = SHType.Button, color = Color.White)
        }
    }
}

@Composable
private fun ChipGiong(nhan: String, chon: Boolean, onChon: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (chon) SH.Blue else SH.Card2)
            .clickable(onClick = onChon)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Text(nhan, style = SHType.Caption, color = if (chon) Color.White else SH.TextDim)
    }
}

/**
 * Muc dung tro ly giong noi.
 *
 * So phut la do chinh app dem nen chac chan dung. So token lay tu
 * `usageMetadata` Gemini gui ve - xem ghi chu "Vi sao so token chi la can
 * duoi" ben duoi.
 */
@Composable
private fun ThongKeGiongNoi(ds: List<MucDungNgay>, cauHinh: CauHinhGiongNoi) {
    val homNay = MucDung.cuaNgay(ds, MucDung.homNay())
    val tranGiay = cauHinh.phutMoiNgay * 60
    val phanTram = if (tranGiay > 0) (homNay.giay.toFloat() / tranGiay).coerceIn(0f, 1f) else 0f
    val giay7 = MucDung.giayNgayGanDay(ds, 7)

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Đã dùng bao nhiêu", style = SHType.CardTitle, color = SH.Text)
        Spacer(Modifier.height(10.dp))

        Text("Hôm nay", style = SHType.Caption, color = SH.TextDim)
        Text(
            text = dinhDangPhut(homNay.giay) +
                if (cauHinh.phutMoiNgay > 0) " / ${cauHinh.phutMoiNgay} phút" else "",
            style = SHType.Value,
            color = when {
                cauHinh.phutMoiNgay <= 0 -> SH.Text
                phanTram >= 1f -> SH.Red
                phanTram >= 0.8f -> SH.Amber
                else -> SH.Green
            },
        )
        if (cauHinh.phutMoiNgay > 0) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { phanTram },
                color = if (phanTram >= 1f) SH.Red else if (phanTram >= 0.8f) SH.Amber else SH.Green,
                trackColor = SH.Card2,
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (phanTram >= 1f) "Đã chạm trần — mai mới nói tiếp được."
                else "Còn " + dinhDangPhut(tranGiay - homNay.giay),
                style = SHType.Caption,
                color = if (phanTram >= 1f) SH.Red else SH.TextDim,
            )
        } else {
            Text("Chưa đặt trần ngày", style = SHType.Caption, color = SH.Amber)
        }

        Spacer(Modifier.height(12.dp))
        MucThongSo("Số phiên hôm nay", homNay.soPhien.toString())
        MucThongSo("7 ngày gần nhất", dinhDangPhut(giay7))
        MucThongSo(
            "Token hôm nay",
            if (homNay.tokenTong > 0) dinhDangSo(homNay.tokenTong) else "--",
        )

        if (ds.any { it.giay > 0 }) {
            Spacer(Modifier.height(12.dp))
            Text("Những ngày gần đây", style = SHType.Caption, color = SH.TextDim)
            Spacer(Modifier.height(4.dp))
            ds.takeLast(7).reversed().forEach { n ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(
                        n.ngay.removePrefix(n.ngay.take(5)).removePrefix("-"),
                        style = SHType.Caption, color = SH.TextDim,
                        modifier = Modifier.width(52.dp),
                    )
                    Text(dinhDangPhut(n.giay), style = SHType.Caption, color = SH.Text)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Ghi(
            "Gói miễn phí thì không mất tiền",
            "Project chưa gắn thanh toán thì vượt hạn mức chỉ bị trả lỗi 429, " +
                "không phát sinh phí. Chỉ khi bạn gắn thẻ hoặc dùng tín dụng từ gói " +
                "AI Pro thì mọi token mới được tính tiền — lúc đó cái trần ngày ở " +
                "bên trái mới thật sự là cái phanh.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Vì sao số token chỉ là cận dưới",
            "Tài liệu Google không nói rõ usageMetadata là số của riêng từng gói tin " +
                "hay số cộng dồn cả phiên. App lấy giá trị lớn nhất thấy được: đúng " +
                "nếu là số cộng dồn, và thấp hơn thực tế nếu là số từng gói. Số phút " +
                "thì do app tự đếm nên luôn chính xác.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Số thật nằm ở Google",
            "Hạn mức miễn phí của riêng tài khoản bạn xem ở aistudio.google.com/rate-limit. " +
                "Hóa đơn thật thì ở Google Cloud Billing. Con số ở đây để bạn tự canh, " +
                "không thay được hai chỗ đó.",
        )
    }
}

private fun dinhDangPhut(giay: Int): String = when {
    giay <= 0 -> "0 phút"
    giay < 60 -> "$giay giây"
    else -> {
        val p = giay / 60
        val g = giay % 60
        if (g == 0) "$p phút" else "$p phút $g giây"
    }
}

private fun dinhDangSo(n: Long): String =
    n.toString().reversed().chunked(3).joinToString(".").reversed()

@Composable
private fun GhiChuGiongNoi() {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Cần biết", style = SHType.CardTitle, color = SH.Text)
        Spacer(Modifier.height(10.dp))

        Ghi(
            "Khóa nằm trên máy tính bảng",
            "Khóa lưu trong DataStore của máy, đã loại khỏi sao lưu đám mây, và " +
                "KHÔNG nằm trong mã nguồn — vì mã nguồn ở GitHub và bản APK phát " +
                "hành công khai cho OTA. Siết thêm thì đặt hạn mức cho khóa ở Google " +
                "AI Studio.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Tính tiền theo phút",
            "Gemini Live tính theo thời lượng nói chuyện. Máy này treo tường nên " +
                "phiên luôn tự đóng khi im lặng và có trần cứng — đừng tắt hai thứ đó.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Thiết bị thật vs dữ liệu mẫu",
            "Công tắc đã nối qua bộ nối máy chủ (như Phòng ngủ 1) điều khiển đồ vật " +
                "ngoài đời thật. Các phòng còn lại vẫn là dữ liệu mẫu — bảng thoại " +
                "ghi rõ điều đó dưới từng lệnh.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Số liệu thì thật",
            "Hỏi điện, server, WiFi, thời tiết, lịch âm thì trợ lý đọc đúng số đang " +
                "có. Chưa nối được nguồn nào thì nó nói thẳng là chưa có số, " +
                "không bịa.",
        )
        Spacer(Modifier.height(10.dp))
        Ghi(
            "Quyền micro",
            "Lần đầu bấm nút mic, Android sẽ hỏi quyền. Từ chối thì nút mic vẫn " +
                "hiện nhưng không thu được gì.",
        )
    }
}

// ---------------------------------------------------------------- Bom nuoc

@Composable
private fun FormBomNuoc(
    cauHinh: CauHinhBomNuoc,
    onLuu: (CauHinhBomNuoc) -> Unit,
) {
    var espHost by remember(cauHinh.espHost) { mutableStateOf(cauHinh.espHost) }
    var uuTienLan by remember(cauHinh.uuTienLan) { mutableStateOf(cauHinh.uuTienLan) }
    var brokerHost by remember(cauHinh.brokerHost) { mutableStateOf(cauHinh.brokerHost) }
    var brokerPort by remember(cauHinh.brokerPort) { mutableStateOf(cauHinh.brokerPort.toString()) }
    var brokerTls by remember(cauHinh.brokerTls) { mutableStateOf(cauHinh.brokerTls) }
    var brokerUser by remember(cauHinh.brokerUser) { mutableStateOf(cauHinh.brokerUser) }
    var brokerPass by remember(cauHinh.brokerPass) { mutableStateOf(cauHinh.brokerPass) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Bo bơm nước ESP32 — AquaControl", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Tách biệt với Mosquitto số điện. Dùng LAN 1s khi ở nhà và MQTT 30s khi ở xa.",
            style = SHType.Caption,
            color = SH.TextDim,
        )
        Spacer(Modifier.height(10.dp))

        O("Địa chỉ bo trong mạng LAN", espHost, { espHost = it }, "vd: 192.168.0.120 hoặc bomnuoc.local")
        Spacer(Modifier.height(4.dp))
        CongTac(
            bat = uuTienLan,
            onDoi = { uuTienLan = it },
            ten = "Ưu tiên kênh LAN (HTTP 1 giây/lần)",
            phu = "Tự chuyển sang MQTT nếu ra khỏi mạng nhà",
        )
        Spacer(Modifier.height(6.dp))

        O("Địa chỉ broker MQTT", brokerHost, { brokerHost = it }, "vd: abc123.ala.asia-southeast1.emqxsl.com")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(110.dp)) {
                O("Cổng", brokerPort, { brokerPort = it.filter { c -> c.isDigit() }.take(5) }, "8883", soThoi = true)
            }
            CongTac(
                bat = brokerTls,
                onDoi = { brokerTls = it },
                ten = "TLS",
                phu = if (brokerTls) "ssl:// — EMQX Cloud" else "tcp:// — không mã hoá",
                phuCanhBao = !brokerTls,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))

        O("Tài khoản MQTT bơm", brokerUser, { brokerUser = it }, "")
        Spacer(Modifier.height(8.dp))
        OMatKhau("Mật khẩu MQTT bơm", brokerPass, { brokerPass = it }, "")
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                onLuu(
                    CauHinhBomNuoc(
                        espHost = espHost,
                        uuTienLan = uuTienLan,
                        brokerHost = brokerHost,
                        brokerPort = brokerPort.toIntOrNull() ?: 8883,
                        brokerTls = brokerTls,
                        brokerUser = brokerUser,
                        brokerPass = brokerPass,
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

@Composable
private fun TrangThaiBomNuocCard(
    cauHinh: CauHinhBomNuoc,
    ketNoi: TrangThaiKetNoiBom,
    kenh: KenhBomNuoc,
    bomNuoc: BomNuocState?,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Trạng thái đường truyền", style = SHType.CardTitle, color = SH.Text)
        Spacer(Modifier.height(8.dp))

        MucThongSo("Trạng thái", ketNoi.nhan)
        MucThongSo("Kênh đang dùng", when (kenh) {
            KenhBomNuoc.LAN -> "LAN trực tiếp (1s)"
            KenhBomNuoc.MQTT -> "MQTT (nhịp ${bomNuoc?.hb ?: 30}s)"
            KenhBomNuoc.KHONG -> "Chưa kết nối"
        })
        MucThongSo("Địa chỉ LAN", cauHinh.espHost.ifBlank { "Chưa nhập" })
        MucThongSo("Broker MQTT", cauHinh.brokerHost.ifBlank { "Chưa nhập" })
        MucThongSo("Mật khẩu MQTT", if (cauHinh.brokerPass.isNotBlank()) "Đã lưu trên máy" else "Chưa nhập")
        bomNuoc?.fw?.let { MucThongSo("Firmware bo", it) }
        bomNuoc?.ip?.takeIf { it.isNotBlank() }?.let { MucThongSo("IP hiện tại", it) }

        Spacer(Modifier.height(12.dp))
        Ghi(
            "Lưu ý bảo mật",
            "Mật khẩu broker chỉ lưu trong DataStore trên máy tính bảng và đã bị loại khỏi sao lưu đám mây, không nhúng trong mã nguồn.",
        )
        Spacer(Modifier.height(8.dp))
        Ghi(
            "Topic cố định",
            "App đọc bomnuoc/state, bomnuoc/status, bomnuoc/usage và gửi lệnh qua bomnuoc/cmd hoặc /api/cmd.",
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

// ---------------------------------------------------------------- He thong & OTA

@Composable
private fun ThongTinHeThong(currentVersion: String) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Text("Thông tin thiết bị", style = SHType.CardTitle, color = SH.Text)
        Text(
            "Cấu hình phần cứng và thông số ứng dụng.",
            style = SHType.Caption, color = SH.TextDim,
        )
        Spacer(Modifier.height(14.dp))

        MucThongSo("Ứng dụng", "SmartHome Android")
        MucThongSo("Phiên bản hiện tại", "v$currentVersion")
        MucThongSo("Thiết bị", "Samsung Galaxy Tab A7 Lite")
        MucThongSo("Độ phân giải UI", "1340 × 800 (Kiosk)")
        MucThongSo("Kênh phát hành", "GitHub Releases OTA")
        MucThongSo("Kho lưu trữ (Git)", "trungpq96dev/app_smart_home")

        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SH.Bg)
                .padding(10.dp)
        ) {
            Column {
                Text(
                    "Lưu ý khi cập nhật:",
                    style = SHType.Caption,
                    color = SH.Amber,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Cần cấp quyền 'Cài đặt ứng dụng không rõ nguồn gốc' cho SmartHome 1 lần duy nhất trên máy tính bảng để hệ điều hành có thể cài đè bản mới.",
                    style = SHType.Caption,
                    color = SH.TextDim,
                )
            }
        }
    }
}

@Composable
private fun MucThongSo(nhan: String, giaTri: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nhan, style = SHType.Caption, color = SH.TextDim)
        Text(giaTri, style = SHType.Body, color = SH.Text, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FormOtaUpdate(
    currentVersion: String,
    otaStatus: OtaStatus,
    onKiemTra: () -> Unit,
    onCapNhat: (ReleaseInfo) -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Cập nhật phần mềm (OTA)", style = SHType.CardTitle, color = SH.Text)
                Text(
                    "Kiểm tra và cập nhật trực tiếp từ kho lưu trữ Git.",
                    style = SHType.Caption, color = SH.TextDim,
                )
            }

            Button(
                onClick = onKiemTra,
                colors = ButtonDefaults.buttonColors(containerColor = SH.Card2),
                shape = RoundedCornerShape(8.dp),
                enabled = otaStatus !is OtaStatus.Checking && otaStatus !is OtaStatus.Downloading,
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(16.dp), tint = SH.Text)
                Spacer(Modifier.width(6.dp))
                Text("Kiểm tra ngay", style = SHType.Caption, color = SH.Text)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Trang thai hien tai
        when (otaStatus) {
            is OtaStatus.Checking -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Đang kết nối GitHub để kiểm tra phiên bản mới...", style = SHType.Body, color = SH.Blue)
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth(0.6f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = SH.Blue,
                            trackColor = SH.Border,
                        )
                    }
                }
            }

            is OtaStatus.UpToDate -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.CheckCircle, color = SH.Green, size = 36.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Ứng dụng đang ở phiên bản mới nhất!", style = SHType.Body, color = SH.Green, fontWeight = FontWeight.Bold)
                            Text("Phiên bản hiện tại v$currentVersion đã là bản phát hành mới nhất trên Git.", style = SHType.Caption, color = SH.TextDim)
                        }
                    }
                }
            }

            is OtaStatus.UpdateAvailable -> {
                val info = otaStatus.info
                val sizeMb = info.sizeBytes / (1024f * 1024f)

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Default.SystemUpdate, color = SH.Blue, size = 36.dp, iconSize = 20.dp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Phát hiện phiên bản mới: ${info.tagName}", style = SHType.Body, color = SH.Blue, fontWeight = FontWeight.Bold)
                                if (sizeMb > 0) {
                                    Text(String.format("Dung lượng: %.1f MB", sizeMb), style = SHType.Caption, color = SH.TextDim)
                                }
                            }
                        }

                        Button(
                            onClick = { onCapNhat(info) },
                            colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tải & Cập nhật ngay", style = SHType.Body, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (info.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Nhật ký cập nhật (Changelog):", style = SHType.Caption, color = SH.TextDim)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SH.Card)
                                .padding(10.dp)
                        ) {
                            Text(info.notes, style = SHType.Body, color = SH.Text)
                        }
                    }
                }
            }

            is OtaStatus.Downloading -> {
                val percent = otaStatus.progressPercent
                val downMb = otaStatus.bytesDownloaded / (1024f * 1024f)
                val totalMb = otaStatus.totalBytes / (1024f * 1024f)

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Đang tải bản cập nhật...", style = SHType.Body, color = SH.Blue, fontWeight = FontWeight.Bold)
                        Text("$percent%", style = SHType.Body, color = SH.Blue, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { percent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = SH.Blue,
                        trackColor = SH.Border,
                    )

                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (totalMb > 0) String.format("Đã tải: %.1f / %.1f MB", downMb, totalMb)
                        else "Đang kết nối...",
                        style = SHType.Caption,
                        color = SH.TextDim,
                    )
                }
            }

            is OtaStatus.Error -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Lỗi khi kiểm tra cập nhật:", style = SHType.Body, color = SH.Red, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(otaStatus.message, style = SHType.Caption, color = SH.TextDim)
                    }
                }
            }

            is OtaStatus.Idling, is OtaStatus.ReadyToInstall -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SH.Bg)
                        .padding(14.dp)
                ) {
                    Text(
                        "Bấm 'Kiểm tra ngay' để đối chiếu phiên bản đang chạy với kho GitHub.",
                        style = SHType.Caption,
                        color = SH.TextDim,
                    )
                }
            }
        }
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
                CauHinhThoiTiet(), CauHinhGiongNoi(), emptyList(),
                {}, {}, {}, {}, {},
            )
        }
    }
}
