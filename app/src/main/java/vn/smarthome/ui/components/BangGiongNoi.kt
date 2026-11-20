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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import vn.smarthome.data.giongnoi.PhienGiongNoi
import vn.smarthome.data.giongnoi.TrangThaiGiongNoi
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Nut mic + bang thoai, noi tren moi man hinh.
 *
 * Nam o goc duoi BEN PHAI chu khong phai giua: SnackbarHost da chiem canh
 * duoi giua, va cot menu chiem canh trai. Man chi cao 601dp nen bang thoai
 * bi khong che chieu cao va cuon ben trong.
 */
@Composable
fun BangGiongNoi(
    phien: PhienGiongNoi,
    onBatTat: () -> Unit,
    onDong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 14.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AnimatedVisibility(
            visible = phien.dangChay || phien.trangThai is TrangThaiGiongNoi.Loi,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            KhungThoai(phien, onDong)
        }
        NutMic(phien, onBatTat)
    }
}

@Composable
private fun NutMic(phien: PhienGiongNoi, onBatTat: () -> Unit) {
    val dangNghe = phien.trangThai is TrangThaiGiongNoi.DangNghe
    val dangTraLoi = phien.trangThai is TrangThaiGiongNoi.DangTraLoi

    // Vong nhip chi chay khi dang nghe. Khong cho chay lien tuc: man hinh nay
    // bat 24/7 tren may treo tuong, mot animation vinh cuu la mot luong ve lai
    // moi khung hinh suot ngay.
    val nhip = if (dangNghe) {
        val tr = rememberInfiniteTransition(label = "nhip-mic")
        tr.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "nhip-mic-scale",
        ).value
    } else {
        1f
    }

    val mau = when {
        phien.trangThai is TrangThaiGiongNoi.Loi -> SH.Red
        dangTraLoi -> SH.Purple
        dangNghe -> SH.Green
        phien.dangChay -> SH.Amber
        else -> SH.Blue
    }

    Box(
        Modifier
            .scale(nhip)
            .size(56.dp)
            .clip(CircleShape)
            .background(mau)
            .clickable(onClick = onBatTat),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = when {
                phien.trangThai is TrangThaiGiongNoi.Loi -> Icons.Filled.ErrorOutline
                phien.dangChay -> Icons.Filled.Mic
                else -> Icons.Filled.MicOff
            },
            contentDescription = if (phien.dangChay) "Tắt trợ lý giọng nói" else "Bật trợ lý giọng nói",
            tint = Color.White,
            modifier = Modifier.size(26.dp),
        )
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
                modifier = Modifier.width(220.dp),
            )
            Box(Modifier.width(60.dp)) {
                if (phien.dangChay) {
                    Text(
                        text = dongHo(phien.giayDaChay),
                        style = SHType.Caption,
                        color = SH.TextOff,
                    )
                }
            }
            Icon(
                Icons.Filled.Close,
                contentDescription = "Đóng bảng thoại",
                tint = SH.TextDim,
                modifier = Modifier.size(18.dp).clickable(onClick = onDong),
            )
        }

        if (phien.banSongCong && phien.dangChay) {
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
