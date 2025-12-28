package iterative.harmony.backend.controller.responses

data class TeamScheduleResponse(
    val playerSchedules: MutableMap<PlayerResponse, AvailabilityResponse>?,
    val error: String?,
)
