package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.DiscordOAuthUser
import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.UserRepository
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.util.RoleConstants
import org.springframework.security.core.context.SecurityContextHolder

@Service
class UserService @Autowired constructor(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository
) {

    fun getOrCreateUser(discordUser: DiscordOAuthUser): User {
       val user = userRepository.findByDiscordId(discordUser.id)
       if (!user.isPresent) {
           val userRole = roleRepository.findByName(RoleConstants.USER_ROLE).get()
           val newUser = User(
               username = discordUser.username,
               displayName = discordUser.username,
               discordId = discordUser.id,
               timeZoneId = 0,
               roles = setOf(userRole),
           )
           return userRepository.save(newUser)
       }
       return user.get()
   }

    fun getUserRoles(userId: UUID): List<String> {
        return userRepository.findById(userId).get().roles.map { it.name }
    }

    fun getCurrentUser(): UserResponse {
        val details = SecurityContextHolder.getContext().authentication.details as Map<*, *>
        val userId = UUID.fromString(details["userId"].toString())
        val user = userRepository.findById(userId).get()
        return mapToUserResponse(user)
    }

    fun updateUser(updateUserRequest: UpdateUserRequest, username: String): UserResponse {
        val userFromDB = userRepository.findByUsername(username)

        if (userFromDB.isPresent) {
            val userToUpdate =
                userFromDB.get().apply {
                    displayName = updateUserRequest.displayName
                    timeZoneId = updateUserRequest.timeZoneId.toInt()
                }
            return mapToUserResponse(userRepository.save(userToUpdate))
        }

        throw UserNotFoundException(username)
    }

    private fun mapToUserResponse(user: User): UserResponse {
        return UserResponse(user.userId, user.displayName, user.timeZoneId)
    }
}
