package iterative.harmony.backend.config

import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import iterative.harmony.backend.util.setUserIdInLogs
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
        val start = System.nanoTime()
        val requestId = UUID.randomUUID().toString().take(8)
        MDC.put("requestId", requestId)
        try {
            val userId = Utils().getUserIdFromSecurityContext()
            setUserIdInLogs(userId)

            filterChain.doFilter(request, response)
        } finally {
            val requestUri = request.requestURI
            val query = request.queryString?.let { "?$it" } ?: ""
            val requestPath = "$requestUri$query"
            val durationMs = (System.nanoTime() - start) / 1_000_000
            val clientIp =
                request.getHeader("X-Forwarded-For")?.split(",")?.first() ?: request.remoteAddr
            log.info(
                "{} {} {} {}ms requestId={} ip={}",
                request.method,
                requestPath,
                response.status,
                durationMs,
                requestId,
                clientIp,
            )
            MDC.clear() // Clear logging context
        }
    }
}
