# Giai đoạn 0 — đo xem CUKTECH 10 Ultra có phát số liệu qua BLE không

Thư mục **tạm**, không phải một phần của app. Theo `docs/KE-HOACH-CUKTECH.md` mục 5,
giai đoạn 0 là cổng chặn: qua được mới viết collector và mã Kotlin.

Qua rồi thì phần giải mã chuyển vào `scripts/cuktech_collector/giai_ma.py`, các gói
`*.jsonl` thành fixture cho unit test, rồi xoá thư mục này. Không qua thì xoá luôn.

---

## Chạy bằng gì

Các gói `bleak`, `xiaomi-ble` được cài trong một venv riêng ở thư mục tạm của phiên
làm việc, **không** cài vào Python hệ thống:

```
C:\Users\Admin\AppData\Local\Temp\claude\2b4b33576e4842e1\2281f70d-5b96-4894-9b86-75297b5fad0e\scratchpad\venv_ble\Scripts\python.exe
```

Thư mục tạm có thể bị dọn. Dựng lại:

```powershell
python -m venv "$env:TEMP\venv_ble"; & "$env:TEMP\venv_ble\Scripts\python.exe" -m pip install bleak xiaomi-ble
```

Máy Windows này có adapter *Intel(R) Wireless Bluetooth* nên quét BLE được ngay, không
cần ESP32 cũng không cần ra server.

---

## Ba phép đo

### 1. Quét xem quanh đây có gì

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\quet_ble.py" quet --giay 20
```

In bảng mọi thiết bị BLE, đánh dấu con nào phát service data `FE95` (Xiaomi MiBeacon).

### 2. Bám theo củ sạc và cắm/rút tải — **phép đo quyết định**

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\quet_ble.py" theo --loc 3C:CD:73:2B:58:8A --giay 180 --ra goi_co_tai.jsonl
```

Trong lúc nó chạy: cắm một tải vào cổng thứ nhất, để ~20 giây, rút ra, để ~20 giây.
Cột cuối in ra **chỉ số byte đã đổi** so với gói trước. Nếu suốt 3 phút không byte nào
đổi thì củ sạc không phát số theo tải, và cả kế hoạch MiBeacon dừng ở đây.

### 3. Tổng kết file đã ghi

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\quet_ble.py" doi goi_co_tai.jsonl
```

Đếm số payload khác nhau, chỉ ra byte nào từng đổi, và **đọc nghĩa khung MiBeacon**
(cờ frame control, product id, có object số liệu hay không).

Đọc lẻ một khung:

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\quet_ble.py" khung 10590e66008a582b73cd3c
```

### 4. Thử giải mã (khi đã có bindkey)

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\giai_ma_thu.py" goi_co_tai.jsonl --mac 3C:CD:73:2B:58:8A --bindkey <32 ký tự hex>
```

---

## Kết quả đo được — 21/11/2026, từ máy Windows này

Đo bằng adapter *Intel(R) Wireless Bluetooth* của máy, không qua ESP32, không qua server.

**Quét 20 giây:** 30 thiết bị BLE quanh đây, đúng **một** con phát `FE95`:

| | |
|---|---|
| Địa chỉ | `3C:CD:73:2B:58:8A` |
| Tên quảng bá | `njcuk.fitting.ad1204` |
| RSSI | −59 (gần) |
| Firmware (char `0x0004`) | `2.1.2_0073` |
| Product ID | `0x660E` |

**Bám theo 90 giây:** 48 gói, **giống hệt nhau từng byte**, frame counter không hề tăng:

```
10590e66008a582b73cd3c
  frame control 0x5910  version 5  authmode 2
  cờ bật: có MAC, registered       <- KHÔNG có cờ "object"
  product id 0x660e   frame counter 0
  MAC trong khung: 3C:CD:73:2B:58:8A
```

Khung dài đúng 11 byte = frame control (2) + product id (2) + counter (1) + MAC (6).
**Không còn byte nào cho số liệu**, và cờ "object" tắt. Đây là quảng bá nhận dạng thuần tuý.

**Nối GATT (chỉ đọc):** nối được ngay, không cần ghép đôi. Có:

* `1800` Generic Access — tên thiết bị trả về rỗng;
* `FE95` — bộ characteristic xác thực/cấu hình chuẩn của Xiaomi (`0x0010`, `0x0017`–`0x001c`);
* **`AF00` — service riêng của hãng**, với `af07` (write-without-response) và `af08` (notify).
  Đây đúng hình dạng một kênh lệnh: ghi lệnh vào `af07`, nhận trả lời trên `af08`.

**Nghe `af08` + `0x0005` trong 45 giây:** **0 gói**. Thiết bị không tự đẩy số liệu; muốn có
thì phải ghi một lệnh vào `af07` trước — mà mã lệnh đó chưa biết.

### Lần đo 2 — 21/11/2026, kiểm giả thuyết "iPhone đang giữ kết nối"

Câu hỏi: có phải không lấy được số vì củ sạc đã nối với iPhone rồi nên không nói với máy khác?

| Đo | Kết quả |
|---|---|
| Quảng bá 15 giây | vẫn `10590e66008a582b73cd3c`, `khong doi`, RSSI −43 |
| Cờ `connected` trong frame control | **0** — thiết bị tự khai là *không* đang trong kết nối nào |
| Nối GATT lần 2 | **nối được ngay**, liệt kê đủ service như lần 1 |

Nối được tức là **không có ai chiếm đường**: thiết bị BLE một liên kết đang bận thì từ chối
central thứ hai. Nên chuyện iPhone giữ kết nối **không phải** là thứ chặn ở đây.

Nhưng có một cơ chế khác gần giống: bộ characteristic `0x0010`, `0x0017`–`0x001c` trong
service `FE95` là **bắt tay xác thực của Xiaomi**. App trên điện thoại làm xong bắt tay đó
(bằng token của thiết bị) thì kênh `AF00` mới chịu trả lời. Máy này nối vào nhưng chưa xác
thực, nên nghe `af08` được 0 gói. Số liệu không bị iPhone "giữ", mà bị **khoá sau một phiên
xác thực** — và muốn mở thì vẫn phải đi đúng con đường của phương án B.

### Lần đo 3 — 21/11/2026, đối chiếu với dự án sẵn có

Tìm được [`kairui1108/cuktech-ble-server`](https://github.com/kairui1108/cuktech-ble-server)
(MIT). Nó dùng đúng những characteristic máy này đo thấy:

| Dự án | `protocol.py` | Máy này đo |
|---|---|---|
| Auth control | `0x0010` | có |
| Auth data | `0x0019` | có |
| Gửi lệnh | `0x001a` | có |
| **Nhận số liệu (notify)** | `0x001b` | có |
| Device info | `0x001c` | có |
| Firmware | `0x0004` | có, `2.1.2_0073` |

Nên **`AF00`/`af08` không phải kênh số liệu** như tôi đoán lúc đầu — kênh thật là `0x001b`
của service `FE95`, và chỉ mở sau bắt tay MiOT (HKDF + HMAC + AES-CCM) bằng token. Lệnh gửi
đi là MiOT `SIID=2, PIID 1–20`. Giữ `--nghe` mặc định `af08` lại cũng được, nhưng muốn nghe
đúng chỗ thì:

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\do_gatt.py" --mac 3C:CD:73:2B:58:8A --nghe 60 --char 0000001b-0000-1000-8000-00805f9b34fb
```

(Vẫn sẽ im nếu chưa xác thực — đó là điều dự kiến, không phải lỗi.)

Một dự án anh em ghi model đã test là `njcuk.fitting.ad1204` firmware `2.1.2_0073`, **trùng
khít** số máy này bắt được → `3C:CD:73:2B:58:8A` đúng là củ sạc, khỏi cần rút điện thử.

### Nghĩa là gì

Nhánh MiBeacon trong tài liệu gốc **không đi được**: quảng bá không chở số liệu nào, nên
`xiaomi-ble` có bindkey cũng không có gì để giải. Đổi lại, có một kênh GATT riêng của hãng —
tức phương án B (mục 6.2 trong `docs/KE-HOACH-CUKTECH.md`) có cửa, nhưng phải dịch ngược mã
lệnh của app CUKTECH.

> Ba điều **chưa** kiểm được và cần anh: (a) con `3C:CD:73:2B:58:8A` này có đúng là củ sạc
> không; (b) lúc **đang sạc** quảng bá có khác không — các phép đo trên làm lúc không rõ củ sạc
> có tải hay không; (c) bindkey. Xem mục dưới.

### Chưa ghi gì vào thiết bị

Mọi phép đo trên đều chỉ đọc: quét quảng bá, liệt kê service, đọc characteristic có quyền
`read`, bám notify. **Chưa ghi một byte nào vào `af07`** — ghi lệnh lạ vào thiết bị là có thể
đổi cấu hình của nó, nên để anh quyết.

---

## Phần phải tự làm bằng tay

Ba việc này cần tay người và điện thoại, không tự động được:

**a. Xác nhận đúng thiết bị.** Rút điện củ sạc, quét lại; nếu `3C:CD:73:2B:58:8A` biến
mất thì đúng là nó. Cắm lại rồi quét tiếp.

**c. Đo lúc đang sạc.** Các phép đo ngày 21/11 làm lúc không rõ củ sạc có tải hay không.
Chạy hai lệnh dưới đây *trong khi* cắm tải vào cổng thứ nhất, để ~20 giây, rồi rút ra —
để xem có gì đổi không:

```powershell
& $py "D:\SmartHomeApp\scripts\cuktech_g0\quet_ble.py" theo --loc 3C:CD:73:2B:58:8A --giay 120 --ra goi_co_tai.jsonl
& $py "D:\SmartHomeApp\scripts\cuktech_g0\do_gatt.py" --mac 3C:CD:73:2B:58:8A --nghe 120
```

Lệnh đầu vẫn in `khong doi` suốt và lệnh sau vẫn `0 goi notify` thì chốt: thiết bị không
nói gì nếu không được hỏi, và chỉ còn phương án B (dịch ngược) hoặc C (ổ cắm đo điện).

**b. Lấy bindkey.** Thêm củ sạc vào **Mi Home vùng Trung Quốc**, rồi:

```powershell
docker run -it --rm ghcr.io/piotrmachowski/xiaomi-cloud-tokens-extractor:latest
```

Nhập tài khoản Xiaomi, chọn server `cn`. Tìm dòng có MAC `3C:CD:73:2B:58:8A`, lấy
trường **BLE key / beaconkey** (32 ký tự hex) — không phải `token`.

> Bindkey là bí mật thật: có nó là đọc được mọi gói của củ sạc. Đừng commit lên git,
> đừng dán vào chat. Để dành cho `.env` của collector ở giai đoạn 2.

Củ sạc **không vào được Mi Home** mà chỉ vào app CUKTECH riêng thì không có bindkey nào
để lấy → nhánh MiBeacon sụp, xem mục 6.2 trong `docs/KE-HOACH-CUKTECH.md`.
