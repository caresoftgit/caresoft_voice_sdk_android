# Caresoft Voice SDK for Android

Tích hợp nghe/gọi vào ứng dụng Android bằng Kotlin hoặc Java: đăng nhập nhân viên, gọi đi, nhận cuộc gọi, thông báo và giao diện cuộc gọi tùy chọn.

**Phiên bản 1.0.1 · Android 8.0/API 26 trở lên · JDK 17**

## Bắt đầu

1. Chuẩn bị domain, API URL HTTPS, App ID và backend cấp C2C token theo [hướng dẫn cấu hình](docs/backend-contract.md). Đăng ký package Android và Firebase project của bạn với Caresoft.
2. Tải repo và cài SDK:

```sh
git clone https://github.com/caresoftgit/caresoft_voice_sdk_android.git
cd caresoft_voice_sdk_android
python3 scripts/setup-sdk.py
```

3. Mở repo bằng Android Studio, cài Android SDK 35 và dùng JDK 17.
4. Chạy module `demo`, nhập cấu hình ứng dụng và C2C token của bạn. Xem [hướng dẫn demo](demo/README.md).
5. Tích hợp vào app riêng theo [hướng dẫn Kotlin/Java](docs/android-integration-guide.md).

Script cài đặt kiểm tra SHA-256 trước khi giải nén Maven repository vào `packages/maven`. Demo dùng `vn.caresoft:voice-android:1.0.1` và `vn.caresoft:voice-android-ui:1.0.1`. Không cần mã nguồn SDK để build.

## Tài liệu

- [Hướng dẫn tích hợp](docs/android-integration-guide.md) · [Bộ tài liệu HTML đọc offline](docs/caresoft-voice-android.html)
- [API Kotlin/Java](docs/api.md) · [Backend, token và Firebase](docs/backend-contract.md)
- [Yêu cầu Android](docs/compatibility.md) · [Checklist nghiệm thu](docs/testing.md)
- [Xử lý lỗi](docs/troubleshooting.md) · [Bảo vệ credential](docs/security.md)
- [Nội dung gói cài đặt](docs/integration-delivery.md) · [Changelog](CHANGELOG.md)
- [Ví dụ Kotlin](integration/examples/kotlin/SdkIntegrationExample.kt) · [Ví dụ Java](consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java)

Demo không chứa tài khoản, App ID, token hoặc cấu hình môi trường thực. SDK không tự đăng nhập hay gọi khi mở demo. Khóa ký Voice API và Firebase service-account key chỉ được lưu ở backend của bạn.

Quyền sử dụng SDK theo thỏa thuận với Caresoft. Xem [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) cho giấy phép của các thành phần đi kèm.
