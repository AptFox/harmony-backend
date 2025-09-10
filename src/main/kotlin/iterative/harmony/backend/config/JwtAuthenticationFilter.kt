package iterative.harmony.backend.config

import io.jsonwebtoken.JwtException
import io.sentry.Sentry
import iterative.harmony.backend.exception.AccessTokenMissingOrMalformedInRequestException
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
            val accessToken = getAccessTokenFromRequest(request)
            val userAgentFingerprint = Utils().generateUserAgentFingerprint(request)
            val auth =
                tokenService.getAuthenticationFromAccessToken(accessToken, userAgentFingerprint)
            SecurityContextHolder.getContext().authentication = auth
            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            if (ex !is JwtException) {
                Sentry.captureException(ex)
            }
            SecurityContextHolder.clearContext()
            response.sendError(401, "Unauthorized")
        }
    }

    private fun getAccessTokenFromRequest(request: HttpServletRequest): String {
        val bearerToken = request.getHeader("Authorization")
        val isMissingOrMalformed = bearerToken == null || !bearerToken.startsWith("Bearer ")
        if (isMissingOrMalformed) throw AccessTokenMissingOrMalformedInRequestException()

        return bearerToken.substring(7)
    }
}
