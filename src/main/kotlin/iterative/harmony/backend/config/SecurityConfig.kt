package iterative.harmony.backend.config

import iterative.harmony.backend.service.JwtTokenService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig() {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        customOAuth2UserService: CustomOAuth2UserService,
        jwtTokenService: JwtTokenService,
        corsConfig: CorsConfig,
    ): SecurityFilterChain {
        http
            .sessionManagement { session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                ) // Disables sessions (JSESSIONID)
            }
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
                }
                oauth2.successHandler(OAuth2LoginSuccessHandler(jwtTokenService))
                oauth2.failureHandler { _, response, _ -> response.sendError(401, "Unauthorized") }
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint { _, response, _ ->
                    response.sendError(401, "Unauthorized")
                }
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/", "/login", "/login/oauth2/**", "/error")
                    .permitAll() // Allow public access to certain endpoints
                    .anyRequest()
                    .authenticated() // Protect all other endpoints
            }
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfig.corsConfigurationSource()) }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )

        return http.build()
    }
}
