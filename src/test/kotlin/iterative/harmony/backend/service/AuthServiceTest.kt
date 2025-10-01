package iterative.harmony.backend.service

import iterative.harmony.backend.exception.RefreshTokenNotInRequestException
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.util.SecurityConstants.AUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import java.text.SimpleDateFormat
import java.util.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
    fun `setUserIdInLogs should set userId in MDC`() {
        val userId = UUID.randomUUID()
        authService.setUserIdInLogs(userId)
        assertEquals(userId.toString(), MDC.get("userId"))
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
        val userAgentFingerprint = "userAgent"
        val userId = UUID.randomUUID()
        val refreshTokenMock =
            mock(RefreshToken::class.java).apply { whenever(this.userId).thenReturn(userId) }

        whenever(tokenService.verifyRefreshToken(refreshToken, userAgentFingerprint))
            .thenReturn(refreshTokenMock)

        authService.deleteRefreshToken(refreshToken, userAgentFingerprint)

        verify(tokenService).verifyRefreshToken(refreshToken, userAgentFingerprint)
        verify(tokenService).deleteRefreshToken(refreshTokenMock)
        assertEquals(userId.toString(), MDC.get("userId"))
    }

    @Nested
    @DisplayName("rotateRefreshToken")
    inner class RotateRefreshToken {

        @Test
        fun `should return new access and refresh tokens`() {
            val userId = UUID.randomUUID()
            val userRoles = listOf("ROLE_USER")
            val refreshTokenFromRequest = "validRefreshToken"
            val userAgent = "SomeUserAgent|127"
            val refreshTokenMock =
                mock(RefreshToken::class.java).apply { whenever(this.userId).thenReturn(userId) }

            whenever(tokenService.verifyRefreshToken(refreshTokenFromRequest, userAgent))
                .thenReturn(refreshTokenMock)
            whenever(userService.getCurrentUserRoles(userId)).thenReturn(userRoles)
            whenever(
                    tokenService.generateAccessToken(
                        eq(userId.toString()),
                        eq(userAgent),
                        eq(userRoles),
                        any(),
                    )
                )
                .thenReturn("newAccessToken")
            whenever(tokenService.generateRefreshToken(eq(userId.toString()), eq(userAgent), any()))
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
            val actual = authService.rotateTokens(refreshTokenFromRequest, userAgent)

            assertEquals(expected, actual)
        }
    }
}
