package iterative.harmony.backend.service

import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.util.SecurityConstants.COOKIE_EXPIRATION_IN_SECONDS
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import java.text.SimpleDateFormat
import java.util.*
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
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock private lateinit var userService: UserService
    @Mock private lateinit var tokenService: JwtTokenService
    @InjectMocks private lateinit var authService: AuthService

    @Test
    fun `generateEmptyRefreshTokenCookie should return an empty, expired cookie`() {
        val expected =
            "${REFRESH_TOKEN_NAME}=; Path=${REFRESH_TOKEN_PATH}; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=None"
        val actual = authService.generateEmptyRefreshTokenCookie()

        assertEquals(expected, actual)
    }

    @Nested
    @DisplayName("rotateRefreshToken")
    inner class RotateRefreshToken {
        @Test
        fun `should throw IllegalArgumentException if refresh token is null or empty`() {
            val exception =
                assertThrows(
                    IllegalArgumentException::class.java,
                    { authService.rotateTokens(null) },
                    "Expected IllegalArgumentException to be thrown",
                )
            assertEquals("No refresh token in request", exception.message)
        }

        @Test
        fun `should return new access and refresh tokens`() {
            val userId = UUID.randomUUID()
            val userRoles = listOf("ROLE_USER")
            val refreshTokenFromRequest = "validRefreshToken"
            val refreshTokenMock =
                mock(RefreshToken::class.java).apply { whenever(this.userId).thenReturn(userId) }

            whenever(tokenService.verifyRefreshToken(refreshTokenFromRequest))
                .thenReturn(refreshTokenMock)
            whenever(userService.getCurrentUserRoles(userId)).thenReturn(userRoles)
            whenever(tokenService.generateAccessToken(userId.toString(), userRoles))
                .thenReturn("newAccessToken")
            whenever(tokenService.generateRefreshToken(userId.toString()))
                .thenReturn("newRefreshToken")

            val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
            dateFormatter.timeZone = TimeZone.getTimeZone("GMT")
            val expirationString =
                dateFormatter.format(
                    Date(Date().time + COOKIE_EXPIRATION_IN_SECONDS.toLong() * 1000)
                )

            val expected =
                Pair(
                    "newAccessToken",
                    "${REFRESH_TOKEN_NAME}=newRefreshToken; Path=${REFRESH_TOKEN_PATH}; Max-Age=${COOKIE_EXPIRATION_IN_SECONDS.toLong()}; Expires=${expirationString}; Secure; HttpOnly; SameSite=None",
                )
            val actual = authService.rotateTokens(refreshTokenFromRequest)

            assertEquals(expected, actual)
        }
    }
}
