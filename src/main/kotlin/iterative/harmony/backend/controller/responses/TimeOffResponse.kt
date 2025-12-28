package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.TimeOff
import iterative.harmony.backend.model.dto.TimeOffDto

data class TimeOffResponse(val timeOff: TimeOffDto? = null, val errors: String? = null) {
    companion object {
        fun fromTimeOff(timeOff: TimeOff): TimeOffResponse {
            return TimeOffResponse(timeOff = TimeOffDto.Companion.fromTimeOff(timeOff))
        }
    }
}
