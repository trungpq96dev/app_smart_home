package vn.smarthome.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.smarthome.data.CameraFeed
import vn.smarthome.data.DemoData
import vn.smarthome.data.QuickItem
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.data.StatKind
import vn.smarthome.data.StatRange
import vn.smarthome.data.dien.CauHinhMqtt
import vn.smarthome.data.dien.CauHinhStore
import vn.smarthome.data.dien.DienState
import vn.smarthome.data.dien.KetNoi
import vn.smarthome.data.dien.MqttDien

data class HomeState(
    val rooms: List<RoomUi> = DemoData.rooms(),
    val quick: List<QuickItem> = DemoData.quickItems(),
    val cameras: List<CameraFeed> = DemoData.cameras(),
    val selectedRoom: Int = 0,
    val selectedCamera: Int = 0,
    val statKind: StatKind = StatKind.POWER,
    val statRange: StatRange = StatRange.WEEK,
)

/**
 * Giu trang thai thao tac cua khung giao dien, VA so dien that doc tu MQTT.
 *
 * Phan den/quat/rem/camera van la du lieu mau vi chua co nguon that.
 * Rieng tab Dien nang lay tu EMQX qua [MqttDien] - khong tron hai thu voi
 * nhau, va khong bao gio lay so mau bu vao khi mat ket noi.
 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val store = CauHinhStore(app)
    private val mqtt = MqttDien()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** So dien that. Rong cho toi khi broker tra retained ve. */
    val dien: StateFlow<DienState> = mqtt.soLieu

    /** Trang thai duong truyen toi EMQX. */
    val ketNoi: StateFlow<KetNoi> = mqtt.ketNoi

    val cauHinh: StateFlow<CauHinhMqtt> = store.cauHinh
        .stateIn(viewModelScope, SharingStarted.Eagerly, CauHinhMqtt())

    init {
        // Doi cau hinh la noi lai. distinctUntilChanged de luu cau hinh y het
        // cu khong lam dut ket noi dang chay.
        viewModelScope.launch {
            store.cauHinh.distinctUntilChanged().collect { c ->
                mqtt.noi(c)
            }
        }
    }

    fun luuCauHinh(c: CauHinhMqtt) {
        viewModelScope.launch { store.luu(c) }
    }

    override fun onCleared() {
        mqtt.ngat()
        super.onCleared()
    }

    // ---- Trang chu ----

    fun selectRoom(index: Int) = _state.update { it.copy(selectedRoom = index) }

    fun toggleQuick(id: String) = _state.update { s ->
        s.copy(quick = s.quick.map { if (it.id == id) it.copy(on = !it.on) else it })
    }

    // ---- Chi tiet phong ----

    fun toggleLight(roomId: String) = editRoom(roomId) { it.copy(lightOn = !it.lightOn) }

    fun toggleFan(roomId: String) = editRoom(roomId) { it.copy(fanOn = !it.fanOn) }

    fun toggleAc(roomId: String) = editRoom(roomId) { it.copy(acOn = !it.acOn) }

    /** Kep trong 16..30 do C - ngoai khoang nay dieu hoa dan dung khong nhan. */
    fun changeAcTemp(roomId: String, delta: Int) = editRoom(roomId) {
        it.copy(acTemp = (it.acTemp + delta).coerceIn(16, 30))
    }

    fun setMode(roomId: String, mode: RoomMode) = editRoom(roomId) { it.copy(mode = mode) }

    fun toggleSchedule(roomId: String) = editRoom(roomId) { it.copy(scheduleOn = !it.scheduleOn) }

    private fun editRoom(roomId: String, block: (RoomUi) -> RoomUi) = _state.update { s ->
        s.copy(rooms = s.rooms.map { if (it.id == roomId) block(it) else it })
    }

    // ---- Camera ----

    fun selectCamera(index: Int) = _state.update { it.copy(selectedCamera = index) }

    // ---- Thong ke ----

    fun setStatKind(kind: StatKind) = _state.update { it.copy(statKind = kind) }

    fun setStatRange(range: StatRange) = _state.update { it.copy(statRange = range) }
}
