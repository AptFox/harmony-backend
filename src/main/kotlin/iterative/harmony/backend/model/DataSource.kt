package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "data_sources")
data class DataSource(
    val orgId: Long,
    val destinationTable: String,
    val url: String,
    val dataFormat: String,
) : LongEntity()
