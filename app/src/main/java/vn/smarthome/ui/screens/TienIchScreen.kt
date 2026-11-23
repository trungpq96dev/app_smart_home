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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import vn.smarthome.data.lunar.AmLich
import vn.smarthome.data.lunar.NgayAm
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.RoundIconButton
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.StatTile
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.ui.theme.tint
import java.time.LocalDate

/**
 * Man Tien ich - Lich van nien (am lich Viet Nam).
 *
 * KHONG co du lieu mau o day: moi con so deu tinh ra tu [AmLich] va dong ho
 * cua may, nen khong deo chip "DU LIEU MAU" va cung khong can mang.
 *
 * Ban thiet ke goc (trang HTML) con co cot menu con: Thoi tiet / Dien luc EVN /
 * Khong khi AQI. CHUA lam o day vi hai muc dau da co that o Trang chu va man
 * Thong ke, con AQI thi chua co nguon. Bay ba the bam vao khong ra gi chi de
 * chiem 150dp ngang cua man 1006dp - them vao khi nao co noi dung that.
 */
@Composable
fun TienIchScreen(
    onBack: () -> Unit,
    homNay: LocalDate = LocalDate.now(),
) {
    var namXem by rememberSaveable { mutableIntStateOf(homNay.year) }
    var thangXem by rememberSaveable { mutableIntStateOf(homNay.monthValue) }
    var chonEpoch by rememberSaveable { mutableLongStateOf(homNay.toEpochDay()) }
    val ngayChon = LocalDate.ofEpochDay(chonEpoch)

    val amHomNay = remember(homNay) { amCua(homNay) }
    val ccHomNay = remember(amHomNay) { AmLich.canChi(amHomNay) }

    Column(Modifier.fillMaxSize()) {

        TopBar(title = "Tiện ích · Lịch vạn niên", onBack = null) {
            StatusPill("Âm lịch Việt Nam (GMT+7)", SH.Green)
        }

        // ---- Hang 4 o so lieu cua HOM NAY (khong doi theo ngay dang chon) ----
        // 86dp chu khong phai 74: StatTile xep doc icon 28 + so 24 + nhan 13 +
        // dem 12, thap hon la nhan duoi bi cat mat mot nua.
        Row(
            Modifier.fillMaxWidth().height(86.dp),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            StatTile(
                icon = Icons.Filled.WbSunny,
                value = "%02d/%02d/%d".format(homNay.dayOfMonth, homNay.monthValue, homNay.year),
                unit = "",
                label = thuTrongTuan(homNay),
                color = SH.Blue,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                icon = Icons.Filled.NightsStay,
                value = "${amHomNay.ngay}/${amHomNay.thang}",
                unit = if (amHomNay.nhuan) " nhuận" else "",
                label = "Âm lịch · năm ${ccHomNay.nam}",
                color = SH.Amber,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatTile(
                icon = Icons.Filled.Autorenew,
                value = ccHomNay.ngay,
                unit = "",
                label = "Ngày Can Chi",
                color = SH.Green,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            val moc = remember(homNay) { mocAmGanNhat(homNay, amHomNay) }
            StatTile(
                icon = Icons.Filled.Celebration,
                value = moc.first,
                unit = "",
                label = moc.second,
                color = SH.Purple,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        Spacer(Modifier.height(Dim.CardGap))

        // ---- Lich thang (trai) + chi tiet ngay dang chon (phai) ----
        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            LichThang(
                nam = namXem,
                thang = thangXem,
                homNay = homNay,
                ngayChon = ngayChon,
                onChon = { d ->
                    chonEpoch = d.toEpochDay()
                    if (d.monthValue != thangXem || d.year != namXem) {
                        thangXem = d.monthValue
                        namXem = d.year
                    }
                },
                onThangTruoc = {
                    val m = LocalDate.of(namXem, thangXem, 1).minusMonths(1)
                    namXem = m.year; thangXem = m.monthValue
                },
                onThangSau = {
                    val m = LocalDate.of(namXem, thangXem, 1).plusMonths(1)
                    namXem = m.year; thangXem = m.monthValue
                },
                onHomNay = {
                    namXem = homNay.year
                    thangXem = homNay.monthValue
                    chonEpoch = homNay.toEpochDay()
                },
                modifier = Modifier.weight(1.55f).fillMaxHeight(),
            )

            ChiTietNgay(
                ngay = ngayChon,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

// =====================================================================
//  LICH THANG
// =====================================================================

@Composable
private fun LichThang(
    nam: Int,
    thang: Int,
    homNay: LocalDate,
    ngayChon: LocalDate,
    onChon: (LocalDate) -> Unit,
    onThangTruoc: () -> Unit,
    onThangSau: () -> Unit,
    onHomNay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier = modifier) {
        // ---- Thanh dieu khien thang ----
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Tháng $thang, $nam",
                style = SHType.CardTitle,
                color = SH.Text,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            RoundIconButton(
                Icons.Filled.ChevronLeft, onThangTruoc,
                tint = SH.Text, modifier = Modifier.size(32.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Hôm nay",
                style = SHType.Label,
                color = SH.Text,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(SH.Card2)
                    .clickable(onClick = onHomNay)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
            )
            Spacer(Modifier.width(6.dp))
            RoundIconButton(
                Icons.Filled.ChevronRight, onThangSau,
                tint = SH.Text, modifier = Modifier.size(32.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // ---- Hang thu: T2..CN, Chu Nhat mau do ----
        Row(Modifier.fillMaxWidth()) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEachIndexed { i, t ->
                Text(
                    t,
                    style = SHType.Label,
                    color = if (i == 6) SH.Red else SH.TextDim,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(5.dp))

        // ---- Luoi 6 hang x 7 cot ----
        // Luon ve du 42 o de chieu cao thang nay bang thang khac: thang nao it
        // hang hon ma co dan ra la lich "nhay" moi lan bam Truoc/Sau.
        val dauThang = LocalDate.of(nam, thang, 1)
        val batDau = dauThang.minusDays((dauThang.dayOfWeek.value - 1).toLong())

        Column(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(6) { hang ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(7) { cot ->
                        val d = batDau.plusDays((hang * 7 + cot).toLong())
                        ONgayLich(
                            ngay = d,
                            trongThang = d.monthValue == thang && d.year == nam,
                            laHomNay = d == homNay,
                            daChon = d == ngayChon,
                            onClick = { onChon(d) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ONgayLich(
    ngay: LocalDate,
    trongThang: Boolean,
    laHomNay: Boolean,
    daChon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val am = remember(ngay) { amCua(ngay) }
    val chuNhat = ngay.dayOfWeek.value == 7

    val nen = when {
        daChon -> SH.Blue
        laHomNay -> SH.Blue.tint(0.18f)
        else -> SH.Card2
    }
    val vien = when {
        daChon -> SH.Blue
        laHomNay -> SH.BorderHi
        else -> Color.Transparent
    }
    // O cua thang khac bi lam nhat de mat bat ngay dau/cuoi thang ngay lap tuc.
    val mo = if (trongThang) 1f else 0.38f
    val mauDuong = when {
        daChon -> Color.White
        chuNhat -> SH.Red
        else -> SH.Text
    }.copy(alpha = mo)
    val mauAm = when {
        daChon -> Color.White
        AmLich.laMocAm(am) -> SH.Amber
        else -> SH.TextDim
    }.copy(alpha = mo)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(nen)
            .border(Dim.BorderWidth, vien, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            ngay.dayOfMonth.toString(),
            style = SHType.CardTitle,
            color = mauDuong,
            maxLines = 1,
        )
        Text(
            AmLich.nhanO(am),
            style = SHType.Caption,
            color = mauAm,
            maxLines = 1,
        )
    }
}

// =====================================================================
//  CHI TIET NGAY DANG CHON
// =====================================================================

@Composable
private fun ChiTietNgay(ngay: LocalDate, modifier: Modifier = Modifier) {
    val am = remember(ngay) { amCua(ngay) }
    val cc = remember(am) { AmLich.canChi(am) }
    val suKien = remember(ngay, am) { AmLich.suKien(ngay.dayOfMonth, ngay.monthValue, am) }

    SmartCard(modifier = modifier) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // ---- Dau the: thu + ngay duong lich day du ----
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(thuTrongTuan(ngay), style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                    Text(
                        "${ngay.dayOfMonth} tháng ${ngay.monthValue}, ${ngay.year}",
                        style = SHType.CardTitle, color = SH.Text, maxLines = 1,
                    )
                }
                Text(
                    ngay.dayOfMonth.toString(),
                    style = SHType.ValueBig,
                    color = SH.Blue,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---- O am lich noi bat ----
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Amber.tint(0.10f))
                    .border(Dim.BorderWidth, SH.Amber.tint(0.30f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            ) {
                Text("NGÀY ÂM LỊCH", style = SHType.Caption, color = SH.Amber, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Text(
                    "Ngày ${am.ngay} tháng ${am.thang}${if (am.nhuan) " nhuận" else ""}",
                    style = SHType.CardTitle, color = SH.Text, maxLines = 1,
                )
                Text("Năm ${cc.nam}", style = SHType.Body, color = SH.TextDim, maxLines = 1)
            }

            Spacer(Modifier.height(10.dp))

            DongTin("Năm Can Chi", cc.nam)
            Spacer(Modifier.height(5.dp))
            DongTin("Tháng Can Chi", cc.thang)
            Spacer(Modifier.height(5.dp))
            DongTin("Ngày Can Chi", cc.ngay)
            Spacer(Modifier.height(5.dp))
            // Khong co le thi noi thang la khong co, khong bia ra mot cau chuc.
            DongTin("Sự kiện", suKien ?: "Không có", mauGiaTri = if (suKien != null) SH.Amber else SH.TextDim)

            Spacer(Modifier.height(11.dp))

            Text("Giờ hoàng đạo", style = SHType.Label, color = SH.TextDim, maxLines = 1)
            Spacer(Modifier.height(6.dp))

            // Chia 2 hang 3 cot thay vi FlowRow: so gio luon la 6, chia tay thi
            // khong phu thuoc API thu nghiem cua Compose.
            AmLich.gioHoangDao(cc.chiNgay).chunked(3).forEach { hang ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    hang.forEach { g ->
                        Text(
                            g,
                            style = SHType.Caption,
                            color = SH.Green,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SH.Green.tint(0.12f))
                                .border(Dim.BorderWidth, SH.Green.tint(0.30f), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DongTin(nhan: String, giaTri: String, mauGiaTri: Color = SH.Text) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Card2)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(nhan, style = SHType.Body, color = SH.TextDim, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(
            giaTri,
            style = SHType.BodyMed,
            color = mauGiaTri,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// =====================================================================
//  HAM PHU
// =====================================================================

private fun amCua(d: LocalDate): NgayAm =
    AmLich.tuDuongLich(d.dayOfMonth, d.monthValue, d.year)

private fun thuTrongTuan(d: LocalDate): String = when (d.dayOfWeek.value) {
    1 -> "Thứ Hai"
    2 -> "Thứ Ba"
    3 -> "Thứ Tư"
    4 -> "Thứ Năm"
    5 -> "Thứ Sáu"
    6 -> "Thứ Bảy"
    else -> "Chủ Nhật"
}

/**
 * Moc am lich gan nhat: Mung 1 hoac Ram.
 *
 * Dem that tung ngay thay vi lay 15 tru di ngay hien tai - thang am lich dai
 * 29 hoac 30 ngay tuy thang, tru nham la lech mot ngay o cuoi thang thieu.
 */
private fun mocAmGanNhat(homNay: LocalDate, am: NgayAm): Pair<String, String> {
    if (am.ngay == 1) return "Hôm nay" to "Mùng 1 tháng ${am.thang}"
    if (am.ngay == 15) return "Hôm nay" to "Rằm tháng ${am.thang}"

    val toiRam = soNgayToi(homNay, 15)
    val toiMung1 = soNgayToi(homNay, 1)
    return when {
        toiRam in 1..toiMung1 -> "$toiRam ngày" to "tới Rằm tháng ${am.thang}"
        toiMung1 >= 1 -> "$toiMung1 ngày" to "tới Mùng 1 tháng sau"
        else -> "--" to "Mốc âm lịch gần nhất"
    }
}

/** So ngay tu [tu] toi ngay am lich [ngayAm] gan nhat phia truoc. */
private fun soNgayToi(tu: LocalDate, ngayAm: Int): Int {
    var d = tu
    for (i in 1..31) {
        d = d.plusDays(1)
        if (amCua(d).ngay == ngayAm) return i
    }
    return Int.MAX_VALUE
}

@Preview(widthDp = Dim.DesignW, heightDp = Dim.DesignH, showBackground = true)
@Composable
private fun TienIchPreview() {
    SmartHomeTheme {
        Box(Modifier.background(SH.Bg).padding(Dim.MainPadding)) {
            TienIchScreen(onBack = {}, homNay = LocalDate.of(2026, 11, 20))
        }
    }
}
