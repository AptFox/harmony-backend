package iterative.harmony.backend.model.base

import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import java.util.UUID
import org.hibernate.annotations.UuidGenerator

@MappedSuperclass
abstract class UuidEntity(@Id @UuidGenerator val id: UUID? = null) : AuditableEntity()
