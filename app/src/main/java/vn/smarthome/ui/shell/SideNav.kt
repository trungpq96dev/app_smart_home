package vn.smarthome.ui.shell

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.LoiBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.giongnoi.PhienGiongNoi
import vn.smarthome.data.server.ServerState
import vn.smarthome.ui.components.NutTroLySidebar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Cot menu trai co dinh:
 *   - Tren cung: Logo SmartHome
 *   - Cum 1 (HANG NGAY): Trang chu, Thiet bi, Camera, Tien ich
 *   - Cum 2 (HE THONG): Tu dong hoa, Cam bien, Thong ke, Server, Cai dat
 *   - Duoi cung: Nut vien thuoc Tro ly AI (thay cho dong ho lap lai)
 *
 * Hien cham tron canh bao do/vang ben phai muc Tu dong hoa / Server khi co su co that.
 */
@Composable
fun SideNav(
    current: Dest,
    onSelect: (Dest) -> Unit,
    phienGiongNoi: PhienGiongNoi = PhienGiongNoi(),
    onBamTroLy: () -> Unit = {},
    canhBaoMuc: Map<Dest, Color> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(Dim.SidebarWidth)
            .fillMaxHeight()
            .background(SH.Sidebar)
            .padding(horizontal = 9.dp, vertical = 8.dp),
    ) {
        // ---- Logo ----
        Image(
            painter = painterResource(vn.smarthome.R.drawable.logo_sh),
            contentDescription = "SmartHome",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 52.dp)
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .padding(bottom = 4.dp),
        )

        // ---- Cum 1: Su dung hang ngay ----
        GroupHeader(NavGroup.DAILY.title)
        Spacer(Modifier.height(3.dp))
        Dest.daily.forEachIndexed { idx, dest ->
            NavItem(
                dest = dest,
                selected = dest == current,
                badgeColor = canhBaoMuc[dest],
                onClick = { onSelect(dest) },
            )
            if (idx < Dest.daily.lastIndex) Spacer(Modifier.height(2.dp))
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            thickness = 1.dp,
            color = SH.Border.copy(alpha = 0.65f),
        )

        // ---- Cum 2: Quan tri & He thong ----
        GroupHeader(NavGroup.SYSTEM.title)
        Spacer(Modifier.height(3.dp))
        Dest.system.forEachIndexed { idx, dest ->
            NavItem(
                dest = dest,
                selected = dest == current,
                badgeColor = canhBaoMuc[dest],
                onClick = { onSelect(dest) },
            )
            if (idx < Dest.system.lastIndex) Spacer(Modifier.height(2.dp))
        }

        Spacer(Modifier.weight(1f))

        // ---- Chan: Nut Tro ly AI co dinh ----
        NutTroLySidebar(
            phien = phienGiongNoi,
            onBam = onBamTroLy,
        )
    }
}

@Composable
private fun GroupHeader(title: String) {
    Text(
        text = title,
        style = SHType.Label.copy(
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = SH.TextOff,
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

@Composable
private fun NavItem(
    dest: Dest,
    selected: Boolean,
    badgeColor: Color?,
    onClick: () -> Unit,
) {
    val bg by animateColorAsState(if (selected) SH.Blue else Color.Transparent, label = "navBg")
    val fg = if (selected) Color.White else SH.TextDim

    Row(
        Modifier
            .fillMaxWidth()
            .height(Dim.NavItemH)
            .clip(RoundedCornerShape(11.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(dest.icon, dest.title, tint = fg, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            dest.title,
            style = SHType.Body.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (badgeColor != null) {
            Spacer(Modifier.width(4.dp))
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
                    .then(
                        if (selected) Modifier.border(1.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        else Modifier
                    ),
            )
        }
    }
}

/**
 * Tinh cham trang thai do/vang ben phai muc Tu dong hoa & Server.
 * Chi bat khi co su co that (bom bi khoa/loi/offline hoac server/MQTT loi);
 * trang thai binh thuong hoac chua cau hinh tra ve khong co cham.
 */
fun tinhCanhBaoSidebar(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    ketNoiMqtt: KetNoi,
    server: ServerState,
): Map<Dest, Color> {
    val out = mutableMapOf<Dest, Color>()

    val bomDo = (bomNuoc != null && (
        bomNuoc.dangKhoa ||
            bomNuoc.bomApDangKhoa ||
            LoiBomNuoc.phanTich(bomNuoc.fault)?.mucDo == LoiBomNuoc.MucDo.NGUY_HIEM
        )) ||
        ketNoiBom is TrangThaiKetNoiBom.BoOffline ||
        ketNoiBom is TrangThaiKetNoiBom.MatBroker ||
        ketNoiBom is TrangThaiKetNoiBom.SaiTaiKhoan ||
        ketNoiBom is TrangThaiKetNoiBom.Loi

    val bomVang = !bomDo && (
        (bomNuoc != null && bomNuoc.coLoi) ||
            ketNoiBom is TrangThaiKetNoiBom.SoCu ||
            ketNoiBom is TrangThaiKetNoiBom.ChiCoBroker
        )

    when {
        bomDo -> out[Dest.AUTOMATION] = SH.Red
        bomVang -> out[Dest.AUTOMATION] = SH.Amber
    }

    val cpuTemp = server.metric.cpuTempC
    val serverDo = ketNoiMqtt is KetNoi.Loi || (server.isRealData && cpuTemp != null && cpuTemp >= 80f)
    val serverVang = !serverDo && server.isRealData && (
        (cpuTemp != null && cpuTemp >= 75f) ||
            server.containers.any { it.state != "running" }
        )

    when {
        serverDo -> out[Dest.SERVER] = SH.Red
        serverVang -> out[Dest.SERVER] = SH.Amber
    }

    return out
}
