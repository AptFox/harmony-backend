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
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any as kotlinAny
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.dao.OptimisticLockingFailureException

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
    private val userAgent = "SomeUserAgent|127"
    private val userAgentFingerprint = Utils().generateFingerprint(userAgent)

    fun buildRefreshToken(
        userId: UUID? = UUID.randomUUID(),
        fingerprint: String? = userAgentFingerprint,
        iat: Long? = issuedAt,
        expiresAt: Long? = expiration,
        jti: UUID? = UUID.randomUUID(),
    ): RefreshToken {
        return RefreshToken(userId!!, fingerprint, iat!!, expiresAt!!, jti!!)
    }

    fun buildTokenString(
        sub: String? = testUuid,
        jti: String? = testUuid,
        fp: String? = userAgentFingerprint,
        roles: List<String>? = userRoles,
        iat: Long? = issuedAt,
        exp: Long? = expiration,
    ): String {
        val issuedAt = if (iat != null) Date(iat) else null
        val expiredAt = if (exp != null) Date(exp) else null
        val claims =
            mapOf(
                "sub" to sub,
                "jti" to jti,
                "fp" to fp,
                "roles" to roles,
                "iat" to iat,
                "exp" to exp,
            )
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
            val utils = mock<Utils>()
            val staticIssuedTime = Utils().getCurrentTimeInMillisRounded()
            val staticExpirationTime =
                staticIssuedTime + JwtTokenService.ACCESS_TOKEN_DURATION_IN_MILLIS
            whenever(utils.getCurrentTimeInMillisRounded()).thenReturn(staticIssuedTime)
            assertDoesNotThrow(
                {
                    val accessToken =
                        jwtTokenService.generateAccessToken(
                            testUuid,
                            userAgentFingerprint,
                            userRoles,
                            utils,
                        )
                    val claims = tokenParser.parseClaimsJws(accessToken).body
                    assertEquals(testUuid, claims.subject)
                    assertEquals(userAgentFingerprint, claims["fp"])
                    assertEquals(userRoles, claims["roles"])
                    assertEquals(staticIssuedTime, claims.issuedAt.time)
                    assertEquals(staticExpirationTime, claims.expiration.time)
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
            val mockRefreshToken =
                RefreshToken(userId, userAgentFingerprint, issuedAt, expiration, userId)
            whenever(mockRefreshTokenRepository.save(any())).thenReturn(mockRefreshToken)
            val utils = mock<Utils>()
            whenever(utils.getCurrentTimeInMillisRounded()).thenReturn(staticIssuedTime)

            assertDoesNotThrow(
                {
                    val token =
                        jwtTokenService.generateRefreshToken(testUuid, userAgentFingerprint, utils)
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
    @DisplayName("deleteRefreshToken")
    inner class DeleteRefreshToken() {
        @Test
        fun `should call repository delete`() {
            val refreshToken = buildRefreshToken()
            assertDoesNotThrow { jwtTokenService.deleteRefreshToken(refreshToken) }
            verify(mockRefreshTokenRepository).delete(refreshToken)
        }

        @Test
        fun `should throw when supplied token is not in DB`() {
            val refreshToken = buildRefreshToken()
            doThrow(OptimisticLockingFailureException("fail"))
                .`when`(mockRefreshTokenRepository)
                .delete(refreshToken)
            val ex =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.deleteRefreshToken(refreshToken) },
                )
            assertEquals("Refresh token not in DB.", ex.message)
        }
    }

    @Nested
    @DisplayName("deleteExpiredRefreshTokensForUser")
    inner class DeleteExpiredRefreshTokensForUser() {
        @Test
        fun `should delete expired tokens`() {
            val expiredToken = buildRefreshToken(expiresAt = issuedAt - 1000)
            whenever(
                    mockRefreshTokenRepository.findAllByUserIdAndCreatedAtBefore(
                        eq(expiredToken.userId),
                        kotlinAny(),
                    )
                )
                .thenReturn(listOf(expiredToken))
            assertDoesNotThrow {
                jwtTokenService.deleteExpiredRefreshTokensForUser(expiredToken.userId)
            }
            verify(mockRefreshTokenRepository).deleteAll(listOf(expiredToken))
        }

        @Test
        fun `should not delete if no expired tokens`() {
            val uuid = UUID.fromString(testUuid)
            whenever(
                    mockRefreshTokenRepository.findAllByUserIdAndCreatedAtBefore(
                        eq(uuid),
                        kotlinAny(),
                    )
                )
                .thenReturn(emptyList())
            assertDoesNotThrow { jwtTokenService.deleteExpiredRefreshTokensForUser(uuid) }
            verify(mockRefreshTokenRepository, never()).deleteAll(kotlinAny())
        }
    }

    @Nested
    @DisplayName("deleteExcessRefreshTokensForUser")
    inner class DeleteExcessRefreshTokensForUser() {
        @Test
        fun `should delete excess tokens`() {
            val uuid = UUID.fromString(testUuid)
            whenever(mockRefreshTokenRepository.countByUserId(uuid)).thenReturn(5)
            val excessTokens =
                listOf(
                    buildRefreshToken(userId = uuid),
                    buildRefreshToken(userId = uuid, iat = issuedAt + 1, expiresAt = expiration + 1),
                )
            whenever(
                    mockRefreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(
                        eq(uuid),
                        kotlinAny(),
                    )
                )
                .thenReturn(excessTokens)
            assertDoesNotThrow { jwtTokenService.deleteExcessRefreshTokensForUser(uuid) }
            verify(mockRefreshTokenRepository).deleteAll(excessTokens)
        }

        @Test
        fun `should not delete if 2 or fewer tokens`() {
            val uuid = UUID.fromString(testUuid)
            whenever(mockRefreshTokenRepository.countByUserId(uuid)).thenReturn(2)
            assertDoesNotThrow { jwtTokenService.deleteExcessRefreshTokensForUser(uuid) }
            verify(mockRefreshTokenRepository, never()).deleteAll(kotlinAny())
        }
    }

    @Nested
    @DisplayName("throwOnRefreshTokenMismatch")
    inner class ThrowOnRefreshTokenMismatch() {
        @Test
        fun `should not throw when tokens match`() {
            val token = buildRefreshToken()
            assertDoesNotThrow { jwtTokenService.throwOnRefreshTokenMismatch(token, token) }
        }

        @Test
        fun `should throw when tokens mismatch`() {
            val uuid = UUID.fromString(testUuid)
            val tokenDb = buildRefreshToken(userId = uuid, jti = uuid)
            val tokenClaims = buildRefreshToken(userId = uuid, iat = issuedAt + 1, jti = uuid)
            val ex =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.throwOnRefreshTokenMismatch(tokenDb, tokenClaims) },
                )
            assertEquals(
                "The supplied refresh token does not match the DB. Mismatched fields: [issuedAt]",
                ex.message,
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
            val refreshToken = RefreshToken(uuid, userAgentFingerprint, issuedAt, expiration, jti)
            val validToken = buildTokenString()

            whenever(mockRefreshTokenRepository.findByJti(refreshToken.jti!!))
                .thenReturn(Optional.of(refreshToken))
            assertDoesNotThrow(
                {
                    val tokenFromDb =
                        jwtTokenService.verifyRefreshToken(validToken, userAgentFingerprint)
                    assertEquals(refreshToken, tokenFromDb)
                },
                "should not throw an exception",
            )
        }

        @Test
        fun `when given a token missing jti, should throw an exception`() {
            val invalidToken = buildTokenString(jti = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.verifyRefreshToken(invalidToken, userAgentFingerprint) },
                    "should have thrown an exception",
                )
            assertEquals("JTI is missing from Refresh token", exception.message)
        }

        @Test
        fun `when token is expired, should throw an exception`() {
            val validToken = buildTokenString()
            val refreshToken =
                RefreshToken(
                    UUID.fromString(testUuid),
                    userAgentFingerprint,
                    issuedAt,
                    issuedAt - 1000,
                    UUID.fromString(testUuid),
                )
            whenever(mockRefreshTokenRepository.findByJti(refreshToken.jti!!))
                .thenReturn(Optional.of(refreshToken))
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.verifyRefreshToken(validToken, userAgentFingerprint) },
                    "should have thrown an exception",
                )
            assertEquals(
                "Unexpected refresh token verification error: The supplied refresh token is expired",
                exception.message,
            )
        }

        @Test
        fun `when token is missing from DB, should throw an exception`() {
            val validToken = buildTokenString()
            whenever(mockRefreshTokenRepository.findByJti(UUID.fromString(testUuid)))
                .thenReturn(Optional.empty())
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.verifyRefreshToken(validToken, userAgentFingerprint) },
                    "should have thrown an exception",
                )
            assertEquals("Refresh token not in DB.", exception.message)
        }

        @Test
        fun `when token fingerprint and request fingerprint dont match, should throw an exception`() {
            val invalidToken = buildTokenString(fp = "testFingerprint")
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.verifyRefreshToken(invalidToken, userAgentFingerprint) },
                    "should have thrown an exception",
                )
            assertEquals(
                "Unexpected refresh token verification error: Request fingerprint and token fingerprint do not match",
                exception.message,
            )
        }
    }

    @Nested
    @DisplayName("getAuthentication")
    inner class GetAuthentication() {
        @Test
        fun `when token is valid, should return an authentication object`() {
            val validToken = buildTokenString()
            val auth =
                jwtTokenService.getAuthenticationFromAccessToken(validToken, userAgentFingerprint)
            val authDetails = auth.details as Map<*, *>

            assertTrue(auth.isAuthenticated)
            assertEquals(testUuid, authDetails["userId"])
            assertEquals(auth.authorities.toString(), userRoles.toString())
        }

        @Test
        fun `should throw an exception if userRoles are missing`() {
            val invalidToken = buildTokenString(roles = null)
            assertThrows(
                NullPointerException::class.java,
                {
                    jwtTokenService.getAuthenticationFromAccessToken(
                        invalidToken,
                        userAgentFingerprint,
                    )
                },
                "should have thrown an exception",
            )
        }

        @Test
        fun `should throw an exception when token is missing iat or exp`() {
            val invalidToken = buildTokenString(iat = null, exp = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    {
                        jwtTokenService.getAuthenticationFromAccessToken(
                            invalidToken,
                            userAgentFingerprint,
                        )
                    },
                    "should have thrown an exception",
                )
            assertEquals(
                "Unable to parse token: Token is missing iat and/or exp",
                exception.message,
            )
        }
    }

    @Nested
    @DisplayName("getClaims")
    inner class GetClaims() {
        @Test
        fun `should return claims from token`() {
            val validToken = buildTokenString()
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
                    { jwtTokenService.getClaims("") },
                    "should have thrown an exception",
                )
            assertEquals(
                "Unable to parse token: JWT String argument cannot be null or empty.",
                exception.message,
            )
        }

        @Test
        fun `should throw an exception when token is expired`() {
            val expiredToken = buildTokenString(exp = issuedAt - 1000)
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
            val invalidToken = buildTokenString(iat = null, exp = null)
            val exception =
                assertThrows(
                    JwtException::class.java,
                    { jwtTokenService.getClaims(invalidToken) },
                    "should have thrown an exception",
                )
            assertEquals(
                "Unable to parse token: Token is missing iat and/or exp",
                exception.message,
            )
        }
    }
}
