package iterative.harmony.backend.service

import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.repository.RefreshTokenRepository
import iterative.harmony.backend.util.RoleConstants
import java.util.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class JwtTokenServiceTest {

    private val secretKey = "some_really_really_long_secret_key"
    private val uuid = "f52b5cb8-a692-455a-8db2-ba416db5429b"
    private var token =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmNTJiNWNiOC1hNjkyLTQ1NWEtOGRiMi1iYTQxNmRiNTQyOWIiLCJyb2xlcyI6WyJVU0VSIl19.38HNklVyRsd9Nxddv4pByz8CYQkdB3lN6okKJvIueTA"
    private val mockRefreshTokenRepository = mock(RefreshTokenRepository::class.java)

    @InjectMocks
    private var jwtTokenService: JwtTokenService =
        JwtTokenService(secretKey, mockRefreshTokenRepository)

    @Nested
    @DisplayName("generateToken")
    inner class GenerateToken() {
        @Test
        fun `should generate a token`() {
            val token = jwtTokenService.generateAccessToken(uuid, listOf(RoleConstants.USER_ROLE))
            assertNotNull(token)
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    inner class GenerateRefreshToken() {
        @Test
        fun `should generate a refresh token`() {
            val mockRefreshToken = RefreshToken(UUID.fromString(uuid), 0, 0, UUID.fromString(uuid))
            whenever(mockRefreshTokenRepository.save(mockRefreshToken)).thenReturn(mockRefreshToken)

            val token = jwtTokenService.generateRefreshToken(uuid)
            assertNotNull(token)
        }
    }

    @Nested
    @Disabled("needs updated")
    @DisplayName("VerifyRefreshTokenClaims")
    inner class VerifyRefreshTokenClaims() {

        @Test
        fun `when given a valid token, should return the token`() {
            // TODO: generate a refresh token string without an exp date
            val refreshToken = RefreshToken(UUID.randomUUID(), 0, 0)
            // mock out token repo response
            whenever(mockRefreshTokenRepository.findByJti(refreshToken.jti!!))
                .thenReturn(Optional.of(refreshToken))
            try {
                //                jwtTokenService.verifyRefreshTokenClaims(refreshToken)
            } catch (e: Exception) {
                fail("Should not throw an exception")
            }
        }

        @Test
        fun `when given an invalid token, should return false`() {
            //            assertFalse(jwtTokenService.verifyRefreshTokenClaims("invalid-token"))
        }
    }

    @Nested
    @DisplayName("getAuthentication")
    inner class GetAuthentication() {
        @Test
        fun `should return an authentication object`() {
            val auth = jwtTokenService.getAuthentication(token)
            val authDetails = auth.details as Map<*, *>

            assertNotNull(auth)
            assertTrue(auth.isAuthenticated)
            assertEquals(uuid.toString(), authDetails["userId"])
        }
    }

    @Nested
    @DisplayName("getClaims")
    inner class GetClaims() {
        @Test
        fun `should return claims from token`() {
            val claims = jwtTokenService.getClaims(token)
            assertNotNull(claims)
            assertEquals(uuid.toString(), claims.subject.toString())
            assertEquals(listOf(RoleConstants.USER_ROLE), claims["roles"])
        }
    }
}
