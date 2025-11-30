package iterative.harmony.backend.repository

import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface WeeklyAvailabilitySlotRepository : JpaRepository<WeeklyAvailabilitySlot, Long> {
    fun findAllByUserId(userId: UUID): List<WeeklyAvailabilitySlot>

    fun findAllByPlayerId(playerId: Long): List<WeeklyAvailabilitySlot>

    @Modifying @Transactional fun deleteAllByUserId(userId: UUID): List<WeeklyAvailabilitySlot>
}
