import { SH } from '../theme/colors.js';

export type RoomMode = 'SLEEP' | 'HOME' | 'AWAY' | 'CUSTOM';

export const ROOM_MODE_LABELS: Record<RoomMode, string> = {
  SLEEP: 'Ngủ',
  HOME: 'Nhà',
  AWAY: 'Vắng nhà',
  CUSTOM: 'Tùy chỉnh',
};

export const ROOM_MODES: RoomMode[] = ['SLEEP', 'HOME', 'AWAY', 'CUSTOM'];

export type HallwayView = 'DAU_HE' | 'XUNG_QUANH' | 'TU_XA';

export const HALLWAY_VIEWS: Array<{ id: HallwayView; label: string; camTitle: string }> = [
  { id: 'DAU_HE', label: 'Đầu hè', camTitle: 'CAM-01 ĐẦU HÈ' },
  { id: 'XUNG_QUANH', label: 'Xung quanh', camTitle: 'CAM-02 XUNG QUANH' },
  { id: 'TU_XA', label: 'Từ xa', camTitle: 'CAM-03 TỪ XA' },
];

export type RoomIconKey = 'weekend' | 'bed' | 'kitchen' | 'stairs';

export interface RoomUi {
  id: string;
  name: string;
  icon: RoomIconKey;
  accent: string;
  lightOn: boolean;
  fanOn: boolean;
  acOn: boolean;
  acTemp: number;
  mode: RoomMode;
  scheduleOn: boolean;
  scheduleLabel: string;
  scheduleTime: string;
  lightBrightness: number; // 10..100 (%)
  fanSpeed: number; // 1..3
  tvOn: boolean;
  curtainOpen: number; // 0..100 (%)
  hoodSpeed: number; // 1..3
  stoveOn: boolean;
  stoveLevel: number; // 1..9
  gasAlert: boolean;
  gasPpm: number;
  hallView: HallwayView;
  hallFrontLightOn: boolean;
  hallFrontBrightness: number;
  hallSurroundingLightOn: boolean;
  hallSurroundingBrightness: number;
  hallDoorLocked: boolean;
  hallCameraOn: boolean;
  hallIrMode: boolean;
  hallMotionAuto: boolean;
  hallMotionDetected: boolean;
}

export function stoveWatts(level: number): number {
  const clamped = Math.min(9, Math.max(1, Math.trunc(level)));
  switch (clamped) {
    case 1: return 200;
    case 2: return 400;
    case 3: return 650;
    case 4: return 900;
    case 5: return 1200;
    case 6: return 1450;
    case 7: return 1700;
    case 8: return 1950;
    default: return 2200;
  }
}

export type QuickIconKey = 'lightbulb' | 'air' | 'ac' | 'blinds';

export interface QuickItem {
  id: string;
  label: string;
  icon: QuickIconKey;
  color: string;
  on: boolean;
}

export type CameraIconKey = 'door' | 'grass' | 'kitchen' | 'stairs' | 'videocam';

export interface CameraFeed {
  id: string;
  name: string;
  icon: CameraIconKey;
  accent: string;
  luong: string | null;
}

export type StatKindId = 'POWER' | 'TEMP' | 'HUMID' | 'WATER' | 'GAS';

export interface StatKindInfo {
  id: StatKindId;
  label: string;
  title: string;
  unit: string;
  color: string;
}

export const STAT_KINDS: StatKindInfo[] = [
  { id: 'POWER', label: 'Điện năng', title: 'Điện năng tiêu thụ', unit: 'kWh', color: SH.Blue },
  { id: 'TEMP', label: 'Nhiệt độ', title: 'Nhiệt độ trung bình', unit: '°C', color: SH.Red },
  { id: 'HUMID', label: 'Độ ẩm', title: 'Độ ẩm trung bình', unit: '%', color: SH.Cyan },
  { id: 'WATER', label: 'Nước', title: 'Nước tiêu thụ', unit: 'm³', color: SH.Teal },
  { id: 'GAS', label: 'Gas', title: 'Mức gas còn lại', unit: '%', color: SH.Amber },
];

export type StatRangeId = 'WEEK' | 'FORTNIGHT' | 'MONTH';

export const STAT_RANGES: Array<{ id: StatRangeId; label: string; days: number }> = [
  { id: 'WEEK', label: '7 ngày', days: 7 },
  { id: 'FORTNIGHT', label: '14 ngày', days: 14 },
  { id: 'MONTH', label: '30 ngày', days: 30 },
];

export interface StatSeries {
  kind: StatKindInfo;
  total: string;
  unit: string;
  deltaPercent: number;
  bars: number[];
  labels: string[];
  gaugeLabel: string;
  gaugeValue: string;
  gaugeFraction: number;
  costLabel: string;
  costValue: string;
  statusLabel: string;
  statusOk: boolean;
}

export type DestRoute =
  | 'home'
  | 'devices'
  | 'camera'
  | 'sensors'
  | 'automation'
  | 'settings'
  | 'server'
  | 'stats'
  | 'utilities';

export type NavGroupId = 'DAILY' | 'SYSTEM';

export const NAV_GROUP_TITLES: Record<NavGroupId, string> = {
  DAILY: 'HẰNG NGÀY',
  SYSTEM: 'HỆ THỐNG',
};

export interface DestItem {
  route: DestRoute;
  title: string;
  built: boolean;
  group: NavGroupId;
}

export const DEST_ITEMS: DestItem[] = [
  // Cụm 1 — Sử dụng hằng ngày
  { route: 'home', title: 'Trang chủ', built: true, group: 'DAILY' },
  { route: 'devices', title: 'Thiết bị', built: true, group: 'DAILY' },
  { route: 'camera', title: 'Camera', built: true, group: 'DAILY' },
  { route: 'utilities', title: 'Tiện ích', built: true, group: 'DAILY' },

  // Cụm 2 — Quản trị & Hệ thống
  { route: 'automation', title: 'Tự động hóa', built: true, group: 'SYSTEM' },
  { route: 'sensors', title: 'Cảm biến', built: true, group: 'SYSTEM' },
  { route: 'stats', title: 'Thống kê', built: true, group: 'SYSTEM' },
  { route: 'server', title: 'Server', built: true, group: 'SYSTEM' },
  { route: 'settings', title: 'Cài đặt', built: true, group: 'SYSTEM' },
];

