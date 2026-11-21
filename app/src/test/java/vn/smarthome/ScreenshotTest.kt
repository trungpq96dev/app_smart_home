package vn.smarthome

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import vn.smarthome.data.DemoData
import vn.smarthome.ui.screens.CameraScreen
import vn.smarthome.ui.screens.DevicesScreen
import vn.smarthome.ui.screens.ChiTietDienScreen
import vn.smarthome.ui.screens.HomeScreen
import vn.smarthome.ui.screens.RoomDetailScreen
import vn.smarthome.ui.screens.ServerScreen
import vn.smarthome.ui.screens.SettingsScreen
import vn.smarthome.ui.screens.StatsScreen
import vn.smarthome.ui.screens.TienIchScreen
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.dien.CauHinhMqtt
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.thoitiet.CauHinhThoiTiet
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.dien.NgayKwh
import vn.smarthome.ui.shell.AppShell
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SmartHomeTheme
import vn.smarthome.vm.HomeState
import java.io.File
import java.io.FileOutputStream

/**
 * Ve giao dien ra file PNG ngay tren may tinh, khong can may tinh bang.
 *
 * `tvdpi` = 213dpi = density 1.33125, cong voi w1006dp-h601dp thanh 1340 x 800 px
 * - DUNG do phan giai va DUNG mat do diem anh do duoc tren may that bang
 * `adb shell wm size` / `wm density`. Anh xuat ra o app/build/screenshots/.
 *
 * Day KHONG thay the viec cai len may that (font, inset thanh dieu huong va
 * ti le man hinh that van co the lech), nhung du de bat loi tran bo cuc,
 * chu bi cat, the chong len nhau - nhung thu truoc day chi phat hien duoc
 * sau khi da cai vao may.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class ScreenshotTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                Box(Modifier.fillMaxSize().background(SH.Bg)) { content() }
            }
        }
        // Chay het animation mo man roi moi chup.
        rule.mainClock.advanceTimeBy(1200)
        rule.waitForIdle()

        val view = rule.activity.window.decorView
        val w = if (view.width > 0) view.width else 1340
        val h = if (view.height > 0) view.height else 800
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))

        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        println("SCREENSHOT " + File(dir, "$name.png").absolutePath + " " + w + "x" + h)
    }

    /** Vung noi dung ben phai sidebar, dung padding nhu AppShell. */
    @Composable
    private fun Content(body: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().padding(12.dp)) { body() }
    }

    @Test
    fun chup_trang_chu() {
        shoot("1-trang-chu") { AppShell() }
    }

    @Test
    fun chup_tien_ich_lich_am() {
        // Ngay co dinh chu khong phai LocalDate.now(): anh chup phai giong nhau
        // moi lan chay, neu khong thi khong doi chieu duoc voi lan truoc.
        shoot("8-tien-ich-lich-am") {
            Content {
                TienIchScreen(onBack = {}, homNay = java.time.LocalDate.of(2026, 11, 20))
            }
        }
    }

    @Test
    fun chup_chi_tiet_phong() {
        shoot("2-chi-tiet-phong") {
            Content {
                RoomDetailScreen(DemoData.rooms()[1], {}, {}, {}, {}, {}, {}, {})
            }
        }
    }

    @Test
    fun chup_camera() {
        shoot("3-camera") {
            // CHUA khai bao go2rtc: khung xem van la o giu cho, khong co
            // WebView nao duoc dung - nen anh render van chup duoc binh thuong.
            Content {
                CameraScreen(
                    HomeState(), CauHinhCamera(), TrangThaiCam.ChuaCauHinh,
                    {}, {}, {}, {},
                )
            }
        }
    }

    @Test
    fun chup_thong_ke_dien_chua_co_so() {
        // Chua khai bao broker: phai hien "--" va noi ro ly do, KHONG duoc
        // lay so mau bu vao (quy uoc "khong bia so" cua du an).
        shoot("4-thong-ke-dien-trong") {
            Content { StatsScreen(HomeState(), DienState(), KetNoi.ChuaCauHinh, {}, {}, {}) }
        }
    }

    @Test
    fun chup_thong_ke_dien_co_so() {
        shoot("4b-thong-ke-dien-co-so") {
            Content { StatsScreen(HomeState(), dienMau(), KetNoi.DaNoi, {}, {}, {}) }
        }
    }

    @Test
    fun chup_chi_tiet_dien() {
        shoot("4c-chi-tiet-dien") {
            Content { ChiTietDienScreen(dienMau(), {}) }
        }
    }

    @Test
    fun chup_server() {
        shoot("8-server") {
            Content { ServerScreen(server = vn.smarthome.data.server.ServerState(), onBack = {}) }
        }
    }

    /**
     * Man Cam bien dung GOI THAT bat tu broker ngay 21/11/2026 (cong C1 dang
     * cam tai, USB-A cam mot thu gi do). Dung `gop()` chu khong dung san
     * `CuktechState` de anh chup di qua dung duong ma so that di.
     */
    @Test
    fun chup_cam_bien_cu_sac() {
        val goiThat = """
            {"connected":true,"authenticated":true,
             "device_model":"njcuk.fitting.ad1204_HS6621C","firmware_version":"2.1.2_0073",
             "ports":{"1":{"voltage":20.1,"current":2.3,"power":46.2,"active":true,"protocol":"PD","enabled":true},
                      "2":{"voltage":0.0,"current":0.0,"power":0.0,"active":false,"protocol":"idle","enabled":true},
                      "3":{"voltage":0.0,"current":0.0,"power":0.0,"active":false,"protocol":"idle","enabled":true},
                      "4":{"voltage":5.1,"current":0.3,"power":1.5,"active":true,"protocol":"5V","enabled":true}}}
        """.trimIndent()
        val ct = vn.smarthome.data.cuktech.CuktechState.gop(
            vn.smarthome.data.cuktech.CuktechState(),
            vn.smarthome.data.cuktech.CuktechTopic.STATUS,
            goiThat,
        )
        val goiCaiDat = """
            {"21":50532111,"5":1,"6":2,"8":0,"9":0,"10":0,"11":0,"12":0,
             "13":0,"15":0,"16":15,"17":120391469,"18":522,"19":1,"20":0}
        """.trimIndent()
        val ct2 = vn.smarthome.data.cuktech.CuktechState.gop(
            ct, vn.smarthome.data.cuktech.CuktechTopic.SETTINGS, goiCaiDat,
        )
        shoot("9-cam-bien-cu-sac") {
            Content { vn.smarthome.ui.screens.CamBienScreen(cuktech = ct2, onBack = {}) }
        }
    }

    /**
     * Ban tin `status` ket o connected:false trong khi so cong van ve - canh
     * gap that ngay 21/11/2026. Man hinh phai noi dung la "trang thai cu" va
     * dua ra nut Noi lai, chu khong keu mat ket noi.
     */
    @Test
    fun chup_cam_bien_trang_thai_cu() {
        val t0 = System.currentTimeMillis()
        val cu = """
            {"connected": false, "device_model":"njcuk.fitting.ad1204_HS6621C",
             "firmware_version":"2.1.2_0073"}
        """.trimIndent()
        val cong = """
            {"voltage":20.1,"current":2.3,"power":46.2,"active":true,
             "protocol":"PD","enabled":true}
        """.trimIndent()
        var ct = vn.smarthome.data.cuktech.CuktechState.gop(
            vn.smarthome.data.cuktech.CuktechState(),
            vn.smarthome.data.cuktech.CuktechTopic.STATUS, cu, t0,
        )
        ct = vn.smarthome.data.cuktech.CuktechState.gop(
            ct, vn.smarthome.data.cuktech.CuktechTopic.TIEN_TO_CONG + "c1", cong, t0,
        )
        ct = vn.smarthome.data.cuktech.CuktechState.gop(
            ct, vn.smarthome.data.cuktech.CuktechTopic.SETTINGS, """{"16":15,"5":1,"6":2}""", t0,
        )
        shoot("9-cam-bien-trang-thai-cu") {
            Content { vn.smarthome.ui.screens.CamBienScreen(cuktech = ct, onBack = {}) }
        }
    }

    /** Chua nhan goi nao: phai noi thang la chua co so, khong duoc bia. */
    @Test
    fun chup_cam_bien_chua_co_so() {
        shoot("9-cam-bien-chua-co-so") {
            Content {
                vn.smarthome.ui.screens.CamBienScreen(
                    cuktech = vn.smarthome.data.cuktech.CuktechState(),
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun chup_thiet_bi() {
        shoot("7-thiet-bi") {
            Content { DevicesScreen(HomeState(), {}, {}) }
        }
    }

    @Test
    fun chup_cai_dat() {
        shoot("6-cai-dat") {
            Content {
                SettingsScreen(
                    CauHinhMqtt(), KetNoi.ChuaCauHinh,
                    CauHinhCamera(), TrangThaiCam.ChuaCauHinh,
                    CauHinhThoiTiet(), CauHinhGiongNoi(), emptyList(),
                    {}, {}, {}, {}, {},
                )
            }
        }
    }

    /**
     * So gia CHI DUNG TRONG TEST de chup anh bo cuc - khong nam trong app.
     * Lay dung vi du cua tai lieu ban giao dien_evn: ~209 kWh ca ky thi tien
     * that khoang 480.650d (bac thang luy tien), khong phai 126.000d.
     */
    private fun dienMau(): DienState {
        val ngay = (1..14).map { "2026-11-%02d".format(it) }
        val kwh = listOf(6.2, 7.1, 5.4, 8.0, 6.6, 9.2, 7.4, 6.9, 8.3, 5.8, 7.7, 6.1, 9.0, 7.2)
        return DienState(
            trangThai = "ok",
            ngaySanLuong = 7.2,
            ngay = "2026-11-14",
            thangSanLuong = 209.0,
            thangTien = 480650.0,
            congToChiSo = 15234.0,
            congNoSoTien = 0.0,
            lichSu = ngay.zip(kwh) { n, k -> NgayKwh(n, k) },
            capNhat = java.time.LocalDateTime.now().minusHours(2)
                .format(DienState.DINH_DANG_CAP_NHAT),
        )
    }

    @Test
    fun chup_man_chua_lam() {
        shoot("5-man-chua-lam") { AppShell() }
        rule.onNodeWithText("Thiết bị").performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(
            maxOf(view.width, 1), maxOf(view.height, 1), Bitmap.Config.ARGB_8888
        )
        view.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        FileOutputStream(File(dir, "5-man-chua-lam.png")).use {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
