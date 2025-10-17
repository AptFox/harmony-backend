package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.DiscordOAuthUser
import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.util.RoleConstants
import java.time.ZoneId
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserService {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository

    fun getOrCreateUser(discordUser: DiscordOAuthUser): User {
        val user = userRepository.findByDiscordId(discordUser.id)
        if (!user.isPresent) {
            val userRole = roleRepository.findByName(RoleConstants.USER_ROLE).get()
            val newUser =
                User(
                    username = discordUser.username,
                    displayName = discordUser.username,
                    discordId = discordUser.id,
                    discordAvatarHash = discordUser.avatarHash,
                    timeZoneId = null,
                    roles = setOf(userRole),
                )
            return userRepository.save(newUser)
        }

        // Update user avatar
        val updatedUser = user.get().apply { discordAvatarHash = discordUser.avatarHash }
        return userRepository.save(updatedUser)
    }

    fun getCurrentUserRoles(userId: UUID): List<String> {
        val user = userRepository.findById(userId)
        if (!user.isPresent) {
            throw UserNotFoundException(userId.toString())
        }
        return user.get().roles.map { it.name }
    }

    fun getCurrentUser(userId: String): UserResponse {
        val user = userRepository.findById(UUID.fromString(userId))
        if (!user.isPresent) {
            throw UserNotFoundException(userId)
        }
        return mapToUserResponse(user.get())
    }

    fun updateUser(updateUserRequest: UpdateUserRequest, userId: String): UserResponse {
        verifyTimeZone(updateUserRequest.timeZoneId)
        val userFromDB = userRepository.findById(UUID.fromString(userId))

        if (userFromDB.isPresent) {
            val userToUpdate =
                userFromDB.get().apply {
                    displayName = updateUserRequest.displayName
                    timeZoneId = updateUserRequest.timeZoneId
                }
            return mapToUserResponse(userRepository.save(userToUpdate))
        }

        throw UserNotFoundException(userId)
    }

    private fun verifyTimeZone(timeZone: String) {
        try {
            ZoneId.of(timeZone)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Invalid timeZoneId supplied", ex)
        }
    }

    private fun mapToUserResponse(user: User): UserResponse {
        return UserResponse(
            user.userId!!,
            user.displayName,
            user.timeZoneId,
            user.discordId,
            user.discordAvatarHash,
        )
    }
}
