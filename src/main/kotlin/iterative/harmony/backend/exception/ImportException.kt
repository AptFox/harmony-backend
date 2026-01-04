package iterative.harmony.backend.exception

class ImportException(message: String, cause: Throwable = RuntimeException(message)) :
    RuntimeException(message, cause)
