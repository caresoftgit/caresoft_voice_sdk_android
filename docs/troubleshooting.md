# Xử lý lỗi tích hợp

| Hiện tượng / mã | Kiểm tra |
|---|---|
| Không tìm thấy vn.caresoft artifact | Chạy setup, trỏ đúng packages/maven, giữ POM/metadata và version1.0.0 |
| Duplicate org.webrtc classes | Chỉ liên kết một WebRTC dependency tương thích trong app |
| LOGIN_REQUIRED / AUTH_REQUIRED | Provider trả C2C mới cho đúng nhân viên; kiểm tra phiên backend |
| REQUEST_REJECTED | Server từ chối nghiệp vụ; kiểm tra scope app/agent và quyền gọi, không tự giả ACK |
| NETWORK_UNAVAILABLE / UNKNOWN | HTTPS/WSS/mạng; giữ phiên và thử checkSession khi có mạng |
| TELECOM_UNAVAILABLE | MANAGE_OWN_CALLS, ConnectionService trong merged manifest và Telecom trên thiết bị; không xin READ_PHONE_NUMBERS để che lỗi |
| MICROPHONE_PERMISSION_REQUIRED | Xin RECORD_AUDIO ở foreground trước answer/start |
| PUSH_NOT_REGISTERED / INVALID_PUSH_CONFIG | Firebase app/package, scope AppPushConfig/PushRegistration và ánh xạ backend |
| Nhận push nhưng không có UI | Khởi tạo client trước receive, kiểm tra notification/full-screen, FGS/OEM, session và thời hạn message |
| Không nhận sau force-stop | Người dùng cần mở app lại; kiểm tra chính sách Android, không coi push là cơ chế vượt force-stop |
| ACTIVE nhưng không nghe | Kiểm tra hai chiều, quyền mic, audio focus, route thực, âm lượng cuộc gọi, ICE/STUN/TURN và mediaState |
| Đổi route nhưng icon chưa đổi | Đợi AudioRoutesChanged; Telecom xử lý bất đồng bộ; lấy id từ getAudioRoutes |
| Loa ngoài im lặng | Thử tiếng thực tế và âm lượng **cuộc gọi** của từng route; callback không tự chứng minh có âm thanh |
| Có phiên nhưng token provider trống | Demo xóa token nhập tay sau connect; đăng nhập lại hoặc dùng provider backend production |

Khi gửi hỗ trợ: phiên bản SDK/app, model/Android version, trạng thái foreground/background, thời điểm, CallState/MediaState và VoiceError.code/operation. Chỉ cung cấp call correlation ID qua kênh được phép; không gửi token, private key, raw SIP/SDP hoặc dữ liệu khách hàng trong log công khai.
