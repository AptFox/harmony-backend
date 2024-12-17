package iterative.harmony.backend.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class BaseExceptionAdvice {
    private val log = LoggerFactory.getLogger(BaseExceptionAdvice::class.java)

    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun exceptionHandler(ex: UserNotFoundException): String? {
        log.info("UserNotFoundException: ${ex.message}")
        return ex.message
    }
}
