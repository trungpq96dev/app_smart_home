package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lightbulb
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
import vn.smarthome.data.RoomUi
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState

/**
 * Man Thiet bi - KHUNG HIEN THI TRANG THAI, chua dieu khien qua MQTT.
 *
 * Moi phong co 3 thiet bi (den, quat, dieu hoa) -> moi cai mot the. Trang
 * thai lay tu [HomeState.rooms], dung nguon voi man chi tiet phong, nen bat
 * den o phong roi qua day thay doi theo ngay.
 *
 * Tu 21/11/2026 nha co hai cong tac THAT (bed1: den + quat), con lai van la
 * du lieu mau. Nen chip "MAU" chuyen tu header xuong TUNG THE - mot co toan
 * cuc khong con noi dung su that nua. The that ma chua doc duoc trang thai
 * thi hien dau gach "—", khong hien "Tat".
 *
 * Bam vao the chi MO MAN CHI TIET PHONG, khong gui lenh tu day.
 */
@Composable
fun DevicesScreen(
    state: HomeState,
    onOpenRoom: (String) -> Unit,
    onBack: () -> Unit,
    /** Mac dinh rong = chua co cong tac that nao; moi the deu deo chip MAU. */
    congTac: CongTacState = CongTacState(),
) {
    val thietBi = thietBiTuPhong(state.rooms, congTac)

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Thiết bị", onBack = null)

        // Dong dem: bao nhieu dang bat / tong.
        val dangBat = thietBi.count { it.bat }
        Text(
            dangBat.toString() + "/" + thietBi.size + " thiết bị đang bật",
            style = SHType.Caption, color = SH.TextDim,
            modifier = Modifier.padding(start = 2.dp, bottom = 8.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 228.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(thietBi, key = { it.id }) { tb ->
                TheThietBi(tb, onClick = { onOpenRoom(tb.roomId) })
            }
        }
    }
}

/** Mot thiet bi suy ra tu phong. */
private data class ThietBiUi(
    val id: String,
    val roomId: String,
    val loai: String,
    val phong: String,
    val icon: ImageVector,
    val mau: Color,
    val bat: Boolean,
    val trangThai: String,
    /** false = du lieu mau, the deo chip "MAU". */
    val that: Boolean = false,
    /** false = thiet bi that nhung khong doc duoc trang thai -> ve "—". */
    val roRang: Boolean = true,
)

/** Bung danh sach the tu cac phong. */
private fun thietBiTuPhong(rooms: List<RoomUi>, ct: CongTacState): List<ThietBiUi> = buildList {
    rooms.forEach { r ->
        // Thiet bi that: trang thai lay tu broker, va khong doc duoc thi ve
        // "—" chu khong ve "Tat".
        val ttDen = ct.trangThaiCua(r.id, "den")
        val denThat = ct.laThat(r.id, "den")
        val denRo = !denThat || (ttDen?.online == true && ttDen.bat != null)
        add(ThietBiUi(
            id = r.id + "-den", roomId = r.id, loai = "Đèn", phong = r.name,
            icon = Icons.Filled.Lightbulb, mau = SH.Amber,
            bat = r.lightOn && denRo,
            trangThai = if (!denRo) "—" else if (r.lightOn) "Bật" else "Tắt",
            that = denThat, roRang = denRo,
        ))
        val ttQuat = ct.trangThaiCua(r.id, "quat")
        val quatThat = ct.laThat(r.id, "quat")
        val quatRo = !quatThat || (ttQuat?.online == true && ttQuat.bat != null)
        add(ThietBiUi(
            id = r.id + "-quat", roomId = r.id, loai = "Quạt", phong = r.name,
            icon = Icons.Filled.Air, mau = SH.Blue,
            bat = r.fanOn && quatRo,
            trangThai = if (!quatRo) "—" else if (r.fanOn) "Bật" else "Tắt",
            that = quatThat, roRang = quatRo,
        ))
        add(ThietBiUi(
            id = r.id + "-dh", roomId = r.id, loai = "Điều hòa", phong = r.name,
            icon = Icons.Filled.AcUnit, mau = SH.Purple,
            bat = r.acOn, trangThai = if (r.acOn) r.acTemp.toString() + "°C" else "Tắt",
        ))
    }
}

@Composable
private fun TheThietBi(tb: ThietBiUi, onClick: () -> Unit) {
    val vienMau = if (tb.bat) tb.mau.tint(0.5f) else SH.Border
    Column(
        Modifier
            .height(132.dp)
            .clip(RoundedCornerShape(Dim.CardRadius))
            .background(SH.Card)
            .border(Dim.BorderWidth, vienMau, RoundedCornerShape(Dim.CardRadius))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon sang mau khi bat, xam khi tat - nhin phat biet trang thai.
            IconBadge(
                tb.icon,
                if (tb.bat) tb.mau else SH.TextOff,
                size = 38.dp, iconSize = 21.dp,
                filled = tb.bat,
            )
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!tb.that) {
                    // Chip theo TUNG the: mot co toan cuc khong con noi dung
                    // su that khi nha vua that vua mau.
                    Text(
                        "MẪU",
                        style = SHType.Caption,
                        color = SH.Amber,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(SH.Amber.tint(0.14f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Dot(if (tb.bat) SH.Green else SH.TextOff, 8.dp)
                Spacer(Modifier.width(5.dp))
                Text(
                    if (!tb.roRang) "—" else if (tb.bat) "Bật" else "Tắt",
                    style = SHType.Caption,
                    color = if (!tb.roRang) SH.TextDim else if (tb.bat) SH.Green else SH.TextOff,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(tb.loai, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        Text(
            tb.phong,
            style = SHType.Caption, color = SH.TextDim,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tb.trangThai,
            style = SHType.BodyMed,
            color = if (tb.bat) tb.mau else SH.TextOff,
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
