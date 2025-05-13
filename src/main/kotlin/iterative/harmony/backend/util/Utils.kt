package iterative.harmony.backend.util

import jakarta.servlet.http.HttpServletRequest
import java.util.*

class Utils {
    /**
     * Returns the current time in milliseconds rounded to the nearest second.
     *
     * @return The current time in milliseconds rounded to the nearest second.
     */
    fun getCurrentTimeInMillisRounded(): Long {
        return ((Date().time + 500) / 1000) * 1000
    }

    /**
     * Returns a user agent string based on the request's User-Agent header and the client's IP
     * address.
     *
     * @param request The HttpServletRequest object containing the request information.
     * @return A string representing the user agent, formatted as "User-Agent|IP-Prefix".
     */
    fun getUserAgentFromRequest(request: HttpServletRequest): String {
        val userAgent = request.getHeader("User-Agent")?.trim() ?: "unknown"
        val rawIp =
            request.getHeader("X-Forwarded-For")?.split(",")?.firstOrNull()
                ?: request.remoteAddr
                ?: "0.0.0.0"
        val ipPrefix = rawIp.substringBefore('.')
        return "${userAgent}|${ipPrefix}"
    }
}
