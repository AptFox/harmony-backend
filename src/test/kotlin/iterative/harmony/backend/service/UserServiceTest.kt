package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.DiscordOAuthUser
import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.Role
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.util.RoleConstants
import java.util.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Mockito.`when` as whenever
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext

@ExtendWith(MockitoExtension::class)
class UserServiceTest {

    @Mock private lateinit var userRepository: UserRepository

    @Mock private lateinit var roleRepository: RoleRepository

    @InjectMocks private lateinit var userService: UserService

    private val uuid: UUID = UUID.fromString("306420e2-5f30-4070-a5c1-b9961bf10ef4")
    private val discordUser = DiscordOAuthUser("1", "username", "globalName")
    private val userRole = Role(1, RoleConstants.USER_ROLE, "The default role for a user")
    private val expectedUser = User(uuid, "username", "username", "1", 0, setOf(userRole))

    @Nested
    @DisplayName("getOrCreateUser")
    inner class GetOrCreateUser() {

        @Nested
        @DisplayName("when called with a new user")
        inner class NewUser() {

            @Test
            fun `should create and return new user`() {
                whenever(userRepository.findByDiscordId(discordUser.id))
                    .thenReturn(Optional.empty())
                whenever(roleRepository.findByName(RoleConstants.USER_ROLE))
                    .thenReturn(Optional.of(userRole))
                whenever(userRepository.save(any())).thenReturn(expectedUser)

                val argumentCaptor: ArgumentCaptor<User> = ArgumentCaptor.captor()
                userService.getOrCreateUser(discordUser)

                verify(userRepository, times(1)).save(argumentCaptor.capture())

                val actualUserArgs = argumentCaptor.value

                assertEquals(actualUserArgs.discordId, expectedUser.discordId)
                assertEquals(actualUserArgs.username, expectedUser.username)
                assertEquals(actualUserArgs.displayName, expectedUser.displayName)
                assertEquals(actualUserArgs.timeZoneId, expectedUser.timeZoneId)
                assertEquals(actualUserArgs.roles, expectedUser.roles)
            }
        }

        @Nested
        @DisplayName("when called with a pre-existing user")
        inner class PreExistingUser() {
            @Test
            fun `should return existing user`() {
                whenever(userRepository.findByDiscordId(discordUser.id))
                    .thenReturn(Optional.of(expectedUser))

                verify(userRepository, never()).save(any())

                val actualUser = userService.getOrCreateUser(discordUser)

                assertEquals(expectedUser, actualUser)
            }
        }
    }

    @Nested
    @DisplayName("getCurrentUser")
    inner class GetCurrentUser() {

        private val securityContext: SecurityContext = mock(SecurityContext::class.java)

        @Test
        fun `should return the current user`() {
            val mockAuth = mock(Authentication::class.java)
            whenever(mockAuth.details).thenReturn(mapOf("userId" to uuid))
            whenever(securityContext.authentication).thenReturn(mockAuth)

            whenever(userRepository.findById(uuid)).thenReturn(Optional.of(expectedUser))
            // assert that the expected userResponse is returned

            val actualUserResponse = userService.getCurrentUser(securityContext)
            val expectedUserResponse =
                UserResponse(uuid, expectedUser.displayName, expectedUser.timeZoneId)

            assertEquals(expectedUserResponse, actualUserResponse)
        }
    }

    @Nested
    @DisplayName("updateUser")
    inner class UpdateUser() {

        private val updateUserRequest = UpdateUserRequest("newUsername", "2")

        @Nested
        @DisplayName("when called with a pre-existing user")
        inner class PreExistingUser() {
            @Test
            fun `should update the user`() {
                val updatedUser =
                    expectedUser.apply {
                        displayName = updateUserRequest.displayName
                        timeZoneId = updateUserRequest.timeZoneId.toInt()
                    }

                whenever(userRepository.findByUsername(expectedUser.username))
                    .thenReturn(Optional.of(expectedUser))
                whenever(userRepository.save(updatedUser)).thenReturn(updatedUser)

                val actual = userService.updateUser(updateUserRequest, expectedUser.username)

                assertEquals(updateUserRequest.displayName, actual.displayName)
                assertEquals(updateUserRequest.timeZoneId.toInt(), actual.timeZoneId)
            }
        }

        @Nested
        @DisplayName("when called with a non-existing user")
        inner class NonExistingUser() {
            @Test
            fun `should throw UserNotFoundException`() {
                whenever(userRepository.findByUsername(expectedUser.username))
                    .thenReturn(Optional.empty())

                assertThrows(UserNotFoundException::class.java) {
                    userService.updateUser(updateUserRequest, expectedUser.username)
                }
            }
        }
    }
}
