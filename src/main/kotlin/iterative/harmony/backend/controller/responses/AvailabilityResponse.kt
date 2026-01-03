package iterative.harmony.backend.controller.responses

data class AvailabilityResponse(
    var weeklyAvailabilitySlots: List<WeeklyAvailabilitySlotResponse>,
    var timeOffs: List<TimeOffResponse>,
)
