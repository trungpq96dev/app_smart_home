package vn.smarthome.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.smarthome.data.baomat.CauHinhBaoMat
import vn.smarthome.data.baomat.ManChoLogic
import vn.smarthome.data.baomat.SuCoKhanCap
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CauHinhBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.lunar.AmLich
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.util.Locale
import kotlin.random.Random

private val BgPureBlack = Color(0xFF000000)
private val IvoryClock = Color(0xFFE8E3D5)
private val IvoryTemp = Color(0xFFDCD7C9)
private val MutedGray = Color(0xFF8E949E)
private val LunarGold = Color(0xFF9E7238)
private val DividerGray = Color(0xFF262A30)
private val PillBg = Color(0xFF14171C)
private val PillBorder = Color(0xFF1E232B)
private val PillText = Color(0xFFA3ABB7)
private val HintGray = Color(0xFF4A5058)
private val MoonBlue = Color(0xFF7693C4)

private fun Context.timActivity(): Activity? {
    var cur: Context? = this
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

private fun thuVietFull(dow: DayOfWeek): String = when (dow) {
    DayOfWeek.MONDAY -> "Thứ Hai"
    DayOfWeek.TUESDAY -> "Thứ Ba"
    DayOfWeek.WEDNESDAY -> "Thứ Tư"
    DayOfWeek.THURSDAY -> "Thứ Năm"
    DayOfWeek.FRIDAY -> "Thứ Sáu"
    DayOfWeek.SATURDAY -> "Thứ Bảy"
    DayOfWeek.SUNDAY -> "Chủ Nhật"
}

private fun wmoShort(ma: Int, banNgay: Boolean): String = when (ma) {
    0 -> if (banNgay) "Trời quang" else "Quang mây"
    1, 2 -> "Ít mây"
    3 -> "Nhiều mây"
    45, 48 -> "Sương mù"
    in 51..57 -> "Mưa phùn"
    in 61..67 -> "Có mưa"
    in 71..86 -> "Mưa tuyết"
    in 80..82 -> "Mưa rào"
    in 95..99 -> "Có giông"
    else -> "Thay đổi"
}

private fun nhanBomNgan(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    cauHinhBom: CauHinhBomNuoc,
): String {
    if (!cauHinhBom.daCauHinh && bomNuoc == null) return "Chưa cấu hình"
    if (bomNuoc == null) {
        return if (ketNoiBom is TrangThaiKetNoiBom.DangNoi) "Đang kết nối" else "Chờ số liệu"
    }
    if (bomNuoc.dangKhoa || bomNuoc.bomApDangKhoa) return "Đang khoá"
    if (bomNuoc.bomChinhDangChay) return "Đang bơm"
    return bomNuoc.cheDoBon.nhan.replaceFirstChar { it.uppercase() }
}

/**
 * Man hinh cho ban dem (Ambient Night Clock) toi uu cho Tablet Kiosk 24/7:
 * - Nen den tuyet doi #000000, chu trang nga & mau nhan tram chong choi
 * - Pixel Shift ±10px moi 90 giay chong luu anh man hinh
 * - Tu ha do sang he thong (WindowManager.LayoutParams.screenBrightness) sau 22:00
 */
@Composable
fun ManHinhCho(
    thoiTiet: ThoiTietUi,
    dien: DienState,
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    cauHinhBom: CauHinhBomNuoc,
    cauHinhBaoMat: CauHinhBaoMat,
    onDanhThuc: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(15_000L)
        }
    }

    // Pixel shift moi 90 giay (±10px X/Y)
    var targetShift by remember { mutableStateOf(IntOffset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(90_000L)
            targetShift = IntOffset(
                x = Random.nextInt(-10, 11),
                y = Random.nextInt(-10, 11),
            )
        }
    }
    val shift by animateIntOffsetAsState(
        targetValue = targetShift,
        animationSpec = tween(durationMillis = 1500),
        label = "pixelShift",
    )

    val dangGioDem = cauHinhBaoMat.giamSangDem &&
        ManChoLogic.laKhungGioDem(now.hour, cauHinhBaoMat.gioBatDauDem, cauHinhBaoMat.gioKetThucDem)

    // Ha do sang cua so Android khi vao man cho ban dem va khoi phuc ngay khi thoat
    val ctx = LocalContext.current
    DisposableEffect(dangGioDem, cauHinhBaoMat.doSangDem) {
        val window = ctx.timActivity()?.window
        if (window != null && dangGioDem) {
            val lp = window.attributes
            lp.screenBrightness = cauHinhBaoMat.doSangDem.coerceIn(0.03f, 0.30f)
            window.attributes = lp
        }
        onDispose {
            if (window != null) {
                val lp = window.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
            }
        }
    }

    val hh = "%02d".format(now.hour)
    val mm = "%02d".format(now.minute)
    val dd = "%02d".format(now.dayOfMonth)
    val mo = "%02d".format(now.monthValue)
    val solarText = "${thuVietFull(now.dayOfWeek)}, $dd/$mo/${now.year}"

    val am = AmLich.tuDuongLich(now.dayOfMonth, now.monthValue, now.year)
    val cc = AmLich.canChi(am)
    val amText = "Âm ${"%02d".format(am.ngay)}/${"%02d".format(am.thang)}" +
        (if (am.nhuan) " Nhuận" else "") +
        " · Năm ${cc.nam}"

    val weatherData = (thoiTiet as? ThoiTietUi.Co)?.data
    val tempText = weatherData?.let { "${it.hienTai.nhietDo}°C" } ?: "--°C"
    val weatherSub = when {
        weatherData != null ->
            "${weatherData.diaDiem} · ${wmoShort(weatherData.hienTai.maWmo, weatherData.hienTai.banNgay)}"
        thoiTiet is ThoiTietUi.DangTai -> "Đang tải thời tiết…"
        else -> "Chưa có thời tiết"
    }

    val dienText = dien.ngaySanLuong?.let {
        "Điện hôm nay: ${String.format(Locale.US, "%.1f", it)} kWh"
    } ?: "Điện hôm nay: -- kWh"
    val bomText = "Bơm: ${nhanBomNgan(bomNuoc, ketNoiBom, cauHinhBom)}"

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BgPureBlack)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDanhThuc,
            )
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        // Khoi trung tam co Pixel Shift
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { shift },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$hh:$mm",
                    style = TextStyle(
                        fontSize = 104.sp,
                        fontWeight = FontWeight.Light,
                        color = IvoryClock,
                        fontFeatureSettings = "tnum",
                        letterSpacing = (-2).sp,
                    ),
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = solarText,
                    fontSize = 19.sp,
                    color = MutedGray,
                    maxLines = 1,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = amText,
                    fontSize = 17.sp,
                    color = LunarGold,
                    maxLines = 1,
                )
            }

            Box(
                Modifier
                    .width(1.dp)
                    .height(172.dp)
                    .background(DividerGray),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AmbientWeatherIcon(
                        maWmo = weatherData?.hienTai?.maWmo ?: 1,
                        banNgay = weatherData?.hienTai?.banNgay ?: false,
                    )
                    Text(
                        text = tempText,
                        style = TextStyle(
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Light,
                            color = IvoryTemp,
                            fontFeatureSettings = "tnum",
                        ),
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = weatherSub,
                    fontSize = 19.sp,
                    color = MutedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Thanh trang thai gon phia duoi
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset { IntOffset(shift.x / 2, shift.y / 2) },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AmbientStatusPill(
                    text = "Hệ thống an toàn",
                    dotColor = SH.Green,
                )
                AmbientStatusPill(text = dienText)
                AmbientStatusPill(text = bomText)
            }
            Text(
                text = "Chạm màn hình để đánh thức",
                fontSize = 11.sp,
                color = HintGray,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AmbientStatusPill(
    text: String,
    dotColor: Color? = null,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(PillBg)
            .border(1.dp, PillBorder, CircleShape)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dotColor != null) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(
            text = text,
            fontSize = 13.sp,
            color = PillText,
            maxLines = 1,
        )
    }
}

@Composable
private fun AmbientWeatherIcon(maWmo: Int, banNgay: Boolean) {
    if (!banNgay && maWmo in 0..2) {
        Canvas(Modifier.size(64.dp, 52.dp)) {
            val w = size.width
            val h = size.height
            // Trang luoi liem xanh tram
            drawCircle(
                color = MoonBlue,
                radius = h * 0.36f,
                center = Offset(w * 0.62f, h * 0.42f),
            )
            drawCircle(
                color = BgPureBlack,
                radius = h * 0.31f,
                center = Offset(w * 0.72f, h * 0.34f),
            )
            // Dam may nho gon
            val cloud = Path().apply {
                moveTo(w * 0.22f, h * 0.84f)
                lineTo(w * 0.62f, h * 0.84f)
                cubicTo(w * 0.72f, h * 0.84f, w * 0.74f, h * 0.66f, w * 0.61f, h * 0.64f)
                cubicTo(w * 0.58f, h * 0.50f, w * 0.38f, h * 0.50f, w * 0.35f, h * 0.64f)
                cubicTo(w * 0.20f, h * 0.64f, w * 0.16f, h * 0.84f, w * 0.22f, h * 0.84f)
                close()
            }
            drawPath(cloud, color = Color(0xFF536B96))
        }
        return
    }
    val (icon, tintCol) = when {
        maWmo == 0 -> Icons.Filled.WbSunny to Color(0xFFB89B5E)
        maWmo in 1..2 -> Icons.Filled.WbCloudy to Color(0xFFB89B5E)
        maWmo == 3 || maWmo == 45 || maWmo == 48 -> Icons.Filled.Cloud to MoonBlue
        maWmo in 51..57 -> Icons.Filled.Grain to MoonBlue
        maWmo in 61..67 || maWmo in 80..82 -> Icons.Filled.Umbrella to MoonBlue
        maWmo in 95..99 -> Icons.Filled.Thunderstorm to Color(0xFF9C84C9)
        else -> Icons.Filled.Cloud to MoonBlue
    }
    Icon(icon, contentDescription = null, tint = tintCol, modifier = Modifier.size(54.dp))
}

@Composable
fun BangCanhBaoKhanCap(
    suCo: SuCoKhanCap,
    onXem: () -> Unit,
    onAn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SH.Red.tint(0.18f),
        border = BorderStroke(1.5.dp, SH.Red),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = SH.Red, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f)) {
                Text(suCo.tieuDe, style = SHType.BodyMed, color = Color(0xFFFEE2E2), maxLines = 1)
                Text(
                    suCo.chiTiet,
                    style = SHType.Caption,
                    color = Color(0xFFFCA5A5),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onXem,
                colors = ButtonDefaults.buttonColors(containerColor = SH.Red),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Mở kiểm tra", style = SHType.Button, color = Color.White, maxLines = 1)
            }
            IconButton(onClick = onAn) {
                Icon(Icons.Filled.Close, contentDescription = "Ẩn tạm cảnh báo", tint = Color(0xFFFCA5A5))
            }
        }
    }
}

