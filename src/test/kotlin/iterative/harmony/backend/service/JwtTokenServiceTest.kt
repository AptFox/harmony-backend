package iterative.harmony.backend.service

import iterative.harmony.backend.util.RoleConstants
import java.util.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class JwtTokenServiceTest {

    private val secretKey = "some_really_really_long_secret_key"
    private val uuid = UUID.fromString("f52b5cb8-a692-455a-8db2-ba416db5429b")
    private var token =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmNTJiNWNiOC1hNjkyLTQ1NWEtOGRiMi1iYTQxNmRiNTQyOWIiLCJyb2xlcyI6WyJVU0VSIl19.38HNklVyRsd9Nxddv4pByz8CYQkdB3lN6okKJvIueTA"

    @InjectMocks private var jwtTokenService: JwtTokenService = JwtTokenService(secretKey)

    @Nested
    @DisplayName("generateToken")
    inner class GenerateToken() {
        @Test
        fun `should generate a token`() {
            val token = jwtTokenService.generateToken(uuid, listOf(RoleConstants.USER_ROLE))
            assertNotNull(token)
        }
    }

    @Nested
    @DisplayName("validateToken")
    inner class ValidateToken() {

        @Test
        fun `when given a valid token, should return true`() {
            assertTrue(jwtTokenService.validateToken(token))
        }

        @Test
        fun `when given an invalid token, should return false`() {
            assertFalse(jwtTokenService.validateToken("invalid-token"))
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
