import React from 'react';
import { type RoomUi, stoveWatts } from '../../data/models.js';
import { SH } from '../../theme/colors.js';
import { StageCanvas } from './StageCanvas.js';

export function KitchenStage({ room, fill = false }: { room: RoomUi; fill?: boolean }) {
  return (
    <StageCanvas
      logicalWidth={800}
      logicalHeight={550}
      fill={fill}
      draw={({ ctx, tSec }) => {
        const lightFrac = room.lightOn ? room.lightBrightness / 100 : 0;

        // Walls
        ctx.fillStyle = lightFrac > 0 ? '#162438' : '#0B1220';
        ctx.fillRect(0, 0, 800, 550);

        // Tile backsplash
        ctx.fillStyle = lightFrac > 0 ? '#1C2E4A' : '#101A2E';
        ctx.fillRect(40, 210, 470, 125);
        ctx.strokeStyle = 'rgba(255,255,255,0.05)';
        ctx.lineWidth = 1;
        for (let x = 40; x <= 510; x += 38) {
          ctx.beginPath();
          ctx.moveTo(x, 210);
          ctx.lineTo(x, 335);
          ctx.stroke();
        }
        for (let y = 210; y <= 335; y += 25) {
          ctx.beginPath();
          ctx.moveTo(40, y);
          ctx.lineTo(510, y);
          ctx.stroke();
        }

        // Floor
        ctx.fillStyle = lightFrac > 0 ? '#1B2234' : '#0F1522';
        ctx.fillRect(0, 455, 800, 95);

        // Ceiling glow
        if (lightFrac > 0) {
          const glow = ctx.createRadialGradient(340, 45, 20, 340, 200, 340);
          glow.addColorStop(0, `rgba(110, 231, 183, ${0.18 * lightFrac})`);
          glow.addColorStop(0.5, `rgba(52, 211, 153, ${0.08 * lightFrac})`);
          glow.addColorStop(1, 'rgba(52, 211, 153, 0)');
          ctx.fillStyle = glow;
          ctx.fillRect(0, 0, 750, 480);
        }

        // Upper cabinets
        ctx.fillStyle = '#1E2D4D';
        ctx.beginPath();
        ctx.roundRect(44, 72, 130, 125, 6);
        ctx.roundRect(380, 72, 130, 125, 6);
        ctx.fill();
        ctx.strokeStyle = '#33487A';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Lower Countertop & Cabinets
        ctx.fillStyle = '#253559';
        ctx.fillRect(36, 332, 480, 123);
        ctx.fillStyle = '#94A3B8';
        ctx.fillRect(30, 326, 492, 12);

        // Range Hood (middle)
        ctx.fillStyle = '#475569';
        ctx.fillRect(250, 30, 56, 95);
        ctx.beginPath();
        ctx.moveTo(192, 178);
        ctx.lineTo(364, 178);
        ctx.lineTo(318, 125);
        ctx.lineTo(238, 125);
        ctx.closePath();
        ctx.fillStyle = '#334155';
        ctx.fill();
        ctx.strokeStyle = room.fanOn ? SH.Green : '#64748B';
        ctx.lineWidth = 2;
        ctx.stroke();

        // Hood suction animation
        if (room.fanOn) {
          for (let i = 0; i < room.hoodSpeed + 2; i++) {
            const p = (tSec * (1.2 + room.hoodSpeed * 0.45) + i * 0.22) % 1;
            const y = 305 - p * 118;
            const x = 225 + (i % 4) * 34 + Math.sin(tSec * 4 + i) * 6;
            ctx.strokeStyle = `rgba(52, 211, 153, ${Math.sin(p * Math.PI) * 0.65})`;
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(x, y);
            ctx.lineTo(x, y - 18);
            ctx.stroke();
          }
        }

        // Induction Cooktop
        ctx.fillStyle = '#090D16';
        ctx.beginPath();
        ctx.roundRect(200, 320, 156, 16, 4);
        ctx.fill();
        if (room.stoveOn) {
          const pulse = 0.65 + 0.35 * Math.sin(tSec * 6);
          ctx.fillStyle = `rgba(239, 68, 68, ${pulse})`;
          ctx.beginPath();
          ctx.ellipse(278, 324, 34, 6, 0, 0, Math.PI * 2);
          ctx.fill();

          // Cooking Pot
          ctx.fillStyle = '#94A3B8';
          ctx.beginPath();
          ctx.roundRect(242, 286, 72, 34, 5);
          ctx.fill();
          ctx.fillStyle = '#CBD5E1';
          ctx.fillRect(236, 282, 84, 5);

          // Steam bubbles rising from pot
          for (let s = 0; s < 4; s++) {
            const sp = (tSec * 0.7 + s * 0.25) % 1;
            const sy = 274 - sp * 65;
            const sx = 258 + s * 13 + Math.sin(tSec * 3 + s) * 6;
            ctx.fillStyle = `rgba(226, 232, 240, ${Math.sin(sp * Math.PI) * 0.45})`;
            ctx.beginPath();
            ctx.arc(sx, sy, 5 + sp * 6, 0, Math.PI * 2);
            ctx.fill();
          }
        }

        // Smart Refrigerator (right)
        ctx.fillStyle = '#1E293B';
        ctx.beginPath();
        ctx.roundRect(555, 86, 185, 370, 12);
        ctx.fill();
        ctx.strokeStyle = '#334155';
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(555, 290);
        ctx.lineTo(740, 290);
        ctx.stroke();
        // Handles
        ctx.fillStyle = '#64748B';
        ctx.fillRect(568, 175, 5, 95);
        ctx.fillRect(568, 308, 5, 80);

        // Fridge smart panel
        ctx.fillStyle = '#0B1220';
        ctx.beginPath();
        ctx.roundRect(650, 130, 72, 68, 8);
        ctx.fill();
        ctx.fillStyle = '#38BDF8';
        ctx.font = 'bold 13px JetBrains Mono, monospace';
        ctx.textAlign = 'center';
        ctx.fillText('4°C', 686, 155);
        ctx.fillStyle = '#818CF8';
        ctx.fillText('-18°C', 686, 182);

        // Gas sensor module (mounted on wall left of hood)
        const gasCol = room.gasAlert ? SH.Red : SH.Green;
        ctx.fillStyle = '#0F172A';
        ctx.beginPath();
        ctx.roundRect(92, 235, 76, 46, 8);
        ctx.fill();
        ctx.strokeStyle = gasCol;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.fillStyle = gasCol;
        ctx.font = 'bold 11px JetBrains Mono, monospace';
        ctx.fillText(`${room.gasPpm} ppm`, 130, 256);
        ctx.fillStyle = '#8A9BB8';
        ctx.font = '600 10px sans-serif';
        ctx.fillText(room.gasAlert ? 'RÒ GAS!' : 'GAS AN TOÀN', 130, 272);

        // Strobe overlay on gas alert
        if (room.gasAlert) {
          const flash = 0.14 + 0.14 * Math.sin(tSec * 10);
          ctx.fillStyle = `rgba(239, 68, 68, ${flash})`;
          ctx.fillRect(0, 0, 800, 550);
        }

        // HUD top badge
        ctx.fillStyle = 'rgba(14, 22, 39, 0.8)';
        ctx.beginPath();
        ctx.roundRect(16, 14, 300, 30, 8);
        ctx.fill();
        ctx.fillStyle = '#E6EDF7';
        ctx.font = '600 12px Be Vietnam Pro, sans-serif';
        ctx.textAlign = 'left';
        const stoveDesc = room.stoveOn ? `Bếp P${room.stoveLevel} (${stoveWatts(room.stoveLevel)}W)` : 'Bếp tắt';
        ctx.fillText(
          `${room.name} · ${stoveDesc} · Hút mùi ${room.fanOn ? `mức ${room.hoodSpeed}` : 'tắt'}`,
          26,
          33,
        );
      }}
    />
  );
}

