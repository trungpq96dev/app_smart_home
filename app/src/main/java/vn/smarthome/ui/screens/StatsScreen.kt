package vn.smarthome.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.DemoData
import vn.smarthome.data.StatKind
import vn.smarthome.data.StatRange
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.DoTuoi
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.ui.components.ArcGauge
import vn.smarthome.ui.components.BarChart
import vn.smarthome.ui.components.DemoBadge
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import vn.smarthome.vm.HomeState
import java.time.LocalDate

/**
 * Man thong ke va bao cao.
 *
 * Tab DIEN NANG lay so THAT tu MQTT (xem data/dien/). Bon tab con lai van la
 * du lieu mau vi chua co nguon - va chung deo chip "DU LIEU MAU", tab Dien
 * nang thi khong.
 *
 * Quy uoc du an: KHONG BIA SO. Mat ket noi hoac chua co retained thi tab Dien
 * nang hien "--" va noi ro vi sao, tuyet doi khong lay so mau bu vao.
 */
@Composable
fun StatsScreen(
    state: HomeState,
    dien: DienState,
    ketNoi: KetNoi,
    onBack: () -> Unit,
    onKind: (StatKind) -> Unit,
    onRange: (StatRange) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val laDien = state.statKind == StatKind.POWER

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Thống kê & Báo cáo", onBack = onBack) {
            if (laDien) TrangThaiKetNoi(ketNoi) else if (DemoData.IS_DEMO) DemoBadge()
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SH.Card2)
                        .clickable { menuOpen = true }
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.statRange.label, style = SHType.Button, color = SH.Text, maxLines = 1)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.ExpandMore, null,
                        tint = SH.TextDim, modifier = Modifier.size(17.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    StatRange.entries.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r.label, style = SHType.Body, color = SH.Text) },
                            onClick = { onRange(r); menuOpen = false },
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            Column(
                Modifier.width(136.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                StatKind.entries.forEach { k ->
                    KindTab(
                        kind = k,
                        icon = kindIcon(k),
                        selected = k == state.statKind,
                        onClick = { onKind(k) },
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (laDien) {
                    TabDienNang(dien, ketNoi, state.statRange)
                } else {
                    TabDuLieuMau(state)
                }
            }
        }
    }
}

// =====================================================================
//  TAB DIEN NANG - so that tu MQTT
// =====================================================================

@Composable
private fun ColumnScope.TabDienNang(dien: DienState, ketNoi: KetNoi, range: StatRange) {
    val mau = StatKind.POWER.color
    val bars = dien.lichSu.takeLast(range.days)
    val tong = dien.tongKwh(range.days)
    val chenh = dien.chenhPhanTram(range.days)

    SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 12.dp) {
        Text("Điện năng tiêu thụ", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                tong?.let { soLe1(it) } ?: "--",
                style = SHType.ValueBig,
                color = if (tong != null) SH.Text else SH.TextOff,
                maxLines = 1,
            )
            Text(
                "kWh",
                style = SHType.CardTitle,
                color = SH.TextDim,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            if (chenh != null) {
                Spacer(Modifier.width(12.dp))
                DeltaChip(chenh, Modifier.padding(bottom = 5.dp))
            }
        }

        // Chi so cong to va cong no - dung topic da co, khong them the moi.
        val phu = buildList {
            dien.congToChiSo?.let { add("Chỉ số công tơ: " + soLe1(it)) }
            dien.congNoSoTien?.let { add("Công nợ: " + tienVnd(it)) }
        }
        if (phu.isNotEmpty()) {
            Text(
                phu.joinToString("  ·  "),
                style = SHType.Caption, color = SH.TextDim, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (bars.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                ChuaCoSo(ketNoi)
            }
        } else {
            BarChart(
                values = bars.map { it.kwh.toFloat() },
                labels = nhanNgay(bars.map { it.ngay }),
                color = mau,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }

    Spacer(Modifier.height(Dim.CardGap))

    Row(
        Modifier.fillMaxWidth().height(116.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // KHONG con tile "Cong suat hien tai": EVN chot theo ngay, khong co so
        // tuc thoi. Thay bang san luong cua ngay EVN da chot gan nhat.
        TheSo(
            nhan = "Sản lượng ngày gần nhất",
            icon = Icons.Filled.Today,
            mau = mau,
            giaTri = dien.ngaySanLuong?.let { soLe1(it) + " kWh" } ?: "--",
            phu = dien.ngay ?: "chưa có ngày",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        // Tien phai la LUY KE CA KY (dien/thang/tien), khong phai theo khoang
        // dang chon. Gia dien bac thang luy tien nen cong tien cua 7 ngay roi
        // suy ra ca thang la sai rat xa.
        TheSo(
            nhan = "Chi phí tháng này đến giờ",
            icon = Icons.Filled.Payments,
            mau = SH.Amber,
            giaTri = dien.thangTien?.let { tienVnd(it) } ?: "--",
            phu = dien.thangSanLuong?.let { soLe1(it) + " kWh luỹ kế" } ?: "chưa có luỹ kế",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )

        TheTrangThai(dien, ketNoi, Modifier.weight(1f).fillMaxHeight())
    }
}

/** The so chung cho tab Dien nang. */
@Composable
private fun TheSo(
    nhan: String,
    icon: ImageVector,
    mau: Color,
    giaTri: String,
    phu: String,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier, padding = 10.dp) {
        Text(nhan, style = SHType.Caption, color = SH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
        IconBadge(icon, mau, size = 30.dp, iconSize = 17.dp)
        Spacer(Modifier.height(5.dp))
        Text(
            giaTri,
            style = SHType.CardTitle,
            color = if (giaTri == "--") SH.TextOff else SH.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(phu, style = SHType.Caption, color = SH.TextOff, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.weight(1f))
    }
}

/**
 * Trang thai: gop tin hieu cua duong ong (dien/trangthai, dien/loi) voi TUOI
 * cua dien/capnhat.
 *
 * Tai lieu ban giao muc 5: duong ong KHONG co Last Will, nen chi co cach nhin
 * tuoi dien/capnhat de biet so con moi hay khong. Script chay 4 cu/ngay.
 */
@Composable
private fun TheTrangThai(dien: DienState, ketNoi: KetNoi, modifier: Modifier = Modifier) {
    val tuoi = dien.doTuoi()
    val (mau, chu, phu) = when {
        ketNoi is KetNoi.Loi -> Triple(SH.Red, "Mất kết nối", ketNoi.moTa)
        ketNoi is KetNoi.ChuaCauHinh -> Triple(SH.TextOff, "Chưa khai báo", "Vào Cài đặt khai báo broker")
        ketNoi is KetNoi.DangNoi -> Triple(SH.Amber, "Đang nối", "Chờ broker trả số")
        !dien.loi.isNullOrBlank() -> Triple(SH.Red, "Đường ống lỗi", dien.loi)
        dien.trangThai == "loi" -> Triple(SH.Red, "Cữ gần nhất lỗi", "Xem log trên serverubuntu")
        tuoi == DoTuoi.CHUA_CO -> Triple(SH.TextOff, "Chưa có số", "Broker chưa có dữ liệu retained")
        tuoi == DoTuoi.QUA_CU -> Triple(SH.Red, "Số quá cũ", "Hơn 24 giờ. Nghi serverubuntu đang ngủ")
        tuoi == DoTuoi.HOI_CU -> Triple(SH.Amber, "Số hơi cũ", "Đã lỡ ít nhất một cữ")
        else -> Triple(SH.Green, "Bình thường", dien.capNhat ?: "")
    }

    SmartCard(modifier, padding = 10.dp) {
        Text("Trạng thái", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(
                if (mau == SH.Green) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                mau, size = 30.dp, iconSize = 17.dp,
            )
            Spacer(Modifier.height(4.dp))
            Text(chu, style = SHType.BodyMed, color = mau, maxLines = 1)
            Text(
                phu, style = SHType.Caption, color = SH.TextOff,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

/** Khong co so thi noi ro vi sao, khong ve bieu do rong cho co. */
@Composable
private fun ChuaCoSo(ketNoi: KetNoi) {
    val chu = when (ketNoi) {
        is KetNoi.ChuaCauHinh -> "Chưa khai báo broker EMQX"
        is KetNoi.DangNoi -> "Đang nối broker..."
        is KetNoi.Loi -> ketNoi.moTa
        is KetNoi.DaNoi -> "Đã nối broker nhưng chưa có dữ liệu trong dien/lichsu"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconBadge(Icons.Filled.Schedule, SH.TextOff, size = 44.dp, iconSize = 23.dp)
        Spacer(Modifier.height(8.dp))
        Text("Chưa có số liệu điện", style = SHType.CardTitle, color = SH.TextDim, maxLines = 1)
        Text(
            chu, style = SHType.Caption, color = SH.TextOff,
            maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp),
        )
    }
}

// =====================================================================
//  BON TAB CON LAI - van la du lieu mau
// =====================================================================

@Composable
private fun ColumnScope.TabDuLieuMau(state: HomeState) {
    val s = DemoData.series(state.statKind, state.statRange)

    SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 12.dp) {
        Text(s.kind.title, style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(s.total, style = SHType.ValueBig, color = SH.Text, maxLines = 1)
            Text(
                s.unit, style = SHType.CardTitle, color = SH.TextDim, maxLines = 1,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            )
            Spacer(Modifier.width(12.dp))
            DeltaChip(s.deltaPercent, Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(8.dp))
        BarChart(
            values = s.bars, labels = s.labels, color = s.kind.color,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }

    Spacer(Modifier.height(Dim.CardGap))

    Row(
        Modifier.fillMaxWidth().height(116.dp),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
            Text(s.gaugeLabel, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            ArcGauge(
                fraction = s.gaugeFraction, value = s.gaugeValue, color = s.kind.color,
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
        TheSo(
            nhan = s.costLabel, icon = Icons.Filled.Payments, mau = SH.Amber,
            giaTri = s.costValue, phu = "",
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 10.dp) {
            Text("Trạng thái", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(
                    Icons.Filled.CheckCircle,
                    if (s.statusOk) SH.Green else SH.Red, size = 34.dp, iconSize = 19.dp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    s.statusLabel, style = SHType.BodyMed,
                    color = if (s.statusOk) SH.Green else SH.Red, maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

// =====================================================================
//  Phu tro
// =====================================================================

@Composable
private fun KindTab(
    kind: StatKind,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SH.Blue else SH.Card)
            .border(
                Dim.BorderWidth,
                if (selected) SH.Blue else SH.Border,
                RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon, null,
            tint = if (selected) Color.White else kind.color,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            kind.label,
            style = SHType.BodyMed,
            color = if (selected) Color.White else SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DeltaChip(percent: Int, modifier: Modifier = Modifier) {
    val up = percent >= 0
    val color = if (up) SH.Red else SH.Green
    Row(
        modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.tint(0.15f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            null, tint = color, modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            (if (up) percent else -percent).toString() + "%",
            style = SHType.Label, color = color, maxLines = 1,
        )
    }
}

private fun kindIcon(k: StatKind): ImageVector = when (k) {
    StatKind.POWER -> Icons.Filled.Bolt
    StatKind.TEMP -> Icons.Filled.Thermostat
    StatKind.HUMID -> Icons.Filled.WaterDrop
    StatKind.WATER -> Icons.Filled.Water
    StatKind.GAS -> Icons.Filled.LocalFireDepartment
}

private fun soLe1(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
}

/** 480650.0 -> "480.650 đ" */
private fun tienVnd(v: Double): String {
    val n = Math.round(v)
    val s = n.toString().reversed().chunked(3).joinToString(".").reversed()
    return s + " đ"
}

/**
 * Nhan truc ngang.
 *
 * Tu 7 ngay tro xuong thi hien thu trong tuan tinh TU NGAY THAT (khong gia dinh
 * phan tu cuoi la Chu nhat). Dai hon thi hien dd/MM thua ra cho do chen chuc.
 */
private fun nhanNgay(ngay: List<String>): List<String> {
    if (ngay.size <= 7) {
        return ngay.map { s ->
            val d = runCatching { LocalDate.parse(s) }.getOrNull() ?: return@map ""
            when (d.dayOfWeek.value) {
                7 -> "CN"
                else -> "T" + (d.dayOfWeek.value + 1)
            }
        }
    }
    val buoc = ngay.size / 7
    return ngay.mapIndexed { i, s ->
        if (buoc > 0 && i % buoc == 0) {
            val d = runCatching { LocalDate.parse(s) }.getOrNull()
            d?.let { "%02d/%02d".format(it.dayOfMonth, it.monthValue) } ?: ""
        } else ""
    }
}

@Preview(widthDp = 830, heightDp = 601, showBackground = true)
@Composable
private fun StatsTrongPreview() {
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            StatsScreen(HomeState(), DienState(), KetNoi.ChuaCauHinh, {}, {}, {})
        }
    }
}
