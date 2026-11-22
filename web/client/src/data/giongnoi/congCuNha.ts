export interface HanhDongNha {
  maPhong(): string[];
  tenPhong(ma: string): string | null;
  datDen(phong: string, bat: boolean): void;
  datQuat(phong: string, bat: boolean): void;
  datDieuHoa(phong: string, bat: boolean): void;
  datLichHen(phong: string, bat: boolean): void;
  datTv(phong: string, bat: boolean): void;
  datRem(phong: string, mo: boolean): void;
  datBepTu(phong: string, bat: boolean): void;
  datNhietDo(phong: string, doC: number): number;
  chinhNhietDo(phong: string, buoc: number): number;
  datCheDo(phong: string, cheDo: string): void;
  datONhanh(id: string, bat: boolean): void;
  dangBat(phong: string, thietBi: string): boolean | null;
  laThat(phong: string, thietBi: string): boolean;
  thietBiThat(): string[];
  moManHinh(dich: string): boolean;
  docSoLieu(muc: string): Record<string, unknown>;
  chupTrangThai(): string;
  guiMqtt(phong: string, thietBi: string, hanhDong: string, giaTri: string | null): boolean;
}

export class LoiLenh extends Error {
  readonly cau: string;
  constructor(cau: string) {
    super(cau);
    this.cau = cau;
  }
}

export type KetQuaCongCu =
  | { ok: true; ket_qua: string; that: boolean }
  | { ok: true; so_lieu: Record<string, unknown> }
  | { ok: false; loi: string };

export class CongCuNha {
  static readonly DIEU_KHIEN = 'dieu_khien_thiet_bi';
  static readonly MO_MAN_HINH = 'mo_man_hinh';
  static readonly DOC_SO_LIEU = 'doc_so_lieu';

  static readonly NHANH = 'nhanh';
  static readonly TAT_CA = 'tat_ca';

  static readonly DEN = 'den';
  static readonly QUAT = 'quat';
  static readonly DIEU_HOA = 'dieu_hoa';
  static readonly REM = 'rem';
  static readonly TV = 'tivi';
  static readonly BEP_TU = 'bep_tu';
  static readonly NHIET_DO = 'nhiet_do';
  static readonly CHE_DO = 'che_do';
  static readonly LICH_HEN = 'lich_hen';

  static readonly BAT = 'bat';
  static readonly TAT = 'tat';
  static readonly DAO = 'dao';
  static readonly DAT = 'dat';
  static readonly TANG = 'tang';
  static readonly GIAM = 'giam';

  static readonly MUC_HOP_LE = [
    'trang_thai_nha',
    'dien',
    'server',
    'wifi',
    'thoi_tiet',
    'lich_am',
    'cong_tac',
  ];

  private readonly nha: HanhDongNha;
  private readonly guiMqttFlag: boolean;

  constructor(nha: HanhDongNha, guiMqtt = false) {
    this.nha = nha;
    this.guiMqttFlag = guiMqtt;
  }

  chay(ten: string, args: Record<string, unknown>): KetQuaCongCu {
    try {
      switch (ten) {
        case CongCuNha.DIEU_KHIEN:
          return this.dieuKhien(args);
        case CongCuNha.MO_MAN_HINH:
          return this.moManHinh(args);
        case CongCuNha.DOC_SO_LIEU:
          return this.docSoLieu(args);
        default:
          return this.loi(`Không có công cụ tên ${ten}.`);
      }
    } catch (e) {
      if (e instanceof LoiLenh) {
        return this.loi(e.cau);
      }
      const msg = e instanceof Error ? e.message : String(e);
      return this.loi(`Lệnh không chạy được: ${msg}`);
    }
  }

  private dieuKhien(args: Record<string, unknown>): KetQuaCongCu {
    const phong = String(args.phong ?? '').trim().toLowerCase();
    const thietBi = String(args.thiet_bi ?? '').trim().toLowerCase();
    const hanhDong = String(args.hanh_dong ?? '').trim().toLowerCase();
    const giaTriRaw = String(args.gia_tri ?? '').trim();
    const giaTri = giaTriRaw ? giaTriRaw : null;

    if (!thietBi) return this.loi('Chưa biết điều khiển thiết bị gì.');
    if (!hanhDong) return this.loi(`Chưa biết làm gì với ${thietBi}.`);

    if (phong === CongCuNha.NHANH) {
      return this.oNhanh(thietBi, hanhDong);
    }

    const dsPhong = phong === CongCuNha.TAT_CA ? this.nha.maPhong() : [phong];
    if (phong !== CongCuNha.TAT_CA && this.nha.tenPhong(phong) === null) {
      const dsTen = this.nha
        .maPhong()
        .map((id) => this.nha.tenPhong(id) ?? id)
        .join(', ');
      return this.loi(`Nhà mình không có phòng nào tên "${phong}". Đang có: ${dsTen}.`);
    }

    const ketQua: string[] = [];
    let moiCaiDeuThat = true;
    for (const p of dsPhong) {
      const ten = this.nha.tenPhong(p);
      if (ten === null) continue;
      const that = this.nha.laThat(p, thietBi);
      if (!that) moiCaiDeuThat = false;

      if (dsPhong.length > 1) {
        try {
          const r = this.motPhong(p, ten, thietBi, hanhDong, giaTri);
          if (r === null) return this.loi(`Chưa làm được "${hanhDong}" với "${thietBi}".`);
          ketQua.push(r);
        } catch (e) {
          if (e instanceof LoiLenh) {
            ketQua.push(`${ten}: không làm được (${e.cau})`);
            continue;
          }
          throw e;
        }
      } else {
        const r = this.motPhong(p, ten, thietBi, hanhDong, giaTri);
        if (r === null) return this.loi(`Chưa làm được "${hanhDong}" với "${thietBi}".`);
        ketQua.push(r);
      }

      if (this.guiMqttFlag && !that) {
        this.nha.guiMqtt(p, thietBi, hanhDong, giaTri);
      }
    }

    if (ketQua.length === 0) return this.loi('Không có phòng nào để điều khiển.');
    return this.ok(ketQua.join('; '), moiCaiDeuThat);
  }

  private motPhong(
    ma: string,
    ten: string,
    thietBi: string,
    hanhDong: string,
    giaTri: string | null,
  ): string | null {
    switch (thietBi) {
      case CongCuNha.DEN:
      case CongCuNha.QUAT:
      case CongCuNha.DIEU_HOA:
      case CongCuNha.LICH_HEN:
      case CongCuNha.TV:
      case CongCuNha.REM:
      case CongCuNha.BEP_TU: {
        const dangBat = this.nha.dangBat(ma, thietBi);
        if (hanhDong === CongCuNha.DAO && dangBat === null && this.nha.laThat(ma, thietBi)) {
          throw new LoiLenh(
            `Chưa biết ${CongCuNha.tenThietBi(thietBi)} ${ten} đang bật hay tắt — công tắc đang mất kết nối. Bạn muốn bật hay tắt?`,
          );
        }
        const bat = this.batHayTat(hanhDong, dangBat);
        if (bat === null) return null;
        switch (thietBi) {
          case CongCuNha.DEN: this.nha.datDen(ma, bat); break;
          case CongCuNha.QUAT: this.nha.datQuat(ma, bat); break;
          case CongCuNha.LICH_HEN: this.nha.datLichHen(ma, bat); break;
          case CongCuNha.TV: this.nha.datTv(ma, bat); break;
          case CongCuNha.REM: this.nha.datRem(ma, bat); break;
          case CongCuNha.BEP_TU: this.nha.datBepTu(ma, bat); break;
          default: this.nha.datDieuHoa(ma, bat); break;
        }
        let nhiet: number | null = null;
        if (thietBi === CongCuNha.DIEU_HOA && bat && giaTri !== null) {
          const parsed = Number.parseInt(giaTri, 10);
          if (Number.isFinite(parsed)) {
            nhiet = this.nha.datNhietDo(ma, parsed);
          }
        }
        const tenTb = CongCuNha.tenThietBi(thietBi);
        const xong = this.nha.laThat(ma, thietBi) ? 'đã gửi lệnh ' : '';
        if (nhiet !== null) return `${tenTb} ${ten}: ${xong}bật, ${nhiet} độ`;
        if (thietBi === CongCuNha.REM) return `${tenTb} ${ten}: ${xong}${bat ? 'mở' : 'đóng'}`;
        return `${tenTb} ${ten}: ${xong}${bat ? 'bật' : 'tắt'}`;
      }

      case CongCuNha.NHIET_DO:
        switch (hanhDong) {
          case CongCuNha.DAT: {
            const so = giaTri !== null ? Number.parseInt(giaTri, 10) : Number.NaN;
            if (!Number.isFinite(so)) {
              throw new LoiLenh(`Chưa biết đặt điều hòa ${ten} bao nhiêu độ.`);
            }
            const that = this.nha.datNhietDo(ma, so);
            if (that !== so) return `Điều hòa ${ten}: ${that} độ (chỉ nhận 16 đến 30 độ)`;
            return `Điều hòa ${ten}: ${that} độ`;
          }
          case CongCuNha.TANG: {
            const step = giaTri !== null && Number.isFinite(Number.parseInt(giaTri, 10))
              ? Number.parseInt(giaTri, 10)
              : 1;
            return `Điều hòa ${ten}: ${this.nha.chinhNhietDo(ma, step)} độ`;
          }
          case CongCuNha.GIAM: {
            const step = giaTri !== null && Number.isFinite(Number.parseInt(giaTri, 10))
              ? Number.parseInt(giaTri, 10)
              : 1;
            return `Điều hòa ${ten}: ${this.nha.chinhNhietDo(ma, -step)} độ`;
          }
          default:
            return null;
        }

      case CongCuNha.CHE_DO: {
        const input = giaTri ?? hanhDong;
        const cd = CongCuNha.docCheDo(input);
        if (!cd) {
          throw new LoiLenh(
            `Không có chế độ "${input}". Chỉ có: Ngủ, Nhà, Vắng nhà, Tùy chỉnh.`,
          );
        }
        this.nha.datCheDo(ma, cd);
        return `${ten}: chế độ ${CongCuNha.tenCheDo(cd)}`;
      }

      default:
        return null;
    }
  }

  private oNhanh(thietBi: string, hanhDong: string): KetQuaCongCu {
    let id: string;
    switch (thietBi) {
      case CongCuNha.DEN: id = 'light'; break;
      case CongCuNha.QUAT: id = 'fan'; break;
      case CongCuNha.DIEU_HOA: id = 'ac'; break;
      case CongCuNha.REM: id = 'curtain'; break;
      default:
        return this.loi('Ô thao tác nhanh chỉ có đèn, quạt, điều hòa và rèm.');
    }
    const bat = this.batHayTat(hanhDong, this.nha.dangBat(CongCuNha.NHANH, id));
    if (bat === null) return this.loi('Ô thao tác nhanh chỉ bật hoặc tắt được.');
    this.nha.datONhanh(id, bat);
    if (this.guiMqttFlag) {
      this.nha.guiMqtt(CongCuNha.NHANH, thietBi, hanhDong, null);
    }
    return this.ok(`${CongCuNha.tenThietBi(thietBi)} (ô nhanh): ${bat ? 'bật' : 'tắt'}`, false);
  }

  private batHayTat(hanhDong: string, dangBat: boolean | null): boolean | null {
    switch (hanhDong) {
      case CongCuNha.BAT: return true;
      case CongCuNha.TAT: return false;
      case CongCuNha.DAO: return !(dangBat ?? false);
      default: return null;
    }
  }

  private moManHinh(args: Record<string, unknown>): KetQuaCongCu {
    const dich = String(args.dich ?? '').trim().toLowerCase();
    if (!dich) return this.loi('Chưa biết mở màn hình nào.');
    return this.nha.moManHinh(dich)
      ? this.ok(`Đã mở ${dich}`, false)
      : this.loi(`Không có màn hình "${dich}".`);
  }

  private docSoLieu(args: Record<string, unknown>): KetQuaCongCu {
    const muc = String(args.muc ?? '').trim().toLowerCase();
    if (!muc) return this.loi('Chưa biết đọc số liệu gì.');
    if (!CongCuNha.MUC_HOP_LE.includes(muc)) return this.loi(`Không đọc được mục "${muc}".`);
    return { ok: true, so_lieu: this.nha.docSoLieu(muc) };
  }

  private ok(cau: string, that = false): KetQuaCongCu {
    return { ok: true, ket_qua: cau, that };
  }

  private loi(cau: string): KetQuaCongCu {
    return { ok: false, loi: cau };
  }

  static docCheDo(s: string | null | undefined): string | null {
    switch (s?.trim().toLowerCase()) {
      case 'ngu':
      case 'sleep':
        return 'ngu';
      case 'nha':
      case 'o_nha':
      case 'home':
        return 'nha';
      case 'vang_nha':
      case 'vang':
      case 'away':
        return 'vang_nha';
      case 'tuy_chinh':
      case 'custom':
        return 'tuy_chinh';
      default:
        return null;
    }
  }

  static tenCheDo(cd: string): string {
    switch (cd) {
      case 'ngu': return 'Ngủ';
      case 'nha': return 'Nhà';
      case 'vang_nha': return 'Vắng nhà';
      default: return 'Tùy chỉnh';
    }
  }

  static tenThietBi(tb: string): string {
    switch (tb) {
      case CongCuNha.DEN: return 'Đèn';
      case CongCuNha.QUAT: return 'Quạt';
      case CongCuNha.DIEU_HOA: return 'Điều hòa';
      case CongCuNha.REM: return 'Rèm';
      case CongCuNha.TV: return 'Smart TV';
      case CongCuNha.BEP_TU: return 'Bếp từ';
      case CongCuNha.LICH_HEN: return 'Lịch hẹn';
      case CongCuNha.NHIET_DO: return 'Nhiệt độ';
      case CongCuNha.CHE_DO: return 'Chế độ';
      default: return tb;
    }
  }

  static khaiBao(maPhong: string[]) {
    const phongEnum = [...maPhong, CongCuNha.TAT_CA, CongCuNha.NHANH];
    return [
      {
        functionDeclarations: [
          {
            name: CongCuNha.DIEU_KHIEN,
            description:
              'Bật, tắt hoặc chỉnh một thiết bị trong nhà. Dùng cho đèn, quạt, ' +
              'điều hòa, tivi, rèm, bếp từ, nhiệt độ điều hòa, chế độ phòng và lịch hẹn.',
            parameters: {
              type: 'OBJECT',
              properties: {
                phong: {
                  type: 'STRING',
                  description:
                    `Mã phòng. '${CongCuNha.TAT_CA}' là mọi phòng. ` +
                    `'${CongCuNha.NHANH}' là bốn ô thao tác nhanh ở Trang chủ.`,
                  enum: phongEnum,
                },
                thiet_bi: {
                  type: 'STRING',
                  description: 'Thiết bị cần điều khiển.',
                  enum: [
                    CongCuNha.DEN,
                    CongCuNha.QUAT,
                    CongCuNha.DIEU_HOA,
                    CongCuNha.TV,
                    CongCuNha.REM,
                    CongCuNha.BEP_TU,
                    CongCuNha.NHIET_DO,
                    CongCuNha.CHE_DO,
                    CongCuNha.LICH_HEN,
                  ],
                },
                hanh_dong: {
                  type: 'STRING',
                  description:
                    `'${CongCuNha.DAO}' là đảo trạng thái. '${CongCuNha.DAT}' dùng với nhiet_do ` +
                    'hoặc che_do và cần gia_tri.',
                  enum: [
                    CongCuNha.BAT,
                    CongCuNha.TAT,
                    CongCuNha.DAO,
                    CongCuNha.DAT,
                    CongCuNha.TANG,
                    CongCuNha.GIAM,
                  ],
                },
                gia_tri: {
                  type: 'STRING',
                  description:
                    'Nhiệt độ 16-30 viết dạng số, ví dụ "24". Hoặc tên chế độ: ngu, nha, vang_nha, tuy_chinh.',
                },
              },
              required: ['phong', 'thiet_bi', 'hanh_dong'],
            },
          },
          {
            name: CongCuNha.MO_MAN_HINH,
            description: 'Chuyển màn hình đang hiện trên ứng dụng.',
            parameters: {
              type: 'OBJECT',
              properties: {
                dich: {
                  type: 'STRING',
                  description:
                    "Màn hình cần mở. 'phong:<mã phòng>' mở màn điều khiển một phòng, ví dụ 'phong:living'.",
                  enum: [
                    'trang_chu',
                    'thiet_bi',
                    'camera',
                    'cam_bien',
                    'tu_dong_hoa',
                    'cai_dat',
                    'server',
                    'thong_ke',
                    'tien_ich',
                    'chi_tiet_dien',
                    'wifi',
                  ],
                },
              },
              required: ['dich'],
            },
          },
          {
            name: CongCuNha.DOC_SO_LIEU,
            description:
              'Đọc số liệu thật đang có trong nhà. Luôn gọi hàm này trước khi ' +
              'trả lời bất kỳ câu hỏi nào về số - không được tự đoán số.',
            parameters: {
              type: 'OBJECT',
              properties: {
                muc: {
                  type: 'STRING',
                  description:
                    'dien: điện EVN. server: máy chủ serverubuntu. ' +
                    'wifi: modem. thoi_tiet: Open-Meteo. lich_am: lịch âm hôm nay. ' +
                    'trang_thai_nha: đèn/quạt/điều hòa từng phòng (gồm cả thiết bị mẫu). ' +
                    'cong_tac: các công tắc Wi-Fi THẬT — đang nối được không, đang bật hay tắt. ' +
                    'Hỏi thiết bị có bật thật không thì dùng mục này.',
                  enum: [...CongCuNha.MUC_HOP_LE],
                },
              },
              required: ['muc'],
            },
          },
        ],
      },
    ];
  }
}

