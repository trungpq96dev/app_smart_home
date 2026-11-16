package vn.smarthome

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import vn.smarthome.ui.shell.AppShell
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SmartHomeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // May tinh bang treo tuong: khong de man tu tat khi dang xem bang dieu khien.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        hideSystemBars()

        setContent {
            SmartHomeTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(SH.Bg)
                        .safeDrawingPadding()
                ) {
                    AppShell()
                }
            }
        }
    }

    /**
     * Che do kiosk: an thanh trang thai va thanh dieu huong.
     *
     * Do tren may that (SM-T225) cho thay de nguyen hai thanh nay thi app chi
     * con 1340x780 px va co mot dai den o canh tren - khong giong ban thiet ke
     * von chiem tron man hinh. An di thi app duoc dung 1340x800 px = 1006x601dp.
     *
     * TRANSIENT_BARS_BY_SWIPE: vuot tu canh man hinh van goi lai duoc hai thanh
     * trong vai giay roi chung tu an - can thiet de con duong thoat app.
     *
     * KHONG MUON KIOSK: xoa loi goi hideSystemBars() o onCreate.
     */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Quay lai app sau khi chuyen di cho khac thi an lai hai thanh.
        if (hasFocus) hideSystemBars()
    }
}
