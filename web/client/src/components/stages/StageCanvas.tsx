import React, { useEffect, useRef } from 'react';

export interface StageFrameCtx {
  ctx: CanvasRenderingContext2D;
  w: number;
  h: number;
  tSec: number;
  dtSec: number;
}

export function hexToRgb(hex: string): [number, number, number] {
  const clean = hex.replace('#', '');
  const full =
    clean.length === 3
      ? clean
          .split('')
          .map((c) => c + c)
          .join('')
      : clean.slice(0, 6);
  const num = Number.parseInt(full, 16) || 0;
  return [(num >> 16) & 255, (num >> 8) & 255, num & 255];
}

export function rgba(hex: string, alpha: number): string {
  const [r, g, b] = hexToRgb(hex);
  const a = Math.min(1, Math.max(0, alpha));
  return `rgba(${r}, ${g}, ${b}, ${a.toFixed(3)})`;
}

export function lerpHex(hexA: string, hexB: string, t: number): string {
  const [r1, g1, b1] = hexToRgb(hexA);
  const [r2, g2, b2] = hexToRgb(hexB);
  const k = Math.min(1, Math.max(0, t));
  const r = Math.round(r1 + (r2 - r1) * k);
  const g = Math.round(g1 + (g2 - g1) * k);
  const b = Math.round(b1 + (b2 - b1) * k);
  return `rgb(${r}, ${g}, ${b})`;
}

export function smoothToward(cur: number, target: number, dtSec: number, durationSec = 0.5): number {
  if (Math.abs(target - cur) < 0.001) return target;
  const speed = 1 / Math.max(0.08, durationSec);
  const step = speed * dtSec * 2.4;
  if (Math.abs(target - cur) <= step) return target;
  return cur + (target - cur) * Math.min(1, dtSec * speed * 4.2);
}

export function tracePoly(ctx: CanvasRenderingContext2D, pts: readonly number[]): void {
  if (pts.length < 4) return;
  ctx.beginPath();
  ctx.moveTo(pts[0], pts[1]);
  for (let i = 2; i + 1 < pts.length; i += 2) {
    ctx.lineTo(pts[i], pts[i + 1]);
  }
  ctx.closePath();
}

export function fillPoly(
  ctx: CanvasRenderingContext2D,
  pts: readonly number[],
  fillStyle: string | CanvasGradient,
  strokeStyle?: string,
  lineWidth = 1,
): void {
  tracePoly(ctx, pts);
  ctx.fillStyle = fillStyle;
  ctx.fill();
  if (strokeStyle) {
    ctx.strokeStyle = strokeStyle;
    ctx.lineWidth = lineWidth;
    ctx.stroke();
  }
}

export function drawWarmGlowOval(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  rx: number,
  ry: number,
  hexColor: string,
  alpha: number,
): void {
  if (alpha <= 0.01 || rx <= 0 || ry <= 0) return;
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(1, ry / rx);
  const grad = ctx.createRadialGradient(0, 0, 0, 0, 0, rx);
  grad.addColorStop(0, rgba(hexColor, Math.min(1, alpha)));
  grad.addColorStop(0.5, rgba(hexColor, Math.min(1, alpha * 0.35)));
  grad.addColorStop(1, rgba(hexColor, 0));
  ctx.fillStyle = grad;
  ctx.beginPath();
  ctx.arc(0, 0, rx, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

export function StageCanvas({
  logicalWidth,
  logicalHeight,
  draw,
  canvasStyle,
  fill = false,
  borderless = false,
  bgColor = '#0B1220',
  className = '',
  children,
}: {
  logicalWidth: number;
  logicalHeight: number;
  draw: (frame: StageFrameCtx) => void;
  canvasStyle?: React.CSSProperties;
  fill?: boolean;
  borderless?: boolean;
  bgColor?: string;
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
      if (typeof document !== 'undefined' && document.hidden) {
        lastMs = nowMs;
        rafId = window.requestAnimationFrame(tick);
        return;
      }
      const dtSec = Math.min(0.05, Math.max(0.001, (nowMs - lastMs) / 1000));
      lastMs = nowMs;
      const tSec = (nowMs - startMs) / 1000;

      const ctx = canvas.getContext('2d');
      if (ctx && canvas.width > 0 && canvas.height > 0) {
        // Stretch sx, sy khop 100% voi Compose Canvas tren Android (size.width / REF_W, size.height / REF_H)
        const sx = canvas.width / logicalWidth;
        const sy = canvas.height / logicalHeight;

        ctx.save();
        ctx.setTransform(1, 0, 0, 1, 0, 0);
        ctx.fillStyle = bgColor;
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        ctx.setTransform(sx, 0, 0, sy, 0, 0);
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
  }, [logicalWidth, logicalHeight, bgColor]);

  return (
    <div
      ref={boxRef}
      className={`relative w-full overflow-hidden select-none ${
        borderless ? '' : 'rounded-2xl border border-[#253459]'
      } ${fill ? 'flex-1 min-h-0 h-full' : ''} ${className}`}
      style={{
        backgroundColor: bgColor,
        ...(fill ? {} : { aspectRatio: `${logicalWidth} / ${logicalHeight}` }),
      }}
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
