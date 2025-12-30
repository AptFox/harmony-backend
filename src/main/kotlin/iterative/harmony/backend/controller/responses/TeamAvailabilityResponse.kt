package iterative.harmony.backend.controller.responses

data class TeamAvailabilityResponse(
    var playerSchedules: MutableMap<PlayerResponse, AvailabilityResponse> = mutableMapOf(),
    var error: String? = null,
)
