package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException
import io.sentry.Sentry
import iterative.harmony.backend.util.getLogger
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BaseExceptionAdvice {
    private val log = getLogger()

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun exceptionHandler(ex: UserNotFoundException): String? {
        log.info("UserNotFoundException: ${ex.message}")
        return null
    }

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun exceptionHandler(ex: IllegalArgumentException): String? {
        log.info("IllegalArgumentException: ${ex.message}")
        return null
    }

    @ExceptionHandler(JwtException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun exceptionHandler(ex: JwtException): String? {
        log.info("JwtException: ${ex.message}")
        return null
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun exceptionHandler(ex: Exception): String? {
        log.error("Exception: ${ex.message}")
        Sentry.captureException(ex)
        return null
    }
}
