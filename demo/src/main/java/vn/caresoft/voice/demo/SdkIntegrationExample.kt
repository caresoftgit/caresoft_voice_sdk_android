package vn.caresoft.voice.demo

import android.content.Context
import vn.caresoft.voice.*

/** A compiled integration recipe; the host owns this instance across screen recreation. */
class SdkIntegrationExample(context: Context, options: VoiceOptions) {
    val client = CaresoftVoiceClient(context.applicationContext, options)

    suspend fun signIn() = client.connect()

    suspend fun checkSavedSession() = client.checkSession()

    suspend fun selectMobile() = client.selectCallDevice(CallDevice.MOBILE)

    suspend fun dial(number: String): VoiceCall {
        // Invoke on main: synchronous createCall reserves the one active call.
        val call = client.createCall(CallOptions(number))
        call.start()
        return call
    }

    suspend fun receiveFcm(data: Map<String, String>) = client.receivePush(data)

    suspend fun signOut() = client.logout()
}
