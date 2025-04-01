package iterative.harmony.backend.service

import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import iterative.harmony.backend.util.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service

@Service
class AuthService {
    @Autowired private lateinit var tokenService: JwtTokenService
    @Autowired private lateinit var userService: UserService

    private val log = getLogger()

    fun generateEmptyRefreshTokenCookie(): String {
        return generateRefreshTokenCookie("", 0)
    }

    fun rotateRefreshToken(refreshTokenFromRequest: String?): Map<String, String> {
        if (refreshTokenFromRequest.isNullOrEmpty())
            throw IllegalArgumentException("No refresh token in request")

        val refreshTokenFromDb = tokenService.verifyRefreshToken(refreshTokenFromRequest)
        val userIdStr = refreshTokenFromDb.userId.toString()
        log.info("issuing new refresh token to $userIdStr")
        val roles = userService.getCurrentUserRoles(refreshTokenFromDb.userId)
        val newAccessToken = tokenService.generateAccessToken(userIdStr, roles)
        val newRefreshToken = tokenService.generateRefreshToken(userIdStr)
        val newRefreshTokenCookie =
            generateRefreshTokenCookie(newRefreshToken, COOKIE_EXPIRATION_IN_SECONDS)

        return mapOf(
            "newAccessToken" to newAccessToken,
            "newRefreshTokenCookie" to newRefreshTokenCookie,
        )
    }

    private fun generateRefreshTokenCookie(
        refreshToken: String,
        cookieExpirationInSeconds: Int,
    ): String {
        return ResponseCookie.from(REFRESH_TOKEN_NAME, refreshToken)
            .httpOnly(true)
            .secure(true)
            .path(REFRESH_TOKEN_PATH)
            .maxAge(cookieExpirationInSeconds.toLong())
            .sameSite("None")
            .build()
            .toString()
    }
}
