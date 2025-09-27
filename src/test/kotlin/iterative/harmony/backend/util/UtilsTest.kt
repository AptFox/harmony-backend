package iterative.harmony.backend.util

import iterative.harmony.backend.util.SecurityConstants.ANON_USER_AGENT
import iterative.harmony.backend.util.SecurityConstants.ANON_USER_ID
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class UtilsTest {

    @Test
    fun `getCurrentTimeInMillisRounded should return current time in milliseconds rounded to the nearest second`() {
        val currentTime = System.currentTimeMillis()
        val actual = Utils().getCurrentTimeInMillisRounded()

        // assert that trailing milliseconds are rounded off
        assertTrue(actual % 1000 == 0L)

        // assert that the returned value is within 1 second of the current time
        assertTrue(actual >= currentTime - 1000 && actual <= currentTime + 1000)
    }

    @Nested
    @DisplayName("getUserAgentFromRequest")
    inner class GetUserAgentFromRequest() {

        @Test
        fun `when User-Agent request header is missing, returns unknown for userAgent`() {
            val request = mock(HttpServletRequest::class.java)
            val ipAddress = "198.168.1.1"

            whenever(request.getHeader("User-Agent")).thenReturn(null)
            whenever(request.getHeader("X-Forwarded-For")).thenReturn(ipAddress)

            val expected = "$ANON_USER_ID|$ANON_USER_AGENT|198"
            val actual = Utils().getUserAgentFromRequest(request)

            assertEquals(expected, actual)
        }

        @Nested
        @DisplayName("when X-Forwarded-For header is missing")
        inner class whenForwardedForHeaderIsMissing() {
            @Test
            fun `returns request remoteAddr`() {
                val request = mock(HttpServletRequest::class.java)
                val ipAddress = null
                val remoteAddr = "178.168.1.1"
                val userAgent = "MySuperCoolComputer"

                whenever(request.getHeader("User-Agent")).thenReturn(userAgent)
                whenever(request.getHeader("X-Forwarded-For")).thenReturn(ipAddress)
                whenever(request.remoteAddr).thenReturn(remoteAddr)

                val expected = "$ANON_USER_ID|${userAgent}|178"
                val actual = Utils().getUserAgentFromRequest(request)

                assertEquals(expected, actual)
            }

            @Test
            fun `when request remoteAddr is missing, returns zeros as IP`() {
                val request = mock(HttpServletRequest::class.java)
                val ipAddress = null
                val remoteAddr = null
                val userAgent = "MySuperCoolComputer"

                whenever(request.getHeader("User-Agent")).thenReturn(userAgent)
                whenever(request.getHeader("X-Forwarded-For")).thenReturn(ipAddress)
                whenever(request.remoteAddr).thenReturn(remoteAddr)

                val expected = "$ANON_USER_ID|${userAgent}|0"
                val actual = Utils().getUserAgentFromRequest(request)

                assertEquals(expected, actual)
            }
        }
    }

    @Test
    fun `generateFingerprint should return a hash of whatever string it's supplied`() {
        val str = "test|127"

        val expected = "vEY_qaH5tmgiQ3p8i-2N2Y4YulOEFhDZbPU-j4v69cI"
        val actual = Utils().generateFingerprint(str)

        assertEquals(expected, actual)
    }
}
