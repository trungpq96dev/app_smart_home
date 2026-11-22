package vn.smarthome.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState

/**
 * Thu tu hien thi 6 muc phong lon tren luoi 3x2.
 * Hang 1: Phong ngu 1, 2, 3. Hang 2: Nha bep, Phong khach, Hanh lang.
 */
private val THU_TU_PHONG = listOf("bed1", "bed2", "bed3", "kitchen", "living", "hall")

/**
 * Man Thiet bi - luoi 6 muc phong lon.
 *
 * Moi the la mot phong (Phong ngu 1, 2, 3, Nha bep, Phong khach, Hanh lang),
 * tom tat trang thai den/quat/dieu hoa trong phong do. Bam vao the la mo man
 * chi tiet phong [RoomDetailScreen] de bat/tat va dieu chinh chi tiet.
 *
 * Voi thiet bi THAT doc tu [CongTacState]: chua ro trang thai thi hien "—",
 * khong doan la "Tat".
 */
@Composable
fun DevicesScreen(
    state: HomeState,
    onOpenRoom: (String) -> Unit,
    onBack: () -> Unit,
    /** Mac dinh rong = chua co cong tac that nao; moi the deu deo chip MAU. */
    congTac: CongTacState = CongTacState(),
) {
    val dsPhong = state.rooms
        .sortedBy { r -> THU_TU_PHONG.indexOf(r.id).let { if (it >= 0) it else Int.MAX_VALUE } }
        .map { r -> tomTatPhong(r, congTac) }

    val tongDangBat = dsPhong.sumOf { it.soDangBat }
    val tongThietBi = dsPhong.size * 3

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Thiết bị", onBack = null) {
            StatusPill("${dsPhong.size} phòng", SH.Blue, showDot = false)
            StatusPill(
                "$tongDangBat/$tongThietBi thiết bị đang bật",
                if (tongDangBat > 0) SH.Green else SH.TextOff,
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            dsPhong.chunked(3).forEach { hang ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
                ) {
                    hang.forEach { tomTat ->
                        ThePhong(
                            tomTat = tomTat,
                            onClick = { onOpenRoom(tomTat.room.id) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    repeat(3 - hang.size) {
                        Spacer(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

private data class TrangThaiMucThietBi(
    val ten: String,
    val icon: ImageVector,
    val mau: Color,
    val bat: Boolean,
    val chuTrangThai: String,
    val that: Boolean,
    val roRang: Boolean,
)

private data class TomTatPhongUi(
    val room: RoomUi,
    val mucThietBi: List<TrangThaiMucThietBi>,
) {
    val coCongTacThat: Boolean get() = mucThietBi.any { it.that }
    val soDangBat: Int get() = mucThietBi.count { it.bat }
}

private fun tomTatPhong(room: RoomUi, ct: CongTacState): TomTatPhongUi {
    if (room.id == "kitchen") {
        val den = mucCongTac(room.id, "den", "Đèn", Icons.Filled.Lightbulb, SH.Amber, room.lightOn, ct)
        val hutMuiGoc = mucCongTac(room.id, "quat", "Hút mùi", Icons.Filled.Air, SH.Blue, room.fanOn, ct)
        val hutMui = if (hutMuiGoc.roRang && hutMuiGoc.bat) {
            hutMuiGoc.copy(chuTrangThai = "Cấp ${room.hoodSpeed}")
        } else {
            hutMuiGoc
        }
        val bepTu = TrangThaiMucThietBi(
            ten = "Bếp từ",
            icon = Icons.Filled.LocalFireDepartment,
            mau = SH.Orange,
            bat = room.stoveOn,
            chuTrangThai = if (room.stoveOn) "Mức ${room.stoveLevel}" else "Tắt",
            that = false,
            roRang = true,
        )
        return TomTatPhongUi(room = room, mucThietBi = listOf(den, hutMui, bepTu))
    }
    val den = mucCongTac(room.id, "den", "Đèn", Icons.Filled.Lightbulb, SH.Amber, room.lightOn, ct)
    val quat = mucCongTac(room.id, "quat", "Quạt", Icons.Filled.Air, SH.Blue, room.fanOn, ct)
    val dieuHoa = TrangThaiMucThietBi(
        ten = "Điều hòa",
        icon = Icons.Filled.AcUnit,
        mau = SH.Purple,
        bat = room.acOn,
        chuTrangThai = if (room.acOn) "${room.acTemp}°C" else "Tắt",
        that = false,
        roRang = true,
    )
    return TomTatPhongUi(room = room, mucThietBi = listOf(den, quat, dieuHoa))
}

private fun mucCongTac(
    roomId: String,
    loai: String,
    ten: String,
    icon: ImageVector,
    mau: Color,
    giaTriRoom: Boolean,
    ct: CongTacState,
): TrangThaiMucThietBi {
    val that = ct.laThat(roomId, loai)
    val tt = ct.trangThaiCua(roomId, loai)
    val roRang = !that || (ct.bridgeSong && tt?.online == true && tt.bat != null)
    val bat = giaTriRoom && roRang
    val chu = if (!roRang) "—" else if (giaTriRoom) "Bật" else "Tắt"
    return TrangThaiMucThietBi(
        ten = ten,
        icon = icon,
        mau = mau,
        bat = bat,
        chuTrangThai = chu,
        that = that,
        roRang = roRang,
    )
}

@Composable
private fun ThePhong(
    tomTat: TomTatPhongUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val room = tomTat.room
    val coThietBiBat = tomTat.soDangBat > 0
    val vienMau = if (coThietBiBat) room.accent.tint(0.55f) else SH.Border
    val hinh = RoundedCornerShape(Dim.CardRadius)

    Column(
        modifier
            .clip(hinh)
            .background(SH.Card)
            .border(Dim.BorderWidth, vienMau, hinh)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        // ---- Dau the: icon + ten phong + chip THAT/MAU ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                icon = room.icon,
                color = if (coThietBiBat) room.accent else SH.TextOff,
                size = 44.dp,
                iconSize = 24.dp,
                filled = coThietBiBat,
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    room.name,
                    style = SHType.PageTitle,
                    color = SH.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val tenCheDo = if (room.id == "kitchen") {
                    when (room.mode) {
                        RoomMode.HOME -> "Nấu ăn"
                        RoomMode.CUSTOM -> "Ăn tối"
                        RoomMode.SLEEP -> "Dọn dẹp"
                        RoomMode.AWAY -> "Vắng nhà"
                    }
                } else {
                    room.mode.label
                }
                val moTa = buildString {
                    append("Chế độ: ").append(tenCheDo)
                    if (room.scheduleOn && room.id != "kitchen") append(" • ").append(room.scheduleTime)
                }
                Text(
                    moTa,
                    style = SHType.Caption,
                    color = SH.TextDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(6.dp))
            val chipMau = if (tomTat.coCongTacThat) SH.Green else SH.Amber
            Text(
                if (tomTat.coCongTacThat) "THẬT" else "MẪU",
                style = SHType.Caption,
                color = chipMau,
                modifier = Modifier
                    .clip(RoundedCornerShape(5.dp))
                    .background(chipMau.tint(0.14f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---- Giua the: 3 o tom tat Den / Quat / Dieu hoa ----
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            tomTat.mucThietBi.forEach { muc ->
                OThietBiNho(muc = muc, modifier = Modifier.weight(1f).fillMaxHeight())
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Chan the: tong dang bat + goi y bam de chinh ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(if (coThietBiBat) SH.Green else SH.TextOff, 8.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                if (coThietBiBat) "${tomTat.soDangBat}/3 đang bật" else "Tất cả đang tắt",
                style = SHType.Caption,
                color = if (coThietBiBat) SH.Green else SH.TextOff,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Vào chỉnh →",
                style = SHType.Label,
                color = room.accent,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun OThietBiNho(
    muc: TrangThaiMucThietBi,
    modifier: Modifier = Modifier,
) {
    val nen = if (muc.bat) muc.mau.tint(0.12f) else SH.Card2
    val vien = if (muc.bat) muc.mau.tint(0.35f) else SH.Border
    val hinh = RoundedCornerShape(10.dp)

    Column(
        modifier
            .clip(hinh)
            .background(nen)
            .border(Dim.BorderWidth, vien, hinh)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            muc.icon,
            contentDescription = null,
            tint = if (muc.bat) muc.mau else SH.TextOff,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            muc.ten,
            style = SHType.Label,
            color = SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            muc.chuTrangThai,
            style = SHType.BodyMed,
            color = if (!muc.roRang) SH.TextDim else if (muc.bat) muc.mau else SH.TextOff,
            maxLines = 1,
        )
    }
}

@Preview(widthDp = 994, heightDp = 589, showBackground = true)
@Composable
private fun DevicesPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            DevicesScreen(HomeState(), {}, {})
        }
    }
}
