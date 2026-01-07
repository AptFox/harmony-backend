package iterative.harmony.backend.controller.responses

data class TeamResponse(
    var id: Long,
    var skillGroup: SkillGroupResponse,
    var franchise: FranchiseResponse,
    var name: String,
)
