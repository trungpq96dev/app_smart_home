package vn.smarthome.vm

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import vn.smarthome.data.giongnoi.MucDung
import vn.smarthome.data.giongnoi.MucDungNgay
import vn.smarthome.data.giongnoi.MucDungStore
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.smarthome.data.giongnoi.BanSongCong
import vn.smarthome.data.giongnoi.CauHinhGiongNoi
import vn.smarthome.data.giongnoi.CheDoThoai
import vn.smarthome.data.giongnoi.CongCuNha
import vn.smarthome.data.giongnoi.DongThoai
import vn.smarthome.data.giongnoi.GeminiLive
import vn.smarthome.data.giongnoi.HanhDongNha
import vn.smarthome.data.giongnoi.PhatAm
import vn.smarthome.data.giongnoi.PhienGiongNoi
import vn.smarthome.data.giongnoi.ThuAm
import vn.smarthome.data.giongnoi.TrangThaiGiongNoi
import vn.smarthome.data.giongnoi.ViecDaLam

/**
 * Mot phien noi chuyen voi Gemini Live: ghep [GeminiLive] + [ThuAm] + [PhatAm]
 * + [CongCuNha] lai thanh mot thu bat/tat duoc bang mot nut.
 *
 * KHONG tu mo gi trong ham khoi tao. App chi dong mic khi nguoi dung bam nut -
 * va kiem thu Robolectric dung thang `AppShell()`, mo mic o do la treo test.
 */
class TroLyGiongNoi(
    private val ctx: Context,
    private val scope: CoroutineScope,
    private val nha: HanhDongNha,
) {

    private val khoMucDung = MucDungStore(ctx)

    /**
     * Muc dung 30 ngay gan nhat.
     *
     * Phai la StateFlow chu khong phai Flow: [bat] can doc so cua hom nay NGAY
     * LAP TUC de quyet dinh co cho mo phien khong, khong the cho mot lan doc
     * bat dong bo tu dia.
     */
    val mucDung: StateFlow<List<MucDungNgay>> =
        khoMucDung.mucDung.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private fun giayDaDungHomNay(): Int =
        MucDung.cuaNgay(mucDung.value, MucDung.homNay()).giay

    /**
     * So token lon nhat Gemini bao trong phien dang chay.
     *
     * LAY LON NHAT chu khong cong don - xem ghi chu o [GeminiLive.Nghe.mucDung].
     */
    @Volatile
    private var tokenVaoPhien: Long = 0

    @Volatile
    private var tokenRaPhien: Long = 0

    @Volatile
    private var tokenTongPhien: Long = 0

    /** So giay da dung trong ngay TRUOC khi mo phien nay. */
    @Volatile
    private var giayTruocPhien: Int = 0

    private val _phien = MutableStateFlow(PhienGiongNoi())
    val phien: StateFlow<PhienGiongNoi> = _phien.asStateFlow()

    private var live: GeminiLive? = null
    private var thu: ThuAm? = null
    private var phat: PhatAm? = null
    private var cheDoThoai: CheDoThoai? = null

    private var dongHo: Job? = null
    private var canhMic: Job? = null

    private var cauHinh: CauHinhGiongNoi = CauHinhGiongNoi()
    private var congCu: CongCuNha? = null

    /** Moc lan cuoi co ai do noi gi - de dem im lang. */
    @Volatile
    private var mocHoatDong: Long = 0L

    @Volatile
    private var banSongCong: Boolean = false

    /** Bo qua phan tieng con lai cua luot tra loi hien tai sau khi nguoi dung bam ngat loi. */
    @Volatile
    private var boQuaLuotHienTai: Boolean = false

    // ---------------------------------------------------------------

    fun bat(c: CauHinhGiongNoi) {
        if (_phien.value.dangChay) return

        if (!c.coKhoa()) {
            dat(TrangThaiGiongNoi.Loi("Chưa khai báo khóa Gemini. Vào Cài đặt → Trợ lý giọng nói."))
            return
        }

        // Cham tran ngay thi khong mo phien moi. Kiem o day chu khong doi toi
        // luc dang noi: mo duong roi moi cat la da ton mot it token roi.
        if (c.phutMoiNgay > 0) {
            val daDung = giayDaDungHomNay()
            if (daDung >= c.phutMoiNgay * 60) {
                dat(
                    TrangThaiGiongNoi.Loi(
                        "Hôm nay đã nói ${daDung / 60} phút, chạm trần ${c.phutMoiNgay} phút. " +
                            "Đổi trần trong Cài đặt nếu muốn nói tiếp.",
                    ),
                )
                return
            }
            giayTruocPhien = daDung
        } else {
            giayTruocPhien = 0
        }

        tokenVaoPhien = 0
        tokenRaPhien = 0
        tokenTongPhien = 0
        boQuaLuotHienTai = false
        cauHinh = c
        congCu = CongCuNha(nha, guiMqtt = c.guiMqtt)
        _phien.value = PhienGiongNoi(trangThai = TrangThaiGiongNoi.DangNoi)
        mocHoatDong = System.currentTimeMillis()

        val p = PhatAm()
        p.batDau()?.let { tat(it); return }
        phat = p

        val t = ThuAm { dem, so -> live?.guiTieng(dem, so) }
        t.batDau()?.let { tat(it); return }
        thu = t

        // TU_DONG: may khong co bo khu vong phan cung thi bat buoc ban song
        // cong, neu khong Gemini se nghe chinh no noi va tu tra loi minh.
        banSongCong = when (c.banSongCong) {
            BanSongCong.LUON_BAT -> true
            BanSongCong.LUON_TAT -> false
            BanSongCong.TU_DONG -> !t.coKhuVong
        }
        _phien.update { it.copy(banSongCong = banSongCong) }

        cheDoThoai = CheDoThoai(ctx).also { it.bat() }

        val g = GeminiLive(nghe)
        live = g
        g.noi(c, loiDan(), CongCuNha.khaiBao(nha.maPhong()))

        batDongHo()
        if (banSongCong) batCanhMic()
    }

    fun tat(vi: String? = null) {
        // Ghi so TRUOC khi don dep: phai lay giayDaChay cua phien vua chay.
        // Ghi ca khi phien hong giua chung - token da tieu roi thi phai dem,
        // khong dem la tu lua minh ve muc dung.
        val giay = _phien.value.giayDaChay
        if (_phien.value.dangChay) {
            val tv = tokenVaoPhien
            val tr = tokenRaPhien
            val tt = tokenTongPhien
            scope.launch { khoMucDung.ghiPhien(giay, tv, tr, tt) }
        }

        dongHo?.cancel(); dongHo = null
        canhMic?.cancel(); canhMic = null
        thu?.dung(); thu = null
        phat?.dung(); phat = null
        live?.dong(); live = null
        cheDoThoai?.tat(); cheDoThoai = null
        congCu = null
        boQuaLuotHienTai = false
        _phien.update {
            it.copy(
                trangThai = if (vi == null) TrangThaiGiongNoi.Tat else TrangThaiGiongNoi.Loi(vi),
            )
        }
    }

    fun batTat(c: CauHinhGiongNoi) {
        if (_phien.value.dangChay) tat() else bat(c)
    }

    /** Ngat loi tro ly khi dang phat tieng: xoa hang doi loa va mo mic lai ngay. */
    fun ngatLoi() {
        if (_phien.value.trangThai !is TrangThaiGiongNoi.DangTraLoi) return
        boQuaLuotHienTai = true
        phat?.xoaHangDoi()
        if (banSongCong) thu?.khoaMic(false)
        mocHoatDong = System.currentTimeMillis()
        dat(TrangThaiGiongNoi.DangNghe)
    }

    /** Xoa cau bao loi dang hien tren bang thoai. */
    fun xoaLoi() {
        if (_phien.value.trangThai is TrangThaiGiongNoi.Loi) {
            _phien.value = PhienGiongNoi()
        }
    }

    // ---------------------------------------------------------------

    private val nghe = object : GeminiLive.Nghe {

        override fun sanSang() {
            mocHoatDong = System.currentTimeMillis()
            boQuaLuotHienTai = false
            dat(TrangThaiGiongNoi.DangNghe)
        }

        override fun tiengNoi(pcm: ByteArray) {
            if (boQuaLuotHienTai) return
            mocHoatDong = System.currentTimeMillis()
            if (_phien.value.trangThai !is TrangThaiGiongNoi.DangTraLoi) {
                dat(TrangThaiGiongNoi.DangTraLoi)
            }
            phat?.phat(pcm)
        }

        override fun biNgatLoi() {
            boQuaLuotHienTai = false
            phat?.xoaHangDoi()
            dat(TrangThaiGiongNoi.DangNghe)
        }

        override fun hetLuot() {
            mocHoatDong = System.currentTimeMillis()
            boQuaLuotHienTai = false
            dat(TrangThaiGiongNoi.DangNghe)
        }

        override fun loiThoai(cau: String, cuaMinh: Boolean) {
            mocHoatDong = System.currentTimeMillis()
            if (cuaMinh) boQuaLuotHienTai = false
            themThoai(cau, cuaMinh)
        }

        override fun goiCongCu(id: String?, ten: String, args: JSONObject) {
            mocHoatDong = System.currentTimeMillis()
            val cc = congCu ?: return
            val ketQua = cc.chay(ten, args)
            ghiViecDaLam(ten, ketQua)
            live?.traKetQuaCongCu(id, ten, ketQua)
        }

        /**
         * LAY LON NHAT, khong cong don.
         *
         * Tai lieu khong noi ro `usageMetadata` la so cua rieng goi tin nay hay
         * so cong don tu dau phien. Lay lon nhat thi dung neu la so cong don,
         * va la can duoi an toan neu la so tung goi. Cong don ma doan sai thi
         * so bao cao gap hang chuc lan so that - sai kieu do con te hon la
         * khong co so.
         */
        override fun mucDung(vao: Long, ra: Long, tong: Long) {
            if (vao > tokenVaoPhien) tokenVaoPhien = vao
            if (ra > tokenRaPhien) tokenRaPhien = ra
            if (tong > tokenTongPhien) tokenTongPhien = tong
        }

        override fun sapHetPhien(conLaiMs: Long?) {
            // Chi ghi mot dong cho nguoi dung thay; de phien tu dut roi nguoi
            // dung bam lai. Noi lai ngam o day thi Gemini quen het cuoc noi
            // chuyen ma nguoi dung khong hieu vi sao.
            themThoai("(sắp hết phiên, bấm mic để nói tiếp)", cuaMinh = false)
        }

        override fun dut(cau: String?) {
            if (cau == null) scope.launch { tat() } else scope.launch { tat(cau) }
        }
    }

    // ---------------------------------------------------------------

    /**
     * Dem giay, ap tran phien va tu dong dong khi im lang.
     *
     * Day la phan giu hoa don Gemini khoi chay: may tinh bang treo tuong, mot
     * lan quen tat mic la tinh tien ca ngay.
     */
    private fun batDongHo() {
        dongHo = scope.launch {
            val batDau = System.currentTimeMillis()
            while (isActive) {
                delay(1000)
                val giay = ((System.currentTimeMillis() - batDau) / 1000).toInt()
                _phien.update { it.copy(giayDaChay = giay) }

                if (cauHinh.giayToiDaPhien in 1..giay) {
                    tat("Phiên đã chạy ${giay / 60} phút nên tự đóng. Bấm mic để nói tiếp.")
                    return@launch
                }

                // Tran NGAY: cong ca phan da dung tu truoc trong ngay. Day la
                // cai chan that su - tran phien o tren chi chan mot lan noi dai,
                // con cai nay chan tong ca ngay.
                if (cauHinh.phutMoiNgay > 0 &&
                    giayTruocPhien + giay >= cauHinh.phutMoiNgay * 60
                ) {
                    tat(
                        "Đã dùng hết ${cauHinh.phutMoiNgay} phút của hôm nay. " +
                            "Mai nói tiếp, hoặc đổi trần trong Cài đặt.",
                    )
                    return@launch
                }
                val imLang = ((System.currentTimeMillis() - mocHoatDong) / 1000).toInt()
                if (cauHinh.giayImLangTuDong in 1..imLang) {
                    tat()
                    return@launch
                }
            }
        }
    }

    /**
     * Ban song cong: khoa mic trong luc loa con tieng.
     *
     * 50ms mot nhip - du nhanh de khong cat mat dau cau nguoi dung noi ngay
     * sau khi Gemini dut loi.
     */
    private fun batCanhMic() {
        canhMic = scope.launch {
            var dangKhoa = false
            while (isActive) {
                delay(50)
                val canKhoa = phat?.dangPhat() == true
                if (canKhoa != dangKhoa) {
                    dangKhoa = canKhoa
                    thu?.khoaMic(canKhoa)
                    // Bao cho server biet luong tieng vua dut, de no khong doi
                    // mot doan im lang rong roi moi tinh la het luot.
                    if (canKhoa) live?.bangTiengDungLai()
                }
            }
        }
    }

    // ---------------------------------------------------------------

    private fun dat(t: TrangThaiGiongNoi) = _phien.update { it.copy(trangThai = t) }

    private fun themThoai(cau: String, cuaMinh: Boolean) = _phien.update { p ->
        // Live API gui loi thoai theo tung manh nho. Noi tiep vao dong cuoi
        // neu van cung mot nguoi, neu khong bang thoai se thanh mot cot tu roi.
        val ds = p.thoai.toMutableList()
        val cuoi = ds.lastOrNull()
        if (cuoi != null && cuoi.cuaMinh == cuaMinh) {
            ds[ds.lastIndex] = cuoi.copy(cau = (cuoi.cau + cau).take(400))
        } else {
            ds += DongThoai(cuaMinh, cau)
        }
        p.copy(thoai = ds.takeLast(PhienGiongNoi.TOI_DA_DONG_THOAI))
    }

    private fun ghiViecDaLam(ten: String, ketQua: JSONObject) {
        val thanhCong = ketQua.optBoolean("ok")
        val cau = if (thanhCong) ketQua.optString("ket_qua") else ketQua.optString("loi")
        if (cau.isBlank()) return
        // Chi dong "chua noi thiet bi that" voi lenh DIEU KHIEN. Doc so lieu va
        // chuyen man la viec that, dong do o day la noi sai.
        //
        // Tu 21/11/2026 khong dung co toan cuc DemoData.IS_DEMO nua: bed1/den
        // va bed1/quat la that con lai van la mau, nen phai hoi theo TUNG lenh.
        // CongCuNha da tra loi ho bang co `that`; thieu co thi canh bao -
        // huong an toan.
        val chuaThat = thanhCong && ten == CongCuNha.DIEU_KHIEN &&
            !ketQua.optBoolean("that", false)
        _phien.update { p ->
            p.copy(daLam = (p.daLam + ViecDaLam(cau, thanhCong, chuaThat)).takeLast(4))
        }
    }

    /**
     * Loi dan cho Gemini.
     *
     * Khai san so do nha va anh chup trang thai: cau hoi don gian ("den phong
     * khach dang bat khong") tra loi duoc ngay, khong ton mot vong goi ham.
     */
    private fun loiDan(): String = buildString {
        append("Bạn là trợ lý nhà thông minh nói tiếng Việt, đang chạy trên máy tính bảng ")
        append("treo tường trong nhà. Xưng \"mình\", gọi người dùng là \"bạn\". ")
        append("Trả lời thật ngắn - một hoặc hai câu, như nói chuyện chứ không phải đọc văn bản. ")
        append("Không đọc lại mã phòng kiểu bed1, hãy nói tên tiếng Việt.\n\n")

        append("Nhà có các phòng sau (mã - tên):\n")
        nha.maPhong().forEach { ma -> append("- ").append(ma).append(" - ").append(nha.tenPhong(ma)).append("\n") }
        append(
            "\nNgười ta hay gọi phòng bằng nhiều tên khác nhau: \"phòng bố mẹ\", \"phòng lớn\", " +
                "\"phòng con\"... Hãy tự đoán sang mã phòng gần nhất thay vì hỏi lại, " +
                "trừ khi thật sự không đoán được.\n\n",
        )

        append("Trạng thái lúc mở phiên:\n").append(nha.chupTrangThai()).append("\n\n")

        append("Khi người dùng muốn bật/tắt/chỉnh thiết bị, gọi ngay dieu_khien_thiet_bi ")
        append("rồi xác nhận ngắn gọn. Đừng hỏi lại cho chắc với những việc đảo ngược được dễ dàng.\n")
        append("Với hệ thống bơm nước (bơm lên bồn, bơm tăng áp, đèn phụ hộp bơm, xoá lỗi bơm, tắt còi), ")
        append("gọi dieu_khien_thiet_bi với phong = \"bom_nuoc\".\n")
        append("Khi người dùng hỏi bất kỳ con số nào (gồm cả mức nước, lưu lượng, nhiệt độ, lỗi bơm), PHẢI gọi doc_so_lieu trước ")
        append("(với bơm nước thì dùng muc = \"bom_nuoc\"). ")
        append("Tuyệt đối không tự nghĩ ra số. Nếu doc_so_lieu trả về co_so bằng false ")
        append("thì nói thẳng là chưa có số và nói lý do.\n")

        // Danh sach thiet bi that lay tu `congtac/info` (retained), nen luc mo
        // phien la da ve tu lau.
        val that = nha.thietBiThat()
        if (that.isEmpty()) {
            append(
                "\nLƯU Ý QUAN TRỌNG: đèn, quạt, điều hòa, rèm trong nhà này HIỆN CHƯA NỐI " +
                    "thiết bị thật - lệnh chỉ đổi trạng thái hiển thị trên màn hình. " +
                    "Cứ xác nhận bình thường, nhưng nếu người dùng hỏi \"đèn có sáng thật không\" " +
                    "thì phải nói thật là chưa nối thiết bị.\n",
            )
        } else {
            append("\nLƯU Ý QUAN TRỌNG: trong nhà này CHỈ những thiết bị sau là thiết bị thật, ")
            append("lệnh vào chúng sẽ bật tắt đồ vật ngoài đời: ")
            append(that.joinToString(", ")).append(".\n")
            append("Mọi thiết bị khác - đèn, quạt, điều hòa, rèm của các phòng còn lại - CHƯA ")
            append("nối thiết bị thật, lệnh chỉ đổi trạng thái hiển thị trên màn hình. ")
            append("Cứ xác nhận bình thường, nhưng nếu người dùng hỏi có thật không thì nói thẳng.\n")
            append("Với thiết bị thật, lệnh đi qua bộ nối trên máy chủ; gửi xong chưa chắc công ")
            append("tắc đã nhận. Đừng khẳng định \"đèn đã sáng\" - hãy nói \"mình đã gửi lệnh bật\". ")
            append("Muốn biết chắc thì gọi doc_so_lieu với muc = cong_tac.\n")
        }
    }
}
