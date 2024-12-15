package iterative.harmony.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@RestController
class Backend {
	@RequestMapping("/health")
	fun health(): String {
		return "I'm here!";
	}

	@RequestMapping("/")
	fun home(): String {
		return "Hello World!";
	}
}

fun main(args: Array<String>) {
	runApplication<Backend>(*args)
}