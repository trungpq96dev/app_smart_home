package vn.smarthome.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Bang mau Smart Home Dark - lay tu anh thiet ke.
 * Khong dung den tuyet doi (#000) o bat ky dau; nen toi chiem ~90% man hinh.
 */
object SH {
    val Bg       = Color(0xFF0B1220)  // nen trang
    val Sidebar  = Color(0xFF0E1627)  // cot menu trai
    val Card     = Color(0xFF16213A)  // the chinh
    val Card2    = Color(0xFF1C2946)  // the phu / o ben trong the
    val Border   = Color(0xFF253453)
    val BorderHi = Color(0xFF3B82F6)  // vien khi duoc chon

    val Blue     = Color(0xFF3B82F6)  // thanh phan tuong tac chinh
    val BlueDeep = Color(0xFF2563EB)
    val Green    = Color(0xFF22C55E)  // dang bat / binh thuong
    val Amber    = Color(0xFFFBBF24)  // canh bao / den
    val Red      = Color(0xFFEF4444)  // loi / nhiet do
    val Purple   = Color(0xFF8B5CF6)  // dieu hoa
    val Cyan     = Color(0xFF22D3EE)  // do am / nuoc
    val Teal     = Color(0xFF14B8A6)  // rem

    val Text     = Color(0xFFF1F5F9)
    val TextDim  = Color(0xFF94A3B8)
    val TextOff  = Color(0xFF64748B)
}

/** Nen mo cho pill/chip/vong icon - giu nen toi. */
fun Color.tint(alpha: Float = 0.16f): Color = copy(alpha = alpha)
