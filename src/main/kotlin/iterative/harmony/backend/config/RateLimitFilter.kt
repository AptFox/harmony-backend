package iterative.harmony.backend.config

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bucket
import io.sentry.Sentry
import iterative.harmony.backend.service.RateLimiterService
import iterative.harmony.backend.util.getLogger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val log = getLogger()
    @Autowired private lateinit var rateLimiterService: RateLimiterService
    private val requestBuckets: MutableMap<String, Bucket> =
        Caffeine.newBuilder()
            .expireAfterAccess(IDLE_BUCKET_EXPIRATION)
            .build<String, Bucket>()
            .asMap()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = rateLimiterService.getRequestId(request)
        if (rateLimiterService.requestIsAllowed(requestId, requestBuckets)) {
            filterChain.doFilter(request, response)
        } else {
            val msg = "Rate limit triggered, key: $requestId, endpoint: ${request.requestURI}"
            log.info(msg)
            Sentry.captureException(Exception(msg))
            response.sendError(429, "Too many requests")
        }
    }

    companion object {
        val IDLE_BUCKET_EXPIRATION: Duration = Duration.ofMinutes(5)
    }
}
