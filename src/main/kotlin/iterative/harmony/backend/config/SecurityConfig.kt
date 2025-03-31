package iterative.harmony.backend.config

import iterative.harmony.backend.util.SecurityConstants.DISCORD_OAUTH_PATH
import iterative.harmony.backend.util.SecurityConstants.ERROR_PATH
import iterative.harmony.backend.util.SecurityConstants.FAVICON_PATH
import iterative.harmony.backend.util.SecurityConstants.LOGOUT_PATH
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_PATH
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Autowired private lateinit var oAuth2LoginSuccessHandler: OAuth2LoginSuccessHandler
    @Autowired private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @Autowired private lateinit var customOAuth2UserService: CustomOAuth2UserService
    @Autowired private lateinit var corsConfig: CorsConfig

    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                session.disable()
            }
            .headers { headers ->
                headers.frameOptions { frameOptions -> frameOptions.deny() }
                headers.contentSecurityPolicy { csp ->
                    csp.policyDirectives(
                        "default-src 'none'; " + "connect-src 'self' ${frontEndBaseUrl};"
                    )
                }
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
                oauth2.successHandler(oAuth2LoginSuccessHandler)
                oauth2.failureHandler { _, response, _ -> response.sendError(401, "Unauthorized") }
            }
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .formLogin { formLogin -> formLogin.disable() }
            .logout { logout -> logout.disable() }
            .csrf { csrf -> csrf.disable() }
            .cors { cors -> cors.configurationSource(corsConfig.corsConfigurationSource()) }

        return http.build()
    }
}
