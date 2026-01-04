package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.WeeklyAvailabilitySlot
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings

@Mapper
interface WeeklyAvailabilitySlotMapper {
    @Mappings(
        Mapping(source = "user.userId", target = "userId"),
        Mapping(source = "player.id", target = "playerId"),
    )
    fun toWeeklyAvailabilitySlotResponse(
        slot: WeeklyAvailabilitySlot
    ): WeeklyAvailabilitySlotResponse

    fun toWeeklyAvailabilitySlotResponseList(
        weeklyAvailabilitySlots: List<WeeklyAvailabilitySlot>
    ): List<WeeklyAvailabilitySlotResponse>
}
