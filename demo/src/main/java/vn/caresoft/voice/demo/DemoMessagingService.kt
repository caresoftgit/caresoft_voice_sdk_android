package vn.caresoft.voice.demo

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.*
import vn.caresoft.voice.CaresoftVoicePush

/** The host owns Firebase; the SDK consumes only the data map. */
class DemoMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        // Persist/schedule only; JobService checks the saved session and fetches Firebase's latest
        // token.
        runBlocking {
            withContext(Dispatchers.Main.immediate) {
                (application as DemoApplication).refreshPushToken()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val app = application as DemoApplication
        if (app.client == null) return
        // FCM gives this callback a short execution window. Server must send high-priority data
        // messages.
        runBlocking {
            try {
                withTimeout(8_000) { CaresoftVoicePush.receive(message.data) }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    app.notice = "Không thể xử lý cuộc gọi đẩy; mở ứng dụng để kiểm tra phiên"
                    app.changed?.invoke()
                }
            }
        }
    }
}
