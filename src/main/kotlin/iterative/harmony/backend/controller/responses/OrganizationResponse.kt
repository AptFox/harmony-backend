package iterative.harmony.backend.controller.responses

data class OrganizationResponse(
    var id: Long,
    var name: String,
    var acronym: String?,
    var timeZoneId: String?,
)
