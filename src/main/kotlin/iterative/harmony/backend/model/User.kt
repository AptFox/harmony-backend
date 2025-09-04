package iterative.harmony.backend.model

import jakarta.persistence.*
import java.sql.Timestamp
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
    val discordId: String,
    var discordAvatarHash: String,
    var timeZoneId: Int,
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")],
    )
    val roles: Set<Role>,
    @Column(nullable = false, updatable = false, insertable = false)
    val createdAt: Timestamp? = null,
    @Column(nullable = false, updatable = false, insertable = false)
    val updatedAt: Timestamp? = null,
)
