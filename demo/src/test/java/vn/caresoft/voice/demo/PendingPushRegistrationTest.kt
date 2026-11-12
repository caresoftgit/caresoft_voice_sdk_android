package vn.caresoft.voice.demo

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PendingPushRegistrationTest {
    private class Client(var authenticated: Boolean = false)

    @Test
    fun coldStartUnknownSessionRestoresBeforeLatestTokenRegistration(): Unit = runBlocking {
        val client = Client()
        val events = mutableListOf<String>()
        var work: PendingPushWork? = null
        val queue =
            PendingPushRegistration(
                { PushBinding(client, "saved-user") },
                { work },
                { work = it },
                {
                    events += "check"
                    it.authenticated = true
                    true
                },
                {
                    events += "token"
                    "rotated-token"
                },
                { c, token ->
                    assertTrue(c.authenticated)
                    events += "register:$token"
                },
            )
        assertTrue(queue.queue("rotation"))
        assertFalse(queue.flush())
        assertEquals(listOf("check", "token", "register:rotated-token"), events)
        assertNull(work)
    }

    @Test
    fun replacementWhileCheckingCannotRegisterOldWorkOnNewClient(): Unit = runBlocking {
        val old = Client()
        val newer = Client()
        var binding = PushBinding(old, "old-user")
        var work: PendingPushWork? = null
        val checkGate = CompletableDeferred<Unit>()
        val registered = mutableListOf<Client>()
        val queue =
            PendingPushRegistration(
                { binding },
                { work },
                { work = it },
                {
                    if (it === old) checkGate.await()
                    true
                },
                { "latest" },
                { c, _ -> registered += c },
            )
        queue.queue("old-request")
        val running = async { queue.flush() }
        yield()
        binding = PushBinding(newer, "new-user")
        queue.queue("new-request")
        checkGate.complete(Unit)
        assertTrue(running.await())
        assertTrue(registered.isEmpty())
        assertFalse(queue.flush())
        assertEquals(listOf(newer), registered)
    }

    @Test
    fun newerRotationDuringTokenFetchPreventsStaleRegistration(): Unit = runBlocking {
        val client = Client()
        var work: PendingPushWork? = null
        val tokenGate = CompletableDeferred<String>()
        val registered = mutableListOf<String>()
        var fetches = 0
        val queue =
            PendingPushRegistration(
                { PushBinding(client, "owner") },
                { work },
                { work = it },
                { true },
                { if (++fetches == 1) tokenGate.await() else "new-token" },
                { _, token -> registered += token },
            )
        queue.queue("first")
        val running = async { queue.flush() }
        yield()
        queue.queue("second")
        tokenGate.complete("old-token")
        assertTrue(running.await())
        assertTrue(registered.isEmpty())
        assertFalse(queue.flush())
        assertEquals(listOf("new-token"), registered)
    }

    @Test
    fun retrySurvivesCoordinatorRecreationAndUsesLatestToken(): Unit = runBlocking {
        val client = Client()
        var work: PendingPushWork? = null
        var latest = "old"
        var fail = true
        val registered = mutableListOf<String>()
        fun coordinator() =
            PendingPushRegistration(
                { PushBinding(client, "owner") },
                { work },
                { work = it },
                { if (fail) throw IllegalStateException("offline") else true },
                { latest },
                { _, token -> registered += token },
            )
        val first = coordinator()
        first.queue("request")
        assertTrue(first.flush())
        assertNotNull(work)
        fail = false
        latest = "rotated"
        assertFalse(coordinator().flush())
        assertEquals(listOf("rotated"), registered)
    }

    @Test
    fun logoutGenerationInvalidatesInFlightTokenFetch(): Unit = runBlocking {
        val client = Client()
        var binding: PushBinding<Client>? = PushBinding(client, "owner")
        var work: PendingPushWork? = null
        val tokenGate = CompletableDeferred<String>()
        var registrations = 0
        val queue =
            PendingPushRegistration(
                { binding },
                { work },
                { work = it },
                { true },
                { tokenGate.await() },
                { _, _ -> registrations++ },
            )
        queue.queue("request")
        val running = async { queue.flush() }
        yield()
        binding = null
        work = null
        tokenGate.complete("old-user-token")
        assertFalse(running.await())
        assertEquals(0, registrations)
    }
}
