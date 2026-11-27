import React, { useEffect, useMemo, useState } from 'react';
import { Bolt, CalendarRange } from 'lucide-react';
import { coSoLieuDien, dinhDangDdMm, doTuoiDien, trungBinhKwh } from '../data/dien.js';
import { BarChart } from '../components/Charts.js';
import { ChoiceChip, EmptyState, SectionHeader, SmartCard, StatusPill, TrangThaiKetNoi } from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

const THU_VN_FULL = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

function formatDdMmYyyy(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim());
  return m ? `${m[3]}/${m[2]}/${m[1]}` : iso;
}

function thuDayDuVn(iso: string): string {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso.trim());
  if (!m) return '';
  const dt = new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]));
  return THU_VN_FULL[dt.getDay()] ?? '';
}

export function ChiTietDienScreen() {
  const { dien, mqttStatus, nowMs } = useSmartHome();
  const [selectedIdx, setSelectedIdx] = useState<number | null>(null);
  const [fromDate, setFromDate] = useState<string>('');
  const [toDate, setToDate] = useState<string>('');
  const [quickRange, setQuickRange] = useState<'7' | '14' | 'all'>('all');

  const minDate = dien.lichSu[0]?.ngay ?? '';
  const maxDate = dien.lichSu[dien.lichSu.length - 1]?.ngay ?? '';

  useEffect(() => {
    if (dien.lichSu.length === 0) return;
    if (!fromDate || !toDate) {
      setFromDate(minDate);
      setToDate(maxDate);
      setQuickRange('all');
    }
  }, [dien.lichSu.length, minDate, maxDate, fromDate, toDate]);

  const applyQuick = (mode: '7' | '14' | 'all') => {
    setQuickRange(mode);
    setSelectedIdx(null);
    if (dien.lichSu.length === 0) return;
    if (mode === 'all') {
      setFromDate(minDate);
      setToDate(maxDate);
    } else {
      const n = mode === '7' ? 7 : 14;
      const startItem = dien.lichSu[Math.max(0, dien.lichSu.length - n)] ?? dien.lichSu[0];
      setFromDate(startItem.ngay);
      setToDate(maxDate);
    }
  };

  const filtered = useMemo(() => {
    return dien.lichSu.filter((d) => {
      if (fromDate && d.ngay < fromDate) return false;
      if (toDate && d.ngay > toDate) return false;
      return true;
    });
  }, [dien.lichSu, fromDate, toDate]);

  const filteredTotal = useMemo(
    () => filtered.reduce((sum, item) => sum + item.kwh, 0),
    [filtered],
  );
  const filteredAvg = useMemo(() => trungBinhKwh(filtered), [filtered]);

  const hasData = coSoLieuDien(dien);
  const tuoi = doTuoiDien(dien, new Date(nowMs));
  const tuoiLabel =
    tuoi === 'MOI'
      ? 'Mới'
      : tuoi === 'HOI_CU'
        ? 'Hơi cũ 8–24h'
        : tuoi === 'QUA_CU'
          ? 'Cũ >24h'
          : 'Chưa rõ mốc giờ';

  const labels = filtered.map((d) => dinhDangDdMm(d.ngay));
  const values = filtered.map((d) => d.kwh);

  const activeIdx = selectedIdx !== null && selectedIdx < filtered.length ? selectedIdx : null;
  const activeItem =
    activeIdx !== null ? filtered[activeIdx] : filtered[filtered.length - 1] ?? null;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      <SmartCard className="shrink-0 flex flex-wrap items-center justify-between gap-2 p-2.5">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h2 className="text-sm font-bold text-[#E6EDF7]">Chi tiết sản lượng & hóa đơn EVN</h2>
            <TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} />
          </div>
          <p className="text-[11px] text-[#8A9BB8] mt-0.5">
            {dien.capNhat
              ? `Đồng bộ lúc ${dien.capNhat} (${tuoiLabel})`
              : 'Chưa có mốc đồng bộ từ máy chủ EVN'}
            {minDate && maxDate
              ? ` · Dữ liệu đã tải: ${formatDdMmYyyy(minDate)} → ${formatDdMmYyyy(maxDate)}`
              : ''}
          </p>
        </div>
        {(dien.loi || dien.trangThai === 'loi') && (
          <StatusPill
            label={dien.loi ? `Lỗi EVN: ${dien.loi}` : 'Cữ gần nhất lỗi (xem log serverubuntu)'}
            color={SH.Red}
          />
        )}
      </SmartCard>

      {!hasData ? (
        <div className="flex-1 min-h-0 flex items-center justify-center">
          <EmptyState
            icon={<Bolt size={28} />}
            title="Chưa nhận được số liệu điện"
            description="Kiểm tra kết nối MQTT và container dien-evn trên máy chủ gia đình."
          />
        </div>
      ) : (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          {/* Top KPI Row including Tổng trong khoảng */}
          <div className="shrink-0 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-2.5">
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Hôm nay / Gần nhất</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {dien.ngaySanLuong !== null ? `${dien.ngaySanLuong.toFixed(2)} kWh` : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">{dien.ngay ? formatDdMmYyyy(dien.ngay) : '—'}</div>
            </SmartCard>

            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Sản lượng tháng</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">
                {dien.thangSanLuong !== null ? `${dien.thangSanLuong.toFixed(1)} kWh` : '—'}
              </div>
              <div className="text-[11px] text-[#22C55E]">
                {dien.thangTien !== null ? `${Math.round(dien.thangTien).toLocaleString('vi-VN')} đ` : 'Chưa tính tiền'}
              </div>
            </SmartCard>

            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Tổng trong khoảng</div>
              <div className="font-mono-num text-lg font-bold text-[#22D3EE] mt-0.5">
                {filtered.length > 0 ? `${filteredTotal.toFixed(2)} kWh` : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">{filtered.length} ngày đang lọc</div>
            </SmartCard>

            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Chỉ số công tơ</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {dien.congToChiSo !== null ? `${dien.congToChiSo.toFixed(1)}` : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">kWh tích lũy</div>
            </SmartCard>

            <SmartCard className="p-2.5 col-span-2 sm:col-span-1">
              <div className="text-xs text-[#8A9BB8]">Công nợ EVN</div>
              <div className="font-mono-num text-lg font-bold text-[#F59E0B] mt-0.5">
                {dien.congNoSoTien !== null
                  ? `${Math.round(dien.congNoSoTien).toLocaleString('vi-VN')} đ`
                  : '—'}
              </div>
              <div className="text-[11px] text-[#5F6F8C]">Số dư phải thanh toán</div>
            </SmartCard>
          </div>

          {/* Date Range Filter Bar */}
          {dien.lichSu.length > 0 && (
            <SmartCard className="shrink-0 flex flex-wrap items-center justify-between gap-2.5 p-2.5">
              <div className="flex flex-wrap items-center gap-2 text-xs">
                <span className="inline-flex items-center gap-1.5 font-semibold text-[#E6EDF7]">
                  <CalendarRange size={14} className="text-[#4F8BFF]" />
                  <span>Khoảng ngày:</span>
                </span>
                <label className="inline-flex items-center gap-1.5 text-[#8A9BB8]">
                  <span>Từ ngày</span>
                  <input
                    type="date"
                    min={minDate}
                    max={toDate || maxDate}
                    value={fromDate}
                    onChange={(e) => {
                      setFromDate(e.target.value);
                      setQuickRange('all');
                      setSelectedIdx(null);
                    }}
                    className="rounded-lg border border-[#253459] bg-[#0E1627] px-2 py-1 text-xs text-[#E6EDF7] font-mono-num outline-none focus:border-[#4F8BFF]"
                  />
                </label>
                <label className="inline-flex items-center gap-1.5 text-[#8A9BB8]">
                  <span>Đến ngày</span>
                  <input
                    type="date"
                    min={fromDate || minDate}
                    max={maxDate}
                    value={toDate}
                    onChange={(e) => {
                      setToDate(e.target.value);
                      setQuickRange('all');
                      setSelectedIdx(null);
                    }}
                    className="rounded-lg border border-[#253459] bg-[#0E1627] px-2 py-1 text-xs text-[#E6EDF7] font-mono-num outline-none focus:border-[#4F8BFF]"
                  />
                </label>
              </div>

              <div className="flex items-center gap-1.5">
                <ChoiceChip label="7 ngày" selected={quickRange === '7'} color={SH.Blue} onClick={() => applyQuick('7')} />
                <ChoiceChip label="14 ngày" selected={quickRange === '14'} color={SH.Blue} onClick={() => applyQuick('14')} />
                <ChoiceChip label="Cả kỳ" selected={quickRange === 'all' && fromDate === minDate && toDate === maxDate} color={SH.Blue} onClick={() => applyQuick('all')} />
              </div>
            </SmartCard>
          )}

          {dien.lichSu.length > 0 && (
            <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
              <SmartCard className="lg:col-span-8 flex flex-col justify-between gap-2 p-3 min-h-0">
                <SectionHeader
                  title={`Biểu đồ từng ngày (${filtered.length} ngày)`}
                  subtitle={
                    activeItem
                      ? `${thuDayDuVn(activeItem.ngay)}, ${formatDdMmYyyy(activeItem.ngay)}: ${activeItem.kwh.toFixed(2)} kWh`
                      : 'Bấm vào cột hoặc dòng để xem'
                  }
                />
                {filtered.length > 0 ? (
                  <div className="flex-1 min-h-0 flex items-center">
                    <BarChart
                      values={values}
                      labels={labels}
                      color={SH.Blue}
                      unit="kWh"
                      labelMode={filtered.length <= 10 ? 'all' : 'key_points'}
                      averageValue={filteredAvg}
                      averageLabel={filteredAvg !== null ? `TB: ${filteredAvg.toFixed(1)} kWh` : undefined}
                      selectedIndex={activeIdx}
                      onSelectIndex={(idx) => setSelectedIdx((prev) => (prev === idx ? null : idx))}
                      startAtZero
                    />
                  </div>
                ) : (
                  <div className="py-10 text-center text-xs text-[#8A9BB8]">
                    Không có ngày nào nằm trong khoảng đã chọn.
                  </div>
                )}
              </SmartCard>

              <SmartCard className="lg:col-span-4 flex flex-col min-h-0 p-3">
                <div className="text-xs font-bold text-[#E6EDF7] pb-2 border-b border-[#253459] shrink-0">
                  Bảng sản lượng từng ngày
                </div>
                <div className="flex-1 min-h-0 overflow-y-auto pt-1 divide-y divide-[#253459]/70">
                  {[...filtered].reverse().map((row, revIdx) => {
                    const origIdx = filtered.length - 1 - revIdx;
                    const active = activeIdx === origIdx;
                    const thu = thuDayDuVn(row.ngay);
                    return (
                      <button
                        key={row.ngay}
                        type="button"
                        onClick={() => setSelectedIdx((prev) => (prev === origIdx ? null : origIdx))}
                        className={`w-full flex items-center justify-between px-2.5 py-2 text-xs rounded-lg transition-colors cursor-pointer ${
                          active ? 'bg-[#4F8BFF]/15 text-white' : 'hover:bg-[#0E1627] text-[#E6EDF7]'
                        }`}
                      >
                        <div className="text-left">
                          <div className="font-mono-num font-semibold">{formatDdMmYyyy(row.ngay)}</div>
                          {thu && <div className="text-[10px] text-[#8A9BB8]">{thu}</div>}
                        </div>
                        <span className="font-mono-num font-bold text-[#4F8BFF]">{row.kwh.toFixed(2)} kWh</span>
                      </button>
                    );
                  })}
                </div>
              </SmartCard>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
