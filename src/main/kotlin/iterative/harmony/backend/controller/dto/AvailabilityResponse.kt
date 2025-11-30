package iterative.harmony.backend.controller.dto

import iterative.harmony.backend.model.AvailabilityException
import iterative.harmony.backend.model.WeeklyAvailabilitySlot

data class AvailabilityResponse(
    val weeklyAvailabilitySlots: List<WeeklyAvailabilitySlot>,
    val availabilityExceptions: List<AvailabilityException>,
)
