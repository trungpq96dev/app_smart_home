package vn.smarthome.data.wifi

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import vn.smarthome.data.DemoData

/** Danh sach topic MQTT WiFi. */
object WifiTopic {
    const val GOC = "wifi/#"
    const val INFO = "wifi/info"
    const val DEVICES = "wifi/devices"
    const val USAGE = "wifi/usage"
    const val TRANG_THAI = "wifi/trangthai"
    const val CAP_NHAT = "wifi/capnhat"
    const val CMD = "wifi/cmd"
}

/** Thong tin ket noi WiFi hien tai. */
data class WifiInfo(
    val ssid: String = "SmartHome_5G",
    val security: String = "WPA2/WPA3",
    val signalDbm: Int = -62,
    val signalLabel: String = "Tốt",
    val downloadMbps: Float = 56.4f,
    val uploadMbps: Float = 24.8f,
    val speedMbps: Int = 150,
    val ipRouter: String = "192.168.0.1",
    val dns: String = "8.8.8.8",
    val gateway: String = "192.168.0.1",
    val isConnected: Boolean = true,
    val uptimeDays: Int = 2,
    val uptimeHours: Int = 3,
)

/** Mot thiet bi dang ket noi WiFi. */
data class WifiDevice(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val ip: String,
    val isOnline: Boolean,
    val downloadMB: Float,
    val uploadMB: Float,
)

/** Trang thai toan bo man WiFi (ho tro ca du lieu mau va du lieu that tu MQTT). */
data class WifiState(
    val info: WifiInfo = DemoData.wifiInfo(),
    val devices: List<WifiDevice> = DemoData.wifiDevices(),
    val usageChart24h: List<Float> = DemoData.wifiUsage24h(),
    val trangThai: String = "ok",
    val capNhat: String? = null,
    val isRealData: Boolean = false,
) {
    companion object {
        fun gop(cur: WifiState, topic: String, payload: String): WifiState {
            if (payload.isBlank()) return cur
            return try {
                when (topic) {
                    WifiTopic.INFO -> {
                        val obj = JSONObject(payload)
                        val inf = WifiInfo(
                            ssid = obj.optString("ssid", cur.info.ssid),
                            security = obj.optString("security", cur.info.security),
                            signalDbm = obj.optInt("signal_dbm", cur.info.signalDbm),
                            signalLabel = obj.optString("signal_label", cur.info.signalLabel),
                            downloadMbps = obj.optDouble("download_mbps", cur.info.downloadMbps.toDouble()).toFloat(),
                            uploadMbps = obj.optDouble("upload_mbps", cur.info.uploadMbps.toDouble()).toFloat(),
                            speedMbps = obj.optInt("speed_mbps", cur.info.speedMbps),
                            ipRouter = obj.optString("router_ip", cur.info.ipRouter),
                            dns = obj.optString("dns", cur.info.dns),
                            gateway = obj.optString("gateway", cur.info.gateway),
                            isConnected = obj.optBoolean("is_connected", cur.info.isConnected),
                            uptimeDays = obj.optInt("uptime_days", cur.info.uptimeDays),
                            uptimeHours = obj.optInt("uptime_hours", cur.info.uptimeHours),
                        )
                        cur.copy(info = inf, isRealData = true)
                    }

                    WifiTopic.DEVICES -> {
                        val arr = JSONArray(payload)
                        val list = mutableListOf<WifiDevice>()
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            val name = o.optString("name", "Thiết bị")
                            list.add(
                                WifiDevice(
                                    id = o.optString("id", i.toString()),
                                    name = name,
                                    icon = doIcon(name),
                                    ip = o.optString("ip", ""),
                                    isOnline = o.optBoolean("online", true),
                                    downloadMB = o.optDouble("download_mb", 0.0).toFloat(),
                                    uploadMB = o.optDouble("upload_mb", 0.0).toFloat(),
                                )
                            )
                        }
                        cur.copy(devices = list, isRealData = true)
                    }

                    WifiTopic.USAGE -> {
                        val arr = JSONArray(payload)
                        val floats = mutableListOf<Float>()
                        for (i in 0 until arr.length()) {
                            floats.add(arr.optDouble(i, 0.0).toFloat())
                        }
                        cur.copy(usageChart24h = floats, isRealData = true)
                    }

                    WifiTopic.TRANG_THAI -> cur.copy(trangThai = payload.trim())
                    WifiTopic.CAP_NHAT -> cur.copy(capNhat = payload.trim(), isRealData = true)
                    else -> cur
                }
            } catch (_: Exception) {
                cur
            }
        }

        private fun doIcon(name: String): ImageVector {
            val lower = name.lowercase()
            return when {
                "phone" in lower || "iphone" in lower || "samsung" in lower || "xiaomi" in lower || "oppo" in lower -> Icons.Filled.PhoneAndroid
                "laptop" in lower || "pc" in lower || "desktop" in lower || "macbook" in lower || "thinkpad" in lower -> Icons.Filled.Laptop
                "tv" in lower || "tivi" in lower || "smart tv" in lower -> Icons.Filled.Tv
                "cam" in lower || "camera" in lower -> Icons.Filled.Videocam
                "loa" in lower || "speaker" in lower || "echo" in lower || "nest" in lower -> Icons.Filled.Speaker
                "đèn" in lower || "den" in lower || "light" in lower -> Icons.Filled.Lightbulb
                "tab" in lower || "ipad" in lower -> Icons.Filled.Tablet
                else -> Icons.Filled.Devices
            }
        }
    }
}
