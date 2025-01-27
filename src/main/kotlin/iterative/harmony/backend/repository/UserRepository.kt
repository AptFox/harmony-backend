package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, UUID>{
    fun findByUsername(username: String): Optional<User>
    fun findByDiscordId(discordId: String): Optional<User>
}
