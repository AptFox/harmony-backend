package iterative.harmony.backend.service

import iterative.harmony.backend.exception.RefreshTokenNotInRequestException
import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.FINGERPRINT_COOKIE_NAME
import iterative.harmony.backend.util.SecurityConstants.FINGERPRINT_COOKIE_PATH
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.getLogger
import iterative.harmony.backend.util.setDisplayNameInLogs
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
        return generateCookie(REFRESH_TOKEN_NAME, "", AUTH_PATH, 0)
    }

    fun generateFingerprintCookie(fingerprint: String): String {
        return generateCookie(
            FINGERPRINT_COOKIE_NAME,
            fingerprint,
            FINGERPRINT_COOKIE_PATH,
            COOKIE_EXPIRATION_IN_SECONDS,
        )
    }

    fun generateEmptyFingerprintCookie(): String {
        return generateCookie(FINGERPRINT_COOKIE_NAME, "", FINGERPRINT_COOKIE_PATH, 0)
    }

    fun throwIfNoRefreshToken(refreshToken: String?): String {
        if (refreshToken.isNullOrEmpty()) throw RefreshTokenNotInRequestException()
        return refreshToken
    }

    fun deleteRefreshToken(refreshToken: String, fingerprintHash: String) {
        val refreshTokenFromDb = tokenService.verifyRefreshToken(refreshToken, fingerprintHash)
        setUserIdInLogs(refreshTokenFromDb.userId)
        log.debug("Logging out")
        tokenService.deleteRefreshToken(refreshTokenFromDb)
    }

    /**
     * Rotates the refresh token and issues a new access token.
     *
     * @param refreshTokenFromRequest The string value of the refresh token from the request.
     * @param fingerprintHash The hash of the fingerprint cookie from the request.
     * @return A pair containing the new access token and the new refresh token cookie respectively.
     */
    fun rotateTokens(
        refreshTokenFromRequest: String,
        fingerprintHash: String,
    ): Pair<String, String> {
        val refreshTokenFromDb =
            tokenService.verifyRefreshToken(refreshTokenFromRequest, fingerprintHash)
        val userId: UUID = refreshTokenFromDb.userId
        setUserIdInLogs(userId)

        tokenService.deleteRefreshToken(refreshTokenFromDb)
        tokenService.deleteExpiredRefreshTokensForUser(userId)
        tokenService.deleteExcessRefreshTokensForUser(userId)

        val user = userService.getUserById(userId)
        setDisplayNameInLogs(user.displayName)

        log.debug("Issuing new access and refresh tokens")
        val roles = user.roles.map { it.name }
        val newAccessToken =
            tokenService.generateAccessToken(
                userId.toString(),
                fingerprintHash,
                roles,
                user.displayName,
            )
        val newRefreshToken = tokenService.generateRefreshToken(userId.toString(), fingerprintHash)
        val newRefreshTokenCookie =
            generateCookie(
                REFRESH_TOKEN_NAME,
                newRefreshToken,
                AUTH_PATH,
                COOKIE_EXPIRATION_IN_SECONDS,
            )

        return Pair(newAccessToken, newRefreshTokenCookie)
    }

    private fun generateCookie(
        name: String,
        value: String,
        path: String,
        cookieExpirationInSeconds: Int,
    ): String {
        val isDevEnv = environment.activeProfiles.contains("dev")
        val secureCookie = !isDevEnv
        val sameSite = if (isDevEnv) "Lax" else "None"
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(secureCookie)
            .path(path)
            .maxAge(cookieExpirationInSeconds.toLong())
            .sameSite(sameSite)
            .build()
            .toString()
    }
}
