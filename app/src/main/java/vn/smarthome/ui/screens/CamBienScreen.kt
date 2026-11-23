package vn.smarthome.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UsbOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.smarthome.data.cuktech.CongSac
import vn.smarthome.data.cuktech.CongSacId
import vn.smarthome.data.cuktech.CuktechCaiDat
import vn.smarthome.data.cuktech.CuktechPiid
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.data.cuktech.KichBan
import vn.smarthome.data.cuktech.SongBle
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.EmptyState
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatusPill
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
    dangNoiLai: Boolean = false,
    onNoiLai: () -> Unit = {},
    /** PIID dang cho cu sac xac nhan. */
    choCaiDat: Set<Int> = emptySet(),
    onDatCaiDat: (Int, Int) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    var hopThoai by remember { mutableStateOf<HopThoaiCaiDat?>(null) }

    Column(Modifier.fillMaxSize()) {

        val (chu, mau) = when {
            !cuktech.coSoThat -> "CHƯA CÓ SỐ" to SH.TextOff
            dangNoiLai -> "ĐANG NỐI LẠI" to SH.Amber
            cuktech.matKetNoiThat() -> "MẤT KẾT NỐI" to SH.Red
            cuktech.coMauThuan() -> "TRẠNG THÁI CŨ" to SH.Amber
            !cuktech.xacThuc -> "CHƯA XÁC THỰC" to SH.Amber
            else -> "ĐANG NỐI" to SH.Green
        }

        Row(
            Modifier.fillMaxWidth().height(Dim.HeaderHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBadge(Icons.Filled.Sensors, SH.Cyan, size = 34.dp, iconSize = 19.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Củ sạc CUKTECH 10 Ultra",
                        style = SHType.PageTitle,
                        color = SH.Text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    cuktech.model?.let {
                        Text(
                            it,
                            style = SHType.Caption,
                            color = SH.TextOff,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusPill(chu, mau)
                PillSong(cuktech.songTotNhat())
                cuktech.firmware?.let {
                    Text(
                        "FW $it",
                        style = SHType.Caption,
                        color = SH.TextDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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

            BangCanhBao(cuktech, dangNoiLai, onNoiLai)

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

            HangCaiDat(
                cd = cuktech.caiDat,
                choCaiDat = choCaiDat,
                onDat = onDatCaiDat,
                onMoHopThoai = { hopThoai = it },
            )
        }
    }

    when (hopThoai) {
        HopThoaiCaiDat.KICH_BAN -> HopThoaiChon(
            tieuDe = "Kịch bản phân phối công suất",
            moTa = "Củ sạc tự chia công suất giữa các cổng theo kịch bản này.",
            luaChon = KichBan.entries.map { it.ma to it.nhan },
            dangChon = cuktech.caiDat?.kichBan?.ma,
            onChon = { onDatCaiDat(CuktechPiid.KICH_BAN, it) },
            onDong = { hopThoai = null },
        )

        HopThoaiCaiDat.SANG_MAN -> HopThoaiChon(
            tieuDe = "Thời gian sáng màn hình",
            moTa = "Màn hình trên thân củ sạc tắt sau bao lâu không ai đụng tới.",
            luaChon = CuktechPiid.SANG_MAN_CHON,
            dangChon = cuktech.caiDat?.tho?.get(CuktechPiid.SANG_MAN),
            onChon = { onDatCaiDat(CuktechPiid.SANG_MAN, it) },
            onDong = { hopThoai = null },
        )

        HopThoaiCaiDat.HEN_GIO -> HopThoaiHenGio(
            cd = cuktech.caiDat,
            choCaiDat = choCaiDat,
            onDat = onDatCaiDat,
            onDong = { hopThoai = null },
        )

        null -> Unit
    }
}

/** Ba muc phai chon trong danh sach; ba muc con lai la cong tac nen bam thang. */
private enum class HopThoaiCaiDat { KICH_BAN, SANG_MAN, HEN_GIO }

/**
 * Bang canh bao khi co gi do khong on.
 *
 * Phan biet hai chuyen rat khac nhau ma truoc day gop lam mot:
 *
 *  - MAT THAT: co `connected` bao mat VA khong con goi nao moi.
 *  - BAN TIN CU: co bao mat nhung bon topic cong van doi so. Gap that ngay
 *    21/11/2026 - `cuktech/charger/status` la ban tin retained, no ket o
 *    `connected: false` tu mot phien cu trong khi BLE van song. Luc do keu
 *    "mat ket noi" la noi sai.
 */
@Composable
private fun BangCanhBao(ct: CuktechState, dangNoiLai: Boolean, onNoiLai: () -> Unit) {
    val matThat = ct.matKetNoiThat()
    val mauThuan = ct.coMauThuan()
    val chuaXacThuc = ct.noiDuoc && !ct.xacThuc
    // Song yeu la canh bao RIENG, hien ca khi dang noi duoc: no la thu giai
    // thich vi sao lat nua lai mat ket noi. Doi den luc mat moi noi thi nguoi
    // dung da di kiem tra nham cho roi.
    val song = ct.songTotNhat()
    val songYeu = song != null && !song.laSoCu() &&
        (song.muc == SongBle.Muc.YEU || song.muc == SongBle.Muc.RAT_YEU)
    if (!matThat && !mauThuan && !chuaXacThuc && !dangNoiLai && !songYeu) return

    val mau = when {
        matThat -> SH.Red
        song != null && song.muc == SongBle.Muc.RAT_YEU && !matThat && !dangNoiLai -> SH.Red
        else -> SH.Amber
    }

    SmartCard(Modifier.fillMaxWidth(), borderColor = mau) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (songYeu && !matThat && !dangNoiLai) {
                    Icons.Filled.BluetoothSearching
                } else {
                    Icons.Filled.PowerOff
                },
                mau, size = 30.dp, iconSize = 16.dp,
            )
            Spacer(Modifier.width(8.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        dangNoiLai ->
                            "Đang bảo máy chủ nối lại Bluetooth. Bắt tay lại mất khoảng 40 giây."
                        matThat ->
                            "Máy chủ mất kết nối Bluetooth tới củ sạc. Số bên dưới là lần đọc cuối."
                        mauThuan ->
                            "Máy chủ báo mất kết nối nhưng số từng cổng vẫn về đều — nhiều khả " +
                                "năng bản tin trạng thái bị cũ chứ thiết bị không sao."
                        chuaXacThuc ->
                            "Đã nối Bluetooth nhưng chưa bắt tay xong. Chưa có số cổng."
                        else ->
                            "Sóng Bluetooth tới củ sạc đang yếu — rớt kết nối là do khoảng " +
                                "cách, không phải do phần mềm."
                    },
                    style = SHType.Body, color = SH.TextDim,
                )
                // Liet ke TUNG diem do chu khong chi diem manh nhat: muc dich
                // cua no la so sanh "dongle o server" voi "tram ESP canh cu
                // sac" de biet doi cho co an thua khong.
                if (songYeu || matThat) HangDiemDo(ct)
            }

            if (matThat || mauThuan || dangNoiLai) {
                Spacer(Modifier.width(10.dp))
                NutNoiLai(dangNoiLai, onNoiLai)
            }
        }
    }
}

/** Mau theo suc song. Khong ro thi xam - KHONG to xanh khi chua biet gi. */
private fun mauSong(muc: SongBle.Muc): Color = when (muc) {
    SongBle.Muc.TOT -> SH.Green
    SongBle.Muc.KHA -> SH.Amber
    SongBle.Muc.YEU -> SH.Amber
    SongBle.Muc.RAT_YEU -> SH.Red
    SongBle.Muc.KHONG_RO -> SH.TextOff
}

/**
 * Pill suc song tren thanh tren cung.
 *
 * Chua do duoc lan nao thi KHONG ve gi: mot pill xam "—" chiem cho ma khong
 * noi them dieu gi so voi viec khong co no. So qua cu cung vay - deo them
 * chu "cũ" chi lam thanh tren dai ra tren man treo tuong.
 */
@Composable
private fun PillSong(song: SongBle?) {
    if (song?.rssi == null) return
    val mau = mauSong(song.muc)
    Spacer(Modifier.width(6.dp))
    StatusPill(
        if (song.laSoCu()) "Sóng ${song.nhanDbm()} (cũ)" else "Sóng ${song.nhanDbm()}",
        mau,
    )
}

/** Mot dong "serverubuntu −81 dBm · Yếu" cho moi diem do. */
@Composable
private fun HangDiemDo(ct: CuktechState) {
    if (ct.song.isEmpty()) return
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ct.song.values.sortedBy { it.nguonId }.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(mauSong(s.muc), 6.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    "${s.nguonTen ?: s.nguonId}: ${s.nhanDbm()} · ${s.muc.nhan}" +
                        if (s.laSoCu()) " (số cũ)" else "",
                    style = SHType.Caption, color = SH.TextOff, maxLines = 1,
                )
            }
        }
    }
}

/** Bam la gui `{"enabled":false}` roi `{"enabled":true}` vao `cuktech/charger/ble`. */
@Composable
private fun NutNoiLai(dangNoiLai: Boolean, onBam: () -> Unit) {
    val mau = if (dangNoiLai) SH.TextOff else SH.Blue
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(mau.tint(0.16f))
            .then(if (dangNoiLai) Modifier else Modifier.clickable(onClick = onBam))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            tint = mau,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            if (dangNoiLai) "Đang nối lại…" else "Nối lại",
            style = SHType.Button, color = mau, maxLines = 1,
        )
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
 * Hang cai dat doc tu bang PIID trong `cuktech/charger/settings`.
 *
 * Ba muc 0/1 (tu tat man, khoa xoay, sac nho giot) bam thang la dao. Ba muc
 * con lai phai chon trong danh sach nen mo hop thoai.
 *
 * Thieu truong nao thi hien "—" va KHONG cho bam: chua biet gia tri hien tai
 * thi khong dao duoc, dao la doan.
 */
@Composable
private fun HangCaiDat(
    cd: CuktechCaiDat?,
    choCaiDat: Set<Int>,
    onDat: (Int, Int) -> Unit,
    onMoHopThoai: (HopThoaiCaiDat) -> Unit,
) {
    SmartCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Tune, SH.Purple, size = 30.dp, iconSize = 16.dp)
            Spacer(Modifier.width(10.dp))

            OCaiDat(
                nhan = "Kịch bản",
                giaTri = cd?.kichBan?.nhan,
                dangCho = CuktechPiid.KICH_BAN in choCaiDat,
                onBam = { onMoHopThoai(HopThoaiCaiDat.KICH_BAN) },
            )
            OCaiDat(
                nhan = "Sáng màn",
                giaTri = cd?.thoiGianSangMan,
                dangCho = CuktechPiid.SANG_MAN in choCaiDat,
                onBam = { onMoHopThoai(HopThoaiCaiDat.SANG_MAN) },
            )
            OCongTac("Tự tắt màn", cd?.tuTatManNhanRoi, CuktechPiid.TU_TAT_MAN, choCaiDat, onDat)
            OCongTac("Khoá xoay", cd?.khoaXoayMan, CuktechPiid.KHOA_XOAY, choCaiDat, onDat)
            OCongTac(
                "Sạc nhỏ giọt A", cd?.sacNhoGiotUsbA, CuktechPiid.SAC_NHO_GIOT_A,
                choCaiDat, onDat,
            )

            val hen = cd?.henGioPhut?.filterValues { it > 0 }
            OCaiDat(
                nhan = "Hẹn tắt",
                giaTri = when {
                    hen == null -> null
                    hen.isEmpty() -> "Chưa đặt"
                    else -> hen.entries.joinToString(", ") { "${it.key.nhan} ${it.value} phút" }
                },
                dangCho = CongSacId.entries.any { CuktechPiid.henGio(it) in choCaiDat },
                onBam = { onMoHopThoai(HopThoaiCaiDat.HEN_GIO) },
            )
        }
    }
}

/** Mot muc 0/1: bam thang la dao, khong qua hop thoai. */
@Composable
private fun RowScope.OCongTac(
    nhan: String,
    dangBat: Boolean?,
    piid: Int,
    choCaiDat: Set<Int>,
    onDat: (Int, Int) -> Unit,
) {
    OCaiDat(
        nhan = nhan,
        giaTri = dangBat?.let { if (it) "Bật" else "Tắt" },
        dangCho = piid in choCaiDat,
        mauGiaTri = when (dangBat) {
            true -> SH.Green
            false -> SH.TextDim
            null -> null
        },
        onBam = dangBat?.let { { onDat(piid, if (it) 0 else 1) } },
    )
}

@Composable
private fun RowScope.OCaiDat(
    nhan: String,
    giaTri: String?,
    dangCho: Boolean = false,
    mauGiaTri: Color? = null,
    onBam: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onBam != null && !dangCho) Modifier.clickable(onClick = onBam) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        Text(nhan, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
        Text(
            when {
                dangCho -> "Đang đổi…"
                giaTri != null -> giaTri
                else -> "—"
            },
            style = SHType.BodyMed,
            color = when {
                dangCho -> SH.Amber
                giaTri == null -> SH.TextOff
                else -> mauGiaTri ?: SH.Text
            },
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Hop thoai chon mot gia tri trong danh sach. */
@Composable
private fun HopThoaiChon(
    tieuDe: String,
    moTa: String,
    luaChon: List<Pair<Int, String>>,
    dangChon: Int?,
    onChon: (Int) -> Unit,
    onDong: () -> Unit,
) {
    KhungHopThoai(tieuDe, moTa, onDong) {
        luaChon.forEach { (ma, nhan) ->
            val chon = ma == dangChon
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (chon) SH.Blue.tint(0.16f) else Color.Transparent)
                    .clickable {
                        if (!chon) onChon(ma)
                        onDong()
                    }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    nhan,
                    style = SHType.BodyMed,
                    color = if (chon) SH.Blue else SH.Text,
                    modifier = Modifier.weight(1f),
                )
                if (chon) Dot(SH.Blue, 8.dp)
            }
        }
    }
}

/**
 * Hop thoai hen gio tat tung cong.
 *
 * Khong cho go so tu do (PIID nhan 0-1440) vi day la man treo tuong: keo ban
 * phim ao len de go "45" la nhoc hon moi thu khac tren man nay.
 */
@Composable
private fun HopThoaiHenGio(
    cd: CuktechCaiDat?,
    choCaiDat: Set<Int>,
    onDat: (Int, Int) -> Unit,
    onDong: () -> Unit,
) {
    KhungHopThoai(
        "Hẹn giờ tắt cổng",
        "Sau khoảng thời gian này củ sạc tự ngắt cổng đó.",
        onDong,
    ) {
        CongSacId.entries.forEach { cong ->
            val piid = CuktechPiid.henGio(cong)
            val hienTai = cd?.henGioPhut?.get(cong)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    cong.nhan,
                    style = SHType.CardTitle, color = SH.Text,
                    modifier = Modifier.width(66.dp),
                )
                if (piid in choCaiDat) {
                    Text("Đang đổi…", style = SHType.Body, color = SH.Amber)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CuktechPiid.HEN_GIO_CHON.forEach { (phut, nhan) ->
                            val chon = hienTai == phut
                            Text(
                                nhan,
                                style = SHType.Caption,
                                color = if (chon) SH.Blue else SH.TextDim,
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (chon) SH.Blue.tint(0.16f) else SH.Border.tint(0.5f)
                                    )
                                    .clickable(enabled = hienTai != null && !chon) {
                                        onDat(piid, phut)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KhungHopThoai(
    tieuDe: String,
    moTa: String,
    onDong: () -> Unit,
    noiDung: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDong) {
        Surface(
            modifier = Modifier.width(430.dp),
            shape = RoundedCornerShape(Dim.CardRadius),
            color = SH.Card,
            border = BorderStroke(Dim.BorderWidth, SH.Border),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(tieuDe, style = SHType.PageTitle, color = SH.Text)
                Spacer(Modifier.height(4.dp))
                Text(moTa, style = SHType.Caption, color = SH.TextOff)
                Spacer(Modifier.height(12.dp))
                noiDung()
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Đóng",
                        style = SHType.Button, color = SH.TextDim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onDong)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

private fun soLe1(v: Float): String = String.format("%.1f", v)

private fun soLe2(v: Float): String = String.format("%.2f", v)
