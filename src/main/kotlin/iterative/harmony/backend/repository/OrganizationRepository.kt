package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Organization
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository interface OrganizationRepository : JpaRepository<Organization, Long>
