package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.User
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<Player, Long>, CrudRepository<Player, Long> {
    fun findByUser(user: User): Optional<Player>
}
