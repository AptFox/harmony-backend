package iterative.harmony.backend.model.dto

import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import java.time.LocalTime
import java.util.UUID

data class WeeklyAvailabilitySlotDto(
    val id: Long? = null,
    val userId: UUID? = null,
    val playerId: Long? = null,
    val dayOfWeek: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val timeZoneId: String,
) {
    companion object {
        fun fromWeeklyAvailabilitySlot(was: WeeklyAvailabilitySlot) =
            WeeklyAvailabilitySlotDto(
                was.id,
                was.user.userId,
                was.player?.id,
                was.dayOfWeek,
                was.startTime,
                was.endTime,
                was.timeZoneId,
            )
    }
}
