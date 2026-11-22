import React, { useMemo, useState } from 'react';
import { Calendar, ChevronLeft, ChevronRight, Sparkles } from 'lucide-react';
import { AmLich } from '../data/amLich.js';
import { FlatButton, SectionHeader, SmartCard, StatusPill } from '../components/Common.js';
import { SH, tint } from '../theme/colors.js';

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

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

  const prevMonth = () => {
    if (viewMonth === 1) {
      setViewYear((y) => y - 1);
      setViewMonth(12);
    } else {
      setViewMonth((m) => m - 1);
    }
  };

  const nextMonth = () => {
    if (viewMonth === 12) {
      setViewYear((y) => y + 1);
      setViewMonth(1);
    } else {
      setViewMonth((m) => m + 1);
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

  const gridCells = useMemo(() => {
    const total = daysInMonth(viewYear, viewMonth);
    const firstDowJs = new Date(viewYear, viewMonth - 1, 1).getDay(); // 0=CN..6=T7
    const leadingBlanks = (firstDowJs + 6) % 7; // Mon=0..Sun=6
    const cells: Array<{
      dd: number;
      mm: number;
      yy: number;
      am: ReturnType<typeof AmLich.tuDuongLich>;
      le: string | null;
      isToday: boolean;
      isSelected: boolean;
    } | null> = [];

    for (let i = 0; i < leadingBlanks; i++) cells.push(null);
    for (let d = 1; d <= total; d++) {
      const am = AmLich.tuDuongLich(d, viewMonth, viewYear);
      const le = AmLich.suKien(d, viewMonth, am);
      cells.push({
        dd: d,
        mm: viewMonth,
        yy: viewYear,
        am,
        le,
        isToday:
          d === today.getDate() &&
          viewMonth === today.getMonth() + 1 &&
          viewYear === today.getFullYear(),
        isSelected: d === selDay && viewMonth === selMonth && viewYear === selYear,
      });
    }
    return cells;
  }, [viewYear, viewMonth, selDay, selMonth, selYear, today]);

  const selectedDetail = useMemo(() => {
    const clampedDay = Math.min(selDay, daysInMonth(selYear, selMonth));
    const am = AmLich.tuDuongLich(clampedDay, selMonth, selYear);
    const canChi = AmLich.canChi(am);
    const hoangDao = AmLich.gioHoangDao(canChi.chiNgay);
    const le = AmLich.suKien(clampedDay, selMonth, am);
    return { dd: clampedDay, mm: selMonth, yy: selYear, am, canChi, hoangDao, le };
  }, [selDay, selMonth, selYear]);

  return (
    <div className="lg:h-full lg:overflow-hidden grid grid-cols-1 lg:grid-cols-12 gap-3">
      {/* Left: Full Month Solar + Lunar Calendar */}
      <SmartCard className="lg:col-span-8 flex flex-col min-h-0 gap-2.5 p-3">
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

        {/* Day grid stretching equally to fit viewport */}
        <div className="flex-1 min-h-0 grid grid-cols-7 auto-rows-fr gap-1.5">
          {gridCells.map((cell, idx) => {
            if (!cell) return <div key={`blank-${idx}`} className="h-full min-h-[54px] rounded-xl bg-[#0E1627]/35" />;
            const mocAm = AmLich.laMocAm(cell.am);
            return (
              <button
                key={cell.dd}
                type="button"
                onClick={() => {
                  setSelDay(cell.dd);
                  setSelMonth(cell.mm);
                  setSelYear(cell.yy);
                }}
                className="relative flex flex-col items-start justify-between rounded-xl p-1.5 h-full min-h-[54px] text-left transition-all cursor-pointer"
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
        <SmartCard className="space-y-3">
          <SectionHeader
            title={`Ngày ${selectedDetail.dd}/${selectedDetail.mm}/${selectedDetail.yy}`}
            subtitle="Chi tiết Âm lịch (Hồ Ngọc Đức · GMT+7)"
          />

          <div className="rounded-xl border border-[#253459] bg-[#0E1627] p-3.5 space-y-2">
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

          {selectedDetail.le && (
            <div className="flex items-center gap-2 rounded-xl border border-[#F59E0B]/40 bg-[#F59E0B]/10 px-3 py-2 text-xs text-[#FCD34D]">
              <Sparkles size={14} className="shrink-0" />
              <span>Ngày lễ / sự kiện: {selectedDetail.le}</span>
            </div>
          )}

          <div className="space-y-2">
            <div className="text-xs font-bold text-[#E6EDF7]">Giờ Hoàng Đạo trong ngày</div>
            <div className="flex flex-wrap gap-1.5">
              {selectedDetail.hoangDao.map((gio) => (
                <StatusPill key={gio} label={gio} color={SH.Green} dot={false} />
              ))}
            </div>
          </div>
        </SmartCard>

        <SmartCard className="space-y-3">
          <SectionHeader title="Tra cứu nhanh ngày Dương → Âm" />
          <input
            type="date"
            value={`${viewYear}-${String(viewMonth).padStart(2, '0')}-${String(selDay).padStart(2, '0')}`}
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
  );
}

