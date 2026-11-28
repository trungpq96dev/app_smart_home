import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowUpDown,
  BatteryCharging,
  Cpu,
  ExternalLink,
  HardDrive,
  Loader2,
  Lock,
  MemoryStick,
  Network,
  Play,
  RefreshCw,
  RotateCw,
  Server,
  ShieldCheck,
  Square,
  Thermometer,
  Unlock,
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
import type { DockerContainerAction, ServerContainer } from '../data/server.js';
import { useSmartHome } from '../store/SmartHomeContext.js';
import { SH } from '../theme/colors.js';

function formatMemMb(mb: number): string {
  if (mb >= 1024) return `${(mb / 1024).toFixed(2)} GB`;
  return `${Math.round(mb)} MB`;
}

export function ServerScreen() {
  const {
    server,
    config,
    adminLockEnabled,
    adminUnlocked,
    requireAdminPin,
    lockAdmin,
    dockerControlStatus,
    dockerBusyName,
    refreshDockerControlStatus,
    refreshDockerContainers,
    controlDockerContainer,
  } = useSmartHome();
  const { info, metric, containers, temps, power, days, capNhat, isRealData } = server;
  const [cockpitRemoteUrl, setCockpitRemoteUrl] = useState<string | null>(null);
  const [sortByLoad, setSortByLoad] = useState(false);
  const [refreshingDocker, setRefreshingDocker] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetch('/api/tunnel/status', { credentials: 'same-origin' })
      .then((r) => (r.ok ? r.json() : null))
      .then((d: { cockpitUrl?: string | null } | null) => {
        if (!cancelled && d?.cockpitUrl) setCockpitRemoteUrl(d.cockpitUrl);
      })
      .catch(() => {});
    void refreshDockerControlStatus();
    return () => {
      cancelled = true;
    };
  }, [refreshDockerControlStatus]);

  const isOnRemoteDomain =
    typeof window !== 'undefined' &&
    !/^(localhost|127\.|192\.168\.|10\.|100\.)/.test(window.location.hostname);
  const lanCockpitHref = `https://${config.mqtt.host || '192.168.0.50'}:9090`;

  const runningCount = containers.filter((c) => c.state.toLowerCase() === 'running').length;
  const totalContainerRamMb = useMemo(
    () =>
      containers.reduce(
        (sum, c) => sum + (c.state.toLowerCase() === 'running' && c.memMb !== null ? c.memMb : 0),
        0,
      ),
    [containers],
  );
  const allowedSet = useMemo(
    () => new Set(dockerControlStatus.allowedContainers.map((n) => n.toLowerCase())),
    [dockerControlStatus.allowedContainers],
  );

  const displayedContainers = useMemo(() => {
    if (!sortByLoad) return containers;
    return [...containers].sort((a, b) => {
      const aRun = a.state.toLowerCase() === 'running' ? 1 : 0;
      const bRun = b.state.toLowerCase() === 'running' ? 1 : 0;
      if (aRun !== bRun) return bRun - aRun;
      const aScore = (a.cpuPercent ?? 0) * 10 + (a.memMb ?? 0) / 10;
      const bScore = (b.cpuPercent ?? 0) * 10 + (b.memMb ?? 0) / 10;
      if (bScore !== aScore) return bScore - aScore;
      return a.name.localeCompare(b.name);
    });
  }, [containers, sortByLoad]);

  const handleContainerAction = (c: ServerContainer, action: DockerContainerAction) => {
    const verb = action === 'start' ? 'Bật' : action === 'stop' ? 'Dừng' : 'Khởi động lại';
    requireAdminPin(`${verb} container ${c.name}`, async () => {
      await controlDockerContainer(c.name, action);
    });
  };

  const handleRefreshDocker = async () => {
    setRefreshingDocker(true);
    try {
      await Promise.all([refreshDockerControlStatus(), refreshDockerContainers()]);
    } finally {
      setRefreshingDocker(false);
    }
  };

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
  const hostRamMb = info.ramTotalGb > 0 ? info.ramTotalGb * 1024 : 16384;

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
      <div className="flex-1 min-h-0 grid grid-cols-1 lg:grid-cols-12 gap-2.5">
        {/* Network, RAPL Power & Battery/UPS */}
        <SmartCard className="flex flex-col min-h-0 gap-2 p-3 lg:col-span-3">
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
          <SmartCard className="lg:col-span-2 flex flex-col min-h-0 gap-2 p-3">
            <SectionHeader
              title="Nhiệt độ"
              subtitle={
                server.tempMinToday !== null && server.tempMaxToday !== null
                  ? `${server.tempMinToday}–${server.tempMaxToday}°C`
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

        {/* Docker Containers (CPU%, RAM MB + Safe Admin Start/Stop/Restart) */}
        <SmartCard
          className={`flex flex-col min-h-0 gap-2 p-3 ${
            temps.length > 0 ? 'lg:col-span-4' : 'lg:col-span-6'
          }`}
        >
          <SectionHeader
            title="Docker Containers"
            subtitle={
              totalContainerRamMb > 0
                ? `${runningCount}/${containers.length} chạy · ${formatMemMb(totalContainerRamMb)} RAM`
                : `${runningCount}/${containers.length} đang chạy`
            }
            action={
              <div className="flex items-center gap-1.5">
                <button
                  type="button"
                  onClick={() => setSortByLoad((v) => !v)}
                  title="Sắp xếp theo mức ăn CPU/RAM nặng nhất"
                  className={`inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-[10.5px] font-semibold transition cursor-pointer ${
                    sortByLoad
                      ? 'border-[#4F8BFF]/60 bg-[#4F8BFF]/15 text-[#93C5FD]'
                      : 'border-[#253459] bg-[#0E1627] text-[#8A9BB8] hover:text-[#E6EDF7]'
                  }`}
                >
                  <ArrowUpDown size={11} />
                  <span>{sortByLoad ? 'Nặng nhất' : 'Theo tên'}</span>
                </button>
                {dockerControlStatus.enabled && adminLockEnabled && (
                  <button
                    type="button"
                    onClick={() => {
                      if (adminUnlocked) {
                        void lockAdmin();
                      } else {
                        requireAdminPin('Mở khóa điều khiển Docker', () => {});
                      }
                    }}
                    title={adminUnlocked ? 'Đang mở khóa PIN kỹ thuật (bấm để khóa lại)' : 'Cần PIN 4 số để bật/tắt container'}
                    className={`inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-[10.5px] font-semibold transition cursor-pointer ${
                      adminUnlocked
                        ? 'border-[#22C55E]/50 bg-[#22C55E]/15 text-[#86EFAC]'
                        : 'border-[#F59E0B]/40 bg-[#F59E0B]/10 text-[#FCD34D]'
                    }`}
                  >
                    {adminUnlocked ? <Unlock size={11} /> : <Lock size={11} />}
                    <span>{adminUnlocked ? 'Mở' : 'PIN'}</span>
                  </button>
                )}
                {dockerControlStatus.enabled && (
                  <button
                    type="button"
                    onClick={() => void handleRefreshDocker()}
                    disabled={refreshingDocker}
                    title="Làm mới danh sách & CPU/RAM container"
                    className="inline-flex items-center justify-center rounded-lg border border-[#253459] bg-[#0E1627] p-1 text-[#8A9BB8] hover:text-[#E6EDF7] disabled:opacity-50 cursor-pointer"
                  >
                    <RefreshCw size={12} className={refreshingDocker ? 'animate-spin' : ''} />
                  </button>
                )}
                <HardDrive size={15} className="text-[#14B8A6] shrink-0" />
              </div>
            }
          />

          {dockerControlStatus.checked && !dockerControlStatus.enabled && (
            <div className="shrink-0 rounded-xl border border-[#253459] bg-[#0E1627]/80 px-2.5 py-1.5 text-[10.5px] text-[#8A9BB8] leading-snug">
              <span className="font-semibold text-[#CBD5E1]">Chế độ chỉ xem:</span>{' '}
              {dockerControlStatus.configured && dockerControlStatus.error
                ? dockerControlStatus.error
                : 'Muốn bật nút Start/Stop/Restart an toàn (không qua MQTT), cấu hình DOCKER_CONTROL_TOKEN và DOCKER_ALLOWED_CONTAINERS trong .env.'}
            </div>
          )}

          <div className="flex-1 min-h-0 overflow-y-auto space-y-2 pr-1">
            {containers.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#253459] bg-[#0E1627]/60 py-8 px-3 text-center text-xs text-[#8A9BB8]">
                Chưa có container Docker nào trong gói server/docker.
              </div>
            ) : (
              displayedContainers.map((c) => {
                const running = c.state.toLowerCase() === 'running';
                const busy = dockerBusyName === c.name;
                const canControl =
                  !c.protected &&
                  dockerControlStatus.enabled &&
                  (c.controllable || allowedSet.has(c.name.toLowerCase()));
                const showLimit =
                  c.memLimitMb !== null && c.memLimitMb > 0 && c.memLimitMb < hostRamMb * 0.9;
                const cpuColor =
                  c.cpuPercent === null
                    ? '#8A9BB8'
                    : c.cpuPercent >= 70
                      ? '#FCA5A5'
                      : c.cpuPercent >= 30
                        ? '#FCD34D'
                        : '#86EFAC';
                const ramColor =
                  c.memMb === null
                    ? '#8A9BB8'
                    : c.memMb >= 512
                      ? '#FCA5A5'
                      : c.memMb >= 200
                        ? '#FCD34D'
                        : '#C4B5FD';

                return (
                  <div
                    key={c.name}
                    className="rounded-xl bg-[#0E1627] border border-[#253459] px-2.5 py-2 space-y-1.5"
                  >
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2 min-w-0">
                        <IconBadge color={running ? SH.Green : SH.Red} size={28}>
                          <ContainerIcon icon={c.icon} size={13} />
                        </IconBadge>
                        <div className="min-w-0">
                          <div className="flex items-center gap-1.5 min-w-0">
                            <span className="text-xs font-bold text-[#E6EDF7] truncate">{c.name}</span>
                            {c.protected && (
                              <span
                                title="Container lõi nằm trong danh sách bảo vệ cứng phía server, không được phép tắt từ xa"
                                className="shrink-0 inline-flex items-center gap-0.5 rounded-full border border-[#F59E0B]/40 bg-[#F59E0B]/12 px-1.5 py-0.5 text-[9.5px] font-semibold text-[#FCD34D]"
                              >
                                <ShieldCheck size={10} />
                                <span>Lõi</span>
                              </span>
                            )}
                          </div>
                          <div className="text-[10.5px] text-[#8A9BB8] truncate">
                            {c.image} {c.status ? `· ${c.status}` : ''}
                          </div>
                        </div>
                      </div>
                      <StatusPill
                        label={running ? 'chạy' : c.state || 'dừng'}
                        color={running ? SH.Green : SH.Red}
                      />
                    </div>

                    {/* Resource chips (CPU% + RAM MB) & Control buttons */}
                    <div className="flex items-center justify-between gap-2 flex-wrap pt-0.5">
                      <div className="flex items-center gap-1.5 font-mono-num text-[10.5px]">
                        {running ? (
                          <>
                            <span
                              className="inline-flex items-center gap-1 rounded-lg border border-[#253459] bg-[#131E36] px-1.5 py-0.5 font-semibold"
                              style={{ color: cpuColor }}
                            >
                              <Cpu size={10} />
                              <span>
                                {c.cpuPercent !== null ? `${c.cpuPercent.toFixed(1)}%` : '—'}
                              </span>
                            </span>
                            <span
                              className="inline-flex items-center gap-1 rounded-lg border border-[#253459] bg-[#131E36] px-1.5 py-0.5 font-semibold"
                              style={{ color: ramColor }}
                            >
                              <MemoryStick size={10} />
                              <span>
                                {c.memMb !== null
                                  ? showLimit && c.memLimitMb !== null
                                    ? `${formatMemMb(c.memMb)} / ${formatMemMb(c.memLimitMb)}`
                                    : formatMemMb(c.memMb)
                                  : '—'}
                              </span>
                            </span>
                          </>
                        ) : (
                          <span className="text-[#5F6F8C] font-sans">Đang dừng · 0% CPU · 0 MB</span>
                        )}
                      </div>

                      {canControl ? (
                        <div className="flex items-center gap-1">
                          {running ? (
                            <>
                              <button
                                type="button"
                                disabled={busy || dockerBusyName !== null}
                                onClick={() => handleContainerAction(c, 'stop')}
                                title={`Dừng container ${c.name} để tiết kiệm CPU/RAM`}
                                className="inline-flex items-center gap-1 rounded-lg border border-[#EF4444]/40 bg-[#EF4444]/15 px-2 py-1 text-[10.5px] font-semibold text-[#FCA5A5] hover:bg-[#EF4444]/25 disabled:opacity-50 transition cursor-pointer"
                              >
                                {busy ? (
                                  <Loader2 size={11} className="animate-spin" />
                                ) : (
                                  <Square size={10} />
                                )}
                                <span>Dừng</span>
                              </button>
                              <button
                                type="button"
                                disabled={busy || dockerBusyName !== null}
                                onClick={() => handleContainerAction(c, 'restart')}
                                title={`Khởi động lại container ${c.name}`}
                                className="inline-flex items-center gap-1 rounded-lg border border-[#4F8BFF]/40 bg-[#4F8BFF]/15 px-2 py-1 text-[10.5px] font-semibold text-[#93C5FD] hover:bg-[#4F8BFF]/25 disabled:opacity-50 transition cursor-pointer"
                              >
                                {busy ? (
                                  <Loader2 size={11} className="animate-spin" />
                                ) : (
                                  <RotateCw size={10} />
                                )}
                                <span>Khởi động lại</span>
                              </button>
                            </>
                          ) : (
                            <button
                              type="button"
                              disabled={busy || dockerBusyName !== null}
                              onClick={() => handleContainerAction(c, 'start')}
                              title={`Bật lại container ${c.name}`}
                              className="inline-flex items-center gap-1 rounded-lg border border-[#22C55E]/40 bg-[#22C55E]/15 px-2.5 py-1 text-[10.5px] font-semibold text-[#86EFAC] hover:bg-[#22C55E]/25 disabled:opacity-50 transition cursor-pointer"
                            >
                              {busy ? (
                                <Loader2 size={11} className="animate-spin" />
                              ) : (
                                <Play size={10} />
                              )}
                              <span>Bật</span>
                            </button>
                          )}
                        </div>
                      ) : !c.protected && dockerControlStatus.enabled ? (
                        <span
                          title="Container chưa nằm trong DOCKER_ALLOWED_CONTAINERS ở ~/server_collector/.env"
                          className="text-[10px] text-[#5F6F8C]"
                        >
                          Chỉ xem
                        </span>
                      ) : null}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </SmartCard>

        {/* Daily Energy Log */}
        <SmartCard className="flex flex-col min-h-0 gap-2 p-3 lg:col-span-3">
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
