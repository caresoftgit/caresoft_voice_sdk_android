# API Kotlin / Java — 1.0.2

Public package: `vn.caresoft.voice`; UI tùy chọn: `vn.caresoft.voice.ui.VoiceCallView`. API mạng/điều khiển dùng Kotlin suspend và overload callback Java. Ví dụ đã compile nằm trong [consumer Java](../consumer-java/src/main/java/vn/caresoft/voice/consumer/JavaConsumer.java) và [recipe Kotlin](../integration/examples/kotlin/SdkIntegrationExample.kt).

## Client

| API | Kết quả/ý nghĩa |
|---|---|
| CaresoftVoiceClient(context, options) | Client giữ phiên và cuộc gọi; dùng applicationContext |
| connect() | Đăng nhập bằng C2C TokenProvider |
| checkSession(): SessionInfo | Xác minh phiên đã lưu; AUTHENTICATED/UNAUTHENTICATED/UNKNOWN |
| getSnapshot(): ClientSnapshot | Session, connection, availability, device, readiness, activeCall |
| snapshots / observe(handler) | StateFlow hoặc ClientEvent + EventMeta; observe trả VoiceSubscription |
| getAvailability() / setAvailability(value) | Đọc/đổi ca trực, server xác nhận |
| getCallDevice() / selectCallDevice(value) | Mobile/Browser/Other/Unknown; không đổi trong call |
| getCallingLines(): List<CallingLine> | id/label/isDefault; dùng id làm callerId |
| checkReadiness(): Readiness | auth, microphone, callUI, push, device: READY/BLOCKED/UNKNOWN |
| registerPushToken(PushRegistration) / unregisterPushToken(PushRegistration) | FCM registration cùng scope app/installation |
| receivePush(data): VoiceCall? | Forward FCM data khi đã khởi tạo client |
| createCall(CallOptions): VoiceCall | Đồng bộ trên main, reserve một cuộc gọi |
| getActiveCall(): VoiceCall? | Gắn lại UI vào cuộc gọi hiện tại |
| logout(): LogoutResult | Xác nhận app/voice session và local clear riêng |
| dispose() | Giải phóng client; không thay thế logout phía server |

### Cấu hình thường dùng

| `VoiceOptions` | Cách cung cấp |
|---|---|
| domain, appId, apiBaseUrl | Cấu hình được Caresoft cấp cho ứng dụng |
| installationId | ID ổn định của một lần cài đặt |
| tokenProvider | Provider lấy C2C token từ backend đã xác thực |
| appVersionCode | Version code của app, dạng `String` |
| pushConfig | `AppPushConfig(applicationId, "default")` cho FCM |
| operationTimeoutMs | Timeout thao tác, mặc định 15000 ms |
| appName | Tên hiển thị, mặc định "Caresoft Voice" |

Năm tham số đầu bắt buộc. Các tùy chọn còn lại dùng mặc định trừ khi được hướng dẫn riêng. Không dùng provider chứa token cố định trong app phát hành.

`TokenProvider.getToken(): String` là suspend; Java dùng `CallbackTokenProvider` + `TokenProviders.fromCallback`. Provider trả C2C token. `AppPushConfig(applicationId, environment="default")` và `PushRegistration(token, installationId, applicationId, environment="default")` phải cùng scope.

## VoiceCall

| API | Kết quả/ý nghĩa |
|---|---|
| start() | Gọi đi sau createCall |
| answer() / reject() | Nhận/từ chối incoming |
| hangup(): EndResult | Kết thúc local + serverAcknowledged |
| setMuted(Boolean) / setHeld(Boolean) | Chỉ dùng khi ACTIVE, state theo ACK server |
| sendDTMF(String) | 0–9/A–D/*/# |
| getAudioRoutes(): List<AudioRoute> | id/name/selected do hệ thống cung cấp |
| setAudioRoute(id) | Yêu cầu route; chờ AudioRoutesChanged cho lựa chọn thực |
| getStats(): CallStats | timestamp, packetsLost, jitterSeconds, roundTripTimeSeconds, bytesSent/Received |
| getSnapshot(): CallSnapshot | State và mediaState độc lập; giữ snapshot terminal |
| snapshots / observe(handler) | StateFlow hoặc CallEvent; hủy subscription khi view hủy |

`CallOptions(to, callerId=null, ticketId=null)`. `CallSnapshot` có id, direction, remote, state, mediaState, muted, held, serverCallId, endReason, createdAt, connectedAt, endedAt. Các mốc thời gian trong `CallSnapshot` là Unix milliseconds; không dùng ordinal enum làm protocol value.

CallState: NEW, DIALING, RINGING, CONNECTING, ACTIVE, ENDED. MediaState: NOT_STARTED, CONNECTING, CONNECTED, DISCONNECTED, FAILED. ENDED terminal; không tái sử dụng call đã kết thúc.

EndReason: LOCAL_ENDED, REMOTE_ENDED, REJECTED, MISSED, ANSWERED_ELSEWHERE, CANCELED_ELSEWHERE, AUTH_FAILED, CONNECTION_FAILED, BUSY, DISPOSED. Dùng để hiển thị kết quả; không suy diễn nguyên nhân chi tiết chỉ từ BUSY hoặc CONNECTION_FAILED. Với ANSWERED_ELSEWHERE/CANCELED_ELSEWHERE, đóng UI nhận cuộc gọi và hiển thị kết quả phù hợp; không gọi reject/end thêm từ callback kết thúc.

## Events và UI

ClientEvent: SessionChanged, ConnectionChanged, AvailabilityChanged, CallDeviceChanged, IncomingCall, CallRestored, Error.

CallEvent: Changed(snapshot), AudioRoutesChanged(routes), Error(error). `EventMeta` gồm clientId/callId/sequence/timestamp; bỏ event của client cũ sau khi đổi tài khoản.

`VoiceCallView(context)` hoặc khai báo bằng XML; dùng trên main thread:

| API UI | Hành vi |
|---|---|
| `bind(call)` | Gắn và hiển thị snapshot hiện tại, cập nhật theo call; không tự gọi/nhận |
| `unbind()` | Hủy quan sát UI; không kết thúc/dispose call |
| `lineLabel: String?` | Nhãn đầu số tùy chọn; không thay `callerId`; Java dùng `setLineLabel()` |

UI hiển thị incoming nhận/từ chối, outgoing, trạng thái kết nối, thời lượng, điều khiển ACTIVE, bàn phím DTMF và kết quả ENDED. Host xin quyền runtime trước thao tác nghe/gọi, cung cấp điều hướng Đóng/Quay lại, giữ client/call qua Activity recreation và unbind khi hủy view. Nếu cần giữ màn hình ENDED sau đó, host giữ call/snapshot vì `getActiveCall()` chỉ trả call đang hoạt động. Xem [tích hợp UI](android-integration-guide.md#8-ui-cuộc-gọi). Không giữ Activity trong client/service.

Java callback có `onSuccess(T)` và `onError(VoiceError)`. Các API điều khiển không có kết quả dữ liệu trả `kotlin.Unit`; reject/hangup trả `EndResult`, logout trả `LogoutResult`. Mẫu compile là nguồn tham khảo chữ ký khi tích hợp Java.

`VoiceError(code, operation, retryable, callId)` có message an toàn theo mã lỗi. Không diễn giải `serverAcknowledged=false` là cuộc gọi vẫn còn local; kiểm tra hai trường EndResult riêng.

## Push bridge và lỗi

`CaresoftVoicePush.configure(client)` đặt client nhận data message; gọi tại Application sau khởi tạo. Kotlin dùng `CaresoftVoicePush.receive(data)`; Java dùng `client.receivePush(data, callback)`. `CaresoftVoicePush.clear(client)` gỡ đúng client cũ khi đăng xuất/đổi tài khoản. Host cung cấp `FirebaseMessagingService`, cấu hình Firebase và cơ chế retry đăng ký token.

Mọi API bất đồng bộ có thể lỗi. Trong coroutine bắt `VoiceError`; không nuốt cancellation của coroutine host. Java xử lý `onError`. Xem [xử lý lỗi](troubleshooting.md) để biết các bước kiểm tra và thông tin được phép gửi hỗ trợ.
