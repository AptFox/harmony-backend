package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SkillGroupRepository : JpaRepository<SkillGroup, Long>, CrudRepository<SkillGroup, Long> {
    fun findAllByOrganization(organization: Organization): List<SkillGroup>
}
