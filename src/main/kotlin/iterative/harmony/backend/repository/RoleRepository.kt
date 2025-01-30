package iterative.harmony.backend.repository

import iterative.harmony.backend.model.Role
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>
}
