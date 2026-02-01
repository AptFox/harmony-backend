package iterative.harmony.backend.util

import java.util.UUID
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

/*
 * This is just syntactic sugar to make it faster to declare loggers for each class
 */
inline fun <reified T> T.getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> T.setUserIdInLogs(userId: UUID) = {
    val userIdPrefix = userId.toString().take(8)
    MDC.put("userId", userIdPrefix)
}

inline fun <reified T> T.setUserIdInLogs(userId: String) = {
    val userIdPrefix = userId.take(8)
    MDC.put("userId", userIdPrefix)
}
