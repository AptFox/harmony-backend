package iterative.harmony.backend.controller.responses

data class PlayerAvailabilityResponse(
    val playerId: Long,
    val playerName: String,
    val availability: AvailabilityResponse,
)

data class TeamAvailabilityResponse(
    var playerSchedules: MutableList<PlayerAvailabilityResponse> = mutableListOf(),
    var error: String? = null,
)
