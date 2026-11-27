import React, { useRef } from 'react';
import { Moon, Sun } from 'lucide-react';
import type { RoomUi } from '../../data/models.js';
import { SH } from '../../theme/colors.js';
import { fillPoly, lerpHex, rgba, smoothToward, StageCanvas, tracePoly } from './StageCanvas.js';

const REF_W = 800;
const REF_H = 600;

const BACK_WALL = [180, 185, 620, 185, 620, 425, 180, 425];
const LEFT_WALL = [0, 0, 180, 185, 180, 425, 0, 565];
const RIGHT_WALL = [800, 0, 620, 185, 620, 425, 800, 565];
const CEILING_POLY = [0, 0, 800, 0, 620, 185, 180, 185];
const FLOOR_POLY = [0, 565, 180, 425, 620, 425, 800, 565, 800, 600, 0, 600];

const RECESS_POLY = [155, 34, 645, 34, 555, 148, 245, 148];
const RECESS_LED_POLY = [162, 38, 638, 38, 552, 145, 248, 145];

const CURTAIN_LEFT = [418, 185, 466, 185, 462, 424, 415, 424];
const CURTAIN_LEFT_FOLD = [428, 185, 438, 185, 436, 424, 426, 424];
const CURTAIN_RIGHT = [572, 185, 620, 185, 622, 424, 576, 424];
const CURTAIN_RIGHT_FOLD = [590, 185, 600, 185, 602, 424, 592, 424];

const WARDROBE_BODY = [318, 186, 415, 186, 415, 424, 318, 424];
const VANITY_DESK = [350, 364, 432, 364, 432, 376, 350, 376];
const CHAIR_SEAT = [418, 388, 450, 388, 446, 424, 422, 424];
const CHAIR_BACK = [444, 358, 454, 358, 450, 392, 440, 392];

const DOOR_OUTER = [0, 122, 44, 160, 44, 532, 0, 566];
const DOOR_INNER = [6, 144, 36, 170, 36, 515, 6, 542];

const RUG_OUTER = [105, 535, 280, 432, 595, 432, 695, 555, 210, 595];
const RUG_INNER = [116, 533, 284, 438, 588, 438, 682, 550, 214, 586];

const HEADBOARD_HALO = [62, 340, 324, 300, 324, 358, 62, 415];
const HEADBOARD_GLOW = [66, 356, 320, 315, 320, 356, 66, 412];
const HEADBOARD_WOOD = [64, 398, 322, 340, 322, 438, 64, 515];

const NIGHTSTAND_FAR = [274, 396, 324, 385, 324, 435, 274, 448];
const LAMP_SHADE_FAR = [288, 358, 308, 358, 312, 379, 284, 379];

const DUVET_POLY = [145, 456, 322, 394, 562, 424, 398, 538];
const BED_SIDE_LEFT = [145, 456, 398, 538, 398, 566, 145, 482];
const BED_SIDE_RIGHT = [398, 538, 562, 424, 562, 456, 398, 566];
const THROW_NAVY = [278, 498, 444, 410, 554, 424, 398, 536];

const NIGHTSTAND_NEAR_TOP = [74, 458, 142, 438, 184, 454, 114, 478];
const NIGHTSTAND_NEAR_LEFT = [74, 458, 114, 478, 114, 548, 74, 522];
const NIGHTSTAND_NEAR_FRONT = [114, 478, 184, 454, 184, 522, 114, 548];
const LAMP_SHADE_NEAR = [104, 398, 140, 398, 146, 434, 98, 434];

const AC_BODY = [682, 115, 790, 62, 790, 148, 682, 184];
const AC_BOTTOM = [682, 184, 790, 148, 776, 162, 674, 192];
const AC_VENT = [686, 184, 780, 152, 775, 160, 682, 189];
const AC_LED_BEZEL = [708, 149, 752, 130, 752, 152, 708, 170];

function drawDownlight(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  haloRx: number,
  haloRy: number,
  coreRx: number,
  coreRy: number,
  lightFactor: number,
) {
  ctx.save();
  ctx.translate(cx, cy);
  ctx.scale(1, Math.max(0.35, haloRy / haloRx));
  const r = haloRx * 2.2;
  const g = ctx.createRadialGradient(0, 0, 0, 0, 0, r);
  g.addColorStop(0, rgba('#FEF9C3', 0.85 * lightFactor));
  g.addColorStop(0.5, rgba('#FEF08A', 0.32 * lightFactor));
  g.addColorStop(1, 'rgba(254,240,138,0)');
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.arc(0, 0, r, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();

  ctx.fillStyle = rgba('#FFFFFF', lightFactor);
  ctx.beginPath();
  ctx.ellipse(cx, cy, coreRx, coreRy, 0, 0, Math.PI * 2);
  ctx.fill();
}

function drawPillow(
  ctx: CanvasRenderingContext2D,
  x0: number,
  y0: number,
  cx1: number,
  cy1: number,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
  cx2: number,
  cy2: number,
  color: string,
) {
  ctx.beginPath();
  ctx.moveTo(x0, y0);
  ctx.quadraticCurveTo(cx1, cy1, x1, y1);
  ctx.lineTo(x2, y2);
  ctx.quadraticCurveTo(cx2, cy2, x0, y0);
  ctx.closePath();
  ctx.fillStyle = color;
  ctx.fill();
}

function drawFanBlades(ctx: CanvasRenderingContext2D, u: number, alpha: number) {
  for (let i = 0; i < 5; i++) {
    ctx.save();
    ctx.rotate((i * 72 * Math.PI) / 180);
    const bladeGrad = ctx.createLinearGradient(-12 * u, -92 * u, 12 * u, 0);
    bladeGrad.addColorStop(0, rgba('#4A362B', alpha));
    bladeGrad.addColorStop(0.5, rgba('#2F2119', alpha));
    bladeGrad.addColorStop(1, rgba('#1E140F', alpha));
    ctx.beginPath();
    ctx.moveTo(-4 * u, -8 * u);
    ctx.lineTo(-10 * u, -86 * u);
    ctx.quadraticCurveTo(0, -94 * u, 10 * u, -86 * u);
    ctx.lineTo(4 * u, -8 * u);
    ctx.closePath();
    ctx.fillStyle = bladeGrad;
    ctx.fill();
    if (alpha >= 0.9) {
      ctx.strokeStyle = rgba('#170F0B', alpha);
      ctx.lineWidth = u;
      ctx.stroke();
    }
    ctx.restore();
  }
}

/**
 * Khung mô phỏng Phòng ngủ Vector 2.5D (port 1:1 từ `MoPhongPhongNgu.kt`).
 */
export function BedroomStage({
  room,
  fill = false,
  compact = false,
}: {
  room: RoomUi;
  fill?: boolean;
  compact?: boolean;
}) {
  const isNightSky = room.mode === 'SLEEP' || !room.lightOn;
  const effectiveBrightness =
    room.mode === 'SLEEP'
      ? Math.min(Math.max(10, Math.min(100, room.lightBrightness)), 20)
      : Math.max(10, Math.min(100, room.lightBrightness));
  const targetLight = room.lightOn ? effectiveBrightness / 100 : 0;

  const animRef = useRef({
    lightFactor: targetLight,
    nightFactor: isNightSky ? 1 : 0,
    acFactor: room.acOn ? 1 : 0,
    fanAngleDeg: 0,
    fanVelocity: room.fanOn ? (room.fanSpeed === 1 ? 4.5 : room.fanSpeed === 2 ? 8.5 : 14) : 0,
    airPhase: 0,
  });

  return (
    <StageCanvas
      logicalWidth={REF_W}
      logicalHeight={REF_H}
      fill={fill}
      borderless={compact}
      bgColor="#0D1322"
      draw={({ ctx, dtSec }) => {
        const st = animRef.current;
        st.lightFactor = smoothToward(st.lightFactor, targetLight, dtSec, 0.6);
        st.nightFactor = smoothToward(st.nightFactor, isNightSky ? 1 : 0, dtSec, 0.6);
        st.acFactor = smoothToward(st.acFactor, room.acOn ? 1 : 0, dtSec, 0.5);

        const speedClamped = Math.max(1, Math.min(3, room.fanSpeed));
        const targetVel = room.fanOn
          ? speedClamped === 1
            ? 4.5
            : speedClamped === 2
              ? 8.5
              : 14.0
          : 0;
        const dtFrames = dtSec * 60;
        const accel = room.fanOn ? 0.045 : 0.022;
        st.fanVelocity += (targetVel - st.fanVelocity) * accel * dtFrames;
        if (!room.fanOn && st.fanVelocity <= 0.05) {
          st.fanVelocity = 0;
        } else {
          st.fanAngleDeg = (st.fanAngleDeg + st.fanVelocity * dtFrames) % 360;
        }

        if (room.acOn) {
          st.airPhase = (st.airPhase + 0.014 * dtFrames) % 1;
        }

        const lf = st.lightFactor;
        const nf = st.nightFactor;
        const af = st.acFactor;

        // 1. KHUNG PHÒNG PHỐI CẢNH 1 ĐIỂM TỤ
        const backGrad = ctx.createLinearGradient(0, 185, 0, 425);
        backGrad.addColorStop(0, '#D4CBC0');
        backGrad.addColorStop(1, '#BEB4A7');
        fillPoly(ctx, BACK_WALL, backGrad);

        const leftGrad = ctx.createLinearGradient(0, 0, 180, 226);
        leftGrad.addColorStop(0, '#C8BFB2');
        leftGrad.addColorStop(1, '#DDD5C8');
        fillPoly(ctx, LEFT_WALL, leftGrad);

        const rightGrad = ctx.createLinearGradient(800, 0, 620, 170);
        rightGrad.addColorStop(0, '#B9B0A4');
        rightGrad.addColorStop(1, '#D7CFC2');
        fillPoly(ctx, RIGHT_WALL, rightGrad);

        const ceilGrad = ctx.createLinearGradient(0, 0, 0, 185);
        ceilGrad.addColorStop(0, '#E7E0D5');
        ceilGrad.addColorStop(1, '#CFC6B8');
        fillPoly(ctx, CEILING_POLY, ceilGrad);

        const floorGrad = ctx.createLinearGradient(0, 425, 0, 600);
        floorGrad.addColorStop(0, '#4A3323');
        floorGrad.addColorStop(0.55, '#3B271A');
        floorGrad.addColorStop(1, '#291A11');
        fillPoly(ctx, FLOOR_POLY, floorGrad);

        ctx.strokeStyle = rgba('#22150D', 0.55);
        ctx.lineWidth = 1.2;
        const plankLines: Array<[number, number]> = [
          [230, 90],
          [295, 220],
          [365, 350],
          [435, 480],
          [505, 610],
          [570, 740],
        ];
        for (const [xTop, xBot] of plankLines) {
          ctx.beginPath();
          ctx.moveTo(xTop, 425);
          ctx.lineTo(xBot, 600);
          ctx.stroke();
        }
        ctx.strokeStyle = rgba('#22150D', 0.22);
        ctx.beginPath();
        ctx.moveTo(120, 472);
        ctx.lineTo(680, 472);
        ctx.moveTo(55, 525);
        ctx.lineTo(745, 525);
        ctx.stroke();

        // 2. TRẦN GIẬT CẤP & ĐÈN LED HẮT TRẦN
        const recessGrad = ctx.createLinearGradient(0, 34, 0, 148);
        recessGrad.addColorStop(0, '#D9CEBE');
        recessGrad.addColorStop(1, '#F1E7D6');
        fillPoly(ctx, RECESS_POLY, recessGrad, '#BDB2A3', 2);

        if (lf > 0.005) {
          ctx.save();
          tracePoly(ctx, RECESS_POLY);
          ctx.clip();
          ctx.translate(400, 91);
          ctx.scale(1, 0.28);
          const cg = ctx.createRadialGradient(0, 0, 0, 0, 0, 270);
          cg.addColorStop(0, rgba('#FDE68A', 0.75 * lf));
          cg.addColorStop(0.6, rgba('#F59E0B', 0.25 * lf));
          cg.addColorStop(1, 'rgba(245,158,11,0)');
          ctx.fillStyle = cg;
          ctx.beginPath();
          ctx.arc(0, 0, 270, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          tracePoly(ctx, RECESS_LED_POLY);
          ctx.strokeStyle = rgba('#FDE047', 0.24 * lf);
          ctx.lineWidth = 12;
          ctx.stroke();
          ctx.strokeStyle = rgba('#FDE047', 0.48 * lf);
          ctx.lineWidth = 6.5;
          ctx.stroke();
          ctx.strokeStyle = rgba('#FEF08A', 0.9 * lf);
          ctx.lineWidth = 3;
          ctx.stroke();

          drawDownlight(ctx, 115, 48, 10, 4, 6, 2.4, lf);
          drawDownlight(ctx, 685, 48, 10, 4, 6, 2.4, lf);
          drawDownlight(ctx, 185, 128, 8, 3.2, 4.8, 1.8, lf);
          drawDownlight(ctx, 615, 128, 8, 3.2, 4.8, 1.8, lf);

          ctx.fillStyle = rgba('#FEF08A', lf);
          ctx.beginPath();
          ctx.ellipse(320, 166, 6, 2.2, 0, 0, Math.PI * 2);
          ctx.ellipse(480, 166, 6, 2.2, 0, 0, Math.PI * 2);
          ctx.fill();
        }

        // 3. CỬA SỔ LỚN & RÈM 2 LỚP (NGÀY / ĐÊM)
        ctx.fillStyle = '#27303F';
        ctx.beginPath();
        ctx.roundRect(445, 198, 150, 205, 2);
        ctx.fill();

        const daySky = ctx.createLinearGradient(0, 206, 0, 395);
        daySky.addColorStop(0, '#7DD3FC');
        daySky.addColorStop(0.55, '#BAE6FD');
        daySky.addColorStop(1, '#FEF3C7');
        ctx.fillStyle = daySky;
        ctx.fillRect(453, 206, 134, 189);

        if (nf > 0.005) {
          const nightSky = ctx.createLinearGradient(0, 206, 0, 395);
          nightSky.addColorStop(0, rgba('#090F24', nf));
          nightSky.addColorStop(0.6, rgba('#172554', nf));
          nightSky.addColorStop(1, rgba('#1E3A5F', nf));
          ctx.fillStyle = nightSky;
          ctx.fillRect(453, 206, 134, 189);

          ctx.fillStyle = rgba('#FEF9C3', 0.9 * nf);
          ctx.beginPath();
          ctx.arc(552, 242, 11, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = rgba('#0E1736', nf);
          ctx.beginPath();
          ctx.arc(557, 239, 10, 0, Math.PI * 2);
          ctx.fill();

          const stars: Array<[number, number, number, number]> = [
            [485, 232, 1.4, 0.8],
            [515, 222, 1.1, 0.7],
            [530, 260, 1.3, 0.75],
            [496, 274, 1.1, 0.6],
          ];
          for (const [sx, sy, sr, sa] of stars) {
            ctx.fillStyle = rgba('#FFFFFF', sa * nf);
            ctx.beginPath();
            ctx.arc(sx, sy, sr, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        ctx.fillStyle = '#334155';
        ctx.fillRect(517, 206, 5, 189);
        ctx.fillRect(453, 305, 134, 4);

        ctx.fillStyle = 'rgba(255,255,255,0.32)';
        ctx.fillRect(453, 194, 38, 226);
        ctx.fillRect(548, 194, 39, 226);

        fillPoly(ctx, CURTAIN_LEFT, '#464A52');
        fillPoly(ctx, CURTAIN_LEFT_FOLD, rgba('#373A40', 0.65));
        fillPoly(ctx, CURTAIN_RIGHT, '#464A52');
        fillPoly(ctx, CURTAIN_RIGHT_FOLD, rgba('#373A40', 0.65));

        // 4. TỦ QUẦN ÁO GỖ & BÀN TRANG ĐIỂM
        const woodDark = ctx.createLinearGradient(318, 186, 415, 424);
        woodDark.addColorStop(0, '#6B4830');
        woodDark.addColorStop(1, '#3E2718');
        fillPoly(ctx, WARDROBE_BODY, woodDark);

        ctx.fillStyle = '#1E1815';
        ctx.fillRect(358, 242, 48, 118);
        ctx.strokeStyle = '#573926';
        ctx.lineWidth = 2;
        ctx.strokeRect(358, 242, 48, 118);

        if (lf > 0.005) {
          ctx.fillStyle = rgba('#FDE047', 0.75 * lf);
          ctx.fillRect(362, 245, 40, 3);
        }

        const vanityGrad = ctx.createLinearGradient(0, 364, 0, 376);
        vanityGrad.addColorStop(0, '#7D563B');
        vanityGrad.addColorStop(1, '#4E3320');
        fillPoly(ctx, VANITY_DESK, vanityGrad);
        fillPoly(ctx, CHAIR_SEAT, '#9CA3AF');
        fillPoly(ctx, CHAIR_BACK, '#B6BCC6');

        // 5. CỬA RA VÀO TƯỜNG TRÁI
        fillPoly(ctx, DOOR_OUTER, '#4A2F1D', '#2D1B0F', 3);
        fillPoly(ctx, DOOR_INNER, '#5A3924');
        ctx.fillStyle = '#CBD5E1';
        ctx.beginPath();
        ctx.roundRect(26, 365, 12, 4, 2);
        ctx.fill();

        // 6. THẢM TRẢI SÀN XÁM
        fillPoly(ctx, RUG_OUTER, rgba('#4B4F57', 0.92));
        fillPoly(ctx, RUG_INNER, rgba('#575B64', 0.65));

        // 7. GIƯỜNG NGỦ, VÁCH ĐẦU GIƯỜNG & 2 ĐÈN NGỦ
        if (lf > 0.005) {
          const haloGrad = ctx.createLinearGradient(196, 382, 180, 305);
          haloGrad.addColorStop(0, rgba('#FBBF24', 0.3 * lf));
          haloGrad.addColorStop(1, 'rgba(251,191,36,0)');
          fillPoly(ctx, HEADBOARD_HALO, haloGrad);

          const glowGrad = ctx.createLinearGradient(196, 382, 184, 312);
          glowGrad.addColorStop(0, rgba('#FBBF24', 0.65 * lf));
          glowGrad.addColorStop(1, 'rgba(251,191,36,0)');
          fillPoly(ctx, HEADBOARD_GLOW, glowGrad);
        }

        const hbWood = ctx.createLinearGradient(0, 340, 0, 515);
        hbWood.addColorStop(0, '#7D563B');
        hbWood.addColorStop(1, '#4E3320');
        fillPoly(ctx, HEADBOARD_WOOD, hbWood, '#3B2516', 1.5);

        const nsFar = ctx.createLinearGradient(274, 385, 324, 448);
        nsFar.addColorStop(0, '#6B4830');
        nsFar.addColorStop(1, '#3E2718');
        fillPoly(ctx, NIGHTSTAND_FAR, nsFar);

        const shadeColor = room.lightOn ? lerpHex('#D6C7AE', '#FEF3C7', lf) : '#8C857B';
        if (lf > 0.005) {
          const farGlow = ctx.createRadialGradient(298, 368, 0, 298, 368, 40);
          farGlow.addColorStop(0, rgba('#FEF08A', 0.95 * lf));
          farGlow.addColorStop(0.35, rgba('#FBBF24', 0.45 * lf));
          farGlow.addColorStop(1, 'rgba(251,191,36,0)');
          ctx.fillStyle = farGlow;
          ctx.beginPath();
          ctx.arc(298, 368, 40, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.fillStyle = '#A1887F';
        ctx.fillRect(295, 378, 6, 14);
        fillPoly(ctx, LAMP_SHADE_FAR, shadeColor);

        const duvetGrad = ctx.createLinearGradient(145, 394, 562, 538);
        duvetGrad.addColorStop(0, '#6B707A');
        duvetGrad.addColorStop(1, '#3F444E');
        fillPoly(ctx, DUVET_POLY, duvetGrad);
        fillPoly(ctx, BED_SIDE_LEFT, '#3B2618');
        fillPoly(ctx, BED_SIDE_RIGHT, '#2E1D12');

        const throwGrad = ctx.createLinearGradient(278, 410, 554, 536);
        throwGrad.addColorStop(0, '#1E3A6E');
        throwGrad.addColorStop(1, '#0F1F40');
        fillPoly(ctx, THROW_NAVY, throwGrad);

        drawPillow(ctx, 168, 426, 192, 402, 228, 412, 212, 442, 178, 448, '#8B919C');
        drawPillow(ctx, 225, 406, 252, 385, 288, 394, 272, 424, 238, 428, '#8B919C');
        drawPillow(ctx, 196, 438, 224, 416, 254, 426, 236, 456, 206, 460, '#1E3A6E');
        drawPillow(ctx, 250, 418, 276, 400, 304, 408, 288, 436, 260, 440, '#1E3A6E');

        fillPoly(ctx, NIGHTSTAND_NEAR_TOP, '#8A6144');
        fillPoly(ctx, NIGHTSTAND_NEAR_LEFT, '#523623');
        fillPoly(ctx, NIGHTSTAND_NEAR_FRONT, '#3F2819');

        if (lf > 0.005) {
          const nearGlow = ctx.createRadialGradient(122, 414, 0, 122, 414, 60);
          nearGlow.addColorStop(0, rgba('#FEF08A', 0.95 * lf));
          nearGlow.addColorStop(0.35, rgba('#FBBF24', 0.45 * lf));
          nearGlow.addColorStop(1, 'rgba(251,191,36,0)');
          ctx.fillStyle = nearGlow;
          ctx.beginPath();
          ctx.arc(122, 414, 60, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.fillStyle = '#8D7B68';
        ctx.beginPath();
        ctx.roundRect(117, 432, 10, 24, 2);
        ctx.fill();
        fillPoly(ctx, LAMP_SHADE_NEAR, shadeColor, '#E5D3B3', 1);

        // 8. CAMERA AN NINH & MÁY LẠNH TREO TƯỜNG PHẢI
        ctx.fillStyle = rgba('#CBD5E1', 0.85);
        ctx.beginPath();
        ctx.ellipse(78, 52, 14, 8, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = rgba('#1E293B', 0.85);
        ctx.beginPath();
        ctx.arc(78, 55, 7, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = rgba('#38BDF8', 0.8);
        ctx.beginPath();
        ctx.arc(76, 56, 2.5, 0, Math.PI * 2);
        ctx.fill();

        fillPoly(ctx, AC_BODY, '#E2E8F0', '#94A3B8', 1.2);
        fillPoly(ctx, AC_BOTTOM, '#CBD5E1');
        fillPoly(ctx, AC_VENT, '#334155');

        const openDrop = 5.5 * af;
        const openShift = -3.5 * af;
        fillPoly(
          ctx,
          [
            684 + openShift,
            187 + openDrop,
            778 + openShift,
            155 + openDrop,
            775 + openShift,
            161 + openDrop,
            681 + openShift,
            192 + openDrop,
          ],
          '#F8FAFC',
          '#94A3B8',
          0.8,
        );
        fillPoly(ctx, AC_LED_BEZEL, rgba('#0F172A', 0.88));

        // 9. LỚP PHỦ BÓNG TỐI MÔI TRƯỜNG
        const rawDarkness = isNightSky ? (1 - lf) * 0.85 + 0.12 : (1 - lf) * 0.75;
        const darknessOpacity = Math.max(0, Math.min(0.88, rawDarkness));
        if (darknessOpacity > 0.01) {
          const dg = ctx.createRadialGradient(800 * 0.68, 600 * 0.42, 0, 800 * 0.68, 600 * 0.42, 800 * 0.85);
          dg.addColorStop(0, `rgba(15, 23, 42, ${(0.45 * darknessOpacity).toFixed(3)})`);
          dg.addColorStop(0.72, `rgba(6, 10, 22, ${(0.84 * darknessOpacity).toFixed(3)})`);
          dg.addColorStop(1, `rgba(3, 6, 15, ${(0.92 * darknessOpacity).toFixed(3)})`);
          ctx.fillStyle = dg;
          ctx.fillRect(0, 0, 800, 600);
        }

        // 10. QUẠT TRẦN 5 CÁNH 3D, LUỒNG GIÓ ĐIỀU HÒA & MÀN HÌNH LED (TRÊN LỚP TỐI)
        const rodGrad = ctx.createLinearGradient(394.5, 0, 405.5, 0);
        rodGrad.addColorStop(0, '#1E1B18');
        rodGrad.addColorStop(0.5, '#4A403A');
        rodGrad.addColorStop(1, '#1E1B18');
        ctx.fillStyle = rodGrad;
        ctx.beginPath();
        ctx.roundRect(394.5, 90, 11, 52, 2);
        ctx.fill();

        const u = 136 / 100;
        ctx.save();
        ctx.translate(400, 141);
        ctx.scale(1, 0.34);
        if (st.fanVelocity > 4.5) {
          ctx.save();
          ctx.rotate(((st.fanAngleDeg - 14) * Math.PI) / 180);
          drawFanBlades(ctx, u, 0.16);
          ctx.restore();
        }
        ctx.save();
        ctx.rotate((st.fanAngleDeg * Math.PI) / 180);
        drawFanBlades(ctx, u, 1);
        ctx.restore();

        ctx.fillStyle = '#2A201B';
        ctx.beginPath();
        ctx.arc(0, 0, 18 * u, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#4D3D35';
        ctx.lineWidth = 2 * u;
        ctx.stroke();
        ctx.fillStyle = '#18120F';
        ctx.beginPath();
        ctx.arc(0, 0, 8 * u, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        if (af > 0.01) {
          const coldIntensity = Math.max(0, Math.min(14, 30 - room.acTemp)) / 14;
          const airAlpha = (0.35 + coldIntensity * 0.65) * af;
          const airHex = room.acTemp <= 21 ? '#38BDF8' : room.acTemp <= 25 ? '#7DD3FC' : '#BAE6FD';
          const cycle = 180;

          ctx.save();
          ctx.lineCap = 'round';

          ctx.strokeStyle = rgba(airHex, 0.85 * airAlpha);
          ctx.lineWidth = 3;
          ctx.setLineDash([55, 125]);
          ctx.lineDashOffset = -st.airPhase * cycle;
          ctx.beginPath();
          ctx.moveTo(740, 187);
          ctx.bezierCurveTo(683, 217, 638, 260, 570, 314);
          ctx.stroke();

          ctx.strokeStyle = rgba(airHex, 0.72 * airAlpha);
          ctx.lineWidth = 2.5;
          ctx.setLineDash([45, 135]);
          ctx.lineDashOffset = -((st.airPhase + 0.33) % 1) * cycle;
          ctx.beginPath();
          ctx.moveTo(723, 203);
          ctx.bezierCurveTo(666, 244, 621, 292, 547, 346);
          ctx.stroke();

          ctx.strokeStyle = rgba(airHex, 0.65 * airAlpha);
          ctx.lineWidth = 2.2;
          ctx.setLineDash([50, 130]);
          ctx.lineDashOffset = -((st.airPhase + 0.66) % 1) * cycle;
          ctx.beginPath();
          ctx.moveTo(751, 198);
          ctx.bezierCurveTo(706, 252, 672, 303, 621, 368);
          ctx.stroke();
          ctx.restore();
        }

        ctx.save();
        ctx.translate(730, 151);
        ctx.transform(1, -0.4, 0, 1, 0, 0);
        ctx.font = 'bold 12px JetBrains Mono, monospace';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        if (room.acOn) {
          ctx.shadowColor = '#38BDF8';
          ctx.shadowBlur = 6;
          ctx.fillStyle = '#E0F2FE';
        } else {
          ctx.fillStyle = rgba('#E0F2FE', 0.35);
        }
        ctx.fillText(room.acOn ? `${room.acTemp}°C` : 'OFF', 0, 0);
        ctx.restore();
      }}
    >
      {!compact && (
        <div className="pointer-events-none absolute top-2 right-2 inline-flex items-center gap-1.5 rounded-full border border-[#94A3B8]/25 bg-[#0F172A]/80 px-2.5 py-1 text-[11px] font-medium text-[#E6EDF7]">
          {isNightSky ? (
            <Moon size={12} className="text-[#FEF08A]" />
          ) : (
            <Sun size={12} style={{ color: SH.Amber }} />
          )}
          <span>{isNightSky ? 'Ban đêm' : 'Ban ngày'}</span>
        </div>
      )}
    </StageCanvas>
  );
}
