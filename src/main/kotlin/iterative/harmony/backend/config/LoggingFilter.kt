package iterative.harmony.backend.config

import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.setUserIdInLogs
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class LoggingFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val userId = Utils().getUserIdFromSecurityContext()
            setUserIdInLogs(userId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.clear() // Clear logging context
        }
    }
}
