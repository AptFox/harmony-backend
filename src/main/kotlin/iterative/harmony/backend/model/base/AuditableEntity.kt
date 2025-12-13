package iterative.harmony.backend.model.base

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import java.time.Instant
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class AuditableEntity(
    @CreatedDate
    @Column(nullable = false, updatable = false, insertable = false)
    var createdAt: Instant? = null,
    @LastModifiedDate
    @Column(nullable = false, updatable = false, insertable = false)
    var updatedAt: Instant? = null,
)
