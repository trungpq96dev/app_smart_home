import React from 'react';
import { BellRing, Smile, Video, X } from 'lucide-react';
import {
  CameraPlaybackClient,
  dinhDangGioVn,
  type FaceGreetEvent,
} from '../data/cameraRecorder.js';

export function PopupChaoKhuonMat({
  ev,
  apiPort,
  onMoCamera,
  onDong,
}: {
  ev: FaceGreetEvent;
  apiPort?: number | null;
  onMoCamera: () => void;
  onDong: () => void;
}) {
  const imgUrl = ev.snapshotUrl ? CameraPlaybackClient.urlAnhMat(ev.snapshotUrl, apiPort) : '';
  const gioStr = ev.ts > 0 ? dinhDangGioVn(ev.ts) : '';

  if (ev.isKnown) {
    return (
      <div className="fixed top-4 right-4 z-50 w-[min(92vw,360px)] rounded-2xl border-2 border-[#22C55E] bg-[#131D33]/95 backdrop-blur-md p-3 shadow-2xl">
        <div className="flex items-center gap-2.5">
          <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#22C55E]/20 text-[#22C55E]">
            <Smile size={21} />
          </div>
          <div className="min-w-0 flex-1">
            <div className="text-sm font-bold text-[#E6EDF7] leading-snug">{ev.greeting}</div>
            <div className="text-[11px] text-[#8A9BB8] truncate">
              {ev.camera}
              {gioStr ? ` · ${gioStr}` : ''}
              {ev.confidence > 0 ? ` · ${Math.round(ev.confidence * 100)}%` : ''}
            </div>
          </div>
          <button
            type="button"
            onClick={onDong}
            aria-label="Đóng"
            className="rounded-lg p-1 text-[#8A9BB8] hover:text-[#E6EDF7] cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed top-4 right-4 z-50 w-[min(92vw,340px)] rounded-2xl border-2 border-[#F97316] bg-[#131D33]/95 backdrop-blur-md p-3 shadow-2xl space-y-2.5">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-[#FB923C]">
          <BellRing size={17} />
          <span className="text-sm font-bold">{ev.greeting || 'Có khách ở cửa'}</span>
        </div>
        <button
          type="button"
          onClick={onDong}
          aria-label="Đóng"
          className="rounded-lg p-1 text-[#8A9BB8] hover:text-[#E6EDF7] cursor-pointer"
        >
          <X size={16} />
        </button>
      </div>

      <div className="aspect-[16/10] w-full overflow-hidden rounded-xl border border-[#253459] bg-[#0B1220] flex items-center justify-center">
        {imgUrl ? (
          <img src={imgUrl} alt="Ảnh khách ở cửa" className="h-full w-full object-cover" />
        ) : (
          <div className="flex flex-col items-center gap-1 text-[#5F6F8C] text-xs">
            <Video size={24} />
            <span>Không có ảnh chụp</span>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] text-[#8A9BB8]">
          {ev.camera}
          {gioStr ? ` · ${gioStr}` : ''}
        </span>
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={onDong}
            className="rounded-lg border border-[#253459] bg-[#0E1627] px-2.5 py-1 text-[11px] font-semibold text-[#8A9BB8] hover:text-[#E6EDF7] cursor-pointer"
          >
            Đóng
          </button>
          <button
            type="button"
            onClick={onMoCamera}
            className="rounded-lg bg-[#4F8BFF] px-3 py-1 text-[11px] font-bold text-white hover:bg-[#3B7BFF] cursor-pointer"
          >
            Mở Camera
          </button>
        </div>
      </div>
    </div>
  );
}
