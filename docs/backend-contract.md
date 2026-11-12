# Cấu hình backend, token và Firebase

## Thông tin cần chuẩn bị

| Thông tin | Nơi cung cấp | Cách dùng |
|---|---|---|
| Domain | Caresoft | `VoiceOptions.domain` của tổ chức/nhân viên |
| API URL HTTPS | Caresoft | `VoiceOptions.apiBaseUrl` |
| Voice App ID | Caresoft Admin | `VoiceOptions.appId` |
| Nhân viên | Hệ thống khách hàng/Caresoft | Backend xác thực đúng người và quyền gọi |
| C2C token | Backend khách hàng | `TokenProvider.getToken()` |
| Android applicationId | Ứng dụng khách hàng | Package đã đăng ký cho push |
| Firebase project | Khách hàng | Android client và cấu hình gửi FCM phía server |
| installationId | Ứng dụng khách hàng | ID ổn định cho một lần cài đặt |

Liên hệ quản trị Caresoft để đăng ký ứng dụng thoại cho domain, package Android và cấu hình push. Không sử dụng cấu hình của ứng dụng khác. Các ví dụ trong tài liệu dùng biến cấu hình, không cung cấp môi trường đăng nhập sẵn.

## C2C token từ backend

1. App đăng nhập backend của bạn bằng cơ chế xác thực hiện có.
2. Backend kiểm tra nhân viên và quyền sử dụng thoại, cấp C2C token theo hợp đồng Voice API do Caresoft cung cấp.
3. App nhận token qua HTTPS; `TokenProvider` trả token đó cho SDK.
4. Khi cần token mới, provider tiếp tục gọi backend đã xác thực. Báo lỗi khi phiên app hết hạn thay vì trả token cũ hoặc chuỗi rỗng.

Endpoint, định dạng yêu cầu và header xác thực thuộc backend của bạn; SDK không quy định một endpoint cấp token chung. Không đặt Voice API signing secret trong source, BuildConfig, resource hoặc cấu hình mà client đọc được. Provider trả **C2C token**, không trả khóa ký, FCM token hoặc token phiên SDK.

Demo có chế độ thử endpoint HTTPS trả:

```json
{ "token": "C2C_TOKEN_FROM_YOUR_BACKEND" }
```

Endpoint demo không tự gắn header xác thực của backend khách hàng. Production triển khai `TokenProvider` bằng HTTP client đã xác thực của app. Demo từ chối redirect, URL chứa user/password và phản hồi quá 64 KiB. Chế độ Agent ID + Voice API token chỉ dùng thử có kiểm soát.

## Firebase và cuộc gọi đến

1. Tạo/chọn Firebase project của bạn; đăng ký Android applicationId chính xác.
2. Với demo, đặt `google-services.json` Android client **cục bộ** tại `demo/google-services.json` rồi build lại. Với app riêng, cấu hình Firebase theo cách của app và khởi tạo Firebase trước khi lấy FCM token.
3. Đăng ký thông tin project, package và cấu hình gửi FCM với backend/Caresoft. Firebase service-account/private key chỉ đưa vào hệ thống server, không vào app hoặc repo demo.
4. Tạo client với `AppPushConfig(applicationId, "default")`. Sau khi xác minh phiên, lấy FCM token và gọi `registerPushToken(PushRegistration(...))` với cùng installationId/package/environment.
5. Xử lý `FirebaseMessagingService.onNewToken()` kể cả khi chưa có Activity. Lưu yêu cầu retry nếu chưa đăng nhập hoặc mất mạng; vô hiệu yêu cầu cũ khi đổi tài khoản. Xem `PendingPushRegistration` và `DemoTokenJobService` trong demo.
6. Trong Application, tạo client và gọi `CaresoftVoicePush.configure(client)` trước khi forward data message tới `CaresoftVoicePush.receive()`.

Backend cần gửi high-priority **data message** theo hợp đồng cuộc gọi Caresoft. Không tự chế payload; không log payload nhận được. FCM có cửa sổ xử lý ngắn và chịu giới hạn nền Android. SDK không tự đăng ký một `FirebaseMessagingService` thay host.

Firebase khởi tạo thành công chưa chứng minh backend gửi đúng cuộc gọi. Kiểm tra đăng ký token phía backend và nhận cuộc gọi trên thiết bị thật. Chưa cấu hình Firebase vẫn có thể thử gọi đi sau khi đăng nhập, nhưng chưa thể nghiệm thu cuộc gọi đến qua push.

## Mạng và thông báo

Cho phép kết nối tới các địa chỉ/cổng được Caresoft cấp cho môi trường của bạn. Không tắt kiểm tra HTTPS/TLS. Nếu tổ chức có VPN, proxy hoặc firewall, phối hợp quản trị mạng và thử cả Wi-Fi/mobile data.

Thông báo cuộc gọi đến dùng chuông/rung theo cài đặt Android. Host xin quyền thông báo và hướng dẫn người dùng bật kênh **Cuộc gọi đến**; không phát thêm chuông chồng lên thông báo. Full-screen khi khóa máy phụ thuộc quyền hệ thống, còn thao tác nhận cần quyền micro đã cấp ở foreground. Xem [yêu cầu Android](compatibility.md).
