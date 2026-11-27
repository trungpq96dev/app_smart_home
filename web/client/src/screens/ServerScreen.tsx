import React, { useEffect, useState } from 'react';
import {
  BatteryCharging,
  ExternalLink,
  HardDrive,
  Network,
  Server,
  Thermometer,
  Zap,
} from 'lucide-react';
import { ArcGauge, ThinBar } from '../components/Charts.js';
import {
  ContainerIcon,
  DemoBadge,
  FlatButton,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function ServerScreen() {
  const { server, config } = useSmartHome();
  const { info, metric, containers, temps, power, days, capNhat, isRealData } = server;
  const [cockpitRemoteUrl, setCockpitRemoteUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    fetch('/api/tunnel/status', { credentials: 'same-origin' })
      .then((r) => (r.ok ? r.json() : null))
      .then((d: { cockpitUrl?: string | null } | null) => {
        if (!cancelled && d?.cockpitUrl) setCockpitRemoteUrl(d.cockpitUrl);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, []);

  const isOnRemoteDomain =
    typeof window !== 'undefined' &&
    !/^(localhost|127\.|192\.168\.|10\.|100\.)/.test(window.location.hostname);
  const lanCockpitHref = `https://${config.mqtt.host || '192.168.0.50'}:9090`;
  const cockpitHref = isOnRemoteDomain && cockpitRemoteUrl ? cockpitRemoteUrl : (cockpitRemoteUrl && isOnRemoteDomain ? cockpitRemoteUrl : isOnRemoteDomain ? null : lanCockpitHref);

  const runningCount = containers.filter((c) => c.state.toLowerCase() === 'running').length;
  const maxDayWh = days.length > 0 ? Math.max(...days.map((d) => d.wh), 0.01) : 1;
  const cpuTemp = metric.cpuTempC;
  const cpuTempColor =
    cpuTemp === null
      ? SH.TextDim
      : cpuTemp >= 75
        ? SH.Red
        : cpuTemp >= 60
          ? SH.Amber
          : SH.Green;

  return (
    <div className="flex flex-col gap-2.5 lg:h-full lg:overflow-hidden">
      {/* Host Header */}
      <SmartCard className="shrink-0 flex flex-col md:flex-row md:items-center justify-between gap-2 p-2.5">
        <div className="flex items-center gap-2.5">
          <IconBadge color={SH.Purple} size={36}>
            <Server size={18} />
          </IconBadge>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <h2 className="text-sm font-bold text-[#E6EDF7]">{info.hostname}</h2>
              {!isRealData ? <DemoBadge /> : <StatusPill label="Dữ liệu thật MQTT" color={SH.Green} />}
            </div>
            <p className="text-[11px] text-[#8A9BB8]">
              {info.os} · Kernel {info.kernel} · {info.cpuModel} ({info.cpuCores} nhân)
            </p>
          </div>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-[11px] text-[#8A9BB8]">
          <span className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1 font-mono-num">
            Uptime: {info.uptimeDays}d {info.uptimeHours}h
          </span>
          {capNhat && (
            <span className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1">
              Cập nhật: {capNhat}
            </span>
          )}
          {(cockpitRemoteUrl || !isOnRemoteDomain) && (
            <FlatButton
              color={SH.Purple}
              onClick={() =>
                window.open(
                  isOnRemoteDomain && cockpitRemoteUrl ? cockpitRemoteUrl : lanCockpitHref,
                  '_blank',
                  'noopener,noreferrer',
                )
              }
            >
              <ExternalLink size={13} />
              <span>Quản trị :9090</span>
            </FlatButton>
          )}
        </div>
      </SmartCard>

      {/* Row 1: 4 Resource KPI Gauges (CPU, RAM, Disk, Nhiệt độ CPU) */}
      <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
        <SmartCard className="flex flex-col items-center justify-between gap-1.5 p-2.5">
          <ArcGauge
            size={112}
            fraction={metric.cpuPercent / 100}
            valueText={`${metric.cpuPercent.toFixed(0)}%`}
            label="Tải CPU"
            color={metric.cpuPercent > 80 ? SH.Red : SH.Blue}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            Load: {metric.load1.toFixed(2)} · {metric.load5.toFixed(2)} · {metric.load15.toFixed(2)}
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col items-center justify-between gap-1.5 p-2.5">
          <ArcGauge
            size={112}
            fraction={metric.ramPercent / 100}
            valueText={`${metric.ramPercent.toFixed(0)}%`}
            label="RAM"
            color={SH.Purple}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            {metric.ramUsedGb.toFixed(1)} / {info.ramTotalGb} GB
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col items-center justify-between gap-1.5 p-2.5">
          <ArcGauge
            size={112}
            fraction={metric.diskPercent / 100}
            valueText={`${metric.diskPercent.toFixed(0)}%`}
            label="Ổ đĩa"
            color={SH.Teal}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            {metric.diskUsedGb} / {info.diskTotalGb} GB
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col items-center justify-between gap-1.5 p-2.5">
          <ArcGauge
            size={112}
            fraction={cpuTemp !== null ? Math.min(1, Math.max(0, cpuTemp / 100)) : 0}
            valueText={cpuTemp !== null ? `${cpuTemp.toFixed(0)}°C` : '—'}
            label="Nhiệt độ"
            color={cpuTempColor}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            {server.tempMinToday !== null && server.tempMaxToday !== null
              ? `Hôm nay: ${server.tempMinToday}°C – ${server.tempMaxToday}°C`
              : cpuTemp !== null && cpuTemp >= 75
                ? 'Nhiệt độ cao (≥75°C)!'
                : 'Cảm biến CPU'}
          </div>
        </SmartCard>
      </div>

      {/* Row 2: Responsive Detail Cards filling remaining viewport */}
      <div
        className={`flex-1 min-h-0 grid grid-cols-1 gap-2.5 ${
          temps.length > 0 ? 'lg:grid-cols-12' : 'lg:grid-cols-3'
        }`}
      >
        {/* Network, RAPL Power & Battery/UPS */}
        <SmartCard
          className={`flex flex-col min-h-0 gap-2 p-3 ${temps.length > 0 ? 'lg:col-span-3' : ''}`}
        >
          <SectionHeader
            title="Mạng, RAPL & Pin"
            action={<Network size={15} className="text-[#4F8BFF]" />}
          />
          <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 text-xs pr-0.5 font-mono-num">
            <div className="flex items-center justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5">
              <span className="font-sans text-[#8A9BB8]">Mạng ↓ / ↑</span>
              <span className="font-bold text-[#E6EDF7]">
                {metric.netRxMbps.toFixed(1)} / {metric.netTxMbps.toFixed(1)} Mbps
              </span>
            </div>

            <div className="rounded-xl bg-[#0E1627] border border-[#253459] p-2.5 space-y-1.5">
              <div className="flex items-center gap-1.5 font-sans text-[11px] font-semibold text-[#94A3B8]">
                <Zap size={12} className="text-[#F59E0B]" />
                <span>Công suất CPU (Intel RAPL)</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">CPU Package</span>
                <span className="font-bold text-[#E6EDF7]">
                  {power.cpuPackageW !== null ? `${power.cpuPackageW.toFixed(2)} W` : '—'}
                </span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">CPU Core</span>
                <span className="text-[#E6EDF7]">
                  {power.cpuCoreW !== null ? `${power.cpuCoreW.toFixed(2)} W` : '—'}
                </span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">Uncore — phần phụ trợ</span>
                <span className="text-[#E6EDF7]">
                  {power.cpuUncoreW !== null ? `${power.cpuUncoreW.toFixed(2)} W` : '—'}
                </span>
              </div>
            </div>

            <div className="rounded-xl bg-[#0E1627] border border-[#253459] p-2.5 space-y-1.5">
              <div className="flex items-center justify-between font-sans text-[11px]">
                <span className="inline-flex items-center gap-1.5 font-semibold text-[#94A3B8]">
                  <BatteryCharging size={13} className="text-[#22C55E]" />
                  <span>Pin / UPS</span>
                </span>
                <span className="font-semibold text-[#22C55E]">{power.batStatus || '—'}</span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">Điện áp</span>
                <span className="font-bold text-[#E6EDF7]">
                  {power.batVoltageV !== null ? `${power.batVoltageV.toFixed(2)} V` : '—'}
                </span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">Dòng điện</span>
                <span className="text-[#E6EDF7]">
                  {power.batCurrentA !== null ? `${power.batCurrentA.toFixed(3)} A` : '—'}
                </span>
              </div>
              <div className="flex justify-between text-[11px]">
                <span className="font-sans text-[#8A9BB8]">Công suất pin</span>
                <span className="text-[#E6EDF7]">
                  {power.batPowerW !== null ? `${power.batPowerW.toFixed(2)} W` : '—'}
                </span>
              </div>
            </div>
          </div>
        </SmartCard>

        {/* Temperature Sensors (hidden when empty) */}
        {temps.length > 0 && (
          <SmartCard className="lg:col-span-3 flex flex-col min-h-0 gap-2 p-3">
            <SectionHeader
              title="Cảm biến nhiệt độ"
              subtitle={
                server.tempMinToday !== null && server.tempMaxToday !== null
                  ? `Hôm nay: ${server.tempMinToday}°C – ${server.tempMaxToday}°C`
                  : undefined
              }
              action={<Thermometer size={15} className="text-[#F59E0B]" />}
            />
            <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
              {temps.map((t, i) => {
                const col = t.tempC >= 75 ? SH.Red : t.tempC >= 55 ? SH.Amber : SH.Green;
                return (
                  <ThinBar
                    key={i}
                    label={t.label}
                    valueText={`${t.tempC.toFixed(0)}°C`}
                    fraction={t.tempC / 95}
                    color={col}
                  />
                );
              })}
            </div>
          </SmartCard>
        )}

        {/* Docker Containers */}
        <SmartCard
          className={`flex flex-col min-h-0 gap-2 p-3 ${temps.length > 0 ? 'lg:col-span-3' : ''}`}
        >
          <SectionHeader
            title="Docker Containers"
            subtitle={`${runningCount}/${containers.length} đang chạy`}
            action={<HardDrive size={15} className="text-[#14B8A6]" />}
          />
          <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
            {containers.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627]/60 py-8 px-3 text-center text-xs text-[#8A9BB8]">
                Chưa có container Docker nào trong gói server/docker.
              </div>
            ) : (
              containers.map((c) => {
                const running = c.state.toLowerCase() === 'running';
                return (
                  <div
                    key={c.name}
                    className="flex items-center justify-between gap-2 rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2"
                  >
                    <div className="flex items-center gap-2 min-w-0">
                      <IconBadge color={running ? SH.Green : SH.Red} size={30}>
                        <ContainerIcon icon={c.icon} size={14} />
                      </IconBadge>
                      <div className="min-w-0">
                        <div className="text-xs font-bold text-[#E6EDF7] truncate">{c.name}</div>
                        <div className="text-[11px] text-[#8A9BB8] truncate">
                          {c.image} {c.status ? `· ${c.status}` : ''}
                        </div>
                      </div>
                    </div>
                    <StatusPill label={c.state} color={running ? SH.Green : SH.Red} />
                  </div>
                );
              })
            )}
          </div>
        </SmartCard>

        {/* Daily Energy Log */}
        <SmartCard
          className={`flex flex-col min-h-0 gap-2 p-3 ${temps.length > 0 ? 'lg:col-span-3' : ''}`}
        >
          <SectionHeader title="Nhật ký điện năng từng ngày" subtitle="Wh tiêu thụ & dải nhiệt CPU" />
          <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
            {days.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627]/60 py-8 px-3 text-center text-xs text-[#8A9BB8]">
                Đang tích lũy số liệu, quay lại sau.
              </div>
            ) : (
              days.map((d) => {
                const barPct = Math.min(100, Math.max(4, (d.wh / maxDayWh) * 100));
                return (
                  <div
                    key={d.date}
                    className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2 space-y-1.5 text-xs"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-mono-num text-[#CBD5E1] font-semibold">{d.date}</span>
                      <div className="flex items-center gap-2.5 font-mono-num">
                        {d.tempMin !== null && d.tempMax !== null && (
                          <span className="text-[11px] text-[#8A9BB8]">
                            {d.tempMin}–{d.tempMax}°C
                          </span>
                        )}
                        <span className="font-bold text-[#4F8BFF]">{d.wh.toFixed(2)} Wh</span>
                      </div>
                    </div>
                    <div className="h-1.5 w-full rounded-full bg-[#1C2946] overflow-hidden">
                      <div
                        className="h-full rounded-full transition-all duration-300"
                        style={{ width: `${barPct}%`, backgroundColor: SH.Blue }}
                      />
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </SmartCard>
      </div>
    </div>
  );
}
