package iterative.harmony.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication @EnableJpaAuditing class HarmonyBackendApplication

fun main(args: Array<String>) {
    runApplication<HarmonyBackendApplication>(*args)
}
