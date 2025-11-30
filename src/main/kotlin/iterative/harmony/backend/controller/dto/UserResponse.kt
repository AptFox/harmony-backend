package iterative.harmony.backend.controller.dto

import java.util.*

data class UserResponse(
    val id: UUID,
    val displayName: String,
    val twelveHourClock: Boolean,
    val timeZoneId: String?,
    val discordId: String,
    val discordAvatarHash: String,
)
