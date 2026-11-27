import React, { useEffect, useRef, useState } from 'react';
import {
  dinhDangGioVn,
  isDoanCucBo,
  isSuKienNguoi,
  type DoanVideo,
  type SuKienCamera,
} from '../data/cameraRecorder.js';
import { SH } from '../theme/colors.js';

export type MucZoomTimelineId = 'GIO_24' | 'GIO_6' | 'GIO_1';

export interface MucZoomTimelineInfo {
  id: MucZoomTimelineId;
  nhan: string;
  cuaSoGiay: number;
  buocVachChinhGiay: number;
}

export const MUC_ZOOM_TIMELINE_LIST: MucZoomTimelineInfo[] = [
  { id: 'GIO_24', nhan: '24h', cuaSoGiay: 86_400, buocVachChinhGiay: 14_400 },
  { id: 'GIO_6', nhan: '6h', cuaSoGiay: 21_600, buocVachChinhGiay: 3_600 },
  { id: 'GIO_1', nhan: '1h', cuaSoGiay: 3_600, buocVachChinhGiay: 600 },
];

/**
 * Thanh dòng thời gian 24/24 kiểu Imou / EZVIZ (port 1:1 từ `ThanhThoiGianCamera.kt`):
 *  - Dải xanh lam = có video ghi 24/24 (sáng hơn nếu nằm ở bộ đệm 10 GB, đậm hơn nếu trên Google Drive).
 *  - Vạch cam nổi bật = khoảng phát hiện CÓ NGƯỜI.
 *  - Kéo ngang bằng ngón tay / chuột hoặc chạm vào bất kỳ vị trí nào trên thước để nhảy tới mốc đó.
 */
export function ThanhThoiGianCamera({
  dayStartTs,
  dayEndTs,
  currentTs,
  zoom,
  segments,
  events,
  onChonMoc,
  className = '',
}: {
  dayStartTs: number;
  dayEndTs: number;
  currentTs: number;
  zoom: MucZoomTimelineInfo;
  segments: DoanVideo[];
  events: SuKienCamera[];
  onChonMoc: (epochSec: number) => void;
  className?: string;
}) {
  const boxRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [dangKeo, setDangKeo] = useState(false);
  const [mocKeo, setMocKeo] = useState(currentTs);
  const [widthPx, setWidthPx] = useState(320);

  const dragStateRef = useRef<{
    pointerId: number;
    startX: number;
    lastX: number;
    moved: boolean;
    mocFloat: number;
  } | null>(null);

  const maxValidTs = Math.max(dayStartTs, dayEndTs - 1);
  const tamHienThiTs = Math.min(
    maxValidTs,
    Math.max(dayStartTs, Math.round(dangKeo ? mocKeo : currentTs)),
  );

  useEffect(() => {
    const box = boxRef.current;
    if (!box) return;
    const syncSize = () => {
      const r = box.getBoundingClientRect();
      if (r.width > 1) setWidthPx(r.width);
    };
    syncSize();
    const ro = new ResizeObserver(syncSize);
    ro.observe(box);
    return () => ro.disconnect();
  }, []);

  useEffect(() => {
    const box = boxRef.current;
    const canvas = canvasRef.current;
    if (!box || !canvas) return;
    const rect = box.getBoundingClientRect();
    const w = Math.max(1, rect.width);
    const h = Math.max(1, rect.height);
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const targetW = Math.round(w * dpr);
    const targetH = Math.round(h * dpr);
    if (canvas.width !== targetW || canvas.height !== targetH) {
      canvas.width = targetW;
      canvas.height = targetH;
    }

    const ctx = canvas.getContext('2d');
    if (!ctx || dayEndTs <= dayStartTs) return;

    ctx.save();
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    const nuaCuaSo = zoom.cuaSoGiay / 2;
    const traiTs = tamHienThiTs - nuaCuaSo;
    const phaiTs = tamHienThiTs + nuaCuaSo;
    const pxMoiGiay = w / zoom.cuaSoGiay;
    const tsSangX = (ts: number) => (ts - traiTs) * pxMoiGiay;

    const barTop = h * 0.24;
    const barH = h * 0.44;
    const barBottom = barTop + barH;

    // 1. Nền rãnh video 00:00..24:00 của ngày đang chọn
    const ngayX1 = Math.max(0, tsSangX(dayStartTs));
    const ngayX2 = Math.min(w, tsSangX(dayEndTs));
    if (ngayX2 > ngayX1) {
      ctx.fillStyle = '#192642';
      ctx.beginPath();
      ctx.roundRect(ngayX1, barTop, ngayX2 - ngayX1, barH, 4);
      ctx.fill();
    }

    // 2. Các đoạn video đã ghi 24/24 (xanh lam)
    for (const seg of segments) {
      if (seg.endTs <= traiTs || seg.startTs >= phaiTs) continue;
      const x1 = Math.max(0, tsSangX(seg.startTs));
      const x2 = Math.min(w, tsSangX(seg.endTs));
      if (x2 > x1) {
        ctx.fillStyle = isDoanCucBo(seg)
          ? 'rgba(79, 139, 255, 0.56)'
          : 'rgba(37, 99, 235, 0.44)';
        ctx.fillRect(x1, barTop, Math.max(1.5, x2 - x1), barH);
      }
    }

    // 3. Vạch cam nổi bật tại những lúc PHÁT HIỆN NGƯỜI
    for (const ev of events) {
      if (!isSuKienNguoi(ev)) continue;
      if (ev.endTs < traiTs || ev.startTs > phaiTs) continue;
      const x1 = tsSangX(ev.startTs);
      const x2 = Math.max(x1 + 3, tsSangX(ev.endTs));
      const cx1 = Math.max(0, x1);
      const cx2 = Math.min(w, x2);
      if (cx2 > cx1) {
        ctx.fillStyle = SH.Orange;
        ctx.fillRect(cx1, barTop - 2, cx2 - cx1, barH + 4);
      }
    }

    // 4. Vạch chia giờ & chữ giờ trên thước
    const buocNho = Math.max(60, Math.trunc(zoom.buocVachChinhGiay / 4));
    const mocBatDauVach = Math.max(
      dayStartTs,
      dayStartTs + Math.floor((traiTs - dayStartTs) / buocNho) * buocNho,
    );
    ctx.font = '500 9.5px JetBrains Mono, monospace';
    ctx.textAlign = 'center';

    for (let t = mocBatDauVach; t <= Math.min(dayEndTs, phaiTs + buocNho); t += buocNho) {
      if (t >= dayStartTs && t <= dayEndTs) {
        const x = tsSangX(t);
        if (x >= 0 && x <= w) {
          const laVachChinh = (t - dayStartTs) % zoom.buocVachChinhGiay === 0;
          ctx.strokeStyle = laVachChinh ? 'rgba(138, 155, 184, 0.55)' : '#253459';
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(x, barBottom);
          ctx.lineTo(x, barBottom + (laVachChinh ? 6 : 3));
          ctx.stroke();

          if (laVachChinh && x >= 18 && x <= w - 18) {
            const nhan = t === dayEndTs ? '24:00' : dinhDangGioVn(t).slice(0, 5);
            ctx.fillStyle = '#8A9BB8';
            ctx.fillText(nhan, x, h - 3);
          }
        }
      }
    }

    // 5. Kim đỏ cố định chính giữa thước
    const midX = w * 0.5;
    const needleColor = dangKeo ? SH.Amber : SH.Red;
    ctx.strokeStyle = needleColor;
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(midX, 0);
    ctx.lineTo(midX, h);
    ctx.stroke();

    ctx.fillStyle = needleColor;
    ctx.beginPath();
    ctx.moveTo(midX - 5, 0);
    ctx.lineTo(midX + 5, 0);
    ctx.lineTo(midX, 6);
    ctx.closePath();
    ctx.fill();

    ctx.restore();
  }, [dayStartTs, dayEndTs, tamHienThiTs, zoom, segments, events, dangKeo, widthPx]);

  const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
    if (dayEndTs <= dayStartTs || widthPx <= 1) return;
    e.currentTarget.setPointerCapture(e.pointerId);
    dragStateRef.current = {
      pointerId: e.pointerId,
      startX: e.clientX,
      lastX: e.clientX,
      moved: false,
      mocFloat: currentTs,
    };
    setMocKeo(currentTs);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    const st = dragStateRef.current;
    if (!st || st.pointerId !== e.pointerId || widthPx <= 1) return;
    const dx = e.clientX - st.lastX;
    st.lastX = e.clientX;
    if (!st.moved && Math.abs(e.clientX - st.startX) > 4) {
      st.moved = true;
      setDangKeo(true);
    }
    if (st.moved) {
      const giayMoiPx = zoom.cuaSoGiay / widthPx;
      // Kéo thước sang trái (dx < 0) -> tiến về tương lai giống Imou/EZVIZ
      st.mocFloat = Math.min(
        maxValidTs,
        Math.max(dayStartTs, st.mocFloat - dx * giayMoiPx),
      );
      setMocKeo(st.mocFloat);
    }
  };

  const finishPointer = (e: React.PointerEvent<HTMLDivElement>, cancelled: boolean) => {
    const st = dragStateRef.current;
    if (!st || st.pointerId !== e.pointerId) return;
    dragStateRef.current = null;
    setDangKeo(false);
    if (cancelled || dayEndTs <= dayStartTs || widthPx <= 1) return;

    if (st.moved) {
      const chot = Math.min(maxValidTs, Math.max(dayStartTs, Math.round(st.mocFloat)));
      onChonMoc(chot);
    } else {
      const rect = e.currentTarget.getBoundingClientRect();
      const offsetX = e.clientX - rect.left;
      const giayMoiPx = zoom.cuaSoGiay / Math.max(1, rect.width);
      const lechPx = offsetX - rect.width * 0.5;
      const mocMoi = Math.min(
        maxValidTs,
        Math.max(dayStartTs, Math.round(currentTs + lechPx * giayMoiPx)),
      );
      onChonMoc(mocMoi);
    }
  };

  return (
    <div
      ref={boxRef}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={(e) => finishPointer(e, false)}
      onPointerCancel={(e) => finishPointer(e, true)}
      className={`relative h-[60px] w-full overflow-hidden rounded-xl border border-[#253459] bg-[#131D33] select-none touch-none cursor-grab active:cursor-grabbing ${className}`}
    >
      <canvas ref={canvasRef} className="block h-full w-full" />
      <div
        className="pointer-events-none absolute top-0.5 left-1/2 -translate-x-1/2 rounded px-1.5 py-0.5 text-[10px] font-mono font-semibold"
        style={{
          backgroundColor: 'rgba(11, 18, 32, 0.88)',
          color: dangKeo ? SH.Amber : '#E6EDF7',
        }}
      >
        {dinhDangGioVn(tamHienThiTs)}
      </div>
    </div>
  );
}
