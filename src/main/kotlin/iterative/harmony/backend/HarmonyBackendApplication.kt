package iterative.harmony.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class HarmonyBackendApplication

fun main(args: Array<String>) {
    runApplication<HarmonyBackendApplication>(*args)
}
