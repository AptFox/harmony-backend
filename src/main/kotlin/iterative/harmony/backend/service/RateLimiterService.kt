package iterative.harmony.backend.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import java.time.Duration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class RateLimiterService() {
    private val AUTH_REQUEST_LIMIT: Long = 5L
    private val ALL_REQUEST_LIMIT: Long = 100L

    fun getRequestId(request: HttpServletRequest): String {
        val authentication = SecurityContextHolder.getContext()?.authentication
        val user = authentication?.name
        val ip = request.remoteAddr ?: "unknown"
        return user ?: ip
    }

    fun requestIsAllowed(
        requestId: String,
        request: HttpServletRequest,
        requestBuckets: MutableMap<String, Bucket>,
    ): Boolean {
        val isAuthEndpoint = request.requestURI.startsWith("/auth/")
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
