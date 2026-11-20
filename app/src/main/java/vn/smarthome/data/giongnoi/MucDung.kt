package vn.smarthome.data.giongnoi

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Muc dung cua tro ly giong noi trong MOT ngay.
 *
 * [giay] do chinh app dem nen chac chan dung. [tokenTong] lay tu truong
 * `usageMetadata` ma Gemini gui ve - xem ghi chu o [MucDung.gopPhien] ve chuyen
 * con so nay dang la CAN DUOI chu chua chac la so chinh xac.
 */
data class MucDungNgay(
    /** Dang ISO: "2026-11-20". De chuoi de luu JSON va so sanh duoc bang <. */
    val ngay: String,
    val giay: Int = 0,
    val soPhien: Int = 0,
    val tokenVao: Long = 0,
    val tokenRa: Long = 0,
    val tokenTong: Long = 0,
) {
    val phut: Double get() = giay / 60.0
}

object MucDung {

    /** Giu lai bao nhieu ngay. Du de nhin xu huong ma khong phinh DataStore. */
    const val SO_NGAY_GIU = 30

    /**
     * Cong mot phien vua xong vao danh sach ngay.
     *
     * Tach rieng ham thuan nay de kiem thu chay tren JVM: phan de sai nhat la
     * cong nham sang ngay khac hoac lam mat ngay cu, chu khong phai phan
     * DataStore.
     */
    fun gopPhien(
        cu: List<MucDungNgay>,
        homNay: String,
        giay: Int,
        tokenVao: Long,
        tokenRa: Long,
        tokenTong: Long,
    ): List<MucDungNgay> {
        val ds = cu.toMutableList()
        val i = ds.indexOfFirst { it.ngay == homNay }
        if (i >= 0) {
            val n = ds[i]
            ds[i] = n.copy(
                giay = n.giay + giay,
                soPhien = n.soPhien + 1,
                tokenVao = n.tokenVao + tokenVao,
                tokenRa = n.tokenRa + tokenRa,
                tokenTong = n.tokenTong + tokenTong,
            )
        } else {
            ds += MucDungNgay(homNay, giay, 1, tokenVao, tokenRa, tokenTong)
        }
        // Sap theo ngay roi cat bot - khong cat thi sau mot nam DataStore giu
        // 365 ban ghi ma khong ai doc toi.
        return ds.sortedBy { it.ngay }.takeLast(SO_NGAY_GIU)
    }

    fun cuaNgay(ds: List<MucDungNgay>, ngay: String): MucDungNgay =
        ds.firstOrNull { it.ngay == ngay } ?: MucDungNgay(ngay)

    /** Tong giay cua n ngay gan nhat, ke ca hom nay. */
    fun giayNgayGanDay(ds: List<MucDungNgay>, soNgay: Int): Int =
        ds.takeLast(soNgay).sumOf { it.giay }

    fun homNay(): String = LocalDate.now().toString()

    // ---- doc/ghi JSON ----

    fun sangJson(ds: List<MucDungNgay>): String {
        val m = JSONArray()
        ds.forEach { n ->
            m.put(
                JSONObject()
                    .put("ngay", n.ngay)
                    .put("giay", n.giay)
                    .put("phien", n.soPhien)
                    .put("tv", n.tokenVao)
                    .put("tr", n.tokenRa)
                    .put("tt", n.tokenTong),
            )
        }
        return m.toString()
    }

    /**
     * Doc nguoc lai. Hong thi tra danh sach RONG chu khong nem ngoai le:
     * mat so thong ke la phien toai, con chet app luc mo man Cai dat la hong.
     */
    fun tuJson(s: String?): List<MucDungNgay> {
        if (s.isNullOrBlank()) return emptyList()
        return try {
            val m = JSONArray(s)
            (0 until m.length()).mapNotNull { i ->
                val o = m.optJSONObject(i) ?: return@mapNotNull null
                val ngay = o.optString("ngay")
                if (ngay.isBlank()) return@mapNotNull null
                MucDungNgay(
                    ngay = ngay,
                    giay = o.optInt("giay"),
                    soPhien = o.optInt("phien"),
                    tokenVao = o.optLong("tv"),
                    tokenRa = o.optLong("tr"),
                    tokenTong = o.optLong("tt"),
                )
            }.sortedBy { it.ngay }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private val Context.dataStoreMucDung by preferencesDataStore(name = "muc_dung_giong_noi")

class MucDungStore(private val ctx: Context) {

    private val kDs = stringPreferencesKey("muc_dung_json")

    val mucDung: Flow<List<MucDungNgay>> =
        ctx.dataStoreMucDung.data.map { p -> MucDung.tuJson(p[kDs]) }

    suspend fun ghiPhien(giay: Int, tokenVao: Long, tokenRa: Long, tokenTong: Long) {
        if (giay <= 0 && tokenTong <= 0) return
        ctx.dataStoreMucDung.edit { p ->
            val cu = MucDung.tuJson(p[kDs])
            p[kDs] = MucDung.sangJson(
                MucDung.gopPhien(cu, MucDung.homNay(), giay, tokenVao, tokenRa, tokenTong),
            )
        }
    }

    suspend fun xoaHet() {
        ctx.dataStoreMucDung.edit { p -> p.remove(kDs) }
    }
}
