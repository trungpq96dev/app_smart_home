import { duongWsGemini, type CauHinhGiongNoi } from './types.js';

export interface GeminiLiveNghe {
  sanSang(): void;
  tiengNoi(pcmBytes: Uint8Array): void;
  biNgatLoi(): void;
  hetLuot(): void;
  loiThoai(cau: string, cuaMinh: boolean): void;
  goiCongCu(id: string | null, ten: string, args: Record<string, unknown>): void;
  mucDung(vao: number, ra: number, tong: number): void;
  sapHetPhien(conLaiMs: number | null): void;
  dut(cau: string | null): void;
}

export function bytesToBase64(bytes: Uint8Array): string {
  let binary = '';
  const chunk = 0x8000;
  for (let i = 0; i < bytes.length; i += chunk) {
    binary += String.fromCharCode(...bytes.subarray(i, i + chunk));
  }
  return btoa(binary);
}

export function base64ToBytes(b64: string): Uint8Array {
  const binary = atob(b64);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    out[i] = binary.charCodeAt(i);
  }
  return out;
}

export function dungSetupGemini(
  cauHinh: CauHinhGiongNoi,
  loiDan: string,
  congCu: unknown[],
) {
  const cleanModel = cauHinh.model.trim().replace(/^models\//, '');
  return {
    setup: {
      model: `models/${cleanModel}`,
      generationConfig: {
        responseModalities: ['AUDIO'],
        speechConfig: {
          voiceConfig: {
            prebuiltVoiceConfig: {
              voiceName: cauHinh.giongDoc,
            },
          },
        },
      },
      systemInstruction: {
        parts: [{ text: loiDan }],
      },
      tools: congCu,
      inputAudioTranscription: {},
      outputAudioTranscription: {},
      realtimeInputConfig: {
        automaticActivityDetection: {
          silenceDurationMs: 600,
        },
      },
    },
  };
}

export function dichMaDongGemini(code: number, reason: string): string | null {
  if (code === 1000) return null;
  if (code === 1008 || code === 4008) {
    return reason
      ? `Gemini từ chối kết nối: ${reason}`
      : 'Khóa API Gemini hoặc tên model không hợp lệ. Kiểm tra lại Cài đặt.';
  }
  if (reason.trim()) return `Gemini đóng kết nối: ${reason}`;
  return `Gemini đóng kết nối (mã ${code}).`;
}

export class GeminiLive {
  private ws: WebSocket | null = null;
  private daSanSang = false;
  private theHe = 0;
  private readonly nghe: GeminiLiveNghe;
  private readonly decoder = new TextDecoder('utf-8');

  constructor(nghe: GeminiLiveNghe) {
    this.nghe = nghe;
  }

  noi(cauHinh: CauHinhGiongNoi, loiDan: string, congCu: unknown[]): void {
    this.dong();
    this.daSanSang = false;
    const heNay = ++this.theHe;

    let socket: WebSocket;
    try {
      socket = new WebSocket(duongWsGemini(cauHinh));
      socket.binaryType = 'arraybuffer';
    } catch (err) {
      this.nghe.dut(err instanceof Error ? err.message : 'Không mở được WebSocket tới Gemini.');
      return;
    }

    this.ws = socket;

    const conDung = () => this.theHe === heNay;

    socket.onopen = () => {
      if (!conDung()) return;
      socket.send(JSON.stringify(dungSetupGemini(cauHinh, loiDan, congCu)));
    };

    socket.onmessage = (ev) => {
      if (!conDung()) return;
      if (typeof ev.data === 'string') {
        this.doc(ev.data);
      } else if (ev.data instanceof ArrayBuffer) {
        this.doc(this.decoder.decode(ev.data));
      } else if (ev.data instanceof Blob) {
        ev.data.text().then((txt) => {
          if (conDung()) this.doc(txt);
        }).catch(() => {});
      }
    };

    socket.onerror = () => {
      if (!conDung()) return;
      if (!this.daSanSang) {
        this.nghe.dut('Không kết nối được tới Gemini Live. Kiểm tra khóa API, tên model và mạng Internet.');
      }
    };

    socket.onclose = (ev) => {
      if (!conDung()) return;
      this.nghe.dut(dichMaDongGemini(ev.code, ev.reason || ''));
    };
  }

  guiTieng(pcmBytes: Uint8Array): void {
    if (!this.daSanSang || !this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(
      JSON.stringify({
        realtimeInput: {
          audio: {
            data: bytesToBase64(pcmBytes),
            mimeType: 'audio/pcm;rate=16000',
          },
        },
      }),
    );
  }

  bangTiengDungLai(): void {
    if (!this.daSanSang || !this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify({ realtimeInput: { audioStreamEnd: true } }));
  }

  traKetQuaCongCu(id: string | null, ten: string, ketQua: unknown): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    const mot: Record<string, unknown> = { name: ten, response: ketQua };
    if (id !== null) mot.id = id;
    this.ws.send(
      JSON.stringify({
        toolResponse: {
          functionResponses: [mot],
        },
      }),
    );
  }

  dong(): void {
    this.daSanSang = false;
    this.theHe++;
    const w = this.ws;
    this.ws = null;
    if (w) {
      try {
        w.close(1000, 'nguoi dung dong phien');
      } catch {
        // ignore close error
      }
    }
  }

  private doc(json: string): void {
    let o: Record<string, unknown>;
    try {
      o = JSON.parse(json) as Record<string, unknown>;
    } catch {
      return;
    }

    if (Object.prototype.hasOwnProperty.call(o, 'setupComplete')) {
      this.daSanSang = true;
      this.nghe.sanSang();
      return;
    }

    const sc = o.serverContent as Record<string, unknown> | undefined;
    if (sc) {
      if (sc.interrupted === true) this.nghe.biNgatLoi();

      const inTr = (sc.inputTranscription as { text?: string } | undefined)?.text;
      if (typeof inTr === 'string' && inTr.trim()) {
        this.nghe.loiThoai(inTr, true);
      }
      const outTr = (sc.outputTranscription as { text?: string } | undefined)?.text;
      if (typeof outTr === 'string' && outTr.trim()) {
        this.nghe.loiThoai(outTr, false);
      }

      const parts = (sc.modelTurn as { parts?: Array<{ inlineData?: { data?: string } }> } | undefined)?.parts;
      if (Array.isArray(parts)) {
        for (const p of parts) {
          const data = p?.inlineData?.data;
          if (typeof data === 'string' && data.length > 0) {
            try {
              this.nghe.tiengNoi(base64ToBytes(data));
            } catch {
              // ignore invalid base64
            }
          }
        }
      }

      if (sc.turnComplete === true) this.nghe.hetLuot();
    }

    const tc = o.toolCall as { functionCalls?: Array<{ id?: string; name?: string; args?: Record<string, unknown> }> } | undefined;
    if (Array.isArray(tc?.functionCalls)) {
      for (const c of tc.functionCalls) {
        if (!c?.name) continue;
        this.nghe.goiCongCu(c.id || null, c.name, c.args ?? {});
      }
    }

    const um = o.usageMetadata as {
      promptTokenCount?: number;
      responseTokenCount?: number;
      totalTokenCount?: number;
    } | undefined;
    if (um) {
      this.nghe.mucDung(
        Number(um.promptTokenCount ?? 0) || 0,
        Number(um.responseTokenCount ?? 0) || 0,
        Number(um.totalTokenCount ?? 0) || 0,
      );
    }

    const ga = o.goAway as { timeLeft?: string } | undefined;
    if (ga) {
      const sec = typeof ga.timeLeft === 'string' ? Number.parseFloat(ga.timeLeft.replace(/s$/, '')) : Number.NaN;
      this.nghe.sapHetPhien(Number.isFinite(sec) ? Math.round(sec * 1000) : null);
    }
  }
}

