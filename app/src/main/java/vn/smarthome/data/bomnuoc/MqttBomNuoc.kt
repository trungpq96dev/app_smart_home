package vn.smarthome.data.bomnuoc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

/**
 * Duong MQTT toi bo `bom_nuoc` - dung duoc ca khi ra khoi mang nha.
 *
 * Topic co dinh trong firmware: `bomnuoc/state`, `bomnuoc/cmd`,
 * `bomnuoc/status`, `bomnuoc/usage`.
 * Dung `clientId` rieng (`tablet-sh-bom-...`) de khong da bo hoac app doc lap.
 * Neu broker tu choi tai khoan thi dung thu lai han de khong dot han muc.
 */
class MqttBomNuoc(
    private val config: CauHinhBomNuoc,
    private val khiNhanTrangThai: (BomNuocState) -> Unit,
) {
    companion object {
        private const val CHUA_THAY_BO_SAU_MS = 15_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<BomNuocState?>(null)
    val state: StateFlow<BomNuocState?> = _state.asStateFlow()

    private val _usage = MutableStateFlow<List<LuongNuocThang>?>(null)
    val usage: StateFlow<List<LuongNuocThang>?> = _usage.asStateFlow()

    private val _link = MutableStateFlow<TrangThaiKetNoiBom>(TrangThaiKetNoiBom.DangNoi)
    val link: StateFlow<TrangThaiKetNoiBom> = _link.asStateFlow()

    private val _lastStateAt = MutableStateFlow(0L)
    private val _deviceOnline = MutableStateFlow(false)

    @Volatile private var client: MqttClient? = null
    @Volatile private var connectedAt = 0L
    @Volatile private var authRejected = false

    fun batDau() {
        if (!config.coMqtt) {
            _link.value = TrangThaiKetNoiBom.Loi("chưa nhập địa chỉ broker")
            return
        }
        authRejected = false
        scope.launch { vongKetNoi() }
        scope.launch { watchdog() }
    }

    fun dung() {
        val c = client
        client = null
        scope.cancel()
        if (c != null) {
            runCatching { c.disconnectForcibly(400L, 400L) }
            runCatching { c.close(true) }
        }
        _deviceOnline.value = false
        _link.value = TrangThaiKetNoiBom.ChuaCauHinh
    }

    private suspend fun vongKetNoi() {
        var backoff = 4_000L
        while (scope.isActive && !authRejected) {
            if (client?.isConnected == true) {
                delay(2_000)
                continue
            }
            _link.value = TrangThaiKetNoiBom.DangNoi
            val ok = runCatching { thucHienKetNoi() }
                .onFailure { e ->
                    if (laLoiTaiKhoan(e)) {
                        authRejected = true
                        _link.value = TrangThaiKetNoiBom.SaiTaiKhoan
                    } else {
                        _link.value = TrangThaiKetNoiBom.Loi(e.message.orEmpty())
                    }
                }
                .isSuccess

            if (authRejected) return
            if (!ok) {
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(60_000L)
            } else {
                backoff = 4_000L
            }
        }
    }

    private fun thucHienKetNoi() {
        val uri = config.mqttUri()
        val cid = "tablet-sh-bom-" + UUID.randomUUID().toString().take(8)
        val c = MqttClient(uri, cid, MemoryPersistence())
        val opts = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = false
            connectionTimeout = 15
            keepAliveInterval = 30
            maxInflight = 20
            if (config.brokerUser.isNotBlank()) userName = config.brokerUser
            if (config.brokerPass.isNotBlank()) password = config.brokerPass.toCharArray()
        }

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                connectedAt = System.currentTimeMillis()
                runCatching {
                    c.subscribe(
                        arrayOf(BomNuocTopic.STATE, BomNuocTopic.STATUS, BomNuocTopic.USAGE),
                        intArrayOf(1, 1, 1),
                    )
                }
            }

            override fun connectionLost(cause: Throwable?) {
                _link.value = TrangThaiKetNoiBom.MatBroker
                _deviceOnline.value = false
            }

            override fun messageArrived(topic: String, message: MqttMessage) {
                xuLyBanTin(topic, String(message.payload, Charsets.UTF_8))
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
        })

        c.connect(opts)
        client = c
        connectedAt = System.currentTimeMillis()
    }

    private fun xuLyBanTin(topic: String, payload: String) {
        when (topic) {
            BomNuocTopic.STATUS -> {
                _deviceOnline.value = payload.trim() == "online"
            }

            BomNuocTopic.STATE -> {
                BomNuocState.tuJson(payload)?.let { s ->
                    _state.value = s
                    _lastStateAt.value = System.currentTimeMillis()
                    _deviceOnline.value = true
                    khiNhanTrangThai(s)
                }
            }

            BomNuocTopic.USAGE -> {
                LuongNuocThang.tuJsonArray(payload)?.let { _usage.value = it }
            }
        }
    }

    private suspend fun watchdog() {
        while (scope.isActive) {
            delay(1_000)
            if (authRejected) continue

            val connected = client?.isConnected == true
            val last = _lastStateAt.value
            _link.value = when {
                !connected -> if (_link.value is TrangThaiKetNoiBom.DangNoi) {
                    TrangThaiKetNoiBom.DangNoi
                } else {
                    TrangThaiKetNoiBom.MatBroker
                }

                last == 0L -> if (connectedAt > 0 &&
                    System.currentTimeMillis() - connectedAt > CHUA_THAY_BO_SAU_MS
                ) {
                    TrangThaiKetNoiBom.ChiCoBroker
                } else {
                    TrangThaiKetNoiBom.DangNoi
                }

                else -> {
                    val ageSec = ((System.currentTimeMillis() - last) / 1000).toInt().coerceAtLeast(0)
                    val stale = ageSec > (_state.value?.nguongImLangGiay ?: 100)
                    when {
                        !_deviceOnline.value -> TrangThaiKetNoiBom.BoOffline
                        stale -> TrangThaiKetNoiBom.SoCu(ageSec)
                        else -> TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.MQTT, ageSec)
                    }
                }
            }
        }
    }

    suspend fun guiLenh(query: String): Result<Unit> = withContext(Dispatchers.IO) {
        val c = client
        if (c == null || !c.isConnected) {
            return@withContext Result.failure(IllegalStateException("chưa nối được broker bơm"))
        }
        runCatching {
            c.publish(BomNuocTopic.CMD, query.toByteArray(Charsets.UTF_8), 1, false)
        }
    }

    private fun laLoiTaiKhoan(e: Throwable): Boolean {
        val m = e.message.orEmpty().lowercase()
        if (m.contains("not authorized") || m.contains("bad user") ||
            m.contains("bad username") || m.contains("password")
        ) return true
        val code = (e as? MqttException)?.reasonCode?.toInt()
            ?: ((e.cause as? MqttException)?.reasonCode?.toInt())
        return code == MqttException.REASON_CODE_FAILED_AUTHENTICATION.toInt() ||
            code == MqttException.REASON_CODE_NOT_AUTHORIZED.toInt()
    }
}
