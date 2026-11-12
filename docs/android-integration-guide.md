# Tích hợp Caresoft Voice SDK Android

**1.0.2 · Android API 26+ · Kotlin/Java**

[TOC]

## 1. Chuẩn bị

Cần JDK 17, Android SDK 35 và Gradle wrapper có trong repo. Chuẩn bị domain, API URL HTTPS, Voice App ID, backend cấp C2C token, package Android và Firebase project theo [cấu hình backend](backend-contract.md). Chỉ dùng cấu hình do quản trị cấp cho ứng dụng của bạn.

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
    implementation 'vn.caresoft:voice-android:1.0.2'
    implementation 'vn.caresoft:voice-android-ui:1.0.2' // tùy chọn
}
```

Giữ toàn bộ Maven repository cùng AAR/POM/Gradle metadata để Gradle lấy đủ dependency. Không dùng `flatDir` chỉ chép riêng AAR. Đặt `minSdk` của app từ 26 trở lên; dùng demo làm cấu hình build tham khảo.

### Nâng từ 1.0.1 lên 1.0.2

Tải bộ tích hợp 1.0.2, kiểm tra checksum rồi chạy lại `python3 scripts/setup-sdk.py`. Cập nhật **cả hai** dependency core/UI về `1.0.2`; khi chép sang app riêng, thay bộ Maven repository cũ bằng bộ mới cùng metadata. Không cần đổi public API hoặc thêm cấu hình chọn chế độ hoạt động.

Giữ applicationId, cấu hình Firebase, domain/App ID và provider của ứng dụng. Demo/APK bàn giao không chứa cấu hình thử nghiệm riêng. Sau nâng cấp, chạy [checklist tích hợp](testing.md), đặc biệt nghe hai chiều, chuyển thiết bị âm thanh, DTMF và nhận cuộc gọi từ nền.

## 3. Quyền và thành phần Android

SDK merge ConnectionService, receiver và foreground service vào manifest host. Kiểm tra merged manifest và bảo đảm host có quyền phù hợp:

| Quyền | Cách dùng |
|---|---|
| RECORD_AUDIO | Xin runtime ở foreground trước nghe/gọi |
| POST_NOTIFICATIONS | Xin runtime trên Android 13+ |
| BLUETOOTH_CONNECT | Xin runtime trên Android 12+ nếu dùng Bluetooth |
| MANAGE_OWN_CALLS | Self-managed Telecom; không yêu cầu app làm dialer mặc định |
| FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL, FOREGROUND_SERVICE_MICROPHONE | Dịch vụ nghe/gọi theo Android target SDK |
| USE_FULL_SCREEN_INTENT | Kiểm tra khả năng được cấp trên Android 14+, cung cấp notification fallback |

Không cần READ_PHONE_NUMBERS chỉ để dùng cuộc gọi VoIP. Không bỏ kiểm tra microphone hoặc dùng FCM để mở micro trái giới hạn OS. Xem manifest demo và [khả năng tương thích](compatibility.md).

## 4. Khởi tạo client và phiên

Các lớp SDK dùng package `vn.caresoft.voice`; UI dùng `vn.caresoft.voice.ui.VoiceCallView`.

Giữ một `CaresoftVoiceClient` ở Application/service lâu hơn vòng đời Activity. Tạo client/createCall trên main thread, gọi suspend API từ coroutine có scope phù hợp. `installationId` phải ổn định trên một cài đặt và không dùng mã thiết bị nhạy cảm.

```kotlin
val options = VoiceOptions(
    domain = domain,
    appId = appId,
    apiBaseUrl = apiUrl,
    installationId = installationId,
    tokenProvider = TokenProvider { authenticatedBackend.fetchC2CToken() },
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

Sau khi đã có phiên và quyền microphone, chạy từ coroutine trên main thread:

```kotlin
val call = client.createCall(CallOptions(to = phoneNumber, callerId = selectedLineId))
call.start()
```

`createCall()` giữ chỗ cuộc gọi đồng bộ; chỉ một call hoạt động. Không tạo/start lại khi Activity xoay màn hình. Giữ call ở tầng Application/service, dùng `getActiveCall()` để gắn lại UI.

Trạng thái công khai: NEW → DIALING/RINGING/CONNECTING → ACTIVE → ENDED. Theo dõi cả `state` và `mediaState`; phải thử tiếng hai chiều để xác nhận cuộc gọi có âm thanh. Không coi kết nối client là cuộc gọi đã được nhận.

## 7. Nhận cuộc gọi bằng FCM

Host sở hữu `FirebaseMessagingService`. Đăng ký Android client đúng package và cấu hình server cùng project theo [hướng dẫn FCM](backend-contract.md#firebase-và-cuộc-gọi-đến). Nếu app đã có messaging service, forward message từ service đó; không khai báo thêm service thứ hai cho cùng sự kiện. Với app chưa có service, khai báo service của bạn trong `<application>`:

```xml
<service
    android:name=".VoiceMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

`VoiceMessagingService` là lớp do host triển khai, kế thừa `FirebaseMessagingService`. Xem [service demo](../demo/src/main/java/vn/caresoft/voice/demo/DemoMessagingService.kt) và [đăng ký token trong demo](../demo/src/main/java/vn/caresoft/voice/demo/DemoTokenJobService.kt).

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

SDK không tạo FirebaseMessagingService thứ hai. Không log data payload. Quan sát `ClientEvent.IncomingCall` và `ClientEvent.CallRestored`, giữ `VoiceCall` rồi hiển thị UI. Khi Activity mở lại từ thông báo, gọi `getActiveCall()` để lấy cuộc gọi hiện tại. UI riêng gọi `call.answer()` để nhận, `call.reject()` để từ chối; quyền micro cần cấp ở foreground trước khi nhận.

Trên Android 14+, app ở nền có thể bị chặn quyền microphone/foreground service; cung cấp notification mở app. High-priority data push không bảo đảm quyền chạy mọi tác vụ nền. Force-stop vẫn cần người dùng mở lại app. Xem demo đầy đủ thay vì chỉ chép vài dòng nhận message.

## 8. UI cuộc gọi

Thêm dependency `voice-android-ui:1.0.2` nếu dùng màn hình sẵn có. `VoiceCallView` là Android View; đặt trong Activity/Fragment hoặc `AndroidView` của Compose. Host chịu trách nhiệm điều hướng, quyền runtime và giữ `VoiceCall` lâu hơn vòng đời màn hình.

```kotlin
// Trong Activity/Fragment, sau khi host đã xử lý quyền micro.
val callView = VoiceCallView(requireContext())
container.addView(callView, ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
callView.lineLabel = selectedLine?.label // tùy chọn; chỉ là nhãn hiển thị
callView.bind(call)

// Trong Fragment.onDestroyView() hoặc lúc bỏ view khỏi màn hình:
callView.unbind()
```

`container`, `selectedLine` và `call` là biến của app. Trong Activity dùng `this` thay `requireContext()`. `lineLabel` không thay đổi đầu số thực: truyền `selectedLine.id` vào `CallOptions.callerId` **khi tạo cuộc gọi**. Đặt lại nhãn khi bind một call khác hoặc đặt `null` để ẩn.

| Trạng thái | UI SDK hiển thị / xử lý |
|---|---|
| Incoming đang đổ chuông | Người gọi, Trả lời, Từ chối |
| Outgoing mới tạo | Nút Gọi; host có thể gọi `start()` trước khi hiển thị |
| Đang kết nối | Trạng thái và nút kết thúc |
| ACTIVE | Thời lượng, mute/unmute, hold/unhold, thiết bị âm thanh và bàn phím DTMF |
| ENDED | Thông tin cuộc gọi, thời lượng và lý do kết thúc; không còn nút điều khiển |

`bind()` chỉ gắn UI; không tự gọi/nhận cuộc gọi. `unbind()` hủy quan sát UI; không hangup hoặc dispose. UI không xin quyền micro thay app. Xin quyền trước khi người dùng bấm Gọi/Trả lời; không tự nhận lại sau lỗi khi chưa kiểm tra trạng thái call.

Giữ client và call ở Application/service của host. Khi xoay màn hình hoặc trở lại từ thông báo, lấy `client.getActiveCall()` và bind lại; không tạo/start một call mới. `getActiveCall()` không giữ cuộc gọi đã kết thúc: nếu muốn tiếp tục hiện chi tiết ENDED, host giữ tham chiếu call hoặc snapshot đến khi người dùng đóng. Thêm nút Đóng/Quay lại ở màn hình host, sau đó unbind và bỏ tham chiếu khi phù hợp. Cuộc gọi tiếp theo dùng object mới.

Java dùng cùng UI:

```java
VoiceCallView callView = new VoiceCallView(this);
callView.setLineLabel(selectedLine.getLabel());
callView.bind(call);
// Trong onDestroy() hoặc trước khi bỏ view:
callView.unbind();
```

Tự dựng UI: dùng `call.snapshots` hoặc `call.observe`, đọc `call.getSnapshot()` ngay khi gắn màn hình và hủy `VoiceSubscription` khi view bị hủy. Hiển thị lỗi từ `CallEvent.Error`, cập nhật route từ `CallEvent.AudioRoutesChanged`; không hangup/dispose chỉ vì đổi màn hình. Không phát thêm chuông trong UI: thông báo cuộc gọi đến đã dùng chuông/rung hệ thống theo cài đặt người dùng.

Demo có **UI của SDK / UI tự dựng** dùng chung một `VoiceCall`. Xem [demo](../demo/README.md) và [ví dụ Java](../consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java).

## 9. Điều khiển cuộc gọi

- `setMuted(true/false)`, `setHeld(true/false)`: state thay đổi sau xác nhận server; lỗi cần hiển thị, không tự giả thành công.
- `sendDTMF("123#")`: dùng khi ACTIVE; không gửi chuỗi ngoài 0–9/A–D/*/#. Chờ thao tác hoàn tất trước chuỗi tiếp theo; nếu yêu cầu thất bại/hủy, hiển thị lỗi và cho phép thử lại khi cuộc gọi còn ACTIVE.
- `getAudioRoutes()`: danh sách route hệ thống hỗ trợ, với id/name/selected. `setAudioRoute(id)` gửi yêu cầu; cập nhật lựa chọn từ `CallEvent.AudioRoutesChanged`.
- Không hardcode AudioRoute id, không xin thêm AudioManager focus hoặc gọi `setCommunicationDevice` để tranh quyền với SDK của cùng cuộc gọi.
- `getStats()`: số liệu media có thể null khi chưa có mẫu. Byte count tăng không tự chứng minh người đối ứng nghe được âm thanh.
- `hangup()` trả `EndResult(localEnded, serverAcknowledged)`. Kết thúc cục bộ có thể thành công dù server chưa xác nhận.

SDK quản lý âm thanh của cuộc gọi. Khi có cuộc gọi khác hoặc mất quyền âm thanh, kiểm tra trạng thái và tiếng thực tế trước khi tiếp tục; unhold là thao tác riêng của người dùng.

## 10. Java, lỗi và vòng đời

Java dùng `TokenProviders.fromCallback(CallbackTokenProvider)` và overload `VoiceCallback<T>`. Backend cấp token bất đồng bộ; không block main thread. Ví dụ khởi tạo trên main thread:

```java
// backendTokenProvider: CallbackTokenProvider do app triển khai,
// gọi backend đã xác thực rồi hoàn tất onSuccess(token) / onError(error).
VoiceOptions options = new VoiceOptions(
    domain, appId, apiUrl, installationId,
    TokenProviders.fromCallback(backendTokenProvider),
    java.util.Collections.emptyList(), appVersionCode,
    new AppPushConfig(getPackageName(), "default")
);
CaresoftVoiceClient client = new CaresoftVoiceClient(getApplicationContext(), options);
CaresoftVoicePush.configure(client);
client.connect(new VoiceCallback<kotlin.Unit>() {
    @Override public void onSuccess(kotlin.Unit value) {
        // Cập nhật màn hình đã đăng nhập; tải trạng thái nhân viên/đầu số.
    }
    @Override public void onError(VoiceError error) {
        // Hiển thị lỗi; chỉ log error.getCode() và error.getOperation().
    }
});
```

Các biến cấu hình và provider trên do app cung cấp. Với gọi đi, tạo `VoiceCall call = client.createCall(new CallOptions(number));` trên main thread, giữ call rồi gọi `call.start(callback)`. Không tạo call lại từ callback UI khi xoay màn hình. Java nhận FCM qua `client.receivePush(data, callback)` sau khi đã tạo client. Xem [mẫu Java đầy đủ](../consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java) cho điều khiển, route, stats, push và logout.

`VoiceError` có `code`, `operation`, `retryable`, `callId`. Hiển thị lỗi và chỉ retry khi phù hợp với trạng thái hiện tại. Log mã lỗi, phiên bản và timestamp; không log token hoặc phản hồi mạng.

Khi đổi domain/nhân viên, kết thúc cuộc gọi hiện tại rồi gọi `logout()`, `CaresoftVoicePush.clear(oldClient)` và `dispose()` client cũ; hủy subscription/job đăng ký push của tài khoản cũ trước khi tạo client mới. Không dispose khi chỉ đóng Activity.

`logout()` trả `LogoutResult` với xác nhận riêng app session/voice session và việc xóa credential cục bộ. Không coi local clear là server đã thu hồi phiên. Xem [API](api.md) và [xử lý lỗi](troubleshooting.md).

## 11. Nghiệm thu trước phát hành

Chạy [checklist](testing.md) trên thiết bị/OS của sản phẩm, gồm gọi ra, nhận từ nền/khóa/cold-start, hai chiều, route, token rotation, logout, mạng yếu và giới hạn OEM. Kết quả trên một máy hoặc APK build thành công không thay thế nghiệm thu các ca đó.
