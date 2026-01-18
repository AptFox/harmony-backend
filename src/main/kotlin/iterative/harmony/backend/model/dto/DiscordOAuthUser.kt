package iterative.harmony.backend.model.dto

data class DiscordOAuthUser(val id: String, val username: String, val avatarHash: String?) {
    companion object {
        fun fromAttributes(attributes: Map<String, Any>): DiscordOAuthUser {
            return DiscordOAuthUser(
                id =
                    attributes["id"] as? String
                        ?: throw IllegalArgumentException(
                            "id is missing from Discord OAuth attributes"
                        ),
                username =
                    attributes["username"] as? String
                        ?: throw IllegalArgumentException(
                            "username is missing from Discord OAuth attributes"
                        ),
                avatarHash = attributes["avatar"] as? String,
            )
        }
    }
}
