package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.User
import java.util.UUID

data class UserResponse(
    val id: UUID,
    val displayName: String,
    val twelveHourClock: Boolean,
    val timeZoneId: String?,
    val discordId: String,
    val discordAvatarHash: String,
) {
    companion object {
        fun fromUser(user: User) =
            UserResponse(
                user.userId!!,
                user.displayName,
                user.twelveHourClock,
                user.timeZoneId,
                user.discordId,
                user.discordAvatarHash,
            )
    }
}
