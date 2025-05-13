package iterative.harmony.backend.service

import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service

@Service
class AuthService {
    @Autowired private lateinit var tokenService: JwtTokenService
    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var environment: Environment

    private val log = getLogger()

    fun generateEmptyRefreshTokenCookie(): String {
        return generateRefreshTokenCookie("", 0)
    }

    /**
     * Rotates the refresh token and issues a new access token.
     *
     * @param refreshTokenFromRequest The string value of the refresh token from the request.
     * @return A pair containing the new access token and the new refresh token cookie respectively.
     */
    fun rotateTokens(
        refreshTokenFromRequest: String?,
        userAgentFingerprint: String,
        initialLogin: Boolean,
    ): Pair<String, String> {
        if (refreshTokenFromRequest.isNullOrEmpty())
            throw IllegalArgumentException("No refresh token in request")

        val refreshTokenFromDb =
            tokenService.verifyRefreshToken(refreshTokenFromRequest, userAgentFingerprint)
        val userIdStr = refreshTokenFromDb.userId.toString()
        log.info("issuing new tokens to $userIdStr, initialLogin: $initialLogin")
        val roles = userService.getCurrentUserRoles(refreshTokenFromDb.userId)
        val newAccessToken =
            tokenService.generateAccessToken(userIdStr, userAgentFingerprint, roles)
        val newRefreshToken = tokenService.generateRefreshToken(userIdStr, userAgentFingerprint)
        val newRefreshTokenCookie =
            generateRefreshTokenCookie(newRefreshToken, COOKIE_EXPIRATION_IN_SECONDS)

        return Pair(newAccessToken, newRefreshTokenCookie)
    }

    private fun generateRefreshTokenCookie(
        refreshToken: String,
        cookieExpirationInSeconds: Int,
    ): String {
        val isDevEnv = environment.activeProfiles.contains("dev")
        val secureCookie = !isDevEnv
        val sameSite = if (isDevEnv) "Lax" else "None"
        return ResponseCookie.from(REFRESH_TOKEN_NAME, refreshToken)
            .httpOnly(true)
            .secure(secureCookie)
            .path(AUTH_PATH)
            .maxAge(cookieExpirationInSeconds.toLong())
            .sameSite(sameSite)
            .build()
            .toString()
    }
}
