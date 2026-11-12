# Nội dung gói cài đặt 1.0.0

| Đường dẫn | Nội dung |
|---|---|
| `packages/CaresoftVoice-Android-1.0.0-binary.zip` | Maven repository gồm core/UI AAR và POM/Gradle metadata |
| `packages/caresoft-voice-demo-1.0.0-debug.apk` | APK demo sạch để thử tích hợp; debug signing, không dùng để phát hành store |
| `packages/CaresoftVoice-Android-1.0.0-integration.zip` | Bản ZIP của repo tích hợp: demo, binary, tài liệu, examples và wrapper |
| `packages/release.json`, `packages/SHA256SUMS` | Phiên bản, kích thước, checksum gói cài đặt/APK |
| `scripts/setup-sdk.py` | Kiểm tra SHA-256 và giải nén binary vào `packages/maven` |
| `demo/` | Demo Kotlin dùng binary SDK |
| `consumer-java/`, `integration/examples/` | Ví dụ Java/Kotlin đã compile |
| `docs/` | Hướng dẫn Markdown và HTML |

Core không nhúng WebRTC, OkHttp, Socket.IO hoặc coroutines. Giữ POM/Gradle metadata và repository Google/Maven Central để nhận dependency bắc cầu. ZIP không phải mirror offline toàn bộ Maven Central.

Gói không chứa source SDK/UI, source JAR, file môi trường local, Google services JSON, khóa APNs hoặc keystore phát hành. Class bytecode trong AAR/APK có thể được phân tích như mọi binary Android; bộ bàn giao không hứa bảo vệ mã bằng cơ chế chống dịch ngược.

Khi nâng phiên bản: tải tag tương ứng, kiểm tra checksum, chạy lại setup và build/kiểm thử app. Không đặt code tùy chỉnh trong `packages/maven`; script thay thế thư mục sinh này. Kiểm tra xung đột dependency nếu app đã chứa WebRTC/Socket.IO.

`release.json` ghi checksum binary/APK. `SHA256SUMS` ngoài cùng còn ghi checksum integration ZIP. Integration ZIP không chứa chính nó hoặc file SHA256SUMS ngoài cùng; sau giải nén, setup kiểm tra binary bằng release.json như khi clone repo.
