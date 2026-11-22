import React, { useState } from 'react';
import { Bolt, ChevronRight, TrendingDown, TrendingUp } from 'lucide-react';
import { DemoData } from '../data/demoData.js';
import {
  chenhPhanTramDien,
  coSoLieuDien,
  doTuoiDien,
  tongKwhDien,
} from '../data/dien.js';
import {
  STAT_KINDS,
  STAT_RANGES,
  type StatKindId,
  type StatRangeId,
} from '../data/models.js';
import {
  ChoiceChip,
  DemoBadge,
  EmptyState,
  FlatButton,
  SectionHeader,
  SmartCard,
  StatusPill,
  TrangThaiKetNoi,
} from '../components/Common.js';
import { ArcGauge, BarChart } from '../components/Charts.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function StatsScreen() {
  const { dien, mqttStatus, nowMs, openChiTietDien, navigateDest } = useSmartHome();
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

  // Other 4 tabs use seeded DemoData + DemoBadge
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
      </div>

      {kindId === 'POWER' ? renderRealPowerStats() : renderDemoStats()}
    </div>
  );
}

