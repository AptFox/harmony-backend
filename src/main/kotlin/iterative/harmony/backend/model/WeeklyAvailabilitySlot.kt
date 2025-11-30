package iterative.harmony.backend.model

import jakarta.persistence.*
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "weekly_availability_slots")
data class WeeklyAvailabilitySlot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, insertable = false)
    val id: Long? = null,
    val userId: UUID,
    val playerId: Long? = null,
    val dayOfWeek: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val timeZoneId: String,
)
