import React, { useMemo, useState } from 'react';
import {
  Check,
  Copy,
  Download,
  Eye,
  EyeOff,
  Globe,
  PauseCircle,
  Power,
  RefreshCw,
  RotateCcw,
  Router,
  Search,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Wifi,
} from 'lucide-react';
import { DualLineAreaChart, ThinBar } from '../components/Charts.js';
import {
  ChoiceChip,
  DemoBadge,
  Dot,
  FlatButton,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
  WifiDevIcon,
} from '../components/Common.js';
import { DeviceSwitchRow } from '../components/Tiles.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH, tint } from '../theme/colors.js';

type WifiTab = 'overview' | 'devices' | 'stats' | 'security' | 'settings';
type StatsPeriod = 'day' | 'week' | 'month';
type DevFilter = 'all' | 'online' | 'offline' | 'new' | 'blocked';

const CATEGORY_META = [
  { label: 'Video & Giải trí 4K', color: SH.Blue },
  { label: 'Làm việc & Học tập', color: SH.Teal },
  { label: 'Thiết bị SmartHome IoT', color: SH.Green },
  { label: 'Mạng xã hội & Lướt web', color: SH.Purple },
  { label: 'Cập nhật & Khác', color: SH.Amber },
];

export function WifiScreen() {
  const {
    wifi,
    guiLenhWifi,
    khoiDongLaiWifi,
    toggleWifiSecurity,
    toggleWifiTrusted,
    toggleWifiBlocked,
    requireAdminPin,
    showToast,
  } = useSmartHome();

  const [tab, setTab] = useState<WifiTab>('overview');
  const [search, setSearch] = useState('');
  const [devFilter, setDevFilter] = useState<DevFilter>('all');
  const [statsPeriod, setStatsPeriod] = useState<StatsPeriod>('day');
  const [ssidInput, setSsidInput] = useState(wifi.info.ssid);
  const [wifiPass, setWifiPass] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [dnsMode, setDnsMode] = useState<'isp' | '1.1.1.1' | '8.8.8.8' | 'custom'>(() => {
    if (wifi.info.dns === '1.1.1.1') return '1.1.1.1';
    if (wifi.info.dns === '8.8.8.8') return '8.8.8.8';
    return 'isp';
  });
  const [customDns, setCustomDns] = useState(wifi.info.dns || '9.9.9.9');
  const [autoChannel5G, setAutoChannel5G] = useState(true);
  const [pingMonitor, setPingMonitor] = useState(true);
  const [guestPaused, setGuestPaused] = useState(false);
  const [confirmReboot, setConfirmReboot] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);

  const onlineUnblockedCount = useMemo(
    () => wifi.devices.filter((d) => d.isOnline && !d.blocked).length,
    [wifi.devices],
  );
  const offlineCount = useMemo(
    () => wifi.devices.filter((d) => !d.isOnline).length,
    [wifi.devices],
  );
  const newCount = useMemo(
    () => wifi.devices.filter((d) => d.isNew).length,
    [wifi.devices],
  );
  const trustedDevices = useMemo(
    () => wifi.devices.filter((d) => d.trusted),
    [wifi.devices],
  );
  const topBandwidthDevices = useMemo(
    () =>
      [...wifi.devices]
        .sort((a, b) => b.downloadMB + b.uploadMB - (a.downloadMB + a.uploadMB))
        .slice(0, 5),
    [wifi.devices],
  );

  const filteredDevices = useMemo(() => {
    const q = search.trim().toLowerCase();
    return wifi.devices.filter((d) => {
      if (devFilter === 'online' && (!d.isOnline || d.blocked)) return false;
      if (devFilter === 'offline' && d.isOnline) return false;
      if (devFilter === 'new' && !d.isNew) return false;
      if (devFilter === 'blocked' && !d.blocked) return false;
      if (!q) return true;
      return (
        d.name.toLowerCase().includes(q) ||
        d.ip.toLowerCase().includes(q) ||
        d.mac.toLowerCase().includes(q) ||
        d.brand.toLowerCase().includes(q) ||
        d.type.toLowerCase().includes(q)
      );
    });
  }, [wifi.devices, devFilter, search]);

  const activeStats =
    statsPeriod === 'day' ? wifi.statsDay : statsPeriod === 'week' ? wifi.statsWeek : wifi.statsMonth;

  const copyText = (label: string, val: string) => {
    if (navigator.clipboard?.writeText) {
      void navigator.clipboard.writeText(val);
      showToast(`Đã sao chép ${label}: ${val}`, 'success');
    }
  };

  const downloadBackupJson = () => {
    const payload = {
      exportedAt: new Date().toISOString(),
      ssid: wifi.info.ssid,
      routerIp: wifi.info.ipRouter,
      gateway: wifi.info.gateway,
      dns: wifi.info.dns,
      securitySettings: wifi.securitySettings,
      trustedDevices: trustedDevices.map((d) => ({ name: d.name, mac: d.mac, ip: d.ip })),
    };
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `wifi-router-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
    showToast('Đã tải xuống bản sao lưu cấu hình Router (.json).', 'success');
  };

  const triggerRouterReboot = () => {
    requireAdminPin('Khởi động lại Router WiFi', () => {
      setConfirmReboot(false);
      khoiDongLaiWifi();
    });
  };

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Router Header */}
      <SmartCard className="shrink-0 flex flex-col lg:flex-row lg:items-center justify-between gap-2 p-2.5">
        <div className="flex items-center gap-2.5">
          <IconBadge color={wifi.info.isConnected ? SH.Teal : SH.Red} size={36}>
            <Router size={18} />
          </IconBadge>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-sm font-bold text-[#E6EDF7]">{wifi.info.ssid}</h2>
              {!wifi.isRealData ? <DemoBadge /> : <StatusPill label="Router thật" color={SH.Green} />}
              <StatusPill
                label={wifi.info.isConnected ? 'Internet thông suốt' : 'Mất Internet WAN'}
                color={wifi.info.isConnected ? SH.Green : SH.Red}
              />
              <StatusPill label={wifi.info.security} color={SH.Blue} dot={false} />
            </div>
            <p className="text-[11px] text-[#8A9BB8] font-mono-num">
              Router {wifi.info.ipRouter} · Gateway {wifi.info.gateway} · DNS {wifi.info.dns} · {wifi.info.signalDbm} dBm ({wifi.info.signalLabel})
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <StatusPill label={`${onlineUnblockedCount}/${wifi.devices.length} online`} color={SH.Green} />
          <FlatButton
            color={SH.Teal}
            onClick={() => guiLenhWifi({ action: 'scan' }, 'Đã gửi lệnh quét thiết bị WiFi tới Router.')}
          >
            <RefreshCw size={13} />
            <span>Quét thiết bị</span>
          </FlatButton>
        </div>
      </SmartCard>

      {/* 5 Tabs */}
      <div className="shrink-0 flex flex-wrap gap-2">
        <ChoiceChip label="Tổng quan" selected={tab === 'overview'} color={SH.Teal} onClick={() => setTab('overview')} />
        <ChoiceChip
          label={`Thiết bị (${wifi.devices.length})`}
          selected={tab === 'devices'}
          color={SH.Teal}
          onClick={() => setTab('devices')}
        />
        <ChoiceChip label="Lưu lượng" selected={tab === 'stats'} color={SH.Teal} onClick={() => setTab('stats')} />
        <ChoiceChip
          label={
            wifi.unknownDevices.length > 0
              ? `Bảo mật (${wifi.unknownDevices.length} lạ)`
              : 'Bảo mật'
          }
          selected={tab === 'security'}
          color={wifi.unknownDevices.length > 0 ? SH.Red : SH.Teal}
          onClick={() => setTab('security')}
        />
        <ChoiceChip label="Cấu hình Router" selected={tab === 'settings'} color={SH.Teal} onClick={() => setTab('settings')} />
      </div>

      {/* Tab 0: Overview */}
      {tab === 'overview' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5 lg:overflow-y-auto pr-0.5">
          {wifi.unknownDevices.length > 0 && (
            <div className="shrink-0 flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-[#F59E0B]/45 bg-[#F59E0B]/10 px-3.5 py-2 text-xs text-[#FDE68A]">
              <div className="flex items-center gap-2">
                <ShieldAlert size={16} className="text-[#F59E0B] shrink-0" />
                <span>
                  Phát hiện <b>{wifi.unknownDevices.length} thiết bị lạ</b> đang chờ kiểm duyệt truy cập mạng.
                </span>
              </div>
              <FlatButton color={SH.Amber} onClick={() => setTab('security')}>
                Kiểm duyệt ngay
              </FlatButton>
            </div>
          )}

          <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Tải xuống (Download)</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">
                {wifi.info.downloadMbps.toFixed(1)} Mbps
              </div>
              <div className="text-[11px] text-[#8A9BB8]">Gói link {wifi.info.speedMbps} Mbps</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Tải lên (Upload)</div>
              <div className="font-mono-num text-lg font-bold text-[#14B8A6] mt-0.5">
                {wifi.info.uploadMbps.toFixed(1)} Mbps
              </div>
              <div className="text-[11px] text-[#8A9BB8]">Độ trễ ổn định</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Cường độ tín hiệu</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {wifi.info.signalDbm} dBm
              </div>
              <div className="text-[11px] text-[#22C55E]">{wifi.info.signalLabel}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Thời gian chạy Router</div>
              <div className="font-mono-num text-lg font-bold text-[#22C55E] mt-0.5">
                {wifi.info.uptimeDays}d {wifi.info.uptimeHours}h
              </div>
              <div className="text-[11px] text-[#8A9BB8]">
                {wifi.info.isConnected ? 'WAN Đã kết nối' : 'WAN Gián đoạn'}
              </div>
            </SmartCard>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-7 flex flex-col justify-between gap-2 p-3">
              <SectionHeader title="Băng thông 24 giờ gần đây (Mbps)" />
              <div className="flex-1 min-h-0 flex items-center">
                <DualLineAreaChart
                  primary={wifi.usageChart24h}
                  labels={wifi.usageChart24h.map((_, i) => (i % 4 === 0 ? `${String(i).padStart(2, '0')}h` : ''))}
                  primaryColor={SH.Teal}
                />
              </div>
            </SmartCard>

            <SmartCard className="lg:col-span-5 flex flex-col justify-between gap-2.5 p-3">
              <SectionHeader title="Thông tin mạng & Công cụ nhanh" />
              <div className="grid grid-cols-2 gap-1.5 text-[11px]">
                {[
                  ['SSID', wifi.info.ssid],
                  ['Bảo mật', wifi.info.security],
                  ['IP Router', wifi.info.ipRouter],
                  ['Gateway', wifi.info.gateway],
                  ['DNS', wifi.info.dns],
                  ['Internet', wifi.info.isConnected ? 'Trực tuyến' : 'Mất kết nối'],
                ].map(([lbl, val]) => (
                  <button
                    key={lbl}
                    type="button"
                    onClick={() => copyText(lbl, val)}
                    className="flex items-center justify-between gap-1.5 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-1.5 text-left hover:border-[#14B8A6]/50 cursor-pointer"
                    title="Bấm để sao chép"
                  >
                    <div className="min-w-0">
                      <div className="text-[10px] text-[#8A9BB8]">{lbl}</div>
                      <div className="font-mono-num font-semibold text-[#E6EDF7] truncate">{val}</div>
                    </div>
                    <Copy size={11} className="text-[#64748B] shrink-0" />
                  </button>
                ))}
              </div>

              <div className="flex flex-wrap gap-2 pt-1">
                <FlatButton
                  color={guestPaused ? SH.Amber : SH.Teal}
                  onClick={() => {
                    const next = !guestPaused;
                    setGuestPaused(next);
                    guiLenhWifi(
                      { action: next ? 'pause_guest_wifi' : 'resume_guest_wifi' },
                      next ? 'Đã tạm dừng mạng WiFi khách.' : 'Đã mở lại mạng WiFi khách.',
                    );
                  }}
                >
                  <PauseCircle size={13} />
                  <span>{guestPaused ? 'Mở lại WiFi' : 'Tạm dừng WiFi'}</span>
                </FlatButton>
                <FlatButton
                  color={SH.Blue}
                  onClick={() => guiLenhWifi({ action: 'security_scan' }, 'Đang quét bảo mật toàn mạng LAN…')}
                >
                  <ShieldCheck size={13} />
                  <span>Quét bảo mật</span>
                </FlatButton>
                <FlatButton color={SH.Red} onClick={triggerRouterReboot}>
                  <Power size={13} />
                  <span>Reboot Router</span>
                </FlatButton>
              </div>
            </SmartCard>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-7 p-3 space-y-2">
              <SectionHeader
                title="Thiết bị đang kết nối nổi bật"
                action={
                  <button
                    type="button"
                    onClick={() => setTab('devices')}
                    className="text-xs font-semibold text-[#14B8A6] hover:underline cursor-pointer"
                  >
                    Xem tất cả ({wifi.devices.length}) →
                  </button>
                }
              />
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                {wifi.devices.slice(0, 5).map((d) => (
                  <div
                    key={d.id}
                    className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 py-1.5 text-xs"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <IconBadge color={d.isOnline ? SH.Teal : SH.TextDim} active={d.isOnline} size={28}>
                        <WifiDevIcon icon={d.icon} size={14} />
                      </IconBadge>
                      <div className="min-w-0">
                        <div className="font-semibold text-[#E6EDF7] truncate">{d.name}</div>
                        <div className="font-mono-num text-[10px] text-[#8A9BB8] truncate">{d.ip} · {d.band}</div>
                      </div>
                    </div>
                    <Dot color={d.isOnline && !d.blocked ? SH.Green : SH.TextDim} />
                  </div>
                ))}
              </div>
            </SmartCard>

            <SmartCard className="lg:col-span-5 p-3 space-y-2">
              <SectionHeader title="Nhiễu kênh sóng 5GHz" />
              <div className="space-y-2">
                {wifi.channelNoise.map((ch) => (
                  <ThinBar
                    key={ch.channel}
                    label={`Kênh ${ch.channel}`}
                    valueText={`${ch.noise}% nhiễu`}
                    fraction={ch.noise / 100}
                    color={ch.noise >= 50 ? SH.Amber : SH.Green}
                  />
                ))}
              </div>
            </SmartCard>
          </div>
        </div>
      )}

      {/* Tab 1: Devices */}
      {tab === 'devices' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          <div className="shrink-0 grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Tổng thiết bị</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7]">{wifi.devices.length}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Đang Online</div>
              <div className="font-mono-num text-lg font-bold text-[#22C55E]">{onlineUnblockedCount}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Offline</div>
              <div className="font-mono-num text-lg font-bold text-[#8A9BB8]">{offlineCount}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Thiết bị mới</div>
              <div className="font-mono-num text-lg font-bold text-[#F59E0B]">{newCount}</div>
            </SmartCard>
          </div>

          <div className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="relative flex-1 max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8A9BB8]" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm tên thiết bị, hãng, IP, MAC…"
                className="w-full rounded-xl border border-[#253459] bg-[#0E1627] pl-9 pr-3 py-1.5 text-xs text-[#E6EDF7] outline-none focus:border-[#14B8A6]"
              />
            </div>
            <div className="flex flex-wrap gap-1.5">
              <ChoiceChip label="Tất cả" selected={devFilter === 'all'} color={SH.Teal} onClick={() => setDevFilter('all')} />
              <ChoiceChip label="Online" selected={devFilter === 'online'} color={SH.Green} onClick={() => setDevFilter('online')} />
              <ChoiceChip label="Offline" selected={devFilter === 'offline'} color={SH.TextSecondary} onClick={() => setDevFilter('offline')} />
              <ChoiceChip label="Mới" selected={devFilter === 'new'} color={SH.Amber} onClick={() => setDevFilter('new')} />
              <ChoiceChip label="Đã chặn" selected={devFilter === 'blocked'} color={SH.Red} onClick={() => setDevFilter('blocked')} />
            </div>
          </div>

          <div className="flex-1 min-h-0 overflow-y-auto grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-2.5 pr-1 content-start">
            {filteredDevices.map((d) => (
              <SmartCard key={d.id} className="space-y-2 p-2.5">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2.5 min-w-0">
                    <IconBadge color={d.isOnline ? SH.Teal : SH.TextDim} active={d.isOnline} size={36}>
                      <WifiDevIcon icon={d.icon} size={17} />
                    </IconBadge>
                    <div className="min-w-0">
                      <div className="flex items-center gap-1.5">
                        <span className="text-xs font-bold text-[#E6EDF7] truncate">{d.name}</span>
                        {d.isNew && <StatusPill label="Mới" color={SH.Amber} dot={false} />}
                      </div>
                      {(d.brand || d.type) && (
                        <div className="text-[11px] text-[#94A3B8] truncate">
                          {[d.brand, d.type].filter(Boolean).join(' · ')}
                        </div>
                      )}
                      <div className="text-[11px] text-[#8A9BB8] font-mono-num truncate">
                        {d.ip} · {d.mac} · {d.band} {d.signal ? `(${d.signal})` : ''}
                      </div>
                    </div>
                  </div>
                  <StatusPill
                    label={d.blocked ? 'Đã chặn' : d.isOnline ? 'Online' : 'Offline'}
                    color={d.blocked ? SH.Red : d.isOnline ? SH.Green : SH.TextDim}
                  />
                </div>

                <div className="flex items-center justify-between pt-2 border-t border-[#253459] text-[11px]">
                  <span className="text-[#8A9BB8] font-mono-num">
                    ↓ {d.downloadMB.toFixed(1)} MB · ↑ {d.uploadMB.toFixed(1)} MB
                  </span>
                  <div className="flex items-center gap-1.5">
                    <FlatButton
                      color={d.trusted ? SH.Green : SH.TextSecondary}
                      onClick={() => toggleWifiTrusted(d.id)}
                    >
                      <Check size={12} />
                      <span>{d.trusted ? 'Tin cậy' : 'Tin cậy?'}</span>
                    </FlatButton>
                    <FlatButton
                      color={d.blocked ? SH.Amber : SH.Red}
                      onClick={() => toggleWifiBlocked(d.id)}
                    >
                      <span>{d.blocked ? 'Bỏ chặn' : 'Chặn'}</span>
                    </FlatButton>
                  </div>
                </div>
              </SmartCard>
            ))}
          </div>
        </div>
      )}

      {/* Tab 2: Stats */}
      {tab === 'stats' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5 lg:overflow-y-auto pr-0.5">
          <div className="shrink-0 flex gap-2">
            <ChoiceChip label="24 giờ" selected={statsPeriod === 'day'} color={SH.Blue} onClick={() => setStatsPeriod('day')} />
            <ChoiceChip label="7 ngày" selected={statsPeriod === 'week'} color={SH.Blue} onClick={() => setStatsPeriod('week')} />
            <ChoiceChip label="30 ngày" selected={statsPeriod === 'month'} color={SH.Blue} onClick={() => setStatsPeriod('month')} />
          </div>

          <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">{activeStats.downLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">{activeStats.downVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.downSub}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">{activeStats.upLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#14B8A6] mt-0.5">{activeStats.upVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.upSub}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">{activeStats.totalLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">{activeStats.totalVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.totalSub}</div>
            </SmartCard>
            <SmartCard className="p-2.5">
              <div className="text-xs text-[#8A9BB8]">Tốc độ đỉnh</div>
              <div className="font-mono-num text-lg font-bold text-[#A78BFA] mt-0.5">{activeStats.peakVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.peakSub}</div>
            </SmartCard>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-7 flex flex-col justify-between gap-2 p-3">
              <SectionHeader title={activeStats.title} subtitle="Xanh dương: Tải xuống · Ngọc: Tải lên" />
              <div className="flex-1 min-h-0 flex items-center">
                <DualLineAreaChart
                  labels={activeStats.labels}
                  primary={activeStats.down}
                  secondary={activeStats.up}
                />
              </div>
            </SmartCard>

            <div className="lg:col-span-5 flex flex-col gap-2.5">
              <SmartCard className="p-3 space-y-2">
                <SectionHeader title="Top 5 thiết bị dùng băng thông" />
                <div className="space-y-1.5">
                  {topBandwidthDevices.map((d, idx) => {
                    const sumMb = d.downloadMB + d.uploadMB;
                    return (
                      <div
                        key={d.id}
                        className="flex items-center justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 text-xs"
                      >
                        <div className="flex items-center gap-2 min-w-0">
                          <span className="font-mono-num text-[11px] font-bold text-[#4F8BFF]">#{idx + 1}</span>
                          <span className="font-semibold text-[#E6EDF7] truncate">{d.name}</span>
                        </div>
                        <span className="font-mono-num font-bold text-[#14B8A6] shrink-0">
                          {sumMb >= 1024 ? `${(sumMb / 1024).toFixed(2)} GB` : `${sumMb.toFixed(0)} MB`}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </SmartCard>

              <SmartCard className="p-3 space-y-2">
                <SectionHeader title="Phân bổ theo nhóm dịch vụ" />
                <div className="space-y-2">
                  {CATEGORY_META.map((cat, idx) => {
                    const pct = activeStats.categories[idx] ?? 0;
                    return (
                      <ThinBar
                        key={cat.label}
                        label={cat.label}
                        valueText={`${pct}%`}
                        fraction={pct / 100}
                        color={cat.color}
                      />
                    );
                  })}
                </div>
              </SmartCard>
            </div>
          </div>
        </div>
      )}

      {/* Tab 3: Security */}
      {tab === 'security' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5 lg:overflow-hidden">
          <SmartCard
            className="shrink-0 flex flex-wrap items-center justify-between gap-2 p-2.5"
            style={{
              borderColor: wifi.unknownDevices.length > 0 ? tint(SH.Amber, 0.5) : tint(SH.Green, 0.4),
            }}
          >
            <div className="flex items-center gap-2.5">
              <IconBadge color={wifi.unknownDevices.length > 0 ? SH.Amber : SH.Green} size={32}>
                {wifi.unknownDevices.length > 0 ? <ShieldAlert size={16} /> : <ShieldCheck size={16} />}
              </IconBadge>
              <div>
                <div className="text-xs font-bold text-[#E6EDF7]">
                  {wifi.unknownDevices.length > 0
                    ? `Cảnh báo: Có ${wifi.unknownDevices.length} thiết bị lạ chưa xác minh`
                    : 'Lá chắn bảo mật WPA3 & Tường lửa đang hoạt động ổn định'}
                </div>
                <div className="text-[11px] text-[#8A9BB8]">
                  {trustedDevices.length} thiết bị tin cậy · Tự động chặn ARP Spoofing & DNS Hijack
                </div>
              </div>
            </div>
            <FlatButton
              color={SH.Teal}
              onClick={() => guiLenhWifi({ action: 'security_scan' }, 'Đã kích hoạt quét bảo mật toàn mạng.')}
            >
              <RefreshCw size={13} />
              <span>Quét lỗ hổng</span>
            </FlatButton>
          </SmartCard>

          <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5">
            <div className="flex flex-col min-h-0 gap-2.5">
              <SmartCard className="flex-1 min-h-0 flex flex-col gap-2 p-3">
                <SectionHeader title="Lá chắn bảo mật mạng" />
                <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
                  {wifi.securitySettings.map((s) => (
                    <DeviceSwitchRow
                      key={s.key}
                      icon={<Shield size={16} />}
                      label={s.title}
                      subtitle={s.desc}
                      color={SH.Teal}
                      checked={s.enabled}
                      onToggle={() => toggleWifiSecurity(s.key)}
                    />
                  ))}
                </div>
              </SmartCard>

              <SmartCard className="shrink-0 max-h-[190px] flex flex-col gap-2 p-3">
                <SectionHeader title={`Thiết bị tin cậy (${trustedDevices.length})`} />
                <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 pr-1">
                  {trustedDevices.length === 0 ? (
                    <div className="text-xs text-[#8A9BB8] py-2">Chưa đánh dấu thiết bị tin cậy nào.</div>
                  ) : (
                    trustedDevices.map((d) => (
                      <div
                        key={d.id}
                        className="flex items-center justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 text-xs"
                      >
                        <div className="min-w-0">
                          <div className="font-semibold text-[#E6EDF7] truncate">{d.name}</div>
                          <div className="font-mono-num text-[10px] text-[#8A9BB8]">{d.mac}</div>
                        </div>
                        <button
                          type="button"
                          onClick={() => toggleWifiTrusted(d.id)}
                          className="text-[11px] font-semibold text-[#FCA5A5] hover:underline shrink-0 cursor-pointer"
                        >
                          Gỡ tin cậy
                        </button>
                      </div>
                    ))
                  )}
                </div>
              </SmartCard>
            </div>

            <div className="flex flex-col min-h-0 gap-2.5">
              {wifi.unknownDevices.length > 0 && (
                <SmartCard className="shrink-0 space-y-2 p-3">
                  <SectionHeader title="Thiết bị lạ chờ duyệt" />
                  {wifi.unknownDevices.map((u) => (
                    <div
                      key={u.id}
                      className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 rounded-xl border border-[#F59E0B]/40 bg-[#F59E0B]/10 p-2.5 text-xs"
                    >
                      <div>
                        <div className="font-bold text-[#E6EDF7]">{u.name}</div>
                        <div className="text-[11px] text-[#8A9BB8] font-mono-num">
                          {u.ip} · {u.mac} · {u.time}
                        </div>
                        <div className="text-[11px] text-[#FCD34D]">{u.reason}</div>
                      </div>
                      <div className="flex gap-2 shrink-0">
                        <FlatButton color={SH.Green} onClick={() => guiLenhWifi({ action: 'allow_mac', mac: u.mac }, `Đã cho phép ${u.mac}`)}>
                          Cho phép
                        </FlatButton>
                        <FlatButton color={SH.Red} onClick={() => guiLenhWifi({ action: 'block_mac', mac: u.mac }, `Đã chặn ${u.mac}`)}>
                          Chặn
                        </FlatButton>
                      </div>
                    </div>
                  ))}
                </SmartCard>
              )}

              <SmartCard className="flex-1 min-h-0 flex flex-col gap-2 p-3">
                <SectionHeader title="Nhật ký kết nối" />
                <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 pr-1">
                  {wifi.connectionLogs.map((log, i) => {
                    const logColor =
                      log.type === 'success'
                        ? SH.Green
                        : log.type === 'warning'
                          ? SH.Amber
                          : log.type === 'danger'
                            ? SH.Red
                            : SH.Blue;
                    return (
                      <div
                        key={i}
                        className="flex items-start justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 text-xs"
                      >
                        <div className="flex items-start gap-2 min-w-0">
                          <span className="mt-1 shrink-0">
                            <Dot color={logColor} size={6} />
                          </span>
                          <div className="min-w-0">
                            <div className="font-semibold text-[#E6EDF7] truncate">{log.device}</div>
                            <div className="text-[11px]" style={{ color: logColor }}>
                              {log.action}
                            </div>
                          </div>
                        </div>
                        <span className="font-mono-num text-[11px] text-[#8A9BB8] shrink-0">
                          {log.time} · {log.date}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </SmartCard>
            </div>
          </div>
        </div>
      )}

      {/* Tab 4: Router Settings */}
      {tab === 'settings' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5 items-start lg:overflow-y-auto pr-0.5">
          {/* Col 1: Router Info + WiFi/LAN */}
          <div className="lg:col-span-6 space-y-2.5">
            <SmartCard className="space-y-2.5 p-3">
              <SectionHeader title="Phần cứng Router & WAN/LAN" />
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2">
                  <div className="text-[10px] text-[#8A9BB8]">Model / Phần cứng</div>
                  <div className="font-semibold text-[#E6EDF7]">AX3000 Wi-Fi 6 · v2.0</div>
                </div>
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2">
                  <div className="text-[10px] text-[#8A9BB8]">Uptime hệ thống</div>
                  <div className="font-mono-num font-semibold text-[#22C55E]">
                    {wifi.info.uptimeDays} ngày {wifi.info.uptimeHours} giờ
                  </div>
                </div>
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2">
                  <div className="text-[10px] text-[#8A9BB8]">LAN / DHCP Router</div>
                  <div className="font-mono-num font-semibold text-[#E6EDF7]">{wifi.info.ipRouter}</div>
                </div>
                <div className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2">
                  <div className="text-[10px] text-[#8A9BB8]">WAN Gateway</div>
                  <div className="font-mono-num font-semibold text-[#E6EDF7]">{wifi.info.gateway}</div>
                </div>
              </div>
            </SmartCard>

            <SmartCard className="space-y-3 p-3">
              <SectionHeader title="Đổi tên WiFi & Mật khẩu" subtitle="Gửi qua topic MQTT wifi/cmd" />
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Tên WiFi (SSID)</label>
                <input
                  type="text"
                  value={ssidInput}
                  onChange={(e) => setSsidInput(e.target.value)}
                  className="w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#14B8A6]"
                />
              </div>
              <div>
                <label className="block text-xs text-[#8A9BB8] mb-1">Mật khẩu mới (tối thiểu 8 ký tự)</label>
                <div className="flex gap-2">
                  <input
                    type={showPass ? 'text' : 'password'}
                    value={wifiPass}
                    onChange={(e) => setWifiPass(e.target.value)}
                    placeholder="Để trống nếu giữ mật khẩu cũ"
                    className="w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#14B8A6]"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPass((s) => !s)}
                    className="rounded-xl border border-[#253459] bg-[#0E1627] px-3 text-[#8A9BB8] hover:text-white cursor-pointer"
                  >
                    {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
                  </button>
                </div>
              </div>
              <FlatButton
                color={SH.Teal}
                variant="solid"
                onClick={() =>
                  requireAdminPin('Đổi tên WiFi / mật khẩu Router', () => {
                    guiLenhWifi(
                      { action: 'set_wifi', ssid: ssidInput, password: wifiPass || undefined },
                      'Đã gửi lệnh cập nhật cấu hình WiFi.',
                    );
                  })
                }
              >
                <Wifi size={14} />
                <span>Áp dụng cấu hình WiFi</span>
              </FlatButton>
            </SmartCard>
          </div>

          {/* Col 2: DNS, Channel Scan & System Tools */}
          <div className="lg:col-span-6 space-y-2.5">
            <SmartCard className="space-y-3 p-3">
              <SectionHeader title="Máy chủ DNS & Tối ưu kênh 5GHz" />
              <div className="flex flex-wrap gap-2">
                {(
                  [
                    ['isp', 'ISP Mặc định'],
                    ['1.1.1.1', 'Cloudflare (1.1.1.1)'],
                    ['8.8.8.8', 'Google (8.8.8.8)'],
                    ['custom', 'Tùy chỉnh'],
                  ] as const
                ).map(([mode, lbl]) => (
                  <ChoiceChip
                    key={mode}
                    label={lbl}
                    selected={dnsMode === mode}
                    color={SH.Blue}
                    onClick={() => {
                      setDnsMode(mode);
                      if (mode !== 'custom') {
                        const targetDns = mode === 'isp' ? 'auto' : mode;
                        requireAdminPin(`Đổi DNS Router thành ${lbl}`, () => {
                          guiLenhWifi({ action: 'set_dns', dns: targetDns }, `Đã gửi lệnh đổi DNS (${lbl}).`);
                        });
                      }
                    }}
                  />
                ))}
              </div>
              {dnsMode === 'custom' && (
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={customDns}
                    onChange={(e) => setCustomDns(e.target.value)}
                    placeholder="vd: 9.9.9.9"
                    className="flex-1 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] font-mono-num outline-none focus:border-[#4F8BFF]"
                  />
                  <FlatButton
                    color={SH.Blue}
                    onClick={() =>
                      requireAdminPin(`Đổi DNS tùy chỉnh ${customDns}`, () => {
                        guiLenhWifi({ action: 'set_dns', dns: customDns.trim() }, `Đã đặt DNS ${customDns.trim()}`);
                      })
                    }
                  >
                    Lưu DNS
                  </FlatButton>
                </div>
              )}

              <div className="space-y-2 pt-1">
                <DeviceSwitchRow
                  icon={<Wifi size={15} />}
                  label="Tự động chọn kênh 5GHz ít nhiễu"
                  subtitle="Quét DFS & chuyển sang kênh sạch mỗi đêm"
                  color={SH.Teal}
                  checked={autoChannel5G}
                  onToggle={() => setAutoChannel5G((v) => !v)}
                />
                <DeviceSwitchRow
                  icon={<Globe size={15} />}
                  label="Giám sát chất lượng WAN liên tục"
                  subtitle="Cảnh báo khi mất kết nối quang > 5 phút"
                  color={SH.Blue}
                  checked={pingMonitor}
                  onToggle={() => setPingMonitor((v) => !v)}
                />
              </div>
            </SmartCard>

            <SmartCard className="space-y-3 p-3">
              <SectionHeader title="Công cụ hệ thống Router" />
              <div className="flex flex-wrap gap-2">
                <FlatButton color={SH.Blue} onClick={downloadBackupJson}>
                  <Download size={13} />
                  <span>Sao lưu (.json)</span>
                </FlatButton>
                <FlatButton
                  color={SH.Teal}
                  onClick={() => guiLenhWifi({ action: 'check_firmware' }, 'Router đang kiểm tra bản cập nhật Firmware mới…')}
                >
                  <RefreshCw size={13} />
                  <span>Kiểm tra Firmware</span>
                </FlatButton>
              </div>

              <div className="border-t border-[#253459] pt-2.5 space-y-2">
                <div className="flex items-center gap-2 text-xs text-[#F59E0B]">
                  <ShieldAlert size={15} />
                  <span>Khởi động lại Router (gửi lệnh chuẩn &quot;reboot&quot; tới wifi/cmd, ngắt mạng ~45s).</span>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  {!confirmReboot ? (
                    <FlatButton color={SH.Red} onClick={() => setConfirmReboot(true)}>
                      <Power size={13} />
                      <span>Khởi động lại Router</span>
                    </FlatButton>
                  ) : (
                    <>
                      <FlatButton color={SH.Red} variant="solid" onClick={triggerRouterReboot}>
                        Xác nhận Reboot ngay
                      </FlatButton>
                      <FlatButton color={SH.TextSecondary} onClick={() => setConfirmReboot(false)}>
                        Hủy
                      </FlatButton>
                    </>
                  )}

                  {!confirmReset ? (
                    <FlatButton color={SH.TextSecondary} variant="outline" onClick={() => setConfirmReset(true)}>
                      <RotateCcw size={13} />
                      <span>Khôi phục gốc</span>
                    </FlatButton>
                  ) : (
                    <>
                      <FlatButton
                        color={SH.Red}
                        variant="outline"
                        onClick={() => {
                          requireAdminPin('Khôi phục cài đặt gốc Router', () => {
                            setConfirmReset(false);
                            guiLenhWifi({ action: 'factory_reset' }, 'Đã gửi yêu cầu khôi phục cài đặt gốc.');
                          });
                        }}
                      >
                        Xác nhận Reset gốc
                      </FlatButton>
                      <FlatButton color={SH.TextSecondary} onClick={() => setConfirmReset(false)}>
                        Hủy
                      </FlatButton>
                    </>
                  )}
                </div>
              </div>
            </SmartCard>
          </div>
        </div>
      )}
    </div>
  );
}
