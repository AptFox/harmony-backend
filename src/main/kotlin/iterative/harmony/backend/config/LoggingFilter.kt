package iterative.harmony.backend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
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
            val authentication = SecurityContextHolder.getContext().authentication
            val isAuthenticatedUser = authentication != null && authentication.isAuthenticated
            val userId = if (isAuthenticatedUser) authentication.name.take(8) else "ANON"
            MDC.put("userId", userId)

            filterChain.doFilter(request, response)
        } finally {
            MDC.clear() // Clear logging context
        }
    }
}
