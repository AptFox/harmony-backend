package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.responses.PlayerResponse
import iterative.harmony.backend.service.PlayerService
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.getLogger
import java.security.Principal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/players")
@Validated
class PlayerController {
    private val log = getLogger()
    @Autowired private lateinit var playerService: PlayerService

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @GetMapping("/@me")
    fun getPlayers(@RequestParam orgId: Long, principal: Principal): PlayerResponse {
        log.debug("getting players")
        return playerService.getPlayerForCurrentUserByOrg(orgId, principal.name)
    }
}
