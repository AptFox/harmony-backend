package iterative.harmony.backend.util

import iterative.harmony.backend.util.LogConstants.SCHEDULED_TASK
import iterative.harmony.backend.util.LogConstants.USER_ID
import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/*
 * This is just syntactic sugar to make it faster to declare loggers for each class
 */
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

// TODO: create 'Loggable' class and have all other classes inherit from it.

fun setUserIdInLogs(userId: UUID) {
    setUserIdInLogs(userId.toString())
}

fun setUserIdInLogs(userId: String) {
    val userIdPrefix = userId.take(8)
    MDC.put(USER_ID, userIdPrefix)
}

fun setScheduledTaskInLogs(task: String) {
    MDC.put(SCHEDULED_TASK, task)
}

// Clear logging context
fun clearLoggingContext() {
    MDC.clear()
}
