package vn.smarthome.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import vn.smarthome.data.RoomMode
import vn.smarthome.data.RoomUi
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import kotlin.math.PI
import kotlin.math.sin

private const val LV_W = 800f
private const val LV_H = 550f

/**
 * Khung mo phong Phong khach Vector 2.5D tuong tac thoi gian thuc.
 *
 * Ve truc tiep bang Compose Canvas theo he toa do 800x550 dong bo voi file
 * thiet ke HTML:
 *   - Den + do sang (LED khe tran, 6 spotlight, den chum, den cay, LED ke TV, bong toi moi truong)
 *   - Quat tran (quay quan tinh muot khi bat)
 *   - Smart TV (Man hinh 4K HDR Gradient + 5 cot Equalizer + Ambilight sau lung + LED nguon)
 *   - Dieu hoa (Canh dao gio + man hinh LED nhiet do + 3 luong gio lanh doi mau theo nhiet do)
 *   - Rem cua tu dong (2 canh co dan 0..100% + bau troi ngay/dem + vet nang hat xuong san go)
 */
@Composable
fun LivingRoomStage(
    room: RoomUi,
    modifier: Modifier = Modifier,
) {
    val isEvening = room.mode == RoomMode.SLEEP || (!room.lightOn && room.curtainOpen < 25)
    val lightFactor by animateFloatAsState(
        targetValue = if (room.lightOn) room.lightBrightness.coerceIn(10, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "lvLightFactor",
    )
    val curtainRatio by animateFloatAsState(
        targetValue = room.curtainOpen.coerceIn(0, 100) / 100f,
        animationSpec = tween(durationMillis = 750),
        label = "lvCurtainRatio",
    )
    val tvAlpha by animateFloatAsState(
        targetValue = if (room.tvOn) 1f else 0f,
        animationSpec = tween(durationMillis = 550),
        label = "lvTvAlpha",
    )
    val acAlpha by animateFloatAsState(
        targetValue = if (room.acOn) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "lvAcAlpha",
    )
    val eveningAlpha by animateFloatAsState(
        targetValue = if (isEvening) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "lvEveningAlpha",
    )

    // Vat ly quan tinh cho quat tran (dung delay(16L) tranh treo Robolectric waitForIdle).
    var fanAngle by remember { mutableFloatStateOf(0f) }
    var fanVelocity by remember { mutableFloatStateOf(if (room.fanOn) 7.5f else 0f) }
    LaunchedEffect(room.fanOn) {
        var lastNs = System.nanoTime()
        while (isActive && (room.fanOn || fanVelocity > 0.05f)) {
            delay(16L)
            val nowNs = System.nanoTime()
            val dt = ((nowNs - lastNs) / 16_670_000f).coerceIn(0.2f, 3f)
            lastNs = nowNs
            val targetVel = if (room.fanOn) 7.5f else 0f
            val accel = if (room.fanOn) 0.045f else 0.022f
            fanVelocity += (targetVel - fanVelocity) * accel * dt
            if (!room.fanOn && fanVelocity <= 0.05f) {
                fanVelocity = 0f
            } else {
                fanAngle = (fanAngle + fanVelocity * dt) % 360f
            }
        }
    }

    // Chu ky chuyen dong cho luong gio dieu hoa & TV Equalizer
    var animPhase by remember { mutableFloatStateOf(0f) }
    val needEffectLoop = room.acOn || room.tvOn
    LaunchedEffect(needEffectLoop) {
        while (isActive && needEffectLoop) {
            delay(32L)
            animPhase = (animPhase + 0.024f) % 1f
        }
    }

    val bladePath = remember { Path() }
    val airPath = remember { Path() }
    val ledPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }
    val tvTextPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    val sceneBadgeText = when {
        room.tvOn && room.mode == RoomMode.SLEEP -> "\uD83C\uDFAC Rạp phim tại gia"
        isEvening -> "\uD83C\uDF19 Không gian buổi tối"
        room.curtainOpen >= 60 -> "☀\uFE0F Ánh sáng tự nhiên"
        else -> "\uD83D\uDECB\uFE0F Không gian ấm cúng"
    }

    val shape = RoundedCornerShape(Dim.CardRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0B101D))
            .border(Dim.BorderWidth, SH.Border, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / LV_W
            val sy = size.height / LV_H

            // 1. Khung phong phoi canh 2.5D
            drawRoomShell(sx, sy)

            // 2. Tran giat cap + LED hat tran + Spotlight + Quat tran + Den chum
            drawCeilingAndLights(bladePath, sx, sy, lightFactor, room.lightOn, fanAngle, fanVelocity)

            // 3. Cua so kinh + bau troi ngay/dem + rem cua tu dong + vet nang san go
            drawWindowAndCurtains(sx, sy, curtainRatio, eveningAlpha)

            // 4. Vach lam go + khung TV tat + Ke TV & Soundbar
            drawTvWallBase(sx, sy, lightFactor)

            // 5. Dieu hoa treo tuong phai (phan than & canh dao gio)
            drawAcBody(sx, sy, acAlpha, room.acOn)

            // 6. Tham, cay xanh, den cay goc trai, ban tra va sofa chu L
            drawForegroundFurniture(sx, sy, lightFactor, room.lightOn)

            // 7. Lop phu bong toi moi truong
            val naturalBoost = if (isEvening) 0f else curtainRatio * 0.25f
            val darkVal = ((1f - lightFactor) * 0.82f - naturalBoost).coerceIn(0f, 0.86f)
            if (darkVal > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF0D1321).copy(alpha = 0.32f * darkVal),
                            0.68f to Color(0xFF070B16).copy(alpha = 0.78f * darkVal),
                            1.0f to Color(0xFF04070F).copy(alpha = 0.92f * darkVal),
                        ),
                        center = Offset(0.55f * size.width, 0.42f * size.height),
                        radius = maxOf(size.width, size.height) * 0.78f,
                    ),
                    size = size,
                )
            }

            // 8. Lop anh sang noi bat (Smart TV Ambilight + Man hinh 4K HDR, LED + gio dieu hoa)
            drawTvActiveOverlay(tvTextPaint, sx, sy, tvAlpha, lightFactor, room.lightOn, room.tvOn, animPhase)
            drawAcLedAndAirflow(airPath, ledPaint, sx, sy, acAlpha, room.acOn, room.acTemp, animPhase)
        }

        // Pill trang thai khong gian o goc tren ben phai
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.86f))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = sceneBadgeText,
                style = SHType.Caption,
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

private fun DrawScope.poly(sx: Float, sy: Float, vararg pts: Pair<Float, Float>): Path =
    Path().apply {
        if (pts.isEmpty()) return@apply
        moveTo(pts[0].first * sx, pts[0].second * sy)
        for (i in 1 until pts.size) {
            lineTo(pts[i].first * sx, pts[i].second * sy)
        }
        close()
    }

private fun DrawScope.drawRoomShell(sx: Float, sy: Float) {
    // Tuong sau
    drawPath(
        poly(sx, sy, 145f to 152f, 655f to 152f, 655f to 388f, 145f to 388f),
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD2CBC1), Color(0xFFB9B0A4)),
            startY = 152f * sy,
            endY = 388f * sy,
        ),
    )
    // Tuong trai
    drawPath(
        poly(sx, sy, 0f to 0f, 145f to 152f, 145f to 388f, 0f to 515f),
        brush = Brush.linearGradient(
            listOf(Color(0xFFB8B0A5), Color(0xFFD5CEC4)),
            start = Offset(0f, 0f),
            end = Offset(145f * sx, 160f * sy),
        ),
    )
    // Tuong phai
    drawPath(
        poly(sx, sy, 800f to 0f, 655f to 152f, 655f to 388f, 800f to 515f),
        brush = Brush.linearGradient(
            listOf(Color(0xFFAFA79C), Color(0xFFCFC8BD)),
            start = Offset(800f * sx, 0f),
            end = Offset(655f * sx, 160f * sy),
        ),
    )
    // Tran nha
    drawPath(
        poly(sx, sy, 0f to 0f, 800f to 0f, 655f to 152f, 145f to 152f),
        brush = Brush.verticalGradient(
            listOf(Color(0xFFE5E0D8), Color(0xFFCBC3B7)),
            startY = 0f,
            endY = 152f * sy,
        ),
    )
    // San go soi am
    drawPath(
        poly(sx, sy, 0f to 515f, 145f to 388f, 655f to 388f, 800f to 515f, 800f to 550f, 0f to 550f),
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(0xFF5C412E),
                0.55f to Color(0xFF483121),
                1f to Color(0xFF312014),
            ),
            startY = 388f * sy,
            endY = 550f * sy,
        ),
    )
    // Duong van san go phoi canh
    val grainColor = Color(0xFF291A10).copy(alpha = 0.48f)
    val strokeW = 1.2f * minOf(sx, sy)
    val seams = listOf(
        (210f to 388f) to (70f to 550f),
        (285f to 388f) to (215f to 550f),
        (365f to 388f) to (355f to 550f),
        (445f to 388f) to (495f to 550f),
        (525f to 388f) to (635f to 550f),
        (595f to 388f) to (760f to 550f),
    )
    seams.forEach { (a, b) ->
        drawLine(grainColor, Offset(a.first * sx, a.second * sy), Offset(b.first * sx, b.second * sy), strokeW)
    }
    val jointColor = grainColor.copy(alpha = 0.35f)
    drawLine(jointColor, Offset(95f * sx, 436f * sy), Offset(705f * sx, 436f * sy), strokeW)
    drawLine(jointColor, Offset(38f * sx, 490f * sy), Offset(762f * sx, 490f * sy), strokeW)
}

private fun DrawScope.drawCeilingAndLights(
    bladePath: Path,
    sx: Float,
    sy: Float,
    lightFactor: Float,
    lightOn: Boolean,
    fanAngle: Float,
    fanVelocity: Float,
) {
    val trayPath = poly(sx, sy, 150f to 28f, 650f to 28f, 570f to 126f, 230f to 126f)
    drawPath(
        trayPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD7CFC3), Color(0xFFEFE7DA)),
            startY = 28f * sy,
            endY = 126f * sy,
        ),
    )
    drawPath(trayPath, Color(0xFFBCB3A4), style = Stroke(width = 2f * minOf(sx, sy)))

    if (lightFactor > 0.01f) {
        drawPath(
            trayPath,
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFDE68A).copy(alpha = 0.72f * lightFactor),
                    0.65f to Color(0xFFF59E0B).copy(alpha = 0.22f * lightFactor),
                    1f to Color.Transparent,
                ),
                center = Offset(400f * sx, 77f * sy),
                radius = 275f * sx,
            ),
        )
        val innerCove = poly(sx, sy, 158f to 32f, 642f to 32f, 566f to 122f, 234f to 122f)
        drawPath(
            innerCove,
            Color(0xFFFDE047).copy(alpha = 0.28f * lightFactor),
            style = Stroke(width = 10f * minOf(sx, sy)),
        )
        drawPath(
            innerCove,
            Color(0xFFFDE047).copy(alpha = 0.85f * lightFactor),
            style = Stroke(width = 3.5f * minOf(sx, sy)),
        )

        val spots = listOf(
            Triple(105f, 44f, 11f to 4f),
            Triple(695f, 44f, 11f to 4f),
            Triple(185f, 115f, 9f to 3.2f),
            Triple(615f, 115f, 9f to 3.2f),
            Triple(320f, 140f, 7f to 2.5f),
            Triple(480f, 140f, 7f to 2.5f),
        )
        spots.forEach { (x, y, rad) ->
            val rx = rad.first * sx
            val ry = rad.second * sy
            drawOval(
                color = Color(0xFFFEF08A).copy(alpha = 0.55f * lightFactor),
                topLeft = Offset(x * sx - rx * 1.4f, y * sy - ry * 1.4f),
                size = Size(rx * 2.8f, ry * 2.8f),
            )
            drawOval(
                color = Color.White.copy(alpha = 0.95f * lightFactor),
                topLeft = Offset(x * sx - rx * 0.55f, y * sy - ry * 0.55f),
                size = Size(rx * 1.1f, ry * 1.1f),
            )
        }

        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFEF08A).copy(alpha = 0.9f * lightFactor),
                    0.45f to Color(0xFFFBBF24).copy(alpha = 0.35f * lightFactor),
                    1f to Color.Transparent,
                ),
                center = Offset(400f * sx, 96f * sy),
                radius = 125f * sx,
            ),
            topLeft = Offset(275f * sx, 44f * sy),
            size = Size(250f * sx, 104f * sy),
        )
    }

    // Quat tran 5 canh phoi canh 3D (o trung tam tran giat cap, phia tren den chum)
    drawLivingCeilingFan(bladePath, sx, sy, fanAngle, fanVelocity)

    // Ket cau den chum 2 vong tron
    val wireColor = Color(0xFF64748B)
    val wireW = 1.2f * minOf(sx, sy)
    drawLine(wireColor, Offset(365f * sx, 72f * sy), Offset(352f * sx, 96f * sy), wireW)
    drawLine(wireColor, Offset(435f * sx, 72f * sy), Offset(448f * sx, 96f * sy), wireW)
    drawLine(wireColor, Offset(400f * sx, 62f * sy), Offset(400f * sx, 109f * sy), wireW)

    val ringColor1 = lerp(Color(0xFF64748B), Color(0xFFFEF08A), if (lightOn) 0.4f + 0.6f * lightFactor else 0f)
    val ringColor2 = lerp(Color(0xFF64748B), Color(0xFFFEF9C3), if (lightOn) 0.4f + 0.6f * lightFactor else 0f)
    drawOval(
        color = ringColor1,
        topLeft = Offset(334f * sx, 84f * sy),
        size = Size(132f * sx, 28f * sy),
        style = Stroke(width = 4f * minOf(sx, sy)),
    )
    drawOval(
        color = Color(0xFF334155).copy(alpha = 0.6f),
        topLeft = Offset(334f * sx, 84f * sy),
        size = Size(132f * sx, 28f * sy),
        style = Stroke(width = 1.4f * minOf(sx, sy)),
    )
    drawOval(
        color = ringColor2,
        topLeft = Offset(362f * sx, 103f * sy),
        size = Size(76f * sx, 16f * sy),
        style = Stroke(width = 3.2f * minOf(sx, sy)),
    )
}

private fun DrawScope.drawLivingCeilingFan(
    bladePath: Path,
    sx: Float,
    sy: Float,
    fanAngle: Float,
    fanVelocity: Float,
) {
    // Ty treo quat
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(396f * sx, 30f * sy),
        size = Size(8f * sx, 22f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )

    val cx = 400f * sx
    val cy = 53f * sy
    val rotorRadius = 82f * sx
    val u = rotorRadius / 100f

    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = 1f, scaleY = 0.30f, pivot = Offset.Zero)
    }) {
        if (fanVelocity > 4.2f) {
            withTransform({ rotate(degrees = fanAngle - 16f, pivot = Offset.Zero) }) {
                drawLivingFanBlades(bladePath, u, alpha = 0.16f)
            }
        }
        withTransform({ rotate(degrees = fanAngle, pivot = Offset.Zero) }) {
            drawLivingFanBlades(bladePath, u, alpha = 1f)
        }
        drawCircle(Color(0xFF283244), radius = 18f * u, center = Offset.Zero)
        drawCircle(Color(0xFF64748B), radius = 18f * u, center = Offset.Zero, style = Stroke(width = 2f * u))
        drawCircle(if (fanVelocity > 0.2f) Color(0xFF38BDF8) else Color(0xFF1E293B), radius = 7f * u, center = Offset.Zero)
    }
}

private fun DrawScope.drawLivingFanBlades(bladePath: Path, u: Float, alpha: Float) {
    val brush = Brush.linearGradient(
        0.0f to Color(0xFF64748B).copy(alpha = alpha),
        0.6f to Color(0xFF475569).copy(alpha = alpha),
        1.0f to Color(0xFF334155).copy(alpha = alpha),
        start = Offset(-12f * u, -92f * u),
        end = Offset(12f * u, 0f),
    )
    bladePath.reset()
    bladePath.moveTo(-4f * u, -8f * u)
    bladePath.lineTo(-10f * u, -86f * u)
    bladePath.quadraticTo(0f, -94f * u, 10f * u, -86f * u)
    bladePath.lineTo(4f * u, -8f * u)
    bladePath.close()

    for (i in 0 until 5) {
        withTransform({ rotate(degrees = i * 72f, pivot = Offset.Zero) }) {
            drawPath(bladePath, brush)
            if (alpha >= 0.9f) {
                drawPath(bladePath, Color(0xFF1E293B).copy(alpha = 0.6f), style = Stroke(width = 1f * u))
            }
        }
    }
}

private fun DrawScope.drawWindowAndCurtains(
    sx: Float,
    sy: Float,
    curtainRatio: Float,
    eveningAlpha: Float,
) {
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(460f * sx, 160f * sy),
        size = Size(176f * sx, 228f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )

    clipRect(466f * sx, 166f * sy, 630f * sx, 380f * sy) {
        // Bau troi ban ngay
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF38BDF8),
                    0.6f to Color(0xFFBAE6FD),
                    1f to Color(0xFFFEF3C7),
                ),
                startY = 166f * sy,
                endY = 380f * sy,
            ),
            topLeft = Offset(466f * sx, 166f * sy),
            size = Size(164f * sx, 214f * sy),
        )

        // Bau troi buoi toi
        if (eveningAlpha > 0.01f) {
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF090F24).copy(alpha = eveningAlpha),
                        0.55f to Color(0xFF172554).copy(alpha = eveningAlpha),
                        1f to Color(0xFF1E3A5F).copy(alpha = eveningAlpha),
                    ),
                    startY = 166f * sy,
                    endY = 380f * sy,
                ),
                topLeft = Offset(466f * sx, 166f * sy),
                size = Size(164f * sx, 214f * sy),
            )
            drawOval(
                color = Color(0xFFFEF9C3).copy(alpha = 0.9f * eveningAlpha),
                topLeft = Offset(574f * sx, 193f * sy),
                size = Size(22f * sx, 22f * sy),
            )
            drawOval(
                color = Color(0xFF111C3D).copy(alpha = eveningAlpha),
                topLeft = Offset(580f * sx, 191f * sy),
                size = Size(20f * sx, 20f * sy),
            )
            listOf(
                Triple(495f, 196f, 1.4f),
                Triple(525f, 186f, 1.2f),
                Triple(545f, 218f, 1.3f),
                Triple(510f, 235f, 1.1f),
            ).forEach { (x, y, r) ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f * eveningAlpha),
                    radius = r * minOf(sx, sy),
                    center = Offset(x * sx, y * sy),
                )
            }
        }

        // Duong chan troi thanh pho
        val skyline = poly(
            sx, sy,
            466f to 380f, 466f to 315f, 484f to 315f, 484f to 295f, 505f to 295f,
            505f to 322f, 522f to 322f, 522f to 276f, 548f to 276f, 548f to 310f,
            568f to 310f, 568f to 288f, 595f to 288f, 595f to 318f, 615f to 318f,
            615f to 302f, 632f to 302f, 632f to 380f,
        )
        drawPath(skyline, Color(0xFF1E293B).copy(alpha = 0.45f))

        if (eveningAlpha > 0.01f) {
            val winGlow = Color(0xFFFDE047).copy(alpha = 0.9f * eveningAlpha)
            listOf(
                528f to 286f, 537f to 294f, 528f to 304f,
                575f to 296f, 584f to 305f, 490f to 304f,
            ).forEach { (wx, wy) ->
                drawRect(winGlow, Offset(wx * sx, wy * sy), Size(4f * sx, 4f * sy))
            }
        }

        drawRect(Color(0xFF334155), Offset(546f * sx, 166f * sy), Size(4f * sx, 214f * sy))
        drawRect(Color(0xFF334155), Offset(466f * sx, 288f * sy), Size(164f * sx, 3.5f * sy))

        // Rem cua tu dong co dan theo curtainRatio
        val scaleX = (1f - curtainRatio * 0.84f).coerceIn(0.16f, 1f)
        val lBase = 466f
        val lW = 84f * scaleX
        drawRect(Color(0xFF475569), Offset(lBase * sx, 166f * sy), Size(lW * sx, 216f * sy))
        drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset((lBase + 12f * scaleX) * sx, 166f * sy), Size(12f * scaleX * sx, 216f * sy))
        drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset((lBase + 38f * scaleX) * sx, 166f * sy), Size(12f * scaleX * sx, 216f * sy))
        drawRect(Color(0xFF64748B).copy(alpha = 0.40f), Offset((lBase + 64f * scaleX) * sx, 166f * sy), Size(10f * scaleX * sx, 216f * sy))

        val rEdge = 630f
        val rLeft = rEdge - 84f * scaleX
        drawRect(Color(0xFF475569), Offset(rLeft * sx, 166f * sy), Size(84f * scaleX * sx, 216f * sy))
        drawRect(Color(0xFF64748B).copy(alpha = 0.40f), Offset((rEdge - 74f * scaleX) * sx, 166f * sy), Size(10f * scaleX * sx, 216f * sy))
        drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset((rEdge - 50f * scaleX) * sx, 166f * sy), Size(12f * scaleX * sx, 216f * sy))
        drawRect(Color(0xFF334155).copy(alpha = 0.55f), Offset((rEdge - 24f * scaleX) * sx, 166f * sy), Size(12f * scaleX * sx, 216f * sy))
    }

    // Thanh ray rem
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(456f * sx, 157f * sy),
        size = Size(184f * sx, 6f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )

    // Vet sang tu nhien tu cua so hat xuong san go
    val spillFactor = ((1f - eveningAlpha) * (curtainRatio * 0.95f) + eveningAlpha * 0.05f).coerceIn(0f, 1f)
    if (spillFactor > 0.01f) {
        drawPath(
            poly(sx, sy, 466f to 388f, 630f to 388f, 530f to 520f, 315f to 520f),
            brush = Brush.linearGradient(
                listOf(
                    Color(0xFFFEF9C3).copy(alpha = 0.42f * spillFactor),
                    Color.Transparent,
                ),
                start = Offset(600f * sx, 388f * sy),
                end = Offset(330f * sx, 520f * sy),
            ),
        )
    }
}

private fun DrawScope.drawTvWallBase(
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    // Vach lam go oc cho
    drawRoundRect(
        color = Color(0xFF3D2618),
        topLeft = Offset(166f * sx, 162f * sy),
        size = Size(268f * sx, 226f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )
    val slatBrush = Brush.verticalGradient(
        listOf(Color(0xFF7B5236), Color(0xFF4B301E)),
        startY = 164f * sy,
        endY = 388f * sy,
    )
    for (i in 0 until 14) {
        drawRect(slatBrush, Offset((172f + i * 18f) * sx, 164f * sy), Size(12f * sx, 224f * sy))
    }
    drawRect(slatBrush, Offset(422f * sx, 164f * sy), Size(8f * sx, 224f * sy))

    // Khung vien & man hinh tat Smart TV
    drawRoundRect(
        color = Color(0xFF0B0F19),
        topLeft = Offset(204f * sx, 198f * sy),
        size = Size(192f * sx, 108f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(204f * sx, 198f * sy),
        size = Size(192f * sx, 108f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
        style = Stroke(width = 2f * minOf(sx, sy)),
    )
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(209f * sx, 203f * sy),
        size = Size(182f * sx, 98f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )

    // Ke TV treo tuong toi gian
    drawRoundRect(
        color = Color(0xFF2A2E37),
        topLeft = Offset(184f * sx, 330f * sy),
        size = Size(232f * sx, 28f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(184f * sx, 330f * sy),
        size = Size(232f * sx, 28f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    drawLine(Color(0xFF1E293B), Offset(260f * sx, 330f * sy), Offset(260f * sx, 358f * sy), 1.5f * minOf(sx, sy))
    drawLine(Color(0xFF1E293B), Offset(340f * sx, 330f * sy), Offset(340f * sx, 358f * sy), 1.5f * minOf(sx, sy))

    if (lightFactor > 0.01f) {
        drawRect(
            color = Color(0xFFFDE047).copy(alpha = 0.28f * lightFactor),
            topLeft = Offset(190f * sx, 357f * sy),
            size = Size(220f * sx, 9f * sy),
        )
        drawRect(
            color = Color(0xFFFDE047).copy(alpha = 0.85f * lightFactor),
            topLeft = Offset(192f * sx, 358f * sy),
            size = Size(216f * sx, 3f * sy),
        )
    }

    // Loa Soundbar
    drawRoundRect(
        color = Color(0xFF111827),
        topLeft = Offset(252f * sx, 322f * sy),
        size = Size(96f * sx, 8f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(252f * sx, 322f * sy),
        size = Size(96f * sx, 8f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
        style = Stroke(width = 0.8f * minOf(sx, sy)),
    )
}

private fun DrawScope.drawTvActiveOverlay(
    textPaint: Paint,
    sx: Float,
    sy: Float,
    tvAlpha: Float,
    lightFactor: Float,
    lightOn: Boolean,
    tvOn: Boolean,
    animPhase: Float,
) {
    if (tvAlpha > 0.01f) {
        // Ambilight hat sang sau lung TV
        val glowStrength = tvAlpha * (if (lightOn) 0.55f + 0.15f * (1f - lightFactor) else 0.95f)
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFF60A5FA).copy(alpha = 0.72f * glowStrength),
                    0.48f to Color(0xFF818CF8).copy(alpha = 0.38f * glowStrength),
                    1f to Color.Transparent,
                ),
                center = Offset(300f * sx, 252f * sy),
                radius = 135f * sx,
            ),
            topLeft = Offset(164f * sx, 174f * sy),
            size = Size(272f * sx, 156f * sy),
        )

        // Ve lai khung vien & man hinh TV sang ro phia tren lop toi
        drawRoundRect(
            color = Color(0xFF0B0F19),
            topLeft = Offset(204f * sx, 198f * sy),
            size = Size(192f * sx, 108f * sy),
            cornerRadius = CornerRadius(4f * sx, 4f * sy),
            alpha = tvAlpha,
        )
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(204f * sx, 198f * sy),
            size = Size(192f * sx, 108f * sy),
            cornerRadius = CornerRadius(4f * sx, 4f * sy),
            style = Stroke(width = 2f * minOf(sx, sy)),
            alpha = tvAlpha,
        )

        val t = (sin(animPhase * 2f * PI.toFloat()) + 1f) * 0.5f
        val c1 = lerp(Color(0xFF0EA5E9), Color(0xFF6366F1), t).copy(alpha = tvAlpha)
        val c2 = lerp(Color(0xFFEC4899), Color(0xFF14B8A6), t).copy(alpha = tvAlpha)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(c1, Color(0xFF6366F1).copy(alpha = tvAlpha), c2),
                start = Offset(209f * sx, 203f * sy),
                end = Offset(391f * sx, 301f * sy),
            ),
            topLeft = Offset(209f * sx, 203f * sy),
            size = Size(182f * sx, 98f * sy),
            cornerRadius = CornerRadius(2f * sx, 2f * sy),
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.2f * tvAlpha),
            topLeft = Offset(209f * sx, 203f * sy),
            size = Size(182f * sx, 98f * sy),
        )

        // 5 cot song nhac Equalizer
        val eqColors = listOf(Color.White, Color(0xFFA5F3FC), Color.White, Color(0xFFFBCFE8), Color.White)
        val eqPhases = listOf(0f, 1.3f, 2.6f, 0.7f, 2.0f)
        val barW = 7f * sx
        val barGap = 6f * sx
        val startX = 300f * sx - (5 * barW + 4 * barGap) / 2f
        val bottomY = 262f * sy
        val maxH = 34f * sy
        for (i in 0 until 5) {
            val hFrac = 0.35f + 0.65f * ((sin(animPhase * 2f * PI.toFloat() * 2f + eqPhases[i]) + 1f) * 0.5f)
            val bh = maxH * hFrac
            drawRoundRect(
                color = eqColors[i].copy(alpha = 0.92f * tvAlpha),
                topLeft = Offset(startX + i * (barW + barGap), bottomY - bh),
                size = Size(barW, bh),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f),
            )
        }

        // Nhan "4K HDR CINEMA" giua man hinh TV
        textPaint.textSize = 10f * minOf(sx, sy)
        textPaint.color = Color.White.copy(alpha = 0.95f * tvAlpha).toArgb()
        textPaint.letterSpacing = 0.12f
        val canvas = drawContext.canvas.nativeCanvas
        canvas.drawText("4K HDR CINEMA", 300f * sx, 282f * sy, textPaint)
    }

    // Den LED trang thai nguon duoi canh TV
    drawCircle(
        color = if (tvOn) Color(0xFF22C55E) else Color(0xFFEF4444),
        radius = 2.2f * minOf(sx, sy),
        center = Offset(300f * sx, 304f * sy),
    )
}

private fun DrawScope.drawAcBody(
    sx: Float,
    sy: Float,
    acAlpha: Float,
    acOn: Boolean,
) {
    val body = poly(sx, sy, 682f to 110f, 790f to 56f, 790f to 140f, 682f to 174f)
    drawPath(body, Color(0xFFE2E8F0))
    drawPath(body, Color(0xFF94A3B8), style = Stroke(width = 1.2f * minOf(sx, sy)))
    drawPath(poly(sx, sy, 682f to 174f, 790f to 140f, 778f to 154f, 674f to 183f), Color(0xFFCBD5E1))
    drawPath(poly(sx, sy, 686f to 174f, 780f to 144f, 776f to 152f, 682f to 180f), Color(0xFF334155))

    val dy = if (acOn) 5f * acAlpha else 0f
    val louver = poly(
        sx, sy,
        684f to (177f + dy),
        778f to (147f + dy),
        775f to (153f + dy + 2f * acAlpha),
        681f to (182f + dy + 2f * acAlpha),
    )
    drawPath(louver, Color(0xFFF8FAFC))
    drawPath(louver, Color(0xFF94A3B8), style = Stroke(width = 0.8f * minOf(sx, sy)))

    // Nen man hinh LED tren than may lanh
    drawPath(poly(sx, sy, 710f to 142f, 754f to 123f, 754f to 144f, 710f to 162f), Color(0xFF0F172A).copy(alpha = 0.88f))
}

private fun DrawScope.drawAcLedAndAirflow(
    airPath: Path,
    ledPaint: Paint,
    sx: Float,
    sy: Float,
    acAlpha: Float,
    acOn: Boolean,
    acTemp: Int,
    animPhase: Float,
) {
    // Luong gio lanh uon luon khi bat dieu hoa
    if (acAlpha > 0.01f) {
        val coldFactor = ((30 - acTemp).coerceIn(0, 14)) / 14f
        val streamAlpha = acAlpha * (0.38f + coldFactor * 0.62f)
        val tint = when {
            acTemp <= 21 -> Color(0xFF38BDF8)
            acTemp <= 25 -> Color(0xFF7DD3FC)
            else -> Color(0xFFBAE6FD)
        }
        val cycle = 160f * minOf(sx, sy)
        val dash = 46f * minOf(sx, sy)
        val gap = 114f * minOf(sx, sy)

        val streams = listOf(
            Triple(Offset(755f * sx, 160f * sy), Offset(695f * sx, 195f * sy), Offset(560f * sx, 285f * sy)),
            Triple(Offset(735f * sx, 174f * sy), Offset(670f * sx, 222f * sy), Offset(525f * sx, 328f * sy)),
            Triple(Offset(770f * sx, 170f * sy), Offset(725f * sx, 232f * sy), Offset(625f * sx, 345f * sy)),
        )
        streams.forEachIndexed { idx, (p0, p1, p2) ->
            val p = (animPhase + idx * 0.33f) % 1f
            airPath.reset()
            airPath.moveTo(p0.x, p0.y)
            airPath.quadraticTo(p1.x, p1.y, p2.x, p2.y)
            drawPath(
                path = airPath,
                color = tint.copy(alpha = streamAlpha * 0.85f),
                style = Stroke(
                    width = (2.8f - idx * 0.3f) * minOf(sx, sy),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), -p * cycle),
                ),
            )
        }
    }

    // Man hinh LED nhiet do (luon ro ke ca khi tat den phong)
    val label = if (acOn) "${acTemp}°C" else "OFF"
    val cx = 732f * sx
    val cy = 143f * sy

    ledPaint.textSize = 11.5f * minOf(sx, sy)
    ledPaint.color = if (acOn) {
        Color(0xFFE0F2FE).toArgb()
    } else {
        Color(0xFFE0F2FE).copy(alpha = 0.38f).toArgb()
    }
    if (acOn) {
        ledPaint.setShadowLayer(6f * sx, 0f, 0f, Color(0xFF38BDF8).toArgb())
    } else {
        ledPaint.clearShadowLayer()
    }

    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.translate(cx, cy)
    canvas.skew(0f, -0.40f * sy / sx)
    val fm = ledPaint.fontMetrics
    val baseline = -(fm.ascent + fm.descent) / 2f
    canvas.drawText(label, 0f, baseline, ledPaint)
    canvas.restore()
}

private fun DrawScope.drawForegroundFurniture(
    sx: Float,
    sy: Float,
    lightFactor: Float,
    lightOn: Boolean,
) {
    // Tham trai san
    drawPath(
        poly(sx, sy, 150f to 510f, 275f to 412f, 585f to 412f, 675f to 515f, 225f to 538f),
        Color(0xFF475569).copy(alpha = 0.88f),
    )
    drawPath(
        poly(sx, sy, 164f to 507f, 282f to 418f, 578f to 418f, 660f to 510f, 230f to 530f),
        Color(0xFF526075).copy(alpha = 0.65f),
    )

    // Cay xanh goc cua so
    drawPath(poly(sx, sy, 622f to 368f, 652f to 368f, 646f to 412f, 628f to 412f), Color(0xFFCBD5E1))
    drawPath(
        poly(sx, sy, 622f to 368f, 652f to 368f, 646f to 412f, 628f to 412f),
        Color(0xFF94A3B8),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    drawOval(Color(0xFF3F2E21), Offset(622f * sx, 364f * sy), Size(30f * sx, 8f * sy))

    fun drawLeaf(c: Color, q1: Pair<Float, Float>, tip: Pair<Float, Float>, q2: Pair<Float, Float>, end: Pair<Float, Float>) {
        val p = Path().apply {
            moveTo(637f * sx, 368f * sy)
            quadraticTo(q1.first * sx, q1.second * sy, tip.first * sx, tip.second * sy)
            quadraticTo(q2.first * sx, q2.second * sy, end.first * sx, end.second * sy)
            close()
        }
        drawPath(p, c)
    }
    drawLeaf(Color(0xFF15803D), 620f to 335f, 608f to 325f, 628f to 322f, 637f to 355f)
    drawLeaf(Color(0xFF16A34A), 655f to 332f, 668f to 318f, 650f to 315f, 638f to 350f)
    drawLeaf(Color(0xFF22C55E), 634f to 322f, 637f to 298f, 648f to 318f, 640f to 358f)
    drawLeaf(Color(0xFF166534), 615f to 350f, 604f to 352f, 618f to 364f, 635f to 368f)

    // Den cay dung goc trai
    if (lightFactor > 0.01f) {
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFEF3C7).copy(alpha = 0.92f * lightFactor),
                    0.45f to Color(0xFFF59E0B).copy(alpha = 0.38f * lightFactor),
                    1f to Color.Transparent,
                ),
                center = Offset(105f * sx, 285f * sy),
                radius = 76f * sx,
            ),
            topLeft = Offset(29f * sx, 209f * sy),
            size = Size(152f * sx, 152f * sy),
        )
    }
    drawOval(Color(0xFF1E293B), Offset(78f * sx, 429f * sy), Size(36f * sx, 10f * sy))
    drawLine(Color(0xFF334155), Offset(96f * sx, 432f * sy), Offset(96f * sx, 300f * sy), 3.5f * minOf(sx, sy))
    val shadePath = poly(sx, sy, 82f to 262f, 118f to 262f, 126f to 304f, 74f to 304f)
    drawPath(shadePath, lerp(Color(0xFF8C857B), Color(0xFFFEF3C7), if (lightOn) 0.35f + 0.65f * lightFactor else 0f))
    drawPath(shadePath, Color(0xFFD6C7B2), style = Stroke(width = 1f * minOf(sx, sy)))

    // Ban tra mat da & bo am tra
    drawOval(Color(0xFF1E293B).copy(alpha = 0.45f), Offset(312f * sx, 452f * sy), Size(136f * sx, 40f * sy))
    drawLine(Color(0xFF334155), Offset(342f * sx, 448f * sy), Offset(342f * sx, 474f * sy), 3f * minOf(sx, sy))
    drawLine(Color(0xFF334155), Offset(418f * sx, 448f * sy), Offset(418f * sx, 474f * sy), 3f * minOf(sx, sy))
    drawOval(Color(0xFFE2E8F0), Offset(314f * sx, 425f * sy), Size(132f * sx, 42f * sy))
    drawOval(Color(0xFFCBD5E1), Offset(314f * sx, 425f * sy), Size(132f * sx, 42f * sy), style = Stroke(width = 2f * minOf(sx, sy)))
    drawOval(Color(0xFF475569), Offset(352f * sx, 437f * sy), Size(40f * sx, 14f * sy))
    drawRoundRect(Color(0xFFF8FAFC), Offset(366f * sx, 435f * sy), Size(9f * sx, 9f * sy), CornerRadius(2f * sx, 2f * sy))

    // Sofa chu L & goi tua
    val cushionGrad = Brush.verticalGradient(
        listOf(Color(0xFF6B778C), Color(0xFF475163)),
        startY = 390f * sy,
        endY = 535f * sy,
    )
    val bodyGrad = Brush.linearGradient(
        listOf(Color(0xFF586375), Color(0xFF353E4D)),
        start = Offset(36f * sx, 358f * sy),
        end = Offset(160f * sx, 438f * sy),
    )
    drawPath(poly(sx, sy, 48f to 438f, 160f to 392f, 225f to 408f, 118f to 468f), cushionGrad)
    drawPath(poly(sx, sy, 36f to 396f, 154f to 358f, 160f to 392f, 48f to 438f), bodyGrad)
    drawPath(poly(sx, sy, 48f to 438f, 118f to 468f, 118f to 505f, 48f to 472f), Color(0xFF2A3240))
    drawPath(poly(sx, sy, 118f to 468f, 320f to 486f, 292f to 535f, 88f to 510f), cushionGrad)
    drawPath(poly(sx, sy, 88f to 510f, 292f to 535f, 292f to 550f, 88f to 550f), Color(0xFF28303E))

    val pillowBlue = Path().apply {
        moveTo(78f * sx, 412f * sy)
        quadraticTo(96f * sx, 395f * sy, 118f * sx, 408f * sy)
        lineTo(108f * sx, 432f * sy)
        quadraticTo(84f * sx, 434f * sy, 78f * sx, 412f * sy)
        close()
    }
    drawPath(pillowBlue, Color(0xFF3B82F6))

    val pillowAmber = Path().apply {
        moveTo(126f * sx, 394f * sy)
        quadraticTo(146f * sx, 378f * sy, 168f * sx, 390f * sy)
        lineTo(156f * sx, 412f * sy)
        quadraticTo(134f * sx, 414f * sy, 126f * sx, 394f * sy)
        close()
    }
    drawPath(pillowAmber, Color(0xFFF59E0B))
}
