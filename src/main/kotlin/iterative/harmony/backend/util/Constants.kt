package iterative.harmony.backend.util

object RoleConstants {
    const val USER_ROLE = "USER"
    const val ADMIN_ROLE = "ADMIN"
}

object SecurityConstants {
    const val AUTH_PATH = "/auth"
    const val REFRESH_TOKEN_PATH = "/auth/refresh_token"
    const val LOGOUT_PATH = "/auth/logout"
    const val DISCORD_OAUTH_PATH = "/oauth2/authorization/discord"
    const val ERROR_PATH = "/error"
    const val FAVICON_PATH = "/favicon.ico"
    const val ACCESS_TOKEN_NAME = "harmony_access_token"
    const val REFRESH_TOKEN_NAME = "harmony_refresh_token"
    const val COOKIE_EXPIRATION_IN_SECONDS = 60 * 60 * 48 // 48 hours
}
