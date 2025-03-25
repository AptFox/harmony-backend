package iterative.harmony.backend.model

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "roles")
data class Role(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long,
    @Column(nullable = false, unique = true) val name: String,
    @Column(nullable = false) val description: String,
    @Column(nullable = false, updatable = false, insertable = false)
    val createdAt: Timestamp? = null,
    @Column(nullable = false, updatable = false, insertable = false)
    val updatedAt: Timestamp? = null,
)
