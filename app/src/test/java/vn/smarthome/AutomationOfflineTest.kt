package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CauHinhBomNuoc
import vn.smarthome.data.bomnuoc.ChanIo
import vn.smarthome.data.bomnuoc.KenhBomNuoc
import vn.smarthome.data.bomnuoc.LuongNuocThang
import vn.smarthome.data.bomnuoc.MucNhatKy
import vn.smarthome.data.bomnuoc.NhomNhatKy
import vn.smarthome.data.bomnuoc.SuKienBom
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.ui.screens.AutomationScreen
import vn.smarthome.ui.theme.SmartHomeTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class AutomationOfflineTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun mauState(
        locked: Int = 1,
        fault: String = "Bom len bon qua nong (71.5 do C)",
    ) = BomNuocState(
        tankOn = 0,
        tankMode = "auto",
        boostOn = 0,
        bstMode = "off",
        den = 1,
        locked = locked,
        fault = fault,
        io = listOf(ChanIo("Còi", 5, 1)),
    )

    @Test
    fun bo_bom_offline_khoa_toan_bo_nut_dieu_khien_nhung_giu_nut_cuc_bo() {
        var modeClicks = 0
        var boosterClicks = 0
        var ledClicks = 0
        var clearClicks = 0
        var muteClicks = 0
        var testBuzzerClicks = 0
        var saveProtClicks = 0
        var emptyClicks = 0
        var fullClicks = 0
        var resetFlowClicks = 0
        var clearUsageClicks = 0

        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                AutomationScreen(
                    onBack = {},
                    onNotice = {},
                    bomNuoc = mauState(locked = 1),
                    luongNuocThang = listOf(LuongNuocThang(thang = "2026-11", bomLit = 500.0, nuocMayLit = null)),
                    ketNoiBom = TrangThaiKetNoiBom.BoOffline,
                    kenhBom = KenhBomNuoc.MQTT,
                    cauHinhBom = CauHinhBomNuoc(espHost = "192.168.0.120"),
                    nhatKyBom = listOf(
                        SuKienBom(1L, 1763800000000L, NhomNhatKy.BOM, "Test", "Chi tiết", MucNhatKy.THONG_TIN),
                    ),
                    onDatCheDoBon = { modeClicks++ },
                    onDatBomTangAp = { boosterClicks++ },
                    onDatDenPhuBom = { ledClicks++ },
                    onXoaLoiBom = { clearClicks++ },
                    onTatCoiBom = { muteClicks++ },
                    onThuCoiBom = { testBuzzerClicks++ },
                    onLuuBaoVeBom = { _, _, _, _, _ -> saveProtClicks++ },
                    onGhiMocCanBom = { emptyClicks++ },
                    onGhiMocDayBom = { fullClicks++ },
                    onDatLaiDemLuuLuong = { resetFlowClicks++ },
                    onXoaLichSuNuocTrenBo = { clearUsageClicks++ },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Bo bơm offline").assertIsDisplayed()

        val nutBat = rule.onNode(hasText("BẬT") and hasClickAction())
        val nutTat = rule.onNode(hasText("TẮT") and hasClickAction())

        nutBat.assertIsNotEnabled()
        nutTat.assertIsNotEnabled()
        rule.onNodeWithText("AUTO").assertIsNotEnabled()
        rule.onNodeWithText("BẬT TAY").assertIsNotEnabled()
        rule.onNodeWithText("TẮT TAY").assertIsNotEnabled()
        rule.onNodeWithText("Xoá lỗi").assertIsNotEnabled()
        rule.onNodeWithText("Tắt còi").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Bơm tăng áp").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Đèn phụ hộp điện").assertIsNotEnabled()

        nutBat.performClick()
        nutTat.performClick()
        rule.onNodeWithText("Xoá lỗi").performClick()
        rule.mainClock.advanceTimeByFrame()

        assertEquals(0, modeClicks)
        assertEquals(0, boosterClicks)
        assertEquals(0, ledClicks)
        assertEquals(0, clearClicks)
        assertEquals(0, muteClicks)

        // Tab Bao ve: tat ca nut gui lenh xuong bo deu phai bi khoa
        rule.onNodeWithText("Bảo vệ").performClick()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Lưu cài đặt bảo vệ").assertIsNotEnabled()
        rule.onNodeWithText("Ghi mốc CẠN").assertIsNotEnabled()
        rule.onNodeWithText("Ghi mốc ĐẦY").assertIsNotEnabled()
        rule.onNodeWithText("Đặt lại bộ đếm").assertIsNotEnabled()
        rule.onNodeWithText("Tắt còi").assertIsNotEnabled()
        rule.onNodeWithText("Kêu thử").assertIsNotEnabled()

        assertEquals(0, saveProtClicks)
        assertEquals(0, emptyClicks)
        assertEquals(0, fullClicks)
        assertEquals(0, resetFlowClicks)
        assertEquals(0, testBuzzerClicks)

        // Tab Nhat ky: Xoa lich su tren bo bi khoa, nhung Xoa nhat ky cuc bo van bam duoc
        rule.onNodeWithText("Nhật ký").performClick()
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Xoá lịch sử trên bo").assertIsNotEnabled()
        rule.onNodeWithText("Xoá nhật ký").assertIsEnabled()
        assertEquals(0, clearUsageClicks)
    }

    @Test
    fun bo_bom_offline_khi_khong_khoa_van_khoa_bom_tang_ap_va_den_phu() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                AutomationScreen(
                    onBack = {},
                    onNotice = {},
                    bomNuoc = mauState(locked = 0, fault = ""),
                    ketNoiBom = TrangThaiKetNoiBom.BoOffline,
                    kenhBom = KenhBomNuoc.LAN,
                    cauHinhBom = CauHinhBomNuoc(espHost = "192.168.0.120"),
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithContentDescription("Bơm tăng áp").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Đèn phụ hộp điện").assertIsNotEnabled()
    }

    @Test
    fun bo_bom_online_cho_phep_bam_dieu_khien() {
        var lastMode: String? = null
        var clearClicks = 0

        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                AutomationScreen(
                    onBack = {},
                    onNotice = {},
                    bomNuoc = mauState(locked = 0, fault = "Hai moc muc nuoc bi dao nguoc"),
                    ketNoiBom = TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.LAN, 0),
                    kenhBom = KenhBomNuoc.LAN,
                    cauHinhBom = CauHinhBomNuoc(espHost = "192.168.0.120"),
                    onDatCheDoBon = { lastMode = it },
                    onXoaLoiBom = { clearClicks++ },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()

        val nutBat = rule.onNode(hasText("BẬT") and hasClickAction())
        val nutTat = rule.onNode(hasText("TẮT") and hasClickAction())

        nutBat.assertIsEnabled()
        nutTat.assertIsEnabled()
        rule.onNodeWithText("AUTO").assertIsEnabled()
        rule.onNodeWithText("Xoá lỗi").assertIsEnabled()
        rule.onNodeWithText("Tắt còi").assertIsEnabled()
        rule.onNodeWithContentDescription("Bơm tăng áp").assertIsEnabled()
        rule.onNodeWithContentDescription("Đèn phụ hộp điện").assertIsEnabled()

        nutBat.performClick()
        rule.onNodeWithText("Xoá lỗi").performClick()
        rule.mainClock.advanceTimeByFrame()

        assertEquals("on", lastMode)
        assertEquals(1, clearClicks)
    }
}
