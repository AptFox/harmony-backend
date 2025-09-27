package iterative.harmony.backend.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import iterative.harmony.backend.util.SecurityConstants.AUTH_PREFIX
import iterative.harmony.backend.util.SecurityConstants.NON_AUTH_PREFIX
import iterative.harmony.backend.util.Utils
import jakarta.servlet.http.HttpServletRequest
import java.time.Duration
import org.springframework.stereotype.Service

@Service
class RateLimiterService() {
    private val AUTH_REQUEST_LIMIT: Long = 5L
    private val ALL_REQUEST_LIMIT: Long = 100L

    fun getRequestId(request: HttpServletRequest): String {
        val userId = Utils().getUserIdFromSecurityContext()
        val userAgent = Utils().getUserAgentFromRequest(request)
        val authPrefix =
            if (request.requestURI.startsWith("/$AUTH_PREFIX/")) AUTH_PREFIX else NON_AUTH_PREFIX
        return "${authPrefix}|$userId|${userAgent}"
    }

    fun requestIsAllowed(requestId: String, requestBuckets: MutableMap<String, Bucket>): Boolean {
        val isAuthEndpoint = requestId.startsWith(AUTH_PREFIX)
        val bucket = requestBuckets.computeIfAbsent(requestId) { createBucket(isAuthEndpoint) }
        return bucket.tryConsume(1)
    }

    private fun createBucket(isAuthEndpoint: Boolean): Bucket {
        // 5 requests per minute if auth endpoint, 100 if not
        val tokens = if (isAuthEndpoint) AUTH_REQUEST_LIMIT else ALL_REQUEST_LIMIT

        val selectedLimit =
            Bandwidth.builder().capacity(tokens).refillGreedy(tokens, Duration.ofMinutes(1)).build()

        return Bucket.builder().addLimit(selectedLimit).build()
    }
}
