package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "time_off")
data class TimeOff(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") val user: User,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "player_id") val player: Player?,
    val startTime: Instant,
    val endTime: Instant,
    val comment: String?,
) : LongEntity()
