package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "time_off")
data class TimeOff(
    val userId: UUID,
    val playerId: Long? = null,
    val startTime: Instant,
    val endTime: Instant,
    val comment: String?,
) : LongEntity()
