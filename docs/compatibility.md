# Yêu cầu và khả năng tương thích

| Thành phần | Bản bàn giao 1.0.2 |
|---|---|
| Android tối thiểu | Android 8.0 / API 26 |
| Demo compile/target SDK | 35 |
| JDK / Gradle / Android Gradle Plugin | 17 / 8.10.2 / 8.7.2 |
| Kotlin của demo | 2.0.21 |
| UI tùy chọn | Android Views; không bắt buộc Compose/AppCompat |
| Java | API callback; có module `consumer-java` |

Một process dùng một client và một cuộc gọi tại một thời điểm. Bản 1.0.2 không cung cấp video, hội nghị, chuyển cuộc gọi hoặc API điều khiển ghi âm. Không tự di chuyển phiên đăng nhập từ SDK nền tảng khác.

Host phải có micro, kết nối mạng và khả năng cuộc gọi của Android. Kiểm tra merged manifest và quyền trước khi gọi/nhận; không yêu cầu ứng dụng trở thành dialer mặc định.

- Android 12+: giới hạn mở UI và chạy foreground service từ nền; xin `BLUETOOTH_CONNECT` nếu dùng Bluetooth.
- Android 13+: xin `POST_NOTIFICATIONS` để bảo đảm người dùng thấy thông báo cuộc gọi.
- Android 14+: kiểm tra quyền full-screen intent và giới hạn micro khi app ở nền; cung cấp đường mở app từ thông báo.
- Mọi phiên bản: cài đặt chuông, DND, Doze và chính sách pin của hãng có thể ảnh hưởng thông báo. Force-stop có thể chặn push đến khi người dùng mở app lại.

Khi phát hành, kiểm tra APK/AAB cuối với ABI và yêu cầu page-size của các thiết bị mục tiêu. Build thành công không thay thế kiểm thử nghe/gọi trên thiết bị thật. Dùng [checklist nghiệm thu](testing.md) cho ma trận Android, thiết bị và mạng của sản phẩm.
