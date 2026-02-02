package iterative.harmony.backend.util

object RoleConstants {
    const val USER_ROLE = "USER"
    const val ADMIN_ROLE = "ADMIN"
}

object CacheConstants {
    const val FRANCHISE_TEAMS = "FranchiseTeamsByFranchiseId"
    const val TEAM_AVAILABILITY_BY_ID = "TeamAvailabilityById"
    const val USER_AVAILABILITY_BY_ID = "UserAvailabilityById"
    const val USER_BY_ID = "UserById"
}

object SecurityConstants {
    const val AUTH_PATH = "/auth"
    const val REFRESH_TOKEN_PATH = "/auth/refresh_token"
    const val LOGOUT_PATH = "/auth/logout"
    const val DISCORD_OAUTH_PATH = "/oauth2/authorization/discord"
    const val FAVICON_PATH = "/favicon.ico"
    const val GIT_PATH = "/.git"
    const val ENV_PATH = "/.env"
    const val ACCESS_TOKEN_NAME = "harmony_access_token"
    const val REFRESH_TOKEN_NAME = "harmony_refresh_token"
    const val COOKIE_EXPIRATION_IN_SECONDS = 60 * 60 * 48 // 48 hours
    const val AUTH_PREFIX: String = "auth"
    const val NON_AUTH_PREFIX: String = "nonAuth"
    const val ANON_USER_ID: String = "ANON"
    const val ANON_USER_AGENT: String = "ANON_BROWSER"
    const val ANON_REQUEST_IP: String = "0.0.0.0"
}

object AvailabilityConstants {
    val DAYS_OF_WEEK = setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val INVALID_DAY_OF_WEEK = "dayOfWeek is not one of $DAYS_OF_WEEK"
    const val END_TIME_BEFORE_START = "endTime is before startTime"
    const val SAME_START_AND_END_TIME = "startTime and endTime are the same"
    const val INVALID_TIME_ZONE_ID = "Invalid timeZoneId"
    const val LESS_THAN_ONE_HOUR = "availability changes must be >=60 min"
    const val MORE_THAN_24_HOURS = "availability changes must be <= 24 hours"
    const val MORE_THAN_90_DAYS_AWAY = "time off must be within 90 days"
    const val TIME_OFF_ALREADY_EXISTS = "Time off with this start time already exists"
}
