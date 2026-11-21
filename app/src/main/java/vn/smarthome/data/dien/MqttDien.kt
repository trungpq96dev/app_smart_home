package vn.smarthome.data.dien

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.data.congtac.CongTacTopic
import vn.smarthome.data.congtac.LenhCongTac
import vn.smarthome.data.cuktech.CongSacId
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.data.cuktech.CuktechTopic
import vn.smarthome.data.server.ServerState
import vn.smarthome.data.server.ServerTopic
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.data.wifi.WifiTopic
import kotlin.random.Random

/**
 * Topic app GHI vao de dieu khien thiet bi.
 *
 * Tach rieng khoi [DienTopic] co chu dinh: `dien/#` la so do script dien_evn
 * quyet dinh, app khong duoc ghi vao do. Day la topic rieng cua app.
 */
object DieuKhienTopic {
    const val CMD = "smarthome/control"
}

/**
 * Ket qua cua mot lan ban lenh dieu khien.
 *
 * Vi sao khong tra Boolean: man hinh can PHAN BIET "chua noi broker" voi
 * "noi roi nhung publish nem loi" de noi dung cau cho nguoi dung. Hai thu do
 * phai sua hai cho khac nhau.
 */
sealed interface KetQuaGui {
    /** Da len broker. KHONG co nghia la cong tac da nhan - xem ghi chu o guiLenh. */
    data class DaGui(val id: String) : KetQuaGui
    data object ChuaNoiBroker : KetQuaGui
    data class Loi(val moTa: String) : KetQuaGui
}

/** Trang thai duong truyen toi EMQX. */
sealed interface KetNoi {
    data object ChuaCauHinh : KetNoi
    data object DangNoi : KetNoi
    data object DaNoi : KetNoi
    data class Loi(val moTa: String) : KetNoi
}

/**
 * Doc so dien that tu EMQX.
 *
 * Dung ban Java thuan cua Paho, KHONG dung Paho Android Service (da bo hoang
 * tu lau). Phan tu ket noi lai giao cho `isAutomaticReconnect` cua Paho.
 *
 * Vi sao chi doc ma khong publish: app la man hinh xem. Nguoi quyet dinh so
 * la script dien_evn chay tren serverubuntu. App khong duoc ghi gi vao
 * `dien/<topic>` - ghi vao la de len so that cua duong ong.
 */
class MqttDien {

    private val _soLieu = MutableStateFlow(DienState())
    val soLieu: StateFlow<DienState> = _soLieu.asStateFlow()

    private val _wifi = MutableStateFlow(WifiState())
    val wifi: StateFlow<WifiState> = _wifi.asStateFlow()

    private val _server = MutableStateFlow(ServerState())
    val server: StateFlow<ServerState> = _server.asStateFlow()

    private val _congTac = MutableStateFlow(CongTacState())
    val congTac: StateFlow<CongTacState> = _congTac.asStateFlow()

    private val _cuktech = MutableStateFlow(CuktechState())
    val cuktech: StateFlow<CuktechState> = _cuktech.asStateFlow()

    private val _ketNoi = MutableStateFlow<KetNoi>(KetNoi.ChuaCauHinh)
    val ketNoi: StateFlow<KetNoi> = _ketNoi.asStateFlow()

    private var client: MqttAsyncClient? = null

    /**
     * Noi toi broker. Goi lai ham nay khi doi cau hinh - no tu ngat ket noi cu.
     *
     * clientId PHAI khac bo bom va bo den. Tai lieu ban giao muc 4 buoc 3:
     * trung id la EMQX da ca hai ra, va trieu chung nhin giong mat song chu
     * khong giong trung id.
     */
    fun noi(cauHinh: CauHinhMqtt) {
        ngat()

        if (!cauHinh.dayDu()) {
            _ketNoi.value = KetNoi.ChuaCauHinh
            return
        }

        val uri = cauHinh.uri()
        val id = cauHinh.clientId.ifBlank { "tablet-smarthome-" + Random.nextInt(100000, 999999) }

        _ketNoi.value = KetNoi.DangNoi

        val c = try {
            MqttAsyncClient(uri, id, MemoryPersistence())
        } catch (e: MqttException) {
            _ketNoi.value = KetNoi.Loi("Địa chỉ broker không hợp lệ: " + uri)
            return
        }
        client = c

        val opts = MqttConnectOptions().apply {
            isCleanSession = true          // chi can retained, khong can hang doi cu
            isAutomaticReconnect = true
            connectionTimeout = 15
            keepAliveInterval = 45
            if (cauHinh.taiKhoan.isNotBlank()) userName = cauHinh.taiKhoan
            if (cauHinh.matKhau.isNotBlank()) password = cauHinh.matKhau.toCharArray()
        }

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                _ketNoi.value = KetNoi.DaNoi
                dangKyTopic(c)
            }

            override fun connectionLost(cause: Throwable?) {
                _ketNoi.value = KetNoi.Loi(
                    "Mất kết nối broker: " + (cause?.message ?: "không rõ lý do") +
                        ". Paho đang tự nối lại."
                )
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                val payload = String(message.payload, Charsets.UTF_8)
                if (topic.startsWith("dien/")) {
                    _soLieu.update { DienState.gop(it, topic, payload) }
                } else if (topic.startsWith("wifi/")) {
                    _wifi.update { WifiState.gop(it, topic, payload) }
                } else if (topic.startsWith("server/")) {
                    _server.update { ServerState.gop(it, topic, payload) }
                } else if (topic.startsWith("congtac/")) {
                    _congTac.update { CongTacState.gop(it, topic, payload) }
                } else if (topic.startsWith("cuktech/")) {
                    _cuktech.update { CuktechState.gop(it, topic, payload) }
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        try {
            c.connect(opts, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                override fun onSuccess(token: IMqttToken?) = Unit
                override fun onFailure(token: IMqttToken?, e: Throwable?) {
                    _ketNoi.value = KetNoi.Loi(dienGiaiLoi(e))
                }
            })
        } catch (e: MqttException) {
            _ketNoi.value = KetNoi.Loi(dienGiaiLoi(e))
        }
    }

    /**
     * Subscribe bon goc so lieu tu broker.
     */
    private fun dangKyTopic(c: MqttAsyncClient) {
        try {
            val topics = GOC_CAN_DOC.toTypedArray()
            // Sinh tu danh sach de khong bao gio lech do dai duoc nua.
            val qoss = IntArray(GOC_CAN_DOC.size) { 1 }
            c.subscribe(topics, qoss, null, object : org.eclipse.paho.client.mqttv3.IMqttActionListener {
                override fun onSuccess(token: IMqttToken?) {
                    // Ban cu chi kiem cap[0], tuc chi `dien/#`. Broker tu choi
                    // goc thu hai tro di thi app IM LANG - man hinh trong ma
                    // khong ai noi vi sao. Broker nha chua co acl_file nen loi
                    // nay chua bao gio ban, nhung no se ban dung ngay siet ACL.
                    val cap = token?.grantedQos ?: return
                    if (cap.size != GOC_CAN_DOC.size) {
                        _ketNoi.value = KetNoi.Loi(
                            "Broker trả về ${cap.size} kết quả cho ${GOC_CAN_DOC.size} topic. " +
                                "Không biết topic nào được cấp quyền — coi như chưa đọc được gì."
                        )
                        return
                    }
                    val tuChoi = GOC_CAN_DOC.filterIndexed { i, _ -> cap[i] == 0x80 }
                    if (tuChoi.isNotEmpty()) {
                        _ketNoi.value = KetNoi.Loi(
                            "Broker từ chối cho đọc: " + tuChoi.joinToString(", ") +
                                ". Cần cấp quyền subscribe cho tài khoản này trên Mosquitto."
                        )
                    }
                }

                override fun onFailure(token: IMqttToken?, e: Throwable?) {
                    _ketNoi.value = KetNoi.Loi("Không đăng ký được topic: " + (e?.message ?: ""))
                }
            })
        } catch (e: MqttException) {
            _ketNoi.value = KetNoi.Loi("Không đăng ký được topic: " + (e.message ?: ""))
        }
    }

    /** Gui lenh dieu khien WiFi (vi du 'reboot' modem). */
    fun guiLenhWifi(lenh: String) {
        client?.let { c ->
            if (c.isConnected) {
                try {
                    val msg = MqttMessage(lenh.toByteArray(Charsets.UTF_8)).apply { qos = 1 }
                    c.publish(WifiTopic.CMD, msg)
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Ban mot lenh dieu khien, tra ve ket qua noi duoc thanh cau.
     *
     * Tu 21/11/2026 da co nguoi nghe topic nay: `scripts/congtac_bridge/` chay
     * tren serverubuntu. Nhung cau quan trong nhat van giu nguyen - ham nay
     * tra ve "da gui len broker hay chua", KHONG phai "cong tac da nhan hay
     * chua". Hai thu do khac nhau, va lan lon chung la noi doi voi nguoi dung.
     * Do chinh la ly do ton tai cua buoc doi soat trong HomeViewModel.
     */
    fun guiLenh(
        phong: String,
        thietBi: String,
        hanhDong: String,
        giaTri: String? = null,
        nguon: String,
    ): KetQuaGui {
        val c = client ?: return KetQuaGui.ChuaNoiBroker
        if (!c.isConnected) return KetQuaGui.ChuaNoiBroker
        val id = LenhCongTac.maLenh()
        val json = LenhCongTac.json(phong, thietBi, hanhDong, giaTri, nguon, id)
        return try {
            val msg = MqttMessage(json.toByteArray(Charsets.UTF_8)).apply { qos = 1 }
            c.publish(DieuKhienTopic.CMD, msg)
            KetQuaGui.DaGui(id)
        } catch (e: Exception) {
            KetQuaGui.Loi(e.message ?: "không gửi được lệnh lên broker")
        }
    }

    /**
     * Ban mot lenh dieu khien thiet bi ra `smarthome/control`.
     *
     * Tang thap, con lai cho duong giong noi cu. Duong moi dung [guiLenh].
     *
     * Khong dung retained: lenh bat den la mot su kien, giu lai thi thiet bi
     * nao vua cam dien len cung bat den theo mot lenh tu hom qua.
     */
    fun guiLenhDieuKhien(json: String): Boolean {
        val c = client ?: return false
        if (!c.isConnected) return false
        return try {
            val msg = MqttMessage(json.toByteArray(Charsets.UTF_8)).apply { qos = 1 }
            c.publish(DieuKhienTopic.CMD, msg)
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        /**
         * Bon goc so lieu app doc. Co TEN di kem de bao duoc dung goc nao bi
         * broker tu choi - xem `dangKyTopic`.
         */
        val GOC_CAN_DOC = listOf(
            DienTopic.GOC, WifiTopic.GOC, ServerTopic.GOC, CongTacTopic.GOC,
            CuktechTopic.GOC,
        )
    }

    /**
     * Bat/tat mot cong cua cu sac CUKTECH.
     *
     * Topic va hinh dang payload KHONG do app dat ra: day la hop dong san co
     * cua `cuktech-ble-server`. Server tu kiem cong hop le roi moi day xuong
     * BLE, nen gui bay khong lam hong thiet bi.
     *
     * Nhu [guiLenh], ham nay tra ve "da gui len broker hay chua", KHONG phai
     * "cu sac da tat cong hay chua". Cu sac khong co topic bao ket qua; cach
     * duy nhat biet no da nghe la doi mat na PIID 16 trong `settings` doi -
     * cham nhat mot nhip lam moi (mac dinh 10 giay). Doi soat o HomeViewModel.
     */
    fun guiLenhCuktech(cong: CongSacId, bat: Boolean): KetQuaGui {
        val c = client ?: return KetQuaGui.ChuaNoiBroker
        if (!c.isConnected) return KetQuaGui.ChuaNoiBroker
        val json = org.json.JSONObject()
            .put("port", cong.khoaTopic)
            .put("action", if (bat) "on" else "off")
            .toString()
        return try {
            val msg = MqttMessage(json.toByteArray(Charsets.UTF_8)).apply { qos = 1 }
            c.publish(CuktechTopic.CMD_CONG, msg)
            KetQuaGui.DaGui(cong.khoaTopic)
        } catch (e: Exception) {
            KetQuaGui.Loi(e.message ?: "không gửi được lệnh lên broker")
        }
    }

    fun ngat() {
        val c = client ?: return
        client = null
        runCatching { c.disconnectForcibly(500, 500) }
        runCatching { c.close(true) }
    }

    /**
     * Doi loi cua Paho thanh cau noi ro CHO PHAI SUA, khong chi in stack trace.
     *
     * Phai di HET chuoi nguyen nhan: Paho boc loi that vao MqttException, va
     * reasonCode nhieu khi chi la 0 trong khi nguyen nhan that (UnknownHost,
     * loi chung chi) nam sau hai ba lop cause.
     */
    private fun dienGiaiLoi(e: Throwable?): String {
        val chuoi = generateSequence(e) { it.cause }.take(8).toList()
        val tenLop = chuoi.joinToString(" ") { it.javaClass.name }
        val loiVan = chuoi.mapNotNull { it.message }.joinToString(" ")

        when {
            tenLop.contains("UnknownHostException") ||
                loiVan.contains("Unable to resolve host", true) ->
                return "Không tìm thấy máy chủ broker. Sai tên miền, hoặc máy tính bảng chưa vào được mạng có broker đó."
            tenLop.contains("SSLHandshakeException") ||
                loiVan.contains("trust anchor", true) ||
                loiVan.contains("CertPathValidator", true) ->
                return "Chứng chỉ TLS của broker không được Android tin. EMQX dùng chứng chỉ tự ký thì phải cài chứng chỉ đó vào máy tính bảng."
            loiVan.contains("WRONG_VERSION_NUMBER", true) ->
                return "Cổng này không nói TLS. Tắt TLS hoặc đổi sang cổng 8883."
            tenLop.contains("ConnectException") ||
                loiVan.contains("ECONNREFUSED", true) ->
                return "Broker từ chối kết nối. Kiểm tra cổng và xem EMQX có đang chạy không."
        }

        val mq = e as? MqttException
        return when (mq?.reasonCode?.toInt()) {
            MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt() ->
                "Sai tài khoản hoặc mật khẩu MQTT."
            MqttException.REASON_CODE_NOT_AUTHORIZED.toInt() ->
                "Tài khoản MQTT không được phép kết nối. Kiểm tra quyền trên EMQX."
            MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt(),
            MqttException.REASON_CODE_CONNECTION_LOST.toInt() ->
                "Không gọi được broker. Kiểm tra địa chỉ, cổng và mạng của máy tính bảng."
            MqttException.REASON_CODE_CLIENT_EXCEPTION.toInt() -> {
                val goc = mq.cause?.message ?: mq.message ?: ""
                when {
                    goc.contains("Unable to resolve host", true) ||
                        goc.contains("UnknownHost", true) ->
                        "Không tìm thấy máy chủ broker. Sai tên miền hoặc máy tính bảng chưa vào được mạng đó."
                    goc.contains("trust anchor", true) || goc.contains("certificate", true) ->
                        "Chứng chỉ TLS của broker không được Android tin. Nếu EMQX dùng chứng chỉ tự ký thì phải cài chứng chỉ đó vào máy tính bảng."
                    else -> "Lỗi kết nối: " + goc
                }
            }
            else -> "Lỗi kết nối: " + (e?.message ?: "không rõ")
        }
    }
}
