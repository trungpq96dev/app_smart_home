package vn.smarthome.data.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

enum class DockerAction(val apiValue: String, val labelVi: String) {
    START("start", "Bật"),
    STOP("stop", "Dừng"),
    RESTART("restart", "Khởi động lại"),
}

data class DockerControlStatus(
    val checked: Boolean = false,
    val configured: Boolean = false,
    val enabled: Boolean = false,
    val protectedContainers: Set<String> = CORE_PROTECTED_NAMES,
    val allowedContainers: Set<String> = emptySet(),
    val error: String? = null,
) {
    companion object {
        val CORE_PROTECTED_NAMES = setOf("mosquitto", "server-collector", "smarthome-web")
    }
}

sealed class DockerActionOutcome {
    data class Ok(
        val message: String,
        val containers: List<ServerContainer>?,
    ) : DockerActionOutcome()

    data class RequirePin(val error: String) : DockerActionOutcome()
    data class Error(val message: String) : DockerActionOutcome()
}

/**
 * Client dieu khien Docker an toan cho app Android:
 *  - Goi qua SmartHome Web Gateway tren LAN (`http://<serverHost>:3080/api/server/docker/...`)
 *  - Xac thuc bang PIN Admin 4 so (`/api/admin/verify` -> header `x-admin-token`)
 *  - KHONG bao gio ghi MQTT va KHONG luu `DOCKER_CONTROL_TOKEN` tren may tinh bang
 *  - Chan cung 3 container loi (mosquitto, server-collector, smarthome-web) ngay ca phia client
 */
class DockerControlClient {

    companion object {
        private val NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9_.-]{0,127}$")
        private val CORE_SUBSTRINGS = listOf(
            "mosquitto",
            "server-collector",
            "server_collector",
            "smarthome-web",
            "smarthome_web",
        )

        fun laContainerLoi(name: String): Boolean {
            val low = name.trim().trimStart('/').lowercase()
            if (low.isEmpty()) return true
            return CORE_SUBSTRINGS.any { it in low }
        }

        fun kiemTraDauVao(name: String, action: DockerAction): String? {
            val clean = name.trim().trimStart('/')
            if (clean.isEmpty() || !NAME_REGEX.matches(clean)) {
                return "Tên container không hợp lệ."
            }
            if (laContainerLoi(clean)) {
                return "Container lõi \"$clean\" nằm trong danh sách bảo vệ cứng, không được phép điều khiển từ xa."
            }
            // action la enum hop le (start/stop/restart)
            return if (action.apiValue in setOf("start", "stop", "restart")) null else "Hành động không hợp lệ."
        }

        fun gatewayBaseUrl(serverHost: String, webPort: Int = 3080): String {
            val cleanHost = serverHost
                .trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
                .ifBlank { "192.168.0.50" }
            return "http://$cleanHost:$webPort"
        }
    }

    private data class HttpResp(val code: Int, val body: String)

    private fun request(
        urlStr: String,
        method: String,
        bodyJson: String? = null,
        adminToken: String? = null,
        connectTimeoutMs: Int = 5_000,
        readTimeoutMs: Int = 15_000,
    ): HttpResp {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("Accept", "application/json")
            if (!adminToken.isNullOrBlank()) {
                setRequestProperty("x-admin-token", adminToken)
            }
            if (bodyJson != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (bodyJson != null) {
                conn.outputStream.use { it.write(bodyJson.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = if (stream != null) {
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
            } else {
                ""
            }
            HttpResp(code, text)
        } finally {
            conn.disconnect()
        }
    }

    suspend fun layTrangThai(baseUrl: String): DockerControlStatus = withContext(Dispatchers.IO) {
        try {
            val resp = request("$baseUrl/api/server/docker/status", "GET", readTimeoutMs = 6_000)
            val o = runCatching { JSONObject(resp.body) }.getOrNull()
            if (resp.code == 200 && o != null) {
                val prot = mutableSetOf<String>().apply {
                    addAll(DockerControlStatus.CORE_PROTECTED_NAMES)
                    val arr = o.optJSONArray("protectedContainers")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            arr.optString(i).trim().lowercase().takeIf { it.isNotEmpty() }?.let { add(it) }
                        }
                    }
                }
                val allowed = mutableSetOf<String>().apply {
                    val arr = o.optJSONArray("allowedContainers")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            arr.optString(i).trim().lowercase().takeIf { it.isNotEmpty() }?.let { add(it) }
                        }
                    }
                }
                DockerControlStatus(
                    checked = true,
                    configured = o.optBoolean("configured", false),
                    enabled = o.optBoolean("enabled", false),
                    protectedContainers = prot,
                    allowedContainers = allowed,
                    error = o.optString("error").takeIf { it.isNotBlank() },
                )
            } else {
                val msg = o?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Gateway trả lỗi HTTP ${resp.code}."
                DockerControlStatus(checked = true, enabled = false, error = msg)
            }
        } catch (e: Exception) {
            DockerControlStatus(
                checked = true,
                enabled = false,
                error = "Không nối được SmartHome Gateway ($baseUrl): ${e.message ?: "lỗi mạng"}",
            )
        }
    }

    suspend fun taiDanhSachContainer(baseUrl: String): List<ServerContainer>? = withContext(Dispatchers.IO) {
        try {
            val resp = request("$baseUrl/api/server/docker/containers", "GET", readTimeoutMs = 8_000)
            if (resp.code != 200) return@withContext null
            val o = JSONObject(resp.body)
            if (!o.optBoolean("ok", false)) return@withContext null
            val arr: JSONArray = o.optJSONArray("containers") ?: return@withContext null
            ServerState.parseContainersJson(arr)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Xac thuc PIN 4 so voi Gateway (`/api/admin/verify`).
     * Tra ve Pair(token, null) khi dung, hoac Pair(null, thongBaoLoi) khi sai/loi mang.
     */
    suspend fun xacThucPinGateway(baseUrl: String, pin: String): Pair<String?, String?> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("pin", pin.trim()).toString()
                val resp = request("$baseUrl/api/admin/verify", "POST", bodyJson = body, readTimeoutMs = 6_000)
                val o = runCatching { JSONObject(resp.body) }.getOrNull()
                if (resp.code == 200 && o?.optBoolean("ok", false) == true) {
                    val token = o.optString("token").takeIf { it.isNotBlank() }
                    if (token != null) return@withContext token to null
                }
                val err = o?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "Mã PIN không đúng hoặc Gateway từ chối (HTTP ${resp.code})."
                null to err
            } catch (e: Exception) {
                null to "Không kết nối được Gateway ($baseUrl) để kiểm tra PIN."
            }
        }

    suspend fun khoaTokenGateway(baseUrl: String, token: String) {
        withContext(Dispatchers.IO) {
            runCatching {
                request("$baseUrl/api/admin/lock", "POST", bodyJson = "{}", adminToken = token, readTimeoutMs = 4_000)
            }
        }
    }

    suspend fun thucThiLenh(
        baseUrl: String,
        name: String,
        action: DockerAction,
        adminToken: String?,
    ): DockerActionOutcome = withContext(Dispatchers.IO) {
        val cleanName = name.trim().trimStart('/')
        kiemTraDauVao(cleanName, action)?.let { return@withContext DockerActionOutcome.Error(it) }

        try {
            val body = JSONObject()
                .put("name", cleanName)
                .put("action", action.apiValue)
                .toString()
            val resp = request(
                urlStr = "$baseUrl/api/server/docker/action",
                method = "POST",
                bodyJson = body,
                adminToken = adminToken,
                readTimeoutMs = 35_000,
            )
            val o = runCatching { JSONObject(resp.body) }.getOrNull()
            if (resp.code == 401 && (o?.optBoolean("requirePin", false) == true)) {
                return@withContext DockerActionOutcome.RequirePin(
                    o.optString("error").ifBlank { "Cần mở khóa PIN kỹ thuật (4 số)." },
                )
            }
            if (resp.code == 200 && o?.optBoolean("ok", false) == true) {
                val arr = o.optJSONArray("containers")
                val list = arr?.let { ServerState.parseContainersJson(it) }
                val msg = o.optString("message").ifBlank { "${action.labelVi} container $cleanName thành công." }
                return@withContext DockerActionOutcome.Ok(msg, list)
            }
            val err = o?.optString("error")?.takeIf { it.isNotBlank() }
                ?: o?.optString("message")?.takeIf { it.isNotBlank() }
                ?: "Không thể ${action.labelVi.lowercase()} container $cleanName (HTTP ${resp.code})."
            DockerActionOutcome.Error(err)
        } catch (e: Exception) {
            DockerActionOutcome.Error(
                "Không kết nối được Gateway ($baseUrl): ${e.message ?: "lỗi mạng"}",
            )
        }
    }
}
