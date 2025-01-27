package iterative.harmony.backend.exception

class UserNotFoundException(username: String) :
    RuntimeException("Could not find user with ID: $username")
