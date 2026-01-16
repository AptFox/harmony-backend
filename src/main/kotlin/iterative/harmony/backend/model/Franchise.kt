package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "franchises")
data class Franchise(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id") val organization: Organization,
    var name: String,
    var acronym: String,
    var imageUrl: String,
) : LongEntity()
