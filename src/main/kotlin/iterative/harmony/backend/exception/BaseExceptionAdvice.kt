package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException
import io.sentry.Sentry
import iterative.harmony.backend.util.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BaseExceptionAdvice {
    private val log = getLogger()
    private val COMMON_EXCEPTIONS =
        setOf(
            AccessTokenMissingOrMalformedInRequestException::class,
            RefreshTokenNotInRequestException::class,
            RefreshTokenExpiredException::class,
        )
    @Autowired private lateinit var environment: Environment

    fun logException(ex: Exception) {
        log.error("${ex.javaClass.simpleName}: ${ex.message}")
    }

    fun reportExceptionToSentry(ex: Exception) {
        if (!environment.activeProfiles.contains("prod")) return // don't report when not in prod
        if (ex::class in COMMON_EXCEPTIONS) return
        Sentry.captureException(ex)
    }

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun notFoundExceptionHandler(ex: UserNotFoundException): String? {
        logException(ex)
        return null
    }

    @ExceptionHandler(
        IllegalArgumentException::class,
        AccessTokenMissingOrMalformedInRequestException::class,
        RefreshTokenNotInRequestException::class,
    )
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequestExceptionHandler(ex: IllegalArgumentException): String? {
        logException(ex)
        return null
    }

    @ExceptionHandler(
        JwtException::class,
        RefreshTokenNotInDBException::class,
        JtiNotInRefreshTokenException::class,
        RefreshTokenExpiredException::class,
        RefreshTokenFieldMismatchException::class,
        TokenFingerprintMismatchException::class,
        UnexpectedRefreshTokenVerificationException::class,
        UnparseableTokenException::class,
    )
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun unauthorizedExceptionHandler(ex: RuntimeException): String? {
        logException(ex)
        return null
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun internalServerExceptionHandler(ex: Exception): String? {
        logException(ex)
        reportExceptionToSentry(ex)
        return null
    }
}
