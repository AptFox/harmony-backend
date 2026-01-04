package iterative.harmony.backend.controller.responses

data class SkillGroupResponse(
    var id: Long,
    var organization: OrganizationResponse,
    var name: String,
    var acronym: String,
    var imageUrl: String,
    var colorHex: String?,
)
