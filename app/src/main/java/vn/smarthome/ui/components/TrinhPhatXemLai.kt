package vn.smarthome.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Khung phat video MP4 xem lai 24/24 dung `MediaPlayer` + `TextureView` co san cua Android.
 *
 * Vi sao dung `TextureView` + `MediaPlayer` (API 26+):
 *  - Ho tro san HTTP `Range: bytes=...` va header `Authorization: Basic ...` qua
 *    `setDataSource(ctx, uri, headers)` -> tua giua doan 10 phut muot ngay.
 *  - Ho tro doi toc do phat `1x / 2x / 4x` bang `PlaybackParams`.
 *  - `TextureView` cat bo goc tron va ve chu de Compose len tren khong bi thung nen
 *    nhu `SurfaceView`, ma APK khong tang them mot KB thu vien ngoai nao.
 */
@Composable
fun TrinhPhatXemLai(
    url: String,
    headers: Map<String, String>,
    seekOffsetMs: Long,
    seekVersion: Int,
    dangPhat: Boolean,
    tocDo: Float,
    laTuDrive: Boolean,
    onTienTrinh: (Long) -> Unit,
    onHetDoan: () -> Unit,
    onLoi: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val curOnTienTrinh by rememberUpdatedState(onTienTrinh)
    val curOnHetDoan by rememberUpdatedState(onHetDoan)
    val curOnLoi by rememberUpdatedState(onLoi)
    val curDangPhat by rememberUpdatedState(dangPhat)
    val curTocDo by rememberUpdatedState(tocDo)

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var surface by remember { mutableStateOf<Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<TextureView?>(null) }
    var daSanSang by remember(url) { mutableStateOf(false) }
    var dangNap by remember(url) { mutableStateOf(true) }
    var videoW by remember { mutableIntStateOf(0) }
    var videoH by remember { mutableIntStateOf(0) }
    var seekChoSanMs by remember(url) { mutableStateOf(seekOffsetMs) }

    // Ap dung ty le khung hinh fit-center de hinh camera khong bi keo meo
    fun canChinhTyLe() {
        val tv = textureViewRef ?: return
        val vw = tv.width.toFloat()
        val vh = tv.height.toFloat()
        if (vw <= 0f || vh <= 0f || videoW <= 0 || videoH <= 0) return

        val tyLeVideo = videoW.toFloat() / videoH.toFloat()
        val tyLeKhung = vw / vh
        val sx: Float
        val sy: Float
        if (tyLeVideo > tyLeKhung) {
            sx = 1f
            sy = tyLeKhung / tyLeVideo
        } else {
            sx = tyLeVideo / tyLeKhung
            sy = 1f
        }
        val matrix = Matrix()
        matrix.setScale(sx, sy, vw / 2f, vh / 2f)
        tv.setTransform(matrix)
    }

    fun apDungTocDoVaPhat(mp: MediaPlayer, phat: Boolean, speed: Float) {
        try {
            if (phat) {
                mp.playbackParams = mp.playbackParams.setSpeed(speed.coerceIn(0.5f, 4.0f))
                if (!mp.isPlaying) mp.start()
            } else if (mp.isPlaying) {
                mp.pause()
            }
        } catch (_: IllegalStateException) {
            if (phat) runCatching { mp.start() }
        }
    }

    // Tam dung khi tat man hinh may tinh bang
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_STOP) {
                runCatching { player?.takeIf { it.isPlaying }?.pause() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Nap lai nguon khi doi segment URL hoac khi Surface vua tao xong
    DisposableEffect(url, surface) {
        val sf = surface
        if (sf == null || url.isBlank()) {
            return@DisposableEffect onDispose {}
        }

        daSanSang = false
        dangNap = true
        val mp = MediaPlayer()
        player = mp

        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mp.setSurface(sf)
            mp.setDataSource(ctx, Uri.parse(url), headers)
            mp.setOnVideoSizeChangedListener { _, w, h ->
                videoW = w
                videoH = h
                canChinhTyLe()
            }
            mp.setOnPreparedListener { p ->
                daSanSang = true
                dangNap = false
                canChinhTyLe()
                val dichMs = seekChoSanMs.coerceAtLeast(0L)
                if (dichMs > 500L) {
                    runCatching { p.seekTo(dichMs, MediaPlayer.SEEK_CLOSEST) }
                }
                apDungTocDoVaPhat(p, curDangPhat, curTocDo)
            }
            mp.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaPlayer.MEDIA_INFO_BUFFERING_START -> dangNap = true
                    MediaPlayer.MEDIA_INFO_BUFFERING_END,
                    MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> dangNap = false
                }
                false
            }
            mp.setOnCompletionListener {
                curOnHetDoan()
            }
            mp.setOnErrorListener { _, what, extra ->
                dangNap = false
                curOnLoi("Không phát được đoạn video này (mã $what/$extra).")
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            dangNap = false
            curOnLoi("Lỗi mở luồng phát lại: " + (e.message ?: e.javaClass.simpleName))
        }

        onDispose {
            daSanSang = false
            player = null
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
    }

    // Tua khi nguoi dung tha tay tren thanh Timeline hoac bam su kien nguoi (cung segment)
    LaunchedEffect(seekVersion) {
        seekChoSanMs = seekOffsetMs
        val mp = player
        if (mp != null && daSanSang) {
            runCatching {
                mp.seekTo(seekOffsetMs.coerceAtLeast(0L), MediaPlayer.SEEK_CLOSEST)
            }
        }
    }

    // Doi trang thai Play/Pause hoac toc do 1x/2x/4x
    LaunchedEffect(dangPhat, tocDo, daSanSang) {
        val mp = player ?: return@LaunchedEffect
        if (daSanSang) {
            apDungTocDoVaPhat(mp, dangPhat, tocDo)
        }
    }

    // Cap nhat kim dong ho tren thanh Timeline moi 300ms khi dang phat
    LaunchedEffect(daSanSang, dangPhat) {
        while (daSanSang && dangPhat) {
            val mp = player
            if (mp != null) {
                val pos = runCatching { if (mp.isPlaying) mp.currentPosition.toLong() else -1L }
                    .getOrDefault(-1L)
                if (pos >= 0L) curOnTienTrinh(pos)
            }
            delay(300L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            surface?.release()
            surface = null
        }
    }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    isOpaque = false
                    addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> canChinhTyLe() }
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                            surface?.release()
                            surface = Surface(st)
                            canChinhTyLe()
                        }

                        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                            canChinhTyLe()
                        }

                        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                            surface?.release()
                            surface = null
                            return true
                        }

                        override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                    }
                    textureViewRef = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (dangNap) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(SH.Bg.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    CircularProgressIndicator(
                        color = if (laTuDrive) SH.Blue else SH.Green,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(26.dp),
                    )
                    Text(
                        if (laTuDrive) "Đang kéo đoạn video từ Google Drive 5TB..."
                        else "Đang nạp đoạn ghi hình...",
                        style = SHType.BodyMed,
                        color = SH.Text,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
