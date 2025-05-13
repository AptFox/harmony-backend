package iterative.harmony.backend.config

import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class TokenRefreshFilter : OncePerRequestFilter() {

    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        return request.requestURI != REFRESH_TOKEN_PATH
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val origin = request.getHeader("Origin").orEmpty()
        val referer = request.getHeader("Referer").orEmpty()

        val originOk = origin.isNotEmpty() && origin.startsWith(frontEndBaseUrl)
        val refererOk = origin.isNotEmpty() && referer.startsWith(frontEndBaseUrl)

        if (!originOk || !refererOk) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid origin or referer")
            return
        }

        filterChain.doFilter(request, response)
    }
}
