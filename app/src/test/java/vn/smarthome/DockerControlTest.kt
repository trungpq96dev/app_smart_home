package vn.smarthome

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import vn.smarthome.data.server.DockerAction
import vn.smarthome.data.server.DockerActionOutcome
import vn.smarthome.data.server.DockerControlClient
import vn.smarthome.data.server.ServerState
import vn.smarthome.data.server.ServerTopic
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DockerControlTest {

    @Test
    fun chan_cung_container_loi_va_ten_khong_hop_le() {
        for (core in listOf("mosquitto", "server-collector", "smarthome-web", "homeserver-mosquitto-1")) {
            assertTrue("$core must be protected", DockerControlClient.laContainerLoi(core))
            assertNotNull(DockerControlClient.kiemTraDauVao(core, DockerAction.STOP))
        }
        assertNotNull(DockerControlClient.kiemTraDauVao("../images", DockerAction.STOP))
        assertNull(DockerControlClient.kiemTraDauVao("camera-recorder", DockerAction.STOP))
        assertEquals("http://192.168.0.50:3080", DockerControlClient.gatewayBaseUrl("192.168.0.50"))
    }

    @Test
    fun parse_cpu_ram_va_co_bao_ve_tu_mqtt_server_docker() {
        val payload = """
            [
              {
                "name": "mosquitto",
                "image": "eclipse-mosquitto:2",
                "state": "running",
                "status": "Up 3 days",
                "cpu_percent": 0.4,
                "mem_mb": 4.2,
                "protected": false,
                "controllable": true
              },
              {
                "name": "camera-recorder",
                "image": "smarthome-camera-recorder:1.0",
                "state": "running",
                "status": "Up 2 hours",
                "cpu_percent": 31.5,
                "mem_mb": 442.8,
                "mem_limit_mb": 1024.0,
                "mem_percent": 43.2,
                "protected": false,
                "controllable": true
              }
            ]
        """.trimIndent()

        val state = ServerState.gop(ServerState(), ServerTopic.DOCKER, payload)
        assertTrue(state.isRealData)
        assertEquals(2, state.containers.size)

        val mosq = state.containers[0]
        assertEquals("mosquitto", mosq.name)
        assertTrue(mosq.isProtected)
        assertFalse(mosq.controllable)
        assertEquals(0.4f, mosq.cpuPercent!!, 0.01f)
        assertEquals(4.2f, mosq.memMb!!, 0.01f)

        val cam = state.containers[1]
        assertEquals("camera-recorder", cam.name)
        assertFalse(cam.isProtected)
        assertTrue(cam.controllable)
        assertEquals(31.5f, cam.cpuPercent!!, 0.01f)
        assertEquals(442.8f, cam.memMb!!, 0.01f)
        assertEquals(1024.0f, cam.memLimitMb!!, 0.01f)
    }

    @Test
    fun docker_control_client_goi_gateway_pin_va_cap_nhat_container() = runBlocking {
        val serverSocket = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        val worker = thread(isDaemon = true) {
            while (!serverSocket.isClosed) {
                val sock = try {
                    serverSocket.accept()
                } catch (_: Exception) {
                    break
                }
                sock.use { s ->
                    val reader = BufferedReader(InputStreamReader(s.getInputStream(), Charsets.UTF_8))
                    val reqLine = reader.readLine() ?: return@use
                    val parts = reqLine.split(" ")
                    val path = parts.getOrNull(1) ?: "/"
                    val headers = mutableMapOf<String, String>()
                    var contentLength = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) break
                        val idx = line.indexOf(':')
                        if (idx > 0) {
                            val k = line.substring(0, idx).trim().lowercase()
                            val v = line.substring(idx + 1).trim()
                            headers[k] = v
                            if (k == "content-length") contentLength = v.toIntOrNull() ?: 0
                        }
                    }
                    val body = if (contentLength > 0) {
                        val buf = CharArray(contentLength)
                        var read = 0
                        while (read < contentLength) {
                            val n = reader.read(buf, read, contentLength - read)
                            if (n <= 0) break
                            read += n
                        }
                        String(buf, 0, read)
                    } else {
                        ""
                    }

                    val (statusLine, respJson) = when (path) {
                        "/api/server/docker/status" ->
                            "200 OK" to """{"ok":true,"configured":true,"enabled":true,"protectedContainers":["mosquitto","server-collector","smarthome-web"],"allowedContainers":["camera-recorder","beszel"]}"""
                        "/api/admin/verify" ->
                            if (body.contains("1234")) {
                                "200 OK" to """{"ok":true,"token":"tok-abc"}"""
                            } else {
                                "401 Unauthorized" to """{"ok":false,"error":"Mã PIN không đúng."}"""
                            }
                        "/api/server/docker/action" ->
                            if (headers["x-admin-token"] != "tok-abc") {
                                "401 Unauthorized" to """{"ok":false,"requirePin":true,"error":"Cần mở khóa PIN kỹ thuật (4 số)."}"""
                            } else {
                                "200 OK" to """{"ok":true,"message":"Container 'camera-recorder' đã dừng.","containers":[{"name":"camera-recorder","image":"smarthome-camera-recorder:1.0","state":"exited","status":"Exited (0)","cpu_percent":null,"mem_mb":null,"protected":false,"controllable":true}]}"""
                            }
                        else -> "404 Not Found" to """{"ok":false,"error":"Not found"}"""
                    }
                    val bytes = respJson.toByteArray(Charsets.UTF_8)
                    val out = s.getOutputStream()
                    out.write(
                        ("HTTP/1.1 $statusLine\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n")
                            .toByteArray(Charsets.UTF_8),
                    )
                    out.write(bytes)
                    out.flush()
                }
            }
        }

        try {
            val baseUrl = "http://127.0.0.1:${serverSocket.localPort}"
            val client = DockerControlClient()

            val st = client.layTrangThai(baseUrl)
            assertTrue(st.enabled)
            assertTrue("camera-recorder" in st.allowedContainers)

            // Chua co token -> RequirePin
            val needPin = client.thucThiLenh(baseUrl, "camera-recorder", DockerAction.STOP, null)
            assertTrue(needPin is DockerActionOutcome.RequirePin)

            // Xac thuc PIN sai -> loi
            val (badTok, badErr) = client.xacThucPinGateway(baseUrl, "9999")
            assertNull(badTok)
            assertNotNull(badErr)

            // Xac thuc PIN dung -> co token va dung container thanh cong
            val (tok, err) = client.xacThucPinGateway(baseUrl, "1234")
            assertNull(err)
            assertEquals("tok-abc", tok)

            val ok = client.thucThiLenh(baseUrl, "camera-recorder", DockerAction.STOP, tok)
            assertTrue(ok is DockerActionOutcome.Ok)
            val okRes = ok as DockerActionOutcome.Ok
            assertEquals(1, okRes.containers?.size)
            assertEquals("exited", okRes.containers?.first()?.state)

            // Container loi bi chan ngay phia client
            val blocked = client.thucThiLenh(baseUrl, "mosquitto", DockerAction.STOP, tok)
            assertTrue(blocked is DockerActionOutcome.Error)
        } finally {
            serverSocket.close()
            worker.join(1000)
        }
    }
}
