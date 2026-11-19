package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.ui.shell.AppShell
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Kiem thu khoi dong, chay tren JVM bang Robolectric - khong can may that.
 *
 * Muc dich KHONG phai kiem tra giao dien dep hay xau (viec do phai nhin tren
 * may that), ma la bat loi chet app: thieu icon, chia cho 0, lay phan tu ngoai
 * mang, ve cung am... Nhung thu chi lo ra luc chay chu khong lo luc bien dich.
 *
 * `qualifiers` dat dung kich thuoc vung ve cua Tab A7 Lite nam ngang.
 *
 * LUU Y ve dong ho: sidebar va khung camera co vong lap `while(true) delay(...)`.
 * Neu de dong ho test tu chay thi `waitForIdle` se cho mai mai, nen phai tat
 * autoAdvance. Day la cach lam chuan khi co animation/vong lap vo han.
 */
@RunWith(AndroidJUnit4::class)
// sdk = 34 vi Robolectric 4.13 chua ho tro API 35 (du app compile voi 35).
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class ShellSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun start() {
        rule.mainClock.autoAdvance = false
        rule.setContent { SmartHomeTheme { AppShell() } }
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun trang_chu_hien_ra() {
        start()
        rule.onNodeWithText("Nhà thông minh của bạn").assertIsDisplayed()
        rule.onNodeWithText("Phòng khách").assertIsDisplayed()
        rule.onNodeWithText("Đèn: Bật").assertIsDisplayed()
    }

    @Test
    fun bam_dieu_khien_mo_duoc_man_chi_tiet_phong() {
        start()
        rule.onNodeWithText("Điều khiển").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Đèn phòng").assertIsDisplayed()
        rule.onNodeWithText("Quạt trần").assertIsDisplayed()
        rule.onNodeWithText("Điều hòa").assertIsDisplayed()
        rule.onNodeWithText("Chế độ hoạt động").assertIsDisplayed()
    }

    @Test
    fun mo_duoc_man_camera() {
        start()
        rule.onNodeWithText("Camera").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Camera an ninh").assertIsDisplayed()
        rule.onNodeWithText("Chụp ảnh").assertIsDisplayed()
        rule.onNodeWithText("Sân vườn").assertIsDisplayed()
        // "Cửa chính" xuat hien 2 cho: dong trong danh sach va nhan duoi khung xem.
        rule.onAllNodesWithText("Cửa chính").onFirst().assertIsDisplayed()
    }

    @Test
    fun man_cai_dat_la_man_that_de_khai_bao_broker() {
        start()
        rule.onNodeWithText("Cài đặt").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Broker EMQX — số điện").assertIsDisplayed()
        rule.onNodeWithText("Chưa khai báo broker").assertIsDisplayed()
        // Cot thu hai: khai bao go2rtc de lay hinh camera. Hai duong noi ra
        // ngoai phai cung nhin thay duoc, khong bat cuon moi thay.
        rule.onNodeWithText("go2rtc — hình camera").assertIsDisplayed()
        rule.onNodeWithText("Chưa khai báo go2rtc").assertIsDisplayed()
    }

    @Test
    fun tab_dien_nang_khong_bia_so_khi_chua_co_du_lieu() {
        start()
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Chua khai bao broker => khong duoc hien so mau nao, phai noi ro ly do.
        rule.onNodeWithText("Chưa có số liệu điện").assertIsDisplayed()
        rule.onNodeWithText("Chưa khai báo broker EMQX").assertIsDisplayed()
    }

    @Test
    fun muc_thong_ke_o_sidebar_mo_duoc_man_thong_ke() {
        start()
        // Muc "Thống kê" moi o sidebar (duoi Cai dat) phai mo man Thong ke.
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điện năng tiêu thụ").assertIsDisplayed()
    }

    @Test
    fun mo_duoc_man_thong_ke_va_doi_duoc_tab() {
        start()
        rule.onNodeWithText("Thống kê").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điện năng tiêu thụ").assertIsDisplayed()

        // Doi sang tab Gas - bieu do va tieu de phai doi theo, khong duoc dung hinh.
        rule.onNodeWithText("Gas").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Mức gas còn lại").assertIsDisplayed()
    }

    @Test
    fun muc_server_o_sidebar_mo_duoc_man_server() {
        start()
        // Muc "Server" moi o sidebar (duoi Cai dat, tren Thong ke) phai mo man Server.
        rule.onNodeWithText("Server").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Máy chủ serverubuntu").assertIsDisplayed()
        rule.onNodeWithText("Tài nguyên").assertIsDisplayed()
    }

    @Test
    fun cac_man_chua_lam_van_mo_duoc() {
        start()
        // Thiet bi da lam xong (hien luoi the), chi con Cam bien va Tu dong hoa
        // la placeholder.
        listOf("Cảm biến", "Tự động hóa").forEach { name ->
            rule.onNodeWithText(name).performClick()
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithText("Màn hình này sẽ làm ở giai đoạn sau").assertIsDisplayed()
        }
    }

    @Test
    fun man_thiet_bi_hien_luoi_the() {
        start()
        rule.onNodeWithText("Thiết bị").performClick()
        rule.mainClock.advanceTimeByFrame()
        // Co dong dem thiet bi va it nhat mot the Den.
        rule.onAllNodesWithText("Đèn").onFirst().assertIsDisplayed()
    }
}
