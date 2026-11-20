package vn.smarthome.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Blinds
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Weekend
import vn.smarthome.data.server.ServerContainer
import vn.smarthome.data.server.ServerDay
import vn.smarthome.data.server.ServerInfo
import vn.smarthome.data.server.ServerMetric
import vn.smarthome.data.server.ServerPower
import vn.smarthome.data.server.ServerTemp
import vn.smarthome.data.wifi.WifiDevice
import vn.smarthome.data.wifi.WifiInfo
import vn.smarthome.ui.theme.SH
import kotlin.math.abs
import kotlin.random.Random

/**
 * ====================================================================
 *  TOAN BO SO LIEU MAU CUA APP NAM TRONG FILE NAY.
 * ====================================================================
 *
 * Day la KHUNG GIAO DIEN: chua noi MQTT / Home Assistant / camera that.
 * Moi con so o day deu do nguoi viet dat ra cho giao dien nhin duoc, KHONG
 * phai so do duoc tu thiet bi.
 *
 * Khi cam du lieu that vao, sua DUNG MOT CHO: thay cac ham khoi tao ben duoi
 * bang mot repository doc tu MQTT/HTTP roi do vao HomeViewModel. Phan giao
 * dien khong phai sua gi.
 *
 * Chip "DU LIEU MAU" tren header lay tu hang so IS_DEMO ngay duoi - tat no
 * khi da co du lieu that.
 *
 * TU 21/11/2026 CO NAY HEP NGHIA LAI. Nha da co hai cong tac THAT (bed1: den
 * Tuya + quat CozyLife, qua `congtac/#`), nen IS_DEMO khong con noi thay cho
 * cac thiet bi nua - no chi con noi ve SO LIEU TONG QUAN o day (nhiet do
 * trong nha, do am, kWh hien tai, bieu do Thong ke).
 *
 * Cau hoi "thiet bi nay that hay mau" hoi `CongTacState.laThat(phong,
 * thietBi)`, nguon la `congtac/info` tu bridge - KHONG hoi hang so nay, va
 * khong hard-code danh sach thiet bi that o bat ky dau trong app.
 */
object DemoData {

    const val IS_DEMO = true

    // ---- So lieu tong o Trang chu ----
    const val indoorTemp = "26.5"
    const val humidity = "68"
    const val powerNow = "1.25"
    const val safetyLabel = "An toàn"
    const val systemStatus = "Hệ thống hoạt động bình thường"

    // ---- Cac phong ----
    fun rooms(): List<RoomUi> = listOf(
        RoomUi(
            id = "living", name = "Phòng khách", icon = Icons.Filled.Weekend, accent = SH.Amber,
            lightOn = true, fanOn = false, acOn = true, acTemp = 26, mode = RoomMode.HOME,
            scheduleOn = true, scheduleLabel = "Tắt đèn lúc", scheduleTime = "23:30",
        ),
        RoomUi(
            id = "bed1", name = "Phòng ngủ 1", icon = Icons.Filled.Bed, accent = SH.Purple,
            lightOn = true, fanOn = false, acOn = true, acTemp = 25, mode = RoomMode.SLEEP,
            scheduleOn = true, scheduleLabel = "Tắt đèn lúc", scheduleTime = "23:00",
        ),
        RoomUi(
            id = "bed2", name = "Phòng ngủ 2", icon = Icons.Filled.Bed, accent = SH.Blue,
            lightOn = false, fanOn = true, acOn = false, acTemp = 27, mode = RoomMode.AWAY,
            scheduleOn = false, scheduleLabel = "Tắt đèn lúc", scheduleTime = "22:30",
        ),
        RoomUi(
            id = "bed3", name = "Phòng ngủ 3", icon = Icons.Filled.Bed, accent = SH.Teal,
            lightOn = false, fanOn = false, acOn = false, acTemp = 26, mode = RoomMode.AWAY,
            scheduleOn = false, scheduleLabel = "Tắt đèn lúc", scheduleTime = "22:00",
        ),
        RoomUi(
            id = "kitchen", name = "Nhà bếp", icon = Icons.Filled.Kitchen, accent = SH.Green,
            lightOn = true, fanOn = true, acOn = false, acTemp = 28, mode = RoomMode.HOME,
            scheduleOn = false, scheduleLabel = "Tắt đèn lúc", scheduleTime = "21:30",
        ),
    )

    // ---- O thao tac nhanh ----
    fun quickItems(): List<QuickItem> = listOf(
        QuickItem("light", "Đèn", Icons.Filled.Lightbulb, SH.Amber, true),
        QuickItem("fan", "Quạt", Icons.Filled.Air, SH.Blue, true),
        QuickItem("ac", "Điều hòa", Icons.Filled.AcUnit, SH.Purple, false),
        QuickItem("curtain", "Rèm", Icons.Filled.Blinds, SH.Teal, false),
    )

    // ---- Camera ----
    fun cameras(): List<CameraFeed> = listOf(
        CameraFeed("front", "Cửa chính", Icons.Filled.DoorFront, SH.Blue),
        CameraFeed("yard", "Sân vườn", Icons.Filled.Grass, SH.Green),
        CameraFeed("kitchen", "Nhà bếp", Icons.Filled.Kitchen, SH.Amber),
        CameraFeed("hall", "Hành lang", Icons.Filled.Stairs, SH.Purple),
    )

    /**
     * Sinh bo so lieu mau cho bieu do. Dung Random co seed co dinh theo
     * (nhom, khoang) nen moi lan mo app bieu do giong het nhau - khong nhay
     * lung tung moi lan ve lai.
     */
    fun series(kind: StatKind, range: StatRange): StatSeries {
        val n = range.days
        val rnd = Random(kind.ordinal * 100 + range.ordinal)
        val base: Float
        val spread: Float
        when (kind) {
            StatKind.POWER -> { base = 6.2f; spread = 3.4f }
            StatKind.TEMP -> { base = 27.5f; spread = 3.0f }
            StatKind.HUMID -> { base = 66f; spread = 12f }
            StatKind.WATER -> { base = 0.9f; spread = 0.5f }
            StatKind.GAS -> { base = 72f; spread = 10f }
        }
        val bars = List(n) { base + (rnd.nextFloat() - 0.45f) * spread }

        val labels = if (n <= 7) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").take(n)
        } else {
            List(n) { i -> if (i % (n / 7) == 0) "${i + 1}" else "" }
        }

        val sum = bars.sum()
        val avg = sum / n
        val delta = ((rnd.nextInt(26) - 14))

        return when (kind) {
            StatKind.POWER -> StatSeries(
                kind, fmt(sum), kind.unit, delta, bars, labels,
                "Công suất hiện tại", "1.25 kW", 0.42f,
                "Chi phí ước tính", vnd(sum * 2600),
                "Bình thường", true,
            )
            StatKind.TEMP -> StatSeries(
                kind, fmt(avg), kind.unit, delta, bars, labels,
                "Nhiệt độ hiện tại", "26.5 °C", 0.55f,
                "Cao nhất trong kỳ", fmt(bars.max()) + " °C",
                "Trong ngưỡng", true,
            )
            StatKind.HUMID -> StatSeries(
                kind, fmt(avg), kind.unit, delta, bars, labels,
                "Độ ẩm hiện tại", "68 %", 0.68f,
                "Thấp nhất trong kỳ", fmt(bars.min()) + " %",
                "Trong ngưỡng", true,
            )
            StatKind.WATER -> StatSeries(
                kind, fmt(sum), kind.unit, delta, bars, labels,
                "Mức bồn trên", "72 %", 0.72f,
                "Chi phí ước tính", vnd(sum * 11000),
                "Bơm nghỉ", true,
            )
            StatKind.GAS -> StatSeries(
                kind, fmt(bars.last()), kind.unit, delta, bars, labels,
                "Bình gas còn lại", fmt(bars.last()) + " %", bars.last() / 100f,
                "Dự kiến hết sau", "18 ngày",
                "Không rò rỉ", true,
            )
        }
    }

    private fun fmt(v: Float): String {
        val r = Math.round(v * 10) / 10.0
        return if (r == Math.floor(r)) r.toInt().toString() else r.toString()
    }

    /** 126000 -> "126.000 đ" (dau cham ngan cach hang nghin kieu Viet Nam). */
    private fun vnd(v: Float): String {
        val n = Math.round(abs(v) / 1000f) * 1000
        val s = n.toString().reversed().chunked(3).joinToString(".").reversed()
        return "$s đ"
    }

    // ---- WiFi ----
    fun wifiInfo(): WifiInfo = WifiInfo()

    fun wifiDevices(): List<WifiDevice> = listOf(
        WifiDevice("1", "DESKTOP-O71L6K8", Icons.Filled.Laptop, "192.168.0.105", true, 16.4f, 3.2f, mac="00:1A:2B:3C:4D:5E", brand="Dell", type="Máy tính", band="5GHz", isNew=false, trusted=true, signal="-41 dBm"),
        WifiDevice("2", "iPhone 15 Pro Max", Icons.Filled.PhoneAndroid, "192.168.0.102", true, 8.2f, 1.9f, mac="3C:00:F8:88:99:AA", brand="Apple", type="Điện thoại", band="5GHz", isNew=true, trusted=true, signal="-38 dBm"),
        WifiDevice("3", "Galaxy-Tab-A7-Lite", Icons.Filled.Tablet, "192.168.0.101", true, 4.1f, 0.6f, mac="B4:EF:39:11:22:08", brand="Samsung", type="Tablet", band="5GHz", isNew=false, trusted=true, signal="-48 dBm"),
        WifiDevice("4", "Smart TV Living Room", Icons.Filled.Tv, "192.168.0.150", true, 12.1f, 0.4f, mac="AA:88:CC:00:EE:FF", brand="LG", type="Smart TV", band="5GHz", isNew=false, trusted=true, signal="-45 dBm"),
        WifiDevice("5", "Dell XPS 15", Icons.Filled.Laptop, "192.168.0.123", true, 11.8f, 2.8f, mac="00:1A:28:3C:40:5E", brand="Dell", type="Laptop", band="5GHz", isNew=false, trusted=true, signal="-43 dBm"),
        WifiDevice("6", "Camera hành lang", Icons.Filled.Videocam, "192.168.0.118", true, 0.4f, 2.1f, mac="A4:C1:38:90:12:4F", brand="Xiaomi", type="IoT", band="2.4GHz", isNew=false, trusted=true, signal="-55 dBm"),
        WifiDevice("7", "MacBook Pro M3", Icons.Filled.Laptop, "192.168.0.112", true, 6.5f, 1.5f, mac="F0:18:98:4A:77:BC", brand="Apple", type="Laptop", band="5GHz", isNew=false, trusted=true, signal="-39 dBm"),
        WifiDevice("8", "Galaxy S24 Ultra", Icons.Filled.PhoneAndroid, "192.168.0.129", true, 3.9f, 0.8f, mac="88:36:6C:AB:21:09", brand="Samsung", type="Điện thoại", band="5GHz", isNew=true, trusted=false, signal="-46 dBm"),
        WifiDevice("9", "TP-Link Smart Plug", Icons.Filled.Lightbulb, "192.168.0.145", true, 0.08f, 0.02f, mac="98:DA:C4:6B:09:11", brand="TP-Link", type="IoT", band="2.4GHz", isNew=false, trusted=false, signal="-52 dBm"),
        WifiDevice("10", "iPad Air", Icons.Filled.Tablet, "192.168.0.131", false, 1.9f, 0.2f, mac="D8:BB:2C:7E:41:90", brand="Apple", type="Tablet", band="5GHz", isNew=false, trusted=true, signal="--"),
        WifiDevice("11", "Xiaomi Air Purifier", Icons.Filled.Lightbulb, "192.168.0.142", false, 0.1f, 0.05f, mac="54:48:E6:1C:33:8A", brand="Xiaomi", type="IoT", band="2.4GHz", isNew=false, trusted=false, signal="--"),
        WifiDevice("12", "Loa AI Nest Hub", Icons.Filled.Speaker, "192.168.0.148", false, 0.3f, 0.1f, mac="74:DA:38:55:19:C2", brand="Google", type="IoT", band="2.4GHz", isNew=false, trusted=false, signal="--"),
    )

    /** 24 gia tri mo phong luu luong mang theo gio (MB). */
    fun wifiUsage24h(): List<Float> = listOf(
            1.2f, 0.8f, 0.4f, 0.3f, 0.2f, 0.5f,   // 00-05h: dem, it dung
            2.1f, 5.4f, 8.2f, 7.6f, 6.3f, 5.8f,     // 06-11h: sang, tang dan
            4.2f, 3.9f, 5.1f, 6.7f, 7.8f, 9.2f,     // 12-17h: chieu
            10.5f, 11.2f, 9.8f, 7.4f, 4.6f, 2.3f,   // 18-23h: toi, dinh roi giam
    )

    // ---- WiFi: Thiết bị kết nối (mở rộng) ----

    fun wifiUnknownDevices(): List<vn.smarthome.data.wifi.UnknownDevice> = listOf(
        vn.smarthome.data.wifi.UnknownDevice("unk-1", "Thiết bị không xác định (Android)", "192.168.0.188", "4E:21:9B:C0:77:1A", "21:32 - Hôm nay", "Cố gắng kết nối lần đầu"),
        vn.smarthome.data.wifi.UnknownDevice("unk-2", "Thiết bị lạ (ESP-IoT)", "192.168.0.194", "CC:50:E3:8F:14:2D", "19:45 - Hôm nay", "Yêu cầu cấp IP DHCP mới"),
    )

    fun wifiConnectionLogs(): List<vn.smarthome.data.wifi.ConnectionLog> = listOf(
        vn.smarthome.data.wifi.ConnectionLog("21:32", "19/11", "4E:21:9B:C0:77:1A", "Phát hiện thiết bị lạ kết nối", "warning"),
        vn.smarthome.data.wifi.ConnectionLog("20:18", "19/11", "iPhone 15 Pro Max", "Đã kết nối vào băng tần 5GHz", "success"),
        vn.smarthome.data.wifi.ConnectionLog("19:45", "19/11", "CC:50:E3:8F:14:2D", "Yêu cầu kết nối bị tạm giữ", "warning"),
        vn.smarthome.data.wifi.ConnectionLog("18:10", "19/11", "Smart TV Living Room", "Đã kết nối thành công", "success"),
        vn.smarthome.data.wifi.ConnectionLog("16:05", "19/11", "iPad Air", "Ngắt kết nối WiFi", "info"),
        vn.smarthome.data.wifi.ConnectionLog("14:20", "19/11", "Hệ thống tường lửa", "Đã chặn 3 gói tin quét cổng bất thường", "danger"),
    )

    fun wifiSecuritySettings(): List<vn.smarthome.data.wifi.SecuritySetting> = listOf(
        vn.smarthome.data.wifi.SecuritySetting("firewall", "Tường lửa (Firewall)", "Lọc lưu lượng độc hại và bảo vệ mạng nội bộ", true),
        vn.smarthome.data.wifi.SecuritySetting("ddos", "Chống tấn công DDoS", "Tự động giới hạn kết nối bất thường từ bên ngoài", true),
        vn.smarthome.data.wifi.SecuritySetting("hideSsid", "Ẩn tên WiFi (SSID)", "Không hiển thị tên WiFi trên danh sách dò tìm", false),
        vn.smarthome.data.wifi.SecuritySetting("macFilter", "Lọc địa chỉ MAC", "Chỉ cho phép thiết bị trong danh sách tin cậy kết nối", false),
        vn.smarthome.data.wifi.SecuritySetting("newDeviceAlert", "Cảnh báo khi có thiết bị mới", "Gửi thông báo ngay khi thiết bị lạ truy cập mạng", true),
    )

    fun wifiStatsDay(): vn.smarthome.data.wifi.StatsDataset = vn.smarthome.data.wifi.StatsDataset(
        title = "Biểu đồ lưu lượng (24 giờ qua)",
        downLabel = "Tải xuống hôm nay", downVal = "45.2 GB", downSub = "+8.4% so với hôm qua",
        upLabel = "Tải lên hôm nay", upVal = "8.7 GB", upSub = "Ổn định trong khung giờ",
        totalLabel = "Tổng lưu lượng tháng này", totalVal = "1.2 TB", totalSub = "Trung bình 40 GB/ngày",
        peakVal = "185 Mb/s", peakSub = "Ghi nhận lúc 20:15",
        labels = listOf("00h", "02h", "04h", "06h", "08h", "10h", "12h", "14h", "16h", "18h", "20h", "22h"),
        down = listOf(1.8f, 1.1f, 0.7f, 1.4f, 3.8f, 4.2f, 3.5f, 4.1f, 5.2f, 6.8f, 7.6f, 5.0f),
        up = listOf(0.3f, 0.2f, 0.15f, 0.4f, 0.9f, 1.1f, 0.8f, 0.9f, 1.0f, 1.2f, 1.1f, 0.65f),
        categories = listOf(42, 26, 22, 10),
    )

    fun wifiStatsWeek(): vn.smarthome.data.wifi.StatsDataset = vn.smarthome.data.wifi.StatsDataset(
        title = "Biểu đồ lưu lượng (7 ngày qua)",
        downLabel = "Tải xuống tuần này", downVal = "286.4 GB", downSub = "+5.1% so với tuần trước",
        upLabel = "Tải lên tuần này", upVal = "54.8 GB", upSub = "Đạt đỉnh vào Thứ 7",
        totalLabel = "Tổng lưu lượng tuần này", totalVal = "341.2 GB", totalSub = "Trung bình 48.7 GB/ngày",
        peakVal = "214 Mb/s", peakSub = "Ghi nhận Thứ 7, 21:05",
        labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN"),
        down = listOf(34.2f, 38.5f, 36.1f, 45.2f, 41.8f, 48.6f, 42.0f),
        up = listOf(6.4f, 7.1f, 6.8f, 8.7f, 7.9f, 9.8f, 8.1f),
        categories = listOf(40, 28, 23, 9),
    )

    fun wifiStatsMonth(): vn.smarthome.data.wifi.StatsDataset = vn.smarthome.data.wifi.StatsDataset(
        title = "Biểu đồ lưu lượng (30 ngày qua)",
        downLabel = "Tải xuống tháng này", downVal = "1,024 GB", downSub = "+11.2% so với tháng trước",
        upLabel = "Tải lên tháng này", upVal = "198.5 GB", upSub = "Sao lưu Camera & Cloud",
        totalLabel = "Tổng lưu lượng tháng này", totalVal = "1.22 TB", totalSub = "Giới hạn gói: Không giới hạn",
        peakVal = "248 Mb/s", peakSub = "Ghi nhận ngày 14/11",
        labels = listOf("Tuần 1", "Tuần 2", "Tuần 3", "Tuần 4"),
        down = listOf(242f, 268f, 254f, 260f),
        up = listOf(46f, 54f, 48f, 50.5f),
        categories = listOf(38, 27, 25, 10),
    )

    fun wifiChannelNoise(): List<vn.smarthome.data.wifi.ChannelNoise> = listOf(
        vn.smarthome.data.wifi.ChannelNoise(36, 62),
        vn.smarthome.data.wifi.ChannelNoise(44, 48),
        vn.smarthome.data.wifi.ChannelNoise(149, 18),
        vn.smarthome.data.wifi.ChannelNoise(157, 35),
    )

    // ---- Server (serverubuntu) ----
    fun serverInfo(): ServerInfo = ServerInfo()

    fun serverMetric(): ServerMetric = ServerMetric()

    fun serverContainers(): List<ServerContainer> = listOf(
        ServerContainer("mosquitto", "eclipse-mosquitto:2", "running", "Up 3 days", Icons.Filled.Hub),
        ServerContainer("dien-evn", "dien-evn:1.0", "running", "Up 3 days", Icons.Filled.ElectricBolt),
        ServerContainer("wifi-collector", "wifi-collector:1.0", "running", "Up 2 days", Icons.Filled.Router),
        ServerContainer("go2rtc", "alexxit/go2rtc", "running", "Up 3 days", Icons.Filled.Videocam),
    )

    fun serverTemps(): List<ServerTemp> = listOf(
        ServerTemp("acpitz temp1", 40f),
        ServerTemp("acpitz temp3", 36f),
        ServerTemp("acpitz temp4", 43f),
        ServerTemp("acpitz temp5", 24f),
        ServerTemp("Package id 0", 45f),
        ServerTemp("Core 0", 43f),
        ServerTemp("Core 1", 39f),
    )

    fun serverPower(): ServerPower = ServerPower()

    fun serverDays(): List<ServerDay> = listOf(
        ServerDay("2026-11-19", 41.43f, 37f, 48f),
        ServerDay("2026-11-18", 47.55f, 37f, 60f),
        ServerDay("2026-11-17", 38.06f, 43f, 57f),
        ServerDay("2026-11-06", 36.41f, 45f, 60f),
        ServerDay("2026-11-05", 72.83f, 42f, 62f),
        ServerDay("2026-11-04", 18.19f, 44f, 60f),
    )
}
