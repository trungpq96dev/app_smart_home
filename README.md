# SmartHome Android — khung giao diện

App nhà thông minh cho **Samsung Galaxy Tab A7 Lite** nằm ngang, Kotlin + Jetpack Compose.

Đây là **khung giao diện**, không phải app hoàn chỉnh: mọi thứ bấm được, gạt được, đổi
được — nhưng chưa nối MQTT, Home Assistant hay camera thật.

Build: xem [BUILD.md](BUILD.md). Ảnh chụp từng màn: [docs/screenshots](docs/screenshots).

---

## Cái gì đã có

| Màn | Trạng thái |
|---|---|
| Trang chủ | Đủ: header + 4 ô số liệu + thẻ phòng vuốt ngang + 4 ô thao tác nhanh + dải trạng thái |
| Điều khiển phòng | Đủ: 2 công tắc + điều hòa −/+ + 4 chế độ + lịch hẹn |
| Camera an ninh | Đủ khung: danh sách 4 camera + khung xem + 3 nút |
| Thống kê & Báo cáo | Đủ: 5 tab. **Tab Điện năng dùng số thật từ MQTT**, 4 tab kia là dữ liệu mẫu |
| Cài đặt | Khai báo broker EMQX để đọc số điện thật |
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
| Chọn camera bên trái | Đổi khung xem và nhãn |
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

## Cái gì CHƯA có — và chỗ để cắm vào sau

| Chưa có | Cắm vào đâu |
|---|---|
| Dữ liệu thật cho đèn/quạt/rèm/cảm biến | `data/DemoData.kt` — **toàn bộ số liệu mẫu còn lại nằm đúng một file này** |
| Ảnh phòng, luồng camera | `ui/components/ImagePlaceholder.kt` — đổi thân hàm thành `Image(...)` hoặc bọc `AndroidView` chứa ExoPlayer/libVLC. Mọi chỗ gọi đều đi qua đây |
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
