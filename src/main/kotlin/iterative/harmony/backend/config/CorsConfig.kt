package iterative.harmony.backend.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {
    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(frontEndBaseUrl)
        configuration.allowedMethods = listOf("GET", "POST", "PUT")
        configuration.allowedHeaders =
            listOf("Authorization", "Content-Type", "Cookie", "User-Agent")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
