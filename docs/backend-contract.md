# Cấu hình backend, token và Firebase

## Thông tin cần chuẩn bị

| Thông tin | Nơi cung cấp | Cách dùng |
|---|---|---|
| Domain/tenant | Caresoft | Tenant của nhân viên |
| API URL HTTPS | Caresoft | `VoiceOptions.apiBaseUrl` |
| Voice App ID | Caresoft Admin | `VoiceOptions.appId` |
| Agent identity | Hệ thống khách hàng/Caresoft | Backend xác thực đúng nhân viên |
| C2C token | Backend khách hàng | `TokenProvider.getToken()` |
| Android applicationId | Ứng dụng khách hàng | Trùng package đã đăng ký cho push |
| Firebase project | Khách hàng | Android client và cấu hình gửi FCM phía server |
| STUN/TURN | Caresoft/nhà cung cấp media | `VoiceOptions.iceServers` theo môi trường |

Liên hệ quản trị Caresoft để đăng ký ứng dụng thoại cho domain, khai báo package Android và ánh xạ cấu hình push. Tên màn hình cấu hình có thể khác theo phiên bản Admin. Không sao chép App ID hoặc Firebase project của ứng dụng mẫu khác.

## C2C token

App đăng nhập backend của khách hàng bằng cơ chế xác thực đang có. Backend xác minh nhân viên, tạo C2C token theo cấu hình Voice API được Caresoft cấp và trả token cho app qua HTTPS. Khóa Voice API dùng để ký chỉ lưu phía backend; không đặt trong source, BuildConfig, resource hay remote config mà client đọc được.

`TokenProvider` phải trả **C2C token**, không trả Voice API signing secret, Firebase token hoặc token phiên của SDK. Provider có thể được gọi lại khi SDK cần xác thực. Bảo vệ endpoint bằng phiên app, kiểm tra quyền theo nhân viên/tenant và không log token.

Demo hỗ trợ endpoint HTTPS trả:

```json
{ "token": "C2C_TOKEN_ISSUED_FOR_THIS_AGENT" }
```

Endpoint demo không tự gắn header xác thực của backend khách hàng; đây chỉ là cách thử. Production hãy triển khai `TokenProvider` bằng HTTP client đã xác thực của app. Demo từ chối redirect, URL chứa user/password và phản hồi quá 64 KiB.

Chế độ Agent ID + Voice token trong demo chỉ phục vụ thử có kiểm soát: nhập thủ công, không lưu signing secret. Không giữ cách ký tại client trong ứng dụng phát hành.

## Firebase và cuộc gọi đến

1. Tạo/chọn Firebase project do khách hàng quản lý; đăng ký Android applicationId chính xác.
2. Tải `google-services.json` Android client và đặt **cục bộ** tại `demo/google-services.json`; file đã được git-ignore. Nếu đổi applicationId trong Gradle, đăng ký Android client tương ứng.
3. Cấu hình Firebase project gửi push ở backend/Caresoft. Firebase service-account JSON/private key chỉ đưa vào hệ thống server theo quy trình quản trị, không gửi vào app hoặc repo demo.
4. Khi đăng nhập, đăng ký FCM token bằng `registerPushToken(PushRegistration(...))`, cùng installationId/package/environment đã dùng lúc tạo client. Android dùng environment `default`.
5. Xử lý `FirebaseMessagingService.onNewToken`, thử lại khi chưa có mạng/phiên chưa xác minh và hủy yêu cầu cũ khi đổi tài khoản. Xem `PendingPushRegistration`/`DemoTokenJobService` trong demo.
6. Forward data message cho `CaresoftVoicePush.receive()` sau khi đã cấu hình client. Server cần gửi high-priority data message; host vẫn phải tuân thủ giới hạn thời gian và foreground service của Android.

Không có cấu hình APNs/PushKit cho Android. Chưa cấu hình Firebase thì demo vẫn build và gọi đi được sau khi cấu hình thoại, nhưng chưa thể nghiệm thu nhận push. Có thể nhập Firebase client metadata trong Settings để thử; thay đổi Firebase project sau khi đã khởi tạo cần khởi động lại process.

## Mạng và media

Cho phép HTTPS/WSS và đường UDP/TURN do môi trường media chỉ định. Cấu hình ICE/STUN/TURN với credential ngắn hạn; không dùng địa chỉ của môi trường thử trong production. Không tắt kiểm tra TLS. Cần thử gọi qua Wi-Fi, mobile data và mạng chỉ cho TURN trước phát hành.
