package iterative.harmony.backend.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

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
}
