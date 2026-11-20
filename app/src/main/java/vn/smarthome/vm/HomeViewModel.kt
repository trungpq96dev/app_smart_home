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
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.giongnoi.CauHinhGiongNoiStore
import vn.smarthome.data.giongnoi.HanhDongNha
import vn.smarthome.data.giongnoi.PhienGiongNoi
import vn.smarthome.data.lunar.AmLich
import vn.smarthome.ui.theme.SH
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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
    private val storeGiongNoi = CauHinhGiongNoiStore(app)
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

    // ---- Tro ly giong noi ----

    val cauHinhGiongNoi: StateFlow<CauHinhGiongNoi> = storeGiongNoi.cauHinh
        .stateIn(viewModelScope, SharingStarted.Eagerly, CauHinhGiongNoi())

    /**
     * Lenh doi man hinh do giong noi phat ra.
     *
     * Phai la SharedFlow chu khong phai StateFlow: dieu huong la SU KIEN, noi
     * "mo camera" hai lan lien tiep thi StateFlow nuot lan thu hai vi gia tri
     * khong doi. `replay = 0` de xoay may khong mo lai man cu.
     */
    private val _lenhDieuHuong = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val lenhDieuHuong: SharedFlow<String> = _lenhDieuHuong.asSharedFlow()

    /**
     * Cai nguoi noi chuyen. Khoi tao LUOI (by lazy) va khong mo mic o day -
     * ShellSmokeTest dung thang AppShell() nen moi thu nang trong init deu
     * chay trong Robolectric.
     */
    private val troLy by lazy { TroLyGiongNoi(app, viewModelScope, hanhDongNha) }

    val phienGiongNoi: StateFlow<PhienGiongNoi>
        get() = troLy.phien

    fun batTatGiongNoi() = troLy.batTat(cauHinhGiongNoi.value)

    fun dongGiongNoi() = troLy.tat()

    fun xoaLoiGiongNoi() = troLy.xoaLoi()

    fun luuCauHinhGiongNoi(c: CauHinhGiongNoi) {
        viewModelScope.launch { storeGiongNoi.luu(c) }
    }

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

    // ---- Dat thang trang thai (giong noi dung, khong phai dao) ----
    //
    // Nut tren man hinh thi DAO la dung: nguoi ta nhin thay cong tac roi moi
    // bam. Giong noi thi khong: "bat den phong khach" luc den dang bat ma lai
    // dao thanh tat la lam nguoc y nguoi noi.

    fun datDen(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(lightOn = bat) }

    fun datQuat(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(fanOn = bat) }

    fun datDieuHoa(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(acOn = bat) }

    fun datLichHen(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(scheduleOn = bat) }

    fun datNhietDo(roomId: String, doC: Int): Int {
        val kep = doC.coerceIn(16, 30)
        editRoom(roomId) { it.copy(acTemp = kep) }
        return _state.value.rooms.firstOrNull { it.id == roomId }?.acTemp ?: kep
    }

    fun datONhanh(id: String, bat: Boolean) = _state.update { s ->
        s.copy(quick = s.quick.map { if (it.id == id) it.copy(on = bat) else it })
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

    // ---------------------------------------------------------------
    // Cai Gemini thuc su dong vao app.
    //
    // Doc so lieu thi lay tu dung cac StateFlow dang chay that. Chua co so thi
    // tra `co_so: false` kem ly do - khong bao gio bia mot con so, dung nguyen
    // tac cua du an (xem README muc "Vi sao co chip DU LIEU MAU").
    // ---------------------------------------------------------------

    private val hanhDongNha = object : HanhDongNha {

        override fun maPhong(): List<String> = _state.value.rooms.map { it.id }

        override fun tenPhong(ma: String): String? =
            _state.value.rooms.firstOrNull { it.id == ma }?.name

        override fun datDen(phong: String, bat: Boolean) = this@HomeViewModel.datDen(phong, bat)

        override fun datQuat(phong: String, bat: Boolean) = this@HomeViewModel.datQuat(phong, bat)

        override fun datDieuHoa(phong: String, bat: Boolean) =
            this@HomeViewModel.datDieuHoa(phong, bat)

        override fun datLichHen(phong: String, bat: Boolean) =
            this@HomeViewModel.datLichHen(phong, bat)

        override fun datNhietDo(phong: String, doC: Int): Int =
            this@HomeViewModel.datNhietDo(phong, doC)

        override fun chinhNhietDo(phong: String, buoc: Int): Int {
            changeAcTemp(phong, buoc)
            return _state.value.rooms.firstOrNull { it.id == phong }?.acTemp ?: 26
        }

        override fun datCheDo(phong: String, cheDo: String) {
            val m = when (cheDo) {
                "ngu" -> RoomMode.SLEEP
                "nha" -> RoomMode.HOME
                "vang_nha" -> RoomMode.AWAY
                else -> RoomMode.CUSTOM
            }
            setMode(phong, m)
        }

        override fun datONhanh(id: String, bat: Boolean) = this@HomeViewModel.datONhanh(id, bat)

        override fun dangBat(phong: String, thietBi: String): Boolean? {
            if (phong == "nhanh") {
                return _state.value.quick.firstOrNull { it.id == thietBi }?.on
            }
            val p = _state.value.rooms.firstOrNull { it.id == phong } ?: return null
            return when (thietBi) {
                "den" -> p.lightOn
                "quat" -> p.fanOn
                "dieu_hoa" -> p.acOn
                "lich_hen" -> p.scheduleOn
                else -> null
            }
        }

        override fun moManHinh(dich: String): Boolean {
            val hopLe = dich in DICH_HOP_LE ||
                (dich.startsWith("phong:") && tenPhong(dich.removePrefix("phong:")) != null)
            if (!hopLe) return false
            // tryEmit chu khong phai emit: day duoc goi tu luong cua OkHttp,
            // khong phai coroutine. extraBufferCapacity=4 nen khong mat lenh.
            return _lenhDieuHuong.tryEmit(dich)
        }

        override fun docSoLieu(muc: String): JSONObject = when (muc) {
            "trang_thai_nha" -> jsonTrangThaiNha()
            "dien" -> jsonDien()
            "server" -> jsonServer()
            "wifi" -> jsonWifi()
            "thoi_tiet" -> jsonThoiTiet()
            "lich_am" -> jsonLichAm()
            else -> chuaCoSo("Không biết mục này.")
        }

        override fun chupTrangThai(): String = _state.value.rooms.joinToString("\n") { p ->
            "- ${p.name}: đèn ${batTat(p.lightOn)}, quạt ${batTat(p.fanOn)}, " +
                "điều hòa ${batTat(p.acOn)} ${p.acTemp} độ, chế độ ${p.mode.label}"
        }

        override fun guiMqtt(
            phong: String,
            thietBi: String,
            hanhDong: String,
            giaTri: String?,
        ): Boolean {
            val o = JSONObject()
                .put("device", if (phong == "nhanh") "nhanh_$thietBi" else phong + "_" + thietBi)
                .put("room", phong)
                .put("action", hanhDong.uppercase())
                .put("source", "voice")
            if (giaTri != null) o.put("value", giaTri)
            return mqtt.guiLenhDieuKhien(o.toString())
        }
    }

    private fun batTat(b: Boolean) = if (b) "bật" else "tắt"

    companion object {
        /**
         * Nhung dich `mo_man_hinh` nhan.
         *
         * Phai khop voi nhanh `when` trong AppShell. Them man moi thi them o CA
         * HAI cho - de lech thi giong noi bao "da mo" ma man hinh khong doi.
         */
        val DICH_HOP_LE = listOf(
            "trang_chu", "thiet_bi", "camera", "cam_bien", "tu_dong_hoa",
            "cai_dat", "server", "thong_ke", "tien_ich", "chi_tiet_dien", "wifi",
        )
    }

    private fun chuaCoSo(lyDo: String) = JSONObject().put("co_so", false).put("ly_do", lyDo)

    private fun jsonTrangThaiNha(): JSONObject {
        val phong = JSONArray()
        _state.value.rooms.forEach { p ->
            phong.put(
                JSONObject()
                    .put("ma", p.id).put("ten", p.name)
                    .put("den", p.lightOn).put("quat", p.fanOn)
                    .put("dieu_hoa", p.acOn).put("nhiet_do", p.acTemp)
                    .put("che_do", p.mode.label)
                    .put("lich_hen", p.scheduleOn)
                    .put("gio_hen", p.scheduleTime),
            )
        }
        val nhanh = JSONArray()
        _state.value.quick.forEach { nhanh.put(JSONObject().put("ma", it.id).put("bat", it.on)) }
        return JSONObject()
            .put("co_so", true)
            .put("phong", phong)
            .put("o_nhanh", nhanh)
            .put("thiet_bi_that", !DemoData.IS_DEMO)
    }

    private fun jsonDien(): JSONObject {
        val d = dien.value
        if (!d.coSoLieu) {
            return chuaCoSo(
                when (val k = ketNoi.value) {
                    is KetNoi.Loi -> k.moTa
                    KetNoi.ChuaCauHinh -> "Chưa khai báo broker MQTT trong Cài đặt."
                    else -> "Đang chờ số điện đầu tiên từ broker."
                },
            )
        }
        val o = JSONObject().put("co_so", true).put("nguon", "EVN qua MQTT")
        d.ngaySanLuong?.let { o.put("san_luong_ngay_gan_nhat_kwh", it) }
        d.ngay?.let { o.put("ngay_gan_nhat", it) }
        d.thangSanLuong?.let { o.put("san_luong_thang_nay_kwh", it) }
        d.thangTien?.let { o.put("tien_thang_nay_dong", it) }
        d.congToChiSo?.let { o.put("chi_so_cong_to", it) }
        d.congNoSoTien?.let { o.put("cong_no_dong", it) }
        d.tongKwh(7)?.let { o.put("tong_7_ngay_kwh", it) }
        d.tongKwh(30)?.let { o.put("tong_30_ngay_kwh", it) }
        d.capNhat?.let { o.put("cap_nhat_luc", it) }
        return o
    }

    private fun jsonServer(): JSONObject {
        val s = server.value
        if (!s.isRealData) return chuaCoSo("Chưa nhận được số từ topic server/# của serverubuntu.")
        return JSONObject()
            .put("co_so", true)
            .put("ten_may", s.info.hostname)
            .put("he_dieu_hanh", s.info.os)
            .put("cpu_phan_tram", s.metric.cpuPercent)
            .put("ram_phan_tram", s.metric.ramPercent)
            .put("ram_dung_gb", s.metric.ramUsedGb)
            .put("ram_tong_gb", s.info.ramTotalGb)
            .put("dia_phan_tram", s.metric.diskPercent)
            .put("dia_dung_gb", s.metric.diskUsedGb)
            .put("dia_tong_gb", s.info.diskTotalGb)
            .put("nhiet_do_cpu", s.metric.cpuTempC ?: JSONObject.NULL)
            .put("chay_lien_tuc_ngay", s.info.uptimeDays)
            .put("so_container_docker", s.containers.size)
    }

    private fun jsonWifi(): JSONObject {
        val w = wifi.value
        if (!w.isRealData) return chuaCoSo("Chưa nhận được số từ topic wifi/# - wifi_collector chưa chạy.")
        return JSONObject()
            .put("co_so", true)
            .put("ten_mang", w.info.ssid)
            .put("dang_noi", w.info.isConnected)
            .put("tin_hieu_dbm", w.info.signalDbm)
            .put("tai_xuong_mbps", w.info.downloadMbps)
            .put("tai_len_mbps", w.info.uploadMbps)
            .put("so_thiet_bi_dang_noi", w.devices.count { it.isOnline })
    }

    private fun jsonThoiTiet(): JSONObject = when (val t = thoiTiet.value) {
        is ThoiTietUi.Co -> JSONObject()
            .put("co_so", true)
            .put("dia_diem", t.data.diaDiem)
            .put("nhiet_do", t.data.hienTai.nhietDo)
            .put("cam_giac_nhu", t.data.hienTai.camGiac)
            .put("do_am", t.data.hienTai.doAm)
            .put("gio_kmh", t.data.hienTai.gio)
            .put("cap_nhat_luc", t.data.gioCapNhat)

        is ThoiTietUi.Loi -> chuaCoSo(t.moTa)
        ThoiTietUi.DangTai -> chuaCoSo("Đang tải thời tiết từ Open-Meteo.")
    }

    /** Lich am tinh tai cho, khong can mang - nen muc nay luon co so. */
    private fun jsonLichAm(): JSONObject {
        val hom = LocalDate.now()
        val am = AmLich.tuDuongLich(hom.dayOfMonth, hom.monthValue, hom.year)
        val cc = AmLich.canChi(am)
        return JSONObject()
            .put("co_so", true)
            .put("duong_lich", "${hom.dayOfMonth}/${hom.monthValue}/${hom.year}")
            .put("am_lich", "${am.ngay}/${am.thang}" + if (am.nhuan) " nhuận" else "")
            .put("nam_am", am.nam)
            .put("can_chi_ngay", cc.ngay)
            .put("can_chi_thang", cc.thang)
            .put("can_chi_nam", cc.nam)
            .put("gio_hoang_dao", JSONArray().apply { AmLich.gioHoangDao(cc.chiNgay).forEach { put(it) } })
            .put("su_kien", AmLich.suKien(hom.dayOfMonth, hom.monthValue, am) ?: JSONObject.NULL)
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
