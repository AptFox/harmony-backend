package iterative.harmony.backend.exception

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
        return ex.message
    }
}
