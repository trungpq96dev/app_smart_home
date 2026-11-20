package vn.smarthome.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.cuktech.CongSac
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.ui.components.EmptyState
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Man Cam bien - hien gio chi co cu sac CUKTECH 10 Ultra.
 *
 * CHI XEM. So that lay tu `cuktech/charger/#` do `cuktech-ble-server` tren
 * serverubuntu publish (no giu ket noi BLE toi cu sac).
 *
 * KHONG bia so: chua nhan goi nao thi noi thang la chua co, khong ve mot cu
 * sac tuong tuong. Cu sac cung KHONG phat nhiet do, toc do quat hay cong suat
 * tong - ba thu do khong co tren man hinh nay du ban thiet ke dau tien co.
 */
@Composable
fun CamBienScreen(
    cuktech: CuktechState = CuktechState(),
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {

        TopBar(title = "", onBack = onBack) {
            val (chu, mau) = when {
                !cuktech.coSoThat -> "CHƯA CÓ SỐ" to SH.TextOff
                !cuktech.noiDuoc -> "MẤT KẾT NỐI" to SH.Red
                !cuktech.xacThuc -> "CHƯA XÁC THỰC" to SH.Amber
                else -> "ĐANG NỐI" to SH.Green
            }
            StatusPill(chu, mau)
            cuktech.firmware?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    "FW $it",
                    style = SHType.Caption, color = SH.TextDim,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Filled.Sensors, SH.Cyan, size = 34.dp, iconSize = 19.dp)
            Spacer(Modifier.width(8.dp))
            Text("Củ sạc CUKTECH 10 Ultra", style = SHType.PageTitle, color = SH.Text, maxLines = 1)
            Spacer(Modifier.weight(1f))
            cuktech.model?.let {
                Text(it, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
            }
        }

        if (!cuktech.coSoThat) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.UsbOff,
                    title = "Chưa có số từ củ sạc",
                    detail = "Chưa nhận được gói nào trên cuktech/charger/#. " +
                        "Kiểm tra dịch vụ cuktech-ble-server trên serverubuntu.",
                )
            }
            return@Column
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {

            // Mat ket noi thi van giu so cu tren man hinh nhung phai noi ro la so cu.
            if (!cuktech.noiDuoc || !cuktech.xacThuc) {
                SmartCard(
                    Modifier.fillMaxWidth(),
                    borderColor = if (!cuktech.noiDuoc) SH.Red else SH.Amber,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(
                            Icons.Filled.PowerOff,
                            if (!cuktech.noiDuoc) SH.Red else SH.Amber,
                            size = 30.dp, iconSize = 16.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (!cuktech.noiDuoc) {
                                "Máy chủ mất kết nối Bluetooth tới củ sạc. Số bên dưới là lần đọc cuối."
                            } else {
                                "Đã nối Bluetooth nhưng chưa bắt tay xong. Chưa có số cổng."
                            },
                            style = SHType.Body, color = SH.TextDim,
                        )
                    }
                }
            }

            TheTongCongSuat(cuktech)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                cuktech.danhSachCong().forEach { cong ->
                    TheCongSac(Modifier.weight(1f), cong)
                }
            }
        }
    }
}

@Composable
private fun TheTongCongSuat(ct: CuktechState) {
    SmartCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Bolt, SH.Amber, size = 38.dp, iconSize = 20.dp)
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(soLe1(ct.tongOat), style = SHType.ValueBig, color = SH.Text)
                    Spacer(Modifier.width(4.dp))
                    Text("W", style = SHType.CardTitle, color = SH.TextDim,
                        modifier = Modifier.padding(bottom = 5.dp))
                }
                // Cu sac khong phat so tong - phai noi ro day la tong cong lai,
                // khong de nguoi dung tuong la so may tu bao.
                Text(
                    "Tổng cộng từ ${CONG_TAT_CA} cổng",
                    style = SHType.Caption, color = SH.TextOff,
                )
            }
            Spacer(Modifier.weight(1f))
            val n = ct.soCongDangChay
            StatusPill(
                if (n == 0) "Không cổng nào đang sạc" else "$n cổng đang sạc",
                if (n == 0) SH.TextOff else SH.Green,
            )
        }
    }
}

@Composable
private fun TheCongSac(modifier: Modifier, cong: CongSac) {
    val mau: Color = if (cong.dangChay) SH.Green else SH.TextOff

    SmartCard(
        modifier.height(118.dp),
        borderColor = if (cong.dangChay) mau.tint(0.45f) else SH.Border,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cong.id.nhan, style = SHType.CardTitle, color = SH.Text)
            Spacer(Modifier.weight(1f))
            // choPhep = null nghia la goi khong noi (cong trong) - khong suy dien.
            if (cong.choPhep == false) {
                Text("đã tắt", style = SHType.Caption, color = SH.Red)
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(soLe1(cong.oat), style = SHType.Value, color = mau)
            Spacer(Modifier.width(3.dp))
            Text("W", style = SHType.Label, color = mau, modifier = Modifier.padding(bottom = 3.dp))
        }

        Spacer(Modifier.height(4.dp))

        Text(
            "${soLe1(cong.volt)}V · ${soLe2(cong.ampe)}A",
            style = SHType.Caption, color = SH.TextDim,
        )

        Spacer(Modifier.weight(1f))

        StatusPill(
            cong.nhanGiaoThuc(),
            if (cong.dangChay) SH.Blue else SH.TextOff,
            showDot = false,
        )
    }
}

private const val CONG_TAT_CA = 4

private fun soLe1(v: Float): String = String.format("%.1f", v)

private fun soLe2(v: Float): String = String.format("%.2f", v)
