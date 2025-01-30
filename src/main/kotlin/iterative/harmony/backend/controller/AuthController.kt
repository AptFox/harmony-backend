package iterative.harmony.backend.controller

import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.service.UserService
import org.springframework.web.bind.annotation.RestController

@RestController("/login/oauth2")
class AuthController(
    private val tokenService: JwtTokenService,
    private val userService: UserService,
) {

    // TODO implement refresh token endpoint

    // TODO implement logout
}
