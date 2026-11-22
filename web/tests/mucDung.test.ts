import { describe, expect, it } from 'vitest';
import { MucDung, type MucDungNgay } from '../client/src/data/giongnoi/types.js';

describe('MucDung', () => {
  it('gop phien trong cung ngay va tach qua ngay moi', () => {
    let ds = MucDung.gopPhien([], '2026-11-20', 600, 10, 20, 30);
    ds = MucDung.gopPhien(ds, '2026-11-20', 60, 5, 5, 10);
    expect(ds.length).toBe(1);
    expect(ds[0].giay).toBe(660);
    expect(ds[0].soPhien).toBe(2);
    expect(ds[0].tokenTong).toBe(40);

    ds = MucDung.gopPhien(ds, '2026-11-21', 45, 0, 0, 0);
    expect(ds.length).toBe(2);
    expect(MucDung.cuaNgay(ds, '2026-11-20').giay).toBe(660);
    expect(MucDung.cuaNgay(ds, '2026-11-21').giay).toBe(45);
    expect(MucDung.cuaNgay(ds, '2026-11-22').giay).toBe(0);
  });

  it('chi giu 30 ngay gan nhat va sap theo ngay', () => {
    let ds: MucDungNgay[] = [];
    for (let d = 1; d <= 40; d++) {
      ds = MucDung.gopPhien(ds, `2026-10-${String(d).padStart(2, '0')}`, 60, 0, 0, 0);
    }
    expect(ds.length).toBe(MucDung.SO_NGAY_GIU);
    expect(ds[0].ngay).toBe('2026-10-11');
    expect(ds[ds.length - 1].ngay).toBe('2026-10-40');
  });

  it('ghi ra doc lai JSON an toan', () => {
    const ds = MucDung.gopPhien([], '2026-11-21', 120, 1, 2, 3);
    expect(MucDung.tuJson(MucDung.sangJson(ds))).toEqual(ds);
    expect(MucDung.tuJson('{rac}')).toEqual([]);
    expect(MucDung.tuJson(null)).toEqual([]);
  });
});

