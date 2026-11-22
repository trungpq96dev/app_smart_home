const WORKLET_CODE = `
class Pcm16Downsampler extends AudioWorkletProcessor {
  constructor() {
    super();
    this.ratio = sampleRate / 16000;
    this.acc = 0;
    this.out = new Int16Array(1600); // ~100ms at 16kHz
    this.outIdx = 0;
  }
  process(inputs) {
    const input = inputs[0]?.[0];
    if (!input) return true;
    for (let i = 0; i < input.length; i++) {
      this.acc += 1;
      if (this.acc >= this.ratio) {
        this.acc -= this.ratio;
        const s = Math.max(-1, Math.min(1, input[i]));
        this.out[this.outIdx++] = s < 0 ? s * 32768 : s * 32767;
        if (this.outIdx >= this.out.length) {
          const copy = this.out.slice(0);
          this.port.postMessage(copy.buffer, [copy.buffer]);
          this.outIdx = 0;
        }
      }
    }
    return true;
  }
}
registerProcessor('pcm16-downsampler', Pcm16Downsampler);
`;

export class ThuAmWeb {
  private stream: MediaStream | null = null;
  private ctx: AudioContext | null = null;
  private source: MediaStreamAudioSourceNode | null = null;
  private workletNode: AudioWorkletNode | null = null;
  private scriptNode: ScriptProcessorNode | null = null;
  private silentGain: GainNode | null = null;
  private khoa = false;
  coKhuVong = true;
  private readonly onPcm: (bytes: Uint8Array) => void;

  constructor(onPcm: (bytes: Uint8Array) => void) {
    this.onPcm = onPcm;
  }

  khoaMic(khoa: boolean): void {
    this.khoa = khoa;
  }

  async batDau(): Promise<string | null> {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      return 'Trình duyệt chỉ cấp quyền micro trên localhost hoặc kết nối HTTPS. Hãy mở qua https:// hoặc localhost.';
    }

    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
          channelCount: 1,
        },
      });
    } catch (err) {
      const name = (err as { name?: string })?.name;
      if (name === 'NotAllowedError' || name === 'PermissionDeniedError') {
        return 'Chưa cấp quyền micro nên chưa nói chuyện được.';
      }
      if (name === 'NotFoundError') {
        return 'Không tìm thấy micro trên thiết bị này.';
      }
      return 'Không mở được micro: ' + (err instanceof Error ? err.message : String(err));
    }

    const track = this.stream.getAudioTracks()[0];
    const settings = track?.getSettings?.();
    this.coKhuVong = settings?.echoCancellation !== false;

    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    const ctx = new AudioCtx();
    this.ctx = ctx;
    if (ctx.state === 'suspended') {
      await ctx.resume();
    }

    const src = ctx.createMediaStreamSource(this.stream);
    this.source = src;
    const silent = ctx.createGain();
    silent.gain.value = 0;
    silent.connect(ctx.destination);
    this.silentGain = silent;

    if (ctx.audioWorklet) {
      try {
        const blob = new Blob([WORKLET_CODE], { type: 'application/javascript' });
        const url = URL.createObjectURL(blob);
        try {
          await ctx.audioWorklet.addModule(url);
        } finally {
          URL.revokeObjectURL(url);
        }
        const node = new AudioWorkletNode(ctx, 'pcm16-downsampler');
        node.port.onmessage = (ev: MessageEvent<ArrayBuffer>) => {
          // Luon doc het goi ke ca khi khoa mic de tranh dong bo tre
          if (this.khoa) return;
          this.onPcm(new Uint8Array(ev.data));
        };
        src.connect(node);
        node.connect(silent);
        this.workletNode = node;
        return null;
      } catch {
        // fallback to ScriptProcessorNode below
      }
    }

    const ratio = ctx.sampleRate / 16000;
    let acc = 0;
    const proc = ctx.createScriptProcessor(4096, 1, 1);
    proc.onaudioprocess = (ev) => {
      const input = ev.inputBuffer.getChannelData(0);
      if (this.khoa) return;
      const samples: number[] = [];
      for (let i = 0; i < input.length; i++) {
        acc += 1;
        if (acc >= ratio) {
          acc -= ratio;
          const s = Math.max(-1, Math.min(1, input[i]));
          samples.push(s < 0 ? s * 32768 : s * 32767);
        }
      }
      if (samples.length > 0) {
        const pcm = new Int16Array(samples);
        this.onPcm(new Uint8Array(pcm.buffer));
      }
    };
    src.connect(proc);
    proc.connect(silent);
    this.scriptNode = proc;
    return null;
  }

  dung(): void {
    try { this.workletNode?.disconnect(); } catch { /* ignore */ }
    try { this.scriptNode?.disconnect(); } catch { /* ignore */ }
    try { this.source?.disconnect(); } catch { /* ignore */ }
    try { this.silentGain?.disconnect(); } catch { /* ignore */ }
    this.workletNode = null;
    this.scriptNode = null;
    this.source = null;
    this.silentGain = null;

    if (this.stream) {
      for (const t of this.stream.getTracks()) {
        try { t.stop(); } catch { /* ignore */ }
      }
      this.stream = null;
    }
    if (this.ctx) {
      try { void this.ctx.close(); } catch { /* ignore */ }
      this.ctx = null;
    }
  }
}

export class PhatAmWeb {
  private ctx: AudioContext | null = null;
  private nextTime = 0;
  private readonly activeSources = new Set<AudioBufferSourceNode>();

  batDau(): string | null {
    try {
      const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
      this.ctx = new AudioCtx({ sampleRate: 24000 });
      this.nextTime = 0;
      return null;
    } catch {
      try {
        const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
        this.ctx = new AudioCtx();
        this.nextTime = 0;
        return null;
      } catch (err) {
        return 'Không mở được loa trình duyệt: ' + (err instanceof Error ? err.message : String(err));
      }
    }
  }

  phat(pcmBytes: Uint8Array): void {
    const ctx = this.ctx;
    if (!ctx || pcmBytes.byteLength < 2) return;
    if (ctx.state === 'suspended') {
      void ctx.resume();
    }

    const sampleCount = Math.floor(pcmBytes.byteLength / 2);
    const aligned =
      pcmBytes.byteOffset % 2 === 0
        ? new Int16Array(pcmBytes.buffer, pcmBytes.byteOffset, sampleCount)
        : new Int16Array(pcmBytes.slice(0, sampleCount * 2).buffer);

    const buf = ctx.createBuffer(1, sampleCount, 24000);
    const ch = buf.getChannelData(0);
    for (let i = 0; i < sampleCount; i++) {
      ch[i] = aligned[i] / 32768;
    }

    const node = ctx.createBufferSource();
    node.buffer = buf;
    node.connect(ctx.destination);

    const now = ctx.currentTime;
    if (this.nextTime < now) {
      this.nextTime = now + 0.02;
    }
    const startAt = this.nextTime;
    this.nextTime = startAt + buf.duration;

    this.activeSources.add(node);
    node.onended = () => {
      this.activeSources.delete(node);
    };
    node.start(startAt);
  }

  dangPhat(): boolean {
    if (!this.ctx) return false;
    return this.activeSources.size > 0 || this.nextTime > this.ctx.currentTime + 0.03;
  }

  xoaHangDoi(): void {
    for (const node of this.activeSources) {
      try {
        node.onended = null;
        node.stop();
        node.disconnect();
      } catch {
        // ignore
      }
    }
    this.activeSources.clear();
    this.nextTime = 0;
  }

  dung(): void {
    this.xoaHangDoi();
    if (this.ctx) {
      try { void this.ctx.close(); } catch { /* ignore */ }
      this.ctx = null;
    }
  }
}

