package vn.caresoft.voice.demo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlinx.coroutines.launch
import vn.caresoft.voice.*
import vn.caresoft.voice.ui.VoiceCallView

class MainActivity : Activity() {
    private val app
        get() = application as DemoApplication

    private var page = "welcome"
    private lateinit var content: LinearLayout
    private var notice: TextView? = null
    private var callStatus: TextView? = null
    private var duration: TextView? = null
    private var callSubscription: VoiceSubscription? = null
    private var callView: VoiceCallView? = null
    private var number = ""
    private var observedCallId: String? = null
    private val timer = Handler(Looper.getMainLooper())
    private val tick =
        object : Runnable {
            override fun run() {
                app.displayedCall?.getSnapshot()?.let {
                    duration?.text =
                        DemoInput.duration(it.connectedAt, it.endedAt, System.currentTimeMillis())
                }
                timer.postDelayed(this, 1000)
            }
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        page = state?.getString("page") ?: if (app.client != null) "session" else "welcome"
        number = state?.getString("number") ?: ""
    }

    override fun onStart() {
        super.onStart()
        app.changed = {
            val currentId = app.displayedCall?.getSnapshot()?.id
            if (currentId != null && currentId != observedCallId) {
                observedCallId = currentId
                page = "call"
                render()
            } else {
                notice?.text = app.notice
                if (
                    page in listOf("session", "login") &&
                        !app.loginInProgress &&
                        !app.sessionChecking &&
                        app.client?.getSnapshot()?.session?.status == SessionStatus.AUTHENTICATED
                )
                    page = "dialer"
                if (
                    page in listOf("dialer", "settings") &&
                        app.client?.getSnapshot()?.session?.status != SessionStatus.AUTHENTICATED
                )
                    page = "session"
                if (
                    page in listOf("settings", "session") ||
                        (page == "dialer" && !app.sessionChecking)
                )
                    render()
            }
        }
        app.client?.getActiveCall()?.let { app.displayedCall = it }
        if (app.displayedCall != null) {
            observedCallId = app.displayedCall!!.getSnapshot().id
            page = "call"
        }
        if (
            page == "login" &&
                !app.loginInProgress &&
                app.client?.getSnapshot()?.session?.status == SessionStatus.AUTHENTICATED
        )
            page = "dialer"
        render()
        timer.post(tick)
        if (
            app.client != null &&
                app.displayedCall?.getSnapshot()?.state !in
                    listOf(
                        CallState.DIALING,
                        CallState.RINGING,
                        CallState.CONNECTING,
                        CallState.ACTIVE,
                    )
        ) {
            app.scope.launch { app.refreshSession() }
        }
    }

    override fun onStop() {
        app.changed = null
        detachCallUi()
        timer.removeCallbacks(tick)
        super.onStop()
    }

    override fun onSaveInstanceState(state: Bundle) {
        state.putString("page", page)
        state.putString("number", number)
        super.onSaveInstanceState(state)
    }

    override fun onBackPressed() {
        page = if (app.client != null) "dialer" else "welcome"
        render()
    }

    private fun detachCallUi() {
        callSubscription?.cancel()
        callSubscription = null
        callView?.unbind()
        callView = null
        callStatus = null
        duration = null
    }

    private fun render() {
        if (isDestroyed) return
        detachCallUi()
        if (
            page in listOf("dialer", "settings") &&
                app.client?.getSnapshot()?.session?.status != SessionStatus.AUTHENTICATED
        )
            page = if (app.client == null) "welcome" else "session"
        if (page == "login") window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val incoming =
            app.displayedCall?.getSnapshot()?.let {
                it.direction == CallDirection.INCOMING &&
                    it.state in listOf(CallState.NEW, CallState.RINGING)
            } == true
        if (incoming)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        else
            window.clearFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24), dp(30), dp(24), dp(28))
                setBackgroundColor(Color.rgb(243, 246, 248))
            }
        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
                addView(content)
                fitsSystemWindows = true
            }
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            view.setPadding(
                insets.systemWindowInsetLeft,
                insets.systemWindowInsetTop,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
            insets
        }
        setContentView(scroll)
        if (page == "welcome") {
            text("C", 42, Color.WHITE).apply {
                gravity = Gravity.CENTER
                background = rounded(Color.rgb(8, 126, 117), 24)
                layoutParams = LinearLayout.LayoutParams(dp(76), dp(76))
            }
            spacer(24)
            text("CARESOFT VOICE DEMO", 11, Color.rgb(8, 126, 117))
        }
        spacer(20)
        when (page) {
            "session" -> sessionScreen()
            "login" -> login()
            "dialer" -> dialer()
            "settings" -> settings()
            "call" -> callScreen()
            else -> welcome()
        }
        spacer(16)
        notice = text(app.notice, 14, Color.rgb(173, 42, 56))
    }

    private fun welcome() {
        spacer(44)
        text("Sẵn sàng cho\ncuộc gọi tiếp theo.", 36)
        spacer(20)
        text("Đăng nhập bằng tài khoản Caresoft để thử gọi ra và nhận cuộc gọi từ tổng đài.", 17)
        spacer(24)
        text("●  Hai giao diện · Cùng một Voice SDK", 15, Color.rgb(8, 126, 117))
        spacer(32)
        button("Đăng nhập Caresoft", primary = true) {
            page = "login"
            render()
        }
        text("Nhập cấu hình ứng dụng được Caresoft cấp để bắt đầu.", 12)
    }

    private fun sessionScreen() {
        text(if (app.sessionChecking) "Đang kiểm tra phiên…" else "Kiểm tra phiên", 30)
        text("Chỉ hiển thị bàn phím sau khi phiên được máy chủ xác nhận.", 16)
        button("Thử lại", primary = true) { perform { app.refreshSession() } }.isEnabled =
            !app.sessionChecking
        button("Đăng nhập lại") {
                page = "login"
                render()
            }
            .isEnabled = !app.loginInProgress
    }

    private fun login() {
        text("Đăng nhập Caresoft", 30)
        text(
            "Token nhập ở đây chỉ giữ trong bộ nhớ; SDK bảo vệ phiên bằng Android Keystore. Ứng dụng production nên lấy C2C token từ backend.",
            14,
        )
        val method =
            Spinner(this).apply {
                contentDescription = "Phương thức đăng nhập"
                adapter =
                    ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_spinner_dropdown_item,
                        listOf("Agent ID + Voice token", "C2C token", "Backend"),
                    )
            }
        content.addView(method)
        val domain = field("Domain / tenant", app.preferences.getString("domain", "")!!)
        val agentId = field("Agent ID", "")
        val token = field("Voice API token / C2C token", "", secret = true)
        text("Lấy Voice API token tại Admin → API → Token voice api.", 12)
        val endpoint = field("Token endpoint (Backend)", "https://", secret = true)
        val api = field("API URL", app.preferences.getString("api", DemoApplication.DEFAULT_API)!!)
        val appId =
            field(
                "App ID (bắt buộc)",
                app.preferences.getString("appId", DemoApplication.DEFAULT_APP_ID)!!,
            )
        val advancedParent = content
        val advanced =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                visibility = View.GONE
            }
        button("Cấu hình nâng cao") {
            advanced.visibility =
                if (advanced.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        content.addView(advanced)
        content = advanced
        val ice =
            field("ICE servers JSON", DemoConfiguration.DEFAULT_ICE, secret = true).apply {
                isSingleLine = false
                maxLines = 6
            }
        content = advancedParent
        method.setSelection(1)
        method.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    token.text.clear()
                    endpoint.isEnabled = position == 2
                    token.isEnabled = position != 2
                    agentId.isEnabled = position == 0
                }
            }
        lateinit var loginButton: Button
        loginButton =
            button("Đăng nhập", primary = true) {
                if (app.loginInProgress) return@button
                loginButton.isEnabled = false
                loginButton.text = "Đang đăng nhập…"
                val value = token.text.toString()
                token.text.clear()
                val selectedMethod = method.selectedItemPosition
                val backend = if (selectedMethod == 2) endpoint.text.toString().trim() else ""
                val iceConfig = ice.text.toString()
                val agent = agentId.text.toString().trim()
                perform {
                    try {
                        val loginToken =
                            if (selectedMethod == 0) C2CTokenSigner.create(agent, value) else value
                        app.login(
                            domain.text.toString(),
                            appId.text.toString(),
                            api.text.toString(),
                            loginToken,
                            backend,
                            iceConfig,
                        )
                        page = "dialer"
                        render()
                    } finally {
                        loginButton.isEnabled = true
                        loginButton.text = "Đăng nhập"
                    }
                }
            }
        button("Quay lại") {
            page = "welcome"
            render()
        }
    }

    private fun dialer() {
        text("Xin chào,", 14, Color.rgb(100, 119, 135))
        val snapshot = app.client?.getSnapshot()
        text(
            snapshot?.session?.agent?.displayName
                ?: snapshot?.session?.agent?.id
                ?: "Phiên Caresoft",
            22,
        )
        button("⚙  Mở cấu hình") {
            page = "settings"
            render()
        }
        text(
                "●  ${availabilityLabel(snapshot?.availability)} · ${deviceLabel(snapshot?.callDevice)}",
                13,
                Color.rgb(8, 126, 117),
            )
            .apply {
                setPadding(dp(12), dp(12), dp(12), dp(12))
                background = rounded(Color.rgb(229, 243, 239), 12)
            }
        if (app.displayedCall != null)
            button("Trở lại cuộc gọi") {
                page = "call"
                render()
            }
        val input =
            field("Số điện thoại", number).apply {
                inputType = InputType.TYPE_CLASS_PHONE
                keyListener = android.text.method.DigitsKeyListener.getInstance("+0123456789*#")
                filters = arrayOf(android.text.InputFilter.LengthFilter(40))
                textSize = 30f
                gravity = Gravity.CENTER
                showSoftInputOnFocus = false
                addTextChangedListener(
                    object : android.text.TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int,
                        ) = Unit

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int,
                        ) {
                            number = s.toString()
                        }

                        override fun afterTextChanged(s: android.text.Editable?) = Unit
                    }
                )
            }
        card("Số gọi ra") {
            val entries =
                listOf("Mặc định tổng đài") +
                    app.callingLines.map { it.label + if (it.isDefault) " · Mặc định" else "" }
            val ids = listOf("0") + app.callingLines.map { it.id }
            content.addView(
                Spinner(this).apply {
                    contentDescription = "Số gọi ra"
                    adapter =
                        ArrayAdapter(
                            this@MainActivity,
                            android.R.layout.simple_spinner_dropdown_item,
                            entries,
                        )
                    setSelection(ids.indexOf(app.selectedLineId).coerceAtLeast(0))
                    onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {
                            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long,
                            ) {
                                app.selectedLineId = ids[position]
                            }
                        }
                }
            )
            button(if (app.loadingLines) "Đang tải…" else "Tải lại số gọi ra") {
                    perform { app.refreshCallingLines() }
                }
                .isEnabled = !app.loadingLines
            if (app.linesError.isNotEmpty()) text(app.linesError, 12)
            else if (!app.loadingLines && app.callingLines.isEmpty())
                text("Tài khoản chưa có số gọi ra riêng.", 12)
        }
        listOf("123", "456", "789", "*0#").forEach { digits ->
            row(
                digits.map { digit ->
                    Button(this).apply {
                        text = "$digit\n${keyLetters(digit)}"
                        contentDescription = "Phím $digit"
                        isAllCaps = false
                        textSize = 22f
                        minHeight = dp(68)
                        background = rounded(Color.WHITE, 22)
                        setTextColor(Color.rgb(18, 48, 71))
                        setOnClickListener { input.append(digit.toString()) }
                        if (digit == '0')
                            setOnLongClickListener {
                                input.append("+")
                                true
                            }
                    }
                }
            )
        }
        button("⌫ Xóa số") {
                if (input.text.isNotEmpty())
                    input.text.delete(input.text.length - 1, input.text.length)
            }
            .setOnLongClickListener {
                input.text.clear()
                true
            }
        text(
            "Giao diện cuộc gọi: ${if (app.sdkUi) "Có sẵn của SDK" else "Tự dựng"}",
            12,
            Color.rgb(100, 119, 135),
        )
        button("Gọi điện", primary = true) {
            number = DemoInput.phone(input.text.toString())
            if (number.isBlank()) {
                showNotice("Nhập số điện thoại cần gọi")
                return@button
            }
            if (!hasMicrophone()) {
                requestCallPermissions()
                showNotice("Cấp quyền micro rồi bấm Gọi điện")
                return@button
            }
            perform {
                val client = app.client ?: throw VoiceError("LOGIN_REQUIRED", "createCall")
                val call = client.createCall(CallOptions(number, callerId = app.selectedLineId))
                app.callLineLabel =
                    app.callingLines.firstOrNull { it.id == app.selectedLineId }?.label
                        ?: "Mặc định tổng đài"
                app.displayedCall = call
                observedCallId = call.getSnapshot().id
                page = "call"
                render()
                call.start()
            }
        }
        if (snapshot?.callDevice in listOf(CallDevice.BROWSER, CallDevice.OTHER))
            text("Chọn thiết bị Mobile trong cấu hình trước khi gọi.", 13, Color.rgb(173, 95, 25))
    }

    private fun settings() {
        button("‹  Bàn phím") {
            page = "dialer"
            render()
        }
        text("Cấu hình thoại", 32)
        val client = app.client
        val snapshot = client?.getSnapshot()
        card("Nhân viên") {
            text(snapshot?.session?.agent?.displayName ?: snapshot?.session?.agent?.id ?: "", 20)
            text(
                "${app.preferences.getString("domain", "")}\nAPI: ${app.preferences.getString("api", DemoApplication.DEFAULT_API)}\nPhiên: ${snapshot?.session?.status}",
                14,
            )
        }
        card("Trạng thái thoại") {
            text(availabilityLabel(snapshot?.availability), 15)
            button("Available — đang trong ca", snapshot?.availability == Availability.AVAILABLE) {
                perform {
                    client?.setAvailability(Availability.AVAILABLE)
                    render()
                }
            }
            button("Not available — ngoài ca", snapshot?.availability == Availability.UNAVAILABLE) {
                perform {
                    client?.setAvailability(Availability.UNAVAILABLE)
                    render()
                }
            }
        }
        card("Thiết bị nhận thoại") {
            text("Thiết bị hiện tại: ${deviceLabel(snapshot?.callDevice)}", 15)
            button("Nghe gọi trên Mobile", snapshot?.callDevice == CallDevice.MOBILE) {
                perform {
                    client?.selectCallDevice(CallDevice.MOBILE)
                    render()
                }
            }
            button("Nghe gọi trên Web", snapshot?.callDevice == CallDevice.BROWSER) {
                perform {
                    client?.selectCallDevice(CallDevice.BROWSER)
                    render()
                }
            }
        }
        card("Giao diện nghe gọi") {
            modePicker()
            text("Cả hai chế độ sử dụng cùng phiên và cùng cuộc gọi SDK.", 14)
        }
        card("Nhận cuộc gọi đến") {
            text(app.pushStatus, 14)
            button("Cấp quyền micro / thông báo / Bluetooth") { requestCallPermissions() }
            if (Build.VERSION.SDK_INT >= 34)
                button("Quyền hiển thị cuộc gọi toàn màn hình") {
                    startActivity(
                        android.content.Intent(
                            android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            android.net.Uri.parse("package:$packageName"),
                        )
                    )
                }
            button("Cấu hình Firebase") { firebaseDialog() }
            button("Đăng ký lại thông báo thoại") { app.refreshPushToken() }
        }
        card("Nhật ký thử nghiệm") {
            text(app.history.joinToString("\n").ifEmpty { "Chưa có sự kiện." }, 12)
            button("Sao chép log chẩn đoán") {
                val clipboard = getSystemService(android.content.ClipboardManager::class.java)
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText(
                        "Caresoft Voice",
                        app.history.joinToString("\n"),
                    )
                )
                showNotice("Đã sao chép nhật ký sự kiện (không chứa token).")
            }
        }
        button("Kiểm tra phiên") { perform { app.refreshSession() } }
        button("Kiểm tra sẵn sàng") { perform { showNotice(client?.checkReadiness().toString()) } }
        button("Đăng xuất") {
            perform {
                app.logout()
                page = "welcome"
                render()
            }
        }
    }

    private fun callScreen() {
        val call =
            app.displayedCall
                ?: run {
                    page = "dialer"
                    dialer()
                    return
                }
        val snapshot = call.getSnapshot()
        text(
            if (snapshot.direction == CallDirection.INCOMING) "Cuộc gọi đến" else "Cuộc gọi đi",
            14,
            Color.rgb(8, 126, 117),
        )
        app.callLineLabel?.let { text(it, 13) }
        var updateCustomCall: (CallSnapshot) -> Unit = {}
        if (app.sdkUi) {
            callView =
                VoiceCallView(this).also {
                    content.addView(it)
                    it.bind(call)
                }
        } else {
            spacer(20)
            text("●", 66, Color.rgb(8, 126, 117)).apply { gravity = Gravity.CENTER }
            text(snapshot.remote.displayName ?: snapshot.remote.number ?: "Caresoft", 30).apply {
                gravity = Gravity.CENTER
            }
            text(app.client?.getSnapshot()?.session?.agent?.displayName ?: "", 13).apply {
                gravity = Gravity.CENTER
            }
            callStatus = text(stateText(snapshot), 18)
            duration =
                text(
                    DemoInput.duration(
                        snapshot.connectedAt,
                        snapshot.endedAt,
                        System.currentTimeMillis(),
                    ),
                    26,
                )
            val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            content.addView(controls)
            fun update(value: CallSnapshot) {
                callStatus?.text = stateText(value)
                controls.removeAllViews()
                fun action(label: String, operation: suspend () -> Unit) {
                    controls.addView(
                        Button(this).apply {
                            text = label
                            isAllCaps = false
                            minHeight = dp(52)
                            background = rounded(Color.WHITE, 22)
                            setTextColor(Color.rgb(8, 126, 117))
                            setOnClickListener { perform(operation) }
                        }
                    )
                }
                if (
                    value.direction == CallDirection.INCOMING &&
                        value.state in listOf(CallState.NEW, CallState.RINGING)
                ) {
                    action("Trả lời") {
                        if (!hasMicrophone()) {
                            requestCallPermissions()
                            showNotice("Cấp quyền micro rồi bấm Trả lời")
                        } else call.answer()
                    }
                    action("Từ chối") { call.reject() }
                }
                if (value.state == CallState.ACTIVE)
                    action(if (value.muted) "Bật micro" else "Tắt micro") {
                        call.setMuted(!value.muted)
                    }
                if (value.state == CallState.ACTIVE) {
                    action(if (value.held) "Tiếp tục" else "Giữ máy") { call.setHeld(!value.held) }
                    action("Âm thanh") {
                        val routes = call.getAudioRoutes()
                        AlertDialog.Builder(this)
                            .setTitle("Chọn đường âm thanh")
                            .setSingleChoiceItems(
                                routes.map { it.name }.toTypedArray(),
                                routes.indexOfFirst { it.selected },
                            ) { dialog, which ->
                                perform { call.setAudioRoute(routes[which].id) }
                                dialog.dismiss()
                            }
                            .show()
                    }
                    action("Bàn phím DTMF") { dtmfDialog(call) }
                    action("Thông số cuộc gọi") { showNotice(call.getStats().toString()) }
                }
                if (value.state != CallState.ENDED) action("Kết thúc cuộc gọi") { call.hangup() }
            }
            update(snapshot)
            updateCustomCall = ::update
        }
        modePicker()
        button("Ẩn màn hình cuộc gọi") {
            page = "dialer"
            render()
        }
        val close =
            button("Đóng chi tiết") {
                app.displayedCall = null
                observedCallId = null
                page = "dialer"
                render()
            }
        close.visibility = if (snapshot.state == CallState.ENDED) View.VISIBLE else View.GONE
        callSubscription =
            call.observe { event, _ ->
                when (event) {
                    is CallEvent.Changed -> {
                        updateCustomCall(event.snapshot)
                        close.visibility =
                            if (event.snapshot.state == CallState.ENDED) View.VISIBLE else View.GONE
                        if (event.snapshot.state == CallState.ENDED)
                            window.clearFlags(
                                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            )
                    }
                    is CallEvent.Error -> showNotice(event.error.message ?: event.error.code)
                    else -> Unit
                }
            }
    }

    private fun dtmfDialog(call: VoiceCall) {
        val grid =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(8), dp(20), dp(8))
            }
        listOf("123", "456", "789", "*0#").forEach { digits ->
            grid.addView(
                LinearLayout(this).apply {
                    digits.forEach { digit ->
                        addView(
                            Button(this@MainActivity).apply {
                                text = digit.toString()
                                setOnClickListener { perform { call.sendDTMF(digit.toString()) } }
                            },
                            LinearLayout.LayoutParams(0, dp(56), 1f),
                        )
                    }
                }
            )
        }
        AlertDialog.Builder(this)
            .setTitle("Bàn phím DTMF")
            .setView(grid)
            .setPositiveButton("Đóng", null)
            .show()
    }

    private fun firebaseDialog() {
        val form =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(8), dp(20), dp(8))
            }
        val entries =
            listOf(
                "firebaseAppId" to "Firebase Android App ID",
                "firebaseApiKey" to "Firebase API key",
                "firebaseSender" to "Messaging sender ID",
                "firebaseProject" to "Project ID",
            )
        val fields =
            entries.associate { (key, hint) ->
                key to
                    EditText(this).apply {
                        this.hint = hint
                        setText(app.preferences.getString(key, ""))
                        isSingleLine = true
                        form.addView(this)
                    }
            }
        AlertDialog.Builder(this)
            .setTitle("Firebase của ứng dụng")
            .setMessage(
                "Dùng cấu hình dự án thật cho package $packageName. Thay đổi cấu hình đã khởi tạo cần khởi động lại ứng dụng."
            )
            .setView(form)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu") { _, _ ->
                app.preferences.edit().apply {
                    fields.forEach { (key, input) -> putString(key, input.text.toString().trim()) }
                    apply()
                }
                app.initializeFirebase()
                app.refreshPushToken()
            }
            .show()
    }

    private fun stateText(value: CallSnapshot) =
        when (value.state) {
            CallState.NEW -> "Sẵn sàng"
            CallState.DIALING -> "Đang gọi…"
            CallState.RINGING ->
                if (value.direction == CallDirection.INCOMING) "Đang đổ chuông · cuộc gọi đến"
                else "Đang đổ chuông…"
            CallState.CONNECTING -> "Đang kết nối…"
            CallState.ACTIVE -> if (value.held) "Đang giữ máy" else "Đang trong cuộc gọi"
            CallState.ENDED -> "Đã kết thúc · ${value.endReason}"
        }

    private fun hasMicrophone() =
        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun requestCallPermissions() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 31) permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        if (Build.VERSION.SDK_INT >= 33) permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing =
            permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 41)
    }

    private fun perform(action: suspend () -> Unit) {
        app.scope.launch {
            try {
                app.notice = ""
                notice?.text = ""
                action()
            } catch (failure: Exception) {
                showNotice(
                    (failure as? VoiceError)?.message
                        ?: failure.message
                        ?: "Thao tác chưa thành công"
                )
            }
        }
    }

    private fun showNotice(message: String) {
        app.notice = message
        notice?.text = message
    }

    private fun field(label: String, value: String, secret: Boolean = false): EditText {
        spacer(12)
        val caption = text(label, 13)
        return EditText(this).apply {
            id = View.generateViewId()
            caption.labelFor = id
            contentDescription = label
            setText(value)
            isSingleLine = true
            inputType =
                if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSaveEnabled = !secret
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            background = rounded(Color.WHITE, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            minHeight = dp(50)
            content.addView(this)
        }
    }

    private fun text(value: String, size: Int, color: Int = Color.rgb(18, 48, 71)): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            setLineSpacing(dp(3).toFloat(), 1f)
            content.addView(this)
        }

    private fun button(label: String, primary: Boolean = false, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            minHeight = dp(52)
            textSize = 16f
            background =
                rounded(if (primary) Color.rgb(8, 126, 117) else Color.rgb(229, 243, 239), 16)
            setTextColor(if (primary) Color.WHITE else Color.rgb(18, 48, 71))
            setOnClickListener { action() }
            content.addView(
                this,
                LinearLayout.LayoutParams(-1, -2).apply {
                    topMargin = dp(10)
                    bottomMargin = dp(4)
                },
            )
        }

    private fun row(views: List<View>) {
        content.addView(
            LinearLayout(this).apply {
                views.forEach {
                    addView(
                        it,
                        LinearLayout.LayoutParams(0, -2, 1f).apply {
                            setMargins(dp(4), dp(5), dp(4), dp(5))
                        },
                    )
                }
            }
        )
    }

    private fun spacer(height: Int) {
        content.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun rounded(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun keyLetters(digit: Char) =
        mapOf(
            '2' to "ABC",
            '3' to "DEF",
            '4' to "GHI",
            '5' to "JKL",
            '6' to "MNO",
            '7' to "PQRS",
            '8' to "TUV",
            '9' to "WXYZ",
            '0' to "+",
        )[digit] ?: " "

    private fun availabilityLabel(value: Availability?) = DemoInput.availabilityLabel(value)

    private fun deviceLabel(value: CallDevice?) =
        when (value) {
            CallDevice.MOBILE -> "Mobile"
            CallDevice.BROWSER -> "Web"
            else -> "Chưa xác định"
        }

    private fun modePicker() {
        val group =
            RadioGroup(this).apply {
                orientation = RadioGroup.HORIZONTAL
                contentDescription = "Giao diện nghe gọi"
            }
        listOf("UI của SDK", "UI tự dựng").forEachIndexed { index, label ->
            group.addView(
                RadioButton(this).apply {
                    id = View.generateViewId()
                    text = label
                    isChecked = app.sdkUi == (index == 0)
                    setOnClickListener {
                        app.sdkUi = index == 0
                        render()
                    }
                },
                LinearLayout.LayoutParams(0, dp(52), 1f),
            )
        }
        content.addView(group)
    }

    private fun card(title: String, body: () -> Unit) {
        val parent = content
        content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(20), dp(20), dp(20), dp(20))
                background = rounded(Color.WHITE, 22)
            }
        parent.addView(content, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        text(title.uppercase(), 12, Color.rgb(100, 119, 135))
        spacer(12)
        body()
        content = parent
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
