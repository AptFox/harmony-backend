package iterative.harmony.backend.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RootController {

    @RequestMapping("/")
    fun home(): ResponseEntity<String> {
        return ResponseEntity.ok().build()
    }
}
