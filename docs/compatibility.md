# Yêu cầu và khả năng tương thích

| Thành phần | Bản bàn giao 1.0.0 |
|---|---|
| Android | API 26+; demo compile/target SDK 35 |
| JDK / Gradle / Android Gradle Plugin | 17 / 8.10.2 / 8.7.2 |
| Kotlin | 2.0.21 |
| UI | Android Views; không bắt buộc Compose/AppCompat |
| API Java | Callback façade, module consumer đã compile |
| Native WebRTC | `org.jitsi:webrtc:124.0.0` |
| Mạng | HTTPS/WSS, WebRTC ICE/DTLS-SRTP; STUN/TURN theo môi trường |
| ABI của WebRTC dependency | arm64-v8a, armeabi-v7a, x86, x86_64 |

SDK độc lập, không cần React Native, JavaScriptCore hoặc WebView. Một process có một client hoạt động và một cuộc gọi tại một thời điểm. Không cung cấp video, conference, transfer hoặc API điều khiển ghi âm trong 1.0.0.

Không liên kết nhiều bản thư viện cùng chứa `org.webrtc` trong một app. Kiểm tra dependency graph nếu app đã dùng WebRTC. Không có migration tự động phiên đăng nhập từ SDK nền tảng khác.

Android 12+ giới hạn mở UI/foreground service ở nền; Android 13+ có quyền thông báo; Android 14+ giới hạn microphone while-in-use và full-screen intent. Force-stop có thể chặn push đến khi người dùng mở app. Cần nghiệm thu thực tế Doze, OEM battery policy, DND, khóa màn hình và cold-start.

Đã thử gọi ra, nhận cuộc gọi, âm thanh hai chiều, remote hangup và chuyển loa trên Samsung A23/Android 14 trong môi trường nghiệm thu. Đây không phải chứng nhận toàn bộ thiết bị/OS. API26–27 audio focus legacy, Bluetooth, TURN-only, chuyển mạng và cuộc gọi dài cần kiểm thử theo sản phẩm khách hàng.

Binary WebRTC 64-bit đã kiểm tra ELF LOAD alignment 16 KiB; 32-bit dùng 4 KiB. Kiểm tra cấu trúc không thay thế chạy app/audio trên thiết bị page-size 16 KiB. Kiểm tra APK/AAB cuối với tất cả native dependency trước phát hành.
