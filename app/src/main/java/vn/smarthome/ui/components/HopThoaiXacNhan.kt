package vn.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType

/**
 * Yêu cầu xác nhận trước khi thực thi một thao tác nhạy cảm lên thiết bị thật
 * (bơm nước AquaControl, công tắc thật, khóa cửa, bếp từ, cổng sạc CUKTECH...).
 */
data class YeuCauXacNhan(
    val tieuDe: String,
    val noiDung: String,
    val nhanXacNhan: String = "Xác nhận",
    val nguyHiem: Boolean = false,
    val hanhDong: () -> Unit,
)

@Composable
fun HopThoaiXacNhan(
    yeuCau: YeuCauXacNhan,
    onXacNhan: () -> Unit,
    onHuy: () -> Unit,
) {
    val accent = if (yeuCau.nguyHiem) SH.Red else SH.Amber
    val confirmColor = if (yeuCau.nguyHiem) SH.Red else SH.Blue

    Dialog(onDismissRequest = onHuy) {
        Surface(
            modifier = Modifier.width(420.dp),
            shape = RoundedCornerShape(Dim.CardRadius),
            color = SH.Card,
            border = BorderStroke(Dim.BorderWidth, accent.copy(alpha = 0.55f)),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.Warning, accent, size = 38.dp, iconSize = 20.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = yeuCau.tieuDe,
                        style = SHType.CardTitle,
                        color = SH.Text,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = yeuCau.noiDung,
                    style = SHType.Body,
                    color = SH.TextDim,
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = onHuy,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SH.TextDim),
                        border = BorderStroke(1.dp, SH.Border),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Hủy", style = SHType.Button, color = SH.TextDim)
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = onXacNhan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = confirmColor,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = yeuCau.nhanXacNhan,
                            style = SHType.Button,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}
