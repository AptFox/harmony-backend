package iterative.harmony.backend.model

import jakarta.persistence.*

@Entity
@Table(name = "roles")
data class Role (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long,

    @Column(nullable = false, unique = true)
    val name: String,

    @Column(nullable = false)
    val description: String
)