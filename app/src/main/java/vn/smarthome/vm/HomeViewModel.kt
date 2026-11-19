package vn.smarthome.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.CauHinhCameraStore
import vn.smarthome.data.camera.Go2rtc
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.thoitiet.CauHinhThoiTiet
import vn.smarthome.data.thoitiet.CauHinhThoiTietStore
import vn.smarthome.data.thoitiet.OpenMeteo
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.data.server.ServerState
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.data.ota.OtaManager
import vn.smarthome.data.ota.OtaStatus
import vn.smarthome.data.ota.ReleaseInfo
import vn.smarthome.ui.theme.SH

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
    private val storeCam = CauHinhCameraStore(app)
    private val storeThoiTiet = CauHinhThoiTietStore(app)
    private val mqtt = MqttDien()

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    /** So dien that. Rong cho toi khi broker tra retained ve. */
    val dien: StateFlow<DienState> = mqtt.soLieu

    /** Trang thai WiFi doc tu broker MQTT (serverubuntu). */
    val wifi: StateFlow<WifiState> = mqtt.wifi

    /** Thong tin may chu serverubuntu doc tu broker MQTT (chi xem, khong dieu khien). */
    val server: StateFlow<ServerState> = mqtt.server

    /** Trang thai duong truyen toi EMQX/Mosquitto. */
    val ketNoi: StateFlow<KetNoi> = mqtt.ketNoi

    val cauHinh: StateFlow<CauHinhMqtt> = store.cauHinh
        .stateIn(viewModelScope, SharingStarted.Eagerly, CauHinhMqtt())

    /** Duong toi go2rtc - noi giu luong camera Tuya. */
    val cauHinhCamera: StateFlow<CauHinhCamera> = storeCam.cauHinh
        .stateIn(viewModelScope, SharingStarted.Eagerly, CauHinhCamera())

    private val _trangThaiCam = MutableStateFlow<TrangThaiCam>(TrangThaiCam.ChuaCauHinh)
    val trangThaiCam: StateFlow<TrangThaiCam> = _trangThaiCam.asStateFlow()

    /** Thoi tiet that o Trang chu. Rong (DangTai) cho toi khi Open-Meteo tra ve. */
    private val _thoiTiet = MutableStateFlow<ThoiTietUi>(ThoiTietUi.DangTai)
    val thoiTiet: StateFlow<ThoiTietUi> = _thoiTiet.asStateFlow()

    /** Dia diem thoi tiet nguoi dung chon trong Cai dat. */
    val cauHinhThoiTiet: StateFlow<CauHinhThoiTiet> = storeThoiTiet.cauHinh
        .stateIn(viewModelScope, SharingStarted.Eagerly, CauHinhThoiTiet())

    // ---- OTA Update ----
    val otaManager = OtaManager(app)
    val currentVersionName = otaManager.getCurrentVersionName()

    private val _otaStatus = MutableStateFlow<OtaStatus>(OtaStatus.Idling)
    val otaStatus: StateFlow<OtaStatus> = _otaStatus.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    init {
        // Doi cau hinh la noi lai. distinctUntilChanged de luu cau hinh y het
        // cu khong lam dut ket noi dang chay.
        viewModelScope.launch {
            store.cauHinh.distinctUntilChanged().collect { c ->
                mqtt.noi(c)
            }
        }
        // collectLatest: doi cau hinh giua chung thi bo lan do dang chay,
        // khong de ket qua cu ve sau de len ket qua moi.
        viewModelScope.launch {
            storeCam.cauHinh.distinctUntilChanged().collectLatest { c -> doCamera(c) }
        }
        // Theo dia diem da chon: doi noi thi collectLatest bo vong lap cu, ha the
        // ve "Dang tai" (so cu la cua noi khac, khong con dung) roi lap lai tu dau.
        // 15 phut moi lan la du - Open-Meteo cung chi cap nhat theo gio.
        viewModelScope.launch {
            storeThoiTiet.cauHinh.distinctUntilChanged().collectLatest { c ->
                _thoiTiet.value = ThoiTietUi.DangTai
                while (true) {
                    taiThoiTiet(c)
                    delay(15 * 60 * 1000L)
                }
            }
        }
        // Kiem tra cap nhat tu dong sau khi app khoi dong 4 giay
        viewModelScope.launch {
            delay(4000L)
            kiemTraCapNhat(thuCong = false)
        }
    }

    // ---- Thoi tiet that qua Open-Meteo ----

    fun luuCauHinhThoiTiet(c: CauHinhThoiTiet) {
        viewModelScope.launch { storeThoiTiet.luu(c) }
    }

    /** Nguoi dung bam nut tai lai tren the thoi tiet - giu dia diem dang chon. */
    fun taiLaiThoiTiet() {
        viewModelScope.launch { taiThoiTiet(cauHinhThoiTiet.value) }
    }

    /**
     * Goi Open-Meteo mot lan cho dia diem [c].
     *
     * Chi ha ve "Dang tai" khi CHUA tung co so; neu da co roi ma lan nay hong
     * thi GIU nguyen so cu chu khong lam the nhay ve trang/loi - giong cach
     * [doCamera] khong bia so mau khi hong.
     */
    private suspend fun taiThoiTiet(c: CauHinhThoiTiet) {
        if (_thoiTiet.value !is ThoiTietUi.Co) _thoiTiet.value = ThoiTietUi.DangTai
        OpenMeteo.layThoiTiet(c.lat, c.lon, c.ten)
            .onSuccess { _thoiTiet.value = ThoiTietUi.Co(it) }
            .onFailure { e ->
                if (_thoiTiet.value !is ThoiTietUi.Co) {
                    _thoiTiet.value = ThoiTietUi.Loi(OpenMeteo.dienGiaiLoi(e))
                }
            }
    }

    // ---- Camera that qua go2rtc ----

    fun luuCauHinhCamera(c: CauHinhCamera) {
        viewModelScope.launch { storeCam.luu(c) }
    }

    /** Hoi lai go2rtc - dung khi vua bat go2rtc len hoac vua them luong. */
    fun doLaiCamera() {
        viewModelScope.launch { doCamera(cauHinhCamera.value) }
    }

    /**
     * Hoi go2rtc dang giu nhung luong nao va dung lai danh sach camera.
     *
     * Khi hong thi danh sach ve RONG chu khong lui ve 4 o giu cho. Nguoi dung
     * da khai bao go2rtc nghia la ho cho doi hinh that; bay ra bon camera gia
     * luc do la noi doi rang moi thu van on.
     */
    private suspend fun doCamera(c: CauHinhCamera) {
        if (!c.dayDu()) {
            _trangThaiCam.value = TrangThaiCam.ChuaCauHinh
            _state.update { it.copy(cameras = DemoData.cameras(), selectedCamera = 0) }
            return
        }
        _trangThaiCam.value = TrangThaiCam.DangDo
        Go2rtc.danhSachLuong(c)
            .onSuccess { ten ->
                if (ten.isEmpty()) {
                    _trangThaiCam.value =
                        TrangThaiCam.Loi("go2rtc chạy nhưng chưa khai luồng nào trong go2rtc.yaml.")
                    _state.update { it.copy(cameras = emptyList(), selectedCamera = 0) }
                } else {
                    _trangThaiCam.value = TrangThaiCam.Co(ten.size)
                    _state.update { s ->
                        s.copy(
                            cameras = ten.map { t ->
                                CameraFeed(id = t, name = t, icon = Icons.Filled.Videocam,
                                    accent = SH.Blue, luong = t)
                            },
                            selectedCamera = 0,
                        )
                    }
                }
            }
            .onFailure { e ->
                _trangThaiCam.value = TrangThaiCam.Loi(Go2rtc.dienGiaiLoi(e))
                _state.update { it.copy(cameras = emptyList(), selectedCamera = 0) }
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

    // ---- WiFi ----

    fun rebootWifi() = mqtt.guiLenhWifi("reboot")

    // ---- OTA Updates ----

    fun kiemTraCapNhat(thuCong: Boolean = false) {
        val currentStatus = _otaStatus.value
        if (currentStatus is OtaStatus.Checking || currentStatus is OtaStatus.Downloading) {
            return
        }

        viewModelScope.launch {
            _otaStatus.value = OtaStatus.Checking
            val result = otaManager.checkLatestRelease()
            result.onSuccess { release ->
                if (release != null && otaManager.isNewerVersion(release.tagName, currentVersionName)) {
                    _otaStatus.value = OtaStatus.UpdateAvailable(release)
                    // Hien pop-up thong bao co ban moi
                    _showUpdateDialog.value = true
                } else {
                    if (thuCong) {
                        _otaStatus.value = OtaStatus.UpToDate
                    } else {
                        _otaStatus.value = OtaStatus.Idling
                    }
                }
            }.onFailure { e ->
                if (thuCong) {
                    _otaStatus.value = OtaStatus.Error(e.message ?: "Không thể kết nối đến máy chủ cập nhật")
                } else {
                    _otaStatus.value = OtaStatus.Idling
                }
            }
        }
    }

    fun dongHopThoaiCapNhat() {
        _showUpdateDialog.value = false
    }

    fun batDauCapNhat(release: ReleaseInfo) {
        viewModelScope.launch {
            _otaStatus.value = OtaStatus.Downloading(0, 0, release.sizeBytes)
            try {
                val apkFile = otaManager.downloadApk(release.downloadUrl) { percent, read, total ->
                    _otaStatus.value = OtaStatus.Downloading(percent, read, total)
                }
                // Tai xong -> dong hop thoai va kich hoat trinh cai dat cua Android
                _showUpdateDialog.value = false
                _otaStatus.value = OtaStatus.Idling
                otaManager.installApk(apkFile)
            } catch (e: Exception) {
                _otaStatus.value = OtaStatus.Error("Tải bản cập nhật thất bại: ${e.message}")
            }
        }
    }
}
