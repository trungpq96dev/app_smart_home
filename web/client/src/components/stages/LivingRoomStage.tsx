import React, { useRef } from 'react';
import type { RoomUi } from '../../data/models.js';
import { fillPoly, lerpHex, rgba, smoothToward, StageCanvas, tracePoly } from './StageCanvas.js';

const LV_W = 800;
const LV_H = 550;

function drawLivingFanBlades(ctx: CanvasRenderingContext2D, u: number, alpha: number) {
  for (let i = 0; i < 5; i++) {
    ctx.save();
    ctx.rotate((i * 72 * Math.PI) / 180);
    const g = ctx.createLinearGradient(-12 * u, -92 * u, 12 * u, 0);
    g.addColorStop(0, rgba('#64748B', alpha));
    g.addColorStop(0.6, rgba('#475569', alpha));
    g.addColorStop(1, rgba('#334155', alpha));
    ctx.beginPath();
    ctx.moveTo(-4 * u, -8 * u);
    ctx.lineTo(-10 * u, -86 * u);
    ctx.quadraticCurveTo(0, -94 * u, 10 * u, -86 * u);
    ctx.lineTo(4 * u, -8 * u);
    ctx.closePath();
    ctx.fillStyle = g;
    ctx.fill();
    if (alpha >= 0.9) {
      ctx.strokeStyle = rgba('#1E293B', 0.6);
      ctx.lineWidth = u;
      ctx.stroke();
    }
    ctx.restore();
  }
}

/**
 * Khung mô phỏng Phòng khách Vector 2.5D (port 1:1 từ `LivingRoomStage.kt`).
 */
export function LivingRoomStage({
  room,
  fill = false,
  compact = false,
}: {
  room: RoomUi;
  fill?: boolean;
  compact?: boolean;
}) {
  const isEvening = room.mode === 'SLEEP' || (!room.lightOn && room.curtainOpen < 25);
  const targetLight = room.lightOn ? Math.max(10, Math.min(100, room.lightBrightness)) / 100 : 0;
  const targetCurtain = Math.max(0, Math.min(100, room.curtainOpen)) / 100;

  const animRef = useRef({
    lightFactor: targetLight,
    curtainRatio: targetCurtain,
    tvAlpha: room.tvOn ? 1 : 0,
    acAlpha: room.acOn ? 1 : 0,
    eveningAlpha: isEvening ? 1 : 0,
    fanAngleDeg: 0,
    fanVelocity: room.fanOn ? 7.5 : 0,
    animPhase: 0,
  });

  const sceneBadgeText =
    room.tvOn && room.mode === 'SLEEP'
      ? '🎬 Rạp phim tại gia'
      : isEvening
        ? '🌙 Không gian buổi tối'
        : room.curtainOpen >= 60
          ? '☀️ Ánh sáng tự nhiên'
          : '🛋️ Không gian ấm cúng';

  return (
    <StageCanvas
      logicalWidth={LV_W}
      logicalHeight={LV_H}
      fill={fill}
      borderless={compact}
      bgColor="#0B101D"
      draw={({ ctx, dtSec }) => {
        const st = animRef.current;
        st.lightFactor = smoothToward(st.lightFactor, targetLight, dtSec, 0.6);
        st.curtainRatio = smoothToward(st.curtainRatio, targetCurtain, dtSec, 0.75);
        st.tvAlpha = smoothToward(st.tvAlpha, room.tvOn ? 1 : 0, dtSec, 0.55);
        st.acAlpha = smoothToward(st.acAlpha, room.acOn ? 1 : 0, dtSec, 0.5);
        st.eveningAlpha = smoothToward(st.eveningAlpha, isEvening ? 1 : 0, dtSec, 0.6);

        const dtFrames = dtSec * 60;
        const targetVel = room.fanOn ? 7.5 : 0;
        const accel = room.fanOn ? 0.045 : 0.022;
        st.fanVelocity += (targetVel - st.fanVelocity) * accel * dtFrames;
        if (!room.fanOn && st.fanVelocity <= 0.05) {
          st.fanVelocity = 0;
        } else {
          st.fanAngleDeg = (st.fanAngleDeg + st.fanVelocity * dtFrames) % 360;
        }

        if (room.acOn || room.tvOn) {
          st.animPhase = (st.animPhase + 0.012 * dtFrames) % 1;
        }

        const lf = st.lightFactor;
        const cr = st.curtainRatio;
        const tva = st.tvAlpha;
        const aca = st.acAlpha;
        const eva = st.eveningAlpha;

        // 1. Khung phòng phối cảnh 2.5D
        const backGrad = ctx.createLinearGradient(0, 152, 0, 388);
        backGrad.addColorStop(0, '#D2CBC1');
        backGrad.addColorStop(1, '#B9B0A4');
        fillPoly(ctx, [145, 152, 655, 152, 655, 388, 145, 388], backGrad);

        const leftGrad = ctx.createLinearGradient(0, 0, 145, 160);
        leftGrad.addColorStop(0, '#B8B0A5');
        leftGrad.addColorStop(1, '#D5CEC4');
        fillPoly(ctx, [0, 0, 145, 152, 145, 388, 0, 515], leftGrad);

        const rightGrad = ctx.createLinearGradient(800, 0, 655, 160);
        rightGrad.addColorStop(0, '#AFA79C');
        rightGrad.addColorStop(1, '#CFC8BD');
        fillPoly(ctx, [800, 0, 655, 152, 655, 388, 800, 515], rightGrad);

        const ceilGrad = ctx.createLinearGradient(0, 0, 0, 152);
        ceilGrad.addColorStop(0, '#E5E0D8');
        ceilGrad.addColorStop(1, '#CBC3B7');
        fillPoly(ctx, [0, 0, 800, 0, 655, 152, 145, 152], ceilGrad);

        const floorGrad = ctx.createLinearGradient(0, 388, 0, 550);
        floorGrad.addColorStop(0, '#5C412E');
        floorGrad.addColorStop(0.55, '#483121');
        floorGrad.addColorStop(1, '#312014');
        fillPoly(ctx, [0, 515, 145, 388, 655, 388, 800, 515, 800, 550, 0, 550], floorGrad);

        ctx.strokeStyle = rgba('#291A10', 0.48);
        ctx.lineWidth = 1.2;
        const seams: Array<[number, number, number, number]> = [
          [210, 388, 70, 550],
          [285, 388, 215, 550],
          [365, 388, 355, 550],
          [445, 388, 495, 550],
          [525, 388, 635, 550],
          [595, 388, 760, 550],
        ];
        for (const [x1, y1, x2, y2] of seams) {
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        }
        ctx.strokeStyle = rgba('#291A10', 0.17);
        ctx.beginPath();
        ctx.moveTo(95, 436);
        ctx.lineTo(705, 436);
        ctx.moveTo(38, 490);
        ctx.lineTo(762, 490);
        ctx.stroke();

        // 2. Trần giật cấp + LED hắt trần + Spotlight + Quạt trần + Đèn chùm
        const trayPoly = [150, 28, 650, 28, 570, 126, 230, 126];
        const trayGrad = ctx.createLinearGradient(0, 28, 0, 126);
        trayGrad.addColorStop(0, '#D7CFC3');
        trayGrad.addColorStop(1, '#EFE7DA');
        fillPoly(ctx, trayPoly, trayGrad, '#BCB3A4', 2);

        if (lf > 0.01) {
          const tg = ctx.createRadialGradient(400, 77, 0, 400, 77, 275);
          tg.addColorStop(0, rgba('#FDE68A', 0.72 * lf));
          tg.addColorStop(0.65, rgba('#F59E0B', 0.22 * lf));
          tg.addColorStop(1, 'rgba(245,158,11,0)');
          fillPoly(ctx, trayPoly, tg);

          tracePoly(ctx, [158, 32, 642, 32, 566, 122, 234, 122]);
          ctx.strokeStyle = rgba('#FDE047', 0.28 * lf);
          ctx.lineWidth = 10;
          ctx.stroke();
          ctx.strokeStyle = rgba('#FDE047', 0.85 * lf);
          ctx.lineWidth = 3.5;
          ctx.stroke();

          const spots: Array<[number, number, number, number]> = [
            [105, 44, 11, 4],
            [695, 44, 11, 4],
            [185, 115, 9, 3.2],
            [615, 115, 9, 3.2],
            [320, 140, 7, 2.5],
            [480, 140, 7, 2.5],
          ];
          for (const [sx, sy, rx, ry] of spots) {
            ctx.fillStyle = rgba('#FEF08A', 0.55 * lf);
            ctx.beginPath();
            ctx.ellipse(sx, sy, rx * 1.4, ry * 1.4, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = rgba('#FFFFFF', 0.95 * lf);
            ctx.beginPath();
            ctx.ellipse(sx, sy, rx * 0.55, ry * 0.55, 0, 0, Math.PI * 2);
            ctx.fill();
          }

          ctx.save();
          ctx.translate(400, 96);
          ctx.scale(1, 52 / 125);
          const chGlow = ctx.createRadialGradient(0, 0, 0, 0, 0, 125);
          chGlow.addColorStop(0, rgba('#FEF08A', 0.9 * lf));
          chGlow.addColorStop(0.45, rgba('#FBBF24', 0.35 * lf));
          chGlow.addColorStop(1, 'rgba(251,191,36,0)');
          ctx.fillStyle = chGlow;
          ctx.beginPath();
          ctx.arc(0, 0, 125, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();
        }

        // Quạt trần 5 cánh
        ctx.fillStyle = '#334155';
        ctx.beginPath();
        ctx.roundRect(396, 30, 8, 22, 2);
        ctx.fill();
        const fanU = 82 / 100;
        ctx.save();
        ctx.translate(400, 53);
        ctx.scale(1, 0.3);
        if (st.fanVelocity > 4.2) {
          ctx.save();
          ctx.rotate(((st.fanAngleDeg - 16) * Math.PI) / 180);
          drawLivingFanBlades(ctx, fanU, 0.16);
          ctx.restore();
        }
        ctx.save();
        ctx.rotate((st.fanAngleDeg * Math.PI) / 180);
        drawLivingFanBlades(ctx, fanU, 1);
        ctx.restore();
        ctx.fillStyle = '#283244';
        ctx.beginPath();
        ctx.arc(0, 0, 18 * fanU, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#64748B';
        ctx.lineWidth = 2 * fanU;
        ctx.stroke();
        ctx.fillStyle = st.fanVelocity > 0.2 ? '#38BDF8' : '#1E293B';
        ctx.beginPath();
        ctx.arc(0, 0, 7 * fanU, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Đèn chùm 2 vòng tròn
        ctx.strokeStyle = '#64748B';
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.moveTo(365, 72);
        ctx.lineTo(352, 96);
        ctx.moveTo(435, 72);
        ctx.lineTo(448, 96);
        ctx.moveTo(400, 62);
        ctx.lineTo(400, 109);
        ctx.stroke();

        const ring1 = lerpHex('#64748B', '#FEF08A', room.lightOn ? 0.4 + 0.6 * lf : 0);
        const ring2 = lerpHex('#64748B', '#FEF9C3', room.lightOn ? 0.4 + 0.6 * lf : 0);
        ctx.strokeStyle = ring1;
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.ellipse(400, 98, 66, 14, 0, 0, Math.PI * 2);
        ctx.stroke();
        ctx.strokeStyle = rgba('#334155', 0.6);
        ctx.lineWidth = 1.4;
        ctx.stroke();

        ctx.strokeStyle = ring2;
        ctx.lineWidth = 3.2;
        ctx.beginPath();
        ctx.ellipse(400, 111, 38, 8, 0, 0, Math.PI * 2);
        ctx.stroke();

        // 3. Cửa sổ kính + bầu trời ngày/đêm + rèm tự động + vệt nắng sàn gỗ
        ctx.fillStyle = '#1E293B';
        ctx.beginPath();
        ctx.roundRect(460, 160, 176, 228, 2);
        ctx.fill();

        ctx.save();
        ctx.beginPath();
        ctx.rect(466, 166, 164, 214);
        ctx.clip();

        const daySky = ctx.createLinearGradient(0, 166, 0, 380);
        daySky.addColorStop(0, '#38BDF8');
        daySky.addColorStop(0.6, '#BAE6FD');
        daySky.addColorStop(1, '#FEF3C7');
        ctx.fillStyle = daySky;
        ctx.fillRect(466, 166, 164, 214);

        if (eva > 0.01) {
          const eveSky = ctx.createLinearGradient(0, 166, 0, 380);
          eveSky.addColorStop(0, rgba('#090F24', eva));
          eveSky.addColorStop(0.55, rgba('#172554', eva));
          eveSky.addColorStop(1, rgba('#1E3A5F', eva));
          ctx.fillStyle = eveSky;
          ctx.fillRect(466, 166, 164, 214);

          ctx.fillStyle = rgba('#FEF9C3', 0.9 * eva);
          ctx.beginPath();
          ctx.arc(585, 204, 11, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = rgba('#111C3D', eva);
          ctx.beginPath();
          ctx.arc(590, 201, 10, 0, Math.PI * 2);
          ctx.fill();

          for (const [sx, sy, sr] of [
            [495, 196, 1.4],
            [525, 186, 1.2],
            [545, 218, 1.3],
            [510, 235, 1.1],
          ]) {
            ctx.fillStyle = rgba('#FFFFFF', 0.8 * eva);
            ctx.beginPath();
            ctx.arc(sx, sy, sr, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        fillPoly(
          ctx,
          [
            466, 380, 466, 315, 484, 315, 484, 295, 505, 295, 505, 322, 522, 322, 522, 276, 548, 276,
            548, 310, 568, 310, 568, 288, 595, 288, 595, 318, 615, 318, 615, 302, 632, 302, 632, 380,
          ],
          rgba('#1E293B', 0.45),
        );

        if (eva > 0.01) {
          ctx.fillStyle = rgba('#FDE047', 0.9 * eva);
          for (const [wx, wy] of [
            [528, 286],
            [537, 294],
            [528, 304],
            [575, 296],
            [584, 305],
            [490, 304],
          ]) {
            ctx.fillRect(wx, wy, 4, 4);
          }
        }

        ctx.fillStyle = '#334155';
        ctx.fillRect(546, 166, 4, 214);
        ctx.fillRect(466, 288, 164, 3.5);

        const scaleX = Math.max(0.16, Math.min(1, 1 - cr * 0.84));
        const lBase = 466;
        const lW = 84 * scaleX;
        ctx.fillStyle = '#475569';
        ctx.fillRect(lBase, 166, lW, 216);
        ctx.fillStyle = rgba('#334155', 0.55);
        ctx.fillRect(lBase + 12 * scaleX, 166, 12 * scaleX, 216);
        ctx.fillRect(lBase + 38 * scaleX, 166, 12 * scaleX, 216);
        ctx.fillStyle = rgba('#64748B', 0.4);
        ctx.fillRect(lBase + 64 * scaleX, 166, 10 * scaleX, 216);

        const rEdge = 630;
        const rLeft = rEdge - 84 * scaleX;
        ctx.fillStyle = '#475569';
        ctx.fillRect(rLeft, 166, 84 * scaleX, 216);
        ctx.fillStyle = rgba('#64748B', 0.4);
        ctx.fillRect(rEdge - 74 * scaleX, 166, 10 * scaleX, 216);
        ctx.fillStyle = rgba('#334155', 0.55);
        ctx.fillRect(rEdge - 50 * scaleX, 166, 12 * scaleX, 216);
        ctx.fillRect(rEdge - 24 * scaleX, 166, 12 * scaleX, 216);
        ctx.restore();

        ctx.fillStyle = '#0F172A';
        ctx.beginPath();
        ctx.roundRect(456, 157, 184, 6, 2);
        ctx.fill();

        const spillFactor = Math.max(0, Math.min(1, (1 - eva) * (cr * 0.95) + eva * 0.05));
        if (spillFactor > 0.01) {
          const sg = ctx.createLinearGradient(600, 388, 330, 520);
          sg.addColorStop(0, rgba('#FEF9C3', 0.42 * spillFactor));
          sg.addColorStop(1, 'rgba(254,249,195,0)');
          fillPoly(ctx, [466, 388, 630, 388, 530, 520, 315, 520], sg);
        }

        // 4. Vách lam gỗ + khung TV tắt + Kệ TV & Soundbar
        ctx.fillStyle = '#3D2618';
        ctx.beginPath();
        ctx.roundRect(166, 162, 268, 226, 2);
        ctx.fill();

        const slatGrad = ctx.createLinearGradient(0, 164, 0, 388);
        slatGrad.addColorStop(0, '#7B5236');
        slatGrad.addColorStop(1, '#4B301E');
        ctx.fillStyle = slatGrad;
        for (let i = 0; i < 14; i++) {
          ctx.fillRect(172 + i * 18, 164, 12, 224);
        }
        ctx.fillRect(422, 164, 8, 224);

        ctx.fillStyle = '#0B0F19';
        ctx.beginPath();
        ctx.roundRect(204, 198, 192, 108, 4);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.fillStyle = '#0F172A';
        ctx.beginPath();
        ctx.roundRect(209, 203, 182, 98, 2);
        ctx.fill();

        ctx.fillStyle = '#2A2E37';
        ctx.beginPath();
        ctx.roundRect(184, 330, 232, 28, 3);
        ctx.fill();
        ctx.strokeStyle = '#475569';
        ctx.lineWidth = 1;
        ctx.stroke();
        ctx.strokeStyle = '#1E293B';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(260, 330);
        ctx.lineTo(260, 358);
        ctx.moveTo(340, 330);
        ctx.lineTo(340, 358);
        ctx.stroke();

        if (lf > 0.01) {
          ctx.fillStyle = rgba('#FDE047', 0.28 * lf);
          ctx.fillRect(190, 357, 220, 9);
          ctx.fillStyle = rgba('#FDE047', 0.85 * lf);
          ctx.fillRect(192, 358, 216, 3);
        }

        ctx.fillStyle = '#111827';
        ctx.beginPath();
        ctx.roundRect(252, 322, 96, 8, 3);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 0.8;
        ctx.stroke();

        // 5. Điều hòa treo tường phải (phần thân & cánh đảo gió)
        fillPoly(ctx, [682, 110, 790, 56, 790, 140, 682, 174], '#E2E8F0', '#94A3B8', 1.2);
        fillPoly(ctx, [682, 174, 790, 140, 778, 154, 674, 183], '#CBD5E1');
        fillPoly(ctx, [686, 174, 780, 144, 776, 152, 682, 180], '#334155');
        const dy = room.acOn ? 5 * aca : 0;
        fillPoly(
          ctx,
          [684, 177 + dy, 778, 147 + dy, 775, 153 + dy + 2 * aca, 681, 182 + dy + 2 * aca],
          '#F8FAFC',
          '#94A3B8',
          0.8,
        );
        fillPoly(ctx, [710, 142, 754, 123, 754, 144, 710, 162], rgba('#0F172A', 0.88));

        // 6. Thảm, cây xanh, đèn cây góc trái, bàn trà và sofa chữ L
        fillPoly(ctx, [150, 510, 275, 412, 585, 412, 675, 515, 225, 538], rgba('#475569', 0.88));
        fillPoly(ctx, [164, 507, 282, 418, 578, 418, 660, 510, 230, 530], rgba('#526075', 0.65));

        fillPoly(ctx, [622, 368, 652, 368, 646, 412, 628, 412], '#CBD5E1', '#94A3B8', 1);
        ctx.fillStyle = '#3F2E21';
        ctx.beginPath();
        ctx.ellipse(637, 368, 15, 4, 0, 0, Math.PI * 2);
        ctx.fill();

        const drawLeaf = (c: string, q1x: number, q1y: number, tx: number, ty: number, q2x: number, q2y: number, ex: number, ey: number) => {
          ctx.beginPath();
          ctx.moveTo(637, 368);
          ctx.quadraticCurveTo(q1x, q1y, tx, ty);
          ctx.quadraticCurveTo(q2x, q2y, ex, ey);
          ctx.closePath();
          ctx.fillStyle = c;
          ctx.fill();
        };
        drawLeaf('#15803D', 620, 335, 608, 325, 628, 322, 637, 355);
        drawLeaf('#16A34A', 655, 332, 668, 318, 650, 315, 638, 350);
        drawLeaf('#22C55E', 634, 322, 637, 298, 648, 318, 640, 358);
        drawLeaf('#166534', 615, 350, 604, 352, 618, 364, 635, 368);

        if (lf > 0.01) {
          const flGlow = ctx.createRadialGradient(105, 285, 0, 105, 285, 76);
          flGlow.addColorStop(0, rgba('#FEF3C7', 0.92 * lf));
          flGlow.addColorStop(0.45, rgba('#F59E0B', 0.38 * lf));
          flGlow.addColorStop(1, 'rgba(245,158,11,0)');
          ctx.fillStyle = flGlow;
          ctx.beginPath();
          ctx.arc(105, 285, 76, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.fillStyle = '#1E293B';
        ctx.beginPath();
        ctx.ellipse(96, 434, 18, 5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 3.5;
        ctx.beginPath();
        ctx.moveTo(96, 432);
        ctx.lineTo(96, 300);
        ctx.stroke();
        fillPoly(
          ctx,
          [82, 262, 118, 262, 126, 304, 74, 304],
          lerpHex('#8C857B', '#FEF3C7', room.lightOn ? 0.35 + 0.65 * lf : 0),
          '#D6C7B2',
          1,
        );

        // Bàn trà mặt đá & bộ ấm trà
        ctx.fillStyle = rgba('#1E293B', 0.45);
        ctx.beginPath();
        ctx.ellipse(380, 472, 68, 20, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(342, 448);
        ctx.lineTo(342, 474);
        ctx.moveTo(418, 448);
        ctx.lineTo(418, 474);
        ctx.stroke();
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.ellipse(380, 446, 66, 21, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#CBD5E1';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.fillStyle = '#475569';
        ctx.beginPath();
        ctx.ellipse(372, 444, 20, 7, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#F8FAFC';
        ctx.beginPath();
        ctx.roundRect(366, 435, 9, 9, 2);
        ctx.fill();

        // Sofa chữ L & gối tựa
        const cushionGrad = ctx.createLinearGradient(0, 390, 0, 535);
        cushionGrad.addColorStop(0, '#6B778C');
        cushionGrad.addColorStop(1, '#475163');
        const bodyGrad = ctx.createLinearGradient(36, 358, 160, 438);
        bodyGrad.addColorStop(0, '#586375');
        bodyGrad.addColorStop(1, '#353E4D');
        fillPoly(ctx, [48, 438, 160, 392, 225, 408, 118, 468], cushionGrad);
        fillPoly(ctx, [36, 396, 154, 358, 160, 392, 48, 438], bodyGrad);
        fillPoly(ctx, [48, 438, 118, 468, 118, 505, 48, 472], '#2A3240');
        fillPoly(ctx, [118, 468, 320, 486, 292, 535, 88, 510], cushionGrad);
        fillPoly(ctx, [88, 510, 292, 535, 292, 550, 88, 550], '#28303E');

        ctx.beginPath();
        ctx.moveTo(78, 412);
        ctx.quadraticCurveTo(96, 395, 118, 408);
        ctx.lineTo(108, 432);
        ctx.quadraticCurveTo(84, 434, 78, 412);
        ctx.closePath();
        ctx.fillStyle = '#3B82F6';
        ctx.fill();

        ctx.beginPath();
        ctx.moveTo(126, 394);
        ctx.quadraticCurveTo(146, 378, 168, 390);
        ctx.lineTo(156, 412);
        ctx.quadraticCurveTo(134, 414, 126, 394);
        ctx.closePath();
        ctx.fillStyle = '#F59E0B';
        ctx.fill();

        // 7. Lớp phủ bóng tối môi trường
        const naturalBoost = isEvening ? 0 : cr * 0.25;
        const darkVal = Math.max(0, Math.min(0.86, (1 - lf) * 0.82 - naturalBoost));
        if (darkVal > 0.01) {
          const dg = ctx.createRadialGradient(800 * 0.55, 550 * 0.42, 0, 800 * 0.55, 550 * 0.42, 800 * 0.78);
          dg.addColorStop(0, rgba('#0D1321', 0.32 * darkVal));
          dg.addColorStop(0.68, rgba('#070B16', 0.78 * darkVal));
          dg.addColorStop(1, rgba('#04070F', 0.92 * darkVal));
          ctx.fillStyle = dg;
          ctx.fillRect(0, 0, 800, 550);
        }

        // 8. Lớp ánh sáng nổi bật (Smart TV Ambilight + Màn hình 4K HDR + LED/gió điều hòa)
        if (tva > 0.01) {
          const glowStrength = tva * (room.lightOn ? 0.55 + 0.15 * (1 - lf) : 0.95);
          ctx.save();
          ctx.translate(300, 252);
          ctx.scale(1, 78 / 136);
          const amb = ctx.createRadialGradient(0, 0, 0, 0, 0, 136);
          amb.addColorStop(0, rgba('#60A5FA', 0.72 * glowStrength));
          amb.addColorStop(0.48, rgba('#818CF8', 0.38 * glowStrength));
          amb.addColorStop(1, 'rgba(129,140,248,0)');
          ctx.fillStyle = amb;
          ctx.beginPath();
          ctx.arc(0, 0, 136, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          ctx.save();
          ctx.globalAlpha = tva;
          ctx.fillStyle = '#0B0F19';
          ctx.beginPath();
          ctx.roundRect(204, 198, 192, 108, 4);
          ctx.fill();
          ctx.strokeStyle = '#334155';
          ctx.lineWidth = 2;
          ctx.stroke();

          const t = (Math.sin(st.animPhase * Math.PI * 2) + 1) * 0.5;
          const tvGrad = ctx.createLinearGradient(209, 203, 391, 301);
          tvGrad.addColorStop(0, lerpHex('#0EA5E9', '#6366F1', t));
          tvGrad.addColorStop(0.5, '#6366F1');
          tvGrad.addColorStop(1, lerpHex('#EC4899', '#14B8A6', t));
          ctx.fillStyle = tvGrad;
          ctx.beginPath();
          ctx.roundRect(209, 203, 182, 98, 2);
          ctx.fill();
          ctx.fillStyle = 'rgba(0,0,0,0.2)';
          ctx.fillRect(209, 203, 182, 98);

          const eqColors = ['#FFFFFF', '#A5F3FC', '#FFFFFF', '#FBCFE8', '#FFFFFF'];
          const eqPhases = [0, 1.3, 2.6, 0.7, 2.0];
          const barW = 7;
          const barGap = 6;
          const startX = 300 - (5 * barW + 4 * barGap) / 2;
          const bottomY = 262;
          const maxH = 34;
          for (let i = 0; i < 5; i++) {
            const hFrac = 0.35 + 0.65 * ((Math.sin(st.animPhase * Math.PI * 4 + eqPhases[i]) + 1) * 0.5);
            const bh = maxH * hFrac;
            ctx.fillStyle = rgba(eqColors[i], 0.92);
            ctx.beginPath();
            ctx.roundRect(startX + i * (barW + barGap), bottomY - bh, barW, bh, barW / 2);
            ctx.fill();
          }

          ctx.fillStyle = 'rgba(255,255,255,0.95)';
          ctx.font = 'bold 10px Be Vietnam Pro, sans-serif';
          ctx.textAlign = 'center';
          ctx.fillText('4K HDR CINEMA', 300, 282);
          ctx.restore();
        }

        ctx.fillStyle = room.tvOn ? '#22C55E' : '#EF4444';
        ctx.beginPath();
        ctx.arc(300, 304, 2.2, 0, Math.PI * 2);
        ctx.fill();

        if (aca > 0.01) {
          const coldFactor = Math.max(0, Math.min(14, 30 - room.acTemp)) / 14;
          const streamAlpha = aca * (0.38 + coldFactor * 0.62);
          const airHex = room.acTemp <= 21 ? '#38BDF8' : room.acTemp <= 25 ? '#7DD3FC' : '#BAE6FD';
          const cycle = 160;
          const streams: Array<[number, number, number, number, number, number]> = [
            [755, 160, 695, 195, 560, 285],
            [735, 174, 670, 222, 525, 328],
            [770, 170, 725, 232, 625, 345],
          ];
          ctx.save();
          ctx.lineCap = 'round';
          streams.forEach(([x0, y0, cx, cy, x1, y1], idx) => {
            const p = (st.animPhase + idx * 0.33) % 1;
            ctx.strokeStyle = rgba(airHex, streamAlpha * 0.85);
            ctx.lineWidth = 2.8 - idx * 0.3;
            ctx.setLineDash([46, 114]);
            ctx.lineDashOffset = -p * cycle;
            ctx.beginPath();
            ctx.moveTo(x0, y0);
            ctx.quadraticCurveTo(cx, cy, x1, y1);
            ctx.stroke();
          });
          ctx.restore();
        }

        ctx.save();
        ctx.translate(732, 143);
        ctx.transform(1, -0.4, 0, 1, 0, 0);
        ctx.font = 'bold 11.5px JetBrains Mono, monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        if (room.acOn) {
          ctx.shadowColor = '#38BDF8';
          ctx.shadowBlur = 6;
          ctx.fillStyle = '#E0F2FE';
        } else {
          ctx.fillStyle = rgba('#E0F2FE', 0.38);
        }
        ctx.fillText(room.acOn ? `${room.acTemp}°C` : 'OFF', 0, 0);
        ctx.restore();
      }}
    >
      {!compact && (
        <div className="pointer-events-none absolute top-2.5 right-2.5 rounded-full border border-[#334155] bg-[#1E293B]/90 px-2.5 py-1 text-[11px] font-medium text-[#E2E8F0]">
          {sceneBadgeText}
        </div>
      )}
    </StageCanvas>
  );
}
