# Xử lý lỗi tích hợp

| Hiện tượng / mã | Kiểm tra |
|---|---|
| Không tìm thấy `vn.caresoft` artifact | Chạy setup, trỏ đúng `packages/maven`, giữ metadata và version `1.0.1` |
| `CLIENT_ALREADY_EXISTS` | Dùng một client ở Application/service; dispose client cũ trước khi tạo lại |
| `MAIN_THREAD_REQUIRED` | Tạo client và `createCall()` trên main thread |
| `LOGIN_REQUIRED` / `AUTH_REQUIRED` | Provider trả C2C mới cho đúng nhân viên; kiểm tra phiên backend |
| `REQUEST_REJECTED` | Kiểm tra App ID, nhân viên và quyền gọi với quản trị Caresoft |
| `NETWORK_UNAVAILABLE` / phiên `UNKNOWN` | Kiểm tra mạng, HTTPS và cấu hình; giữ phiên và thử `checkSession()` khi có mạng |
| `TELECOM_UNAVAILABLE` | Kiểm tra merged manifest, `MANAGE_OWN_CALLS` và khả năng cuộc gọi của thiết bị |
| `MICROPHONE_PERMISSION_REQUIRED` | Xin `RECORD_AUDIO` ở foreground trước answer/start |
| `FOREGROUND_SERVICE_NOT_ALLOWED` | Mở app từ thông báo, kiểm tra quyền micro và giới hạn nền của Android |
| `PUSH_NOT_REGISTERED` / `INVALID_PUSH_CONFIG` | Firebase đúng package; `AppPushConfig`/`PushRegistration` cùng scope; cấu hình backend đúng |
| Nhận push nhưng không thấy UI | Tạo/configure client trước receive; kiểm tra phiên, quyền thông báo/full-screen và hạn chế nền |
| Có thông báo nhưng không có chuông | Kiểm tra kênh **Cuộc gọi đến**, âm lượng chuông, DND và cài đặt thông báo của app |
| Không nhận sau force-stop | Người dùng cần mở app lại |
| `ACTIVE` nhưng không nghe | Thử tiếng hai chiều; quyền micro, thiết bị âm thanh thực tế, âm lượng cuộc gọi, mạng và `mediaState` |
| Đổi thiết bị âm thanh chưa cập nhật | Chờ `AudioRoutesChanged`; lấy id từ `getAudioRoutes()` |
| `CALL_IN_PROGRESS` khi trở lại màn hình | Dùng `getActiveCall()` và bind lại; không tạo cuộc gọi thứ hai |
| Provider demo trống sau connect | Token nhập tay đã được xóa; nhập lại hoặc dùng provider lấy token từ backend |

Khi gửi hỗ trợ, cung cấp phiên bản SDK/app, model/Android, foreground/background, thời điểm, `CallState`/`MediaState` và `VoiceError.code`/`operation`. Chỉ gửi ID đối chiếu cuộc gọi qua kênh được phép. Không gửi token, khóa riêng, raw payload hoặc dữ liệu khách hàng trong log công khai.
