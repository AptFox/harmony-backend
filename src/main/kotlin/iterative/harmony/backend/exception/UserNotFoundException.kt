package iterative.harmony.backend.exception

import java.util.UUID

class UserNotFoundException(userId: UUID) : RuntimeException("Could not find user with ID: $userId")