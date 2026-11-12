# Demo Caresoft Voice Android 1.0.0

Demo Kotlin gồm màn hình đăng nhập, bàn phím, cài đặt và nghe/gọi. Android 8.0/API26+, JDK17, Android SDK35. Package mặc định `vn.caresoft.voice.demo`.

## Build và chạy

Trong bộ tích hợp khách hàng, chạy trước `python3 scripts/setup-sdk.py` tại thư mục gốc. Sau đó mở Android Studio và chạy module `demo`, hoặc:

```sh
./gradlew :demo:assembleDebug
adb install -r demo/build/outputs/apk/debug/demo-debug.apk
```

APK demo đóng gói sẵn nằm trong `packages/` của bộ tích hợp. APK dùng debug signing, dành cho thử tích hợp. Khi phát hành ứng dụng riêng, dùng applicationId và signing của bạn.

1. Mở **Đăng nhập Caresoft**. Nhập domain, API URL HTTPS, App ID và C2C token do backend cấp; đây là chế độ mặc định. Các trường môi trường đều để trống. Có thể chọn endpoint HTTPS trả `{ "token": "…" }` để thử hoặc nhập Agent ID + Voice API token thủ công trong môi trường kiểm soát. Production phải ký/cấp token ở backend.
2. Token nhập giữ trong RAM, được xóa khỏi provider sau connect; phiên SDK được mã hóa bằng Android Keystore. Demo không tự đăng nhập/gọi khi chạy. Domain/API/App ID/installation ID được lưu để dùng lại; endpoint/ICE tùy chỉnh được lưu mã hóa. Token không lưu trong Activity saved state; đóng/rotate login cần nhập lại.
3. Trong **Cài đặt**, cấp quyền micro, thông báo và Bluetooth nếu cần. Chọn trạng thái sẵn sàng và thiết bị nhận **điện thoại** nếu muốn nhận cuộc gọi. App không tự đổi hai lựa chọn này khi đăng nhập.
4. Bàn phím và danh sách **Số gọi ra** dùng API SDK thật; `callerId` giữ ID tổng đài trả về. Chọn **UI của SDK / UI tự dựng** để đổi giao diện cùng một cuộc gọi. Có answer/reject/hangup, mute, hold, chuyển loa, DTMF và thống kê.
5. **Ẩn màn hình cuộc gọi** chỉ đóng UI; **Trở lại cuộc gọi** bind lại call đang giữ ở Application. Cuộc gọi kết thúc giữ chi tiết tới khi đóng hoặc tạo call tiếp theo.

## Firebase và nhận cuộc gọi

Đăng ký Firebase Android client đúng applicationId. Đặt file Android client của **bạn** ở `demo/google-services.json` cục bộ (git-ignore), rồi build lại. Gradle kiểm tra package và sinh Firebase resource; không cần Google Services Gradle plugin. Không có file này vẫn build được; có thể nhập Firebase client metadata trong cài đặt để thử. Đổi project sau khởi tạo phải restart process. Không đưa service-account/private key vào APK.

Cấu hình cùng Firebase project phía backend/Caresoft và ánh xạ package/environment `default`. Chỉ cùng project Firebase chưa đủ để backend gửi cuộc gọi. Chưa cấu hình Firebase thì chưa nhận được push.

`DemoMessagingService` chuyển data message cho `CaresoftVoicePush.receive()`. `onNewToken()` ghi yêu cầu retry theo thế hệ tài khoản, lên lịch `DemoTokenJobService`. Job xác minh phiên qua `checkSession()` rồi gọi `registerPushToken()`; lỗi mạng/phiên UNKNOWN được retry với backoff. Đổi client hoặc đăng xuất làm vô hiệu công việc cũ. Không lưu FCM token plaintext. Job có thể trì hoãn bởi Doze/OEM, force-stop cần mở app lại.

Khi process được FCM khởi tạo, Application tạo client từ cấu hình đã lưu, SDK xác minh phiên trước xử lý incoming. Provider demo xóa token sau connect nên phiên hết hạn cần đăng nhập lại. Production cần provider có khả năng lấy token mới từ backend đã xác thực. Callback FCM có cửa sổ xử lý 8 giây, cần nghiệm thu với mạng/thiết bị thật.

## Bảo vệ và giới hạn

Demo tắt backup/device-transfer dữ liệu ứng dụng và chặn chụp màn hình nhập token. Nhật ký chỉ giữ tối đa30 tên sự kiện trong RAM, không log số/token/payload; sao chép log do người dùng chủ động.

Incoming dùng self-managed Telecom và foreground service. Lock-screen full-screen phụ thuộc quyền hệ thống (Android14+ có liên kết trong cài đặt); khi bị từ chối dùng thông báo có thể chạm. Nghiệm thu trên thiết bị thật cho background/cold-start, quyền micro/thông báo, DND, Doze, Android12–15, Bluetooth và hạn chế pin OEM. APK build thành công không chứng minh các ca này.

Xem hướng dẫn tích hợp và checklist trong `docs/` ở thư mục gốc.
