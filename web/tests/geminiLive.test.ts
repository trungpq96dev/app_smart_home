import { describe, expect, it } from 'vitest';
import { CongCuNha } from '../client/src/data/giongnoi/congCuNha.js';
import { dichMaDongGemini, dungSetupGemini } from '../client/src/data/giongnoi/geminiLive.js';
import { defaultCauHinhGiongNoi, duongWsGemini, choThayTrongLoiWs } from '../client/src/data/giongnoi/types.js';

describe('GeminiLive', () => {
  it('setup dung cau truc v1beta BidiGenerateContent', () => {
    const cfg = { ...defaultCauHinhGiongNoi(), khoaApi: 'SECRET_KEY', model: 'models/gemini-3.8-live', giongDoc: 'Kore' };
    const payload = dungSetupGemini(cfg, 'lời dặn', CongCuNha.khaiBao(['living']));
    const s = payload.setup as Record<string, unknown>;

    expect(s.model).toBe('models/gemini-3.8-live');
    expect((s.generationConfig as { responseModalities: string[] }).responseModalities).toEqual(['AUDIO']);
    expect(
      (s.generationConfig as { speechConfig: { voiceConfig: { prebuiltVoiceConfig: { voiceName: string } } } })
        .speechConfig.voiceConfig.prebuiltVoiceConfig.voiceName,
    ).toBe('Kore');
    expect(Object.prototype.hasOwnProperty.call(s, 'responseModalities')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(s, 'speechConfig')).toBe(false);
    expect(Object.prototype.hasOwnProperty.call(s, 'inputAudioTranscription')).toBe(true);
    expect(Object.prototype.hasOwnProperty.call(s, 'outputAudioTranscription')).toBe(true);
    expect(JSON.stringify(payload)).not.toContain('SECRET_KEY');
  });

  it('duong ws mang khoa va che khoa khi hien loi', () => {
    const cfg = { ...defaultCauHinhGiongNoi(), khoaApi: 'ABC123' };
    expect(duongWsGemini(cfg)).toContain('v1beta.GenerativeService.BidiGenerateContent?key=ABC123');
    expect(choThayTrongLoiWs()).not.toContain('ABC123');
    expect(choThayTrongLoiWs()).toContain('?key=***');
  });

  it('dich ma dong binh thuong va bat thuong', () => {
    expect(dichMaDongGemini(1000, '')).toBeNull();
    expect(dichMaDongGemini(1011, 'model not found')).toContain('model not found');
  });
});

