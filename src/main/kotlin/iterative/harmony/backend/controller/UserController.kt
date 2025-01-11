package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.dto.CreateUserRequest
import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.service.UserService
import iterative.harmony.backend.util.getLogger
import jakarta.validation.Valid
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController @Autowired constructor(private val userService: UserService) {
    private val log = getLogger()

    @GetMapping("/user/{uuid}")
    fun getUser(@PathVariable uuid: UUID): UserResponse {
        log.info("getting user: $uuid")
        return userService.getUser(uuid)
    }

    @GetMapping("/users")
    fun getAllUsers(): List<UserResponse> {
        log.info("getting all users")
        return userService.getAllUsers()
    }

    @PostMapping("/users")
    fun createNewUser(@Valid @RequestBody request: CreateUserRequest): UserResponse {
        log.info("creating new user from: $request")
        return userService.createUser(request)
    }

    @PutMapping("/user/{uuid}")
    fun updateUser(
        @Valid @RequestBody request: UpdateUserRequest,
        @PathVariable uuid: UUID,
    ): UserResponse {
        log.info("updating user: $uuid")
        return userService.updateUser(request, uuid)
    }

    @DeleteMapping("/user/{uuid}")
    fun deleteUser(@PathVariable uuid: UUID): String {
        log.info("deleting user: $uuid")
        userService.deleteUser(uuid)
        return "User $uuid successfully deleted"
    }
}
