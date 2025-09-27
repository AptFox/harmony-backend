package iterative.harmony.backend.config

import io.github.bucket4j.Bucket
import io.sentry.Sentry
import iterative.harmony.backend.service.RateLimiterService
import iterative.harmony.backend.util.getLogger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.ConcurrentHashMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class RateLimitFilter : OncePerRequestFilter() {
    private val log = getLogger()
    @Autowired private lateinit var rateLimiterService: RateLimiterService
    private val requestBuckets: MutableMap<String, Bucket> = ConcurrentHashMap()

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
}
