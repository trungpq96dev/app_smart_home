import React from 'react';
import { AlertCircle, Mic, Volume2, X } from 'lucide-react';
import type { PhienGiongNoi } from '../data/giongnoi/types.js';
import { DEST_ITEMS, NAV_GROUP_TITLES, type DestRoute, type NavGroupId } from '../data/models.js';
import { SH, tint } from '../theme/colors.js';
import { DestIcon } from './Common.js';

function WaveBars({ color }: { color: string }) {
  return (
    <span className="inline-flex items-center gap-[2.5px] h-4" aria-hidden="true">
      <span
        className="w-[3px] h-3.5 rounded-full animate-sh-wave"
        style={{ backgroundColor: color, animationDelay: '0ms' }}
      />
      <span
        className="w-[3px] h-4 rounded-full animate-sh-wave"
        style={{ backgroundColor: color, animationDelay: '160ms' }}
      />
      <span
        className="w-[3px] h-3 rounded-full animate-sh-wave"
        style={{ backgroundColor: color, animationDelay: '320ms' }}
      />
    </span>
  );
}

function SidebarVoiceButton({
  phien,
  onClick,
}: {
  phien: PhienGiongNoi;
  onClick: () => void;
}) {
  const st = phien.trangThai.state;
  const isListening = st === 'dang_nghe';
  const isSpeaking = st === 'dang_tra_loi';
  const isConnecting = st === 'dang_noi';
  const isError = st === 'loi';

  const containerStyle: React.CSSProperties = isError
    ? {
        background: tint(SH.Red, 0.18),
        border: `1.5px solid ${SH.Red}`,
        color: '#FFFFFF',
      }
    : isSpeaking
      ? {
          background: tint(SH.Purple, 0.24),
          border: `1.5px solid ${SH.Purple}`,
          boxShadow: `0 0 18px -4px ${tint(SH.Purple, 0.55)}`,
          color: '#FFFFFF',
        }
      : isListening
        ? {
            background: tint(SH.Green, 0.2),
            border: `1.5px solid ${SH.Green}`,
            boxShadow: `0 0 18px -4px ${tint(SH.Green, 0.5)}`,
            color: '#FFFFFF',
          }
        : isConnecting
          ? {
              background: tint(SH.Amber, 0.18),
              border: `1.5px solid ${tint(SH.Amber, 0.8)}`,
              color: '#FFFFFF',
            }
          : {
              background: `linear-gradient(90deg, #1D4ED8 0%, ${SH.Blue} 100%)`,
              border: '1.2px solid rgba(96, 165, 250, 0.85)',
              boxShadow: `0 6px 18px -6px ${tint(SH.Blue, 0.65)}`,
              color: '#FFFFFF',
            };

  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={
        isSpeaking
          ? 'Ngắt lời trợ lý giọng nói'
          : isListening || isConnecting
            ? 'Tắt trợ lý giọng nói'
            : 'Bật trợ lý giọng nói'
      }
      className={`w-full h-11 rounded-full flex items-center justify-center gap-2 px-3 text-xs font-semibold transition-all active:scale-[0.98] cursor-pointer ${
        isListening || isSpeaking ? 'animate-pulse' : 'hover:brightness-110'
      }`}
      style={containerStyle}
    >
      {isError ? (
        <>
          <AlertCircle size={17} color={SH.Red} className="shrink-0" />
          <span className="truncate">Lỗi trợ lý</span>
        </>
      ) : isSpeaking ? (
        <>
          <Volume2 size={17} color="#D8B4FE" className="shrink-0" />
          <WaveBars color="#D8B4FE" />
          <span className="truncate">Đang trả lời</span>
        </>
      ) : isListening ? (
        <>
          <WaveBars color={SH.Green} />
          <span className="truncate">Đang nghe...</span>
        </>
      ) : isConnecting ? (
        <>
          <Mic size={16} color={SH.Amber} className="shrink-0" />
          <span className="truncate">Đang nối...</span>
        </>
      ) : (
        <>
          <Mic size={17} color="#FFFFFF" className="shrink-0" />
          <span className="truncate">Trợ lý AI</span>
        </>
      )}
    </button>
  );
}

export function SideNav({
  current,
  onSelect,
  mobileOpen,
  onCloseMobile,
  canhBaoMuc,
  phienGiongNoi,
  onBamTroLy,
}: {
  current: DestRoute;
  onSelect: (r: DestRoute) => void;
  mobileOpen: boolean;
  onCloseMobile: () => void;
  canhBaoMuc: Partial<Record<DestRoute, string>>;
  phienGiongNoi: PhienGiongNoi;
  onBamTroLy: () => void;
}) {
  const renderGroup = (group: NavGroupId) => {
    const items = DEST_ITEMS.filter((item) => item.group === group);
    return (
      <div className="space-y-1">
        <div className="px-2.5 pb-0.5 text-[10px] font-semibold uppercase tracking-[0.08em] text-[#64748B]">
          {NAV_GROUP_TITLES[group]}
        </div>
        {items.map((item) => {
          const active = current === item.route;
          const badgeColor = canhBaoMuc[item.route];
          return (
            <button
              key={item.route}
              type="button"
              onClick={() => {
                onSelect(item.route);
                onCloseMobile();
              }}
              className="flex w-full items-center gap-2.5 rounded-xl px-2.5 py-2 text-left text-xs font-semibold transition-all cursor-pointer"
              style={{
                backgroundColor: active ? SH.Blue : 'transparent',
                color: active ? '#FFFFFF' : SH.TextDim,
                boxShadow: active ? `0 4px 14px -4px ${tint(SH.Blue, 0.7)}` : undefined,
              }}
            >
              <span className="shrink-0" style={{ color: active ? '#FFFFFF' : SH.TextDim }}>
                <DestIcon route={item.route} size={17} />
              </span>
              <span className="truncate flex-1">{item.title}</span>
              {badgeColor && (
                <span
                  className="h-2 w-2 rounded-full shrink-0"
                  style={{
                    backgroundColor: badgeColor,
                    boxShadow: active
                      ? '0 0 0 1.5px rgba(255,255,255,0.85)'
                      : `0 0 6px ${tint(badgeColor, 0.75)}`,
                  }}
                />
              )}
            </button>
          );
        })}
      </div>
    );
  };

  const navContent = (
    <div className="flex h-full flex-col bg-[#0E1627] border-r border-[#253459] w-[196px] shrink-0">
      {/* Brand header */}
      <div className="flex items-center justify-between px-3.5 py-3">
        <div className="flex items-center gap-2.5 mx-auto md:mx-0">
          <img
            src="/logo_sh.png"
            alt="SmartHome Logo"
            className="h-9 w-9 rounded-xl object-cover border border-[#253459]"
          />
          <div className="text-sm font-bold tracking-tight text-[#E6EDF7]">SmartHome</div>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          className="md:hidden text-[#8A9BB8] hover:text-white cursor-pointer"
        >
          <X size={18} />
        </button>
      </div>

      {/* Grouped Navigation items */}
      <nav className="flex-1 overflow-y-auto px-2.5 py-1 space-y-2.5">
        {renderGroup('DAILY')}
        <div className="mx-1.5 border-t border-[#253459]/70" />
        {renderGroup('SYSTEM')}
      </nav>

      {/* Fixed bottom Voice AI pill button (replaces duplicate footer clock & floating FAB) */}
      <div className="p-2.5 pt-2">
        <SidebarVoiceButton
          phien={phienGiongNoi}
          onClick={() => {
            onBamTroLy();
            onCloseMobile();
          }}
        />
      </div>
    </div>
  );

  return (
    <>
      {/* Desktop persistent sidebar */}
      <aside className="hidden md:flex h-full shrink-0">{navContent}</aside>

      {/* Mobile slide-over drawer */}
      {mobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <div className="fixed inset-0 bg-black/65 backdrop-blur-xs" onClick={onCloseMobile} />
          <div className="relative z-10 h-full">{navContent}</div>
        </div>
      )}
    </>
  );
}
