package vn.smarthome.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import vn.smarthome.data.bomnuoc.BomNuocState
import vn.smarthome.data.bomnuoc.CauHinhBomNuoc
import vn.smarthome.data.bomnuoc.KenhBomNuoc
import vn.smarthome.data.bomnuoc.LoiBomNuoc
import vn.smarthome.data.bomnuoc.LuongNuocThang
import vn.smarthome.data.bomnuoc.MucNhatKy
import vn.smarthome.data.bomnuoc.NhomNhatKy
import vn.smarthome.data.bomnuoc.QuanLyBomNuoc
import vn.smarthome.data.bomnuoc.SuKienBom
import vn.smarthome.data.bomnuoc.TrangThaiKetNoiBom
import vn.smarthome.data.bomnuoc.coTheDieuKhienBom
import vn.smarthome.ui.components.BieuDoNuocThang
import vn.smarthome.ui.components.BoChonCheDo
import vn.smarthome.ui.components.CongTacBom
import vn.smarthome.ui.components.DongThongSoBom
import vn.smarthome.ui.components.DongTrangThaiBom
import vn.smarthome.ui.components.Dot
import vn.smarthome.ui.components.EmptyState
import vn.smarthome.ui.components.IconBadge
import vn.smarthome.ui.components.NutHanhDongBom
import vn.smarthome.ui.components.SmartCard
import vn.smarthome.ui.components.SoDoBonNuoc
import vn.smarthome.ui.components.StatusPill
import vn.smarthome.ui.components.TrangThaiKetNoiBomPill
import vn.smarthome.ui.components.VongMucNuoc
import vn.smarthome.ui.components.catSoThapPhan
import vn.smarthome.ui.components.dinhDangThoiGianChay
import vn.smarthome.ui.shell.TopBar
import vn.smarthome.ui.theme.Dim
import vn.smarthome.ui.theme.SH
import vn.smarthome.ui.theme.SHType
import vn.smarthome.ui.theme.tint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Man Tu dong hoa.
 *
 * Tich hop truc tiep cac chuc nang chinh cua AquaControl (dieu khien bom
 * len bon, bom tang ap, giam sat cam bien that, canh bao & xoa loi, cai dat
 * bao ve & hieu chuan, nhat ky hoat dong) cung cac kich ban tu dong hoa.
 */
@Composable
fun AutomationScreen(
    onBack: () -> Unit,
    onNotice: (String) -> Unit,
    bomNuoc: BomNuocState? = null,
    luongNuocThang: List<LuongNuocThang> = emptyList(),
    ketNoiBom: TrangThaiKetNoiBom = TrangThaiKetNoiBom.ChuaCauHinh,
    kenhBom: KenhBomNuoc = KenhBomNuoc.KHONG,
    choBomNuoc: Set<String> = emptySet(),
    cauHinhBom: CauHinhBomNuoc = CauHinhBomNuoc(),
    nhatKyBom: List<SuKienBom> = emptyList(),
    onDatCheDoBon: (String) -> Unit = {},
    onDatBomTangAp: (Boolean) -> Unit = {},
    onDatDenPhuBom: (Boolean) -> Unit = {},
    onXoaLoiBom: () -> Unit = {},
    onTatCoiBom: () -> Unit = {},
    onThuCoiBom: () -> Unit = {},
    onLuuBaoVeBom: (Int, Double, Int, Double, Double) -> Unit = { _, _, _, _, _ -> },
    onGhiMocCanBom: () -> Unit = {},
    onGhiMocDayBom: () -> Unit = {},
    onDatLaiDemLuuLuong: () -> Unit = {},
    onXoaLichSuNuocTrenBo: () -> Unit = {},
    onXoaNhatKyBomCucBo: () -> Unit = {},
    onLuuCauHinhBom: (CauHinhBomNuoc) -> Unit = {},
    initialTab: Int = 0,
) {
    val context = LocalContext.current
    val installed = remember { isAquaControlInstalled(context) }
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showConfigDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TopBar(title = "Tự động hóa", onBack = null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Card)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                AutoTabButton("Bơm nước", selectedTab == 0) { selectedTab = 0 }
                AutoTabButton("Bảo vệ", selectedTab == 1) { selectedTab = 1 }
                AutoTabButton("Nhật ký", selectedTab == 2) { selectedTab = 2 }
                AutoTabButton("Kịch bản", selectedTab == 3) { selectedTab = 3 }
            }

            Spacer(Modifier.width(6.dp))
            TrangThaiKetNoiBomPill(ketNoiBom, kenhBom)

            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Card2)
                    .clickable { showConfigDialog = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Cấu hình bo bơm",
                    tint = SH.TextDim,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val canControlBom = coTheDieuKhienBom(bomNuoc, ketNoiBom)

        Box(Modifier.fillMaxWidth().weight(1f)) {
            when (selectedTab) {
                0 -> TabBomNuoc(
                    bomNuoc = bomNuoc,
                    ketNoiBom = ketNoiBom,
                    choBomNuoc = choBomNuoc,
                    cauHinhBom = cauHinhBom,
                    installed = installed,
                    onDatCheDoBon = onDatCheDoBon,
                    onDatBomTangAp = onDatBomTangAp,
                    onDatDenPhuBom = onDatDenPhuBom,
                    onXoaLoiBom = onXoaLoiBom,
                    onTatCoiBom = onTatCoiBom,
                    onMoCauHinh = { showConfigDialog = true },
                    onMoAquaApp = { launchAquaControl(context, onNotice) },
                )

                1 -> TabBaoVeHieuChuan(
                    bomNuoc = bomNuoc,
                    ketNoiBom = ketNoiBom,
                    choBomNuoc = choBomNuoc,
                    onLuuBaoVeBom = onLuuBaoVeBom,
                    onGhiMocCanBom = onGhiMocCanBom,
                    onGhiMocDayBom = onGhiMocDayBom,
                    onDatLaiDemLuuLuong = onDatLaiDemLuuLuong,
                    onTatCoiBom = onTatCoiBom,
                    onThuCoiBom = onThuCoiBom,
                )

                2 -> TabNhatKyNuoc(
                    nhatKy = nhatKyBom,
                    usage = luongNuocThang,
                    canControl = canControlBom,
                    choBomNuoc = choBomNuoc,
                    onXoaNhatKyCucBo = onXoaNhatKyBomCucBo,
                    onXoaLichSuBo = onXoaLichSuNuocTrenBo,
                )

                else -> TabKichBan(
                    bomNuoc = bomNuoc,
                    ketNoiBom = ketNoiBom,
                    cauHinhBom = cauHinhBom,
                    installed = installed,
                    onMoTabBom = { selectedTab = 0 },
                    onMoAquaApp = { launchAquaControl(context, onNotice) },
                    onNotice = onNotice,
                )
            }
        }
    }

    if (showConfigDialog) {
        HopThoaiCauHinhBom(
            cauHinh = cauHinhBom,
            onLuu = {
                onLuuCauHinhBom(it)
                showConfigDialog = false
            },
            onDong = { showConfigDialog = false },
        )
    }
}

@Composable
private fun AutoTabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) SH.Blue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SHType.Caption,
            color = if (selected) Color.White else SH.TextDim,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

// =====================================================================
//  TAB 0: BOM NUOC - TONG QUAN & DIEU KHIEN TRUC TIEP
// =====================================================================

@Composable
private fun TabBomNuoc(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    choBomNuoc: Set<String>,
    cauHinhBom: CauHinhBomNuoc,
    installed: Boolean,
    onDatCheDoBon: (String) -> Unit,
    onDatBomTangAp: (Boolean) -> Unit,
    onDatDenPhuBom: (Boolean) -> Unit,
    onXoaLoiBom: () -> Unit,
    onTatCoiBom: () -> Unit,
    onMoCauHinh: () -> Unit,
    onMoAquaApp: () -> Unit,
) {
    if (!cauHinhBom.daCauHinh && bomNuoc == null) {
        SmartCard(Modifier.fillMaxSize(), padding = 20.dp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EmptyState(
                        icon = Icons.Filled.WaterDrop,
                        title = "Chưa cấu hình kết nối bo bơm nước",
                        detail = "Khai báo địa chỉ IP/hostname trong LAN (1s) hoặc EMQX broker (30s) để điều khiển trực tiếp.",
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onMoCauHinh,
                            colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Icon(Icons.Filled.Settings, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("Cấu hình kết nối bo bơm", style = SHType.Button, color = Color.White)
                        }
                        if (installed) {
                            OutlinedButton(
                                onClick = onMoAquaApp,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = SH.Cyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mở ứng dụng AquaControl", style = SHType.Button, color = SH.Cyan)
                            }
                        }
                    }
                }
            }
        }
        return
    }

    val fault = LoiBomNuoc.phanTich(bomNuoc?.fault)
    val locked = bomNuoc?.dangKhoa == true || bomNuoc?.bomApDangKhoa == true
    val canControl = coTheDieuKhienBom(bomNuoc, ketNoiBom)

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (fault != null || locked) {
            BangCanhBaoBom(
                fault = fault,
                locked = locked,
                buzzerOn = bomNuoc?.coiDangKeu == true,
                clearing = "clear" in choBomNuoc,
                enabled = canControl,
                mutingBuzzer = "coiTat" in choBomNuoc,
                onXoaLoi = onXoaLoiBom,
                onTatCoi = onTatCoiBom,
            )
        }

        Row(
            Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            // ---------------- COT TRAI: SO DO + MUC NUOC + THONG SO ----------------
            Column(
                Modifier.weight(1.16f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TheSoDoHeThong(
                    bomNuoc = bomNuoc,
                    ketNoiBom = ketNoiBom,
                    modifier = Modifier.fillMaxWidth().weight(1.06f),
                )

                Row(
                    Modifier.fillMaxWidth().weight(0.94f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TheMucNuocBon(
                        bomNuoc = bomNuoc,
                        modifier = Modifier.weight(0.90f).fillMaxHeight(),
                    )
                    TheThongSoCamBien(
                        bomNuoc = bomNuoc,
                        modifier = Modifier.weight(1.10f).fillMaxHeight(),
                    )
                }
            }

            // ---------------- COT PHAI: DIEU KHIEN BOM CHINH + TANG AP ----------------
            Column(
                Modifier.weight(0.84f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TheDieuKhienBomChinh(
                    bomNuoc = bomNuoc,
                    ketNoiBom = ketNoiBom,
                    busy = "tank" in choBomNuoc,
                    onDatCheDo = onDatCheDoBon,
                    modifier = Modifier.fillMaxWidth(),
                )

                TheBomTangApVaPhu(
                    bomNuoc = bomNuoc,
                    canControl = canControl,
                    choBomNuoc = choBomNuoc,
                    onDatBomTangAp = onDatBomTangAp,
                    onDatDenPhuBom = onDatDenPhuBom,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BangCanhBaoBom(
    fault: LoiBomNuoc.ChiTietLoi?,
    locked: Boolean,
    buzzerOn: Boolean,
    clearing: Boolean,
    enabled: Boolean,
    mutingBuzzer: Boolean,
    onXoaLoi: () -> Unit,
    onTatCoi: () -> Unit,
) {
    val isDanger = fault?.mucDo == LoiBomNuoc.MucDo.NGUY_HIEM
    val mau = if (isDanger) SH.Red else SH.Amber
    val msg = fault?.noiDung ?: "Hệ thống bơm đang khoá bảo vệ sau sự cố trước đó."

    SmartCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = mau,
        padding = 9.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (isDanger) Icons.Filled.Error else Icons.Filled.Warning,
                mau, size = 28.dp, iconSize = 16.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                msg,
                style = SHType.BodyMed,
                color = SH.Text,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (buzzerOn) {
                Spacer(Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onTatCoi,
                    enabled = enabled && !mutingBuzzer,
                    modifier = Modifier
                        .height(34.dp)
                        .alpha(
                            when {
                                !enabled -> 0.40f
                                mutingBuzzer -> 0.75f
                                else -> 1f
                            }
                        ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = SH.Amber, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tắt còi", style = SHType.Caption, color = SH.Amber)
                }
            }
            if (fault?.xoaDuoc != false || locked) {
                Spacer(Modifier.width(6.dp))
                NutHanhDongBom(
                    text = "Xoá lỗi",
                    icon = Icons.Filled.LockOpen,
                    color = SH.Blue,
                    onClick = onXoaLoi,
                    enabled = enabled,
                    loading = clearing,
                    height = 34.dp,
                )
            }
        }
    }
}

@Composable
private fun TheSoDoHeThong(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    modifier: Modifier = Modifier,
) {
    val (tongThe, mauTong) = when {
        bomNuoc == null -> "Chờ số liệu" to SH.TextOff
        bomNuoc.coLoi -> "Có cảnh báo" to SH.Red
        ketNoiBom !is TrangThaiKetNoiBom.TrucTiep -> "Mất liên lạc" to SH.Red
        else -> "Hoạt động bình thường" to SH.Green
    }

    SmartCard(modifier, padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.WaterDrop, SH.Cyan, size = 26.dp, iconSize = 15.dp)
            Spacer(Modifier.width(8.dp))
            Text("Trạng thái hệ thống", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f))
            StatusPill(tongThe, mauTong)
        }
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(
                Modifier.weight(0.46f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                if (bomNuoc == null) {
                    Text(ketNoiBom.nhan, style = SHType.Caption, color = SH.TextDim, maxLines = 3)
                } else {
                    DongTrangThaiBom(
                        Icons.Filled.Power, "Bơm chính",
                        if (bomNuoc.bomChinhDangChay) "ĐANG CHẠY" else "TẮT",
                        if (bomNuoc.bomChinhDangChay) SH.Green else SH.TextOff,
                    )
                    DongTrangThaiBom(
                        Icons.Filled.Speed, "Bơm áp",
                        when {
                            bomNuoc.bomApDangKhoa -> "ĐANG KHOÁ"
                            bomNuoc.bomApDangChay -> "ĐANG CHẠY"
                            else -> "TẮT"
                        },
                        when {
                            bomNuoc.bomApDangKhoa -> SH.Red
                            bomNuoc.bomApDangChay -> SH.Green
                            else -> SH.TextOff
                        },
                    )
                    DongTrangThaiBom(
                        Icons.Filled.Waves, "Phao bồn",
                        if (bomNuoc.bonDay) "BÁO ĐẦY" else "CHƯA ĐẦY",
                        if (bomNuoc.bonDay) SH.Green else SH.TextDim,
                    )
                    if (bomNuoc.coNhanhNuocMay) {
                        DongTrangThaiBom(
                            Icons.Filled.WaterDrop, "Nước máy",
                            if (bomNuoc.nuocMayDangChay) "ĐANG VÀO" else "KHÔNG DÒNG",
                            if (bomNuoc.nuocMayDangChay) SH.Cyan else SH.TextOff,
                        )
                    }
                    DongTrangThaiBom(
                        Icons.Filled.Settings, "Chế độ bồn",
                        bomNuoc.cheDoBon.nhan.uppercase(),
                        SH.Blue,
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Box(Modifier.weight(0.54f).fillMaxHeight()) {
                SoDoBonNuoc(
                    levelPercent = bomNuoc?.mucNuocPhanTram,
                    tankPumpRunning = bomNuoc?.bomChinhDangChay == true,
                    boostPumpRunning = bomNuoc?.bomApDangChay == true,
                    cityFlowing = bomNuoc?.nuocMayDangChay == true,
                    tankFull = bomNuoc?.bonDay == true,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun TheMucNuocBon(bomNuoc: BomNuocState?, modifier: Modifier = Modifier) {
    SmartCard(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Waves, SH.Cyan, size = 24.dp, iconSize = 14.dp)
            Spacer(Modifier.width(7.dp))
            Text("Mức nước bồn", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))

        Column(
            Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            VongMucNuoc(
                percent = bomNuoc?.mucNuocPhanTram,
                diameter = 86.dp,
                strokeWidth = 9.dp,
            )
            Spacer(Modifier.height(4.dp))
            val lit = bomNuoc?.mucNuocLit
            if (lit != null) {
                Text(
                    "${lit.toInt()} / ${BomNuocState.DUNG_TICH_BON_LIT} L",
                    style = SHType.BodyMed,
                    color = SH.Text,
                    maxLines = 1,
                )
            } else {
                Text(
                    "Chưa lắp/chưa hiệu chuẩn cảm biến",
                    style = SHType.Caption,
                    color = SH.TextOff,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(7.dp))
                .background(SH.Card2)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Dot(if (bomNuoc?.bonDay == true) SH.Green else SH.TextOff, 7.dp)
            Spacer(Modifier.width(6.dp))
            Text(
                when {
                    bomNuoc == null -> "Phao bồn: —"
                    bomNuoc.bonDay -> "1 phao: Đã đầy"
                    else -> "1 phao: Chưa đầy"
                },
                style = SHType.Caption,
                color = if (bomNuoc?.bonDay == true) SH.Green else SH.TextDim,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TheThongSoCamBien(bomNuoc: BomNuocState?, modifier: Modifier = Modifier) {
    SmartCard(modifier, padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Thermostat, SH.Cyan, size = 24.dp, iconSize = 14.dp)
            Spacer(Modifier.width(7.dp))
            Text("Thông số cảm biến", style = SHType.CardTitle, color = SH.Text, maxLines = 1)
        }
        Spacer(Modifier.height(4.dp))

        if (bomNuoc == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Đang chờ số liệu từ bo…", style = SHType.Caption, color = SH.TextOff)
            }
            return@SmartCard
        }

        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            DongThongSoBom(
                Icons.Filled.Waves, "Lưu lượng bơm",
                bomNuoc.fTank?.let { String.format("%.1f", it) }, "L/phút",
            )
            if (bomNuoc.coNhanhNuocMay) {
                DongThongSoBom(
                    Icons.Filled.WaterDrop, "Lưu lượng nước máy",
                    bomNuoc.fCity?.let { String.format("%.1f", it) }, "L/phút",
                )
            }
            DongThongSoBom(
                Icons.Filled.Thermostat, "Nhiệt độ nước",
                bomNuoc.nhietDoNuoc?.let { String.format("%.1f", it) }, "°C",
                unavailableReason = if (bomNuoc.nhietDoNuoc == null) "không có cảm biến" else null,
            )
            DongThongSoBom(
                Icons.Filled.Bolt, "Nhiệt độ bơm bồn",
                bomNuoc.nhietDoBomBon?.let { String.format("%.1f", it) }, "°C",
                accent = SH.Orange,
                unavailableReason = if (bomNuoc.nhietDoBomBon == null) "không có cảm biến" else null,
            )
            DongThongSoBom(
                Icons.Filled.WaterDrop, "Nước tháng này",
                bomNuoc.useT?.let { String.format("%.0f", it) }, "L",
                accent = SH.Green,
            )
            DongThongSoBom(
                Icons.Filled.Wifi, "RSSI WiFi",
                bomNuoc.wifiRssi?.toString(), "dBm",
                accent = SH.Blue,
                unavailableReason = if (bomNuoc.wifiRssi == null) "mất WiFi" else null,
            )
        }
    }
}

@Composable
private fun TheDieuKhienBomChinh(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    busy: Boolean,
    onDatCheDo: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val canControl = coTheDieuKhienBom(bomNuoc, ketNoiBom)
    val offline = bomNuoc != null && !canControl
    SmartCard(modifier, padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Power, SH.Blue, size = 24.dp, iconSize = 14.dp)
            Spacer(Modifier.width(7.dp))
            Text("Bơm chính (lên bồn)", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f))
            StatusPill(
                if (bomNuoc?.bomChinhDangChay == true) "ĐANG CHẠY" else "TẮT",
                if (bomNuoc?.bomChinhDangChay == true) SH.Green else SH.TextOff,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NutHanhDongBom(
                text = "BẬT",
                icon = Icons.Filled.PlayArrow,
                color = SH.Green,
                onClick = { onDatCheDo("on") },
                modifier = Modifier.weight(1f),
                enabled = canControl,
                loading = busy,
                active = bomNuoc?.tankMode == "on",
                height = 38.dp,
            )
            NutHanhDongBom(
                text = "TẮT",
                icon = Icons.Filled.Stop,
                color = SH.Red,
                onClick = { onDatCheDo("off") },
                modifier = Modifier.weight(1f),
                enabled = canControl,
                loading = busy,
                active = bomNuoc?.tankMode == "off",
                height = 38.dp,
            )
        }

        Spacer(Modifier.height(8.dp))
        Text("Chế độ bồn", style = SHType.Caption, color = SH.TextDim)
        Spacer(Modifier.height(4.dp))
        BoChonCheDo(
            options = listOf("auto" to "AUTO", "on" to "BẬT TAY", "off" to "TẮT TAY"),
            selected = bomNuoc?.tankMode,
            onSelect = onDatCheDo,
            enabled = canControl,
            loading = busy,
        )

        Spacer(Modifier.height(6.dp))
        Text(
            when {
                offline -> "Bo bơm đang mất kết nối (${ketNoiBom.nhan}) — tạm khoá nút điều khiển."
                bomNuoc?.tankMode == "auto" -> "Tự chạy khi chưa đầy, tự ngắt khi phao báo đầy."
                bomNuoc?.tankMode == "on" -> "Chạy liên tục; phao đầy vẫn luôn cắt để chống tràn."
                bomNuoc?.tankMode == "off" -> "Ngừng chạy cho tới khi đổi chế độ."
                else -> "Đang chờ trạng thái từ bo…"
            },
            style = SHType.Caption,
            color = if (offline) SH.Amber else SH.TextOff,
            maxLines = 2,
        )
        if (bomNuoc?.bomChinhDangChay == true && (bomNuoc.tankRun ?: 0L) > 0L) {
            Spacer(Modifier.height(4.dp))
            DongThongSoBom(
                Icons.Filled.Timer, "Đã chạy",
                dinhDangThoiGianChay(bomNuoc.tankRun!!),
                accent = SH.Green,
            )
        }
    }
}

@Composable
private fun TheBomTangApVaPhu(
    bomNuoc: BomNuocState?,
    canControl: Boolean,
    choBomNuoc: Set<String>,
    onDatBomTangAp: (Boolean) -> Unit,
    onDatDenPhuBom: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locked = bomNuoc?.bomApDangKhoa == true || bomNuoc?.dangKhoa == true
    val offline = bomNuoc != null && !canControl

    SmartCard(modifier, padding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Speed, SH.Cyan, size = 24.dp, iconSize = 14.dp)
            Spacer(Modifier.width(7.dp))
            Text("Bơm tăng áp & Phụ trợ", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f))
            StatusPill(
                when {
                    bomNuoc == null -> "—"
                    bomNuoc.bomApDangKhoa -> "ĐANG KHOÁ"
                    bomNuoc.bomApDangChay -> "ĐANG CHẠY"
                    else -> "TẮT"
                },
                when {
                    bomNuoc?.bomApDangKhoa == true -> SH.Red
                    bomNuoc?.bomApDangChay == true -> SH.Green
                    else -> SH.TextOff
                },
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Bơm tăng áp", style = SHType.BodyMed, color = SH.Text)
                Text(
                    when {
                        bomNuoc == null -> "—"
                        bomNuoc.bomApDangKhoa ->
                            if (offline) "Khoá sau sự cố. Chờ bo bơm online để xoá lỗi."
                            else "Khoá sau sự cố. Bấm Xoá lỗi để mở."
                        bomNuoc.dangKhoa ->
                            if (offline) "Bơm chính đang khoá · bo bơm đang offline."
                            else "Bơm chính đang khoá nên bơm áp bị chặn."
                        offline -> "Bo bơm đang mất kết nối — tạm khoá điều khiển."
                        else -> "Chỉ có BẬT/TẮT tay; bo tự cắt nếu chạy quá 15 phút."
                    },
                    style = SHType.Caption,
                    color = when {
                        locked -> SH.Red
                        offline -> SH.Amber
                        else -> SH.TextOff
                    },
                    maxLines = 2,
                )
            }
            Spacer(Modifier.width(8.dp))
            CongTacBom(
                checked = bomNuoc?.bstMode == "on",
                onToggle = onDatBomTangAp,
                enabled = canControl && !locked,
                loading = "booster" in choBomNuoc,
                contentDescription = "Bơm tăng áp",
            )
        }

        if (bomNuoc?.bomApDangChay == true && (bomNuoc.boostRun ?: 0L) > 0L) {
            Spacer(Modifier.height(4.dp))
            DongThongSoBom(
                Icons.Filled.Timer, "Bơm áp đã chạy",
                dinhDangThoiGianChay(bomNuoc.boostRun!!),
                accent = SH.Cyan,
            )
        }

        if (bomNuoc?.coDenPhu == true) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Đèn phụ hộp điện", style = SHType.BodyMed, color = SH.Text)
                    Text("GPIO 16 trên bo bơm", style = SHType.Caption, color = SH.TextOff)
                }
                CongTacBom(
                    checked = bomNuoc.denPhuBat,
                    onToggle = onDatDenPhuBom,
                    enabled = canControl,
                    loading = "led" in choBomNuoc,
                    activeColor = SH.Amber,
                    contentDescription = "Đèn phụ hộp điện",
                )
            }
        }

        Spacer(Modifier.weight(1f))
        bomNuoc?.nhietDoBomAp?.let {
            DongThongSoBom(
                Icons.Filled.Thermostat, "Nhiệt độ bơm áp",
                String.format("%.1f", it), "°C",
                accent = if (it >= 60.0) SH.Red else SH.Orange,
            )
        }
        DongTrangThaiBom(
            if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
            "Khoá bảo vệ",
            when {
                bomNuoc == null -> "—"
                locked -> "ĐANG KHOÁ"
                else -> "MỞ"
            },
            when {
                bomNuoc == null -> SH.TextOff
                locked -> SH.Red
                else -> SH.Green
            },
        )
    }
}

// =====================================================================
//  TAB 1: BAO VE BOM & HIEU CHUAN CAM BIEN
// =====================================================================

@Composable
private fun TabBaoVeHieuChuan(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    choBomNuoc: Set<String>,
    onLuuBaoVeBom: (Int, Double, Int, Double, Double) -> Unit,
    onGhiMocCanBom: () -> Unit,
    onGhiMocDayBom: () -> Unit,
    onDatLaiDemLuuLuong: () -> Unit,
    onTatCoiBom: () -> Unit,
    onThuCoiBom: () -> Unit,
) {
    val canControl = coTheDieuKhienBom(bomNuoc, ketNoiBom)
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // ---------------- COT TRAI: 5 NGUONG BAO VE ----------------
        SmartCard(Modifier.weight(1f).fillMaxHeight(), padding = 12.dp) {
            var dry by remember { mutableStateOf("") }
            var minFlow by remember { mutableStateOf("") }
            var run by remember { mutableStateOf("") }
            var rest by remember { mutableStateOf("") }
            var pulses by remember { mutableStateOf("") }
            var dirty by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(
                bomNuoc?.dryCheck, bomNuoc?.minRest, bomNuoc?.maxRun,
                bomNuoc?.pulses, bomNuoc?.minFlow,
            ) {
                if (!dirty && bomNuoc != null) {
                    dry = bomNuoc.dryCheck?.toString() ?: ""
                    rest = bomNuoc.minRest?.let { catSoThapPhan(it) } ?: ""
                    run = bomNuoc.maxRun?.toString() ?: ""
                    pulses = bomNuoc.pulses?.let { catSoThapPhan(it) } ?: ""
                    minFlow = bomNuoc.minFlow?.let { String.format("%.2f", it) } ?: ""
                }
            }

            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Shield, SH.Orange, size = 26.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Ngưỡng bảo vệ bơm", style = SHType.CardTitle, color = SH.Text)
                }
                Spacer(Modifier.height(8.dp))

                ONhapSo("Thời gian kiểm tra chạy khô (giây)", dry) { dry = it; dirty = true }
                ONhapSo("Ngưỡng chạy khô (L/phút)", minFlow) { minFlow = it; dirty = true }
                ONhapSo("Thời gian chạy tối đa (phút)", run) { run = it; dirty = true }
                ONhapSo("Thời gian nghỉ tối thiểu (phút)", rest) { rest = it; dirty = true }
                ONhapSo("Hiệu chuẩn lưu lượng (xung/lít)", pulses) { pulses = it; dirty = true }

                error?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = SHType.Caption, color = SH.Red)
                }

                Spacer(Modifier.height(8.dp))
                NutHanhDongBom(
                    text = "Lưu cài đặt bảo vệ",
                    icon = Icons.Filled.Save,
                    color = SH.Blue,
                    onClick = {
                        val d = dry.trim().toIntOrNull()
                        val f = minFlow.trim().replace(',', '.').toDoubleOrNull()
                        val m = run.trim().toIntOrNull()
                        val r = rest.trim().replace(',', '.').toDoubleOrNull()
                        val p = pulses.trim().replace(',', '.').toDoubleOrNull()

                        val loi = QuanLyBomNuoc.kiemTraBaoVe(d, f, m, r, p)
                        error = loi
                        if (loi == null) {
                            dirty = false
                            onLuuBaoVeBom(d!!, r!!, m!!, p!!, f!!)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canControl,
                    loading = "protection" in choBomNuoc,
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    "Bo tự cắt bơm khi nhiệt độ bơm đạt 70 °C (ngưỡng cố định trong firmware).",
                    style = SHType.Caption,
                    color = SH.TextOff,
                )
            }
        }

        // ---------------- COT PHAI: HIEU CHUAN & COI ----------------
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dim.CardGap),
        ) {
            SmartCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Straighten, SH.Cyan, size = 26.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Hiệu chuẩn mức nước", style = SHType.CardTitle, color = SH.Text)
                }
                Spacer(Modifier.height(6.dp))
                DongThongSoBom(Icons.Filled.Waves, "Giá trị ADC hiện tại", bomNuoc?.lvlRaw?.toString())
                DongThongSoBom(Icons.Filled.Waves, "Mốc CẠN đã lưu", bomNuoc?.lvlE?.toString())
                DongThongSoBom(Icons.Filled.Waves, "Mốc ĐẦY đã lưu", bomNuoc?.lvlF?.toString())
                Spacer(Modifier.height(4.dp))
                Text(
                    "Ghi mốc CẠN lúc bồn cạn và mốc ĐẦY lúc bồn đầy (cách nhau ≥ 200 đơn vị). Dung tích bồn 1500 L cố định trong firmware.",
                    style = SHType.Caption,
                    color = SH.TextOff,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutHanhDongBom(
                        "Ghi mốc CẠN", Icons.Filled.Straighten, SH.Card2,
                        onClick = onGhiMocCanBom,
                        modifier = Modifier.weight(1f),
                        enabled = canControl,
                        loading = "lvlSetEmpty" in choBomNuoc,
                        height = 38.dp,
                    )
                    NutHanhDongBom(
                        "Ghi mốc ĐẦY", Icons.Filled.Straighten, SH.Card2,
                        onClick = onGhiMocDayBom,
                        modifier = Modifier.weight(1f),
                        enabled = canControl,
                        loading = "lvlSetFull" in choBomNuoc,
                        height = 38.dp,
                    )
                }
            }

            SmartCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Tune, SH.Green, size = 26.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Hiệu chuẩn lưu lượng", style = SHType.CardTitle, color = SH.Text)
                }
                Spacer(Modifier.height(6.dp))
                DongThongSoBom(
                    Icons.Filled.Waves, "Bộ đếm xung",
                    bomNuoc?.calPT?.let { "$it xung" },
                )
                DongThongSoBom(
                    Icons.Filled.Waves, "Quy ra",
                    bomNuoc?.calLT?.let { String.format("%.2f", it) }, "lít",
                )
                Spacer(Modifier.height(6.dp))
                NutHanhDongBom(
                    "Đặt lại bộ đếm", Icons.Filled.Tune, SH.Card2,
                    onClick = onDatLaiDemLuuLuong,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canControl,
                    loading = "calReset" in choBomNuoc,
                    height = 38.dp,
                )
            }

            SmartCard(Modifier.fillMaxWidth(), padding = 12.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.AutoMirrored.Filled.VolumeUp, SH.Amber, size = 26.dp, iconSize = 15.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Còi báo động (GPIO 5)", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f))
                    Text(
                        when (bomNuoc?.coiDangKeu) {
                            true -> "Đang kêu"
                            false -> "Im"
                            null -> "—"
                        },
                        style = SHType.BodyMed,
                        color = if (bomNuoc?.coiDangKeu == true) SH.Red else SH.TextDim,
                    )
                }
                Spacer(Modifier.height(6.dp))
                val mutingBuzzer = "coiTat" in choBomNuoc
                val testingBuzzer = "coiThu" in choBomNuoc
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onTatCoiBom,
                        enabled = canControl && !mutingBuzzer,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(
                                when {
                                    !canControl -> 0.40f
                                    mutingBuzzer -> 0.75f
                                    else -> 1f
                                }
                            ),
                    ) {
                        Text("Tắt còi", style = SHType.Button, color = SH.Amber)
                    }
                    OutlinedButton(
                        onClick = onThuCoiBom,
                        enabled = canControl && !testingBuzzer,
                        modifier = Modifier
                            .weight(1f)
                            .alpha(
                                when {
                                    !canControl -> 0.40f
                                    testingBuzzer -> 0.75f
                                    else -> 1f
                                }
                            ),
                    ) {
                        Text("Kêu thử", style = SHType.Button, color = SH.TextDim)
                    }
                }
            }
        }
    }
}

@Composable
private fun ONhapSo(label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = SHType.Caption, color = SH.TextDim)
        Spacer(Modifier.height(2.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = SHType.Body.copy(color = SH.Text),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SH.Text,
                unfocusedTextColor = SH.Text,
                focusedBorderColor = SH.Blue,
                unfocusedBorderColor = SH.Border,
                focusedContainerColor = SH.Card2,
                unfocusedContainerColor = SH.Card2,
                cursorColor = SH.Cyan,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// =====================================================================
//  TAB 2: NHAT KY SU KIEN (TABLET) + LUONG NUOC THANG (BO)
// =====================================================================

@Composable
private fun TabNhatKyNuoc(
    nhatKy: List<SuKienBom>,
    usage: List<LuongNuocThang>,
    canControl: Boolean,
    choBomNuoc: Set<String>,
    onXoaNhatKyCucBo: () -> Unit,
    onXoaLichSuBo: () -> Unit,
) {
    var filter by remember { mutableStateOf<NhomNhatKy?>(null) }
    val danhSachLoc = remember(nhatKy, filter) {
        if (filter == null) nhatKy else nhatKy.filter { it.nhom == filter }
    }

    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        // ---------------- TRAI: NHAT KY SU KIEN PHIA TABLET ----------------
        SmartCard(Modifier.weight(0.58f).fillMaxHeight(), padding = 12.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.AutoMirrored.Filled.Article, SH.Blue, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.width(8.dp))
                Text("Nhật ký hoạt động", style = SHType.CardTitle, color = SH.Text, modifier = Modifier.weight(1f))
                if (nhatKy.isNotEmpty()) {
                    NutXacNhanHaiBuoc(
                        nhanThuong = "Xoá nhật ký",
                        nhanXacNhan = "Bấm lần nữa để xoá",
                        onXacNhan = onXoaNhatKyCucBo,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SH.Amber.tint(0.12f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Info, null, tint = SH.Amber, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "Nhật ký do máy tính bảng ghi lại (tối đa 100 sự kiện). Bo không lưu lịch sử nên lúc app không chạy sẽ bị trống.",
                    style = SHType.Caption,
                    color = SH.TextDim,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipLoc("Tất cả", filter == null) { filter = null }
                NhomNhatKy.entries.forEach { nhom ->
                    ChipLoc(nhom.nhan, filter == nhom) { filter = nhom }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (danhSachLoc.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.AutoMirrored.Filled.Article,
                        title = "Chưa có sự kiện nào",
                        detail = "Nhật ký sẽ tự ghi khi bơm, phao hoặc cảnh báo thay đổi.",
                    )
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(danhSachLoc, key = { it.id }) { e ->
                        DongSuKienBom(e)
                    }
                }
            }
        }

        // ---------------- PHAI: LUONG NUOC 12 THANG THAT TU BO ----------------
        SmartCard(Modifier.weight(0.42f).fillMaxHeight(), padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.BarChart, SH.Cyan, size = 26.dp, iconSize = 15.dp)
                Spacer(Modifier.width(8.dp))
                Text("Lượng nước theo tháng", style = SHType.CardTitle, color = SH.Text)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Số liệu thật do bo ghi trong bộ nhớ (tối đa 12 tháng).",
                style = SHType.Caption,
                color = SH.TextOff,
            )
            Spacer(Modifier.height(8.dp))

            if (usage.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Filled.BarChart,
                        title = "Chưa có số liệu tháng nào",
                        detail = "Bo sẽ ghi khi có nước chảy qua cảm biến.",
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChuThichMau(SH.Cyan, "Bơm lên bồn")
                    if (usage.any { it.nuocMayLit != null }) {
                        ChuThichMau(SH.Green, "Nước máy")
                    }
                }
                Spacer(Modifier.height(8.dp))
                BieuDoNuocThang(usage, maxMonths = 6, modifier = Modifier.fillMaxWidth().weight(1f))
                Spacer(Modifier.height(8.dp))

                usage.take(4).forEach { m ->
                    Row(Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (m.laThangChuaRo) "Chưa rõ tháng" else m.thang,
                            style = SHType.Caption,
                            color = SH.TextDim,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${m.bomLit.toInt()} L bơm", style = SHType.Caption, color = SH.Cyan)
                        m.nuocMayLit?.let {
                            Spacer(Modifier.width(8.dp))
                            Text("${it.toInt()} L máy", style = SHType.Caption, color = SH.Green)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                NutXacNhanHaiBuoc(
                    nhanThuong = "Xoá lịch sử trên bo",
                    nhanXacNhan = "Bấm lần nữa để xoá hẳn",
                    enabled = canControl && "usageClear" !in choBomNuoc,
                    onXacNhan = onXoaLichSuBo,
                )
            }
        }
    }
}

@Composable
private fun ChipLoc(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) SH.Blue else SH.Card2)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = SHType.Caption, color = if (selected) Color.White else SH.TextDim)
    }
}

@Composable
private fun DongSuKienBom(e: SuKienBom) {
    val color = when (e.muc) {
        MucNhatKy.LOI -> SH.Red
        MucNhatKy.CANH_BAO -> SH.Amber
        MucNhatKy.THONG_TIN -> SH.Green
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SH.Card2)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 4.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(e.tieuDe, style = SHType.BodyMed, color = SH.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(e.chiTiet, style = SHType.Caption, color = SH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        Text(nhatKyFmt.format(Date(e.luc)), style = SHType.Caption, color = SH.TextOff)
    }
}

@Composable
private fun NutXacNhanHaiBuoc(
    nhanThuong: String,
    nhanXacNhan: String,
    onXacNhan: () -> Unit,
    enabled: Boolean = true,
) {
    var armed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(enabled) {
        if (!enabled) armed = false
    }

    TextButton(
        onClick = {
            if (armed) {
                armed = false
                onXacNhan()
            } else {
                armed = true
                scope.launch { delay(5_000L); armed = false }
            }
        },
        enabled = enabled,
        modifier = Modifier.alpha(if (enabled) 1f else 0.40f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Icon(
            Icons.Filled.DeleteForever, null,
            tint = if (armed) SH.Red else SH.TextOff,
            modifier = Modifier.size(15.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            if (armed) nhanXacNhan else nhanThuong,
            style = SHType.Caption,
            color = if (armed) SH.Red else SH.TextOff,
        )
    }
}

@Composable
private fun ChuThichMau(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color, 7.dp)
        Spacer(Modifier.width(4.dp))
        Text(label, style = SHType.Caption, color = SH.TextDim)
    }
}

private val nhatKyFmt = SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN"))

// =====================================================================
//  TAB 3: KICH BAN TU DONG HOA & LIEN KET AQUACONTROL
// =====================================================================

@Composable
private fun TabKichBan(
    bomNuoc: BomNuocState?,
    ketNoiBom: TrangThaiKetNoiBom,
    cauHinhBom: CauHinhBomNuoc,
    installed: Boolean,
    onMoTabBom: () -> Unit,
    onMoAquaApp: () -> Unit,
    onNotice: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(Dim.CardGap),
    ) {
        SmartCard(
            modifier = Modifier.weight(1.02f).fillMaxHeight(),
            borderColor = if (installed) SH.Cyan.tint(0.45f) else SH.Border,
            padding = 14.dp,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.WaterDrop, SH.Cyan, size = 36.dp, iconSize = 20.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("AquaControl", style = SHType.PageTitle, color = SH.Text, maxLines = 1)
                    Text("Hệ thống Bơm & Cấp Nước Tự Động", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                }
                StatusPill(
                    if (installed) "Đã cài app rời" else "Tích hợp sẵn",
                    if (installed) SH.Green else SH.Cyan,
                )
            }

            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SH.Card2)
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                AquaFeatureRow(
                    Icons.Filled.WaterDrop, SH.Cyan,
                    "Điều khiển bơm & Sơ đồ trực quan",
                    "Chế độ AUTO/BẬT/TẮT, bơm tăng áp, cảm biến mức 1500L và lưu lượng thật.",
                )
                AquaFeatureRow(
                    Icons.Filled.Bolt, SH.Amber,
                    "Bảo vệ động cơ & Xoá lỗi từ xa",
                    "Giám sát chạy khô, quá giờ, quá nhiệt 70°C, tắt còi và mở khoá ngay trong app.",
                )
                AquaFeatureRow(
                    Icons.Filled.CheckCircle, SH.Green,
                    "Đồng bộ LAN 1s & MQTT 30s",
                    "Tự chọn LAN khi ở nhà, rơi về MQTT khi ra ngoài, hỗ trợ giọng nói Gemini.",
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMoTabBom,
                    modifier = Modifier.weight(1f).height(Dim.ButtonH),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                ) {
                    Text("Mở bảng bơm nước", style = SHType.Button, color = Color.White, maxLines = 1)
                }
                if (installed) {
                    OutlinedButton(
                        onClick = onMoAquaApp,
                        modifier = Modifier.height(Dim.ButtonH),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = SH.Cyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("App gốc", style = SHType.Button, color = SH.Cyan, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Tác vụ đổi WiFi bo ESP32, cài OLED hoặc nạp OTA vẫn dùng ở app gốc vn.aquacontrol hoặc trang web nhúng trên bo.",
                style = SHType.Caption,
                color = SH.TextOff,
                maxLines = 2,
            )
        }

        SmartCard(Modifier.weight(0.98f).fillMaxHeight(), padding = 14.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Filled.AutoAwesome, SH.Amber, size = 36.dp, iconSize = 20.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Kịch bản tự động", style = SHType.PageTitle, color = SH.Text, maxLines = 1)
                    Text("Thiết lập tự động hóa trong nhà", style = SHType.Caption, color = SH.TextDim, maxLines = 1)
                }
            }

            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val (pumpStatus, pumpColor) = when {
                    !cauHinhBom.daCauHinh -> "Chưa cấu hình" to SH.TextOff
                    bomNuoc?.coLoi == true -> "Có cảnh báo" to SH.Red
                    bomNuoc?.bomChinhDangChay == true -> "Đang bơm" to SH.Green
                    ketNoiBom is TrangThaiKetNoiBom.TrucTiep -> {
                        val cheDo = bomNuoc?.cheDoBon?.nhan?.replaceFirstChar { it.uppercase() }
                        (if (cheDo != null) "$cheDo · Online" else "Online") to SH.Cyan
                    }
                    else -> "Offline" to SH.Amber
                }

                AutomationScenarioCard(
                    icon = Icons.Filled.WaterDrop,
                    color = pumpColor,
                    title = "Bơm nước thông minh",
                    desc = "Tự động bơm lên bồn 1500L theo phao & cảm biến chống chạy khô",
                    status = pumpStatus,
                    onClick = onMoTabBom,
                )

                AutomationScenarioCard(
                    icon = Icons.Filled.Bolt,
                    color = SH.Amber,
                    title = "Cảnh báo quá tải điện EVN",
                    desc = "Cảnh báo khi công suất phụ tải toàn nhà vượt 5000W",
                    status = "Đang chạy",
                    onClick = { onNotice("Hệ thống giám sát điện EVN đang hoạt động tự động.") },
                )

                AutomationScenarioCard(
                    icon = Icons.Filled.Nightlight,
                    color = SH.Purple,
                    title = "Chiếu sáng ban đêm",
                    desc = "Tự động bật đèn ban công & sân khi trời tối sau 18:00",
                    status = "Đã lên lịch",
                    onClick = { onNotice("Kịch bản chiếu sáng đã được kích hoạt theo giờ.") },
                )

                AutomationScenarioCard(
                    icon = Icons.Filled.MeetingRoom,
                    color = SH.Teal,
                    title = "Chế độ vắng nhà (Away)",
                    desc = "Tắt toàn bộ máy lạnh, quạt và đèn khi rời khỏi nhà",
                    status = "Sẵn sàng",
                    onClick = { onNotice("Chế độ vắng nhà đã sẵn sàng.") },
                )
            }
        }
    }
}

// =====================================================================
//  HOP THOAI CAU HINH KET NOI BO BOM NHANH
// =====================================================================

@Composable
private fun HopThoaiCauHinhBom(
    cauHinh: CauHinhBomNuoc,
    onLuu: (CauHinhBomNuoc) -> Unit,
    onDong: () -> Unit,
) {
    var espHost by remember(cauHinh.espHost) { mutableStateOf(cauHinh.espHost) }
    var uuTienLan by remember(cauHinh.uuTienLan) { mutableStateOf(cauHinh.uuTienLan) }
    var brokerHost by remember(cauHinh.brokerHost) { mutableStateOf(cauHinh.brokerHost) }
    var brokerPort by remember(cauHinh.brokerPort) { mutableStateOf(cauHinh.brokerPort.toString()) }
    var brokerTls by remember(cauHinh.brokerTls) { mutableStateOf(cauHinh.brokerTls) }
    var brokerUser by remember(cauHinh.brokerUser) { mutableStateOf(cauHinh.brokerUser) }
    var brokerPass by remember(cauHinh.brokerPass) { mutableStateOf(cauHinh.brokerPass) }
    var hienPass by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDong) {
        Surface(
            modifier = Modifier.width(520.dp),
            shape = RoundedCornerShape(Dim.CardRadius),
            color = SH.Card,
            border = BorderStroke(Dim.BorderWidth, SH.Border),
        ) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text("Cấu hình kết nối bo bơm nước", style = SHType.PageTitle, color = SH.Text)
                Text(
                    "Ưu tiên HTTP LAN (1s) khi ở nhà; tự động rơi về MQTT (30s) khi ra ngoài.",
                    style = SHType.Caption,
                    color = SH.TextDim,
                )
                Spacer(Modifier.height(10.dp))

                ONhapDialog("Địa chỉ bo trong LAN (IP hoặc bomnuoc.local)", espHost, { espHost = it }, "vd: 192.168.0.120")
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ưu tiên dùng LAN khi gọi được bo", style = SHType.Body, color = SH.Text, modifier = Modifier.weight(1f))
                    Switch(
                        checked = uuTienLan,
                        onCheckedChange = { uuTienLan = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SH.Green, checkedThumbColor = Color.White),
                    )
                }

                Spacer(Modifier.height(6.dp))
                ONhapDialog("Broker MQTT (EMQX / Mosquitto)", brokerHost, { brokerHost = it }, "vd: abc.emqxsl.com")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.width(120.dp)) {
                        ONhapDialog("Cổng", brokerPort, { brokerPort = it.filter { c -> c.isDigit() }.take(5) }, "8883", numeric = true)
                    }
                    Row(Modifier.weight(1f).padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (brokerTls) "TLS (ssl://)" else "Không TLS (tcp://)", style = SHType.Body, color = SH.Text, modifier = Modifier.weight(1f))
                        Switch(
                            checked = brokerTls,
                            onCheckedChange = { brokerTls = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = SH.Green, checkedThumbColor = Color.White),
                        )
                    }
                }
                ONhapDialog("Tài khoản MQTT", brokerUser, { brokerUser = it }, "")

                OutlinedTextField(
                    value = brokerPass,
                    onValueChange = { brokerPass = it },
                    label = { Text("Mật khẩu MQTT", style = SHType.Caption) },
                    singleLine = true,
                    visualTransformation = if (hienPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { hienPass = !hienPass }) {
                            Icon(
                                if (hienPass) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = SH.TextDim,
                            )
                        }
                    },
                    textStyle = SHType.Body,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                )

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDong, modifier = Modifier.weight(1f)) {
                        Text("Huỷ", style = SHType.Button, color = SH.TextDim)
                    }
                    Button(
                        onClick = {
                            onLuu(
                                CauHinhBomNuoc(
                                    espHost = espHost,
                                    uuTienLan = uuTienLan,
                                    brokerHost = brokerHost,
                                    brokerPort = brokerPort.toIntOrNull() ?: 8883,
                                    brokerTls = brokerTls,
                                    brokerUser = brokerUser,
                                    brokerPass = brokerPass,
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SH.Blue),
                    ) {
                        Text("Lưu và kết nối", style = SHType.Button, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ONhapDialog(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    numeric: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = SHType.Caption) },
        placeholder = { Text(hint, style = SHType.Caption, color = SH.TextOff) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        textStyle = SHType.Body,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    )
}

@Composable
private fun AquaFeatureRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    desc: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = SHType.Body.copy(fontWeight = FontWeight.Medium), color = SH.Text, maxLines = 1)
            Text(desc, style = SHType.Caption, color = SH.TextDim, maxLines = 2)
        }
    }
}

@Composable
private fun AutomationScenarioCard(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String,
    status: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SH.Card2)
            .border(Dim.BorderWidth, SH.Border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, color, size = 32.dp, iconSize = 16.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = SHType.Body.copy(fontWeight = FontWeight.Medium), color = SH.Text, maxLines = 1)
            Text(desc, style = SHType.Caption, color = SH.TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(6.dp))
        StatusPill(status, color)
    }
}

/**
 * Kiem tra ung dung AquaControl da duoc cai tren thiet bi hay chua.
 */
fun isAquaControlInstalled(context: Context): Boolean {
    val pm = context.packageManager
    return pm.getLaunchIntentForPackage("vn.aquacontrol") != null ||
        pm.getLaunchIntentForPackage("vn.aquacontrol.debug") != null
}

/**
 * Khoi chay ung dung AquaControl thong qua Intent.
 */
fun launchAquaControl(context: Context, onNotice: (String) -> Unit) {
    val pm = context.packageManager
    val packages = listOf("vn.aquacontrol", "vn.aquacontrol.debug")
    for (pkg in packages) {
        val launchIntent = pm.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(launchIntent)
                return
            } catch (e: Exception) {
                onNotice("Lỗi khi mở AquaControl: ${e.message}")
                return
            }
        }
    }
    onNotice("Không tìm thấy ứng dụng AquaControl (vn.aquacontrol) trên máy tính bảng.")
}
