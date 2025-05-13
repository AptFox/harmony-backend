package iterative.harmony.backend.config

import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.Utils
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler : AuthenticationSuccessHandler {

    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String
    @Autowired private lateinit var environment: Environment

    @Autowired private lateinit var jwtTokenService: JwtTokenService

    private fun createRefreshTokenCookie(refreshToken: String): Cookie {
        val isDevEnv = environment.activeProfiles.contains("dev")
        val secureCookie = !isDevEnv
        val sameSite = if (isDevEnv) "Lax" else "None"
        return Cookie(REFRESH_TOKEN_NAME, refreshToken).apply {
            isHttpOnly = true
            secure = secureCookie
            path = AUTH_PATH
            maxAge = COOKIE_EXPIRATION_IN_SECONDS
            setAttribute("SameSite", sameSite)
        }
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as DefaultOAuth2User
        val userId = principal.name

        val userAgent = Utils().getUserAgentFromRequest(request)
        val refreshToken = jwtTokenService.generateRefreshToken(userId, userAgent)
        val refreshTokenCookie = createRefreshTokenCookie(refreshToken)

        response.addCookie(refreshTokenCookie)

        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.sendRedirect("${frontEndBaseUrl}/auth/callback")
    }
}
