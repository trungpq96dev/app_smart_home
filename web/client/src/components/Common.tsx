import React from 'react';
import {
  Activity,
  AirVent,
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  BedDouble,
  Blinds,
  Bolt,
  Box,
  Calendar,
  Check,
  ChevronRight,
  Cpu,
  Database,
  DoorOpen,
  Fan,
  Flame,
  Footprints,
  Home,
  Laptop,
  LayoutGrid,
  Lightbulb,
  Menu,
  Mic,
  MicOff,
  Radio,
  RefreshCw,
  Router,
  Server,
  Settings,
  Smartphone,
  Speaker,
  Tablet,
  Thermometer,
  Trees,
  Tv,
  Utensils,
  Video,
  Wifi,
  Workflow,
  X,
} from 'lucide-react';
import { type DienState, doTuoiDien, type KetNoi } from '../data/dien.js';
import type { CameraIconKey, DestRoute, QuickIconKey, RoomIconKey } from '../data/models.js';
import type { ServerContainerIconKey } from '../data/server.js';
import type { WifiDeviceIconKey } from '../data/wifi.js';
import type { ToastMessage, TrangThaiCam } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

export function DestIcon({ route, size = 18 }: { route: DestRoute; size?: number }) {
  switch (route) {
    case 'home': return <Home size={size} />;
    case 'devices': return <Cpu size={size} />;
    case 'camera': return <Video size={size} />;
    case 'sensors': return <Activity size={size} />;
    case 'automation': return <Workflow size={size} />;
    case 'settings': return <Settings size={size} />;
    case 'server': return <Server size={size} />;
    case 'stats': return <BarChart3 size={size} />;
    case 'utilities': return <LayoutGrid size={size} />;
  }
}

export function RoomIcon({ icon, size = 18 }: { icon: RoomIconKey; size?: number }) {
  switch (icon) {
    case 'weekend': return <Home size={size} />;
    case 'bed': return <BedDouble size={size} />;
    case 'kitchen': return <Utensils size={size} />;
    case 'stairs': return <Footprints size={size} />;
  }
}

export function QuickIcon({ icon, size = 20 }: { icon: QuickIconKey; size?: number }) {
  switch (icon) {
    case 'lightbulb': return <Lightbulb size={size} />;
    case 'air': return <Fan size={size} />;
    case 'ac': return <AirVent size={size} />;
    case 'blinds': return <Blinds size={size} />;
  }
}

export function CamIcon({ icon, size = 18 }: { icon: CameraIconKey; size?: number }) {
  switch (icon) {
    case 'door': return <DoorOpen size={size} />;
    case 'grass': return <Trees size={size} />;
    case 'kitchen': return <Utensils size={size} />;
    case 'stairs': return <Footprints size={size} />;
    case 'videocam': return <Video size={size} />;
  }
}

export function WifiDevIcon({ icon, size = 18 }: { icon: WifiDeviceIconKey; size?: number }) {
  switch (icon) {
    case 'phone': return <Smartphone size={size} />;
    case 'laptop': return <Laptop size={size} />;
    case 'tv': return <Tv size={size} />;
    case 'videocam': return <Video size={size} />;
    case 'speaker': return <Speaker size={size} />;
    case 'lightbulb': return <Lightbulb size={size} />;
    case 'tablet': return <Tablet size={size} />;
    case 'devices': return <Cpu size={size} />;
  }
}

export function ContainerIcon({ icon, size = 17 }: { icon: ServerContainerIconKey; size?: number }) {
  switch (icon) {
    case 'hub': return <Radio size={size} />;
    case 'bolt': return <Bolt size={size} />;
    case 'router': return <Router size={size} />;
    case 'videocam': return <Video size={size} />;
    case 'dns': return <Server size={size} />;
    case 'storage': return <Database size={size} />;
    case 'widgets': return <Box size={size} />;
  }
}

export function SmartCard({
  children,
  className = '',
  style,
  glowColor,
  onClick,
}: {
  children: React.ReactNode;
  className?: string;
  style?: React.CSSProperties;
  glowColor?: string;
  onClick?: () => void;
}) {
  const baseStyle: React.CSSProperties = {
    backgroundColor: SH.Card,
    border: `1px solid ${glowColor ? tint(glowColor, 0.35) : SH.Border}`,
    boxShadow: glowColor ? `0 0 24px -6px ${tint(glowColor, 0.28)}` : undefined,
    ...style,
  };
  return (
    <div
      onClick={onClick}
      className={`rounded-2xl p-3 transition-colors ${onClick ? 'cursor-pointer hover:bg-[#1A2744]' : ''} ${className}`}
      style={baseStyle}
    >
      {children}
    </div>
  );
}

export function IconBadge({
  children,
  color,
  size = 38,
  active = true,
  className = '',
}: {
  children: React.ReactNode;
  color: string;
  size?: number;
  active?: boolean;
  className?: string;
}) {
  return (
    <div
      className={`inline-flex items-center justify-center rounded-xl shrink-0 transition-colors ${className}`}
      style={{
        width: size,
        height: size,
        backgroundColor: active ? tint(color, 0.16) : tint(SH.TextDim, 0.14),
        border: `1px solid ${active ? tint(color, 0.32) : tint(SH.Border, 0.6)}`,
        color: active ? color : SH.TextSecondary,
      }}
    >
      {children}
    </div>
  );
}

export function Dot({ color, pulse = false, size = 7 }: { color: string; pulse?: boolean; size?: number }) {
  return (
    <span className="relative inline-flex shrink-0" style={{ width: size, height: size }}>
      {pulse && (
        <span
          className="animate-ping absolute inline-flex h-full w-full rounded-full opacity-75"
          style={{ backgroundColor: color }}
        />
      )}
      <span
        className="relative inline-flex rounded-full"
        style={{ width: size, height: size, backgroundColor: color }}
      />
    </span>
  );
}

export function StatusPill({
  label,
  color,
  dot = true,
  pulse = false,
  className = '',
}: {
  label: string;
  color: string;
  dot?: boolean;
  pulse?: boolean;
  className?: string;
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold tracking-tight whitespace-nowrap ${className}`}
      style={{
        backgroundColor: tint(color, 0.14),
        border: `1px solid ${tint(color, 0.32)}`,
        color,
      }}
    >
      {dot && <Dot color={color} pulse={pulse} size={6} />}
      <span>{label}</span>
    </span>
  );
}

export function DemoBadge({ label = 'MẪU', className = '' }: { label?: string; className?: string }) {
  return (
    <span
      className={`inline-flex items-center rounded-md px-1.5 py-0.5 text-[10px] font-bold tracking-wider uppercase ${className}`}
      style={{
        backgroundColor: tint(SH.Amber, 0.14),
        border: `1px solid ${tint(SH.Amber, 0.35)}`,
        color: SH.Amber,
      }}
    >
      {label}
    </span>
  );
}

export function RoundIconButton({
  children,
  onClick,
  disabled = false,
  title,
  color,
  size = 34,
}: {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  title?: string;
  color?: string;
  size?: number;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      title={title}
      className="inline-flex items-center justify-center rounded-xl transition-all active:scale-95 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
      style={{
        width: size,
        height: size,
        backgroundColor: color ? tint(color, 0.16) : SH.SurfaceHi,
        border: `1px solid ${color ? tint(color, 0.35) : SH.Border}`,
        color: color || SH.TextPrimary,
      }}
    >
      {children}
    </button>
  );
}

export function FlatButton({
  children,
  onClick,
  disabled = false,
  title,
  color = SH.Blue,
  variant = 'soft',
  className = '',
  type = 'button',
}: {
  children: React.ReactNode;
  onClick?: () => void;
  disabled?: boolean;
  title?: string;
  color?: string;
  variant?: 'soft' | 'solid' | 'outline';
  className?: string;
  type?: 'button' | 'submit';
}) {
  const style: React.CSSProperties =
    variant === 'solid'
      ? { backgroundColor: color, color: '#ffffff', border: `1px solid ${color}` }
      : variant === 'outline'
        ? { backgroundColor: 'transparent', color, border: `1px solid ${tint(color, 0.5)}` }
        : { backgroundColor: tint(color, 0.16), color, border: `1px solid ${tint(color, 0.35)}` };

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={disabled}
      title={title}
      style={style}
      className={`inline-flex items-center justify-center gap-2 rounded-xl px-3.5 py-2 text-xs font-semibold transition-all enabled:hover:brightness-110 enabled:active:scale-[0.98] disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer ${className}`}
    >
      {children}
    </button>
  );
}

export function ChoiceChip({
  label,
  selected,
  onClick,
  color = SH.Blue,
  icon,
  disabled = false,
}: {
  label: string;
  selected: boolean;
  onClick: () => void;
  color?: string;
  icon?: React.ReactNode;
  disabled?: boolean;
}) {
  return (
    <button
      type="button"
      disabled={disabled}
      onClick={onClick}
      className="inline-flex items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition-all active:scale-95 disabled:opacity-45 cursor-pointer"
      style={{
        backgroundColor: selected ? tint(color, 0.2) : SH.Surface,
        border: `1px solid ${selected ? color : SH.Border}`,
        color: selected ? color : SH.TextSecondary,
      }}
    >
      {icon}
      <span>{label}</span>
    </button>
  );
}

export function SectionHeader({
  title,
  subtitle,
  action,
  badge,
}: {
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
  badge?: React.ReactNode;
}) {
  return (
    <div className="flex items-center justify-between gap-3">
      <div className="min-w-0">
        <div className="flex items-center gap-2">
          <h2 className="text-sm font-bold tracking-tight text-[#E6EDF7]">{title}</h2>
          {badge}
        </div>
        {subtitle && <p className="text-xs text-[#8A9BB8] mt-0.5 truncate">{subtitle}</p>}
      </div>
      {action && <div className="shrink-0">{action}</div>}
    </div>
  );
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
}) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-8 px-4 rounded-2xl border border-dashed border-[#253459] bg-[#0E1627]/60">
      <div className="mb-3 text-[#5F6F8C]">{icon}</div>
      <div className="text-sm font-semibold text-[#E6EDF7]">{title}</div>
      {description && <p className="text-xs text-[#8A9BB8] mt-1 max-w-md">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function ImagePlaceholder({
  title,
  subtitle,
  color = SH.Blue,
  icon,
  aspect = '16 / 9',
  fill = false,
}: {
  title: string;
  subtitle: string;
  color?: string;
  icon?: React.ReactNode;
  aspect?: string;
  fill?: boolean;
}) {
  return (
    <div
      className={`relative w-full overflow-hidden rounded-xl border border-[#253459] flex flex-col items-center justify-center p-3 text-center ${
        fill ? 'flex-1 min-h-0 h-full' : ''
      }`}
      style={{
        ...(fill ? {} : { aspectRatio: aspect }),
        background: `radial-gradient(circle at 50% 35%, ${tint(color, 0.16)}, #0B1220 75%)`,
      }}
    >
      <div
        className="mb-2.5 flex h-11 w-11 items-center justify-center rounded-2xl"
        style={{
          backgroundColor: tint(color, 0.15),
          border: `1px solid ${tint(color, 0.35)}`,
          color,
        }}
      >
        {icon || <Video size={22} />}
      </div>
      <div className="text-xs font-bold text-[#E6EDF7]">{title}</div>
      <div className="text-[11px] text-[#8A9BB8] mt-1 max-w-xs">{subtitle}</div>
    </div>
  );
}

export function TrangThaiKetNoi({
  ketNoi,
  dien,
  nowMs,
  compact = false,
}: {
  ketNoi: KetNoi;
  dien?: DienState;
  nowMs?: number;
  compact?: boolean;
}) {
  if (ketNoi.state === 'chua_cau_hinh') {
    return <StatusPill label={compact ? 'Chưa cài MQTT' : 'Chưa cấu hình MQTT'} color={SH.TextDim} />;
  }
  if (ketNoi.state === 'dang_noi') {
    return <StatusPill label="Đang nối MQTT…" color={SH.Amber} pulse />;
  }
  if (ketNoi.state === 'loi') {
    return <StatusPill label={compact ? 'Lỗi MQTT' : `Lỗi: ${ketNoi.moTa}`} color={SH.Red} />;
  }
  if (dien) {
    const tuoi = doTuoiDien(dien, new Date(nowMs ?? Date.now()));
    if (tuoi === 'QUA_CU') {
      return <StatusPill label={compact ? 'Số cũ >24h' : 'Đã nối · số điện >24h'} color={SH.Amber} />;
    }
    if (tuoi === 'HOI_CU') {
      return <StatusPill label={compact ? 'Hơi cũ' : 'Đã nối · hơi cũ'} color={SH.Amber} />;
    }
  }
  return <StatusPill label={compact ? 'Đã nối MQTT' : 'Đã nối máy chủ'} color={SH.Green} />;
}

export function TrangThaiCameraPill({ cam }: { cam: TrangThaiCam }) {
  if (cam.state === 'dang_do') {
    return <StatusPill label="Đang dò go2rtc…" color={SH.Amber} pulse />;
  }
  if (cam.state === 'co') {
    return (
      <StatusPill
        label={cam.ds.length > 0 ? `${cam.ds.length} luồng live` : '0 luồng'}
        color={cam.ds.length > 0 ? SH.Green : SH.Amber}
      />
    );
  }
  return <StatusPill label="go2rtc ngoại tuyến" color={SH.Red} />;
}

export function TopBar({
  title,
  subtitle,
  onBack,
  backLabel,
  onMenuOpen,
  onOpenSettings,
  mqttStatus,
  cameraStatus,
  dien,
  nowMs,
  onVoiceToggle,
  voiceActive,
  extra,
}: {
  title: string;
  subtitle?: string;
  onBack?: () => void;
  backLabel?: string;
  onMenuOpen?: () => void;
  onOpenSettings?: () => void;
  mqttStatus: KetNoi;
  cameraStatus: TrangThaiCam;
  dien: DienState;
  nowMs: number;
  onVoiceToggle: () => void;
  voiceActive: boolean;
  extra?: React.ReactNode;
}) {
  return (
    <header className="sticky top-0 z-20 flex items-center justify-between gap-3 border-b border-[#253459] bg-[#0B1220]/90 px-4 py-2 backdrop-blur-md shrink-0">
      <div className="flex items-center gap-2.5 min-w-0">
        {onMenuOpen && (
          <button
            type="button"
            onClick={onMenuOpen}
            className="md:hidden inline-flex h-8 w-8 items-center justify-center rounded-xl border border-[#253459] bg-[#16213A] text-[#E6EDF7]"
            title="Mở menu"
          >
            <Menu size={17} />
          </button>
        )}
        {onBack && (
          <button
            type="button"
            onClick={onBack}
            className="inline-flex items-center gap-1.5 rounded-xl border border-[#253459] bg-[#16213A] px-2.5 py-1 text-xs font-semibold text-[#E6EDF7] hover:bg-[#1D2B4C] cursor-pointer"
          >
            <ArrowLeft size={14} />
            <span>{backLabel || 'Quay lại'}</span>
          </button>
        )}
        <div className="min-w-0">
          <h1 className="text-sm sm:text-base font-bold tracking-tight text-[#E6EDF7] truncate">{title}</h1>
          {subtitle && <p className="text-[11px] text-[#8A9BB8] truncate">{subtitle}</p>}
        </div>
      </div>

      <div className="flex items-center gap-2 shrink-0">
        {extra}
        <button
          type="button"
          onClick={onOpenSettings}
          title="Bấm để mở Cài đặt & cấu hình MQTT / go2rtc"
          className={`hidden sm:flex items-center gap-2 ${onOpenSettings ? 'cursor-pointer hover:brightness-110 transition-all' : ''}`}
        >
          <TrangThaiKetNoi ketNoi={mqttStatus} dien={dien} nowMs={nowMs} compact />
          <TrangThaiCameraPill cam={cameraStatus} />
        </button>
        <button
          type="button"
          onClick={onVoiceToggle}
          title="Trợ lý giọng nói Gemini"
          className="md:hidden inline-flex items-center gap-1.5 rounded-xl px-3 py-1 text-xs font-semibold transition-all cursor-pointer"
          style={{
            backgroundColor: voiceActive ? tint(SH.Green, 0.22) : tint(SH.Blue, 0.16),
            border: `1px solid ${voiceActive ? SH.Green : tint(SH.Blue, 0.4)}`,
            color: voiceActive ? SH.Green : SH.Blue,
          }}
        >
          {voiceActive ? <Mic size={15} /> : <MicOff size={15} />}
          <span className="hidden sm:inline">{voiceActive ? 'Đang nghe' : 'Giọng nói'}</span>
        </button>
      </div>
    </header>
  );
}

export function ToastHost({
  toasts,
  onDismiss,
}: {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}) {
  if (toasts.length === 0) return null;
  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none px-4 sm:px-0">
      {toasts.map((t) => {
        const color =
          t.tone === 'success'
            ? SH.Green
            : t.tone === 'warning'
              ? SH.Amber
              : t.tone === 'error'
                ? SH.Red
                : SH.Blue;
        return (
          <div
            key={t.id}
            className="pointer-events-auto flex items-start gap-2.5 rounded-xl px-3.5 py-2.5 shadow-xl backdrop-blur-md"
            style={{
              backgroundColor: '#16213AEF',
              border: `1px solid ${tint(color, 0.55)}`,
            }}
          >
            <span className="mt-0.5 shrink-0" style={{ color }}>
              {t.tone === 'success' ? (
                <Check size={16} />
              ) : t.tone === 'warning' ? (
                <AlertTriangle size={16} />
              ) : t.tone === 'error' ? (
                <AlertCircle size={16} />
              ) : (
                <Thermometer size={16} />
              )}
            </span>
            <p className="text-xs text-[#E6EDF7] leading-relaxed flex-1">{t.text}</p>
            <button
              type="button"
              onClick={() => onDismiss(t.id)}
              className="text-[#8A9BB8] hover:text-white cursor-pointer"
            >
              <X size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
}

export { ChevronRight, RefreshCw, Wifi, Flame, Calendar };

