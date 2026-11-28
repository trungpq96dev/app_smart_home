import React from 'react';
import { AlertTriangle, ShieldAlert, X } from 'lucide-react';

export interface ConfirmPromptState {
  title: string;
  message: string;
  confirmLabel: string;
  danger: boolean;
}

export function ConfirmDialog({
  prompt,
  onConfirm,
  onCancel,
}: {
  prompt: ConfirmPromptState | null;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  if (!prompt) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
      onClick={onCancel}
    >
      <div
        className="w-full max-w-sm rounded-2xl border border-[#253459] bg-[#131D33] p-5 shadow-2xl space-y-4"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <div
              className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-xl ${
                prompt.danger
                  ? 'bg-[#EF4444]/15 text-[#FCA5A5]'
                  : 'bg-[#F59E0B]/15 text-[#FCD34D]'
              }`}
            >
              {prompt.danger ? <ShieldAlert size={19} /> : <AlertTriangle size={19} />}
            </div>
            <h3 className="text-sm font-bold text-[#E6EDF7]">{prompt.title}</h3>
          </div>
          <button
            type="button"
            onClick={onCancel}
            aria-label="Đóng"
            className="rounded-lg p-1 text-[#8A9BB8] hover:text-[#E6EDF7] cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        <p className="text-xs text-[#94A3B8] leading-relaxed">{prompt.message}</p>

        <div className="flex items-center justify-end gap-2 pt-1">
          <button
            type="button"
            onClick={onCancel}
            className="rounded-xl border border-[#253459] bg-[#0E1627] px-3.5 py-2 text-xs font-semibold text-[#CBD5E1] hover:text-white cursor-pointer"
          >
            Hủy
          </button>
          <button
            type="button"
            onClick={onConfirm}
            className={`rounded-xl px-4 py-2 text-xs font-bold text-white cursor-pointer ${
              prompt.danger
                ? 'bg-[#EF4444] hover:bg-[#DC2626]'
                : 'bg-[#4F8BFF] hover:bg-[#3B7BFF]'
            }`}
          >
            {prompt.confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
