package iterative.harmony.backend.controller.responses

data class WeeklyAvailabilityResponse(
    var weeklyAvailabilitySlots: List<WeeklyAvailabilitySlotResponse>? = null,
    var errors: List<Map<String, String>>? = null,
)
