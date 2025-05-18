package iterative.harmony.backend.util

import jakarta.servlet.http.HttpServletRequest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
     * Creates a unique user agent string from the supplied request.
     *
     * @param request the HttpServletRequest
     * @return "userAgent|ipPrefix"
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

    /**
     * Generates a hash for the given string.
     *
     * @param str The input string to hash
     * @return SHA-256 hash
     */
    fun generateFingerprint(str: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(str.toByteArray(StandardCharsets.UTF_8))

        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Returns a user agent fingerprint string based on the request's User-Agent header and the
     * client's IP address.
     *
     * @param request The HttpServletRequest object containing the request information.
     * @return A hashed fingerprint representing the user agent.
     */
    fun generateUserAgentFingerprint(request: HttpServletRequest): String {
        val uniqueUserAgent = getUserAgentFromRequest(request)
        return generateFingerprint(uniqueUserAgent)
    }
}
