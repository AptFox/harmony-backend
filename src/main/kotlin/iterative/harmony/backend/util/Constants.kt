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
    const val AUTH_PREFIX: String = "auth"
    const val NON_AUTH_PREFIX: String = "nonAuth"
    const val ANON_USER_ID: String = "anonUser"
    const val ANON_USER_AGENT: String = "anonBrowser"
    const val ANON_REQUEST_IP: String = "0.0.0.0"
}
