package iterative.harmony.backend.controller.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

data class TimeOffRequest(
    @field:NotNull(message = "startTime is required")
    @field:Future(message = "startTime must be in future")
    val startTime: Instant,
    @field:NotNull(message = "endTime is required")
    @field:Future(message = "endTime must be in the future")
    val endTime: Instant,
    @field:Pattern(
        regexp = "^[a-zA-Z0-9\\s.,']+$",
        message = "comments can only be alpha-numeric with spaces",
    )
    @field:Size(max = 255, message = "comment must not exceed 255 characters")
    val comment: String?,
)
