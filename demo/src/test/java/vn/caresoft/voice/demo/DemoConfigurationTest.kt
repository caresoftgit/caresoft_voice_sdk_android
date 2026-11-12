package vn.caresoft.voice.demo

import org.junit.Assert.*
import org.junit.Test

class DemoConfigurationTest {
    @Test
    fun iceRequiresValidBoundedUrlsAndKeepsTurnCredentials() {
        val servers =
            DemoConfiguration.parseIce(
                """[{"urls":["turn:voice.example:3478"],"username":"agent","credential":"temporary"}]"""
            )
        assertEquals("temporary", servers.single().credential)
        listOf(
                "{}",
                "[{\"urls\":[]}]",
                "[{\"urls\":[\"https://bad\"]}]",
                "[{\"urls\":[\"turn:bad\\n\"]}]",
            )
            .forEach { assertThrows(Exception::class.java) { DemoConfiguration.parseIce(it) } }
    }

    @Test
    fun tokenEndpointNeverAcceptsPlaintextOrEmbeddedCredentials() {
        DemoConfiguration.validateEndpoint("https://tokens.example/voice")
        listOf(
                "http://tokens.example",
                "https://user:pass@tokens.example",
                "https://tokens.example/#secret",
                "file:///tmp/token",
            )
            .forEach {
                assertThrows(IllegalArgumentException::class.java) {
                    DemoConfiguration.validateEndpoint(it)
                }
            }
    }

    @Test
    fun backendResponseMustContainABoundedNonBlankToken() {
        assertEquals("token", DemoConfiguration.parseToken("""{"token":"token"}"""))
        listOf("{}", "{\"token\":\"\"}", "{\"token\":12}", "x".repeat(65536)).forEach {
            assertThrows(Exception::class.java) { DemoConfiguration.parseToken(it) }
        }
    }
}
