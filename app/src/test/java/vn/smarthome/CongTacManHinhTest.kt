package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isToggleable
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.DemoData
import vn.smarthome.data.congtac.OThietBi
import vn.smarthome.ui.screens.RoomDetailScreen
import vn.smarthome.ui.theme.SmartHomeTheme

/**
 * Man chi tiet phong voi cong tac THAT.
 *
 * Kiem dung mot thu, nhung la thu de sai nhat va dat nhat neu sai: **mat ket
 * noi thi KHONG duoc ve mot cai cong tac nam o the TAT**. Nguoi nhin vao se
 * tuong den da tat, trong khi app khong he biet den dang the nao.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class CongTacManHinhTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val phong = DemoData.rooms().first { it.id == "bed1" }

    private fun ve(oDen: OThietBi?, oQuat: OThietBi? = null) {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                RoomDetailScreen(
                    room = phong,
                    onBack = {}, onHome = {},
                    onToggleLight = {}, onToggleFan = {},
                    onAcTemp = {}, onMode = {}, onToggleSchedule = {},
                    oDen = oDen, oQuat = oQuat,
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
    }

    private fun that(
        online: Boolean = true,
        bat: Boolean? = true,
        bridgeSong: Boolean = true,
        dangCho: Boolean = false,
        mong: Boolean? = null,
        lyDoLoi: String? = null,
    ) = OThietBi.That("Đèn Phòng ngủ 1", bridgeSong, online, bat, dangCho, mong, lyDoLoi)

    @Test
    fun thiet_bi_mau_giu_nguyen_hanh_vi_cu() {
        // oDen = null: man hinh phai chay y het truoc khi co cong tac that.
        ve(null)
        rule.onNodeWithText("Đèn phòng").assertIsDisplayed()
        // Ba cong tac gat duoc: den, quat, lich hen.
        assertEquals(3, rule.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
    }

    @Test
    fun mat_ket_noi_thi_KHONG_VE_cong_tac_o_the_tat() {
        ve(that(online = false, bat = null, lyDoLoi = "mất điện"))
        rule.onNodeWithText("mất điện").assertIsDisplayed()
        rule.onNodeWithText("—").assertIsDisplayed()
        // Den mat con 2 cong tac gat duoc (quat + lich hen) - den da bien mat.
        assertEquals(2, rule.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
    }

    @Test
    fun bridge_chua_chay_thi_cung_khong_ve_cong_tac() {
        ve(that(bridgeSong = false, online = false, bat = null))
        rule.onNodeWithText("Bộ nối chưa chạy trên máy chủ").assertIsDisplayed()
        rule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun online_ma_khong_doc_duoc_cong_tac_cung_ve_dau_gach() {
        ve(that(online = true, bat = null))
        rule.onNodeWithText("Chưa đọc được công tắc").assertIsDisplayed()
        rule.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun online_va_ro_thi_gat_duoc_binh_thuong() {
        ve(that(online = true, bat = true))
        assertEquals(3, rule.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
        rule.onAllNodes(isToggleable())[0].assertIsEnabled()
    }

    @Test
    fun dang_cho_thi_ve_the_mong_muon_nhung_khoa_gat_lai() {
        ve(that(online = true, bat = false, dangCho = true, mong = true))
        rule.onNodeWithText("Đang gửi lệnh…").assertIsDisplayed()
        // Van con la mot cong tac (de mat khong nhay), nhung bam khong an.
        rule.onAllNodes(isToggleable())[0].assertIsNotEnabled()
    }
}
