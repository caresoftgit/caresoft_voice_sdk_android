# Checklist tích hợp

## Build và cài đặt

- Chạy `python3 scripts/setup-sdk.py` và `python3 scripts/check-delivery.py`; build demo/consumer Java bằng JDK17.
- Build không có google-services.json vẫn thành công, nhưng phải cấu hình Firebase đúng package trước khi thử incoming push.
- Kiểm tra merged manifest, dependency graph, signing và native ABI/page-size của APK/AAB cuối.

## Phiên và cấu hình

- Đăng nhập đúng domain/App ID/C2C; token sai/hết hạn, HTTPS lỗi, offline được báo đúng.
- Mở lại app kiểm tra phiên đã lưu; UNKNOWN giữ phiên, không tự đổi thành đăng xuất.
- Logout/đổi agent/tenant không khôi phục credential hoặc công việc đăng ký push của tài khoản cũ.
- Backend cấp token mới không cần đóng gói khóa ký trong app; không log credential.

## Gọi và âm thanh

- Đọc/chọn Mobile và ca trực, tải/chọn đầu số được phép gọi.
- Gọi đi: ringing, bên kia nhận, ACTIVE, nói **hai chiều**, remote/local hangup; UI và system call cùng kết thúc.
- Gọi vào: bấm nhận/từ chối/cancel từ đối ứng; nhận không bị kẹt ở CONNECTING; gọi đến lặp không tạo hai call.
- Mute/unmute, hold/unhold, DTMF, loa trong/loa ngoài/tai nghe/Bluetooth; kiểm tra **tiếng thực tế**, không chỉ icon hoặc callback.
- Xoay/đổi màn hình, vào nền rồi quay lại không tạo/start call lần hai. Mất audio focus không phát mic trái trạng thái OS.
- Mạng Wi-Fi/mobile, TURN-only, rớt mạng/đổi mạng, cuộc gọi dài và hết session timer.

## FCM và nền Android

- FCM token mới khi chưa login, token rotation trong nền/process restart, lỗi đăng ký rồi retry, đổi tài khoản trong lúc job đang chạy.
- Incoming khi app foreground/background/khóa/cold-start, thông báo bị từ chối, full-screen intent không được cấp.
- Android 12–15 microphone/FGS restrictions, Doze/OEM battery policy, DND, reboot và force-stop.
- Đối chiếu đăng ký token với backend; Firebase initialization không chứng minh FCM delivery.

Bản 1.0.0 đã có kiểm thử JVM và build consumer binary; gọi đi/vào và chuyển loa đã được thử trên Samsung A23/Android14. Khách hàng vẫn cần nghiệm thu ma trận thiết bị, mạng và backend riêng; phiên bản 1.0.0 không chứng nhận mọi ca ở trên đã đạt.
