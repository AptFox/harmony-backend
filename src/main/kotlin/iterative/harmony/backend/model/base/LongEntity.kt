package iterative.harmony.backend.model.base

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass

@MappedSuperclass
abstract class LongEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null
) : AuditableEntity()
