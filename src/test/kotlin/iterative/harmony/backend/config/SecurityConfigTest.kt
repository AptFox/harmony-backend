package iterative.harmony.backend.config

import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.service.JwtTokenService
import iterative.harmony.backend.service.UserService
import iterative.harmony.backend.util.RoleConstants.USER_ROLE
import java.util.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when` as whenever
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
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

    @MockBean private lateinit var jwtTokenService: JwtTokenService
    @MockBean private lateinit var userService: UserService
    @Autowired private lateinit var mockMvc: MockMvc

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
    @WithMockUser(username = "306420e2-5f30-4070-a5c1-b9961bf10ef4", roles = [USER_ROLE])
    fun `authenticated user can access protected endpoints`() {
        val userId = UUID.fromString("306420e2-5f30-4070-a5c1-b9961bf10ef4")
        val jwtTokenString = "valid JWT token"
        val expectedUser = UserResponse(userId, "expectedUser", 0)

        whenever(userService.getCurrentUser(any())).thenReturn(expectedUser)
        val authorities = listOf(SimpleGrantedAuthority(USER_ROLE))
        val auth = UsernamePasswordAuthenticationToken(userId, null, authorities)
        whenever(jwtTokenService.getAuthentication(jwtTokenString)).thenReturn(auth)

        val multiValueMap = LinkedMultiValueMap<String, String>()
        multiValueMap.add("Authorization", "Bearer " + jwtTokenString)

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
                        "id": "${expectedUser.id}",
                        "displayName": "${expectedUser.displayName}",
                        "timeZoneId": ${expectedUser.timeZoneId}
                    }
                    """
                            .trimIndent()
                    )
            )
    }
}
