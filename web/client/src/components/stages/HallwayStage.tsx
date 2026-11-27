import React, { useEffect, useRef, useState } from 'react';
import { HALLWAY_VIEWS, type HallwayView, type RoomUi } from '../../data/models.js';
import { SH, tint } from '../../theme/colors.js';
import { drawWarmGlowOval, fillPoly, rgba, smoothToward, StageCanvas } from './StageCanvas.js';

const HW_W = 800;
const HW_H = 500;

function formatHudClock(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function drawViewDauHe(
  ctx: CanvasRenderingContext2D,
  fHall: number,
  hallOn: boolean,
  fFront: number,
  fSurr: number,
  isLocked: boolean,
  motionAuto: boolean,
  motionDetected: boolean,
) {
  const sky = ctx.createLinearGradient(0, 0, 0, 500);
  sky.addColorStop(0, '#060C1C');
  sky.addColorStop(0.6, '#112040');
  sky.addColorStop(1, '#1D355E');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, 800, 500);

  ctx.fillStyle = rgba('#FEF9C3', 0.86);
  ctx.beginPath();
  ctx.arc(728, 70, 13, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#0D1730';
  ctx.beginPath();
  ctx.arc(734, 66, 12, 0, Math.PI * 2);
  ctx.fill();

  // Bụi cây sân vườn 2 bên hông
  ctx.fillStyle = '#133829';
  ctx.beginPath();
  ctx.moveTo(0, 330);
  ctx.quadraticCurveTo(26, 285, 68, 312);
  ctx.lineTo(68, 455);
  ctx.lineTo(0, 455);
  ctx.closePath();
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(800, 330);
  ctx.quadraticCurveTo(774, 285, 732, 312);
  ctx.lineTo(732, 455);
  ctx.lineTo(800, 455);
  ctx.closePath();
  ctx.fill();

  if (fSurr > 0.01) {
    drawWarmGlowOval(ctx, 22, 340, 78, 125, '#FCD34D', 0.62 * fSurr);
    drawWarmGlowOval(ctx, 778, 340, 78, 125, '#FCD34D', 0.62 * fSurr);
    drawWarmGlowOval(ctx, 18, 395, 14, 14, '#FEF08A', 0.85 * fSurr);
    drawWarmGlowOval(ctx, 782, 395, 14, 14, '#FEF08A', 0.85 * fSurr);
  }

  fillPoly(ctx, [0, 0, 800, 0, 762, 28, 38, 28], '#1E293B', '#0F172A', 1.5);

  const porchCeil = ctx.createLinearGradient(0, 28, 0, 112);
  porchCeil.addColorStop(0, '#523724');
  porchCeil.addColorStop(1, '#2C1C12');
  fillPoly(ctx, [28, 28, 772, 28, 655, 112, 145, 112], porchCeil);

  ctx.strokeStyle = rgba('#1B110B', 0.55);
  ctx.lineWidth = 1.2;
  ctx.beginPath();
  for (const [x1, x2] of [
    [120, 215],
    [230, 300],
    [340, 380],
    [460, 420],
    [570, 500],
    [680, 585],
  ]) {
    ctx.moveTo(x1, 28);
    ctx.lineTo(x2, 112);
  }
  ctx.stroke();

  if (fFront > 0.01) {
    ctx.fillStyle = rgba('#FEF08A', 0.85 * fFront);
    ctx.fillRect(44, 25, 712, 4);
    const fg = ctx.createLinearGradient(0, 29, 0, 140);
    fg.addColorStop(0, rgba('#FDE68A', 0.82 * 0.72 * fFront));
    fg.addColorStop(0.55, rgba('#FBBF24', 0.22 * 0.72 * fFront));
    fg.addColorStop(1, 'rgba(251,191,36,0)');
    fillPoly(ctx, [40, 29, 760, 29, 800, 140, 0, 140], fg);
    ctx.fillStyle = rgba('#FFFFFF', fFront);
    for (const [cx, r] of [
      [145, 4.5],
      [400, 5],
      [655, 4.5],
    ]) {
      ctx.beginPath();
      ctx.arc(cx, 27, r, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  const facadeGrad = ctx.createLinearGradient(0, 112, 0, 364);
  facadeGrad.addColorStop(0, '#ECE8E1');
  facadeGrad.addColorStop(1, '#C9C2B6');
  ctx.fillStyle = facadeGrad;
  ctx.fillRect(145, 112, 510, 252);
  ctx.fillStyle = rgba('#4E3220', 0.22);
  ctx.fillRect(145, 112, 115, 252);
  ctx.fillRect(540, 112, 115, 252);
  ctx.strokeStyle = rgba('#3E2313', 0.3);
  ctx.lineWidth = 1.4;
  ctx.beginPath();
  for (const x of [168, 192, 216, 584, 608, 632]) {
    ctx.moveTo(x, 112);
    ctx.lineTo(x, 364);
  }
  ctx.stroke();

  const stepGrad = ctx.createLinearGradient(0, 364, 0, 432);
  stepGrad.addColorStop(0, '#5D6470');
  stepGrad.addColorStop(0.55, '#404650');
  stepGrad.addColorStop(1, '#262A33');
  fillPoly(ctx, [50, 432, 145, 364, 655, 364, 750, 432], stepGrad);
  fillPoly(ctx, [34, 432, 766, 432, 776, 454, 24, 454], '#475569', '#1E293B', 1.2);
  ctx.fillStyle = '#1E293B';
  ctx.fillRect(24, 454, 752, 6);
  fillPoly(ctx, [18, 460, 782, 460, 792, 480, 8, 480], '#3B4352', '#1E293B', 1.2);
  ctx.fillStyle = '#18202C';
  ctx.fillRect(8, 480, 784, 6);
  ctx.fillStyle = '#2D3440';
  ctx.fillRect(0, 486, 800, 14);

  // Thảm WELCOME
  fillPoly(ctx, [318, 378, 482, 378, 500, 418, 300, 418], '#3A2F28', '#9A7B5C', 2);
  ctx.fillStyle = rgba('#E2E8F0', 0.82);
  ctx.font = 'bold 13px Be Vietnam Pro, sans-serif';
  ctx.textAlign = 'center';
  ctx.fillText('WELCOME', 400, 403);

  // Cửa chính gỗ 2 cánh & khóa thông minh
  ctx.fillStyle = '#26140A';
  ctx.beginPath();
  ctx.roundRect(296, 144, 208, 220, 3);
  ctx.fill();
  const doorGrad = ctx.createLinearGradient(305, 152, 495, 200);
  doorGrad.addColorStop(0, '#764729');
  doorGrad.addColorStop(0.5, '#59341D');
  doorGrad.addColorStop(1, '#3C2111');
  ctx.fillStyle = doorGrad;
  ctx.fillRect(305, 152, 92, 212);
  ctx.fillRect(403, 152, 92, 212);

  for (const gx of [322, 458]) {
    ctx.fillStyle = rgba('#94A3B8', 0.28);
    ctx.beginPath();
    ctx.roundRect(gx, 172, 20, 162, 2);
    ctx.fill();
    ctx.strokeStyle = '#26140A';
    ctx.lineWidth = 1.5;
    ctx.stroke();
  }
  ctx.fillStyle = '#E2E8F0';
  ctx.beginPath();
  ctx.roundRect(384, 238, 5.5, 56, 2.5);
  ctx.roundRect(410.5, 238, 5.5, 56, 2.5);
  ctx.fill();

  const lockHex = isLocked ? '#22C55E' : '#EF4444';
  ctx.fillStyle = '#0F172A';
  ctx.beginPath();
  ctx.roundRect(421, 247, 22, 44, 4);
  ctx.fill();
  ctx.strokeStyle = '#475569';
  ctx.lineWidth = 1.2;
  ctx.stroke();
  drawWarmGlowOval(ctx, 432, 259, 14, 14, lockHex, 0.55);
  ctx.fillStyle = lockHex;
  ctx.beginPath();
  ctx.arc(432, 259, 4, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#1E293B';
  ctx.beginPath();
  ctx.roundRect(426, 269, 12, 16, 1.5);
  ctx.fill();

  // 2 cột trụ vuông ốp đá & 2 đèn tường [ĐÈN HÀNH LANG]
  for (const [px, sx] of [
    [46, 40],
    [688, 682],
  ]) {
    const pg = ctx.createLinearGradient(px, 0, px + 66, 0);
    pg.addColorStop(0, '#CBD5E1');
    pg.addColorStop(0.5, '#F1F5F9');
    pg.addColorStop(1, '#94A3B8');
    ctx.fillStyle = pg;
    ctx.fillRect(px, 28, 66, 324);
    ctx.strokeStyle = '#64748B';
    ctx.lineWidth = 1.2;
    ctx.strokeRect(px, 28, 66, 324);

    const sg = ctx.createLinearGradient(sx, 0, sx + 78, 0);
    sg.addColorStop(0, '#334155');
    sg.addColorStop(0.5, '#475569');
    sg.addColorStop(1, '#1E293B');
    ctx.fillStyle = sg;
    ctx.fillRect(sx, 352, 78, 84);
    ctx.strokeStyle = '#1E293B';
    ctx.lineWidth = 1.5;
    ctx.strokeRect(sx, 352, 78, 84);
    ctx.fillStyle = '#334155';
    ctx.fillRect(sx, 28, 78, 14);
  }

  const sconceBulb = hallOn ? '#FEF08A' : '#475569';
  for (const cx of [79, 721]) {
    if (fHall > 0.01) {
      drawWarmGlowOval(ctx, cx, 204, 52, 72, '#F59E0B', 0.72 * fHall);
    }
    ctx.fillStyle = '#1E293B';
    ctx.beginPath();
    ctx.roundRect(cx - 7, 188, 14, 32, 2.5);
    ctx.fill();
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1;
    ctx.stroke();
    ctx.fillStyle = sconceBulb;
    ctx.beginPath();
    ctx.roundRect(cx - 4, 191, 8, 26, 1.5);
    ctx.fill();
  }

  if (fHall > 0.01) {
    const hg = ctx.createRadialGradient(400, 120, 0, 400, 120, 360);
    hg.addColorStop(0, rgba('#FEF08A', 0.92 * 0.72 * fHall));
    hg.addColorStop(0.42, rgba('#F59E0B', 0.34 * 0.72 * fHall));
    hg.addColorStop(1, 'rgba(245,158,11,0)');
    fillPoly(ctx, [220, 66, 580, 66, 710, 455, 90, 455], hg);

    for (const [cx, rx] of [
      [275, 16],
      [400, 18],
      [525, 16],
    ]) {
      drawWarmGlowOval(ctx, cx, 66, rx * 1.4, 8, '#FEF08A', 0.85 * fHall);
      ctx.fillStyle = rgba('#FFFFFF', fHall);
      ctx.beginPath();
      ctx.ellipse(cx, 66, rx * 0.5, 2.6, 0, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  if (fFront > 0.01) {
    drawWarmGlowOval(ctx, 400, 495, 340, 95, '#FDE68A', 0.58 * fFront);
  }

  // Cảm biến chuyển động PIR Dome gắn trần sảnh
  ctx.fillStyle = '#E2E8F0';
  ctx.beginPath();
  ctx.ellipse(400, 95, 22, 7, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.strokeStyle = '#94A3B8';
  ctx.lineWidth = 1.2;
  ctx.stroke();
  ctx.fillStyle = '#CBD5E1';
  ctx.beginPath();
  ctx.moveTo(386, 95);
  ctx.quadraticCurveTo(400, 110, 414, 95);
  ctx.closePath();
  ctx.fill();
  ctx.fillStyle = motionDetected ? '#EF4444' : motionAuto ? '#22C55E' : '#64748B';
  ctx.beginPath();
  ctx.arc(400, 101, 3.2, 0, Math.PI * 2);
  ctx.fill();
}

function drawViewXungQuanh(
  ctx: CanvasRenderingContext2D,
  fHall: number,
  hallOn: boolean,
  fFront: number,
  fSurr: number,
  surrOn: boolean,
  isLocked: boolean,
  camOn: boolean,
  camFovAlpha: number,
  camSweepDeg: number,
) {
  const sky = ctx.createLinearGradient(0, 0, 0, 500);
  sky.addColorStop(0, '#060C1C');
  sky.addColorStop(1, '#15264B');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, 800, 500);

  for (const [sx, sy, sr, sa] of [
    [670, 60, 1.4, 0.8],
    [718, 92, 1.2, 0.7],
    [600, 80, 1.1, 0.65],
  ]) {
    ctx.fillStyle = rgba('#FFFFFF', sa);
    ctx.beginPath();
    ctx.arc(sx, sy, sr, 0, Math.PI * 2);
    ctx.fill();
  }

  const roofGrad = ctx.createLinearGradient(0, 0, 0, 152);
  roofGrad.addColorStop(0, '#334155');
  roofGrad.addColorStop(0.55, '#1E293B');
  roofGrad.addColorStop(1, '#0F172A');
  fillPoly(ctx, [0, 0, 505, 0, 568, 152, 468, 152, 0, 50], roofGrad);

  ctx.strokeStyle = rgba('#475569', 0.35);
  ctx.lineWidth = 1;
  ctx.beginPath();
  for (const [x1, y1, x2, y2] of [
    [65, 0, 150, 78],
    [155, 0, 255, 98],
    [250, 0, 355, 120],
    [345, 0, 452, 140],
    [435, 0, 532, 150],
  ]) {
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
  }
  ctx.stroke();
  fillPoly(ctx, [0, 50, 468, 152, 552, 152, 0, 70], '#3B2618');

  if (fSurr > 0.01) {
    for (const [cx, cy, rx, ry] of [
      [140, 94, 22, 6],
      [285, 124, 18, 5],
      [425, 148, 14, 4],
    ]) {
      drawWarmGlowOval(ctx, cx, cy, rx * 1.4, ry * 1.8, '#FEF08A', 0.9 * fSurr);
      ctx.fillStyle = rgba('#FFFFFF', fSurr);
      ctx.beginPath();
      ctx.ellipse(cx, cy, 5, 1.8, 0, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  const wallGrad = ctx.createLinearGradient(0, 70, 468, 160);
  wallGrad.addColorStop(0, '#EAE6DF');
  wallGrad.addColorStop(1, '#A8A095');
  fillPoly(ctx, [0, 70, 468, 152, 468, 325, 0, 468], wallGrad);
  fillPoly(ctx, [186, 102, 244, 113, 244, 386, 186, 405], rgba('#4A2E1C', 0.88));
  ctx.strokeStyle = rgba('#29170D', 0.6);
  ctx.lineWidth = 1.2;
  ctx.beginPath();
  for (const [x, yb] of [
    [200, 400],
    [215, 395],
    [230, 390],
  ]) {
    ctx.moveTo(x, 100 + (x - 186) * 0.25);
    ctx.lineTo(x, yb);
  }
  ctx.stroke();

  fillPoly(ctx, [48, 140, 166, 162, 166, 306, 48, 324], '#0F172A', '#1E293B', 3.5);
  fillPoly(ctx, [56, 148, 158, 167, 158, 300, 56, 316], rgba('#334155', 0.6));
  ctx.strokeStyle = '#0F172A';
  ctx.lineWidth = 2.8;
  ctx.beginPath();
  ctx.moveTo(108, 156);
  ctx.lineTo(108, 308);
  ctx.stroke();

  fillPoly(ctx, [262, 174, 352, 191, 352, 296, 262, 308], '#0F172A', '#1E293B', 3);
  fillPoly(ctx, [268, 180, 346, 195, 346, 291, 268, 302], rgba('#334155', 0.55));

  const lockHex = isLocked ? '#22C55E' : '#EF4444';
  fillPoly(ctx, [392, 198, 446, 208, 446, 318, 392, 324], '#4E2F1B', '#24150B', 2);
  ctx.fillStyle = lockHex;
  ctx.beginPath();
  ctx.arc(436, 270, 3.5, 0, Math.PI * 2);
  ctx.fill();

  fillPoly(ctx, [468, 236, 800, 190, 800, 388, 468, 325], '#273142', '#18202C', 1.5);
  const surrBulb = surrOn ? '#FEF08A' : '#475569';
  if (fSurr > 0.01) {
    drawWarmGlowOval(ctx, 615, 216, 34, 34, '#F59E0B', 0.55 * fSurr);
    drawWarmGlowOval(ctx, 745, 198, 40, 40, '#F59E0B', 0.6 * fSurr);
  }
  ctx.fillStyle = surrBulb;
  ctx.beginPath();
  ctx.roundRect(610, 210, 10, 10, 2);
  ctx.roundRect(739, 191, 12, 12, 2);
  ctx.fill();

  ctx.fillStyle = '#14362A';
  ctx.beginPath();
  ctx.moveTo(510, 338);
  ctx.quadraticCurveTo(560, 285, 620, 308);
  ctx.quadraticCurveTo(675, 260, 740, 294);
  ctx.quadraticCurveTo(778, 278, 800, 310);
  ctx.lineTo(800, 460);
  ctx.lineTo(510, 355);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = '#1B4D3E';
  ctx.beginPath();
  ctx.moveTo(545, 360);
  ctx.quadraticCurveTo(605, 316, 668, 338);
  ctx.quadraticCurveTo(725, 305, 800, 345);
  ctx.lineTo(800, 500);
  ctx.lineTo(545, 388);
  ctx.closePath();
  ctx.fill();

  const walkGrad = ctx.createLinearGradient(0, 325, 0, 500);
  walkGrad.addColorStop(0, '#373C47');
  walkGrad.addColorStop(1, '#585F6E');
  fillPoly(ctx, [0, 468, 468, 325, 555, 345, 585, 500, 0, 500], walkGrad);
  ctx.strokeStyle = rgba('#242933', 0.55);
  ctx.lineWidth = 1.4;
  ctx.beginPath();
  for (const [x1, y1, x2, y2] of [
    [88, 440, 558, 462],
    [210, 402, 554, 416],
    [315, 370, 545, 380],
    [398, 344, 538, 352],
  ]) {
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
  }
  ctx.stroke();

  if (fFront > 0.01) {
    drawWarmGlowOval(ctx, 15, 360, 185, 180, '#FDE68A', 0.7 * fFront);
  }

  const hallBulb = hallOn ? '#FEF08A' : '#475569';
  const sconces: Array<[number[], [number, number, number, number], [number, number, number, number]]> = [
    [[214, 190, 142, 422, 288, 392], [206, 172, 16, 30], [209, 176, 10, 22]],
    [[366, 210, 316, 374, 416, 356], [360, 196, 12, 24], [362, 199, 8, 18]],
    [[454, 224, 422, 338, 485, 330], [450, 214, 9, 18], [452, 216, 5, 14]],
  ];
  for (const [cone, [hx, hy, hw, hh], [bx, by, bw, bh]] of sconces) {
    if (fHall > 0.01) {
      const cg = ctx.createLinearGradient(0, cone[1], 0, Math.max(cone[3], cone[5]));
      cg.addColorStop(0, rgba('#FEF08A', 0.9 * fHall));
      cg.addColorStop(0.55, rgba('#F59E0B', 0.28 * fHall));
      cg.addColorStop(1, 'rgba(245,158,11,0)');
      fillPoly(ctx, cone, cg);
    }
    ctx.fillStyle = '#1E293B';
    ctx.beginPath();
    ctx.roundRect(hx, hy, hw, hh, 2.5);
    ctx.fill();
    ctx.fillStyle = hallBulb;
    ctx.beginPath();
    ctx.roundRect(bx, by, bw, bh, 1.5);
    ctx.fill();
  }

  const bollards: Array<[number, number, number, number, [number, number, number, number], [number, number, number, number]]> = [
    [536, 430, 48, 20, [531, 404, 10, 30], [532, 407, 8, 6]],
    [526, 378, 36, 15, [522, 358, 8, 24], [523, 361, 6, 5]],
    [515, 342, 26, 11, [512, 328, 6, 18], [513, 330, 4, 4]],
  ];
  for (const [cx, cy, grx, gry, [px, py, pw, ph], [kx, ky, kw, kh]] of bollards) {
    if (fSurr > 0.01) drawWarmGlowOval(ctx, cx, cy, grx, gry, '#F59E0B', 0.5 * fSurr);
    ctx.fillStyle = '#1E293B';
    ctx.beginPath();
    ctx.roundRect(px, py, pw, ph, 2);
    ctx.fill();
    ctx.fillStyle = surrBulb;
    ctx.beginPath();
    ctx.roundRect(kx, ky, kw, kh, 1);
    ctx.fill();
  }

  if (camFovAlpha > 0.01) {
    ctx.save();
    ctx.translate(508, 156);
    ctx.rotate((camSweepDeg * Math.PI) / 180);
    ctx.translate(-508, -156);
    const fovG = ctx.createLinearGradient(508, 156, 675, 432);
    fovG.addColorStop(0, rgba('#38BDF8', 0.34 * camFovAlpha));
    fovG.addColorStop(1, 'rgba(56,189,248,0)');
    fillPoly(ctx, [508, 156, 675, 382, 385, 432], fovG);
    ctx.restore();
  }
  ctx.strokeStyle = '#94A3B8';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(494, 148);
  ctx.lineTo(508, 156);
  ctx.stroke();

  ctx.save();
  ctx.translate(510, 155);
  ctx.rotate((24 * Math.PI) / 180);
  ctx.translate(-510, -155);
  ctx.fillStyle = '#E2E8F0';
  ctx.beginPath();
  ctx.roundRect(500, 149, 22, 12, 3);
  ctx.fill();
  ctx.strokeStyle = '#475569';
  ctx.lineWidth = 1;
  ctx.stroke();
  ctx.restore();

  ctx.fillStyle = '#0F172A';
  ctx.beginPath();
  ctx.arc(519, 160, 3, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = camOn ? '#EF4444' : '#475569';
  ctx.beginPath();
  ctx.arc(519, 160, 1.6, 0, Math.PI * 2);
  ctx.fill();
}

function drawViewTuXa(
  ctx: CanvasRenderingContext2D,
  fHall: number,
  fFront: number,
  frontOn: boolean,
  fSurr: number,
  surrOn: boolean,
  isLocked: boolean,
  camOn: boolean,
  camFovAlpha: number,
  camSweepDeg: number,
) {
  const sky = ctx.createLinearGradient(0, 0, 0, 335);
  sky.addColorStop(0, '#050914');
  sky.addColorStop(0.55, '#0F1D3A');
  sky.addColorStop(1, '#1B3158');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, 800, 335);

  ctx.fillStyle = rgba('#FEF9C3', 0.9);
  ctx.beginPath();
  ctx.arc(114, 64, 17, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#091227';
  ctx.beginPath();
  ctx.arc(122, 59, 16, 0, Math.PI * 2);
  ctx.fill();

  for (const [sx, sy, sr] of [
    [220, 44, 1.4],
    [312, 38, 1.1],
    [515, 40, 1.3],
    [628, 64, 1.5],
    [722, 50, 1.2],
    [680, 102, 1.1],
  ]) {
    ctx.fillStyle = rgba('#FFFFFF', 0.75);
    ctx.beginPath();
    ctx.arc(sx, sy, sr, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.fillStyle = '#0D1B2A';
  ctx.beginPath();
  ctx.moveTo(0, 318);
  ctx.quadraticCurveTo(110, 264, 220, 294);
  ctx.quadraticCurveTo(380, 290, 540, 285);
  ctx.quadraticCurveTo(670, 290, 800, 305);
  ctx.lineTo(800, 348);
  ctx.lineTo(0, 348);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = '#123026';
  ctx.beginPath();
  ctx.moveTo(585, 332);
  ctx.quadraticCurveTo(645, 260, 715, 286);
  ctx.quadraticCurveTo(765, 264, 800, 296);
  ctx.lineTo(800, 348);
  ctx.lineTo(585, 348);
  ctx.closePath();
  ctx.fill();

  ctx.beginPath();
  ctx.moveTo(0, 332);
  ctx.quadraticCurveTo(55, 276, 125, 295);
  ctx.quadraticCurveTo(172, 275, 215, 308);
  ctx.lineTo(215, 348);
  ctx.lineTo(0, 348);
  ctx.closePath();
  ctx.fill();

  const yardGrad = ctx.createLinearGradient(0, 335, 0, 500);
  yardGrad.addColorStop(0, '#1F2937');
  yardGrad.addColorStop(1, '#111827');
  ctx.fillStyle = yardGrad;
  ctx.fillRect(0, 335, 800, 165);
  fillPoly(ctx, [0, 338, 298, 338, 220, 500, 0, 500], '#123026');
  fillPoly(ctx, [502, 338, 800, 338, 800, 500, 580, 500], '#123026');
  fillPoly(ctx, [304, 338, 496, 338, 565, 500, 235, 500], '#374151');
  ctx.strokeStyle = rgba('#1F2937', 0.65);
  ctx.lineWidth = 1.4;
  ctx.beginPath();
  for (const [x1, y, x2] of [
    [290, 376, 510],
    [270, 420, 530],
    [250, 466, 550],
  ]) {
    ctx.moveTo(x1, y);
    ctx.lineTo(x2, y);
  }
  ctx.stroke();

  const surrBulb = surrOn ? '#FEF08A' : '#475569';
  for (const [cx, cy, grx, gry] of [
    [122, 348, 68, 26],
    [176, 364, 46, 18],
    [678, 348, 68, 26],
    [624, 364, 46, 18],
  ]) {
    if (fSurr > 0.01) drawWarmGlowOval(ctx, cx, cy, grx, gry, '#F59E0B', 0.55 * fSurr);
    ctx.fillStyle = '#1E293B';
    ctx.beginPath();
    ctx.roundRect(cx - 4, cy - 16, 6, 16, 1.5);
    ctx.fill();
    ctx.fillStyle = surrBulb;
    ctx.fillRect(cx - 3, cy - 14, 4, 4);
  }

  // Tầng 2 Biệt thự mái Nhật
  const wallGrad = ctx.createLinearGradient(0, 132, 0, 342);
  wallGrad.addColorStop(0, '#F1EDE6');
  wallGrad.addColorStop(1, '#C4BDB2');
  ctx.fillStyle = wallGrad;
  ctx.fillRect(206, 132, 388, 100);
  ctx.fillStyle = '#5B3A24';
  ctx.fillRect(218, 138, 128, 90);
  ctx.strokeStyle = rgba('#2D1A0E', 0.55);
  ctx.lineWidth = 1.2;
  ctx.beginPath();
  for (const x of [234, 250, 266, 282, 298, 314, 330]) {
    ctx.moveTo(x, 138);
    ctx.lineTo(x, 228);
  }
  ctx.stroke();

  if (fFront > 0.01) {
    drawWarmGlowOval(ctx, 475, 184, 115, 48, '#FEF08A', 0.8 * fFront);
  }

  const winWarm = ctx.createLinearGradient(0, 150, 0, 315);
  winWarm.addColorStop(0, rgba('#FDE68A', 0.65));
  winWarm.addColorStop(1, rgba('#F59E0B', 0.28));
  ctx.fillStyle = '#0F172A';
  ctx.fillRect(234, 150, 94, 72);
  ctx.fillStyle = winWarm;
  ctx.fillRect(240, 156, 82, 60);
  ctx.strokeStyle = '#0F172A';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(281, 156);
  ctx.lineTo(281, 216);
  ctx.stroke();

  ctx.fillStyle = '#0F172A';
  ctx.fillRect(372, 148, 194, 78);
  ctx.fillStyle = winWarm;
  ctx.fillRect(378, 154, 182, 66);
  ctx.beginPath();
  ctx.moveTo(438, 154);
  ctx.lineTo(438, 220);
  ctx.moveTo(500, 154);
  ctx.lineTo(500, 220);
  ctx.stroke();

  ctx.fillStyle = rgba('#94A3B8', 0.26);
  ctx.fillRect(358, 198, 242, 30);
  ctx.strokeStyle = '#CBD5E1';
  ctx.lineWidth = 1;
  ctx.strokeRect(358, 198, 242, 30);
  ctx.fillStyle = '#0F172A';
  ctx.fillRect(356, 196, 246, 3.5);
  ctx.strokeStyle = '#1E293B';
  ctx.lineWidth = 2;
  ctx.beginPath();
  for (const x of [418, 480, 542]) {
    ctx.moveTo(x, 198);
    ctx.lineTo(x, 228);
  }
  ctx.stroke();

  const roofMain = ctx.createLinearGradient(0, 68, 0, 138);
  roofMain.addColorStop(0, '#3B485C');
  roofMain.addColorStop(0.48, '#253042');
  roofMain.addColorStop(1, '#141C29');
  const roofSide = ctx.createLinearGradient(154, 68, 646, 134);
  roofSide.addColorStop(0, '#2C3748');
  roofSide.addColorStop(1, '#111827');
  fillPoly(ctx, [154, 134, 278, 68, 522, 68, 646, 134], roofMain, '#0F172A', 1.5);
  fillPoly(ctx, [154, 134, 278, 68, 294, 134], roofSide);
  fillPoly(ctx, [646, 134, 522, 68, 506, 134], roofSide);
  ctx.strokeStyle = '#64748B';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(278, 68);
  ctx.lineTo(522, 68);
  ctx.stroke();
  ctx.strokeStyle = '#475569';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(278, 68);
  ctx.lineTo(154, 134);
  ctx.moveTo(522, 68);
  ctx.lineTo(646, 134);
  ctx.stroke();
  ctx.strokeStyle = rgba('#475569', 0.35);
  ctx.lineWidth = 0.9;
  ctx.beginPath();
  for (const [x1, y, x2] of [
    [252, 82, 548],
    [226, 96, 574],
    [198, 110, 602],
    [172, 124, 628],
  ]) {
    ctx.moveTo(x1, y);
    ctx.lineTo(x2, y);
  }
  ctx.stroke();

  fillPoly(ctx, [348, 138, 432, 96, 562, 96, 632, 138], roofMain, '#0F172A', 1.2);
  ctx.strokeStyle = '#64748B';
  ctx.lineWidth = 2.2;
  ctx.beginPath();
  ctx.moveTo(432, 96);
  ctx.lineTo(562, 96);
  ctx.stroke();
  ctx.fillStyle = '#3A2315';
  ctx.fillRect(158, 133, 484, 5);

  if (fFront > 0.01) {
    for (const x of [195, 275, 365, 455, 545, 605]) {
      drawWarmGlowOval(ctx, x, 138, 12, 12, '#F59E0B', 0.7 * fFront);
      ctx.fillStyle = rgba('#FEF08A', fFront);
      ctx.beginPath();
      ctx.arc(x, 137, 2.8, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // Tầng 1 & sảnh chính
  ctx.fillStyle = wallGrad;
  ctx.fillRect(176, 228, 448, 114);
  if (fHall > 0.01) {
    drawWarmGlowOval(ctx, 400, 298, 145, 62, '#FDE68A', 0.92 * fHall);
  }
  const roof1 = ctx.createLinearGradient(0, 206, 0, 242);
  roof1.addColorStop(0, '#3B485C');
  roof1.addColorStop(1, '#141C29');
  fillPoly(ctx, [156, 236, 218, 206, 582, 206, 644, 236], roof1, '#0F172A', 1.2);
  fillPoly(ctx, [282, 242, 326, 214, 474, 214, 518, 242], '#2D394C', '#0F172A', 1.2);
  ctx.strokeStyle = '#64748B';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(326, 214);
  ctx.lineTo(474, 214);
  ctx.stroke();
  ctx.fillStyle = '#3A2315';
  ctx.fillRect(284, 241, 232, 4.5);

  if (fFront > 0.01) {
    for (const x of [192, 246, 554, 608]) {
      drawWarmGlowOval(ctx, x, 238, 12, 12, '#F59E0B', 0.7 * fFront);
      ctx.fillStyle = rgba('#FEF08A', fFront);
      ctx.beginPath();
      ctx.arc(x, 237, 3, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  if (fHall > 0.01) {
    for (const [cx, r, hex] of [
      [332, 4, '#FEF08A'],
      [400, 4.5, '#FFFFFF'],
      [468, 4, '#FEF08A'],
    ] as Array<[number, number, string]>) {
      ctx.fillStyle = rgba(hex, fHall);
      ctx.beginPath();
      ctx.arc(cx, 245, r, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  for (const [px, bx] of [
    [304, 302],
    [478, 476],
  ]) {
    ctx.fillStyle = '#E2E8F0';
    ctx.fillRect(px, 245, 18, 70);
    ctx.strokeStyle = '#94A3B8';
    ctx.lineWidth = 0.8;
    ctx.strokeRect(px, 245, 18, 70);
    ctx.fillStyle = '#334155';
    ctx.fillRect(bx, 315, 22, 27);
  }

  const lockHex = isLocked ? '#22C55E' : '#EF4444';
  ctx.fillStyle = '#59341D';
  ctx.fillRect(358, 256, 84, 78);
  ctx.strokeStyle = '#26140A';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(358, 256, 84, 78);
  ctx.beginPath();
  ctx.moveTo(400, 256);
  ctx.lineTo(400, 334);
  ctx.stroke();
  drawWarmGlowOval(ctx, 412, 296, 10, 10, lockHex, 0.55);
  ctx.fillStyle = lockHex;
  ctx.beginPath();
  ctx.arc(412, 296, 3, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#64748B';
  ctx.fillRect(304, 334, 192, 5);
  ctx.fillStyle = '#475569';
  ctx.fillRect(296, 339, 208, 5);

  for (const wx of [200, 520]) {
    ctx.fillStyle = '#0F172A';
    ctx.fillRect(wx, 258, 80, 62);
    ctx.fillStyle = winWarm;
    ctx.fillRect(wx + 5, 263, 70, 52);
    ctx.strokeStyle = '#0F172A';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    ctx.moveTo(wx + 40, 263);
    ctx.lineTo(wx + 40, 315);
    ctx.stroke();
  }

  if (camFovAlpha > 0.01) {
    ctx.save();
    ctx.translate(612, 236);
    ctx.rotate((camSweepDeg * Math.PI) / 180);
    ctx.translate(-612, -236);
    const fovG = ctx.createLinearGradient(0, 236, 0, 465);
    fovG.addColorStop(0, rgba('#38BDF8', 0.28 * camFovAlpha));
    fovG.addColorStop(1, 'rgba(56,189,248,0)');
    fillPoly(ctx, [612, 236, 765, 455, 335, 465], fovG);
    ctx.restore();
  }
  ctx.fillStyle = camOn ? '#EF4444' : '#475569';
  ctx.beginPath();
  ctx.arc(612, 236, 3, 0, Math.PI * 2);
  ctx.fill();

  // Tường rào tiền cảnh & cổng mái Nhật
  ctx.fillStyle = '#1E293B';
  ctx.fillRect(0, 380, 244, 68);
  ctx.fillRect(556, 380, 244, 68);
  ctx.strokeStyle = '#334155';
  ctx.lineWidth = 1.5;
  ctx.strokeRect(0, 380, 244, 68);
  ctx.strokeRect(556, 380, 244, 68);

  for (const fx of [64, 152, 648, 736]) {
    if (fSurr > 0.01) drawWarmGlowOval(ctx, fx, 378, 24, 24, '#F59E0B', 0.55 * fSurr);
    ctx.fillStyle = surrBulb;
    ctx.beginPath();
    ctx.roundRect(fx - 5, 373, 10, 8, 1.5);
    ctx.fill();
  }

  const frontBulb = frontOn ? '#FEF08A' : '#475569';
  for (const [gx, cx] of [
    [238, 252],
    [534, 548],
  ]) {
    ctx.fillStyle = '#334155';
    ctx.fillRect(gx, 356, 28, 102);
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1;
    ctx.strokeRect(gx, 356, 28, 102);
    fillPoly(ctx, [gx - 5, 356, cx, 346, gx + 33, 356], '#1E293B');
    if (fFront > 0.01) drawWarmGlowOval(ctx, cx, 342, 32, 32, '#F59E0B', 0.62 * fFront);
    ctx.fillStyle = frontBulb;
    ctx.beginPath();
    ctx.roundRect(cx - 7, 336, 14, 11, 2);
    ctx.fill();
  }

  ctx.strokeStyle = '#475569';
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.moveTo(268, 386);
  ctx.lineTo(532, 386);
  ctx.moveTo(268, 442);
  ctx.lineTo(532, 442);
  for (const bx of [302, 338, 374, 426, 462, 498]) {
    ctx.moveTo(bx, 386);
    ctx.lineTo(bx, 442);
  }
  ctx.stroke();
  ctx.fillStyle = lockHex;
  ctx.beginPath();
  ctx.arc(252, 396, 4.5, 0, Math.PI * 2);
  ctx.fill();
}

function StageLightBadge({ name, isOn, brightness }: { name: string; isOn: boolean; brightness: number }) {
  return (
    <span
      className="inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-[9px] font-semibold"
      style={{
        backgroundColor: 'rgba(0,0,0,0.62)',
        border: `1px solid ${isOn ? tint(SH.Amber, 0.5) : '#334155'}`,
        color: isOn ? '#FCD34D' : '#8A9BB8',
      }}
    >
      <span
        className="h-1.5 w-1.5 rounded-full"
        style={{ backgroundColor: isOn ? SH.Amber : '#475569' }}
      />
      <span>{isOn ? `${name}: ${brightness}%` : `${name}: Tắt`}</span>
    </span>
  );
}

/**
 * Khung mô phỏng 2.5D Hành lang - Biệt thự mái Nhật 2 tầng (port 1:1 từ `HallwayStage.kt`).
 */
export function HallwayStage({
  room,
  onSelectView,
  onDismissMotion,
  fill = false,
  compact = false,
}: {
  room: RoomUi;
  onSelectView?: (v: HallwayView) => void;
  onDismissMotion?: () => void;
  fill?: boolean;
  compact?: boolean;
}) {
  const targetHall = room.lightOn ? Math.max(10, Math.min(100, room.lightBrightness)) / 100 : 0;
  const targetFront = room.hallFrontLightOn
    ? Math.max(10, Math.min(100, room.hallFrontBrightness)) / 100
    : 0;
  const targetSurr = room.hallSurroundingLightOn
    ? Math.max(10, Math.min(100, room.hallSurroundingBrightness)) / 100
    : 0;
  const irActive = room.hallCameraOn && room.hallIrMode;

  const animRef = useRef({
    fHall: targetHall,
    fFront: targetFront,
    fSurr: targetSurr,
    camHudAlpha: room.hallCameraOn ? 1 : 0,
    irMix: irActive ? 1 : 0,
    camSweepPhase: 0,
    radarPhase: 0,
    pulsePhase: 0,
  });

  const [hudTime, setHudTime] = useState(() => formatHudClock(new Date()));
  useEffect(() => {
    if (compact || !room.hallCameraOn) return;
    setHudTime(formatHudClock(new Date()));
    const id = window.setInterval(() => setHudTime(formatHudClock(new Date())), 1000);
    return () => window.clearInterval(id);
  }, [compact, room.hallCameraOn]);

  const activeViewMeta = HALLWAY_VIEWS.find((v) => v.id === room.hallView) ?? HALLWAY_VIEWS[0];

  const viewport = (
    <StageCanvas
      logicalWidth={HW_W}
      logicalHeight={HW_H}
      fill={fill || !compact}
      borderless={compact}
      bgColor="#080D1A"
      className={
        !compact
          ? `!rounded-xl ${room.hallMotionDetected ? '!border-[#EF4444] !border-2' : '!border-[#253459]'}`
          : ''
      }
      canvasStyle={
        irActive
          ? {
              filter: 'grayscale(1) sepia(1) hue-rotate(78deg) saturate(3.2) brightness(0.92) contrast(1.12)',
              transition: 'filter 400ms ease',
            }
          : { transition: 'filter 400ms ease' }
      }
      draw={({ ctx, dtSec }) => {
        const st = animRef.current;
        st.fHall = smoothToward(st.fHall, targetHall, dtSec, 0.5);
        st.fFront = smoothToward(st.fFront, targetFront, dtSec, 0.5);
        st.fSurr = smoothToward(st.fSurr, targetSurr, dtSec, 0.5);
        st.camHudAlpha = smoothToward(st.camHudAlpha, room.hallCameraOn ? 1 : 0, dtSec, 0.35);
        st.irMix = smoothToward(st.irMix, irActive ? 1 : 0, dtSec, 0.45);

        if (room.hallCameraOn) {
          st.camSweepPhase = (st.camSweepPhase + dtSec / 5.2) % 1;
        }
        if (room.hallMotionDetected) {
          st.radarPhase = (st.radarPhase + dtSec / 1.55) % 1;
          st.pulsePhase = (st.pulsePhase + dtSec / 1.1) % 1;
        } else {
          st.radarPhase = 0;
          st.pulsePhase = 0;
        }

        const camSweepDeg = 0.5 + 11.5 * Math.sin(st.camSweepPhase * Math.PI * 2);

        if (room.hallView === 'DAU_HE') {
          drawViewDauHe(
            ctx,
            st.fHall,
            room.lightOn,
            st.fFront,
            st.fSurr,
            room.hallDoorLocked,
            room.hallMotionAuto,
            room.hallMotionDetected,
          );
        } else if (room.hallView === 'XUNG_QUANH') {
          drawViewXungQuanh(
            ctx,
            st.fHall,
            room.lightOn,
            st.fFront,
            st.fSurr,
            room.hallSurroundingLightOn,
            room.hallDoorLocked,
            room.hallCameraOn,
            st.camHudAlpha,
            camSweepDeg,
          );
        } else {
          drawViewTuXa(
            ctx,
            st.fHall,
            st.fFront,
            room.hallFrontLightOn,
            st.fSurr,
            room.hallSurroundingLightOn,
            room.hallDoorLocked,
            room.hallCameraOn,
            st.camHudAlpha,
            camSweepDeg,
          );
        }

        // Bóng tối môi trường theo trung bình 3 cụm đèn
        const avgLight = (st.fHall + st.fFront + st.fSurr) / 3;
        const darkness = Math.max(0, Math.min(0.68, (1 - avgLight) * 0.68));
        if (darkness > 0.01) {
          const dg = ctx.createRadialGradient(400, 500 * 0.44, 0, 400, 500 * 0.44, 800 * 0.72);
          dg.addColorStop(0, rgba('#080C18', 0.2 * darkness));
          dg.addColorStop(0.68, rgba('#050812', 0.72 * darkness));
          dg.addColorStop(1, rgba('#02050E', 0.9 * darkness));
          ctx.fillStyle = dg;
          ctx.fillRect(0, 0, 800, 500);
        }

        // Sọc quét IR
        if (st.irMix > 0.01) {
          ctx.fillStyle = rgba('#22C55E', 0.07 * st.irMix);
          for (let y = 0; y < 500; y += 3) {
            ctx.fillRect(0, y, 800, 1);
          }
        }

        // Sóng Radar 3 vòng & viền đỏ khi phát hiện chuyển động
        if (room.hallMotionDetected) {
          const alertPulse = (Math.sin(st.pulsePhase * Math.PI * 2) + 1) * 0.5;
          const eg = ctx.createRadialGradient(400, 250, 0, 400, 250, 800 * 0.75);
          eg.addColorStop(0.55, 'rgba(239,68,68,0)');
          eg.addColorStop(1, rgba('#EF4444', 0.22 + 0.3 * alertPulse));
          ctx.fillStyle = eg;
          ctx.fillRect(0, 0, 800, 500);

          const baseR = compact ? 42 : 58;
          for (const offset of [0, 0.33, 0.66]) {
            const p = (st.radarPhase + offset) % 1;
            const r = baseR * (0.18 + (1.75 - 0.18) * p);
            const alpha = Math.max(0, Math.min(1, 1 - p)) * 0.9;
            const rg = ctx.createRadialGradient(400, 280, 0, 400, 280, r);
            rg.addColorStop(0, rgba('#EF4444', 0.24 * alpha));
            rg.addColorStop(1, 'rgba(239,68,68,0)');
            ctx.fillStyle = rg;
            ctx.beginPath();
            ctx.arc(400, 280, r, 0, Math.PI * 2);
            ctx.fill();
            ctx.strokeStyle = rgba('#EF4444', alpha);
            ctx.lineWidth = 2;
            ctx.stroke();
          }
        }
      }}
    >
      {!compact && (
        <div className="pointer-events-none absolute inset-0 p-2 flex flex-col justify-between">
          <div className="flex flex-col gap-1.5">
            {room.hallCameraOn && (
              <div className="flex items-center justify-between gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-md border border-white/15 bg-black/60 px-2 py-0.5 font-mono text-[10px] font-bold text-white">
                  <span className="h-1.5 w-1.5 rounded-full bg-[#EF4444] animate-pulse" />
                  <span>REC | {activeViewMeta.camTitle}</span>
                </span>
                <div className="flex items-center gap-1.5">
                  {room.hallIrMode && (
                    <span className="rounded border border-[#22C55E]/55 bg-[#22C55E]/20 px-1.5 py-0.5 font-mono text-[9px] font-bold text-[#86EFAC]">
                      IR NIGHT
                    </span>
                  )}
                  <span className="rounded-md border border-white/15 bg-black/60 px-2 py-0.5 font-mono text-[9.5px] text-[#E2E8F0]">
                    {hudTime}
                  </span>
                </div>
              </div>
            )}

            {room.hallMotionDetected && (
              <div className="pointer-events-auto flex items-center gap-2 rounded-lg border border-[#FCA5A5]/65 bg-[#B91C1C]/95 px-2.5 py-1.5 shadow-lg">
                <div className="min-w-0 flex-1">
                  <div className="text-[11px] font-bold text-white truncate">
                    Phát hiện chuyển động tại {activeViewMeta.label}!
                  </div>
                  <div className="text-[9.5px] text-[#FEE2E2] truncate">
                    {room.hallMotionAuto
                      ? 'Đã tự động bật Đèn hành lang & Đèn xung quanh.'
                      : 'Cảm biến PIR kích hoạt • Tự bật đèn đang tắt.'}
                  </div>
                </div>
                {onDismissMotion && (
                  <button
                    type="button"
                    onClick={onDismissMotion}
                    className="shrink-0 rounded bg-white px-2 py-0.5 text-[10px] font-bold text-[#DC2626] hover:bg-[#FEE2E2] cursor-pointer"
                  >
                    Đã xem
                  </button>
                )}
              </div>
            )}
          </div>

          <div className="flex flex-wrap items-end justify-between gap-1.5">
            <div className="flex flex-wrap items-center gap-1">
              <StageLightBadge name="Hành lang" isOn={room.lightOn} brightness={room.lightBrightness} />
              <StageLightBadge
                name="Trước nhà"
                isOn={room.hallFrontLightOn}
                brightness={room.hallFrontBrightness}
              />
              <StageLightBadge
                name="Xung quanh"
                isOn={room.hallSurroundingLightOn}
                brightness={room.hallSurroundingBrightness}
              />
            </div>
            <span
              className="rounded px-1.5 py-0.5 font-mono text-[9px] font-semibold"
              style={{
                backgroundColor: room.hallDoorLocked ? 'rgba(0,0,0,0.62)' : 'rgba(69,10,10,0.82)',
                border: `1px solid ${room.hallDoorLocked ? 'rgba(255,255,255,0.12)' : tint(SH.Red, 0.55)}`,
                color: room.hallDoorLocked ? '#34D399' : '#FCA5A5',
              }}
            >
              {room.hallDoorLocked ? 'LOCK: ĐÃ KHÓA' : 'LOCK: ĐANG MỞ'}
            </span>
          </div>
        </div>
      )}
    </StageCanvas>
  );

  if (compact) return viewport;

  return (
    <div
      className={`flex flex-col gap-1.5 rounded-2xl border border-[#253459] bg-[#131D33] p-2 ${
        fill ? 'flex-1 min-h-0 h-full' : ''
      }`}
    >
      <div className="shrink-0 flex items-center justify-between gap-2">
        <span className="text-[11px] font-semibold text-[#8A9BB8]">Mô phỏng 2.5D</span>
        <div className="inline-flex rounded-xl border border-[#1E293B] bg-[#0B1120] p-0.5 gap-1">
          {HALLWAY_VIEWS.map((v) => {
            const active = room.hallView === v.id;
            return (
              <button
                key={v.id}
                type="button"
                onClick={() => onSelectView?.(v.id)}
                className={`rounded-lg px-2.5 py-1 text-[11px] font-semibold transition-colors cursor-pointer ${
                  active ? 'bg-[#4F8BFF] text-white' : 'text-[#8A9BB8] hover:text-[#E6EDF7]'
                }`}
              >
                {v.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className={fill ? 'flex-1 min-h-0 flex flex-col' : ''}>{viewport}</div>
    </div>
  );
}
