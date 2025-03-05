package iterative.harmony.backend.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DashboardController {

    @RequestMapping("/")
    fun home(): String {
        return "Hello World!"
    }
}
