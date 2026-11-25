package vn.smarthome.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.smarthome.data.camera.DoanVideo
import vn.smarthome.data.camera.SuKienCamera
import vn.smarthome.data.camera.dinhDangGioVn
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

enum class MucZoomTimeline(val nhan: String, val cuaSoGiay: Long, val buocVachChinhGiay: Long) {
    GIO_24("24h", 86_400L, 14_400L), // Cua so 24h, nhan chu moi 4 gio
    GIO_6("6h", 21_600L, 3_600L),    // Cua so 6h, nhan chu moi 1 gio
    GIO_1("1h", 3_600L, 600L),       // Cua so 1h, nhan chu moi 10 phut
}

/**
 * Thanh dong thoi gian 24/24 kieu Imou / EZVIZ:
 *  - Dai xanh lam = co video ghi 24/24 (sang hon neu con o bo dem 10 GB, dam hon neu tren Drive).
 *  - Vach cam/do noi bat = khoang phat hien CO NGUOI.
 *  - Keo ngang bang ngon tay hoac cham vao vi tri bat ky tren thuoc de nhay toi moc do.
 */
@Composable
fun ThanhThoiGianCamera(
    dayStartTs: Long,
    dayEndTs: Long,
    currentTs: Long,
    zoom: MucZoomTimeline,
    segments: List<DoanVideo>,
    events: List<SuKienCamera>,
    onChonMoc: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val curCurrentTs by rememberUpdatedState(currentTs)
    val curDayStart by rememberUpdatedState(dayStartTs)
    val curDayEnd by rememberUpdatedState(dayEndTs)
    val curZoom by rememberUpdatedState(zoom)
    val curOnChonMoc by rememberUpdatedState(onChonMoc)

    var dangKeo by remember { mutableStateOf(false) }
    var mocKeoFloat by remember { mutableFloatStateOf(currentTs.toFloat()) }
    var chieuRongPx by remember { mutableFloatStateOf(1f) }

    val tamHienThiTs: Long = if (dangKeo) {
        mocKeoFloat.roundToLong().coerceIn(dayStartTs, max(dayStartTs, dayEndTs - 1L))
    } else {
        currentTs.coerceIn(dayStartTs, max(dayStartTs, dayEndTs - 1L))
    }

    val density = LocalDensity.current
    val textPaint = remember(density) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = SH.TextDim.toArgb()
            textSize = with(density) { 9.5.sp.toPx() }
            textAlign = Paint.Align.CENTER
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
            .pointerInput(dayStartTs, dayEndTs) {
                detectTapGestures { offset ->
                    if (chieuRongPx <= 1f || curDayEnd <= curDayStart) return@detectTapGestures
                    val giayMoiPx = curZoom.cuaSoGiay.toFloat() / chieuRongPx
                    val lechPx = offset.x - chieuRongPx * 0.5f
                    val mocMoi = (curCurrentTs + (lechPx * giayMoiPx).roundToLong())
                        .coerceIn(curDayStart, curDayEnd - 1L)
                    curOnChonMoc(mocMoi)
                }
            }
            .pointerInput(dayStartTs, dayEndTs) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        dangKeo = true
                        mocKeoFloat = curCurrentTs.toFloat()
                    },
                    onDragEnd = {
                        val chot = mocKeoFloat.roundToLong()
                            .coerceIn(curDayStart, max(curDayStart, curDayEnd - 1L))
                        dangKeo = false
                        curOnChonMoc(chot)
                    },
                    onDragCancel = {
                        dangKeo = false
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        if (chieuRongPx <= 1f) return@detectHorizontalDragGestures
                        val giayMoiPx = curZoom.cuaSoGiay.toFloat() / chieuRongPx
                        // Keo thuoc sang trai (dragAmount < 0) -> tien ve tuong lai (giong Imou/EZVIZ)
                        mocKeoFloat = (mocKeoFloat - dragAmount * giayMoiPx)
                            .coerceIn(curDayStart.toFloat(), max(curDayStart, curDayEnd - 1L).toFloat())
                    },
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            chieuRongPx = size.width
            val w = size.width
            val h = size.height
            if (w <= 1f || h <= 1f || dayEndTs <= dayStartTs) return@Canvas

            val nuaCuaSo = zoom.cuaSoGiay / 2.0
            val traiTs = tamHienThiTs - nuaCuaSo
            val phaiTs = tamHienThiTs + nuaCuaSo
            val pxMoiGiay = w / zoom.cuaSoGiay.toFloat()

            fun tsSangX(ts: Long): Float = ((ts - traiTs) * pxMoiGiay).toFloat()

            val barTop = h * 0.24f
            val barH = h * 0.44f
            val barBottom = barTop + barH

            // 1. Nen ranh video 00:00..24:00 cua ngay dang chon
            val ngayX1 = max(0f, tsSangX(dayStartTs))
            val ngayX2 = min(w, tsSangX(dayEndTs))
            if (ngayX2 > ngayX1) {
                drawRoundRect(
                    color = SH.Card2,
                    topLeft = Offset(ngayX1, barTop),
                    size = Size(ngayX2 - ngayX1, barH),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }

            // 2. Cac doan video da ghi 24/24 (xanh lam)
            for (seg in segments) {
                if (seg.endTs <= traiTs || seg.startTs >= phaiTs) continue
                val x1 = max(0f, tsSangX(seg.startTs))
                val x2 = min(w, tsSangX(seg.endTs))
                if (x2 > x1) {
                    val mauDoan = if (seg.laCucBo) {
                        SH.Blue.copy(alpha = 0.56f)
                    } else {
                        SH.BlueDeep.copy(alpha = 0.44f)
                    }
                    drawRect(
                        color = mauDoan,
                        topLeft = Offset(x1, barTop),
                        size = Size(max(1.5f, x2 - x1), barH),
                    )
                }
            }

            // 3. Vach cam/do noi bat tai nhung luc PHAT HIEN NGUOI
            val doRongToiThieuPx = 3.dp.toPx()
            for (ev in events) {
                if (!ev.laNguoi) continue
                if (ev.endTs < traiTs || ev.startTs > phaiTs) continue
                val x1 = tsSangX(ev.startTs)
                val x2 = max(x1 + doRongToiThieuPx, tsSangX(ev.endTs))
                val cx1 = max(0f, x1)
                val cx2 = min(w, x2)
                if (cx2 > cx1) {
                    drawRect(
                        color = SH.Orange,
                        topLeft = Offset(cx1, barTop - 2.dp.toPx()),
                        size = Size(cx2 - cx1, barH + 4.dp.toPx()),
                    )
                }
            }

            // 4. Vach chia gio & chu gio tren thuoc
            val buocNho = zoom.buocVachChinhGiay / 4L
            val mocBatDauVach = max(
                dayStartTs,
                dayStartTs + (((traiTs.toLong() - dayStartTs) / buocNho) * buocNho),
            )
            var t = mocBatDauVach
            while (t <= min(dayEndTs, phaiTs.toLong() + buocNho)) {
                if (t >= dayStartTs && t <= dayEndTs) {
                    val x = tsSangX(t)
                    if (x in 0f..w) {
                        val laVachChinh = ((t - dayStartTs) % zoom.buocVachChinhGiay) == 0L
                        drawLine(
                            color = if (laVachChinh) SH.TextDim.copy(alpha = 0.55f) else SH.Border,
                            start = Offset(x, barBottom),
                            end = Offset(x, barBottom + (if (laVachChinh) 6.dp.toPx() else 3.dp.toPx())),
                            strokeWidth = 1.dp.toPx(),
                        )
                        if (laVachChinh && x in 18f..(w - 18f)) {
                            val nhan = if (t == dayEndTs) "24:00" else dinhDangGioVn(t).substring(0, 5)
                            drawContext.canvas.nativeCanvas.drawText(
                                nhan,
                                x,
                                h - 3.dp.toPx(),
                                textPaint,
                            )
                        }
                    }
                }
                t += buocNho
            }

            // 5. Kim do co dinh chinh giua thuoc
            val midX = w * 0.5f
            drawLine(
                color = if (dangKeo) SH.Amber else SH.Red,
                start = Offset(midX, 0f),
                end = Offset(midX, h),
                strokeWidth = 2.dp.toPx(),
            )
            val tamGiac = Path().apply {
                moveTo(midX - 5.dp.toPx(), 0f)
                lineTo(midX + 5.dp.toPx(), 0f)
                lineTo(midX, 6.dp.toPx())
                close()
            }
            drawPath(tamGiac, color = if (dangKeo) SH.Amber else SH.Red)
        }

        // Hop hien thi gio chinh xac HH:mm:ss o dinh kim giua
        Text(
            dinhDangGioVn(tamHienThiTs),
            style = SHType.Caption,
            color = if (dangKeo) SH.Amber else SH.Text,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SH.Bg.copy(alpha = 0.86f))
                .padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}
