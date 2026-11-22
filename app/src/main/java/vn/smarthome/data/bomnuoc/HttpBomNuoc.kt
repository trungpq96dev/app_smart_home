package vn.smarthome.data.bomnuoc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Duong HTTP truc tiep trong mang LAN toi bo `bom_nuoc`.
 *
 * WebServer cua ESP32 chay DONG BO (chi phuc vu 1 ket noi mot luc), nen moi
 * request deu phai di qua [cong] (`Mutex`). Nhip poll tu dong thich ung:
 * 1 giay khi nguoi dung dang mo man Tu dong hoa hoac bom dang chay, va gian
 * ra 5 giay khi o man khac va he thong dang nghi.
 */
class HttpBomNuoc(
    private val host: String,
    private val dangXemMan: StateFlow<Boolean>,
    private val khiNhanTrangThai: (BomNuocState) -> Unit,
) {
    companion object {
        private const val POLL_NHANH_MS = 1_000L
        private const val POLL_CHAM_MS = 5_000L
        private const val POLL_USAGE_MS = 30_000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cong = Mutex()

    private val _state = MutableStateFlow<BomNuocState?>(null)
    val state: StateFlow<BomNuocState?> = _state.asStateFlow()

    private val _usage = MutableStateFlow<List<LuongNuocThang>?>(null)
    val usage: StateFlow<List<LuongNuocThang>?> = _usage.asStateFlow()

    private val _link = MutableStateFlow<TrangThaiKetNoiBom>(TrangThaiKetNoiBom.DangNoi)
    val link: StateFlow<TrangThaiKetNoiBom> = _link.asStateFlow()

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val baseUrl: HttpUrl? = "http://${CauHinhBomNuocStore.chuanHoaHost(host)}/".toHttpUrlOrNull()

    fun batDau() {
        if (baseUrl == null) {
            _link.value = TrangThaiKetNoiBom.Loi("địa chỉ bo LAN không hợp lệ")
            return
        }
        scope.launch { pollState() }
        scope.launch { pollUsage() }
    }

    fun dung() {
        scope.cancel()
        _link.value = TrangThaiKetNoiBom.ChuaCauHinh
    }

    private suspend fun pollState() {
        while (scope.isActive) {
            cong.withLock {
                docState()
                    .onSuccess { s ->
                        _state.value = s
                        _link.value = TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.LAN, 0)
                        khiNhanTrangThai(s)
                    }
                    .onFailure {
                        _link.value = TrangThaiKetNoiBom.BoOffline
                    }
            }
            val nhanh = dangXemMan.value ||
                _state.value?.bomChinhDangChay == true ||
                _state.value?.bomApDangChay == true
            delay(if (nhanh) POLL_NHANH_MS else POLL_CHAM_MS)
        }
    }

    private suspend fun pollUsage() {
        while (scope.isActive) {
            if (_link.value is TrangThaiKetNoiBom.TrucTiep || _state.value != null) {
                cong.withLock {
                    docUsage().onSuccess { _usage.value = it }
                }
            }
            delay(if (_usage.value == null) 3_000L else POLL_USAGE_MS)
        }
    }

    suspend fun thuGoi(): Boolean = withContext(Dispatchers.IO) {
        if (baseUrl == null) return@withContext false
        cong.withLock { docState().isSuccess }
    }

    suspend fun guiLenh(query: String): Result<BomNuocState> = withContext(Dispatchers.IO) {
        val params = tachQuery(query)
        if (params.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("lệnh rỗng"))
        }
        val root = baseUrl
            ?: return@withContext Result.failure(IllegalStateException("địa chỉ bo LAN không hợp lệ"))
        val url = root.newBuilder().addPathSegments("api/cmd").apply {
            params.forEach { (k, v) -> addQueryParameter(k, v) }
        }.build()

        cong.withLock {
            goiGet(url) { body ->
                BomNuocState.tuJson(body) ?: throw IOException("JSON trạng thái không hợp lệ")
            }.onSuccess { s ->
                _state.value = s
                _link.value = TrangThaiKetNoiBom.TrucTiep(KenhBomNuoc.LAN, 0)
                khiNhanTrangThai(s)
            }
        }
    }

    private fun docState(): Result<BomNuocState> {
        val root = baseUrl ?: return Result.failure(IllegalStateException("địa chỉ bo không hợp lệ"))
        val url = root.newBuilder().addPathSegments("api/state").build()
        return goiGet(url) { body ->
            BomNuocState.tuJson(body) ?: throw IOException("JSON trạng thái không hợp lệ")
        }
    }

    private fun docUsage(): Result<List<LuongNuocThang>> {
        val root = baseUrl ?: return Result.failure(IllegalStateException("địa chỉ bo không hợp lệ"))
        val url = root.newBuilder().addPathSegments("api/usage").build()
        return goiGet(url) { body ->
            LuongNuocThang.tuJsonArray(body) ?: throw IOException("JSON usage không hợp lệ")
        }
    }

    private inline fun <T> goiGet(url: HttpUrl, parse: (String) -> T): Result<T> = runCatching {
        val req = Request.Builder().url(url).get().build()
        okHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw IOException("Phản hồi rỗng")
            parse(body)
        }
    }

    private fun tachQuery(q: String): List<Pair<String, String>> =
        q.split('&').mapNotNull { cap ->
            val i = cap.indexOf('=')
            if (i <= 0) null else cap.substring(0, i) to cap.substring(i + 1)
        }
}
