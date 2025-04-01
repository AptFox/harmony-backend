package iterative.harmony.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import iterative.harmony.backend.service.AuthService
import iterative.harmony.backend.util.SecurityConstants.ACCESS_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController {

    @Autowired private lateinit var authService: AuthService

    @PostMapping("/logout")
    fun logout(): ResponseEntity<String> {
        val emptyRefreshTokenCookie = authService.generateEmptyRefreshTokenCookie()
        return ResponseEntity.ok().header(SET_COOKIE, emptyRefreshTokenCookie).build()
    }

    @PostMapping("/refresh_token")
    fun refreshToken(
        @CookieValue(REFRESH_TOKEN_NAME) refreshTokenFromRequest: String?
    ): ResponseEntity<String> {
        val newTokenMap = authService.rotateRefreshToken(refreshTokenFromRequest)

        return ResponseEntity.ok()
            .header(SET_COOKIE, newTokenMap["newRefreshTokenCookie"])
            .body(
                ObjectMapper()
                    .writeValueAsString(mapOf(ACCESS_TOKEN_NAME to newTokenMap["newAccessToken"]))
            )
    }
}
