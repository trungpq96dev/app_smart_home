import React, { useState } from 'react';
import { Bolt, ChevronRight, Droplets, TrendingDown, TrendingUp } from 'lucide-react';
import { DemoData } from '../data/demoData.js';
import {
  chenhPhanTramDien,
  coSoLieuDien,
  doTuoiDien,
  tongKwhDien,
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

  const kindInfo = STAT_KINDS.find((k) => k.id === kindId)!;
  const rangeInfo = STAT_RANGES.find((r) => r.id === rangeId)!;

  // POWER tab uses strict real EVN/MQTT telemetry (khong bia so)
  const renderRealPowerStats = () => {
    const hasReal = coSoLieuDien(dien);
    const tuoi = doTuoiDien(dien, new Date(nowMs));
    const slice = dien.lichSu.slice(-rangeInfo.days);
    const total = tongKwhDien(dien, rangeInfo.days);
    const delta = chenhPhanTramDien(dien, rangeInfo.days);
    const labels = slice.map((d) => {
      const m = /^\d{4}-(\d{2})-(\d{2})$/.exec(d.ngay);
      return m ? `${m[2]}/${m[1]}` : d.ngay.slice(-5);
    });
    const values = slice.map((d) => d.kwh);

    const statusOk =
      mqttStatus.state === 'da_noi' &&
      !dien.loi &&
      dien.trangThai !== 'loi' &&
      tuoi !== 'QUA_CU';

    const statusText = dien.loi
      ? `Lỗi: ${dien.loi}`
      : mqttStatus.state !== 'da_noi'
        ? 'Chưa nối MQTT'
        : tuoi === 'QUA_CU'
          ? 'Dữ liệu cũ > 24h'
          : tuoi === 'HOI_CU'
            ? 'Số hơi cũ (8–24h)'
            : 'Hoạt động bình thường';

    const latestDayVal = dien.ngaySanLuong !== null ? `${dien.ngaySanLuong.toFixed(2)} kWh` : '—';
    const gaugeFraction = dien.ngaySanLuong !== null ? Math.min(1, dien.ngaySanLuong / 25) : 0;

    return (
      <div className="flex-1 min-h-0 flex flex-col gap-2.5">
        <SmartCard className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-2.5">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-sm font-bold text-[#E6EDF7]">Điện năng EVN · Dữ liệu thật MQTT</span>
              <TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} />
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
          <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-8 flex flex-col justify-between gap-2 p-3 min-h-0">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <div className="text-xs text-[#8A9BB8]">
                    Tổng {slice.length} ngày có số liệu (cửa sổ {rangeInfo.label})
                  </div>
                  <div className="font-mono-num text-2xl font-bold text-[#E6EDF7] mt-1">
                    {total !== null ? `${total.toFixed(1)} kWh` : '—'}
                  </div>
                </div>
                {delta !== null && (
                  <StatusPill
                    label={`${delta >= 0 ? '+' : ''}${delta}% so với kỳ trước`}
                    color={delta > 10 ? SH.Red : delta < 0 ? SH.Green : SH.Blue}
                  />
                )}
              </div>

              {values.length > 0 ? (
                <BarChart values={values} labels={labels} color={SH.Blue} unit="kWh" />
              ) : (
                <div className="py-10 text-center text-xs text-[#8A9BB8]">
                  Chưa có chuỗi ngày trong topic dien/lichsu.
                </div>
              )}
            </SmartCard>

            <SmartCard className="lg:col-span-4 space-y-4 flex flex-col justify-between">
              <ArcGauge
                fraction={gaugeFraction}
                valueText={latestDayVal}
                label={dien.ngay ? `Sản lượng ${dien.ngay}` : 'Sản lượng ngày gần nhất'}
                color={SH.Blue}
              />
              <div className="space-y-2 border-t border-[#253459] pt-3 text-xs">
                <div className="flex items-center justify-between">
                  <span className="text-[#8A9BB8]">Chi phí tháng này đến giờ</span>
                  <span className="font-mono-num font-bold text-[#22C55E]">
                    {dien.thangTien !== null
                      ? `${Math.round(dien.thangTien).toLocaleString('vi-VN')} đ`
                      : dien.thangSanLuong !== null
                        ? `${dien.thangSanLuong.toFixed(1)} kWh`
                        : '—'}
                  </span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="text-[#8A9BB8]">Trạng thái</span>
                  <StatusPill label={statusText} color={statusOk ? SH.Green : SH.Amber} />
                </div>
              </div>
            </SmartCard>
          </div>
        )}
      </div>
    );
  };

  // WATER tab uses strict real AquaControl telemetry (khong bia so, khong DemoBadge)
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
        ? { label: nhanKetNoiBom(ketNoiBom, kenhBom), color: SH.TextOff }
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
            <BarChart values={s.bars} labels={s.labels} color={s.kind.color} unit={s.unit} />
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
                onClick={() => setRangeId(r.id)}
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
