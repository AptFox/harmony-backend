package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import iterative.harmony.backend.model.dto.TimeOffDto
import iterative.harmony.backend.model.dto.WeeklyAvailabilitySlotDto

data class AvailabilityResponse(
    val weeklyAvailabilitySlots: List<WeeklyAvailabilitySlotDto>,
    val timeOffs: List<TimeOffDto>,
) {
    companion object {
        fun fromWeeklyAvailabilitySlotsAndTimeOffs(
            slots: List<WeeklyAvailabilitySlot>,
            timeOffs: List<TimeOff>,
        ): AvailabilityResponse {
            val slotDtos = mutableListOf<WeeklyAvailabilitySlotDto>()
            val timeOffDtos = mutableListOf<TimeOffDto>()

            slots.forEach { slot ->
                slotDtos.add(WeeklyAvailabilitySlotDto.Companion.fromWeeklyAvailabilitySlot(slot))
            }

            timeOffs.forEach { timeOff ->
                timeOffDtos.add(TimeOffDto.Companion.fromTimeOff(timeOff))
            }

            return AvailabilityResponse(slotDtos, timeOffDtos)
        }
    }
}
