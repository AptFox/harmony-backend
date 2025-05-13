package iterative.harmony.backend.config

import io.jsonwebtoken.JwtException
import io.sentry.Sentry
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.util.SecurityConstants.DISCORD_OAUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.ERROR_PATH
import iterative.harmony.backend.util.SecurityConstants.FAVICON_PATH
import iterative.harmony.backend.util.SecurityConstants.LOGOUT_PATH
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import iterative.harmony.backend.util.Utils
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter : OncePerRequestFilter() {

    @Autowired private lateinit var tokenService: JwtTokenService

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        try {
            val urlIsPublic =
                listOf(
                        "/",
                        REFRESH_TOKEN_PATH,
                        DISCORD_OAUTH_PATH,
                        ERROR_PATH,
                        LOGOUT_PATH,
                        FAVICON_PATH,
                    )
                    .any { pattern -> request.requestURI!!.contentEquals(pattern) }
            return urlIsPublic
        } catch (e: Exception) {
            Sentry.captureException(e)
            return false
        }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            val token = getTokenFromRequest(request)
            val userAgentFingerprint = Utils().generateUserAgentFingerprint(request)
            val auth = tokenService.getAuthentication(token, userAgentFingerprint)
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
        } catch (e: Exception) {
            if (e !is JwtException) {
                Sentry.captureException(e)
            }
            SecurityContextHolder.clearContext()
            response.sendError(401, "Unauthorized")
        }
    }

    private fun getTokenFromRequest(request: HttpServletRequest): String {
        val bearerToken = request.getHeader("Authorization")
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw JwtException("Missing or invalid token")
        }
        return bearerToken.substring(7)
    }
}
