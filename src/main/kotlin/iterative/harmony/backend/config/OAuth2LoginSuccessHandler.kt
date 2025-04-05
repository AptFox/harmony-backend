package iterative.harmony.backend.config

import com.fasterxml.jackson.databind.ObjectMapper
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.util.SecurityConstants.ACCESS_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2LoginSuccessHandler : AuthenticationSuccessHandler {

    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    @Autowired private lateinit var jwtTokenService: JwtTokenService

    private fun createRefreshTokenCookie(refreshToken: String): Cookie {
        return Cookie(REFRESH_TOKEN_NAME, refreshToken).apply {
            isHttpOnly = true
            secure = true
            path = REFRESH_TOKEN_PATH
            maxAge = COOKIE_EXPIRATION_IN_SECONDS
            setAttribute("SameSite", "None")
        }
    }

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        val principal = authentication.principal as DefaultOAuth2User
        val userId = principal.name

        val refreshToken = jwtTokenService.generateRefreshToken(userId)
        val refreshTokenCookie = createRefreshTokenCookie(refreshToken)

        response.addCookie(refreshTokenCookie)

        val roles = principal.authorities.map { it.authority }
        val accessToken = jwtTokenService.generateAccessToken(userId, roles)

        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            ObjectMapper().writeValueAsString(mapOf(ACCESS_TOKEN_NAME to accessToken))
        )
        // TODO re-enable this redirect after frontend is ready for pop-up oauth
        //        response.sendRedirect("${frontEndBaseUrl}/auth/callback")
    }
}
