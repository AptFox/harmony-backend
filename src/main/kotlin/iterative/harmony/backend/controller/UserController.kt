package iterative.harmony.backend.controller

import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.model.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

@RestController
class UserController @Autowired constructor(private val userRepository: UserRepository) {

    private val log = LoggerFactory.getLogger(UserController::class.java)

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

    // TODO: Add validation to requests with @Valid
    @PostMapping("/users")
    fun createNewUser(@RequestBody newUser: User): User {
        log.info("creating new user from: $newUser")
        return userRepository.save(newUser)
    }


}