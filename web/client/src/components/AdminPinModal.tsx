import React, { useCallback, useEffect, useState } from 'react';
import { Delete, Lock, X } from 'lucide-react';
import { SH, tint } from '../theme/colors.js';

export function AdminPinModal({
  open,
  actionLabel,
  error,
  busy,
  onSubmit,
  onClose,
}: {
  open: boolean;
  actionLabel: string;
  error: string | null;
  busy: boolean;
  onSubmit: (pin: string) => void;
  onClose: () => void;
}) {
  const [pin, setPin] = useState('');

  useEffect(() => {
    if (open) setPin('');
  }, [open]);

  const appendDigit = useCallback(
    (d: string) => {
      if (busy) return;
      setPin((prev) => {
        if (prev.length >= 4) return prev;
        const next = prev + d;
        if (next.length === 4) {
          window.setTimeout(() => onSubmit(next), 60);
        }
        return next;
      });
    },
    [busy, onSubmit],
  );

  const backspace = useCallback(() => {
    if (busy) return;
    setPin((prev) => prev.slice(0, -1));
  }, [busy]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key >= '0' && e.key <= '9') {
        e.preventDefault();
        appendDigit(e.key);
      } else if (e.key === 'Backspace') {
        e.preventDefault();
        backspace();
      } else if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, appendDigit, backspace, onClose]);

  useEffect(() => {
    if (error) setPin('');
  }, [error]);

  if (!open) return null;

  const keys = ['1', '2', '3', '4', '5', '6', '7', '8', '9', 'Xóa', '0', '⌫'];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      onClick={onClose}
    >
      <div
        className="w-full max-w-xs rounded-2xl border border-[#253459] bg-[#16213A] p-5 shadow-2xl space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-center gap-2.5">
            <div
              className="flex h-9 w-9 items-center justify-center rounded-xl shrink-0"
              style={{
                backgroundColor: tint(SH.Amber, 0.16),
                border: `1px solid ${tint(SH.Amber, 0.35)}`,
                color: SH.Amber,
              }}
            >
              <Lock size={17} />
            </div>
            <div>
              <h3 className="text-sm font-bold text-[#E6EDF7]">Khóa PIN Kỹ thuật</h3>
              <p className="text-[11px] text-[#8A9BB8] leading-snug mt-0.5">
                {actionLabel || 'Nhập mã PIN 4 số để tiếp tục'}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-[#8A9BB8] hover:text-white cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* 4 PIN dots */}
        <div className="flex items-center justify-center gap-4 py-1">
          {[0, 1, 2, 3].map((idx) => {
            const filled = idx < pin.length;
            return (
              <div
                key={idx}
                className="h-4 w-4 rounded-full transition-all"
                style={{
                  backgroundColor: filled ? SH.Blue : '#0E1627',
                  border: `2px solid ${filled ? SH.Blue : '#33466E'}`,
                  transform: filled ? 'scale(1.12)' : 'scale(1)',
                }}
              />
            );
          })}
        </div>

        {error && (
          <div className="rounded-xl border border-[#EF4444]/40 bg-[#EF4444]/10 px-3 py-2 text-center text-[11px] text-[#FCA5A5]">
            {error}
          </div>
        )}

        {/* 3x4 keypad */}
        <div className="grid grid-cols-3 gap-2">
          {keys.map((k) => {
            const isClear = k === 'Xóa';
            const isBack = k === '⌫';
            return (
              <button
                key={k}
                type="button"
                disabled={busy}
                onClick={() => {
                  if (isClear) setPin('');
                  else if (isBack) backspace();
                  else appendDigit(k);
                }}
                className="h-12 rounded-xl border border-[#253459] bg-[#0E1627] text-sm font-bold text-[#E6EDF7] hover:bg-[#1D2B4C] active:scale-95 transition-all disabled:opacity-45 cursor-pointer flex items-center justify-center font-mono-num"
              >
                {isBack ? <Delete size={17} className="text-[#8A9BB8]" /> : isClear ? (
                  <span className="text-xs font-semibold text-[#8A9BB8]">Xóa</span>
                ) : k}
              </button>
            );
          })}
        </div>

        <div className="text-center text-[10px] text-[#64748B]">
          Mở khóa có hiệu lực trong 5 phút trên phiên này · Mặc định: 1234
        </div>
      </div>
    </div>
  );
}

