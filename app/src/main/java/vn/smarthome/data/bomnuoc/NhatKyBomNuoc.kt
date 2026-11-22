package vn.smarthome.data.bomnuoc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

enum class NhomNhatKy(val ma: String, val nhan: String) {
    BOM("pump", "Bơm"),
    NUOC("water", "Nước"),
    CANH_BAO("alarm", "Cảnh báo"),
    HE_THONG("system", "Hệ thống");

    companion object {
        fun tuMa(ma: String): NhomNhatKy = entries.firstOrNull { it.ma == ma } ?: HE_THONG
    }
}

enum class MucNhatKy(val ma: String) {
    THONG_TIN("info"),
    CANH_BAO("warn"),
    LOI("error");

    companion object {
        fun tuMa(ma: String): MucNhatKy = entries.firstOrNull { it.ma == ma } ?: THONG_TIN
    }
}

data class SuKienBom(
    val id: Long,
    val luc: Long,
    val nhom: NhomNhatKy,
    val tieuDe: String,
    val chiTiet: String,
    val muc: MucNhatKy,
)

/**
 * Nhat ky su kien phia may tinh bang, luu gon trong mot file JSON (toi da 100 muc).
 *
 * Firmware `bom_nuoc` khong luu lich su su kien, nen app tu ghi lai moi lan
 * quan sat thay bom, phao, che do hay canh bao thay doi. Lan quan sat dau tien
 * cua moi khoa luon duoc bo qua de khong sinh su kien gia luc moi mo app.
 */
class NhatKyBomNuoc(
    private val file: File,
    private val scope: CoroutineScope,
) {
    companion object {
        const val TOI_DA_MUC = 100
        private const val GIU_NGAY_MS = 90L * 24 * 3600 * 1000
    }

    private val khoaGhi = Mutex()
    private val truoc = mutableMapOf<String, String>()
    private var daGan = false
    private var idTiepTheo = 1L

    private val _danhSach = MutableStateFlow<List<SuKienBom>>(emptyList())
    val danhSach: StateFlow<List<SuKienBom>> = _danhSach.asStateFlow()

    init {
        napTuDia()
    }

    fun theoDoi(state: StateFlow<BomNuocState?>) {
        if (daGan) return
        daGan = true
        scope.launch {
            state.filterNotNull().collect { s ->
                quanSat(s, System.currentTimeMillis())
            }
        }
    }

    /** Cho phep goi truc tiep trong unit test de kiem chung theo tung goi tin. */
    suspend fun quanSat(s: BomNuocState, luc: Long = System.currentTimeMillis()) {
        val moi = mutableListOf<SuKienBom>()

        theoDoiKhoa("tank", if (s.bomChinhDangChay) "1" else "0") { on ->
            val mode = s.cheDoBon.nhan
            val muc = s.mucNuocPhanTram?.let { "mức ${it.toInt()}%" }
                ?: if (s.bonDay) "phao báo đầy" else "phao chưa đầy"
            moi += tao(
                luc,
                NhomNhatKy.BOM,
                if (on == "1") "Bơm lên bồn bắt đầu chạy" else "Bơm lên bồn đã dừng",
                "Chế độ $mode · $muc",
                MucNhatKy.THONG_TIN,
            )
        }

        theoDoiKhoa("boost", if (s.bomApDangChay) "1" else "0") { on ->
            moi += tao(
                luc,
                NhomNhatKy.BOM,
                if (on == "1") "Bơm tăng áp bật" else "Bơm tăng áp tắt",
                "Điều khiển bằng tay",
                MucNhatKy.THONG_TIN,
            )
        }

        theoDoiKhoa("mode", s.tankMode.orEmpty()) { m ->
            if (m.isNotBlank()) {
                moi += tao(
                    luc,
                    NhomNhatKy.HE_THONG,
                    "Đổi chế độ bồn",
                    "Sang ${s.cheDoBon.nhan}",
                    MucNhatKy.THONG_TIN,
                )
            }
        }

        if (s.coNhanhNuocMay) {
            theoDoiKhoa("city", if (s.nuocMayDangChay) "1" else "0") { on ->
                moi += tao(
                    luc,
                    NhomNhatKy.NUOC,
                    if (on == "1") "Nước máy đang vào bồn" else "Nước máy ngừng chảy",
                    "Nhánh nước máy",
                    MucNhatKy.THONG_TIN,
                )
            }
        }

        theoDoiKhoa("full", if (s.bonDay) "1" else "0") { on ->
            moi += tao(
                luc,
                NhomNhatKy.NUOC,
                if (on == "1") "Bồn đã đầy" else "Bồn rời mức đầy",
                if (on == "1") "Phao báo đầy" else "Phao nhả",
                MucNhatKy.THONG_TIN,
            )
        }

        theoDoiKhoa("fault", s.fault.orEmpty()) { f ->
            if (f.isBlank()) {
                moi += tao(
                    luc,
                    NhomNhatKy.CANH_BAO,
                    "Đã xoá lỗi, hệ thống mở khoá",
                    "Sẵn sàng chạy lại",
                    MucNhatKy.THONG_TIN,
                )
            } else {
                val parsed = LoiBomNuoc.phanTich(f)
                moi += tao(
                    luc,
                    NhomNhatKy.CANH_BAO,
                    "Sự cố: ${parsed?.noiDung ?: f}",
                    "Thiết bị đang bị khoá",
                    if (parsed?.mucDo == LoiBomNuoc.MucDo.NGUY_HIEM) MucNhatKy.LOI else MucNhatKy.CANH_BAO,
                )
            }
        }

        if (s.coDenPhu) {
            theoDoiKhoa("den", if (s.denPhuBat) "1" else "0") { on ->
                moi += tao(
                    luc,
                    NhomNhatKy.HE_THONG,
                    if (on == "1") "Đèn phụ bật" else "Đèn phụ tắt",
                    "Trong hộp điện",
                    MucNhatKy.THONG_TIN,
                )
            }
        }

        if (moi.isNotEmpty()) {
            khoaGhi.withLock {
                val capNhat = (moi.reversed() + _danhSach.value).take(TOI_DA_MUC)
                _danhSach.value = capNhat
                luuRaDia(capNhat)
            }
        }
    }

    suspend fun xoaTatCa() {
        khoaGhi.withLock {
            _danhSach.value = emptyList()
            runCatching { if (file.exists()) file.delete() }
        }
    }

    fun datLaiTheoDoi() {
        truoc.clear()
    }

    private inline fun theoDoiKhoa(khoa: String, giaTri: String, khiDoi: (String) -> Unit) {
        val cu = truoc[khoa]
        if (cu == null) {
            truoc[khoa] = giaTri
            return
        }
        if (cu == giaTri) return
        truoc[khoa] = giaTri
        khiDoi(giaTri)
    }

    private fun tao(
        luc: Long,
        nhom: NhomNhatKy,
        tieuDe: String,
        chiTiet: String,
        muc: MucNhatKy,
    ): SuKienBom = SuKienBom(
        id = idTiepTheo++,
        luc = luc,
        nhom = nhom,
        tieuDe = tieuDe,
        chiTiet = chiTiet,
        muc = muc,
    )

    private fun napTuDia() {
        if (!file.exists()) return
        val mocCu = System.currentTimeMillis() - GIU_NGAY_MS
        val loaded = runCatching {
            val arr = JSONArray(file.readText(Charsets.UTF_8))
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val luc = o.optLong("luc", 0L)
                    if (luc <= 0L || luc < mocCu) continue
                    val id = o.optLong("id", 0L).let { if (it > 0) it else idTiepTheo++ }
                    if (id >= idTiepTheo) idTiepTheo = id + 1
                    add(
                        SuKienBom(
                            id = id,
                            luc = luc,
                            nhom = NhomNhatKy.tuMa(o.optString("nhom")),
                            tieuDe = o.optString("tieuDe"),
                            chiTiet = o.optString("chiTiet"),
                            muc = MucNhatKy.tuMa(o.optString("muc")),
                        )
                    )
                }
            }.take(TOI_DA_MUC)
        }.getOrNull() ?: emptyList()

        _danhSach.value = loaded
    }

    private fun luuRaDia(ds: List<SuKienBom>) {
        runCatching {
            file.parentFile?.mkdirs()
            val arr = JSONArray()
            ds.forEach { e ->
                arr.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("luc", e.luc)
                        .put("nhom", e.nhom.ma)
                        .put("tieuDe", e.tieuDe)
                        .put("chiTiet", e.chiTiet)
                        .put("muc", e.muc.ma)
                )
            }
            val tam = File(file.parentFile, file.name + ".tmp")
            tam.writeText(arr.toString(), Charsets.UTF_8)
            if (!tam.renameTo(file)) {
                file.writeText(arr.toString(), Charsets.UTF_8)
                tam.delete()
            }
        }
    }
}
