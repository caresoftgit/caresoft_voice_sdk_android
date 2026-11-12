package vn.caresoft.voice.demo

import android.app.job.JobParameters
import android.app.job.JobService
import kotlinx.coroutines.*

/**
 * Normal persisted network work: never starts a call, microphone, Activity, or foreground service.
 */
class DemoTokenJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var task: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        task?.cancel()
        task =
            scope.launch {
                yield()
                val retry =
                    try {
                        withTimeout(120_000) {
                            (application as DemoApplication).flushPendingPushRegistration()
                        }
                    } catch (_: TimeoutCancellationException) {
                        true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        true
                    }
                jobFinished(params, retry)
            }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        task?.cancel()
        task = null
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
