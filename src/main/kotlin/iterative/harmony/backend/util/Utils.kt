package iterative.harmony.backend.util

import java.util.*

class Utils {
    fun getCurrentTimeInMillisRounded(): Long {
        return ((Date().time + 500) / 1000) * 1000
    }
}
