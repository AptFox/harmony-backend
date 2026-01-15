package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.TimeOff
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface TimeOffMapper {
    @Mapping(source = "user.userId", target = "userId")
    fun toTimeOffResponse(timeOff: TimeOff): TimeOffResponse

    @Mapping(source = "user.userId", target = "userId")
    fun toTimeOffResponseList(timeOffs: List<TimeOff>): List<TimeOffResponse>
}
