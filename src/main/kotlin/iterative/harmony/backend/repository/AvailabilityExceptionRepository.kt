package iterative.harmony.backend.repository

import iterative.harmony.backend.model.AvailabilityException
import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AvailabilityExceptionRepository : JpaRepository<AvailabilityException, Long> {
    fun findAllByUserId(userId: UUID): List<AvailabilityException>

    fun findAllByPlayerId(playerId: Long): List<AvailabilityException>

    fun existsByUserIdAndStartTimeEquals(userId: UUID, startTime: Instant): Boolean
}
