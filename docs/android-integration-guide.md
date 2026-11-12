# Tích hợp Caresoft Voice SDK Android

**1.0.0 · Android API 26+ · Kotlin/Java**

[TOC]

## 1. Chuẩn bị

Cần JDK 17, Android SDK 35 và Gradle wrapper có trong repo. Chuẩn bị domain, API URL HTTPS, Voice App ID, backend cấp C2C token, package Android và Firebase project theo [cấu hình backend](backend-contract.md). ICE/STUN/TURN phải dùng cấu hình được cấp cho môi trường của bạn.

Repo không chứa tài khoản hoặc môi trường mẫu đã đăng ký. Bản APK cài sẵn mở ở màn hình đăng nhập trống, không tự tạo cuộc gọi. App ID và API URL phải nhập trước khi đăng nhập.

## 2. Cài SDK vào ứng dụng

Chạy ở root repo:

```sh
python3 scripts/setup-sdk.py
./gradlew :demo:assembleDebug :consumer-java:assembleDebug
```

Mở root repo bằng Android Studio và chạy module `demo`. Nếu dùng CLI, đặt `ANDROID_HOME` hoặc tạo `local.properties` với `sdk.dir` của máy. Dependency cần Google Maven/Maven Central ở lần resolve đầu.

Để tích hợp vào app riêng, chép thư mục `packages/maven` đã giải nén tới `vendor/caresoft/maven` trong app. Trong `settings.gradle` của app:

```groovy
dependencyResolutionManagement {
    repositories {
        maven { url uri('vendor/caresoft/maven') }
        google()
        mavenCentral()
    }
}
```

Trong module app:

```groovy
dependencies {
    implementation 'vn.caresoft:voice-android:1.0.0'
    implementation 'vn.caresoft:voice-android-ui:1.0.0' // tùy chọn
}
```

Giữ toàn bộ AAR/POM/Gradle metadata. AAR core không nhúng dependency bắc cầu. Không dùng `flatDir` rồi bỏ dependency WebRTC/coroutines/OkHttp/Socket.IO; không thêm đồng thời nhiều thư viện cùng chứa `org.webrtc`.

## 3. Quyền và thành phần Android

SDK merge ConnectionService, receiver và foreground service vào manifest host. Kiểm tra merged manifest và bảo đảm host có quyền phù hợp:

| Quyền | Cách dùng |
|---|---|
| RECORD_AUDIO | Xin runtime ở foreground trước nghe/gọi |
| POST_NOTIFICATIONS | Xin runtime trên Android 13+ |
| BLUETOOTH_CONNECT | Xin runtime trên Android 12+ nếu dùng Bluetooth |
| MANAGE_OWN_CALLS | Self-managed Telecom; không yêu cầu app làm dialer mặc định |
| FOREGROUND_SERVICE/PHONE_CALL/MICROPHONE | Dịch vụ nghe/gọi theo Android target SDK |
| USE_FULL_SCREEN_INTENT | Kiểm tra khả năng được cấp trên Android 14+, cung cấp notification fallback |

Không cần READ_PHONE_NUMBERS chỉ để dùng cuộc gọi VoIP. Không bỏ kiểm tra microphone hoặc dùng FCM để mở micro trái giới hạn OS. Xem manifest demo và [khả năng tương thích](compatibility.md).

## 4. Khởi tạo client và phiên

Giữ một `CaresoftVoiceClient` ở Application/service lâu hơn vòng đời Activity. Tạo client/createCall trên main thread, gọi suspend API từ coroutine có scope phù hợp. `installationId` phải ổn định trên một cài đặt và không dùng mã thiết bị nhạy cảm.

```kotlin
val options = VoiceOptions(
    domain = domain,
    appId = appId,
    apiBaseUrl = apiUrl,
    installationId = installationId,
    tokenProvider = TokenProvider { authenticatedBackend.fetchC2CToken() },
    iceServers = environmentIceServers,
    appVersionCode = appVersionCode,
    pushConfig = AppPushConfig(context.packageName, "default")
)
val client = CaresoftVoiceClient(context.applicationContext, options)
CaresoftVoicePush.configure(client)
```

Các biến trên thuộc cấu hình/backend của app, không phải hằng số credential trong SDK. `fetchC2CToken()` minh họa API của backend khách hàng; endpoint và cơ chế xác thực do app triển khai. Xem [ví dụ Kotlin đã compile](../integration/examples/kotlin/SdkIntegrationExample.kt).

- `connect()` đăng nhập bằng C2C từ TokenProvider.
- `checkSession()` kiểm tra/khôi phục phiên đã lưu; không đăng nhập lại ngầm khi mạng lỗi.
- `AUTHENTICATED`: có phiên được xác minh; `UNAUTHENTICATED`: cần đăng nhập; `UNKNOWN`: chưa xác minh được, giữ credential và thử lại khi có mạng.
- Không chuyển UI thành đăng nhập thành công trước khi API hoàn tất. Không coi lỗi mạng là sai mật khẩu.

Demo xóa token nhập tay sau connect; khi phiên hết hạn cần nhập lại. Provider production phải tự lấy token mới từ backend đã xác thực.

## 5. Nhân viên và đầu số gọi ra

Đọc `getAvailability()` và `getCallDevice()`. Chọn Mobile bằng `selectCallDevice(CallDevice.MOBILE)`, bật sẵn sàng bằng `setAvailability(Availability.AVAILABLE)` theo thao tác người dùng. Đăng nhập không tự đổi hai lựa chọn này. Không đổi thiết bị trong lúc có cuộc gọi.

`getCallingLines()` trả các `CallingLine(id, label, isDefault)`. Hiển thị label, truyền **id** được chọn vào `CallOptions.callerId`; không dùng số hiển thị làm id. Khi tải thất bại, báo lỗi và cho thử lại, không giả thành danh sách rỗng thành công.

## 6. Gọi đi

Sau khi đã có phiên, quyền microphone và cấu hình thoại:

```kotlin
val call = client.createCall(CallOptions(to = phoneNumber, callerId = selectedLineId))
call.start()
```

`createCall()` giữ chỗ cuộc gọi đồng bộ; chỉ một call hoạt động. Không tạo/start lại khi Activity xoay màn hình. Giữ call ở tầng Application/service, dùng `getActiveCall()` để gắn lại UI.

Trạng thái công khai: NEW → DIALING/RINGING/CONNECTING → ACTIVE → ENDED. ACTIVE xác nhận signaling; `mediaState` phản ánh đường media riêng. Kết nối socket không chứng minh cuộc gọi hoạt động hoặc có âm thanh.

## 7. Nhận cuộc gọi bằng FCM

Host sở hữu `FirebaseMessagingService`. Đăng ký Android client đúng package và cấu hình server cùng project theo [hướng dẫn FCM](backend-contract.md#firebase-và-cuộc-gọi-đến).

Sau đăng nhập, lấy FCM token và đăng ký:

```kotlin
client.registerPushToken(
    PushRegistration(fcmToken, installationId, context.packageName, "default")
)
```

Xử lý token rotation cả khi Activity chưa mở. Demo lưu cờ retry theo tài khoản và dùng JobScheduler kiểm tra phiên, lấy FCM token mới nhất rồi đăng ký lại; không lưu FCM token plaintext. Logout/đổi client phải làm mất hiệu lực công việc cũ.

Ở `Application.onCreate()`, khôi phục cấu hình app và tạo/configure client trước khi forward message:

```kotlin
// Chạy trong cửa sổ xử lý FCM và coroutine theo lifecycle của host.
CaresoftVoicePush.receive(message.data)
```

SDK không tạo FirebaseMessagingService thứ hai. Không log data payload. Quan sát `ClientEvent.IncomingCall`, giữ VoiceCall và hiển thị UI của app. Khi người dùng bấm nhận, kiểm tra quyền microphone rồi gọi `call.answer()`; từ chối dùng `call.reject()`.

Trên Android 14+, app ở nền có thể bị chặn quyền microphone/foreground service; cung cấp notification mở app. High-priority data push không bảo đảm quyền chạy mọi tác vụ nền. Force-stop vẫn cần người dùng mở lại app. Xem demo đầy đủ thay vì chỉ chép vài dòng nhận message.

## 8. UI cuộc gọi

UI có sẵn:

```kotlin
val callView = VoiceCallView(context)
callView.bind(call)
// Khi view bị hủy:
callView.unbind()
```

Tự dựng UI: dùng `call.snapshots` hoặc `call.observe`; map `CallEvent.Changed` sang trạng thái và `CallEvent.Error` sang thông báo. Hủy subscription khi view biến mất; không hangup/dispose chỉ vì đổi màn hình. Giữ snapshot ENDED để hiển thị nguyên nhân kết thúc trước khi trở lại bàn phím.

Demo có **UI của SDK / UI tự dựng** sử dụng cùng VoiceCall. Ví dụ Java ở [consumer-java](../consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java).

## 9. Điều khiển cuộc gọi

- `setMuted(true/false)`, `setHeld(true/false)`: state thay đổi sau xác nhận server; lỗi cần hiển thị, không tự giả thành công.
- `sendDTMF("123#")`: dùng khi ACTIVE; không gửi chuỗi ngoài 0–9/A–D/*/#.
- `getAudioRoutes()`: danh sách route hệ thống hỗ trợ, với id/name/selected. `setAudioRoute(id)` gửi yêu cầu; cập nhật lựa chọn từ `CallEvent.AudioRoutesChanged`.
- Không hardcode AudioRoute id, không xin thêm AudioManager focus hoặc gọi `setCommunicationDevice` để tranh quyền với Telecom của cùng cuộc gọi.
- `getStats()`: số liệu media có thể null khi chưa có mẫu. Byte count tăng không tự chứng minh người đối ứng nghe được âm thanh.
- `hangup()` trả `EndResult(localEnded, serverAcknowledged)`. Kết thúc cục bộ có thể thành công dù server chưa xác nhận.

Audio focus được SDK phối hợp với ConnectionService; mất focus chỉ ngắt local audio, không tự đổi server mute/hold. Chọn unhold vẫn là thao tác rõ ràng của người dùng.

## 10. Java, lỗi và vòng đời

Java dùng `TokenProviders.fromCallback(CallbackTokenProvider)` để cấp token bất đồng bộ và overload `VoiceCallback<T>` để gọi API. Callback không được block main thread. Mẫu Java compile bao gồm đăng nhập, gọi, nhận, mute/hold, route, stats, push và logout.

`VoiceError` có `code`, `operation`, `retryable`, `callId`. Log mã lỗi/phiên bản/timestamp; không log token hoặc phản hồi mạng. Trước dispose, kết thúc cuộc gọi hiện tại. Đổi tenant/agent cần logout/dispose client cũ; gọi `CaresoftVoicePush.clear(oldClient)` để tránh trỏ tới phiên cũ.

`logout()` trả xác nhận riêng app session/voice session và việc xóa credential cục bộ. Không coi local clear là server đã thu hồi phiên. Xem [API](api.md) và [xử lý lỗi](troubleshooting.md).

## 11. Nghiệm thu trước phát hành

Chạy [checklist](testing.md) trên thiết bị/OS của sản phẩm, gồm gọi ra, nhận từ nền/khóa/cold-start, hai chiều, route, token rotation, logout, mạng yếu và giới hạn OEM. Kết quả trên một máy hoặc APK build thành công không thay thế nghiệm thu các ca đó.
