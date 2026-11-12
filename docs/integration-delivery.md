# Nội dung gói cài đặt 1.0.1

| Đường dẫn | Nội dung |
|---|---|
| `packages/CaresoftVoice-Android-1.0.1-binary.zip` | Maven repository chứa SDK core/UI và metadata |
| `packages/caresoft-voice-demo-1.0.1-debug.apk` | APK demo thử tích hợp, debug signing |
| `packages/CaresoftVoice-Android-1.0.1-integration.zip` | ZIP bộ tích hợp: demo, binary, tài liệu, examples và wrapper |
| `packages/release.json`, `packages/SHA256SUMS` | Phiên bản, kích thước và SHA-256 |
| `scripts/setup-sdk.py` | Kiểm tra checksum, giải nén binary vào `packages/maven` |
| `demo/` | Demo Kotlin dùng binary SDK |
| `consumer-java/`, `integration/examples/` | Ví dụ Java/Kotlin |
| `docs/caresoft-voice-android.html` | Bộ tài liệu tích hợp đọc offline |

Giữ toàn bộ Maven repository cùng POM/Gradle metadata. Lần resolve đầu cần truy cập Google Maven/Maven Central để tải dependency; gói cài đặt không phải mirror offline toàn bộ các repository này.

Gói không chứa source SDK/UI, source JAR, cấu hình môi trường thực, `google-services.json`, khóa riêng hoặc keystore phát hành. APK demo chỉ để thử; phát hành ứng dụng của bạn bằng signing riêng.

Khi nâng phiên bản: tải bản tương ứng, kiểm tra checksum, chạy lại setup, cập nhật dependency rồi build và nghiệm thu app. Không đặt code tùy chỉnh trong `packages/maven` vì đây là thư mục sinh lại. Không thêm AAR bằng `flatDir` làm mất thông tin dependency.

`release.json` ghi checksum binary/APK. `SHA256SUMS` ngoài cùng còn ghi checksum integration ZIP. Integration ZIP không chứa chính nó hoặc `SHA256SUMS` ngoài cùng; sau giải nén, setup kiểm tra binary bằng `release.json` như khi clone repo.
