package iterative.harmony.backend.config

import iterative.harmony.backend.controller.responses.UserResponse
import iterative.harmony.backend.service.AuthService
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.service.UserService
import iterative.harmony.backend.util.RoleConstants.USER_ROLE
import iterative.harmony.backend.util.SecurityConstants.ACCESS_TOKEN_NAME
import iterative.harmony.backend.util.SecurityConstants.REFRESH_TOKEN_NAME
import iterative.harmony.backend.util.Utils
import jakarta.servlet.http.Cookie
import java.util.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.SET_COOKIE
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.util.LinkedMultiValueMap

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired @MockBean private lateinit var authService: AuthService
    @MockBean private lateinit var jwtTokenService: JwtTokenService
    @MockBean private lateinit var userService: UserService
    @Autowired private lateinit var mockMvc: MockMvc
    @Value("\${frontEndBaseUrl}") private lateinit var frontEndBaseUrl: String

    @Test
    @WithMockUser(username = "306420e2-5f30-4070-a5c1-b9961bf10ef4", roles = [USER_ROLE])
    fun `authenticated user can access protected endpoints`() {
        val userId = UUID.fromString("306420e2-5f30-4070-a5c1-b9961bf10ef4")
        val accessTokenString = "valid JWT token"
        val expectedUser =
            UserResponse(
                userId,
                "expectedUser",
                true,
                "America/New_York",
                "123456",
                "1234567",
                listOf(),
            )

        whenever(userService.getCurrentUser(any())).thenReturn(expectedUser)
        val authorities = listOf(SimpleGrantedAuthority(USER_ROLE))
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        val userAgent = "SomeUserAgent"
        val userAgentFingerprint = Utils().generateFingerprint("${userAgent}|127")

        whenever(
                jwtTokenService.getAuthenticationFromAccessToken(
                    accessTokenString,
                    userAgentFingerprint,
                )
            )
            .thenReturn(auth)

        val multiValueMap = LinkedMultiValueMap<String, String>()
        multiValueMap.add("Authorization", "Bearer $accessTokenString")
        multiValueMap.add("User-Agent", userAgent)

        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/user/@me")
                    .contentType("application/json")
                    .headers(HttpHeaders(multiValueMap))
            )
            .andExpect(
                MockMvcResultMatchers.content()
                    .json(
                        """
                    {
                        "userId": "${expectedUser.userId}",
                        "displayName": "${expectedUser.displayName}",
                        "timeZoneId": "${expectedUser.timeZoneId}",
                        "discordId": "${expectedUser.discordId}",
                        "discordAvatarHash": "${expectedUser.discordAvatarHash}"
                    }
                    """
                            .trimIndent()
                    )
            )
    }

    @Nested
    @DisplayName("anonymousUser")
    inner class AnonymousUser() {
        @Test
        @WithAnonymousUser
        fun `anonymous user can access public endpoints`() {
            mockMvc
                .perform(MockMvcRequestBuilders.get("/"))
                .andExpect(MockMvcResultMatchers.status().isNoContent)
        }

        @Test
        @WithAnonymousUser
        fun `anonymous user cannot access protected endpoints`() {
            mockMvc
                .perform(MockMvcRequestBuilders.get("/api/user/@me"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        @WithAnonymousUser
        fun `anonymous user cannot access favicon endpoint`() {
            mockMvc
                .perform(MockMvcRequestBuilders.get("/favicon.ico"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        @WithAnonymousUser
        fun `anonymous user can access error endpoint`() {
            mockMvc
                .perform(MockMvcRequestBuilders.get("/error"))
                .andExpect(MockMvcResultMatchers.status().isInternalServerError)
        }

        @Test
        @WithAnonymousUser
        fun `anonymous user can access logout endpoint`() {
            val refreshTokenCookie = "some_refresh_token"
            val refreshTokenCookieString: String =
                ResponseCookie.from(REFRESH_TOKEN_NAME, refreshTokenCookie).build().toString()

            whenever(authService.throwIfNoRefreshToken(refreshTokenCookie))
                .thenReturn(refreshTokenCookie)
            whenever(authService.generateEmptyRefreshTokenCookie())
                .thenReturn(refreshTokenCookieString)

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/auth/logout")
                        .cookie(Cookie(REFRESH_TOKEN_NAME, refreshTokenCookie))
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(
                    MockMvcResultMatchers.header()
                        .stringValues(SET_COOKIE, refreshTokenCookieString)
                )
        }

        @Test
        @WithAnonymousUser
        fun `anonymous user can access refresh token endpoint`() {
            val refreshTokenCookie = "some_refresh_token"
            val refreshTokenCookieString: String =
                ResponseCookie.from(REFRESH_TOKEN_NAME, refreshTokenCookie).build().toString()

            whenever(authService.throwIfNoRefreshToken(refreshTokenCookie))
                .thenReturn(refreshTokenCookie)
            whenever(authService.rotateTokens(eq(refreshTokenCookie), any()))
                .thenReturn(Pair("someNewAccessToken", refreshTokenCookieString))

            mockMvc
                .perform(
                    MockMvcRequestBuilders.post("/auth/refresh_token")
                        .cookie(Cookie(REFRESH_TOKEN_NAME, refreshTokenCookie))
                )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andExpect(
                    MockMvcResultMatchers.content()
                        .string("{\"$ACCESS_TOKEN_NAME\":\"someNewAccessToken\"}")
                )
        }
    }
}
