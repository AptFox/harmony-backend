package iterative.harmony.backend.controller

import iterative.harmony.backend.controller.dto.DiscordOAuthUser
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/login/oauth2")
class AuthController (private val tokenService: JwtTokenService, private val userService: UserService) {

//    @GetMapping("/discord")
//    fun handleDiscordRedirect(discordUser: DiscordOAuthUser): Map<String, String> {
//        val user = userService.getOrCreateUser(discordUser)
//        val roles = userService.getUserRoles(user.userId)
//        val jwtToken = tokenService.generateToken(user.userId, roles)
//        return mapOf("harmony_access_token" to jwtToken)
//    }



    //TODO implement refresh token endpoint

    //TODO implement logout
}