package iterative.harmony.backend

import jakarta.annotation.PostConstruct
import org.springframework.boot.test.context.TestConfiguration

@TestConfiguration
class DisableSentryTestConfig {
    @PostConstruct
    fun disableSentry() {
        io.sentry.Sentry.close() // Ensures no events are sent
    }
}
