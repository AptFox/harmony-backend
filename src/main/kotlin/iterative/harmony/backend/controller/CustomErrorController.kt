package iterative.harmony.backend.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.view.RedirectView

@Controller
class CustomErrorController : ErrorController {
    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest): RedirectView {
        val requestStatus = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)?.toString()
        val statusCode =
            if (requestStatus.isNullOrEmpty()) HttpStatus.INTERNAL_SERVER_ERROR.value()
            else requestStatus
        return RedirectView("$frontEndBaseUrl/error?statusCode=$statusCode")
    }
}
