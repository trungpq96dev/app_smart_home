import React from 'react';
import { X } from 'lucide-react';
import { DEST_ITEMS, type DestRoute } from '../data/models.js';
import { SH, tint } from '../theme/colors.js';
import { DestIcon, Dot } from './Common.js';

export function SideNav({
  current,
  onSelect,
  mobileOpen,
  onCloseMobile,
  gatewayOnline,
}: {
  current: DestRoute;
  onSelect: (r: DestRoute) => void;
  mobileOpen: boolean;
  onCloseMobile: () => void;
  gatewayOnline: boolean;
}) {
  const navContent = (
    <div className="flex h-full flex-col bg-[#0E1627] border-r border-[#253459] w-[196px] shrink-0">
      {/* Brand header */}
      <div className="flex items-center justify-between px-3.5 py-3 border-b border-[#253459]">
        <div className="flex items-center gap-2.5">
          <img
            src="/logo_sh.png"
            alt="SmartHome Logo"
            className="h-9 w-9 rounded-xl object-cover border border-[#253459]"
          />
          <div>
            <div className="text-sm font-bold tracking-tight text-[#E6EDF7]">SmartHome</div>
            <div className="flex items-center gap-1.5 text-[11px] text-[#8A9BB8]">
              <Dot color={gatewayOnline ? SH.Green : SH.Red} size={6} />
              <span>{gatewayOnline ? 'Web Gateway' : 'Mất gateway'}</span>
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          className="md:hidden text-[#8A9BB8] hover:text-white cursor-pointer"
        >
          <X size={18} />
        </button>
      </div>

      {/* Navigation items */}
      <nav className="flex-1 overflow-y-auto px-2 py-2.5 space-y-1">
        {DEST_ITEMS.map((item) => {
          const active = current === item.route;
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
                backgroundColor: active ? tint(SH.Blue, 0.18) : 'transparent',
                border: `1px solid ${active ? tint(SH.Blue, 0.35) : 'transparent'}`,
                color: active ? '#FFFFFF' : SH.TextSecondary,
              }}
            >
              <span style={{ color: active ? SH.Blue : SH.TextSecondary }}>
                <DestIcon route={item.route} size={18} />
              </span>
              <span className="truncate">{item.title}</span>
            </button>
          );
        })}
      </nav>

      {/* Footer version */}
      <div className="px-4 py-3 border-t border-[#253459] text-[11px] text-[#5F6F8C]">
        Phiên bản Web 0.8.9
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

