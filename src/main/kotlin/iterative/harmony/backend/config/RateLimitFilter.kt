package iterative.harmony.backend.config

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.sentry.Sentry
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RateLimitFilter : OncePerRequestFilter() {

    private val buckets: MutableMap<String, Bucket> = ConcurrentHashMap()

    private val AUTH_REQUEST_LIMIT: Long = 5L
    private val ALL_REQUEST_LIMIT: Long = 100L

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val key = resolveKey(request)
        val isAuthEndpoint = request.requestURI.startsWith("/auth/")
        val bucket = buckets.computeIfAbsent(key) { createBucket(isAuthEndpoint) }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            Sentry.captureException(
                Exception("Rate limit triggered, key: $key, endpoint: ${request.requestURI}")
            )
            response.sendError(429, "Too many requests")
        }
    }

    private fun resolveKey(request: HttpServletRequest): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.name
        val ip = request.remoteAddr ?: "unknown"
        return user ?: ip
    }

    private fun createBucket(isAuthEndpoint: Boolean): Bucket {
        val tokens = if (isAuthEndpoint) AUTH_REQUEST_LIMIT else ALL_REQUEST_LIMIT
        // 5 requests per minute if auth endpoint, 100 if not

        val selectedLimit =
            Bandwidth.builder().capacity(tokens).refillGreedy(tokens, Duration.ofMinutes(1)).build()

        return Bucket.builder().addLimit(selectedLimit).build()
    }
}
