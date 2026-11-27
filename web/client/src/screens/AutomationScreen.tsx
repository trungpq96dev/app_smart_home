import React, { useEffect, useMemo, useState } from 'react';
import {
  AlertCircle,
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Droplets,
  ExternalLink,
  FileText,
  Gauge,
  Info,
  Lock,
  LockOpen,
  Moon,
  Play,
  Power,
  Radio,
  Ruler,
  Save,
  Settings,
  Shield,
  ShieldAlert,
  SlidersHorizontal,
  Square,
  Sun,
  Thermometer,
  Timer,
  Volume2,
  VolumeOff,
  Waves,
  Wifi,
  Zap,
} from 'lucide-react';
import {
  BieuDoNuocThang,
  HopThoaiCauHinhBom,
  NutXacNhanHaiBuoc,
  SoDoBonNuoc,
  TrangThaiKetNoiBomPill,
  VongMucNuoc,
} from '../components/BomNuocUi.js';
import {
  ChoiceChip,
  DemoBadge,
  Dot,
  EmptyState,
  FlatButton,
  IconBadge,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { SwitchToggle } from '../components/Tiles.js';
import {
  bomApDangChay,
  bomApDangKhoa,
  bomChinhDangChay,
  bomTrucTuyen,
  bonDay,
  catSoThapPhan,
  coDenPhu,
  coLoiBom,
  coNhanhNuocMay,
  coTheDieuKhienBom,
  coiDangKeu,
  daCauHinhBom,
  dangKhoaBom,
  denPhuBat,
  dinhDangThoiGianChay,
  DUNG_TICH_BON_LIT,
  kiemTraBaoVe,
  laThangChuaRo,
  mucNuocLit,
  mucNuocPhanTram,
  nhanCheDoBon,
  nhanKetNoiBom,
  nhietDoBomAp,
  nhietDoBomBon,
  nhietDoNuoc,
  NHOM_NHAT_KY_LIST,
  nuocMayDangChay,
  phanTichLoiBomNuoc,
  wifiRssi,
  type BomNuocState,
  type NhomNhatKy,
  type SuKienBom,
} from '../data/bomnuoc.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

type AutoTab = 'pump' | 'protection' | 'log' | 'scenarios';

function fmtLogTime(ms: number): string {
  const d = new Date(ms);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  const mo = String(d.getMonth() + 1).padStart(2, '0');
  return `${dd}/${mo} ${hh}:${mm}`;
}

export function AutomationScreen() {
  const {
    config,
    bomNuoc,
    luongNuocThang,
    ketNoiBom,
    kenhBom,
    choBomNuoc,
    nhatKyBom,
    luuBomNuocConfig,
    datCheDoBon,
    datBomTangAp,
    datDenPhuBom,
    xoaLoiBom,
    tatCoiBom,
    thuCoiBom,
    luuBaoVeBom,
    ghiMocCanBom,
    ghiMocDayBom,
    datLaiDemLuuLuong,
    xoaLichSuNuocTrenBo,
    xoaNhatKyBomCucBo,
    requireAdminPin,
    adminLockEnabled,
    adminUnlocked,
    showToast,
  } = useSmartHome();

  const [tab, setTab] = useState<AutoTab>('pump');
  const [configModalOpen, setConfigModalOpen] = useState(false);

  const daCauHinh = daCauHinhBom(config.bomNuoc);
  const hasAquaUrl = config.aquaControlUrl.trim().length > 0;
  const ketNoiDirect = bomTrucTuyen(ketNoiBom);
  const canControlPump = coTheDieuKhienBom(bomNuoc, ketNoiBom);

  const openAquaControl = () => {
    if (!hasAquaUrl) {
      showToast('Chưa cấu hình địa chỉ web AquaControl. Vào Cài đặt → Thông tin & Hệ thống để thêm.', 'warning');
      return;
    }
    window.open(config.aquaControlUrl, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Thanh tab + trang thai ket noi bo bom + nut cau hinh */}
      <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap gap-2">
          <ChoiceChip label="Bơm nước" selected={tab === 'pump'} color={SH.Cyan} onClick={() => setTab('pump')} />
          <ChoiceChip label="Bảo vệ" selected={tab === 'protection'} color={SH.Orange} onClick={() => setTab('protection')} />
          <ChoiceChip label="Nhật ký" selected={tab === 'log'} color={SH.Blue} onClick={() => setTab('log')} />
          <ChoiceChip label="Kịch bản" selected={tab === 'scenarios'} color={SH.Teal} onClick={() => setTab('scenarios')} />
        </div>

        <div className="flex items-center gap-2">
          <TrangThaiKetNoiBomPill ketNoi={ketNoiBom} kenh={kenhBom} />
          <button
            type="button"
            onClick={() => requireAdminPin('Cấu hình kết nối bo bơm nước', () => setConfigModalOpen(true))}
            title="Cấu hình kết nối bo bơm nước"
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#253459] bg-[#16213A] px-2.5 py-1.5 text-xs font-semibold text-[#94A3B8] hover:text-white hover:bg-[#1C2946] cursor-pointer"
          >
            {adminLockEnabled && !adminUnlocked ? <Lock size={13} className="text-[#F59E0B]" /> : <Settings size={14} />}
            <span className="hidden sm:inline">Cấu hình bơm</span>
          </button>
        </div>
      </div>

      {tab === 'pump' && (
        <TabBomNuoc
          daCauHinh={daCauHinh}
          bomNuoc={bomNuoc}
          ketNoiText={nhanKetNoiBom(ketNoiBom, kenhBom)}
          ketNoiDirect={ketNoiDirect}
          canControl={canControlPump}
          choBomNuoc={choBomNuoc}
          hasAquaUrl={hasAquaUrl}
          onDatCheDoBon={(m) => void datCheDoBon(m)}
          onDatBomTangAp={(b) => void datBomTangAp(b)}
          onDatDenPhuBom={(b) => void datDenPhuBom(b)}
          onXoaLoiBom={() => void xoaLoiBom()}
          onTatCoiBom={() => void tatCoiBom()}
          onOpenConfig={() => requireAdminPin('Cấu hình kết nối bo bơm nước', () => setConfigModalOpen(true))}
          onOpenAquaWeb={openAquaControl}
        />
      )}

      {tab === 'protection' && (
        <TabBaoVeHieuChuan
          bomNuoc={bomNuoc}
          canControl={canControlPump}
          choBomNuoc={choBomNuoc}
          onLuuBaoVeBom={(d, r, m, p, f) =>
            requireAdminPin('Lưu ngưỡng bảo vệ bơm', () => void luuBaoVeBom(d, r, m, p, f))
          }
          onGhiMocCan={() =>
            requireAdminPin('Hiệu chuẩn mốc CẠN mức nước', () => void ghiMocCanBom())
          }
          onGhiMocDay={() =>
            requireAdminPin('Hiệu chuẩn mốc ĐẦY mức nước', () => void ghiMocDayBom())
          }
          onDatLaiDemLuuLuong={() =>
            requireAdminPin('Đặt lại bộ đếm hiệu chuẩn lưu lượng', () => void datLaiDemLuuLuong())
          }
          onTatCoi={() => void tatCoiBom()}
          onThuCoi={() => void thuCoiBom()}
        />
      )}

      {tab === 'log' && (
        <TabNhatKyNuoc
          nhatKy={nhatKyBom}
          usage={luongNuocThang}
          canControl={canControlPump}
          clearingUsage={Boolean(choBomNuoc.usageClear)}
          onXoaNhatKyCucBo={() =>
            requireAdminPin('Xóa nhật ký bơm trên Gateway', () => void xoaNhatKyBomCucBo())
          }
          onXoaLichSuBo={() =>
            requireAdminPin('Xóa lịch sử lượng nước trên bo', () => void xoaLichSuNuocTrenBo())
          }
        />
      )}

      {tab === 'scenarios' && (
        <TabKichBan
          daCauHinh={daCauHinh}
          bomNuoc={bomNuoc}
          ketNoiDirect={ketNoiDirect}
          hasAquaUrl={hasAquaUrl}
          aquaUrl={config.aquaControlUrl}
          onOpenPumpTab={() => setTab('pump')}
          onOpenAquaWeb={openAquaControl}
          onShowToast={showToast}
        />
      )}

      <HopThoaiCauHinhBom
        open={configModalOpen}
        config={config.bomNuoc}
        onClose={() => setConfigModalOpen(false)}
        onSave={luuBomNuocConfig}
      />
    </div>
  );
}

// =====================================================================
//  TAB 0: BOM NUOC - TONG QUAN & DIEU KHIEN TRUC TIEP
// =====================================================================

function TabBomNuoc({
  daCauHinh,
  bomNuoc,
  ketNoiText,
  ketNoiDirect,
  canControl,
  choBomNuoc,
  hasAquaUrl,
  onDatCheDoBon,
  onDatBomTangAp,
  onDatDenPhuBom,
  onXoaLoiBom,
  onTatCoiBom,
  onOpenConfig,
  onOpenAquaWeb,
}: {
  daCauHinh: boolean;
  bomNuoc: BomNuocState | null;
  ketNoiText: string;
  ketNoiDirect: boolean;
  canControl: boolean;
  choBomNuoc: Record<string, boolean>;
  hasAquaUrl: boolean;
  onDatCheDoBon: (mode: 'auto' | 'on' | 'off') => void;
  onDatBomTangAp: (bat: boolean) => void;
  onDatDenPhuBom: (bat: boolean) => void;
  onXoaLoiBom: () => void;
  onTatCoiBom: () => void;
  onOpenConfig: () => void;
  onOpenAquaWeb: () => void;
}) {
  if (!daCauHinh && bomNuoc === null) {
    return (
      <div className="flex-1 min-h-0 flex items-center justify-center">
        <EmptyState
          icon={<Droplets size={32} />}
          title="Chưa cấu hình kết nối bo bơm nước"
          description="Khai báo địa chỉ IP/hostname trong LAN (1s) hoặc EMQX broker (30s) để điều khiển trực tiếp hệ bơm nước từ trình duyệt."
          action={
            <div className="flex flex-wrap items-center justify-center gap-2">
              <FlatButton color={SH.Blue} variant="solid" onClick={onOpenConfig}>
                <Settings size={14} />
                <span>Cấu hình kết nối bo bơm</span>
              </FlatButton>
              {hasAquaUrl && (
                <FlatButton color={SH.Cyan} variant="outline" onClick={onOpenAquaWeb}>
                  <ExternalLink size={14} />
                  <span>Mở AquaControl Web</span>
                </FlatButton>
              )}
            </div>
          }
        />
      </div>
    );
  }

  const fault = phanTichLoiBomNuoc(bomNuoc?.fault);
  const locked = bomNuoc ? dangKhoaBom(bomNuoc) || bomApDangKhoa(bomNuoc) : false;
  const buzzerOn = bomNuoc ? coiDangKeu(bomNuoc) === true : false;
  const isDanger = fault?.mucDo === 'NGUY_HIEM';

  const isOffline = bomNuoc !== null && !canControl;
  const offlineTitle = !canControl ? 'Bo bơm đang mất kết nối' : undefined;
  const tankBusy = Boolean(choBomNuoc.tank);
  const boosterBusy = Boolean(choBomNuoc.booster);
  const ledBusy = Boolean(choBomNuoc.led);
  const clearBusy = Boolean(choBomNuoc.clear);

  const pct = bomNuoc ? mucNuocPhanTram(bomNuoc) : null;
  const lit = bomNuoc ? mucNuocLit(bomNuoc) : null;
  const full = bomNuoc ? bonDay(bomNuoc) : false;
  const tankRunning = bomNuoc ? bomChinhDangChay(bomNuoc) : false;
  const boostRunning = bomNuoc ? bomApDangChay(bomNuoc) : false;
  const cityFlow = bomNuoc ? nuocMayDangChay(bomNuoc) : false;
  const nhietNuoc = bomNuoc ? nhietDoNuoc(bomNuoc) : null;
  const nhietBon = bomNuoc ? nhietDoBomBon(bomNuoc) : null;
  const nhietAp = bomNuoc ? nhietDoBomAp(bomNuoc) : null;
  const rssi = bomNuoc ? wifiRssi(bomNuoc) : null;

  const overallPill =
    bomNuoc === null
      ? { label: 'Chờ số liệu', color: SH.TextOff }
      : coLoiBom(bomNuoc)
        ? { label: 'Có cảnh báo', color: SH.Red }
        : !ketNoiDirect
          ? { label: 'Mất liên lạc', color: SH.Red }
          : { label: 'Hoạt động bình thường', color: SH.Green };

  return (
    <div className="flex-1 min-h-0 flex flex-col gap-2.5 lg:overflow-hidden">
      {(fault !== null || locked) && (
        <SmartCard
          className="shrink-0 p-2.5"
          glowColor={isDanger ? SH.Red : SH.Amber}
          style={{ borderColor: isDanger ? SH.Red : SH.Amber }}
        >
          <div className="flex flex-wrap items-center gap-2.5">
            <IconBadge color={isDanger ? SH.Red : SH.Amber} size={30}>
              {isDanger ? <AlertCircle size={16} /> : <AlertTriangle size={16} />}
            </IconBadge>
            <p className="flex-1 min-w-[200px] text-xs font-semibold text-[#F1F5F9]">
              {fault?.noiDung ?? 'Hệ thống bơm đang khoá bảo vệ sau sự cố trước đó.'}
            </p>
            {buzzerOn && (
              <FlatButton
                color={SH.Amber}
                variant="outline"
                onClick={onTatCoiBom}
                disabled={!canControl || Boolean(choBomNuoc.coiTat)}
                title={offlineTitle}
              >
                <VolumeOff size={14} />
                <span>Tắt còi</span>
              </FlatButton>
            )}
            {(fault?.xoaDuoc !== false || locked) && (
              <FlatButton
                color={SH.Blue}
                variant="solid"
                onClick={onXoaLoiBom}
                disabled={!canControl || clearBusy}
                title={offlineTitle}
              >
                <LockOpen size={14} />
                <span>{clearBusy ? 'Đang gửi…' : 'Xoá lỗi'}</span>
              </FlatButton>
            )}
          </div>
        </SmartCard>
      )}

      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5 items-stretch">
        {/* ---------------- COT TRAI: SO DO + MUC NUOC + THONG SO ---------------- */}
        <div className="lg:col-span-7 flex flex-col gap-2.5 min-h-0">
          <SmartCard className="flex-1 min-h-[210px] flex flex-col gap-2 p-3">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <IconBadge color={SH.Cyan} size={28}>
                  <Droplets size={15} />
                </IconBadge>
                <span className="text-xs font-bold text-[#F1F5F9]">Trạng thái hệ thống</span>
              </div>
              <StatusPill label={overallPill.label} color={overallPill.color} />
            </div>

            <div className="flex-1 min-h-0 grid grid-cols-1 sm:grid-cols-12 gap-2 items-center">
              <div className="sm:col-span-5 space-y-1.5 text-xs">
                {bomNuoc === null ? (
                  <p className="text-xs text-[#94A3B8] leading-relaxed">{ketNoiText}</p>
                ) : (
                  <>
                    <StatusRow
                      icon={<Power size={13} />}
                      label="Bơm chính"
                      pill={tankRunning ? 'ĐANG CHẠY' : 'TẮT'}
                      color={tankRunning ? SH.Green : SH.TextOff}
                    />
                    <StatusRow
                      icon={<Gauge size={13} />}
                      label="Bơm áp"
                      pill={bomApDangKhoa(bomNuoc) ? 'ĐANG KHOÁ' : boostRunning ? 'ĐANG CHẠY' : 'TẮT'}
                      color={bomApDangKhoa(bomNuoc) ? SH.Red : boostRunning ? SH.Green : SH.TextOff}
                    />
                    <StatusRow
                      icon={<Waves size={13} />}
                      label="Phao bồn"
                      pill={full ? 'BÁO ĐẦY' : 'CHƯA ĐẦY'}
                      color={full ? SH.Green : SH.TextDim}
                    />
                    {coNhanhNuocMay(bomNuoc) && (
                      <StatusRow
                        icon={<Droplets size={13} />}
                        label="Nước máy"
                        pill={cityFlow ? 'ĐANG VÀO' : 'KHÔNG DÒNG'}
                        color={cityFlow ? SH.Cyan : SH.TextOff}
                      />
                    )}
                    <StatusRow
                      icon={<Settings size={13} />}
                      label="Chế độ bồn"
                      pill={nhanCheDoBon(bomNuoc.tankMode).toUpperCase()}
                      color={SH.Blue}
                    />
                  </>
                )}
              </div>

              <div className="sm:col-span-7 h-full flex items-center justify-center min-h-[160px]">
                <SoDoBonNuoc
                  levelPercent={pct}
                  tankPumpRunning={tankRunning}
                  boostPumpRunning={boostRunning}
                  cityFlowing={cityFlow}
                  tankFull={full}
                />
              </div>
            </div>
          </SmartCard>

          <div className="shrink-0 grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-stretch">
            <SmartCard className="sm:col-span-5 flex flex-col justify-between gap-2 p-3">
              <div className="flex items-center gap-2">
                <IconBadge color={SH.Cyan} size={26}>
                  <Waves size={14} />
                </IconBadge>
                <span className="text-xs font-bold text-[#F1F5F9]">Mức nước bồn</span>
              </div>

              <div className="flex flex-col items-center justify-center gap-1 py-1">
                <VongMucNuoc percent={pct} size={86} strokeWidth={8} />
                <div className="font-mono-num text-xs font-semibold text-[#E6EDF7] mt-0.5 text-center">
                  {lit !== null ? (
                    `${Math.trunc(lit)} / ${DUNG_TICH_BON_LIT} L`
                  ) : (
                    <span className="text-[11px] font-normal text-[#64748B]">Chưa lắp/chưa hiệu chuẩn cảm biến</span>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-2 rounded-lg bg-[#1C2946] px-2.5 py-1.5 text-[11px]">
                <Dot color={full ? SH.Green : SH.TextOff} size={7} />
                <span className={full ? 'text-[#22C55E] font-medium' : 'text-[#94A3B8]'}>
                  {bomNuoc === null ? 'Phao bồn: —' : full ? '1 phao: Đã đầy' : '1 phao: Chưa đầy'}
                </span>
              </div>
            </SmartCard>

            <SmartCard className="sm:col-span-7 flex flex-col justify-between gap-1.5 p-3">
              <div className="flex items-center gap-2">
                <IconBadge color={SH.Cyan} size={26}>
                  <Thermometer size={14} />
                </IconBadge>
                <span className="text-xs font-bold text-[#F1F5F9]">Thông số cảm biến</span>
              </div>

              {bomNuoc === null ? (
                <div className="flex-1 flex items-center justify-center text-xs text-[#64748B]">
                  Đang chờ số liệu từ bo…
                </div>
              ) : (
                <div className="space-y-1.5 my-auto">
                  <MetricRow
                    icon={<Waves size={13} />}
                    label="Lưu lượng bơm"
                    value={bomNuoc.fTank !== null ? bomNuoc.fTank.toFixed(1) : null}
                    unit="L/phút"
                    accent={SH.Cyan}
                  />
                  {coNhanhNuocMay(bomNuoc) && (
                    <MetricRow
                      icon={<Droplets size={13} />}
                      label="Lưu lượng nước máy"
                      value={bomNuoc.fCity !== null ? bomNuoc.fCity.toFixed(1) : null}
                      unit="L/phút"
                      accent={SH.Cyan}
                    />
                  )}
                  <MetricRow
                    icon={<Thermometer size={13} />}
                    label="Nhiệt độ nước"
                    value={nhietNuoc !== null ? nhietNuoc.toFixed(1) : null}
                    unit="°C"
                    accent={SH.Cyan}
                    reason={nhietNuoc === null ? 'không có cảm biến' : undefined}
                  />
                  <MetricRow
                    icon={<Zap size={13} />}
                    label="Nhiệt độ bơm bồn"
                    value={nhietBon !== null ? nhietBon.toFixed(1) : null}
                    unit="°C"
                    accent={SH.Orange}
                    reason={nhietBon === null ? 'không có cảm biến' : undefined}
                  />
                  <MetricRow
                    icon={<Droplets size={13} />}
                    label="Nước tháng này"
                    value={bomNuoc.useT !== null ? String(Math.round(bomNuoc.useT)) : null}
                    unit="L"
                    accent={SH.Green}
                  />
                  <MetricRow
                    icon={<Wifi size={13} />}
                    label="RSSI WiFi"
                    value={rssi !== null ? String(rssi) : null}
                    unit="dBm"
                    accent={SH.Blue}
                    reason={rssi === null ? 'mất WiFi' : undefined}
                  />
                </div>
              )}
            </SmartCard>
          </div>
        </div>

        {/* ---------------- COT PHAI: DIEU KHIEN BOM CHINH + TANG AP ---------------- */}
        <div className="lg:col-span-5 flex flex-col gap-2.5 min-h-0">
          <SmartCard className="shrink-0 flex flex-col gap-2.5 p-3">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <IconBadge color={SH.Blue} size={26}>
                  <Power size={14} />
                </IconBadge>
                <span className="text-xs font-bold text-[#F1F5F9]">Bơm chính (lên bồn)</span>
              </div>
              <StatusPill
                label={tankRunning ? 'ĐANG CHẠY' : 'TẮT'}
                color={tankRunning ? SH.Green : SH.TextOff}
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <FlatButton
                color={SH.Green}
                variant={bomNuoc?.tankMode === 'on' ? 'solid' : 'soft'}
                disabled={!canControl || tankBusy}
                title={offlineTitle}
                onClick={() => onDatCheDoBon('on')}
              >
                <Play size={14} />
                <span>{tankBusy ? 'Đang gửi…' : 'BẬT'}</span>
              </FlatButton>
              <FlatButton
                color={SH.Red}
                variant={bomNuoc?.tankMode === 'off' ? 'solid' : 'soft'}
                disabled={!canControl || tankBusy}
                title={offlineTitle}
                onClick={() => onDatCheDoBon('off')}
              >
                <Square size={14} />
                <span>{tankBusy ? 'Đang gửi…' : 'TẮT'}</span>
              </FlatButton>
            </div>

            <div>
              <div className="text-[11px] text-[#94A3B8] mb-1.5">Chế độ bồn</div>
              <div className="grid grid-cols-3 gap-1.5">
                {(
                  [
                    ['auto', 'AUTO'],
                    ['on', 'BẬT TAY'],
                    ['off', 'TẮT TAY'],
                  ] as const
                ).map(([key, label]) => {
                  const selected = bomNuoc?.tankMode === key;
                  return (
                    <button
                      key={key}
                      type="button"
                      disabled={!canControl || tankBusy}
                      title={offlineTitle}
                      onClick={() => onDatCheDoBon(key)}
                      className="rounded-xl py-2 text-xs font-bold transition-all disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
                      style={{
                        backgroundColor: selected ? SH.Blue : SH.Card2,
                        border: `1px solid ${selected ? SH.Blue : SH.Border}`,
                        color: selected ? '#FFFFFF' : SH.TextDim,
                      }}
                    >
                      {label}
                    </button>
                  );
                })}
              </div>
            </div>

            <p className={`text-[11px] ${isOffline ? 'text-[#FBBF24]' : 'text-[#64748B]'}`}>
              {isOffline
                ? `Bo bơm đang mất kết nối (${ketNoiText}) — tạm khoá nút điều khiển.`
                : bomNuoc?.tankMode === 'auto'
                  ? 'Tự chạy khi chưa đầy, tự ngắt khi phao báo đầy.'
                  : bomNuoc?.tankMode === 'on'
                    ? 'Chạy liên tục; phao đầy vẫn luôn cắt để chống tràn.'
                    : bomNuoc?.tankMode === 'off'
                      ? 'Ngừng chạy cho tới khi đổi chế độ.'
                      : 'Đang chờ trạng thái từ bo…'}
            </p>

            {tankRunning && (bomNuoc?.tankRun ?? 0) > 0 && (
              <MetricRow
                icon={<Timer size={13} />}
                label="Đã chạy"
                value={dinhDangThoiGianChay(bomNuoc!.tankRun!)}
                accent={SH.Green}
              />
            )}
          </SmartCard>

          <SmartCard className="flex-1 min-h-0 flex flex-col justify-between gap-2.5 p-3">
            <div className="space-y-2.5">
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <IconBadge color={SH.Cyan} size={26}>
                    <Gauge size={14} />
                  </IconBadge>
                  <span className="text-xs font-bold text-[#F1F5F9]">Bơm tăng áp & Phụ trợ</span>
                </div>
                <StatusPill
                  label={
                    bomNuoc === null
                      ? '—'
                      : bomApDangKhoa(bomNuoc)
                        ? 'ĐANG KHOÁ'
                        : boostRunning
                          ? 'ĐANG CHẠY'
                          : 'TẮT'
                  }
                  color={
                    bomNuoc && bomApDangKhoa(bomNuoc)
                      ? SH.Red
                      : boostRunning
                        ? SH.Green
                        : SH.TextOff
                  }
                />
              </div>

              <div className="flex items-center justify-between gap-3 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2.5">
                <div className="min-w-0">
                  <div className="text-xs font-semibold text-[#F1F5F9]">Bơm tăng áp</div>
                  <p
                    className="text-[11px] mt-0.5"
                    style={{ color: locked ? SH.Red : isOffline ? SH.Amber : SH.TextOff }}
                  >
                    {bomNuoc === null
                      ? '—'
                      : bomApDangKhoa(bomNuoc)
                        ? isOffline
                          ? 'Khoá sau sự cố. Chờ bo bơm online để xoá lỗi.'
                          : 'Khoá sau sự cố. Bấm Xoá lỗi để mở.'
                        : dangKhoaBom(bomNuoc)
                          ? isOffline
                            ? 'Bơm chính đang khoá · bo bơm đang offline.'
                            : 'Bơm chính đang khoá nên bơm áp bị chặn.'
                          : isOffline
                            ? 'Bo bơm đang mất kết nối — tạm khoá điều khiển.'
                            : 'Chỉ có BẬT/TẮT tay; bo tự cắt nếu chạy quá 15 phút.'}
                  </p>
                </div>
                <SwitchToggle
                  checked={bomNuoc?.bstMode === 'on'}
                  onChange={() => onDatBomTangAp(bomNuoc?.bstMode !== 'on')}
                  color={SH.Green}
                  disabled={!canControl || locked}
                  pending={boosterBusy}
                  title={offlineTitle}
                />
              </div>

              {boostRunning && (bomNuoc?.boostRun ?? 0) > 0 && (
                <MetricRow
                  icon={<Timer size={13} />}
                  label="Bơm áp đã chạy"
                  value={dinhDangThoiGianChay(bomNuoc!.boostRun!)}
                  accent={SH.Cyan}
                />
              )}

              {bomNuoc && coDenPhu(bomNuoc) && (
                <div className="flex items-center justify-between gap-3 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2.5">
                  <div>
                    <div className="text-xs font-semibold text-[#F1F5F9]">Đèn phụ hộp điện</div>
                    <div className="text-[11px] text-[#64748B]">GPIO 16 trên bo bơm</div>
                  </div>
                  <SwitchToggle
                    checked={denPhuBat(bomNuoc)}
                    onChange={() => onDatDenPhuBom(!denPhuBat(bomNuoc))}
                    color={SH.Amber}
                    disabled={!canControl}
                    pending={ledBusy}
                    title={offlineTitle}
                  />
                </div>
              )}
            </div>

            <div className="space-y-1.5 pt-2 border-t border-[#253459]">
              {nhietAp !== null && (
                <MetricRow
                  icon={<Thermometer size={13} />}
                  label="Nhiệt độ bơm áp"
                  value={nhietAp.toFixed(1)}
                  unit="°C"
                  accent={nhietAp >= 60 ? SH.Red : SH.Orange}
                />
              )}
              <StatusRow
                icon={locked ? <Lock size={13} /> : <LockOpen size={13} />}
                label="Khoá bảo vệ"
                pill={bomNuoc === null ? '—' : locked ? 'ĐANG KHOÁ' : 'MỞ'}
                color={bomNuoc === null ? SH.TextOff : locked ? SH.Red : SH.Green}
              />
            </div>
          </SmartCard>
        </div>
      </div>
    </div>
  );
}

// =====================================================================
//  TAB 1: BAO VE BOM & HIEU CHUAN CAM BIEN
// =====================================================================

function TabBaoVeHieuChuan({
  bomNuoc,
  canControl,
  choBomNuoc,
  onLuuBaoVeBom,
  onGhiMocCan,
  onGhiMocDay,
  onDatLaiDemLuuLuong,
  onTatCoi,
  onThuCoi,
}: {
  bomNuoc: BomNuocState | null;
  canControl: boolean;
  choBomNuoc: Record<string, boolean>;
  onLuuBaoVeBom: (dry: number, rest: number, run: number, pulses: number, minFlow: number) => void;
  onGhiMocCan: () => void;
  onGhiMocDay: () => void;
  onDatLaiDemLuuLuong: () => void;
  onTatCoi: () => void;
  onThuCoi: () => void;
}) {
  const [dry, setDry] = useState('');
  const [minFlow, setMinFlow] = useState('');
  const [maxRun, setMaxRun] = useState('');
  const [minRest, setMinRest] = useState('');
  const [pulses, setPulses] = useState('');
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!dirty && bomNuoc) {
      setDry(bomNuoc.dryCheck !== null ? String(bomNuoc.dryCheck) : '');
      setMinRest(bomNuoc.minRest !== null ? catSoThapPhan(bomNuoc.minRest) : '');
      setMaxRun(bomNuoc.maxRun !== null ? String(bomNuoc.maxRun) : '');
      setPulses(bomNuoc.pulses !== null ? catSoThapPhan(bomNuoc.pulses) : '');
      setMinFlow(bomNuoc.minFlow !== null ? bomNuoc.minFlow.toFixed(2) : '');
    }
  }, [dirty, bomNuoc]);

  const inputCls =
    'w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF] font-mono-num';
  const buzzer = bomNuoc ? coiDangKeu(bomNuoc) : null;
  const offlineTitle = !canControl ? 'Bo bơm đang mất kết nối' : undefined;

  return (
    <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5 items-stretch">
      <SmartCard className="flex flex-col justify-between gap-3 p-3.5 lg:overflow-y-auto">
        <div className="space-y-2.5">
          <div className="flex items-center gap-2">
            <IconBadge color={SH.Orange} size={28}>
              <Shield size={15} />
            </IconBadge>
            <span className="text-xs font-bold text-[#F1F5F9]">Ngưỡng bảo vệ bơm</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs">
            <div>
              <label className="block text-[#94A3B8] mb-1">Kiểm tra chạy khô (giây)</label>
              <input
                type="number"
                value={dry}
                onChange={(e) => { setDry(e.target.value); setDirty(true); }}
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-[#94A3B8] mb-1">Ngưỡng chạy khô (L/phút)</label>
              <input
                type="number"
                step="0.01"
                value={minFlow}
                onChange={(e) => { setMinFlow(e.target.value); setDirty(true); }}
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-[#94A3B8] mb-1">Thời gian chạy tối đa (phút)</label>
              <input
                type="number"
                value={maxRun}
                onChange={(e) => { setMaxRun(e.target.value); setDirty(true); }}
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-[#94A3B8] mb-1">Thời gian nghỉ tối thiểu (phút)</label>
              <input
                type="number"
                step="0.1"
                value={minRest}
                onChange={(e) => { setMinRest(e.target.value); setDirty(true); }}
                className={inputCls}
              />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-[#94A3B8] mb-1">Hiệu chuẩn lưu lượng (xung/lít)</label>
              <input
                type="number"
                step="0.1"
                value={pulses}
                onChange={(e) => { setPulses(e.target.value); setDirty(true); }}
                className={inputCls}
              />
            </div>
          </div>

          {error && <p className="text-xs text-[#EF4444]">{error}</p>}
        </div>

        <div className="space-y-2">
          <FlatButton
            color={SH.Blue}
            variant="solid"
            className="w-full"
            disabled={!canControl || Boolean(choBomNuoc.protection)}
            title={offlineTitle}
            onClick={() => {
              const d = Number.parseInt(dry.trim(), 10);
              const f = Number.parseFloat(minFlow.trim().replace(',', '.'));
              const m = Number.parseInt(maxRun.trim(), 10);
              const r = Number.parseFloat(minRest.trim().replace(',', '.'));
              const p = Number.parseFloat(pulses.trim().replace(',', '.'));
              const dVal = Number.isNaN(d) ? null : d;
              const mVal = Number.isNaN(m) ? null : m;
              const fVal = Number.isFinite(f) ? f : null;
              const rVal = Number.isFinite(r) ? r : null;
              const pVal = Number.isFinite(p) ? p : null;

              const msg = kiemTraBaoVe(dVal, fVal, mVal, rVal, pVal);
              setError(msg);
              if (!msg && dVal !== null && rVal !== null && mVal !== null && pVal !== null && fVal !== null) {
                setDirty(false);
                onLuuBaoVeBom(dVal, rVal, mVal, pVal, fVal);
              }
            }}
          >
            <Save size={14} />
            <span>{choBomNuoc.protection ? 'Đang gửi…' : 'Lưu cài đặt bảo vệ'}</span>
          </FlatButton>

          <p className="text-[11px] text-[#64748B]">
            Bo tự cắt bơm khi nhiệt độ bơm đạt 70 °C (ngưỡng cố định trong firmware).
          </p>
        </div>
      </SmartCard>

      <div className="flex flex-col gap-2.5 min-h-0 lg:overflow-y-auto">
        <SmartCard className="p-3.5 space-y-2">
          <div className="flex items-center gap-2">
            <IconBadge color={SH.Cyan} size={26}>
              <Ruler size={14} />
            </IconBadge>
            <span className="text-xs font-bold text-[#F1F5F9]">Hiệu chuẩn mức nước</span>
          </div>
          <div className="space-y-1">
            <MetricRow icon={<Waves size={13} />} label="Giá trị ADC hiện tại" value={bomNuoc?.lvlRaw !== null && bomNuoc?.lvlRaw !== undefined ? String(bomNuoc.lvlRaw) : null} accent={SH.Cyan} />
            <MetricRow icon={<Waves size={13} />} label="Mốc CẠN đã lưu" value={bomNuoc?.lvlE !== null && bomNuoc?.lvlE !== undefined ? String(bomNuoc.lvlE) : null} accent={SH.Cyan} />
            <MetricRow icon={<Waves size={13} />} label="Mốc ĐẦY đã lưu" value={bomNuoc?.lvlF !== null && bomNuoc?.lvlF !== undefined ? String(bomNuoc.lvlF) : null} accent={SH.Cyan} />
          </div>
          <p className="text-[11px] text-[#64748B]">
            Ghi mốc CẠN lúc bồn cạn và mốc ĐẦY lúc bồn đầy (cách nhau ≥ 200 đơn vị). Dung tích bồn 1500 L cố định trong firmware.
          </p>
          <div className="grid grid-cols-2 gap-2 pt-1">
            <FlatButton
              color={SH.TextSecondary}
              disabled={!canControl || Boolean(choBomNuoc.lvlSetEmpty)}
              title={offlineTitle}
              onClick={onGhiMocCan}
            >
              <Ruler size={14} />
              <span>{choBomNuoc.lvlSetEmpty ? 'Đang ghi…' : 'Ghi mốc CẠN'}</span>
            </FlatButton>
            <FlatButton
              color={SH.TextSecondary}
              disabled={!canControl || Boolean(choBomNuoc.lvlSetFull)}
              title={offlineTitle}
              onClick={onGhiMocDay}
            >
              <Ruler size={14} />
              <span>{choBomNuoc.lvlSetFull ? 'Đang ghi…' : 'Ghi mốc ĐẦY'}</span>
            </FlatButton>
          </div>
        </SmartCard>

        <SmartCard className="p-3.5 space-y-2">
          <div className="flex items-center gap-2">
            <IconBadge color={SH.Green} size={26}>
              <SlidersHorizontal size={14} />
            </IconBadge>
            <span className="text-xs font-bold text-[#F1F5F9]">Hiệu chuẩn lưu lượng</span>
          </div>
          <div className="space-y-1">
            <MetricRow
              icon={<Waves size={13} />}
              label="Bộ đếm xung"
              value={bomNuoc?.calPT !== null && bomNuoc?.calPT !== undefined ? `${bomNuoc.calPT} xung` : null}
              accent={SH.Green}
            />
            <MetricRow
              icon={<Waves size={13} />}
              label="Quy ra"
              value={bomNuoc?.calLT !== null && bomNuoc?.calLT !== undefined ? bomNuoc.calLT.toFixed(2) : null}
              unit="lít"
              accent={SH.Green}
            />
          </div>
          <FlatButton
            color={SH.TextSecondary}
            className="w-full"
            disabled={!canControl || Boolean(choBomNuoc.calReset)}
            title={offlineTitle}
            onClick={onDatLaiDemLuuLuong}
          >
            <SlidersHorizontal size={14} />
            <span>{choBomNuoc.calReset ? 'Đang gửi…' : 'Đặt lại bộ đếm'}</span>
          </FlatButton>
        </SmartCard>

        <SmartCard className="p-3.5 space-y-2">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <IconBadge color={SH.Amber} size={26}>
                <Volume2 size={14} />
              </IconBadge>
              <span className="text-xs font-bold text-[#F1F5F9]">Còi báo động (GPIO 5)</span>
            </div>
            <span
              className="text-xs font-bold"
              style={{ color: buzzer ? SH.Red : SH.TextDim }}
            >
              {buzzer === true ? 'Đang kêu' : buzzer === false ? 'Im' : '—'}
            </span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <FlatButton
              color={SH.Amber}
              variant="outline"
              disabled={!canControl || Boolean(choBomNuoc.coiTat)}
              title={offlineTitle}
              onClick={onTatCoi}
            >
              <VolumeOff size={14} />
              <span>Tắt còi</span>
            </FlatButton>
            <FlatButton
              color={SH.TextSecondary}
              variant="outline"
              disabled={!canControl || Boolean(choBomNuoc.coiThu)}
              title={offlineTitle}
              onClick={onThuCoi}
            >
              <Volume2 size={14} />
              <span>Kêu thử</span>
            </FlatButton>
          </div>
        </SmartCard>
      </div>
    </div>
  );
}

// =====================================================================
//  TAB 2: NHAT KY SU KIEN (GATEWAY) + LUONG NUOC THANG (BO)
// =====================================================================

function TabNhatKyNuoc({
  nhatKy,
  usage,
  canControl,
  clearingUsage,
  onXoaNhatKyCucBo,
  onXoaLichSuBo,
}: {
  nhatKy: SuKienBom[];
  usage: ReturnType<typeof useSmartHome>['luongNuocThang'];
  canControl: boolean;
  clearingUsage: boolean;
  onXoaNhatKyCucBo: () => void;
  onXoaLichSuBo: () => void;
}) {
  const [filter, setFilter] = useState<NhomNhatKy | null>(null);
  const filtered = useMemo(
    () => (filter === null ? nhatKy : nhatKy.filter((e) => e.nhom === filter)),
    [nhatKy, filter],
  );

  return (
    <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5 items-stretch">
      <SmartCard className="lg:col-span-7 flex flex-col gap-2.5 p-3.5 min-h-[260px]">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <IconBadge color={SH.Blue} size={26}>
              <FileText size={14} />
            </IconBadge>
            <span className="text-xs font-bold text-[#F1F5F9]">Nhật ký hoạt động</span>
          </div>
          {nhatKy.length > 0 && (
            <NutXacNhanHaiBuoc
              nhanThuong="Xoá nhật ký"
              nhanXacNhan="Bấm lần nữa để xoá"
              onXacNhan={onXoaNhatKyCucBo}
            />
          )}
        </div>

        <div
          className="flex items-start gap-2 rounded-xl px-2.5 py-2 text-[11px] text-[#94A3B8]"
          style={{ backgroundColor: tint(SH.Amber, 0.12) }}
        >
          <Info size={14} className="text-[#FBBF24] shrink-0 mt-0.5" />
          <span>
            Nhật ký do Node.js Gateway tự động ghi lại (tối đa 100 sự kiện). Bo không lưu lịch sử nên khoảng thời gian Gateway tắt sẽ không có mục mới.
          </span>
        </div>

        <div className="flex flex-wrap gap-1.5">
          <ChoiceChip label="Tất cả" selected={filter === null} color={SH.Blue} onClick={() => setFilter(null)} />
          {NHOM_NHAT_KY_LIST.map((g) => (
            <ChoiceChip
              key={g.ma}
              label={g.nhan}
              selected={filter === g.ma}
              color={SH.Blue}
              onClick={() => setFilter(g.ma)}
            />
          ))}
        </div>

        {filtered.length === 0 ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<FileText size={24} />}
              title="Chưa có sự kiện nào"
              description="Nhật ký sẽ tự ghi khi bơm, phao hoặc cảnh báo thay đổi."
            />
          </div>
        ) : (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 pr-1">
            {filtered.map((e) => {
              const dotColor = e.muc === 'error' ? SH.Red : e.muc === 'warn' ? SH.Amber : SH.Green;
              return (
                <div
                  key={e.id}
                  className="flex items-start gap-2.5 rounded-xl bg-[#1C2946] px-3 py-2 text-xs"
                >
                  <div className="mt-1.5">
                    <Dot color={dotColor} size={7} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="font-semibold text-[#F1F5F9] truncate">{e.tieuDe}</div>
                    <div className="text-[11px] text-[#94A3B8] truncate">{e.chiTiet}</div>
                  </div>
                  <span className="font-mono-num text-[11px] text-[#64748B] shrink-0">
                    {fmtLogTime(e.luc)}
                  </span>
                </div>
              );
            })}
          </div>
        )}
      </SmartCard>

      <SmartCard className="lg:col-span-5 flex flex-col justify-between gap-2.5 p-3.5 min-h-[260px]">
        <div>
          <div className="flex items-center gap-2">
            <IconBadge color={SH.Cyan} size={26}>
              <BarChart3 size={14} />
            </IconBadge>
            <span className="text-xs font-bold text-[#F1F5F9]">Lượng nước theo tháng</span>
          </div>
          <p className="text-[11px] text-[#64748B] mt-1">
            Số liệu thật do bo ghi trong bộ nhớ (tối đa 12 tháng).
          </p>
        </div>

        {usage.length === 0 ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<BarChart3 size={24} />}
              title="Chưa có số liệu tháng nào"
              description="Bo sẽ ghi khi có nước chảy qua cảm biến."
            />
          </div>
        ) : (
          <>
            <div className="space-y-2 flex-1 min-h-0 flex flex-col justify-center">
              <div className="flex items-center gap-4 text-[11px] text-[#94A3B8]">
                <span className="inline-flex items-center gap-1.5">
                  <Dot color={SH.Cyan} size={7} /> Bơm lên bồn
                </span>
                {usage.some((m) => m.nuocMayLit !== null) && (
                  <span className="inline-flex items-center gap-1.5">
                    <Dot color={SH.Green} size={7} /> Nước máy
                  </span>
                )}
              </div>
              <BieuDoNuocThang usage={usage} maxMonths={6} height={175} />
            </div>

            <div className="space-y-1.5 pt-2 border-t border-[#253459]">
              {usage.slice(0, 4).map((m, idx) => (
                <div key={`${m.thang}-${idx}`} className="flex items-center text-[11px]">
                  <span className="flex-1 text-[#94A3B8]">
                    {laThangChuaRo(m) ? 'Chưa rõ tháng' : m.thang}
                  </span>
                  <span className="font-mono-num text-[#22D3EE]">{Math.trunc(m.bomLit)} L bơm</span>
                  {m.nuocMayLit !== null && (
                    <span className="font-mono-num text-[#22C55E] ml-2">
                      {Math.trunc(m.nuocMayLit)} L máy
                    </span>
                  )}
                </div>
              ))}
              <div className="pt-1">
                <NutXacNhanHaiBuoc
                  nhanThuong="Xoá lịch sử trên bo"
                  nhanXacNhan="Bấm lần nữa để xoá hẳn"
                  disabled={!canControl || clearingUsage}
                  title={!canControl ? 'Bo bơm đang mất kết nối' : undefined}
                  onXacNhan={onXoaLichSuBo}
                />
              </div>
            </div>
          </>
        )}
      </SmartCard>
    </div>
  );
}

// =====================================================================
//  TAB 3: KICH BAN TU DONG HOA & LIEN KET AQUACONTROL
// =====================================================================

function TabKichBan({
  daCauHinh,
  bomNuoc,
  ketNoiDirect,
  hasAquaUrl,
  aquaUrl,
  onOpenPumpTab,
  onOpenAquaWeb,
  onShowToast,
}: {
  daCauHinh: boolean;
  bomNuoc: BomNuocState | null;
  ketNoiDirect: boolean;
  hasAquaUrl: boolean;
  aquaUrl: string;
  onOpenPumpTab: () => void;
  onOpenAquaWeb: () => void;
  onShowToast: (msg: string, tone?: 'info' | 'success' | 'warning' | 'error') => void;
}) {
  const modeRaw = nhanCheDoBon(bomNuoc?.tankMode);
  const modeCap = modeRaw ? modeRaw.charAt(0).toUpperCase() + modeRaw.slice(1) : modeRaw;
  const pumpBadge = !daCauHinh
    ? { label: 'Chưa cấu hình', color: SH.TextOff }
    : bomNuoc && coLoiBom(bomNuoc)
      ? { label: 'Có cảnh báo', color: SH.Red }
      : bomNuoc && bomChinhDangChay(bomNuoc)
        ? { label: 'Đang bơm', color: SH.Green }
        : ketNoiDirect
          ? { label: `${modeCap} · Online`, color: SH.Cyan }
          : { label: 'Offline', color: SH.Amber };

  return (
    <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5 items-stretch">
      <SmartCard className="lg:col-span-5 flex flex-col justify-between gap-3 p-4">
        <div className="space-y-3">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-3">
              <IconBadge color={SH.Cyan} size={40}>
                <Droplets size={20} />
              </IconBadge>
              <div>
                <h2 className="text-sm font-bold text-[#F1F5F9]">AquaControl</h2>
                <p className="text-[11px] text-[#94A3B8]">Hệ thống Bơm & Cấp Nước Tự Động</p>
              </div>
            </div>
            <StatusPill label="Tích hợp sẵn" color={SH.Cyan} />
          </div>

          <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2.5 text-xs">
            <div className="flex items-start gap-2.5">
              <Droplets size={16} className="text-[#22D3EE] shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold text-[#F1F5F9]">Điều khiển bơm & Sơ đồ trực quan</div>
                <div className="text-[11px] text-[#94A3B8]">
                  Chế độ AUTO/BẬT/TẮT, bơm tăng áp, cảm biến mức 1500L và lưu lượng thật.
                </div>
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <Zap size={16} className="text-[#FBBF24] shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold text-[#F1F5F9]">Bảo vệ động cơ & Xoá lỗi từ xa</div>
                <div className="text-[11px] text-[#94A3B8]">
                  Giám sát chạy khô, quá giờ, quá nhiệt 70°C, tắt còi và mở khoá ngay trên web.
                </div>
              </div>
            </div>
            <div className="flex items-start gap-2.5">
              <CheckCircle2 size={16} className="text-[#22C55E] shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold text-[#F1F5F9]">Đồng bộ LAN 1s & MQTT 30s</div>
                <div className="text-[11px] text-[#94A3B8]">
                  Gateway tự chọn LAN khi gọi được bo, tự chuyển sang EMQX MQTT và hỗ trợ giọng nói Gemini.
                </div>
              </div>
            </div>
          </div>

          {hasAquaUrl && (
            <div className="rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-[11px]">
              <span className="text-[#94A3B8]">URL AquaControl Web ngoài: </span>
              <span className="font-mono-num text-[#E6EDF7] break-all">{aquaUrl}</span>
            </div>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <FlatButton color={SH.Blue} variant="solid" onClick={onOpenPumpTab}>
            <Droplets size={14} />
            <span>Mở bảng bơm nước</span>
          </FlatButton>
          {hasAquaUrl && (
            <FlatButton color={SH.Cyan} variant="outline" onClick={onOpenAquaWeb}>
              <ExternalLink size={14} />
              <span>Mở Web gốc</span>
            </FlatButton>
          )}
        </div>
      </SmartCard>

      <SmartCard className="lg:col-span-7 flex flex-col gap-3 p-4 min-h-0">
        <div className="flex items-center justify-between gap-2">
          <div>
            <h2 className="text-sm font-bold text-[#F1F5F9]">Kịch bản tự động hóa</h2>
            <p className="text-xs text-[#94A3B8]">
              Theo dõi trạng thái bơm thật cùng các quy tắc ngữ cảnh phòng
            </p>
          </div>
          <DemoBadge label="MỘT PHẦN MẪU" />
        </div>

        <div className="flex-1 min-h-0 grid grid-cols-1 sm:grid-cols-2 gap-2.5 overflow-y-auto">
          <div
            onClick={onOpenPumpTab}
            className="sm:col-span-2 rounded-xl border border-[#253459] bg-[#0E1627] p-3 flex items-center justify-between gap-3 cursor-pointer hover:bg-[#1A2744] transition-colors"
          >
            <div className="flex items-center gap-2.5 min-w-0">
              <IconBadge color={pumpBadge.color} size={34}>
                <Droplets size={17} />
              </IconBadge>
              <div className="min-w-0">
                <div className="text-xs font-bold text-[#F1F5F9]">Bơm nước thông minh (Thật)</div>
                <p className="text-[11px] text-[#94A3B8] truncate">
                  Tự động bơm lên bồn 1500L theo phao & cảm biến chống chạy khô
                </p>
              </div>
            </div>
            <StatusPill label={pumpBadge.label} color={pumpBadge.color} />
          </div>

          {[
            {
              title: 'Đi ngủ 23:00',
              desc: 'Tắt đèn phòng khách, bật quạt nhẹ, đặt điều hòa 27°C',
              color: SH.Purple,
              icon: <Moon size={17} />,
            },
            {
              title: 'Chào buổi sáng',
              desc: 'Mở rèm phòng khách 100%, tắt chế độ hồng ngoại hành lang',
              color: SH.Amber,
              icon: <Sun size={17} />,
            },
            {
              title: 'An toàn rò rỉ Gas',
              desc: 'Tự bật máy hút mùi bếp mức 3 và hú cảnh báo toàn nhà',
              color: SH.Red,
              icon: <ShieldAlert size={17} />,
            },
            {
              title: 'Cảm biến PIR hành lang',
              desc: 'Tự bật đèn đầu hè & sân quanh vào ban đêm (18:00 - 05:30)',
              color: SH.Cyan,
              icon: <Radio size={17} />,
            },
          ].map((sc) => (
            <button
              key={sc.title}
              type="button"
              onClick={() =>
                onShowToast(
                  `Kịch bản mẫu "${sc.title}" — tùy chỉnh trực tiếp trong từng phòng chi tiết.`,
                  'info',
                )
              }
              className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2 text-left hover:bg-[#1A2744] hover:border-[#3B4F7D] transition-colors cursor-pointer"
            >
              <div className="flex items-center justify-between gap-2">
                <IconBadge color={sc.color} size={32}>
                  {sc.icon}
                </IconBadge>
                <StatusPill label="Đang bật" color={SH.Green} />
              </div>
              <div className="text-xs font-bold text-[#E6EDF7]">{sc.title}</div>
              <p className="text-[11px] text-[#8A9BB8] leading-relaxed">{sc.desc}</p>
            </button>
          ))}
        </div>
      </SmartCard>
    </div>
  );
}

// =====================================================================
//  PHU TRO NHO
// =====================================================================

function StatusRow({
  icon,
  label,
  pill,
  color,
}: {
  icon: React.ReactNode;
  label: string;
  pill: string;
  color: string;
}) {
  return (
    <div className="flex items-center justify-between gap-2">
      <div className="flex items-center gap-2 min-w-0">
        <span
          className="inline-flex h-5 w-5 items-center justify-center rounded-md shrink-0"
          style={{ backgroundColor: tint(color, 0.16), color }}
        >
          {icon}
        </span>
        <span className="text-[11px] text-[#94A3B8] truncate">{label}</span>
      </div>
      <StatusPill label={pill} color={color} dot={false} />
    </div>
  );
}

function MetricRow({
  icon,
  label,
  value,
  unit,
  accent,
  reason,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | null;
  unit?: string;
  accent: string;
  reason?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-2 text-xs">
      <div className="flex items-center gap-1.5 min-w-0">
        <span className="shrink-0" style={{ color: accent }}>
          {icon}
        </span>
        <span className="text-[11px] text-[#94A3B8] truncate">{label}</span>
      </div>
      {value === null ? (
        <span className="text-[11px] text-[#64748B] shrink-0">
          --{reason ? ` · ${reason}` : ''}
        </span>
      ) : (
        <span className="font-mono-num font-bold text-[#F1F5F9] shrink-0">
          {value}
          {unit ? <span className="ml-1 text-[11px] font-normal text-[#94A3B8]">{unit}</span> : null}
        </span>
      )}
    </div>
  );
}
