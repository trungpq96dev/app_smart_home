import React, { useRef } from 'react';
import type { RoomUi } from '../../data/models.js';
import { SH, tint } from '../../theme/colors.js';
import { StageCanvas } from './StageCanvas.js';

export function LivingRoomStage({ room, fill = false }: { room: RoomUi; fill?: boolean }) {
  const animRef = useRef({
    fanAngle: 0,
    fanVel: 0,
    curtainVis: room.curtainOpen / 100,
    eqBars: [0.4, 0.7, 0.55, 0.85, 0.6],
  });

  return (
    <StageCanvas
      logicalWidth={800}
      logicalHeight={550}
      fill={fill}
      draw={({ ctx, tSec, dtSec }) => {
        const st = animRef.current;
        // Smooth fan inertia
        const targetVel = room.fanOn ? room.fanSpeed * 3.8 : 0;
        st.fanVel += (targetVel - st.fanVel) * Math.min(1, dtSec * 3.2);
        st.fanAngle = (st.fanAngle + st.fanVel * dtSec) % (Math.PI * 2);

        // Smooth curtain fraction (0 closed, 1 open)
        const targetCurtain = room.curtainOpen / 100;
        st.curtainVis += (targetCurtain - st.curtainVis) * Math.min(1, dtSec * 5);

        const lightFrac = room.lightOn ? room.lightBrightness / 100 : 0;

        // 1. Background walls & floor perspective
        const wallGrad = ctx.createLinearGradient(0, 0, 0, 380);
        wallGrad.addColorStop(0, lightFrac > 0 ? '#182442' : '#0B1220');
        wallGrad.addColorStop(1, lightFrac > 0 ? '#131D36' : '#0D1526');
        ctx.fillStyle = wallGrad;
        ctx.fillRect(0, 0, 800, 550);

        // Left perspective wall
        ctx.fillStyle = lightFrac > 0 ? '#14203C' : '#0A101E';
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(190, 82);
        ctx.lineTo(190, 390);
        ctx.lineTo(0, 515);
        ctx.closePath();
        ctx.fill();

        // Back wall
        ctx.fillStyle = lightFrac > 0 ? '#192746' : '#0E172A';
        ctx.fillRect(190, 82, 610, 308);

        // Ceiling strip
        ctx.fillStyle = lightFrac > 0 ? '#121C35' : '#090F1C';
        ctx.beginPath();
        ctx.moveTo(0, 0);
        ctx.lineTo(800, 0);
        ctx.lineTo(800, 82);
        ctx.lineTo(190, 82);
        ctx.closePath();
        ctx.fill();

        // Warm hardwood floor
        const floorGrad = ctx.createLinearGradient(0, 390, 0, 550);
        floorGrad.addColorStop(0, lightFrac > 0 ? '#231D29' : '#14131E');
        floorGrad.addColorStop(1, lightFrac > 0 ? '#191522' : '#0C0E18');
        ctx.fillStyle = floorGrad;
        ctx.beginPath();
        ctx.moveTo(190, 390);
        ctx.lineTo(800, 390);
        ctx.lineTo(800, 550);
        ctx.lineTo(0, 550);
        ctx.lineTo(0, 515);
        ctx.closePath();
        ctx.fill();

        // Floor perspective lines
        ctx.strokeStyle = 'rgba(255,255,255,0.04)';
        ctx.lineWidth = 1;
        for (let i = 1; i <= 5; i++) {
          const y = 390 + i * 28;
          ctx.beginPath();
          ctx.moveTo(190 - i * 32, y);
          ctx.lineTo(800, y);
          ctx.stroke();
        }

        // Wall edges
        ctx.strokeStyle = '#253459';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(190, 82);
        ctx.lineTo(190, 390);
        ctx.lineTo(800, 390);
        ctx.moveTo(190, 390);
        ctx.lineTo(0, 515);
        ctx.stroke();

        // 2. Left perspective window & sky + animated curtains
        ctx.save();
        ctx.beginPath();
        ctx.moveTo(28, 108);
        ctx.lineTo(155, 138);
        ctx.lineTo(155, 340);
        ctx.lineTo(28, 395);
        ctx.closePath();
        ctx.clip();

        // Sky inside window (daylight hint via curtain openness + stars)
        const skyGrad = ctx.createLinearGradient(0, 108, 0, 395);
        skyGrad.addColorStop(0, '#0D2248');
        skyGrad.addColorStop(1, '#1A3A6E');
        ctx.fillStyle = skyGrad;
        ctx.fillRect(25, 105, 135, 295);

        // City skyline silhouettes
        ctx.fillStyle = '#09152E';
        ctx.fillRect(36, 265, 24, 130);
        ctx.fillRect(66, 235, 28, 160);
        ctx.fillRect(102, 250, 26, 145);
        ctx.fillStyle = tint(SH.Amber, 0.65);
        ctx.fillRect(72, 248, 5, 6);
        ctx.fillRect(82, 266, 5, 6);
        ctx.fillRect(109, 268, 5, 6);

        // Soft moon
        ctx.fillStyle = '#FDE68A';
        ctx.beginPath();
        ctx.arc(124, 175, 12, 0, Math.PI * 2);
        ctx.fill();

        // Dual sliding curtains covering 0..(1 - curtainVis)
        const coverFrac = 1 - st.curtainVis;
        const winW = 127;
        const leftPanelW = (winW * 0.5) * Math.max(0.12, coverFrac);
        ctx.fillStyle = '#31416B';
        ctx.fillRect(28, 108, leftPanelW, 290);
        ctx.fillRect(155 - leftPanelW, 108, leftPanelW, 290);
        ctx.strokeStyle = 'rgba(255,255,255,0.08)';
        for (let f = 1; f < 4; f++) {
          const lx = 28 + (leftPanelW * f) / 4;
          ctx.beginPath();
          ctx.moveTo(lx, 112);
          ctx.lineTo(lx, 392);
          ctx.stroke();
          const rx = 155 - leftPanelW + (leftPanelW * f) / 4;
          ctx.beginPath();
          ctx.moveTo(rx, 112);
          ctx.lineTo(rx, 392);
          ctx.stroke();
        }
        ctx.restore();

        // Window frame
        ctx.strokeStyle = '#33487A';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(28, 108);
        ctx.lineTo(155, 138);
        ctx.lineTo(155, 340);
        ctx.lineTo(28, 395);
        ctx.closePath();
        ctx.stroke();

        // Sunlight spill onto floor when curtains open
        if (st.curtainVis > 0.1) {
          ctx.fillStyle = `rgba(96, 165, 250, ${0.06 * st.curtainVis})`;
          ctx.beginPath();
          ctx.moveTo(28, 395);
          ctx.lineTo(155, 340);
          ctx.lineTo(280, 460);
          ctx.lineTo(95, 530);
          ctx.closePath();
          ctx.fill();
        }

        // 3. Ceiling Light Glow & Fixture
        if (lightFrac > 0) {
          const glow = ctx.createRadialGradient(485, 65, 10, 485, 180, 320);
          glow.addColorStop(0, `rgba(253, 224, 71, ${0.26 * lightFrac})`);
          glow.addColorStop(0.5, `rgba(251, 191, 36, ${0.10 * lightFrac})`);
          glow.addColorStop(1, 'rgba(251, 191, 36, 0)');
          ctx.fillStyle = glow;
          ctx.fillRect(150, 0, 650, 480);
        }
        // Chandelier disc
        ctx.fillStyle = lightFrac > 0 ? '#FBBF24' : '#334155';
        ctx.beginPath();
        ctx.ellipse(485, 42, 46, 11, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = lightFrac > 0 ? '#FEF08A' : '#475569';
        ctx.lineWidth = 2;
        ctx.stroke();

        // 4. Air Conditioner (back wall right)
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.roundRect(585, 112, 165, 46, 10);
        ctx.fill();
        ctx.fillStyle = '#CBD5E1';
        ctx.fillRect(595, 146, 145, 7);

        // AC LED Display skewed in 2.5D
        ctx.save();
        ctx.translate(715, 131);
        ctx.transform(1, -0.4 * (1 / 4), 0, 1, 0, 0);
        ctx.fillStyle = room.acOn ? '#0F172A' : '#334155';
        ctx.fillRect(-18, -9, 34, 16);
        if (room.acOn) {
          ctx.fillStyle = '#38BDF8';
          ctx.font = 'bold 11px JetBrains Mono, monospace';
          ctx.textAlign = 'center';
          ctx.textBaseline = 'middle';
          ctx.fillText(`${room.acTemp}°`, -1, 0);
        }
        ctx.restore();

        // Animated AC cool airflow ribbons
        if (room.acOn) {
          for (let i = 0; i < 3; i++) {
            const prog = ((tSec * 0.9 + i * 0.33) % 1);
            const alpha = Math.sin(prog * Math.PI) * 0.55;
            ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`;
            ctx.lineWidth = 2.2;
            ctx.beginPath();
            const startY = 158 + prog * 70;
            ctx.moveTo(615 + i * 38, startY);
            ctx.quadraticCurveTo(605 + i * 38 + Math.sin(tSec * 4 + i) * 10, startY + 22, 625 + i * 38, startY + 42);
            ctx.stroke();
          }
        }

        // 5. Smart TV + Cabinet (back wall center)
        // Media console
        ctx.fillStyle = '#2B213A';
        ctx.beginPath();
        ctx.roundRect(275, 342, 260, 48, 6);
        ctx.fill();
        ctx.strokeStyle = '#3F3256';
        ctx.lineWidth = 1;
        ctx.stroke();

        // TV Stand + Bezel
        ctx.fillStyle = '#0F172A';
        ctx.fillRect(396, 326, 18, 16);
        ctx.fillRect(370, 338, 70, 4);
        ctx.fillStyle = '#090D16';
        ctx.beginPath();
        ctx.roundRect(295, 198, 220, 128, 8);
        ctx.fill();
        ctx.strokeStyle = room.tvOn ? '#3B82F6' : '#334155';
        ctx.lineWidth = 2.5;
        ctx.stroke();

        // TV Screen content
        if (room.tvOn) {
          const tvGrad = ctx.createLinearGradient(303, 206, 507, 318);
          tvGrad.addColorStop(0, '#1D4ED8');
          tvGrad.addColorStop(0.5, '#7C3AED');
          tvGrad.addColorStop(1, '#0EA5E9');
          ctx.fillStyle = tvGrad;
          ctx.fillRect(303, 206, 204, 112);

          // Animated equalizer bars
          for (let b = 0; b < 5; b++) {
            const val = 0.25 + 0.65 * Math.abs(Math.sin(tSec * 3.8 + b * 1.1));
            const bh = val * 44;
            ctx.fillStyle = 'rgba(255,255,255,0.85)';
            ctx.fillRect(375 + b * 13, 290 - bh, 7, bh);
          }
          ctx.fillStyle = '#FFFFFF';
          ctx.font = 'bold 11px sans-serif';
          ctx.textAlign = 'left';
          ctx.fillText('4K HDR · Smart TV', 316, 226);

          // Screen glow on wall
          ctx.fillStyle = 'rgba(59, 130, 246, 0.14)';
          ctx.beginPath();
          ctx.ellipse(405, 262, 135, 85, 0, 0, Math.PI * 2);
          ctx.fill();
        } else {
          ctx.fillStyle = '#0F172A';
          ctx.fillRect(303, 206, 204, 112);
        }

        // 6. Rug & Sofa (foreground right)
        ctx.fillStyle = '#1E293B';
        ctx.beginPath();
        ctx.ellipse(420, 480, 175, 42, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Modern Sofa
        ctx.fillStyle = '#253559';
        ctx.beginPath();
        ctx.roundRect(515, 385, 215, 76, 14);
        ctx.fill();
        ctx.fillStyle = '#2E416B';
        ctx.beginPath();
        ctx.roundRect(528, 415, 190, 44, 10);
        ctx.fill();
        // Pillows
        ctx.fillStyle = SH.Amber;
        ctx.beginPath();
        ctx.roundRect(542, 398, 28, 26, 6);
        ctx.fill();
        ctx.fillStyle = SH.Teal;
        ctx.beginPath();
        ctx.roundRect(680, 398, 28, 26, 6);
        ctx.fill();

        // 7. Standing Smart Fan (left foreground) with inertia rotation
        ctx.strokeStyle = '#475569';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(212, 465);
        ctx.lineTo(212, 338);
        ctx.stroke();
        ctx.fillStyle = '#334155';
        ctx.beginPath();
        ctx.ellipse(212, 468, 28, 8, 0, 0, Math.PI * 2);
        ctx.fill();

        // Fan cage
        ctx.save();
        ctx.translate(212, 315);
        ctx.strokeStyle = room.fanOn ? '#38BDF8' : '#475569';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(0, 0, 42, 0, Math.PI * 2);
        ctx.stroke();

        // Rotating 5 blades
        ctx.rotate(st.fanAngle);
        for (let b = 0; b < 5; b++) {
          ctx.rotate((Math.PI * 2) / 5);
          ctx.fillStyle = room.fanOn ? 'rgba(56, 189, 248, 0.72)' : 'rgba(148, 163, 184, 0.45)';
          ctx.beginPath();
          ctx.ellipse(18, 0, 16, 6, 0.25, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.arc(0, 0, 6, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // HUD badges in top corners
        ctx.fillStyle = 'rgba(14, 22, 39, 0.78)';
        ctx.beginPath();
        ctx.roundRect(16, 14, 220, 30, 8);
        ctx.fill();
        ctx.fillStyle = '#E6EDF7';
        ctx.font = '600 12px Be Vietnam Pro, sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(
          `${room.name} · Đèn ${room.lightOn ? `${room.lightBrightness}%` : 'tắt'} · Rèm ${room.curtainOpen}%`,
          26,
          33,
        );
      }}
    />
  );
}

