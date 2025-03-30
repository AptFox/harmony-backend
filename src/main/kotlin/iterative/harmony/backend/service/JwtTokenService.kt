package iterative.harmony.backend.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import iterative.harmony.backend.model.RefreshToken
import iterative.harmony.backend.repository.RefreshTokenRepository
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import java.security.Key
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
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

    fun generateAccessToken(userId: String, roles: List<String>): String {
        val claims = Jwts.claims().setSubject(userId)
        claims["roles"] = roles
        val tokenIssuedAt = Utils().getCurrentTimeInMillisRounded()
        val tokenExpiration = tokenIssuedAt + ACCESS_TOKEN_DURATION_IN_MILLIS

        return buildToken(claims, tokenIssuedAt, tokenExpiration)
    }

    fun generateRefreshToken(userId: String): String {
        val tokenIssuedAt = Utils().getCurrentTimeInMillisRounded()
        val tokenExpiration = tokenIssuedAt + REFRESH_TOKEN_DURATION_IN_MILLIS
        val refreshToken =
            refreshTokenRepository.save(
                RefreshToken(UUID.fromString(userId), tokenIssuedAt, tokenExpiration)
            )

        val claims = generateClaimsFromRefreshToken(refreshToken)

        return buildToken(claims, tokenIssuedAt, tokenExpiration)
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
                "iat" to refreshToken.issuedAt,
                "exp" to refreshToken.expiresAt,
            )
        )
    }

    fun verifyRefreshToken(refreshToken: String?): RefreshToken {
        try {
            val tokenClaims = getClaims(refreshToken)
            val tokenFromClaims = getRefreshTokenFromClaims(tokenClaims)
            val tokenFromDb = refreshTokenRepository.findByJti(tokenFromClaims.jti!!).get()
            val tokenIsExpired = Date(tokenFromDb.expiresAt).before(Date())
            if (tokenFromDb.revoked || tokenIsExpired) {
                throw JwtException("Token is revoked or expired")
            }
            tokenFromDb.throwOnTokenMismatch(tokenFromClaims)

            return tokenFromDb
        } catch (ex: KotlinNullPointerException) {
            log.info("JTI is missing from Refresh token")
            throw JwtException("JTI is missing from Refresh token")
        } catch (ex: NullPointerException) {
            log.info("JTI is missing from Refresh token")
            throw JwtException("JTI is missing from Refresh token")
        } catch (e: NoSuchElementException) {
            throw JwtException("Refresh token not found in database")
        } catch (e: Exception) {
            log.info("Error while verifying refresh token: ${e.message}")
            throw e
        }
    }

    private fun getRefreshTokenFromClaims(tokenClaims: Claims): RefreshToken {
        val jti = UUID.fromString(tokenClaims.get("jti", String::class.java))
        val userId = UUID.fromString(tokenClaims.subject)
        val exp = tokenClaims.expiration.time
        val iat = tokenClaims.issuedAt.time

        return RefreshToken(userId, iat, exp, jti)
    }

    fun getAuthentication(token: String): Authentication {
        val tokenClaims = getClaims(token)
        val userId = tokenClaims.subject
        val roles = tokenClaims.get("roles", List::class.java).filterIsInstance<String>()

        val authorities = roles.map { role -> SimpleGrantedAuthority(role) }
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        auth.details = mapOf("userId" to userId) // I can probably put more info here
        return auth
    }

    fun getClaims(token: String?): Claims {
        if (token.isNullOrEmpty()) {
            throw JwtException("Token is null")
        }
        return tokenParser.parseClaimsJws(token).body
    }

    companion object {
        const val ACCESS_TOKEN_DURATION_IN_MILLIS: Long = 7200000 // 2 hours
        const val REFRESH_TOKEN_DURATION_IN_MILLIS: Long = 172800000 // 48 hours
    }
}
