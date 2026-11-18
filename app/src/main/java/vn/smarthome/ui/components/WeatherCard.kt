package vn.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import vn.smarthome.data.thoitiet.NgayThoiTiet
import vn.smarthome.data.thoitiet.OpenMeteo
import vn.smarthome.data.thoitiet.ThoiTietData
import vn.smarthome.data.thoitiet.ThoiTietUi
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint

/**
 * The thoi tiet o Trang chu - so lieu that tu Open-Meteo.
 *
 * Nhin theo kieu Pixel Weather (goi y tu repo WeatherMaster): nhiet do lon,
 * tinh trang, va mot dai du bao vai ngay. KHONG co so mau: khi dang tai thi
 * quay vong, khi hong thi bao loi ro + nut thu lai, con khi lay duoc thi hien
 * so that. Tai lai luc hong van giu so cu (xem HomeViewModel.taiThoiTiet).
 */
@Composable
fun WeatherCard(
    ui: ThoiTietUi,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmartCard(modifier = modifier, padding = 12.dp) {
        val diaDiem = (ui as? ThoiTietUi.Co)?.data?.diaDiem ?: OpenMeteo.TEN_DIA_DIEM
        val phu = when (ui) {
            is ThoiTietUi.Co -> "Cập nhật " + ui.data.gioCapNhat
            ThoiTietUi.DangTai -> "Đang cập nhật..."
            is ThoiTietUi.Loi -> "Chưa cập nhật được"
        }

        // ---------- Dau the: dia diem + nut tai lai ----------
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.LocationOn, SH.Blue, size = 28.dp, iconSize = 16.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(diaDiem, style = SHType.CardTitle, color = SH.Text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(phu, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
            }
            Box(
                Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(SH.Card2)
                    .clickable(onClick = onRefresh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, "Tải lại thời tiết",
                    tint = SH.TextDim, modifier = Modifier.size(17.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        when (ui) {
            ThoiTietUi.DangTai -> DangTai(Modifier.weight(1f).fillMaxWidth())
            is ThoiTietUi.Loi -> Loi(ui.moTa, onRefresh, Modifier.weight(1f).fillMaxWidth())
            is ThoiTietUi.Co -> NoiDung(ui.data, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun DangTai(modifier: Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = SH.Blue, modifier = Modifier.size(28.dp))
        Text("Đang tải thời tiết...", style = SHType.Caption, color = SH.TextDim)
    }
}

@Composable
private fun Loi(moTa: String, onRefresh: () -> Unit, modifier: Modifier) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(Icons.Filled.CloudOff, SH.TextOff, size = 44.dp, iconSize = 24.dp,
            shape = CircleShape)
        Text("Không lấy được thời tiết", style = SHType.BodyMed, color = SH.TextDim,
            maxLines = 1)
        Text(moTa, style = SHType.Caption, color = SH.TextOff, textAlign = TextAlign.Center,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Box(
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(SH.Blue.tint(0.16f))
                .clickable(onClick = onRefresh)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text("Thử lại", style = SHType.Button, color = SH.Blue, maxLines = 1)
        }
    }
}

@Composable
private fun NoiDung(data: ThoiTietData, modifier: Modifier) {
    Column(modifier) {
        val mt = moTaThoiTiet(data.hienTai.maWmo, data.hienTai.banNgay)

        // ---------- Khoi hien tai ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(mt.icon, null, tint = mt.mau, modifier = Modifier.size(54.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(data.hienTai.nhietDo.toString(), style = SHType.ValueBig, color = SH.Text,
                        maxLines = 1)
                    Text("°C", style = SHType.Body, color = SH.TextDim,
                        modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
                }
                Text(mt.nhan, style = SHType.BodyMed, color = mt.mau, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- Ba o nho: cam giac / do am / gio ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(Icons.Filled.Thermostat, "Cảm giác", data.hienTai.camGiac.toString() + "°",
                SH.Red, Modifier.weight(1f))
            MiniStat(Icons.Filled.WaterDrop, "Độ ẩm", data.hienTai.doAm.toString() + "%",
                SH.Cyan, Modifier.weight(1f))
            MiniStat(Icons.Filled.Air, "Gió", data.hienTai.gio.toString() + " km/h",
                SH.Blue, Modifier.weight(1f))
        }

        Spacer(Modifier.weight(1f))

        Box(Modifier.fillMaxWidth().height(1.dp).background(SH.Border))
        Spacer(Modifier.height(8.dp))

        // ---------- Du bao vai ngay ----------
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            data.cacNgay.forEach { ngay ->
                NgayCol(ngay, Modifier.weight(1f))
            }
        }
    }
}

/** O so lieu nho trong the thoi tiet. */
@Composable
private fun MiniStat(
    icon: ImageVector,
    nhan: String,
    giaTri: String,
    mau: Color,
    modifier: Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card2)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, null, tint = mau, modifier = Modifier.size(16.dp))
        Text(giaTri, style = SHType.Label, color = SH.Text, maxLines = 1)
        Text(nhan, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
    }
}

/** Mot cot trong dai du bao: thu / icon / cao-thap. */
@Composable
private fun NgayCol(ngay: NgayThoiTiet, modifier: Modifier) {
    val mt = moTaThoiTiet(ngay.maWmo, banNgay = true)
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ngay.thu, style = SHType.Caption, color = SH.TextDim, maxLines = 1)
        Icon(mt.icon, null, tint = mt.mau, modifier = Modifier.size(20.dp))
        Row {
            Text(ngay.caoNhat.toString() + "°", style = SHType.Label, color = SH.Text, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            Text(ngay.thapNhat.toString() + "°", style = SHType.Label, color = SH.TextOff,
                maxLines = 1)
        }
    }
}

private data class MoTa(val icon: ImageVector, val nhan: String, val mau: Color)

/**
 * Doi ma thoi tiet WMO sang icon + chu + mau.
 *
 * Bang ma WMO la chuan Open-Meteo tra ve (0 = quang, 95 = dong...). Gom nhom
 * cho gon; ban ngay/ban dem chi doi o troi quang/it may (mat troi <-> mat
 * trang).
 */
private fun moTaThoiTiet(ma: Int, banNgay: Boolean): MoTa = when (ma) {
    0 -> if (banNgay) MoTa(Icons.Filled.WbSunny, "Trời quang", SH.Amber)
         else MoTa(Icons.Filled.NightsStay, "Trời quang", SH.Blue)
    1 -> if (banNgay) MoTa(Icons.Filled.WbSunny, "Nắng nhẹ", SH.Amber)
         else MoTa(Icons.Filled.NightsStay, "Ít mây", SH.Blue)
    2 -> MoTa(Icons.Filled.WbCloudy, "Có mây", SH.Cyan)
    3 -> MoTa(Icons.Filled.Cloud, "Nhiều mây", SH.TextDim)
    45, 48 -> MoTa(Icons.Filled.Cloud, "Sương mù", SH.Cyan)
    51, 53, 55 -> MoTa(Icons.Filled.Grain, "Mưa phùn", SH.Cyan)
    56, 57 -> MoTa(Icons.Filled.Grain, "Mưa phùn lạnh", SH.Cyan)
    61, 63, 65 -> MoTa(Icons.Filled.Umbrella, "Mưa", SH.Blue)
    66, 67 -> MoTa(Icons.Filled.Umbrella, "Mưa lạnh", SH.Blue)
    71, 73, 75 -> MoTa(Icons.Filled.AcUnit, "Tuyết", SH.Cyan)
    77 -> MoTa(Icons.Filled.AcUnit, "Hạt tuyết", SH.Cyan)
    80, 81, 82 -> MoTa(Icons.Filled.Grain, "Mưa rào", SH.Blue)
    85, 86 -> MoTa(Icons.Filled.AcUnit, "Mưa tuyết", SH.Cyan)
    95 -> MoTa(Icons.Filled.Thunderstorm, "Dông", SH.Purple)
    96, 99 -> MoTa(Icons.Filled.Thunderstorm, "Dông, mưa đá", SH.Purple)
    else -> MoTa(Icons.Filled.Cloud, "Không rõ", SH.TextDim)
}
