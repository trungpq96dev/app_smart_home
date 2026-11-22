import { CongCuNha, type HanhDongNha } from './congCuNha.js';
import { GeminiLive } from './geminiLive.js';
import {
  coKhoaGiongNoi,
  createEmptyPhienGiongNoi,
  dangChayTrangThai,
  defaultCauHinhGiongNoi,
  MucDung,
  TOI_DA_DONG_THOAI,
  type CauHinhGiongNoi,
  type MucDungNgay,
  type PhienGiongNoi,
  type TrangThaiGiongNoi,
} from './types.js';
import { PhatAmWeb, ThuAmWeb } from './webAudio.js';

export class TroLyGiongNoi {
  private readonly nha: HanhDongNha;
  private readonly onPhienChange: (p: PhienGiongNoi) => void;
  private readonly onRecordUsage: (giay: number, tv: number, tr: number, tt: number) => void;
  private readonly getMucDung: () => MucDungNgay[];

  private phienState: PhienGiongNoi = createEmptyPhienGiongNoi();
  private live: GeminiLive | null = null;
  private thu: ThuAmWeb | null = null;
  private phat: PhatAmWeb | null = null;
  private dongHoTimer: number | null = null;
  private canhMicTimer: number | null = null;

  private cauHinh: CauHinhGiongNoi = defaultCauHinhGiongNoi();
  private congCu: CongCuNha | null = null;
  private tokenVaoPhien = 0;
  private tokenRaPhien = 0;
  private tokenTongPhien = 0;
  private giayTruocPhien = 0;
  private mocHoatDong = 0;
  private banSongCong = false;

  constructor(params: {
    nha: HanhDongNha;
    getMucDung: () => MucDungNgay[];
    onPhienChange: (p: PhienGiongNoi) => void;
    onRecordUsage: (giay: number, tv: number, tr: number, tt: number) => void;
  }) {
    this.nha = params.nha;
    this.getMucDung = params.getMucDung;
    this.onPhienChange = params.onPhienChange;
    this.onRecordUsage = params.onRecordUsage;
  }

  get phien(): PhienGiongNoi {
    return this.phienState;
  }

  private emitPhien(updater: (prev: PhienGiongNoi) => PhienGiongNoi): void {
    this.phienState = updater(this.phienState);
    this.onPhienChange(this.phienState);
  }

  private dat(trangThai: TrangThaiGiongNoi): void {
    this.emitPhien((p) => ({ ...p, trangThai }));
  }

  async bat(c: CauHinhGiongNoi): Promise<void> {
    if (dangChayTrangThai(this.phienState.trangThai)) return;

    if (!coKhoaGiongNoi(c)) {
      this.dat({ state: 'loi', cau: 'Chưa khai báo khóa Gemini. Vào Cài đặt → Trợ lý giọng nói.' });
      return;
    }

    if (c.phutMoiNgay > 0) {
      const daDung = MucDung.cuaNgay(this.getMucDung(), MucDung.homNay()).giay;
      if (daDung >= c.phutMoiNgay * 60) {
        this.dat({
          state: 'loi',
          cau:
            `Hôm nay đã nói ${Math.trunc(daDung / 60)} phút, chạm trần ${c.phutMoiNgay} phút. ` +
            'Đổi trần trong Cài đặt nếu muốn nói tiếp.',
        });
        return;
      }
      this.giayTruocPhien = daDung;
    } else {
      this.giayTruocPhien = 0;
    }

    this.tokenVaoPhien = 0;
    this.tokenRaPhien = 0;
    this.tokenTongPhien = 0;
    this.cauHinh = c;
    this.congCu = new CongCuNha(this.nha, c.guiMqtt);
    this.mocHoatDong = Date.now();
    this.emitPhien(() => ({
      trangThai: { state: 'dang_noi' },
      thoai: [],
      daLam: [],
      giayDaChay: 0,
      banSongCong: false,
    }));

    const p = new PhatAmWeb();
    const errLoa = p.batDau();
    if (errLoa) {
      this.tat(errLoa);
      return;
    }
    this.phat = p;

    const t = new ThuAmWeb((bytes) => this.live?.guiTieng(bytes));
    const errMic = await t.batDau();
    if (errMic) {
      this.tat(errMic);
      return;
    }
    this.thu = t;

    this.banSongCong =
      c.banSongCong === 'LUON_BAT'
        ? true
        : c.banSongCong === 'LUON_TAT'
          ? false
          : !t.coKhuVong;
    this.emitPhien((prev) => ({ ...prev, banSongCong: this.banSongCong }));

    const g = new GeminiLive({
      sanSang: () => {
        this.mocHoatDong = Date.now();
        this.dat({ state: 'dang_nghe' });
      },
      tiengNoi: (pcmBytes) => {
        this.mocHoatDong = Date.now();
        if (this.phienState.trangThai.state !== 'dang_tra_loi') {
          this.dat({ state: 'dang_tra_loi' });
        }
        this.phat?.phat(pcmBytes);
      },
      biNgatLoi: () => {
        this.phat?.xoaHangDoi();
        this.dat({ state: 'dang_nghe' });
      },
      hetLuot: () => {
        this.mocHoatDong = Date.now();
        this.dat({ state: 'dang_nghe' });
      },
      loiThoai: (cau, cuaMinh) => {
        this.mocHoatDong = Date.now();
        this.themThoai(cau, cuaMinh);
      },
      goiCongCu: (id, ten, args) => {
        this.mocHoatDong = Date.now();
        if (!this.congCu) return;
        const ketQua = this.congCu.chay(ten, args);
        this.ghiViecDaLam(ten, ketQua);
        this.live?.traKetQuaCongCu(id, ten, ketQua);
      },
      mucDung: (vao, ra, tong) => {
        if (vao > this.tokenVaoPhien) this.tokenVaoPhien = vao;
        if (ra > this.tokenRaPhien) this.tokenRaPhien = ra;
        if (tong > this.tokenTongPhien) this.tokenTongPhien = tong;
      },
      sapHetPhien: () => {
        this.themThoai('(sắp hết phiên, bấm mic để nói tiếp)', false);
      },
      dut: (cau) => {
        this.tat(cau);
      },
    });
    this.live = g;
    g.noi(c, this.loiDan(), CongCuNha.khaiBao(this.nha.maPhong()));

    this.batDongHo();
    if (this.banSongCong) this.batCanhMic();
  }

  tat(vi: string | null = null): void {
    const giay = this.phienState.giayDaChay;
    if (dangChayTrangThai(this.phienState.trangThai) && (giay > 0 || this.tokenTongPhien > 0)) {
      this.onRecordUsage(giay, this.tokenVaoPhien, this.tokenRaPhien, this.tokenTongPhien);
    }

    if (this.dongHoTimer !== null) {
      window.clearInterval(this.dongHoTimer);
      this.dongHoTimer = null;
    }
    if (this.canhMicTimer !== null) {
      window.clearInterval(this.canhMicTimer);
      this.canhMicTimer = null;
    }

    this.thu?.dung();
    this.thu = null;
    this.phat?.dung();
    this.phat = null;
    this.live?.dong();
    this.live = null;
    this.congCu = null;

    this.emitPhien((prev) => ({
      ...prev,
      trangThai: vi === null ? { state: 'tat' } : { state: 'loi', cau: vi },
    }));
  }

  batTat(c: CauHinhGiongNoi): void {
    if (dangChayTrangThai(this.phienState.trangThai)) {
      this.tat();
    } else {
      void this.bat(c);
    }
  }

  xoaLoi(): void {
    if (this.phienState.trangThai.state === 'loi') {
      this.emitPhien(() => createEmptyPhienGiongNoi());
    }
  }

  private batDongHo(): void {
    const batDau = Date.now();
    this.dongHoTimer = window.setInterval(() => {
      const giay = Math.trunc((Date.now() - batDau) / 1000);
      this.emitPhien((p) => ({ ...p, giayDaChay: giay }));

      if (this.cauHinh.giayToiDaPhien > 0 && giay >= this.cauHinh.giayToiDaPhien) {
        this.tat(`Phiên đã chạy ${Math.trunc(giay / 60)} phút nên tự đóng. Bấm mic để nói tiếp.`);
        return;
      }

      if (
        this.cauHinh.phutMoiNgay > 0 &&
        this.giayTruocPhien + giay >= this.cauHinh.phutMoiNgay * 60
      ) {
        this.tat(
          `Đã dùng hết ${this.cauHinh.phutMoiNgay} phút của hôm nay. ` +
            'Mai nói tiếp, hoặc đổi trần trong Cài đặt.',
        );
        return;
      }

      const imLang = Math.trunc((Date.now() - this.mocHoatDong) / 1000);
      if (this.cauHinh.giayImLangTuDong > 0 && imLang >= this.cauHinh.giayImLangTuDong) {
        this.tat();
      }
    }, 1000);
  }

  private batCanhMic(): void {
    let dangKhoa = false;
    this.canhMicTimer = window.setInterval(() => {
      const canKhoa = this.phat?.dangPhat() === true;
      if (canKhoa !== dangKhoa) {
        dangKhoa = canKhoa;
        this.thu?.khoaMic(canKhoa);
        if (canKhoa) this.live?.bangTiengDungLai();
      }
    }, 50);
  }

  private themThoai(cau: string, cuaMinh: boolean): void {
    this.emitPhien((p) => {
      const ds = [...p.thoai];
      const cuoi = ds[ds.length - 1];
      if (cuoi && cuoi.cuaMinh === cuaMinh) {
        ds[ds.length - 1] = { ...cuoi, cau: (cuoi.cau + cau).slice(0, 400) };
      } else {
        ds.push({ cuaMinh, cau });
      }
      return { ...p, thoai: ds.slice(-TOI_DA_DONG_THOAI) };
    });
  }

  private ghiViecDaLam(
    ten: string,
    ketQua: { ok: boolean; ket_qua?: string; loi?: string; that?: boolean },
  ): void {
    const thanhCong = ketQua.ok === true;
    const cau = thanhCong ? (ketQua.ket_qua ?? '') : (ketQua.loi ?? '');
    if (!cau.trim()) return;
    const chuaThat = thanhCong && ten === CongCuNha.DIEU_KHIEN && ketQua.that !== true;
    this.emitPhien((p) => ({
      ...p,
      daLam: [...p.daLam, { cau, thanhCong, chuaCoThietBiThat: chuaThat }].slice(-4),
    }));
  }

  private loiDan(): string {
    const lines: string[] = [];
    lines.push(
      'Bạn là trợ lý nhà thông minh nói tiếng Việt, đang chạy trên bảng điều khiển web ' +
        'SmartHome. Xưng "mình", gọi người dùng là "bạn". ' +
        'Trả lời thật ngắn - một hoặc hai câu, như nói chuyện chứ không phải đọc văn bản. ' +
        'Không đọc lại mã phòng kiểu bed1, hãy nói tên tiếng Việt.\n',
    );
    lines.push('Nhà có các phòng sau (mã - tên):');
    for (const ma of this.nha.maPhong()) {
      lines.push(`- ${ma} - ${this.nha.tenPhong(ma)}`);
    }
    lines.push(
      '\nNgười ta hay gọi phòng bằng nhiều tên khác nhau: "phòng bố mẹ", "phòng lớn", ' +
        '"phòng con"... Hãy tự đoán sang mã phòng gần nhất thay vì hỏi lại, ' +
        'trừ khi thật sự không đoán được.\n',
    );
    lines.push('Trạng thái lúc mở phiên:\n' + this.nha.chupTrangThai() + '\n');
    lines.push(
      'Khi người dùng muốn bật/tắt/chỉnh thiết bị, gọi ngay dieu_khien_thiet_bi ' +
        'rồi xác nhận ngắn gọn. Đừng hỏi lại cho chắc với những việc đảo ngược được dễ dàng.',
    );
    lines.push(
      'Khi người dùng hỏi bất kỳ con số nào, PHẢI gọi doc_so_lieu trước. ' +
        'Tuyệt đối không tự nghĩ ra số. Nếu doc_so_lieu trả về co_so bằng false ' +
        'thì nói thẳng là chưa có số và nói lý do.',
    );

    const that = this.nha.thietBiThat();
    if (that.length === 0) {
      lines.push(
        '\nLƯU Ý QUAN TRỌNG: đèn, quạt, điều hòa, rèm trong nhà này HIỆN CHƯA NỐI ' +
          'thiết bị thật - lệnh chỉ đổi trạng thái hiển thị trên màn hình. ' +
          'Cứ xác nhận bình thường, nhưng nếu người dùng hỏi "đèn có sáng thật không" ' +
          'thì phải nói thật là chưa nối thiết bị.',
      );
    } else {
      lines.push(
        `\nLƯU Ý QUAN TRỌNG: trong nhà này CHỈ những thiết bị sau là thiết bị thật, lệnh vào chúng sẽ bật tắt đồ vật ngoài đời: ${that.join(', ')}.`,
      );
      lines.push(
        'Mọi thiết bị khác - đèn, quạt, điều hòa, rèm của các phòng còn lại - CHƯA ' +
          'nối thiết bị thật, lệnh chỉ đổi trạng thái hiển thị trên màn hình. ' +
          'Cứ xác nhận bình thường, nhưng nếu người dùng hỏi có thật không thì nói thẳng.',
      );
      lines.push(
        'Với thiết bị thật, lệnh đi qua bộ nối trên máy chủ; gửi xong chưa chắc công ' +
          'tắc đã nhận. Đừng khẳng định "đèn đã sáng" - hãy nói "mình đã gửi lệnh bật". ' +
          'Muốn biết chắc thì gọi doc_so_lieu với muc = cong_tac.',
      );
    }
    lines.push(
      'Khi người dùng hỏi mực nước bồn, lưu lượng, nhiệt độ hay lỗi máy bơm thì gọi ' +
        'doc_so_lieu với muc = bom_nuoc. Khi muốn bật/tắt bơm lên bồn, bơm tăng áp, ' +
        'chuyển auto, xoá lỗi hoặc tắt còi thì gọi dieu_khien_thiet_bi với phong = bom_nuoc.',
    );
    return lines.join('\n');
  }
}

