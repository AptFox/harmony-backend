package iterative.harmony.backend

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class UserController @Autowired constructor(private val userRepository: UserRepository) {

//    TODO: move the health and root endpoints elsewhere
    @RequestMapping("/health")
    fun health(): String {
        return "I'm here!";
    }

    @RequestMapping("/")
    fun home(): String {
        return "Hello World!";
    }

    @GetMapping("/users")
    fun getAllUsers(): List<User>{
        return userRepository.findAll();
    }
}