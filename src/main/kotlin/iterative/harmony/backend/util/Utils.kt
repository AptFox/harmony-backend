package iterative.harmony.backend.util

import iterative.harmony.backend.exception.FingerprintNotInRequestException
import iterative.harmony.backend.util.SecurityConstants.ANON_REQUEST_IP
import iterative.harmony.backend.util.SecurityConstants.ANON_USER_ID
import iterative.harmony.backend.util.SecurityConstants.CF_CONNECTING_IP_HEADER
import iterative.harmony.backend.util.SecurityConstants.FINGERPRINT_COOKIE_NAME
import jakarta.servlet.http.HttpServletRequest
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.ZoneId
import java.util.*
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.util.WebUtils

class Utils {
    /**
     * Returns the current time in milliseconds rounded to the nearest second.
     *
     * @return The current time in milliseconds rounded to the nearest second.
     */
    fun getCurrentTimeInMillisRounded(): Long {
        return ((Date().time + 500) / 1000) * 1000
    }

    fun getUserIdFromSecurityContext(): String {
        val authentication = SecurityContextHolder.getContext()?.authentication
        return authentication?.name ?: ANON_USER_ID
    }

    fun getDisplayNameFromSecurityContext(): String? {
        val details = SecurityContextHolder.getContext()?.authentication?.details as? Map<*, *>
        return details?.get("displayName") as? String
    }

    /** Returns the client IP from Cloudflare's CF-Connecting-IP header, or remoteAddr locally. */
    fun getClientIp(request: HttpServletRequest): String {
        return request.getHeader(CF_CONNECTING_IP_HEADER) ?: request.remoteAddr ?: ANON_REQUEST_IP
    }

    /** Generates a random fingerprint that binds tokens to the browser they were issued to. */
    fun generateRandomFingerprint(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /**
     * Hashes the given fingerprint.
     *
     * @param fingerprint The fingerprint to hash
     * @return SHA-256 hash
     */
    fun hashFingerprint(fingerprint: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(fingerprint.toByteArray(StandardCharsets.UTF_8))

        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /**
     * Returns the fingerprint from the request's fingerprint cookie.
     *
     * @param request The HttpServletRequest object containing the request information.
     * @return The raw (unhashed) fingerprint
     * @throws FingerprintNotInRequestException if the request has no fingerprint cookie
     */
    fun getFingerprintFromRequest(request: HttpServletRequest): String {
        val fingerprintCookie =
            WebUtils.getCookie(request, FINGERPRINT_COOKIE_NAME)
                ?: throw FingerprintNotInRequestException()
        return fingerprintCookie.value
    }

    fun verifyTimeZone(timeZone: String) {
        try {
            ZoneId.of(timeZone)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid timeZone supplied", ex)
        }
    }
}
