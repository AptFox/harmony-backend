package iterative.harmony.backend.exception

class DataSourceUnavailableException(
    message: String,
    cause: Throwable = RuntimeException(message),
) : RuntimeException(message, cause)
