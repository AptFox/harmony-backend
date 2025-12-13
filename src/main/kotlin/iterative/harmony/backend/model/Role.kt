package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class Role(
    @Column(nullable = false, unique = true) val name: String,
    @Column(nullable = false) val description: String,
) : LongEntity()
