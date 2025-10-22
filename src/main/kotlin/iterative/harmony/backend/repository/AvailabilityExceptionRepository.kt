package iterative.harmony.backend.repository

import iterative.harmony.backend.model.AvailabilityException
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface AvailabilityExceptionRepository : JpaRepository<AvailabilityException, Long> {
    fun findAllByUserId(userId: UUID): List<AvailabilityException>

    fun findAllByPlayerId(playerId: Long): List<AvailabilityException>

    fun existsByUserIdAndStartTimeEquals(userId: UUID, startTime: Instant): Boolean

    @Modifying @Transactional fun deleteByIdAndUserId(id: Long, userId: UUID)
}
