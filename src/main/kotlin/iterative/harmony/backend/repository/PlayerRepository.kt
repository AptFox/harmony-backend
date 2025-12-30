package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<Player, Long>, CrudRepository<Player, Long> {
    fun findByUser(user: User): Optional<Player>

    fun findAllByUser(user: User): List<Player>

    fun findByUserAndOrganization(user: User, org: Organization): Optional<Player>

    fun findAllByTeam(team: Team): List<Player>
}
