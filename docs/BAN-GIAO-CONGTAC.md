# Bàn giao: Công tắc Wi-Fi phòng ngủ 1 (Smart Life + CozyLife)

> Viết ngày 22/11/2026. Tóm tắt lại toàn bộ việc đã làm trong phiên ngày 20–21/11
> để người tiếp theo sửa tiếp mà không phải đọc lại transcript.
>
> Mọi con số trong file này là **đo thật**, không phải suy luận — chỗ nào chưa
> kiểm được thì có ghi rõ.

## Mục tiêu

Nối 2 công tắc Wi-Fi thật ở phòng ngủ 1 — **đèn** (Tuya/Smart Life) và **quạt**
(CozyLife) — để bật tắt được từ app tablet và từ trợ lý giọng nói.

## Kiến trúc đã chốt

```
Thiết bị (LAN)  <--TCP-->  congtac_bridge (Docker, serverubuntu)  <--MQTT-->  app tablet
```

Bridge nói **thẳng LAN** với thiết bị, không qua cloud. App **không hard-code
thiết bị nào** — danh sách dựng từ `congtac/info`, nên thêm/bớt thiết bị chỉ cần
sửa `.env` trên server, **không build lại APK**.

## Hợp đồng topic

| Topic | Chiều | Nội dung |
|---|---|---|
| `congtac/trangthai` | ra, retained | `online` / `offline` (LWT) |
| `congtac/info` | ra, retained | danh sách thiết bị: id, tên, phòng, driver, ip |
| `congtac/thietbi/<id>` | ra, retained | `{online, is_on, nguon, doc_luc, ly_do_loi}` |
| `congtac/capnhat` | ra, 10 giây/lần | mốc thời gian |
| `congtac/lenh` | **vào** | app ghi lệnh, có `tra_loi_id` để gỡ cờ "Đang gửi lệnh…" |

`nguon` phân biệt `thiet_bi` (người bấm công tắc cơ trên tường) với `lenh` (lệnh
từ app). Bridge **đọc lại từ thiết bị** sau khi đặt, nên `is_on` là sự thật chứ
không phải tiếng vọng của lệnh.

## Hai thiết bị

| | Đèn | Quạt |
|---|---|---|
| Hệ | Tuya | CozyLife |
| Mã cố định | gwId `eb74553feb16595327lzam` | did `727700990050c2ecbf27` |
| MAC | — | `00:50:c2:ec:bf:27` |
| Protocol / cổng | 3.3 / TCP 6668 | TCP 5555 |
| IP lần cuối thấy | `192.168.0.100` | `192.168.0.103` |
| Trạng thái | ⛔ **tắt trong bridge** (`DEN_BAT=0`) | ✅ đã nghiệm thu chạy thật |

Camera Tuya ở `.104` (protocol 3.4, productKey khác hẳn) — đã loại trừ, nên
`.100` chắc chắn là công tắc đèn chứ không phải camera đổi IP.

Quạt khai **hai kênh** `attr:[1,2]` dù công tắc trên tường chỉ có một nút. Đã bật
thật và xác nhận **`dpid 1` là quạt**; kênh `2` **chưa rõ là gì**. Độ trễ đo được
96 ms / 458 ms, trong khi hạn chờ của app là 6 giây.

## Code nằm đâu

| | |
|---|---|
| [scripts/congtac_g0/](../scripts/congtac_g0/README.md) | 3 script dò thủ công |
| [scripts/congtac_bridge/](../scripts/congtac_bridge/README.md) | `driver.py`, `congtac_bridge.py`, `test_driver.py` (31 test), Dockerfile, compose, `cau_hinh.mau.env` |
| [docs/KE-HOACH-CONGTAC.md](KE-HOACH-CONGTAC.md) | kế hoạch đầy đủ |
| [CongTacModels.kt](../app/src/main/java/vn/smarthome/data/congtac/CongTacModels.kt) | `CongTacState`, `gop()`, `LenhCongTac`, `HangChoCongTac` |
| [HomeViewModel.kt](../app/src/main/java/vn/smarthome/vm/HomeViewModel.kt) | một chiều broker → `rooms`; `gatCongTac` chặn **trước khi publish** ở 4 ca |
| [MqttDien.kt](../app/src/main/java/vn/smarthome/data/dien/MqttDien.kt) | gốc thứ 4 `congtac/#` |

Test: `CongTacStateTest` 27, `CongTacManHinhTest` 6, `CongCuNhaTest` +9,
`test_driver.py` 31.

## Luật thiết kế — đừng phá

**Không bịa trạng thái.** Mất kết nối → `is_on: null`, và app **ẩn hẳn công tắc**
chứ không vẽ nó ở thế TẮT. Có hẳn test `mat_ket_noi_thi_KHONG_VE_cong_tac_o_the_tat`
canh chỗ này.

Thiết bị thật thì giọng nói nói *"đã gửi lệnh bật"*, **không** nói *"đã bật"* —
lúc tool trả về thì lệnh mới rời broker. `chupTrangThai()` in "không rõ" thay vì
"tắt" khi mất kết nối, vì nó đi thẳng vào `system_instruction`.

**Nhận diện bằng mã, không bằng IP.** Cổng 5555 cũng là ADB của Android, 6668 thì
camera Tuya cũng mở — thấy cổng mở là **chưa đủ**, phải đối chiếu `did`/`gwId` mới
nhận. Không khai mã thì bridge từ chối, thà chịu offline còn hơn bật nhầm đồ nhà
khác. IP trong `.env` chỉ là gợi ý; sai thì bridge tự quét tìm lại (~1,4 giây).

## Bảy cái bẫy đã trả giá để biết

1. **CozyLife `sn` phải là chuỗi 13 chữ số mili-giây.** Tài liệu gốc ghi 10 chữ số
   → thiết bị **im lặng tuyệt đối**, không lỗi, triệu chứng giống hệt sai IP.
2. **Thiếu `"pv":0`** cũng im lặng y như vậy.
3. **`res` nằm ngoài cùng**, không nằm trong `msg`.
4. **Mật khẩu MQTT ở `~/server_collector/.env`**, KHÔNG phải `~/homeserver/.env` —
   file đó đặt tên biến là `MQTT_PASSWORD` còn bridge đọc `MQTT_PASS`, chép nhầm
   là ra biến rỗng và bridge không nối được broker với lỗi mơ hồ.
5. **`docker compose restart` không đọc lại `.env`** — phải `up -d --force-recreate`.
6. **Server không có `mosquitto_sub`** — soi topic bằng chính image của bridge.
7. **ssh dùng khoá `smarthome_go2rtc`**, đã có trong `~/.ssh/config` kèm bí danh `srv`.

## Git — đã vào `main` hết

| Commit | Nội dung |
|---|---|
| `7d9852c` | app Kotlin + bridge + G0 (30 file) |
| `a4786d2` | tài liệu sau khi nghiệm thu quạt thật |
| `7a1727c` | tự dò IP theo mã thiết bị |

Đều đã merge vào `main`. Hai nhánh `feat/cong-tac-wifi-bed1` và
`feat/tu-do-ip-thiet-bi` **đã gộp xong nhưng còn trên origin**, xoá được.

Mã trên server đã đối chiếu trùng từng byte với repo (trừ ký tự xuống dòng ở hai
file Docker — server nhận bản CRLF từ tarball Windows, vô hại).

## Trạng thái SỐNG lúc 22/11/2026 ~04:25

Container `congtac-bridge` vẫn chạy, nhưng **không thấy quạt**:

```
04:24:32  bed1_quat: mat dau o 192.168.0.103, quet 192.168.0.0/24 cong 5555...
04:24:33  bed1_quat: quet 0 may mo cong 5555, khong may nao dung ma thiet bi.
```

Quét cả dải, **0 máy** mở 5555. Dò độc lập từ máy Windows: `.103`, `.100`, `.104`
đều không bắt được — cả quạt, đèn lẫn camera đều mất.

**Đây là lỗi hạ tầng, không phải lỗi code.** Nhiều khả năng mất điện, hoặc AP /
Wi-Fi của nhánh IoT có vấn đề. Việc đầu tiên nên làm là kiểm tra điện và Wi-Fi của
mấy thiết bị đó, **trước khi sửa bất cứ dòng nào**.

`DEN_LOCAL_KEY` vẫn **RỖNG**, `DEN_BAT=0`.

## Còn lại

1. **`local_key` cho đèn** — thứ duy nhất chặn. Chỉ moi ra được qua `iot.tuya.com`,
   không lấy được từ app Smart Life trên máy chưa root. Bẫy lớn nhất là **chọn sai
   Data Center**: sai thì wizard chạy trơn tru nhưng danh sách thiết bị **rỗng mà
   không báo lỗi gì**. Thử `eu` → `us` → `cn`. Lưu ý: ghép lại đèn vào Smart Life
   là `local_key` **đổi**.

   Lấy xong, sửa `~/congtac_bridge/.env`:

   ```
   DEN_BAT=1
   DEN_LOCAL_KEY=<khoá>
   ```

   rồi `cd ~/congtac_bridge && docker compose up -d --force-recreate`.
   Log ra `Quan 2 thiet bi` là xong. **Không cần build lại APK.**

2. **Quạt đang offline** — xem mục trạng thái sống ở trên.

3. **Tailscale** cho điều khiển từ xa: hạ tầng có sẵn (server `100.123.83.127`, đã
   `AdvertiseRoutes: 192.168.0.0/24`) nhưng **route chưa được duyệt**
   (`PrimaryRoutes` rỗng) và iPhone offline. Vào `login.tailscale.com` → Machines →
   `serverubuntu` → duyệt `192.168.0.0/24`, rồi bật Tailscale trên iPhone kèm
   "Use subnet routes".

4. **Funnel đang phơi ra Internet.** `serverubuntu.tailabb031.ts.net` proxy tới
   `127.0.0.1:8090`, mà cổng đó **không có gì lắng nghe**. Hiện vô hại nhưng là cửa
   bỏ ngỏ — hôm nào có dịch vụ tình cờ bind 8090 là public ngay. `tailscale funnel
   reset` nếu không dùng.

5. **Mosquitto chưa có `acl_file`.** Từ khi có bridge, mất mật khẩu `iot` không còn
   là lộ số liệu nữa mà là **bật tắt được đèn quạt trong nhà** — và mật khẩu đó nằm
   trong cái tablet treo tường. Tuyệt đối không mở broker ra Internet chừng nào chưa
   siết chỗ này.

6. **Kênh `2` của quạt chưa biết là gì.** Dò bằng:

   ```
   python scripts/congtac_g0/thu_cozylife.py --ip 192.168.0.103 theo --giay 120
   ```

   rồi bấm nút trên tường, xem kênh nào nhảy.
