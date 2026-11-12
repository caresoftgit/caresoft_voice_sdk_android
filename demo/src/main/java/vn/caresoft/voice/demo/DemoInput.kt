package vn.caresoft.voice.demo

internal object DemoInput {
    fun validateLogin(domain: String, appId: String, api: String, token: String) {
        fun validText(value: String, max: Int) =
            value.isNotBlank() &&
                value.toByteArray().size <= max &&
                value.none { it.code < 32 || it.code == 127 }
        require(validText(domain, 256)) { "Domain không hợp lệ" }
        require(validText(appId, 64)) { "App ID không hợp lệ" }
        require(validText(token, 16384)) { "Token không hợp lệ" }
        val uri = runCatching { java.net.URI(api) }.getOrNull()
        require(
            uri != null &&
                uri.scheme == "https" &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        ) {
            "API phải là URL HTTPS hợp lệ, không chứa thông tin đăng nhập"
        }
    }

    fun availabilityLabel(value: vn.caresoft.voice.Availability?): String =
        when (value) {
            vn.caresoft.voice.Availability.AVAILABLE -> "Đang trong ca"
            vn.caresoft.voice.Availability.UNAVAILABLE -> "Ngoài ca"
            else -> "Chưa xác định"
        }

    fun phone(value: String): String = value.filter { it in "+0123456789*#" }.take(40)

    fun duration(connected: Long?, ended: Long?, now: Long): String {
        val seconds =
            if (connected == null) 0 else ((ended ?: now) - connected).coerceAtLeast(0) / 1000
        return "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
    }
}
