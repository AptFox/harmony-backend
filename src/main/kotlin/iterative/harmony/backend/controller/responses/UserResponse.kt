package iterative.harmony.backend.controller.responses

import java.util.UUID

data class UserResponse(
    var userId: UUID,
    var displayName: String,
    var twelveHourClock: Boolean,
    var timeZoneId: String?,
    var discordId: String,
    var discordAvatarHash: String?,
)
