package iterative.harmony.backend.exception

class ScheduledTaskException(message: String, cause: Throwable = RuntimeException(message)) :
    RuntimeException(message, cause)
