# Bảo vệ cấu hình và credential

- Lấy C2C token từ backend đã xác thực. Không đóng gói Voice API signing secret, Firebase service account/private key, mật khẩu SIP hoặc session token vào APK.
- Không log raw push payload, SIP/SDP, số điện thoại, C2C/FCM token hoặc phản hồi chứa credential. Dùng `VoiceError.code`/`operation`, phiên bản và timestamp để trao đổi hỗ trợ.
- SDK mặc định bảo vệ phiên lưu bằng Android Keystore. Demo tắt backup, không lưu token nhập tay sau đăng nhập và chặn screenshot ở màn hình token. Host vẫn phải bảo vệ backend và vòng đời phiên riêng.
- TURN credential và token endpoint nhập trong demo được mã hóa trong no-backup storage. Cấu hình nonsecret như domain/App ID được giữ để khôi phục phiên; không coi SharedPreferences là nơi lưu bí mật.
- Đổi tenant/nhân viên: logout rồi dispose client cũ. Kiểm tra `LogoutResult` khi server chưa xác nhận; xóa phiên cục bộ không thay thế thu hồi credential phía server.
- Không tắt TLS validation, không theo redirect để gửi token sang host khác, không đưa credential vào query string hoặc crash breadcrumb.
- APK demo dùng debug signing để thử. App production cần applicationId/Firebase/signing riêng và quy trình quản lý khóa của khách hàng.
