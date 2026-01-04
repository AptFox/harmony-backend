package iterative.harmony.backend.controller.responses

data class TeamResponse(
    var id: Long,
    var organization: OrganizationResponse,
    var skillGroup: SkillGroupResponse,
    var name: String,
    var acronym: String,
    var imageUrl: String,
)
