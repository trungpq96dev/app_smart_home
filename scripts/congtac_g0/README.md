# Giai đoạn 0 — dò 2 công tắc Wi-Fi phòng ngủ 1

Thư mục **tạm**, không phải một phần của app. Theo `docs/KE-HOACH-CONGTAC.md`, G0 là cổng chặn:
qua được mới viết bridge và mã Kotlin.

Qua rồi thì phần giao thức chuyển vào `scripts/congtac_bridge/driver.py`, các gói `*.jsonl` thành
fixture cho unit test, rồi xoá thư mục này. Không qua thì xoá luôn.

Mọi script ở đây **chỉ đọc**. Không script nào gửi lệnh bật/tắt — `thu_cozylife.py` cố ý không
khai hằng số `cmd 3`.

---

## Chạy bằng gì

`quet_lan.py` và `thu_cozylife.py` chỉ dùng thư viện chuẩn. `nghe_tuya.py` cần một thư viện AES
(`cryptography` hoặc `pycryptodome`) — máy này đã có sẵn `cryptography`.

```powershell
python -I D:\SmartHomeApp\scripts\congtac_g0\quet_lan.py
```

Thiếu AES thì dựng venv ở thư mục tạm, **không cài vào Python hệ thống**:

```powershell
python -m venv "$env:TEMP\venv_congtac"; & "$env:TEMP\venv_congtac\Scripts\python.exe" -m pip install cryptography
```

---

## Kết quả đã đo — 21/11/2026, từ máy Windows 192.168.0.105

Quét toàn dải `192.168.0.2-254` ở hai cổng; cả mạng chỉ có đúng hai máy trả lời.

| | Đèn — Tuya | Quạt — CozyLife |
|---|---|---|
| IP | 192.168.0.100 | 192.168.0.103 |
| MAC | `84:7a:b6:1f:95:fb` | `00:50:c2:ec:bf:27` |
| Cổng mở | 6668 (và chỉ 6668) | 5555 |

**Đèn** — `nghe_tuya.py`, gói quảng bá UDP 6667:

```
device_id : eb74553feb16595327lzam
version   : 3.3
productKey: noxmjxuwck4yvbyi
```

`device_id` và protocol đã có. Còn thiếu đúng một thứ: `local_key` (xem G1 trong kế hoạch).

**Quạt** — `thu_cozylife.py`:

```
did 727700990050c2ecbf27   dtp 02   pid osP4vT   mac 0050c2ecbf27   sv 1.0.0   hv 0.0.1
attr: [1, 2]   data: {'1': 0, '2': 0}
```

---

## Ba chỗ tài liệu gốc ghi SAI — chỉ đo ra mới biết

1. **`sn` phải là chuỗi 13 chữ số mili-giây.** Tài liệu gốc viết `"sn": 1700000000` — số nguyên
   10 chữ số. Gửi như thế thiết bị **im lặng tuyệt đối**: không trả lời, không báo lỗi, chỉ
   timeout. Triệu chứng giống hệt "sai IP" và "thiết bị hỏng". Đây là cái bẫy đắt nhất của cả
   tài liệu.
2. **Thiếu `"pv": 0` cũng im lặng y như vậy.** Không phải trường tuỳ chọn.
3. **`res` nằm ở cấp ngoài cùng, không nằm trong `msg`.** `res:0` ok, `res:1` lỗi — đo được bằng
   cách gửi `"msg":{}`.

Và thiết bị khai **hai** kênh `attr:[1,2]`, không phải một.

---

## Ba phép đo còn lại — cần tay người

### G0-a · `192.168.0.100` là công tắc đèn hay là camera?

Camera Tuya ở `192.168.0.104` (`CAU-HINH.txt` mục 1) **hiện đang tắt** — không ping được, không
mở cổng nào. Nên chưa loại trừ được khả năng `.100` chính là camera đã đổi IP.

**Rút điện công tắc đèn**, rồi:

```powershell
python -I D:\SmartHomeApp\scripts\congtac_g0\quet_lan.py
```

`.100` biến mất = đúng nó là công tắc đèn. Vẫn còn = `.100` là camera, và công tắc đèn chưa tìm
thấy (lúc đó nhánh đèn phải dò lại từ đầu).

### G0-b · Đặt IP tĩnh

Hai IP hiện tại **đều nằm trong dải DHCP động** (`.100–.199` mặc định của TL-WR840N) nên **sẽ đổi**
sau một lần mất điện cả nhà.

`http://192.168.0.1` → **DHCP → Address Reservation → Add New**, gán:

| MAC | IP đề nghị |
|---|---|
| `84:7a:b6:1f:95:fb` (đèn) | `192.168.0.61` |
| `00:50:c2:ec:bf:27` (quạt) | `192.168.0.62` |

Kiểm `DHCP → DHCP Settings` xem dải động thật sự bắt đầu từ đâu rồi chọn IP **ngoài** dải đó.
Save → rút điện hai công tắc 5 giây cho xin lease mới → đối chiếu ở `DHCP → DHCP Client List`.

> TL-WR840N mất hết reservation khi reset về mặc định. Ghi hai dòng này vào `CAU-HINH.txt`.

### G0-c · Kênh nào của CozyLife là quạt — ĐÃ CHỐT: `dpid 1`

> Đo 21/11/2026: gửi `ON` rồi `OFF` qua bridge với `QUAT_DPID=1`, quạt quay thật rồi dừng
> (người nghe xác nhận). Kênh `2` là thứ khác, không đụng tới. Phần dưới giữ lại cho lần
> thêm công tắc sau.

```powershell
python -I D:\SmartHomeApp\scripts\congtac_g0\thu_cozylife.py --ip 192.168.0.103 theo --giay 120
```

Trong lúc nó chạy: **bật rồi tắt quạt bằng nút trên tường**, mỗi thế giữ ~10 giây. Script in ra
kênh nào đổi giá trị. Ghi lại cả **giá trị BẬT thật** (`255` là thường thấy, nhưng đừng tin sẵn).

Không kênh nào đổi thì công tắc cơ không báo về thiết bị — khi đó cả cơ chế "màn hình thấy được
lúc người bấm tay" sụp, và thiết kế phải đổi.

---

## Các script

| File | Việc | Gửi gì đi |
|---|---|---|
| `quet_lan.py` | quét TCP 6668/5555 toàn dải, tra MAC từ ARP | mở socket rồi đóng, 0 byte |
| `nghe_tuya.py` | nghe quảng bá UDP 6666/6667/7000, giải khung, in `gwId`/`version` | không gửi gì |
| `thu_cozylife.py` | `lyLich` (cmd 0) · `hoi` (cmd 2) · `theo` (G0-c) | chỉ lệnh hỏi |

Khoá giải gói quảng bá 6667 là `MD5("yGAdlopoPVldABfn")` — **hằng số công khai của giao thức**,
in trong mọi thư viện Tuya. Nó không phải `local_key` và không mở được thiết bị nào.

## Bẫy khi chạy

* **Windows Firewall** chặn inbound UDP cho `python.exe`. Lần đầu chạy `nghe_tuya.py` sẽ hiện hộp
  thoại — phải chọn **Private networks**. Không hiện hộp thoại mà cũng không nhận gói nào = đã bị
  chặn im lặng từ trước.
* Máy có VirtualBox/VMware/WSL thì nhiều adapter ảo; kiểm bằng `ipconfig` xem đang ở đúng
  `192.168.0.x` chưa.
* **Docker bridge network không nhận được broadcast UDP.** Đó là lý do G0 chạy trần, không Docker.
