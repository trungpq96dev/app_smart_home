import React from 'react';
import { Minus, Plus } from 'lucide-react';
import { SH, tint } from '../theme/colors.js';
import { IconBadge, RoundIconButton, SmartCard } from './Common.js';

export function SwitchToggle({
  checked,
  onChange,
  color = SH.Blue,
  disabled = false,
  pending = false,
  title,
}: {
  checked: boolean;
  onChange: () => void;
  color?: string;
  disabled?: boolean;
  pending?: boolean;
  title?: string;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled || pending}
      title={title}
      onClick={(e) => {
        e.stopPropagation();
        onChange();
      }}
      className="relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors duration-200 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
      style={{
        backgroundColor: checked ? color : '#253459',
        boxShadow: checked ? `0 0 12px ${tint(color, 0.45)}` : undefined,
      }}
    >
      <span
        className={`inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform duration-200 ${
          checked ? 'translate-x-6' : 'translate-x-1'
        } ${pending ? 'animate-pulse' : ''}`}
      />
    </button>
  );
}

export function StatTile({
  label,
  value,
  unit,
  color,
  icon,
  subtitle,
  badge,
  onClick,
}: {
  label: string;
  value: string;
  unit?: string;
  color: string;
  icon: React.ReactNode;
  subtitle?: string;
  badge?: React.ReactNode;
  onClick?: () => void;
}) {
  return (
    <SmartCard onClick={onClick} className="p-2.5 h-full flex flex-col justify-between">
      <div className="flex items-center justify-between gap-1.5">
        <div className="flex items-center gap-2 min-w-0">
          <IconBadge color={color} size={28}>
            {icon}
          </IconBadge>
          <span className="text-[11px] font-medium text-[#8A9BB8] leading-tight">{label}</span>
        </div>
        {badge}
      </div>
      <div className="mt-1.5">
        <div className="flex items-baseline gap-1 font-mono-num">
          <span className="text-lg xl:text-xl font-bold text-[#E6EDF7] tracking-tight">{value}</span>
          {unit && <span className="text-[11px] font-semibold text-[#8A9BB8]">{unit}</span>}
        </div>
        {subtitle && <div className="text-[11px] text-[#8A9BB8] mt-0.5 truncate">{subtitle}</div>}
      </div>
    </SmartCard>
  );
}

export function QuickActionTile({
  label,
  icon,
  color,
  on,
  subtitle,
  pending = false,
  disabled = false,
  unknownState = false,
  subtitleWarning = false,
  statusText,
  badge,
  onClick,
}: {
  label: string;
  icon: React.ReactNode;
  color: string;
  on: boolean;
  subtitle?: string;
  pending?: boolean;
  disabled?: boolean;
  unknownState?: boolean;
  subtitleWarning?: boolean;
  statusText?: string;
  badge?: React.ReactNode;
  onClick: () => void;
}) {
  const isLit = !unknownState && on;
  const pillLabel =
    statusText ?? (pending ? 'Đang gửi…' : unknownState ? '—' : on ? 'BẬT' : 'TẮT');

  return (
    <button
      type="button"
      disabled={disabled || pending}
      onClick={onClick}
      className="text-left rounded-xl p-2.5 transition-all active:scale-[0.98] disabled:opacity-55 disabled:cursor-not-allowed cursor-pointer w-full"
      style={{
        backgroundColor: isLit ? tint(color, 0.14) : SH.Card,
        border: `1px solid ${isLit ? tint(color, 0.45) : SH.Border}`,
        boxShadow: isLit ? `0 0 18px -6px ${tint(color, 0.32)}` : undefined,
      }}
    >
      <div className="flex items-center justify-between gap-1.5">
        <IconBadge color={color} active={isLit} size={30}>
          {icon}
        </IconBadge>
        <div className="flex items-center gap-1">
          {badge}
          <span
            className={`text-[10px] font-bold px-2 py-0.5 rounded-full font-mono-num ${pending ? 'animate-pulse' : ''}`}
            style={{
              backgroundColor: isLit ? tint(color, 0.22) : tint(SH.TextDim, 0.18),
              color: isLit ? color : SH.TextSecondary,
            }}
          >
            {pillLabel}
          </span>
        </div>
      </div>
      <div className="mt-1.5 text-xs font-semibold text-[#E6EDF7] truncate">{label}</div>
      {subtitle && (
        <div
          className="text-[11px] truncate"
          style={{ color: subtitleWarning ? SH.Amber : '#8A9BB8' }}
        >
          {subtitle}
        </div>
      )}
    </button>
  );
}

export function DeviceSwitchRow({
  icon,
  label,
  subtitle,
  color,
  checked,
  onToggle,
  disabled = false,
  pending = false,
  unknownState = false,
  subtitleWarning = false,
  badge,
}: {
  icon: React.ReactNode;
  label: string;
  subtitle?: string;
  color: string;
  checked: boolean;
  onToggle: () => void;
  disabled?: boolean;
  pending?: boolean;
  unknownState?: boolean;
  subtitleWarning?: boolean;
  badge?: React.ReactNode;
}) {
  const isLit = !unknownState && checked;
  return (
    <div
      className="flex items-center justify-between gap-2.5 rounded-xl px-2.5 py-1.5 border transition-colors"
      style={{
        backgroundColor: isLit ? tint(color, 0.08) : SH.Surface,
        borderColor: isLit ? tint(color, 0.3) : SH.Border,
      }}
    >
      <div className="flex items-center gap-2.5 min-w-0">
        <IconBadge color={color} active={isLit} size={30}>
          {icon}
        </IconBadge>
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-[#E6EDF7] truncate">{label}</span>
            {badge}
          </div>
          {subtitle && (
            <div
              className="text-[11px] truncate"
              style={{ color: subtitleWarning ? SH.Amber : '#8A9BB8' }}
            >
              {subtitle}
            </div>
          )}
        </div>
      </div>
      {unknownState && !pending ? (
        <button
          type="button"
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation();
            onToggle();
          }}
          title="Chưa rõ trạng thái công tắc thật (không giả định TẮT)"
          className="shrink-0 rounded-lg border border-[#253459] bg-[#16213A] px-2.5 py-1 font-mono-num text-xs font-bold text-[#8A9BB8] hover:border-[#3B4F7D] disabled:opacity-45 disabled:cursor-not-allowed cursor-pointer"
        >
          —
        </button>
      ) : (
        <SwitchToggle
          checked={checked}
          onChange={onToggle}
          color={color}
          disabled={disabled}
          pending={pending}
        />
      )}
    </div>
  );
}

export function TempStepperRow({
  label,
  valueText,
  onMinus,
  onPlus,
  minusDisabled = false,
  plusDisabled = false,
  color = SH.Purple,
  icon,
  statusText,
  active,
  onToggle,
}: {
  label: string;
  valueText: string;
  onMinus: () => void;
  onPlus: () => void;
  minusDisabled?: boolean;
  plusDisabled?: boolean;
  color?: string;
  icon?: React.ReactNode;
  statusText?: string;
  active?: boolean;
  onToggle?: () => void;
}) {
  const isLit = active === true;
  return (
    <div
      className="flex items-center justify-between gap-3 rounded-xl border px-3 py-2 transition-colors"
      style={{
        backgroundColor: isLit ? tint(color, 0.08) : '#0E1627',
        borderColor: isLit ? tint(color, 0.32) : '#253459',
      }}
    >
      {icon || onToggle ? (
        <button
          type="button"
          onClick={onToggle}
          disabled={!onToggle}
          className={`flex items-center gap-2.5 min-w-0 text-left ${onToggle ? 'cursor-pointer' : 'cursor-default'}`}
        >
          {icon && (
            <IconBadge color={color} active={active !== false} size={30}>
              {icon}
            </IconBadge>
          )}
          <div className="min-w-0">
            <div className="text-xs font-semibold text-[#E6EDF7] truncate">{label}</div>
            {statusText && <div className="text-[11px] text-[#8A9BB8] truncate">{statusText}</div>}
          </div>
        </button>
      ) : (
        <span className="text-xs font-medium text-[#8A9BB8]">{label}</span>
      )}
      <div className="flex items-center gap-2 shrink-0">
        <RoundIconButton onClick={onMinus} disabled={minusDisabled} size={30} color={color}>
          <Minus size={14} />
        </RoundIconButton>
        <span className="min-w-[54px] text-center font-mono-num text-sm font-bold text-[#E6EDF7]">
          {valueText}
        </span>
        <RoundIconButton onClick={onPlus} disabled={plusDisabled} size={30} color={color}>
          <Plus size={14} />
        </RoundIconButton>
      </div>
    </div>
  );
}

export function CompactSlider({
  label,
  value,
  min,
  max,
  step = 1,
  unit = '%',
  color = SH.Amber,
  disabled = false,
  onChange,
}: {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  unit?: string;
  color?: string;
  disabled?: boolean;
  onChange: (val: number) => void;
}) {
  const pct = max > min ? ((value - min) / (max - min)) * 100 : 0;
  return (
    <div className={`rounded-xl bg-[#0E1627] border border-[#253459] px-3 py-2.5 ${disabled ? 'opacity-50' : ''}`}>
      <div className="flex items-center justify-between text-xs mb-2">
        <span className="text-[#8A9BB8] font-medium">{label}</span>
        <span className="font-mono-num font-bold" style={{ color }}>
          {value}
          {unit}
        </span>
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(Number(e.target.value))}
        className="sh-slider w-full"
        style={
          {
            '--slider-accent': color,
            background: `linear-gradient(to right, ${color} 0%, ${color} ${pct}%, #253459 ${pct}%, #253459 100%)`,
          } as React.CSSProperties
        }
      />
    </div>
  );
}
