package vn.smarthome.data.bomnuoc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToLong

data class ThamSoBaoVeDaLamTron(
    val dryCheck: Int,
    val minRest: Double,
    val maxRun: Int,
    val pulses: Double,
    val minFlow: Double,
) {
    fun thanhQuery(): String =
        "cfgDry=$dryCheck&cfgRest=$minRest&cfgRun=$maxRun&cfgPulses=$pulses&cfgMinFlow=$minFlow"
}

/**
 * Quan ly dong thoi 2 duong ket noi (HTTP LAN 1s & MQTT 30s) + bo gui lenh
 * co cho bo xac nhan 8 giay (`CommandBus`), chong bam lien tuc.
 */
class QuanLyBomNuoc(
    private val scope: CoroutineScope,
) {
    companion object {
        private const val DO_LAN_MOI_MS = 15_000L
        const val CHO_XAC_NHAN_MS = 8_000L

        fun kiemTraBaoVe(
            dry: Int?,
            minFlow: Double?,
            maxRun: Int?,
            minRest: Double?,
            pulses: Double?,
        ): String? = when {
            dry == null || dry !in 5..600 ->
                "Thời gian kiểm tra chạy khô phải từ 5 đến 600 giây"
            minFlow == null || minFlow < 0.1 || minFlow > 50.0 ->
                "Ngưỡng chạy khô phải từ 0.1 đến 50 L/phút"
            maxRun == null || maxRun !in 1..360 ->
                "Thời gian chạy tối đa phải từ 1 đến 360 phút"
            minRest == null || minRest < 0.1 || minRest > 180.0 ->
                "Thời gian nghỉ phải từ 0.1 đến 180 phút"
            pulses == null || pulses < 5.0 || pulses > 10000.0 ->
                "Hiệu chuẩn cảm biến phải từ 5 đến 10000 xung/lít"
            else -> null
        }

        fun lamTronBaoVe(
            dry: Int,
            rest: Double,
            run: Int,
            pulses: Double,
            minFlow: Double,
        ): ThamSoBaoVeDaLamTron = ThamSoBaoVeDaLamTron(
            dryCheck = dry,
            minRest = (rest * 10.0).roundToLong() / 10.0,
            maxRun = run,
            pulses = (pulses * 10.0).roundToLong() / 10.0,
            minFlow = (minFlow * 100.0).roundToLong() / 100.0,
        )

        fun ganBang(a: Double?, b: Double, saiSo: Double): Boolean =
            a != null && abs(a - b) < saiSo
    }

    private var mqtt: MqttBomNuoc? = null
    private var http: HttpBomNuoc? = null
    private val jobs = mutableListOf<Job>()

    private val _dangXemMan = MutableStateFlow(false)

    private val _state = MutableStateFlow<BomNuocState?>(null)
    val state: StateFlow<BomNuocState?> = _state.asStateFlow()

    private val _usage = MutableStateFlow<List<LuongNuocThang>>(emptyList())
    val usage: StateFlow<List<LuongNuocThang>> = _usage.asStateFlow()

    private val _link = MutableStateFlow<TrangThaiKetNoiBom>(TrangThaiKetNoiBom.ChuaCauHinh)
    val link: StateFlow<TrangThaiKetNoiBom> = _link.asStateFlow()

    private val _kenh = MutableStateFlow(KenhBomNuoc.KHONG)
    val kenh: StateFlow<KenhBomNuoc> = _kenh.asStateFlow()

    private val _choLenh = MutableStateFlow<Set<String>>(emptySet())
    val choLenh: StateFlow<Set<String>> = _choLenh.asStateFlow()

    private val stateSeq = MutableStateFlow(0L)
    private val khoaLenh = ConcurrentHashMap<String, Boolean>()

    fun datDangXemMan(dangXem: Boolean) {
        _dangXemMan.value = dangXem
    }

    fun ketNoi(config: CauHinhBomNuoc) {
        ngat()
        if (!config.daCauHinh) {
            _state.value = null
            _usage.value = emptyList()
            _link.value = TrangThaiKetNoiBom.ChuaCauHinh
            _kenh.value = KenhBomNuoc.KHONG
            return
        }

        if (config.coMqtt) {
            mqtt = MqttBomNuoc(config) { s ->
                if (_kenh.value != KenhBomNuoc.LAN) {
                    _state.value = s
                    stateSeq.update { it + 1 }
                }
            }.also { t ->
                t.batDau()
                jobs += scope.launch {
                    t.usage.collect { if (it != null && _kenh.value != KenhBomNuoc.LAN) _usage.value = it }
                }
                jobs += scope.launch {
                    t.link.collect { if (_kenh.value != KenhBomNuoc.LAN) _link.value = it }
                }
            }
        }

        if (config.coLan) {
            val httpOwns = { _kenh.value == KenhBomNuoc.LAN || !config.coMqtt }
            http = HttpBomNuoc(config.espHost, _dangXemMan) { s ->
                if (httpOwns()) {
                    _state.value = s
                    stateSeq.update { it + 1 }
                }
            }.also { t ->
                t.batDau()
                jobs += scope.launch {
                    t.usage.collect { if (it != null && httpOwns()) _usage.value = it }
                }
                jobs += scope.launch {
                    t.link.collect { if (httpOwns()) _link.value = it }
                }
            }
            jobs += scope.launch { theoDoiLan(config) }
        } else {
            _kenh.value = KenhBomNuoc.MQTT
        }
    }

    fun ngat() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        mqtt?.dung(); mqtt = null
        http?.dung(); http = null
        _kenh.value = KenhBomNuoc.KHONG
        _link.value = TrangThaiKetNoiBom.ChuaCauHinh
    }

    private suspend fun theoDoiLan(config: CauHinhBomNuoc) {
        while (scope.isActive) {
            val h = http
            val goiDuoc = h != null && config.uuTienLan && h.thuGoi()
            val kenhMoi = when {
                goiDuoc -> KenhBomNuoc.LAN
                config.coMqtt -> KenhBomNuoc.MQTT
                config.coLan -> KenhBomNuoc.LAN
                else -> KenhBomNuoc.KHONG
            }
            if (kenhMoi != _kenh.value) {
                _kenh.value = kenhMoi
                dongBoTuKenh(kenhMoi)
            }
            delay(DO_LAN_MOI_MS)
        }
    }

    private fun dongBoTuKenh(k: KenhBomNuoc) {
        when (k) {
            KenhBomNuoc.LAN -> http?.let {
                _link.value = it.link.value
                it.state.value?.let { s ->
                    _state.value = s
                    stateSeq.update { n -> n + 1 }
                }
                it.usage.value?.let { u -> _usage.value = u }
            }

            KenhBomNuoc.MQTT -> mqtt?.let {
                _link.value = it.link.value
                it.state.value?.let { s ->
                    _state.value = s
                    stateSeq.update { n -> n + 1 }
                }
                it.usage.value?.let { u -> _usage.value = u }
            }

            KenhBomNuoc.KHONG -> _link.value = TrangThaiKetNoiBom.ChuaCauHinh
        }
    }

    suspend fun guiLenh(
        khoa: String,
        query: String,
        xacNhan: (BomNuocState) -> Boolean,
    ): KetQuaLenhBom {
        if (khoaLenh.putIfAbsent(khoa, true) != null) {
            return KetQuaLenhBom.ChuaXacNhan
        }
        _choLenh.update { it + khoa }
        try {
            val seqTruoc = stateSeq.value
            val daGui = guiThang(query)
            if (daGui.isFailure) {
                return KetQuaLenhBom.GuiThatBai(
                    daGui.exceptionOrNull()?.message ?: "không gửi được lệnh"
                )
            }
            val khop = withTimeoutOrNull(CHO_XAC_NHAN_MS) {
                combine(stateSeq, _state) { seq, st -> seq to st }
                    .first { (seq, st) -> seq > seqTruoc && st != null && xacNhan(st) }
            }
            return if (khop != null) KetQuaLenhBom.DaXacNhan else KetQuaLenhBom.ChuaXacNhan
        } finally {
            khoaLenh.remove(khoa)
            _choLenh.update { it - khoa }
        }
    }

    suspend fun guiBomTangAp(bat: Boolean): KetQuaLenhBom {
        val want = if (bat) "on" else "off"
        val kq = guiLenh("booster", "booster=$want") { it.bstMode == want }
        if (kq is KetQuaLenhBom.DaXacNhan && bat) {
            val s = _state.value
            if (s != null && !s.bomApDangChay && (s.bomApDangKhoa || s.dangKhoa)) {
                return KetQuaLenhBom.BiKhoaBaoVe
            }
        }
        return kq
    }

    private suspend fun guiThang(query: String): Result<Unit> {
        val h = http
        if (_kenh.value == KenhBomNuoc.LAN && h != null) {
            val r = h.guiLenh(query)
            if (r.isSuccess) return Result.success(Unit)
        }
        val m = mqtt ?: return Result.failure(IllegalStateException("chưa cấu hình kết nối bo bơm"))
        return m.guiLenh(query)
    }
}
