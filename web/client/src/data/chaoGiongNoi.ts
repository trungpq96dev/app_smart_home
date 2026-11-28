import { CameraPlaybackClient } from './cameraRecorder.js';
import { laKhungGioDem, type ScreensaverConfig } from './screensaver.js';

let activeAudio: HTMLAudioElement | null = null;
let activeAudioUrl: string | null = null;

function cleanupAudio() {
  if (activeAudio) {
    try {
      activeAudio.pause();
      activeAudio.src = '';
    } catch {
      // ignore
    }
    activeAudio = null;
  }
  if (activeAudioUrl) {
    URL.revokeObjectURL(activeAudioUrl);
    activeAudioUrl = null;
  }
}

function speakBrowserFallback(text: string): boolean {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) return false;
  try {
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = 'vi-VN';
    utter.rate = 0.96;
    const voices = window.speechSynthesis.getVoices();
    const vi = voices.find((v) => v.lang.toLowerCase().startsWith('vi'));
    if (vi) utter.voice = vi;
    window.speechSynthesis.speak(utter);
    return true;
  } catch {
    return false;
  }
}

/**
 * Phát câu chào Tiếng Việt từ `/api/camera/recorder/tts` (MP3 Neural/Google TTS trên serverubuntu),
 * tự rơi về Web Speech API nếu trình duyệt hoặc đầu ghi không trả được MP3.
 * Tôn trọng khung giờ ngủ (22:00–06:00) trừ khi `docChaoGioNgu = true` hoặc `forceTest = true`.
 */
export async function phatChaoGiongNoi(
  text: string,
  opts: {
    apiPort?: number | null;
    screensaverConfig?: ScreensaverConfig;
    forceTest?: boolean;
  } = {},
): Promise<{ ok: boolean; skipped?: string }> {
  const clean = text.trim();
  if (!clean) return { ok: false };

  const sc = opts.screensaverConfig;
  if (!opts.forceTest && sc) {
    if (!sc.docChaoBat) return { ok: false, skipped: 'muted' };
    const nowHour = new Date().getHours();
    if (!sc.docChaoGioNgu && laKhungGioDem(nowHour, sc.nightStartHour, sc.nightEndHour)) {
      return { ok: false, skipped: 'night_quiet' };
    }
  }

  cleanupAudio();

  try {
    const url = CameraPlaybackClient.urlTts(clean, opts.apiPort);
    const resp = await fetch(url, { credentials: 'same-origin' });
    if (resp.ok) {
      const blob = await resp.blob();
      if (blob.size > 256) {
        const objUrl = URL.createObjectURL(blob);
        activeAudioUrl = objUrl;
        const audio = new Audio(objUrl);
        activeAudio = audio;
        audio.onended = cleanupAudio;
        audio.onerror = cleanupAudio;
        await audio.play();
        return { ok: true };
      }
    }
  } catch {
    cleanupAudio();
  }

  const fbOk = speakBrowserFallback(clean);
  return { ok: fbOk };
}
