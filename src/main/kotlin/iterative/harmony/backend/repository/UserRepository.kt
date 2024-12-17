package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, UUID>
