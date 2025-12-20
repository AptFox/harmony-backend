package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import java.time.Instant
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByDiscordId(discordId: String): Optional<User>

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLogin WHERE u.userId = :userId")
    fun updateLastLogin(userId: UUID, lastLogin: Instant = Instant.now())
}
