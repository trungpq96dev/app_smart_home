package vn.smarthome.data.congtac

import org.json.JSONArray
import org.json.JSONObject

/**
 * Danh sach topic MQTT cua hai cong tac Wi-Fi phong ngu 1.
 *
 * Nguon: `scripts/congtac_bridge/` chay Docker tren serverubuntu. Bridge noi
 * chuyen voi cong tac Tuya (TCP 6668) va CozyLife (TCP 5555) trong LAN roi
 * publish len day. App chi DOC nhanh nay; lenh thi ghi vao `smarthome/control`
 * (xem [vn.smarthome.data.dien.DieuKhienTopic]).
 *
 * Vi sao trang thai tung may la `congtac/thietbi/<ma>` chu khong phai
 * `congtac/trangthai/<ma>`: de [TRANG_THAI] - mot chuoi thuong "online" -
 * khong dung tien to voi mot nhanh JSON. Trung tien to thi [CongTacState.gop]
 * phai nho "so khop chinh xac truoc, tien to sau", va cai ngay ai do sua nham
 * thu tu thi LWT bi dem di JSONObject(...) roi nuot loi lang le.
 */
object CongTacTopic {
    const val GOC = "congtac/#"
    const val INFO = "congtac/info"
    const val TRANG_THAI = "congtac/trangthai"
    const val TIEN_TO_TB = "congtac/thietbi/"
    const val CAP_NHAT = "congtac/capnhat"
    const val KET_QUA = "congtac/cmd/result"
}

/**
 * Mot cong tac THAT, do bridge khai qua `congtac/info`.
 *
 * App khong bao gio hard-code danh sach nay. Them cong tac thu ba thi sua
 * `.env` tren server, khong phai build lai APK.
 */
data class CongTac(
    /** `<phong>_<thietBi>`, vi du "bed1_den". Khop chuoi HomeViewModel sinh ra. */
    val ma: String,
    /** Khop [vn.smarthome.data.RoomUi.id]. */
    val phong: String,
    /** Khop hang so cua CongCuNha: "den", "quat". */
    val thietBi: String,
    /** Ten tieng Viet co dau, de hien va de doc len. */
    val ten: String,
    /** "tuya" | "cozylife". Chi de hien trong man chan doan. */
    val he: String,
)

/**
 * Trang thai mot cong tac, doc tu `congtac/thietbi/<ma>`.
 *
 * [bat] nullable CO CHU DINH: bridge thay thiet bi nhung chua doc duoc cong
 * tac (mat dien, timeout) thi phai la null. Dat false o day la bia - man hinh
 * se ve cong tac o the TAT trong khi den dang sang. Day la cho nguyen tac
 * "khong bia so" cua du an song hoac chet.
 */
data class TrangThaiCongTac(
    val ma: String,
    val online: Boolean,
    val bat: Boolean?,
    val luc: String?,
    /** Chep lai `id` cua lenh bridge vua thuc thi. Dung de go co "dang cho". */
    val traLoiId: String?,
    val lyDoLoi: String?,
)

/**
 * Trang thai ca nhanh `congtac/#`.
 *
 * Khoi tao RONG, khong lay tu DemoData nhu [vn.smarthome.data.server.ServerState].
 * Ly do: danh sach thiet bi that do `congtac/info` quyet dinh, nen truoc khi
 * nhan duoc ban tin nao thi dung la "chua biet thiet bi that nao" - va moi thu
 * mac dinh la mau.
 */
data class CongTacState(
    val danhSach: List<CongTac> = emptyList(),
    val trangThai: Map<String, TrangThaiCongTac> = emptyMap(),
    /** LWT: "online" | "offline" | null (chua nghe thay gi). */
    val bridge: String? = null,
    val capNhat: String? = null,
) {
    val bridgeSong: Boolean get() = bridge == "online"

    fun tim(phong: String, thietBi: String): CongTac? =
        danhSach.firstOrNull { it.phong == phong && it.thietBi == thietBi }

    /** True khi cap (phong, thietBi) la cong tac that - lenh se doi do vat ngoai doi. */
    fun laThat(phong: String, thietBi: String): Boolean = tim(phong, thietBi) != null

    /** null = thiet bi mau, hoac that ma chua co ban tin trang thai nao. */
    fun trangThaiCua(phong: String, thietBi: String): TrangThaiCongTac? =
        tim(phong, thietBi)?.let { trangThai[it.ma] }

    /** Ten tieng Viet cac cong tac that, de nhet vao loi dan cho Gemini. */
    fun tenThietBiThat(): List<String> = danhSach.map { it.ten }

    companion object {
        fun gop(cur: CongTacState, topic: String, payload: String): CongTacState {
            // Payload rong thi giu nguyen, KE CA voi `info`. Broker khoi dong
            // lai mat retained thi thiet bi that khong duoc bien thanh mau
            // trong mot nhay mat. Muon xoa thiet bi, bridge publish `[]`.
            if (payload.isBlank()) return cur
            return try {
                when {
                    // So khop CHINH XAC truoc, tien to sau - xem ghi chu o CongTacTopic.
                    topic == CongTacTopic.TRANG_THAI -> cur.copy(bridge = payload.trim())
                    topic == CongTacTopic.CAP_NHAT -> cur.copy(capNhat = payload.trim())
                    topic == CongTacTopic.INFO -> cur.copy(danhSach = docDanhSach(payload))
                    topic.startsWith(CongTacTopic.TIEN_TO_TB) -> {
                        val ma = topic.removePrefix(CongTacTopic.TIEN_TO_TB)
                        if (ma.isBlank()) cur
                        else cur.copy(trangThai = cur.trangThai + (ma to docTrangThai(ma, payload)))
                    }
                    else -> cur
                }
            } catch (_: Exception) {
                cur
            }
        }

        /**
         * Doc `congtac/info`.
         *
         * Bo rieng phan tu hong chu khong vut ca mang: mat mot cong tac con
         * hon mat ca hai. Cung tinh than voi `DienState.docLichSu`.
         */
        fun docDanhSach(payload: String): List<CongTac> {
            val goc = JSONObject(payload).optJSONArray("thiet_bi") ?: JSONArray()
            val ds = mutableListOf<CongTac>()
            for (i in 0 until goc.length()) {
                val o = goc.optJSONObject(i) ?: continue
                val ma = o.optString("id").trim()
                val phong = o.optString("phong").trim()
                val thietBi = o.optString("loai").trim()
                if (ma.isEmpty() || phong.isEmpty() || thietBi.isEmpty()) continue
                ds += CongTac(
                    ma = ma,
                    phong = phong,
                    thietBi = thietBi,
                    ten = o.optString("ten").ifBlank { ma },
                    he = o.optString("driver").ifBlank { "?" },
                )
            }
            return ds
        }

        fun docTrangThai(ma: String, payload: String): TrangThaiCongTac {
            val o = JSONObject(payload)
            val online = o.optBoolean("online", false)
            return TrangThaiCongTac(
                ma = ma,
                online = online,
                // Thieu khoa, JSON null, hoac offline -> null. KHONG phai false.
                bat = if (online) optBool(o, "is_on") else null,
                luc = o.optString("doc_luc").ifBlank { null },
                traLoiId = o.optString("tra_loi_id").ifBlank { null },
                lyDoLoi = if (o.isNull("ly_do_loi")) null else o.optString("ly_do_loi").ifBlank { null },
            )
        }

        /** Doc bool; null khi thieu khoa hoac JSON null (khong doan la tat). */
        private fun optBool(o: JSONObject, key: String): Boolean? =
            if (o.has(key) && !o.isNull(key)) o.optBoolean(key) else null
    }
}

/**
 * Dung JSON lenh ban ra `smarthome/control`.
 *
 * Tach khoi ViewModel de kiem thu duoc. Giu nguyen khuon payload da co tu
 * truoc (device/room/action/source) va chi THEM `id` - de lenh cu cua giong
 * noi khong gay, va de app doi soat duoc ban tin tra loi.
 */
object LenhCongTac {
    const val NGUON_MAN_HINH = "man_hinh"
    const val NGUON_GIONG_NOI = "voice"

    fun maLenh(): String = java.util.UUID.randomUUID().toString().take(8)

    fun json(
        phong: String,
        thietBi: String,
        hanhDong: String,
        giaTri: String? = null,
        nguon: String,
        id: String,
    ): String {
        val o = JSONObject()
            .put("device", if (phong == "nhanh") "nhanh_$thietBi" else phong + "_" + thietBi)
            .put("room", phong)
            .put("action", hanhDong.uppercase())
            .put("source", nguon)
            .put("id", id)
        if (giaTri != null) o.put("value", giaTri)
        return o.toString()
    }
}

/**
 * Mot o cong tac can gi de ve. Man hinh khong phai biet MQTT la gi.
 *
 * [Mau] la thiet bi chua noi gi - gat la doi ngay tren man hinh, nhu truoc.
 * [That] thi man hinh KHONG duoc tu bia: khong ro trang thai thi ve "—".
 */
sealed interface OThietBi {
    data class Mau(val bat: Boolean) : OThietBi

    data class That(
        val ten: String,
        val bridgeSong: Boolean,
        val online: Boolean,
        val bat: Boolean?,
        val dangCho: Boolean,
        /** Khi [dangCho]: the nguoi dung vua gat toi. */
        val mong: Boolean?,
        val lyDoLoi: String?,
    ) : OThietBi {
        /** Chi cho gat khi biet chac dang o the nao. */
        val gatDuoc: Boolean get() = bridgeSong && online && bat != null && !dangCho

        /** Cau ngan hien duoi ten thiet bi. null = khong can noi gi. */
        val ghiChu: String?
            get() = when {
                dangCho -> "Đang gửi lệnh…"
                !bridgeSong -> "Bộ nối chưa chạy trên máy chủ"
                !online -> lyDoLoi ?: "Mất kết nối"
                bat == null -> lyDoLoi ?: "Chưa đọc được công tắc"
                else -> null
            }
    }
}

/**
 * Hang cho: lenh da ban ra broker ma cong tac chua xac nhan.
 *
 * Tach thanh lop thuan (khong Android, khong Compose, khong doc dong ho he
 * thong) de kiem thu chay thang tren JVM - dung ly le cua [CongCuNha].
 *
 * Go co "dang cho" bang MA LENH chu khong bang moc thoi gian: moc trong
 * payload do MAY CHU sinh, con moc "luc toi gui" do MAY TINH BANG sinh. Hai
 * dong ho do khong bao gio khop, va lech vai giay la co che chong-nhay-nguoc
 * hong theo cach rat kho tim. So chuoi thi khong can dong ho nao.
 *
 * MOI HAM DEU `@Synchronized`: lop nay bi cham tu HAI luong. Nut tren man
 * hinh goi tu luong giao dien, con tro ly giong noi goi tu luong cua OkHttp
 * (xem ghi chu o `HanhDongNha.moManHinh` trong HomeViewModel), va collector
 * cua `congtac/#` lai chay tren luong khac nua. Mot MutableMap tran o day la
 * dua luong - hong hiem, khong tai hien duoc, va hong dung luc nguoi ta dang
 * bam cong tac.
 */
class HangChoCongTac(private val giayCho: Int = GIAY_CHO_MAC_DINH) {

    data class Cho(val mong: Boolean, val id: String, val mocMs: Long)

    private val ds = mutableMapOf<String, Cho>()

    @Synchronized
    fun cua(ma: String): Cho? = ds[ma]

    @Synchronized
    fun dangCho(ma: String): Boolean = ma in ds

    @Synchronized
    fun ganh(ma: String, mong: Boolean, id: String, bayGioMs: Long) {
        ds[ma] = Cho(mong, id, bayGioMs)
    }

    @Synchronized
    fun go(ma: String) {
        ds.remove(ma)
    }

    /** Anh chup de dung UI; tra ban sao de nguoi goi khong duyet map dang doi. */
    @Synchronized
    fun anhChup(): Map<String, Boolean> = ds.mapValues { it.value.mong }

    /**
     * Nhan mot ban tin trang thai. Tra ve true neu no la cau tra loi cho lenh
     * dang cho (va da go co).
     *
     * CHI go khi ma lenh khop. Bridge tra ve trang thai NGUOC voi mong muon
     * (cong tac tu choi lenh) thi van go - vi do dung la cau tra loi, va man
     * hinh phai hien su that chu khong phai tiep tuc quay vong.
     */
    @Synchronized
    fun nhanTrangThai(ma: String, traLoiId: String?): Boolean {
        val cho = ds[ma] ?: return false
        if (traLoiId == null || traLoiId != cho.id) return false
        ds.remove(ma)
        return true
    }

    /** Nhung ma da qua han cho. Goi xong thi chung bi go khoi hang. */
    @Synchronized
    fun quaHan(bayGioMs: Long): List<String> {
        val het = ds.filterValues { bayGioMs - it.mocMs >= giayCho * 1000L }.keys.toList()
        het.forEach { ds.remove(it) }
        return het
    }

    companion object {
        /**
         * Tuya LAN va CozyLife LAN deu tra trong duoi mot giay; di qua may
         * Tuya thi 2-3 giay. 6 giay du rong de khong bao dong gia, du ngan de
         * khong ai dung nhin mot cong tac mo.
         *
         * Chot lai con so nay bang so do THAT luc nghiem thu - sua chi tieu
         * theo so do, khong sua so do theo chi tieu.
         */
        const val GIAY_CHO_MAC_DINH = 6
    }
}
