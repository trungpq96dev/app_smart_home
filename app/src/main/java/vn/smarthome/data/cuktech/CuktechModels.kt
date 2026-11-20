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
}

/**
 * Bon cong cua cu sac.
 *
 * [khoaStatus] la khoa trong `cuktech/charger/status` (JSON danh so "1".."4"),
 * [khoaTopic] la duoi cua topic cong roi. Hai cach goi khac nhau cho cung mot
 * cong - do la cua du an goc, khong phai cho tu nghi ra.
 */
enum class CongSacId(val khoaStatus: String, val khoaTopic: String, val nhan: String) {
    C1("1", "c1", "C1"),
    C2("2", "c2", "C2"),
    C3("3", "c3", "C3"),
    USB_A("4", "a", "USB-A");

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
) {
    /**
     * Tong cong suat.
     *
     * Cu sac KHONG phat so tong - day la tong cong lai tu 4 cong. Man hinh phai
     * noi ro nhu vay, dung de nguoi dung tuong day la so cu sac tu bao.
     */
    val tongOat: Float get() = cong.values.fold(0f) { t, c -> t + c.oat }

    val soCongDangChay: Int get() = cong.values.count { it.dangChay }

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

                    // `cuktech/charger/settings` la bang PIID tho
                    // ({"5":1,"17":120391469,...}) chua ai giai nghia. Bo qua
                    // han: doan y nghia roi hien len man hinh la bia.
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
            return cur.copy(
                coSoThat = true,
                noiDuoc = o.optBoolean("connected", cur.noiDuoc),
                xacThuc = o.optBoolean("authenticated", cur.xacThuc),
                model = o.optString("device_model", "").ifBlank { cur.model },
                firmware = o.optString("firmware_version", "").ifBlank { cur.firmware },
                cong = cong,
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
