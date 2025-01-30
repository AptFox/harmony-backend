package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): Optional<User>

    fun findByDiscordId(discordId: String): Optional<User>
}
