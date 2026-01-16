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
    @Query(
        "SELECT u FROM User u LEFT JOIN FETCH u.players p LEFT JOIN FETCH p.organization o WHERE u.userId = :id"
    )
    fun findByIdWithEagerOrgFetch(id: UUID): User?

    fun findByDiscordId(discordId: String): Optional<User>

    fun findByImportId(importId: String): Optional<User>

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLogin WHERE u.userId = :userId")
    fun updateLastLogin(userId: UUID, lastLogin: Instant = Instant.now())
}
