package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface WeeklyAvailabilitySlotRepository : JpaRepository<WeeklyAvailabilitySlot, Long> {
    fun findAllByUser(user: User): List<WeeklyAvailabilitySlot>

    @Modifying @Transactional fun deleteAllByUser(user: User): List<WeeklyAvailabilitySlot>
}
