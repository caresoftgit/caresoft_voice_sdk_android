# Demo Caresoft Voice Android 1.0.2

Demo Kotlin gồm đăng nhập, bàn phím, cài đặt và màn hình nghe/gọi. Yêu cầu Android 8.0/API 26+, JDK 17, Android SDK 35. Package mặc định `vn.caresoft.voice.demo`; versionCode `10202`.

## Build và chạy

Tại thư mục gốc bộ tích hợp:

```sh
python3 scripts/setup-sdk.py
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
```

Hoặc mở repo bằng Android Studio và chạy module `demo`. APK sẵn có trong `packages/` dùng debug signing để thử tích hợp. Khi phát hành app riêng, dùng applicationId và signing của bạn.

1. Mở **Đăng nhập Caresoft**. Nhập domain, API URL HTTPS, App ID và C2C token do backend cấp. Các trường môi trường ban đầu để trống; demo không tự đăng nhập/gọi.
2. Có thể thử endpoint HTTPS trả `{ "token": "…" }`. Endpoint demo không tự gửi thông tin xác thực của app. Production phải dùng `TokenProvider` gọi backend đã xác thực của bạn. Chế độ Agent ID + Voice API token chỉ phục vụ thử có kiểm soát; không đưa khóa ký vào app phát hành.
3. Trong **Cài đặt**, cấp quyền micro, thông báo và Bluetooth nếu cần. Chọn trạng thái sẵn sàng và thiết bị nhận **điện thoại** để nhận cuộc gọi. Đăng nhập không tự đổi các lựa chọn này.
4. Tải/chọn **Số gọi ra**, nhập số rồi gọi. `callerId` dùng ID được trả về, không dùng nhãn hiển thị.
5. Chọn **UI của SDK / UI tự dựng** để thử trên cùng một cuộc gọi. UI SDK hiển thị nhận/từ chối, trạng thái kết nối, thời lượng, mute, hold, thiết bị âm thanh, bàn phím DTMF và kết quả sau khi kết thúc. Điều khiển chỉ bật khi trạng thái cho phép.
6. **Ẩn màn hình cuộc gọi** chỉ đóng UI; **Trở lại cuộc gọi** hiển thị lại cuộc gọi hiện tại. Xoay màn hình không tạo cuộc gọi mới. Cuộc gọi kết thúc giữ chi tiết tới khi đóng hoặc tạo cuộc gọi tiếp theo.

## Nâng phiên bản

Bộ này dùng core/UI `1.0.2`, versionCode `10202`. Khi cập nhật từ bản trước, chạy lại setup và build để tránh dùng binary cũ trong `packages/maven`. Demo không cần tùy chọn chế độ nội bộ; khách hàng chỉ nhập cấu hình được cấp cho ứng dụng của mình. Không sử dụng bản APK riêng đã cấu hình tài khoản làm bộ bàn giao.

## Firebase và nhận cuộc gọi

Đăng ký Firebase Android client đúng applicationId. Đặt file Android client của **bạn** ở `demo/google-services.json` cục bộ (git-ignore), rồi build lại. Demo tự đọc file này khi build; không cần thêm Google Services Gradle plugin vào demo. Không có file vẫn build được nhưng chưa nhận được push. Có thể nhập Firebase client metadata trong **Cài đặt** để thử; đổi Firebase project sau khởi tạo cần khởi động lại app.

Cấu hình cùng Firebase project phía backend/Caresoft và ánh xạ đúng package, App ID và environment `default`. Firebase service-account/private key chỉ lưu phía server. Xem [cấu hình backend](../docs/backend-contract.md).

`DemoMessagingService` chuyển data message tới `CaresoftVoicePush.receive()`. `onNewToken()` lên lịch đăng ký lại qua `DemoTokenJobService`; job xác minh phiên, lấy FCM token mới nhất và retry khi mạng/phiên chưa sẵn sàng. Đăng xuất/đổi tài khoản hủy hiệu lực yêu cầu cũ. Production cần xử lý token rotation cả khi Activity chưa mở.

Khi app được push khởi động, Application tạo client từ cấu hình đã lưu trước khi xử lý thông báo. Token nhập tay chỉ giữ trong RAM và được xóa khỏi provider sau đăng nhập; phiên hết hạn cần nhập lại. Production dùng provider có thể lấy token mới từ backend đã xác thực.

## Chuông, quyền và nền Android

Thông báo cuộc gọi đến có chuông/rung theo cài đặt hệ thống. Kiểm tra quyền thông báo, kênh **Cuộc gọi đến**, âm lượng chuông và DND. UI cuộc gọi không tự phát thêm chuông.

Quyền micro phải được cấp khi app ở foreground trước khi gọi/nhận. Full-screen trên màn hình khóa phụ thuộc cài đặt hệ thống; Android 14+ có liên kết cấp quyền trong demo. Khi không được cấp, dùng thông báo để mở app. Force-stop cần người dùng mở lại app.

Thử trên thiết bị thật cho foreground/background/khóa/cold-start, quyền bị từ chối, Doze, hạn chế pin OEM, Bluetooth và âm thanh hai chiều. Xem [checklist](../docs/testing.md) và [hướng dẫn tích hợp UI](../docs/android-integration-guide.md#8-ui-cuộc-gọi).

Token không lưu trong Activity saved state; rời/rotate màn hình đăng nhập cần nhập lại. Demo bảo vệ phiên bằng Android Keystore, tắt backup dữ liệu và chặn chụp màn hình token. Nhật ký chỉ giữ tên sự kiện trong RAM; không log token, số điện thoại hoặc payload.
