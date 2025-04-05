package iterative.harmony.backend.repository

import iterative.harmony.backend.model.RefreshToken
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, String> {
    fun findByJti(jti: UUID): Optional<RefreshToken>
}
