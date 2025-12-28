package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.model.dto.WeeklyAvailabilitySlotDto

data class WeeklyAvailabilityResponse(
    val weeklyAvailabilitySlots: List<WeeklyAvailabilitySlotDto>? = null,
    val errors: List<Map<String, String>>? = null,
) {
    companion object {
        fun fromWeeklyAvailabilitySlots(
            slots: List<WeeklyAvailabilitySlot>
        ): WeeklyAvailabilityResponse {
            val weeklyAvailabilitySlots = mutableListOf<WeeklyAvailabilitySlotDto>()

            slots.forEach { slot ->
                weeklyAvailabilitySlots.add(
                    WeeklyAvailabilitySlotDto.Companion.fromWeeklyAvailabilitySlot(slot)
                )
            }

            return WeeklyAvailabilityResponse(weeklyAvailabilitySlots)
        }
    }
}
