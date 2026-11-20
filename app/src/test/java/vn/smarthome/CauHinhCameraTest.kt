package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.camera.CauHinhCamera

/**
 * Kiem phan dung dia chi go2rtc.
 *
 * Nho be nhung dang kiem: sai mot ky tu trong URL thi tren may that chi thay
 * mot khung den, khong co thong bao gi de lan ra nguyen nhan.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CauHinhCameraTest {

    @Test
    fun `host rong thi chua day du`() {
        assertFalse(CauHinhCamera(host = "").dayDu())
        assertFalse(CauHinhCamera(host = "   ").dayDu())
    }

    @Test
    fun `mac dinh da tro san ve serverubuntu`() {
        // Tu ban 0.3.1 cau hinh mac dinh KHONG con rong: host 192.168.0.50 va
        // cong 1984 duoc dien san de may cai moi tu len hinh, khong phai go tay.
        // Doi mac dinh ve rong thi phai sua lai ca man Cai dat lan mo ta nay.
        val macDinh = CauHinhCamera()
        assertTrue(macDinh.dayDu())
        assertEquals("192.168.0.50", macDinh.host)
        assertEquals(1984, macDinh.cong)
    }

    @Test
    fun `cong ngoai khoang thi chua day du`() {
        assertFalse(CauHinhCamera(host = "192.168.0.105", cong = 0).dayDu())
        assertFalse(CauHinhCamera(host = "192.168.0.105", cong = 70000).dayDu())
        assertTrue(CauHinhCamera(host = "192.168.0.105", cong = 1984).dayDu())
    }

    @Test
    fun `mac dinh la http cong 1984`() {
        assertEquals(
            "http://192.168.0.105:1984",
            CauHinhCamera(host = "192.168.0.105").goc(),
        )
    }

    @Test
    fun `bat tls thi doi sang https`() {
        assertEquals(
            "https://abc.ts.net:443",
            CauHinhCamera(host = "abc.ts.net", cong = 443, tls = true).goc(),
        )
    }

    @Test
    fun `go khoang trang va dau gach cuoi`() {
        assertEquals(
            "http://192.168.0.105:1984",
            CauHinhCamera(host = "  192.168.0.105/  ").goc(),
        )
    }

    /**
     * Day la cho de sai nhat. `width=100%` viet thang vao URL thi trinh duyet
     * doc `%` la mo dau ma hoa, `%25` moi la dau phan tram that. Sai cho nay
     * thi khung hinh tut ve 320px giua man den.
     */
    @Test
    fun `dau phan tram trong width phai duoc ma hoa`() {
        val url = CauHinhCamera(host = "192.168.0.105").urlTrangPhat("cam1")
        assertEquals(
            "http://192.168.0.105:1984/stream.html?src=cam1&width=100%25",
            url,
        )
    }

    @Test
    fun `ten luong co ky tu la thi duoc ma hoa`() {
        val url = CauHinhCamera(host = "192.168.0.105").urlTrangPhat("san vuon")
        assertTrue(url, url.contains("src=san%20vuon"))
    }

    @Test
    fun `dia chi api streams dung duong dan go2rtc`() {
        assertEquals(
            "http://192.168.0.105:1984/api/streams",
            CauHinhCamera(host = "192.168.0.105").urlDanhSach(),
        )
    }
}
