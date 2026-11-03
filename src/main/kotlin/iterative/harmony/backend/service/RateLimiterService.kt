package iterative.harmony.backend.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import iterative.harmony.backend.util.SecurityConstants.ANON_USER_ID
import iterative.harmony.backend.util.SecurityConstants.AUTH_PREFIX
import iterative.harmony.backend.util.SecurityConstants.NON_AUTH_PREFIX
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.setUserIdInLogs
import jakarta.servlet.http.HttpServletRequest
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class RateLimiterService() {
    private val AUTH_REQUEST_LIMIT: Long = 5L
    private val ALL_REQUEST_LIMIT: Long = 100L

    /** Returns the rate limit key: the user ID if authenticated, otherwise the client's network. */
    fun getRequestId(request: HttpServletRequest): String {
        val userId = Utils().getUserIdFromSecurityContext()
        setUserIdInLogs(userId)
        val authPrefix =
            if (request.requestURI.startsWith("/$AUTH_PREFIX/")) AUTH_PREFIX else NON_AUTH_PREFIX

        if (userId != ANON_USER_ID) return "$authPrefix|$userId"

        val clientNetwork = getClientNetwork(Utils().getClientIp(request))
        return "$authPrefix|$ANON_USER_ID|$clientNetwork"
    }

    fun requestIsAllowed(requestId: String, requestBuckets: MutableMap<String, Bucket>): Boolean {
        val isAuthEndpoint = requestId.startsWith(AUTH_PREFIX)
        val bucket = requestBuckets.computeIfAbsent(requestId) { createBucket(isAuthEndpoint) }
        return bucket.tryConsume(1)
    }

    /**
     * Returns the full IPv4 address, or the /64 prefix of an IPv6 address since a single client is
     * usually assigned a whole /64.
     */
    private fun getClientNetwork(clientIp: String): String {
        if (!isIpv6Literal(clientIp)) return clientIp

        val address =
            try {
                InetAddress.getByName(clientIp)
            } catch (ex: UnknownHostException) {
                return clientIp
            }
        if (address !is Inet6Address) return address.hostAddress

        val networkPrefixBytes = address.address.copyOf(8) + ByteArray(8)
        val networkPrefix = InetAddress.getByAddress(networkPrefixBytes).hostAddress
        return "$networkPrefix/64"
    }

    /** Only IP literals are parsed so InetAddress never falls back to a DNS lookup. */
    private fun isIpv6Literal(clientIp: String): Boolean {
        val hasOnlyIpCharacters =
            clientIp.all { it == ':' || it == '.' || it.digitToIntOrNull(16) != null }
        return clientIp.contains(':') && hasOnlyIpCharacters
    }

    private fun createBucket(isAuthEndpoint: Boolean): Bucket {
        // 5 requests per minute if auth endpoint, 100 if not
        val tokens = if (isAuthEndpoint) AUTH_REQUEST_LIMIT else ALL_REQUEST_LIMIT

        val selectedLimit =
            Bandwidth.builder().capacity(tokens).refillGreedy(tokens, Duration.ofMinutes(1)).build()

        return Bucket.builder().addLimit(selectedLimit).build()
    }
}
