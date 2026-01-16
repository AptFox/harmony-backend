package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.responses.TeamResponse
import iterative.harmony.backend.service.TeamService
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.getLogger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/teams")
@Validated
class TeamsController {
    private val log = getLogger()
    @Autowired private lateinit var teamService: TeamService

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @GetMapping()
    fun getTeamsForFranchise(@RequestParam franchiseId: Long): List<TeamResponse> {
        log.debug("getting teams for franchiseId: $franchiseId")

        return teamService.getTeamsForFranchise(franchiseId)
    }
}
