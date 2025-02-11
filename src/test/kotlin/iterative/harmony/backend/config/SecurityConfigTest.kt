package iterative.harmony.backend.config

import iterative.harmony.backend.controller.dto.UserResponse
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
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockBean private lateinit var userService: UserService

    @Test
    @WithAnonymousUser
    fun `anonymous user can access public endpoints`() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/"))
            .andExpect(MockMvcResultMatchers.status().isOk)
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
        val expectedUser = UserResponse(userId, "expectedUser", 0)

        whenever(userService.getCurrentUser(any())).thenReturn(expectedUser)

        mockMvc
            .perform(MockMvcRequestBuilders.get("/api/user/@me"))
            .andExpect(MockMvcResultMatchers.status().isOk)
    }
}
