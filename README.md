# SmartHome Android — khung giao diện

App nhà thông minh cho **Samsung Galaxy Tab A7 Lite** nằm ngang, Kotlin + Jetpack Compose.

Hai thứ đã chạy bằng dữ liệu thật: **số điện** (EVN → MQTT) và **hình camera**
(camera Tuya → go2rtc). Phần còn lại — đèn, quạt, rèm, cảm biến — vẫn là khung giao diện
chạy trên số liệu mẫu: bấm được, gạt được, đổi được, nhưng chưa nối tới thiết bị nào.

Build: xem [BUILD.md](BUILD.md). Ảnh chụp từng màn: [docs/screenshots](docs/screenshots).

---

## Cái gì đã có

| Màn | Trạng thái |
|---|---|
| Trang chủ | Đủ: header + 4 ô số liệu + thẻ phòng vuốt ngang + 4 ô thao tác nhanh + dải trạng thái |
| Điều khiển phòng | Đủ: 2 công tắc + điều hòa −/+ + 4 chế độ + lịch hẹn |
| Camera an ninh | **Hình thật từ camera Tuya** qua go2rtc. Chưa khai báo go2rtc thì vẫn là ô giữ chỗ |
| Thống kê & Báo cáo | Đủ: 5 tab. **Tab Điện năng dùng số thật từ MQTT**, 4 tab kia là dữ liệu mẫu |
| Cài đặt | Hai cột: broker EMQX (số điện) và go2rtc (hình camera) |
| Thiết bị / Cảm biến / Tự động hóa | Màn trống, nói rõ "sẽ làm ở giai đoạn sau" |

### Thao tác thật sự hoạt động

Trạng thái nằm ở `HomeViewModel` chứ không nằm trong từng màn, nên **sống qua lúc chuyển
màn và xoay máy**: bật đèn ở màn chi tiết phòng rồi quay về Trang chủ sẽ thấy thẻ phòng đó
đổi thành "Đèn: Bật".

| Thao tác | Kết quả |
|---|---|
| Bấm menu trái | Đổi màn |
| Vuốt thẻ phòng | Đổi phòng, chấm chỉ số chạy theo |
| Bấm "Điều khiển" trên thẻ phòng | Mở màn chi tiết đúng phòng đó |
| Gạt Đèn phòng / Quạt trần | Đổi trạng thái, icon đổi màu theo |
| Bấm − / + Điều hòa | 16–30 °C, kẹp hai đầu |
| Chọn Ngủ / Nhà / Vắng nhà / Tùy chỉnh | Chip đổi trạng thái chọn |
| Bấm 4 ô thao tác nhanh | Bật/tắt, vòng tròn icon sáng/tối |
| Chọn camera bên trái | Đổi luồng đang xem; luồng cũ bị cắt ngay |
| Đổi tab Điện năng → Gas | Biểu đồ, số liệu, đơn vị, màu đổi theo |
| Đổi "7 ngày" → 14 / 30 ngày | Đổi số cột biểu đồ |
| Nút Back của máy | Thoát màn con trước, chưa thoát app ngay |

---

## Điện năng: số thật từ EVN qua MQTT

Tab **Điện năng** ở màn Thống kê không còn dùng dữ liệu mẫu. Nó đọc thẳng từ EMQX theo
hợp đồng topic của dự án `dien_evn` (xem tài liệu bàn giao `bangiao_env_cskh.html`).

Đường đi: EVN CSKH → script Python trên `serverubuntu` → EMQX → app subscribe `dien/#`.
Mọi topic đều **retained** nên vừa kết nối là có số ngay.

| Ô trên màn hình | Lấy từ |
|---|---|
| "Điện năng tiêu thụ … kWh" | cộng `kwh` trong `dien/lichsu` theo khoảng đang chọn |
| Biểu đồ cột | các phần tử cuối của `dien/lichsu`, nhãn thứ tính từ ngày thật |
| Chênh % | so n ngày cuối với n ngày liền trước |
| "Sản lượng ngày gần nhất" | `dien/ngay/sanluong` + `dien/ngay/ngay` |
| "Chi phí tháng này đến giờ" | `dien/thang/tien` + `dien/thang/sanluong` |
| "Trạng thái" | `dien/trangthai`, `dien/loi` và **tuổi** của `dien/capnhat` |
| Chỉ số công tơ · Công nợ | `dien/congto/chiso`, `dien/congno/sotien` |

Khai báo broker ở màn **Cài đặt**. Mật khẩu lưu trong DataStore trên máy và đã loại khỏi
sao lưu đám mây — mã nguồn không nhúng sẵn mật khẩu nào vì nó nằm trên GitHub.

### Ba chỗ đã sửa theo tài liệu bàn giao

1. **Bỏ tile "Công suất hiện tại … kW".** EVN chốt theo ngày, không có số tức thời. Thay
   bằng "Sản lượng ngày gần nhất".
2. **"Chi phí ước tính" → "Chi phí tháng này đến giờ"**, lấy `dien/thang/tien` (luỹ kế cả
   kỳ) thay vì suy từ khoảng đang chọn. Giá điện bậc thang luỹ tiến nên cộng tiền 7 ngày
   rồi nhân ra cả tháng là sai rất xa: tài liệu dẫn ví dụ 48,7 kWh → 126.000đ trong khi
   mức dùng đó cả tháng (~209 kWh) tốn **~480.650đ**.
3. **Không bịa số.** Mất kết nối hoặc chưa có retained thì hiện `--` và nói rõ lý do,
   tuyệt đối không lấy số mẫu bù vào. Bốn tab còn lại vẫn là dữ liệu mẫu và vẫn đeo chip
   "DỮ LIỆU MẪU"; tab Điện năng thì không đeo, thay bằng trạng thái kết nối.

### App không ghi gì vào `dien/`

Số điện do script trên `serverubuntu` quyết định. App chỉ subscribe, không publish — ghi
vào là đè lên số thật của đường ống.

### Một bẫy được bắt sẵn

Tài liệu bàn giao cảnh báo: thiếu ACL `dien/#` trên EMQX thì "script publish được nhưng
bên đọc không nhận gì, mà lỗi lại không rõ ràng". EMQX từ chối subscribe bằng cách trả
granted QoS `0x80` chứ không ném lỗi. `MqttDien.kt` kiểm đúng chỗ đó và báo thẳng
*"Broker từ chối cho đọc dien/#"* thay vì để người dùng đi sửa địa chỉ broker oan.

---

## Camera: hình thật từ camera Tuya

Màn **Camera an ninh** phát hình thật. Camera Tuya không nói chuyện trực tiếp với app —
đường đi là:

```
Camera Tuya  →  go2rtc (serverubuntu, Docker, đăng nhập tài khoản Tuya)  →  WebRTC  →  máy tính bảng
```

go2rtc chạy trên `serverubuntu` (`192.168.0.50:1984`) chứ không phải trên máy Windows nữa —
lý do ở mục *Vì sao chuyển go2rtc sang server* bên dưới.

Khai báo địa chỉ máy chạy go2rtc ở màn **Cài đặt**, cột phải. App hỏi `/api/streams` để biết
go2rtc đang giữ những luồng nào rồi dựng danh sách camera bên trái từ đó — không phải gõ tay
tên từng luồng.

### Vì sao là WebView chứ không phải ExoPlayer

go2rtc phát WebRTC. ExoPlayer không nói WebRTC, phải lui về RTSP — trễ hơn và kéo thêm vài MB
thư viện. Trang `stream.html` sẵn có của go2rtc thì **tự chọn đường truyền** (WebRTC trước,
không được thì MSE), nên app không phải đoán, và go2rtc nâng cấp thì app được hưởng theo mà
không cần build lại. WebView có sẵn trong Android, không thêm phụ thuộc nào.

Toàn bộ nằm ở `ui/components/LuongCamera.kt` — một file, đổi cách phát thì sửa đúng chỗ đó.

### Vì sao chuyển go2rtc sang server

Lúc đầu go2rtc chạy trên máy tính Windows, và xem bị giật rõ trong khi app Tuya trên điện
thoại thì mượt. Đo ra:

| | |
|---|---|
| Băng thông | 810 kbit/s — không nghẽn |
| Gói mất | 0 |
| Jitter | **104 ms** |
| Bộ đệm phải giữ | **376 ms** |

Chỗ quyết định: con số đó đo **qua loopback**, tức chặng go2rtc → người xem không đi qua
mạng nào cả. Vậy mà jitter vẫn 104 ms — nghĩa là nó sinh ra ở chặng **camera → go2rtc**,
nơi cả camera lẫn máy Windows đều chạy Wi-Fi.

App Tuya nối **thẳng** camera → điện thoại: một chặng, đệm một lần. Đường cũ của app này đi
camera → AP → máy Windows → AP → tablet: hình bay qua không khí **bốn lần** và bị đệm hai
lần. `serverubuntu` nối dây (`enp4s0`), nên còn **hai lần**. Chuyển xong thì đỡ giật rõ.

> Không sửa được bằng cách cho app nối thẳng camera. Quét cổng camera `192.168.0.104`: 554
> (RTSP), 80, 443, 8080 đều **đóng**; chỉ 6668 mở, mà đó là cổng điều khiển Tuya chứ không
> phải video. Muốn lấy hình bắt buộc phải qua đúng quy trình Tuya (đăng nhập cloud → MQTT →
> WebRTC P2P), tức là viết lại toàn bộ giao thức đó bằng Kotlin **và** nhét mật khẩu Tuya vào
> máy tính bảng. Không đáng.

### Lần nối đầu chậm, và app tự thử lại

Nối bình thường mất **4–12 giây**: go2rtc phải đăng nhập máy chủ Tuya rồi mới mở được đường
P2P. Trong lúc đó trang `stream.html` của go2rtc in ra dòng đỏ của nó —
`streams: tuya: mqtt: disconnect` — rồi **nằm im, không tự thử lại** khi người xem nối bằng
websocket.

App che kín trang đó bằng lớp *"Đang nối camera..."* của riêng mình, và tự nạp lại khi thấy
trang báo lỗi. Dòng lỗi thô của go2rtc không bao giờ lọt ra màn hình.

> **Thử lại phải thưa, không được dày.** Mỗi lần nạp lại là một lần đăng nhập lại lên máy chủ
> Tuya. Đo ngày 18/11/2026: thử lại 6 lần liên tiếp thì Tuya chặn tạm và **mọi** lần nối sau
> đó đều hỏng; để yên 4 phút rồi thử đúng một lần thì lên hình trong **4,3 giây**. Nên giãn
> cách tăng dần 4s → 15s → 40s, tối đa 3 lần.

### Rời màn là cắt luồng

Đây là phần dễ làm sai nhất và tốn kém nhất nếu sai: không cắt thì camera gửi hình về máy
tính bảng suốt ngày, tốn pin và băng thông, còn go2rtc vẫn đếm máy tính bảng là một người
đang xem. Nên khi khung hình bị gỡ khỏi màn, WebView được nạp `about:blank` rồi huỷ hẳn.

Đo trên máy thật (SM-T225): đang ở màn Camera thì go2rtc đếm **1** người xem, về Trang chủ
còn **0**, quay lại thì lên **1**. Đo lại sau khi chuyển sang server: vẫn đúng như vậy.

### App không bao giờ đăng nhập Tuya

Tài khoản Tuya chỉ nằm trong `go2rtc.yaml` trên máy chạy go2rtc. App chỉ nói chuyện với
go2rtc.

### Chỗ hở ở API go2rtc — đã bịt ngày 17/11/2026

`/api/streams` của go2rtc trả về cả URL nguồn, mà trong URL đó có email và **mật khẩu tài
khoản Tuya** ở dạng chữ thường. go2rtc mặc định không đặt mật khẩu cho API và lắng nghe trên
mọi địa chỉ, nên trước đó **ai vào được Wi-Fi cũng đọc được**.

Đã đặt `api: username/password` trong `go2rtc.yaml`. Giờ gọi từ địa chỉ mạng mà không có tài
khoản thì nhận **401**. App giữ tài khoản đó trong DataStore và đưa cho WebView khi go2rtc
hỏi, nên hình vẫn chạy bình thường.

App vẫn cố ý **chỉ lấy khoá** của JSON và không bao giờ đụng tới phần url — mật khẩu Tuya
không có lý do gì để đi vào app.

### Âm thanh

Camera có micro và go2rtc phát kèm tiếng, nên khung hình **có tiếng**. Muốn tắt thì thêm
`&media=video` vào URL trong `CauHinhCamera.urlTrangPhat()`.

---

## Cái gì CHƯA có — và chỗ để cắm vào sau

| Chưa có | Cắm vào đâu |
|---|---|
| Dữ liệu thật cho đèn/quạt/rèm/cảm biến | `data/DemoData.kt` — **toàn bộ số liệu mẫu còn lại nằm đúng một file này** |
| Ảnh phòng | `ui/components/ImagePlaceholder.kt` — đổi thân hàm thành `Image(...)`. Mọi chỗ gọi đều đi qua đây |
| Ảnh tĩnh / quay video ở màn Camera | Ảnh tĩnh cần `ffmpeg` trên máy chạy go2rtc (`/api/frame.jpeg` trả 500 khi thiếu). Hình trực tiếp thì không cần |
| Điều khiển thiết bị qua MQTT | `vm/HomeViewModel.kt` — các hàm toggle đổi thành gửi lệnh rồi chờ thiết bị xác nhận. Lớp MQTT đã có sẵn ở `data/dien/MqttDien.kt` để dùng lại |
| 3 màn Thiết bị / Cảm biến / Tự động hóa | `ui/screens/PlaceholderScreen.kt` (màn Cài đặt đã làm xong) |
| Chế độ sáng, font Inter, ký release | Xem BUILD.md |

---

## Vì sao có chip "DỮ LIỆU MẪU"

Mọi con số trên màn hình (26.5 °C, 68 %, 1.25 kW, 48.7 kWh, 126.000 đ…) đều do người viết
đặt ra cho giao diện nhìn được, **không phải số đọc từ thiết bị**.

Tài liệu `Design Specification` của dự án AquaControl có quy tắc *"Không có số liệu giả"*.
Khung giao diện thì bắt buộc phải có số mẫu mới nhìn ra được bố cục, nên cách xử lý là:
gom hết vào một file, và **nói thẳng trên màn hình** rằng đó là số mẫu.

Tắt chip: đặt `DemoData.IS_DEMO = false`.

Ba nút ở màn Camera (Chụp ảnh / Quay video / Toàn màn hình) cũng theo nguyên tắc đó — bấm
vào thì báo *"Chưa nối camera thật"* chứ không giả vờ đã chụp được.

---

## Cấu trúc

```
data/
  Models.kt     RoomUi, QuickItem, CameraFeed, StatSeries, RoomMode, StatKind, StatRange
  DemoData.kt   TOÀN BỘ số liệu mẫu
  dien/         CauHinhMqtt, DienState, MqttDien — số điện thật qua EMQX
  camera/       CauHinhCamera, Go2rtc — hình camera thật qua go2rtc
vm/
  HomeViewModel.kt   State + các hàm toggle
ui/
  theme/        Color, Dimens (ngân sách 893×533dp), Type, Theme
  components/   SmartCard, StatTile, QuickActionTile, DeviceSwitchRow, TempStepperRow,
                ChoiceChip, ImagePlaceholder, BarChart, ArcGauge, PagerDots…
  shell/        Nav, SideNav, TopBar, AppShell, Clock
  screens/      Home, RoomDetail, Camera, Stats, Placeholder
```

### Hai quyết định đáng nói

**Không dùng Navigation-Compose.** App chỉ có vài màn phẳng; một biến `current` trong
`AppShell` là đủ, bớt được một thư viện và toàn bộ phần khai báo route.

**Không dùng thư viện biểu đồ.** Biểu đồ cột và đồng hồ cung vẽ bằng `Canvas` thuần
(`ui/components/Charts.kt`), đủ dùng ở kích thước này và không kéo thêm phụ thuộc nào.

Phụ thuộc cố ý rút gọn: không Room, không Retrofit, không OkHttp, không MQTT, không KSP —
khung giao diện không cần, giữ lại chỉ làm APK nặng và build chậm.

---

## Ràng buộc kích thước — điểm dễ làm hỏng nhất

Số đo thật trên máy (SM-T225, Android 13), không phải ước lượng:

```
adb shell wm size      ->  Physical size: 800x1340
adb shell wm density   ->  Physical density: 213      (tvdpi)
```

213 dpi → density 1,33125 → vùng vẽ **1006 × 601 dp**.

> Dự án AquaControl ghi 893 × 533 dp vì đoán density là 1.5. **Con số đó sai với máy này.**
> Đừng chép lại.

App chạy kiosk (ẩn cả thanh trạng thái lẫn thanh điều hướng) nên được dùng trọn 1006 × 601 dp.
Để nguyên hai thanh thì chỉ còn 1340 × 780 px và có một dải đen ở cạnh trên — đo được bằng
`dumpsys window displays`, `app=1340x780`. Không muốn kiosk: xoá lời gọi `hideSystemBars()`
trong `MainActivity`.

**Chiều cao vẫn là phía eo.** Đây là chỗ hay làm hỏng layout chép từ ảnh thiết kế, nên sidebar
và thanh tiêu đề cố định, phần còn lại chia theo `weight`, và các dải cố định (header 64dp,
hàng ô số liệu 88dp, dải trạng thái 40dp) được tính sao cho tổng không vượt ngân sách.
Xem chú thích trong `ui/theme/Dimens.kt`.

Sửa bố cục xong thì chạy `build.bat testDebugUnitTest` rồi xem lại ảnh trong
`app\build\screenshots\` — rẻ hơn nhiều so với cài lại lên máy tính bảng mỗi lần.
