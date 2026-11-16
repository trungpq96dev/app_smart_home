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
    fun mo_duoc_man_thong_ke_va_doi_duoc_tab() {
        start()
        rule.onNodeWithText("Xem chi tiết").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Điện năng tiêu thụ").assertIsDisplayed()

        // Doi sang tab Gas - bieu do va tieu de phai doi theo, khong duoc dung hinh.
        rule.onNodeWithText("Gas").performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Mức gas còn lại").assertIsDisplayed()
    }

    @Test
    fun cac_man_chua_lam_van_mo_duoc() {
        start()
        listOf("Thiết bị", "Cảm biến", "Tự động hóa", "Cài đặt").forEach { name ->
            rule.onNodeWithText(name).performClick()
            rule.mainClock.advanceTimeByFrame()
            rule.onNodeWithText("Màn hình này sẽ làm ở giai đoạn sau").assertIsDisplayed()
        }
    }
}
