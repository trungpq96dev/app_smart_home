package vn.smarthome.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Dung font he thong (Roboto tren Android) de du an build duoc ngay khong can mang.
 *
 * MUON DUNG INTER:
 *   1. Chep 4 file vao res/font/: inter_regular.ttf, inter_medium.ttf,
 *      inter_semibold.ttf, inter_bold.ttf
 *   2. Doi dong duoi thanh FontFamily(Font(R.font.inter_regular, FontWeight.Normal), ...)
 *      va them import androidx.compose.ui.text.font.Font + vn.smarthome.R
 *
 * (Khong de san doan code do sau mot co boolean, vi Kotlin van phai phan giai
 * R.font.inter_* ngay ca trong nhanh khong chay - thieu file font la hong bien dich.)
 */
val SHFont: FontFamily = FontFamily.SansSerif

object SHType {
    val Logo      = TextStyle(fontFamily = SHFont, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val PageTitle = TextStyle(fontFamily = SHFont, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
    val CardTitle = TextStyle(fontFamily = SHFont, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    val Body      = TextStyle(fontFamily = SHFont, fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val BodyMed   = TextStyle(fontFamily = SHFont, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val Value     = TextStyle(fontFamily = SHFont, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val ValueBig  = TextStyle(fontFamily = SHFont, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    val Label     = TextStyle(fontFamily = SHFont, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val Caption   = TextStyle(fontFamily = SHFont, fontSize = 10.sp, fontWeight = FontWeight.Normal)
    val Button    = TextStyle(fontFamily = SHFont, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

val SHTypography = Typography(
    titleLarge  = SHType.PageTitle,
    titleMedium = SHType.CardTitle,
    bodyMedium  = SHType.Body,
    bodySmall   = SHType.Label,
    labelSmall  = SHType.Caption,
)
