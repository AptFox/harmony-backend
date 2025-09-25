package iterative.harmony.backend.service

import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder

@ExtendWith(MockitoExtension::class)
@SpringBootTest
class RateLimiterServiceTest {

    @InjectMocks private val rateLimiterService = RateLimiterService()
    private val requestBuckets = mutableMapOf<String, Bucket>()

    @BeforeEach
    fun setup() {
        requestBuckets.clear()
        SecurityContextHolder.clearContext()
    }

    val testUserId = "testUser"
    val testIp = "1.2.3.4"

    @Nested
    @DisplayName("getRequestId")
    inner class GetRequestId() {
        fun setupRequestMock(requestIp: String? = testIp): HttpServletRequest {
            return mock<HttpServletRequest> { on { remoteAddr } doReturn requestIp }
        }

        @Test
        fun `returns user name if authenticated`() {
            val authenticationMock = mock<Authentication> { on { name } doReturn testUserId }
            val securityContextMock =
                mock<SecurityContext> { on { authentication } doReturn authenticationMock }
            SecurityContextHolder.setContext(securityContextMock)
            val request = mock<HttpServletRequest> { on { remoteAddr } doReturn testIp }
            val result = rateLimiterService.getRequestId(request)
            assertEquals(testUserId, result)
        }

        @Test
        fun `returns IP if unauthenticated`() {
            val request = setupRequestMock()
            val result = rateLimiterService.getRequestId(request)
            assertEquals(testIp, result)
        }

        @Test
        fun `returns unknown if no IP and unauthenticated`() {
            val request = setupRequestMock(null)
            val result = rateLimiterService.getRequestId(request)
            assertEquals("unknown", result)
        }
    }

    @Nested
    @DisplayName("requestIsAllowed")
    inner class RequestIsAllowed() {
        fun setupRequestMock(uri: String): HttpServletRequest {
            return mock<HttpServletRequest> { on { requestURI } doReturn uri }
        }

        @Test
        fun `allows up to 5 requests for auth endpoint`() {
            val request = setupRequestMock("/auth/refresh")
            val requestId = "user1"
            repeat(5) {
                assertTrue(rateLimiterService.requestIsAllowed(requestId, request, requestBuckets))
            }
            // 6th request should be denied
            assertFalse(rateLimiterService.requestIsAllowed(requestId, request, requestBuckets))
        }

        @Test
        fun `allows up to 100 requests for non-auth endpoint`() {
            val request = setupRequestMock("/api/user/@me")
            val requestId = "user2"
            repeat(100) {
                assertTrue(rateLimiterService.requestIsAllowed(requestId, request, requestBuckets))
            }
            // 101st request should be denied
            assertFalse(rateLimiterService.requestIsAllowed(requestId, request, requestBuckets))
        }

        @Test
        fun `creates bucket only once per requestId`() {
            val request = setupRequestMock("/api/user/@me")
            val requestId = "user3"
            rateLimiterService.requestIsAllowed(requestId, request, requestBuckets)
            val bucket1 = requestBuckets[requestId]
            rateLimiterService.requestIsAllowed(requestId, request, requestBuckets)
            val bucket2 = requestBuckets[requestId]
            assertSame(bucket1, bucket2)
        }
    }
}
