import React from 'react';
import {
  Activity,
  BatteryCharging,
  Cpu,
  HardDrive,
  Network,
  Server,
  Thermometer,
} from 'lucide-react';
import { ArcGauge, ThinBar } from '../components/Charts.js';
import {
  ContainerIcon,
  DemoBadge,
  IconBadge,
  SectionHeader,
  SmartCard,
  StatusPill,
} from '../components/Common.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

export function ServerScreen() {
  const { server } = useSmartHome();
  const { info, metric, containers, temps, power, days, capNhat, isRealData } = server;

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
        </div>
      </SmartCard>

      {/* Row 1: 3 Resource Gauges + Network/Power */}
      <div className="shrink-0 grid grid-cols-2 lg:grid-cols-4 gap-2.5">
        <SmartCard className="flex flex-col items-center justify-between gap-1 p-2.5">
          <ArcGauge
            size={104}
            fraction={metric.cpuPercent / 100}
            valueText={`${metric.cpuPercent.toFixed(0)}%`}
            label="Tải CPU"
            color={metric.cpuPercent > 80 ? SH.Red : SH.Blue}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            Load: {metric.load1.toFixed(2)} · {metric.load5.toFixed(2)} · {metric.load15.toFixed(2)}
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col items-center justify-between gap-1 p-2.5">
          <ArcGauge
            size={104}
            fraction={metric.ramPercent / 100}
            valueText={`${metric.ramPercent.toFixed(0)}%`}
            label="RAM"
            color={SH.Purple}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            {metric.ramUsedGb.toFixed(1)} / {info.ramTotalGb} GB
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col items-center justify-between gap-1 p-2.5">
          <ArcGauge
            size={104}
            fraction={metric.diskPercent / 100}
            valueText={`${metric.diskPercent.toFixed(0)}%`}
            label="Ổ đĩa"
            color={SH.Teal}
          />
          <div className="text-[11px] text-[#8A9BB8] font-mono-num text-center">
            {metric.diskUsedGb} / {info.diskTotalGb} GB
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col justify-between gap-2 p-2.5 text-xs">
          <div className="flex items-center justify-between">
            <span className="font-bold text-[#E6EDF7]">Mạng & Nguồn</span>
            <Network size={14} className="text-[#4F8BFF]" />
          </div>
          <div className="space-y-1 font-mono-num text-[11px]">
            <div className="flex justify-between">
              <span className="text-[#8A9BB8]">Mạng ↓ / ↑</span>
              <span className="text-[#E6EDF7] font-semibold">
                {metric.netRxMbps.toFixed(1)} / {metric.netTxMbps.toFixed(1)} Mbps
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#8A9BB8]">CPU Pkg / Core</span>
              <span className="text-[#E6EDF7]">
                {power.cpuPackageW !== null ? `${power.cpuPackageW.toFixed(1)}W` : '—'} /{' '}
                {power.cpuCoreW !== null ? `${power.cpuCoreW.toFixed(1)}W` : '—'}
              </span>
            </div>
            {(power.batVoltageV !== null || power.batStatus) && (
              <div className="flex justify-between">
                <span className="inline-flex items-center gap-1 text-[#22C55E]">
                  <BatteryCharging size={12} /> {power.batStatus || 'UPS'}
                </span>
                <span className="text-[#E6EDF7] font-bold">
                  {power.batVoltageV !== null ? `${power.batVoltageV.toFixed(2)}V` : '—'}
                </span>
              </div>
            )}
          </div>
        </SmartCard>
      </div>

      {/* Row 2: 3 Equal Scrollable Cards filling remaining viewport */}
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-3 gap-2.5">
        <SmartCard className="flex flex-col min-h-0 gap-2 p-3">
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
              const col = t.tempC >= 70 ? SH.Red : t.tempC >= 55 ? SH.Amber : SH.Green;
              return (
                <ThinBar
                  key={i}
                  label={t.label}
                  valueText={`${t.tempC.toFixed(0)}°C`}
                  fraction={t.tempC / 90}
                  color={col}
                />
              );
            })}
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col min-h-0 gap-2 p-3">
          <SectionHeader
            title={`Docker Containers (${containers.length})`}
            action={<HardDrive size={15} className="text-[#14B8A6]" />}
          />
          <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
            {containers.map((c) => {
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
            })}
          </div>
        </SmartCard>

        <SmartCard className="flex flex-col min-h-0 gap-2 p-3">
          <SectionHeader title="Nhật ký điện năng từng ngày" subtitle="Wh & dải nhiệt CPU" />
          <div className="flex-1 min-h-0 overflow-y-auto space-y-1.5 pr-1">
            {days.map((d) => (
              <div
                key={d.date}
                className="flex items-center justify-between rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-1.5 text-xs"
              >
                <span className="font-mono-num text-[#8A9BB8]">{d.date}</span>
                <div className="flex items-center gap-3 font-mono-num">
                  {d.tempMin !== null && d.tempMax !== null && (
                    <span className="text-[#8A9BB8]">
                      {d.tempMin}–{d.tempMax}°C
                    </span>
                  )}
                  <span className="font-bold text-[#4F8BFF]">{d.wh.toFixed(2)} Wh</span>
                </div>
              </div>
            ))}
          </div>
        </SmartCard>
      </div>

      <Cpu className="hidden" size={1} />
      <Activity className="hidden" size={1} />
    </div>
  );
}

