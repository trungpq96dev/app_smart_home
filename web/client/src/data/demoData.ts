import { SH } from '../theme/colors.js';
import {
  STAT_KINDS,
  STAT_RANGES,
  type CameraFeed,
  type QuickItem,
  type RoomUi,
  type StatKindId,
  type StatRangeId,
  type StatSeries,
} from './models.js';
import type {
  ServerContainer,
  ServerDay,
  ServerInfo,
  ServerMetric,
  ServerPower,
  ServerState,
  ServerTemp,
} from './server.js';
import type {
  ChannelNoise,
  ConnectionLog,
  SecuritySetting,
  StatsDataset,
  UnknownDevice,
  WifiDevice,
  WifiInfo,
  WifiState,
} from './wifi.js';

/**
 * TOAN BO SO LIEU MAU CUA APP NAM TRONG FILE NAY (port 1:1 tu DemoData.kt).
 */

function createSeededRng(seed: number) {
  let s = seed | 0;
  return {
    nextFloat(): number {
      s = (Math.imul(1664525, s) + 1013904223) | 0;
      return ((s >>> 0) % 1000000) / 1000000;
    },
    nextInt(bound: number): number {
      return Math.floor(this.nextFloat() * bound);
    },
  };
}

function fmt1(v: number): string {
  const r = Math.round(v * 10) / 10;
  return r === Math.floor(r) ? String(Math.trunc(r)) : String(r);
}

function vnd(v: number): string {
  const n = Math.round(Math.abs(v) / 1000) * 1000;
  const s = String(n).split('').reverse().join('').match(/.{1,3}/g)?.join('.').split('').reverse().join('') || '0';
  return `${s} đ`;
}

export const DemoData = {
  IS_DEMO: true,
  indoorTemp: '26.5',
  humidity: '68',
  powerNow: '1.25',
  safetyLabel: 'An toàn',
  systemStatus: 'Hệ thống hoạt động bình thường',

  rooms(): RoomUi[] {
    return [
      {
        id: 'living',
        name: 'Phòng khách',
        icon: 'weekend',
        accent: SH.Amber,
        lightOn: true,
        fanOn: false,
        acOn: true,
        acTemp: 25,
        mode: 'HOME',
        scheduleOn: true,
        scheduleLabel: 'Tắt đèn lúc',
        scheduleTime: '23:30',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
      {
        id: 'bed1',
        name: 'Phòng ngủ 1',
        icon: 'bed',
        accent: SH.Purple,
        lightOn: true,
        fanOn: false,
        acOn: true,
        acTemp: 25,
        mode: 'HOME',
        scheduleOn: true,
        scheduleLabel: 'Tắt đèn lúc',
        scheduleTime: '23:00',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
      {
        id: 'bed2',
        name: 'Phòng ngủ 2',
        icon: 'bed',
        accent: SH.Blue,
        lightOn: false,
        fanOn: true,
        acOn: false,
        acTemp: 27,
        mode: 'AWAY',
        scheduleOn: false,
        scheduleLabel: 'Tắt đèn lúc',
        scheduleTime: '22:30',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
      {
        id: 'bed3',
        name: 'Phòng ngủ 3',
        icon: 'bed',
        accent: SH.Teal,
        lightOn: false,
        fanOn: false,
        acOn: false,
        acTemp: 26,
        mode: 'AWAY',
        scheduleOn: false,
        scheduleLabel: 'Tắt đèn lúc',
        scheduleTime: '22:00',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
      {
        id: 'kitchen',
        name: 'Nhà bếp',
        icon: 'kitchen',
        accent: SH.Green,
        lightOn: true,
        fanOn: false,
        acOn: false,
        acTemp: 28,
        mode: 'SLEEP',
        scheduleOn: true,
        scheduleLabel: 'Tự tắt hút mùi sau 15 phút',
        scheduleTime: 'Hẹn giờ thông minh',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
      {
        id: 'hall',
        name: 'Hành lang',
        icon: 'stairs',
        accent: SH.Cyan,
        lightOn: true,
        fanOn: false,
        acOn: false,
        acTemp: 26,
        mode: 'HOME',
        scheduleOn: true,
        scheduleLabel: 'Tự bật đèn phía trước & xung quanh',
        scheduleTime: '18:00 - 05:30',
        lightBrightness: 100,
        fanSpeed: 1,
        tvOn: false,
        curtainOpen: 100,
        hoodSpeed: 1,
        stoveOn: false,
        stoveLevel: 5,
        gasAlert: false,
        gasPpm: 18,
        hallView: 'DAU_HE',
        hallFrontLightOn: true,
        hallFrontBrightness: 100,
        hallSurroundingLightOn: true,
        hallSurroundingBrightness: 80,
        hallDoorLocked: true,
        hallCameraOn: true,
        hallIrMode: false,
        hallMotionAuto: true,
        hallMotionDetected: false,
      },
    ];
  },

  quickItems(): QuickItem[] {
    return [
      { id: 'light', label: 'Đèn', icon: 'lightbulb', color: SH.Amber, on: true },
      { id: 'fan', label: 'Quạt', icon: 'air', color: SH.Blue, on: true },
      { id: 'ac', label: 'Điều hòa', icon: 'ac', color: SH.Purple, on: false },
      { id: 'curtain', label: 'Rèm', icon: 'blinds', color: SH.Teal, on: false },
    ];
  },

  cameras(): CameraFeed[] {
    return [
      { id: 'front', name: 'Cửa chính', icon: 'door', accent: SH.Blue, luong: null },
      { id: 'yard', name: 'Sân vườn', icon: 'grass', accent: SH.Green, luong: null },
      { id: 'kitchen', name: 'Nhà bếp', icon: 'kitchen', accent: SH.Amber, luong: null },
      { id: 'hall', name: 'Hành lang', icon: 'stairs', accent: SH.Purple, luong: null },
    ];
  },

  series(kindId: StatKindId, rangeId: StatRangeId): StatSeries {
    const kindIdx = STAT_KINDS.findIndex((k) => k.id === kindId);
    const rangeIdx = STAT_RANGES.findIndex((r) => r.id === rangeId);
    const kind = STAT_KINDS[Math.max(0, kindIdx)];
    const range = STAT_RANGES[Math.max(0, rangeIdx)];
    const n = range.days;
    const rnd = createSeededRng(kindIdx * 100 + rangeIdx + 17);

    let base = 6.2;
    let spread = 3.4;
    if (kindId === 'TEMP') { base = 27.5; spread = 3.0; }
    else if (kindId === 'HUMID') { base = 66; spread = 12; }
    else if (kindId === 'WATER') { base = 0.9; spread = 0.5; }
    else if (kindId === 'GAS') { base = 72; spread = 10; }

    const bars = Array.from({ length: n }, () => base + (rnd.nextFloat() - 0.45) * spread);
    const labels =
      n <= 7
        ? ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'].slice(0, n)
        : Array.from({ length: n }, (_, i) => (i % Math.trunc(n / 7) === 0 ? String(i + 1) : ''));

    const sum = bars.reduce((a, b) => a + b, 0);
    const avg = sum / n;
    const delta = rnd.nextInt(26) - 14;
    const minVal = Math.min(...bars);
    const maxVal = Math.max(...bars);
    const lastVal = bars[bars.length - 1];

    switch (kindId) {
      case 'POWER':
        return {
          kind,
          total: fmt1(sum),
          unit: kind.unit,
          deltaPercent: delta,
          bars,
          labels,
          gaugeLabel: 'Công suất hiện tại',
          gaugeValue: '1.25 kW',
          gaugeFraction: 0.42,
          costLabel: 'Chi phí ước tính',
          costValue: vnd(sum * 2600),
          statusLabel: 'Bình thường',
          statusOk: true,
        };
      case 'TEMP':
        return {
          kind,
          total: fmt1(avg),
          unit: kind.unit,
          deltaPercent: delta,
          bars,
          labels,
          gaugeLabel: 'Nhiệt độ hiện tại',
          gaugeValue: '26.5 °C',
          gaugeFraction: 0.55,
          costLabel: 'Cao nhất trong kỳ',
          costValue: `${fmt1(maxVal)} °C`,
          statusLabel: 'Trong ngưỡng',
          statusOk: true,
        };
      case 'HUMID':
        return {
          kind,
          total: fmt1(avg),
          unit: kind.unit,
          deltaPercent: delta,
          bars,
          labels,
          gaugeLabel: 'Độ ẩm hiện tại',
          gaugeValue: '68 %',
          gaugeFraction: 0.68,
          costLabel: 'Thấp nhất trong kỳ',
          costValue: `${fmt1(minVal)} %`,
          statusLabel: 'Trong ngưỡng',
          statusOk: true,
        };
      case 'WATER':
        return {
          kind,
          total: fmt1(sum),
          unit: kind.unit,
          deltaPercent: delta,
          bars,
          labels,
          gaugeLabel: 'Mức bồn trên',
          gaugeValue: '72 %',
          gaugeFraction: 0.72,
          costLabel: 'Chi phí ước tính',
          costValue: vnd(sum * 11000),
          statusLabel: 'Bơm nghỉ',
          statusOk: true,
        };
      case 'GAS':
        return {
          kind,
          total: fmt1(lastVal),
          unit: kind.unit,
          deltaPercent: delta,
          bars,
          labels,
          gaugeLabel: 'Bình gas còn lại',
          gaugeValue: `${fmt1(lastVal)} %`,
          gaugeFraction: lastVal / 100,
          costLabel: 'Dự kiến hết sau',
          costValue: '18 ngày',
          statusLabel: 'Không rò rỉ',
          statusOk: true,
        };
    }
  },

  wifiInfo(): WifiInfo {
    return {
      ssid: 'SmartHome_5G',
      security: 'WPA2/WPA3',
      signalDbm: -62,
      signalLabel: 'Tốt',
      downloadMbps: 56.4,
      uploadMbps: 24.8,
      speedMbps: 150,
      ipRouter: '192.168.0.1',
      dns: '8.8.8.8',
      gateway: '192.168.0.1',
      isConnected: true,
      uptimeDays: 2,
      uptimeHours: 3,
    };
  },

  wifiDevices(): WifiDevice[] {
    return [
      { id: '1', name: 'DESKTOP-O71L6K8', icon: 'laptop', ip: '192.168.0.105', isOnline: true, downloadMB: 16.4, uploadMB: 3.2, mac: '00:1A:2B:3C:4D:5E', brand: 'Dell', type: 'Máy tính', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '-41 dBm' },
      { id: '2', name: 'iPhone 15 Pro Max', icon: 'phone', ip: '192.168.0.102', isOnline: true, downloadMB: 8.2, uploadMB: 1.9, mac: '3C:00:F8:88:99:AA', brand: 'Apple', type: 'Điện thoại', band: '5GHz', isNew: true, trusted: true, blocked: false, signal: '-38 dBm' },
      { id: '3', name: 'Galaxy-Tab-A7-Lite', icon: 'tablet', ip: '192.168.0.101', isOnline: true, downloadMB: 4.1, uploadMB: 0.6, mac: 'B4:EF:39:11:22:08', brand: 'Samsung', type: 'Tablet', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '-48 dBm' },
      { id: '4', name: 'Smart TV Living Room', icon: 'tv', ip: '192.168.0.150', isOnline: true, downloadMB: 12.1, uploadMB: 0.4, mac: 'AA:88:CC:00:EE:FF', brand: 'LG', type: 'Smart TV', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '-45 dBm' },
      { id: '5', name: 'Dell XPS 15', icon: 'laptop', ip: '192.168.0.123', isOnline: true, downloadMB: 11.8, uploadMB: 2.8, mac: '00:1A:28:3C:40:5E', brand: 'Dell', type: 'Laptop', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '-43 dBm' },
      { id: '6', name: 'Camera hành lang', icon: 'videocam', ip: '192.168.0.118', isOnline: true, downloadMB: 0.4, uploadMB: 2.1, mac: 'A4:C1:38:90:12:4F', brand: 'Xiaomi', type: 'IoT', band: '2.4GHz', isNew: false, trusted: true, blocked: false, signal: '-55 dBm' },
      { id: '7', name: 'MacBook Pro M3', icon: 'laptop', ip: '192.168.0.112', isOnline: true, downloadMB: 6.5, uploadMB: 1.5, mac: 'F0:18:98:4A:77:BC', brand: 'Apple', type: 'Laptop', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '-39 dBm' },
      { id: '8', name: 'Galaxy S24 Ultra', icon: 'phone', ip: '192.168.0.129', isOnline: true, downloadMB: 3.9, uploadMB: 0.8, mac: '88:36:6C:AB:21:09', brand: 'Samsung', type: 'Điện thoại', band: '5GHz', isNew: true, trusted: false, blocked: false, signal: '-46 dBm' },
      { id: '9', name: 'TP-Link Smart Plug', icon: 'lightbulb', ip: '192.168.0.145', isOnline: true, downloadMB: 0.08, uploadMB: 0.02, mac: '98:DA:C4:6B:09:11', brand: 'TP-Link', type: 'IoT', band: '2.4GHz', isNew: false, trusted: false, blocked: false, signal: '-52 dBm' },
      { id: '10', name: 'iPad Air', icon: 'tablet', ip: '192.168.0.131', isOnline: false, downloadMB: 1.9, uploadMB: 0.2, mac: 'D8:BB:2C:7E:41:90', brand: 'Apple', type: 'Tablet', band: '5GHz', isNew: false, trusted: true, blocked: false, signal: '--' },
      { id: '11', name: 'Xiaomi Air Purifier', icon: 'lightbulb', ip: '192.168.0.142', isOnline: false, downloadMB: 0.1, uploadMB: 0.05, mac: '54:48:E6:1C:33:8A', brand: 'Xiaomi', type: 'IoT', band: '2.4GHz', isNew: false, trusted: false, blocked: false, signal: '--' },
      { id: '12', name: 'Loa AI Nest Hub', icon: 'speaker', ip: '192.168.0.148', isOnline: false, downloadMB: 0.3, uploadMB: 0.1, mac: '74:DA:38:55:19:C2', brand: 'Google', type: 'IoT', band: '2.4GHz', isNew: false, trusted: false, blocked: false, signal: '--' },
    ];
  },

  wifiUsage24h(): number[] {
    return [
      1.2, 0.8, 0.4, 0.3, 0.2, 0.5,
      2.1, 5.4, 8.2, 7.6, 6.3, 5.8,
      4.2, 3.9, 5.1, 6.7, 7.8, 9.2,
      10.5, 11.2, 9.8, 7.4, 4.6, 2.3,
    ];
  },

  wifiUnknownDevices(): UnknownDevice[] {
    return [
      { id: 'unk-1', name: 'Thiết bị không xác định (Android)', ip: '192.168.0.188', mac: '4E:21:9B:C0:77:1A', time: '21:32 - Hôm nay', reason: 'Cố gắng kết nối lần đầu' },
      { id: 'unk-2', name: 'Thiết bị lạ (ESP-IoT)', ip: '192.168.0.194', mac: 'CC:50:E3:8F:14:2D', time: '19:45 - Hôm nay', reason: 'Yêu cầu cấp IP DHCP mới' },
    ];
  },

  wifiConnectionLogs(): ConnectionLog[] {
    return [
      { time: '21:32', date: '19/11', device: '4E:21:9B:C0:77:1A', action: 'Phát hiện thiết bị lạ kết nối', type: 'warning' },
      { time: '20:18', date: '19/11', device: 'iPhone 15 Pro Max', action: 'Đã kết nối vào băng tần 5GHz', type: 'success' },
      { time: '19:45', date: '19/11', device: 'CC:50:E3:8F:14:2D', action: 'Yêu cầu kết nối bị tạm giữ', type: 'warning' },
      { time: '18:10', date: '19/11', device: 'Smart TV Living Room', action: 'Đã kết nối thành công', type: 'success' },
      { time: '16:05', date: '19/11', device: 'iPad Air', action: 'Ngắt kết nối WiFi', type: 'info' },
      { time: '14:20', date: '19/11', device: 'Hệ thống tường lửa', action: 'Đã chặn 3 gói tin quét cổng bất thường', type: 'danger' },
    ];
  },

  wifiSecuritySettings(): SecuritySetting[] {
    return [
      { key: 'firewall', title: 'Tường lửa (Firewall)', desc: 'Lọc lưu lượng độc hại và bảo vệ mạng nội bộ', enabled: true },
      { key: 'ddos', title: 'Chống tấn công DDoS', desc: 'Tự động giới hạn kết nối bất thường từ bên ngoài', enabled: true },
      { key: 'hideSsid', title: 'Ẩn tên WiFi (SSID)', desc: 'Không hiển thị tên WiFi trên danh sách dò tìm', enabled: false },
      { key: 'macFilter', title: 'Lọc địa chỉ MAC', desc: 'Chỉ cho phép thiết bị trong danh sách tin cậy kết nối', enabled: false },
      { key: 'newDeviceAlert', title: 'Cảnh báo khi có thiết bị mới', desc: 'Gửi thông báo ngay khi thiết bị lạ truy cập mạng', enabled: true },
    ];
  },

  wifiStatsDay(): StatsDataset {
    return {
      title: 'Biểu đồ lưu lượng (24 giờ qua)',
      downLabel: 'Tải xuống hôm nay', downVal: '45.2 GB', downSub: '+8.4% so với hôm qua',
      upLabel: 'Tải lên hôm nay', upVal: '8.7 GB', upSub: 'Ổn định trong khung giờ',
      totalLabel: 'Tổng lưu lượng tháng này', totalVal: '1.2 TB', totalSub: 'Trung bình 40 GB/ngày',
      peakVal: '185 Mb/s', peakSub: 'Ghi nhận lúc 20:15',
      labels: ['00h', '02h', '04h', '06h', '08h', '10h', '12h', '14h', '16h', '18h', '20h', '22h'],
      down: [1.8, 1.1, 0.7, 1.4, 3.8, 4.2, 3.5, 4.1, 5.2, 6.8, 7.6, 5.0],
      up: [0.3, 0.2, 0.15, 0.4, 0.9, 1.1, 0.8, 0.9, 1.0, 1.2, 1.1, 0.65],
      categories: [42, 26, 22, 10],
    };
  },

  wifiStatsWeek(): StatsDataset {
    return {
      title: 'Biểu đồ lưu lượng (7 ngày qua)',
      downLabel: 'Tải xuống tuần này', downVal: '286.4 GB', downSub: '+5.1% so với tuần trước',
      upLabel: 'Tải lên tuần này', upVal: '54.8 GB', upSub: 'Đạt đỉnh vào Thứ 7',
      totalLabel: 'Tổng lưu lượng tuần này', totalVal: '341.2 GB', totalSub: 'Trung bình 48.7 GB/ngày',
      peakVal: '214 Mb/s', peakSub: 'Ghi nhận Thứ 7, 21:05',
      labels: ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'],
      down: [34.2, 38.5, 36.1, 45.2, 41.8, 48.6, 42.0],
      up: [6.4, 7.1, 6.8, 8.7, 7.9, 9.8, 8.1],
      categories: [40, 28, 23, 9],
    };
  },

  wifiStatsMonth(): StatsDataset {
    return {
      title: 'Biểu đồ lưu lượng (30 ngày qua)',
      downLabel: 'Tải xuống tháng này', downVal: '1,024 GB', downSub: '+11.2% so với tháng trước',
      upLabel: 'Tải lên tháng này', upVal: '198.5 GB', upSub: 'Sao lưu Camera & Cloud',
      totalLabel: 'Tổng lưu lượng tháng này', totalVal: '1.22 TB', totalSub: 'Giới hạn gói: Không giới hạn',
      peakVal: '248 Mb/s', peakSub: 'Ghi nhận ngày 14/11',
      labels: ['Tuần 1', 'Tuần 2', 'Tuần 3', 'Tuần 4'],
      down: [242, 268, 254, 260],
      up: [46, 54, 48, 50.5],
      categories: [38, 27, 25, 10],
    };
  },

  wifiChannelNoise(): ChannelNoise[] {
    return [
      { channel: 36, noise: 62 },
      { channel: 44, noise: 48 },
      { channel: 149, noise: 18 },
      { channel: 157, noise: 35 },
    ];
  },

  initialWifiState(): WifiState {
    return {
      info: this.wifiInfo(),
      devices: this.wifiDevices(),
      usageChart24h: this.wifiUsage24h(),
      trangThai: 'ok',
      capNhat: null,
      isRealData: false,
      unknownDevices: this.wifiUnknownDevices(),
      connectionLogs: this.wifiConnectionLogs(),
      securitySettings: this.wifiSecuritySettings(),
      statsDay: this.wifiStatsDay(),
      statsWeek: this.wifiStatsWeek(),
      statsMonth: this.wifiStatsMonth(),
      channelNoise: this.wifiChannelNoise(),
    };
  },

  serverInfo(): ServerInfo {
    return {
      hostname: 'serverubuntu',
      os: 'Ubuntu 24.04 LTS',
      kernel: '6.8.0-generic',
      cpuModel: 'Intel Core i5',
      cpuCores: 4,
      ramTotalGb: 16,
      diskTotalGb: 256,
      uptimeDays: 3,
      uptimeHours: 7,
    };
  },

  serverMetric(): ServerMetric {
    return {
      cpuPercent: 18,
      ramUsedGb: 5.4,
      ramPercent: 34,
      diskUsedGb: 96,
      diskPercent: 38,
      cpuTempC: 46,
      load1: 0.35,
      load5: 0.42,
      load15: 0.38,
      netRxMbps: 1.8,
      netTxMbps: 0.6,
    };
  },

  serverContainers(): ServerContainer[] {
    return [
      {
        name: 'mosquitto',
        image: 'eclipse-mosquitto:2',
        state: 'running',
        status: 'Up 3 days',
        icon: 'hub',
        cpuPercent: 0.4,
        memMb: 14.2,
        memLimitMb: null,
        memPercent: null,
        protected: true,
        controllable: false,
      },
      {
        name: 'server-collector',
        image: 'server-collector:1.1',
        state: 'running',
        status: 'Up 3 days',
        icon: 'dns',
        cpuPercent: 1.2,
        memMb: 36.5,
        memLimitMb: 128,
        memPercent: 28.5,
        protected: true,
        controllable: false,
      },
      {
        name: 'smarthome-web',
        image: 'smarthome-web:0.9.4',
        state: 'running',
        status: 'Up 3 days',
        icon: 'dns',
        cpuPercent: 1.8,
        memMb: 92.4,
        memLimitMb: null,
        memPercent: null,
        protected: true,
        controllable: false,
      },
      {
        name: 'camera-recorder',
        image: 'smarthome-camera-recorder:1.0',
        state: 'running',
        status: 'Up 2 days',
        icon: 'videocam',
        cpuPercent: 42.6,
        memMb: 418.0,
        memLimitMb: 1024,
        memPercent: 40.8,
        protected: false,
        controllable: true,
      },
      {
        name: 'go2rtc',
        image: 'alexxit/go2rtc',
        state: 'running',
        status: 'Up 3 days',
        icon: 'videocam',
        cpuPercent: 4.5,
        memMb: 68.3,
        memLimitMb: null,
        memPercent: null,
        protected: false,
        controllable: false,
      },
      {
        name: 'wifi-collector',
        image: 'wifi-collector:1.0',
        state: 'running',
        status: 'Up 2 days',
        icon: 'router',
        cpuPercent: 0.6,
        memMb: 28.4,
        memLimitMb: 96,
        memPercent: 29.6,
        protected: false,
        controllable: true,
      },
      {
        name: 'dien-evn',
        image: 'dien-evn:1.0',
        state: 'running',
        status: 'Up 3 days',
        icon: 'bolt',
        cpuPercent: 0.2,
        memMb: 22.1,
        memLimitMb: null,
        memPercent: null,
        protected: false,
        controllable: true,
      },
    ];
  },

  serverTemps(): ServerTemp[] {
    return [
      { label: 'acpitz temp1', tempC: 40 },
      { label: 'acpitz temp3', tempC: 36 },
      { label: 'acpitz temp4', tempC: 43 },
      { label: 'acpitz temp5', tempC: 24 },
      { label: 'Package id 0', tempC: 45 },
      { label: 'Core 0', tempC: 43 },
      { label: 'Core 1', tempC: 39 },
    ];
  },

  serverPower(): ServerPower {
    return {
      batVoltageV: 12.13,
      batCurrentA: 0,
      batPowerW: 0,
      batStatus: 'Full',
      cpuPackageW: 4.2,
      cpuCoreW: 2.1,
      cpuUncoreW: 0,
    };
  },

  serverDays(): ServerDay[] {
    return [
      { date: '2026-11-19', wh: 41.43, tempMin: 37, tempMax: 48 },
      { date: '2026-11-18', wh: 47.55, tempMin: 37, tempMax: 60 },
      { date: '2026-11-17', wh: 38.06, tempMin: 43, tempMax: 57 },
      { date: '2026-11-06', wh: 36.41, tempMin: 45, tempMax: 60 },
      { date: '2026-11-05', wh: 72.83, tempMin: 42, tempMax: 62 },
      { date: '2026-11-04', wh: 18.19, tempMin: 44, tempMax: 60 },
    ];
  },

  initialServerState(): ServerState {
    return {
      info: this.serverInfo(),
      metric: this.serverMetric(),
      containers: this.serverContainers(),
      temps: this.serverTemps(),
      tempMinToday: 37,
      tempMaxToday: 47,
      power: this.serverPower(),
      days: this.serverDays(),
      capNhat: null,
      isRealData: false,
    };
  },
};

