package iterative.harmony.backend.service

import iterative.harmony.backend.exception.RefreshTokenNotInRequestException
import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.getLogger
import iterative.harmony.backend.util.setUserIdInLogs
import java.util.UUID
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

    fun throwIfNoRefreshToken(refreshToken: String?): String {
        if (refreshToken.isNullOrEmpty()) throw RefreshTokenNotInRequestException()
        return refreshToken
    }

    fun deleteRefreshToken(refreshToken: String, userAgentFingerprint: String) {
        val refreshTokenFromDb = tokenService.verifyRefreshToken(refreshToken, userAgentFingerprint)
        setUserIdInLogs(refreshTokenFromDb.userId)
        log.debug("Logging out")
        tokenService.deleteRefreshToken(refreshTokenFromDb)
    }

    /**
     * Rotates the refresh token and issues a new access token.
     *
     * @param refreshTokenFromRequest The string value of the refresh token from the request.
     * @return A pair containing the new access token and the new refresh token cookie respectively.
     */
    fun rotateTokens(
        refreshTokenFromRequest: String,
        userAgentFingerprint: String,
    ): Pair<String, String> {
        val refreshTokenFromDb =
            tokenService.verifyRefreshToken(refreshTokenFromRequest, userAgentFingerprint)
        val userId: UUID = refreshTokenFromDb.userId
        setUserIdInLogs(userId)

        tokenService.deleteRefreshToken(refreshTokenFromDb)
        tokenService.deleteExpiredRefreshTokensForUser(userId)
        tokenService.deleteExcessRefreshTokensForUser(userId)

        log.debug("Issuing new access and refresh tokens")
        val roles = userService.getCurrentUserRoles(userId)
        val newAccessToken =
            tokenService.generateAccessToken(userId.toString(), userAgentFingerprint, roles)
        val newRefreshToken =
            tokenService.generateRefreshToken(userId.toString(), userAgentFingerprint)
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
