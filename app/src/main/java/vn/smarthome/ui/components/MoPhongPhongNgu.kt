package vn.smarthome.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import vn.smarthome.data.RoomMode
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

private const val REF_W = 800f
private const val REF_H = 600f

// Toa do da giac theo he quy chieu 800x600 tu file thiet ke HTML/SVG
private val BACK_WALL = floatArrayOf(180f, 185f, 620f, 185f, 620f, 425f, 180f, 425f)
private val LEFT_WALL = floatArrayOf(0f, 0f, 180f, 185f, 180f, 425f, 0f, 565f)
private val RIGHT_WALL = floatArrayOf(800f, 0f, 620f, 185f, 620f, 425f, 800f, 565f)
private val CEILING_POLY = floatArrayOf(0f, 0f, 800f, 0f, 620f, 185f, 180f, 185f)
private val FLOOR_POLY = floatArrayOf(0f, 565f, 180f, 425f, 620f, 425f, 800f, 565f, 800f, 600f, 0f, 600f)

private val RECESS_POLY = floatArrayOf(155f, 34f, 645f, 34f, 555f, 148f, 245f, 148f)
private val RECESS_LED_POLY = floatArrayOf(162f, 38f, 638f, 38f, 552f, 145f, 248f, 145f)

private val CURTAIN_LEFT = floatArrayOf(418f, 185f, 466f, 185f, 462f, 424f, 415f, 424f)
private val CURTAIN_LEFT_FOLD = floatArrayOf(428f, 185f, 438f, 185f, 436f, 424f, 426f, 424f)
private val CURTAIN_RIGHT = floatArrayOf(572f, 185f, 620f, 185f, 622f, 424f, 576f, 424f)
private val CURTAIN_RIGHT_FOLD = floatArrayOf(590f, 185f, 600f, 185f, 602f, 424f, 592f, 424f)

private val WARDROBE_BODY = floatArrayOf(318f, 186f, 415f, 186f, 415f, 424f, 318f, 424f)
private val VANITY_DESK = floatArrayOf(350f, 364f, 432f, 364f, 432f, 376f, 350f, 376f)
private val CHAIR_SEAT = floatArrayOf(418f, 388f, 450f, 388f, 446f, 424f, 422f, 424f)
private val CHAIR_BACK = floatArrayOf(444f, 358f, 454f, 358f, 450f, 392f, 440f, 392f)

private val DOOR_OUTER = floatArrayOf(0f, 122f, 44f, 160f, 44f, 532f, 0f, 566f)
private val DOOR_INNER = floatArrayOf(6f, 144f, 36f, 170f, 36f, 515f, 6f, 542f)

private val RUG_OUTER = floatArrayOf(105f, 535f, 280f, 432f, 595f, 432f, 695f, 555f, 210f, 595f)
private val RUG_INNER = floatArrayOf(116f, 533f, 284f, 438f, 588f, 438f, 682f, 550f, 214f, 586f)

private val HEADBOARD_HALO = floatArrayOf(62f, 340f, 324f, 300f, 324f, 358f, 62f, 415f)
private val HEADBOARD_GLOW = floatArrayOf(66f, 356f, 320f, 315f, 320f, 356f, 66f, 412f)
private val HEADBOARD_WOOD = floatArrayOf(64f, 398f, 322f, 340f, 322f, 438f, 64f, 515f)

private val NIGHTSTAND_FAR = floatArrayOf(274f, 396f, 324f, 385f, 324f, 435f, 274f, 448f)
private val LAMP_SHADE_FAR = floatArrayOf(288f, 358f, 308f, 358f, 312f, 379f, 284f, 379f)

private val DUVET_POLY = floatArrayOf(145f, 456f, 322f, 394f, 562f, 424f, 398f, 538f)
private val BED_SIDE_LEFT = floatArrayOf(145f, 456f, 398f, 538f, 398f, 566f, 145f, 482f)
private val BED_SIDE_RIGHT = floatArrayOf(398f, 538f, 562f, 424f, 562f, 456f, 398f, 566f)
private val THROW_NAVY = floatArrayOf(278f, 498f, 444f, 410f, 554f, 424f, 398f, 536f)

private val NIGHTSTAND_NEAR_TOP = floatArrayOf(74f, 458f, 142f, 438f, 184f, 454f, 114f, 478f)
private val NIGHTSTAND_NEAR_LEFT = floatArrayOf(74f, 458f, 114f, 478f, 114f, 548f, 74f, 522f)
private val NIGHTSTAND_NEAR_FRONT = floatArrayOf(114f, 478f, 184f, 454f, 184f, 522f, 114f, 548f)
private val LAMP_SHADE_NEAR = floatArrayOf(104f, 398f, 140f, 398f, 146f, 434f, 98f, 434f)

private val AC_BODY = floatArrayOf(682f, 115f, 790f, 62f, 790f, 148f, 682f, 184f)
private val AC_BOTTOM = floatArrayOf(682f, 184f, 790f, 148f, 776f, 162f, 674f, 192f)
private val AC_VENT = floatArrayOf(686f, 184f, 780f, 152f, 775f, 160f, 682f, 189f)
private val AC_LED_BEZEL = floatArrayOf(708f, 149f, 752f, 130f, 752f, 152f, 708f, 170f)

/**
 * Khung mo phong phong ngu Vector 2.5D cho 3 phong ngu (bed1, bed2, bed3).
 *
 * Tai hien day du hieu ung tu file design:
 *  - Chuyen sang/toi muot 600ms cho den hat tran, den dau giuong, den tu, 2 den ngu
 *  - Bau troi cua so tu chuyen Ngay / Dem (trang + sao) khi tat den hoac che do Ngu
 *  - Quat tran 5 canh 3D quay theo vat ly quan tinh (tang toc khi bat, giam toc tu tu khi tat)
 *  - Dieu hoa: man hinh LED luon sang ro ke ca khi tat den, canh dao gio mo/dong va
 *    3 luong gio lanh doi mau theo nhiet do (16..30 do C)
 */
@Composable
fun MoPhongPhongNgu(
    lightOn: Boolean,
    fanOn: Boolean,
    acOn: Boolean,
    acTemp: Int,
    mode: RoomMode,
    modifier: Modifier = Modifier,
    lightBrightness: Int = 100,
    fanSpeed: Int = 1,
    compact: Boolean = false,
) {
    val effectiveBrightness = if (mode == RoomMode.SLEEP) {
        minOf(lightBrightness.coerceIn(10, 100), 20)
    } else {
        lightBrightness.coerceIn(10, 100)
    }
    val targetLightFactor = if (lightOn) effectiveBrightness / 100f else 0f
    val lightFactor by animateFloatAsState(
        targetValue = targetLightFactor,
        animationSpec = tween(durationMillis = 600),
        label = "bedroomLight",
    )

    val isNightSky = mode == RoomMode.SLEEP || !lightOn
    val nightFactor by animateFloatAsState(
        targetValue = if (isNightSky) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "bedroomSky",
    )

    val acFactor by animateFloatAsState(
        targetValue = if (acOn) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "bedroomAc",
    )

    // Vat ly quan tinh cho quat tran: dung delay(16L) de khong treo Robolectric waitForIdle().
    val targetVel = if (fanOn) {
        when (fanSpeed.coerceIn(1, 3)) {
            1 -> 4.5f
            2 -> 8.5f
            else -> 14.0f
        }
    } else {
        0f
    }
    var fanAngle by remember { mutableFloatStateOf(0f) }
    var fanVelocity by remember { mutableFloatStateOf(targetVel) }
    LaunchedEffect(fanOn, fanSpeed) {
        var lastNs = System.nanoTime()
        while (isActive && (fanOn || fanVelocity > 0.05f)) {
            delay(16L)
            val nowNs = System.nanoTime()
            val dt = ((nowNs - lastNs) / 16_670_000f).coerceIn(0.2f, 3f)
            lastNs = nowNs
            val accel = if (fanOn) 0.045f else 0.022f
            fanVelocity += (targetVel - fanVelocity) * accel * dt
            if (!fanOn && fanVelocity <= 0.05f) {
                fanVelocity = 0f
            } else {
                fanAngle = (fanAngle + fanVelocity * dt) % 360f
            }
        }
    }

    // Chuyen dong song gio mat tu dieu hoa
    var airPhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(acOn) {
        while (isActive && acOn) {
            delay(16L)
            airPhase = (airPhase + 0.014f) % 1f
        }
    }

    val path = remember { Path() }
    val bladePath = remember { Path() }
    val airPath = remember { Path() }
    val ledPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
    }

    val shape = RoundedCornerShape(Dim.CardRadius)
    val boxMod = if (compact) {
        modifier.background(Color(0xFF0D1322))
    } else {
        modifier
            .clip(shape)
            .background(Color(0xFF0D1322))
            .border(Dim.BorderWidth, SH.Border, shape)
    }
    Box(modifier = boxMod) {
        Canvas(Modifier.fillMaxSize()) {
            val sx = size.width / REF_W
            val sy = size.height / REF_H

            // 1. KHUNG PHONG PHOI CANH 1 DIEM TU
            drawRoomShell(path, sx, sy)

            // 2. TRAN GIAT CAP & DEN LED HAT TRAN
            drawCeilingRecessAndLights(path, sx, sy, lightFactor)

            // 3. CUA SO LON & REM 2 LOP (NGAY / DEM)
            drawWindowAndCurtains(path, sx, sy, nightFactor)

            // 4. TU QUAN AO GO & BAN TRANG DIEM
            drawWardrobeAndVanity(path, sx, sy, lightFactor)

            // 5. CUA RA VAO TUONG TRAI
            drawLeftDoor(path, sx, sy)

            // 6. THAM TRAI SAN XAM
            drawFloorRug(path, sx, sy)

            // 7. GIUONG NGU, VACH DAU GIUONG & 2 DEN NGU
            drawBedAndLamps(path, sx, sy, lightOn, lightFactor)

            // 8. CAMERA AN NINH & MAY LANH TREO TUONG PHAI
            drawSecurityCamera(sx, sy)
            drawAcUnit(path, sx, sy, acFactor)

            // 9. LOP PHU BONG TOI MOI TRUONG
            val rawDarkness = if (isNightSky) {
                (1f - lightFactor) * 0.85f + 0.12f
            } else {
                (1f - lightFactor) * 0.75f
            }
            val darknessOpacity = rawDarkness.coerceIn(0f, 0.88f)
            if (darknessOpacity > 0.01f) {
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color(15, 23, 42, (0.45f * darknessOpacity * 255).toInt()),
                        0.72f to Color(6, 10, 22, (0.84f * darknessOpacity * 255).toInt()),
                        1.0f to Color(3, 6, 15, (0.92f * darknessOpacity * 255).toInt()),
                        center = Offset(size.width * 0.68f, size.height * 0.42f),
                        radius = maxOf(size.width, size.height) * 0.85f,
                    ),
                )
            }

            // 10. QUAT TRAN 5 CANH 3D, LUONG GIO DIEU HOA & MAN HINH LED (TREN LOP TOI)
            drawCeilingFan(bladePath, sx, sy, fanAngle, fanVelocity)
            if (acFactor > 0.01f) {
                drawAcAirflow(airPath, sx, sy, acTemp, acFactor, airPhase)
            }
            drawAcLedText(ledPaint, sx, sy, acOn, acTemp)
        }

        // Badge trang thai Ngay / Dem gon gang o goc tren phai (phia tren cuc lanh)
        if (!compact) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xB30F172A))
                    .border(1.dp, Color(0x3394A3B8), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = if (isNightSky) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                    contentDescription = null,
                    tint = if (isNightSky) Color(0xFFFEF08A) else SH.Amber,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (isNightSky) "Ban đêm" else "Ban ngày",
                    style = SHType.Caption,
                    color = SH.Text,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun Path.setPoly(sx: Float, sy: Float, pts: FloatArray) {
    reset()
    moveTo(pts[0] * sx, pts[1] * sy)
    var i = 2
    while (i < pts.size) {
        lineTo(pts[i] * sx, pts[i + 1] * sy)
        i += 2
    }
    close()
}

private fun DrawScope.drawRoomShell(path: Path, sx: Float, sy: Float) {
    // Tuong sau
    path.setPoly(sx, sy, BACK_WALL)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFFD4CBC0), Color(0xFFBEB4A7)),
            startY = 185f * sy,
            endY = 425f * sy,
        ),
    )

    // Tuong trai
    path.setPoly(sx, sy, LEFT_WALL)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFFC8BFB2), Color(0xFFDDD5C8)),
            start = Offset(0f, 0f),
            end = Offset(180f * sx, 226f * sy),
        ),
    )

    // Tuong phai
    path.setPoly(sx, sy, RIGHT_WALL)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFFB9B0A4), Color(0xFFD7CFC2)),
            start = Offset(800f * sx, 0f),
            end = Offset(620f * sx, 170f * sy),
        ),
    )

    // Tran nha
    path.setPoly(sx, sy, CEILING_POLY)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFFE7E0D5), Color(0xFFCFC6B8)),
            startY = 0f,
            endY = 185f * sy,
        ),
    )

    // San go nau tram
    path.setPoly(sx, sy, FLOOR_POLY)
    drawPath(
        path,
        Brush.verticalGradient(
            0.0f to Color(0xFF4A3323),
            0.55f to Color(0xFF3B271A),
            1.0f to Color(0xFF291A11),
            startY = 425f * sy,
            endY = 600f * sy,
        ),
    )

    // Duong ron van san go phoi canh
    val plankColor = Color(0xFF22150D).copy(alpha = 0.55f)
    val plankW = 1.2f * sx
    val plankLines = arrayOf(
        230f to 90f,
        295f to 220f,
        365f to 350f,
        435f to 480f,
        505f to 610f,
        570f to 740f,
    )
    plankLines.forEach { (xTop, xBot) ->
        drawLine(plankColor, Offset(xTop * sx, 425f * sy), Offset(xBot * sx, 600f * sy), plankW)
    }
    val hPlankColor = Color(0xFF22150D).copy(alpha = 0.22f)
    drawLine(hPlankColor, Offset(120f * sx, 472f * sy), Offset(680f * sx, 472f * sy), plankW)
    drawLine(hPlankColor, Offset(55f * sx, 525f * sy), Offset(745f * sx, 525f * sy), plankW)
}

private fun DrawScope.drawCeilingRecessAndLights(
    path: Path,
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    path.setPoly(sx, sy, RECESS_POLY)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFFD9CEBE), Color(0xFFF1E7D6)),
            startY = 34f * sy,
            endY = 148f * sy,
        ),
    )
    drawPath(path, Color(0xFFBDB2A3), style = Stroke(width = 2f * sx))

    if (lightFactor <= 0.005f) return

    // Quang sang LED hat tran (elip khop ti le tran giat cap nhu SVG objectBoundingBox)
    path.setPoly(sx, sy, RECESS_POLY)
    clipPath(path) {
        withTransform({
            translate(left = 400f * sx, top = 91f * sy)
            scale(scaleX = 1f, scaleY = 0.28f, pivot = Offset.Zero)
        }) {
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFFFDE68A).copy(alpha = 0.75f * lightFactor),
                    0.60f to Color(0xFFF59E0B).copy(alpha = 0.25f * lightFactor),
                    1.0f to Color.Transparent,
                    center = Offset.Zero,
                    radius = 270f * sx,
                ),
                radius = 270f * sx,
                center = Offset.Zero,
            )
        }
    }

    // Vien LED hat tran phat sang (ve nhieu lop mem tranh phu thuoc BlurMaskFilter)
    path.setPoly(sx, sy, RECESS_LED_POLY)
    drawPath(path, Color(0xFFFDE047).copy(alpha = 0.24f * lightFactor), style = Stroke(width = 12f * sx))
    drawPath(path, Color(0xFFFDE047).copy(alpha = 0.48f * lightFactor), style = Stroke(width = 6.5f * sx))
    drawPath(path, Color(0xFFFEF08A).copy(alpha = 0.90f * lightFactor), style = Stroke(width = 3f * sx))

    // Cac bong den am tran (Downlights)
    drawDownlight(115f, 48f, 10f, 4f, 6f, 2.4f, sx, sy, lightFactor)
    drawDownlight(685f, 48f, 10f, 4f, 6f, 2.4f, sx, sy, lightFactor)
    drawDownlight(185f, 128f, 8f, 3.2f, 4.8f, 1.8f, sx, sy, lightFactor)
    drawDownlight(615f, 128f, 8f, 3.2f, 4.8f, 1.8f, sx, sy, lightFactor)
    drawOval(
        color = Color(0xFFFEF08A).copy(alpha = lightFactor),
        topLeft = Offset((320f - 6f) * sx, (166f - 2.2f) * sy),
        size = Size(12f * sx, 4.4f * sy),
    )
    drawOval(
        color = Color(0xFFFEF08A).copy(alpha = lightFactor),
        topLeft = Offset((480f - 6f) * sx, (166f - 2.2f) * sy),
        size = Size(12f * sx, 4.4f * sy),
    )
}

private fun DrawScope.drawDownlight(
    cx: Float,
    cy: Float,
    haloRx: Float,
    haloRy: Float,
    coreRx: Float,
    coreRy: Float,
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    withTransform({
        translate(left = cx * sx, top = cy * sy)
        scale(scaleX = 1f, scaleY = (haloRy / haloRx).coerceAtLeast(0.35f), pivot = Offset.Zero)
    }) {
        val r = haloRx * 2.2f * sx
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFEF9C3).copy(alpha = 0.85f * lightFactor),
                0.5f to Color(0xFFFEF08A).copy(alpha = 0.32f * lightFactor),
                1.0f to Color.Transparent,
                center = Offset.Zero,
                radius = r,
            ),
            radius = r,
            center = Offset.Zero,
        )
    }
    drawOval(
        color = Color.White.copy(alpha = lightFactor),
        topLeft = Offset((cx - coreRx) * sx, (cy - coreRy) * sy),
        size = Size(coreRx * 2f * sx, coreRy * 2f * sy),
    )
}

private fun DrawScope.drawWindowAndCurtains(
    path: Path,
    sx: Float,
    sy: Float,
    nightFactor: Float,
) {
    // Khung ngoai cua so
    drawRoundRect(
        color = Color(0xFF27303F),
        topLeft = Offset(445f * sx, 198f * sy),
        size = Size(150f * sx, 205f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )

    // Bau troi ban ngay
    drawRect(
        brush = Brush.verticalGradient(
            0.0f to Color(0xFF7DD3FC),
            0.55f to Color(0xFFBAE6FD),
            1.0f to Color(0xFFFEF3C7),
            startY = 206f * sy,
            endY = 395f * sy,
        ),
        topLeft = Offset(453f * sx, 206f * sy),
        size = Size(134f * sx, 189f * sy),
    )

    // Bau troi ban dem (trang + sao)
    if (nightFactor > 0.005f) {
        drawRect(
            brush = Brush.verticalGradient(
                0.0f to Color(0xFF090F24).copy(alpha = nightFactor),
                0.60f to Color(0xFF172554).copy(alpha = nightFactor),
                1.0f to Color(0xFF1E3A5F).copy(alpha = nightFactor),
                startY = 206f * sy,
                endY = 395f * sy,
            ),
            topLeft = Offset(453f * sx, 206f * sy),
            size = Size(134f * sx, 189f * sy),
        )
        // Mat trang khuyet & cac ngoi sao
        drawCircle(
            color = Color(0xFFFEF9C3).copy(alpha = 0.9f * nightFactor),
            radius = 11f * sx,
            center = Offset(552f * sx, 242f * sy),
        )
        drawCircle(
            color = Color(0xFF0E1736).copy(alpha = nightFactor),
            radius = 10f * sx,
            center = Offset(557f * sx, 239f * sy),
        )
        drawCircle(Color.White.copy(alpha = 0.80f * nightFactor), 1.4f * sx, Offset(485f * sx, 232f * sy))
        drawCircle(Color.White.copy(alpha = 0.70f * nightFactor), 1.1f * sx, Offset(515f * sx, 222f * sy))
        drawCircle(Color.White.copy(alpha = 0.75f * nightFactor), 1.3f * sx, Offset(530f * sx, 260f * sy))
        drawCircle(Color.White.copy(alpha = 0.60f * nightFactor), 1.1f * sx, Offset(496f * sx, 274f * sy))
    }

    // Thanh chia o cua kinh
    drawRect(Color(0xFF334155), Offset(517f * sx, 206f * sy), Size(5f * sx, 189f * sy))
    drawRect(Color(0xFF334155), Offset(453f * sx, 305f * sy), Size(134f * sx, 4f * sy))

    // Rem voan trang mo
    val voanColor = Color.White.copy(alpha = 0.32f)
    drawRect(voanColor, Offset(453f * sx, 194f * sy), Size(38f * sx, 226f * sy))
    drawRect(voanColor, Offset(548f * sx, 194f * sy), Size(39f * sx, 226f * sy))

    // Rem vai day 2 ben
    path.setPoly(sx, sy, CURTAIN_LEFT)
    drawPath(path, Color(0xFF464A52))
    path.setPoly(sx, sy, CURTAIN_LEFT_FOLD)
    drawPath(path, Color(0xFF373A40).copy(alpha = 0.65f))

    path.setPoly(sx, sy, CURTAIN_RIGHT)
    drawPath(path, Color(0xFF464A52))
    path.setPoly(sx, sy, CURTAIN_RIGHT_FOLD)
    drawPath(path, Color(0xFF373A40).copy(alpha = 0.65f))
}

private fun DrawScope.drawWardrobeAndVanity(
    path: Path,
    sx: Float,
    sy: Float,
    lightFactor: Float,
) {
    val woodDark = Brush.linearGradient(
        listOf(Color(0xFF6B4830), Color(0xFF3E2718)),
        start = Offset(318f * sx, 186f * sy),
        end = Offset(415f * sx, 424f * sy),
    )
    path.setPoly(sx, sy, WARDROBE_BODY)
    drawPath(path, woodDark)

    // Canh tu kinh toi mau
    drawRect(Color(0xFF1E1815), Offset(358f * sx, 242f * sy), Size(48f * sx, 118f * sy))
    drawRect(
        Color(0xFF573926),
        Offset(358f * sx, 242f * sy),
        Size(48f * sx, 118f * sy),
        style = Stroke(width = 2f * sx),
    )

    // Den LED trong tu
    if (lightFactor > 0.005f) {
        drawRect(
            Color(0xFFFDE047).copy(alpha = 0.75f * lightFactor),
            Offset(362f * sx, 245f * sy),
            Size(40f * sx, 3f * sy),
        )
    }

    // Ban phan & ghe ngoi
    path.setPoly(sx, sy, VANITY_DESK)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFF7D563B), Color(0xFF4E3320)),
            startY = 364f * sy,
            endY = 376f * sy,
        ),
    )
    path.setPoly(sx, sy, CHAIR_SEAT)
    drawPath(path, Color(0xFF9CA3AF))
    path.setPoly(sx, sy, CHAIR_BACK)
    drawPath(path, Color(0xFFB6BCC6))
}

private fun DrawScope.drawLeftDoor(path: Path, sx: Float, sy: Float) {
    path.setPoly(sx, sy, DOOR_OUTER)
    drawPath(path, Color(0xFF4A2F1D))
    drawPath(path, Color(0xFF2D1B0F), style = Stroke(width = 3f * sx))

    path.setPoly(sx, sy, DOOR_INNER)
    drawPath(path, Color(0xFF5A3924))

    drawRoundRect(
        color = Color(0xFFCBD5E1),
        topLeft = Offset(26f * sx, 365f * sy),
        size = Size(12f * sx, 4f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )
}

private fun DrawScope.drawFloorRug(path: Path, sx: Float, sy: Float) {
    path.setPoly(sx, sy, RUG_OUTER)
    drawPath(path, Color(0xFF4B4F57).copy(alpha = 0.92f))
    path.setPoly(sx, sy, RUG_INNER)
    drawPath(path, Color(0xFF575B64).copy(alpha = 0.65f))
}

private fun DrawScope.drawBedAndLamps(
    path: Path,
    sx: Float,
    sy: Float,
    lightOn: Boolean,
    lightFactor: Float,
) {
    // Dai LED hat sau vach dau giuong (vuong goc mep nghieng vach go)
    if (lightFactor > 0.005f) {
        path.setPoly(sx, sy, HEADBOARD_HALO)
        drawPath(
            path,
            Brush.linearGradient(
                listOf(Color(0xFFFBBF24).copy(alpha = 0.30f * lightFactor), Color.Transparent),
                start = Offset(196f * sx, 382f * sy),
                end = Offset(180f * sx, 305f * sy),
            ),
        )
        path.setPoly(sx, sy, HEADBOARD_GLOW)
        drawPath(
            path,
            Brush.linearGradient(
                listOf(Color(0xFFFBBF24).copy(alpha = 0.65f * lightFactor), Color.Transparent),
                start = Offset(196f * sx, 382f * sy),
                end = Offset(184f * sx, 312f * sy),
            ),
        )
    }

    // Vach op go dau giuong
    path.setPoly(sx, sy, HEADBOARD_WOOD)
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(Color(0xFF7D563B), Color(0xFF4E3320)),
            startY = 340f * sy,
            endY = 515f * sy,
        ),
    )
    drawPath(path, Color(0xFF3B2516), style = Stroke(width = 1.5f * sx))

    // Tap dau giuong ben xa & den ngu ben xa
    path.setPoly(sx, sy, NIGHTSTAND_FAR)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFF6B4830), Color(0xFF3E2718)),
            start = Offset(274f * sx, 385f * sy),
            end = Offset(324f * sx, 448f * sy),
        ),
    )

    val shadeColor = if (lightOn) {
        lerp(Color(0xFFD6C7AE), Color(0xFFFEF3C7), lightFactor)
    } else {
        Color(0xFF8C857B)
    }
    if (lightFactor > 0.005f) {
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFEF08A).copy(alpha = 0.95f * lightFactor),
                0.35f to Color(0xFFFBBF24).copy(alpha = 0.45f * lightFactor),
                1.0f to Color.Transparent,
                center = Offset(298f * sx, 368f * sy),
                radius = 40f * sx,
            ),
            radius = 40f * sx,
            center = Offset(298f * sx, 368f * sy),
        )
    }
    drawRect(Color(0xFFA1887F), Offset(295f * sx, 378f * sy), Size(6f * sx, 14f * sy))
    path.setPoly(sx, sy, LAMP_SHADE_FAR)
    drawPath(path, shadeColor)

    // Khung nem giuong & chan ga xam dam
    path.setPoly(sx, sy, DUVET_POLY)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFF6B707A), Color(0xFF3F444E)),
            start = Offset(145f * sx, 394f * sy),
            end = Offset(562f * sx, 538f * sy),
        ),
    )
    path.setPoly(sx, sy, BED_SIDE_LEFT)
    drawPath(path, Color(0xFF3B2618))
    path.setPoly(sx, sy, BED_SIDE_RIGHT)
    drawPath(path, Color(0xFF2E1D12))

    // Tam phu chan giuong xanh Navy
    path.setPoly(sx, sy, THROW_NAVY)
    drawPath(
        path,
        Brush.linearGradient(
            listOf(Color(0xFF1E3A6E), Color(0xFF0F1F40)),
            start = Offset(278f * sx, 410f * sy),
            end = Offset(554f * sx, 536f * sy),
        ),
    )

    // 4 goi ngu (2 xam, 2 xanh Navy)
    drawPillow(path, sx, sy, 168f, 426f, 192f, 402f, 228f, 412f, 212f, 442f, 178f, 448f, Color(0xFF8B919C))
    drawPillow(path, sx, sy, 225f, 406f, 252f, 385f, 288f, 394f, 272f, 424f, 238f, 428f, Color(0xFF8B919C))
    drawPillow(path, sx, sy, 196f, 438f, 224f, 416f, 254f, 426f, 236f, 456f, 206f, 460f, Color(0xFF1E3A6E))
    drawPillow(path, sx, sy, 250f, 418f, 276f, 400f, 304f, 408f, 288f, 436f, 260f, 440f, Color(0xFF1E3A6E))

    // Tap dau giuong ben gan (goc trai) & den ngu ben gan
    path.setPoly(sx, sy, NIGHTSTAND_NEAR_TOP)
    drawPath(path, Color(0xFF8A6144))
    path.setPoly(sx, sy, NIGHTSTAND_NEAR_LEFT)
    drawPath(path, Color(0xFF523623))
    path.setPoly(sx, sy, NIGHTSTAND_NEAR_FRONT)
    drawPath(path, Color(0xFF3F2819))

    if (lightFactor > 0.005f) {
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color(0xFFFEF08A).copy(alpha = 0.95f * lightFactor),
                0.35f to Color(0xFFFBBF24).copy(alpha = 0.45f * lightFactor),
                1.0f to Color.Transparent,
                center = Offset(122f * sx, 414f * sy),
                radius = 60f * sx,
            ),
            radius = 60f * sx,
            center = Offset(122f * sx, 414f * sy),
        )
    }
    drawRoundRect(
        color = Color(0xFF8D7B68),
        topLeft = Offset(117f * sx, 432f * sy),
        size = Size(10f * sx, 24f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sy),
    )
    path.setPoly(sx, sy, LAMP_SHADE_NEAR)
    drawPath(path, shadeColor)
    drawPath(path, Color(0xFFE5D3B3), style = Stroke(width = 1f * sx))
}

private fun DrawScope.drawPillow(
    path: Path,
    sx: Float,
    sy: Float,
    x0: Float,
    y0: Float,
    cx1: Float,
    cy1: Float,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    cx2: Float,
    cy2: Float,
    color: Color,
) {
    path.reset()
    path.moveTo(x0 * sx, y0 * sy)
    path.quadraticTo(cx1 * sx, cy1 * sy, x1 * sx, y1 * sy)
    path.lineTo(x2 * sx, y2 * sy)
    path.quadraticTo(cx2 * sx, cy2 * sy, x0 * sx, y0 * sy)
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawSecurityCamera(sx: Float, sy: Float) {
    drawOval(
        color = Color(0xFFCBD5E1).copy(alpha = 0.85f),
        topLeft = Offset((78f - 14f) * sx, (52f - 8f) * sy),
        size = Size(28f * sx, 16f * sy),
    )
    drawCircle(
        color = Color(0xFF1E293B).copy(alpha = 0.85f),
        radius = 7f * sx,
        center = Offset(78f * sx, 55f * sy),
    )
    drawCircle(
        color = Color(0xFF38BDF8).copy(alpha = 0.80f),
        radius = 2.5f * sx,
        center = Offset(76f * sx, 56f * sy),
    )
}

private fun DrawScope.drawAcUnit(
    path: Path,
    sx: Float,
    sy: Float,
    acFactor: Float,
) {
    path.setPoly(sx, sy, AC_BODY)
    drawPath(path, Color(0xFFE2E8F0))
    drawPath(path, Color(0xFF94A3B8), style = Stroke(width = 1.2f * sx))

    path.setPoly(sx, sy, AC_BOTTOM)
    drawPath(path, Color(0xFFCBD5E1))

    path.setPoly(sx, sy, AC_VENT)
    drawPath(path, Color(0xFF334155))

    // Canh dao gio mo ra khi bat dieu hoa
    val openDrop = 5.5f * acFactor
    val openShift = -3.5f * acFactor
    path.reset()
    path.moveTo((684f + openShift) * sx, (187f + openDrop) * sy)
    path.lineTo((778f + openShift) * sx, (155f + openDrop) * sy)
    path.lineTo((775f + openShift) * sx, (161f + openDrop) * sy)
    path.lineTo((681f + openShift) * sx, (192f + openDrop) * sy)
    path.close()
    drawPath(path, Color(0xFFF8FAFC))
    drawPath(path, Color(0xFF94A3B8), style = Stroke(width = 0.8f * sx))

    // Nen man hinh LED tren dieu hoa
    path.setPoly(sx, sy, AC_LED_BEZEL)
    drawPath(path, Color(0xFF0F172A).copy(alpha = 0.88f))
}

private fun DrawScope.drawCeilingFan(
    bladePath: Path,
    sx: Float,
    sy: Float,
    fanAngle: Float,
    fanVelocity: Float,
) {
    // Ty treo quat tu tran xuong
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(Color(0xFF1E1B18), Color(0xFF4A403A), Color(0xFF1E1B18)),
            startX = 394.5f * sx,
            endX = 405.5f * sx,
        ),
        topLeft = Offset(394.5f * sx, 90f * sy),
        size = Size(11f * sx, 52f * sy),
        cornerRadius = CornerRadius(2f * sx, 2f * sx),
    )

    // Quat tran 5 canh phoi canh 3D (rotateX ~70 deg => scaleY = 0.34f)
    val cx = 400f * sx
    val cy = 141f * sy
    val rotorRadius = 136f * sx
    val u = rotorRadius / 100f

    withTransform({
        translate(left = cx, top = cy)
        scale(scaleX = 1f, scaleY = 0.34f, pivot = Offset.Zero)
    }) {
        // Bong mo chuyen dong nhe khi quay toc do cao
        if (fanVelocity > 4.5f) {
            withTransform({ rotate(degrees = fanAngle - 14f, pivot = Offset.Zero) }) {
                drawFanBlades(bladePath, u, alpha = 0.16f)
            }
        }

        // 5 canh quat chinh
        withTransform({ rotate(degrees = fanAngle, pivot = Offset.Zero) }) {
            drawFanBlades(bladePath, u, alpha = 1f)
        }

        // Bau mo to giua
        drawCircle(Color(0xFF2A201B), radius = 18f * u, center = Offset.Zero)
        drawCircle(
            Color(0xFF4D3D35),
            radius = 18f * u,
            center = Offset.Zero,
            style = Stroke(width = 2f * u),
        )
        drawCircle(Color(0xFF18120F), radius = 8f * u, center = Offset.Zero)
    }
}

private fun DrawScope.drawFanBlades(bladePath: Path, u: Float, alpha: Float) {
    val bladeBrush = Brush.linearGradient(
        0.0f to Color(0xFF4A362B).copy(alpha = alpha),
        0.5f to Color(0xFF2F2119).copy(alpha = alpha),
        1.0f to Color(0xFF1E140F).copy(alpha = alpha),
        start = Offset(-12f * u, -92f * u),
        end = Offset(12f * u, 0f),
    )
    val bladeStroke = Color(0xFF170F0B).copy(alpha = alpha)

    bladePath.reset()
    bladePath.moveTo(-4f * u, -8f * u)
    bladePath.lineTo(-10f * u, -86f * u)
    bladePath.quadraticTo(0f, -94f * u, 10f * u, -86f * u)
    bladePath.lineTo(4f * u, -8f * u)
    bladePath.close()

    for (i in 0 until 5) {
        withTransform({ rotate(degrees = i * 72f, pivot = Offset.Zero) }) {
            drawPath(bladePath, bladeBrush)
            if (alpha >= 0.9f) {
                drawPath(bladePath, bladeStroke, style = Stroke(width = 1f * u))
            }
        }
    }
}

private fun DrawScope.drawAcAirflow(
    airPath: Path,
    sx: Float,
    sy: Float,
    acTemp: Int,
    acFactor: Float,
    airPhase: Float,
) {
    val coldIntensity = ((30 - acTemp).coerceIn(0, 14)) / 14f
    val airAlpha = (0.35f + coldIntensity * 0.65f) * acFactor
    val airColor = when {
        acTemp <= 21 -> Color(0xFF38BDF8)
        acTemp <= 25 -> Color(0xFF7DD3FC)
        else -> Color(0xFFBAE6FD)
    }

    val cycle = 180f * sx
    val dashLen1 = 55f * sx
    val gapLen1 = 125f * sx
    val dashLen2 = 45f * sx
    val gapLen2 = 135f * sx
    val dashLen3 = 50f * sx
    val gapLen3 = 130f * sx

    // Luong 1
    airPath.reset()
    airPath.moveTo(740f * sx, 187f * sy)
    airPath.cubicTo(683f * sx, 217f * sy, 638f * sx, 260f * sy, 570f * sx, 314f * sy)
    drawPath(
        airPath,
        color = airColor.copy(alpha = 0.85f * airAlpha),
        style = Stroke(
            width = 3f * sx,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLen1, gapLen1),
                phase = -airPhase * cycle,
            ),
        ),
    )

    // Luong 2
    val phase2 = (airPhase + 0.33f) % 1f
    airPath.reset()
    airPath.moveTo(723f * sx, 203f * sy)
    airPath.cubicTo(666f * sx, 244f * sy, 621f * sx, 292f * sy, 547f * sx, 346f * sy)
    drawPath(
        airPath,
        color = airColor.copy(alpha = 0.72f * airAlpha),
        style = Stroke(
            width = 2.5f * sx,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLen2, gapLen2),
                phase = -phase2 * cycle,
            ),
        ),
    )

    // Luong 3
    val phase3 = (airPhase + 0.66f) % 1f
    airPath.reset()
    airPath.moveTo(751f * sx, 198f * sy)
    airPath.cubicTo(706f * sx, 252f * sy, 672f * sx, 303f * sy, 621f * sx, 368f * sy)
    drawPath(
        airPath,
        color = airColor.copy(alpha = 0.65f * airAlpha),
        style = Stroke(
            width = 2.2f * sx,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLen3, gapLen3),
                phase = -phase3 * cycle,
            ),
        ),
    )
}

private fun DrawScope.drawAcLedText(
    paint: Paint,
    sx: Float,
    sy: Float,
    acOn: Boolean,
    acTemp: Int,
) {
    val label = if (acOn) "${acTemp}°C" else "OFF"
    val cx = 730f * sx
    val cy = 151f * sy

    paint.textSize = 12f * minOf(sx, sy)
    paint.color = if (acOn) {
        Color(0xFFE0F2FE).toArgb()
    } else {
        Color(0xFFE0F2FE).copy(alpha = 0.35f).toArgb()
    }
    if (acOn) {
        paint.setShadowLayer(6f * sx, 0f, 0f, Color(0xFF38BDF8).toArgb())
    } else {
        paint.clearShadowLayer()
    }

    val canvas = drawContext.canvas.nativeCanvas
    canvas.save()
    canvas.translate(cx, cy)
    // Nghieng theo mat phang phoi canh man hinh LED cua cuc lanh
    canvas.skew(0f, -0.40f * sy / sx)
    val metrics = paint.fontMetrics
    val baseline = -(metrics.ascent + metrics.descent) / 2f
    canvas.drawText(label, 0f, baseline, paint)
    canvas.restore()
}
