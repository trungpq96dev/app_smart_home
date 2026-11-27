import React, { useRef } from 'react';
import { AlertTriangle } from 'lucide-react';
import { stoveWatts, type RoomUi } from '../../data/models.js';
import { SH } from '../../theme/colors.js';
import { fillPoly, lerpHex, rgba, smoothToward, StageCanvas, tracePoly } from './StageCanvas.js';

const KT_W = 800;
const KT_H = 550;

/**
 * Khung mô phỏng Phòng bếp Vector 2.5D (port 1:1 từ `KitchenStage.kt`).
 */
export function KitchenStage({
  room,
  onSilenceGasAlert,
  fill = false,
  compact = false,
}: {
  room: RoomUi;
  onSilenceGasAlert?: () => void;
  fill?: boolean;
  compact?: boolean;
}) {
  const hoodOn = room.fanOn;
  const hoodSpeed = Math.max(1, Math.min(3, room.hoodSpeed));
  const stoveLevel = Math.max(1, Math.min(9, room.stoveLevel));
  const targetLight = room.lightOn ? Math.max(10, Math.min(100, room.lightBrightness)) / 100 : 0;
  const targetStoveRatio = room.stoveOn ? 0.28 + (stoveLevel / 9) * 0.72 : 0;

  const animRef = useRef({
    lightFactor: targetLight,
    hoodAlpha: hoodOn ? 1 : 0,
    stoveRatio: targetStoveRatio,
    hoodPhase: 0,
    steamPhase: 0,
    alertPulseT: 0,
  });

  const watts = room.stoveOn ? stoveWatts(stoveLevel) : 0;
  const stateBadgeText = room.gasAlert
    ? '🚨 Đang xử lý thông gió khẩn cấp'
    : room.stoveOn
      ? `🍳 Đang nấu ăn (${watts}W)`
      : !room.lightOn
        ? '🌙 Chế độ nghỉ / Tiết kiệm điện'
        : '✨ Không gian bếp sẵn sàng';

  return (
    <StageCanvas
      logicalWidth={KT_W}
      logicalHeight={KT_H}
      fill={fill}
      borderless={compact}
      bgColor="#090E1A"
      className={!compact && room.gasAlert ? '!border-[#EF4444] !border-2' : ''}
      draw={({ ctx, dtSec }) => {
        const st = animRef.current;
        st.lightFactor = smoothToward(st.lightFactor, targetLight, dtSec, 0.6);
        st.hoodAlpha = smoothToward(st.hoodAlpha, hoodOn ? 1 : 0, dtSec, 0.45);
        st.stoveRatio = smoothToward(st.stoveRatio, targetStoveRatio, dtSec, 0.45);

        if (hoodOn) {
          const periodSec = hoodSpeed === 1 ? 2.1 : hoodSpeed === 2 ? 1.35 : 0.8;
          st.hoodPhase = (st.hoodPhase + dtSec / periodSec) % 1;
        }
        if (room.stoveOn) {
          const steamSec = hoodOn
            ? Math.max(0.85, 2.2 - hoodSpeed * 0.42)
            : Math.max(1.3, 3.2 - stoveLevel * 0.19);
          st.steamPhase = (st.steamPhase + dtSec / steamSec) % 1;
        }
        if (room.gasAlert) {
          st.alertPulseT = (st.alertPulseT + dtSec / 1.1) % 1;
        } else {
          st.alertPulseT = 0;
        }
        const alertPulse = room.gasAlert ? (Math.sin(st.alertPulseT * Math.PI * 2) + 1) * 0.5 : 0;

        const lf = st.lightFactor;
        const ha = st.hoodAlpha;
        const sr = st.stoveRatio;

        // 1. Khung phòng bếp phối cảnh 2.5D
        const backGrad = ctx.createLinearGradient(0, 132, 0, 392);
        backGrad.addColorStop(0, '#D6D1C9');
        backGrad.addColorStop(1, '#BEB7AC');
        fillPoly(ctx, [105, 132, 695, 132, 695, 392, 105, 392], backGrad);

        const leftGrad = ctx.createLinearGradient(0, 0, 105, 110);
        leftGrad.addColorStop(0, '#B0A89D');
        leftGrad.addColorStop(1, '#CFC8BD');
        fillPoly(ctx, [0, 0, 105, 132, 105, 392, 0, 515], leftGrad);

        const rightGrad = ctx.createLinearGradient(800, 0, 695, 110);
        rightGrad.addColorStop(0, '#B0A89D');
        rightGrad.addColorStop(1, '#CFC8BD');
        fillPoly(ctx, [800, 0, 695, 132, 695, 392, 800, 515], rightGrad);

        const ceilGrad = ctx.createLinearGradient(0, 0, 0, 132);
        ceilGrad.addColorStop(0, '#E4DFD7');
        ceilGrad.addColorStop(1, '#C9C1B5');
        fillPoly(ctx, [0, 0, 800, 0, 695, 132, 105, 132], ceilGrad);

        const floorGrad = ctx.createLinearGradient(0, 392, 0, 550);
        floorGrad.addColorStop(0, '#4A4E57');
        floorGrad.addColorStop(0.55, '#373B44');
        floorGrad.addColorStop(1, '#252932');
        fillPoly(ctx, [0, 515, 105, 392, 695, 392, 800, 515, 800, 550, 0, 550], floorGrad);

        ctx.strokeStyle = rgba('#1E222B', 0.5);
        ctx.lineWidth = 1.2;
        for (const [x1, y1, x2, y2] of [
          [205, 392, 90, 550],
          [335, 392, 290, 550],
          [465, 392, 510, 550],
          [595, 392, 710, 550],
          [65, 445, 735, 445],
          [15, 505, 785, 505],
        ]) {
          ctx.beginPath();
          ctx.moveTo(x1, y1);
          ctx.lineTo(x2, y2);
          ctx.stroke();
        }

        // 2. Trần thạch cao giật cấp & đèn LED trần
        const trayPoly = [125, 22, 675, 22, 605, 108, 195, 108];
        const trayGrad = ctx.createLinearGradient(0, 22, 0, 108);
        trayGrad.addColorStop(0, '#D7CFC2');
        trayGrad.addColorStop(1, '#F0E8DA');
        fillPoly(ctx, trayPoly, trayGrad, '#BDB4A7', 1.8);

        if (lf > 0.01) {
          const tg = ctx.createRadialGradient(400, 65, 0, 400, 65, 300);
          tg.addColorStop(0, rgba('#FDE68A', 0.72 * lf));
          tg.addColorStop(0.65, rgba('#F59E0B', 0.22 * lf));
          tg.addColorStop(1, 'rgba(245,158,11,0)');
          fillPoly(ctx, trayPoly, tg);

          tracePoly(ctx, [132, 26, 668, 26, 600, 104, 200, 104]);
          ctx.strokeStyle = rgba('#FDE047', 0.3 * lf);
          ctx.lineWidth = 10;
          ctx.stroke();
          ctx.strokeStyle = rgba('#FDE047', 0.88 * lf);
          ctx.lineWidth = 3.5;
          ctx.stroke();

          for (const [cx, cy, rx, ry] of [
            [90, 38, 10, 3.8],
            [710, 38, 10, 3.8],
            [220, 118, 8, 3],
            [580, 118, 8, 3],
          ]) {
            ctx.fillStyle = rgba('#FEF08A', 0.58 * lf);
            ctx.beginPath();
            ctx.ellipse(cx, cy, rx * 1.4, ry * 1.4, 0, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = rgba('#FFFFFF', 0.95 * lf);
            ctx.beginPath();
            ctx.ellipse(cx, cy, rx * 0.55, ry * 0.55, 0, 0, Math.PI * 2);
            ctx.fill();
          }

          for (const px of [290, 510]) {
            ctx.save();
            ctx.translate(px, 155);
            ctx.scale(1, 62 / 72);
            const pg = ctx.createRadialGradient(0, -17, 0, 0, -17, 72);
            pg.addColorStop(0, rgba('#FEF08A', 0.92 * lf));
            pg.addColorStop(0.45, rgba('#FBBF24', 0.32 * lf));
            pg.addColorStop(1, 'rgba(251,191,36,0)');
            ctx.fillStyle = pg;
            ctx.beginPath();
            ctx.arc(0, 0, 72, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
          }
        }

        // 3. Cảm biến khói / gas gắn trần (thân thiết bị)
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.ellipse(628, 74, 22, 7.5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#94A3B8';
        ctx.lineWidth = 1.2;
        ctx.stroke();
        ctx.fillStyle = '#CBD5E1';
        ctx.beginPath();
        ctx.ellipse(628, 77, 16, 5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = rgba('#475569', 0.6);
        ctx.beginPath();
        ctx.ellipse(628, 78, 9, 2.5, 0, 0, Math.PI * 2);
        ctx.fill();

        // 4. Hệ tủ bếp gỗ óc chó + đá Marble + LED gầm tủ + bồn rửa
        const marbleGrad = ctx.createLinearGradient(115, 232, 535, 324);
        marbleGrad.addColorStop(0, '#F8FAFC');
        marbleGrad.addColorStop(0.5, '#E2E8F0');
        marbleGrad.addColorStop(1, '#CBD5E1');
        ctx.fillStyle = marbleGrad;
        ctx.fillRect(115, 232, 420, 92);

        ctx.strokeStyle = rgba('#94A3B8', 0.38);
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(135, 235);
        ctx.quadraticCurveTo(180, 275, 235, 295);
        ctx.quadraticCurveTo(298, 277, 360, 260);
        ctx.quadraticCurveTo(438, 285, 515, 310);
        ctx.stroke();
        ctx.strokeStyle = rgba('#94A3B8', 0.3);
        ctx.lineWidth = 0.9;
        ctx.beginPath();
        ctx.moveTo(180, 320);
        ctx.quadraticCurveTo(260, 285, 330, 242);
        ctx.stroke();

        const walnutCab = ctx.createLinearGradient(0, 138, 0, 232);
        walnutCab.addColorStop(0, '#6F4B32');
        walnutCab.addColorStop(1, '#442C1B');
        ctx.fillStyle = walnutCab;
        ctx.fillRect(115, 138, 152, 94);
        ctx.fillRect(425, 138, 110, 94);
        ctx.strokeStyle = '#2E1C11';
        ctx.lineWidth = 1.5;
        ctx.strokeRect(115, 138, 152, 94);
        ctx.strokeRect(425, 138, 110, 94);
        ctx.beginPath();
        ctx.moveTo(191, 138);
        ctx.lineTo(191, 232);
        ctx.moveTo(480, 138);
        ctx.lineTo(480, 232);
        ctx.stroke();

        if (lf > 0.01) {
          const beamGrad = ctx.createLinearGradient(0, 236, 0, 290);
          beamGrad.addColorStop(0, rgba('#FEF08A', 0.88 * lf));
          beamGrad.addColorStop(0.4, rgba('#FDE047', 0.28 * lf));
          beamGrad.addColorStop(1, 'rgba(253,224,71,0)');
          ctx.fillStyle = rgba('#FEF08A', lf);
          ctx.fillRect(118, 232, 146, 4);
          ctx.fillRect(428, 232, 104, 4);
          fillPoly(ctx, [118, 236, 264, 236, 270, 290, 115, 290], beamGrad);
          fillPoly(ctx, [428, 236, 532, 236, 535, 290, 422, 290], beamGrad);
        }

        const ctGrad = ctx.createLinearGradient(94, 324, 552, 340);
        ctGrad.addColorStop(0, '#F8FAFC');
        ctGrad.addColorStop(0.5, '#E2E8F0');
        ctGrad.addColorStop(1, '#CBD5E1');
        fillPoly(ctx, [105, 324, 542, 324, 552, 340, 94, 340], ctGrad, '#94A3B8', 1);

        const lowerGrad = ctx.createLinearGradient(105, 340, 542, 408);
        lowerGrad.addColorStop(0, '#5B3C27');
        lowerGrad.addColorStop(1, '#342013');
        ctx.fillStyle = lowerGrad;
        ctx.fillRect(105, 340, 437, 68);
        ctx.strokeStyle = '#24150C';
        ctx.lineWidth = 1.5;
        ctx.strokeRect(105, 340, 437, 68);
        ctx.beginPath();
        for (const dx of [192, 278, 414]) {
          ctx.moveTo(dx, 340);
          ctx.lineTo(dx, 408);
        }
        ctx.stroke();

        ctx.fillStyle = '#1E293B';
        for (const [hx, hw] of [
          [140, 24],
          [224, 24],
          [334, 26],
          [464, 26],
        ]) {
          ctx.beginPath();
          ctx.roundRect(hx, 348, hw, 3, 1.5);
          ctx.fill();
        }

        // Bồn rửa chén & vòi nước inox
        ctx.fillStyle = '#94A3B8';
        ctx.beginPath();
        ctx.ellipse(185, 331, 34, 5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#64748B';
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.save();
        ctx.strokeStyle = '#CBD5E1';
        ctx.lineWidth = 3.2;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(185, 326);
        ctx.lineTo(185, 302);
        ctx.quadraticCurveTo(185, 290, 198, 292);
        ctx.quadraticCurveTo(206, 294, 206, 304);
        ctx.stroke();
        ctx.restore();

        // 5. Máy hút mùi chữ T đen nhám + đèn chiếu vùng nấu
        const hoodGrad = ctx.createLinearGradient(266, 0, 426, 0);
        hoodGrad.addColorStop(0, '#1E293B');
        hoodGrad.addColorStop(0.5, '#334155');
        hoodGrad.addColorStop(1, '#0F172A');
        ctx.fillStyle = hoodGrad;
        ctx.fillRect(321, 132, 50, 86);
        ctx.strokeStyle = '#0F172A';
        ctx.lineWidth = 1;
        ctx.strokeRect(321, 132, 50, 86);

        fillPoly(ctx, [274, 216, 418, 216, 426, 232, 266, 232], hoodGrad, '#475569', 1);
        ctx.fillStyle = '#0F172A';
        ctx.beginPath();
        ctx.roundRect(322, 222, 48, 6, 2);
        ctx.fill();
        [332, 346, 360].forEach((lx, idx) => {
          const lit = hoodOn && hoodSpeed >= idx + 1;
          ctx.fillStyle = lit ? '#38BDF8' : '#475569';
          ctx.beginPath();
          ctx.arc(lx, 225, 1.6, 0, Math.PI * 2);
          ctx.fill();
        });

        if (ha > 0.01) {
          ctx.fillStyle = rgba('#E0F2FE', ha);
          ctx.beginPath();
          ctx.roundRect(292, 232, 18, 3, 1);
          ctx.roundRect(382, 232, 18, 3, 1);
          ctx.fill();

          const hLightGrad = ctx.createLinearGradient(0, 235, 0, 334);
          hLightGrad.addColorStop(0, rgba('#E0F2FE', 0.85 * ha));
          hLightGrad.addColorStop(0.55, rgba('#BAE6FD', 0.28 * ha));
          hLightGrad.addColorStop(1, 'rgba(186,230,253,0)');
          fillPoly(ctx, [282, 235, 410, 235, 442, 334, 250, 334], hLightGrad);
        }

        // 6. Bếp từ âm kính + nồi inox (phần thân)
        fillPoly(ctx, [282, 325, 412, 325, 420, 338, 274, 338], '#0F172A', '#334155', 1.2);

        ctx.strokeStyle = '#94A3B8';
        ctx.lineWidth = 2.2;
        ctx.beginPath();
        ctx.moveTo(322, 308);
        ctx.lineTo(314, 308);
        ctx.quadraticCurveTo(312, 312, 315, 315);
        ctx.lineTo(322, 315);
        ctx.moveTo(370, 308);
        ctx.lineTo(378, 308);
        ctx.quadraticCurveTo(380, 312, 377, 315);
        ctx.lineTo(370, 315);
        ctx.stroke();

        const potGrad = ctx.createLinearGradient(322, 0, 370, 0);
        potGrad.addColorStop(0, '#64748B');
        potGrad.addColorStop(0.35, '#E2E8F0');
        potGrad.addColorStop(0.7, '#94A3B8');
        potGrad.addColorStop(1, '#475569');
        ctx.fillStyle = potGrad;
        ctx.beginPath();
        ctx.roundRect(322, 303, 48, 27, 4);
        ctx.fill();
        ctx.strokeStyle = '#475569';
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = '#CBD5E1';
        ctx.beginPath();
        ctx.ellipse(346, 303, 24, 4.5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#64748B';
        ctx.lineWidth = 1;
        ctx.stroke();
        ctx.fillStyle = '#334155';
        ctx.beginPath();
        ctx.roundRect(342, 296, 8, 5, 2);
        ctx.fill();

        // 7. Tủ lạnh thông minh 2 cánh
        const fridgeGrad = ctx.createLinearGradient(556, 150, 680, 200);
        fridgeGrad.addColorStop(0, '#475569');
        fridgeGrad.addColorStop(0.5, '#334155');
        fridgeGrad.addColorStop(1, '#1E293B');
        ctx.fillStyle = fridgeGrad;
        ctx.beginPath();
        ctx.roundRect(556, 150, 124, 258, 6);
        ctx.fill();
        ctx.strokeStyle = '#0F172A';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(618, 150);
        ctx.lineTo(618, 408);
        ctx.stroke();

        ctx.fillStyle = '#94A3B8';
        ctx.beginPath();
        ctx.roundRect(610, 220, 3, 64, 1.5);
        ctx.roundRect(623, 220, 3, 64, 1.5);
        ctx.fill();

        ctx.fillStyle = '#0F172A';
        ctx.beginPath();
        ctx.roundRect(572, 218, 28, 44, 3);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 1;
        ctx.stroke();

        // 8. Đảo bếp tiền cảnh & 2 đèn thả
        const bulbColor = lerpHex('#475569', '#FEF08A', room.lightOn ? 0.4 + 0.6 * lf : 0);
        for (const px of [290, 510]) {
          ctx.strokeStyle = '#334155';
          ctx.lineWidth = 1.6;
          ctx.beginPath();
          ctx.moveTo(px, 54);
          ctx.lineTo(px, 128);
          ctx.stroke();

          ctx.beginPath();
          ctx.moveTo(px - 16, 128);
          ctx.quadraticCurveTo(px, 118, px + 16, 128);
          ctx.lineTo(px + 22, 144);
          ctx.lineTo(px - 22, 144);
          ctx.closePath();
          ctx.fillStyle = '#1E293B';
          ctx.fill();
          ctx.strokeStyle = '#475569';
          ctx.lineWidth = 1;
          ctx.stroke();

          ctx.fillStyle = bulbColor;
          ctx.beginPath();
          ctx.ellipse(px, 144, 18, 4, 0, 0, Math.PI * 2);
          ctx.fill();
        }

        const islTop = ctx.createLinearGradient(122, 432, 638, 460);
        islTop.addColorStop(0, '#F8FAFC');
        islTop.addColorStop(0.5, '#E2E8F0');
        islTop.addColorStop(1, '#CBD5E1');
        fillPoly(ctx, [150, 432, 610, 432, 638, 460, 122, 460], islTop, '#94A3B8', 1.2);

        const islBody = ctx.createLinearGradient(135, 460, 625, 550);
        islBody.addColorStop(0, '#5B3C27');
        islBody.addColorStop(1, '#342013');
        ctx.fillStyle = islBody;
        ctx.fillRect(135, 460, 490, 90);
        ctx.strokeStyle = '#24150C';
        ctx.lineWidth = 1.5;
        ctx.strokeRect(135, 460, 490, 90);

        ctx.strokeStyle = rgba('#29170C', 0.65);
        ctx.lineWidth = 2;
        ctx.beginPath();
        for (let x = 175; x <= 575; x += 40) {
          ctx.moveTo(x, 460);
          ctx.lineTo(x, 550);
        }
        ctx.stroke();

        ctx.fillStyle = '#CBD5E1';
        ctx.beginPath();
        ctx.ellipse(465, 446, 28, 8, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#94A3B8';
        ctx.lineWidth = 1;
        ctx.stroke();
        ctx.fillStyle = '#22C55E';
        ctx.beginPath();
        ctx.arc(456, 442, 6, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#F97316';
        ctx.beginPath();
        ctx.arc(468, 440, 6.5, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#EAB308';
        ctx.beginPath();
        ctx.arc(476, 444, 5.5, 0, Math.PI * 2);
        ctx.fill();

        // 9. Lớp phủ bóng tối môi trường
        const hoodLightOffset = hoodOn ? 0.08 * ha : 0;
        const darkVal = Math.max(0, Math.min(0.88, (1 - lf) * 0.86 - hoodLightOffset));
        if (darkVal > 0.01) {
          const dg = ctx.createRadialGradient(400, 550 * 0.44, 0, 400, 550 * 0.44, 800 * 0.75);
          dg.addColorStop(0, rgba('#0B1120', 0.25 * darkVal));
          dg.addColorStop(0.68, rgba('#070B16', 0.78 * darkVal));
          dg.addColorStop(1, rgba('#03060F', 0.94 * darkVal));
          ctx.fillStyle = dg;
          ctx.fillRect(0, 0, 800, 550);
        }

        // 10. Lớp phát sáng nổi bật trên bóng tối
        if (sr > 0.01) {
          ctx.save();
          ctx.translate(346, 331);
          ctx.scale(1, 12 / 42);
          const sg = ctx.createRadialGradient(0, 0, 0, 0, 0, 42);
          sg.addColorStop(0, rgba('#FFF7ED', 0.95 * sr));
          sg.addColorStop(0.3, rgba('#FB923C', 0.92 * sr));
          sg.addColorStop(0.62, rgba('#EF4444', 0.72 * sr));
          sg.addColorStop(1, 'rgba(239,68,68,0)');
          ctx.fillStyle = sg;
          ctx.beginPath();
          ctx.arc(0, 0, 42, 0, Math.PI * 2);
          ctx.fill();
          ctx.restore();

          ctx.strokeStyle = rgba('#FB923C', 0.95 * sr);
          ctx.lineWidth = 2;
          ctx.beginPath();
          ctx.ellipse(346, 331, 24, 4.2, 0, 0, Math.PI * 2);
          ctx.stroke();
        }

        ctx.fillStyle = rgba('#F97316', room.stoveOn ? 1 : 0.35);
        ctx.font = 'bold 9px JetBrains Mono, monospace';
        ctx.textAlign = 'left';
        ctx.fillText(room.stoveOn ? `P${stoveLevel}` : '--', 396, 335);

        if (room.stoveOn && sr > 0.05) {
          const baseAlpha = (0.35 + (stoveLevel / 9) * 0.65) * sr;
          const plumes: Array<[number, number, string, number]> = [
            [336, 0, '#F8FAFC', 3.2],
            [348, 0.27, '#FFFFFF', 3.6],
            [358, 0.52, '#E2E8F0', 2.8],
          ];
          ctx.save();
          ctx.lineCap = 'round';
          for (const [baseX, offset, hex, w] of plumes) {
            const t = (st.steamPhase + offset) % 1;
            const a =
              (t < 0.22
                ? (t / 0.22) * 0.75
                : t < 0.75
                  ? 0.75 - ((t - 0.22) / 0.53) * 0.33
                  : ((1 - t) / 0.25) * 0.42) * baseAlpha;
            if (a > 0.01) {
              const dy = 12 - t * 88;
              const sway = Math.sin(t * Math.PI * 2) * 4;
              ctx.beginPath();
              ctx.moveTo(baseX, 296 + dy);
              ctx.quadraticCurveTo(baseX - 8 + sway, 277 + dy, baseX + 2 - sway, 242 + dy);
              ctx.strokeStyle = rgba(hex, a * 0.35);
              ctx.lineWidth = w * 2.2;
              ctx.stroke();
              ctx.strokeStyle = rgba(hex, a * 0.72);
              ctx.lineWidth = w;
              ctx.stroke();
            }
          }
          ctx.restore();
        }

        if (ha > 0.01) {
          const flowAlpha = (0.45 + hoodSpeed * 0.18) * ha;
          const cycle = 160;
          const curves: Array<[number, number, number, number, number, number]> = [
            [312, 316, 304, 282, 322, 237],
            [346, 318, 346, 278, 346, 236],
            [380, 316, 388, 282, 370, 237],
          ];
          ctx.save();
          ctx.lineCap = 'round';
          ctx.setLineDash([42, 118]);
          curves.forEach(([x0, y0, cx, cy, x1, y1], idx) => {
            const p = (st.hoodPhase + idx * 0.25) % 1;
            ctx.strokeStyle = rgba(idx === 1 ? '#38BDF8' : '#7DD3FC', flowAlpha);
            ctx.lineWidth = idx === 1 ? 2.8 : 2.4;
            ctx.lineDashOffset = -p * cycle;
            ctx.beginPath();
            ctx.moveTo(x0, y0);
            ctx.quadraticCurveTo(cx, cy, x1, y1);
            ctx.stroke();
          });
          ctx.restore();
        }

        // Màn hình Smart Display tủ lạnh
        ctx.fillStyle = rgba('#0B1120', 0.95);
        ctx.beginPath();
        ctx.roundRect(632, 198, 36, 54, 4);
        ctx.fill();
        ctx.strokeStyle = '#38BDF8';
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = '#38BDF8';
        ctx.font = 'bold 11px Be Vietnam Pro, sans-serif';
        ctx.textAlign = 'center';
        ctx.fillText('4°C', 650, 217);
        ctx.strokeStyle = '#1E293B';
        ctx.beginPath();
        ctx.moveTo(638, 225);
        ctx.lineTo(662, 225);
        ctx.stroke();
        ctx.fillStyle = '#94A3B8';
        ctx.font = 'bold 8.5px Be Vietnam Pro, sans-serif';
        ctx.fillText('-18°C', 650, 239);
        ctx.fillStyle = '#22C55E';
        ctx.beginPath();
        ctx.arc(650, 247, 1.8, 0, Math.PI * 2);
        ctx.fill();

        // Đèn LED cảm biến Gas trần
        const ledHex = room.gasAlert ? '#EF4444' : '#22C55E';
        const haloAlpha = room.gasAlert ? 0.45 + 0.45 * alertPulse : 0.25;
        const haloR = room.gasAlert ? 10 + 5 * alertPulse : 9;
        const dotR = room.gasAlert ? 3.2 + 1.2 * alertPulse : 3.4;
        const dotAlpha = room.gasAlert ? 0.45 + 0.55 * alertPulse : 1;
        ctx.fillStyle = rgba(ledHex, haloAlpha);
        ctx.beginPath();
        ctx.arc(628, 80, haloR, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = rgba(ledHex, dotAlpha);
        ctx.beginPath();
        ctx.arc(628, 80, dotR, 0, Math.PI * 2);
        ctx.fill();

        if (room.gasAlert) {
          const edgeAlpha = 0.2 + 0.26 * alertPulse;
          const eg = ctx.createRadialGradient(400, 275, 0, 400, 275, 800 * 0.72);
          eg.addColorStop(0.55, 'rgba(239,68,68,0)');
          eg.addColorStop(1, rgba('#EF4444', edgeAlpha));
          ctx.fillStyle = eg;
          ctx.fillRect(0, 0, 800, 550);
        }
      }}
    >
      {!compact && (
        <div className="pointer-events-none absolute inset-x-0 top-0 p-2 flex flex-col gap-1.5">
          <div className="flex flex-wrap items-center justify-between gap-1.5">
            <div className="flex flex-wrap items-center gap-1.5">
              <span className="rounded-full border border-[#334155]/80 bg-[#0F172A]/85 px-2.5 py-1 text-[11px] font-medium text-[#E2E8F0]">
                {stateBadgeText}
              </span>
              {hoodOn && (
                <span className="rounded-full border border-[#38BDF8]/40 bg-[#0C2440]/85 px-2.5 py-1 text-[11px] font-medium text-[#7DD3FC]">
                  💨 Hút mùi mức {hoodSpeed}
                </span>
              )}
            </div>

            <span
              className="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-[11px] font-semibold"
              style={{
                backgroundColor: room.gasAlert ? 'rgba(127, 29, 29, 0.9)' : 'rgba(6, 44, 34, 0.85)',
                border: `1px solid ${room.gasAlert ? SH.Red : rgba(SH.Green, 0.5)}`,
                color: room.gasAlert ? '#FCA5A5' : '#6EE7B7',
              }}
            >
              <span
                className="h-1.5 w-1.5 rounded-full"
                style={{ backgroundColor: room.gasAlert ? SH.Red : SH.Green }}
              />
              <span>
                {room.gasAlert ? `CẢNH BÁO: ${room.gasPpm} ppm` : `Gas/Khói: ${room.gasPpm} ppm`}
              </span>
            </span>
          </div>

          {room.gasAlert && (
            <div className="pointer-events-auto flex items-center gap-2 rounded-xl border border-[#FCA5A5]/60 bg-[#DC2626]/95 px-2.5 py-1.5 shadow-lg">
              <div className="flex h-6 w-6 shrink-0 items-center justify-center rounded-lg bg-white/20 text-white">
                <AlertTriangle size={14} />
              </div>
              <div className="min-w-0 flex-1">
                <div className="text-xs font-bold text-white truncate">
                  Cảnh báo rò rỉ khí Gas / Khói cao!
                </div>
                <div className="text-[10px] text-[#FEE2E2] truncate">
                  Đã tự động kích hoạt Máy hút mùi mức 3 (Mạnh) để thông gió.
                </div>
              </div>
              {onSilenceGasAlert && (
                <button
                  type="button"
                  onClick={onSilenceGasAlert}
                  className="shrink-0 rounded-lg bg-white px-2.5 py-1 text-[11px] font-bold text-[#DC2626] hover:bg-[#FEE2E2] cursor-pointer"
                >
                  Tắt cảnh báo
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </StageCanvas>
  );
}
