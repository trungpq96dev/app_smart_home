package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CheDoBon
import vn.smarthome.data.bomnuoc.KenhBomNuoc
import vn.smarthome.data.bomnuoc.LoiBomNuoc
import vn.smarthome.data.bomnuoc.LuongNuocThang
import vn.smarthome.data.bomnuoc.NhatKyBomNuoc
import vn.smarthome.data.bomnuoc.NhomNhatKy
import vn.smarthome.data.bomnuoc.QuanLyBomNuoc
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.bomnuoc.coTheDieuKhienBom
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class BomNuocStateTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun parse_goi_state_day_du_cua_firmware() {
        val raw = """
            {
              "up": 3725, "fw": "6.261111.2", "hb": 30,
              "rssi": -58, "ssid": "Nha_5G", "ip": "192.168.0.120",
              "aboveLow": 0, "isFull": 0,
              "lvl": 72.4, "litres": 1086.0, "lvlRaw": 13400, "lvlE": 2600, "lvlF": 17500,
              "tankOn": 1, "tankMode": "auto", "tankRun": 145, "fTank": 18.6,
              "boostOn": 0, "boostRun": 0, "bstMode": "off", "bstLock": 0,
              "fCity": 4.2, "useC": 320.0, "cityPulses": 450.0,
              "den": 1, "locked": 0, "fault": "", "temp": 27.5,
              "dryCheck": 45, "minRest": 2.0, "maxRun": 90, "pulses": 450.0, "minFlow": 1.0,
              "calPT": 900, "calLT": 2.0,
              "useM": "2026-11", "useT": 1450.0,
              "temps": [
                {"n": "Nhiệt độ nước", "v": 27.5},
                {"n": "Bơm lên bồn", "v": 41.2},
                {"n": "Bơm áp", "v": -127.0}
              ],
              "io": [
                {"n": "Còi", "g": 5, "v": 1},
                {"n": "Bơm", "g": 19, "v": 1}
              ]
            }
        """.trimIndent()

        val s = BomNuocState.tuJson(raw)
        assertNotNull(s)
        s!!
        assertEquals("6.261111.2", s.fw)
        assertEquals(72.4, s.mucNuocPhanTram!!, 0.01)
        assertEquals(1086.0, s.mucNuocLit!!, 0.01)
        assertEquals(27.5, s.nhietDoNuoc!!, 0.01)
        assertEquals(41.2, s.nhietDoBomBon!!, 0.01)
        assertNull(s.nhietDoBomAp) // -127 la mat cam bien
        assertEquals(-58, s.wifiRssi)
        assertTrue(s.bomChinhDangChay)
        assertFalse(s.bomApDangChay)
        assertTrue(s.coNhanhNuocMay)
        assertTrue(s.nuocMayDangChay)
        assertTrue(s.coDenPhu)
        assertTrue(s.denPhuBat)
        assertEquals(true, s.coiDangKeu)
        assertEquals(CheDoBon.AUTO, s.cheDoBon)
        assertFalse(s.coLoi)
    }

    @Test
    fun loc_gia_tri_sentinel_khong_bia_so() {
        val raw = """
            {
              "rssi": 0,
              "lvl": -1,
              "litres": -1,
              "temp": -127,
              "isFull": 1,
              "tankOn": 0,
              "tankMode": "off"
            }
        """.trimIndent()

        val s = BomNuocState.tuJson(raw)!!
        assertNull(s.wifiRssi)
        assertFalse(s.coCamBienMuc)
        assertNull(s.mucNuocPhanTram)
        assertNull(s.mucNuocLit)
        assertNull(s.nhietDoNuoc)
        assertFalse(s.coNhanhNuocMay)
        assertFalse(s.coDenPhu)
        assertNull(s.coiDangKeu)
        assertTrue(s.bonDay)
    }

    @Test
    fun parse_luong_nuoc_theo_thang() {
        val raw = """
            [
              {"m": "2026-11", "t": 1450.0, "c": 320.0},
              {"m": "chua ro thang", "t": 200.0}
            ]
        """.trimIndent()
        val list = LuongNuocThang.tuJsonArray(raw)
        assertNotNull(list)
        assertEquals(2, list!!.size)
        assertEquals("T11", list[0].nhanNgan)
        assertEquals(1770.0, list[0].tongLit, 0.01)
        assertTrue(list[1].laThangChuaRo)
        assertEquals("?", list[1].nhanNgan)
        assertNull(list[1].nuocMayLit)
    }

    @Test
    fun dich_loi_khong_dau_va_giu_nguyen_tham_so_so() {
        val l1 = LoiBomNuoc.phanTich("Bom len bon chay qua 90 phut ma chua day")
        assertNotNull(l1)
        assertTrue(l1!!.noiDung.contains("90 phút"))
        assertEquals(LoiBomNuoc.MucDo.NGUY_HIEM, l1.mucDo)

        val l2 = LoiBomNuoc.phanTich("Bom ap qua nong (72.5 do C)")
        assertNotNull(l2)
        assertTrue(l2!!.noiDung.contains("72.5 °C"))

        val l3 = LoiBomNuoc.phanTich("Loi phan cung la chua biet")
        assertNotNull(l3)
        assertEquals("Loi phan cung la chua biet", l3!!.noiDung)

        assertNull(LoiBomNuoc.phanTich("   "))
    }

    @Test
    fun nhat_ky_bo_qua_lan_dau_va_luu_khi_doi_trang_thai() = runBlocking {
        val file = File(tempFolder.root, "nk.json")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val nk = NhatKyBomNuoc(file, scope)
        val bayGio = System.currentTimeMillis()

        val s0 = BomNuocState(tankOn = 0, tankMode = "auto", boostOn = 0, isFull = 0, fault = "")
        nk.quanSat(s0, luc = bayGio - 20_000L)
        assertTrue(nk.danhSach.value.isEmpty())

        // Bom bat dau chay + co loi
        val s1 = s0.copy(tankOn = 1, fault = "Bom len bon qua nong (71 do C)")
        nk.quanSat(s1, luc = bayGio - 10_000L)
        assertEquals(2, nk.danhSach.value.size)
        assertTrue(nk.danhSach.value.any { it.nhom == NhomNhatKy.BOM && it.tieuDe.contains("bắt đầu chạy") })
        assertTrue(nk.danhSach.value.any { it.nhom == NhomNhatKy.CANH_BAO && it.tieuDe.contains("71 °C") })

        // Doc lai tu file moi
        val nk2 = NhatKyBomNuoc(file, scope)
        assertEquals(2, nk2.danhSach.value.size)

        nk2.xoaTatCa()
        assertTrue(nk2.danhSach.value.isEmpty())
        assertFalse(file.exists())
    }

    @Test
    fun kiem_tra_va_lam_tron_tham_so_bao_ve() {
        assertNull(QuanLyBomNuoc.kiemTraBaoVe(45, 1.0, 90, 2.0, 450.0))
        assertNotNull(QuanLyBomNuoc.kiemTraBaoVe(2, 1.0, 90, 2.0, 450.0))
        assertNotNull(QuanLyBomNuoc.kiemTraBaoVe(45, 0.01, 90, 2.0, 450.0))

        val r = QuanLyBomNuoc.lamTronBaoVe(45, 2.36, 90, 450.14, 1.256)
        assertEquals(2.4, r.minRest, 0.001)
        assertEquals(450.1, r.pulses, 0.001)
        assertEquals(1.26, r.minFlow, 0.0001)
        assertEquals("cfgDry=45&cfgRest=2.4&cfgRun=90&cfgPulses=450.1&cfgMinFlow=1.26", r.thanhQuery())
    }

    @Test
    fun co_the_dieu_khien_bom_chi_true_khi_truc_tuyen_va_co_state() {
        val st = BomNuocState(tankOn = 0, tankMode = "auto")
        val lan = TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.LAN, 0)
        val mqtt = TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.MQTT, 3)

        assertTrue(lan.trucTuyen)
        assertTrue(mqtt.trucTuyen)
        assertTrue(coTheDieuKhienBom(st, lan))
        assertTrue(coTheDieuKhienBom(st, mqtt))

        assertFalse(coTheDieuKhienBom(null, lan))
        assertFalse(coTheDieuKhienBom(null, mqtt))

        val offlineStates = listOf(
            TrangThaiKetNoiBom.ChuaCauHinh,
            TrangThaiKetNoiBom.DangNoi,
            TrangThaiKetNoiBom.ChiCoBroker,
            TrangThaiKetNoiBom.SoCu(45),
            TrangThaiKetNoiBom.BoOffline,
            TrangThaiKetNoiBom.MatBroker,
            TrangThaiKetNoiBom.SaiTaiKhoan,
            TrangThaiKetNoiBom.Loi("timeout"),
        )
        offlineStates.forEach { link ->
            assertFalse(link.trucTuyen)
            assertFalse(coTheDieuKhienBom(st, link))
        }
    }
}
