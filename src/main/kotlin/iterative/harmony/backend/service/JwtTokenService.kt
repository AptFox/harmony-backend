package iterative.harmony.backend.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import iterative.harmony.backend.util.getLogger
import java.security.Key
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service

@Service
class JwtTokenService(@Value("\${jwt.secret}") private val secretKey: String) {

    private val key: Key = Keys.hmacShaKeyFor(secretKey.toByteArray())
    private val tokenParser = Jwts.parserBuilder().setSigningKey(key).build()
    private val log = getLogger()

    fun generateToken(userId: UUID, roles: List<String>): String {
        val claims = Jwts.claims().setSubject(userId.toString())
        claims["roles"] = roles
        val tokenIssuedAt = Date()
        val tokenExpiration = Date(tokenIssuedAt.time + TOKEN_DURATION_IN_MILLIS)

        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(tokenIssuedAt)
            .setExpiration(tokenExpiration)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean {
        try {
            tokenParser.parseClaimsJws(token)
            return true
        } catch (ex: JwtException) {
            log.info("Invalid JWT token: ${ex.message}")
            return false
        }
    }

    fun getAuthentication(token: String): Authentication {
        val claims = getClaims(token)
        val userId = claims.subject
        val roles = claims.get("roles", List::class.java).filterIsInstance<String>()

        val authorities = roles.map { role -> SimpleGrantedAuthority(role) }
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        auth.details = mapOf("userId" to userId)
        return auth
    }

    fun getClaims(token: String): Claims {
        return tokenParser.parseClaimsJws(token).body
    }

    companion object {
        private const val TOKEN_DURATION_IN_MILLIS = 3600000 // 1 hour
    }
}
