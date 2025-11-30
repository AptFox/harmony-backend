package iterative.harmony.backend.controller.dto

import iterative.harmony.backend.model.AvailabilityException

data class AvailabilityExceptionResponse(
    val exceptions: AvailabilityException? = null,
    val errors: String? = null,
)
