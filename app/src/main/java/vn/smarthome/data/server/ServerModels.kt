package vn.smarthome.data.server

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector
import org.json.JSONArray
import org.json.JSONObject
import vn.smarthome.data.DemoData

/** Danh sach topic MQTT cua may chu serverubuntu. */
object ServerTopic {
    const val GOC = "server/#"
    const val INFO = "server/info"
    const val METRICS = "server/metrics"
    const val DOCKER = "server/docker"
    const val NHIET_DO = "server/nhietdo"
    const val DIEN_NANG = "server/diennang"
    const val DIEN_NANG_NGAY = "server/diennang_ngay"
    const val CAP_NHAT = "server/capnhat"
}

/** Thong tin tinh cua may chu (it doi). */
data class ServerInfo(
    val hostname: String = "serverubuntu",
    val os: String = "Ubuntu 24.04 LTS",
    val kernel: String = "6.8.0-generic",
    val cpuModel: String = "Intel Core i5",
    val cpuCores: Int = 4,
    val ramTotalGb: Float = 16f,
    val diskTotalGb: Float = 256f,
    val uptimeDays: Int = 3,
    val uptimeHours: Int = 7,
)

/** So do tuc thoi (CPU/RAM/o dia/nhiet do/mang). */
data class ServerMetric(
    val cpuPercent: Float = 18f,
    val ramUsedGb: Float = 5.4f,
    val ramPercent: Float = 34f,
    val diskUsedGb: Float = 96f,
    val diskPercent: Float = 38f,
    /** null = may khong co cam bien nhiet do; man hinh hien "—", khong bia so. */
    val cpuTempC: Float? = 46f,
    val load1: Float = 0.35f,
    val load5: Float = 0.42f,
    val load15: Float = 0.38f,
    val netRxMbps: Float = 1.8f,
    val netTxMbps: Float = 0.6f,
)

/** Mot container Docker dang co tren may chu. */
data class ServerContainer(
    val name: String,
    val image: String,
    /** "running" | "exited" | ... */
    val state: String,
    /** Vi du "Up 3 days". */
    val status: String,
    val icon: ImageVector,
)

/** Mot cam bien nhiet do. */
data class ServerTemp(
    val label: String,
    val tempC: Float,
)

/** Dien nang: pin + RAPL CPU. null = khong doc duoc (khong bia). */
data class ServerPower(
    val batVoltageV: Float? = 12.13f,
    val batCurrentA: Float? = 0f,
    val batPowerW: Float? = 0f,
    val batStatus: String? = "Full",
    val cpuPackageW: Float? = 4.2f,
    val cpuCoreW: Float? = 2.1f,
    val cpuUncoreW: Float? = 0f,
)

/** Dien nang CPU + khoang nhiet do trong mot ngay. */
data class ServerDay(
    val date: String,
    val wh: Float,
    val tempMin: Float?,
    val tempMax: Float?,
)

/** Trang thai toan bo man Server (ho tro ca du lieu mau va du lieu that tu MQTT). */
data class ServerState(
    val info: ServerInfo = DemoData.serverInfo(),
    val metric: ServerMetric = DemoData.serverMetric(),
    val containers: List<ServerContainer> = DemoData.serverContainers(),
    val temps: List<ServerTemp> = DemoData.serverTemps(),
    val tempMinToday: Float? = 37f,
    val tempMaxToday: Float? = 47f,
    val power: ServerPower = DemoData.serverPower(),
    val days: List<ServerDay> = DemoData.serverDays(),
    val capNhat: String? = null,
    val isRealData: Boolean = false,
) {
    companion object {
        fun gop(cur: ServerState, topic: String, payload: String): ServerState {
            if (payload.isBlank()) return cur
            return try {
                when (topic) {
                    ServerTopic.INFO -> {
                        val o = JSONObject(payload)
                        val inf = ServerInfo(
                            hostname = o.optString("hostname", cur.info.hostname),
                            os = o.optString("os", cur.info.os),
                            kernel = o.optString("kernel", cur.info.kernel),
                            cpuModel = o.optString("cpu_model", cur.info.cpuModel),
                            cpuCores = o.optInt("cpu_cores", cur.info.cpuCores),
                            ramTotalGb = o.optDouble("ram_total_gb", cur.info.ramTotalGb.toDouble()).toFloat(),
                            diskTotalGb = o.optDouble("disk_total_gb", cur.info.diskTotalGb.toDouble()).toFloat(),
                            uptimeDays = o.optInt("uptime_days", cur.info.uptimeDays),
                            uptimeHours = o.optInt("uptime_hours", cur.info.uptimeHours),
                        )
                        cur.copy(info = inf, isRealData = true)
                    }

                    ServerTopic.METRICS -> {
                        val o = JSONObject(payload)
                        // Nhiet do: chi nhan khi server gui ve; thieu/khong co -> null (khong bia).
                        val temp: Float? = if (o.has("cpu_temp_c") && !o.isNull("cpu_temp_c"))
                            o.optDouble("cpu_temp_c").toFloat() else null
                        val m = ServerMetric(
                            cpuPercent = o.optDouble("cpu_percent", cur.metric.cpuPercent.toDouble()).toFloat(),
                            ramUsedGb = o.optDouble("ram_used_gb", cur.metric.ramUsedGb.toDouble()).toFloat(),
                            ramPercent = o.optDouble("ram_percent", cur.metric.ramPercent.toDouble()).toFloat(),
                            diskUsedGb = o.optDouble("disk_used_gb", cur.metric.diskUsedGb.toDouble()).toFloat(),
                            diskPercent = o.optDouble("disk_percent", cur.metric.diskPercent.toDouble()).toFloat(),
                            cpuTempC = temp,
                            load1 = o.optDouble("load1", cur.metric.load1.toDouble()).toFloat(),
                            load5 = o.optDouble("load5", cur.metric.load5.toDouble()).toFloat(),
                            load15 = o.optDouble("load15", cur.metric.load15.toDouble()).toFloat(),
                            netRxMbps = o.optDouble("net_rx_mbps", cur.metric.netRxMbps.toDouble()).toFloat(),
                            netTxMbps = o.optDouble("net_tx_mbps", cur.metric.netTxMbps.toDouble()).toFloat(),
                        )
                        cur.copy(metric = m, isRealData = true)
                    }

                    ServerTopic.DOCKER -> {
                        val arr = JSONArray(payload)
                        val list = mutableListOf<ServerContainer>()
                        for (i in 0 until arr.length()) {
                            val c = arr.getJSONObject(i)
                            val name = c.optString("name", "container")
                            val image = c.optString("image", "")
                            list.add(
                                ServerContainer(
                                    name = name,
                                    image = image,
                                    state = c.optString("state", "running"),
                                    status = c.optString("status", ""),
                                    icon = doIcon(name, image),
                                )
                            )
                        }
                        cur.copy(containers = list, isRealData = true)
                    }

                    ServerTopic.NHIET_DO -> {
                        val o = JSONObject(payload)
                        val arr = o.optJSONArray("sensors") ?: JSONArray()
                        val list = mutableListOf<ServerTemp>()
                        for (i in 0 until arr.length()) {
                            val s = arr.getJSONObject(i)
                            list.add(
                                ServerTemp(
                                    label = s.optString("label", "cảm biến"),
                                    tempC = s.optDouble("temp_c", 0.0).toFloat(),
                                )
                            )
                        }
                        cur.copy(
                            temps = list,
                            tempMinToday = optFloat(o, "today_min"),
                            tempMaxToday = optFloat(o, "today_max"),
                            isRealData = true,
                        )
                    }

                    ServerTopic.DIEN_NANG -> {
                        val o = JSONObject(payload)
                        cur.copy(
                            power = ServerPower(
                                batVoltageV = optFloat(o, "bat_voltage_v"),
                                batCurrentA = optFloat(o, "bat_current_a"),
                                batPowerW = optFloat(o, "bat_power_w"),
                                batStatus = if (o.has("bat_status") && !o.isNull("bat_status"))
                                    o.optString("bat_status") else null,
                                cpuPackageW = optFloat(o, "cpu_package_w"),
                                cpuCoreW = optFloat(o, "cpu_core_w"),
                                cpuUncoreW = optFloat(o, "cpu_uncore_w"),
                            ),
                            isRealData = true,
                        )
                    }

                    ServerTopic.DIEN_NANG_NGAY -> {
                        val arr = JSONArray(payload)
                        val list = mutableListOf<ServerDay>()
                        for (i in 0 until arr.length()) {
                            val d = arr.getJSONObject(i)
                            list.add(
                                ServerDay(
                                    date = d.optString("date", ""),
                                    wh = d.optDouble("wh", 0.0).toFloat(),
                                    tempMin = optFloat(d, "temp_min"),
                                    tempMax = optFloat(d, "temp_max"),
                                )
                            )
                        }
                        cur.copy(days = list, isRealData = true)
                    }

                    ServerTopic.CAP_NHAT -> cur.copy(capNhat = payload.trim(), isRealData = true)
                    else -> cur
                }
            } catch (_: Exception) {
                cur
            }
        }

        /** Doc so thuc; null khi thieu hoac JSON null (khong bia so 0). */
        private fun optFloat(o: JSONObject, key: String): Float? =
            if (o.has(key) && !o.isNull(key)) o.optDouble(key).toFloat() else null

        private fun doIcon(name: String, image: String): ImageVector {
            val s = (name + " " + image).lowercase()
            return when {
                "mosquitto" in s || "mqtt" in s -> Icons.Filled.Hub
                "dien" in s || "evn" in s -> Icons.Filled.ElectricBolt
                "wifi" in s || "router" in s -> Icons.Filled.Router
                "go2rtc" in s || "cam" in s || "rtsp" in s -> Icons.Filled.Videocam
                "cockpit" in s -> Icons.Filled.Dns
                "db" in s || "sql" in s || "postgres" in s || "mysql" in s || "redis" in s -> Icons.Filled.Storage
                else -> Icons.Filled.Widgets
            }
        }
    }
}
