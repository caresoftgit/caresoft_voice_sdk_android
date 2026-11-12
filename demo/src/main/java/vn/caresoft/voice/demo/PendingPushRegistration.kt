package vn.caresoft.voice.demo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Only owner/revision are persisted. Firebase retains the token; every attempt fetches its latest
 * value.
 */
internal data class PendingPushWork(val owner: String, val revision: String)

internal data class PushBinding<C : Any>(val client: C, val owner: String)

internal class PendingPushRegistration<C : Any>(
    private val current: () -> PushBinding<C>?,
    private val read: () -> PendingPushWork?,
    private val write: (PendingPushWork?) -> Unit,
    private val checkSession: suspend (C) -> Boolean,
    private val latestToken: suspend () -> String,
    private val register: suspend (C, String) -> Unit,
) {
    private val mutex = Mutex()

    fun queue(revision: String): Boolean {
        val binding = current() ?: return false
        write(PendingPushWork(binding.owner, revision))
        return true
    }

    /** True means work remains and the scheduler must retry, never that registration succeeded. */
    suspend fun flush(): Boolean =
        mutex.withLock {
            val work = read() ?: return@withLock false
            val binding = current() ?: return@withLock true
            if (work.owner != binding.owner) {
                if (read() == work) write(null)
                return@withLock read() != null
            }
            fun unchanged(): Boolean =
                current()?.let { it.client === binding.client && it.owner == binding.owner } ==
                    true && read() == work
            try {
                if (!checkSession(binding.client) || !unchanged()) return@withLock read() != null
                val token = latestToken()
                if (!unchanged()) return@withLock read() != null
                check(token.isNotBlank())
                register(binding.client, token)
                if (unchanged()) write(null)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                /* Keep the marker for JobScheduler's bounded-backoff retry. */
            }
            read() != null
        }
}
