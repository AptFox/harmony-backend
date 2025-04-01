package iterative.harmony.backend.service

import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {

    @Mock private lateinit var userService: UserService
    @Mock private lateinit var tokenService: JwtTokenService
    @InjectMocks private lateinit var authService: AuthService

    @Test
    fun `setEmptyCookie should return an empty, expired cookie`() {
        val expected =
            "${REFRESH_TOKEN_NAME}=; Path=${REFRESH_TOKEN_PATH}; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure; HttpOnly; SameSite=None"
        val actual = authService.setEmptyCookie()

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
                    { authService.rotateRefreshToken(null) },
                    "Expected IllegalArgumentException to be thrown",
                )
            assertEquals("No refresh token in request", exception.message)
        }

        // TODO: add tests for remaining possible exceptions

        @Test
        fun `should return new access and refresh tokens`() {
            throw NotImplementedError("Test not implemented yet")
            // Mock the behavior of the tokenService and userService as needed
            // Call the rotateRefreshToken method and verify the results
        }
    }

    fun rotateRefreshToken() {}
}
