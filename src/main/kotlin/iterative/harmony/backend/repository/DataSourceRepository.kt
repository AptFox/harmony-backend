package iterative.harmony.backend.repository

import iterative.harmony.backend.model.DataSource
import iterative.harmony.backend.model.Organization
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DataSourceRepository : JpaRepository<DataSource, Long> {
    @EntityGraph(attributePaths = ["organization"])
    fun findAllByOrganizationAndDestinationTableAndEnabled(
        organization: Organization,
        destinationTable: String,
        enabled: Boolean,
    ): List<DataSource>
}
