package iterative.harmony.backend.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
// @RequestMapping("/api")
class DashboardController {

    @RequestMapping("/")
    fun home(): String {
        return "Hello World!"
    }

    @GetMapping("/api/dashboard")
    fun dashboard(@AuthenticationPrincipal principal: OAuth2User): String {
        val name = principal.getAttribute<String>("name")
        val email = principal.getAttribute<String>("email")

        return "dashboard"
    }
}
