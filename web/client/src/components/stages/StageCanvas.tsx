import React, { useEffect, useRef } from 'react';

export interface StageFrameCtx {
  ctx: CanvasRenderingContext2D;
  w: number;
  h: number;
  tSec: number;
  dtSec: number;
}

export function StageCanvas({
  logicalWidth,
  logicalHeight,
  draw,
  canvasStyle,
  fill = false,
  className = '',
  children,
}: {
  logicalWidth: number;
  logicalHeight: number;
  draw: (frame: StageFrameCtx) => void;
  canvasStyle?: React.CSSProperties;
  fill?: boolean;
  className?: string;
  children?: React.ReactNode;
}) {
  const boxRef = useRef<HTMLDivElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const drawRef = useRef(draw);
  drawRef.current = draw;

  useEffect(() => {
    const box = boxRef.current;
    const canvas = canvasRef.current;
    if (!box || !canvas) return;
    let rafId = 0;
    let lastMs = performance.now();
    const startMs = lastMs;

    const resize = () => {
      const rect = box.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      const targetW = Math.max(1, Math.round(rect.width * dpr));
      const targetH = Math.max(1, Math.round(rect.height * dpr));
      if (canvas.width !== targetW || canvas.height !== targetH) {
        canvas.width = targetW;
        canvas.height = targetH;
      }
    };

    resize();
    const ro = new ResizeObserver(resize);
    ro.observe(box);

    const tick = (nowMs: number) => {
      const dtSec = Math.min(0.05, Math.max(0.001, (nowMs - lastMs) / 1000));
      lastMs = nowMs;
      const tSec = (nowMs - startMs) / 1000;

      const ctx = canvas.getContext('2d');
      if (ctx && canvas.width > 0 && canvas.height > 0) {
        const scale = Math.min(canvas.width / logicalWidth, canvas.height / logicalHeight);
        const offsetX = (canvas.width - logicalWidth * scale) / 2;
        const offsetY = (canvas.height - logicalHeight * scale) / 2;

        ctx.save();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.fillStyle = '#0B1220';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.setTransform(scale, 0, 0, scale, offsetX, offsetY);
        ctx.beginPath();
        ctx.rect(0, 0, logicalWidth, logicalHeight);
        ctx.clip();
        drawRef.current({ ctx, w: logicalWidth, h: logicalHeight, tSec, dtSec });
        ctx.restore();
      }
      rafId = window.requestAnimationFrame(tick);
    };

    rafId = window.requestAnimationFrame(tick);
    return () => {
      ro.disconnect();
      window.cancelAnimationFrame(rafId);
    };
  }, [logicalWidth, logicalHeight]);

  return (
    <div
      ref={boxRef}
      className={`relative w-full overflow-hidden rounded-xl border border-[#253459] bg-[#0B1220] select-none ${
        fill ? 'flex-1 min-h-0 h-full' : ''
      } ${className}`}
      style={fill ? undefined : { aspectRatio: `${logicalWidth} / ${logicalHeight}` }}
    >
      <canvas
        ref={canvasRef}
        className={fill ? 'absolute inset-0 block h-full w-full' : 'block h-full w-full'}
        style={canvasStyle}
      />
      {children}
    </div>
  );
}
