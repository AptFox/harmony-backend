package iterative.harmony.backend.repository

import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.User
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TimeOffRepository : JpaRepository<TimeOff, Long> {

    @Query(
        """
        SELECT t FROM TimeOff t 
        WHERE t.user.userId = :userId 
        AND (t.startTime > :now OR t.endTime > :now)
    """
    )
    fun findFutureTimeOffForUser(
        @Param("userId") userId: UUID,
        @Param("now") now: Instant,
    ): List<TimeOff>

    @Query(
        """
        SELECT t FROM TimeOff t 
        WHERE t.player.id = :playerId 
        AND (t.startTime > :now OR t.endTime > :now)
        AND (t.startTime < :sevenDaysFromNow OR t.endTime < :sevenDaysFromNow)
    """
    )
    fun findTimeOffWithinNextWeekForPlayer(
        @Param("playerId") playerId: Long,
        @Param("now") now: Instant,
        @Param("sevenDaysFromNow") sevenDaysFromNow: Instant,
    ): List<TimeOff>

    fun findAllByUserAndEndTimeIsBefore(user: User, now: Instant): List<TimeOff>

    fun existsByUserAndStartTimeEquals(user: User, startTime: Instant): Boolean

    fun findByIdAndUser(id: Long, user: User): Optional<TimeOff>

    @Modifying @Transactional fun deleteByIdAndUser(id: Long, user: User)
}
