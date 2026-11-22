package vn.smarthome.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Ngan sach kich thuoc cho Samsung Galaxy Tab A7 Lite nam ngang.
 *
 * SO LIEU DO TREN MAY THAT (SM-T225, Android 13), khong phai uoc luong:
 *   adb shell wm size    -> Physical size: 800x1340
 *   adb shell wm density -> Physical density: 213      (tvdpi, khong phai 240)
 *
 * 213dpi => density 1.33125 => vung ve 1340/1.33125 x 800/1.33125 = 1006 x 601 dp.
 *
 * (Du an AquaControl ghi 893 x 533 dp vi doan density la 1.5. Con so do SAI
 * voi may nay - do thuc te ra 213dpi. Giu lai ghi chu nay de lan sau khong ai
 * chep nham lai.)
 *
 * App chay kiosk an ca hai thanh he thong nen duoc dung tron 1006 x 601 dp.
 * De nguyen hai thanh thi chieu cao chi con ~586dp.
 *
 * CHIEU CAO VAN LA PHIA EO: sidebar va thanh tieu de co dinh, phan con lai
 * chia theo weight, khong de the tu dan ra.
 */
object Dim {
    const val DesignW = 1006
    const val DesignH = 601

    val SidebarWidth = 164.dp
    val HeaderHeight = 68.dp
    val MainPadding  = 12.dp
    val CardGap      = 10.dp
    val CardRadius   = 14.dp
    val CardPadding  = 12.dp
    val BorderWidth  = 1.dp

    val TouchTarget  = 44.dp
    val NavItemH     = 38.dp
    val DeviceRowH   = 60.dp
    val ButtonH      = 42.dp
}
