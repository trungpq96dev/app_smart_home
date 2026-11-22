import React from 'react';
import { SH, tint } from '../theme/colors.js';

export type BarLabelMode = 'none' | 'all' | 'key_points';

function formatBarVal(v: number): string {
  if (v >= 100) return String(Math.round(v));
  return v.toFixed(1).replace(/\.0$/, '');
}

export function BarChart({
  values,
  labels,
  color,
  unit = '',
  height = 200,
  highlightIndex,
  selectedIndex,
  onSelectIndex,
  labelMode = 'none',
  averageValue,
  averageLabel,
  startAtZero = false,
}: {
  values: number[];
  labels: string[];
  color: string;
  unit?: string;
  height?: number;
  highlightIndex?: number | null;
  selectedIndex?: number | null;
  onSelectIndex?: (idx: number) => void;
  labelMode?: BarLabelMode;
  averageValue?: number | null;
  averageLabel?: string;
  startAtZero?: boolean;
}) {
  const uid = React.useId().replace(/[^a-zA-Z0-9_-]/g, '');
  if (values.length === 0) return null;

  const activeIdx =
    selectedIndex !== undefined
      ? selectedIndex
      : highlightIndex !== undefined
        ? highlightIndex
        : null;
  const hasActive = activeIdx !== null && activeIdx >= 0 && activeIdx < values.length;

  const maxVal = Math.max(...values, averageValue ?? 0, 0.001);
  const minVal = Math.max(0, Math.min(...values));
  const floor = !startAtZero && minVal / maxVal > 0.6 ? minVal * 0.92 : 0;
  const span = Math.max(maxVal - floor, 0.001);

  const w = Math.max(360, values.length * (values.length <= 7 ? 72 : 34));
  const padX = 14;
  const padTop = labelMode !== 'none' ? 28 : 16;
  const padBottom = 26;
  const plotH = Math.max(40, height - padTop - padBottom);
  const barStep = (w - padX * 2) / values.length;
  const barW = Math.min(values.length <= 7 ? 56 : 26, Math.max(8, barStep * (values.length <= 7 ? 0.56 : 0.64)));

  const peakIdx = values.reduce((best, val, idx) => (val > values[best] ? idx : best), 0);
  const keyLabelIndices = (() => {
    if (labelMode === 'none') return new Set<number>();
    if (labelMode === 'all') return new Set(values.map((_, i) => i));
    const chosen: number[] = [];
    const addIfRoom = (idx: number | null) => {
      if (idx === null || idx < 0 || idx >= values.length || chosen.includes(idx)) return;
      const cx = padX + idx * barStep + barStep / 2;
      const minDist = values.length > 20 ? 34 : 26;
      if (chosen.some((other) => Math.abs(padX + other * barStep + barStep / 2 - cx) < minDist)) {
        return;
      }
      chosen.push(idx);
    };
    addIfRoom(activeIdx);
    addIfRoom(peakIdx);
    addIfRoom(values.length - 1);
    return new Set(chosen);
  })();

  const avgY =
    averageValue !== null && averageValue !== undefined && averageValue > 0
      ? padTop + plotH * (1 - Math.min(1, Math.max(0, (averageValue - floor) / span)))
      : null;

  const gradId = `barGrad-${uid}`;

  return (
    <div className="w-full overflow-x-auto select-none">
      <svg
        viewBox={`0 0 ${w} ${height}`}
        className="w-full"
        style={{ minWidth: values.length > 14 ? 520 : 300 }}
      >
        <defs>
          <linearGradient id={gradId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={color} stopOpacity={1} />
            <stop offset="100%" stopColor={color} stopOpacity={0.55} />
          </linearGradient>
        </defs>

        {values.map((val, i) => {
          const frac = Math.min(1, Math.max(0.04, (val - floor) / span));
          const bh = Math.max(4, frac * plotH);
          const slotX = padX + i * barStep;
          const x = slotX + (barStep - barW) / 2;
          const y = padTop + plotH - bh;
          const isSelected = activeIdx === i;
          const dimmed = hasActive && !isSelected;
          const lbl = labels[i] ?? '';
          const showTopLabel = keyLabelIndices.has(i);
          const rx = Math.min(barW * 0.28, bh / 2);

          return (
            <g
              key={i}
              onClick={() => onSelectIndex?.(i)}
              className={onSelectIndex ? 'cursor-pointer' : undefined}
            >
              {/* Full slot hit target */}
              <rect x={slotX} y={0} width={barStep} height={height} fill="transparent" />

              {/* Subtle backdrop around active column */}
              {isSelected && (
                <rect
                  x={x - Math.min(8, (barStep - barW) * 0.35)}
                  y={4}
                  width={barW + Math.min(16, (barStep - barW) * 0.7)}
                  height={height - 6}
                  rx={12}
                  fill="rgba(255,255,255,0.05)"
                />
              )}

              {/* Bar track */}
              <rect
                x={x}
                y={padTop}
                width={barW}
                height={plotH}
                rx={barW * 0.28}
                fill={SH.Card2}
                opacity={dimmed ? 0.5 : 0.9}
              />

              {/* Data bar */}
              <rect
                x={x}
                y={y}
                width={barW}
                height={bh}
                rx={rx}
                fill={`url(#${gradId})`}
                opacity={dimmed ? 0.45 : 1}
              />

              {/* Active glow outline */}
              {isSelected && (
                <>
                  <rect
                    x={x}
                    y={y}
                    width={barW}
                    height={bh}
                    rx={rx}
                    fill="none"
                    stroke="#60A5FA"
                    strokeOpacity={0.38}
                    strokeWidth={4.5}
                  />
                  <rect
                    x={x}
                    y={y}
                    width={barW}
                    height={bh}
                    rx={rx}
                    fill="none"
                    stroke="#93C5FD"
                    strokeWidth={2}
                  />
                </>
              )}

              {/* Top value label */}
              {showTopLabel && (
                <text
                  x={x + barW / 2}
                  y={Math.max(14, y - 7)}
                  textAnchor="middle"
                  fill={isSelected ? '#FFFFFF' : '#CBD5E1'}
                  opacity={dimmed ? 0.5 : 1}
                  fontSize={isSelected ? '11.5' : '10.5'}
                  fontWeight={isSelected ? '700' : '500'}
                >
                  {formatBarVal(val)}
                  {isSelected && unit ? ` ${unit}` : ''}
                </text>
              )}

              {/* Bottom axis label */}
              {lbl && (
                <text
                  x={x + barW / 2}
                  y={height - 7}
                  textAnchor="middle"
                  fill={isSelected ? '#FFFFFF' : SH.TextOff}
                  opacity={dimmed ? 0.55 : 1}
                  fontSize="10.5"
                  fontWeight={isSelected ? '700' : '500'}
                >
                  {lbl}
                </text>
              )}
            </g>
          );
        })}

        {/* Average dashed line + right edge label rendered above bars */}
        {avgY !== null && (
          <g pointerEvents="none">
            <line
              x1={padX}
              x2={w - padX}
              y1={avgY}
              y2={avgY}
              stroke="#CBD5E1"
              strokeOpacity={0.45}
              strokeDasharray="5 4"
              strokeWidth={1.1}
            />
            {averageLabel && (() => {
              const pillW = averageLabel.length * 6.1 + 10;
              const pillX = Math.max(padX, w - padX - pillW - 2);
              return (
                <g>
                  <rect
                    x={pillX}
                    y={Math.max(2, avgY - 19)}
                    width={pillW}
                    height={16}
                    rx={4}
                    fill={SH.Card}
                    fillOpacity={0.88}
                  />
                  <text
                    x={pillX + pillW - 5}
                    y={Math.max(13, avgY - 7)}
                    textAnchor="end"
                    fill="#CBD5E1"
                    fontSize="10"
                    fontWeight="500"
                  >
                    {averageLabel}
                  </text>
                </g>
              );
            })()}
          </g>
        )}
      </svg>
    </div>
  );
}

export function ArcGauge({
  fraction,
  valueText,
  label,
  color,
  size = 136,
}: {
  fraction: number;
  valueText: string;
  label: string;
  color: string;
  size?: number;
}) {
  const f = Math.min(1, Math.max(0, fraction));
  const stroke = 11;
  const r = (size - stroke) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;
  const sweep = circumference * 0.72;
  const dashOffset = sweep * (1 - f);

  return (
    <div className="flex flex-col items-center">
      <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-[220deg]">
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke="#253459"
            strokeWidth={stroke}
            strokeDasharray={`${sweep} ${circumference}`}
            strokeLinecap="round"
          />
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke={color}
            strokeWidth={stroke}
            strokeDasharray={`${sweep} ${circumference}`}
            strokeDashoffset={dashOffset}
            strokeLinecap="round"
            style={{ transition: 'stroke-dashoffset 0.4s ease' }}
          />
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-2">
          <span className="font-mono-num text-lg font-bold text-[#E6EDF7] leading-tight">{valueText}</span>
          <span className="text-[11px] text-[#8A9BB8] mt-1">{label}</span>
        </div>
      </div>
    </div>
  );
}

export function ThinBar({
  label,
  valueText,
  fraction,
  color,
}: {
  label: string;
  valueText: string;
  fraction: number;
  color: string;
}) {
  const pct = Math.min(100, Math.max(0, fraction * 100));
  return (
    <div>
      <div className="flex items-center justify-between text-xs mb-1.5">
        <span className="text-[#8A9BB8] font-medium truncate">{label}</span>
        <span className="font-mono-num font-bold text-[#E6EDF7] shrink-0 ml-2">{valueText}</span>
      </div>
      <div className="h-2 w-full rounded-full bg-[#253459] overflow-hidden">
        <div
          className="h-full rounded-full transition-all duration-300"
          style={{ width: `${pct}%`, backgroundColor: color }}
        />
      </div>
    </div>
  );
}

export function DualLineAreaChart({
  labels,
  primary,
  secondary,
  primaryColor = SH.Blue,
  secondaryColor = SH.Teal,
  height = 170,
}: {
  labels: string[];
  primary: number[];
  secondary?: number[];
  primaryColor?: string;
  secondaryColor?: string;
  height?: number;
}) {
  const n = primary.length;
  if (n < 2) return null;
  const w = 480;
  const padX = 18;
  const padTop = 14;
  const padBot = 24;
  const plotW = w - padX * 2;
  const plotH = height - padTop - padBot;
  const maxVal = Math.max(...primary, ...(secondary ?? []), 0.1);

  const pts = (arr: number[]) =>
    arr.map((v, i) => {
      const x = padX + (i / (n - 1)) * plotW;
      const y = padTop + plotH * (1 - v / maxVal);
      return `${x.toFixed(1)},${y.toFixed(1)}`;
    });

  const pPts = pts(primary);
  const pLine = `M ${pPts.join(' L ')}`;
  const pArea = `${pLine} L ${(padX + plotW).toFixed(1)},${(padTop + plotH).toFixed(1)} L ${padX},${(padTop + plotH).toFixed(1)} Z`;

  const sLine = secondary && secondary.length === n ? `M ${pts(secondary).join(' L ')}` : null;

  return (
    <svg viewBox={`0 0 ${w} ${height}`} className="w-full">
      {[0.25, 0.5, 0.75, 1].map((f) => (
        <line
          key={f}
          x1={padX}
          x2={w - padX}
          y1={padTop + plotH * (1 - f)}
          y2={padTop + plotH * (1 - f)}
          stroke={SH.Border}
          strokeDasharray="3 4"
        />
      ))}
      <path d={pArea} fill={tint(primaryColor, 0.18)} />
      <path d={pLine} fill="none" stroke={primaryColor} strokeWidth={2.5} strokeLinejoin="round" />
      {sLine && <path d={sLine} fill="none" stroke={secondaryColor} strokeWidth={2} strokeLinejoin="round" />}
      {labels.map((lbl, i) => {
        if (!lbl) return null;
        const step = labels.length > 10 && i % 2 !== 0 ? false : true;
        if (!step) return null;
        const x = padX + (i / Math.max(1, labels.length - 1)) * plotW;
        return (
          <text key={i} x={x} y={height - 6} textAnchor="middle" fill={SH.TextDim} fontSize="10">
            {lbl}
          </text>
        );
      })}
    </svg>
  );
}
