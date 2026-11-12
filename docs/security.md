# Bảo vệ cấu hình và credential

- Lấy C2C token từ backend đã xác thực và kiểm tra quyền đúng nhân viên. Không đóng gói khóa ký Voice API, Firebase service-account/private key hoặc token đăng nhập vào APK.
- Không log số điện thoại, C2C/FCM token, raw push payload hoặc phản hồi mạng chứa credential. Dùng `VoiceError.code`/`operation`, phiên bản và timestamp khi hỗ trợ.
- SDK mặc định bảo vệ phiên đã lưu bằng Android Keystore. Host bảo vệ phiên backend riêng và tránh đưa token vào Activity saved state, backup, clipboard hoặc crash breadcrumb.
- Demo chỉ giữ token nhập tay trong RAM, xóa khỏi provider sau đăng nhập, chặn screenshot màn hình token và tắt backup dữ liệu app. Khi phiên hết hạn cần nhập token mới.
- Không coi SharedPreferences là nơi lưu bí mật. Chỉ lưu cấu hình cần để khôi phục app, bảo vệ dữ liệu nhạy cảm bằng cơ chế phù hợp.
- Đổi domain/nhân viên: kết thúc cuộc gọi, logout, clear push bridge và dispose client cũ. Kiểm tra `LogoutResult`: xóa credential cục bộ không thay thế xác nhận thu hồi phiên phía server.
- Không tắt TLS validation, đưa credential vào query string hoặc chuyển tiếp token qua redirect sang host khác.
- APK demo dùng debug signing để thử. App phát hành cần applicationId, Firebase và signing riêng của khách hàng.
