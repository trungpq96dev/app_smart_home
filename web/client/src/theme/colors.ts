/**
 * Bang mau Smart Home Dark - dong bo 1:1 voi ui/theme/Color.kt.
 * Khong dung den tuyet doi (#000) o bat ky dau; nen toi chiem ~90% man hinh.
 */
export const SH = {
  Bg: '#0B1220',
  Sidebar: '#0E1627',
  Card: '#16213A',
  Card2: '#1C2946',
  Border: '#253453',
  BorderHi: '#3B82F6',

  Blue: '#3B82F6',
  BlueDeep: '#2563EB',
  Green: '#22C55E',
  Amber: '#FBBF24',
  Orange: '#F97316',
  Red: '#EF4444',
  Purple: '#8B5CF6',
  Cyan: '#22D3EE',
  Teal: '#14B8A6',

  Text: '#F1F5F9',
  TextDim: '#94A3B8',
  TextOff: '#64748B',

  Surface: '#0E1627',
  SurfaceHi: '#1C2946',
  TextPrimary: '#F1F5F9',
  TextSecondary: '#94A3B8',
} as const;

/** Tao mau rgba tu ma hex #RRGGBB kem do trong suot alpha (0..1). */
export function tint(hex: string, alpha = 0.16): string {
  const clean = hex.replace('#', '');
  if (clean.length === 6) {
    const r = Number.parseInt(clean.slice(0, 2), 16);
    const g = Number.parseInt(clean.slice(2, 4), 16);
    const b = Number.parseInt(clean.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }
  return hex;
}
