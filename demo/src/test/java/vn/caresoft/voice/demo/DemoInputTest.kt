package vn.caresoft.voice.demo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DemoInputTest {
    @Test
    fun validatesConfigurationBeforeItCanBeSaved() {
        DemoInput.validateLogin("tenant", "app", "https://test.example.vn", "runtime-token")
        listOf(
                "http://example.vn",
                "https://user:password@example.vn",
                "https://example.vn/#fragment",
                "https://",
                "bad url",
            )
            .forEach { api ->
                assertThrows(IllegalArgumentException::class.java) {
                    DemoInput.validateLogin("tenant", "app", api, "runtime-token")
                }
            }
        assertThrows(IllegalArgumentException::class.java) {
            DemoInput.validateLogin("tenant\n", "app", "https://example.vn", "token")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DemoInput.validateLogin("tenant", "x".repeat(65), "https://example.vn", "token")
        }
        assertThrows(IllegalArgumentException::class.java) {
            DemoInput.validateLogin("tenant", "app", "https://example.vn", "")
        }
    }

    @Test
    fun unknownAvailabilityNeverAppearsAsOffShift() {
        assertEquals(
            "Đang trong ca",
            DemoInput.availabilityLabel(vn.caresoft.voice.Availability.AVAILABLE),
        )
        assertEquals(
            "Ngoài ca",
            DemoInput.availabilityLabel(vn.caresoft.voice.Availability.UNAVAILABLE),
        )
        assertEquals(
            "Chưa xác định",
            DemoInput.availabilityLabel(vn.caresoft.voice.Availability.UNKNOWN),
        )
        assertEquals("Chưa xác định", DemoInput.availabilityLabel(null))
    }

    @Test
    fun phonePreservesLeadingZeroAndLimitsLength() {
        assertEquals("090123*#", DemoInput.phone(" 090 123a*# "))
        assertEquals(40, DemoInput.phone("1".repeat(80)).length)
    }

    @Test
    fun durationUsesEndedTimeAndNeverGoesNegative() {
        assertEquals("00:00", DemoInput.duration(null, null, 90000))
        assertEquals("01:05", DemoInput.duration(1000, 66000, 200000))
        assertEquals("00:00", DemoInput.duration(1000, null, 0))
    }
}
