package iterative.harmony.backend.controller

import org.springframework.core.env.Environment
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController(private val env: Environment) {

    @RequestMapping("/")
    fun home(): Map<String, String> {
        val name = env.getProperty("spring.application.name") ?: "Unknown"
        return mapOf("name" to name)
    }
}
