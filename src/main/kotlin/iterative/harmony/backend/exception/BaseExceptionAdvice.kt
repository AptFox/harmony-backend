package iterative.harmony.backend.exception

import io.jsonwebtoken.JwtException
import io.sentry.Sentry
import iterative.harmony.backend.util.getLogger
import jakarta.validation.ConstraintViolationException
import kotlin.collections.mapOf
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
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
        if (ex::class in COMMON_EXCEPTIONS) return
        log.error("${ex.javaClass.simpleName}: ${ex.message}", ex)
    }

    fun reportExceptionToSentry(ex: Exception) {
        if (!environment.activeProfiles.contains("prod")) return // don't report unless prod
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

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException
    ): ResponseEntity<Map<String, List<String?>>> {
        val errors =
            ex.bindingResult.allErrors.map { error ->
                val field = error.code ?: error.objectName
                "Input Validation Error: $field: ${error.defaultMessage}"
            }
        return ResponseEntity.badRequest().body(mapOf("errors" to errors))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        ex: ConstraintViolationException
    ): ResponseEntity<Map<String, List<String>>> {

        val errors =
            ex.constraintViolations.map { violation ->
                val path = violation.propertyPath.toString()
                val message = violation.message
                val field = path.substringAfterLast('.')

                "Input Validation Error: $field (${path}): $message"
            }

        return ResponseEntity.badRequest().body(mapOf("errors" to errors))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleParsingError(
        ex: HttpMessageNotReadableException
    ): ResponseEntity<Map<String, String>?> {
        val errorMsg = "Input Validation Error: ${ex.message.toString()}"
        return ResponseEntity.badRequest().body(mapOf("errorMsg" to errorMsg))
    }

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun internalServerExceptionHandler(ex: Exception): String? {
        logException(ex)
        reportExceptionToSentry(ex)
        return null
    }
}
