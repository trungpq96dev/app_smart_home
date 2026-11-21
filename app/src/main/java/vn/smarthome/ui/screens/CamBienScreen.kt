package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.cuktech.CongSac
import vn.smarthome.data.cuktech.CongSacId
import vn.smarthome.data.cuktech.CuktechCaiDat
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.ui.components.Dot
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
 * So that lay tu `cuktech/charger/#` do `cuktech-ble-server` tren serverubuntu
 * publish (no giu ket noi BLE toi cu sac). Mot thu duy nhat GHI xuong thiet bi
 * la nut bat/tat cong, di qua `cuktech/charger/port`.
 *
 * KHONG bia so: chua nhan goi nao thi noi thang la chua co, khong ve mot cu
 * sac tuong tuong. Cu sac cung KHONG phat nhiet do, toc do quat hay cong suat
 * tong - ba thu do khong co tren man hinh nay du ban thiet ke dau tien co.
 */
@Composable
fun CamBienScreen(
    cuktech: CuktechState = CuktechState(),
    /** Cong dang cho cu sac xac nhan; UI ve "đang gửi" chu khong to mau nhu da xong. */
    dangCho: Map<CongSacId, Boolean> = emptyMap(),
    onDatCong: (CongSacId, Boolean) -> Unit = { _, _ -> },
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

        // KHONG cuon doc: man nay vua het mot man hinh ngang. Dung weight de 4
        // the cong cao het cho trong - the cao co dinh lam nua duoi man hinh
        // trong hoac, va so doc tu xa thi be. Day la man treo tuong.
        Column(
            Modifier.fillMaxWidth().weight(1f),
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
                Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            ) {
                cuktech.danhSachCong().forEach { cong ->
                    TheCongSac(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        cong = cong,
                        biTat = cuktech.congBiTat(cong.id),
                        dangCho = dangCho[cong.id],
                        onDat = { bat -> onDatCong(cong.id, bat) },
                    )
                }
            }

            HangCaiDat(cuktech.caiDat)
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
                    Text(
                        "W", style = SHType.CardTitle, color = SH.TextDim,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                // Cu sac khong phat so tong - phai noi ro day la tong cong lai,
                // khong de nguoi dung tuong la so may tu bao.
                Text("Tổng cộng từ 4 cổng", style = SHType.Caption, color = SH.TextOff)
            }

            Spacer(Modifier.weight(1f))
            DaiPhanBo(ct, Modifier.width(320.dp))
            Spacer(Modifier.width(14.dp))

            val n = ct.soCongDangChay
            StatusPill(
                if (n == 0) "Không cổng nào đang sạc" else "$n cổng đang sạc",
                if (n == 0) SH.TextOff else SH.Green,
            )
        }
    }
}

/**
 * Dai phan phoi cong suat giua cac cong.
 *
 * Tong bang 0 thi KHONG ve dai chia deu - ve mot dai xam va noi "chua sac".
 * Chia deu 0% trong lai nhu dang co so.
 */
@Composable
private fun DaiPhanBo(ct: CuktechState, modifier: Modifier = Modifier) {
    val tyLe = ct.tyLePhanBo()

    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SH.Border),
        ) {
            CongSacId.entries.forEach { id ->
                val phan = tyLe[id] ?: return@forEach
                Box(Modifier.weight(phan).fillMaxHeight().background(mauCong(id)))
            }
        }

        Spacer(Modifier.height(5.dp))

        if (tyLe.isEmpty()) {
            Text("Chưa cổng nào sạc", style = SHType.Caption, color = SH.TextOff)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CongSacId.entries.forEach { id ->
                    val phan = tyLe[id]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(if (phan == null) SH.TextOff else mauCong(id), 6.dp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            id.nhan + " " + (phan?.let { "${it.toInt()}%" } ?: "—"),
                            style = SHType.Caption,
                            color = if (phan == null) SH.TextOff else SH.TextDim,
                        )
                    }
                }
            }
        }
    }
}

/** Moi cong mot mau co dinh, de dai phan bo va the cong doc duoc cheo nhau. */
private fun mauCong(id: CongSacId): Color = when (id) {
    CongSacId.C1 -> SH.Green
    CongSacId.C2 -> SH.Blue
    CongSacId.C3 -> SH.Cyan
    CongSacId.USB_A -> SH.Amber
}

@Composable
private fun TheCongSac(
    modifier: Modifier,
    cong: CongSac,
    biTat: Boolean?,
    dangCho: Boolean?,
    onDat: (Boolean) -> Unit,
) {
    val tat = biTat == true
    val mau: Color = when {
        tat -> SH.Red
        cong.dangChay -> mauCong(cong.id)
        else -> SH.TextOff
    }

    SmartCard(
        modifier,
        borderColor = if (cong.dangChay && !tat) mau.tint(0.45f) else SH.Border,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(cong.id.nhan, style = SHType.CardTitle, color = SH.Text)
            Spacer(Modifier.weight(1f))

            // Chua biet trang thai cong (chua nhan goi settings) thi KHONG ve
            // nut bat/tat: ve ra la phai doan no dang bat hay dang tat.
            if (biTat != null) {
                NutNguon(dangBat = !tat, dangCho = dangCho, onBam = { onDat(tat) })
            }
        }

        Spacer(Modifier.weight(1f))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(soLe1(cong.oat), style = SHType.ValueBig, color = mau)
            Spacer(Modifier.width(4.dp))
            Text(
                "W", style = SHType.CardTitle, color = mau,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "${soLe1(cong.volt)} V · ${soLe2(cong.ampe)} A",
            style = SHType.Body, color = SH.TextDim,
        )

        Spacer(Modifier.weight(1f))

        StatusPill(
            when {
                dangCho != null -> if (dangCho) "Đang bật…" else "Đang tắt…"
                tat -> "Đã tắt cổng"
                else -> cong.nhanGiaoThuc()
            },
            when {
                dangCho != null -> SH.Amber
                tat -> SH.Red
                cong.dangChay -> SH.Blue
                else -> SH.TextOff
            },
            showDot = dangCho != null,
        )
    }
}

/** Nut tron bat/tat mot cong. Dang cho xac nhan thi khoa lai, khong cho bam tiep. */
@Composable
private fun NutNguon(dangBat: Boolean, dangCho: Boolean?, onBam: () -> Unit) {
    val mau = when {
        dangCho != null -> SH.Amber
        dangBat -> SH.Green
        else -> SH.Red
    }
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(mau.tint(0.18f))
            .then(if (dangCho == null) Modifier.clickable(onClick = onBam) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.PowerSettingsNew,
            contentDescription = if (dangBat) "Tắt cổng" else "Bật cổng",
            tint = mau,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * Hang cai dat doc tu bang PIID trong `cuktech/charger/settings`. CHI XEM -
 * doi cai dat la viec khac, chua lam.
 *
 * Thieu truong nao thi hien "—": chua nhan goi `settings` thi khong biet gi ca.
 */
@Composable
private fun HangCaiDat(cd: CuktechCaiDat?) {
    SmartCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Tune, SH.Purple, size = 30.dp, iconSize = 16.dp)
            Spacer(Modifier.width(10.dp))

            OCaiDat("Kịch bản", cd?.kichBan?.nhan)
            OCaiDat("Sáng màn", cd?.thoiGianSangMan)
            OCaiDat("Tự tắt màn", cd?.tuTatManNhanRoi?.let { if (it) "Bật" else "Tắt" })
            OCaiDat("Khoá xoay", cd?.khoaXoayMan?.let { if (it) "Bật" else "Tắt" })
            OCaiDat("Sạc nhỏ giọt A", cd?.sacNhoGiotUsbA?.let { if (it) "Bật" else "Tắt" })

            val hen = cd?.henGioPhut?.filterValues { it > 0 }
            OCaiDat(
                "Hẹn tắt",
                when {
                    hen == null -> null
                    hen.isEmpty() -> "Chưa đặt"
                    else -> hen.entries.joinToString(", ") { "${it.key.nhan} ${it.value} phút" }
                },
            )
        }
    }
}

@Composable
private fun RowScope.OCaiDat(nhan: String, giaTri: String?) {
    Column(Modifier.weight(1f)) {
        Text(nhan, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
        Text(
            giaTri ?: "—",
            style = SHType.BodyMed,
            color = if (giaTri == null) SH.TextOff else SH.Text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun soLe1(v: Float): String = String.format("%.1f", v)

private fun soLe2(v: Float): String = String.format("%.2f", v)
