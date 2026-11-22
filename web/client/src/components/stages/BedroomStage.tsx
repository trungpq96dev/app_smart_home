import React, { useRef } from 'react';
import type { RoomUi } from '../../data/models.js';
import { SH } from '../../theme/colors.js';
import { StageCanvas } from './StageCanvas.js';

export function BedroomStage({ room, fill = false }: { room: RoomUi; fill?: boolean }) {
  const animRef = useRef({
    fanAngle: 0,
    fanVel: 0,
  });

  return (
    <StageCanvas
      logicalWidth={800}
      logicalHeight={600}
      fill={fill}
      draw={({ ctx, tSec, dtSec }) => {
        const st = animRef.current;
        const targetVel = room.fanOn ? room.fanSpeed * 4.0 : 0;
        st.fanVel += (targetVel - st.fanVel) * Math.min(1, dtSec * 3.0);
        st.fanAngle = (st.fanAngle + st.fanVel * dtSec) % (Math.PI * 2);

        const lightFrac = room.lightOn ? room.lightBrightness / 100 : 0;
        const isNight = room.mode === 'SLEEP';

        // Walls & Ceiling
        ctx.fillStyle = lightFrac > 0 ? '#181E38' : '#090F1D';
        ctx.fillRect(0, 0, 800, 600);

        // Floor
        const floorGrad = ctx.createLinearGradient(0, 420, 0, 600);
        floorGrad.addColorStop(0, lightFrac > 0 ? '#1E1A2E' : '#10101D');
        floorGrad.addColorStop(1, lightFrac > 0 ? '#141224' : '#090B14');
        ctx.fillStyle = floorGrad;
        ctx.fillRect(0, 420, 800, 180);

        ctx.strokeStyle = '#253459';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.moveTo(0, 420);
        ctx.lineTo(800, 420);
        ctx.stroke();

        // Warm Ceiling Cove glow
        if (lightFrac > 0) {
          const glow = ctx.createRadialGradient(400, 85, 20, 400, 220, 360);
          glow.addColorStop(0, `rgba(196, 181, 253, ${0.24 * lightFrac})`);
          glow.addColorStop(0.55, `rgba(167, 139, 250, ${0.10 * lightFrac})`);
          glow.addColorStop(1, 'rgba(167, 139, 250, 0)');
          ctx.fillStyle = glow;
          ctx.fillRect(40, 20, 720, 500);
        }

        // Ceiling strip cove
        ctx.fillStyle = lightFrac > 0 ? '#DDD6FE' : '#334155';
        ctx.fillRect(140, 46, 520, 6);

        // Window on Left wall (Night stars / Moon or Soft Dusk)
        ctx.save();
        ctx.beginPath();
        ctx.roundRect(52, 128, 175, 165, 10);
        ctx.clip();
        const sky = ctx.createLinearGradient(0, 128, 0, 293);
        sky.addColorStop(0, isNight ? '#060C1E' : '#142A54');
        sky.addColorStop(1, isNight ? '#111F42' : '#254C8A');
        ctx.fillStyle = sky;
        ctx.fillRect(52, 128, 175, 165);

        // Crescent Moon
        ctx.fillStyle = '#FDE68A';
        ctx.beginPath();
        ctx.arc(105, 180, 18, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = isNight ? '#0A132B' : '#1A386E';
        ctx.beginPath();
        ctx.arc(113, 175, 15, 0, Math.PI * 2);
        ctx.fill();

        // Twinkling stars
        const stars = [
          [155, 162],
          [190, 186],
          [135, 220],
          [185, 240],
          [85, 238],
        ];
        stars.forEach(([sx, sy], idx) => {
          const alpha = 0.4 + 0.6 * Math.abs(Math.sin(tSec * 2 + idx));
          ctx.fillStyle = `rgba(254, 240, 138, ${alpha})`;
          ctx.beginPath();
          ctx.arc(sx, sy, 2, 0, Math.PI * 2);
          ctx.fill();
        });
        ctx.restore();

        ctx.strokeStyle = '#33487A';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.roundRect(52, 128, 175, 165, 10);
        ctx.stroke();

        // Bed Headboard & Platform
        ctx.fillStyle = '#2B2446';
        ctx.beginPath();
        ctx.roundRect(270, 240, 310, 150, 12);
        ctx.fill();
        ctx.strokeStyle = '#473D72';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Bedside nightstands + Lamps
        [210, 595].forEach((nx) => {
          ctx.fillStyle = '#23203A';
          ctx.beginPath();
          ctx.roundRect(nx, 368, 52, 62, 6);
          ctx.fill();
          // Lamp base & shade
          ctx.fillStyle = '#94A3B8';
          ctx.fillRect(nx + 23, 344, 6, 24);
          ctx.fillStyle = lightFrac > 0 ? '#FCD34D' : '#475569';
          ctx.beginPath();
          ctx.moveTo(nx + 12, 346);
          ctx.lineTo(nx + 40, 346);
          ctx.lineTo(nx + 34, 326);
          ctx.lineTo(nx + 18, 326);
          ctx.closePath();
          ctx.fill();

          if (lightFrac > 0) {
            const lg = ctx.createRadialGradient(nx + 26, 336, 4, nx + 26, 346, 60);
            lg.addColorStop(0, `rgba(253, 224, 71, ${0.28 * lightFrac})`);
            lg.addColorStop(1, 'rgba(253, 224, 71, 0)');
            ctx.fillStyle = lg;
            ctx.beginPath();
            ctx.arc(nx + 26, 342, 60, 0, Math.PI * 2);
            ctx.fill();
          }
        });

        // Mattress & Blanket
        ctx.fillStyle = '#CBD5E1';
        ctx.beginPath();
        ctx.roundRect(285, 346, 280, 118, 10);
        ctx.fill();
        ctx.fillStyle = '#4C1D95';
        ctx.beginPath();
        ctx.roundRect(285, 385, 280, 82, 10);
        ctx.fill();
        // Pillows
        ctx.fillStyle = '#F8FAFC';
        ctx.beginPath();
        ctx.roundRect(306, 318, 105, 42, 10);
        ctx.roundRect(438, 318, 105, 42, 10);
        ctx.fill();

        // Air Conditioner on right wall
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.roundRect(575, 115, 175, 48, 10);
        ctx.fill();
        ctx.fillStyle = room.acOn ? '#0F172A' : '#334155';
        ctx.fillRect(700, 128, 36, 16);
        if (room.acOn) {
          ctx.fillStyle = '#38BDF8';
          ctx.font = 'bold 11px JetBrains Mono, monospace';
          ctx.textAlign = 'center';
          ctx.fillText(`${room.acTemp}°`, 718, 140);

          for (let i = 0; i < 3; i++) {
            const p = (tSec * 0.8 + i * 0.33) % 1;
            const alpha = Math.sin(p * Math.PI) * 0.5;
            ctx.strokeStyle = `rgba(56, 189, 248, ${alpha})`;
            ctx.lineWidth = 2;
            ctx.beginPath();
            const y = 166 + p * 76;
            ctx.moveTo(605 + i * 40, y);
            ctx.quadraticCurveTo(595 + i * 40, y + 20, 615 + i * 40, y + 40);
            ctx.stroke();
          }
        }

        // Ceiling 5-blade Fan
        ctx.save();
        ctx.translate(420, 118);
        ctx.strokeStyle = '#64748B';
        ctx.lineWidth = 4;
        ctx.beginPath();
        ctx.moveTo(0, -72);
        ctx.lineTo(0, 0);
        ctx.stroke();

        ctx.scale(1, 0.34); // Perspective foreshortening
        ctx.rotate(st.fanAngle);
        for (let b = 0; b < 5; b++) {
          ctx.rotate((Math.PI * 2) / 5);
          ctx.fillStyle = room.fanOn ? '#A78BFA' : '#64748B';
          ctx.beginPath();
          ctx.ellipse(52, 0, 44, 14, 0.15, 0, Math.PI * 2);
          ctx.fill();
        }
        ctx.fillStyle = '#E2E8F0';
        ctx.beginPath();
        ctx.arc(0, 0, 18, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();

        // Top HUD badge
        ctx.fillStyle = 'rgba(14, 22, 39, 0.78)';
        ctx.beginPath();
        ctx.roundRect(16, 14, 235, 30, 8);
        ctx.fill();
        ctx.fillStyle = '#E6EDF7';
        ctx.font = '600 12px Be Vietnam Pro, sans-serif';
        ctx.textAlign = 'left';
        ctx.fillText(
          `${room.name} · Đèn ${room.lightOn ? `${room.lightBrightness}%` : 'tắt'} · Quạt mức ${room.fanSpeed}`,
          26,
          33,
        );
        void SH;
      }}
    />
  );
}

