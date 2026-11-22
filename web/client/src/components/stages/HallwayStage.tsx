import React from 'react';
import { HALLWAY_VIEWS, type RoomUi } from '../../data/models.js';
import { SH } from '../../theme/colors.js';
import { StageCanvas } from './StageCanvas.js';

export function HallwayStage({ room, fill = false }: { room: RoomUi; fill?: boolean }) {
  const irFilterStyle: React.CSSProperties | undefined = room.hallIrMode
    ? { filter: 'url(#hw-ir-filter)' }
    : undefined;

  return (
    <div className={fill ? 'relative flex-1 min-h-0 h-full w-full flex flex-col' : 'relative'}>
      {/* Inline SVG color matrix matching Kotlin IR night-vision ColorMatrix */}
      <svg className="absolute h-0 w-0 pointer-events-none" aria-hidden="true">
        <defs>
          <filter id="hw-ir-filter">
            <feColorMatrix
              type="matrix"
              values="
                0.22 0.45 0.08 0 0.02
                0.36 0.78 0.16 0 0.05
                0.14 0.30 0.06 0 0.01
                0    0    0    1 0
              "
            />
          </filter>
        </defs>
      </svg>

      <StageCanvas
        logicalWidth={800}
        logicalHeight={500}
        fill={fill}
        canvasStyle={irFilterStyle}
        draw={({ ctx, tSec }) => {
          const frontFrac = room.hallFrontLightOn ? room.hallFrontBrightness / 100 : 0;
          const surrFrac = room.hallSurroundingLightOn ? room.hallSurroundingBrightness / 100 : 0;

          // Sky / Corridor night background
          const bg = ctx.createLinearGradient(0, 0, 0, 500);
          bg.addColorStop(0, '#070D19');
          bg.addColorStop(1, '#111D35');
          ctx.fillStyle = bg;
          ctx.fillRect(0, 0, 800, 500);

          if (room.hallView === 'DAU_HE') {
            // Front porch perspective
            ctx.fillStyle = '#18243E';
            ctx.fillRect(180, 90, 440, 290);
            ctx.fillStyle = '#121C33';
            ctx.beginPath();
            ctx.moveTo(0, 380);
            ctx.lineTo(800, 380);
            ctx.lineTo(800, 500);
            ctx.lineTo(0, 500);
            ctx.closePath();
            ctx.fill();

            // Front door + Smart Lock
            ctx.fillStyle = '#293757';
            ctx.beginPath();
            ctx.roundRect(320, 145, 160, 235, 6);
            ctx.fill();
            ctx.strokeStyle = '#475569';
            ctx.lineWidth = 2;
            ctx.stroke();

            // Smart lock badge on door
            ctx.fillStyle = room.hallDoorLocked ? SH.Green : SH.Amber;
            ctx.beginPath();
            ctx.roundRect(444, 248, 22, 34, 4);
            ctx.fill();

            // Porch light fixture + illumination cone
            ctx.fillStyle = frontFrac > 0 ? '#FDE047' : '#475569';
            ctx.beginPath();
            ctx.ellipse(400, 108, 36, 10, 0, 0, Math.PI * 2);
            ctx.fill();

            if (frontFrac > 0) {
              const cone = ctx.createRadialGradient(400, 112, 10, 400, 270, 250);
              cone.addColorStop(0, `rgba(253, 224, 71, ${0.32 * frontFrac})`);
              cone.addColorStop(1, 'rgba(253, 224, 71, 0)');
              ctx.fillStyle = cone;
              ctx.fillRect(140, 95, 520, 385);
            }
          } else if (room.hallView === 'XUNG_QUANH') {
            // Perimeter wall + pathway + 3 sconces
            ctx.fillStyle = '#131E35';
            ctx.fillRect(0, 140, 800, 210);
            ctx.fillStyle = '#0D1527';
            ctx.fillRect(0, 350, 800, 150);

            [180, 400, 620].forEach((lx) => {
              ctx.fillStyle = surrFrac > 0 ? '#38BDF8' : '#334155';
              ctx.beginPath();
              ctx.arc(lx, 175, 12, 0, Math.PI * 2);
              ctx.fill();
              if (surrFrac > 0) {
                const sg = ctx.createRadialGradient(lx, 180, 6, lx, 275, 150);
                sg.addColorStop(0, `rgba(56, 189, 248, ${0.32 * surrFrac})`);
                sg.addColorStop(1, 'rgba(56, 189, 248, 0)');
                ctx.fillStyle = sg;
                ctx.beginPath();
                ctx.arc(lx, 250, 150, 0, Math.PI * 2);
                ctx.fill();
              }
            });
          } else {
            // Top-down remote overview (TU_XA)
            ctx.fillStyle = '#17233D';
            ctx.beginPath();
            ctx.roundRect(140, 75, 520, 340, 18);
            ctx.fill();
            ctx.strokeStyle = '#2A3C66';
            ctx.lineWidth = 2;
            ctx.stroke();

            // Zones
            ctx.fillStyle = frontFrac > 0 ? `rgba(253, 224, 71, ${0.22 * frontFrac})` : 'rgba(15, 23, 42, 0.45)';
            ctx.fillRect(310, 85, 180, 90);
            ctx.fillStyle = surrFrac > 0 ? `rgba(56, 189, 248, ${0.20 * surrFrac})` : 'rgba(15, 23, 42, 0.35)';
            ctx.fillRect(160, 195, 480, 200);

            ctx.fillStyle = '#E6EDF7';
            ctx.font = '600 12px Be Vietnam Pro, sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('HÀNH LANG ĐẦU HÈ', 400, 134);
            ctx.fillText('SÂN QUANH & LỐI ĐI', 400, 295);
          }

          // CCTV Sweep cone
          if (room.hallCameraOn) {
            const sweep = Math.sin(tSec * 1.1) * 0.4;
            ctx.save();
            ctx.translate(650, 86);
            ctx.rotate(sweep);
            const fovGrad = ctx.createRadialGradient(0, 0, 8, 0, 0, 220);
            fovGrad.addColorStop(0, 'rgba(34, 211, 238, 0.25)');
            fovGrad.addColorStop(1, 'rgba(34, 211, 238, 0)');
            ctx.fillStyle = fovGrad;
            ctx.beginPath();
            ctx.moveTo(0, 0);
            ctx.lineTo(-150, 195);
            ctx.lineTo(-40, 215);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
          }

          // PIR Motion radar rings
          if (room.hallMotionDetected) {
            for (let r = 0; r < 3; r++) {
              const rp = (tSec * 1.2 + r * 0.33) % 1;
              ctx.strokeStyle = `rgba(245, 158, 11, ${1 - rp})`;
              ctx.lineWidth = 2.5;
              ctx.beginPath();
              ctx.arc(400, 320, 24 + rp * 110, 0, Math.PI * 2);
              ctx.stroke();
            }
          }

          // IR Scanlines when night-vision mode active
          if (room.hallIrMode) {
            ctx.fillStyle = 'rgba(0, 20, 0, 0.16)';
            for (let y = 0; y < 500; y += 4) {
              ctx.fillRect(0, y, 800, 1.5);
            }
          }

          // CCTV HUD overlay
          const camMeta = HALLWAY_VIEWS.find((v) => v.id === room.hallView) ?? HALLWAY_VIEWS[0];
          ctx.fillStyle = 'rgba(11, 18, 32, 0.8)';
          ctx.beginPath();
          ctx.roundRect(16, 14, 230, 30, 8);
          ctx.fill();
          ctx.fillStyle = room.hallCameraOn ? SH.Red : '#64748B';
          ctx.beginPath();
          ctx.arc(32, 29, 5, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = '#E6EDF7';
          ctx.font = 'bold 11px JetBrains Mono, monospace';
          ctx.textAlign = 'left';
          ctx.fillText(`${camMeta.camTitle}${room.hallIrMode ? ' · IR' : ''}`, 46, 33);

          // Live clock top right
          const d = new Date();
          const clk = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
          ctx.fillStyle = 'rgba(11, 18, 32, 0.8)';
          ctx.beginPath();
          ctx.roundRect(670, 14, 114, 30, 8);
          ctx.fill();
          ctx.fillStyle = '#E6EDF7';
          ctx.textAlign = 'center';
          ctx.fillText(clk, 727, 33);
        }}
      />
    </div>
  );
}

