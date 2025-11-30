package iterative.harmony.backend.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalTime

data class WeeklyAvailabilitySlotRequest(
    @field:NotBlank(message = "dayOfWeek is required")
    @field:Pattern(
        regexp = "^[a-zA-Z]+$",
        message = "dayOfWeek must be in 3 character format (ex: 'Tue')",
    )
    @field:Size(min = 3, max = 3, message = "dayOfWeek must be in 3 character format (ex: 'Tue')")
    val dayOfWeek: String,
    @field:NotNull(message = "startTime is required") val startTime: LocalTime,
    @field:NotNull(message = "endTime is required") val endTime: LocalTime,
    @field:NotBlank(message = "timeZoneId is required")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9/\\-_]+$",
        message = "timeZoneId must be in a valid IANA format (ex: 'America/New_York').",
    )
    @field:Size(
        min = 2,
        message = "timeZoneId must be in a valid IANA format (ex: 'America/New_York').",
    )
    val timeZoneId: String,
)
