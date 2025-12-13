package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.AuditableEntity
import jakarta.persistence.*
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
) : AuditableEntity()
