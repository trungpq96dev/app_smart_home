import React, { useMemo, useState } from 'react';
import {
  Check,
  RefreshCw,
  Router,
  Search,
  Shield,
  ShieldAlert,
  Wifi,
} from 'lucide-react';
import { DualLineAreaChart, ThinBar } from '../components/Charts.js';
import {
  ChoiceChip,
  DemoBadge,
  FlatButton,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
  WifiDevIcon,
} from '../components/Common.js';
import { DeviceSwitchRow } from '../components/Tiles.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

type WifiTab = 'overview' | 'devices' | 'stats' | 'security' | 'settings';
type StatsPeriod = 'day' | 'week' | 'month';

export function WifiScreen() {
  const {
    wifi,
    guiLenhWifi,
    toggleWifiSecurity,
    toggleWifiTrusted,
    toggleWifiBlocked,
  } = useSmartHome();

  const [tab, setTab] = useState<WifiTab>('overview');
  const [search, setSearch] = useState('');
  const [devFilter, setDevFilter] = useState<'all' | 'online' | 'blocked'>('all');
  const [statsPeriod, setStatsPeriod] = useState<StatsPeriod>('day');
  const [ssidInput, setSsidInput] = useState(wifi.info.ssid);
  const [wifiPass, setWifiPass] = useState('');
  const [selectedDns, setSelectedDns] = useState(wifi.info.dns || '8.8.8.8');
  const [confirmReboot, setConfirmReboot] = useState(false);

  const filteredDevices = useMemo(() => {
    const q = search.trim().toLowerCase();
    return wifi.devices.filter((d) => {
      if (devFilter === 'online' && !d.isOnline) return false;
      if (devFilter === 'blocked' && !d.blocked) return false;
      if (!q) return true;
      return (
        d.name.toLowerCase().includes(q) ||
        d.ip.toLowerCase().includes(q) ||
        d.mac.toLowerCase().includes(q)
      );
    });
  }, [wifi.devices, devFilter, search]);

  const activeStats =
    statsPeriod === 'day' ? wifi.statsDay : statsPeriod === 'week' ? wifi.statsWeek : wifi.statsMonth;

  const onlineCount = wifi.devices.filter((d) => d.isOnline).length;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Router Header */}
      <SmartCard className="shrink-0 flex flex-col lg:flex-row lg:items-center justify-between gap-2 p-2.5">
        <div className="flex items-center gap-2.5">
          <IconBadge color={SH.Teal} size={36}>
            <Router size={18} />
          </IconBadge>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-sm font-bold text-[#E6EDF7]">{wifi.info.ssid}</h2>
              {!wifi.isRealData ? <DemoBadge /> : <StatusPill label="Router thật" color={SH.Green} />}
              <StatusPill label={wifi.info.security} color={SH.Blue} dot={false} />
            </div>
            <p className="text-[11px] text-[#8A9BB8] font-mono-num">
              Router {wifi.info.ipRouter} · DNS {wifi.info.dns} · Tín hiệu {wifi.info.signalDbm} dBm ({wifi.info.signalLabel})
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <StatusPill label={`${onlineCount}/${wifi.devices.length} online`} color={SH.Green} />
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
        {(
          [
            ['overview', 'Tổng quan'],
            ['devices', `Thiết bị (${wifi.devices.length})`],
            ['stats', 'Lưu lượng'],
            ['security', 'Bảo mật'],
            ['settings', 'Cấu hình Router'],
          ] as Array<[WifiTab, string]>
        ).map(([id, label]) => (
          <ChoiceChip key={id} label={label} selected={tab === id} color={SH.Teal} onClick={() => setTab(id)} />
        ))}
      </div>

      {/* Tab 1: Overview */}
      {tab === 'overview' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Tải xuống (Download)</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">
                {wifi.info.downloadMbps.toFixed(1)} Mbps
              </div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Tải lên (Upload)</div>
              <div className="font-mono-num text-lg font-bold text-[#14B8A6] mt-0.5">
                {wifi.info.uploadMbps.toFixed(1)} Mbps
              </div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Gói cước / Link</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">
                {wifi.info.speedMbps} Mbps
              </div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Thời gian chạy Router</div>
              <div className="font-mono-num text-lg font-bold text-[#22C55E] mt-0.5">
                {wifi.info.uptimeDays}d {wifi.info.uptimeHours}h
              </div>
            </SmartCard>
          </div>

          <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
            <SmartCard className="lg:col-span-8 flex flex-col justify-between gap-2 p-3 min-h-0">
              <SectionHeader title="Băng thông 24 giờ gần đây (Mbps)" />
              <div className="flex-1 min-h-0 flex items-center">
                <DualLineAreaChart
                  primary={wifi.usageChart24h}
                  labels={wifi.usageChart24h.map((_, i) => (i % 4 === 0 ? `${String(i).padStart(2, '0')}h` : ''))}
                  primaryColor={SH.Teal}
                />
              </div>
            </SmartCard>

            <SmartCard className="lg:col-span-4 flex flex-col justify-between gap-2 p-3">
              <SectionHeader title="Nhiễu kênh sóng 5GHz" />
              <div className="space-y-2.5 my-auto">
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

      {/* Tab 2: Devices */}
      {tab === 'devices' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          <div className="shrink-0 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="relative flex-1 max-w-sm">
              <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#8A9BB8]" />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm tên thiết bị, IP, MAC…"
                className="w-full rounded-xl border border-[#253459] bg-[#0E1627] pl-9 pr-3 py-1.5 text-xs text-[#E6EDF7] outline-none focus:border-[#14B8A6]"
              />
            </div>
            <div className="flex gap-2">
              <ChoiceChip label="Tất cả" selected={devFilter === 'all'} color={SH.Teal} onClick={() => setDevFilter('all')} />
              <ChoiceChip label="Đang online" selected={devFilter === 'online'} color={SH.Green} onClick={() => setDevFilter('online')} />
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
                      <div className="text-[11px] text-[#8A9BB8] font-mono-num truncate">
                        {d.ip} · {d.mac} · {d.band} ({d.signal})
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
                  <div className="flex items-center gap-2">
                    <FlatButton
                      color={d.trusted ? SH.Green : SH.TextSecondary}
                      onClick={() => toggleWifiTrusted(d.id)}
                    >
                      <Check size={12} />
                      <span>{d.trusted ? 'Tin cậy' : 'Đánh dấu tin cậy'}</span>
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

      {/* Tab 3: Stats */}
      {tab === 'stats' && (
        <div className="flex-1 min-h-0 flex flex-col gap-2.5">
          <div className="shrink-0 flex gap-2">
            <ChoiceChip label="24 giờ" selected={statsPeriod === 'day'} color={SH.Blue} onClick={() => setStatsPeriod('day')} />
            <ChoiceChip label="7 ngày" selected={statsPeriod === 'week'} color={SH.Blue} onClick={() => setStatsPeriod('week')} />
            <ChoiceChip label="30 ngày" selected={statsPeriod === 'month'} color={SH.Blue} onClick={() => setStatsPeriod('month')} />
          </div>

          <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">{activeStats.downLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#4F8BFF] mt-0.5">{activeStats.downVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.downSub}</div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">{activeStats.upLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#14B8A6] mt-0.5">{activeStats.upVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.upSub}</div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">{activeStats.totalLabel}</div>
              <div className="font-mono-num text-lg font-bold text-[#E6EDF7] mt-0.5">{activeStats.totalVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.totalSub}</div>
            </SmartCard>
            <SmartCard>
              <div className="text-xs text-[#8A9BB8]">Tốc độ đỉnh</div>
              <div className="font-mono-num text-lg font-bold text-[#A78BFA] mt-0.5">{activeStats.peakVal}</div>
              <div className="text-[11px] text-[#8A9BB8]">{activeStats.peakSub}</div>
            </SmartCard>
          </div>

          <SmartCard className="flex-1 min-h-0 flex flex-col justify-between gap-2 p-3">
            <SectionHeader title={activeStats.title} subtitle="Xanh dương: Tải xuống · Ngọc: Tải lên" />
            <div className="flex-1 min-h-0 flex items-center">
              <DualLineAreaChart
                labels={activeStats.labels}
                primary={activeStats.down}
                secondary={activeStats.up}
              />
            </div>
          </SmartCard>
        </div>
      )}

      {/* Tab 4: Security */}
      {tab === 'security' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5">
          <SmartCard className="flex flex-col min-h-0 gap-2.5 p-3">
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

          <div className="flex flex-col min-h-0 gap-2.5">
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

            <SmartCard className="flex-1 min-h-0 flex flex-col gap-2 p-3">
              <SectionHeader title="Nhật ký kết nối" />
              <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 pr-1">
                {wifi.connectionLogs.map((log, i) => (
                  <div key={i} className="flex items-start justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 text-xs">
                    <div>
                      <div className="font-semibold text-[#E6EDF7]">{log.device}</div>
                      <div className="text-[11px] text-[#8A9BB8]">{log.action}</div>
                    </div>
                    <span className="font-mono-num text-[11px] text-[#5F6F8C] shrink-0">
                      {log.time} · {log.date}
                    </span>
                  </div>
                ))}
              </div>
            </SmartCard>
          </div>
        </div>
      )}

      {/* Tab 5: Router Settings */}
      {tab === 'settings' && (
        <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-2 gap-2.5 items-start">
          <SmartCard className="space-y-3">
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
              <input
                type="password"
                value={wifiPass}
                onChange={(e) => setWifiPass(e.target.value)}
                placeholder="Để trống nếu giữ mật khẩu cũ"
                className="w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#14B8A6]"
              />
            </div>
            <FlatButton
              color={SH.Teal}
              variant="solid"
              onClick={() =>
                guiLenhWifi(
                  { action: 'set_wifi', ssid: ssidInput, password: wifiPass || undefined },
                  'Đã gửi lệnh cập nhật cấu hình WiFi.',
                )
              }
            >
              <Wifi size={14} />
              <span>Áp dụng cấu hình WiFi</span>
            </FlatButton>
          </SmartCard>

          <SmartCard className="space-y-4">
            <SectionHeader title="DNS & Khởi động lại Router" />
            <div className="space-y-2">
              <div className="text-xs text-[#8A9BB8]">Chọn máy chủ DNS ưu tiên</div>
              <div className="flex flex-wrap gap-2">
                {[
                  ['1.1.1.1', 'Cloudflare (1.1.1.1)'],
                  ['8.8.8.8', 'Google (8.8.8.8)'],
                  ['9.9.9.9', 'Quad9 (9.9.9.9)'],
                ].map(([ip, lbl]) => (
                  <ChoiceChip
                    key={ip}
                    label={lbl}
                    selected={selectedDns === ip}
                    color={SH.Blue}
                    onClick={() => {
                      setSelectedDns(ip);
                      guiLenhWifi({ action: 'set_dns', dns: ip }, `Đã gửi lệnh đổi DNS thành ${ip}`);
                    }}
                  />
                ))}
              </div>
            </div>

            <div className="border-t border-[#253459] pt-3 space-y-2">
              <div className="flex items-center gap-2 text-xs text-[#F59E0B]">
                <ShieldAlert size={15} />
                <span>Khởi động lại sẽ ngắt mạng trong khoảng 45–60 giây.</span>
              </div>
              {!confirmReboot ? (
                <FlatButton color={SH.Red} onClick={() => setConfirmReboot(true)}>
                  Khởi động lại Router
                </FlatButton>
              ) : (
                <div className="flex items-center gap-2">
                  <FlatButton
                    color={SH.Red}
                    variant="solid"
                    onClick={() => {
                      setConfirmReboot(false);
                      guiLenhWifi({ action: 'reboot' }, 'Đã gửi lệnh khởi động lại Router.');
                    }}
                  >
                    Xác nhận Reboot ngay
                  </FlatButton>
                  <FlatButton color={SH.TextSecondary} onClick={() => setConfirmReboot(false)}>
                    Hủy
                  </FlatButton>
                </div>
              )}
            </div>
          </SmartCard>
        </div>
      )}
    </div>
  );
}

