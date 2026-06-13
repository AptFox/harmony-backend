package iterative.harmony.backend.config

import iterative.harmony.backend.util.LogConstants.CLIENT_IP
import iterative.harmony.backend.util.LogConstants.REQUEST_DURATION_IN_MS
import iterative.harmony.backend.util.LogConstants.REQUEST_ID
import iterative.harmony.backend.util.LogConstants.REQUEST_METHOD
import iterative.harmony.backend.util.LogConstants.REQUEST_PATH
import iterative.harmony.backend.util.LogConstants.RESPONSE_STATUS
import iterative.harmony.backend.util.LogConstants.USER_ID
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.clearLoggingContext
import iterative.harmony.backend.util.getLogger
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.UUID
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class LoggingFilter : OncePerRequestFilter() {

    private val log = getLogger()

    private val ignoredExactPaths =
        setOf("/robots.txt", "/favicon.ico", "/.env", "/.git", "/.git/config")

    private val ignoredPathPrefixes =
        listOf("/.git/", "/.well-known/", "/wp-", "/phpmyadmin", "/phpinfo.php")

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI

        if (path in ignoredExactPaths && request.method == "GET") return true

        return request.method == "GET" && ignoredPathPrefixes.any { path.startsWith(it) }
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        logRequestMetrics(request)
        val requestStartTime = System.nanoTime()
        try {
            filterChain.doFilter(request, response)
        } finally {
            val requestDurationMs = (System.nanoTime() - requestStartTime) / 1_000_000
            log.info("$REQUEST_DURATION_IN_MS = $requestDurationMs")
            log.info("$RESPONSE_STATUS = ${response.status}")
            clearLoggingContext()
        }
    }

    fun logRequestMetrics(request: HttpServletRequest) {
        val requestId = UUID.randomUUID().toString().take(8)
        val requestUri = request.requestURI
        val query = request.queryString?.let { "?$it" } ?: ""
        val requestPath = "$requestUri$query"
        val clientIp =
            request.getHeader("X-Forwarded-For")?.split(",")?.first() ?: request.remoteAddr
        val userId = Utils().getUserIdFromSecurityContext()
        val userIdPrefix = userId.take(8)
        MDC.put(USER_ID, userIdPrefix)
        MDC.put(REQUEST_ID, requestId)
        MDC.put(REQUEST_METHOD, request.method)
        MDC.put(REQUEST_PATH, requestPath)
        MDC.put(CLIENT_IP, clientIp)
    }
}
