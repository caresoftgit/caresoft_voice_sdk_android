package vn.caresoft.voice.demo

import android.app.Application
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.*
import vn.caresoft.voice.*

class DemoApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val preferences by lazy { getSharedPreferences("demo_settings", MODE_PRIVATE) }
    var client: CaresoftVoiceClient? = null
        private set

    var displayedCall: VoiceCall? = null
    var callingLines = emptyList<CallingLine>()
        private set

    var selectedLineId = "0"
    var linesError = ""
        private set

    var loadingLines = false
        private set

    private var lineSelectionInitialized = false
    var callLineLabel: String? = null
    var sdkUi: Boolean
        get() = preferences.getBoolean("sdkUi", true)
        set(value) {
            preferences.edit().putBoolean("sdkUi", value).apply()
        }

    var notice = ""
    val history = ArrayDeque<String>()
    var pushStatus = "Chưa đăng ký hoàn tất"
        private set

    private fun record(value: String) {
        if (history.size == 30) history.removeFirst()
        history.addLast(value)
    }

    var changed: (() -> Unit)? = null
    private var subscription: VoiceSubscription? = null
    private var runtimeToken = ""
    private var tokenEndpoint = ""
    private var iceJSON = DemoConfiguration.DEFAULT_ICE
    private val secureConfiguration by lazy { DemoSecureConfiguration(this) }
    private var pushPaused = false
    private val pushOwner: String
        get() =
            preferences.getString("push_owner", null)
                ?: UUID.randomUUID().toString().also {
                    check(preferences.edit().putString("push_owner", it).commit())
                }

    private val tokenJobs
        get() = getSystemService(JobScheduler::class.java)

    private fun pendingPushWork(): PendingPushWork? {
        val owner = preferences.getString("push_pending_owner", null) ?: return null
        val revision = preferences.getString("push_pending_revision", null) ?: return null
        return PendingPushWork(owner, revision)
    }

    private val pendingPush by lazy {
        PendingPushRegistration(
            current = {
                if (pushPaused || loginInProgress) null
                else client?.let { PushBinding(it, pushOwner) }
            },
            read = ::pendingPushWork,
            write = { work ->
                val editor = preferences.edit()
                if (work == null)
                    editor.remove("push_pending_owner").remove("push_pending_revision")
                else
                    editor
                        .putString("push_pending_owner", work.owner)
                        .putString("push_pending_revision", work.revision)
                check(editor.commit())
            },
            checkSession = { it.checkSession().status == SessionStatus.AUTHENTICATED },
            latestToken = {
                check(FirebaseApp.getApps(this).isNotEmpty())
                suspendCancellableCoroutine<String> { continuation ->
                    FirebaseMessaging.getInstance()
                        .token
                        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                        .addOnFailureListener {
                            if (continuation.isActive) continuation.resumeWithException(it)
                        }
                }
            },
            register = { current, token ->
                current.registerPushToken(
                    PushRegistration(token, installationId, packageName, "default")
                )
                pushStatus = "Đã đăng ký thông báo thoại"
                record(pushStatus)
                changed?.invoke()
            },
        )
    }

    private fun invalidatePushRegistration() {
        check(
            preferences
                .edit()
                .putString("push_owner", UUID.randomUUID().toString())
                .remove("push_pending_owner")
                .remove("push_pending_revision")
                .commit()
        )
        tokenJobs.cancel(PUSH_TOKEN_JOB)
    }

    private fun schedulePushRegistration() {
        val job =
            JobInfo.Builder(PUSH_TOKEN_JOB, ComponentName(this, DemoTokenJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setBackoffCriteria(30_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build()
        if (tokenJobs.schedule(job) != JobScheduler.RESULT_SUCCESS) {
            notice = "Chưa lên lịch đăng ký push; yêu cầu vẫn được giữ để thử lại"
            changed?.invoke()
        }
    }

    internal suspend fun flushPendingPushRegistration(): Boolean = pendingPush.flush()

    var loginInProgress = false
        private set

    var sessionChecking = false
        private set

    val installationId: String by lazy {
        preferences.getString("installation", null)
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString("installation", it).apply()
            }
    }

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        if (!preferences.getString("domain", "").isNullOrBlank()) {
            runCatching {
                    secureConfiguration.read()?.let { saved ->
                        val config = org.json.JSONObject(saved)
                        iceJSON = config.getString("ice")
                        tokenEndpoint = config.optString("endpoint")
                    }
                    createClient()
                }
                .onFailure { notice = "Không thể khôi phục cấu hình. Hãy đăng nhập lại." }
        }
        if (pendingPushWork() != null && tokenJobs.getPendingJob(PUSH_TOKEN_JOB) == null)
            schedulePushRegistration()
    }

    fun createClient(
        domain: String = preferences.getString("domain", "")!!,
        appId: String = preferences.getString("appId", DEFAULT_APP_ID)!!,
        api: String = preferences.getString("api", DEFAULT_API)!!,
    ): CaresoftVoiceClient {
        check(client == null) { "Dispose the current client first" }
        val result =
            CaresoftVoiceClient(
                this,
                VoiceOptions(
                    domain = domain,
                    appId = appId,
                    apiBaseUrl = api,
                    installationId = installationId,
                    tokenProvider =
                        TokenProvider {
                            if (tokenEndpoint.isNotEmpty())
                                DemoConfiguration.fetchToken(tokenEndpoint)
                            else
                                runtimeToken.takeIf { it.isNotBlank() }
                                    ?: throw VoiceError("LOGIN_REQUIRED", "tokenProvider")
                        },
                    iceServers = DemoConfiguration.parseIce(iceJSON),
                    appVersionCode = BuildConfig.VERSION_CODE.toString(),
                    pushConfig = AppPushConfig(packageName, "default"),
                    appName = "Caresoft Voice Demo",
                    operationTimeoutMs = 45000,
                ),
            )
        client = result
        CaresoftVoicePush.configure(result)
        subscription =
            result.observe { event, _ ->
                record(event.javaClass.simpleName)
                when (event) {
                    is ClientEvent.IncomingCall -> {
                        displayedCall = event.call
                        callLineLabel = null
                    }
                    is ClientEvent.CallRestored -> {
                        displayedCall = event.call
                        callLineLabel = null
                    }
                    is ClientEvent.Error -> notice = event.error.message ?: event.error.code
                    else -> Unit
                }
                changed?.invoke()
            }
        return result
    }

    suspend fun login(
        domain: String,
        appId: String,
        api: String,
        c2cToken: String,
        endpoint: String = "",
        ice: String = DemoConfiguration.DEFAULT_ICE,
    ) {
        DemoInput.validateLogin(
            domain.trim(),
            appId.trim(),
            api.trim(),
            if (endpoint.isEmpty()) c2cToken.trim() else "backend",
        )
        if (endpoint.isNotEmpty()) DemoConfiguration.validateEndpoint(endpoint)
        DemoConfiguration.parseIce(ice)
        check(!loginInProgress) { "Đang đăng nhập, vui lòng chờ" }
        loginInProgress = true
        try {
            invalidatePushRegistration()
            client?.let { old ->
                old.dispose()
                CaresoftVoicePush.clear(old)
            }
            subscription?.cancel()
            client = null
            displayedCall = null
            callingLines = emptyList()
            selectedLineId = "0"
            lineSelectionInitialized = false
            callLineLabel = null
            runtimeToken = c2cToken.trim()
            tokenEndpoint = endpoint
            iceJSON = ice
            createClient(domain.trim(), appId.trim(), api.trim()).connect()
            secureConfiguration.write(
                org.json.JSONObject().put("ice", ice).put("endpoint", endpoint).toString()
            )
            preferences
                .edit()
                .putString("domain", domain.trim())
                .putString("appId", appId.trim())
                .putString("api", api.trim())
                .apply()
        } catch (failure: Exception) {
            client?.let { old ->
                old.dispose()
                CaresoftVoicePush.clear(old)
            }
            subscription?.cancel()
            subscription = null
            client = null
            throw failure
        } finally {
            runtimeToken = ""
            loginInProgress = false
            changed?.invoke()
        }
        refreshPushToken()
        refreshAgentStatus()
        refreshCallingLines()
    }

    suspend fun refreshSession() {
        if (sessionChecking || loginInProgress) return
        val current = client ?: return
        sessionChecking = true
        changed?.invoke()
        try {
            val session = current.checkSession()
            if (client === current) {
                notice =
                    when (session.status) {
                        SessionStatus.AUTHENTICATED -> ""
                        SessionStatus.UNAUTHENTICATED -> "Phiên đã hết hạn. Vui lòng đăng nhập lại."
                        SessionStatus.UNKNOWN ->
                            "Chưa xác định phiên. Kiểm tra mạng và thử lại; thông tin đăng nhập vẫn được giữ."
                    }
                if (session.status == SessionStatus.AUTHENTICATED) {
                    refreshPushToken()
                    refreshAgentStatus()
                    if (!lineSelectionInitialized) refreshCallingLines()
                }
            }
        } catch (failure: Exception) {
            if (client === current)
                notice =
                    (failure as? VoiceError)?.message
                        ?: "Chưa kiểm tra được phiên. Vui lòng thử lại."
        } finally {
            sessionChecking = false
            changed?.invoke()
        }
    }

    private suspend fun refreshAgentStatus() {
        val current = client ?: return
        try {
            // This read updates both availability and receiving device in the SDK snapshot.
            current.getAvailability()
        } catch (canceled: CancellationException) {
            throw canceled
        } catch (_: Exception) {
            if (client === current)
                notice =
                    "Chưa tải được trạng thái thoại. Phiên đăng nhập vẫn được giữ; thử Kiểm tra phiên để tải lại."
        }
        if (client === current) changed?.invoke()
    }

    suspend fun refreshCallingLines() {
        val current = client ?: return
        if (loadingLines || current.getSnapshot().session.status != SessionStatus.AUTHENTICATED)
            return
        loadingLines = true
        linesError = ""
        changed?.invoke()
        try {
            val lines = current.getCallingLines()
            if (client !== current) return
            callingLines = lines
            if (!lineSelectionInitialized) {
                selectedLineId =
                    lines.firstOrNull { it.isDefault }?.id ?: lines.firstOrNull()?.id ?: "0"
                lineSelectionInitialized = true
            }
            if (selectedLineId != "0" && lines.none { it.id == selectedLineId })
                selectedLineId = "0"
        } catch (_: Exception) {
            if (client === current)
                linesError =
                    "Chưa tải được số gọi ra. Bạn có thể tải lại hoặc dùng mặc định tổng đài."
        } finally {
            loadingLines = false
            changed?.invoke()
        }
    }

    suspend fun logout() {
        if (client?.getActiveCall() != null) throw VoiceError("CALL_IN_PROGRESS", "logout")
        pushPaused = true
        try {
            invalidatePushRegistration()
            client?.let { current ->
                val result = current.logout()
                notice =
                    if (result.errors.isEmpty()) "Đã đăng xuất"
                    else "Đã xóa phiên cục bộ; một số yêu cầu máy chủ chưa xác nhận"
                current.dispose()
                CaresoftVoicePush.clear(current)
            }
            subscription?.cancel()
            subscription = null
            client = null
            runtimeToken = ""
            displayedCall = null
            pushStatus = "Chưa đăng ký hoàn tất"
            history.clear()
            callingLines = emptyList()
            selectedLineId = "0"
            lineSelectionInitialized = false
            callLineLabel = null
            secureConfiguration.clear()
            tokenEndpoint = ""
            iceJSON = DemoConfiguration.DEFAULT_ICE
            preferences.edit().remove("domain").apply()
            changed?.invoke()
        } finally {
            pushPaused = false
        }
    }

    fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isNotEmpty()) return
        if (FirebaseApp.initializeApp(this) != null) return
        val firebaseAppId = preferences.getString("firebaseAppId", "")!!
        val key = preferences.getString("firebaseApiKey", "")!!
        val sender = preferences.getString("firebaseSender", "")!!
        val project = preferences.getString("firebaseProject", "")!!
        if (listOf(firebaseAppId, key, sender, project).any { it.isBlank() }) return
        runCatching {
                FirebaseApp.initializeApp(
                    this,
                    FirebaseOptions.Builder()
                        .setApplicationId(firebaseAppId)
                        .setApiKey(key)
                        .setGcmSenderId(sender)
                        .setProjectId(project)
                        .build(),
                )
            }
            .onFailure { notice = "Cấu hình Firebase không hợp lệ" }
    }

    fun refreshPushToken() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            pushStatus = "Firebase chưa được cấu hình"
            changed?.invoke()
            return
        }
        pushStatus = "Đang chờ đăng ký thông báo thoại"
        // Commit the account-scoped retry marker before the short FCM callback returns.
        if (pendingPush.queue(UUID.randomUUID().toString())) schedulePushRegistration()
    }

    companion object {
        private const val PUSH_TOKEN_JOB = 7106
        const val DEFAULT_APP_ID = ""
        const val DEFAULT_API = ""
    }
}
