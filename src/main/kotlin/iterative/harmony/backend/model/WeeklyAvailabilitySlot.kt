package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.*
import java.time.LocalTime

@Entity
@Table(name = "weekly_availability_slots")
data class WeeklyAvailabilitySlot(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") val user: User,
    val playerId: Long? = null,
    val dayOfWeek: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val timeZoneId: String,
) : LongEntity()
