package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.Team
import org.mapstruct.Mapper

@Mapper
interface TeamMapper {
    fun toTeamResponse(team: Team): TeamResponse

    fun toTeamResponseList(teams: List<Team>): List<TeamResponse>
}
