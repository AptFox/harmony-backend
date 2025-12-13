package iterative.harmony.backend.controller.dto

import iterative.harmony.backend.model.TimeOff

data class TimeOffResponse(val timeOff: TimeOff? = null, val errors: String? = null)
