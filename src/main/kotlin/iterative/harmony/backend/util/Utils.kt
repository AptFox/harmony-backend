package iterative.harmony.backend.util

import java.util.*

class Utils {
    /**
     * Returns the current time in milliseconds rounded to the nearest second.
     *
     * @return The current time in milliseconds rounded to the nearest second.
     */
    fun getCurrentTimeInMillisRounded(): Long {
        return ((Date().time + 500) / 1000) * 1000
    }
}
