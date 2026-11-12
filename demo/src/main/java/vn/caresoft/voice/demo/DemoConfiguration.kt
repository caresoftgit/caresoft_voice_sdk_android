package vn.caresoft.voice.demo

import java.net.URI
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import vn.caresoft.voice.IceServer

internal object DemoConfiguration {
    const val DEFAULT_ICE = "[]"

    fun validateEndpoint(value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri != null &&
                uri.scheme == "https" &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        ) {
            "Token endpoint phải dùng HTTPS, không chứa thông tin đăng nhập"
        }
    }

    fun parseIce(value: String): List<IceServer> {
        require(value.length <= 65536) { "ICE servers JSON quá dài" }
        val rows = JSONArray(value)
        require(rows.length() <= 16) { "Quá nhiều ICE servers" }
        return (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            val urls = row.getJSONArray("urls")
            require(urls.length() in 1..16) { "ICE server cần ít nhất một URL" }
            val addresses =
                (0 until urls.length()).map { n ->
                    val url = urls.getString(n)
                    require(
                        url.length <= 2048 &&
                            url.none { it.isWhitespace() || it.code < 32 } &&
                            url.substringBefore(':') in listOf("stun", "stuns", "turn", "turns") &&
                            url.substringAfter(':').isNotBlank()
                    ) {
                        "ICE URL không hợp lệ"
                    }
                    url
                }
            IceServer(
                addresses,
                row.optString("username").takeIf { it.isNotEmpty() },
                row.optString("credential").takeIf { it.isNotEmpty() },
            )
        }
    }

    fun parseToken(body: String): String {
        require(body.toByteArray().size < 65536) { "Phản hồi token quá lớn" }
        val token = JSONObject(body).get("token")
        require(
            token is String &&
                token.isNotBlank() &&
                token.length <= 16384 &&
                token.none { it.code < 32 }
        ) {
            "Token backend không hợp lệ"
        }
        return token
    }

    suspend fun fetchToken(endpoint: String): String =
        withContext(Dispatchers.IO) {
            validateEndpoint(endpoint)
            val connection = URI(endpoint).toURL().openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                require(connection.responseCode == 200) { "Backend chưa cấp token" }
                val bytes =
                    connection.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (output.size() < 65536) {
                            val count =
                                input.read(buffer, 0, minOf(buffer.size, 65536 - output.size()))
                            if (count == -1) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                require(bytes.size < 65536) { "Phản hồi token quá lớn" }
                parseToken(bytes.toString(Charsets.UTF_8))
            } finally {
                connection.disconnect()
            }
        }
}
