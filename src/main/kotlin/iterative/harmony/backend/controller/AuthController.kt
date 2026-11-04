package iterative.harmony.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import iterative.harmony.backend.service.AuthService
import iterative.harmony.backend.util.SecurityConstants.ACCESS_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.Utils
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.CONTENT_TYPE
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
    fun logout(
        @CookieValue(value = REFRESH_TOKEN_NAME) refreshToken: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val nonNullRefreshToken = authService.throwIfNoRefreshToken(refreshToken)
        val fingerprint = Utils().getFingerprintFromRequest(request)
        val fingerprintHash = Utils().hashFingerprint(fingerprint)
        authService.deleteRefreshToken(nonNullRefreshToken, fingerprintHash)

        val emptyRefreshTokenCookie = authService.generateEmptyRefreshTokenCookie()
        val emptyFingerprintCookie = authService.generateEmptyFingerprintCookie()

        return ResponseEntity.status(200)
            .headers { headers ->
                headers.add(SET_COOKIE, emptyRefreshTokenCookie)
                headers.add(SET_COOKIE, emptyFingerprintCookie)
                headers.set(CONTENT_TYPE, "application/json")
            }
            .build()
    }

    @PostMapping("/refresh_token")
    fun refreshToken(
        @CookieValue(REFRESH_TOKEN_NAME) refreshTokenFromRequest: String?,
        request: HttpServletRequest,
    ): ResponseEntity<String> {
        val nonNullRefreshToken = authService.throwIfNoRefreshToken(refreshTokenFromRequest)
        val fingerprint = Utils().getFingerprintFromRequest(request)
        val fingerprintHash = Utils().hashFingerprint(fingerprint)
        val (newAccessToken, newRefreshTokenCookie) =
            authService.rotateTokens(nonNullRefreshToken, fingerprintHash)

        val renewedFingerprintCookie = authService.generateFingerprintCookie(fingerprint)

        return ResponseEntity.ok()
            .header(SET_COOKIE, newRefreshTokenCookie, renewedFingerprintCookie)
            .body(ObjectMapper().writeValueAsString(mapOf(ACCESS_TOKEN_NAME to newAccessToken)))
    }
}
