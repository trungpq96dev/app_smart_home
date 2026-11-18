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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.StatKind
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.NgayKwh
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Man xem chi tiet dien nang theo tung ngay, loc duoc theo khoang ngay.
 *
 * Giong danh sach trong app EVN CSKH: moi ngay mot the, moi nhat len tren,
 * co tong o dau. Khac mot diem QUAN TRONG, phai noi ro voi nguoi dung:
 *
 *   App EVN goi thang API nen chon ngay nao cung co. App nay CHI doc tu MQTT,
 *   ma dien/lichsu chi giu mot so ngay gan nhat (script lay tu dau ky hoac
 *   ~60 ngay). Nen chon khoang ngoai pham vi do thi khong co so - va man nay
 *   noi thang "ngoai pham vi da tai ve" chu khong bia.
 *
 * Khong goi EVN truc tiep: app khong co tai khoan EVN va khong duoc co (ma
 * nguon nam tren GitHub). Muon xem xa hon thi tang so ngay script lay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChiTietDienScreen(
    dien: DienState,
    onBack: () -> Unit,
) {
    // lichSu da sap xep tang dan (cu -> moi) tu lop MQTT. Doi sang LocalDate
    // mot lan de loc va hien.
    val tatCa = remember(dien.lichSu) {
        dien.lichSu.mapNotNull { nk ->
            runCatching { LocalDate.parse(nk.ngay) to nk.kwh }.getOrNull()
        }
    }
    val ngayDau = tatCa.firstOrNull()?.first
    val ngayCuoi = tatCa.lastOrNull()?.first

    // Mac dinh: ca pham vi du lieu dang co.
    var tuNgay by remember(ngayDau) { mutableStateOf(ngayDau) }
    var denNgay by remember(ngayCuoi) { mutableStateOf(ngayCuoi) }
    var moChon by remember { mutableStateOf<ChonNgay?>(null) }

    // Loc va dao nguoc de moi nhat len tren, giong app EVN.
    val loc = remember(tatCa, tuNgay, denNgay) {
        tatCa.filter { (d, _) ->
            (tuNgay == null || !d.isBefore(tuNgay)) &&
                (denNgay == null || !d.isAfter(denNgay))
        }.reversed()
    }
    val tong = loc.sumOf { it.second }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Chi tiết điện năng", onBack = onBack)

        if (tatCa.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Chưa có dữ liệu ngày nào từ broker.",
                    style = SHType.Body, color = SH.TextDim,
                )
            }
            return@Column
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------- Trai: bo loc + tong ----------
            Column(Modifier.width(300.dp).fillMaxHeight()) {
                SmartCard(Modifier.fillMaxWidth(), padding = 14.dp) {
                    Text("Khoảng ngày", style = SHType.CardTitle, color = SH.Text)
                    Spacer(Modifier.height(10.dp))

                    ONgay(
                        nhan = "Từ ngày",
                        ngay = tuNgay,
                        onClick = { moChon = ChonNgay.TU },
                    )
                    Spacer(Modifier.height(8.dp))
                    ONgay(
                        nhan = "Đến ngày",
                        ngay = denNgay,
                        onClick = { moChon = ChonNgay.DEN },
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ChipNhanh("7 ngày") { denNgay = ngayCuoi; tuNgay = lui(ngayCuoi, 6, ngayDau) }
                        ChipNhanh("14 ngày") { denNgay = ngayCuoi; tuNgay = lui(ngayCuoi, 13, ngayDau) }
                        ChipNhanh("Cả kỳ") { tuNgay = ngayDau; denNgay = ngayCuoi }
                    }
                }

                Spacer(Modifier.height(Dim.CardGap))

                SmartCard(Modifier.fillMaxWidth().weight(1f), padding = 14.dp) {
                    Text("Tổng trong khoảng", style = SHType.Caption, color = SH.TextDim)
                    Spacer(Modifier.height(8.dp))
                    IconBadge(Icons.Filled.Bolt, StatKind.POWER.color, size = 34.dp, iconSize = 19.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(soKwh(tong), style = SHType.ValueBig, color = SH.Text, maxLines = 1)
                        Text(
                            "kWh", style = SHType.CardTitle, color = SH.TextDim,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                        )
                    }
                    Text(
                        loc.size.toString() + " ngày",
                        style = SHType.Caption, color = SH.TextOff,
                    )
                    Spacer(Modifier.weight(1f))
                    // Noi ro gioi han pham vi du lieu - khong de nguoi dung
                    // tuong app chon duoc moi ngay nhu app EVN.
                    if (ngayDau != null && ngayCuoi != null) {
                        Text(
                            "Dữ liệu đã tải: " + DINH_DANG.format(ngayDau) +
                                " → " + DINH_DANG.format(ngayCuoi),
                            style = SHType.Caption, color = SH.TextOff,
                        )
                    }
                }
            }

            // ---------- Phai: danh sach tung ngay ----------
            SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 8.dp) {
                if (loc.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Không có ngày nào trong khoảng đã chọn.",
                            style = SHType.Body, color = SH.TextDim,
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(loc, key = { it.first.toString() }) { (d, kwh) ->
                            DongNgay(d, kwh)
                        }
                    }
                }
            }
        }
    }

    // ---------- Hop chon ngay ----------
    moChon?.let { cai ->
        val chon = if (cai == ChonNgay.TU) tuNgay else denNgay
        val trong = (chon ?: ngayCuoi ?: LocalDate.now())
        val state = rememberDatePickerState(
            initialSelectedDateMillis = trong.toEpochDay() * 86_400_000L,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val d = LocalDate.ofEpochDay(Math.floorDiv(utcTimeMillis, 86_400_000L))
                    return (ngayDau == null || !d.isBefore(ngayDau)) &&
                        (ngayCuoi == null || !d.isAfter(ngayCuoi))
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { moChon = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = LocalDate.ofEpochDay(Math.floorDiv(ms, 86_400_000L))
                        if (cai == ChonNgay.TU) {
                            tuNgay = d
                            if (denNgay != null && d.isAfter(denNgay)) denNgay = d
                        } else {
                            denNgay = d
                            if (tuNgay != null && d.isBefore(tuNgay)) tuNgay = d
                        }
                    }
                    moChon = null
                }) { Text("Chọn", color = SH.Blue) }
            },
            dismissButton = {
                TextButton(onClick = { moChon = null }) { Text("Huỷ", color = SH.TextDim) }
            },
            colors = DatePickerDefaults.colors(containerColor = SH.Card),
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
}

private enum class ChonNgay { TU, DEN }

/** Lui tu `moc` ve `so` ngay, khong vuot qua `san`. */
private fun lui(moc: LocalDate?, so: Long, san: LocalDate?): LocalDate? {
    if (moc == null) return null
    val t = moc.minusDays(so)
    return if (san != null && t.isBefore(san)) san else t
}

/** O hien ngay da chon, bam mo lich. */
@Composable
private fun ONgay(nhan: String, ngay: LocalDate?, onClick: () -> Unit) {
    Column {
        Text(nhan, style = SHType.Caption, color = SH.TextDim)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SH.Card2)
                .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Event, null, tint = SH.Blue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                ngay?.let { DINH_DANG.format(it) } ?: "--",
                style = SHType.Body,
                color = if (ngay != null) SH.Text else SH.TextOff,
            )
        }
    }
}

@Composable
private fun ChipNhanh(chu: String, onClick: () -> Unit) {
    Text(
        chu,
        style = SHType.Caption,
        color = SH.Blue,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Blue.tint(0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

/** Mot dong trong danh sach - ngay + san luong, giong the cua app EVN. */
@Composable
private fun DongNgay(d: LocalDate, kwh: Double) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(SH.Card2)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(Icons.Filled.Bolt, StatKind.POWER.color, size = 34.dp, iconSize = 18.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Ngày " + DINH_DANG.format(d), style = SHType.BodyMed, color = SH.Text)
            Text(THU[d.dayOfWeek.value - 1], style = SHType.Caption, color = SH.TextOff)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(soKwh(kwh), style = SHType.CardTitle, color = StatKind.POWER.color, fontWeight = FontWeight.Bold)
            Text(
                "kWh", style = SHType.Caption, color = SH.TextDim,
                modifier = Modifier.padding(start = 4.dp, bottom = 1.dp),
            )
        }
    }
}

private val DINH_DANG: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private val THU = listOf(
    "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật",
)

/** So kWh kieu Viet Nam: dau phay thap phan, bo so 0 thua. "10,46" / "8,1". */
private fun soKwh(v: Double): String {
    val s = (Math.round(v * 100.0) / 100.0).toString()
        .trimEnd('0').trimEnd('.')
    return s.replace('.', ',')
}

@Preview(widthDp = 994, heightDp = 589, showBackground = true)
@Composable
private fun ChiTietPreview() {
    val mau = DienState(
        lichSu = buildList {
            val d0 = LocalDate.of(2026, 11, 1)
            val kwh = listOf(6.05, 6.96, 8.89, 8.10, 8.96, 10.46, 7.2, 9.1, 6.4, 8.8)
            kwh.forEachIndexed { i, k -> add(NgayKwh(d0.plusDays(i.toLong()).toString(), k)) }
        },
    )
    SmartHomeTheme {
        Box(Modifier.fillMaxSize().padding(12.dp)) {
            ChiTietDienScreen(mau, {})
        }
    }
}
