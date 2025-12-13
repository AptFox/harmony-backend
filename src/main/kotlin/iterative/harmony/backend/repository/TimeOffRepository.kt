package iterative.harmony.backend.repository

import iterative.harmony.backend.model.TimeOff
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TimeOffRepository : JpaRepository<TimeOff, Long> {

    fun findAllByUserIdAndStartTimeIsAfterOrEndTimeIsAfter(
        userId: UUID,
        nowStart: Instant,
        nowEnd: Instant,
    ): List<TimeOff>

    fun findAllByUserIdAndEndTimeIsBefore(userId: UUID, now: Instant): List<TimeOff>

    fun existsByUserIdAndStartTimeEquals(userId: UUID, startTime: Instant): Boolean

    @Modifying @Transactional fun deleteByIdAndUserId(id: Long, userId: UUID)
}
