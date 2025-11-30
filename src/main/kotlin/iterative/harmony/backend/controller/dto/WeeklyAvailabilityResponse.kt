package iterative.harmony.backend.controller.dto

import iterative.harmony.backend.model.WeeklyAvailabilitySlot

data class WeeklyAvailabilityResponse(
    val weeklyAvailabilitySlots: List<WeeklyAvailabilitySlot>? = null,
    val errors: List<Map<String, String>>? = null,
)
