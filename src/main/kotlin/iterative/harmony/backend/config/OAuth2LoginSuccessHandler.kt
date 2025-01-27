package iterative.harmony.backend.config

import iterative.harmony.backend.service.JwtTokenService
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import java.util.UUID

class OAuth2LoginSuccessHandler(
    private val jwtTokenService: JwtTokenService
): AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as DefaultOAuth2User
        val userId = principal.attributes["user_id"] as UUID

        val roles = principal.authorities.map { it.authority }
        val jwtToken = jwtTokenService.generateToken(userId, roles)
        val harmonyCookie = Cookie("harmony_access_token", jwtToken)
        harmonyCookie.isHttpOnly = true
        harmonyCookie.path = "/"
        harmonyCookie.maxAge = 60 * 60 * 24
        response.addCookie(harmonyCookie)

        response.sendRedirect("/") // TODO: redirect to front-end here (/user/@me maybe?)
    }
}