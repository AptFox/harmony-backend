package iterative.harmony.backend.config

import io.sentry.Sentry
import iterative.harmony.backend.exception.ScheduledTaskException
import iterative.harmony.backend.util.getLogger
import java.time.Instant
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

@Configuration
@EnableScheduling
class SchedulingConfig {
    private val log = getLogger()

    @Bean
    fun taskScheduler(): TaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 1
        scheduler.setThreadNamePrefix("harmony-scheduled-task-")
        scheduler.setErrorHandler { ex -> reportErrorToSentry(ex) }
        scheduler.setWaitForTasksToCompleteOnShutdown(true)
        scheduler.setAwaitTerminationSeconds(30)
        scheduler.initialize()
        return scheduler
    }

    private fun reportErrorToSentry(ex: Throwable) {
        val threadName = Thread.currentThread().name
        Sentry.withScope { scope ->
            scope.setTag("scheduler", "taskScheduler")
            scope.setTag("thread_name", threadName)

            scope.setContexts(
                "Task Details",
                mapOf("timestamp" to Instant.now().toString(), "executor" to threadName),
            )

            val wrappedEx =
                ScheduledTaskException("Scheduled task failed on thread: $threadName", ex)
            Sentry.captureException(wrappedEx)
        }
        log.error("Scheduled task failed on thread $threadName", ex)
    }
}
