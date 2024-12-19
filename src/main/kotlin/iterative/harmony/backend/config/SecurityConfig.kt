package iterative.harmony.backend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/api/login", "/error")
                    .permitAll() // Allow public access to certain endpoints
                    .anyRequest()
                    .authenticated() // Protect all other endpoints
            }
            .oauth2Login { oauth2 ->
                oauth2.defaultSuccessUrl("/api/dashboard", true) // Redirect to dashboard after login
            }

        return http.build()
    }
}
