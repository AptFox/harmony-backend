package iterative.harmony.backend.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.JwtException
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.service.UserService
import iterative.harmony.backend.util.SecurityConstants.ACCESS_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import iterative.harmony.backend.util.getLogger
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val tokenService: JwtTokenService,
    private val userService: UserService,
) {
    private val log = getLogger()

    @PostMapping("/logout")
    fun logout(): ResponseEntity<String> {
        return ResponseEntity.ok()
            .header(
                "Set-Cookie",
                ResponseCookie.from(REFRESH_TOKEN_NAME, "")
                    .httpOnly(true)
                    .secure(true)
                    .path(REFRESH_TOKEN_PATH)
                    .maxAge(0)
                    .sameSite("None")
                    .build()
                    .toString(),
            )
            .build()
    }

    @PostMapping("/refresh_token")
    fun refreshToken(
        @CookieValue(REFRESH_TOKEN_NAME) refreshTokenFromRequest: String?
    ): ResponseEntity<String> {
        try {
            if (refreshTokenFromRequest.isNullOrEmpty())
                throw IllegalArgumentException("No refresh token in request")

            val refreshTokenFromDb = tokenService.verifyRefreshTokenClaims(refreshTokenFromRequest)
            val userIdStr = refreshTokenFromDb.userId.toString()
            log.info("issuing new refresh token to $userIdStr")

            // generate new access and refresh tokens
            val roles = userService.getCurrentUserRoles(refreshTokenFromDb.userId)
            val newAccessToken = tokenService.generateAccessToken(userIdStr, roles)

            val newRefreshToken = tokenService.generateRefreshToken(userIdStr)

            val newRefreshTokenCookie =
                ResponseCookie.from(REFRESH_TOKEN_NAME, newRefreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/auth/refresh_token")
                    .maxAge(COOKIE_EXPIRATION_IN_SECONDS.toLong()) // This is probably wrong too
                    .sameSite("Strict")
                    .build()

            return ResponseEntity.ok()
                .header("Set-Cookie", newRefreshTokenCookie.toString())
                .body(ObjectMapper().writeValueAsString(mapOf(ACCESS_TOKEN_NAME to newAccessToken)))
        } catch (ex: JwtException) {
            log.info("Invalid refresh token: ${ex.message}")
            return ResponseEntity.status(401).build()
        } catch (e: Exception) {
            log.info("Error while refreshing token: ${e.message}")
            return ResponseEntity.status(400).build()
        }
    }
}
