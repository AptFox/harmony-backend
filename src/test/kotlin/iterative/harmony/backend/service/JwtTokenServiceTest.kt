package iterative.harmony.backend.service

import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.repository.RefreshTokenRepository
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.Utils
import java.util.*
import kotlin.test.fail
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class JwtTokenServiceTest {

    @Mock
    private var mockRefreshTokenRepository: RefreshTokenRepository =
        mock(RefreshTokenRepository::class.java)

    private val secretKey: String = "some_really_really_long_secret_key"
    private val tokenParser: JwtParser =
        Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secretKey.toByteArray())).build()

    @InjectMocks private var jwtTokenService: JwtTokenService = JwtTokenService(secretKey)

    private val testUuid = "f52b5cb8-a692-455a-8db2-ba416db5429b"
    // Lacks a JTI and should throw an exception when parsed as a refresh token
    private var mockToken =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmNTJiNWNiOC1hNjkyLTQ1NWEtOGRiMi1iYTQxNmRiNTQyOWIiLCJyb2xlcyI6WyJVU0VSIl19.38HNklVyRsd9Nxddv4pByz8CYQkdB3lN6okKJvIueTA"
    private val userRoles = listOf(RoleConstants.USER_ROLE)
    private val issuedAt = Utils().getCurrentTimeInMillisRounded()
    private val expiration = issuedAt + JwtTokenService.ACCESS_TOKEN_DURATION_IN_MILLIS

    private fun buildValidRefreshToken(): String {
        val claims =
            mapOf("sub" to testUuid, "jti" to testUuid, "iat" to issuedAt, "exp" to expiration)
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(issuedAt))
            .setExpiration(Date(expiration))
            .signWith(Keys.hmacShaKeyFor(secretKey.toByteArray()))
            .compact()
    }

    @Nested
    @DisplayName("generateAccessToken")
    inner class GenerateAccessToken() {

        @Test
        fun `should generate a parseable access token`() {
            try {
                val accessToken = jwtTokenService.generateAccessToken(testUuid, userRoles)
                tokenParser.parseClaimsJws(accessToken)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    @Nested
    @DisplayName("generateRefreshToken")
    inner class GenerateRefreshToken() {
        @Test
        fun `should generate a parseable refresh token`() {
            val staticIssuedTime = Utils().getCurrentTimeInMillisRounded()
            val staticExpirationTime =
                staticIssuedTime + JwtTokenService.REFRESH_TOKEN_DURATION_IN_MILLIS

            val userId = UUID.fromString(testUuid)
            val mockRefreshToken = RefreshToken(userId, issuedAt, expiration, userId)
            whenever(mockRefreshTokenRepository.save(any())).thenReturn(mockRefreshToken)

            try {
                val token = jwtTokenService.generateRefreshToken(testUuid)
                val claims = tokenParser.parseClaimsJws(token).body
                assertEquals(userId.toString(), claims.subject)
                assertEquals(mockRefreshToken.jti.toString(), claims["jti"])
                assertEquals(staticIssuedTime, claims.issuedAt.time)
                assertEquals(staticExpirationTime, claims.expiration.time)
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    @Nested
    @DisplayName("verifyRefreshToken")
    inner class VerifyRefreshTokenClaims() {

        @Test
        fun `when given a valid token, should return the token`() {
            val uuid = UUID.fromString(testUuid)
            val jti = UUID.fromString(testUuid)
            val refreshToken = RefreshToken(uuid, issuedAt, expiration, jti)
            val validToken = buildValidRefreshToken()

            whenever(mockRefreshTokenRepository.findByJti(refreshToken.jti!!))
                .thenReturn(Optional.of(refreshToken))
            try {
                val tokenFromDb = jwtTokenService.verifyRefreshToken(validToken)
                assertEquals(refreshToken, tokenFromDb)
            } catch (e: Exception) {
                fail(e)
            }
        }

        @Test
        fun `when given an invalid token, should return false`() {
            try {
                jwtTokenService.verifyRefreshToken(mockToken)
                fail("Exception should have been thrown")
            } catch (_: Exception) {}
        }
    }

    @Nested
    @DisplayName("getAuthentication")
    inner class GetAuthentication() {
        @Test
        fun `should return an authentication object`() {
            val auth = jwtTokenService.getAuthentication(mockToken)
            val authDetails = auth.details as Map<*, *>

            assertNotNull(auth)
            assertTrue(auth.isAuthenticated)
            assertEquals(testUuid, authDetails["userId"])
        }
    }

    @Nested
    @DisplayName("getClaims")
    inner class GetClaims() {
        @Test
        fun `should return claims from token`() {
            val claims = jwtTokenService.getClaims(mockToken)
            assertNotNull(claims)
            assertEquals(testUuid, claims.subject)
            assertEquals(userRoles, claims["roles"])
        }
    }
}
