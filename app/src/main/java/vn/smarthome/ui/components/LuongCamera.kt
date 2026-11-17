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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

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
    }
}
