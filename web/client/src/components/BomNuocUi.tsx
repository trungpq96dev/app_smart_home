import React, { useEffect, useState } from 'react';
import { Eye, EyeOff, Save, X } from 'lucide-react';
import {
  nhanKetNoiBom,
  type BomNuocConfigForm,
  type KenhBomNuoc,
  type LuongNuocThang,
  type PublicBomNuocConfig,
  type TrangThaiKetNoiBom,
} from '../data/bomnuoc.js';
import { FlatButton, StatusPill } from './Common.js';
import { SH, tint } from '../theme/colors.js';

/**
 * Sơ đồ hệ thống bơm nước vẽ bằng SVG đáp ứng theo bảng màu SH.
 * Chỉ có 1 phao (GPIO 25) và van một chiều cơ khí (không có van điện từ bản 5.x).
 * Bổ sung hiệu ứng mặt nước gợn sóng hình sin khi có dòng chảy.
 */
export function SoDoBonNuoc({
  levelPercent,
  tankPumpRunning,
  boostPumpRunning,
  cityFlowing,
  tankFull,
}: {
  levelPercent: number | null;
  tankPumpRunning: boolean;
  boostPumpRunning: boolean;
  cityFlowing: boolean;
  tankFull: boolean;
}) {
  const pct = levelPercent !== null ? Math.min(100, Math.max(0, levelPercent)) : null;
  const tankX = 175;
  const tankY = 14;
  const tankW = 152;
  const tankH = 98;
  const waterH = pct !== null && pct > 0.5 ? (tankH - 6) * (pct / 100) : 0;
  const waterY = tankY + tankH - 3 - waterH;
  const anyFlow = tankPumpRunning || boostPumpRunning || cityFlowing;

  const pumpCx = 64;
  const pumpCy = 136;
  const rPump = 20;

  const boostCx = 250;
  const boostCy = 184;
  const rBoost = 17;

  const cityY = 59;
  const valveCx = 99;
  const riserX = tankX + tankW * 0.22;
  const outletX = tankX + tankW * 0.52;

  const waveSpan = 48;
  const buildSineSurfacePath = () => {
    const startX = tankX - waveSpan;
    const endX = tankX + tankW + waveSpan;
    const bottomY = tankY + tankH + 6;
    let d = `M ${startX} ${bottomY} L ${startX} ${waterY}`;
    for (let x = startX; x <= endX; x += waveSpan) {
      d += ` Q ${x + waveSpan * 0.25} ${waterY - 3.2}, ${x + waveSpan * 0.5} ${waterY}`;
      d += ` T ${x + waveSpan} ${waterY}`;
    }
    d += ` L ${endX} ${bottomY} Z`;
    return d;
  };

  const renderPipe = (d: string, active: boolean) => (
    <g>
      <path d={d} fill="none" stroke={SH.Border} strokeWidth={6.5} strokeLinecap="round" strokeLinejoin="round" />
      <path d={d} fill="none" stroke={SH.Card} strokeWidth={3.5} strokeLinecap="round" strokeLinejoin="round" />
      {active && (
        <>
          <path d={d} fill="none" stroke={tint(SH.Cyan, 0.35)} strokeWidth={3.5} strokeLinecap="round" strokeLinejoin="round" />
          <path
            d={d}
            fill="none"
            stroke={SH.Cyan}
            strokeWidth={2.2}
            strokeDasharray="7 12"
            strokeLinecap="round"
            strokeLinejoin="round"
          >
            <animate attributeName="stroke-dashoffset" from="0" to="-38" dur="0.9s" repeatCount="indefinite" />
          </path>
        </>
      )}
    </g>
  );

  const renderPump = (cx: number, cy: number, r: number, running: boolean) => (
    <g>
      <circle
        cx={cx}
        cy={cy}
        r={r}
        fill={running ? SH.Blue : SH.Card2}
        stroke={running ? SH.Cyan : SH.Border}
        strokeWidth={1.6}
      />
      <g>
        {running && (
          <animateTransform
            attributeName="transform"
            type="rotate"
            from={`0 ${cx} ${cy}`}
            to={`360 ${cx} ${cy}`}
            dur="1.1s"
            repeatCount="indefinite"
          />
        )}
        {[0, 120, 240].map((deg) => (
          <line
            key={deg}
            x1={cx}
            y1={cy}
            x2={cx}
            y2={cy - r * 0.62}
            stroke={running ? '#FFFFFF' : SH.TextOff}
            strokeWidth={1.8}
            strokeLinecap="round"
            transform={`rotate(${deg} ${cx} ${cy})`}
          />
        ))}
      </g>
      <circle cx={cx} cy={cy} r={r * 0.18} fill={running ? '#FFFFFF' : SH.TextOff} />
    </g>
  );

  return (
    <svg viewBox="0 0 380 220" className="w-full h-full max-h-[220px] select-none">
      <defs>
        <clipPath id="bom-tank-clip">
          <rect x={tankX + 3} y={tankY + 3} width={tankW - 6} height={tankH - 6} rx={6} />
        </clipPath>
      </defs>

      {/* Thân bồn */}
      <rect x={tankX} y={tankY} width={tankW} height={tankH} rx={8} fill={SH.Card2} />
      {waterH > 0 && (
        <g clipPath="url(#bom-tank-clip)">
          {anyFlow ? (
            <g>
              <path d={buildSineSurfacePath()} fill={tint(SH.Cyan, 0.54)}>
                <animateTransform
                  attributeName="transform"
                  type="translate"
                  from="0 0"
                  to={`${waveSpan} 0`}
                  dur="1.6s"
                  repeatCount="indefinite"
                />
              </path>
              <path
                d={buildSineSurfacePath()}
                fill="none"
                stroke={tint(SH.Cyan, 0.92)}
                strokeWidth={2}
              >
                <animateTransform
                  attributeName="transform"
                  type="translate"
                  from="0 0"
                  to={`${waveSpan} 0`}
                  dur="1.6s"
                  repeatCount="indefinite"
                />
              </path>
            </g>
          ) : (
            <>
              <rect
                x={tankX + 3}
                y={waterY}
                width={tankW - 6}
                height={waterH + 3}
                fill={tint(SH.Cyan, 0.52)}
              />
              <rect
                x={tankX + 3}
                y={waterY}
                width={tankW - 6}
                height={3.5}
                fill={tint(SH.Cyan, 0.85)}
              />
            </>
          )}
        </g>
      )}
      <rect
        x={tankX}
        y={tankY}
        width={tankW}
        height={tankH}
        rx={8}
        fill="none"
        stroke={tankFull ? SH.Green : SH.Border}
        strokeWidth={2}
      />

      {/* Phao duy nhất (GPIO 25) */}
      <circle
        cx={tankX + tankW - 11}
        cy={tankY + tankH * 0.18}
        r={4.2}
        fill={tankFull ? SH.Green : SH.TextOff}
      />

      {/* Chân đế bồn */}
      <line x1={tankX + 10} y1={tankY + tankH} x2={tankX + 10} y2={145} stroke={SH.Border} strokeWidth={2.5} />
      <line x1={tankX + tankW - 10} y1={tankY + tankH} x2={tankX + tankW - 10} y2={145} stroke={SH.Border} strokeWidth={2.5} />

      {/* Đường ống */}
      {renderPipe(
        `M ${pumpCx + rPump} ${pumpCy} L ${riserX} ${pumpCy} L ${riserX} ${tankY + tankH - 3}`,
        tankPumpRunning,
      )}
      {renderPipe(
        `M 16 ${cityY} L ${tankX + tankW * 0.62} ${cityY} L ${tankX + tankW * 0.62} ${tankY + 3}`,
        cityFlowing,
      )}
      {renderPipe(
        `M ${outletX} ${tankY + tankH} L ${outletX} ${boostCy} L ${boostCx - rBoost} ${boostCy}`,
        boostPumpRunning,
      )}
      {renderPipe(
        `M ${boostCx + rBoost} ${boostCy} L 356 ${boostCy}`,
        boostPumpRunning,
      )}

      {/* Van một chiều cơ khí */}
      <g>
        <circle cx={valveCx} cy={cityY} r={8.5} fill={SH.Bg} stroke={cityFlowing ? SH.Cyan : SH.TextOff} strokeWidth={1.4} />
        <polygon
          points={`${valveCx - 3.5},${cityY - 4} ${valveCx + 4.5},${cityY} ${valveCx - 3.5},${cityY + 4}`}
          fill={cityFlowing ? SH.Cyan : SH.TextOff}
        />
      </g>

      {/* Hai máy bơm */}
      {renderPump(pumpCx, pumpCy, rPump, tankPumpRunning)}
      {renderPump(boostCx, boostCy, rBoost, boostPumpRunning)}

      {/* Nhãn không chồng chéo */}
      <text x={pumpCx} y={pumpCy + rPump + 16} textAnchor="middle" fill={SH.TextDim} fontSize="10.5" fontWeight="600">
        Bơm lên bồn
      </text>
      <text x={boostCx + rBoost + 8} y={boostCy - 4} textAnchor="start" fill={SH.TextDim} fontSize="10.5" fontWeight="600">
        Bơm tăng áp
      </text>
      <text x={valveCx} y={cityY - 14} textAnchor="middle" fill={SH.TextDim} fontSize="10" fontWeight="600">
        Van cơ khí
      </text>
      <text x={12} y={cityY + 18} textAnchor="start" fill={SH.TextDim} fontSize="10" fontWeight="600">
        Nước máy
      </text>
    </svg>
  );
}

/** Vòng tròn hiển thị phần trăm mức nước thật. Null -> "--" (không bao giờ đoán). */
export function VongMucNuoc({
  percent,
  size = 96,
  strokeWidth = 9,
}: {
  percent: number | null;
  size?: number;
  strokeWidth?: number;
}) {
  const pct = percent !== null ? Math.min(100, Math.max(0, percent)) : 0;
  const r = (size - strokeWidth) / 2;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;
  const dashOffset = circumference * (1 - pct / 100);

  return (
    <div className="relative inline-flex items-center justify-center shrink-0" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <defs>
          <linearGradient id="bom-ring-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor={SH.Blue} />
            <stop offset="100%" stopColor={SH.Cyan} />
          </linearGradient>
        </defs>
        <circle cx={cx} cy={cy} r={r} fill="none" stroke={SH.Card2} strokeWidth={strokeWidth} />
        {percent !== null && pct > 0 && (
          <circle
            cx={cx}
            cy={cy}
            r={r}
            fill="none"
            stroke="url(#bom-ring-grad)"
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={dashOffset}
            strokeLinecap="round"
            style={{ transition: 'stroke-dashoffset 0.4s ease' }}
          />
        )}
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center px-1">
        {percent === null ? (
          <>
            <span className="font-mono-num text-lg font-bold text-[#64748B]">--</span>
            <span className="text-[9px] text-[#64748B] leading-tight">chưa có cảm biến</span>
          </>
        ) : (
          <span className="font-mono-num text-xl font-bold text-[#F1F5F9]">{Math.trunc(pct)}%</span>
        )}
      </div>
    </div>
  );
}

/** Biểu đồ cột nhóm lượng nước theo tháng (Cyan = bơm lên bồn, Green = nước máy). */
export function BieuDoNuocThang({
  usage,
  maxMonths = 6,
  height = 170,
}: {
  usage: LuongNuocThang[];
  maxMonths?: number;
  height?: number;
}) {
  const data = usage.slice(0, maxMonths).reverse();
  if (data.length === 0) return null;
  const maxVal = Math.max(
    1,
    ...data.map((m) => Math.max(m.bomLit, m.nuocMayLit ?? 0)),
  );
  const hasCity = data.some((m) => m.nuocMayLit !== null);

  return (
    <div className="w-full flex items-end gap-2 pt-2" style={{ height }}>
      {data.map((m, idx) => {
        const tankH = Math.max(4, (m.bomLit / maxVal) * 100);
        const cityH = m.nuocMayLit !== null ? Math.max(4, (m.nuocMayLit / maxVal) * 100) : 0;
        const label =
          m.thang === 'chua ro thang' || !m.thang.trim()
            ? '?'
            : m.thang.includes('-')
              ? `T${Number.parseInt(m.thang.split('-')[1] || '', 10) || m.thang}`
              : m.thang;
        return (
          <div
            key={`${m.thang}-${idx}`}
            className="flex-1 h-full flex flex-col items-center justify-end min-w-0"
            title={`${m.thang || 'Chưa rõ'}: ${Math.round(m.bomLit)} L bơm${m.nuocMayLit !== null ? ` · ${Math.round(m.nuocMayLit)} L nước máy` : ''}`}
          >
            <div className="w-full flex-1 flex items-end justify-center gap-1 min-h-0">
              <div
                className="w-full max-w-[20px] rounded-t transition-all duration-300"
                style={{ height: `${tankH}%`, backgroundColor: SH.Cyan }}
              />
              {hasCity && m.nuocMayLit !== null && (
                <div
                  className="w-full max-w-[20px] rounded-t transition-all duration-300"
                  style={{ height: `${cityH}%`, backgroundColor: SH.Green }}
                />
              )}
            </div>
            <span className="mt-1 text-[10px] font-medium text-[#94A3B8] truncate">{label}</span>
          </div>
        );
      })}
    </div>
  );
}

export function TrangThaiKetNoiBomPill({
  ketNoi,
  kenh,
}: {
  ketNoi: TrangThaiKetNoiBom;
  kenh: KenhBomNuoc;
}) {
  let label = nhanKetNoiBom(ketNoi, kenh);
  let color: string = SH.TextOff;
  let pulse = false;

  switch (ketNoi.state) {
    case 'truc_tiep':
      label = kenh === 'LAN' || ketNoi.kenh === 'LAN' ? 'LAN · 1s' : 'MQTT · 30s';
      color = SH.Green;
      break;
    case 'chua_cau_hinh':
      label = 'Chưa cấu hình bơm';
      color = SH.TextOff;
      break;
    case 'dang_noi':
      label = 'Đang nối bo bơm…';
      color = SH.Amber;
      pulse = true;
      break;
    case 'chi_co_broker':
      label = 'Chưa thấy bo bơm';
      color = SH.Amber;
      break;
    case 'so_cu':
      label = `Số bơm cũ (${ketNoi.tuoiGiay}s)`;
      color = SH.Amber;
      break;
    case 'bo_offline':
      label = 'Bo bơm offline';
      color = SH.Red;
      break;
    case 'mat_broker':
      label = 'Mất broker bơm';
      color = SH.Red;
      break;
    case 'sai_tai_khoan':
      label = 'Sai tài khoản MQTT';
      color = SH.Red;
      break;
    case 'loi':
      label = 'Lỗi nối bo bơm';
      color = SH.Red;
      break;
  }

  return <StatusPill label={label} color={color} pulse={pulse} />;
}

export function NutXacNhanHaiBuoc({
  nhanThuong,
  nhanXacNhan,
  onXacNhan,
  disabled = false,
  title,
}: {
  nhanThuong: string;
  nhanXacNhan: string;
  onXacNhan: () => void;
  disabled?: boolean;
  title?: string;
}) {
  const [armed, setArmed] = useState(false);

  useEffect(() => {
    if (disabled) setArmed(false);
  }, [disabled]);

  useEffect(() => {
    if (!armed) return;
    const t = window.setTimeout(() => setArmed(false), 5000);
    return () => window.clearTimeout(t);
  }, [armed]);

  return (
    <button
      type="button"
      disabled={disabled}
      title={title}
      onClick={() => {
        if (armed) {
          setArmed(false);
          onXacNhan();
        } else {
          setArmed(true);
        }
      }}
      className="inline-flex items-center gap-1 rounded-lg px-2 py-1 text-[11px] font-semibold transition-colors disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
      style={{
        backgroundColor: armed ? tint(SH.Red, 0.18) : 'transparent',
        border: `1px solid ${armed ? tint(SH.Red, 0.5) : 'transparent'}`,
        color: armed ? SH.Red : SH.TextOff,
      }}
    >
      {armed ? nhanXacNhan : nhanThuong}
    </button>
  );
}

export function HopThoaiCauHinhBom({
  open,
  config,
  onClose,
  onSave,
}: {
  open: boolean;
  config: PublicBomNuocConfig;
  onClose: () => void;
  onSave: (form: BomNuocConfigForm) => Promise<boolean>;
}) {
  const [espHost, setEspHost] = useState(config.espHost);
  const [uuTienLan, setUuTienLan] = useState(config.uuTienLan);
  const [brokerHost, setBrokerHost] = useState(config.brokerHost);
  const [brokerPort, setBrokerPort] = useState(String(config.brokerPort || 8883));
  const [brokerTls, setBrokerTls] = useState(config.brokerTls);
  const [brokerUser, setBrokerUser] = useState(config.brokerUser);
  const [brokerPass, setBrokerPass] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [xoaMatKhau, setXoaMatKhau] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setEspHost(config.espHost);
    setUuTienLan(config.uuTienLan);
    setBrokerHost(config.brokerHost);
    setBrokerPort(String(config.brokerPort || 8883));
    setBrokerTls(config.brokerTls);
    setBrokerUser(config.brokerUser);
    setBrokerPass('');
    setShowPass(false);
    setXoaMatKhau(false);
  }, [open, config]);

  if (!open) return null;

  const inputCls =
    'w-full rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 text-xs text-[#E6EDF7] outline-none focus:border-[#4F8BFF] font-mono-num';

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-lg rounded-2xl border border-[#253459] bg-[#16213A] p-4 shadow-2xl space-y-3"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="text-sm font-bold text-[#F1F5F9]">Cấu hình kết nối bo bơm nước</h3>
            <p className="text-xs text-[#94A3B8] mt-0.5">
              Ưu tiên HTTP LAN (1s) khi ở nhà; tự động rơi về EMQX MQTT (30s) khi ra ngoài.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-[#94A3B8] hover:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        <div className="space-y-2.5 text-xs">
          <div>
            <label className="block text-[#94A3B8] mb-1">Địa chỉ bo trong LAN (IP hoặc bomnuoc.local)</label>
            <input
              type="text"
              value={espHost}
              onChange={(e) => setEspHost(e.target.value)}
              placeholder="vd: 192.168.0.x hoặc bomnuoc.local"
              className={inputCls}
            />
          </div>

          <label className="flex items-center justify-between gap-2 rounded-xl border border-[#253459] bg-[#0E1627] px-3 py-2 cursor-pointer">
            <span className="text-[#E6EDF7]">Ưu tiên dùng LAN khi gọi được bo</span>
            <input type="checkbox" checked={uuTienLan} onChange={(e) => setUuTienLan(e.target.checked)} />
          </label>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
            <div className="sm:col-span-2">
              <label className="block text-[#94A3B8] mb-1">Broker MQTT (EMQX / Mosquitto)</label>
              <input
                type="text"
                value={brokerHost}
                onChange={(e) => setBrokerHost(e.target.value)}
                placeholder="vd: abc.emqxsl.com"
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-[#94A3B8] mb-1">Cổng</label>
              <input
                type="number"
                value={brokerPort}
                onChange={(e) => setBrokerPort(e.target.value)}
                placeholder="8883"
                className={inputCls}
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
            <div>
              <label className="block text-[#94A3B8] mb-1">Tài khoản MQTT bơm</label>
              <input
                type="text"
                value={brokerUser}
                onChange={(e) => setBrokerUser(e.target.value)}
                className={inputCls}
              />
            </div>
            <div>
              <label className="block text-[#94A3B8] mb-1">Mật khẩu MQTT bơm</label>
              <div className="flex gap-1.5">
                <input
                  type={showPass ? 'text' : 'password'}
                  value={brokerPass}
                  onChange={(e) => setBrokerPass(e.target.value)}
                  placeholder={config.coMatKhau ? 'Đã lưu — để trống giữ nguyên' : 'Nhập mật khẩu'}
                  className={inputCls}
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  title={showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  className="rounded-xl border border-[#253459] bg-[#0E1627] px-2.5 text-[#94A3B8] hover:text-white cursor-pointer shrink-0"
                >
                  {showPass ? <EyeOff size={15} /> : <Eye size={15} />}
                </button>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap gap-4 text-[#94A3B8]">
            <label className="inline-flex items-center gap-1.5 cursor-pointer">
              <input type="checkbox" checked={brokerTls} onChange={(e) => setBrokerTls(e.target.checked)} />
              <span>Bật TLS (8883)</span>
            </label>
            {config.coMatKhau && (
              <label className="inline-flex items-center gap-1.5 cursor-pointer text-[#FCA5A5]">
                <input type="checkbox" checked={xoaMatKhau} onChange={(e) => setXoaMatKhau(e.target.checked)} />
                <span>Xóa mật khẩu cũ</span>
              </label>
            )}
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t border-[#253459]">
          <FlatButton color={SH.TextSecondary} variant="outline" onClick={onClose}>
            <span>Huỷ</span>
          </FlatButton>
          <FlatButton
            color={SH.Blue}
            variant="solid"
            disabled={saving}
            onClick={async () => {
              setSaving(true);
              const ok = await onSave({
                espHost,
                uuTienLan,
                brokerHost,
                brokerPort: Number(brokerPort) || 8883,
                brokerTls,
                brokerUser,
                brokerPass: brokerPass || undefined,
                xoaMatKhau,
              });
              setSaving(false);
              if (ok) onClose();
            }}
          >
            <Save size={14} />
            <span>{saving ? 'Đang lưu…' : 'Lưu và kết nối'}</span>
          </FlatButton>
        </div>
      </div>
    </div>
  );
}
