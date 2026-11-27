import React, { useState } from 'react';
import {
  AlertTriangle,
  Bolt,
  Calendar,
  CheckCircle2,
  ChevronRight,
  Droplets,
  TrendingDown,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { DemoData } from '../data/demoData.js';
import {
  chenhPhanTramDien,
  coSoLieuDien,
  dinhDangDdMm,
  doTuoiDien,
  nhanThuNgayDayDu,
  nhanThuNgayNgan,
  tongKwhDien,
  trungBinhKwh,
  uocTinhTienNgay,
} from '../data/dien.js';
import {
  bomChinhDangChay,
  bonDay,
  coNhanhNuocMay,
  daCauHinhBom,
  dangKhoaBom,
  bomApDangKhoa,
  DUNG_TICH_BON_LIT,
  mucNuocLit,
  mucNuocPhanTram,
  nhanCheDoBon,
  nhanKetNoiBom,
  nhietDoNuoc,
  phanTichLoiBomNuoc,
} from '../data/bomnuoc.js';
import {
  STAT_KINDS,
  STAT_RANGES,
  type StatKindId,
  type StatRangeId,
} from '../data/models.js';
import {
  ChoiceChip,
  DemoBadge,
  Dot,
  EmptyState,
  FlatButton,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
  TrangThaiKetNoi,
} from '../components/Common.js';
import { ArcGauge, BarChart } from '../components/Charts.js';
import { BieuDoNuocThang, TrangThaiKetNoiBomPill, VongMucNuoc } from '../components/BomNuocUi.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function StatsScreen() {
  const {
    config,
    dien,
    mqttStatus,
    nowMs,
    openChiTietDien,
    navigateDest,
    bomNuoc,
    luongNuocThang,
    ketNoiBom,
    kenhBom,
  } = useSmartHome();
  const [kindId, setKindId] = useState<StatKindId>('POWER');
  const [rangeId, setRangeId] = useState<StatRangeId>('WEEK');
  const [selectedBarIdx, setSelectedBarIdx] = useState<number | null>(null);

  const kindInfo = STAT_KINDS.find((k) => k.id === kindId)!;
  const rangeInfo = STAT_RANGES.find((r) => r.id === rangeId)!;

  const fmt1 = (v: number) => v.toFixed(1).replace(/\.0$/, '');

  // POWER tab uses strict real EVN/MQTT telemetry (khong bia so)
  const renderRealPowerStats = () => {
    const hasReal = coSoLieuDien(dien);
    const tuoi = doTuoiDien(dien, new Date(nowMs));
    const slice = dien.lichSu.slice(-rangeInfo.days);
    const total = tongKwhDien(dien, rangeInfo.days);
    const delta = chenhPhanTramDien(dien, rangeInfo.days);
    const avgKwh = trungBinhKwh(slice);
    const activeBarIdx = selectedBarIdx !== null && selectedBarIdx < slice.length ? selectedBarIdx : null;
    const selectedDay = activeBarIdx !== null ? slice[activeBarIdx] : null;

    const labels =
      slice.length <= 7
        ? slice.map((d) => nhanThuNgayNgan(d.ngay))
        : slice.map((d, i) => {
            const step = Math.max(1, Math.floor(slice.length / 7));
            return i % step === 0 ? dinhDangDdMm(d.ngay) : '';
          });
    const values = slice.map((d) => d.kwh);

    const baseStatus = (() => {
      if (mqttStatus.state === 'chua_cau_hinh') {
        return { ok: false, color: SH.TextDim, label: 'Chưa cấu hình MQTT', sub: 'Vào Cài đặt → Dịch vụ' };
      }
      if (mqttStatus.state === 'loi') {
        return { ok: false, color: SH.Red, label: 'Lỗi kết nối MQTT', sub: mqttStatus.moTa };
      }
      if (mqttStatus.state !== 'da_noi') {
        return { ok: false, color: SH.Amber, label: 'Đang nối MQTT…', sub: dien.capNhat || 'Chờ máy chủ' };
      }
      if (dien.loi) {
        return { ok: false, color: SH.Red, label: 'Cữ gần nhất lỗi', sub: dien.loi };
      }
      if (dien.trangThai === 'loi') {
        return { ok: false, color: SH.Red, label: 'Cữ gần nhất lỗi', sub: 'Xem log trên serverubuntu' };
      }
      if (tuoi === 'CHUA_CO') {
        return { ok: false, color: SH.TextDim, label: 'Chưa có mốc cập nhật', sub: 'Chờ gói dien/capnhat' };
      }
      if (tuoi === 'QUA_CU') {
        return { ok: false, color: SH.Red, label: 'Dữ liệu cũ > 24h', sub: dien.capNhat || '' };
      }
      if (tuoi === 'HOI_CU') {
        return { ok: false, color: SH.Amber, label: 'Số hơi cũ (8–24h)', sub: dien.capNhat || '' };
      }
      return { ok: true, color: SH.Green, label: 'Bình thường', sub: dien.capNhat || '' };
    })();

    const statusOk = baseStatus.ok;
    const selectedCost = selectedDay ? uocTinhTienNgay(selectedDay.kwh, dien) : null;
    const diffPctVsAvg =
      selectedDay && avgKwh !== null && avgKwh > 0
        ? Math.round(((selectedDay.kwh - avgKwh) / avgKwh) * 100)
        : null;

    const dayStatus = (() => {
      if (!selectedDay || !statusOk) {
        return baseStatus;
      }
      if (avgKwh !== null && avgKwh > 0 && selectedDay.kwh > avgKwh * 1.25) {
        return {
          ok: false,
          color: SH.Amber,
          label: 'Cao hơn TB',
          sub: dinhDangDdMm(selectedDay.ngay),
        };
      }
      if (avgKwh !== null && avgKwh > 0 && selectedDay.kwh < avgKwh * 0.85) {
        return {
          ok: true,
          color: SH.Green,
          label: 'Tiết kiệm',
          sub: dinhDangDdMm(selectedDay.ngay),
        };
      }
      return {
        ok: true,
        color: SH.Green,
        label: 'Bình thường',
        sub: dinhDangDdMm(selectedDay.ngay),
      };
    })();

    return (
      <div className="flex-1 min-h-0 flex flex-col gap-2.5">
        <SmartCard className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-bold text-[#E6EDF7]">Điện năng EVN · Dữ liệu thật MQTT</span>
              <TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} />
              {(dien.loi || dien.trangThai === 'loi') && (
                <StatusPill label="Cữ gần nhất lỗi" color={SH.Red} />
              )}
            </div>
            <p className="text-[11px] text-[#8A9BB8]">
              {dien.capNhat ? `Cập nhật gần nhất: ${dien.capNhat}` : 'Chưa nhận được gói dien/capnhat'}
            </p>
          </div>
          {dien.lichSu.length > 0 && (
            <FlatButton color={SH.Blue} onClick={openChiTietDien}>
              <span>Xem theo ngày</span>
              <ChevronRight size={14} />
            </FlatButton>
          )}
        </SmartCard>

        {!hasReal ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<Bolt size={28} />}
              title="Chưa có dữ liệu điện từ MQTT"
              description={
                mqttStatus.state === 'chua_cau_hinh'
                  ? 'Vào Cài đặt → Kết nối để khai báo máy chủ MQTT. Ứng dụng tuyệt đối không điền số giả thay thế.'
                  : 'Đang chờ script dien-evn công bố dữ liệu vào topic dien/#'
              }
              action={
                <FlatButton color={SH.Blue} onClick={() => navigateDest('settings')}>
                  <span>Mở Cài đặt MQTT</span>
                </FlatButton>
              }
            />
          </div>
        ) : (
          <div className="flex-1 min-h-0 flex flex-col gap-2.5">
            {/* Main chart card */}
            <SmartCard className="flex-1 min-h-0 flex flex-col justify-between gap-2 p-3">
              <div className="flex items-start justify-between gap-2 flex-wrap">
                <div>
                  <div className="text-xs text-[#8A9BB8]">
                    Điện năng tiêu thụ · {slice.length} ngày ({rangeInfo.label})
                  </div>
                  <div className="flex items-baseline gap-2.5 mt-1 flex-wrap">
                    <span className="font-mono-num text-2xl sm:text-3xl font-bold text-[#E6EDF7]">
                      {total !== null ? fmt1(total) : '—'}
                    </span>
                    <span className="text-sm font-semibold text-[#8A9BB8]">kWh</span>
                    {delta !== null && (
                      <span
                        className="inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-[11px] font-semibold"
                        style={{
                          color: delta >= 0 ? SH.Red : SH.Green,
                          backgroundColor: delta >= 0 ? 'rgba(239,68,68,0.15)' : 'rgba(34,197,94,0.15)',
                        }}
                      >
                        {delta >= 0 ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                        <span>{Math.abs(delta)}%</span>
                      </span>
                    )}
                  </div>
                  {(dien.congToChiSo !== null || dien.congNoSoTien !== null) && (
                    <div className="text-[11px] text-[#8A9BB8] mt-0.5">
                      {[
                        dien.congToChiSo !== null ? `Chỉ số công tơ: ${fmt1(dien.congToChiSo)}` : null,
                        dien.congNoSoTien !== null
                          ? `Công nợ: ${Math.round(dien.congNoSoTien).toLocaleString('vi-VN')} đ`
                          : null,
                      ]
                        .filter(Boolean)
                        .join(' · ')}
                    </div>
                  )}
                </div>
                {selectedDay && (
                  <button
                    type="button"
                    onClick={() => setSelectedBarIdx(null)}
                    className="rounded-lg border border-[#253459] bg-[#1C2946] px-2.5 py-1 text-[11px] text-[#CBD5E1] hover:text-white cursor-pointer"
                  >
                    Bỏ chọn ngày
                  </button>
                )}
              </div>

              {values.length > 0 ? (
                <div className="flex-1 min-h-0 flex items-center">
                  <BarChart
                    values={values}
                    labels={labels}
                    color={SH.Blue}
                    unit="kWh"
                    height={210}
                    labelMode={slice.length <= 7 ? 'all' : 'key_points'}
                    averageValue={avgKwh}
                    averageLabel={avgKwh !== null ? `TB: ${fmt1(avgKwh)} kWh` : undefined}
                    selectedIndex={activeBarIdx}
                    onSelectIndex={(idx) => setSelectedBarIdx((prev) => (prev === idx ? null : idx))}
                    startAtZero
                  />
                </div>
              ) : (
                <div className="py-10 text-center text-xs text-[#8A9BB8]">
                  Chưa có chuỗi ngày trong topic dien/lichsu.
                </div>
              )}
            </SmartCard>

            {/* Bottom 3 summary cards */}
            <div className="shrink-0 grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <SmartCard
                glowColor={selectedDay ? SH.Blue : undefined}
                className="flex flex-col justify-between gap-1.5 p-3"
              >
                <div className="text-[11px] text-[#8A9BB8] truncate">
                  {selectedDay
                    ? `Sản lượng ${nhanThuNgayDayDu(selectedDay.ngay)} (${dinhDangDdMm(selectedDay.ngay)})`
                    : 'Sản lượng ngày gần nhất'}
                </div>
                <div className="flex items-center gap-2.5">
                  <IconBadge color={SH.Blue} size={32}>
                    <Calendar size={16} />
                  </IconBadge>
                  <div className="min-w-0">
                    <div className="font-mono-num text-base font-bold text-[#E6EDF7] truncate">
                      {selectedDay
                        ? `${fmt1(selectedDay.kwh)} kWh`
                        : dien.ngaySanLuong !== null
                          ? `${fmt1(dien.ngaySanLuong)} kWh`
                          : '—'}
                    </div>
                    <div className="text-[11px] text-[#64748B] truncate">
                      {selectedDay
                        ? diffPctVsAvg !== null
                          ? `${diffPctVsAvg >= 0 ? '+' : ''}${diffPctVsAvg}% so với TB kỳ`
                          : dinhDangDdMm(selectedDay.ngay)
                        : dien.ngay || 'chưa có ngày'}
                    </div>
                  </div>
                </div>
              </SmartCard>

              <SmartCard
                glowColor={selectedDay ? SH.Blue : undefined}
                className="flex flex-col justify-between gap-1.5 p-3"
              >
                <div className="text-[11px] text-[#8A9BB8] truncate">
                  {selectedDay ? 'Chi phí ước tính' : 'Chi phí tháng này đến giờ'}
                </div>
                <div className="flex items-center gap-2.5">
                  <IconBadge color={SH.Amber} size={32}>
                    <Wallet size={16} />
                  </IconBadge>
                  <div className="min-w-0">
                    <div className="font-mono-num text-base font-bold text-[#E6EDF7] truncate">
                      {selectedDay
                        ? selectedCost !== null
                          ? `${Math.round(selectedCost).toLocaleString('vi-VN')} đ`
                          : '—'
                        : dien.thangTien !== null
                          ? `${Math.round(dien.thangTien).toLocaleString('vi-VN')} đ`
                          : '—'}
                    </div>
                    <div className="text-[11px] text-[#64748B] truncate">
                      {selectedDay
                        ? selectedCost !== null
                          ? `Ước tính cho ${fmt1(selectedDay.kwh)} kWh`
                          : 'Chưa có lũy kế tháng'
                        : dien.thangSanLuong !== null
                          ? `${fmt1(dien.thangSanLuong)} kWh lũy kế`
                          : 'chưa có lũy kế'}
                    </div>
                  </div>
                </div>
              </SmartCard>

              <SmartCard
                glowColor={selectedDay && statusOk ? SH.Blue : undefined}
                className="flex flex-col justify-between gap-1.5 p-3"
              >
                <div className="text-[11px] text-[#8A9BB8] truncate">Trạng thái</div>
                <div className="flex items-center gap-2.5">
                  <IconBadge color={dayStatus.color} size={32}>
                    {dayStatus.color === SH.Green ? (
                      <CheckCircle2 size={16} />
                    ) : (
                      <AlertTriangle size={16} />
                    )}
                  </IconBadge>
                  <div className="min-w-0">
                    <div className="text-sm font-semibold truncate" style={{ color: dayStatus.color }}>
                      {dayStatus.label}
                    </div>
                    <div className="text-[11px] text-[#64748B] truncate">{dayStatus.sub || '—'}</div>
                  </div>
                </div>
              </SmartCard>
            </div>
          </div>
        )}
      </div>
    );
  };

  // WATER tab uses strict real AquaControl telemetry
  const renderRealWaterStats = () => {
    const tongThangLit = bomNuoc?.useT ?? luongNuocThang[0]?.bomLit ?? null;
    const nuocMayThangLit = bomNuoc?.useC ?? luongNuocThang[0]?.nuocMayLit ?? null;
    const tongM3 = tongThangLit !== null ? tongThangLit / 1000 : null;
    const pct = bomNuoc ? mucNuocPhanTram(bomNuoc) : null;
    const lit = bomNuoc ? mucNuocLit(bomNuoc) : null;
    const tempWater = bomNuoc ? nhietDoNuoc(bomNuoc) : null;
    const fault = phanTichLoiBomNuoc(bomNuoc?.fault);
    const locked = bomNuoc ? dangKhoaBom(bomNuoc) || bomApDangKhoa(bomNuoc) : false;

    const statusBadge =
      bomNuoc === null
        ? { label: nhanKetNoiBom(ketNoiBom, kenhBom), color: SH.TextDim }
        : fault !== null
          ? { label: fault.noiDung, color: SH.Red }
          : locked
            ? { label: 'Đang khoá bảo vệ', color: SH.Red }
            : bomChinhDangChay(bomNuoc)
              ? { label: `Đang bơm (${nhanCheDoBon(bomNuoc.tankMode)})`, color: SH.Green }
              : { label: `Bơm nghỉ (${nhanCheDoBon(bomNuoc.tankMode)})`, color: SH.Green };

    const thangLabel =
      bomNuoc?.useM && bomNuoc.useM.trim() && bomNuoc.useM !== 'chua ro thang'
        ? bomNuoc.useM
        : 'hiện tại';

    return (
      <div className="flex-1 min-h-0 flex flex-col gap-2.5">
        <SmartCard className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-bold text-[#E6EDF7]">Nước tiêu thụ · Dữ liệu thật bo bơm</span>
              <TrangThaiKetNoiBomPill ketNoi={ketNoiBom} kenh={kenhBom} />
            </div>
            <p className="text-[11px] text-[#8A9BB8]">
              {bomNuoc?.fw ? `Firmware ${bomNuoc.fw} · Tháng ${thangLabel}` : nhanKetNoiBom(ketNoiBom, kenhBom)}
            </p>
          </div>
          <FlatButton color={SH.Cyan} onClick={() => navigateDest('automation')}>
            <span>Điều khiển bơm</span>
            <ChevronRight size={14} />
          </FlatButton>
        </SmartCard>

        {bomNuoc === null && luongNuocThang.length === 0 ? (
          <div className="flex-1 min-h-0 flex items-center justify-center">
            <EmptyState
              icon={<Droplets size={28} />}
              title="Chưa có số liệu nước từ bo bơm"
              description={
                daCauHinhBom(config.bomNuoc)
                  ? `Gateway đang chờ dữ liệu từ bo AquaControl (${nhanKetNoiBom(ketNoiBom, kenhBom)}).`
                  : 'Vào Tự động hóa hoặc Cài đặt → Bơm nước để khai báo địa chỉ bo ESP32 AquaControl.'
              }
              action={
                <FlatButton color={SH.Cyan} onClick={() => navigateDest('automation')}>
                  <span>Mở Tự động hóa</span>
                </FlatButton>
              }
            />
          </div>
        ) : (
          <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-8 flex flex-col justify-between gap-2 p-3 min-h-0">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <div className="text-xs text-[#8A9BB8]">Lượng nước tháng {thangLabel}</div>
                  <div className="flex items-baseline gap-2 mt-1">
                    <span className="font-mono-num text-2xl font-bold text-[#E6EDF7]">
                      {tongM3 !== null ? `${tongM3.toFixed(1)} m³` : '—'}
                    </span>
                    {tongThangLit !== null && (
                      <span className="text-xs text-[#94A3B8]">
                        ({Math.trunc(tongThangLit)} L bơm
                        {nuocMayThangLit !== null ? ` · ${Math.trunc(nuocMayThangLit)} L nước máy` : ''})
                      </span>
                    )}
                  </div>
                </div>

                {luongNuocThang.length > 0 && (
                  <div className="flex items-center gap-3 text-[11px] text-[#94A3B8]">
                    <span className="inline-flex items-center gap-1.5">
                      <Dot color={SH.Cyan} size={7} /> Bơm lên bồn
                    </span>
                    {luongNuocThang.some((m) => m.nuocMayLit !== null) && (
                      <span className="inline-flex items-center gap-1.5">
                        <Dot color={SH.Green} size={7} /> Nước máy
                      </span>
                    )}
                  </div>
                )}
              </div>

              {luongNuocThang.length > 0 ? (
                <BieuDoNuocThang usage={luongNuocThang} maxMonths={12} height={200} />
              ) : (
                <div className="py-10 text-center text-xs text-[#8A9BB8]">
                  Đã nối bo bơm nhưng chưa có lịch sử nước theo tháng.
                </div>
              )}
            </SmartCard>

            <SmartCard className="lg:col-span-4 space-y-3 flex flex-col justify-between p-3">
              <div className="flex flex-col items-center justify-center gap-1 pt-1">
                <VongMucNuoc percent={pct} size={112} strokeWidth={10} />
                <span className="text-xs text-[#8A9BB8] mt-1">
                  {lit !== null
                    ? `${Math.trunc(lit)} / ${DUNG_TICH_BON_LIT} L trên bồn`
                    : bomNuoc === null
                      ? 'Chưa nối bo bơm'
                      : bonDay(bomNuoc)
                        ? 'Phao báo đầy'
                        : 'Chưa lắp/chưa hiệu chuẩn cảm biến'}
                </span>
              </div>

              <div className="space-y-2 border-t border-[#253459] pt-3 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-[#8A9BB8]">Lưu lượng bơm</span>
                  <span className="font-mono-num font-bold text-[#22D3EE]">
                    {bomNuoc?.fTank !== null && bomNuoc?.fTank !== undefined
                      ? `${bomNuoc.fTank.toFixed(1)} L/phút`
                      : '—'}
                  </span>
                </div>
                {bomNuoc && coNhanhNuocMay(bomNuoc) && (
                  <div className="flex items-center justify-between">
                    <span className="text-[#8A9BB8]">Lưu lượng nước máy</span>
                    <span className="font-mono-num font-bold text-[#22C55E]">
                      {bomNuoc.fCity !== null ? `${bomNuoc.fCity.toFixed(1)} L/phút` : '—'}
                    </span>
                  </div>
                )}
                <div className="flex items-center justify-between">
                  <span className="text-[#8A9BB8]">Nhiệt độ nước</span>
                  <span className="font-mono-num font-bold text-[#E6EDF7]">
                    {tempWater !== null ? `${tempWater.toFixed(1)} °C` : '—'}
                  </span>
                </div>
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[#8A9BB8] shrink-0">Trạng thái</span>
                  <StatusPill label={statusBadge.label} color={statusBadge.color} />
                </div>
              </div>
            </SmartCard>
          </div>
        )}
      </div>
    );
  };

  // Other 3 tabs (TEMP, HUMID, GAS) use seeded DemoData + DemoBadge
  const renderDemoStats = () => {
    const s = DemoData.series(kindId, rangeId);
    const up = s.deltaPercent >= 0;

    return (
      <SmartCard className="flex-1 min-h-0 flex flex-col gap-3 p-3">
        <SectionHeader
          title={s.kind.title}
          subtitle={`Kỳ ${rangeInfo.label} · Dữ liệu mô phỏng đồng nhất theo seed`}
          badge={<DemoBadge />}
        />
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-3 items-center">
          <div className="lg:col-span-8 flex flex-col justify-between gap-2 min-h-0">
            <div className="flex items-center justify-between">
              <div className="font-mono-num text-2xl font-bold text-[#E6EDF7]">
                {s.total} <span className="text-sm font-semibold text-[#8A9BB8]">{s.unit}</span>
              </div>
              <span
                className="inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold"
                style={{
                  color: up ? SH.Amber : SH.Green,
                  backgroundColor: up ? 'rgba(245,158,11,0.14)' : 'rgba(34,197,94,0.14)',
                }}
              >
                {up ? <TrendingUp size={13} /> : <TrendingDown size={13} />}
                <span>
                  {up ? '+' : ''}
                  {s.deltaPercent}%
                </span>
              </span>
            </div>
            <BarChart
              values={s.bars}
              labels={s.labels}
              color={s.kind.color}
              unit={s.unit}
              labelMode={s.bars.length <= 7 ? 'all' : 'key_points'}
              averageValue={s.bars.length > 0 ? s.bars.reduce((a, b) => a + b, 0) / s.bars.length : null}
              averageLabel={
                s.bars.length > 0
                  ? `TB: ${fmt1(s.bars.reduce((a, b) => a + b, 0) / s.bars.length)} ${s.unit}`
                  : undefined
              }
            />
          </div>

          <div className="lg:col-span-4 flex flex-col justify-between rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-3">
            <ArcGauge
              fraction={s.gaugeFraction}
              valueText={s.gaugeValue}
              label={s.gaugeLabel}
              color={s.kind.color}
            />
            <div className="space-y-2 border-t border-[#253459] pt-2.5 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-[#8A9BB8]">{s.costLabel}</span>
                <span className="font-mono-num font-bold text-[#E6EDF7]">{s.costValue}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-[#8A9BB8]">Đánh giá</span>
                <StatusPill label={s.statusLabel} color={s.statusOk ? SH.Green : SH.Red} />
              </div>
            </div>
          </div>
        </div>
      </SmartCard>
    );
  };

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      <div className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex flex-wrap gap-2">
          {STAT_KINDS.map((k) => (
            <ChoiceChip
              key={k.id}
              label={k.label}
              selected={kindId === k.id}
              color={k.color}
              onClick={() => setKindId(k.id)}
            />
          ))}
        </div>
        {kindId !== 'WATER' && (
          <div className="flex gap-2">
            {STAT_RANGES.map((r) => (
              <ChoiceChip
                key={r.id}
                label={r.label}
                selected={rangeId === r.id}
                color={kindInfo.color}
                onClick={() => {
                  setRangeId(r.id);
                  setSelectedBarIdx(null);
                }}
              />
            ))}
          </div>
        )}
      </div>

      {kindId === 'POWER'
        ? renderRealPowerStats()
        : kindId === 'WATER'
          ? renderRealWaterStats()
          : renderDemoStats()}
    </div>
  );
}
