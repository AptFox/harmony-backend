package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.AuditableEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, insertable = false)
    val userId: UUID? = null,
    val username: String,
    var displayName: String,
    var twelveHourClock: Boolean = true,
    val discordId: String,
    var discordAvatarHash: String?,
    var timeZoneId: String?,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    val roles: Set<Role>,
    val lastLoginAt: Instant? = null,
    var importId: String? = null,
) : AuditableEntity()
