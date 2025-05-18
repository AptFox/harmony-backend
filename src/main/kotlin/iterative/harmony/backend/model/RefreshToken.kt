package iterative.harmony.backend.model

import io.jsonwebtoken.JwtException
import jakarta.persistence.*
import java.sql.Timestamp
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
data class RefreshToken(
    @Column(nullable = false) val userId: UUID,
    @Transient val fingerprint: String? = null,
    @Column(nullable = false) val issuedAt: Long,
    @Column(nullable = false) val expiresAt: Long,
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, insertable = false)
    val jti: UUID? = null,
    @Column(nullable = false) val revoked: Boolean = false,
    @Column(nullable = false, updatable = false, insertable = false)
    val createdAt: Timestamp? = null,
    @Column(nullable = false, updatable = false, insertable = false)
    val updatedAt: Timestamp? = null,
) {
    fun throwOnTokenMismatch(other: RefreshToken) {
        if (this == other) return

        val mismatches = mutableListOf<String>()

        if (this.jti != other.jti) mismatches.add("jti")
        if (this.userId != other.userId) mismatches.add("userId")
        if (this.expiresAt != other.expiresAt) mismatches.add("expiresAt")
        if (this.issuedAt != other.issuedAt) mismatches.add("issuedAt")

        if (mismatches.isNotEmpty()) {
            throw JwtException("Mismatched fields: ${mismatches.joinToString(", ")}")
        }
    }
}
