package vn.smarthome.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/** Che do hien thi nhan so tren dinh cot bieu do. */
enum class BarLabelMode {
    NONE,
    ALL,
    KEY_POINTS,
}

/**
 * Bieu do cot - ve bang Canvas thuan, khong dung thu vien chart nao.
 *
 * Ho tro:
 *  - Nhan so tren dinh cot (hien tat ca o 7 ngay, chi hien Peak / Hom nay / Dang chon o 14-30 ngay)
 *  - Duong ke ngang net dut muc trung binh + nhan o ria phai (vd: "TB: 8.1 kWh")
 *  - Cham chon cot de xem chi tiet ngay: lam sang vien cot duoc chon, giam do sang cac cot con lai
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String = "",
    labelMode: BarLabelMode = BarLabelMode.NONE,
    averageValue: Float? = null,
    averageLabel: String? = null,
    selectedIndex: Int? = null,
    onSelectIndex: ((Int?) -> Unit)? = null,
    startAtZero: Boolean = false,
) {
    if (values.isEmpty()) return

    val maxV = maxOf(values.max(), averageValue ?: 0f).coerceAtLeast(0.001f)
    val minV = values.min().coerceAtLeast(0f)
    // Day bieu do khong bat dau tu 0 khi so lieu nam o dai hep (vi du nhiet do
    // 26..29) - neu khong moi cot se cao gan bang nhau va bieu do vo nghia.
    val floor = if (!startAtZero && minV / maxV > 0.6f) minV * 0.92f else 0f
    val span = (maxV - floor).coerceAtLeast(0.001f)

    val grow by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "barGrow-" + values.size.toString() + "-" + maxV.toString(),
    )

    val measurer = rememberTextMeasurer()
    val hasSelection = selectedIndex != null && selectedIndex in values.indices
    val peakIdx = remember(values) {
        values.indices.maxByOrNull { values[it] } ?: 0
    }

    Column(modifier) {
        val tapModifier = if (onSelectIndex != null) {
            Modifier.pointerInput(values.size, selectedIndex) {
                detectTapGestures { offset ->
                    val slot = size.width.toFloat() / values.size
                    val idx = (offset.x / slot).toInt().coerceIn(0, values.lastIndex)
                    onSelectIndex(if (selectedIndex == idx) null else idx)
                }
            }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(tapModifier),
        ) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas
            val n = values.size
            val slot = size.width / n
            val barW = (slot * if (n <= 7) 0.56f else 0.66f).coerceAtMost(72.dp.toPx())
            val inset = (slot - barW) / 2f
            val topPad = if (labelMode != BarLabelMode.NONE) 22.dp.toPx() else 0f
            val plotH = (size.height - topPad).coerceAtLeast(8.dp.toPx())
            val dimAlpha = 0.45f

            // 1. Ve nen cot, cot va vien sang cot dang chon
            val barRects = ArrayList<Triple<Float, Float, Float>>(n) // x, topY, height
            values.forEachIndexed { i, v ->
                val rawFrac = ((v - floor) / span).coerceIn(0.04f, 1f)
                val frac = rawFrac * grow
                val h = (plotH * frac).coerceAtLeast(minOf(4.dp.toPx(), plotH))
                val x = i * slot + inset
                val topY = topPad + plotH - h
                barRects.add(Triple(x, topY, h))

                val isSelected = selectedIndex == i
                val itemAlpha = if (hasSelection && !isSelected) dimAlpha else 1f
                val corner = CornerRadius(minOf(barW * 0.28f, h / 2f))

                // Khung nen nhe bao quanh ca slot khi cot duoc chon
                if (isSelected) {
                    val padSlot = minOf(inset * 0.65f, 10.dp.toPx())
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.06f),
                        topLeft = Offset(x - padSlot, 2.dp.toPx()),
                        size = Size(barW + padSlot * 2f, size.height - 2.dp.toPx()),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                    )
                }

                // Nen cot mo de thay chieu cao toi da
                drawRoundRect(
                    color = SH.Card2.copy(alpha = if (hasSelection && !isSelected) 0.55f else 0.9f),
                    topLeft = Offset(x, topPad),
                    size = Size(barW, plotH),
                    cornerRadius = CornerRadius(barW * 0.28f),
                )

                // Cot du lieu
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            color.copy(alpha = itemAlpha),
                            color.copy(alpha = 0.55f * itemAlpha),
                        ),
                        startY = topY,
                        endY = topPad + plotH,
                    ),
                    topLeft = Offset(x, topY),
                    size = Size(barW, h),
                    cornerRadius = corner,
                )

                // Vien phat sang nhe khi cham chon cot
                if (isSelected) {
                    drawRoundRect(
                        color = Color(0xFF60A5FA).copy(alpha = 0.35f),
                        topLeft = Offset(x, topY),
                        size = Size(barW, h),
                        cornerRadius = corner,
                        style = Stroke(width = 4.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color(0xFF93C5FD),
                        topLeft = Offset(x, topY),
                        size = Size(barW, h),
                        cornerRadius = corner,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            // 2. Duong ke ngang net dut muc trung binh + nhan ben phai
            if (averageValue != null && averageValue > 0f) {
                val avgFrac = ((averageValue - floor) / span).coerceIn(0f, 1f)
                val avgY = topPad + plotH * (1f - avgFrac)
                drawLine(
                    color = Color(0xFFCBD5E1).copy(alpha = 0.45f),
                    start = Offset(0f, avgY),
                    end = Offset(size.width, avgY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                        0f,
                    ),
                )
                if (!averageLabel.isNullOrBlank()) {
                    val avgLayout = measurer.measure(
                        text = AnnotatedString(averageLabel),
                        style = SHType.Caption.copy(
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    val padH = 5.dp.toPx()
                    val padV = 2.dp.toPx()
                    val boxW = avgLayout.size.width + padH * 2f
                    val boxH = avgLayout.size.height + padV * 2f
                    val boxX = (size.width - boxW - 2.dp.toPx()).coerceAtLeast(0f)
                    val boxY = (avgY - boxH - 3.dp.toPx()).coerceIn(0f, (size.height - boxH).coerceAtLeast(0f))
                    drawRoundRect(
                        color = SH.Card.copy(alpha = 0.88f),
                        topLeft = Offset(boxX, boxY),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    drawText(
                        textLayoutResult = avgLayout,
                        topLeft = Offset(boxX + padH, boxY + padV),
                    )
                }
            }

            // 3. Nhan so tren dinh cot
            if (labelMode != BarLabelMode.NONE) {
                val candidateIndices: List<Int> = when (labelMode) {
                    BarLabelMode.ALL -> values.indices.toList()
                    BarLabelMode.KEY_POINTS -> buildList {
                        if (selectedIndex != null && selectedIndex in values.indices) add(selectedIndex)
                        if (peakIdx in values.indices && peakIdx !in this) add(peakIdx)
                        if (values.lastIndex >= 0 && values.lastIndex !in this) add(values.lastIndex)
                    }
                    BarLabelMode.NONE -> emptyList()
                }

                val placedRanges = mutableListOf<Pair<Float, Float>>()
                val minGap = 4.dp.toPx()
                candidateIndices.forEach { i ->
                    val (x, topY, _) = barRects[i]
                    val isSelected = selectedIndex == i
                    val showUnit = isSelected && unit.isNotEmpty()
                    val text = formatBarValue(values[i]) + if (showUnit) " $unit" else ""
                    val textColor = when {
                        isSelected -> Color.White
                        hasSelection -> Color(0xFFCBD5E1).copy(alpha = 0.5f)
                        else -> Color(0xFFCBD5E1)
                    }
                    val layout = measurer.measure(
                        text = AnnotatedString(text),
                        style = SHType.Caption.copy(
                            color = textColor,
                            fontSize = if (isSelected) 11.sp else 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                    )
                    val tw = layout.size.width.toFloat()
                    val th = layout.size.height.toFloat()
                    val cx = x + barW / 2f
                    val drawX = (cx - tw / 2f).coerceIn(0f, (size.width - tw).coerceAtLeast(0f))
                    val drawY = (topY - th - 4.dp.toPx()).coerceAtLeast(0f)

                    if (labelMode == BarLabelMode.KEY_POINTS) {
                        val overlaps = placedRanges.any { (l, r) ->
                            drawX < r + minGap && (drawX + tw) > l - minGap
                        }
                        if (overlaps) return@forEach
                        placedRanges.add(drawX to (drawX + tw))
                    }

                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(drawX, drawY),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEachIndexed { i, l ->
                val isSelected = selectedIndex == i
                val labelMod = Modifier
                    .weight(1f)
                    .let { base ->
                        if (onSelectIndex != null) {
                            val desc = if (l.isNotBlank()) "Cột $l" else "Cột ${i + 1}"
                            base
                                .semantics { contentDescription = desc }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    onSelectIndex(if (selectedIndex == i) null else i)
                                }
                        } else {
                            base
                        }
                    }
                Text(
                    l,
                    style = SHType.Caption.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                    color = when {
                        isSelected -> Color.White
                        hasSelection -> SH.TextOff.copy(alpha = 0.55f)
                        else -> SH.TextOff
                    },
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = labelMod,
                )
            }
        }
    }
}

private fun formatBarValue(v: Float): String {
    if (v >= 100f) return Math.round(v).toString()
    val r = Math.round(v * 10f) / 10f
    return if (r == kotlin.math.floor(r)) r.toInt().toString() else r.toString()
}

/**
 * Dong ho cung (nua vong tron) - dung cho "Cong suat hien tai", "Muc bon"...
 * Cung chay tu 180 do den 360 do, tuc nua tren cua vong tron.
 */
@Composable
fun ArcGauge(
    fraction: Float,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val f by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 480),
        label = "gauge",
    )

    Box(modifier, contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = size.height * 0.13f
            // Khi o chua qua be, d co the am - ve cung am la crash, nen kep lai.
            val d = (minOf(size.width, size.height * 2f) - stroke).coerceAtLeast(0f)
            val topLeft = Offset((size.width - d) / 2f, size.height - d / 2f - stroke / 2f)
            val arcSize = Size(d, d)
            drawArc(
                color = SH.Card2,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f * f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            value,
            style = SHType.CardTitle,
            color = SH.Text,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

/** Thanh tien trinh ngang mong - dung trong cac the nho. */
@Composable
fun ThinBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "thinBar",
    )
    Canvas(modifier.height(6.dp).fillMaxWidth()) {
        drawRoundRect(
            color = SH.Card2,
            size = size,
            cornerRadius = CornerRadius(size.height / 2f),
        )
        drawRoundRect(
            color = color,
            size = Size(size.width * f, size.height),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    }
}
