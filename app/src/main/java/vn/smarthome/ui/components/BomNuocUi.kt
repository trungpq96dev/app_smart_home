package vn.smarthome.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.smarthome.data.bomnuoc.KenhBomNuoc
import vn.smarthome.data.bomnuoc.LuongNuocThang
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint
import kotlin.math.sin

/**
 * So do he thong bom nuoc ve bang Canvas theo bang mau [SH].
 *
 * Chi co van mot chieu co khi tinh (khong co van dien tu ban 5.x) va chi 1
 * phao vat ly (GPIO 25). Animation chi chay khi [anyFlow] = true de tiet kiem
 * CPU/GPU tren Tab A7 Lite va khong lam treo Robolectric khi dung yen.
 */
@Composable
fun SoDoBonNuoc(
    levelPercent: Double?,
    tankPumpRunning: Boolean,
    boostPumpRunning: Boolean,
    cityFlowing: Boolean,
    tankFull: Boolean,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val transition = rememberInfiniteTransition(label = "soDoBom")
    val anyFlow = tankPumpRunning || boostPumpRunning || cityFlowing

    val spin: Float = if (!(tankPumpRunning || boostPumpRunning)) 0f else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
            label = "spinBom",
        ).value
    }

    val flowPhase: Float = if (!anyFlow) 0f else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 28f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "flowBom",
        ).value
    }

    val wave: Float = if (levelPercent == null || !anyFlow) 0f else {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(
                tween(2600, easing = LinearEasing),
                RepeatMode.Restart,
            ),
            label = "waveBom",
        ).value
    }

    val levelTarget = ((levelPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val level: Float = animateFloatAsState(
        targetValue = levelTarget,
        animationSpec = tween(500),
        label = "mucNuocSoDo",
    ).value

    Box(modifier.heightIn(min = 140.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawHeThongBom(
                level = level,
                coCamBienMuc = levelPercent != null,
                tankPumpRunning = tankPumpRunning,
                boostPumpRunning = boostPumpRunning,
                cityFlowing = cityFlowing,
                tankFull = tankFull,
                spin = spin,
                flowPhase = flowPhase,
                wave = wave,
                measurer = measurer,
            )
        }
    }
}

private fun DrawScope.drawHeThongBom(
    level: Float,
    coCamBienMuc: Boolean,
    tankPumpRunning: Boolean,
    boostPumpRunning: Boolean,
    cityFlowing: Boolean,
    tankFull: Boolean,
    spin: Float,
    flowPhase: Float,
    wave: Float,
    measurer: TextMeasurer,
) {
    val w = size.width
    val h = size.height
    if (w <= 4f || h <= 4f) return

    val rCorner = 8.dp.toPx()
    val sThin = 2.dp.toPx()
    val sLeg = 2.5.dp.toPx()

    val tankW = w * 0.40f
    val tankH = h * 0.45f
    val tankX = w * 0.46f
    val tankY = h * 0.06f
    val tankRect = Rect(tankX, tankY, tankX + tankW, tankY + tankH)

    drawRoundRect(
        color = SH.Card2,
        topLeft = Offset(tankRect.left, tankRect.top),
        size = Size(tankW, tankH),
        cornerRadius = CornerRadius(rCorner, rCorner),
    )

    if (coCamBienMuc && level > 0.005f) {
        val waterH = tankH * level
        val waterTop = tankRect.bottom - waterH
        val inset = 3.dp.toPx()

        clipRect(
            left = tankRect.left + inset,
            top = tankRect.top + inset,
            right = tankRect.right - inset,
            bottom = tankRect.bottom - inset,
        ) {
            drawRect(
                color = SH.Cyan.copy(alpha = 0.52f),
                topLeft = Offset(tankRect.left + inset, waterTop),
                size = Size((tankW - inset * 2).coerceAtLeast(0f), waterH + inset),
            )
            val path = Path().apply {
                val amp = 2.5.dp.toPx()
                val step = 3.dp.toPx()
                val band = 7.dp.toPx()
                moveTo(tankRect.left + inset, waterTop)
                var x = tankRect.left + inset
                while (x <= tankRect.right - inset) {
                    val rel = (x - tankRect.left) / tankW
                    val y = waterTop + sin(wave + rel * 6.4f) * amp
                    lineTo(x, y)
                    x += step
                }
                lineTo(tankRect.right - inset, waterTop + band)
                lineTo(tankRect.left + inset, waterTop + band)
                close()
            }
            drawPath(path, SH.Cyan.copy(alpha = 0.85f))
        }
    }

    drawRoundRect(
        color = if (tankFull) SH.Green else SH.Border,
        topLeft = Offset(tankRect.left, tankRect.top),
        size = Size(tankW, tankH),
        cornerRadius = CornerRadius(rCorner, rCorner),
        style = Stroke(width = sThin),
    )

    // Phao duy nhat (GPIO 25)
    val floatY = tankRect.top + tankH * 0.18f
    drawCircle(
        color = if (tankFull) SH.Green else SH.TextOff,
        radius = 4.dp.toPx(),
        center = Offset(tankRect.right - 10.dp.toPx(), floatY),
    )

    val legTop = tankRect.bottom
    val legBottom = h * 0.66f
    val legInset = 10.dp.toPx()
    listOf(tankRect.left + legInset, tankRect.right - legInset).forEach { x ->
        drawLine(SH.Border, Offset(x, legTop), Offset(x, legBottom), strokeWidth = sLeg)
    }

    val rPump = (h * 0.10f).coerceIn(7.dp.toPx(), 18.dp.toPx())
    val rBoost = rPump * 0.85f

    // ---- Ong tu bom len bon (dat bom len bon o ben trai) ----
    val pumpCx = w * 0.17f
    val pumpCy = h * 0.62f
    val riserX = tankRect.left + tankW * 0.22f

    drawPipe(
        points = listOf(
            Offset(pumpCx + rPump, pumpCy),
            Offset(riserX, pumpCy),
            Offset(riserX, tankRect.bottom - 3.dp.toPx()),
        ),
        active = tankPumpRunning,
        phase = flowPhase,
    )

    // ---- Ong nuoc may vao bon (qua van mot chieu co khi) ----
    val cityY = h * 0.27f
    val cityValveX = w * 0.26f
    drawPipe(
        points = listOf(
            Offset(w * 0.04f, cityY),
            Offset(tankRect.left + tankW * 0.62f, cityY),
            Offset(tankRect.left + tankW * 0.62f, tankRect.top + 3.dp.toPx()),
        ),
        active = cityFlowing,
        phase = flowPhase,
    )
    drawCheckValve(Offset(cityValveX, cityY), cityFlowing)

    // ---- Ong ra tu bom tang ap (dat duoi bon lech phai de khong de chu bom len bon) ----
    val boostCx = w * 0.66f
    val boostCy = h * 0.84f
    val outletX = tankRect.left + tankW * 0.52f
    drawPipe(
        points = listOf(
            Offset(outletX, tankRect.bottom),
            Offset(outletX, boostCy),
            Offset(boostCx - rBoost, boostCy),
        ),
        active = boostPumpRunning,
        phase = flowPhase,
    )
    drawPipe(
        points = listOf(
            Offset(boostCx + rBoost, boostCy),
            Offset(w * 0.94f, boostCy),
        ),
        active = boostPumpRunning,
        phase = flowPhase,
    )

    drawPump(Offset(pumpCx, pumpCy), tankPumpRunning, spin, rPump)
    drawPump(Offset(boostCx, boostCy), boostPumpRunning, spin, rBoost)

    val gap = 5.dp.toPx()
    drawLabel(measurer, "Bơm lên bồn", Offset(pumpCx - rPump, pumpCy + rPump + gap))
    drawLabel(measurer, "Bơm tăng áp", Offset(boostCx + rBoost + gap, boostCy - 18.dp.toPx()))
    drawLabel(measurer, "Van cơ khí", Offset(cityValveX - 22.dp.toPx(), cityY - 21.dp.toPx()))
    drawLabel(measurer, "Nước máy", Offset(w * 0.02f, cityY + 9.dp.toPx()))
}

private fun DrawScope.drawPipe(
    points: List<Offset>,
    active: Boolean,
    phase: Float,
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    val outer = 6.5.dp.toPx()
    val inner = 3.5.dp.toPx()

    drawPath(path, SH.Border, style = Stroke(width = outer))
    drawPath(path, SH.Card, style = Stroke(width = inner))

    if (active) {
        drawPath(path, SH.Cyan.copy(alpha = 0.35f), style = Stroke(width = inner))
        drawPath(
            path,
            SH.Cyan,
            style = Stroke(
                width = 2.2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 12.dp.toPx()),
                    -phase * density,
                ),
            ),
        )
    }
}

private fun DrawScope.drawPump(
    center: Offset,
    running: Boolean,
    spin: Float,
    radius: Float,
) {
    val body = if (running) SH.Blue else SH.Card2
    drawCircle(body, radius, center)
    drawCircle(
        if (running) SH.Cyan else SH.Border,
        radius,
        center,
        style = Stroke(width = 1.6.dp.toPx()),
    )

    rotate(degrees = spin, pivot = center) {
        val bladeColor = if (running) Color.White else SH.TextOff
        repeat(3) { i ->
            rotate(degrees = i * 120f, pivot = center) {
                drawLine(
                    bladeColor,
                    start = center,
                    end = Offset(center.x, center.y - radius * 0.62f),
                    strokeWidth = 1.8.dp.toPx(),
                )
            }
        }
    }
    drawCircle(if (running) Color.White else SH.TextOff, radius * 0.17f, center)
}

private fun DrawScope.drawCheckValve(center: Offset, flowing: Boolean) {
    val c = if (flowing) SH.Cyan else SH.TextOff
    val s = 6.dp.toPx()
    val r = s + 2.dp.toPx()
    drawCircle(SH.Bg, r, center)
    drawCircle(c, r, center, style = Stroke(width = 1.4.dp.toPx()))
    val tri = Path().apply {
        moveTo(center.x - s * 0.55f, center.y - s * 0.6f)
        lineTo(center.x + s * 0.7f, center.y)
        lineTo(center.x - s * 0.55f, center.y + s * 0.6f)
        close()
    }
    drawPath(tri, c)
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, at: Offset) {
    val layout = measurer.measure(
        text = AnnotatedString(text),
        style = SHType.Caption.copy(color = SH.TextDim),
    )
    val tw = layout.size.width.toFloat()
    val th = layout.size.height.toFloat()
    if (tw > size.width || th > size.height) return
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(
            x = at.x.coerceIn(0f, size.width - tw),
            y = at.y.coerceIn(0f, size.height - th),
        ),
    )
}

/** Vong tron hien thi phan tram muc nuoc that. Null -> "--" (khong bao gio doan). */
@Composable
fun VongMucNuoc(
    percent: Double?,
    modifier: Modifier = Modifier,
    diameter: Dp = 104.dp,
    strokeWidth: Dp = 11.dp,
) {
    val target = ((percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 400),
        label = "vongMucNuoc",
    )

    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f))
            val topLeft = Offset(inset, inset)

            drawArc(
                color = SH.Card2,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (percent != null && animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0f to SH.Blue,
                        0.5f to SH.Cyan,
                        1f to SH.Blue,
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (percent == null) {
                Text("--", style = SHType.Value, color = SH.TextOff)
                Text("chưa có cảm biến", style = SHType.Caption, color = SH.TextOff)
            } else {
                Text("${percent.toInt()}%", style = SHType.Value, color = SH.Text)
            }
        }
    }
}

@Composable
fun BoChonCheDo(
    options: List<Pair<String, String>>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val chipAlpha = when {
        !enabled -> 0.40f
        loading -> 0.75f
        else -> 1f
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (key, label) ->
            val isOn = selected == key
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .alpha(chipAlpha)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isOn) SH.Blue else SH.Card2)
                    .then(
                        if (isOn) Modifier
                        else Modifier.border(
                            BorderStroke(Dim.BorderWidth, SH.Border),
                            RoundedCornerShape(10.dp),
                        )
                    )
                    .clickable(enabled = enabled && !loading) { onSelect(key) },
                contentAlignment = Alignment.Center,
            ) {
                if (loading && isOn) {
                    VongXoayNho(color = Color.White, size = 15.dp)
                } else {
                    Text(
                        label,
                        style = SHType.Button,
                        color = if (isOn) Color.White else SH.TextDim,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun NutHanhDongBom(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    active: Boolean = false,
    height: Dp = Dim.ButtonH,
) {
    val container = if (active) color else color.copy(alpha = 0.90f)
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .alpha(
                when {
                    !enabled -> 0.40f
                    loading -> 0.75f
                    else -> 1f
                }
            ),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = Color.White,
            disabledContainerColor = container,
            disabledContentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp),
    ) {
        if (loading) {
            VongXoayNho(color = Color.White, size = 16.dp)
            Spacer(Modifier.width(7.dp))
            Text("Đang gửi…", style = SHType.Button, maxLines = 1)
        } else {
            Icon(icon, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, style = SHType.Button, maxLines = 1)
        }
    }
}

@Composable
fun CongTacBom(
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    activeColor: Color = SH.Green,
    contentDescription: String? = null,
) {
    val target = if (checked) 1f else 0f
    val t by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(150),
        label = "congTacBom",
    )

    val track = if (checked) activeColor else SH.Border

    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .alpha(
                when {
                    !enabled -> 0.40f
                    loading -> 0.75f
                    else -> 1f
                }
            )
            .clip(RoundedCornerShape(14.dp))
            .background(track)
            .semantics {
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .clickable(enabled = enabled && !loading, role = Role.Switch) { onToggle(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (loading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                VongXoayNho(color = Color.White, size = 14.dp)
            }
        } else {
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .padding(start = 20.dp * t)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
fun VongXoayNho(color: Color = SH.Text, size: Dp = 16.dp) {
    CircularProgressIndicator(
        modifier = Modifier.size(size),
        color = color,
        strokeWidth = 2.dp,
    )
}

@Composable
fun DongThongSoBom(
    icon: ImageVector,
    label: String,
    value: String?,
    unit: String? = null,
    accent: Color = SH.Cyan,
    unavailableReason: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            style = SHType.Caption,
            color = SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value == null) {
            Text("--", style = SHType.BodyMed, color = SH.TextOff)
            if (unavailableReason != null) {
                Spacer(Modifier.width(4.dp))
                Text(unavailableReason, style = SHType.Caption, color = SH.TextOff, maxLines = 1)
            }
        } else {
            Text(value, style = SHType.BodyMed, color = SH.Text, maxLines = 1)
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(unit, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
        }
    }
}

@Composable
fun DongTrangThaiBom(
    icon: ImageVector,
    label: String,
    pillText: String,
    pillColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, pillColor, size = 22.dp, iconSize = 13.dp, shape = RoundedCornerShape(6.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            style = SHType.Caption,
            color = SH.TextDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        StatusPill(pillText, pillColor, showDot = false)
    }
}

@Composable
fun TrangThaiKetNoiBomPill(
    ketNoi: TrangThaiKetNoiBom,
    kenh: KenhBomNuoc,
    modifier: Modifier = Modifier,
) {
    val (chu, mau) = when (ketNoi) {
        is TrangThaiKetNoiBom.TrucTiep ->
            (if (kenh == KenhBomNuoc.LAN) "LAN · 1s" else "MQTT · 30s") to SH.Green
        TrangThaiKetNoiBom.ChuaCauHinh -> "Chưa cấu hình bơm" to SH.TextOff
        TrangThaiKetNoiBom.DangNoi -> "Đang nối bo bơm…" to SH.Amber
        TrangThaiKetNoiBom.ChiCoBroker -> "Chưa thấy bo bơm" to SH.Amber
        is TrangThaiKetNoiBom.SoCu -> "Số bơm cũ (${ketNoi.tuoiGiay}s)" to SH.Amber
        TrangThaiKetNoiBom.BoOffline -> "Bo bơm offline" to SH.Red
        TrangThaiKetNoiBom.MatBroker -> "Mất broker bơm" to SH.Red
        TrangThaiKetNoiBom.SaiTaiKhoan -> "Sai tài khoản MQTT" to SH.Red
        is TrangThaiKetNoiBom.Loi -> "Lỗi nối bo bơm" to SH.Red
    }
    StatusPill(chu, mau, modifier = modifier)
}

/**
 * Bieu do cot nhom luong nuoc theo thang (so lieu that tu /api/usage).
 * Cot Cyan = bom len bon, Cot Green = nuoc may (neu co).
 */
@Composable
fun BieuDoNuocThang(
    usage: List<LuongNuocThang>,
    maxMonths: Int = 6,
    modifier: Modifier = Modifier,
) {
    val data = usage.take(maxMonths).reversed()
    if (data.isEmpty()) return
    val max = data.maxOfOrNull { maxOf(it.bomLit, it.nuocMayLit ?: 0.0) }
        ?.coerceAtLeast(1.0) ?: 1.0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { m ->
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    CotDon((m.bomLit / max).toFloat(), SH.Cyan, Modifier.weight(1f))
                    m.nuocMayLit?.let {
                        CotDon((it / max).toFloat(), SH.Green, Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(m.nhanNgan, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CotDon(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxHeight(fraction.coerceIn(0.03f, 1f))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(color)
    )
}

fun dinhDangThoiGianChay(giay: Long): String {
    val h = giay / 3600
    val m = (giay % 3600) / 60
    val s = giay % 60
    return when {
        h > 0 -> "%d giờ %02d phút".format(h, m)
        m > 0 -> "%d phút %02d giây".format(m, s)
        else -> "$s giây"
    }
}

fun catSoThapPhan(v: Double): String =
    if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
