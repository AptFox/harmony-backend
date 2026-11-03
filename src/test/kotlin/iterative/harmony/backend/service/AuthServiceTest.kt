package iterative.harmony.backend.service

import iterative.harmony.backend.exception.RefreshTokenNotInRequestException
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.model.Role
import iterative.harmony.backend.model.User
import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.FINGERPRINT_COOKIE_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import java.text.SimpleDateFormat
import java.util.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.core.env.Environment

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock private lateinit var userService: UserService
    @Mock private lateinit var tokenService: JwtTokenService
    @Mock private lateinit var environment: Environment
    @InjectMocks private lateinit var authService: AuthService

    @AfterEach
    fun clearMDC() {
        MDC.clear()
    }

    @Test
    fun `generateEmptyRefreshTokenCookie should return an empty, expired cookie`() {
        val expected =
            "${REFRESH_TOKEN_NAME}=; Path=${AUTH_PATH}; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=None"
        whenever(environment.activeProfiles).thenReturn(arrayOf("test"))
        val actual = authService.generateEmptyRefreshTokenCookie()

        assertEquals(expected, actual)
    }

    @Test
    fun `generateFingerprintCookie should return an HttpOnly cookie sent on every path`() {
        whenever(environment.activeProfiles).thenReturn(arrayOf("test"))
        val actual = authService.generateFingerprintCookie("someFingerprint")

        assertTrue(actual.startsWith("${FINGERPRINT_COOKIE_NAME}=someFingerprint; Path=/; "))
        assertTrue(actual.contains("Max-Age=${COOKIE_EXPIRATION_IN_SECONDS}"))
        assertTrue(actual.endsWith("Secure; HttpOnly; SameSite=None"))
    }

    @Test
    fun `generateEmptyFingerprintCookie should return an empty, expired cookie`() {
        val expected =
            "${FINGERPRINT_COOKIE_NAME}=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=None"
        whenever(environment.activeProfiles).thenReturn(arrayOf("test"))
        val actual = authService.generateEmptyFingerprintCookie()

        assertEquals(expected, actual)
    }

    @Test
    fun `throwIfNoRefreshToken should throw if refreshToken is null or empty`() {
        assertThrows(RefreshTokenNotInRequestException::class.java) {
            authService.throwIfNoRefreshToken(null)
        }
        assertThrows(RefreshTokenNotInRequestException::class.java) {
            authService.throwIfNoRefreshToken("")
        }
    }

    @Test
    fun `throwIfNoRefreshToken should return token if present`() {
        val token = "token"
        assertEquals(token, authService.throwIfNoRefreshToken(token))
    }

    @Test
    fun `deleteRefreshToken should verify and delete refresh token`() {
        val refreshToken = "refreshToken"
        val fingerprintHash = "someFingerprintHash"
        val userId = UUID.randomUUID()
        val refreshTokenMock =
            mock(RefreshToken::class.java).apply { whenever(this.userId).thenReturn(userId) }

        whenever(tokenService.verifyRefreshToken(refreshToken, fingerprintHash))
            .thenReturn(refreshTokenMock)

        authService.deleteRefreshToken(refreshToken, fingerprintHash)

        verify(tokenService).verifyRefreshToken(refreshToken, fingerprintHash)
        verify(tokenService).deleteRefreshToken(refreshTokenMock)
        assertEquals(userId.toString().take(8), MDC.get("userId"))
    }

    @Nested
    @DisplayName("rotateRefreshToken")
    inner class RotateRefreshToken {

        @Test
        fun `should return new access and refresh tokens`() {
            val userId = UUID.randomUUID()
            val userRole = Role("ROLE_USER", "The default role for a user")
            val userRoles = listOf(userRole.name)
            val displayName = "someDisplayName"
            val user = User(userId, "username", displayName, true, "1", null, null, setOf(userRole))
            val refreshTokenFromRequest = "validRefreshToken"
            val fingerprintHash = "someFingerprintHash"
            val refreshTokenMock =
                mock(RefreshToken::class.java).apply { whenever(this.userId).thenReturn(userId) }

            whenever(tokenService.verifyRefreshToken(refreshTokenFromRequest, fingerprintHash))
                .thenReturn(refreshTokenMock)
            whenever(userService.getUserById(userId)).thenReturn(user)
            whenever(
                    tokenService.generateAccessToken(
                        eq(userId.toString()),
                        eq(fingerprintHash),
                        eq(userRoles),
                        eq(displayName),
                        any(),
                    )
                )
                .thenReturn("newAccessToken")
            whenever(
                    tokenService.generateRefreshToken(
                        eq(userId.toString()),
                        eq(fingerprintHash),
                        any(),
                    )
                )
                .thenReturn("newRefreshToken")
            whenever(environment.activeProfiles).thenReturn(arrayOf("test"))

            val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("GMT")
            val expirationString =
                dateFormatter.format(
                    Date(Date().time + COOKIE_EXPIRATION_IN_SECONDS.toLong() * 1000)
                )

            val expected =
                Pair(
                    "newAccessToken",
                    "${REFRESH_TOKEN_NAME}=newRefreshToken; Path=${AUTH_PATH}; Max-Age=${COOKIE_EXPIRATION_IN_SECONDS.toLong()}; Expires=${expirationString}; Secure; HttpOnly; SameSite=None",
                )
            val actual = authService.rotateTokens(refreshTokenFromRequest, fingerprintHash)

            assertEquals(expected, actual)
            assertEquals(displayName, MDC.get("displayName"))
        }
    }
}
