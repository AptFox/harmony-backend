package iterative.harmony.backend.config

import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InitializeDatabase {

    private val log = LoggerFactory.getLogger(InitializeDatabase::class.java)

    @Bean
    fun initDatabase(userRepo: UserRepository): CommandLineRunner {
        return CommandLineRunner {
            log.info("Initializing database...")
            val user1 = User("some_name", "some_tz")
            val user2 = User("some_name", "some_tz")

            log.info("Preloading database: "+ userRepo.save(user1))
            log.info("Preloading database: "+ userRepo.save(user2))
        }
    }
}