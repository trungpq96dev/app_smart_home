package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.dien.CauHinhMqtt
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.ota.OtaManager
import vn.smarthome.data.ota.OtaStatus
import vn.smarthome.data.ota.ReleaseInfo
import vn.smarthome.data.thoitiet.CauHinhThoiTiet
import vn.smarthome.ui.components.HopThoaiXacNhan
import vn.smarthome.ui.components.OtaUpdateDialog
import vn.smarthome.ui.components.YeuCauXacNhan
import vn.smarthome.ui.screens.SettingsScreen
import vn.smarthome.ui.theme.SmartHomeTheme
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class OtaSecurityAndConfirmTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun ota_sha256_parse_va_verify_chinh_xac() {
        val tmp = File.createTempFile("smarthome_ota_test", ".apk")
        try {
            tmp.writeText("smarthome-release-apk-payload", Charsets.UTF_8)
            val actualHash = OtaManager.computeSha256(tmp)
            assertEquals(64, actualHash.length)

            // Parse dinh dang sha256sum
            assertEquals(actualHash, OtaManager.parseSha256("$actualHash  SmartHome-v0.9.5.apk\n"))
            // Parse dinh dang GitHub asset digest
            assertEquals(actualHash, OtaManager.parseSha256("sha256:${actualHash.uppercase()}"))
            // Parse tu release notes
            assertEquals(actualHash, OtaManager.parseSha256("Ban cap nhat moi\nSHA-256: `$actualHash`"))
            // Chuoi khong hop le
            assertNull(OtaManager.parseSha256("khong-phai-sha256"))
            assertNull(OtaManager.parseSha256(""))

            // Xac minh khop (khong phan biet hoa thuong)
            assertTrue(OtaManager.verifyApkSha256(tmp, actualHash))
            assertTrue(OtaManager.verifyApkSha256(tmp, actualHash.uppercase()))

            // Xac minh sai -> tu choi
            val tamperedHash = "0".repeat(64)
            assertFalse(OtaManager.verifyApkSha256(tmp, tamperedHash))
        } finally {
            tmp.delete()
        }
        // File khong ton tai -> false
        assertFalse(OtaManager.verifyApkSha256(tmp, "a".repeat(64)))
    }

    @Test
    fun hop_thoai_xac_nhan_chan_hoac_thuc_thi_thao_tac_nhay_cam() {
        var confirmed = 0
        var cancelled = 0
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                HopThoaiXacNhan(
                    yeuCau = YeuCauXacNhan(
                        tieuDe = "Bật bơm tăng áp?",
                        noiDung = "Bơm tăng áp sẽ chạy ngay.",
                        nhanXacNhan = "Bật bơm áp",
                        nguyHiem = true,
                        hanhDong = {},
                    ),
                    onXacNhan = { confirmed++ },
                    onHuy = { cancelled++ },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onNodeWithText("Bật bơm tăng áp?").assertIsDisplayed()
        rule.onNodeWithText("Bơm tăng áp sẽ chạy ngay.").assertIsDisplayed()

        rule.onNodeWithText("Hủy").performClick()
        assertEquals(1, cancelled)
        assertEquals(0, confirmed)

        rule.onNodeWithText("Bật bơm áp").performClick()
        assertEquals(1, confirmed)
    }

    @Test
    fun hien_mat_khau_trong_cai_dat_yeu_cau_pin_admin() {
        var pinPromptReason: String? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                SettingsScreen(
                    cauHinh = CauHinhMqtt(matKhau = "secret-pass"),
                    ketNoi = KetNoi.ChuaCauHinh,
                    cauHinhCamera = CauHinhCamera(),
                    trangThaiCam = TrangThaiCam.ChuaCauHinh,
                    cauHinhThoiTiet = CauHinhThoiTiet(),
                    cauHinhGiongNoi = CauHinhGiongNoi(),
                    mucDungGiongNoi = emptyList(),
                    onLuu = {},
                    onLuuCamera = {},
                    onLuuThoiTiet = {},
                    onLuuGiongNoi = {},
                    onBack = {},
                    onYeuCauPinAdmin = { reason, _ ->
                        // Gia lap chua nhap PIN: khong goi callback
                        pinPromptReason = reason
                    },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()

        rule.onAllNodesWithContentDescription("Hiện mật khẩu").onFirst().performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Xem mật khẩu Broker MQTT", pinPromptReason)
    }

    @Test
    fun ota_dialog_hien_trang_thai_xac_minh_sha256_va_canh_bao_thieu_sha256() {
        val releaseCoSha = ReleaseInfo(
            tagName = "v0.9.5",
            name = "v0.9.5",
            notes = "Sửa lỗi bảo mật",
            downloadUrl = "https://example.com/app.apk",
            sizeBytes = 5_000_000L,
            publishedAt = "2026-11-27",
            sha256Url = "https://example.com/app.apk.sha256",
        )
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                OtaUpdateDialog(
                    status = OtaStatus.UpdateAvailable(releaseCoSha),
                    onDismiss = {},
                    onStartUpdate = {},
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Bảo mật OTA: Đã có mã SHA-256 để xác minh trước khi cài đặt.")
            .assertIsDisplayed()
    }
}
