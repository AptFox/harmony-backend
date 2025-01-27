package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.service.UserService
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.getLogger
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api")
class UserController @Autowired constructor(private val userService: UserService) {
    private val log = getLogger()

    @PreAuthorize("hasRole('${RoleConstants.USER_ROLE}')")
    @GetMapping("/user/@me")
    fun getUser(principal: Principal): UserResponse {
        log.info("getting user: ${principal.name}")

        return userService.getCurrentUser();
    }

    @PutMapping("/user/@me")
    fun updateUser(
        @Valid @RequestBody request: UpdateUserRequest, principal: Principal,
    ): UserResponse {
        log.info("updating user: ${principal.name}")
        return userService.updateUser(request, principal.name)
    }
}
