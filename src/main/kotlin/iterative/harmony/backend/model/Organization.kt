package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "organizations")
data class Organization(val name: String, val acronym: String, val timeZoneId: String?) :
    LongEntity()
