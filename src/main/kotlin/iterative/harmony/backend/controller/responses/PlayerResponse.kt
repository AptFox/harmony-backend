package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.Player
import java.util.UUID

data class PlayerResponse(
    val id: Long,
    val name: String,
    val orgId: Long,
    val skillGroupId: Long,
    val teamId: Long,
    val userId: UUID,
    val teamRole: String?,
) {
    companion object {
        fun fromPlayer(player: Player) =
            PlayerResponse(
                player.id!!,
                player.name,
                player.organization.id!!,
                player.skillGroup.id!!,
                player.team?.id!!,
                player.user.userId!!,
                player.teamRole,
            )
    }
}
