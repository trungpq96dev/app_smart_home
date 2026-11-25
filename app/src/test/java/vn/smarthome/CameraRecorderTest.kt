package vn.smarthome

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import vn.smarthome.data.camera.CameraPlaybackApi
import vn.smarthome.data.camera.CameraRecorderState
import vn.smarthome.data.camera.CameraTopic
import vn.smarthome.data.camera.CauHinhCamera

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CameraRecorderTest {

    @Test
    fun gop_mqtt_camera_state_va_person_khong_bia_so() {
        var st = CameraRecorderState()
        assertNull(st.online)
        assertFalse(st.camDangGhi("cua_chinh"))

        st = CameraRecorderState.gop(st, CameraTopic.TRANG_THAI, "online")
        assertEquals(true, st.online)

        val payloadState = """
            {
              "recording_enabled": true,
              "detect_person_enabled": true,
              "telegram_enabled": true,
              "drive_enabled": true,
              "api_port": 1985,
              "cache_used_gb": 3.45,
              "cache_max_gb": 10.0,
              "pending_uploads": 1,
              "uploaded_today": 52,
              "person_today": 4,
              "cameras": {
                "cua_chinh": {"recording": true, "last_segment_ts": 1764050000, "error": null}
              },
              "tra_loi_id": "cmd_99"
            }
        """.trimIndent()
        st = CameraRecorderState.gop(st, CameraTopic.STATE, payloadState)
        assertTrue(st.camDangGhi("cua_chinh"))
        assertEquals(1985, st.apiPort)
        assertEquals(52, st.uploadedToday)
        assertEquals(4, st.personToday)
        assertEquals("cmd_99", st.traLoiId)

        val payloadPerson = """
            {
              "event_id": 18,
              "camera": "cua_chinh",
              "start_ts": 1764051234,
              "thoi_gian": "2026-11-25 13:53:54",
              "person_count": 2,
              "confidence": 0.91
            }
        """.trimIndent()
        st = CameraRecorderState.gop(st, CameraTopic.PERSON, payloadPerson)
        assertNotNull(st.nguoiGanNhat)
        assertEquals(18, st.nguoiGanNhat?.eventId)
        assertEquals(2, st.nguoiGanNhat?.soNguoi)

        st = CameraRecorderState.gop(st, CameraTopic.TRANG_THAI, "offline")
        assertFalse(st.dangSong)
        assertFalse(st.camDangGhi("cua_chinh"))
    }

    @Test
    fun dong_thoi_gian_tim_vi_tri_tua_va_nhay_su_kien_nguoi() {
        val json = JSONObject(
            """
            {
              "camera": "cua_chinh",
              "date": "2026-11-25",
              "day_start_ts": 1000,
              "day_end_ts": 87400,
              "segments": [
                {"id": 1, "start_ts": 1000, "end_ts": 1600, "duration_sec": 600, "size_bytes": 10485760, "tier": "drive"},
                {"id": 2, "start_ts": 1605, "end_ts": 2205, "duration_sec": 600, "size_bytes": 10485760, "tier": "hot"}
              ],
              "events": [
                {"id": 10, "kind": "person", "start_ts": 1200, "end_ts": 1245, "max_conf": 0.86, "person_count": 1, "has_thumb": true},
                {"id": 11, "kind": "snapshot", "start_ts": 1500, "end_ts": 1501, "max_conf": 1.0, "person_count": 0, "has_thumb": true},
                {"id": 12, "kind": "person", "start_ts": 1800, "end_ts": 1830, "max_conf": 0.92, "person_count": 2, "has_thumb": true}
              ]
            }
            """.trimIndent()
        )
        val tl = CameraPlaybackApi.phanTichDongThoiGian(json)
        assertEquals(2, tl.segments.size)
        assertEquals(2, tl.suKienNguoi.size)

        // Tua vao giua doan 1 (moc 1250s -> offset 250_000 ms)
        val v1 = tl.timViTriPhat(1250L)
        assertNotNull(v1)
        assertEquals(1, v1!!.segment.id)
        assertEquals(250_000L, v1.offsetMs)
        assertFalse(v1.segment.laCucBo)

        // Tua trung khoang ho 5 giay giua doan 1 va doan 2 (moc 1602s) -> nhay sang dau doan 2
        val vGap = tl.timViTriPhat(1602L)
        assertNotNull(vGap)
        assertEquals(2, vGap!!.segment.id)
        assertEquals(0L, vGap.offsetMs)
        assertTrue(vGap.segment.laCucBo)

        // Bam vao su kien nguoi #10 (start=1200) -> lui truoc 3s = 1197s
        assertEquals(1197L, tl.mocNhaySuKien(tl.suKienNguoi[0]))
        assertEquals(12, tl.suKienNguoiTiep(1250L)?.id)
        assertEquals(10, tl.suKienNguoiTruoc(1800L)?.id)
        assertEquals(2, tl.doanTiepTheo(1)?.id)
        assertNull(tl.doanTiepTheo(2))
    }

    @Test
    fun cau_hinh_camera_sinh_url_playback_dung_cong() {
        val c = CauHinhCamera(host = "192.168.0.50", cong = 1984, congGhiHinh = 1985)
        assertEquals("http://192.168.0.50:1985/api/video/42", c.urlVideoSegment(42))
        assertEquals("http://192.168.0.50:1985/api/thumb/7.jpg", c.urlThumbnail(7))
        assertEquals("http://192.168.0.50:1999/api/video/42", c.urlVideoSegment(42, congOverride = 1999))
    }
}
