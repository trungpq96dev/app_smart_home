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
| Thống kê & Báo cáo | Đủ: 5 tab, biểu đồ cột, đồng hồ cung, chi phí, trạng thái |
| Thiết bị / Cảm biến / Tự động hóa / Cài đặt | Màn trống, nói rõ "sẽ làm ở giai đoạn sau" |

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

## Cái gì CHƯA có — và chỗ để cắm vào sau

| Chưa có | Cắm vào đâu |
|---|---|
| Dữ liệu thật | `data/DemoData.kt` — **toàn bộ số liệu mẫu nằm đúng một file này** |
| Ảnh phòng, luồng camera | `ui/components/ImagePlaceholder.kt` — đổi thân hàm thành `Image(...)` hoặc bọc `AndroidView` chứa ExoPlayer/libVLC. Mọi chỗ gọi đều đi qua đây |
| MQTT / Home Assistant | `vm/HomeViewModel.kt` — thay khởi tạo từ `DemoData` bằng repository, các hàm toggle đổi thành gửi lệnh rồi chờ thiết bị xác nhận |
| 4 màn Thiết bị / Cảm biến / Tự động hóa / Cài đặt | `ui/screens/PlaceholderScreen.kt` |
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
