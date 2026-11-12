# Caresoft Voice SDK for Android

Tích hợp nghe/gọi vào ứng dụng Android bằng Kotlin hoặc Java. SDK cung cấp phiên nhân viên, gọi đi/nhận cuộc gọi, FCM bridge, self-managed Telecom và giao diện cuộc gọi tùy chọn.

**Phiên bản 1.0.0 · Android 8.0/API 26 trở lên · JDK 17**

## Bắt đầu

1. Chuẩn bị domain, API URL, App ID và backend cấp C2C token theo [hướng dẫn cấu hình](docs/backend-contract.md). Đăng ký package Android và Firebase project của bạn với Caresoft.
2. Tải repo và cài gói SDK:

```sh
git clone https://github.com/caresoftgit/caresoft_voice_sdk_android.git
cd caresoft_voice_sdk_android
python3 scripts/setup-sdk.py
```

3. Mở thư mục repo bằng Android Studio, cài Android SDK 35 và dùng JDK 17.
4. Chạy module `demo`, nhập cấu hình ứng dụng và C2C token của bạn. Xem [demo/README.md](demo/README.md).
5. Thêm SDK vào ứng dụng riêng theo [hướng dẫn tích hợp](docs/android-integration-guide.md).

Script cài đặt kiểm tra SHA-256 trước khi giải nén Maven repository vào `packages/maven`. Demo dùng binary `vn.caresoft:voice-android:1.0.0` và `vn.caresoft:voice-android-ui:1.0.0`. Không cần mã nguồn SDK để build.

## Tài liệu

- [Hướng dẫn tích hợp từng bước](docs/android-integration-guide.md) · [HTML đọc offline](docs/caresoft-voice-android.html)
- [API Kotlin/Java](docs/api.md)
- [Backend, token và Firebase](docs/backend-contract.md)
- [Khả năng tương thích](docs/compatibility.md)
- [Checklist nghiệm thu](docs/testing.md) · [Xử lý lỗi](docs/troubleshooting.md)
- [Bảo vệ credential](docs/security.md) · [Nội dung gói cài đặt](docs/integration-delivery.md)
- [Ví dụ Kotlin](integration/examples/kotlin/SdkIntegrationExample.kt) · [Consumer Java](consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java)

Demo và APK bàn giao không chứa tài khoản, App ID thật, Voice API token, APNs key, Firebase client/service-account config hoặc STUN/TURN của môi trường thử. SDK không tự đăng nhập hay gọi khi mở demo. Khóa ký Voice API và Firebase service-account key thuộc backend của khách hàng, không đưa vào APK.

Đây là bộ tích hợp binary và demo. Quyền sử dụng SDK theo thỏa thuận với Caresoft; dependency bên thứ ba có giấy phép riêng, xem [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
