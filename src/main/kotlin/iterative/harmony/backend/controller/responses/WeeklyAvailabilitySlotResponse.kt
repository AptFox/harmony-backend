package iterative.harmony.backend.controller.responses

import java.time.LocalTime
import java.util.UUID

data class WeeklyAvailabilitySlotResponse(
    var id: Long,
    var userId: UUID,
    var playerId: Long?,
    var dayOfWeek: String,
    var startTime: LocalTime,
    var endTime: LocalTime,
    var timeZoneId: String,
)
