package vn.smarthome.ui.components

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import vn.smarthome.data.HallwayView
import vn.smarthome.data.RoomUi
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

private const val HW_W = 800f
private const val HW_H = 500f

private val IR_MATRIX_VALUES = floatArrayOf(
    0.08f, 0.16f, 0.03f, 0f, -6f,
    0.34f, 0.66f, 0.13f, 0f, -8f,
    0.10f, 0.20f, 0.04f, 0f, -6f,
    0f, 0f, 0f, 1f, 0f,
)

private val IDENTITY_MATRIX_VALUES = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/**
 * Khung mo phong 2.5D Hanh lang - Nha mai Nhat 2 tang.
 *
 * Dong bo day du voi file thiet ke HTML:
 *  - 3 goc nhin Vector 2.5D chuyen muot (Dau he, Xung quanh, Tu xa)
 *  - 3 cum den dieu khien doc lap: Den hanh lang, Den phia truoc nha, Den xung quanh
 *  - Khoa cua chinh thong minh (LED xanh / do), Camera CCTV quet FOV, che do Hong ngoai (IR)
 *  - Cam bien chuyen dong PIR: vien nhap nhay do, 3 vong song Radar, banner canh bao
 */
@Composable
fun HallwayStage(
    room: RoomUi,
    onSelectView: (HallwayView) -> Unit = {},
    onDismissMotion: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val fHall by animateFloatAsState(
        targetValue = if (room.lightOn) room.lightBrightness.coerceIn(10, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "hwHallLight",
    )
    val fFront by animateFloatAsState(
        targetValue = if (room.hallFrontLightOn) room.hallFrontBrightness.coerceIn(10, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "hwFrontLight",
    )
    val fSurr by animateFloatAsState(
        targetValue = if (room.hallSurroundingLightOn) room.hallSurroundingBrightness.coerceIn(10, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "hwSurrLight",
    )
    val irMix by animateFloatAsState(
        targetValue = if (room.hallCameraOn && room.hallIrMode) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "hwIrMix",
    )
    val camHudAlpha by animateFloatAsState(
        targetValue = if (room.hallCameraOn) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "hwCamHudAlpha",
    )

    // Goc quet FOV camera va nhap nhay REC (khong dung rememberInfiniteTransition de khong treo Robolectric)
    var camSweepPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(room.hallCameraOn) {
        if (!room.hallCameraOn) return@LaunchedEffect
        while (isActive) {
            delay(32L)
            camSweepPhase = (camSweepPhase + (0.032f / 5.2f)) % 1f
        }
    }
    val camSweepDeg = 0.5f + 11.5f * sin(camSweepPhase * 2f * PI.toFloat())
    val recDotAlpha = if (!room.hallCameraOn) 0f else 0.35f + 0.65f * ((sin(camSweepPhase * 9.45f * PI.toFloat()) + 1f) * 0.5f)

    // Vong song radar & vien nhap nhay do khi phat hien chuyen dong
    var radarPhase by remember { mutableFloatStateOf(0f) }
    var alertPulse by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(room.hallMotionDetected) {
        if (!room.hallMotionDetected) {
            radarPhase = 0f
            alertPulse = 0f
            return@LaunchedEffect
        }
        var tPulse = 0f
        while (isActive && room.hallMotionDetected) {
            delay(32L)
            radarPhase = (radarPhase + (0.032f / 1.55f)) % 1f
            tPulse = (tPulse + (0.032f / 1.1f)) % 1f
            alertPulse = ((sin(tPulse * 2f * PI.toFloat()) + 1f) * 0.5f)
        }
    }

    // Dong ho thoi gian thuc cho HUD camera
    val clockFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    var hudTime by remember { mutableStateOf(clockFmt.format(Date())) }
    LaunchedEffect(room.hallCameraOn) {
        while (isActive && room.hallCameraOn) {
            hudTime = clockFmt.format(Date())
            delay(1000L)
        }
    }

    val welcomePaint = remember {
        AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = AndroidPaint.Align.CENTER
            letterSpacing = 0.22f
        }
    }
    val auxPath = remember { Path() }
    val irPaint = remember { Paint() }
    val irColorMatrix = remember { ColorMatrix() }

    val cardShape = RoundedCornerShape(Dim.CardRadius)
    val stageShape = RoundedCornerShape(10.dp)
    val stageBorderColor = if (room.hallMotionDetected) {
        SH.Red.copy(alpha = 0.55f + 0.45f * alertPulse)
    } else {
        SH.Border
    }

    Column(
        modifier = modifier
            .clip(cardShape)
            .background(SH.Card)
            .border(Dim.BorderWidth, SH.Border, cardShape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Thanh chon 3 goc nhin: Dau he | Xung quanh | Tu xa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Mô phỏng 2.5D",
                style = SHType.Caption,
                fontWeight = FontWeight.SemiBold,
                color = SH.TextDim,
                maxLines = 1,
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0B1120))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HallwayView.entries.forEach { view ->
                    val active = room.hallView == view
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (active) SH.Blue else Color.Transparent)
                            .clickable { onSelectView(view) }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = view.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) Color.White else SH.TextDim,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // Khung mo phong chinh + Bo loc IR + Radar + CCTV HUD + Badge trang thai nhanh
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(stageShape)
                .background(Color(0xFF080D1A))
                .border(if (room.hallMotionDetected) 2.dp else 1.dp, stageBorderColor, stageShape),
        ) {
            // Lop hinh ve 3 goc nhin Crossfade + Bo loc hong ngoai IR
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        if (irMix > 0.01f) {
                            val m = irColorMatrix.values
                            for (i in 0 until 20) {
                                m[i] = IDENTITY_MATRIX_VALUES[i] + (IR_MATRIX_VALUES[i] - IDENTITY_MATRIX_VALUES[i]) * irMix
                            }
                            irPaint.colorFilter = ColorFilter.colorMatrix(irColorMatrix)
                            val canvas = drawContext.canvas
                            canvas.saveLayer(Rect(Offset.Zero, size), irPaint)
                            try {
                                drawContent()
                            } finally {
                                canvas.restore()
                            }
                        } else {
                            drawContent()
                        }
                    },
            ) {
                Crossfade(
                    targetState = room.hallView,
                    animationSpec = tween(durationMillis = 400),
                    label = "hallwayViewCrossfade",
                ) { activeView ->
                    Canvas(Modifier.fillMaxSize()) {
                        val sx = size.width / HW_W
                        val sy = size.height / HW_H

                        when (activeView) {
                            HallwayView.DAU_HE -> drawViewDauHe(
                                path = auxPath,
                                textPaint = welcomePaint,
                                sx = sx,
                                sy = sy,
                                fHall = fHall,
                                hallOn = room.lightOn,
                                fFront = fFront,
                                fSurr = fSurr,
                                isLocked = room.hallDoorLocked,
                                motionAuto = room.hallMotionAuto,
                                motionDetected = room.hallMotionDetected,
                            )
                            HallwayView.XUNG_QUANH -> drawViewXungQuanh(
                                path = auxPath,
                                sx = sx,
                                sy = sy,
                                fHall = fHall,
                                hallOn = room.lightOn,
                                fFront = fFront,
                                fSurr = fSurr,
                                surrOn = room.hallSurroundingLightOn,
                                isLocked = room.hallDoorLocked,
                                camOn = room.hallCameraOn,
                                camFovAlpha = camHudAlpha,
                                camSweepDeg = camSweepDeg,
                            )
                            HallwayView.TU_XA -> drawViewTuXa(
                                path = auxPath,
                                sx = sx,
                                sy = sy,
                                fHall = fHall,
                                fFront = fFront,
                                frontOn = room.hallFrontLightOn,
                                fSurr = fSurr,
                                surrOn = room.hallSurroundingLightOn,
                                isLocked = room.hallDoorLocked,
                                camOn = room.hallCameraOn,
                                camFovAlpha = camHudAlpha,
                                camSweepDeg = camSweepDeg,
                            )
                        }

                        // Lop phu toi moi truong theo trung binh do sang 3 cum den
                        val avgLight = (fHall + fFront + fSurr) / 3f
                        val darkness = ((1f - avgLight) * 0.68f).coerceIn(0f, 0.68f)
                        if (darkness > 0.01f) {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color(0xFF080C18).copy(alpha = 0.20f * darkness),
                                        0.68f to Color(0xFF050812).copy(alpha = 0.72f * darkness),
                                        1.0f to Color(0xFF02050E).copy(alpha = 0.90f * darkness),
                                    ),
                                    center = Offset(size.width * 0.50f, size.height * 0.44f),
                                    radius = maxOf(size.width, size.height) * 0.72f,
                                ),
                                size = size,
                            )
                        }
                    }
                }
            }

            // Soc quet IR Night Vision + Song Radar phat hien chuyen dong + Vien do
            if (irMix > 0.01f || room.hallMotionDetected) {
                Canvas(Modifier.fillMaxSize()) {
                    if (irMix > 0.01f) {
                        val stepPx = 3.dp.toPx()
                        val lineColor = Color(0xFF22C55E).copy(alpha = 0.07f * irMix)
                        var y = 0f
                        while (y < size.height) {
                            drawRect(color = lineColor, topLeft = Offset(0f, y), size = Size(size.width, 1.dp.toPx()))
                            y += stepPx
                        }
                    }
                    if (room.hallMotionDetected) {
                        val edgeAlpha = 0.22f + 0.30f * alertPulse
                        drawRect(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.55f to Color.Transparent,
                                    1.0f to Color(0xFFEF4444).copy(alpha = edgeAlpha),
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = maxOf(size.width, size.height) * 0.75f,
                            ),
                            size = size,
                        )

                        // 3 vong song Radar lech pha tai giua hanh lang
                        val center = Offset(size.width * 0.50f, size.height * 0.56f)
                        val baseRadius = 56.dp.toPx()
                        listOf(0f, 0.33f, 0.66f).forEach { offset ->
                            val p = (radarPhase + offset) % 1f
                            val scale = 0.18f + (1.75f - 0.18f) * p
                            val r = baseRadius * scale
                            val alpha = (1f - p).coerceIn(0f, 1f) * 0.9f
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFEF4444).copy(alpha = 0.24f * alpha),
                                        Color(0xFFEF4444).copy(alpha = 0f),
                                    ),
                                    center = center,
                                    radius = r,
                                ),
                                radius = r,
                                center = center,
                            )
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = alpha),
                                radius = r,
                                center = center,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }
                }
            }

            // Lop HUD Camera + Banner Canh bao + Dai badge trang thai 3 den & khoa
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            ) {
                // Top: CCTV HUD + Banner chuyen dong
                Column(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (camHudAlpha > 0.01f) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(camHudAlpha),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(Color.Black.copy(alpha = 0.58f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SH.Red.copy(alpha = recDotAlpha)),
                                )
                                Text(
                                    text = "REC | ${room.hallView.camTitle}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (room.hallCameraOn && room.hallIrMode) {
                                    Text(
                                        text = "IR NIGHT",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF86EFAC),
                                        maxLines = 1,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(SH.Green.tint(0.22f))
                                            .border(1.dp, SH.Green.tint(0.55f), RoundedCornerShape(5.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Text(
                                    text = hudTime,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFE2E8F0),
                                    maxLines = 1,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.Black.copy(alpha = 0.58f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(7.dp))
                                        .padding(horizontal = 7.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = room.hallMotionDetected,
                        enter = fadeIn(tween(220)),
                        exit = fadeOut(tween(220)),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xEEB91C1C))
                                .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.65f), RoundedCornerShape(9.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Phát hiện chuyển động tại ${room.hallView.label}!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (room.hallMotionAuto) {
                                        "Đã tự động bật Đèn hành lang & Đèn xung quanh."
                                    } else {
                                        "Cảm biến PIR kích hoạt • Tự bật đèn đang tắt."
                                    },
                                    fontSize = 9.5.sp,
                                    color = Color(0xFFFEE2E2),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .clickable(onClick = onDismissMotion)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "Đã xem",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Bottom: 3 badge trang thai nhanh den + LOCK mini
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        StageLightBadge("Hành lang", room.lightOn, room.lightBrightness)
                        StageLightBadge("Trước nhà", room.hallFrontLightOn, room.hallFrontBrightness)
                        StageLightBadge("Xung quanh", room.hallSurroundingLightOn, room.hallSurroundingBrightness)
                    }
                    Text(
                        text = if (room.hallDoorLocked) "LOCK: ĐÃ KHÓA" else "LOCK: ĐANG MỞ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (room.hallDoorLocked) Color(0xFF34D399) else Color(0xFFFCA5A5),
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (room.hallDoorLocked) Color.Black.copy(alpha = 0.62f) else Color(0xCC450A0A))
                            .border(
                                1.dp,
                                if (room.hallDoorLocked) Color.White.copy(alpha = 0.12f) else SH.Red.tint(0.55f),
                                RoundedCornerShape(5.dp),
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StageLightBadge(name: String, isOn: Boolean, brightness: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .border(
                1.dp,
                if (isOn) SH.Amber.tint(0.50f) else Color(0xFF334155),
                RoundedCornerShape(5.dp),
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(if (isOn) SH.Amber else Color(0xFF475569)),
        )
        Text(
            text = if (isOn) "$name: $brightness%" else "$name: Tắt",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isOn) Color(0xFFFCD34D) else SH.TextDim,
            maxLines = 1,
        )
    }
}

// =========================================================================
// GOC NHIN 1: DAU HE (dau_he)
// =========================================================================
private fun DrawScope.drawViewDauHe(
    path: Path,
    textPaint: AndroidPaint,
    sx: Float,
    sy: Float,
    fHall: Float,
    hallOn: Boolean,
    fFront: Float,
    fSurr: Float,
    isLocked: Boolean,
    motionAuto: Boolean,
    motionDetected: Boolean,
) {
    val u = minOf(sx, sy)

    // Bau troi dem
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(0xFF060C1C),
                0.60f to Color(0xFF112040),
                1f to Color(0xFF1D355E),
            ),
            startY = 0f,
            endY = 500f * sy,
        ),
        size = size,
    )
    // Trang khuyet goc phai
    drawCircle(Color(0xFFFEF9C3).copy(alpha = 0.86f), radius = 13f * u, center = Offset(728f * sx, 70f * sy))
    drawCircle(Color(0xFF0D1730), radius = 12f * u, center = Offset(734f * sx, 66f * sy))

    // Bui cay san vuon 2 ben hong
    path.reset()
    path.moveTo(0f, 330f * sy)
    path.quadraticTo(26f * sx, 285f * sy, 68f * sx, 312f * sy)
    path.lineTo(68f * sx, 455f * sy)
    path.lineTo(0f, 455f * sy)
    path.close()
    drawPath(path, Color(0xFF133829))

    path.reset()
    path.moveTo(800f * sx, 330f * sy)
    path.quadraticTo(774f * sx, 285f * sy, 732f * sx, 312f * sy)
    path.lineTo(732f * sx, 455f * sy)
    path.lineTo(800f * sx, 455f * sy)
    path.close()
    drawPath(path, Color(0xFF133829))

    // [DEN XUNG QUANH] Anh sang san vuon 2 ben hong & den nam thap
    if (fSurr > 0.01f) {
        drawWarmGlowOval(Offset(22f * sx, 340f * sy), 78f * sx, 125f * sy, Color(0xFFFCD34D), 0.62f * fSurr)
        drawWarmGlowOval(Offset(778f * sx, 340f * sy), 78f * sx, 125f * sy, Color(0xFFFCD34D), 0.62f * fSurr)
        drawWarmGlowOval(Offset(18f * sx, 395f * sy), 14f * sx, 14f * sy, Color(0xFFFEF08A), 0.85f * fSurr)
        drawWarmGlowOval(Offset(782f * sx, 395f * sy), 14f * sx, 14f * sy, Color(0xFFFEF08A), 0.85f * fSurr)
    }

    // Diem mai ngoi Nhat dua rong
    drawPolygon(path, sx, sy, floatArrayOf(0f, 0f, 800f, 0f, 762f, 28f, 38f, 28f), fill = Color(0xFF1E293B), stroke = Color(0xFF0F172A), strokePx = 1.5f * u)

    // Tran sanh hien op go nhua nan
    drawPolygonBrush(
        path, sx, sy,
        floatArrayOf(28f, 28f, 772f, 28f, 655f, 112f, 145f, 112f),
        Brush.verticalGradient(listOf(Color(0xFF523724), Color(0xFF2C1C12)), startY = 28f * sy, endY = 112f * sy),
    )
    listOf(
        120f to 215f, 230f to 300f, 340f to 380f,
        460f to 420f, 570f to 500f, 680f to 585f,
    ).forEach { (x1, x2) ->
        drawLine(Color(0xFF1B110B).copy(alpha = 0.55f), Offset(x1 * sx, 28f * sy), Offset(x2 * sx, 112f * sy), strokeWidth = 1.2f * u)
    }

    // [DEN PHIA TRUOC NHA] Dai sang hat tu diem mai Nhat
    if (fFront > 0.01f) {
        drawRect(Color(0xFFFEF08A).copy(alpha = 0.85f * fFront), Offset(44f * sx, 25f * sy), Size(712f * sx, 4f * sy))
        drawPolygonBrush(
            path, sx, sy,
            floatArrayOf(40f, 29f, 760f, 29f, 800f, 140f, 0f, 140f),
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFDE68A).copy(alpha = 0.82f * 0.72f * fFront),
                    0.55f to Color(0xFFFBBF24).copy(alpha = 0.22f * 0.72f * fFront),
                    1f to Color.Transparent,
                ),
                startY = 29f * sy,
                endY = 140f * sy,
            ),
        )
        drawCircle(Color.White.copy(alpha = fFront), 4.5f * u, Offset(145f * sx, 27f * sy))
        drawCircle(Color.White.copy(alpha = fFront), 5f * u, Offset(400f * sx, 27f * sy))
        drawCircle(Color.White.copy(alpha = fFront), 4.5f * u, Offset(655f * sx, 27f * sy))
    }

    // Tuong mat tien sanh trang su + lam go
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFECE8E1), Color(0xFFC9C2B6)), startY = 112f * sy, endY = 364f * sy),
        topLeft = Offset(145f * sx, 112f * sy),
        size = Size(510f * sx, 252f * sy),
    )
    drawRect(Color(0xFF4E3220).copy(alpha = 0.22f), Offset(145f * sx, 112f * sy), Size(115f * sx, 252f * sy))
    drawRect(Color(0xFF4E3220).copy(alpha = 0.22f), Offset(540f * sx, 112f * sy), Size(115f * sx, 252f * sy))
    listOf(168f, 192f, 216f, 584f, 608f, 632f).forEach { x ->
        drawLine(Color(0xFF3E2313).copy(alpha = 0.30f), Offset(x * sx, 112f * sy), Offset(x * sx, 364f * sy), strokeWidth = 1.4f * u)
    }

    // San sanh & Bac tam cap da Granite
    drawPolygonBrush(
        path, sx, sy,
        floatArrayOf(50f, 432f, 145f, 364f, 655f, 364f, 750f, 432f),
        Brush.verticalGradient(
            colorStops = arrayOf(0f to Color(0xFF5D6470), 0.55f to Color(0xFF404650), 1f to Color(0xFF262A33)),
            startY = 364f * sy,
            endY = 432f * sy,
        ),
    )
    drawPolygon(path, sx, sy, floatArrayOf(34f, 432f, 766f, 432f, 776f, 454f, 24f, 454f), Color(0xFF475569), Color(0xFF1E293B), 1.2f * u)
    drawRect(Color(0xFF1E293B), Offset(24f * sx, 454f * sy), Size(752f * sx, 6f * sy))
    drawPolygon(path, sx, sy, floatArrayOf(18f, 460f, 782f, 460f, 792f, 480f, 8f, 480f), Color(0xFF3B4352), Color(0xFF1E293B), 1.2f * u)
    drawRect(Color(0xFF18202C), Offset(8f * sx, 480f * sy), Size(784f * sx, 6f * sy))
    drawRect(Color(0xFF2D3440), Offset(0f, 486f * sy), Size(800f * sx, 14f * sy))

    // Tham chui chan WELCOME
    drawPolygon(path, sx, sy, floatArrayOf(318f, 378f, 482f, 378f, 500f, 418f, 300f, 418f), Color(0xFF3A2F28), Color(0xFF9A7B5C), 2f * u)
    textPaint.color = Color(0xFFE2E8F0).copy(alpha = 0.82f).toArgb()
    textPaint.textSize = 13f * u
    drawContext.canvas.nativeCanvas.drawText("WELCOME", 400f * sx, 403f * sy, textPaint)

    // Cua chinh go 2 canh & Khoa cua thong minh
    drawRoundRect(Color(0xFF26140A), Offset(296f * sx, 144f * sy), Size(208f * sx, 220f * sy), CornerRadius(3f * sx, 3f * sy))
    val woodDoorBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF764729), Color(0xFF59341D), Color(0xFF3C2111)),
        start = Offset(305f * sx, 152f * sy),
        end = Offset(495f * sx, 200f * sy),
    )
    drawRect(woodDoorBrush, Offset(305f * sx, 152f * sy), Size(92f * sx, 212f * sy))
    drawRect(woodDoorBrush, Offset(403f * sx, 152f * sy), Size(92f * sx, 212f * sy))
    // O kinh cua go
    drawRoundRect(Color(0xFF94A3B8).copy(alpha = 0.28f), Offset(322f * sx, 172f * sy), Size(20f * sx, 162f * sy), CornerRadius(2f * sx, 2f * sy))
    drawRoundRect(Color(0xFF26140A), Offset(322f * sx, 172f * sy), Size(20f * sx, 162f * sy), CornerRadius(2f * sx, 2f * sy), style = Stroke(1.5f * u))
    drawRoundRect(Color(0xFF94A3B8).copy(alpha = 0.28f), Offset(458f * sx, 172f * sy), Size(20f * sx, 162f * sy), CornerRadius(2f * sx, 2f * sy))
    drawRoundRect(Color(0xFF26140A), Offset(458f * sx, 172f * sy), Size(20f * sx, 162f * sy), CornerRadius(2f * sx, 2f * sy), style = Stroke(1.5f * u))
    // Tay nam
    drawRoundRect(Color(0xFFE2E8F0), Offset(384f * sx, 238f * sy), Size(5.5f * sx, 56f * sy), CornerRadius(2.5f * sx, 2.5f * sy))
    drawRoundRect(Color(0xFFE2E8F0), Offset(410.5f * sx, 238f * sy), Size(5.5f * sx, 56f * sy), CornerRadius(2.5f * sx, 2.5f * sy))
    // Khoa cua thong minh
    val lockColor = if (isLocked) Color(0xFF22C55E) else Color(0xFFEF4444)
    drawRoundRect(Color(0xFF0F172A), Offset(421f * sx, 247f * sy), Size(22f * sx, 44f * sy), CornerRadius(4f * sx, 4f * sy))
    drawRoundRect(Color(0xFF475569), Offset(421f * sx, 247f * sy), Size(22f * sx, 44f * sy), CornerRadius(4f * sx, 4f * sy), style = Stroke(1.2f * u))
    drawWarmGlowOval(Offset(432f * sx, 259f * sy), 14f * u, 14f * u, lockColor, 0.55f)
    drawCircle(lockColor, 4f * u, Offset(432f * sx, 259f * sy))
    drawRoundRect(Color(0xFF1E293B), Offset(426f * sx, 269f * sy), Size(12f * sx, 16f * sy), CornerRadius(1.5f * sx, 1.5f * sy))

    // 2 Cot tru vuong op da chan cot & 2 den tuong [DEN HANH LANG]
    val pillarBrushLeft = Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFF1F5F9), Color(0xFF94A3B8)), startX = 46f * sx, endX = 112f * sx)
    val stoneLeft = Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF1E293B)), startX = 40f * sx, endX = 118f * sx)
    drawRect(pillarBrushLeft, Offset(46f * sx, 28f * sy), Size(66f * sx, 324f * sy))
    drawRect(Color(0xFF64748B), Offset(46f * sx, 28f * sy), Size(66f * sx, 324f * sy), style = Stroke(1.2f * u))
    drawRect(stoneLeft, Offset(40f * sx, 352f * sy), Size(78f * sx, 84f * sy))
    drawRect(Color(0xFF1E293B), Offset(40f * sx, 352f * sy), Size(78f * sx, 84f * sy), style = Stroke(1.5f * u))
    drawRect(Color(0xFF334155), Offset(40f * sx, 28f * sy), Size(78f * sx, 14f * sy))

    val pillarBrushRight = Brush.horizontalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFF1F5F9), Color(0xFF94A3B8)), startX = 688f * sx, endX = 754f * sx)
    val stoneRight = Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF475569), Color(0xFF1E293B)), startX = 682f * sx, endX = 760f * sx)
    drawRect(pillarBrushRight, Offset(688f * sx, 28f * sy), Size(66f * sx, 324f * sy))
    drawRect(Color(0xFF64748B), Offset(688f * sx, 28f * sy), Size(66f * sx, 324f * sy), style = Stroke(1.2f * u))
    drawRect(stoneRight, Offset(682f * sx, 352f * sy), Size(78f * sx, 84f * sy))
    drawRect(Color(0xFF1E293B), Offset(682f * sx, 352f * sy), Size(78f * sx, 84f * sy), style = Stroke(1.5f * u))
    drawRect(Color(0xFF334155), Offset(682f * sx, 28f * sy), Size(78f * sx, 14f * sy))

    val sconceBulbColor = if (hallOn) Color(0xFFFEF08A) else Color(0xFF475569)
    listOf(79f, 721f).forEach { cx ->
        if (fHall > 0.01f) {
            drawWarmGlowOval(Offset(cx * sx, 204f * sy), 52f * sx, 72f * sy, Color(0xFFF59E0B), 0.72f * fHall)
        }
        drawRoundRect(Color(0xFF1E293B), Offset((cx - 7f) * sx, 188f * sy), Size(14f * sx, 32f * sy), CornerRadius(2.5f * sx, 2.5f * sy))
        drawRoundRect(Color(0xFF475569), Offset((cx - 7f) * sx, 188f * sy), Size(14f * sx, 32f * sy), CornerRadius(2.5f * sx, 2.5f * sy), style = Stroke(1f * u))
        drawRoundRect(sconceBulbColor, Offset((cx - 4f) * sx, 191f * sy), Size(8f * sx, 26f * sy), CornerRadius(1.5f * sx, 1.5f * sy))
    }

    // [DEN HANH LANG] Den am tran go nhua sanh hien + Vung sang am phu cua & bac tam cap
    if (fHall > 0.01f) {
        drawPolygonBrush(
            path, sx, sy,
            floatArrayOf(220f, 66f, 580f, 66f, 710f, 455f, 90f, 455f),
            Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFEF08A).copy(alpha = 0.92f * 0.72f * fHall),
                    0.42f to Color(0xFFF59E0B).copy(alpha = 0.34f * 0.72f * fHall),
                    1f to Color.Transparent,
                ),
                center = Offset(400f * sx, 120f * sy),
                radius = 360f * maxOf(sx, sy),
            ),
        )
        listOf(275f to 16f, 400f to 18f, 525f to 16f).forEach { (cx, rx) ->
            drawWarmGlowOval(Offset(cx * sx, 66f * sy), rx * 1.4f * sx, 8f * sy, Color(0xFFFEF08A), 0.85f * fHall)
            drawOval(Color.White.copy(alpha = fHall), Offset((cx - rx * 0.5f) * sx, 63.4f * sy), Size(rx * sx, 5.2f * sy))
        }
    }

    // [DEN PHIA TRUOC NHA] Anh sang san truoc hat len bac tam cap
    if (fFront > 0.01f) {
        drawWarmGlowOval(Offset(400f * sx, 495f * sy), 340f * sx, 95f * sy, Color(0xFFFDE68A), 0.58f * fFront)
    }

    // Cam bien chuyen dong PIR Dome gan tran sanh
    drawOval(Color(0xFFE2E8F0), Offset(378f * sx, 88f * sy), Size(44f * sx, 14f * sy))
    drawOval(Color(0xFF94A3B8), Offset(378f * sx, 88f * sy), Size(44f * sx, 14f * sy), style = Stroke(1.2f * u))
    path.reset()
    path.moveTo(386f * sx, 95f * sy)
    path.quadraticTo(400f * sx, 110f * sy, 414f * sx, 95f * sy)
    path.close()
    drawPath(path, Color(0xFFCBD5E1))
    val pirLedColor = when {
        motionDetected -> Color(0xFFEF4444)
        motionAuto -> Color(0xFF22C55E)
        else -> Color(0xFF64748B)
    }
    drawCircle(pirLedColor, 3.2f * u, Offset(400f * sx, 101f * sy))
}

// =========================================================================
// GOC NHIN 2: XUNG QUANH (xung_quanh)
// =========================================================================
private fun DrawScope.drawViewXungQuanh(
    path: Path,
    sx: Float,
    sy: Float,
    fHall: Float,
    hallOn: Boolean,
    fFront: Float,
    fSurr: Float,
    surrOn: Boolean,
    isLocked: Boolean,
    camOn: Boolean,
    camFovAlpha: Float,
    camSweepDeg: Float,
) {
    val u = minOf(sx, sy)

    // Bau troi dem + sao
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF060C1C), Color(0xFF15264B)), startY = 0f, endY = 500f * sy),
        size = size,
    )
    drawCircle(Color.White.copy(alpha = 0.8f), 1.4f * u, Offset(670f * sx, 60f * sy))
    drawCircle(Color.White.copy(alpha = 0.7f), 1.2f * u, Offset(718f * sx, 92f * sy))
    drawCircle(Color.White.copy(alpha = 0.65f), 1.1f * u, Offset(600f * sx, 80f * sy))

    // Mai ngoi Nhat xanh than dua rong doc hong nha + diem duoi mai
    drawPolygonBrush(
        path, sx, sy,
        floatArrayOf(0f, 0f, 505f, 0f, 568f, 152f, 468f, 152f, 0f, 50f),
        Brush.verticalGradient(
            colorStops = arrayOf(0f to Color(0xFF334155), 0.55f to Color(0xFF1E293B), 1f to Color(0xFF0F172A)),
            startY = 0f,
            endY = 152f * sy,
        ),
    )
    listOf(
        Offset(65f, 0f) to Offset(150f, 78f),
        Offset(155f, 0f) to Offset(255f, 98f),
        Offset(250f, 0f) to Offset(355f, 120f),
        Offset(345f, 0f) to Offset(452f, 140f),
        Offset(435f, 0f) to Offset(532f, 150f),
    ).forEach { (a, b) ->
        drawLine(Color(0xFF475569).copy(alpha = 0.35f), Offset(a.x * sx, a.y * sy), Offset(b.x * sx, b.y * sy), strokeWidth = 1f * u)
    }
    drawPolygon(path, sx, sy, floatArrayOf(0f, 50f, 468f, 152f, 552f, 152f, 0f, 70f), fill = Color(0xFF3B2618))

    // [DEN XUNG QUANH] Den hat duoi diem mai hong nha
    if (fSurr > 0.01f) {
        listOf(
            Triple(140f, 94f, 22f to 6f),
            Triple(285f, 124f, 18f to 5f),
            Triple(425f, 148f, 14f to 4f),
        ).forEach { (cx, cy, r) ->
            drawWarmGlowOval(Offset(cx * sx, cy * sy), r.first * 1.4f * sx, r.second * 1.8f * sy, Color(0xFFFEF08A), 0.9f * fSurr)
            drawOval(Color.White.copy(alpha = fSurr), Offset((cx - 5f) * sx, (cy - 1.8f) * sy), Size(10f * sx, 3.6f * sy))
        }
    }

    // Tuong trang ket hop lam go hong nha
    drawPolygonBrush(
        path, sx, sy,
        floatArrayOf(0f, 70f, 468f, 152f, 468f, 325f, 0f, 468f),
        Brush.linearGradient(listOf(Color(0xFFEAE6DF), Color(0xFFA8A095)), start = Offset(0f, 70f * sy), end = Offset(468f * sx, 160f * sy)),
    )
    drawPolygon(path, sx, sy, floatArrayOf(186f, 102f, 244f, 113f, 244f, 386f, 186f, 405f), fill = Color(0xFF4A2E1C).copy(alpha = 0.88f))
    listOf(200f to 400f, 215f to 395f, 230f to 390f).forEach { (x, yBottom) ->
        drawLine(Color(0xFF29170D).copy(alpha = 0.6f), Offset(x * sx, (100f + (x - 186f) * 0.25f) * sy), Offset(x * sx, yBottom * sy), strokeWidth = 1.2f * u)
    }

    // Cua so nhom Xingfa den
    drawPolygon(path, sx, sy, floatArrayOf(48f, 140f, 166f, 162f, 166f, 306f, 48f, 324f), Color(0xFF0F172A), Color(0xFF1E293B), 3.5f * u)
    drawPolygon(path, sx, sy, floatArrayOf(56f, 148f, 158f, 167f, 158f, 300f, 56f, 316f), Color(0xFF334155).copy(alpha = 0.60f))
    drawLine(Color(0xFF0F172A), Offset(108f * sx, 156f * sy), Offset(108f * sx, 308f * sy), strokeWidth = 2.8f * u)

    drawPolygon(path, sx, sy, floatArrayOf(262f, 174f, 352f, 191f, 352f, 296f, 262f, 308f), Color(0xFF0F172A), Color(0xFF1E293B), 3f * u)
    drawPolygon(path, sx, sy, floatArrayOf(268f, 180f, 346f, 195f, 346f, 291f, 268f, 302f), Color(0xFF334155).copy(alpha = 0.55f))

    // Cua phu cuoi hanh lang + LED khoa
    val lockColor = if (isLocked) Color(0xFF22C55E) else Color(0xFFEF4444)
    drawPolygon(path, sx, sy, floatArrayOf(392f, 198f, 446f, 208f, 446f, 318f, 392f, 324f), Color(0xFF4E2F1B), Color(0xFF24150B), 2f * u)
    drawCircle(lockColor, 3.5f * u, Offset(436f * sx, 270f * sy))

    // Hang rao xung quanh + [DEN XUNG QUANH] 2 den tru rao
    drawPolygon(path, sx, sy, floatArrayOf(468f, 236f, 800f, 190f, 800f, 388f, 468f, 325f), Color(0xFF273142), Color(0xFF18202C), 1.5f * u)
    val surrBulbColor = if (surrOn) Color(0xFFFEF08A) else Color(0xFF475569)
    if (fSurr > 0.01f) {
        drawWarmGlowOval(Offset(615f * sx, 216f * sy), 34f * sx, 34f * sy, Color(0xFFF59E0B), 0.55f * fSurr)
        drawWarmGlowOval(Offset(745f * sx, 198f * sy), 40f * sx, 40f * sy, Color(0xFFF59E0B), 0.60f * fSurr)
    }
    drawRoundRect(surrBulbColor, Offset(610f * sx, 210f * sy), Size(10f * sx, 10f * sy), CornerRadius(2f * sx, 2f * sy))
    drawRoundRect(surrBulbColor, Offset(739f * sx, 191f * sy), Size(12f * sx, 12f * sy), CornerRadius(2f * sx, 2f * sy))

    // Tieu canh cay xanh ben phai
    path.reset()
    path.moveTo(510f * sx, 338f * sy)
    path.quadraticTo(560f * sx, 285f * sy, 620f * sx, 308f * sy)
    path.quadraticTo(675f * sx, 260f * sy, 740f * sx, 294f * sy)
    path.quadraticTo(778f * sx, 278f * sy, 800f * sx, 310f * sy)
    path.lineTo(800f * sx, 460f * sy)
    path.lineTo(510f * sx, 355f * sy)
    path.close()
    drawPath(path, Color(0xFF14362A))

    path.reset()
    path.moveTo(545f * sx, 360f * sy)
    path.quadraticTo(605f * sx, 316f * sy, 668f * sx, 338f * sy)
    path.quadraticTo(725f * sx, 305f * sy, 800f * sx, 345f * sy)
    path.lineTo(800f * sx, 500f * sy)
    path.lineTo(545f * sx, 388f * sy)
    path.close()
    drawPath(path, Color(0xFF1B4D3E))

    // Loi di hanh lang lat da
    drawPolygonBrush(
        path, sx, sy,
        floatArrayOf(0f, 468f, 468f, 325f, 555f, 345f, 585f, 500f, 0f, 500f),
        Brush.verticalGradient(listOf(Color(0xFF373C47), Color(0xFF585F6E)), startY = 325f * sy, endY = 500f * sy),
    )
    listOf(
        Offset(88f, 440f) to Offset(558f, 462f),
        Offset(210f, 402f) to Offset(554f, 416f),
        Offset(315f, 370f) to Offset(545f, 380f),
        Offset(398f, 344f) to Offset(538f, 352f),
    ).forEach { (a, b) ->
        drawLine(Color(0xFF242933).copy(alpha = 0.55f), Offset(a.x * sx, a.y * sy), Offset(b.x * sx, b.y * sy), strokeWidth = 1.4f * u)
    }

    // [DEN PHIA TRUOC NHA] Quang sang am hat tu mat tien o goc truoc trai
    if (fFront > 0.01f) {
        drawWarmGlowOval(Offset(15f * sx, 360f * sy), 185f * sx, 180f * sy, Color(0xFFFDE68A), 0.70f * fFront)
    }

    // [DEN HANH LANG] Day den tuong doc hanh lang hong nha
    val hallBulbColor = if (hallOn) Color(0xFFFEF08A) else Color(0xFF475569)
    val sconces = listOf(
        Triple(floatArrayOf(214f, 190f, 142f, 422f, 288f, 392f), BoxF(206f, 172f, 16f, 30f), BoxF(209f, 176f, 10f, 22f)),
        Triple(floatArrayOf(366f, 210f, 316f, 374f, 416f, 356f), BoxF(360f, 196f, 12f, 24f), BoxF(362f, 199f, 8f, 18f)),
        Triple(floatArrayOf(454f, 224f, 422f, 338f, 485f, 330f), BoxF(450f, 214f, 9f, 18f), BoxF(452f, 216f, 5f, 14f)),
    )
    sconces.forEach { (conePts, housing, bulb) ->
        if (fHall > 0.01f) {
            drawPolygonBrush(
                path, sx, sy,
                conePts,
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFFFEF08A).copy(alpha = 0.90f * fHall),
                        0.55f to Color(0xFFF59E0B).copy(alpha = 0.28f * fHall),
                        1f to Color.Transparent,
                    ),
                    startY = conePts[1] * sy,
                    endY = maxOf(conePts[3], conePts[5]) * sy,
                ),
            )
        }
        drawRoundRect(Color(0xFF1E293B), Offset(housing.x * sx, housing.y * sy), Size(housing.w * sx, housing.h * sy), CornerRadius(2.5f * sx, 2.5f * sy))
        drawRoundRect(hallBulbColor, Offset(bulb.x * sx, bulb.y * sy), Size(bulb.w * sx, bulb.h * sy), CornerRadius(1.5f * sx, 1.5f * sy))
    }

    // [DEN XUNG QUANH] Day den nam san vuon dan loi doc mep vuon
    val bollards = listOf(
        Triple(536f, 430f, Pair(48f to 20f, BoxF(531f, 404f, 10f, 30f) to BoxF(532f, 407f, 8f, 6f))),
        Triple(526f, 378f, Pair(36f to 15f, BoxF(522f, 358f, 8f, 24f) to BoxF(523f, 361f, 6f, 5f))),
        Triple(515f, 342f, Pair(26f to 11f, BoxF(512f, 328f, 6f, 18f) to BoxF(513f, 330f, 4f, 4f))),
    )
    bollards.forEach { (cx, cy, spec) ->
        val (glowR, boxes) = spec
        val (post, cap) = boxes
        if (fSurr > 0.01f) {
            drawWarmGlowOval(Offset(cx * sx, cy * sy), glowR.first * sx, glowR.second * sy, Color(0xFFF59E0B), 0.50f * fSurr)
        }
        drawRoundRect(Color(0xFF1E293B), Offset(post.x * sx, post.y * sy), Size(post.w * sx, post.h * sy), CornerRadius(2f * sx, 2f * sy))
        drawRoundRect(surrBulbColor, Offset(cap.x * sx, cap.y * sy), Size(cap.w * sx, cap.h * sy), CornerRadius(1f * sx, 1f * sy))
    }

    // CAMERA CCTV GOC MAI NHAT + CHUM QUET FOV DONG
    if (camFovAlpha > 0.01f) {
        rotate(degrees = camSweepDeg, pivot = Offset(508f * sx, 156f * sy)) {
            drawPolygonBrush(
                path, sx, sy,
                floatArrayOf(508f, 156f, 675f, 382f, 385f, 432f),
                Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.34f * camFovAlpha), Color.Transparent),
                    start = Offset(508f * sx, 156f * sy),
                    end = Offset(675f * sx, 432f * sy),
                ),
            )
        }
    }
    drawLine(Color(0xFF94A3B8), Offset(494f * sx, 148f * sy), Offset(508f * sx, 156f * sy), strokeWidth = 3f * u)
    rotate(degrees = 24f, pivot = Offset(510f * sx, 155f * sy)) {
        drawRoundRect(Color(0xFFE2E8F0), Offset(500f * sx, 149f * sy), Size(22f * sx, 12f * sy), CornerRadius(3f * sx, 3f * sy))
        drawRoundRect(Color(0xFF475569), Offset(500f * sx, 149f * sy), Size(22f * sx, 12f * sy), CornerRadius(3f * sx, 3f * sy), style = Stroke(1f * u))
    }
    drawCircle(Color(0xFF0F172A), 3f * u, Offset(519f * sx, 160f * sy))
    drawCircle(if (camOn) Color(0xFFEF4444) else Color(0xFF475569), 1.6f * u, Offset(519f * sx, 160f * sy))
}

// =========================================================================
// GOC NHIN 3: TU XA (tu_xa) — Toan canh NHA MAI NHAT 2 TANG
// =========================================================================
private fun DrawScope.drawViewTuXa(
    path: Path,
    sx: Float,
    sy: Float,
    fHall: Float,
    fFront: Float,
    frontOn: Boolean,
    fSurr: Float,
    surrOn: Boolean,
    isLocked: Boolean,
    camOn: Boolean,
    camFovAlpha: Float,
    camSweepDeg: Float,
) {
    val u = minOf(sx, sy)

    // Bau troi dem + trang sao
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to Color(0xFF050914), 0.55f to Color(0xFF0F1D3A), 1f to Color(0xFF1B3158)),
            startY = 0f,
            endY = 335f * sy,
        ),
        size = Size(800f * sx, 335f * sy),
    )
    drawCircle(Color(0xFFFEF9C3).copy(alpha = 0.90f), 17f * u, Offset(114f * sx, 64f * sy))
    drawCircle(Color(0xFF091227), 16f * u, Offset(122f * sx, 59f * sy))
    listOf(
        Triple(220f, 44f, 1.4f), Triple(312f, 38f, 1.1f), Triple(515f, 40f, 1.3f),
        Triple(628f, 64f, 1.5f), Triple(722f, 50f, 1.2f), Triple(680f, 102f, 1.1f),
    ).forEach { (x, y, r) ->
        drawCircle(Color.White.copy(alpha = 0.75f), r * u, Offset(x * sx, y * sy))
    }

    // Hang cay xa & tieu canh 2 ben hong nha
    path.reset()
    path.moveTo(0f, 318f * sy)
    path.quadraticTo(110f * sx, 264f * sy, 220f * sx, 294f * sy)
    path.quadraticTo(380f * sx, 290f * sy, 540f * sx, 285f * sy)
    path.quadraticTo(670f * sx, 290f * sy, 800f * sx, 305f * sy)
    path.lineTo(800f * sx, 348f * sy)
    path.lineTo(0f, 348f * sy)
    path.close()
    drawPath(path, Color(0xFF0D1B2A))

    path.reset()
    path.moveTo(585f * sx, 332f * sy)
    path.quadraticTo(645f * sx, 260f * sy, 715f * sx, 286f * sy)
    path.quadraticTo(765f * sx, 264f * sy, 800f * sx, 296f * sy)
    path.lineTo(800f * sx, 348f * sy)
    path.lineTo(585f * sx, 348f * sy)
    path.close()
    drawPath(path, Color(0xFF123026))

    path.reset()
    path.moveTo(0f, 332f * sy)
    path.quadraticTo(55f * sx, 276f * sy, 125f * sx, 295f * sy)
    path.quadraticTo(172f * sx, 275f * sy, 215f * sx, 308f * sy)
    path.lineTo(215f * sx, 348f * sy)
    path.lineTo(0f, 348f * sy)
    path.close()
    drawPath(path, Color(0xFF123026))

    // San truoc lat gach + tham co 2 ben + loi di chinh
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF1F2937), Color(0xFF111827)), startY = 335f * sy, endY = 500f * sy),
        topLeft = Offset(0f, 335f * sy),
        size = Size(800f * sx, 165f * sy),
    )
    drawPolygon(path, sx, sy, floatArrayOf(0f, 338f, 298f, 338f, 220f, 500f, 0f, 500f), fill = Color(0xFF123026))
    drawPolygon(path, sx, sy, floatArrayOf(502f, 338f, 800f, 338f, 800f, 500f, 580f, 500f), fill = Color(0xFF123026))
    drawPolygon(path, sx, sy, floatArrayOf(304f, 338f, 496f, 338f, 565f, 500f, 235f, 500f), fill = Color(0xFF374151))
    listOf(290f to 376f to 510f, 270f to 420f to 530f, 250f to 466f to 550f).forEach { (xy1, x2) ->
        drawLine(Color(0xFF1F2937).copy(alpha = 0.65f), Offset(xy1.first * sx, xy1.second * sy), Offset(x2 * sx, xy1.second * sy), strokeWidth = 1.4f * u)
    }

    // [DEN XUNG QUANH] Den san vuon 2 ben hong
    val surrBulbColor = if (surrOn) Color(0xFFFEF08A) else Color(0xFF475569)
    listOf(
        Triple(122f, 348f, 68f to 26f),
        Triple(176f, 364f, 46f to 18f),
        Triple(678f, 348f, 68f to 26f),
        Triple(624f, 364f, 46f to 18f),
    ).forEach { (cx, cy, gr) ->
        if (fSurr > 0.01f) {
            drawWarmGlowOval(Offset(cx * sx, cy * sy), gr.first * sx, gr.second * sy, Color(0xFFF59E0B), 0.55f * fSurr)
        }
        drawRoundRect(Color(0xFF1E293B), Offset((cx - 4f) * sx, (cy - 16f) * sy), Size(6f * sx, 16f * sy), CornerRadius(1.5f * sx, 1.5f * sy))
        drawRect(surrBulbColor, Offset((cx - 3f) * sx, (cy - 14f) * sy), Size(4f * sx, 4f * sy))
    }

    // -------- TANG 2 BIET THU MAI NHAT --------
    val wallBrush = Brush.verticalGradient(listOf(Color(0xFFF1EDE6), Color(0xFFC4BDB2)), startY = 132f * sy, endY = 342f * sy)
    drawRect(wallBrush, Offset(206f * sx, 132f * sy), Size(388f * sx, 100f * sy))
    drawRect(Color(0xFF5B3A24), Offset(218f * sx, 138f * sy), Size(128f * sx, 90f * sy))
    listOf(234f, 250f, 266f, 282f, 298f, 314f, 330f).forEach { x ->
        drawLine(Color(0xFF2D1A0E).copy(alpha = 0.55f), Offset(x * sx, 138f * sy), Offset(x * sx, 228f * sy), strokeWidth = 1.2f * u)
    }

    // [DEN PHIA TRUOC NHA] Anh sang ban cong tang 2
    if (fFront > 0.01f) {
        drawWarmGlowOval(Offset(475f * sx, 184f * sy), 115f * sx, 48f * sy, Color(0xFFFEF08A), 0.80f * fFront)
    }

    // Cua kinh nhom Xingfa tang 2
    val winWarm = Brush.verticalGradient(listOf(Color(0xFFFDE68A).copy(alpha = 0.65f), Color(0xFFF59E0B).copy(alpha = 0.28f)))
    drawRect(Color(0xFF0F172A), Offset(234f * sx, 150f * sy), Size(94f * sx, 72f * sy))
    drawRect(winWarm, Offset(240f * sx, 156f * sy), Size(82f * sx, 60f * sy))
    drawLine(Color(0xFF0F172A), Offset(281f * sx, 156f * sy), Offset(281f * sx, 216f * sy), strokeWidth = 3f * u)

    drawRect(Color(0xFF0F172A), Offset(372f * sx, 148f * sy), Size(194f * sx, 78f * sy))
    drawRect(winWarm, Offset(378f * sx, 154f * sy), Size(182f * sx, 66f * sy))
    drawLine(Color(0xFF0F172A), Offset(438f * sx, 154f * sy), Offset(438f * sx, 220f * sy), strokeWidth = 3f * u)
    drawLine(Color(0xFF0F172A), Offset(500f * sx, 154f * sy), Offset(500f * sx, 220f * sy), strokeWidth = 3f * u)

    // Ban cong kinh tang 2
    drawRect(Color(0xFF94A3B8).copy(alpha = 0.26f), Offset(358f * sx, 198f * sy), Size(242f * sx, 30f * sy))
    drawRect(Color(0xFFCBD5E1), Offset(358f * sx, 198f * sy), Size(242f * sx, 30f * sy), style = Stroke(1f * u))
    drawRect(Color(0xFF0F172A), Offset(356f * sx, 196f * sy), Size(246f * sx, 3.5f * sy))
    listOf(418f, 480f, 542f).forEach { x ->
        drawLine(Color(0xFF1E293B), Offset(x * sx, 198f * sy), Offset(x * sx, 228f * sy), strokeWidth = 2f * u)
    }

    // HE MAI NHAT TANG 2
    val roofMainBrush = Brush.verticalGradient(
        colorStops = arrayOf(0f to Color(0xFF3B485C), 0.48f to Color(0xFF253042), 1f to Color(0xFF141C29)),
        startY = 68f * sy,
        endY = 138f * sy,
    )
    val roofSideBrush = Brush.linearGradient(listOf(Color(0xFF2C3748), Color(0xFF111827)))
    drawPolygonBrush(path, sx, sy, floatArrayOf(154f, 134f, 278f, 68f, 522f, 68f, 646f, 134f), roofMainBrush, Color(0xFF0F172A), 1.5f * u)
    drawPolygonBrush(path, sx, sy, floatArrayOf(154f, 134f, 278f, 68f, 294f, 134f), roofSideBrush)
    drawPolygonBrush(path, sx, sy, floatArrayOf(646f, 134f, 522f, 68f, 506f, 134f), roofSideBrush)
    drawLine(Color(0xFF64748B), Offset(278f * sx, 68f * sy), Offset(522f * sx, 68f * sy), strokeWidth = 3f * u, cap = StrokeCap.Round)
    drawLine(Color(0xFF475569), Offset(278f * sx, 68f * sy), Offset(154f * sx, 134f * sy), strokeWidth = 2f * u)
    drawLine(Color(0xFF475569), Offset(522f * sx, 68f * sy), Offset(646f * sx, 134f * sy), strokeWidth = 2f * u)
    listOf(252f to 82f to 548f, 226f to 96f to 574f, 198f to 110f to 602f, 172f to 124f to 628f).forEach { (xy1, x2) ->
        drawLine(Color(0xFF475569).copy(alpha = 0.35f), Offset(xy1.first * sx, xy1.second * sy), Offset(x2 * sx, xy1.second * sy), strokeWidth = 0.9f * u)
    }
    // Mai phu che ban cong phai
    drawPolygonBrush(path, sx, sy, floatArrayOf(348f, 138f, 432f, 96f, 562f, 96f, 632f, 138f), roofMainBrush, Color(0xFF0F172A), 1.2f * u)
    drawLine(Color(0xFF64748B), Offset(432f * sx, 96f * sy), Offset(562f * sx, 96f * sy), strokeWidth = 2.2f * u)
    drawRect(Color(0xFF3A2315), Offset(158f * sx, 133f * sy), Size(484f * sx, 5f * sy))

    // [DEN PHIA TRUOC NHA] 6 den LED am diem mai tang 2
    if (fFront > 0.01f) {
        listOf(195f, 275f, 365f, 455f, 545f, 605f).forEach { x ->
            drawWarmGlowOval(Offset(x * sx, 138f * sy), 12f * u, 12f * u, Color(0xFFF59E0B), 0.70f * fFront)
            drawCircle(Color(0xFFFEF08A).copy(alpha = fFront), 2.8f * u, Offset(x * sx, 137f * sy))
        }
    }

    // -------- TANG 1 & SANH CHINH --------
    drawRect(wallBrush, Offset(176f * sx, 228f * sy), Size(448f * sx, 114f * sy))

    // [DEN HANH LANG] Quang sang sanh hien tang 1
    if (fHall > 0.01f) {
        drawWarmGlowOval(Offset(400f * sx, 298f * sy), 145f * sx, 62f * sy, Color(0xFFFDE68A), 0.92f * fHall)
    }

    // Mai phu tang 1 & mai sanh don
    drawPolygonBrush(path, sx, sy, floatArrayOf(156f, 236f, 218f, 206f, 582f, 206f, 644f, 236f), roofMainBrush, Color(0xFF0F172A), 1.2f * u)
    drawPolygon(path, sx, sy, floatArrayOf(282f, 242f, 326f, 214f, 474f, 214f, 518f, 242f), Color(0xFF2D394C), Color(0xFF0F172A), 1.2f * u)
    drawLine(Color(0xFF64748B), Offset(326f * sx, 214f * sy), Offset(474f * sx, 214f * sy), strokeWidth = 2f * u)
    drawRect(Color(0xFF3A2315), Offset(284f * sx, 241f * sy), Size(232f * sx, 4.5f * sy))

    // [DEN PHIA TRUOC NHA] 4 den LED am diem mai tang 1
    if (fFront > 0.01f) {
        listOf(192f, 246f, 554f, 608f).forEach { x ->
            drawWarmGlowOval(Offset(x * sx, 238f * sy), 12f * u, 12f * u, Color(0xFFF59E0B), 0.70f * fFront)
            drawCircle(Color(0xFFFEF08A).copy(alpha = fFront), 3f * u, Offset(x * sx, 237f * sy))
        }
    }

    // [DEN HANH LANG] 3 bong am tran sanh chinh
    if (fHall > 0.01f) {
        drawCircle(Color(0xFFFEF08A).copy(alpha = fHall), 4f * u, Offset(332f * sx, 245f * sy))
        drawCircle(Color.White.copy(alpha = fHall), 4.5f * u, Offset(400f * sx, 245f * sy))
        drawCircle(Color(0xFFFEF08A).copy(alpha = fHall), 4f * u, Offset(468f * sx, 245f * sy))
    }

    // 2 Cot tru sanh tang 1
    listOf(304f to 302f, 478f to 476f).forEach { (px, bx) ->
        drawRect(Color(0xFFE2E8F0), Offset(px * sx, 245f * sy), Size(18f * sx, 70f * sy))
        drawRect(Color(0xFF94A3B8), Offset(px * sx, 245f * sy), Size(18f * sx, 70f * sy), style = Stroke(0.8f * u))
        drawRect(Color(0xFF334155), Offset(bx * sx, 315f * sy), Size(22f * sx, 27f * sy))
    }

    // Cua chinh go lon + LED khoa + Bac tam cap
    val lockColor = if (isLocked) Color(0xFF22C55E) else Color(0xFFEF4444)
    drawRect(Color(0xFF59341D), Offset(358f * sx, 256f * sy), Size(84f * sx, 78f * sy))
    drawRect(Color(0xFF26140A), Offset(358f * sx, 256f * sy), Size(84f * sx, 78f * sy), style = Stroke(1.5f * u))
    drawLine(Color(0xFF26140A), Offset(400f * sx, 256f * sy), Offset(400f * sx, 334f * sy), strokeWidth = 1.5f * u)
    drawWarmGlowOval(Offset(412f * sx, 296f * sy), 10f * u, 10f * u, lockColor, 0.55f)
    drawCircle(lockColor, 3f * u, Offset(412f * sx, 296f * sy))
    drawRect(Color(0xFF64748B), Offset(304f * sx, 334f * sy), Size(192f * sx, 5f * sy))
    drawRect(Color(0xFF475569), Offset(296f * sx, 339f * sy), Size(208f * sx, 5f * sy))

    // Cua so tang 1
    listOf(200f, 520f).forEach { wx ->
        drawRect(Color(0xFF0F172A), Offset(wx * sx, 258f * sy), Size(80f * sx, 62f * sy))
        drawRect(winWarm, Offset((wx + 5f) * sx, 263f * sy), Size(70f * sx, 52f * sy))
        drawLine(Color(0xFF0F172A), Offset((wx + 40f) * sx, 263f * sy), Offset((wx + 40f) * sx, 315f * sy), strokeWidth = 2.5f * u)
    }

    // Camera toan canh + quet FOV
    if (camFovAlpha > 0.01f) {
        rotate(degrees = camSweepDeg, pivot = Offset(612f * sx, 236f * sy)) {
            drawPolygonBrush(
                path, sx, sy,
                floatArrayOf(612f, 236f, 765f, 455f, 335f, 465f),
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.28f * camFovAlpha), Color.Transparent),
                    startY = 236f * sy,
                    endY = 465f * sy,
                ),
            )
        }
    }
    drawCircle(if (camOn) Color(0xFFEF4444) else Color(0xFF475569), 3f * u, Offset(612f * sx, 236f * sy))

    // TUONG RAO TIEN CANH & CONG NHA MAI NHAT
    drawRect(Color(0xFF1E293B), Offset(0f, 380f * sy), Size(244f * sx, 68f * sy))
    drawRect(Color(0xFF334155), Offset(0f, 380f * sy), Size(244f * sx, 68f * sy), style = Stroke(1.5f * u))
    drawRect(Color(0xFF1E293B), Offset(556f * sx, 380f * sy), Size(244f * sx, 68f * sy))
    drawRect(Color(0xFF334155), Offset(556f * sx, 380f * sy), Size(244f * sx, 68f * sy), style = Stroke(1.5f * u))

    // [DEN XUNG QUANH] 4 den tuong rao
    listOf(64f, 152f, 648f, 736f).forEach { fx ->
        if (fSurr > 0.01f) {
            drawWarmGlowOval(Offset(fx * sx, 378f * sy), 24f * u, 24f * u, Color(0xFFF59E0B), 0.55f * fSurr)
        }
        drawRoundRect(surrBulbColor, Offset((fx - 5f) * sx, 373f * sy), Size(10f * sx, 8f * sy), CornerRadius(1.5f * sx, 1.5f * sy))
    }

    // 2 Tru cong mai Nhat + [DEN PHIA TRUOC NHA] 2 den tru cong
    val frontBulbColor = if (frontOn) Color(0xFFFEF08A) else Color(0xFF475569)
    listOf(238f to 252f, 534f to 548f).forEach { (gx, cx) ->
        drawRect(Color(0xFF334155), Offset(gx * sx, 356f * sy), Size(28f * sx, 102f * sy))
        drawRect(Color(0xFF475569), Offset(gx * sx, 356f * sy), Size(28f * sx, 102f * sy), style = Stroke(1f * u))
        drawPolygon(path, sx, sy, floatArrayOf(gx - 5f, 356f, cx, 346f, gx + 33f, 356f), fill = Color(0xFF1E293B))
        if (fFront > 0.01f) {
            drawWarmGlowOval(Offset(cx * sx, 342f * sy), 32f * u, 32f * u, Color(0xFFF59E0B), 0.62f * fFront)
        }
        drawRoundRect(frontBulbColor, Offset((cx - 7f) * sx, 336f * sy), Size(14f * sx, 11f * sy), CornerRadius(2f * sx, 2f * sy))
    }

    // Canh cong nan sat den + Den bao khoa cong
    drawLine(Color(0xFF475569), Offset(268f * sx, 386f * sy), Offset(532f * sx, 386f * sy), strokeWidth = 3f * u)
    drawLine(Color(0xFF475569), Offset(268f * sx, 442f * sy), Offset(532f * sx, 442f * sy), strokeWidth = 3f * u)
    listOf(302f, 338f, 374f, 426f, 462f, 498f).forEach { bx ->
        drawLine(Color(0xFF475569), Offset(bx * sx, 386f * sy), Offset(bx * sx, 442f * sy), strokeWidth = 3f * u)
    }
    drawCircle(lockColor, 4.5f * u, Offset(252f * sx, 396f * sy))
}

// =========================================================================
// HAM HO TRO VE DA GIAC & QUANG SANG
// =========================================================================
private fun DrawScope.drawPolygon(
    path: Path,
    sx: Float,
    sy: Float,
    pts: FloatArray,
    fill: Color,
    stroke: Color? = null,
    strokePx: Float = 1f,
) {
    loadPath(path, sx, sy, pts)
    drawPath(path, fill)
    if (stroke != null) {
        drawPath(path, stroke, style = Stroke(width = strokePx))
    }
}

private fun DrawScope.drawPolygonBrush(
    path: Path,
    sx: Float,
    sy: Float,
    pts: FloatArray,
    brush: Brush,
    stroke: Color? = null,
    strokePx: Float = 1f,
) {
    loadPath(path, sx, sy, pts)
    drawPath(path, brush)
    if (stroke != null) {
        drawPath(path, stroke, style = Stroke(width = strokePx))
    }
}

private fun loadPath(path: Path, sx: Float, sy: Float, pts: FloatArray) {
    path.reset()
    path.moveTo(pts[0] * sx, pts[1] * sy)
    var i = 2
    while (i + 1 < pts.size) {
        path.lineTo(pts[i] * sx, pts[i + 1] * sy)
        i += 2
    }
    path.close()
}

private fun DrawScope.drawWarmGlowOval(
    center: Offset,
    rx: Float,
    ry: Float,
    color: Color,
    alpha: Float,
) {
    if (alpha <= 0.01f) return
    val maxR = maxOf(rx, ry)
    drawOval(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = alpha.coerceIn(0f, 1f)),
                0.5f to color.copy(alpha = (alpha * 0.35f).coerceIn(0f, 1f)),
                1f to Color.Transparent,
            ),
            center = center,
            radius = maxR,
        ),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(rx * 2f, ry * 2f),
    )
}

private data class BoxF(val x: Float, val y: Float, val w: Float, val h: Float)
