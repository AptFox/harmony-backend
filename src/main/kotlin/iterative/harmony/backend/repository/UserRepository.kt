package iterative.harmony.backend.repository

import iterative.harmony.backend.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository: JpaRepository<User, UUID> {}