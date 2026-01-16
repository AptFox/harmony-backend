package iterative.harmony.backend.controller.responses

import java.time.Instant
import java.util.UUID

data class TimeOffResponse(
    var id: Long? = null,
    var userId: UUID? = null,
    var startTime: Instant? = null,
    var endTime: Instant? = null,
    var comment: String? = null,
    var errors: String? = null,
)
