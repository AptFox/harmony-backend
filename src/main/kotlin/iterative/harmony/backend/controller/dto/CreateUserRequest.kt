package iterative.harmony.backend.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/*
* this is a DTO class to represent/validate a request to create a user
*/
data class CreateUserRequest(
    @field:NotBlank(message = "displayName is required")
    @field:Pattern(regexp = "^[a-zA-Z0-9 _@]+$")
    @field:Size(min = 2, max = 50, message = "displayName must be between 2 and 50 characters")
    val displayName: String,

    @field:NotBlank(message = "timeZoneId is required")
    @field:Pattern(regexp = "^[0-9]+$")
    @field:Size(min = 1, max = 2, message = "timeZoneId must be 1-2 characters")
    val timeZoneId: String,
)
