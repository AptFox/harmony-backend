package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FranchiseRepository : JpaRepository<Franchise, Long> {
    fun findAllByOrganization(organization: Organization): List<Franchise>

    fun findByOrganizationAndName(organization: Organization, name: String): Optional<Franchise>
}
