# Checklist tích hợp 1.0.2

## Build và cấu hình

- Chạy `python3 scripts/setup-sdk.py` và `python3 scripts/check-delivery.py`; build demo/consumer Java bằng JDK 17.
- Dùng cả hai dependency phiên bản `1.0.2`; giữ Maven metadata khi chép sang app riêng.
- Cấu hình Firebase đúng applicationId và cấu hình gửi push phía backend; không đưa khóa ký hoặc service-account vào app.
- Kiểm tra merged manifest, signing, ABI/page-size và yêu cầu phát hành của APK/AAB cuối.

## Phiên và nhân viên

- Đăng nhập đúng cấu hình; token sai/hết hạn, HTTPS lỗi và offline báo lỗi rõ ràng.
- Mở lại app và kiểm tra phiên đã lưu; `UNKNOWN` không tự bị coi là đã đăng xuất.
- Provider lấy được token mới từ backend đã xác thực, không cần đóng gói khóa ký.
- Logout/đổi tài khoản không khôi phục phiên hoặc đăng ký push của nhân viên cũ.
- Chọn thiết bị điện thoại, trạng thái sẵn sàng và đầu số được phép gọi.

## Cuộc gọi và UI

- Gọi đi: đổ chuông, nhận, `ACTIVE`, tiếng **hai chiều**, local/remote hangup.
- Gọi đến: nhận/từ chối, đối ứng hủy, lặp push không tạo hai cuộc gọi, không kẹt ở kết nối.
- Mute/unmute, hold/unhold, DTMF, loa trong/loa ngoài/tai nghe/Bluetooth; xác nhận tiếng thực tế.
- `VoiceCallView`: incoming, active, bàn phím DTMF, nhãn đầu số, thời lượng và thông tin kết thúc.
- Xoay màn hình, ẩn/quay lại UI và đổi UI không tạo/start lại call; `unbind()` không kết thúc cuộc gọi.
- Cuộc gọi đã nhận/kết thúc từ thiết bị khác: UI đóng đúng, không gửi thêm lệnh từ chối; cuộc gọi tiếp theo vẫn nhận được.
- Giữ kết quả `ENDED` đến khi người dùng đóng; cuộc gọi tiếp theo tạo object mới.
- Wi-Fi/mobile data, VPN/firewall của sản phẩm, mạng yếu/đổi mạng và cuộc gọi dài.

## Push, chuông và nền Android

- Token mới khi chưa login, token rotation trong nền, process restart, retry lỗi và đổi tài khoản lúc job đang chạy.
- Incoming ở foreground/background/khóa/cold-start, cả khi không có Activity.
- Chuông/rung theo kênh **Cuộc gọi đến**, âm lượng chuông và DND; nhận/từ chối/kết thúc dừng thông báo phù hợp.
- Từ chối quyền micro/thông báo/Bluetooth/full-screen và mở lại phần cài đặt để cấp quyền.
- Android 12–15, Doze, chính sách pin OEM, reboot và force-stop.
- Đối chiếu đăng ký token phía backend; chỉ khởi tạo Firebase chưa chứng minh nhận được cuộc gọi.

Ghi nhận thiết bị, Android, phiên bản app/SDK, mạng và kết quả từng ca. APK build thành công hoặc kết quả trên một máy không chứng nhận toàn bộ các ca trên.
