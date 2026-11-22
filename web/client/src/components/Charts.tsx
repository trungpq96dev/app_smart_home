import React from 'react';
import { SH, tint } from '../theme/colors.js';

export function BarChart({
  values,
  labels,
  color,
  unit = '',
  height = 176,
  highlightIndex,
  onSelectIndex,
}: {
  values: number[];
  labels: string[];
  color: string;
  unit?: string;
  height?: number;
  highlightIndex?: number | null;
  onSelectIndex?: (idx: number) => void;
}) {
  if (values.length === 0) return null;
  const maxVal = Math.max(...values, 0.001);
  const w = Math.max(320, values.length * 36);
  const padX = 12;
  const padTop = 24;
  const padBottom = 26;
  const plotH = height - padTop - padBottom;
  const barStep = (w - padX * 2) / values.length;
  const barW = Math.min(26, Math.max(8, barStep * 0.56));

  return (
    <div className="w-full overflow-x-auto">
      <svg viewBox={`0 0 ${w} ${height}`} className="w-full" style={{ minWidth: values.length > 14 ? 480 : 280 }}>
        {/* Grid lines */}
        {[0.25, 0.5, 0.75, 1].map((f) => {
          const y = padTop + plotH * (1 - f);
          return (
            <line
              key={f}
              x1={padX}
              x2={w - padX}
              y1={y}
              y2={y}
              stroke={SH.Border}
              strokeDasharray="3 4"
              strokeWidth={1}
            />
          );
        })}

        {values.map((val, i) => {
          const ratio = Math.max(0.03, val / maxVal);
          const bh = ratio * plotH;
          const x = padX + i * barStep + (barStep - barW) / 2;
          const y = padTop + plotH - bh;
          const isHi = highlightIndex === i || (highlightIndex == null && i === values.length - 1);
          const lbl = labels[i] ?? '';
          return (
            <g
              key={i}
              onClick={() => onSelectIndex?.(i)}
              className={onSelectIndex ? 'cursor-pointer' : undefined}
            >
              <rect
                x={x}
                y={y}
                width={barW}
                height={bh}
                rx={Math.min(5, barW / 2)}
                fill={isHi ? color : tint(color, 0.45)}
              />
              {isHi && (
                <text
                  x={x + barW / 2}
                  y={Math.max(12, y - 6)}
                  textAnchor="middle"
                  fill={SH.TextPrimary}
                  fontSize="10"
                  fontWeight="700"
                >
                  {val >= 100 ? Math.round(val) : val.toFixed(1).replace(/\.0$/, '')}
                  {unit ? ` ${unit}` : ''}
                </text>
              )}
              {lbl && (
                <text
                  x={x + barW / 2}
                  y={height - 7}
                  textAnchor="middle"
                  fill={isHi ? SH.TextPrimary : SH.TextDim}
                  fontSize="10"
                  fontWeight={isHi ? '700' : '500'}
                >
                  {lbl}
                </text>
              )}
            </g>
          );
        })}
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

