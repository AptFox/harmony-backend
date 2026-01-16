package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface WeeklyAvailabilitySlotMapper {
    @Mapping(source = "user.userId", target = "userId")
    fun toWeeklyAvailabilitySlotResponse(
        slot: WeeklyAvailabilitySlot
    ): WeeklyAvailabilitySlotResponse

    fun toWeeklyAvailabilitySlotResponseList(
        weeklyAvailabilitySlots: List<WeeklyAvailabilitySlot>
    ): List<WeeklyAvailabilitySlotResponse>
}
