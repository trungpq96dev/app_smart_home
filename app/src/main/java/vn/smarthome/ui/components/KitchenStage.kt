package vn.smarthome.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import vn.smarthome.data.RoomUi
import vn.smarthome.data.stoveWatts
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import kotlin.math.PI
import kotlin.math.sin

private const val KT_W = 800f
private const val KT_H = 550f

/**
 * Khung mo phong Phong bep Vector 2.5D tuong tac thoi gian thuc.
 *
 * Ve truc tiep bang Compose Canvas theo he toa do 800x550 dong bo voi file
 * thiet ke HTML:
 *   - Den bep + do sang (LED tran thach cao, 4 downlight, 2 den tha, LED gam tu, bong toi)
 *   - May hut mui chu T (den chieu vung nau, 3 LED cap do, 3 luong khi hut bay len)
 *   - Bep tu am kinh (vong nhiet do cam, man hinh P1..P9, noi inox, hoi nuoc boc len)
 *   - Tu lanh thong minh 2 canh (man hinh 4C / -18C) & Dao bep go oc cho
 *   - Cam bien Gas & Khoi gan tran (LED xanh/do nhap nhay + vien canh bao + banner)
 */
@Composable
fun KitchenStage(
    room: RoomUi,
    onSilenceGasAlert: () -> Unit = {},
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hoodOn = room.fanOn
    val hoodSpeed = room.hoodSpeed.coerceIn(1, 3)
    val stoveLevel = room.stoveLevel.coerceIn(1, 9)

    val lightFactor by animateFloatAsState(
        targetValue = if (room.lightOn) room.lightBrightness.coerceIn(10, 100) / 100f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "ktLightFactor",
    )
    val hoodAlpha by animateFloatAsState(
        targetValue = if (hoodOn) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "ktHoodAlpha",
    )
    val stoveRatio by animateFloatAsState(
        targetValue = if (room.stoveOn) 0.28f + (stoveLevel / 9f) * 0.72f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "ktStoveRatio",
    )

    // Chuyen dong luong hut may hut mui (toc do tang theo cap 1 -> 2 -> 3)
    var hoodPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(hoodOn, hoodSpeed) {
        while (isActive && hoodOn) {
            delay(16L)
            val periodSec = when (hoodSpeed) {
                1 -> 2.1f
                2 -> 1.35f
                else -> 0.8f
            }
            hoodPhase = (hoodPhase + (0.016f / periodSec)) % 1f
        }
    }

    // Chuyen dong hoi nuoc boc len tu noi (nhanh hon khi cong suat cao hoac bat hut mui)
    var steamPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(room.stoveOn, stoveLevel, hoodOn, hoodSpeed) {
        while (isActive && room.stoveOn) {
            delay(16L)
            val steamSec = if (hoodOn) {
                maxOf(0.85f, 2.2f - hoodSpeed * 0.42f)
            } else {
                maxOf(1.3f, 3.2f - stoveLevel * 0.19f)
            }
            steamPhase = (steamPhase + (0.016f / steamSec)) % 1f
        }
    }

    // Nhap nhay canh bao do khi ro ri Gas / Khoi
    var alertPulse by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(room.gasAlert) {
        if (!room.gasAlert) {
            alertPulse = 0f
            return@LaunchedEffect
        }
        var t = 0f
        while (isActive && room.gasAlert) {
            delay(32L)
            t = (t + 0.032f / 1.1f) % 1f
            alertPulse = ((sin(t * 2f * PI.toFloat()) + 1f) * 0.5f)
        }
    }

    val auxPath = remember { Path() }
    val ledPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
    }
    val fridgePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    val watts = if (room.stoveOn) stoveWatts(stoveLevel) else 0
    val stateBadgeText = when {
        room.gasAlert -> "\uD83D\uDEA8 Đang xử lý thông gió khẩn cấp"
        room.stoveOn -> "\uD83C\uDF73 Đang nấu ăn (${watts}W)"
        !room.lightOn -> "\uD83C\uDF19 Chế độ nghỉ / Tiết kiệm điện"
        else -> "✨ Không gian bếp sẵn sàng"
    }

    val shape = RoundedCornerShape(Dim.CardRadius)
    val borderColor = if (room.gasAlert) {
        SH.Red.copy(alpha = 0.55f + 0.45f * alertPulse)
    } else {
        SH.Border
    }
    val boxMod = if (compact) {
        modifier.background(Color(0xFF090E1A))
    } else {
        modifier
            .clip(shape)
            .background(Color(0xFF090E1A))
            .border(if (room.gasAlert) 2.dp else Dim.BorderWidth, borderColor, shape)
    }

    Box(modifier = boxMod) {
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / KT_W
            val sy = size.height / KT_H

            // 1. Khung phong bep phoi canh 2.5D
            drawKitchenShell(sx, sy)

            // 2. Tran thach cao giat cap & den LED tran
            drawKitchenCeilingAndLights(sx, sy, lightFactor)

            // 3. Cam bien khoi / gas gan tran (than thiet bi)
            drawGasSensorBody(sx, sy)

            // 4. He tu bep go oc cho + da Marble + LED gam tu + bon rua
            drawCabinetsAndCounter(auxPath, sx, sy, lightFactor)

            // 5. May hut mui chu T den nham + den chieu vung nau
            drawRangeHoodBase(sx, sy, hoodAlpha, hoodOn, hoodSpeed)

            // 6. Bep tu am kinh + noi inox (phan than)
            drawStoveAndPotBase(sx, sy)

            // 7. Tu lanh thong minh 2 canh (than + tay nam + khay nuoc)
            drawFridgeBody(sx, sy)

            // 8. Dao bep tien canh & 2 den tha
            drawIslandAndPendants(sx, sy, lightFactor, room.lightOn)

            // 9. Lop phu bong toi moi truong
            val hoodLightOffset = if (hoodOn) 0.08f * hoodAlpha else 0f
            val darkVal = ((1f - lightFactor) * 0.86f - hoodLightOffset).coerceIn(0f, 0.88f)
            if (darkVal > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF0B1120).copy(alpha = 0.25f * darkVal),
                            0.68f to Color(0xFF070B16).copy(alpha = 0.78f * darkVal),
                            1.0f to Color(0xFF03060F).copy(alpha = 0.94f * darkVal),
                        ),
                        center = Offset(0.50f * size.width, 0.44f * size.height),
                        radius = maxOf(size.width, size.height) * 0.75f,
                    ),
                    size = size,
                )
            }

            // 10. Lop phat sang noi bat tren bong toi (vong nhiet bep tu, hoi nuoc, luong hut, man hinh tu lanh, LED cam bien)
            drawStoveActiveEffects(
                auxPath = auxPath,
                ledPaint = ledPaint,
                sx = sx,
                sy = sy,
                stoveOn = room.stoveOn,
                stoveLevel = stoveLevel,
                stoveRatio = stoveRatio,
                steamPhase = steamPhase,
            )
            drawHoodSuctionFlow(
                auxPath = auxPath,
                sx = sx,
                sy = sy,
                hoodAlpha = hoodAlpha,
                hoodSpeed = hoodSpeed,
                hoodPhase = hoodPhase,
            )
            drawFridgeSmartDisplay(fridgePaint, sx, sy)
            drawGasSensorLed(sx, sy, room.gasAlert, alertPulse)

            // Vien sang do canh bao ro ri gas ben trong khung
            if (room.gasAlert) {
                val edgeAlpha = 0.20f + 0.26f * alertPulse
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.55f to Color.Transparent,
                            1.0f to Color(0xFFEF4444).copy(alpha = edgeAlpha),
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.5f),
                        radius = maxOf(size.width, size.height) * 0.72f,
                    ),
                    size = size,
                )
            }
        }

        // Lop phu tren cung: Hang pill trang thai + Banner canh bao Gas (khong chong len nhau)
        if (!compact) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xD90F172A))
                                .border(1.dp, Color(0x80334155), RoundedCornerShape(20.dp))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = stateBadgeText,
                                style = SHType.Caption,
                                color = Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                        }

                        if (hoodOn) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xCC0C2440))
                                    .border(1.dp, Color(0x6638BDF8), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = "\uD83D\uDCA8 Hút mùi mức $hoodSpeed",
                                    style = SHType.Caption,
                                    color = Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }

                    // Chip trang thai Gas / Khoi
                    val gasBg = if (room.gasAlert) Color(0xE67F1D1D) else Color(0xD9062C22)
                    val gasBorder = if (room.gasAlert) SH.Red else SH.Green.copy(alpha = 0.5f)
                    val gasFg = if (room.gasAlert) Color(0xFFFCA5A5) else Color(0xFF6EE7B7)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(gasBg)
                            .border(1.dp, gasBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (room.gasAlert) SH.Red else SH.Green),
                        )
                        Text(
                            text = if (room.gasAlert) "CẢNH BÁO: ${room.gasPpm} ppm" else "Gas/Khói: ${room.gasPpm} ppm",
                            style = SHType.Caption,
                            color = gasFg,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = room.gasAlert,
                    enter = fadeIn(tween(250)),
                    exit = fadeOut(tween(250)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xF2DC2626))
                            .border(1.dp, Color(0x99FCA5A5), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Cảnh báo rò rỉ khí Gas / Khói cao!",
                                style = SHType.Label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Đã tự động kích hoạt Máy hút mùi mức 3 (Mạnh) để thông gió.",
                                style = SHType.Caption,
                                color = Color(0xFFFEE2E2),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable(onClick = onSilenceGasAlert)
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = "Tắt cảnh báo",
                                style = SHType.Caption,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
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

private fun DrawScope.drawKitchenShell(sx: Float, sy: Float) {
    // Tuong sau
    drawPath(
        poly(sx, sy, 105f to 132f, 695f to 132f, 695f to 392f, 105f to 392f),
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD6D1C9), Color(0xFFBEB7AC)),
            startY = 132f * sy,
            endY = 392f * sy,
        ),
    )
    // Tuong trai
    drawPath(
        poly(sx, sy, 0f to 0f, 105f to 132f, 105f to 392f, 0f to 515f),
        brush = Brush.linearGradient(
            listOf(Color(0xFFB0A89D), Color(0xFFCFC8BD)),
            start = Offset(0f, 0f),
            end = Offset(105f * sx, 110f * sy),
        ),
    )
    // Tuong phai
    drawPath(
        poly(sx, sy, 800f to 0f, 695f to 132f, 695f to 392f, 800f to 515f),
        brush = Brush.linearGradient(
            listOf(Color(0xFFB0A89D), Color(0xFFCFC8BD)),
            start = Offset(800f * sx, 0f),
            end = Offset(695f * sx, 110f * sy),
        ),
    )
    // Tran nha
    drawPath(
        poly(sx, sy, 0f to 0f, 800f to 0f, 695f to 132f, 105f to 132f),
        brush = Brush.verticalGradient(
            listOf(Color(0xFFE4DFD7), Color(0xFFC9C1B5)),
            startY = 0f,
            endY = 132f * sy,
        ),
    )
    // San gach gia da xam hien dai
    drawPath(
        poly(sx, sy, 0f to 515f, 105f to 392f, 695f to 392f, 800f to 515f, 800f to 550f, 0f to 550f),
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(0xFF4A4E57),
                0.55f to Color(0xFF373B44),
                1f to Color(0xFF252932),
            ),
            startY = 392f * sy,
            endY = 550f * sy,
        ),
    )
    // Duong ron gach lat san
    val groutColor = Color(0xFF1E222B).copy(alpha = 0.5f)
    val groutW = 1.2f * minOf(sx, sy)
    val seams = listOf(
        (205f to 392f) to (90f to 550f),
        (335f to 392f) to (290f to 550f),
        (465f to 392f) to (510f to 550f),
        (595f to 392f) to (710f to 550f),
        (65f to 445f) to (735f to 445f),
        (15f to 505f) to (785f to 505f),
    )
    seams.forEach { (a, b) ->
        drawLine(groutColor, Offset(a.first * sx, a.second * sy), Offset(b.first * sx, b.second * sy), groutW)
    }
}

private fun DrawScope.drawKitchenCeilingAndLights(
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    val trayPath = poly(sx, sy, 125f to 22f, 675f to 22f, 605f to 108f, 195f to 108f)
    drawPath(
        trayPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFD7CFC2), Color(0xFFF0E8DA)),
            startY = 22f * sy,
            endY = 108f * sy,
        ),
    )
    drawPath(trayPath, Color(0xFFBDB4A7), style = Stroke(width = 1.8f * minOf(sx, sy)))

    if (lightFactor <= 0.01f) return

    // Quang sang hat tran
    drawPath(
        trayPath,
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color(0xFFFDE68A).copy(alpha = 0.72f * lightFactor),
                0.65f to Color(0xFFF59E0B).copy(alpha = 0.22f * lightFactor),
                1f to Color.Transparent,
            ),
            center = Offset(400f * sx, 65f * sy),
            radius = 300f * sx,
        ),
    )
    val covePath = poly(sx, sy, 132f to 26f, 668f to 26f, 600f to 104f, 200f to 104f)
    drawPath(
        covePath,
        Color(0xFFFDE047).copy(alpha = 0.30f * lightFactor),
        style = Stroke(width = 10f * minOf(sx, sy)),
    )
    drawPath(
        covePath,
        Color(0xFFFDE047).copy(alpha = 0.88f * lightFactor),
        style = Stroke(width = 3.5f * minOf(sx, sy)),
    )

    // 4 den LED am tran (downlights)
    val spots = listOf(
        Triple(90f, 38f, 10f to 3.8f),
        Triple(710f, 38f, 10f to 3.8f),
        Triple(220f, 118f, 8f to 3f),
        Triple(580f, 118f, 8f to 3f),
    )
    spots.forEach { (cx, cy, rad) ->
        val rx = rad.first * sx
        val ry = rad.second * sy
        drawOval(
            color = Color(0xFFFEF08A).copy(alpha = 0.58f * lightFactor),
            topLeft = Offset(cx * sx - rx * 1.4f, cy * sy - ry * 1.4f),
            size = Size(rx * 2.8f, ry * 2.8f),
        )
        drawOval(
            color = Color.White.copy(alpha = 0.95f * lightFactor),
            topLeft = Offset(cx * sx - rx * 0.55f, cy * sy - ry * 0.55f),
            size = Size(rx * 1.1f, ry * 1.1f),
        )
    }

    // Quang sang 2 den tha dao bep
    listOf(290f, 510f).forEach { px ->
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFEF08A).copy(alpha = 0.92f * lightFactor),
                    0.45f to Color(0xFFFBBF24).copy(alpha = 0.32f * lightFactor),
                    1f to Color.Transparent,
                ),
                center = Offset(px * sx, 138f * sy),
                radius = 72f * sx,
            ),
            topLeft = Offset((px - 72f) * sx, (155f - 62f) * sy),
            size = Size(144f * sx, 124f * sy),
        )
    }
}

private fun DrawScope.drawGasSensorBody(sx: Float, sy: Float) {
    drawOval(
        color = Color(0xFFE2E8F0),
        topLeft = Offset((628f - 22f) * sx, (74f - 7.5f) * sy),
        size = Size(44f * sx, 15f * sy),
    )
    drawOval(
        color = Color(0xFF94A3B8),
        topLeft = Offset((628f - 22f) * sx, (74f - 7.5f) * sy),
        size = Size(44f * sx, 15f * sy),
        style = Stroke(width = 1.2f * minOf(sx, sy)),
    )
    drawOval(
        color = Color(0xFFCBD5E1),
        topLeft = Offset((628f - 16f) * sx, (77f - 5f) * sy),
        size = Size(32f * sx, 10f * sy),
    )
    drawOval(
        color = Color(0xFF475569).copy(alpha = 0.6f),
        topLeft = Offset((628f - 9f) * sx, (78f - 2.5f) * sy),
        size = Size(18f * sx, 5f * sy),
    )
}

private fun DrawScope.drawGasSensorLed(
    sx: Float,
    sy: Float,
    gasAlert: Boolean,
    alertPulse: Float,
) {
    val ledColor = if (gasAlert) Color(0xFFEF4444) else Color(0xFF22C55E)
    val haloAlpha = if (gasAlert) 0.45f + 0.45f * alertPulse else 0.25f
    val haloRadius = if (gasAlert) (10f + 5f * alertPulse) * minOf(sx, sy) else 9f * minOf(sx, sy)
    val dotRadius = if (gasAlert) (3.2f + 1.2f * alertPulse) * minOf(sx, sy) else 3.4f * minOf(sx, sy)
    val dotAlpha = if (gasAlert) 0.45f + 0.55f * alertPulse else 1f

    drawCircle(
        color = ledColor.copy(alpha = haloAlpha),
        radius = haloRadius,
        center = Offset(628f * sx, 80f * sy),
    )
    drawCircle(
        color = ledColor.copy(alpha = dotAlpha),
        radius = dotRadius,
        center = Offset(628f * sx, 80f * sy),
    )
}

private fun DrawScope.drawCabinetsAndCounter(
    veinPath: Path,
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    // Mang tuong op da Marble giua tu tren va tu duoi
    drawRect(
        brush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color(0xFFF8FAFC),
                0.5f to Color(0xFFE2E8F0),
                1f to Color(0xFFCBD5E1),
            ),
            start = Offset(115f * sx, 232f * sy),
            end = Offset(535f * sx, 324f * sy),
        ),
        topLeft = Offset(115f * sx, 232f * sy),
        size = Size(420f * sx, 92f * sy),
    )

    // Duong van da Marble tinh te
    veinPath.reset()
    veinPath.moveTo(135f * sx, 235f * sy)
    veinPath.quadraticTo(180f * sx, 275f * sy, 235f * sx, 295f * sy)
    veinPath.quadraticTo(298f * sx, 277f * sy, 360f * sx, 260f * sy)
    veinPath.quadraticTo(438f * sx, 285f * sy, 515f * sx, 310f * sy)
    drawPath(veinPath, Color(0xFF94A3B8).copy(alpha = 0.38f), style = Stroke(width = 1f * minOf(sx, sy)))

    veinPath.reset()
    veinPath.moveTo(180f * sx, 320f * sy)
    veinPath.quadraticTo(260f * sx, 285f * sy, 330f * sx, 242f * sy)
    drawPath(veinPath, Color(0xFF94A3B8).copy(alpha = 0.30f), style = Stroke(width = 0.9f * minOf(sx, sy)))

    val walnutCabBrush = Brush.verticalGradient(
        listOf(Color(0xFF6F4B32), Color(0xFF442C1B)),
        startY = 138f * sy,
        endY = 232f * sy,
    )
    val cabStrokeW = 1.5f * minOf(sx, sy)

    // Tu bep tren ben trai
    drawRect(walnutCabBrush, Offset(115f * sx, 138f * sy), Size(152f * sx, 94f * sy))
    drawRect(Color(0xFF2E1C11), Offset(115f * sx, 138f * sy), Size(152f * sx, 94f * sy), style = Stroke(cabStrokeW))
    drawLine(Color(0xFF2E1C11), Offset(191f * sx, 138f * sy), Offset(191f * sx, 232f * sy), cabStrokeW)

    // Tu bep tren ben phai
    drawRect(walnutCabBrush, Offset(425f * sx, 138f * sy), Size(110f * sx, 94f * sy))
    drawRect(Color(0xFF2E1C11), Offset(425f * sx, 138f * sy), Size(110f * sx, 94f * sy), style = Stroke(cabStrokeW))
    drawLine(Color(0xFF2E1C11), Offset(480f * sx, 138f * sy), Offset(480f * sx, 232f * sy), cabStrokeW)

    // Dai den LED hat gam tu bep (dong bo voi Den bep)
    if (lightFactor > 0.01f) {
        val beamBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color(0xFFFEF08A).copy(alpha = 0.88f * lightFactor),
                0.4f to Color(0xFFFDE047).copy(alpha = 0.28f * lightFactor),
                1f to Color.Transparent,
            ),
            startY = 236f * sy,
            endY = 290f * sy,
        )
        drawRect(Color(0xFFFEF08A).copy(alpha = lightFactor), Offset(118f * sx, 232f * sy), Size(146f * sx, 4f * sy))
        drawPath(poly(sx, sy, 118f to 236f, 264f to 236f, 270f to 290f, 115f to 290f), beamBrush)

        drawRect(Color(0xFFFEF08A).copy(alpha = lightFactor), Offset(428f * sx, 232f * sy), Size(104f * sx, 4f * sy))
        drawPath(poly(sx, sy, 428f to 236f, 532f to 236f, 535f to 290f, 422f to 290f), beamBrush)
    }

    // Mat ban bep da Marble
    val counterTop = poly(sx, sy, 105f to 324f, 542f to 324f, 552f to 340f, 94f to 340f)
    drawPath(
        counterTop,
        Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFFF8FAFC), 0.5f to Color(0xFFE2E8F0), 1f to Color(0xFFCBD5E1)),
            start = Offset(94f * sx, 324f * sy),
            end = Offset(552f * sx, 340f * sy),
        ),
    )
    drawPath(counterTop, Color(0xFF94A3B8), style = Stroke(width = 1f * minOf(sx, sy)))

    // He tu bep duoi go oc cho
    val lowerBrush = Brush.linearGradient(
        listOf(Color(0xFF5B3C27), Color(0xFF342013)),
        start = Offset(105f * sx, 340f * sy),
        end = Offset(542f * sx, 408f * sy),
    )
    drawRect(lowerBrush, Offset(105f * sx, 340f * sy), Size(437f * sx, 68f * sy))
    drawRect(Color(0xFF24150C), Offset(105f * sx, 340f * sy), Size(437f * sx, 68f * sy), style = Stroke(cabStrokeW))
    listOf(192f, 278f, 414f).forEach { dx ->
        drawLine(Color(0xFF24150C), Offset(dx * sx, 340f * sy), Offset(dx * sx, 408f * sy), cabStrokeW)
    }
    // Tay nam tu am mau den
    listOf(140f to 24f, 224f to 24f, 334f to 26f, 464f to 26f).forEach { (hx, hw) ->
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(hx * sx, 348f * sy),
            size = Size(hw * sx, 3f * sy),
            cornerRadius = CornerRadius(1.5f * sx, 1.5f * sy),
        )
    }

    // Bon rua chen & voi nuoc inox ben trai
    drawOval(Color(0xFF94A3B8), Offset((185f - 34f) * sx, (331f - 5f) * sy), Size(68f * sx, 10f * sy))
    drawOval(
        Color(0xFF64748B),
        Offset((185f - 34f) * sx, (331f - 5f) * sy),
        Size(68f * sx, 10f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    val faucetPath = Path().apply {
        moveTo(185f * sx, 326f * sy)
        lineTo(185f * sx, 302f * sy)
        quadraticTo(185f * sx, 290f * sy, 198f * sx, 292f * sy)
        quadraticTo(206f * sx, 294f * sy, 206f * sx, 304f * sy)
    }
    drawPath(
        faucetPath,
        Color(0xFFCBD5E1),
        style = Stroke(width = 3.2f * minOf(sx, sy), cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawRangeHoodBase(
    sx: Float,
    sy: Float,
    hoodAlpha: Float,
    hoodOn: Boolean,
    hoodSpeed: Int,
) {
    val hoodBrush = Brush.horizontalGradient(
        colorStops = arrayOf(0f to Color(0xFF1E293B), 0.5f to Color(0xFF334155), 1f to Color(0xFF0F172A)),
        startX = 266f * sx,
        endX = 426f * sx,
    )
    // Ong khoi dung
    drawRect(hoodBrush, Offset(321f * sx, 132f * sy), Size(50f * sx, 86f * sy))
    drawRect(Color(0xFF0F172A), Offset(321f * sx, 132f * sy), Size(50f * sx, 86f * sy), style = Stroke(1f * minOf(sx, sy)))

    // Than may chu T ngang
    val hoodBody = poly(sx, sy, 274f to 216f, 418f to 216f, 426f to 232f, 266f to 232f)
    drawPath(hoodBody, hoodBrush)
    drawPath(hoodBody, Color(0xFF475569), style = Stroke(1f * minOf(sx, sy)))

    // Bang dieu khien cam ung & 3 den LED chi bao cap do
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(322f * sx, 222f * sy),
        size = Size(48f * sx, 6f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )
    val ledXs = listOf(332f, 346f, 360f)
    ledXs.forEachIndexed { idx, lx ->
        val lit = hoodOn && hoodSpeed >= idx + 1
        drawCircle(
            color = if (lit) Color(0xFF38BDF8) else Color(0xFF475569),
            radius = 1.6f * minOf(sx, sy),
            center = Offset(lx * sx, 225f * sy),
        )
    }

    // Den chieu sang nho duoi may hut mui
    if (hoodAlpha > 0.01f) {
        drawRoundRect(
            color = Color(0xFFE0F2FE).copy(alpha = hoodAlpha),
            topLeft = Offset(292f * sx, 232f * sy),
            size = Size(18f * sx, 3f * sy),
            cornerRadius = CornerRadius(1f * sx, 1f * sy),
        )
        drawRoundRect(
            color = Color(0xFFE0F2FE).copy(alpha = hoodAlpha),
            topLeft = Offset(382f * sx, 232f * sy),
            size = Size(18f * sx, 3f * sy),
            cornerRadius = CornerRadius(1f * sx, 1f * sy),
        )
        drawPath(
            poly(sx, sy, 282f to 235f, 410f to 235f, 442f to 334f, 250f to 334f),
            Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFE0F2FE).copy(alpha = 0.85f * hoodAlpha),
                    0.55f to Color(0xFFBAE6FD).copy(alpha = 0.28f * hoodAlpha),
                    1f to Color.Transparent,
                ),
                startY = 235f * sy,
                endY = 334f * sy,
            ),
        )
    }
}

private fun DrawScope.drawHoodSuctionFlow(
    auxPath: Path,
    sx: Float,
    sy: Float,
    hoodAlpha: Float,
    hoodSpeed: Int,
    hoodPhase: Float,
) {
    if (hoodAlpha <= 0.01f) return
    val flowAlpha = (0.45f + hoodSpeed * 0.18f) * hoodAlpha
    val cycle = 160f * minOf(sx, sy)
    val dash = 42f * minOf(sx, sy)
    val gap = 118f * minOf(sx, sy)

    val curves = listOf(
        Triple(Offset(312f * sx, 316f * sy), Offset(304f * sx, 282f * sy), Offset(322f * sx, 237f * sy)),
        Triple(Offset(346f * sx, 318f * sy), Offset(346f * sx, 278f * sy), Offset(346f * sx, 236f * sy)),
        Triple(Offset(380f * sx, 316f * sy), Offset(388f * sx, 282f * sy), Offset(370f * sx, 237f * sy)),
    )
    curves.forEachIndexed { idx, (p0, ctrl, p1) ->
        val p = (hoodPhase + idx * 0.25f) % 1f
        val color = if (idx == 1) Color(0xFF38BDF8) else Color(0xFF7DD3FC)
        val width = (if (idx == 1) 2.8f else 2.4f) * minOf(sx, sy)
        auxPath.reset()
        auxPath.moveTo(p0.x, p0.y)
        auxPath.quadraticTo(ctrl.x, ctrl.y, p1.x, p1.y)
        drawPath(
            path = auxPath,
            color = color.copy(alpha = flowAlpha),
            style = Stroke(
                width = width,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), -p * cycle),
            ),
        )
    }
}

private fun DrawScope.drawStoveAndPotBase(sx: Float, sy: Float) {
    // Mat kinh bep tu am
    val glass = poly(sx, sy, 282f to 325f, 412f to 325f, 420f to 338f, 274f to 338f)
    drawPath(glass, Color(0xFF0F172A))
    drawPath(glass, Color(0xFF334155), style = Stroke(width = 1.2f * minOf(sx, sy)))

    // Quai noi 2 ben
    val handlePathL = Path().apply {
        moveTo(322f * sx, 308f * sy)
        lineTo(314f * sx, 308f * sy)
        quadraticTo(312f * sx, 312f * sy, 315f * sx, 315f * sy)
        lineTo(322f * sx, 315f * sy)
    }
    val handlePathR = Path().apply {
        moveTo(370f * sx, 308f * sy)
        lineTo(378f * sx, 308f * sy)
        quadraticTo(380f * sx, 312f * sy, 377f * sx, 315f * sy)
        lineTo(370f * sx, 315f * sy)
    }
    drawPath(handlePathL, Color(0xFF94A3B8), style = Stroke(width = 2.2f * minOf(sx, sy)))
    drawPath(handlePathR, Color(0xFF94A3B8), style = Stroke(width = 2.2f * minOf(sx, sy)))

    // Than noi inox
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colorStops = arrayOf(
                0f to Color(0xFF64748B),
                0.35f to Color(0xFFE2E8F0),
                0.70f to Color(0xFF94A3B8),
                1f to Color(0xFF475569),
            ),
            startX = 322f * sx,
            endX = 370f * sx,
        ),
        topLeft = Offset(322f * sx, 303f * sy),
        size = Size(48f * sx, 27f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
    )
    drawRoundRect(
        color = Color(0xFF475569),
        topLeft = Offset(322f * sx, 303f * sy),
        size = Size(48f * sx, 27f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    // Nap kinh noi & num cam
    drawOval(Color(0xFFCBD5E1), Offset((346f - 24f) * sx, (303f - 4.5f) * sy), Size(48f * sx, 9f * sy))
    drawOval(
        Color(0xFF64748B),
        Offset((346f - 24f) * sx, (303f - 4.5f) * sy),
        Size(48f * sx, 9f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(342f * sx, 296f * sy),
        size = Size(8f * sx, 5f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )
}

private fun DrawScope.drawStoveActiveEffects(
    auxPath: Path,
    ledPaint: Paint,
    sx: Float,
    sy: Float,
    stoveOn: Boolean,
    stoveLevel: Int,
    stoveRatio: Float,
    steamPhase: Float,
) {
    // Vong nhiet phat sang do cam
    if (stoveRatio > 0.01f) {
        drawOval(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color(0xFFFFF7ED).copy(alpha = 0.95f * stoveRatio),
                    0.30f to Color(0xFFFB923C).copy(alpha = 0.92f * stoveRatio),
                    0.62f to Color(0xFFEF4444).copy(alpha = 0.72f * stoveRatio),
                    1f to Color.Transparent,
                ),
                center = Offset(346f * sx, 331f * sy),
                radius = 40f * sx,
            ),
            topLeft = Offset((346f - 42f) * sx, (331f - 12f) * sy),
            size = Size(84f * sx, 24f * sy),
        )
        drawOval(
            color = Color(0xFFFB923C).copy(alpha = 0.95f * stoveRatio),
            topLeft = Offset((346f - 24f) * sx, (331f - 4.2f) * sy),
            size = Size(48f * sx, 8.4f * sy),
            style = Stroke(width = 2f * minOf(sx, sy)),
        )
    }

    // Man hinh LED nho tren mat kinh bep tu (luon doc duoc ke ca khi tat den)
    ledPaint.textSize = 9f * minOf(sx, sy)
    ledPaint.color = Color(0xFFF97316).copy(alpha = if (stoveOn) 1f else 0.35f).toArgb()
    drawContext.canvas.nativeCanvas.drawText(
        if (stoveOn) "P$stoveLevel" else "--",
        396f * sx,
        335f * sy,
        ledPaint,
    )

    // Hoi nuoc / khoi nau an boc len tu noi
    if (stoveOn && stoveRatio > 0.05f) {
        val baseAlpha = (0.35f + (stoveLevel / 9f) * 0.65f) * stoveRatio
        val plumes = listOf(
            Triple(336f, 0f, Color(0xFFF8FAFC) to 3.2f),
            Triple(348f, 0.27f, Color.White to 3.6f),
            Triple(358f, 0.52f, Color(0xFFE2E8F0) to 2.8f),
        )
        plumes.forEach { (baseX, offset, styleSpec) ->
            val t = (steamPhase + offset) % 1f
            val alpha = when {
                t < 0.22f -> (t / 0.22f) * 0.75f
                t < 0.75f -> 0.75f - ((t - 0.22f) / 0.53f) * 0.33f
                else -> (1f - t) / 0.25f * 0.42f
            } * baseAlpha
            if (alpha > 0.01f) {
                val dy = 12f - t * 88f // 12 -> -76
                val sway = sin(t * 2f * PI.toFloat()) * 4f
                val y0 = (296f + dy) * sy
                val yMid = (277f + dy) * sy
                val yEnd = (242f + dy) * sy
                auxPath.reset()
                auxPath.moveTo(baseX * sx, y0)
                auxPath.quadraticTo((baseX - 8f + sway) * sx, yMid, (baseX + 2f - sway) * sx, yEnd)
                val w = styleSpec.second * minOf(sx, sy)
                drawPath(
                    auxPath,
                    styleSpec.first.copy(alpha = alpha * 0.35f),
                    style = Stroke(width = w * 2.2f, cap = StrokeCap.Round),
                )
                drawPath(
                    auxPath,
                    styleSpec.first.copy(alpha = alpha * 0.72f),
                    style = Stroke(width = w, cap = StrokeCap.Round),
                )
            }
        }
    }
}

private fun DrawScope.drawFridgeBody(sx: Float, sy: Float) {
    val fridgeBrush = Brush.linearGradient(
        colorStops = arrayOf(0f to Color(0xFF475569), 0.5f to Color(0xFF334155), 1f to Color(0xFF1E293B)),
        start = Offset(556f * sx, 150f * sy),
        end = Offset(680f * sx, 200f * sy),
    )
    drawRoundRect(
        brush = fridgeBrush,
        topLeft = Offset(556f * sx, 150f * sy),
        size = Size(124f * sx, 258f * sy),
        cornerRadius = CornerRadius(6f * sx, 6f * sy),
    )
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(556f * sx, 150f * sy),
        size = Size(124f * sx, 258f * sy),
        cornerRadius = CornerRadius(6f * sx, 6f * sy),
        style = Stroke(width = 2f * minOf(sx, sy)),
    )
    // Khe giua 2 canh
    drawLine(Color(0xFF0F172A), Offset(618f * sx, 150f * sy), Offset(618f * sx, 408f * sy), 2.2f * minOf(sx, sy))
    // Tay nam doc
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(610f * sx, 220f * sy),
        size = Size(3f * sx, 64f * sy),
        cornerRadius = CornerRadius(1.5f * sx, 1.5f * sy),
    )
    drawRoundRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(623f * sx, 220f * sy),
        size = Size(3f * sx, 64f * sy),
        cornerRadius = CornerRadius(1.5f * sx, 1.5f * sy),
    )
    // Khay lay nuoc mat ben canh trai
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(572f * sx, 218f * sy),
        size = Size(28f * sx, 44f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
    )
    drawRoundRect(
        color = Color(0xFF334155),
        topLeft = Offset(572f * sx, 218f * sy),
        size = Size(28f * sx, 44f * sy),
        cornerRadius = CornerRadius(3f * sx, 3f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
}

private fun DrawScope.drawFridgeSmartDisplay(
    fridgePaint: Paint,
    sx: Float,
    sy: Float,
) {
    // Man hinh Smart Display tren canh tu lanh (luon ro ke ca khi tat den)
    drawRoundRect(
        color = Color(0xFF0B1120).copy(alpha = 0.95f),
        topLeft = Offset(632f * sx, 198f * sy),
        size = Size(36f * sx, 54f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
    )
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(632f * sx, 198f * sy),
        size = Size(36f * sx, 54f * sy),
        cornerRadius = CornerRadius(4f * sx, 4f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )

    val canvas = drawContext.canvas.nativeCanvas
    fridgePaint.textSize = 11f * minOf(sx, sy)
    fridgePaint.color = Color(0xFF38BDF8).toArgb()
    canvas.drawText("4°C", 650f * sx, 217f * sy, fridgePaint)

    drawLine(Color(0xFF1E293B), Offset(638f * sx, 225f * sy), Offset(662f * sx, 225f * sy), 1f * minOf(sx, sy))

    fridgePaint.textSize = 8.5f * minOf(sx, sy)
    fridgePaint.color = Color(0xFF94A3B8).toArgb()
    canvas.drawText("-18°C", 650f * sx, 239f * sy, fridgePaint)

    drawCircle(Color(0xFF22C55E), radius = 1.8f * minOf(sx, sy), center = Offset(650f * sx, 247f * sy))
}

private fun DrawScope.drawIslandAndPendants(
    sx: Float,
    sy: Float,
    lightFactor: Float,
    lightOn: Boolean,
) {
    val bulbColor = lerp(Color(0xFF475569), Color(0xFFFEF08A), if (lightOn) 0.4f + 0.6f * lightFactor else 0f)
    listOf(290f, 510f).forEach { px ->
        drawLine(Color(0xFF334155), Offset(px * sx, 54f * sy), Offset(px * sx, 128f * sy), 1.6f * minOf(sx, sy))
        val shade = Path().apply {
            moveTo((px - 16f) * sx, 128f * sy)
            quadraticTo(px * sx, 118f * sy, (px + 16f) * sx, 128f * sy)
            lineTo((px + 22f) * sx, 144f * sy)
            lineTo((px - 22f) * sx, 144f * sy)
            close()
        }
        drawPath(shade, Color(0xFF1E293B))
        drawPath(shade, Color(0xFF475569), style = Stroke(width = 1f * minOf(sx, sy)))
        drawOval(bulbColor, Offset((px - 18f) * sx, (144f - 4f) * sy), Size(36f * sx, 8f * sy))
    }

    // Khoi dao bep tien canh
    val islandTop = poly(sx, sy, 150f to 432f, 610f to 432f, 638f to 460f, 122f to 460f)
    drawPath(
        islandTop,
        Brush.linearGradient(
            colorStops = arrayOf(0f to Color(0xFFF8FAFC), 0.5f to Color(0xFFE2E8F0), 1f to Color(0xFFCBD5E1)),
            start = Offset(122f * sx, 432f * sy),
            end = Offset(638f * sx, 460f * sy),
        ),
    )
    drawPath(islandTop, Color(0xFF94A3B8), style = Stroke(width = 1.2f * minOf(sx, sy)))

    drawRect(
        brush = Brush.linearGradient(
            listOf(Color(0xFF5B3C27), Color(0xFF342013)),
            start = Offset(135f * sx, 460f * sy),
            end = Offset(625f * sx, 550f * sy),
        ),
        topLeft = Offset(135f * sx, 460f * sy),
        size = Size(490f * sx, 90f * sy),
    )
    drawRect(
        color = Color(0xFF24150C),
        topLeft = Offset(135f * sx, 460f * sy),
        size = Size(490f * sx, 90f * sy),
        style = Stroke(width = 1.5f * minOf(sx, sy)),
    )

    // Duong soc nan go trang tri mat dao bep
    val slatColor = Color(0xFF29170C).copy(alpha = 0.65f)
    val slatW = 2f * minOf(sx, sy)
    var x = 175f
    while (x <= 575f) {
        drawLine(slatColor, Offset(x * sx, 460f * sy), Offset(x * sx, 550f * sy), slatW)
        x += 40f
    }

    // Dia hoa qua trang tri tren dao bep
    drawOval(Color(0xFFCBD5E1), Offset((465f - 28f) * sx, (446f - 8f) * sy), Size(56f * sx, 16f * sy))
    drawOval(
        Color(0xFF94A3B8),
        Offset((465f - 28f) * sx, (446f - 8f) * sy),
        Size(56f * sx, 16f * sy),
        style = Stroke(width = 1f * minOf(sx, sy)),
    )
    drawCircle(Color(0xFF22C55E), radius = 6f * minOf(sx, sy), center = Offset(456f * sx, 442f * sy))
    drawCircle(Color(0xFFF97316), radius = 6.5f * minOf(sx, sy), center = Offset(468f * sx, 440f * sy))
    drawCircle(Color(0xFFEAB308), radius = 5.5f * minOf(sx, sy), center = Offset(476f * sx, 444f * sy))
}
