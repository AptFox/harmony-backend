package iterative.harmony.backend.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import iterative.harmony.backend.exception.JtiNotInRefreshTokenException
import iterative.harmony.backend.exception.RefreshTokenExpiredException
import iterative.harmony.backend.exception.RefreshTokenFieldMismatchException
import iterative.harmony.backend.exception.RefreshTokenNotInDBException
import iterative.harmony.backend.exception.TokenFingerprintMismatchException
import iterative.harmony.backend.exception.UnexpectedRefreshTokenVerificationException
import iterative.harmony.backend.exception.UnparseableTokenException
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.repository.RefreshTokenRepository
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import java.security.Key
import java.sql.Timestamp
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.domain.Limit
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

@Service
class JwtTokenService(@Value("\${jwt.secret}") private val secretKey: String) {
    @Autowired private lateinit var refreshTokenRepository: RefreshTokenRepository
    private val key: Key = Keys.hmacShaKeyFor(secretKey.toByteArray())
    private val tokenParser = Jwts.parserBuilder().setSigningKey(key).build()
    private val log = getLogger()

    fun generateAccessToken(
        userId: String,
        fingerprint: String,
        roles: List<String>,
        utils: Utils = Utils(),
    ): String {
        val claims = Jwts.claims().setSubject(userId)
        claims["roles"] = roles
        claims["fp"] = fingerprint
        val tokenIssuedAt = utils.getCurrentTimeInMillisRounded()
        val tokenExpiration = tokenIssuedAt + ACCESS_TOKEN_DURATION_IN_MILLIS

        return buildToken(claims, tokenIssuedAt, tokenExpiration)
    }

    fun generateRefreshToken(userId: String, fingerprint: String, utils: Utils = Utils()): String {
        val tokenIssuedAt = utils.getCurrentTimeInMillisRounded()
        val tokenExpiration = tokenIssuedAt + REFRESH_TOKEN_DURATION_IN_MILLIS
        val refreshToken =
            refreshTokenRepository.save(
                RefreshToken(UUID.fromString(userId), fingerprint, tokenIssuedAt, tokenExpiration)
            )

        val claims = generateClaimsFromRefreshToken(refreshToken)

        return buildToken(claims, tokenIssuedAt, tokenExpiration)
    }

    fun deleteRefreshToken(refreshToken: RefreshToken) {
        try {
            refreshTokenRepository.delete(refreshToken)
        } catch (ex: OptimisticLockingFailureException) {
            throw RefreshTokenNotInDBException(ex)
        }
    }

    fun deleteExpiredRefreshTokensForUser(userId: UUID) {
        log.info("querying for expired refresh tokens")
        val timestamp =
            Timestamp(Utils().getCurrentTimeInMillisRounded() - REFRESH_TOKEN_DURATION_IN_MILLIS)
        val expiredRefreshTokens: List<RefreshToken> =
            refreshTokenRepository.findAllByUserIdAndCreatedAtBefore(userId, timestamp)

        if (expiredRefreshTokens.count() > 0) {
            log.info("${expiredRefreshTokens.count()} expired tokens found. Deleting.")
            refreshTokenRepository.deleteAll(expiredRefreshTokens)
        }
    }

    fun deleteExcessRefreshTokensForUser(userId: UUID) {
        log.info("querying for excess refresh tokens")
        val refreshTokenCountForUser = refreshTokenRepository.countByUserId(userId)
        if (refreshTokenCountForUser > 2) {
            log.info(
                "$refreshTokenCountForUser active refresh tokens found. Deleting all but the 2 most recently issued tokens..."
            )
            val limit = Limit.of(refreshTokenCountForUser - 2)
            val refreshTokens: List<RefreshToken> =
                refreshTokenRepository.findAllByUserIdOrderByCreatedAtAsc(userId, limit)
            refreshTokenRepository.deleteAll(refreshTokens)
        }
    }

    private fun buildToken(claims: Claims, issuedAt: Long, expiration: Long): String {
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(Date(issuedAt))
            .setExpiration(Date(expiration))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    private fun generateClaimsFromRefreshToken(refreshToken: RefreshToken): Claims {
        return Jwts.claims(
            mapOf(
                "sub" to refreshToken.userId.toString(),
                "jti" to refreshToken.jti.toString(),
                "fp" to refreshToken.fingerprint,
                "iat" to refreshToken.issuedAt,
                "exp" to refreshToken.expiresAt,
            )
        )
    }

    fun verifyRefreshToken(refreshToken: String, userAgentFingerprint: String): RefreshToken {
        try {
            val tokenClaims = getClaims(refreshToken)
            val tokenFromClaims = getRefreshTokenFromClaims(tokenClaims)

            if (tokenFromClaims.fingerprint != userAgentFingerprint)
                throw TokenFingerprintMismatchException()

            val tokenFromDb = refreshTokenRepository.findByJti(tokenFromClaims.jti!!).get()
            val tokenIsExpired = Date(tokenFromDb.expiresAt).before(Date())
            if (tokenIsExpired) throw RefreshTokenExpiredException()

            throwOnRefreshTokenMismatch(tokenFromDb, tokenFromClaims)

            return tokenFromDb
        } catch (ex: NullPointerException) {
            throw JtiNotInRefreshTokenException(ex)
        } catch (ex: NoSuchElementException) {
            throw RefreshTokenNotInDBException(ex)
        } catch (ex: Exception) {
            throw UnexpectedRefreshTokenVerificationException(ex)
        }
    }

    fun throwOnRefreshTokenMismatch(tokenFromDb: RefreshToken, tokenFromClaims: RefreshToken) {
        if (tokenFromDb == tokenFromClaims) return

        val mismatches = mutableListOf<String>()

        if (tokenFromDb.jti != tokenFromClaims.jti) mismatches.add("jti")
        if (tokenFromDb.userId != tokenFromClaims.userId) mismatches.add("userId")
        if (tokenFromDb.expiresAt != tokenFromClaims.expiresAt) mismatches.add("expiresAt")
        if (tokenFromDb.issuedAt != tokenFromClaims.issuedAt) mismatches.add("issuedAt")

        if (mismatches.isNotEmpty()) throw RefreshTokenFieldMismatchException(mismatches)
    }

    private fun getRefreshTokenFromClaims(tokenClaims: Claims): RefreshToken {
        val jti = UUID.fromString(tokenClaims.get("jti", String::class.java))
        val userId = UUID.fromString(tokenClaims.subject)
        val fp = tokenClaims.get("fp", String::class.java)
        val exp = tokenClaims.expiration.time
        val iat = tokenClaims.issuedAt.time

        return RefreshToken(userId, fp, iat, exp, jti)
    }

    fun getAuthenticationFromAccessToken(
        accessToken: String,
        userAgentFingerprint: String,
    ): Authentication {
        val accessTokenClaims = getClaims(accessToken)
        val accessTokenFingerprint = accessTokenClaims.get("fp", String::class.java)

        if (accessTokenFingerprint != userAgentFingerprint)
            throw TokenFingerprintMismatchException()

        val userId = accessTokenClaims.subject
        val roles = accessTokenClaims.get("roles", List::class.java).filterIsInstance<String>()

        val authorities = roles.map { role -> SimpleGrantedAuthority(role) }
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        auth.details = mapOf("userId" to userId) // I can probably put more info here
        return auth
    }

    fun getClaims(token: String): Claims {
        try {
            val claims = tokenParser.parseClaimsJws(token).body
            if (claims.issuedAt == null || claims.expiration == null) {
                throw MalformedJwtException("Token is missing iat and/or exp")
            }
            return claims
        } catch (ex: Exception) {
            throw UnparseableTokenException(ex)
        }
    }

    companion object {
        const val ACCESS_TOKEN_DURATION_IN_MILLIS: Long = 7200000 // 2 hours
        const val REFRESH_TOKEN_DURATION_IN_MILLIS: Long = 172800000 // 48 hours
    }
}
