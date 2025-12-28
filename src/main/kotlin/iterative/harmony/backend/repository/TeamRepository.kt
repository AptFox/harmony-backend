package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Team
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface TeamRepository : JpaRepository<Team, Long>, CrudRepository<Team, Long> {
    fun findAllByOrganization(organization: Organization): List<Team>
}
