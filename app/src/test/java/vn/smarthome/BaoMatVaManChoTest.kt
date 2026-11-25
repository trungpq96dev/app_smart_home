package vn.smarthome

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.baomat.CauHinhBaoMat
import vn.smarthome.data.baomat.GasMqttState
import vn.smarthome.data.baomat.ManChoLogic
import vn.smarthome.data.baomat.PinHasher
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CauHinhBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.thoitiet.NgayThoiTiet
import vn.smarthome.data.thoitiet.ThoiTietData
import vn.smarthome.data.thoitiet.ThoiTietHienTai
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.ui.components.HopThoaiPinAdmin
import vn.smarthome.ui.components.ManHinhCho
import vn.smarthome.ui.theme.SmartHomeTheme

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w1006dp-h601dp-land-tvdpi")
class BaoMatVaManChoTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pin_hasher_kiem_tra_va_tao_muoi() {
        val def = CauHinhBaoMat()
        assertTrue(PinHasher.kiemTra("1234", def.pinSalt, def.pinHash))
        assertFalse(PinHasher.kiemTra("9999", def.pinSalt, def.pinHash))
        assertFalse(PinHasher.hopLe4So("123"))
        assertFalse(PinHasher.hopLe4So("12ab"))

        val salt = PinHasher.taoMuoi()
        val hash = PinHasher.hash("5678", salt)
        assertNotEquals(def.pinHash, hash)
        assertTrue(PinHasher.kiemTra("5678", salt, hash))
    }

    @Test
    fun logic_khung_gio_dem_pir_va_su_co_khan_cap() {
        assertTrue(ManChoLogic.laKhungGioDem(22, 22, 6))
        assertTrue(ManChoLogic.laKhungGioDem(2, 22, 6))
        assertFalse(ManChoLogic.laKhungGioDem(6, 22, 6))
        assertFalse(ManChoLogic.laKhungGioDem(15, 22, 6))

        assertTrue(ManChoLogic.parsePirMotion("1"))
        assertTrue(ManChoLogic.parsePirMotion("""{"motion":true}"""))
        assertFalse(ManChoLogic.parsePirMotion("""{"motion":false}"""))

        assertEquals(
            GasMqttState(alert = true, ppm = 320),
            ManChoLogic.parseGasAlert("""{"ppm":320}""", 200),
        )

        assertNull(
            ManChoLogic.kiemTraSuCoKhanCap(
                kitchenGasAlert = false,
                gasMqtt = GasMqttState(),
                bomNuoc = null,
                ketNoiBom = TrangThaiKetNoiBom.ChuaCauHinh,
                ketNoiMqtt = KetNoi.ChuaCauHinh,
            )
        )

        val pumpLock = ManChoLogic.kiemTraSuCoKhanCap(
            kitchenGasAlert = false,
            gasMqtt = GasMqttState(),
            bomNuoc = BomNuocState(locked = 1, fault = "Bom len bon chay nhung khong co nuoc"),
            ketNoiBom = TrangThaiKetNoiBom.ChuaCauHinh,
            ketNoiMqtt = KetNoi.DaNoi,
        )
        assertNotNull(pumpLock)
        assertEquals("pump", pumpLock?.loai)
        assertEquals("automation", pumpLock?.routeDich)
    }

    @Test
    fun man_hinh_cho_hien_du_thong_tin_va_cham_de_danh_thuc() {
        var woke = false
        val weather = ThoiTietUi.Co(
            ThoiTietData(
                diaDiem = "Hà Nội",
                hienTai = ThoiTietHienTai(
                    nhietDo = 20,
                    camGiac = 19,
                    doAm = 78,
                    gio = 8,
                    maWmo = 1,
                    banNgay = false,
                ),
                cacNgay = listOf(NgayThoiTiet("Hôm nay", 1, 24, 18)),
                gioCapNhat = "23:25",
            )
        )
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                ManHinhCho(
                    thoiTiet = weather,
                    dien = DienState(ngaySanLuong = 6.1),
                    bomNuoc = BomNuocState(tankMode = "auto"),
                    ketNoiBom = TrangThaiKetNoiBom.ChuaCauHinh,
                    cauHinhBom = CauHinhBomNuoc(espHost = "192.168.0.120"),
                    cauHinhBaoMat = CauHinhBaoMat(giamSangDem = false),
                    onDanhThuc = { woke = true },
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("20°C").assertIsDisplayed()
        rule.onNodeWithText("Hà Nội · Ít mây").assertIsDisplayed()
        rule.onNodeWithText("Hệ thống an toàn").assertIsDisplayed()
        rule.onNodeWithText("Điện hôm nay: 6.1 kWh").assertIsDisplayed()
        rule.onNodeWithText("Bơm: Tự động").assertIsDisplayed()

        rule.onNodeWithText("Chạm màn hình để đánh thức").performClick()
        assertTrue(woke)
    }

    @Test
    fun hop_thoai_pin_nhan_du_4_so_va_goi_xac_nhan() {
        var submittedPin: String? = null
        rule.mainClock.autoAdvance = false
        rule.setContent {
            SmartHomeTheme {
                HopThoaiPinAdmin(
                    moTaHanhDong = "Khởi động lại Router",
                    loi = null,
                    onXacNhan = { submittedPin = it },
                    onDong = {},
                )
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Khóa PIN Kỹ thuật").assertIsDisplayed()
        listOf("1", "2", "3", "4").forEach { d ->
            rule.onNodeWithText(d).performClick()
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals("1234", submittedPin)
    }
}

