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

    /**
     * Topic app GHI de bat/tat bo giu ket noi BLE: `{"enabled": false}` roi
     * `{"enabled": true}` = "noi lai".
     *
     * Do tren thiet bi that ngay 21/11/2026: bat tay lai mat **42 giay**, khop
     * voi tai lieu du an ghi "bat tay nam buoc, 30-60 giay".
     */
    const val CMD_BLE = "cuktech/charger/ble"

    /** Topic app GHI de doi mot cai dat: `{"piid": 5, "value": 2}`. */
    const val CMD_SET = "cuktech/charger/set"

    /**
     * Tien to cua suc song BLE: `cuktech/rssi/<nguon>`.
     *
     * KHONG nam duoi `cuktech/charger/` co y: do la khong gian cua
     * `cuktech-ble-server`, va chinh no co ham xoa sach retained duoi tien to
     * ay. Nguon so nay la `scripts/cuktech_rssi/` cua du an nay, nen de ra
     * ngoai de khong bi ban kia don mat.
     *
     * Nhieu nguon cung luc la CO Y: mot dong cho dongle o server, mot dong cho
     * tram ESP32 canh cu sac. Nhin hai so canh nhau moi biet doi tram co an
     * thua khong.
     */
    const val TIEN_TO_RSSI = "cuktech/rssi/"
}

/**
 * So hieu PIID va cac lua chon hop le.
 *
 * Khoang gia tri lay tu `state.py` cua `cuktech-ble-server` (`PIID_RANGES`);
 * server tu chan gia tri ngoai khoang, nhung app khong nen de nguoi dung bam
 * ra mot gia tri chac chan bi chan.
 *
 * CHI khai nhung PIID da biet chac nghia. PIID 7, 17, 18, 21 thi chinh du an
 * cung ghi "chua ro" hoac chua doi chieu duoc - khong dung.
 */
object CuktechPiid {
    const val KICH_BAN = 5          // 1-4
    const val SANG_MAN = 6          // 0-5
    const val SAC_NHO_GIOT_A = 15   // 0-1
    const val TU_TAT_MAN = 19       // 0-1
    const val KHOA_XOAY = 20        // 0-1

    /** PIID hen gio tat cua tung cong (so phut, 0-1440). */
    fun henGio(cong: CongSacId): Int = when (cong) {
        CongSacId.C1 -> 9
        CongSacId.C2 -> 10
        CongSacId.C3 -> 11
        CongSacId.USB_A -> 12
    }

    /**
     * Lua chon thoi gian sang man, xep theo thu tu nguoi doc chu khong theo
     * so hieu: ma 5 la "1 phut" nhung phai dung dau danh sach.
     */
    val SANG_MAN_CHON = listOf(
        5 to "1 phút", 1 to "5 phút", 2 to "10 phút", 3 to "30 phút", 4 to "Luôn sáng",
    )

    /**
     * Lua chon hen gio tat cong. Khong cho go so tu do: man treo tuong go so
     * la phai keo ban phim ao len, va 1440 gia tri thi khong ai can.
     */
    val HEN_GIO_CHON = listOf(
        0 to "Tắt hẹn", 15 to "15 phút", 30 to "30 phút", 60 to "1 giờ", 120 to "2 giờ",
    )
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
    /**
     * Bang PIID THO, giu nguyen so.
     *
     * Vi sao can ca ban tho khi da co cac truong da dich: sau khi gui lenh
     * doi cai dat, cach duy nhat biet cu sac da nghe la doc lai DUNG cai PIID
     * vua gui. Doi soat tren ban tho thi mot vong lap lo duoc het, khong phai
     * viet rieng cho tung truong.
     */
    val tho: Map<Int, Int> = emptyMap(),
) {
    companion object {
        private val MAN_SANG = CuktechPiid.SANG_MAN_CHON.toMap()

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

            val tho = buildMap {
                for (khoa in o.keys()) {
                    val piid = khoa.toIntOrNull() ?: continue
                    if (!o.isNull(khoa)) put(piid, o.optInt(khoa))
                }
            }

            return CuktechCaiDat(
                kichBan = so(5)?.let { KichBan.tuMa(it) },
                thoiGianSangMan = so(6)?.let { MAN_SANG[it] },
                tuTatManNhanRoi = co(19),
                khoaXoayMan = co(20),
                sacNhoGiotUsbA = co(15),
                congBat = congBat,
                henGioPhut = henGio,
                tho = tho,
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
 * Suc song BLE do duoc tai MOT diem do.
 *
 * Vi sao man hinh can con so nay: ngay 21/11/2026 cu sac rot ket noi 37 lan
 * trong 2 gio, va phai SSH vao server moi biet nguyen nhan la song yeu
 * (-81 den -96 dBm, trong khi may Windows canh cu sac do duoc -43). Khong co
 * con so nay thi moi lan "mat ket noi" deu phai di dieu tra lai tu dau.
 *
 * [rssi] null = chua do duoc lan nao, KHONG phai 0 dBm (0 la muc rat manh).
 */
data class SongBle(
    /** Doan cuoi cua topic: `server`, `esp`... */
    val nguonId: String,
    val rssi: Int? = null,
    /** Ten de doc cua noi do, lay tu goi tin. */
    val nguonTen: String? = null,
    /** Bao lau roi chua thay gia tri moi. null = chua do duoc lan nao. */
    val tuoiGiay: Int? = null,
    /** Noi do nay thay thiet bi dang trong mot ket noi khong. */
    val dangNoi: Boolean? = null,
) {
    /**
     * Xep hang theo nguong do duoc tren chinh he nay, khong phai nguong sach vo.
     *
     * -81 den -96 la muc dang rot lien tuc; -43 (may Windows ngay canh) la muc
     * giu duoc. Nen ranh gioi "yeu" dat o -80.
     */
    enum class Muc(val nhan: String) {
        TOT("Tốt"), KHA("Khá"), YEU("Yếu"), RAT_YEU("Rất yếu"),

        /**
         * Co goi tin nhung khong co so.
         *
         * Nhan la "Không bắt được" chu khong phai "Không rõ": diem do co chay,
         * no vua quet xong va khong thay quang ba nao. Do la mot ket luan,
         * khong phai mot cho trong - va la ket luan nang hon ca "Rất yếu".
         */
        KHONG_RO("Không bắt được")
    }

    val muc: Muc get() = when {
        rssi == null -> Muc.KHONG_RO
        rssi >= -65 -> Muc.TOT
        rssi >= -80 -> Muc.KHA
        rssi >= -90 -> Muc.YEU
        else -> Muc.RAT_YEU
    }

    /** So dang `-81 dBm`, hoac `—` khi chua do duoc. */
    fun nhanDbm(): String = rssi?.let { "$it dBm" } ?: "—"

    /**
     * So da qua cu de tin chua.
     *
     * 5 phut: BlueZ chi cap nhat RSSI khi co quet, va script do tu quet lai
     * moi 60 giay khi so cu. Qua 5 phut van khong co so moi nghia la ca quet
     * cung khong thay gi.
     */
    fun laSoCu(): Boolean = (tuoiGiay ?: Int.MAX_VALUE) > 300

    companion object {
        fun doc(nguonId: String, o: JSONObject): SongBle = SongBle(
            nguonId = nguonId,
            rssi = if (o.has("rssi") && !o.isNull("rssi")) o.optInt("rssi") else null,
            nguonTen = o.optString("nguon", "").ifBlank { null },
            tuoiGiay = if (o.has("tuoi_giay") && !o.isNull("tuoi_giay")) {
                o.optInt("tuoi_giay")
            } else {
                null
            },
            dangNoi = if (o.has("dang_noi") && !o.isNull("dang_noi")) {
                o.optBoolean("dang_noi")
            } else {
                null
            },
        )
    }
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
    /**
     * Suc song BLE theo tung diem do, khoa la doan cuoi cua topic.
     *
     * Rong = chua chay `scripts/cuktech_rssi/`. Man hinh phai im lang trong
     * truong hop do chu khong ve mot thanh song bang 0.
     */
    val song: Map<String, SongBle> = emptyMap(),
    /**
     * Moc nhan goi gan nhat (ms). 0 = chua nhan gi.
     *
     * Vi sao can: co `connected` nam trong mot ban tin RETAINED, nen no co the
     * ket o `false` tu mot phien cu trong khi so cong van ve deu moi 10 giay -
     * dung canh gap ngay 21/11/2026. So dang chay thi thang mot cai co cu.
     *
     * Han che da biet: ban tin retained toi ngay luc subscribe nen moc nay
     * tuoi ngay ca khi so da cu. Khong co cach nao khac - luong nay khong co
     * moc thoi gian cua server. Xem [laSoCu].
     */
    val mocNhanMs: Long = 0L,
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

    /**
     * Diem do co song manh nhat.
     *
     * Lay manh nhat chu khong phai trung binh: cau hoi can tra loi la "co cho
     * nao dat tram ma song du khong", va mot diem do tot la du de tra loi.
     */
    fun songTotNhat(): SongBle? = song.values
        .filter { it.rssi != null }
        .maxByOrNull { it.rssi ?: Int.MIN_VALUE }

    /** Cong nay co dang bi tat han khong (PIID 16). null = chua biet. */
    fun congBiTat(id: CongSacId): Boolean? = caiDat?.congBat?.get(id)?.let { !it }

    /** Da lau khong nhan goi nao -> so tren man hinh la so cu. */
    fun laSoCu(bayGioMs: Long = System.currentTimeMillis()): Boolean =
        mocNhanMs > 0L && bayGioMs - mocNhanMs > GIAY_COI_LA_CU * 1000L

    /**
     * Co that su mat ket noi khong.
     *
     * KHONG chi doc co `connected`: co do nam trong ban tin retained va tung
     * ket o `false` suot trong khi bon topic cong van doi so. Chi ket luan mat
     * khi ca hai dau hieu cung chi ve mot huong: co bao mat VA khong con goi
     * nao moi.
     */
    fun matKetNoiThat(bayGioMs: Long = System.currentTimeMillis()): Boolean =
        !noiDuoc && laSoCu(bayGioMs)

    /** Co bao mat nhung so van ve - ban tin trang thai dang cu, khong phai thiet bi hong. */
    fun coMauThuan(bayGioMs: Long = System.currentTimeMillis()): Boolean =
        !noiDuoc && !laSoCu(bayGioMs)

    /** Thu tu co dinh C1 -> C2 -> C3 -> USB-A, cong thieu thi tra ve cong rong. */
    fun danhSachCong(): List<CongSac> =
        CongSacId.entries.map { cong[it] ?: CongSac(it) }

    companion object {

        /** Bao lau khong nhan goi thi coi so tren man hinh la cu (giay). */
        const val GIAY_COI_LA_CU = 60

        fun gop(
            cur: CuktechState,
            topic: String,
            payload: String,
            bayGioMs: Long = System.currentTimeMillis(),
        ): CuktechState {
            // Topic song xu ly TRUOC phep thu payload rong: voi no, rong la
            // LWT cua script do - nghia la "diem do nay chet roi", phai BO
            // dong do khoi man hinh chu khong giu lai so cu.
            if (topic.startsWith(CuktechTopic.TIEN_TO_RSSI)) {
                val nguon = topic.removePrefix(CuktechTopic.TIEN_TO_RSSI)
                if (nguon.isBlank() || nguon.contains('/')) return cur
                if (payload.isBlank()) return cur.copy(song = cur.song - nguon)
                return try {
                    cur.copy(song = cur.song + (nguon to SongBle.doc(nguon, JSONObject(payload))))
                } catch (_: Exception) {
                    cur
                }
            }
            if (payload.isBlank()) return cur
            return try {
                when {
                    topic == CuktechTopic.STATUS ->
                        docStatus(cur, JSONObject(payload)).copy(mocNhanMs = bayGioMs)

                    topic.startsWith(CuktechTopic.TIEN_TO_CONG) -> {
                        val id = CongSacId.tuTopic(topic.removePrefix(CuktechTopic.TIEN_TO_CONG))
                            ?: return cur
                        cur.copy(
                            coSoThat = true,
                            mocNhanMs = bayGioMs,
                            cong = cur.cong + (id to docCong(id, JSONObject(payload))),
                        )
                    }

                    topic == CuktechTopic.SETTINGS -> cur.copy(
                        coSoThat = true,
                        mocNhanMs = bayGioMs,
                        caiDat = CuktechCaiDat.doc(JSONObject(payload)),
                    )

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
