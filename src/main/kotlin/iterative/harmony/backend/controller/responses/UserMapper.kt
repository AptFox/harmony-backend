package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface UserMapper {

    @Mapping(target = "organizations", source = "players")
    fun toUserResponse(user: User): UserResponse

    @Mapping(target = "id", source = "organization.id")
    @Mapping(target = "name", source = "organization.name")
    @Mapping(target = "acronym", source = "organization.acronym")
    @Mapping(target = "timeZoneId", source = "organization.timeZoneId")
    fun toOrganizationResponse(player: Player): OrganizationResponse

    fun toOrganizationResponseList(organizations: List<Organization>): List<OrganizationResponse>
}
