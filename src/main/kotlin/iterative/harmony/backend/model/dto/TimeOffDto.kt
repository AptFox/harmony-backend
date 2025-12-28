package iterative.harmony.backend.model.dto

import iterative.harmony.backend.model.TimeOff
import java.time.Instant
import java.util.UUID

data class TimeOffDto(
    val id: Long? = null,
    val userId: UUID? = null,
    val playerId: Long? = null,
    val startTime: Instant,
    val endTime: Instant,
    val comment: String?,
) {
    companion object {
        fun fromTimeOff(timeOff: TimeOff) =
            TimeOffDto(
                timeOff.id,
                timeOff.user.userId,
                timeOff.playerId,
                timeOff.startTime,
                timeOff.endTime,
                timeOff.comment,
            )
    }
}
