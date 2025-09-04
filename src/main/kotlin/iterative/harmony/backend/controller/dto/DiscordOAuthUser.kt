package iterative.harmony.backend.controller.dto

data class DiscordOAuthUser(
    val id: String,
    val username: String,
    val globalName: String?,
    val avatarHash: String,
)
