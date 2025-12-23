package iterative.harmony.backend.service

import iterative.harmony.backend.controller.requests.UpdateUserRequest
import iterative.harmony.backend.controller.responses.UserResponse
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.dto.DiscordOAuthUser
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.Utils
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository

    @Transactional
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
        return UserResponse.fromUser(user.get())
    }

    @Transactional
    fun updateUser(updateUserRequest: UpdateUserRequest, userId: String): UserResponse {
        Utils().verifyTimeZone(updateUserRequest.timeZoneId)
        val userFromDB = userRepository.findById(UUID.fromString(userId))

        if (userFromDB.isPresent) {
            val userToUpdate =
                userFromDB.get().apply {
                    displayName = updateUserRequest.displayName
                    timeZoneId = updateUserRequest.timeZoneId
                }
            return UserResponse.fromUser(userRepository.save(userToUpdate))
        }

        throw UserNotFoundException(userId)
    }

    @Transactional
    suspend fun import(row: Map<String, String>) {
        val discordId = row["discord_id"].toString()
        val memberId = row["member_id"].toString()
        val name = row["name"].toString()
        if (discordId.isEmpty() || memberId.isEmpty() || name.isEmpty()) return

        val userRole = roleRepository.findByName(RoleConstants.USER_ROLE).get()
        val preExistingUser = userRepository.findByDiscordId(discordId)
        if (preExistingUser.isPresent) {
            val updatedUser =
                preExistingUser.get().apply {
                    displayName = name
                    importId = memberId
                }
            userRepository.save(updatedUser)
        } else {
            val importedUser =
                User(
                    userId = null,
                    username = name,
                    displayName = name,
                    twelveHourClock = true,
                    discordId = discordId,
                    discordAvatarHash = null,
                    timeZoneId = null,
                    roles = setOf(userRole),
                    importId = memberId,
                )
            userRepository.save(importedUser)
        }
    }
}
