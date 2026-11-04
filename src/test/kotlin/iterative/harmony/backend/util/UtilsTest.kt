package iterative.harmony.backend.util

import iterative.harmony.backend.exception.FingerprintNotInRequestException
import iterative.harmony.backend.util.SecurityConstants.ANON_REQUEST_IP
import iterative.harmony.backend.util.SecurityConstants.CF_CONNECTING_IP_HEADER
import iterative.harmony.backend.util.SecurityConstants.FINGERPRINT_COOKIE_NAME
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockHttpServletRequest

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
    @DisplayName("getClientIp")
    inner class GetClientIp() {
        @Test
        fun `returns the CF-Connecting-IP header when present`() {
            val request = MockHttpServletRequest()
            request.addHeader(CF_CONNECTING_IP_HEADER, "198.168.1.1")
            request.addHeader("X-Forwarded-For", "6.6.6.6")
            request.remoteAddr = "10.0.0.1"

            assertEquals("198.168.1.1", Utils().getClientIp(request))
        }

        @Test
        fun `when CF-Connecting-IP header is missing, returns request remoteAddr`() {
            val request = MockHttpServletRequest()
            request.remoteAddr = "178.168.1.1"

            assertEquals("178.168.1.1", Utils().getClientIp(request))
        }

        @Test
        fun `when request remoteAddr is missing, returns the anonymous IP`() {
            val request = mock(HttpServletRequest::class.java)
            whenever(request.remoteAddr).thenReturn(null)

            assertEquals(ANON_REQUEST_IP, Utils().getClientIp(request))
        }
    }

    @Nested
    @DisplayName("getFingerprintFromRequest")
    inner class GetFingerprintFromRequest() {
        @Test
        fun `returns the value of the fingerprint cookie`() {
            val request = MockHttpServletRequest()
            request.setCookies(Cookie(FINGERPRINT_COOKIE_NAME, "someFingerprint"))

            val actual = Utils().getFingerprintFromRequest(request)

            assertEquals("someFingerprint", actual)
        }

        @Test
        fun `when the fingerprint cookie is missing, throws FingerprintNotInRequestException`() {
            val request = MockHttpServletRequest()

            assertThrows(FingerprintNotInRequestException::class.java) {
                Utils().getFingerprintFromRequest(request)
            }
        }
    }

    @Test
    fun `generateRandomFingerprint should return a different value each time`() {
        val first = Utils().generateRandomFingerprint()
        val second = Utils().generateRandomFingerprint()

        assertNotEquals(first, second)
    }

    @Test
    fun `hashFingerprint should return a hash of whatever string it's supplied`() {
        val str = "test|127"

        val expected = "vEY_qaH5tmgiQ3p8i-2N2Y4YulOEFhDZbPU-j4v69cI"
        val actual = Utils().hashFingerprint(str)

        assertEquals(expected, actual)
    }

    @Test
    fun `verifyTimeZone throws an exception if supplied an invalid time zone`() {
        assertThrows(IllegalArgumentException::class.java, { Utils().verifyTimeZone("taco") })
    }

    @Test
    fun `verifyTimeZone does not throw an exception if supplied a valid time zone`() {
        assertDoesNotThrow({ Utils().verifyTimeZone("America/New_York") })
    }
}
