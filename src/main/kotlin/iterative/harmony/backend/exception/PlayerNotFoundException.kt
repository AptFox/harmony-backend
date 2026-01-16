package iterative.harmony.backend.exception

class PlayerNotFoundException(orgId: Long, userId: String) :
    RuntimeException("Could not find player associated with userId: $userId in org: $orgId")
