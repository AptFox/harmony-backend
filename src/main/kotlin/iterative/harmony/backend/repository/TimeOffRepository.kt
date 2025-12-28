package iterative.harmony.backend.repository

import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.User
import java.time.Instant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TimeOffRepository : JpaRepository<TimeOff, Long> {

    fun findAllByUserAndStartTimeIsAfterOrEndTimeIsAfter(
        user: User,
        nowStart: Instant,
        nowEnd: Instant,
    ): List<TimeOff>

    fun findAllByUserAndEndTimeIsBefore(user: User, now: Instant): List<TimeOff>

    fun existsByUserAndStartTimeEquals(user: User, startTime: Instant): Boolean

    @Modifying @Transactional fun deleteByIdAndUser(id: Long, user: User)
}
