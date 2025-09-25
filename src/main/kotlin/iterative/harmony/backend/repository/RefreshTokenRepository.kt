package iterative.harmony.backend.repository

import iterative.harmony.backend.model.RefreshToken
import java.sql.Timestamp
import java.util.*
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByJti(jti: UUID): Optional<RefreshToken>

    fun countByUserId(userId: UUID): Int

    fun findAllByUserIdOrderByCreatedAtAsc(userId: UUID, limit: Limit): List<RefreshToken>

    fun findAllByUserIdAndCreatedAtBefore(userId: UUID, timestamp: Timestamp): List<RefreshToken>
}
