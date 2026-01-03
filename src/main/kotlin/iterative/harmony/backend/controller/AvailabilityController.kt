package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.requests.TimeOffRequest
import iterative.harmony.backend.controller.requests.WeeklyAvailabilitySlotRequest
import iterative.harmony.backend.controller.responses.AvailabilityResponse
import iterative.harmony.backend.controller.responses.TeamAvailabilityResponse
import iterative.harmony.backend.controller.responses.TimeOffResponse
import iterative.harmony.backend.controller.responses.WeeklyAvailabilityResponse
import iterative.harmony.backend.service.AvailabilityService
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.getLogger
import jakarta.validation.Valid
import java.security.Principal
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/availability")
@Validated
class AvailabilityController {
    private val log = getLogger()
    @Autowired private lateinit var availabilityService: AvailabilityService

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @GetMapping("/@me")
    fun getUserAvailability(principal: Principal): AvailabilityResponse {
        log.debug("getting availability and timeOff")

        return availabilityService.getCurrentUserAvailability(principal.name)
    }

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @PostMapping("/weekly")
    fun addWeeklyAvailability(
        @Valid @RequestBody slots: List<WeeklyAvailabilitySlotRequest>,
        principal: Principal,
    ): ResponseEntity<WeeklyAvailabilityResponse> {
        log.debug("setting weekly availability")
        if (slots.isEmpty())
            throw IllegalArgumentException("Request must contain at least 1 WeeklyAvailabilitySlot")
        if (slots.size > 112)
            throw IllegalArgumentException(
                "Request must contain 112 or less WeeklyAvailabilitySlots"
            )

        val response = availabilityService.overwriteWeeklyAvailability(principal.name, slots)
        val status = if (!response.errors.isNullOrEmpty()) HttpStatus.BAD_REQUEST else HttpStatus.OK

        return ResponseEntity.status(status).body(response)
    }

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @GetMapping("/weekly/team/{teamId}")
    fun getTeamAvailability(@PathVariable teamId: Long): ResponseEntity<TeamAvailabilityResponse> {
        val response = availabilityService.getTeamAvailability(teamId)
        val status = if (!response.error.isNullOrEmpty()) HttpStatus.BAD_REQUEST else HttpStatus.OK

        return ResponseEntity.status(status).body(response)
    }

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @DeleteMapping("/weekly")
    fun deleteWeeklyAvailability(principal: Principal): ResponseEntity<Void> {
        availabilityService.deleteWeeklyAvailability(principal.name)
        return ResponseEntity.ok().build()
    }

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @PostMapping("/time_off")
    fun addTimeOff(
        @Valid @RequestBody timeOff: TimeOffRequest,
        principal: Principal,
    ): ResponseEntity<TimeOffResponse> {
        log.debug("setting timeOff")

        val response = availabilityService.addTimeOff(principal.name, timeOff)
        val status = if (!response.errors.isNullOrEmpty()) HttpStatus.BAD_REQUEST else HttpStatus.OK

        return ResponseEntity.status(status).body(response)
    }

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @DeleteMapping("/time_off/{timeOffId}")
    fun deleteTimeOff(
        principal: Principal,
        @PathVariable timeOffId: Long,
    ): ResponseEntity<String?> {
        try {
            availabilityService.deleteTimeOff(principal.name, timeOffId)
        } catch (ex: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(ex.message)
        }
        return ResponseEntity.ok().build()
    }
}
