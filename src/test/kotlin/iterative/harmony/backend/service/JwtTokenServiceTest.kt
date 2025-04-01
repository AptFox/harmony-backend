package iterative.harmony.backend.service

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParser
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.repository.RefreshTokenRepository
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.Utils
import java.util.*
import kotlin.test.assertContains
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

    private val testUuid = UUID.randomUUID().toString()
    private val userRoles = listOf(RoleConstants.USER_ROLE)
    private val issuedAt = Utils().getCurrentTimeInMillisRounded()
    private val expiration = issuedAt + JwtTokenService.ACCESS_TOKEN_DURATION_IN_MILLIS

    private fun buildToken(
        sub: String? = testUuid,
        jti: String? = testUuid,
        roles: List<String>? = userRoles,
        iat: Long? = issuedAt,
        exp: Long? = expiration,
    ): String {
        val issuedAt = if (iat != null) Date(iat) else null
        val expiredAt = if (exp != null) Date(exp) else null
        val claims = mapOf("sub" to sub, "jti" to jti, "roles" to roles, "iat" to iat, "exp" to exp)
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(issuedAt)
            .setExpiration(expiredAt)
            .signWith(Keys.hmacShaKeyFor(secretKey.toByteArray()))
            .compact()
    }

    @Nested
    @DisplayName("generateAccessToken")
    inner class GenerateAccessToken() {

        @Test
        fun `should generate a parseable access token`() {
            assertDoesNotThrow(
                {
                    val accessToken = jwtTokenService.generateAccessToken(testUuid, userRoles)
                    tokenParser.parseClaimsJws(accessToken)
                },
                "should not throw an exception",
            )
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

            assertDoesNotThrow(
                {
                    val token = jwtTokenService.generateRefreshToken(testUuid)
                    val claims = tokenParser.parseClaimsJws(token).body
                    assertEquals(userId.toString(), claims.subject)
                    assertEquals(mockRefreshToken.jti.toString(), claims["jti"])
                    assertEquals(staticIssuedTime, claims.issuedAt.time)
                    assertEquals(staticExpirationTime, claims.expiration.time)
                },
                "should not throw an exception",
            )
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
            val validToken = buildToken()

            whenever(mockRefreshTokenRepository.findByJti(refreshToken.jti!!))
                .thenReturn(Optional.of(refreshToken))
            assertDoesNotThrow(
                {
                    val tokenFromDb = jwtTokenService.verifyRefreshToken(validToken)
                    assertEquals(refreshToken, tokenFromDb)
                },
                "should not throw an exception",
            )
        }

        @Test
        fun `when given a token missing jti, should throw an exception`() {
            val invalidToken = buildToken(jti = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.verifyRefreshToken(invalidToken) },
                    "should have thrown an exception",
                )
            assertEquals("JTI is missing from Refresh token", exception.message)
        }
    }

    @Nested
    @DisplayName("getAuthentication")
    inner class GetAuthentication() {
        @Test
        fun `when token is valid, should return an authentication object`() {
            val validToken = buildToken()
            val auth = jwtTokenService.getAuthentication(validToken)
            val authDetails = auth.details as Map<*, *>

            assertTrue(auth.isAuthenticated)
            assertEquals(testUuid, authDetails["userId"])
            assertEquals(auth.authorities.toString(), userRoles.toString())
        }

        @Test
        fun `should throw an exception if userRoles are missing`() {
            val invalidToken = buildToken(roles = null)
            assertThrows(
                NullPointerException::class.java,
                { jwtTokenService.getAuthentication(invalidToken) },
                "should have thrown an exception",
            )
        }

        @Test
        fun `should throw an exception when token is missing iat or exp`() {
            val invalidToken = buildToken(iat = null, exp = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.getAuthentication(invalidToken) },
                    "should have thrown an exception",
                )
            assertEquals("Token is missing iat and/or exp", exception.message)
        }
    }

    @Nested
    @DisplayName("getClaims")
    inner class GetClaims() {
        @Test
        fun `should return claims from token`() {
            val validToken = buildToken()
            val claims = jwtTokenService.getClaims(validToken)
            assertNotNull(claims)
            assertEquals(testUuid, claims.subject)
            assertEquals(userRoles, claims["roles"])
        }

        @Test
        fun `should throw an exception when token is null or empty`() {
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.getClaims(null) },
                    "should have thrown an exception",
                )
            assertEquals("Token is null", exception.message)
        }

        @Test
        fun `should throw an exception when token is expired`() {
            val expiredToken = buildToken(exp = issuedAt - 1000)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.getClaims(expiredToken) },
                    "should have thrown an exception",
                )
            assertContains(exception.message.toString(), "JWT expired at")
        }

        @Test
        fun `should throw an exception when token is missing iat or exp`() {
            val invalidToken = buildToken(iat = null, exp = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.getClaims(invalidToken) },
                    "should have thrown an exception",
                )
            assertEquals("Token is missing iat and/or exp", exception.message)
        }
    }
}
