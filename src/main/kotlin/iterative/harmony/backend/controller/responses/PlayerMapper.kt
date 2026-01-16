package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface PlayerMapper {
    @Mapping(source = "user.userId", target = "userId")
    fun toPlayerResponse(player: Player): PlayerResponse

    fun toTeamResponse(team: Team?): TeamResponse?

    fun toOrganizationResponse(organization: Organization): OrganizationResponse

    fun toSkillGroupResponse(skillGroup: SkillGroup): SkillGroupResponse

    fun toFranchiseResponse(franchise: Franchise): FranchiseResponse
}
