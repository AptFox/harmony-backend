package iterative.harmony.backend.model

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "availability_exceptions")
data class AvailabilityException(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, insertable = false)
    val id: Long? = null,
    val userId: UUID,
    val playerId: Long? = null,
    val startTime: Instant,
    val endTime: Instant,
    val comment: String?,
)
