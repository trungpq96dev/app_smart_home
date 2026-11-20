package vn.smarthome.data.giongnoi

import org.json.JSONArray
import org.json.JSONObject

/**
 * Nhung viec tro ly giong noi lam duoc trong app.
 *
 * Tach thanh interface co chu dinh: [CongCuNha] - noi chua toan bo phan doc
 * tham so, kep khoang va sinh cau tra loi - khong duoc dinh gi toi Compose hay
 * ViewModel, de kiem thu chay thang tren JVM voi mot ban gia.
 *
 * Ban that nam trong [vn.smarthome.vm.HomeViewModel].
 */
interface HanhDongNha {

    /** Danh sach ma phong dang co: living, bed1, bed2, bed3, kitchen. */
    fun maPhong(): List<String>

    /** Ten tieng Viet co dau de Gemini doc len. Null neu khong co phong do. */
    fun tenPhong(ma: String): String?

    fun datDen(phong: String, bat: Boolean)
    fun datQuat(phong: String, bat: Boolean)
    fun datDieuHoa(phong: String, bat: Boolean)
    fun datLichHen(phong: String, bat: Boolean)

    /** Dat nhiet do dieu hoa. Tra ve nhiet do THAT sau khi kep vao 16..30. */
    fun datNhietDo(phong: String, doC: Int): Int

    /** Tang/giam nhiet do. Tra ve nhiet do THAT sau khi kep. */
    fun chinhNhietDo(phong: String, buoc: Int): Int

    /** [cheDo] la mot trong: ngu, nha, vang_nha, tuy_chinh. */
    fun datCheDo(phong: String, cheDo: String)

    /** [id] la mot trong: light, fan, ac, curtain. */
    fun datONhanh(id: String, bat: Boolean)

    /** Trang thai hien tai cua mot thiet bi, de biet "dao" thanh bat hay tat. */
    fun dangBat(phong: String, thietBi: String): Boolean?

    /**
     * True khi cap (phong, thietBi) la cong tac THAT - lenh se doi do vat
     * ngoai doi chu khong chi doi hinh tren man hinh.
     *
     * Nguon: `congtac/info` tu bridge. KHONG hard-code o day - them cong tac
     * thu ba thi sua .env tren server, khong phai build lai APK.
     */
    fun laThat(phong: String, thietBi: String): Boolean

    /** Ten tieng Viet cac cong tac that, de nhet vao loi dan. Rong = chua co cai nao. */
    fun thietBiThat(): List<String>

    /** Doi man hinh. Tra ve false neu khong co dich do. */
    fun moManHinh(dich: String): Boolean

    /**
     * So lieu THAT dang co. Chua co so thi tra `{"co_so": false, "ly_do": ...}`
     * chu khong bao gio bia mot con so - dung nguyen tac cua du an.
     */
    fun docSoLieu(muc: String): JSONObject

    /** Anh chup trang thai ca nha, nhet vao system_instruction luc mo phien. */
    fun chupTrangThai(): String

    /** Ban lenh ra MQTT `smarthome/control`. Tra ve false neu chua noi broker. */
    fun guiMqtt(phong: String, thietBi: String, hanhDong: String, giaTri: String?): Boolean
}

/**
 * Lenh khong chay duoc, kem cau giai thich viet cho NGUOI doc.
 *
 * Nem thay vi tra ve chuoi de khong co duong nao bao "ok" kem mot cau giai
 * thich vi sao that bai - Gemini thay `ok` la di xac nhan voi nguoi dung rang
 * da lam xong.
 */
class LoiLenh(val cau: String) : Exception(cau)

/**
 * Doc mot `toolCall` cua Gemini va chay no.
 *
 * Moi ham tra ve mot JSONObject gui nguoc lai Gemini trong `toolResponse`.
 * Quy uoc: `{"ok": true, "ket_qua": "..."}` hoac `{"ok": false, "loi": "..."}`.
 * Gemini doc cau do roi tu dien dat lai bang giong noi, nen cau phai viet nhu
 * noi voi nguoi - khong phai ma loi.
 */
class CongCuNha(
    private val nha: HanhDongNha,
    /** Bat thi moi lenh dieu khien con ban them ra MQTT. */
    private val guiMqtt: Boolean = false,
) {

    fun chay(ten: String, args: JSONObject): JSONObject = try {
        when (ten) {
            DIEU_KHIEN -> dieuKhien(args)
            MO_MAN_HINH -> moManHinh(args)
            DOC_SO_LIEU -> docSoLieu(args)
            else -> loi("Không có công cụ tên $ten.")
        }
    } catch (e: LoiLenh) {
        loi(e.cau)
    } catch (e: Exception) {
        // Khong de mot tham so la lam dut ca phien noi chuyen: tra loi ve cho
        // Gemini de no hoi lai nguoi dung.
        loi("Lệnh không chạy được: " + (e.message ?: e.javaClass.simpleName))
    }

    // ---------------------------------------------------------------
    // dieu_khien_thiet_bi
    // ---------------------------------------------------------------

    private fun dieuKhien(args: JSONObject): JSONObject {
        val phong = args.optString("phong").trim().lowercase()
        val thietBi = args.optString("thiet_bi").trim().lowercase()
        val hanhDong = args.optString("hanh_dong").trim().lowercase()
        val giaTri = args.optString("gia_tri").trim().ifEmpty { null }

        if (thietBi.isEmpty()) return loi("Chưa biết điều khiển thiết bị gì.")
        if (hanhDong.isEmpty()) return loi("Chưa biết làm gì với $thietBi.")

        // O thao tac nhanh o Trang chu - khong thuoc phong nao.
        if (phong == NHANH) return oNhanh(thietBi, hanhDong)

        val dsPhong = if (phong == TAT_CA) nha.maPhong() else listOf(phong)
        if (phong != TAT_CA && nha.tenPhong(phong) == null) {
            return loi(
                "Nhà mình không có phòng nào tên \"$phong\". Đang có: " +
                    nha.maPhong().joinToString(", ") { nha.tenPhong(it) ?: it } + "."
            )
        }

        val ketQua = mutableListOf<String>()
        var moiCaiDeuThat = true
        for (p in dsPhong) {
            val ten = nha.tenPhong(p) ?: continue
            val that = nha.laThat(p, thietBi)
            if (!that) moiCaiDeuThat = false
            val r = motPhong(p, ten, thietBi, hanhDong, giaTri) ?: return loi(
                "Chưa làm được \"$hanhDong\" với \"$thietBi\"."
            )
            ketQua += r
            // Thiet bi THAT: nha.datDen/datQuat da ban lenh ra MQTT roi. Ban
            // them o day la hai lenh cho mot cau noi - cong tac nhay hai lan.
            if (guiMqtt && !that) nha.guiMqtt(p, thietBi, hanhDong, giaTri)
        }
        if (ketQua.isEmpty()) return loi("Không có phòng nào để điều khiển.")
        // Con mot cai mau thi ca lenh coi la chua-that, de canh bao van hien.
        // Tha canh bao thua con hon thieu.
        return ok(ketQua.joinToString("; "), that = moiCaiDeuThat)
    }

    /** Tra ve null neu cap thiet_bi/hanh_dong khong hop le. */
    private fun motPhong(
        ma: String,
        ten: String,
        thietBi: String,
        hanhDong: String,
        giaTri: String?,
    ): String? = when (thietBi) {
        DEN, QUAT, DIEU_HOA, LICH_HEN -> {
            val dangBat = nha.dangBat(ma, thietBi)
            // `dao` tren thiet bi THAT ma khong ro trang thai thi phai tu choi.
            // batHayTat() coi khong-ro la "tat" roi dao thanh bat - doan the
            // voi thiet bi mau thi hop ly, voi cong tac that dang mat ket noi
            // thi la doan mo.
            if (hanhDong == DAO && dangBat == null && nha.laThat(ma, thietBi)) {
                throw LoiLenh(
                    "Chưa biết ${tenThietBi(thietBi)} $ten đang bật hay tắt — " +
                        "công tắc đang mất kết nối. Bạn muốn bật hay tắt?",
                )
            }
            val bat = batHayTat(hanhDong, dangBat) ?: return null
            when (thietBi) {
                DEN -> nha.datDen(ma, bat)
                QUAT -> nha.datQuat(ma, bat)
                LICH_HEN -> nha.datLichHen(ma, bat)
                else -> nha.datDieuHoa(ma, bat)
            }
            // Bat dieu hoa ma nguoi dung noi kem nhiet do thi dat luon, khong
            // bat nguoi ta noi them mot cau nua.
            val nhiet = if (thietBi == DIEU_HOA && bat && giaTri != null) {
                giaTri.toIntOrNull()?.let { nha.datNhietDo(ma, it) }
            } else {
                null
            }
            val tenTb = tenThietBi(thietBi)
            // Thiet bi that: luc ham nay tra ve, lenh moi roi broker - cong
            // tac chua chac da nhan. Noi "da gui lenh bat" chu dung noi "bat".
            val xong = if (nha.laThat(ma, thietBi)) "đã gửi lệnh " else ""
            if (nhiet != null) "$tenTb $ten: ${xong}bật, $nhiet độ"
            else "$tenTb $ten: " + xong + (if (bat) "bật" else "tắt")
        }

        NHIET_DO -> when (hanhDong) {
            DAT -> {
                // Thieu nhiet do thi phai bao HONG, khong duoc bao thanh cong
                // kem mot cau giai thich: Gemini doc co "ok" la no di xac nhan
                // voi nguoi dung rang da chinh xong.
                val so = giaTri?.toIntOrNull()
                    ?: throw LoiLenh("Chưa biết đặt điều hòa $ten bao nhiêu độ.")
                val that = nha.datNhietDo(ma, so)
                if (that != so) "Điều hòa $ten: $that độ (chỉ nhận 16 đến 30 độ)"
                else "Điều hòa $ten: $that độ"
            }

            TANG -> "Điều hòa $ten: " + nha.chinhNhietDo(ma, giaTri?.toIntOrNull() ?: 1) + " độ"
            GIAM -> "Điều hòa $ten: " + nha.chinhNhietDo(ma, -(giaTri?.toIntOrNull() ?: 1)) + " độ"
            else -> null
        }

        CHE_DO -> {
            val cd = docCheDo(giaTri ?: hanhDong)
                ?: throw LoiLenh(
                    "Không có chế độ \"${giaTri ?: hanhDong}\". " +
                        "Chỉ có: Ngủ, Nhà, Vắng nhà, Tùy chỉnh.",
                )
            nha.datCheDo(ma, cd)
            "$ten: chế độ " + tenCheDo(cd)
        }

        else -> null
    }

    private fun oNhanh(thietBi: String, hanhDong: String): JSONObject {
        val id = when (thietBi) {
            DEN -> "light"
            QUAT -> "fan"
            DIEU_HOA -> "ac"
            REM -> "curtain"
            else -> return loi("Ô thao tác nhanh chỉ có đèn, quạt, điều hòa và rèm.")
        }
        val bat = batHayTat(hanhDong, nha.dangBat(NHANH, id))
            ?: return loi("Ô thao tác nhanh chỉ bật hoặc tắt được.")
        nha.datONhanh(id, bat)
        if (guiMqtt) nha.guiMqtt(NHANH, thietBi, hanhDong, null)
        return ok(tenThietBi(thietBi) + " (ô nhanh): " + if (bat) "bật" else "tắt")
    }

    /**
     * `bat`/`tat` thi ro rang. `dao` phai biet trang thai hien tai - khong biet
     * thi coi nhu bat, vi nguoi ta hay noi "bat den di" hon la "dao den".
     */
    private fun batHayTat(hanhDong: String, dangBat: Boolean?): Boolean? = when (hanhDong) {
        BAT -> true
        TAT -> false
        DAO -> !(dangBat ?: false)
        else -> null
    }

    // ---------------------------------------------------------------
    // mo_man_hinh
    // ---------------------------------------------------------------

    private fun moManHinh(args: JSONObject): JSONObject {
        val dich = args.optString("dich").trim().lowercase()
        if (dich.isEmpty()) return loi("Chưa biết mở màn hình nào.")
        return if (nha.moManHinh(dich)) ok("Đã mở $dich")
        else loi("Không có màn hình \"$dich\".")
    }

    // ---------------------------------------------------------------
    // doc_so_lieu
    // ---------------------------------------------------------------

    private fun docSoLieu(args: JSONObject): JSONObject {
        val muc = args.optString("muc").trim().lowercase()
        if (muc.isEmpty()) return loi("Chưa biết đọc số liệu gì.")
        if (muc !in MUC_HOP_LE) return loi("Không đọc được mục \"$muc\".")
        return JSONObject().put("ok", true).put("so_lieu", nha.docSoLieu(muc))
    }

    // ---------------------------------------------------------------

    /**
     * [that] = lenh nay di toi thiet bi THAT.
     *
     * TroLyGiongNoi doc co nay de quyet dinh co gan dong xam "chua noi thiet
     * bi that" hay khong. Mac dinh false: thieu co thi canh bao - huong an toan.
     */
    private fun ok(cau: String, that: Boolean = false) =
        JSONObject().put("ok", true).put("ket_qua", cau).put("that", that)

    private fun loi(cau: String) = JSONObject().put("ok", false).put("loi", cau)

    companion object {
        const val DIEU_KHIEN = "dieu_khien_thiet_bi"
        const val MO_MAN_HINH = "mo_man_hinh"
        const val DOC_SO_LIEU = "doc_so_lieu"

        const val NHANH = "nhanh"
        const val TAT_CA = "tat_ca"

        const val DEN = "den"
        const val QUAT = "quat"
        const val DIEU_HOA = "dieu_hoa"
        const val REM = "rem"
        const val NHIET_DO = "nhiet_do"
        const val CHE_DO = "che_do"
        const val LICH_HEN = "lich_hen"

        const val BAT = "bat"
        const val TAT = "tat"
        const val DAO = "dao"
        const val DAT = "dat"
        const val TANG = "tang"
        const val GIAM = "giam"

        /**
         * `cong_tac` tach rieng khoi `trang_thai_nha` co chu dinh: muc do
         * tron that lan mau, nen cau "den phong ngu 1 co dang sang that
         * khong" se duoc tra loi bang mot so mau.
         */
        val MUC_HOP_LE = listOf(
            "trang_thai_nha", "dien", "server", "wifi", "thoi_tiet", "lich_am", "cong_tac",
        )

        fun docCheDo(s: String?): String? = when (s?.trim()?.lowercase()) {
            "ngu", "sleep" -> "ngu"
            "nha", "o_nha", "home" -> "nha"
            "vang_nha", "vang", "away" -> "vang_nha"
            "tuy_chinh", "custom" -> "tuy_chinh"
            else -> null
        }

        fun tenCheDo(cd: String): String = when (cd) {
            "ngu" -> "Ngủ"
            "nha" -> "Nhà"
            "vang_nha" -> "Vắng nhà"
            else -> "Tùy chỉnh"
        }

        fun tenThietBi(tb: String): String = when (tb) {
            DEN -> "Đèn"
            QUAT -> "Quạt"
            DIEU_HOA -> "Điều hòa"
            REM -> "Rèm"
            LICH_HEN -> "Lịch hẹn"
            NHIET_DO -> "Nhiệt độ"
            CHE_DO -> "Chế độ"
            else -> tb
        }

        /**
         * Khai bao ba cong cu cho Gemini, dung dang `tools` cua Live API:
         * `[{"functionDeclarations": [...]}]`.
         *
         * Vi sao chi ba: model goi dung hon khi it cong cu. Gop het phan doc so
         * lieu vao mot cong cu co tham so `muc` thay vi sau cong cu rieng.
         */
        fun khaiBao(maPhong: List<String>): JSONArray {
            val phongEnum = JSONArray().apply {
                maPhong.forEach { put(it) }
                put(TAT_CA)
                put(NHANH)
            }

            val dieuKhien = JSONObject()
                .put("name", DIEU_KHIEN)
                .put(
                    "description",
                    "Bật, tắt hoặc chỉnh một thiết bị trong nhà. Dùng cho đèn, quạt, " +
                        "điều hòa, nhiệt độ điều hòa, chế độ phòng và lịch hẹn.",
                )
                .put(
                    "parameters",
                    JSONObject()
                        .put("type", "OBJECT")
                        .put(
                            "properties",
                            JSONObject()
                                .put(
                                    "phong",
                                    thamSo(
                                        "Mã phòng. '$TAT_CA' là mọi phòng. " +
                                            "'$NHANH' là bốn ô thao tác nhanh ở Trang chủ.",
                                        phongEnum,
                                    ),
                                )
                                .put(
                                    "thiet_bi",
                                    thamSo(
                                        "Thiết bị cần điều khiển.",
                                        enum(DEN, QUAT, DIEU_HOA, REM, NHIET_DO, CHE_DO, LICH_HEN),
                                    ),
                                )
                                .put(
                                    "hanh_dong",
                                    thamSo(
                                        "'$DAO' là đảo trạng thái. '$DAT' dùng với nhiet_do " +
                                            "hoặc che_do và cần gia_tri.",
                                        enum(BAT, TAT, DAO, DAT, TANG, GIAM),
                                    ),
                                )
                                .put(
                                    "gia_tri",
                                    thamSo(
                                        "Nhiệt độ 16-30 viết dạng số, ví dụ \"24\". Hoặc tên " +
                                            "chế độ: ngu, nha, vang_nha, tuy_chinh.",
                                        null,
                                    ),
                                ),
                        )
                        .put("required", enum("phong", "thiet_bi", "hanh_dong")),
                )

            val moMan = JSONObject()
                .put("name", MO_MAN_HINH)
                .put("description", "Chuyển màn hình đang hiện trên máy tính bảng.")
                .put(
                    "parameters",
                    JSONObject()
                        .put("type", "OBJECT")
                        .put(
                            "properties",
                            JSONObject().put(
                                "dich",
                                thamSo(
                                    "Màn hình cần mở. 'phong:<mã phòng>' mở màn điều khiển " +
                                        "một phòng, ví dụ 'phong:living'.",
                                    enum(
                                        "trang_chu", "thiet_bi", "camera", "cam_bien",
                                        "tu_dong_hoa", "cai_dat", "server", "thong_ke",
                                        "tien_ich", "chi_tiet_dien", "wifi",
                                    ),
                                ),
                            ),
                        )
                        .put("required", enum("dich")),
                )

            val docSo = JSONObject()
                .put("name", DOC_SO_LIEU)
                .put(
                    "description",
                    "Đọc số liệu thật đang có trong nhà. Luôn gọi hàm này trước khi " +
                        "trả lời bất kỳ câu hỏi nào về số - không được tự đoán số.",
                )
                .put(
                    "parameters",
                    JSONObject()
                        .put("type", "OBJECT")
                        .put(
                            "properties",
                            JSONObject().put(
                                "muc",
                                thamSo(
                                    "dien: điện EVN. server: máy chủ serverubuntu. " +
                                        "wifi: modem. thoi_tiet: Open-Meteo. " +
                                        "lich_am: lịch âm hôm nay. " +
                                        "trang_thai_nha: đèn/quạt/điều hòa từng phòng " +
                                        "(gồm cả thiết bị mẫu). " +
                                        "cong_tac: các công tắc Wi-Fi THẬT — đang nối " +
                                        "được không, đang bật hay tắt. Hỏi thiết bị có " +
                                        "bật thật không thì dùng mục này.",
                                    JSONArray().apply { MUC_HOP_LE.forEach { put(it) } },
                                ),
                            ),
                        )
                        .put("required", enum("muc")),
                )

            return JSONArray().put(
                JSONObject().put(
                    "functionDeclarations",
                    JSONArray().put(dieuKhien).put(moMan).put(docSo),
                ),
            )
        }

        private fun thamSo(moTa: String, enum: JSONArray?): JSONObject =
            JSONObject().put("type", "STRING").put("description", moTa)
                .apply { if (enum != null) put("enum", enum) }

        private fun enum(vararg v: String): JSONArray =
            JSONArray().apply { v.forEach { put(it) } }
    }
}
