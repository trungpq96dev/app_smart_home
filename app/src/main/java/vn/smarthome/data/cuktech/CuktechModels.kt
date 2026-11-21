package vn.smarthome.data.cuktech

import org.json.JSONObject

/**
 * Topic cua cu sac CUKTECH 10 Ultra.
 *
 * KHONG phai hop dong do app dat ra: day la topic san co cua
 * `cuktech-ble-server` (MIT) chay tren serverubuntu, lay nguyen ten. Doi ten
 * cho giong `dien/`/`server/` nghia la phai nuoi mot ban va rieng cua du an do
 * mai mai - khong dang.
 *
 * App CHI DOC. Hai topic `cuktech/charger/set` va `.../port` la duong GHI de
 * bat/tat cong sac; app la man hinh xem nen khong dung toi.
 */
object CuktechTopic {
    const val GOC = "cuktech/#"
    const val STATUS = "cuktech/charger/status"
    const val SETTINGS = "cuktech/charger/settings"

    /** Tien to cua 4 topic cong roi: `.../port/c1` `c2` `c3` `a`. */
    const val TIEN_TO_CONG = "cuktech/charger/port/"

    /**
     * Topic app GHI de bat/tat cong sac: `{"port":"c1","action":"on"}`.
     *
     * KHONG retained: bat cong la mot su kien. Giu lai thi cu sac nao vua cam
     * dien len cung bat cong theo mot lenh tu hom qua - dung ly le voi
     * [vn.smarthome.data.dien.DieuKhienTopic].
     */
    const val CMD_CONG = "cuktech/charger/port"
}

/**
 * Kich ban phan phoi cong suat (PIID 5).
 *
 * Ten tieng Viet lay theo app CUKTECH tren dien thoai, khong dich tu ten goc
 * trong thu vien (`数码生态` = "he sinh thai so"), de nguoi dung thay hai ben
 * noi cung mot thu.
 */
enum class KichBan(val ma: Int, val nhan: String) {
    AI(1, "AI"),
    KET_HOP(2, "Kết hợp"),
    DON(3, "Đơn"),
    CAN_BANG(4, "Cân bằng");

    companion object {
        fun tuMa(ma: Int): KichBan? = entries.firstOrNull { it.ma == ma }
    }
}

/**
 * Cai dat cua cu sac, giai tu bang PIID trong `cuktech/charger/settings`.
 *
 * Bang nay tung bi ghi la "chua ai giai nghia". Sai: `src/cuktech_ble/protocol.py`
 * cua du an trung gian chu thich day du, va doi chieu goi that voi anh chup app
 * CUKTECH ngay 21/11/2026 thi TAM gia tri khop het (xem docs/KE-HOACH-CUKTECH.md
 * muc 4). Rieng PIID 17, 18, 21 thi chinh du an cung ghi "chua ro" - khong khai
 * vao day.
 *
 * Moi truong deu nullable: thieu PIID nao thi man hinh hien "—" chu khong doan.
 */
data class CuktechCaiDat(
    /** PIID 5. */
    val kichBan: KichBan? = null,
    /** PIID 6, da doi sang chu. */
    val thoiGianSangMan: String? = null,
    /** PIID 19. */
    val tuTatManNhanRoi: Boolean? = null,
    /** PIID 20. */
    val khoaXoayMan: Boolean? = null,
    /** PIID 15. */
    val sacNhoGiotUsbA: Boolean? = null,
    /** PIID 16, mat na bit: bit0=C1, bit1=C2, bit2=C3, bit3=A. */
    val congBat: Map<CongSacId, Boolean> = emptyMap(),
    /** PIID 9-12, so phut. 0 = chua dat hen gio. */
    val henGioPhut: Map<CongSacId, Int> = emptyMap(),
) {
    companion object {
        /** PIID 6 -> chu. Bang nay o PIID_DISPLAY cua thu vien. */
        private val MAN_SANG = mapOf(
            1 to "5 phút", 2 to "10 phút", 3 to "30 phút", 4 to "Luôn sáng", 5 to "1 phút",
        )

        /** PIID 9-12 theo thu tu C1, C2, C3, USB-A. */
        private val PIID_HEN_GIO = mapOf(
            9 to CongSacId.C1, 10 to CongSacId.C2, 11 to CongSacId.C3, 12 to CongSacId.USB_A,
        )

        fun doc(o: JSONObject): CuktechCaiDat {
            fun so(piid: Int): Int? =
                if (o.has("$piid") && !o.isNull("$piid")) o.optInt("$piid") else null

            fun co(piid: Int): Boolean? = so(piid)?.let { it != 0 }

            val mask = so(16)
            val congBat = if (mask == null) emptyMap() else CongSacId.entries.associateWith {
                (mask shr it.viTriBit) and 1 == 1
            }

            val henGio = buildMap {
                for ((piid, cong) in PIID_HEN_GIO) so(piid)?.let { put(cong, it) }
            }

            return CuktechCaiDat(
                kichBan = so(5)?.let { KichBan.tuMa(it) },
                thoiGianSangMan = so(6)?.let { MAN_SANG[it] },
                tuTatManNhanRoi = co(19),
                khoaXoayMan = co(20),
                sacNhoGiotUsbA = co(15),
                congBat = congBat,
                henGioPhut = henGio,
            )
        }
    }
}

/**
 * Bon cong cua cu sac.
 *
 * [khoaStatus] la khoa trong `cuktech/charger/status` (JSON danh so "1".."4"),
 * [khoaTopic] la duoi cua topic cong roi. Hai cach goi khac nhau cho cung mot
 * cong - do la cua du an goc, khong phai cho tu nghi ra.
 */
enum class CongSacId(
    val khoaStatus: String,
    val khoaTopic: String,
    val nhan: String,
    /** Vi tri bit trong mat na PIID 16: bit0=C1, bit1=C2, bit2=C3, bit3=A. */
    val viTriBit: Int,
) {
    C1("1", "c1", "C1", 0),
    C2("2", "c2", "C2", 1),
    C3("3", "c3", "C3", 2),
    USB_A("4", "a", "USB-A", 3);

    companion object {
        fun tuTopic(duoi: String): CongSacId? =
            entries.firstOrNull { it.khoaTopic.equals(duoi, ignoreCase = true) }

        fun tuStatus(khoa: String): CongSacId? =
            entries.firstOrNull { it.khoaStatus == khoa }
    }
}

/**
 * Mot cong sac tai mot thoi diem.
 *
 * Ten truong lay tu GOI THAT doc duoc tren broker ngay 21/11/2026:
 *
 *   cong dang sac: {"voltage":20.1,"current":2.3,"power":46.2,"active":true,
 *                   "protocol":"PD","status_raw":1,"_raw_code":"0x03",
 *                   "_proto_num":7,"enabled":true}
 *   cong trong   : {"voltage":0.0,"current":0.0,"power":0.0,"active":false,
 *                   "protocol":"idle"}
 *
 * Hai dieu rut ra va phai giu:
 *
 *  1. Cong trong CHI CO 5 truong - thieu han `enabled`. Nen [choPhep] la
 *     nullable; khai no `Boolean = true` la bia ra mot su that khong co trong
 *     goi.
 *  2. Truong co gach duoi (`_raw_code`, `_proto_num`) la noi bo cua du an
 *     trung gian, co the doi giua cac ban. KHONG khai vao day.
 */
data class CongSac(
    val id: CongSacId,
    val volt: Float = 0f,
    val ampe: Float = 0f,
    val oat: Float = 0f,
    /**
     * Cu sac tu bao cong nay co dang lam viec khong.
     *
     * Dung truong nay de to mau, DUNG so `oat > 0.5` nhu tai lieu goc de xuat:
     * sac gan day pin thi dong tut ve gan 0 ma cong van dang sac that.
     */
    val dangChay: Boolean = false,
    /** "PD", "5V", "QC"... va "idle" khi trong. Khong bao gio null trong goi that. */
    val giaoThuc: String = "idle",
    /** null = goi khong noi (cong trong). Xem ghi chu (1) o tren. */
    val choPhep: Boolean? = null,
) {
    /** Cong trong thi khong hien "idle" ra man hinh - doc kho hieu. */
    fun nhanGiaoThuc(): String = if (dangChay) giaoThuc else "Trống"
}

/**
 * Trang thai ca cu sac.
 *
 * KHONG khoi tao bang DemoData nhu [vn.smarthome.data.server.ServerState] va
 * WifiState. Hai man do co phan du lieu mau deo chip "DỮ LIỆU MẪU"; cu sac thi
 * khong co so mau nao, va ve mot cu sac tuong tuong dang ra 85W la bia. Chua
 * nhan goi nao thi [coSoThat] = false va man hinh noi thang la chua co so.
 */
data class CuktechState(
    val coSoThat: Boolean = false,
    /** `connected` trong status: server da noi BLE toi cu sac chua. */
    val noiDuoc: Boolean = false,
    /** `authenticated`: da qua bat tay MiOT chua. Chua xong thi khong co so cong. */
    val xacThuc: Boolean = false,
    val model: String? = null,
    val firmware: String? = null,
    val cong: Map<CongSacId, CongSac> = emptyMap(),
    /** null = chua nhan goi `settings` nao. */
    val caiDat: CuktechCaiDat? = null,
) {
    /**
     * Tong cong suat.
     *
     * Cu sac KHONG phat so tong - day la tong cong lai tu 4 cong. Man hinh phai
     * noi ro nhu vay, dung de nguoi dung tuong day la so cu sac tu bao.
     */
    val tongOat: Float get() = cong.values.fold(0f) { t, c -> t + c.oat }

    val soCongDangChay: Int get() = cong.values.count { it.dangChay }

    /**
     * Phan tram cong suat moi cong dang chiem, de ve dai phan phoi.
     *
     * Tong bang 0 thi tra ve map rong chu KHONG tra 0% cho tat ca - hai thu do
     * khac nhau tren man hinh: "khong sac gi" va "chia deu 0%".
     */
    fun tyLePhanBo(): Map<CongSacId, Float> {
        val tong = tongOat
        if (tong <= 0f) return emptyMap()
        return cong.filterValues { it.oat > 0f }.mapValues { it.value.oat / tong * 100f }
    }

    /** Cong nay co dang bi tat han khong (PIID 16). null = chua biet. */
    fun congBiTat(id: CongSacId): Boolean? = caiDat?.congBat?.get(id)?.let { !it }

    /** Thu tu co dinh C1 -> C2 -> C3 -> USB-A, cong thieu thi tra ve cong rong. */
    fun danhSachCong(): List<CongSac> =
        CongSacId.entries.map { cong[it] ?: CongSac(it) }

    companion object {

        fun gop(cur: CuktechState, topic: String, payload: String): CuktechState {
            if (payload.isBlank()) return cur
            return try {
                when {
                    topic == CuktechTopic.STATUS -> docStatus(cur, JSONObject(payload))

                    topic.startsWith(CuktechTopic.TIEN_TO_CONG) -> {
                        val id = CongSacId.tuTopic(topic.removePrefix(CuktechTopic.TIEN_TO_CONG))
                            ?: return cur
                        cur.copy(
                            coSoThat = true,
                            cong = cur.cong + (id to docCong(id, JSONObject(payload))),
                        )
                    }

                    topic == CuktechTopic.SETTINGS ->
                        cur.copy(coSoThat = true, caiDat = CuktechCaiDat.doc(JSONObject(payload)))

                    else -> cur
                }
            } catch (_: Exception) {
                cur
            }
        }

        /**
         * `status` cho luon ca 4 cong nen mot goi nay la du dung man hinh.
         * Topic cong roi van duoc doc vi no toi day hon.
         */
        private fun docStatus(cur: CuktechState, o: JSONObject): CuktechState {
            val ports = o.optJSONObject("ports")
            val cong = if (ports == null) cur.cong else buildMap {
                putAll(cur.cong)
                for (khoa in ports.keys()) {
                    val id = CongSacId.tuStatus(khoa) ?: continue
                    val co = ports.optJSONObject(khoa) ?: continue
                    put(id, docCong(id, co))
                }
            }
            // `status` cho kem ca bang PIID, nen doc luon - do nhanh hon mot
            // nhip so voi topic `settings` rieng.
            val caiDat = o.optJSONObject("settings")?.let { CuktechCaiDat.doc(it) } ?: cur.caiDat

            return cur.copy(
                coSoThat = true,
                noiDuoc = o.optBoolean("connected", cur.noiDuoc),
                xacThuc = o.optBoolean("authenticated", cur.xacThuc),
                model = o.optString("device_model", "").ifBlank { cur.model },
                firmware = o.optString("firmware_version", "").ifBlank { cur.firmware },
                cong = cong,
                caiDat = caiDat,
            )
        }

        private fun docCong(id: CongSacId, o: JSONObject): CongSac = CongSac(
            id = id,
            volt = o.optDouble("voltage", 0.0).toFloat(),
            ampe = o.optDouble("current", 0.0).toFloat(),
            oat = o.optDouble("power", 0.0).toFloat(),
            dangChay = o.optBoolean("active", false),
            giaoThuc = o.optString("protocol", "idle").ifBlank { "idle" },
            // Vang mat o cong trong - giu null thay vi doan.
            choPhep = if (o.has("enabled") && !o.isNull("enabled")) {
                o.optBoolean("enabled")
            } else {
                null
            },
        )
    }
}
