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
import vn.smarthome.data.dien.KetQuaGui
import vn.smarthome.data.dien.MqttDien
import vn.smarthome.data.congtac.CongTacState
import vn.smarthome.data.congtac.HangChoCongTac
import vn.smarthome.data.congtac.LenhCongTac
import vn.smarthome.data.congtac.OThietBi
import vn.smarthome.data.camera.CauHinhCamera
import vn.smarthome.data.camera.CauHinhCameraStore
import vn.smarthome.data.camera.Go2rtc
import vn.smarthome.data.camera.TrangThaiCam
import vn.smarthome.data.thoitiet.CauHinhThoiTiet
import vn.smarthome.data.thoitiet.CauHinhThoiTietStore
import vn.smarthome.data.thoitiet.OpenMeteo
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.data.cuktech.CongSacId
import vn.smarthome.data.cuktech.CuktechState
import vn.smarthome.data.server.ServerState
import vn.smarthome.data.wifi.WifiState
import vn.smarthome.data.ota.OtaManager
import vn.smarthome.data.ota.OtaStatus
import vn.smarthome.data.ota.ReleaseInfo
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.giongnoi.CongCuNha
import vn.smarthome.data.giongnoi.CauHinhGiongNoiStore
import vn.smarthome.data.giongnoi.HanhDongNha
import vn.smarthome.data.giongnoi.LoiLenh
import vn.smarthome.data.giongnoi.MucDungNgay
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
/**
 * Doi bao lau truoc khi ket luan cu sac khong nghe lenh bat/tat cong.
 *
 * `settings_refresh_interval` mac dinh ben `cuktech-ble-server` la 10 giay, nen
 * 25 giay la hai nhip ru rang. Ngan hon thi bao oan luc mang cham.
 */
private const val GIAY_CHO_CUKTECH = 25

/**
 * Doi bao lau cho mot lan "noi lai" BLE.
 *
 * Do that ngay 21/11/2026: 42 giay. Tai lieu du an ghi "bat tay nam buoc,
 * 30-60 giay". Lay 90 cho ru - cat o 60 la bao oan dung luc no sap xong.
 */
private const val GIAY_CHO_NOI_LAI = 90

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

    /**
     * Cu sac CUKTECH 10 Ultra doc tu `cuktech/charger/#`.
     *
     * Nguon la `cuktech-ble-server` chay tren serverubuntu: no giu ket noi BLE
     * toi cu sac roi publish len broker. App chi doc - bat/tat cong sac la
     * duong khac (`cuktech/charger/port`), chua dung toi.
     */
    val cuktech: StateFlow<CuktechState> = mqtt.cuktech

    private val _dangNoiLaiCuktech = MutableStateFlow(false)

    /** Dang trong mot lan "noi lai" -> UI khoa nut va ve "Đang nối lại…". */
    val dangNoiLaiCuktech: StateFlow<Boolean> = _dangNoiLaiCuktech.asStateFlow()

    /**
     * Bao `cuktech-ble-server` dung bo giu ket noi BLE roi dung lai tu dau.
     *
     * Gui `{"enabled": false}` roi `{"enabled": true}` vao `cuktech/charger/ble`.
     * Do tren thiet bi that ngay 21/11/2026: bat tay xong mat **42 giay**, nen
     * cho toi [GIAY_CHO_NOI_LAI] giay roi moi ket luan la that bai. Cat som la
     * bao oan trong khi no sap xong.
     */
    fun noiLaiCuktech(baoLoi: (String) -> Unit) {
        if (_dangNoiLaiCuktech.value) return

        fun gui(bat: Boolean): Boolean = when (val kq = mqtt.guiLenhBleCuktech(bat)) {
            is KetQuaGui.ChuaNoiBroker -> {
                baoLoi("Chưa nối được broker nên chưa gửi được lệnh nối lại.")
                false
            }

            is KetQuaGui.Loi -> {
                baoLoi("Không gửi được lệnh nối lại: " + kq.moTa)
                false
            }

            is KetQuaGui.DaGui -> true
        }

        if (!gui(false)) return
        _dangNoiLaiCuktech.value = true

        viewModelScope.launch {
            // Cho supervisor dung han roi moi bao chay lai; bao lien tay thi
            // lenh thu hai den luc no con dang go xuong.
            delay(2000)
            if (!gui(true)) {
                _dangNoiLaiCuktech.value = false
                return@launch
            }

            val hetHan = System.currentTimeMillis() + GIAY_CHO_NOI_LAI * 1000L
            while (System.currentTimeMillis() < hetHan) {
                if (cuktech.value.noiDuoc) {
                    _dangNoiLaiCuktech.value = false
                    return@launch
                }
                delay(1000)
            }
            _dangNoiLaiCuktech.value = false
            baoLoi(
                "Đã bảo máy chủ nối lại nhưng sau $GIAY_CHO_NOI_LAI giây củ sạc vẫn chưa " +
                    "báo nối được. Kiểm tra củ sạc còn cắm điện và còn trong tầm Bluetooth không."
            )
        }
    }

    private val _choCaiDatCuktech = MutableStateFlow<Set<Int>>(emptySet())

    /** PIID dang cho cu sac xac nhan -> UI khoa muc do lai. */
    val choCaiDatCuktech: StateFlow<Set<Int>> = _choCaiDatCuktech.asStateFlow()

    /**
     * Doi mot cai dat cua cu sac roi doi soat.
     *
     * Doi soat tren BANG PIID THO: doc lai dung cai `piid` vua gui va so voi
     * `giaTri`. Mot vong lap lo duoc het sau muc, khong phai viet rieng cho
     * tung truong - va quan trong hon, no so dung thu minh vua gui chu khong
     * phai mot truong da dich co the trung nhau.
     */
    fun datCaiDatCuktech(piid: Int, giaTri: Int, baoLoi: (String) -> Unit) {
        if (piid in _choCaiDatCuktech.value) return

        when (val kq = mqtt.guiCaiDatCuktech(piid, giaTri)) {
            is KetQuaGui.ChuaNoiBroker -> {
                baoLoi("Chưa nối được broker nên chưa gửi được lệnh.")
                return
            }

            is KetQuaGui.Loi -> {
                baoLoi("Không gửi được lệnh: " + kq.moTa)
                return
            }

            is KetQuaGui.DaGui -> Unit
        }

        _choCaiDatCuktech.update { it + piid }

        viewModelScope.launch {
            val hetHan = System.currentTimeMillis() + GIAY_CHO_CUKTECH * 1000L
            while (System.currentTimeMillis() < hetHan) {
                if (cuktech.value.caiDat?.tho?.get(piid) == giaTri) {
                    _choCaiDatCuktech.update { it - piid }
                    return@launch
                }
                delay(500)
            }
            _choCaiDatCuktech.update { it - piid }
            baoLoi(
                "Củ sạc chưa xác nhận đổi cài đặt sau $GIAY_CHO_CUKTECH giây. " +
                    "Lệnh đã lên broker, nhưng thiết bị chưa đổi."
            )
        }
    }

    private val _choCuktech = MutableStateFlow<Map<CongSacId, Boolean>>(emptyMap())

    /** Cong dang cho cu sac xac nhan -> UI ve trang thai "dang gui". */
    val choCuktech: StateFlow<Map<CongSacId, Boolean>> = _choCuktech.asStateFlow()

    /**
     * Bat/tat mot cong cua cu sac, roi DOI SOAT xem no co that su nghe khong.
     *
     * Cu sac khong co topic bao ket qua. Bang chung duy nhat la mat na bit
     * PIID 16 trong `cuktech/charger/settings` doi theo - cham nhat mot nhip
     * lam moi (mac dinh 10 giay ben server). Nen:
     *
     *  - gui xong KHONG to mau nut nhu da xong, ma danh dau "dang cho";
     *  - thay PIID 16 doi dung nhu mong muon thi go co;
     *  - qua [GIAY_CHO_CUKTECH] giay van chua doi thi go co va BAO THAT ra
     *    man hinh. To mau nhu da thanh cong trong khi cu sac khong nghe la
     *    noi doi voi nguoi dung.
     */
    fun datCongSac(cong: CongSacId, bat: Boolean, baoLoi: (String) -> Unit) {
        when (val kq = mqtt.guiLenhCuktech(cong, bat)) {
            is KetQuaGui.ChuaNoiBroker -> {
                baoLoi("Chưa nối được broker nên chưa gửi được lệnh.")
                return
            }

            is KetQuaGui.Loi -> {
                baoLoi("Không gửi được lệnh: " + kq.moTa)
                return
            }

            is KetQuaGui.DaGui -> Unit
        }

        _choCuktech.update { it + (cong to bat) }

        viewModelScope.launch {
            val hetHan = System.currentTimeMillis() + GIAY_CHO_CUKTECH * 1000L
            while (System.currentTimeMillis() < hetHan) {
                if (cuktech.value.caiDat?.congBat?.get(cong) == bat) {
                    _choCuktech.update { it - cong }
                    return@launch
                }
                delay(500)
            }
            _choCuktech.update { it - cong }
            baoLoi(
                "Củ sạc chưa xác nhận ${if (bat) "bật" else "tắt"} cổng ${cong.nhan} " +
                    "sau $GIAY_CHO_CUKTECH giây. Lệnh đã lên broker, nhưng thiết bị chưa đổi."
            )
        }
    }

    /** Trang thai duong truyen toi EMQX/Mosquitto. */
    val ketNoi: StateFlow<KetNoi> = mqtt.ketNoi

    // ---- Cong tac Wi-Fi that (bed1: den Tuya + quat CozyLife) ----

    /**
     * Cong tac THAT, do bridge tren serverubuntu khai qua `congtac/info`.
     *
     * Day la nguon su that duy nhat cho cau hoi "thiet bi nay that hay mau".
     * Khong hard-code o dau trong app: them cong tac thu ba thi sua .env tren
     * server, khong phai build lai APK.
     */
    val congTac: StateFlow<CongTacState> = mqtt.congTac

    private val hangCho = HangChoCongTac()

    /** Phoi ra UI de ve "Dang gui lenh...". Doi moi lan hang cho doi. */
    private val _cho = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val choCongTac: StateFlow<Map<String, Boolean>> = _cho.asStateFlow()

    /**
     * Cau can noi mot lan voi nguoi dung (khong gui duoc lenh, khong phan hoi...).
     *
     * SharedFlow chu khong phai StateFlow vi day la SU KIEN - cung ly do voi
     * [_lenhDieuHuong]: bao "mat ket noi" hai lan lien tiep thi StateFlow nuot
     * lan thu hai.
     */
    private val _thongBao = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val thongBao: SharedFlow<String> = _thongBao.asSharedFlow()

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

    /** Muc dung 30 ngay gan nhat cua tro ly giong noi. */
    val mucDungGiongNoi: StateFlow<List<MucDungNgay>>
        get() = troLy.mucDung

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
        // Mot chieu duy nhat: broker -> _state.rooms. Khong man nao phai doi
        // cach doc trang thai bat/tat; chung chi nhan them OThietBi de biet ve
        // dau gach "—" hay ve Switch.
        viewModelScope.launch {
            mqtt.congTac.collect { ct -> thuTrangThaiThat(ct) }
        }
        // Dong ho het gio cho hang cho. Quet moi giay la du min: nguoi dung
        // khong phan biet duoc 6,0 voi 6,9 giay, ma mot coroutine moi lenh thi
        // phai tu quan ly huy bo.
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                quetQuaHan()
            }
        }
    }

    /** Gop trang thai that tu broker vao `rooms`, va go hang cho neu khop ma lenh. */
    private fun thuTrangThaiThat(ct: CongTacState) {
        var doiCho = false
        ct.danhSach.forEach { tb ->
            val tt = ct.trangThai[tb.ma] ?: return@forEach
            if (hangCho.nhanTrangThai(tb.ma, tt.traLoiId)) doiCho = true
        }
        if (doiCho) dongBoCho()

        _state.update { s ->
            s.copy(
                rooms = s.rooms.map { p ->
                    var r = p
                    // `?.bat?.let`: bat == null thi KHONG ghi gi, giu gia tri
                    // biet duoc lan cuoi. Nhung OThietBi luc do la bat=null nen
                    // man hinh ve "—" chu khong ve gia tri cu. Hai thu tach
                    // nhau co chu dinh: `rooms` la "lan cuoi biet duoc",
                    // OThietBi la "co duoc phep noi ra khong".
                    ct.trangThaiCua(p.id, CongCuNha.DEN)?.bat?.let { bat ->
                        val doiSangBat = !r.lightOn && bat
                        r = r.copy(
                            lightOn = bat,
                            lightBrightness = if (doiSangBat && (r.lightBrightness <= 25 || r.mode == RoomMode.SLEEP)) 100 else r.lightBrightness,
                            mode = if (doiSangBat && (r.mode == RoomMode.SLEEP || r.mode == RoomMode.AWAY)) RoomMode.CUSTOM else r.mode,
                        )
                    }
                    ct.trangThaiCua(p.id, CongCuNha.QUAT)?.bat?.let { r = r.copy(fanOn = it) }
                    r
                },
            )
        }
    }

    private fun quetQuaHan() {
        val het = hangCho.quaHan(System.currentTimeMillis())
        if (het.isEmpty()) return
        dongBoCho()
        val ct = congTac.value
        het.forEach { ma ->
            val ten = ct.danhSach.firstOrNull { it.ma == ma }?.ten ?: ma
            _thongBao.tryEmit(
                "$ten không phản hồi sau ${HangChoCongTac.GIAY_CHO_MAC_DINH} giây. " +
                    "Không rõ lệnh đã chạy chưa.",
            )
        }
    }

    private fun dongBoCho() {
        _cho.value = hangCho.anhChup()
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

    fun toggleLight(roomId: String) {
        gatCongTac(roomId, CongCuNha.DEN, dao = true, bat = false,
            nguon = LenhCongTac.NGUON_MAN_HINH)
    }

    fun setLightBrightness(roomId: String, brightness: Int) {
        val b = brightness.coerceIn(10, 100)
        editRoom(roomId) {
            it.copy(lightOn = true, lightBrightness = b, mode = RoomMode.CUSTOM)
        }
    }

    fun toggleFan(roomId: String) {
        gatCongTac(roomId, CongCuNha.QUAT, dao = true, bat = false,
            nguon = LenhCongTac.NGUON_MAN_HINH)
    }

    fun setFanSpeed(roomId: String, speed: Int) {
        val sp = speed.coerceIn(1, 3)
        editRoom(roomId) {
            it.copy(fanOn = true, fanSpeed = sp, mode = RoomMode.CUSTOM)
        }
        val ct = congTac.value
        val tb = ct.tim(roomId, CongCuNha.QUAT)
        val tt = tb?.let { ct.trangThai[it.ma] }
        if (tb != null && ct.bridgeSong && tt?.online == true && tt.bat != true) {
            gatCongTac(roomId, CongCuNha.QUAT, dao = false, bat = true, nguon = LenhCongTac.NGUON_MAN_HINH)
        }
    }

    fun toggleTv(roomId: String) = editRoom(roomId) { it.copy(tvOn = !it.tvOn) }

    fun datTv(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(tvOn = bat) }

    fun toggleAc(roomId: String) = editRoom(roomId) { it.copy(acOn = !it.acOn) }

    /** Kep trong 16..30 do C - ngoai khoang nay dieu hoa dan dung khong nhan. */
    fun changeAcTemp(roomId: String, delta: Int) = editRoom(roomId) {
        it.copy(acOn = true, acTemp = (it.acTemp + delta).coerceIn(16, 30))
    }

    fun toggleCurtain(roomId: String) = editRoom(roomId) {
        it.copy(curtainOpen = if (it.curtainOpen > 0) 0 else 100)
    }

    fun setCurtain(roomId: String, openPercent: Int) = editRoom(roomId) {
        it.copy(curtainOpen = openPercent.coerceIn(0, 100))
    }

    fun setHoodSpeed(roomId: String, speed: Int) = editRoom(roomId) {
        it.copy(fanOn = true, hoodSpeed = speed.coerceIn(1, 3))
    }

    fun toggleStove(roomId: String) = editRoom(roomId) { it.copy(stoveOn = !it.stoveOn) }

    fun datBepTu(roomId: String, bat: Boolean) = editRoom(roomId) { it.copy(stoveOn = bat) }

    fun changeStoveLevel(roomId: String, delta: Int) = editRoom(roomId) {
        it.copy(stoveOn = true, stoveLevel = (it.stoveLevel + delta).coerceIn(1, 9))
    }

    fun setGasAlert(roomId: String, alert: Boolean) = editRoom(roomId) { r ->
        if (alert) {
            r.copy(gasAlert = true, gasPpm = 485, fanOn = true, hoodSpeed = 3)
        } else {
            r.copy(gasAlert = false, gasPpm = 18)
        }
    }

    fun setMode(roomId: String, mode: RoomMode) {
        val ct = congTac.value
        val denThat = ct.laThat(roomId, CongCuNha.DEN) &&
            ct.bridgeSong && (ct.trangThaiCua(roomId, CongCuNha.DEN)?.online == true)
        val quatThat = ct.laThat(roomId, CongCuNha.QUAT) &&
            ct.bridgeSong && (ct.trangThaiCua(roomId, CongCuNha.QUAT)?.online == true)
        editRoom(roomId) { r ->
            if (roomId == "living") {
                when (mode) {
                    RoomMode.HOME -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 100,
                        acOn = true,
                        acTemp = 24,
                        curtainOpen = 100,
                        tvOn = false,
                    )
                    RoomMode.SLEEP -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 20,
                        acOn = true,
                        acTemp = 23,
                        curtainOpen = 0,
                        tvOn = true,
                    )
                    RoomMode.CUSTOM -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 55,
                        acOn = true,
                        acTemp = 25,
                        curtainOpen = 50,
                        tvOn = false,
                    )
                    RoomMode.AWAY -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else false,
                        fanOn = if (quatThat) r.fanOn else false,
                        acOn = false,
                        tvOn = false,
                        curtainOpen = 0,
                    )
                }
            } else if (roomId == "kitchen") {
                when (mode) {
                    // Nau an: den 100%, hut mui muc 2, bep tu muc 5
                    RoomMode.HOME -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 100,
                        fanOn = if (quatThat) r.fanOn else true,
                        hoodSpeed = 2,
                        stoveOn = true,
                        stoveLevel = 5,
                    )
                    // An toi: den am ap 55%, tat hut mui & bep tu
                    RoomMode.CUSTOM -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 55,
                        fanOn = if (quatThat) r.fanOn else false,
                        stoveOn = false,
                    )
                    // Don dep: den 100%, hut mui muc 1, tat bep tu
                    RoomMode.SLEEP -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 100,
                        fanOn = if (quatThat) r.fanOn else true,
                        hoodSpeed = 1,
                        stoveOn = false,
                    )
                    // Vang nha: tat toan bo thiet bi, giu giam sat Gas 24/7
                    RoomMode.AWAY -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else false,
                        fanOn = if (quatThat) r.fanOn else false,
                        stoveOn = false,
                    )
                }
            } else {
                when (mode) {
                    RoomMode.SLEEP -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 20,
                        fanOn = if (quatThat) r.fanOn else true,
                        fanSpeed = 1,
                        acOn = true,
                        acTemp = 26,
                    )
                    RoomMode.HOME -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else true,
                        lightBrightness = 100,
                        fanOn = if (quatThat) r.fanOn else false,
                        acOn = true,
                        acTemp = 25,
                    )
                    RoomMode.AWAY -> r.copy(
                        mode = mode,
                        lightOn = if (denThat) r.lightOn else false,
                        fanOn = if (quatThat) r.fanOn else false,
                        acOn = false,
                    )
                    RoomMode.CUSTOM -> r.copy(mode = mode)
                }
            }
        }
    }

    fun toggleSchedule(roomId: String) = editRoom(roomId) { it.copy(scheduleOn = !it.scheduleOn) }

    private fun editRoom(roomId: String, block: (RoomUi) -> RoomUi) = _state.update { s ->
        s.copy(rooms = s.rooms.map { if (it.id == roomId) block(it) else it })
    }

    // ---- Dat thang trang thai (giong noi dung, khong phai dao) ----
    //
    // Nut tren man hinh thi DAO la dung: nguoi ta nhin thay cong tac roi moi
    // bam. Giong noi thi khong: "bat den phong khach" luc den dang bat ma lai
    // dao thanh tat la lam nguoc y nguoi noi.

    fun datDen(roomId: String, bat: Boolean): String? =
        gatCongTac(roomId, CongCuNha.DEN, dao = false, bat = bat,
            nguon = LenhCongTac.NGUON_GIONG_NOI)

    fun datQuat(roomId: String, bat: Boolean): String? =
        gatCongTac(roomId, CongCuNha.QUAT, dao = false, bat = bat,
            nguon = LenhCongTac.NGUON_GIONG_NOI)

    // ---- Cua duy nhat di toi cong tac that ----

    /**
     * Gat mot cong tac. Nut tren man hinh va giong noi deu di qua day.
     *
     * Thiet bi MAU thi doi `_state` ngay nhu truoc, het. Thiet bi THAT thi
     * dung lai TRUOC KHI publish o bon ca - vi gui mot lenh ma biet truoc no
     * vo nghia thi tha noi thang con hon de nguoi dung nhin cong tac mo roi
     * tu doan. Tra ve null khi thanh cong (hoac da gui len broker), tra cau
     * loi tieng Viet khi bi chan.
     */
    private fun gatCongTac(
        roomId: String,
        thietBi: String,
        dao: Boolean,
        bat: Boolean,
        nguon: String,
    ): String? {
        val tuManHinh = nguon == LenhCongTac.NGUON_MAN_HINH
        fun capNhatCucBo() {
            editRoom(roomId) { r ->
                val hienTai = if (thietBi == CongCuNha.DEN) r.lightOn else r.fanOn
                val moi = if (dao) !hienTai else bat
                if (thietBi == CongCuNha.DEN) {
                    r.copy(
                        lightOn = moi,
                        lightBrightness = if (moi && r.lightBrightness <= 25) 100 else r.lightBrightness,
                        mode = if (tuManHinh) RoomMode.CUSTOM else r.mode,
                    )
                } else {
                    r.copy(
                        fanOn = moi,
                        mode = if (tuManHinh) RoomMode.CUSTOM else r.mode,
                    )
                }
            }
        }

        val ct = congTac.value
        val tb = ct.tim(roomId, thietBi)
        if (tb == null) {
            // Thiet bi mau: y nguyen hanh vi cu.
            capNhatCucBo()
            return null
        }

        val tt = ct.trangThai[tb.ma]
        if (!ct.bridgeSong) {
            if (tuManHinh) capNhatCucBo()
            val loi = "Bộ nối công tắc chưa chạy trên máy chủ nên chưa gửi được lệnh."
            _thongBao.tryEmit(loi)
            return loi
        }
        if (tt == null || !tt.online) {
            if (tuManHinh) capNhatCucBo()
            val loi = "${tb.ten} đang mất kết nối" + (tt?.lyDoLoi?.let { " ($it)" } ?: "") + "."
            _thongBao.tryEmit(loi)
            return loi
        }
        // Dao ma khong biet dang o the nao thi KHONG duoc doan. Doan o day la
        // "bam bat thi no tat" - rat kho chiu va rat kho tai hien.
        if (dao && tt.bat == null) {
            if (tuManHinh) capNhatCucBo()
            val loi = "Chưa biết ${tb.ten} đang bật hay tắt nên không đảo được. " +
                "Hãy nói rõ bật hoặc tắt."
            _thongBao.tryEmit(loi)
            return loi
        }

        val mong = if (dao) !tt.bat!! else bat
        return when (val kq = mqtt.guiLenh(roomId, thietBi, if (mong) "ON" else "OFF", nguon = nguon)) {
            is KetQuaGui.DaGui -> {
                hangCho.ganh(tb.ma, mong, kq.id, System.currentTimeMillis())
                dongBoCho()
                if (tuManHinh) {
                    editRoom(roomId) { r ->
                        if (thietBi == CongCuNha.DEN) {
                            r.copy(
                                lightBrightness = if (mong && r.lightBrightness <= 25) 100 else r.lightBrightness,
                                mode = RoomMode.CUSTOM,
                            )
                        } else {
                            r.copy(mode = RoomMode.CUSTOM)
                        }
                    }
                }
                null
            }
            KetQuaGui.ChuaNoiBroker -> {
                if (tuManHinh) capNhatCucBo()
                val loi = "Chưa nối broker MQTT nên chưa gửi được lệnh. Kiểm tra Cài đặt."
                _thongBao.tryEmit(loi)
                loi
            }
            is KetQuaGui.Loi -> {
                if (tuManHinh) capNhatCucBo()
                val loi = "Không gửi được lệnh: ${kq.moTa}"
                _thongBao.tryEmit(loi)
                loi
            }
        }
    }

    /**
     * Mot o cong tac can gi de ve. Man hinh goi ham nay thay vi tu suy tu
     * `rooms` - de luat "khong bia trang thai" nam dung mot cho.
     *
     * Nhan [ct] va [cho] la THAM SO chu khong tu doc `congTac.value` ben
     * trong: ham nay duoc goi tu ham @Composable, va Compose chi ve lai khi
     * no THAY mot State duoc doc. Doc `.value` o day thi noi goi khong dang
     * ky theo doi gi ca, va "Dang gui lenh..." se khong bao gio hien ra cho
     * toi khi co thu khac tinh co lam ve lai man hinh.
     */
    fun oThietBi(
        roomId: String,
        thietBi: String,
        ct: CongTacState,
        cho: Map<String, Boolean>,
    ): OThietBi {
        val tb = ct.tim(roomId, thietBi) ?: run {
            val p = _state.value.rooms.firstOrNull { it.id == roomId }
            val b = if (thietBi == CongCuNha.DEN) p?.lightOn else p?.fanOn
            return OThietBi.Mau(b ?: false)
        }
        val tt = ct.trangThai[tb.ma]
        val mong = cho[tb.ma]
        return OThietBi.That(
            ten = tb.ten,
            bridgeSong = ct.bridgeSong,
            online = tt?.online ?: false,
            bat = tt?.bat,
            dangCho = mong != null,
            mong = mong,
            lyDoLoi = tt?.lyDoLoi,
        )
    }

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

        override fun datDen(phong: String, bat: Boolean) {
            this@HomeViewModel.datDen(phong, bat)?.let { throw LoiLenh(it) }
        }

        override fun datQuat(phong: String, bat: Boolean) {
            this@HomeViewModel.datQuat(phong, bat)?.let { throw LoiLenh(it) }
        }

        override fun datDieuHoa(phong: String, bat: Boolean) =
            this@HomeViewModel.datDieuHoa(phong, bat)

        override fun datLichHen(phong: String, bat: Boolean) =
            this@HomeViewModel.datLichHen(phong, bat)

        override fun datTv(phong: String, bat: Boolean) =
            this@HomeViewModel.datTv(phong, bat)

        override fun datRem(phong: String, mo: Boolean) {
            this@HomeViewModel.setCurtain(phong, if (mo) 100 else 0)
        }

        override fun datBepTu(phong: String, bat: Boolean) =
            this@HomeViewModel.datBepTu(phong, bat)

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

        override fun laThat(phong: String, thietBi: String): Boolean =
            congTac.value.laThat(phong, thietBi)

        override fun thietBiThat(): List<String> = congTac.value.tenThietBiThat()

        override fun dangBat(phong: String, thietBi: String): Boolean? {
            if (phong == "nhanh") {
                return _state.value.quick.firstOrNull { it.id == thietBi }?.on
            }
            // Thiet bi that: su that nam o broker, khong nam o man hinh. Va
            // mat ket noi (hoac bridge tat) thi tra null chu khong tra gia
            // tri cuoi cung - de `dao` biet duong ma tu choi thay vi doan.
            val ct = congTac.value
            if (ct.laThat(phong, thietBi)) {
                if (!ct.bridgeSong) return null
                val tt = ct.trangThaiCua(phong, thietBi)
                return if (tt?.online == true) tt.bat else null
            }
            val p = _state.value.rooms.firstOrNull { it.id == phong } ?: return null
            return when (thietBi) {
                "den" -> p.lightOn
                "quat" -> p.fanOn
                "dieu_hoa" -> p.acOn
                "tivi" -> p.tvOn
                "rem" -> p.curtainOpen > 0
                "bep_tu" -> p.stoveOn
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
            "cong_tac" -> jsonCongTac()
            "trang_thai_nha" -> jsonTrangThaiNha()
            "dien" -> jsonDien()
            "server" -> jsonServer()
            "wifi" -> jsonWifi()
            "thoi_tiet" -> jsonThoiTiet()
            "lich_am" -> jsonLichAm()
            else -> chuaCoSo("Không biết mục này.")
        }

        /**
         * Anh chup nay di THANG vao system_instruction, nen bia o day la
         * Gemini noi sai suot ca phien. Thiet bi that ma khong ro trang thai
         * phai in "khong ro", khong duoc in "tat".
         */
        override fun chupTrangThai(): String = _state.value.rooms.joinToString("\n") { p ->
            if (p.id == "kitchen") {
                val cheDoBep = when (p.mode) {
                    RoomMode.HOME -> "Nấu ăn"
                    RoomMode.CUSTOM -> "Ăn tối"
                    RoomMode.SLEEP -> "Dọn dẹp"
                    RoomMode.AWAY -> "Vắng nhà"
                }
                val gasNote = if (p.gasAlert) "cảnh báo rò rỉ ${p.gasPpm} ppm" else "an toàn ${p.gasPpm} ppm"
                "- ${p.name}: đèn ${theNao(p.id, CongCuNha.DEN, p.lightOn)} (${p.lightBrightness}%), " +
                    "máy hút mùi ${theNao(p.id, CongCuNha.QUAT, p.fanOn)} (cấp ${p.hoodSpeed}), " +
                    "bếp từ ${batTat(p.stoveOn)} (mức ${p.stoveLevel}), " +
                    "cảm biến Gas $gasNote, chế độ $cheDoBep"
            } else {
                val extra = if (p.id == "living") {
                    ", TV ${batTat(p.tvOn)}, rèm mở ${p.curtainOpen}%"
                } else {
                    ""
                }
                "- ${p.name}: đèn ${theNao(p.id, CongCuNha.DEN, p.lightOn)}, " +
                    "quạt ${theNao(p.id, CongCuNha.QUAT, p.fanOn)}, " +
                    "điều hòa ${batTat(p.acOn)} ${p.acTemp} độ$extra, chế độ ${p.mode.label}"
            }
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

    /** Mau thi lay tu man hinh; that thi lay tu broker, khong ro thi noi khong ro. */
    private fun theNao(phong: String, thietBi: String, mau: Boolean): String {
        val ct = congTac.value
        if (!ct.laThat(phong, thietBi)) return batTat(mau)
        if (!ct.bridgeSong) return "không rõ (bộ nối chưa chạy)"
        val tt = ct.trangThaiCua(phong, thietBi) ?: return "không rõ"
        if (!tt.online) return "không rõ (mất kết nối)"
        return tt.bat?.let { batTat(it) } ?: "không rõ"
    }

    /**
     * `congtac/#` cho giong noi.
     *
     * Tach khoi `trang_thai_nha` vi muc do tron that lan mau - cau hoi "den
     * phong ngu 1 co dang sang that khong" ma tra loi bang so mau la noi doi.
     */
    private fun jsonCongTac(): JSONObject {
        val ct = congTac.value
        if (ct.danhSach.isEmpty()) {
            return chuaCoSo(
                "Chưa nhận được gì từ topic congtac/# — bộ nối trên máy chủ chưa chạy.",
            )
        }
        val ds = JSONArray()
        ct.danhSach.forEach { tb ->
            val tt = ct.trangThai[tb.ma]
            val online = ct.bridgeSong && (tt?.online == true)
            ds.put(
                JSONObject()
                    .put("ma", tb.ma).put("ten", tb.ten).put("he", tb.he)
                    .put("online", online)
                    // Khong ro thi JSONObject.NULL, khong phai false.
                    .put("bat", if (online && tt?.bat != null) tt.bat else JSONObject.NULL)
                    .put("luc", tt?.luc ?: JSONObject.NULL)
                    .put("loi", tt?.lyDoLoi ?: JSONObject.NULL),
            )
        }
        return JSONObject()
            .put("co_so", true)
            .put("bo_noi", if (ct.bridgeSong) "đang chạy" else "không chạy")
            .put("thiet_bi", ds)
    }

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
        val ct = congTac.value
        val phong = JSONArray()
        _state.value.rooms.forEach { p ->
            val denThat = ct.laThat(p.id, CongCuNha.DEN)
            val quatThat = ct.laThat(p.id, CongCuNha.QUAT)
            val ttDen = ct.trangThaiCua(p.id, CongCuNha.DEN)
            val ttQuat = ct.trangThaiCua(p.id, CongCuNha.QUAT)
            val denVal: Any = if (!denThat) p.lightOn
            else if (ct.bridgeSong && ttDen?.online == true && ttDen.bat != null) ttDen.bat
            else JSONObject.NULL
            val quatVal: Any = if (!quatThat) p.fanOn
            else if (ct.bridgeSong && ttQuat?.online == true && ttQuat.bat != null) ttQuat.bat
            else JSONObject.NULL
            phong.put(
                JSONObject()
                    .put("ma", p.id).put("ten", p.name)
                    .put("den", denVal).put("quat", quatVal)
                    .put("dieu_hoa", p.acOn).put("nhiet_do", p.acTemp)
                    .put("che_do", p.mode.label)
                    .put("lich_hen", p.scheduleOn)
                    .put("gio_hen", p.scheduleTime)
                    // Co theo TUNG thiet bi, khong con mot co toan cuc: tu nay
                    // bed1/den va bed1/quat la that con lai van la mau.
                    .put("den_that", denThat)
                    .put("quat_that", quatThat)
                    .apply {
                        if (p.id == "living") {
                            put("do_sang_den", p.lightBrightness)
                            put("tivi", p.tvOn)
                            put("rem_mo", p.curtainOpen)
                        } else if (p.id == "kitchen") {
                            put("do_sang_den", p.lightBrightness)
                            put("cap_hut_mui", p.hoodSpeed)
                            put("bep_tu", p.stoveOn)
                            put("muc_bep_tu", p.stoveLevel)
                            put("canh_bao_gas", p.gasAlert)
                            put("gas_ppm", p.gasPpm)
                        }
                    },
            )
        }
        val nhanh = JSONArray()
        _state.value.quick.forEach { nhanh.put(JSONObject().put("ma", it.id).put("bat", it.on)) }
        return JSONObject()
            .put("co_so", true)
            .put("phong", phong)
            .put("o_nhanh", nhanh)
            // Khong con co toan cuc - that/mau nay theo tung thiet bi, o tren.
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
