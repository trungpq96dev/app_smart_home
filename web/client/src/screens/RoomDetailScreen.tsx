import React from 'react';
import {
  Activity,
  AirVent,
  AlertTriangle,
  Blinds,
  Briefcase,
  Calendar,
  Check,
  Clock,
  Fan,
  Flame,
  Home,
  Lightbulb,
  Lock,
  Minus,
  Moon,
  Plus,
  Shield,
  Sparkles,
  Star,
  Sun,
  Tv,
  Unlock,
  Users,
  Utensils,
  Video,
} from 'lucide-react';
import {
  oThietBiBatHienThi,
  oThietBiRoRang,
  type OThietBi,
} from '../data/congtac.js';
import {
  stoveWatts,
  type RoomMode,
  type RoomUi,
} from '../data/models.js';
import {
  ChoiceChip,
  Dot,
  IconBadge,
  SmartCard,
} from '../components/Common.js';
import { SwitchToggle } from '../components/Tiles.js';
import { BedroomStage } from '../components/stages/BedroomStage.js';
import { HallwayStage } from '../components/stages/HallwayStage.js';
import { KitchenStage } from '../components/stages/KitchenStage.js';
import { LivingRoomStage } from '../components/stages/LivingRoomStage.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

function resolveSwitch(o: OThietBi, fallback: boolean) {
  if (o.kind === 'that') {
    const checked = o.dangCho && o.mong !== null ? o.mong : (o.bat ?? false);
    const roRang = oThietBiRoRang(o);
    const enabled = o.gatDuoc;
    return {
      isReal: true,
      checked,
      roRang,
      enabled,
      pending: o.dangCho,
      ghiChu: o.ghiChu,
    };
  }
  return {
    isReal: false,
    checked: fallback,
    roRang: true,
    enabled: true,
    pending: false,
    ghiChu: null as string | null,
  };
}

function InlineRangeSlider({
  value,
  min,
  max,
  color,
  disabled = false,
  onChange,
}: {
  value: number;
  min: number;
  max: number;
  color: string;
  disabled?: boolean;
  onChange: (val: number) => void;
}) {
  const pct = max > min ? ((value - min) / (max - min)) * 100 : 0;
  return (
    <input
      type="range"
      min={min}
      max={max}
      step={1}
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(Number(e.target.value))}
      className="sh-slider flex-1 min-w-0"
      style={
        {
          '--slider-accent': color,
          background: `linear-gradient(to right, ${color} 0%, ${color} ${pct}%, #26324D ${pct}%, #26324D 100%)`,
        } as React.CSSProperties
      }
    />
  );
}

function StepCircleButton({
  children,
  disabled = false,
  onClick,
  title,
}: {
  children: React.ReactNode;
  disabled?: boolean;
  onClick: () => void;
  title?: string;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      title={title}
      onClick={onClick}
      className="inline-flex h-7 w-7 items-center justify-center rounded-full border border-[#253453] bg-[#1C2946] text-[#F1F5F9] transition-all enabled:hover:brightness-110 enabled:active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shrink-0"
    >
      {children}
    </button>
  );
}

function LightBrightnessCard({
  label,
  brightness,
  sw,
  onToggleLight,
  onLightBrightness,
  className = '',
}: {
  label: string;
  brightness: number;
  sw: ReturnType<typeof resolveSwitch>;
  onToggleLight: () => void;
  onLightBrightness: (val: number) => void;
  className?: string;
}) {
  const lit = sw.roRang && sw.checked;
  const subtitle =
    sw.ghiChu ?? (!sw.roRang ? '—' : lit ? `Đang bật • Độ sáng ${brightness}%` : 'Đang tắt');

  return (
    <SmartCard className={`p-2.5 flex flex-col justify-between gap-2 ${className}`}>
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          disabled={sw.roRang && !sw.enabled}
          onClick={onToggleLight}
          className="flex items-center gap-2.5 min-w-0 flex-1 text-left cursor-pointer disabled:cursor-not-allowed"
        >
          <IconBadge color={lit ? SH.Amber : SH.TextOff} active={lit} size={32}>
            <Lightbulb size={17} />
          </IconBadge>
          <div className="min-w-0 flex-1">
            <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">{label}</div>
            <div
              className="text-[11px] truncate"
              style={{ color: sw.ghiChu && !sw.pending ? SH.Amber : SH.TextDim }}
            >
              {subtitle}
            </div>
          </div>
        </button>

        {!sw.roRang && !sw.pending ? (
          <button
            type="button"
            onClick={onToggleLight}
            title="Chưa rõ trạng thái công tắc thật"
            className="shrink-0 px-2 py-0.5 font-mono-num text-sm font-bold text-[#94A3B8] cursor-pointer"
          >
            —
          </button>
        ) : (
          <SwitchToggle
            checked={sw.checked}
            onChange={onToggleLight}
            color={SH.Green}
            disabled={!sw.enabled}
            pending={sw.pending}
          />
        )}
      </div>

      <div
        className="flex items-center gap-2"
        style={{ opacity: lit ? 1 : 0.35 }}
      >
        <Sun size={14} className="text-[#94A3B8] shrink-0" />
        <InlineRangeSlider
          value={brightness}
          min={10}
          max={100}
          color={SH.Amber}
          disabled={!lit}
          onChange={onLightBrightness}
        />
        <span className="w-9 text-right font-mono-num text-xs font-semibold text-[#FBBF24] shrink-0">
          {brightness}%
        </span>
      </div>
    </SmartCard>
  );
}

function HallwayLightRow({
  icon,
  name,
  desc,
  checked,
  roRang,
  enabled,
  pending,
  brightness,
  note,
  onToggle,
  onBrightness,
}: {
  icon: React.ReactNode;
  name: string;
  desc: string;
  checked: boolean;
  roRang: boolean;
  enabled: boolean;
  pending: boolean;
  brightness: number;
  note: string | null;
  onToggle: () => void;
  onBrightness: (val: number) => void;
}) {
  const lit = roRang && checked;
  const subText =
    note ?? (!roRang ? '—' : checked ? `${desc} • ${brightness}%` : `${desc} • Đang tắt`);

  return (
    <div
      className="rounded-lg px-2.5 py-1.5 flex flex-col justify-between gap-1.5 flex-1 min-h-0"
      style={{ backgroundColor: tint(SH.Card2, 0.65) }}
    >
      <div className="flex items-center gap-1.5 min-w-0">
        <span className="shrink-0" style={{ color: lit ? SH.Amber : SH.TextOff }}>
          {icon}
        </span>
        <span className="text-xs font-semibold text-[#F1F5F9] whitespace-nowrap">{name}</span>
        <span
          className="text-[11px] truncate flex-1 min-w-0 ml-1"
          style={{ color: note && !pending ? SH.Amber : SH.TextDim }}
        >
          {subText}
        </span>
        {!roRang && !pending ? (
          <button
            type="button"
            onClick={onToggle}
            className="shrink-0 px-1.5 font-mono-num text-xs font-bold text-[#94A3B8] cursor-pointer"
          >
            —
          </button>
        ) : (
          <SwitchToggle
            checked={checked}
            onChange={onToggle}
            color={SH.Green}
            disabled={!enabled}
            pending={pending}
          />
        )}
      </div>

      <div
        className="flex items-center gap-2"
        style={{ opacity: lit ? 1 : 0.35 }}
      >
        <span className="text-[11px] text-[#94A3B8] shrink-0">Độ sáng</span>
        <InlineRangeSlider
          value={brightness}
          min={10}
          max={100}
          color={SH.Amber}
          disabled={!lit}
          onChange={onBrightness}
        />
        <span className="w-8 text-right font-mono-num text-[11px] font-semibold text-[#FBBF24] shrink-0">
          {brightness}%
        </span>
      </div>
    </div>
  );
}

function getModeItems(roomId: string): Array<{ mode: RoomMode; label: string; icon: React.ReactNode }> {
  if (roomId === 'kitchen') {
    return [
      { mode: 'HOME', label: 'Nấu ăn', icon: <Flame size={14} /> },
      { mode: 'CUSTOM', label: 'Ăn tối', icon: <Utensils size={14} /> },
      { mode: 'SLEEP', label: 'Dọn dẹp', icon: <Sparkles size={14} /> },
      { mode: 'AWAY', label: 'Vắng nhà', icon: <Briefcase size={14} /> },
    ];
  }
  if (roomId === 'hall') {
    return [
      { mode: 'SLEEP', label: 'Đêm an ninh', icon: <Moon size={14} /> },
      { mode: 'HOME', label: 'Về nhà', icon: <Home size={14} /> },
      { mode: 'CUSTOM', label: 'Tiếp khách', icon: <Users size={14} /> },
      { mode: 'AWAY', label: 'Đi vắng', icon: <Briefcase size={14} /> },
    ];
  }
  return [
    { mode: 'SLEEP', label: 'Ngủ', icon: <Moon size={14} /> },
    { mode: 'HOME', label: 'Nhà', icon: <Home size={14} /> },
    { mode: 'AWAY', label: 'Vắng nhà', icon: <Briefcase size={14} /> },
    { mode: 'CUSTOM', label: 'Tùy chỉnh', icon: <Star size={14} /> },
  ];
}

export function RoomDetailScreen({ roomId }: { roomId: string }) {
  const {
    rooms,
    oThietBi,
    bamCongTac,
    toggleRoomLight,
    toggleRoomFan,
    toggleRoomAc,
    stepAcTemp,
    setRoomMode,
    toggleRoomSchedule,
    setLightBrightness,
    setFanSpeed,
    toggleRoomTv,
    setCurtainOpen,
    toggleCurtain,
    setHoodSpeed,
    toggleStove,
    stepStoveLevel,
    setGasAlert,
    dismissGasAlert,
    setHallView,
    toggleHallFrontLight,
    setHallFrontBrightness,
    toggleHallSurroundingLight,
    setHallSurroundingBrightness,
    toggleHallAllLights,
    toggleHallDoorLock,
    toggleHallCamera,
    toggleHallIrMode,
    toggleHallMotionAuto,
    triggerHallMotionTest,
    clearHallMotion,
  } = useSmartHome();

  const room = rooms.find((r) => r.id === roomId) ?? rooms[0];

  const denObj = oThietBi(room.id, 'den', room.lightOn);
  const quatObj = oThietBi(room.id, 'quat', room.fanOn);
  const dhObj = oThietBi(room.id, 'dieu_hoa', room.acOn);

  const denSw = resolveSwitch(denObj, room.lightOn);
  const quatSw = resolveSwitch(quatObj, room.fanOn);
  const dhSw = resolveSwitch(dhObj, room.acOn);

  const denHienThi =
    denObj.kind === 'that'
      ? denObj.dangCho
        ? (denObj.mong ?? room.lightOn)
        : denObj.bridgeSong && denObj.online && denObj.bat !== null
          ? denObj.bat
          : room.lightOn
      : room.lightOn;

  const quatHienThi =
    quatObj.kind === 'that'
      ? quatObj.dangCho
        ? (quatObj.mong ?? room.fanOn)
        : quatObj.bridgeSong && quatObj.online && quatObj.bat !== null
          ? quatObj.bat
          : room.fanOn
      : room.fanOn;

  const acHienThi =
    dhObj.kind === 'that' ? oThietBiBatHienThi(dhObj, room.acOn) : room.acOn;

  const stageRoom: RoomUi = {
    ...room,
    lightOn: denHienThi,
    fanOn: quatHienThi,
    acOn: acHienThi,
  };

  const handleToggleLight = () => {
    if (denObj.kind === 'that') bamCongTac(room.id, 'den');
    else toggleRoomLight(room.id);
  };

  const handleToggleFan = () => {
    if (quatObj.kind === 'that') bamCongTac(room.id, 'quat');
    else toggleRoomFan(room.id);
  };

  const handleToggleAc = () => {
    if (dhObj.kind === 'that') bamCongTac(room.id, 'dieu_hoa');
    else toggleRoomAc(room.id);
  };

  const renderControlsColumn = () => {
    if (room.id === 'living') {
      const acLevel = !acHienThi
        ? 'Tắt'
        : room.acTemp <= 20
          ? 'Lạnh sâu'
          : room.acTemp <= 25
            ? 'Mát dịu'
            : 'Tiết kiệm';
      const curtainText =
        room.curtainOpen === 0
          ? 'Đang đóng kín • 0%'
          : room.curtainOpen === 100
            ? 'Đang mở hoàn toàn • 100%'
            : `Đang mở một phần • ${room.curtainOpen}%`;

      return (
        <div className="flex flex-col justify-between gap-1.5 h-full min-h-0">
          {/* 1. Đèn phòng */}
          <LightBrightnessCard
            label="Đèn phòng"
            brightness={room.lightBrightness}
            sw={denSw}
            onToggleLight={handleToggleLight}
            onLightBrightness={(v) => setLightBrightness(room.id, v)}
            className="flex-1 min-h-0"
          />

          {/* 2. Quạt trần */}
          <SmartCard className="p-2.5 flex items-center justify-between gap-2 shrink-0">
            <div className="flex items-center gap-2.5 min-w-0">
              <IconBadge
                color={quatSw.roRang && quatSw.checked ? SH.Blue : SH.TextOff}
                active={quatSw.roRang && quatSw.checked}
                size={30}
              >
                <Fan size={16} />
              </IconBadge>
              <div className="min-w-0">
                <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">Quạt trần</div>
                <div
                  className="text-[11px] truncate"
                  style={{ color: quatSw.ghiChu && !quatSw.pending ? SH.Amber : SH.TextDim }}
                >
                  {quatSw.ghiChu ?? (!quatSw.roRang ? '—' : quatSw.checked ? 'Đang bật' : 'Đang tắt')}
                </div>
              </div>
            </div>
            {!quatSw.roRang && !quatSw.pending ? (
              <button
                type="button"
                onClick={handleToggleFan}
                className="shrink-0 px-2 py-0.5 font-mono-num text-sm font-bold text-[#94A3B8] cursor-pointer"
              >
                —
              </button>
            ) : (
              <SwitchToggle
                checked={quatSw.checked}
                onChange={handleToggleFan}
                color={SH.Green}
                disabled={!quatSw.enabled}
                pending={quatSw.pending}
              />
            )}
          </SmartCard>

          {/* 3. Smart TV */}
          <SmartCard className="p-2.5 flex items-center justify-between gap-2 shrink-0">
            <div className="flex items-center gap-2.5 min-w-0">
              <IconBadge
                color={room.tvOn ? SH.Blue : SH.TextOff}
                active={room.tvOn}
                size={30}
              >
                <Tv size={16} />
              </IconBadge>
              <div className="min-w-0">
                <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">Smart TV</div>
                <div className="flex items-center gap-1.5 text-[11px] truncate">
                  <Dot color={room.tvOn ? SH.Green : SH.TextOff} size={6} />
                  <span style={{ color: room.tvOn ? SH.Text : SH.TextDim }}>
                    {room.tvOn ? 'Đang phát • 4K HDR' : 'Đang tắt'}
                  </span>
                </div>
              </div>
            </div>
            <SwitchToggle
              checked={room.tvOn}
              onChange={() => toggleRoomTv(room.id)}
              color={SH.Green}
            />
          </SmartCard>

          {/* 4. Điều hòa */}
          <SmartCard className="p-2.5 flex items-center justify-between gap-2 shrink-0">
            <div className="flex items-center gap-2.5 min-w-0 flex-1">
              <IconBadge
                color={acHienThi ? SH.Purple : SH.TextOff}
                active={acHienThi}
                size={30}
              >
                <AirVent size={16} />
              </IconBadge>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">Điều hòa</span>
                  <span
                    className="rounded px-1.5 py-0.5 text-[10px] font-semibold"
                    style={{
                      color: acHienThi ? SH.Cyan : SH.TextOff,
                      backgroundColor: acHienThi ? tint(SH.Cyan, 0.14) : SH.Card2,
                    }}
                  >
                    {acLevel}
                  </span>
                </div>
                <div className="text-[11px] text-[#94A3B8] truncate">
                  {acHienThi ? `Đang làm lạnh • ${room.acTemp}°C` : 'Đang tắt'}
                </div>
              </div>
            </div>

            <div
              className="flex items-center gap-1 shrink-0"
              style={{ opacity: acHienThi ? 1 : 0.4 }}
            >
              <StepCircleButton onClick={() => stepAcTemp(room.id, -1)} disabled={room.acTemp <= 16}>
                <Minus size={13} />
              </StepCircleButton>
              <span className="w-11 text-center font-mono-num text-xs font-bold text-[#F1F5F9]">
                {room.acTemp}°C
              </span>
              <StepCircleButton onClick={() => stepAcTemp(room.id, 1)} disabled={room.acTemp >= 30}>
                <Plus size={13} />
              </StepCircleButton>
            </div>

            <SwitchToggle
              checked={dhSw.checked}
              onChange={handleToggleAc}
              color={SH.Green}
              disabled={!dhSw.enabled}
              pending={dhSw.pending}
            />
          </SmartCard>

          {/* 5. Rèm cửa tự động */}
          <SmartCard className="p-2.5 flex flex-col justify-between gap-2 flex-1 min-h-0">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <IconBadge
                  color={room.curtainOpen > 0 ? SH.Teal : SH.TextOff}
                  active={room.curtainOpen > 0}
                  size={30}
                >
                  <Blinds size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">
                    Rèm cửa tự động
                  </div>
                  <div className="text-[11px] text-[#94A3B8] truncate">{curtainText}</div>
                </div>
              </div>
              <button
                type="button"
                onClick={() => toggleCurtain(room.id)}
                className="shrink-0 rounded-lg border border-[#253453] bg-[#1C2946] px-2.5 py-1 text-xs font-semibold text-[#14B8A6] hover:brightness-110 cursor-pointer"
              >
                {room.curtainOpen > 0 ? 'Đóng rèm' : 'Mở rèm'}
              </button>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-[11px] text-[#94A3B8] shrink-0">Đóng</span>
              <InlineRangeSlider
                value={room.curtainOpen}
                min={0}
                max={100}
                color={SH.Teal}
                onChange={(v) => setCurtainOpen(room.id, v)}
              />
              <span className="w-9 text-right font-mono-num text-xs font-semibold text-[#14B8A6] shrink-0">
                {room.curtainOpen}%
              </span>
            </div>
          </SmartCard>
        </div>
      );
    }

    if (room.id === 'kitchen') {
      const hoodSpeed = Math.min(3, Math.max(1, room.hoodSpeed));
      const hoodLabel = hoodSpeed === 1 ? 'Nhẹ' : hoodSpeed === 2 ? 'Vừa' : 'Mạnh';
      const hoodLit = quatSw.roRang && quatSw.checked;
      const stoveLevel = Math.min(9, Math.max(1, room.stoveLevel));
      const watts = room.stoveOn ? stoveWatts(stoveLevel) : 0;

      return (
        <div className="flex flex-col justify-between gap-1.5 h-full min-h-0">
          {/* 1. Đèn bếp */}
          <LightBrightnessCard
            label="Đèn bếp"
            brightness={room.lightBrightness}
            sw={denSw}
            onToggleLight={handleToggleLight}
            onLightBrightness={(v) => setLightBrightness(room.id, v)}
            className="flex-1 min-h-0"
          />

          {/* 2. Máy hút mùi */}
          <SmartCard className="p-2.5 flex flex-col justify-between gap-2 flex-1 min-h-0">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <IconBadge
                  color={hoodLit ? '#38BDF8' : SH.TextOff}
                  active={hoodLit}
                  size={30}
                >
                  <Fan size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">Máy hút mùi</div>
                  <div
                    className="text-[11px] truncate"
                    style={{ color: quatSw.ghiChu && !quatSw.pending ? SH.Amber : SH.TextDim }}
                  >
                    {quatSw.ghiChu ??
                      (!quatSw.roRang
                        ? '—'
                        : hoodLit
                          ? `Đang hút • Cấp ${hoodSpeed} (${hoodLabel})`
                          : 'Đang tắt')}
                  </div>
                </div>
              </div>
              {!quatSw.roRang && !quatSw.pending ? (
                <button
                  type="button"
                  onClick={handleToggleFan}
                  className="shrink-0 px-2 py-0.5 font-mono-num text-sm font-bold text-[#94A3B8] cursor-pointer"
                >
                  —
                </button>
              ) : (
                <SwitchToggle
                  checked={quatSw.checked}
                  onChange={handleToggleFan}
                  color={SH.Green}
                  disabled={!quatSw.enabled}
                  pending={quatSw.pending}
                />
              )}
            </div>

            <div
              className="flex items-center justify-between gap-2"
              style={{ opacity: hoodLit ? 1 : 0.4 }}
            >
              <span className="text-[11px] text-[#94A3B8]">Cấp độ hút</span>
              <div className="flex items-center gap-1.5">
                {(
                  [
                    [1, '1 - Nhẹ'],
                    [2, '2 - Vừa'],
                    [3, '3 - Mạnh'],
                  ] as const
                ).map(([spd, txt]) => {
                  const selected = hoodLit && hoodSpeed === spd;
                  return (
                    <button
                      key={spd}
                      type="button"
                      disabled={!hoodLit}
                      onClick={() => setHoodSpeed(room.id, spd)}
                      className="rounded-lg px-2 py-1 text-[11px] font-semibold transition-colors disabled:cursor-not-allowed cursor-pointer"
                      style={{
                        backgroundColor: selected ? SH.Blue : SH.Card2,
                        border: `1px solid ${selected ? SH.Blue : SH.Border}`,
                        color: selected ? '#FFFFFF' : SH.TextDim,
                      }}
                    >
                      {txt}
                    </button>
                  );
                })}
              </div>
            </div>
          </SmartCard>

          {/* 3. Bếp từ */}
          <SmartCard className="p-2.5 flex flex-col justify-between gap-2 flex-1 min-h-0">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <IconBadge
                  color={room.stoveOn ? SH.Orange : SH.TextOff}
                  active={room.stoveOn}
                  size={30}
                >
                  <Flame size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <span className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">Bếp từ</span>
                    <span
                      className="rounded px-1.5 py-0.5 text-[10px] font-semibold font-mono-num"
                      style={{
                        color: room.stoveOn ? '#FDBA74' : SH.TextDim,
                        backgroundColor: room.stoveOn ? tint(SH.Orange, 0.18) : SH.Card2,
                        border: `1px solid ${room.stoveOn ? tint(SH.Orange, 0.4) : SH.Border}`,
                      }}
                    >
                      {watts}W
                    </span>
                  </div>
                  <div className="text-[11px] text-[#94A3B8] truncate">
                    {room.stoveOn
                      ? `Đang nấu • Công suất mức ${stoveLevel}`
                      : 'Đang tắt • Vùng nấu nguội'}
                  </div>
                </div>
              </div>
              <SwitchToggle
                checked={room.stoveOn}
                onChange={() => toggleStove(room.id)}
                color={SH.Orange}
              />
            </div>

            <div
              className="flex items-center justify-between gap-2"
              style={{ opacity: room.stoveOn ? 1 : 0.4 }}
            >
              <span className="text-[11px] text-[#94A3B8]">Mức công suất (1–9)</span>
              <div className="flex items-center gap-1.5">
                <StepCircleButton
                  disabled={!room.stoveOn || stoveLevel <= 1}
                  onClick={() => stepStoveLevel(room.id, -1)}
                >
                  <Minus size={13} />
                </StepCircleButton>
                <span className="w-13 text-center font-mono-num text-xs font-bold text-[#F97316]">
                  Mức {stoveLevel}
                </span>
                <StepCircleButton
                  disabled={!room.stoveOn || stoveLevel >= 9}
                  onClick={() => stepStoveLevel(room.id, 1)}
                >
                  <Plus size={13} />
                </StepCircleButton>
              </div>
            </div>
          </SmartCard>

          {/* 4. Cảm biến Gas & Khói */}
          <SmartCard
            className="p-2.5 flex flex-col justify-between gap-2 flex-1 min-h-0"
            style={{
              borderColor: room.gasAlert ? tint(SH.Red, 0.7) : SH.Border,
            }}
          >
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <IconBadge
                  color={room.gasAlert ? SH.Red : SH.Green}
                  active
                  size={30}
                >
                  <Shield size={16} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9] truncate">
                    Cảm biến Gas & Khói
                  </div>
                  <div
                    className="text-[11px] truncate"
                    style={{ color: room.gasAlert ? SH.Red : SH.Green }}
                  >
                    {room.gasAlert
                      ? 'NGUY HIỂM • Phát hiện rò rỉ Gas/Khói!'
                      : 'An toàn • Không phát hiện rò rỉ'}
                  </div>
                </div>
              </div>
              <div className="text-right shrink-0">
                <div
                  className="font-mono-num text-xs font-bold"
                  style={{ color: room.gasAlert ? SH.Red : SH.Green }}
                >
                  {room.gasPpm} ppm
                </div>
                <div className="text-[10px] text-[#94A3B8]">Ngưỡng &lt; 100</div>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-1.5">
              <button
                type="button"
                onClick={() => setGasAlert(room.id, true)}
                className="inline-flex items-center justify-center gap-1.5 rounded-lg py-1.5 text-xs font-semibold text-[#FCA5A5] hover:brightness-110 cursor-pointer"
                style={{
                  backgroundColor: tint(SH.Red, 0.15),
                  border: `1px solid ${tint(SH.Red, 0.4)}`,
                }}
              >
                <AlertTriangle size={13} />
                <span>Test cảnh báo</span>
              </button>
              <button
                type="button"
                disabled={!room.gasAlert}
                onClick={() => dismissGasAlert(room.id)}
                className="inline-flex items-center justify-center gap-1.5 rounded-lg border border-[#253453] bg-[#1C2946] py-1.5 text-xs font-semibold text-[#F1F5F9] enabled:hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
              >
                <Check size={13} />
                <span>Tắt cảnh báo</span>
              </button>
            </div>
          </SmartCard>
        </div>
      );
    }

    if (room.id === 'hall') {
      const anyLightOn =
        (denSw.roRang && denSw.checked) ||
        room.hallFrontLightOn ||
        room.hallSurroundingLightOn;

      return (
        <div className="flex flex-col justify-between gap-1.5 h-full min-h-0">
          {/* 1. Hệ thống chiếu sáng (3 cụm đèn) */}
          <SmartCard className="p-2 flex flex-col justify-between gap-1.5 flex-[2] min-h-0">
            <div className="flex items-center justify-between gap-2 shrink-0">
              <div className="flex items-center gap-1.5 min-w-0">
                <Dot color={SH.Amber} size={6} />
                <span className="text-xs font-semibold text-[#F1F5F9] truncate">
                  Hệ thống chiếu sáng (3 cụm đèn)
                </span>
              </div>
              <button
                type="button"
                onClick={() => toggleHallAllLights(room.id)}
                className="shrink-0 rounded-md border border-[#253453] bg-[#1C2946] px-2 py-0.5 text-[11px] font-semibold text-[#FBBF24] hover:brightness-110 cursor-pointer"
              >
                {anyLightOn ? 'Tắt tất cả đèn' : 'Bật tất cả đèn'}
              </button>
            </div>

            <div className="flex flex-col gap-1.5 flex-1 min-h-0">
              <HallwayLightRow
                icon={<Lightbulb size={14} />}
                name="Đèn hành lang"
                desc="Sảnh hiên & tường hành lang"
                checked={denSw.checked}
                roRang={denSw.roRang}
                enabled={denSw.enabled}
                pending={denSw.pending}
                brightness={room.lightBrightness}
                note={denSw.ghiChu}
                onToggle={handleToggleLight}
                onBrightness={(v) => setLightBrightness(room.id, v)}
              />
              <HallwayLightRow
                icon={<Sun size={14} />}
                name="Đèn phía trước nhà"
                desc="Diềm mái Nhật & trụ cổng"
                checked={room.hallFrontLightOn}
                roRang
                enabled
                pending={false}
                brightness={room.hallFrontBrightness}
                note={null}
                onToggle={() => toggleHallFrontLight(room.id)}
                onBrightness={(v) => setHallFrontBrightness(room.id, v)}
              />
              <HallwayLightRow
                icon={<Sparkles size={14} />}
                name="Đèn xung quanh"
                desc="Sân vườn, đèn nấm & tường rào"
                checked={room.hallSurroundingLightOn}
                roRang
                enabled
                pending={false}
                brightness={room.hallSurroundingBrightness}
                note={null}
                onToggle={() => toggleHallSurroundingLight(room.id)}
                onBrightness={(v) => setHallSurroundingBrightness(room.id, v)}
              />
            </div>
          </SmartCard>

          {/* 2. Khóa cửa chính */}
          <SmartCard className="p-2 flex items-center justify-between gap-2 shrink-0">
            <div className="flex items-center gap-2 min-w-0">
              <IconBadge
                color={room.hallDoorLocked ? SH.Green : SH.Red}
                active
                size={28}
              >
                {room.hallDoorLocked ? <Lock size={15} /> : <Unlock size={15} />}
              </IconBadge>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5">
                  <span className="text-xs font-bold text-[#F1F5F9] truncate">Khóa cửa chính</span>
                  <span
                    className="rounded-full px-1.5 py-0.5 text-[10px] font-semibold"
                    style={{
                      color: room.hallDoorLocked ? SH.Green : SH.Red,
                      backgroundColor: tint(room.hallDoorLocked ? SH.Green : SH.Red, 0.16),
                      border: `1px solid ${tint(room.hallDoorLocked ? SH.Green : SH.Red, 0.4)}`,
                    }}
                  >
                    {room.hallDoorLocked ? 'Đã khóa' : 'Đang mở'}
                  </span>
                </div>
                <div className="text-[11px] text-[#94A3B8] truncate">
                  {room.hallDoorLocked
                    ? 'Chốt an ninh đang kích hoạt'
                    : 'Cảnh báo: Cửa chính đang mở'}
                </div>
              </div>
            </div>
            <SwitchToggle
              checked={room.hallDoorLocked}
              onChange={() => toggleHallDoorLock(room.id)}
              color={SH.Green}
            />
          </SmartCard>

          {/* 3. Camera hành lang + Hồng ngoại (IR) */}
          <SmartCard className="p-2 flex flex-col justify-between gap-1.5 shrink-0">
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <IconBadge
                  color={room.hallCameraOn ? SH.Blue : SH.TextOff}
                  active={room.hallCameraOn}
                  size={28}
                >
                  <Video size={15} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs font-bold text-[#F1F5F9] truncate">Camera hành lang</div>
                  <div className="text-[11px] text-[#94A3B8] truncate">
                    {!room.hallCameraOn
                      ? 'Camera đang tắt'
                      : room.hallIrMode
                        ? 'Đang ghi hình • Hồng ngoại (IR)'
                        : 'Đang ghi hình • 3 góc nhìn'}
                  </div>
                </div>
              </div>
              <SwitchToggle
                checked={room.hallCameraOn}
                onChange={() => toggleHallCamera(room.id)}
                color={SH.Green}
              />
            </div>

            <div
              className="flex items-center justify-between gap-2"
              style={{ opacity: room.hallCameraOn ? 1 : 0.4 }}
            >
              <div className="flex items-center gap-1.5">
                <Dot color={SH.Green} size={6} />
                <span className="text-[11px] text-[#F1F5F9]">Chế độ Hồng ngoại (IR)</span>
              </div>
              <SwitchToggle
                checked={room.hallIrMode}
                disabled={!room.hallCameraOn}
                onChange={() => toggleHallIrMode(room.id)}
                color={SH.Blue}
              />
            </div>
          </SmartCard>

          {/* 4. Cảm biến chuyển động */}
          <SmartCard
            className="p-2 flex flex-col justify-between gap-1.5 shrink-0"
            style={{
              borderColor: room.hallMotionDetected ? tint(SH.Red, 0.7) : SH.Border,
            }}
          >
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <IconBadge
                  color={room.hallMotionDetected ? SH.Red : SH.Purple}
                  active
                  size={28}
                >
                  <Activity size={15} />
                </IconBadge>
                <div className="min-w-0">
                  <div className="text-xs font-bold text-[#F1F5F9] truncate">
                    Cảm biến chuyển động
                  </div>
                  <div className="text-[11px] text-[#94A3B8] truncate">
                    Tự bật Đèn hành lang & Xung quanh
                  </div>
                </div>
              </div>
              <SwitchToggle
                checked={room.hallMotionAuto}
                onChange={() => toggleHallMotionAuto(room.id)}
                color={SH.Green}
              />
            </div>

            <div className="flex items-center justify-between gap-2">
              <div className="text-[11px]">
                <span className="text-[#94A3B8]">Tự động bật đèn: </span>
                <span
                  className="font-semibold"
                  style={{ color: room.hallMotionAuto ? SH.Green : SH.TextOff }}
                >
                  {room.hallMotionAuto ? 'Đang bật' : 'Đang tắt'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => triggerHallMotionTest(room.id)}
                className="rounded-md px-2.5 py-1 text-[11px] font-semibold text-[#FCA5A5] hover:brightness-110 cursor-pointer"
                style={{
                  backgroundColor: tint(SH.Red, 0.16),
                  border: `1px solid ${tint(SH.Red, 0.4)}`,
                }}
              >
                Test phát hiện
              </button>
            </div>
          </SmartCard>
        </div>
      );
    }

    // Phòng ngủ 1, 2, 3 (khớp BedroomControlsColumn trong RoomDetailScreen.kt)
    const speedLabels = room.fanSpeed === 1 ? 'Nhẹ' : room.fanSpeed === 2 ? 'Vừa' : 'Mạnh';
    const acDesc = !acHienThi
      ? 'Đang tắt'
      : room.acTemp <= 20
        ? 'Đang làm lạnh • Lạnh sâu'
        : room.acTemp <= 25
          ? 'Đang làm lạnh • Mát dịu'
          : 'Đang làm lạnh • Tiết kiệm';

    return (
      <div className="flex flex-col justify-between gap-2.5 h-full min-h-0">
        {/* 1. Đèn phòng + thanh trượt độ sáng */}
        <SmartCard className="p-3 flex flex-col justify-between gap-2.5 flex-1 min-h-0">
          <div className="flex items-center justify-between gap-2">
            <button
              type="button"
              disabled={denSw.roRang && !denSw.enabled}
              onClick={handleToggleLight}
              className="flex items-center gap-2.5 min-w-0 flex-1 text-left cursor-pointer disabled:cursor-not-allowed"
            >
              <IconBadge
                color={denHienThi ? SH.Amber : SH.TextOff}
                active={denHienThi}
                size={36}
              >
                <Lightbulb size={19} />
              </IconBadge>
              <div className="min-w-0 flex-1">
                <div className="text-[13px] sm:text-sm font-bold text-[#F1F5F9] truncate">
                  Đèn phòng
                </div>
                <div
                  className="text-xs truncate"
                  style={{ color: denSw.ghiChu && !denSw.pending ? SH.Amber : SH.TextDim }}
                >
                  {denSw.ghiChu ??
                    (denHienThi ? `Đang bật • Độ sáng ${room.lightBrightness}%` : 'Đang tắt')}
                </div>
              </div>
            </button>

            {!denSw.roRang && !denSw.pending ? (
              <span className="font-mono-num text-sm font-bold text-[#94A3B8] px-1">—</span>
            ) : (
              <SwitchToggle
                checked={denSw.checked}
                onChange={handleToggleLight}
                color={SH.Green}
                disabled={!denSw.enabled}
                pending={denSw.pending}
              />
            )}
          </div>

          <div
            className="flex items-center gap-2"
            style={{ opacity: denHienThi ? 1 : 0.35 }}
          >
            <Sun size={16} className="text-[#94A3B8] shrink-0" />
            <InlineRangeSlider
              value={room.lightBrightness}
              min={10}
              max={100}
              color={SH.Amber}
              disabled={!denHienThi || (denSw.roRang && !denSw.enabled)}
              onChange={(v) => setLightBrightness(room.id, v)}
            />
            <span className="w-9 text-right font-mono-num text-xs font-semibold text-[#FBBF24] shrink-0">
              {room.lightBrightness}%
            </span>
          </div>
        </SmartCard>

        {/* 2. Quạt trần + tốc độ gió 1 - 2 - 3 */}
        <SmartCard className="p-3 flex flex-col justify-between gap-2.5 flex-1 min-h-0">
          <div className="flex items-center justify-between gap-2">
            <button
              type="button"
              disabled={quatSw.roRang && !quatSw.enabled}
              onClick={handleToggleFan}
              className="flex items-center gap-2.5 min-w-0 flex-1 text-left cursor-pointer disabled:cursor-not-allowed"
            >
              <IconBadge
                color={quatHienThi ? SH.Blue : SH.TextOff}
                active={quatHienThi}
                size={36}
              >
                <Fan size={19} />
              </IconBadge>
              <div className="min-w-0 flex-1">
                <div className="text-[13px] sm:text-sm font-bold text-[#F1F5F9] truncate">
                  Quạt trần
                </div>
                <div
                  className="text-xs truncate"
                  style={{ color: quatSw.ghiChu && !quatSw.pending ? SH.Amber : SH.TextDim }}
                >
                  {quatSw.ghiChu ??
                    (quatHienThi
                      ? `Đang quay • Tốc độ ${room.fanSpeed} (${speedLabels})`
                      : 'Đang tắt')}
                </div>
              </div>
            </button>

            {!quatSw.roRang && !quatSw.pending ? (
              <div className="flex items-center gap-2 shrink-0">
                <button
                  type="button"
                  onClick={handleToggleFan}
                  className="rounded-lg px-2.5 py-1 text-xs font-semibold transition-colors cursor-pointer"
                  style={{
                    backgroundColor: quatHienThi ? tint(SH.Blue, 0.18) : SH.Card2,
                    border: `1px solid ${quatHienThi ? SH.Blue : SH.Border}`,
                    color: quatHienThi ? SH.Blue : SH.Text,
                  }}
                >
                  {quatHienThi ? 'Tắt quạt' : 'Bật quạt'}
                </button>
                <span className="font-mono-num text-sm font-bold text-[#94A3B8]">—</span>
              </div>
            ) : (
              <SwitchToggle
                checked={quatSw.checked}
                onChange={handleToggleFan}
                color={SH.Green}
                disabled={!quatSw.enabled}
                pending={quatSw.pending}
              />
            )}
          </div>

          <div className="flex items-center justify-between gap-2">
            <span
              className="text-xs font-medium"
              style={{ color: quatHienThi ? SH.TextDim : SH.TextOff }}
            >
              Tốc độ gió
            </span>
            <div className="flex items-center gap-1.5">
              {[1, 2, 3].map((spd) => {
                const active = quatHienThi && room.fanSpeed === spd;
                return (
                  <button
                    key={spd}
                    type="button"
                    onClick={() => setFanSpeed(room.id, spd)}
                    className="h-7 w-[34px] rounded-lg text-xs font-semibold transition-colors cursor-pointer"
                    style={{
                      backgroundColor: active ? SH.Blue : SH.Card2,
                      border: `1px solid ${active ? SH.Blue : SH.Border}`,
                      color: active ? '#FFFFFF' : SH.TextDim,
                    }}
                  >
                    {spd}
                  </button>
                );
              })}
            </div>
          </div>
        </SmartCard>

        {/* 3. Điều hòa + bật/tắt + tăng/giảm nhiệt độ */}
        <SmartCard className="p-3 flex flex-col justify-between gap-2.5 flex-1 min-h-0">
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0 flex-1">
              <IconBadge
                color={acHienThi ? SH.Purple : SH.TextOff}
                active={acHienThi}
                size={36}
              >
                <AirVent size={19} />
              </IconBadge>
              <div className="min-w-0 flex-1">
                <div className="text-[13px] sm:text-sm font-bold text-[#F1F5F9] truncate">
                  Điều hòa
                </div>
                <div className="text-xs text-[#94A3B8] truncate">{acDesc}</div>
              </div>
            </div>

            {!dhSw.roRang && !dhSw.pending ? (
              <span className="font-mono-num text-sm font-bold text-[#94A3B8] px-1">—</span>
            ) : (
              <SwitchToggle
                checked={dhSw.checked}
                onChange={handleToggleAc}
                color={SH.Green}
                disabled={!dhSw.enabled}
                pending={dhSw.pending}
              />
            )}
          </div>

          <div
            className="flex items-center justify-between gap-2"
            style={{ opacity: acHienThi ? 1 : 0.4 }}
          >
            <span className="text-xs font-medium text-[#94A3B8]">Nhiệt độ mục tiêu</span>
            <div className="flex items-center gap-1.5">
              <StepCircleButton onClick={() => stepAcTemp(room.id, -1)} disabled={room.acTemp <= 16}>
                <Minus size={14} />
              </StepCircleButton>
              <span className="w-12 text-center font-mono-num text-[13px] font-bold text-[#F1F5F9]">
                {room.acTemp}°C
              </span>
              <StepCircleButton onClick={() => stepAcTemp(room.id, 1)} disabled={room.acTemp >= 30}>
                <Plus size={14} />
              </StepCircleButton>
            </div>
          </div>
        </SmartCard>
      </div>
    );
  };

  const modeItems = getModeItems(room.id);

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Hàng trên: Mô phỏng 2.5D bên trái | Các thẻ thiết bị bên phải */}
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        <div className="lg:col-span-6 flex flex-col min-h-[250px] lg:min-h-0">
          {room.id === 'living' ? (
            <LivingRoomStage room={stageRoom} fill />
          ) : room.id === 'kitchen' ? (
            <KitchenStage
              room={stageRoom}
              fill
              onSilenceGasAlert={() => dismissGasAlert(room.id)}
            />
          ) : room.id === 'hall' ? (
            <HallwayStage
              room={stageRoom}
              fill
              onSelectView={(v) => setHallView(room.id, v)}
              onDismissMotion={() => clearHallMotion(room.id)}
            />
          ) : (
            <BedroomStage room={stageRoom} fill />
          )}
        </div>

        <div className="lg:col-span-6 flex flex-col min-h-0">{renderControlsColumn()}</div>
      </div>

      {/* Hàng dưới: Chế độ hoạt động (trái) | Lịch hẹn (phải) khớp 1:1 RoomDetailScreen.kt */}
      <div className="shrink-0 grid grid-cols-1 lg:grid-cols-5 gap-2.5">
        <SmartCard className="lg:col-span-3 p-2.5 flex flex-col justify-between gap-2">
          <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9]">Chế độ hoạt động</div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            {modeItems.map(({ mode, label, icon }) => (
              <ChoiceChip
                key={mode}
                icon={icon}
                label={label}
                selected={room.mode === mode}
                color={SH.Blue}
                onClick={() => setRoomMode(room.id, mode)}
              />
            ))}
          </div>
        </SmartCard>

        <SmartCard className="lg:col-span-2 p-2.5 flex flex-col justify-between gap-2">
          {room.id === 'kitchen' ? (
            <>
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs sm:text-[13px] font-bold text-[#F1F5F9]">
                  Lịch hẹn & An toàn
                </span>
                <span
                  className="rounded-full px-2 py-0.5 text-[11px] font-semibold"
                  style={{
                    color: room.gasAlert ? SH.Red : SH.Green,
                    backgroundColor: tint(room.gasAlert ? SH.Red : SH.Green, 0.16),
                  }}
                >
                  {room.gasAlert ? 'Cảnh báo rò rỉ!' : 'An toàn 24/7'}
                </span>
              </div>
              <div className="flex items-center gap-2.5">
                <IconBadge color={SH.Blue} size={34}>
                  <Clock size={17} />
                </IconBadge>
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-semibold text-[#F1F5F9] truncate">
                    Tự tắt hút mùi sau 15 phút
                  </div>
                  <div
                    className="text-[11px] truncate"
                    style={{ color: room.scheduleOn ? SH.TextDim : SH.TextOff }}
                  >
                    {room.scheduleOn ? 'Hẹn giờ thông minh đang bật' : 'Đã tạm tắt hẹn giờ'}
                  </div>
                </div>
                <SwitchToggle
                  checked={room.scheduleOn}
                  onChange={() => toggleRoomSchedule(room.id)}
                  color={SH.Blue}
                />
              </div>
            </>
          ) : room.id === 'hall' ? (
            <>
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs sm:text-[13px] font-bold text-[#F1F5F9]">Lịch hẹn</span>
                <span
                  className="rounded-full px-2 py-0.5 text-[11px] font-semibold"
                  style={{
                    color: room.scheduleOn ? SH.Blue : SH.TextDim,
                    backgroundColor: room.scheduleOn ? tint(SH.Blue, 0.16) : SH.Card2,
                  }}
                >
                  {room.scheduleOn ? 'Đang bật' : 'Đã tắt'}
                </span>
              </div>
              <div className="flex items-center gap-2.5">
                <IconBadge color={SH.Blue} size={34}>
                  <Calendar size={17} />
                </IconBadge>
                <div className="flex-1 min-w-0">
                  <div className="text-[11px] text-[#94A3B8] truncate">{room.scheduleLabel}</div>
                  <div
                    className="text-xs sm:text-[13px] font-bold font-mono-num truncate"
                    style={{ color: room.scheduleOn ? SH.Text : SH.TextOff }}
                  >
                    {room.scheduleTime}
                  </div>
                </div>
                <SwitchToggle
                  checked={room.scheduleOn}
                  onChange={() => toggleRoomSchedule(room.id)}
                  color={SH.Blue}
                />
              </div>
            </>
          ) : (
            <>
              <div className="text-xs sm:text-[13px] font-bold text-[#F1F5F9]">Lịch hẹn</div>
              <div className="flex items-center gap-2.5">
                <IconBadge color={SH.Blue} size={34}>
                  <Calendar size={17} />
                </IconBadge>
                <div className="flex-1 min-w-0">
                  <div className="text-[11px] text-[#94A3B8] truncate">{room.scheduleLabel}</div>
                  <div
                    className="text-xs sm:text-[13px] font-bold font-mono-num truncate"
                    style={{ color: room.scheduleOn ? SH.Text : SH.TextOff }}
                  >
                    {room.scheduleTime}
                  </div>
                </div>
                <SwitchToggle
                  checked={room.scheduleOn}
                  onChange={() => toggleRoomSchedule(room.id)}
                  color={SH.Blue}
                />
              </div>
            </>
          )}
        </SmartCard>
      </div>
    </div>
  );
}
