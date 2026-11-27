import React, { useMemo, useState } from 'react';
import { Calendar, ChevronLeft, ChevronRight, Moon, Sparkles, Sun, Timer } from 'lucide-react';
import { AmLich } from '../data/amLich.js';
import { FlatButton, SectionHeader, SmartCard, StatusPill } from '../components/Common.js';
import { StatTile } from '../components/Tiles.js';
import { SH, tint } from '../theme/colors.js';

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const THU_DAY_DU = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

export function TienIchScreen() {
  const today = useMemo(() => new Date(), []);
  const [viewYear, setViewYear] = useState(today.getFullYear());
  const [viewMonth, setViewMonth] = useState(today.getMonth() + 1);
  const [selDay, setSelDay] = useState(today.getDate());
  const [selMonth, setSelMonth] = useState(today.getMonth() + 1);
  const [selYear, setSelYear] = useState(today.getFullYear());

  const jumpToMonth = (yy: number, mm: number, selectInMonth = false) => {
    const clamped = Math.min(selDay, daysInMonth(yy, mm));
    setViewYear(yy);
    setViewMonth(mm);
    if (selectInMonth) {
      setSelYear(yy);
      setSelMonth(mm);
      setSelDay(clamped);
    }
  };

  const prevMonth = () => {
    if (viewMonth === 1) {
      jumpToMonth(viewYear - 1, 12, false);
    } else {
      jumpToMonth(viewYear, viewMonth - 1, false);
    }
  };

  const nextMonth = () => {
    if (viewMonth === 12) {
      jumpToMonth(viewYear + 1, 1, false);
    } else {
      jumpToMonth(viewYear, viewMonth + 1, false);
    }
  };

  const goToday = () => {
    const d = new Date();
    setViewYear(d.getFullYear());
    setViewMonth(d.getMonth() + 1);
    setSelDay(d.getDate());
    setSelMonth(d.getMonth() + 1);
    setSelYear(d.getFullYear());
  };

  const todaySummary = useMemo(() => {
    const dd = today.getDate();
    const mm = today.getMonth() + 1;
    const yy = today.getFullYear();
    const am = AmLich.tuDuongLich(dd, mm, yy);
    const cc = AmLich.canChi(am);

    let nextMoc: { soNgay: number; nhan: string; ngayDuong: string } | null = null;
    for (let offset = 0; offset <= 31; offset++) {
      const dt = new Date(yy, mm - 1, dd + offset);
      const d2 = dt.getDate();
      const m2 = dt.getMonth() + 1;
      const y2 = dt.getFullYear();
      const am2 = AmLich.tuDuongLich(d2, m2, y2);
      if (am2.ngay === 1 || am2.ngay === 15) {
        nextMoc = {
          soNgay: offset,
          nhan: am2.ngay === 1 ? `Mùng 1/${am2.thang}${am2.nhuan ? ' nhuận' : ''} ÂL` : `Rằm tháng ${am2.thang}${am2.nhuan ? ' nhuận' : ''} ÂL`,
          ngayDuong: `${d2}/${m2}`,
        };
        break;
      }
    }

    return {
      solarStr: `${String(dd).padStart(2, '0')}/${String(mm).padStart(2, '0')}/${yy}`,
      thuStr: THU_DAY_DU[today.getDay()],
      lunarStr: `${am.ngay}/${am.thang}${am.nhuan ? ' nhuận' : ''}`,
      cc,
      nextMoc,
    };
  }, [today]);

  // Fixed 6x7 = 42 cells with clickable dimmed adjacent-month days
  const gridCells = useMemo(() => {
    const total = daysInMonth(viewYear, viewMonth);
    const firstDowJs = new Date(viewYear, viewMonth - 1, 1).getDay(); // 0=CN..6=T7
    const leadingCount = (firstDowJs + 6) % 7; // Mon=0..Sun=6

    const prevYy = viewMonth === 1 ? viewYear - 1 : viewYear;
    const prevMm = viewMonth === 1 ? 12 : viewMonth - 1;
    const prevTotal = daysInMonth(prevYy, prevMm);

    const nextYy = viewMonth === 12 ? viewYear + 1 : viewYear;
    const nextMm = viewMonth === 12 ? 1 : viewMonth + 1;

    const makeCell = (dd: number, mm: number, yy: number, inCurrentMonth: boolean) => {
      const am = AmLich.tuDuongLich(dd, mm, yy);
      const le = AmLich.suKien(dd, mm, am);
      return {
        dd,
        mm,
        yy,
        inCurrentMonth,
        am,
        le,
        isToday:
          dd === today.getDate() &&
          mm === today.getMonth() + 1 &&
          yy === today.getFullYear(),
        isSelected: dd === selDay && mm === selMonth && yy === selYear,
      };
    };

    const cells: Array<ReturnType<typeof makeCell>> = [];
    for (let i = leadingCount - 1; i >= 0; i--) {
      cells.push(makeCell(prevTotal - i, prevMm, prevYy, false));
    }
    for (let d = 1; d <= total; d++) {
      cells.push(makeCell(d, viewMonth, viewYear, true));
    }
    let trailingDay = 1;
    while (cells.length < 42) {
      cells.push(makeCell(trailingDay++, nextMm, nextYy, false));
    }
    return cells;
  }, [viewYear, viewMonth, selDay, selMonth, selYear, today]);

  const selectedDetail = useMemo(() => {
    const clampedDay = Math.min(selDay, daysInMonth(selYear, selMonth));
    const am = AmLich.tuDuongLich(clampedDay, selMonth, selYear);
    const canChi = AmLich.canChi(am);
    const hoangDao = AmLich.gioHoangDao(canChi.chiNgay);
    const le = AmLich.suKien(clampedDay, selMonth, am);
    const thu = THU_DAY_DU[new Date(selYear, selMonth - 1, clampedDay).getDay()];
    return { dd: clampedDay, mm: selMonth, yy: selYear, thu, am, canChi, hoangDao, le };
  }, [selDay, selMonth, selYear]);

  const dateInputVal = `${selectedDetail.yy}-${String(selectedDetail.mm).padStart(2, '0')}-${String(selectedDetail.dd).padStart(2, '0')}`;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Top Row: 4 Today StatTiles */}
      <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
        <StatTile
          label="Dương lịch hôm nay"
          value={todaySummary.solarStr}
          color={SH.Blue}
          icon={<Sun size={15} />}
          subtitle={todaySummary.thuStr}
        />
        <StatTile
          label="Âm lịch hôm nay"
          value={todaySummary.lunarStr}
          unit="ÂL"
          color={SH.Amber}
          icon={<Moon size={15} />}
          subtitle={`Năm ${todaySummary.cc.nam}`}
        />
        <StatTile
          label="Ngày Can Chi"
          value={todaySummary.cc.ngay}
          color={SH.Purple}
          icon={<Sparkles size={15} />}
          subtitle={`Tháng ${todaySummary.cc.thang}`}
        />
        <StatTile
          label="Mùng 1 / Rằm kế tiếp"
          value={
            todaySummary.nextMoc
              ? todaySummary.nextMoc.soNgay === 0
                ? 'Hôm nay!'
                : `Còn ${todaySummary.nextMoc.soNgay} ngày`
              : '—'
          }
          color={SH.Green}
          icon={<Timer size={15} />}
          subtitle={
            todaySummary.nextMoc
              ? `${todaySummary.nextMoc.nhan} (${todaySummary.nextMoc.ngayDuong})`
              : undefined
          }
        />
      </div>

      {/* Main Split: 42-cell Calendar + Selected Day Details */}
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        {/* Left: Full Month Solar + Lunar Calendar */}
        <SmartCard className="lg:col-span-8 flex flex-col min-h-0 gap-2 p-3">
          <div className="shrink-0 flex flex-wrap items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Calendar size={17} className="text-[#4F8BFF]" />
              <h2 className="text-sm font-bold text-[#E6EDF7]">
                Lịch Vạn Niên — Tháng {viewMonth}/{viewYear}
              </h2>
            </div>

            <div className="flex items-center gap-1.5">
              <FlatButton color={SH.TextSecondary} onClick={prevMonth}>
                <ChevronLeft size={14} />
              </FlatButton>
              <FlatButton color={SH.Blue} onClick={goToday}>
                Hôm nay
              </FlatButton>
              <FlatButton color={SH.TextSecondary} onClick={nextMonth}>
                <ChevronRight size={14} />
              </FlatButton>
            </div>
          </div>

          {/* Weekday header */}
          <div className="shrink-0 grid grid-cols-7 gap-1.5 text-center text-xs font-bold text-[#8A9BB8]">
            {WEEKDAYS.map((w, i) => (
              <div key={w} className={i === 6 ? 'text-[#EF4444]' : i === 5 ? 'text-[#4F8BFF]' : ''}>
                {w}
              </div>
            ))}
          </div>

          {/* Fixed 6x7 = 42 Day grid */}
          <div className="flex-1 min-h-0 grid grid-cols-7 grid-rows-6 gap-1.5">
            {gridCells.map((cell, idx) => {
              const mocAm = AmLich.laMocAm(cell.am);
              return (
                <button
                  key={`${cell.yy}-${cell.mm}-${cell.dd}`}
                  type="button"
                  onClick={() => {
                    setSelDay(cell.dd);
                    setSelMonth(cell.mm);
                    setSelYear(cell.yy);
                    if (!cell.inCurrentMonth) {
                      setViewYear(cell.yy);
                      setViewMonth(cell.mm);
                    }
                  }}
                  className={`relative flex flex-col items-start justify-between rounded-xl p-1.5 h-full min-h-[46px] text-left transition-all cursor-pointer ${
                    cell.inCurrentMonth ? '' : 'opacity-40 hover:opacity-75'
                  }`}
                  style={{
                    backgroundColor: cell.isSelected
                      ? tint(SH.Blue, 0.22)
                      : cell.isToday
                        ? tint(SH.Green, 0.14)
                        : '#0E1627',
                    border: `1px solid ${
                      cell.isSelected ? SH.Blue : cell.isToday ? SH.Green : '#253459'
                    }`,
                  }}
                >
                  <div className="flex w-full items-center justify-between">
                    <span
                      className={`font-mono-num text-xs sm:text-sm font-bold ${
                        idx % 7 === 6 ? 'text-[#FCA5A5]' : 'text-[#E6EDF7]'
                      }`}
                    >
                      {cell.dd}
                    </span>
                    {cell.le && (
                      <span className="h-1.5 w-1.5 rounded-full bg-[#F59E0B]" title={cell.le} />
                    )}
                  </div>

                  <div className="w-full flex items-end justify-between gap-1">
                    <span
                      className="font-mono-num text-[10px] sm:text-[11px] font-semibold"
                      style={{ color: mocAm ? SH.Amber : '#8A9BB8' }}
                    >
                      {AmLich.nhanO(cell.am)}
                      {cell.am.nhuan ? '*' : ''}
                    </span>
                  </div>
                </button>
              );
            })}
          </div>
        </SmartCard>

        {/* Right: Selected Day Details + Date Lookup */}
        <div className="lg:col-span-4 flex flex-col min-h-0 gap-2.5 justify-between">
          <SmartCard className="space-y-2.5 p-3">
            <SectionHeader
              title={`${selectedDetail.thu}, ${selectedDetail.dd}/${selectedDetail.mm}/${selectedDetail.yy}`}
              subtitle="Chi tiết Âm lịch (Hồ Ngọc Đức · GMT+7)"
            />

            <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-[#8A9BB8]">Âm lịch</span>
                <span className="text-sm font-bold text-[#F59E0B]">
                  {selectedDetail.am.ngay}/{selectedDetail.am.thang}
                  {selectedDetail.am.nhuan ? ' (Nhuận)' : ''}/{selectedDetail.am.nam}
                </span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#8A9BB8]">Ngày Can Chi</span>
                <span className="font-semibold text-[#E6EDF7]">{selectedDetail.canChi.ngay}</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#8A9BB8]">Tháng Can Chi</span>
                <span className="font-semibold text-[#E6EDF7]">{selectedDetail.canChi.thang}</span>
              </div>
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#8A9BB8]">Năm Can Chi</span>
                <span className="font-semibold text-[#E6EDF7]">{selectedDetail.canChi.nam}</span>
              </div>
            </div>

            {selectedDetail.le ? (
              <div className="flex items-center gap-2 rounded-xl border border-[#F59E0B]/40 bg-[#F59E0B]/10 px-3 py-2 text-xs text-[#FCD34D]">
                <Sparkles size={14} className="shrink-0" />
                <span>Sự kiện: {selectedDetail.le}</span>
              </div>
            ) : (
              <div className="rounded-xl border border-[#253459] bg-[#0E1627]/70 px-3 py-2 text-xs text-[#8A9BB8]">
                Sự kiện: Không có
              </div>
            )}

            <div className="space-y-1.5">
              <div className="text-xs font-bold text-[#E6EDF7]">Giờ Hoàng Đạo trong ngày</div>
              <div className="flex flex-wrap gap-1.5">
                {selectedDetail.hoangDao.map((gio) => (
                  <StatusPill key={gio} label={gio} color={SH.Green} dot={false} />
                ))}
              </div>
            </div>
          </SmartCard>

          <SmartCard className="space-y-2.5 p-3">
            <SectionHeader title="Tra cứu nhanh ngày Dương → Âm" />
            <input
              type="date"
              value={dateInputVal}
              onChange={(e) => {
                const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(e.target.value);
                if (!m) return;
                const yy = Number(m[1]);
                const mm = Number(m[2]);
                const dd = Number(m[3]);
                setViewYear(yy);
                setViewMonth(mm);
                setSelYear(yy);
                setSelMonth(mm);
                setSelDay(dd);
              }}
              className="w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF]"
            />
          </SmartCard>
        </div>
      </div>
    </div>
  );
}
