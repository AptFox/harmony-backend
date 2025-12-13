package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.*
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(name = "weekly_availability_slots")
data class WeeklyAvailabilitySlot(
    val userId: UUID,
    val playerId: Long? = null,
    val dayOfWeek: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val timeZoneId: String,
) : LongEntity()
