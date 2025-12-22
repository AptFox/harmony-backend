package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "data_sources")
data class DataSource(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id") val organization: Organization,
    var name: String,
    var destinationTable: String,
    var url: String,
    var dataFormat: String,
    var comment: String?,
    var enabled: Boolean,
) : LongEntity()
