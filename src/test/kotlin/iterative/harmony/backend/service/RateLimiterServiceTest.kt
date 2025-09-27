package iterative.harmony.backend.service

import io.github.bucket4j.Bucket
import iterative.harmony.backend.util.SecurityConstants.ANON_USER_AGENT
import iterative.harmony.backend.util.SecurityConstants.ANON_USER_ID
import iterative.harmony.backend.util.SecurityConstants.AUTH_PREFIX
import iterative.harmony.backend.util.SecurityConstants.NON_AUTH_PREFIX
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

class RateLimiterServiceTest {

    private val rateLimiterService = RateLimiterService()
    private val requestBuckets = mutableMapOf<String, Bucket>()

    @BeforeEach
    fun setup() {
        requestBuckets.clear()
        SecurityContextHolder.clearContext()
    }

    val testUserId = "testUser"
    val testIp = "1.2.3.4"
    val ipBlock = testIp.substringBefore(".")
    val userEndpoint = "/api/user/@me"
    val refreshTokenEndpoint = "/auth/refresh_token"

    @Nested
    @DisplayName("getRequestId")
    inner class GetRequestId() {
        fun setupRequestMock(
            requestIp: String? = testIp,
            uri: String? = userEndpoint,
        ): HttpServletRequest {
            return mock<HttpServletRequest> {
                on { remoteAddr } doReturn requestIp
                on { requestURI } doReturn uri
            }
        }

        @Test
        fun `returns user name if authenticated`() {
            val authenticationMock = mock<Authentication> { on { name } doReturn testUserId }
            val securityContextMock =
                mock<SecurityContext> { on { authentication } doReturn authenticationMock }
            SecurityContextHolder.setContext(securityContextMock)
            val request = setupRequestMock()
            val actual = rateLimiterService.getRequestId(request)
            val expected = "$NON_AUTH_PREFIX|$testUserId|$ANON_USER_AGENT|$ipBlock"
            assertEquals(expected, actual)
        }

        @Test
        fun `returns IP if unauthenticated`() {
            val request = setupRequestMock()
            val actual = rateLimiterService.getRequestId(request)
            val expected = "$NON_AUTH_PREFIX|$ANON_USER_ID|$ANON_USER_AGENT|$ipBlock"
            assertEquals(expected, actual)
        }

        @Test
        fun `returns unknown if no IP and unauthenticated`() {
            val request = setupRequestMock(null)
            val actual = rateLimiterService.getRequestId(request)
            val expected = "$NON_AUTH_PREFIX|$ANON_USER_ID|$ANON_USER_AGENT|0"
            assertEquals(expected, actual)
        }

        @Test
        fun `returns with auth prefix if auth request`() {
            val request = setupRequestMock(uri = refreshTokenEndpoint)
            val actual = rateLimiterService.getRequestId(request)
            val expected = "$AUTH_PREFIX|$ANON_USER_ID|$ANON_USER_AGENT|$ipBlock"
            assertEquals(expected, actual)
        }
    }

    @Nested
    @DisplayName("requestIsAllowed")
    inner class RequestIsAllowed() {

        @Test
        fun `allows up to 5 requests for auth endpoint`() {
            val requestId = "$AUTH_PREFIX|user1"
            repeat(5) { assertTrue(rateLimiterService.requestIsAllowed(requestId, requestBuckets)) }
            // 6th request should be denied
            assertFalse(rateLimiterService.requestIsAllowed(requestId, requestBuckets))
        }

        @Test
        fun `allows up to 100 requests for non-auth endpoint`() {
            val requestId = "$NON_AUTH_PREFIX|user2"
            repeat(100) {
                assertTrue(rateLimiterService.requestIsAllowed(requestId, requestBuckets))
            }
            // 101st request should be denied
            assertFalse(rateLimiterService.requestIsAllowed(requestId, requestBuckets))
        }

        @Test
        fun `creates bucket only once per requestId`() {
            val requestId = "$NON_AUTH_PREFIX|user3"
            rateLimiterService.requestIsAllowed(requestId, requestBuckets)
            val bucket1 = requestBuckets[requestId]
            rateLimiterService.requestIsAllowed(requestId, requestBuckets)
            val bucket2 = requestBuckets[requestId]
            assertSame(bucket1, bucket2)
        }
    }
}
