package vn.smarthome

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.smarthome.data.camera.CameraPlaybackApi
import vn.smarthome.data.camera.CameraRecorderState
import vn.smarthome.data.camera.CameraTopic
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.FaceGreetEvent
import vn.smarthome.data.camera.FaceGreetLogic

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaceGreetAndCameraPtzTest {

    @Test
    fun faceGreet_tuChoiTinCuQua30Giay() {
        val ev = FaceGreetEvent.parse(
            """{"name":"Trung","is_known":true,"greeting":"Chào Trung!","camera":"cua_chinh","confidence":0.82,"snapshot_url":"/api/face/snapshot/a.jpg","ts":1764250000}"""
        )
        assertNotNull(ev)
        val qd = FaceGreetLogic.quyetDinh(ev!!, nowEpochSec = 1764250045L, gioHienTai = 10)
        assertFalse(qd.hopLe)
        assertFalse(qd.hienPopup)
        assertNull(qd.docTts)
    }

    @Test
    fun faceGreet_nguoiQuenBanNgay_banner5sVaDocChao() {
        val ev = FaceGreetEvent.parse(
            """{"name":"Trung","is_known":true,"greeting":"Chào mừng về nhà, Trung!","camera":"cua_chinh","confidence":0.85,"snapshot_url":"/api/face/snapshot/a.jpg","ts":1764250000,"mode":"home","silent":false}"""
        )!!
        val qd = FaceGreetLogic.quyetDinh(ev, nowEpochSec = 1764250005L, gioHienTai = 18)
        assertTrue(qd.hopLe)
        assertTrue(qd.hienPopup)
        assertEquals(FaceGreetLogic.BANNER_QUEN_MS, qd.thoiLuongMs)
        assertFalse(qd.phatChuongTingTong)
        assertEquals("Chào mừng về nhà, Trung!", qd.docTts)
    }

    @Test
    fun faceGreet_nguoiLaBanNgay_tingTongVaPopup10s() {
        val ev = FaceGreetEvent.parse(
            """{"name":"unknown","is_known":false,"greeting":"Có khách ở cửa","camera":"cua_chinh","confidence":0.91,"snapshot_url":"/api/face/snapshot/la.jpg","ts":1764250000,"mode":"home","silent":false}"""
        )!!
        val qd = FaceGreetLogic.quyetDinh(ev, nowEpochSec = 1764250003L, gioHienTai = 14)
        assertTrue(qd.hopLe)
        assertTrue(qd.hienPopup)
        assertEquals(FaceGreetLogic.POPUP_LA_MS, qd.thoiLuongMs)
        assertTrue(qd.phatChuongTingTong)
        assertEquals("Có khách ở cửa", qd.docTts)
    }

    @Test
    fun faceGreet_gioNguHoacVangNha_imLangTrenTablet() {
        val ev = FaceGreetEvent.parse(
            """{"name":"unknown","is_known":false,"greeting":"Có khách ở cửa","camera":"cua_chinh","confidence":0.91,"snapshot_url":"/api/face/snapshot/la.jpg","ts":1764250000,"mode":"home","silent":false}"""
        )!!
        val qdDem = FaceGreetLogic.quyetDinh(ev, nowEpochSec = 1764250003L, gioHienTai = 23)
        assertTrue(qdDem.hopLe)
        assertFalse(qdDem.hienPopup)
        assertFalse(qdDem.phatChuongTingTong)
        assertNull(qdDem.docTts)

        val evAway = ev.copy(mode = "away", silent = true)
        val qdAway = FaceGreetLogic.quyetDinh(evAway, nowEpochSec = 1764250003L, gioHienTai = 10)
        assertFalse(qdAway.hienPopup)
        assertNull(qdAway.docTts)

        // Khi bat cong tac docChaoGioNgu: khung 22:00-06:00 van doc TTS va hien banner/popup (ke ca khi server gui mode=sleep, silent=true)
        val evNguoiQuenDem = FaceGreetEvent.parse(
            """{"name":"Trung","is_known":true,"greeting":"Chào mừng về nhà, Trung!","camera":"cam1","confidence":0.86,"snapshot_url":"/api/face/snapshot/a.jpg","ts":1764250000,"mode":"sleep","silent":true}"""
        )!!
        val qdBatDem = FaceGreetLogic.quyetDinh(
            evNguoiQuenDem,
            nowEpochSec = 1764250003L,
            gioHienTai = 23,
            gioBatDauNgu = 22,
            gioKetThucNgu = 6,
            docChaoGioNgu = true,
        )
        assertTrue(qdBatDem.hopLe)
        assertTrue(qdBatDem.hienPopup)
        assertEquals("Chào mừng về nhà, Trung!", qdBatDem.docTts)

        // Che do Vang nha (away) van giu im lang ke ca khi bat docChaoGioNgu
        val qdAwayStillSilent = FaceGreetLogic.quyetDinh(
            evAway,
            nowEpochSec = 1764250003L,
            gioHienTai = 23,
            docChaoGioNgu = true,
        )
        assertFalse(qdAwayStillSilent.hienPopup)
        assertNull(qdAwayStillSilent.docTts)
    }

    @Test
    fun privacyMode_chiAnCameraTrongNhaVaGiuCamNgoaiCua() {
        val st = CameraRecorderState.gop(
            CameraRecorderState(),
            CameraTopic.STATE,
            """{"recording_enabled":true,"detect_person_enabled":true,"privacy_mode":true,"home_mode":"away","indoor_cameras":["phong_khach","cau_thang"],"ptz_cameras":["cua_chinh","phong_khach"]}"""
        )
        assertTrue(st.privacyMode)
        assertEquals("away", st.homeMode)
        assertTrue(st.camBiAnRiengTu("phong_khach"))
        assertTrue(st.camBiAnRiengTu("phong_khach_sd"))
        assertFalse(st.camBiAnRiengTu("cua_chinh"))
    }

    @Test
    fun webViewUuTienLuongSdVaDocSuKienMiniNvr() {
        val cfg = CauHinhCamera(host = "192.168.0.50", cong = 1984, congGhiHinh = 1985)
        val allStreams = listOf("cua_chinh", "cua_chinh_sd", "san_truoc")
        assertEquals("cua_chinh_sd", cfg.tenLuongWebView("cua_chinh", allStreams))
        assertEquals("san_truoc", cfg.tenLuongWebView("san_truoc", allStreams))

        val ds = CameraPlaybackApi.phanTichDanhSachSuKien(
            JSONObject(
                """{"events":[{"id":12,"camera":"cua_chinh","kind":"face_unknown","start_ts":1764250000,"end_ts":1764250025,"max_conf":0.92,"person_count":1,"has_thumb":true,"has_clip":true,"label":"Người lạ"}]}"""
            )
        )
        assertEquals(1, ds.size)
        assertTrue(ds[0].laNguoiLa)
        assertTrue(ds[0].hasClip)
        assertEquals("Người lạ", ds[0].nhanHienThi())

        val people = CameraPlaybackApi.phanTichDanhSachNguoiQuen(
            JSONObject("""{"people":[{"name":"Trung","count":4,"sample_url":"/api/face/sample/Trung/a.jpg","updated_ts":1764250000}]}""")
        )
        assertEquals(1, people.size)
        assertEquals("Trung", people[0].name)
        assertEquals(4, people[0].count)

        val unknowns = CameraPlaybackApi.phanTichDanhSachNguoiLa(
            JSONObject("""{"items":[{"filename":"20261127_230005_cam1_unknown.jpg","url":"/api/face/snapshot/20261127_230005_cam1_unknown.jpg","ts":1764250005}]}""")
        )
        assertEquals(1, unknowns.size)
        assertEquals("20261127_230005_cam1_unknown.jpg", unknowns[0].filename)
    }
}
