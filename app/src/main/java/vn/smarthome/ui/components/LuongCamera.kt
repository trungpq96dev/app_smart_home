package vn.smarthome.ui.components

import android.graphics.Color as AColor
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import kotlin.coroutines.resume

/**
 * Khung phat luong camera.
 *
 * Ben trong la mot WebView tro vao trang `stream.html` cua go2rtc. Vi sao
 * khong dung ExoPlayer/libVLC:
 *
 *  - go2rtc phat WebRTC. ExoPlayer khong noi WebRTC, phai lui ve RTSP - ma
 *    RTSP qua ExoPlayer thi tre hon va keo them vai MB thu vien.
 *  - Trang stream.html cua go2rtc TU CHON duong truyen (WebRTC truoc, khong
 *    duoc thi MSE). App khong phai doan, va khi go2rtc nang cap thi app
 *    duoc huong theo ma khong can build lai.
 *  - Khong them phu thuoc nao: WebView co san trong Android.
 *
 * DIEU QUAN TRONG NHAT o day la CAT LUONG khi roi man hinh. Khong cat thi
 * camera van gui hinh ve may tinh bang suot ngay: ton pin, ton bang thong,
 * va go2rtc van dem may tinh bang la mot client dang xem. Nen khi composable
 * nay bi go ra, WebView duoc nap `about:blank` roi huy han.
 */
@Composable
fun LuongCamera(
    url: String,
    taiKhoan: String,
    matKhau: String,
    modifier: Modifier = Modifier,
    onLoi: (String) -> Unit = {},
) {
    var wv by remember { mutableStateOf<WebView?>(null) }
    var dangNoi by remember(url) { mutableStateOf(true) }
    var lanThu by remember(url) { mutableIntStateOf(0) }

    // Man hinh tat / app xuong nen thi tam dung, len lai thi chay tiep.
    val chuSoHuu = LocalLifecycleOwner.current
    DisposableEffect(chuSoHuu) {
        val theoDoi = LifecycleEventObserver { _, bienCo ->
            when (bienCo) {
                Lifecycle.Event.ON_PAUSE -> wv?.onPause()
                Lifecycle.Event.ON_RESUME -> wv?.onResume()
                else -> Unit
            }
        }
        chuSoHuu.lifecycle.addObserver(theoDoi)
        onDispose { chuSoHuu.lifecycle.removeObserver(theoDoi) }
    }

    /*
     * Vi sao phai co vong canh chung nay.
     *
     * Lan noi dau toi camera Tuya mat 12-25 giay: go2rtc phai dang nhap len
     * may chu Tuya, bat tay MQTT (lan dau thuong hong mot lan roi no tu thu
     * lai), xong moi mo duoc duong P2P toi camera. Trong luc do trang
     * stream.html in ra dung mot dong do cua no - "streams: tuya: mqtt:
     * disconnect" - roi NAM IM, khong tu thu lai.
     *
     * Nguoi dung nhin vao chi thay mot dong chu la giua khung den va tuong
     * la hong. Chinh nguoi viet app nay da tuong nhu vay ngay 18/11/2026 va
     * di do nham IPv6, DNS, dong ho, Tailscale mat ca tieng - trong khi doi
     * them muoi giay nua la len hinh.
     *
     * Nen: cu 1,5 giay hoi WebView dang o trang thai nao, va che kin trang
     * do cho toi khi co hinh that.
     *
     * Vi sao phan biet "dang cho" voi "bao loi" thay vi chi dem gio: go2rtc
     * bo cuoc roi KHONG tu thu lai khi nguoi xem noi bang websocket (log:
     * `dial cons=1 prod=0`). Ngoi cho het gio roi moi nap lai la phi thoi
     * gian khong lam gi.
     *
     * Va khong nap lai khi dang "cho": luc do go2rtc dang bat tay do, nap lai
     * la giet dung lan thu sap thanh cong (log: `websocket: close 1001`).
     *
     * THU THUA RA, DUNG THU DAY. Moi lan nap lai la mot lan go2rtc dang nhap
     * lai len may chu Tuya. Ngay 18/11/2026 da do duoc: thu lai 6 lan lien
     * tuc thi Tuya chan tam, moi lan noi deo dai 12-21 giay roi hong han; de
     * yen 4 phut roi thu DUNG MOT lan thi len hinh trong 4,3 giay. Nen gian
     * cach tang dan [CHO_THU_LAI], va chi thu 3 lan.
     */
    LaunchedEffect(url) {
        dangNoi = true
        lanThu = 0
        var moc = System.currentTimeMillis()

        suspend fun napLai(v: WebView): Boolean {
            if (lanThu >= CHO_THU_LAI.size) {
                onLoi(
                    "Không lên được hình sau " + lanThu + " lần thử. Kiểm tra go2rtc " +
                        "còn chạy không, và camera có đang bị app Tuya chiếm không. " +
                        "Thử lại sau vài phút — máy chủ Tuya có thể đang chặn tạm."
                )
                return false
            }
            delay(CHO_THU_LAI[lanThu])
            lanThu++
            v.reload()
            moc = System.currentTimeMillis()
            return true
        }

        while (true) {
            delay(1500)
            val v = wv ?: continue
            when (hoiTrangThai(v)) {
                TrangThaiTrang.CHAY -> {
                    dangNoi = false
                    lanThu = 0
                    moc = System.currentTimeMillis()
                }

                TrangThaiTrang.LOI -> {
                    dangNoi = true
                    if (!napLai(v)) return@LaunchedEffect
                }

                TrangThaiTrang.CHO -> {
                    // Dang chay roi bong dung dut - quay lai trang thai cho.
                    if (!dangNoi) {
                        dangNoi = true
                        moc = System.currentTimeMillis()
                    }
                    // Luoi an toan: khong bao gio bao loi ma cung khong chay.
                    // 25 giay la rong rai - lan noi binh thuong mat 4-12 giay.
                    if (System.currentTimeMillis() - moc > 25_000) {
                        if (!napLai(v)) return@LaunchedEffect
                    }
                }
            }
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setBackgroundColor(AColor.BLACK)
                    // Khung xem, khong phai trang web - bo het cuon va hieu
                    // ung keo le cho no khong nhun nhay khi cham vao.
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER

                    settings.apply {
                        javaScriptEnabled = true          // stream.html la mot module JS
                        domStorageEnabled = true
                        // Khong co nut Play trong khung nay, nen hinh phai tu
                        // chay. Thieu dong nay thi khung den cho toi khi cham.
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {

                        /**
                         * go2rtc co the bat tai khoan cho API. WebView khong
                         * tu biet, phai dua tay vao day.
                         */
                        override fun onReceivedHttpAuthRequest(
                            view: WebView?,
                            handler: HttpAuthHandler?,
                            host: String?,
                            realm: String?,
                        ) {
                            if (taiKhoan.isNotBlank()) handler?.proceed(taiKhoan, matKhau)
                            else handler?.cancel()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            // Chi bao loi cua chinh trang, bo qua icon va
                            // manifest.json ma stream.html lay tu go2rtc.org
                            // - may tinh bang trong nha khong ra internet duoc
                            // thi nhung thu do hong la binh thuong.
                            if (request?.isForMainFrame != true) return
                            val moTa = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                error?.description?.toString() ?: ""
                            } else {
                                ""
                            }
                            onLoi(
                                "Không mở được khung hình" +
                                    (if (moTa.isBlank()) "" else ": " + moTa)
                            )
                        }
                    }
                    wv = this
                    tag = url
                    loadUrl(url)
                }
            },
            update = { view ->
                // So voi tag chu KHONG so voi view.url: ngay sau factory,
                // loadUrl chua chay xong nen view.url con null, so kieu do la
                // nap trang lan thu hai. Hai lan nap = hai ket noi WebRTC toi
                // go2rtc trong vai giay, ton gap doi bang thong ma khong lam
                // duoc gi them.
                if (view.tag != url) {
                    view.tag = url
                    view.loadUrl(url)
                }
            },
            onRelease = { view ->
                // Thu tu nay co y: nap trang trong TRUOC de WebRTC dong
                // ket noi tu te, roi moi huy WebView.
                view.loadUrl("about:blank")
                view.stopLoading()
                view.onPause()
                (view.parent as? ViewGroup)?.removeView(view)
                view.destroy()
                wv = null
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (dangNoi) {
            Box(
                // Duc HAN, khong de lot: dong loi cua go2rtc ma lo mo hien ra
                // sau lop che thi van du lam nguoi xem tuong hong.
                Modifier.fillMaxSize().background(SH.Bg),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(
                        color = SH.Blue,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(34.dp),
                    )
                    Text(
                        if (lanThu == 0) "Đang nối camera..." else "Thử lại lần " + lanThu + "...",
                        style = SHType.CardTitle,
                        color = SH.Text,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        "Lần đầu thường mất 10–25 giây: go2rtc phải đăng nhập Tuya rồi " +
                            "mới mở được đường thẳng tới camera.",
                        style = SHType.Caption,
                        color = SH.TextDim,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, start = 40.dp, end = 40.dp),
                    )
                }
            }
        }
    }
}

/**
 * Cho bao lau truoc moi lan nap lai, theo ms. So phan tu = so lan thu.
 *
 * Tang dan chu khong deu: lan hong dau thuong chi la cham, nhung hong lien
 * tiep thuong la may chu Tuya dang chan tam - luc do cang thu day cang lau
 * duoc phuc vu.
 */
private val CHO_THU_LAI = longArrayOf(4_000, 15_000, 40_000)

private enum class TrangThaiTrang { CHAY, CHO, LOI }

/**
 * Doan JS hoi thang trang stream.html dang o trang thai nao.
 *
 * go2rtc bao hong bang cach in chu thang vao trang chu khong nem su kien gi,
 * nen phai doc chu. Cac tu khoa duoi day lay tu thong bao that gap tren may:
 * "streams: tuya: mqtt: disconnect".
 */
private const val JS_KIEM = """
(function () {
  try {
    var h = document.querySelector('video-stream');
    if (!h) return 'cho';
    var v = h.querySelector('video') || (h.shadowRoot && h.shadowRoot.querySelector('video'));
    if (v && v.videoWidth > 0 && v.currentTime > 0 && !v.paused) return 'chay';
    var chu = (document.body.innerText || '') + ' ' + (h.textContent || '');
    chu = chu.toLowerCase();
    if (chu.indexOf('disconnect') >= 0 || chu.indexOf('error') >= 0 ||
        chu.indexOf('streams:') >= 0 || chu.indexOf('404') >= 0) return 'loi';
    return 'cho';
  } catch (e) { return 'cho'; }
})()
"""

/**
 * Hoi WebView mot cau va cho tra loi.
 *
 * `evaluateJavascript` phai goi tren luong giao dien - [LaunchedEffect] chay
 * dung tren luong do nen khong phai chuyen luong.
 */
private suspend fun hoiTrangThai(v: WebView): TrangThaiTrang =
    suspendCancellableCoroutine { cont ->
        v.evaluateJavascript(JS_KIEM) { kq ->
            if (!cont.isActive) return@evaluateJavascript
            val t = kq ?: ""
            cont.resume(
                when {
                    t.contains("chay") -> TrangThaiTrang.CHAY
                    t.contains("loi") -> TrangThaiTrang.LOI
                    else -> TrangThaiTrang.CHO
                }
            )
        }
    }
