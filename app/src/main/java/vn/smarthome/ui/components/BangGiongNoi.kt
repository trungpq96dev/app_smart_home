package vn.smarthome.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.giongnoi.PhienGiongNoi
import vn.smarthome.data.giongnoi.TrangThaiGiongNoi
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * Nut vien thuoc Tro ly AI co dinh o day Sidebar.
 *
 * Thay cho nut mic noi (FAB) o goc duoi phai de khong che noi dung chinh
 * va giup bam de bang tay trai tren tablet treo tuong.
 */
@Composable
fun NutTroLySidebar(
    phien: PhienGiongNoi,
    onBam: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trangThai = phien.trangThai
    val dangNoi = trangThai is TrangThaiGiongNoi.DangNoi
    val dangNghe = trangThai is TrangThaiGiongNoi.DangNghe
    val dangTraLoi = trangThai is TrangThaiGiongNoi.DangTraLoi
    val coLoi = trangThai is TrangThaiGiongNoi.Loi

    // Chi tao rememberInfiniteTransition khi dang nghe hoac dang tra loi:
    // o trang thai nghi (Tat), man hinh bat 24/7 khong duoc chay animation vo han
    // va kiem thu Robolectric dung yen khi mo man hinh chinh.
    val (pulseAlpha, wave1, wave2, wave3) = if (dangNghe || dangTraLoi) {
        val tr = rememberInfiniteTransition(label = "troLySongAm")
        val p by tr.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
            label = "vienTho",
        )
        val w1 by tr.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
            label = "song1",
        )
        val w2 by tr.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(360), RepeatMode.Reverse),
            label = "song2",
        )
        val w3 by tr.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
            label = "song3",
        )
        listOf(p, w1, w2, w3)
    } else {
        listOf(1f, 0.5f, 0.8f, 0.5f)
    }

    val desc = when {
        dangTraLoi -> "Ngắt lời trợ lý giọng nói"
        phien.dangChay -> "Tắt trợ lý giọng nói"
        else -> "Bật trợ lý giọng nói"
    }

    val shape = RoundedCornerShape(50)
    val bgModifier = when {
        coLoi -> Modifier.background(SH.Red.tint(0.18f), shape)
        dangTraLoi -> Modifier.background(SH.Purple.tint(0.24f), shape)
        dangNghe -> Modifier.background(SH.Green.tint(0.20f), shape)
        dangNoi -> Modifier.background(SH.Amber.tint(0.18f), shape)
        else -> Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), SH.Blue)),
            shape,
        )
    }
    val borderColor = when {
        coLoi -> SH.Red
        dangTraLoi -> SH.Purple.copy(alpha = pulseAlpha)
        dangNghe -> SH.Green.copy(alpha = pulseAlpha)
        dangNoi -> SH.Amber.copy(alpha = 0.8f)
        else -> Color(0xFF60A5FA).copy(alpha = 0.85f)
    }
    val borderWidth = if (dangNghe || dangTraLoi) 1.6.dp else 1.2.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .then(bgModifier)
            .border(borderWidth, borderColor, shape)
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(onClick = onBam)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        when {
            coLoi -> {
                Icon(Icons.Filled.ErrorOutline, null, tint = SH.Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Lỗi trợ lý",
                    style = SHType.Button.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            dangTraLoi -> {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    null,
                    tint = Color(0xFFD8B4FE),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                ThanhSongAm(wave1, wave2, wave3, color = Color(0xFFD8B4FE))
                Spacer(Modifier.width(5.dp))
                Text(
                    "Đang trả lời",
                    style = SHType.Button.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            dangNghe -> {
                ThanhSongAm(wave1, wave2, wave3, color = SH.Green)
                Spacer(Modifier.width(7.dp))
                Text(
                    "Đang nghe...",
                    style = SHType.Button.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            dangNoi -> {
                Icon(Icons.Filled.Mic, null, tint = SH.Amber, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Đang nối...",
                    style = SHType.Button.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            else -> {
                Icon(Icons.Filled.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    "Trợ lý AI",
                    style = SHType.Button.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThanhSongAm(f1: Float, f2: Float, f3: Float, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(16.dp),
    ) {
        listOf(f1, f2, f3).forEach { frac ->
            val h = (5f + 10f * frac.coerceIn(0f, 1f)).dp
            Box(
                Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

/**
 * Bang thoai cua Tro ly AI, chi hien khi phien dang chay hoac co loi.
 * Nut bam dieu khien da duoc dua vao day Sidebar (`NutTroLySidebar`).
 */
@Composable
fun BangThoaiGiongNoi(
    phien: PhienGiongNoi,
    onDong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = phien.dangChay || phien.trangThai is TrangThaiGiongNoi.Loi,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        KhungThoai(phien, onDong)
    }
}

@Composable
private fun KhungThoai(phien: PhienGiongNoi, onDong: () -> Unit) {
    Column(
        Modifier
            .width(340.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SH.Card)
            .border(1.dp, SH.Border, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = nhanTrangThai(phien),
                style = SHType.CardTitle,
                color = mauTrangThai(phien.trangThai),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (phien.dangChay) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = dongHo(phien.giayDaChay),
                    style = SHType.Caption,
                    color = SH.TextOff,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Close,
                contentDescription = "Đóng bảng thoại",
                tint = SH.TextDim,
                modifier = Modifier.size(18.dp).clickable(onClick = onDong),
            )
        }

        if (phien.trangThai is TrangThaiGiongNoi.DangTraLoi) {
            Text(
                "Bấm nút Trợ lý AI ở thanh bên trái để ngắt lời ngay",
                style = SHType.Caption,
                color = SH.TextOff,
            )
        } else if (phien.banSongCong && phien.dangChay) {
            Text(
                "Bán song công — máy không khử vọng tốt nên mic tạm ngừng khi loa đang nói",
                style = SHType.Caption,
                color = SH.TextOff,
            )
        }

        if (phien.thoai.isNotEmpty()) {
            Column(
                Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                phien.thoai.forEach { d ->
                    Text(
                        text = (if (d.cuaMinh) "Bạn: " else "Trợ lý: ") + d.cau,
                        style = SHType.Body,
                        color = if (d.cuaMinh) SH.Text else SH.TextDim,
                    )
                }
            }
        }

        phien.daLam.forEach { v ->
            Column {
                Text(
                    text = (if (v.thanhCong) "✓ " else "✕ ") + v.cau,
                    style = SHType.Label,
                    color = if (v.thanhCong) SH.Green else SH.Amber,
                )
                // Nguyen tac cua du an: khong gia vo da lam duoc viec that.
                if (v.chuaCoThietBiThat) {
                    Text(
                        "chỉ đổi trên màn hình — chưa nối thiết bị thật",
                        style = SHType.Caption,
                        color = SH.TextOff,
                    )
                }
            }
        }
    }
}

private fun nhanTrangThai(phien: PhienGiongNoi): String = when (val t = phien.trangThai) {
    TrangThaiGiongNoi.Tat -> "Trợ lý đã tắt"
    TrangThaiGiongNoi.DangNoi -> "Đang nối tới Gemini..."
    TrangThaiGiongNoi.DangNghe -> "Đang nghe — cứ nói tự nhiên"
    TrangThaiGiongNoi.DangTraLoi -> "Trợ lý đang trả lời"
    is TrangThaiGiongNoi.Loi -> t.cau
}

private fun mauTrangThai(t: TrangThaiGiongNoi): Color = when (t) {
    is TrangThaiGiongNoi.Loi -> SH.Red
    TrangThaiGiongNoi.DangNghe -> SH.Green
    TrangThaiGiongNoi.DangTraLoi -> SH.Purple
    else -> SH.Text
}

private fun dongHo(giay: Int): String {
    val p = giay / 60
    val g = giay % 60
    return p.toString() + ":" + g.toString().padStart(2, '0')
}
