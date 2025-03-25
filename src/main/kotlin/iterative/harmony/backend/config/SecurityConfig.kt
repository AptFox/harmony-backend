package iterative.harmony.backend.config

import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.util.SecurityConstants.DISCORD_OAUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.ERROR_PATH
import iterative.harmony.backend.util.SecurityConstants.FAVICON_PATH
import iterative.harmony.backend.util.SecurityConstants.LOGOUT_PATH
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import org.springframework.beans.factory.annotation.Value
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

    @Value("\${frontEndBaseUrl}") private var frontEndBaseUrl: String = "http://localhost:3000"

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
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                session.disable()
            }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/",
                        LOGOUT_PATH,
                        REFRESH_TOKEN_PATH,
                        DISCORD_OAUTH_PATH,
                        ERROR_PATH,
                    )
                    .permitAll() // Allow public access to certain endpoints
                    .requestMatchers(FAVICON_PATH)
                    .denyAll() // don't waste resources on /favicon.ico requests
                    .anyRequest()
                    .authenticated() // Protect all other endpoints
            }
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter::class.java,
            )
            .oauth2Login { oauth2 ->
                oauth2.userInfoEndpoint { userInfo ->
                    userInfo.userService(customOAuth2UserService)
                }
                oauth2.successHandler(OAuth2LoginSuccessHandler(jwtTokenService, frontEndBaseUrl))
                oauth2.failureHandler { _, response, _ -> response.sendError(401, "Unauthorized") }
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint { request, response, authException ->
                    response.sendError(401, "Unauthorized")
                }
            }
            .formLogin { formLogin -> formLogin.disable() }
            .logout { logout -> logout.disable() }
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfig.corsConfigurationSource()) }

        return http.build()
    }
}
