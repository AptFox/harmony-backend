package iterative.harmony.backend.controller.responses

import java.util.UUID

data class PlayerResponse(
    var id: Long,
    var name: String,
    var team: TeamResponse?,
    var userId: UUID,
    var teamRole: String?,
)
