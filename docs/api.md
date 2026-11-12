# API Kotlin / Java — 1.0.0

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

`VoiceOptions(domain, appId, apiBaseUrl, installationId, tokenProvider, iceServers, appVersionCode, pushConfig, operationTimeoutMs, appName, credentialStore, pushBackend)`. Năm tham số đầu bắt buộc. Mặc định timeout 15000ms, ICE rỗng, appName "Caresoft Voice"; cấu hình ICE theo môi trường thực tế. Demo dùng timeout dài hơn để thử. Chỉ dùng override credentialStore/pushBackend khi host có nhu cầu và bảo đảm cùng hợp đồng bảo mật.

`TokenProvider.getToken(): String` là suspend; Java dùng `CallbackTokenProvider` + `TokenProviders.fromCallback`. Provider trả C2C token. `AppPushConfig(applicationId, environment="default")` và `PushRegistration(token, installationId, applicationId, environment="default")` phải cùng scope.

## VoiceCall

| API | Kết quả/ý nghĩa |
|---|---|
| start() | Gọi đi sau createCall |
| answer() / reject() | Nhận/từ chối incoming |
| hangup(): EndResult | Kết thúc local + serverAcknowledged |
| setMuted(Boolean) / setHeld(Boolean) | Chỉ dùng khi ACTIVE, state theo ACK server |
| sendDTMF(String) | 0–9/A–D/*/# |
| getAudioRoutes(): List<AudioRoute> | id/name/selected từ Telecom |
| setAudioRoute(id) | Yêu cầu route; chờ AudioRoutesChanged cho lựa chọn thực |
| getStats(): CallStats | timestamp, packetsLost, jitterSeconds, roundTripTimeSeconds, bytesSent/Received |
| getSnapshot(): CallSnapshot | State và mediaState độc lập; giữ snapshot terminal |
| snapshots / observe(handler) | StateFlow hoặc CallEvent; hủy subscription khi view hủy |

`CallOptions(to, callerId=null, ticketId=null)`. `CallSnapshot` có id, direction, remote, state, mediaState, muted, held, serverCallId, endReason, createdAt, connectedAt, endedAt. Timestamp Android là Unix milliseconds; không dùng ordinal enum làm protocol value.

CallState: NEW, DIALING, RINGING, CONNECTING, ACTIVE, ENDED. MediaState: NOT_STARTED, CONNECTING, CONNECTED, DISCONNECTED, FAILED. ENDED terminal; không tái sử dụng call đã kết thúc.

EndReason: LOCAL_ENDED, REMOTE_ENDED, REJECTED, MISSED, ANSWERED_ELSEWHERE, CANCELED_ELSEWHERE, AUTH_FAILED, CONNECTION_FAILED, BUSY, DISPOSED. Dùng để hiển thị và chẩn đoán, không giả định mọi lỗi SIP đều được map thành BUSY riêng.

## Events và UI

ClientEvent: SessionChanged, ConnectionChanged, AvailabilityChanged, CallDeviceChanged, IncomingCall, CallRestored, Error.

CallEvent: Changed(snapshot), AudioRoutesChanged(routes), Error(error). `EventMeta` gồm clientId/callId/sequence/timestamp; bỏ event của client cũ sau khi đổi tài khoản.

`VoiceCallView.bind(call)` / `unbind()`: UI không sở hữu vòng đời engine. Host giữ call qua Activity recreation. Không giữ Activity trong client/service.

Java callback có `onSuccess(T)` và `onError(VoiceError)`. Các mutation trả `kotlin.Unit`; reject/hangup trả `EndResult`, logout trả `LogoutResult`. Mẫu compile là nguồn tham khảo chữ ký khi tích hợp Java.

`VoiceError(code, operation, retryable, callId)` có message an toàn theo mã lỗi. Không diễn giải `serverAcknowledged=false` là cuộc gọi vẫn còn local; kiểm tra hai trường EndResult riêng.
