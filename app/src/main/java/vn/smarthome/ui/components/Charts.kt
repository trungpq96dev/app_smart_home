package vn.smarthome.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Bieu do cot - ve bang Canvas thuan, khong dung thu vien chart nao.
 *
 * Cot cao theo ti le so lon nhat trong bo so lieu. Co animation khi doi tab
 * nen nguoi dung thay ro la so lieu da doi chu khong phai man hinh dung hinh.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (values.isEmpty()) return

    val maxV = (values.max()).coerceAtLeast(0.001f)
    val minV = (values.min()).coerceAtLeast(0f)
    // Day bieu do khong bat dau tu 0 khi so lieu nam o dai hep (vi du nhiet do
    // 26..29) - neu khong moi cot se cao gan bang nhau va bieu do vo nghia.
    val floor = if (minV / maxV > 0.6f) minV * 0.92f else 0f
    val span = (maxV - floor).coerceAtLeast(0.001f)

    val grow by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 420),
        label = "barGrow-" + values.size.toString() + "-" + maxV.toString(),
    )

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val n = values.size
            // Moi cot chiem dung mot o rong size.width/n va nam GIUA o do.
            // Lam kieu "barW + gap" cong don thi tam cot lech dan sang phai va
            // khong con khop voi nhan ngay ben duoi (nhan chia deu theo weight).
            val slot = size.width / n
            val barW = slot * 0.66f
            val inset = (slot - barW) / 2f
            values.forEachIndexed { i, v ->
                val frac = ((v - floor) / span).coerceIn(0.04f, 1f) * grow
                val h = size.height * frac
                val x = i * slot + inset
                // Nen cot mo de thay chieu cao toi da
                drawRoundRect(
                    color = SH.Card2,
                    topLeft = Offset(x, 0f),
                    size = Size(barW, size.height),
                    cornerRadius = CornerRadius(barW * 0.3f),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.55f)),
                        startY = size.height - h,
                        endY = size.height,
                    ),
                    topLeft = Offset(x, size.height - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW * 0.3f),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { l ->
                Text(
                    l,
                    style = SHType.Caption,
                    color = SH.TextOff,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
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
