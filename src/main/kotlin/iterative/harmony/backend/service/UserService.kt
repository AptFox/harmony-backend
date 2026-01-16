package iterative.harmony.backend.service

import iterative.harmony.backend.controller.requests.UpdateUserRequest
import iterative.harmony.backend.controller.responses.UserMapper
import iterative.harmony.backend.controller.responses.UserResponse
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.Role
import iterative.harmony.backend.model.User
import iterative.harmony.backend.model.dto.DiscordOAuthUser
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.util.CacheConstants.USER_BY_ID
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.Utils
import iterative.harmony.backend.util.getLogger
import java.util.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService {
    private val log = getLogger()
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var userMapper: UserMapper

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

        // Update user
        val updatedUser =
            user.get().apply {
                this.username = discordUser.username
                this.discordAvatarHash = discordUser.avatarHash
            }
        return userRepository.save(updatedUser)
    }

    fun getCurrentUserRoles(userId: UUID): List<String> {
        val user = userRepository.findById(userId)
        if (!user.isPresent) {
            throw UserNotFoundException(userId.toString())
        }
        return user.get().roles.map { it.name }
    }

    @Cacheable(value = [USER_BY_ID], key = "#userId")
    fun getCurrentUser(userId: String): UserResponse {
        val user = Optional.of(userRepository.findByIdWithEagerOrgFetch(UUID.fromString(userId)))
        if (!user.isPresent) {
            throw UserNotFoundException(userId)
        }
        return userMapper.toUserResponse(user.get())
    }

    @Transactional
    @CachePut(value = [USER_BY_ID], key = "#userId")
    fun updateUser(updateUserRequest: UpdateUserRequest, userId: String): UserResponse {
        Utils().verifyTimeZone(updateUserRequest.timeZoneId)
        val userFromDB = userRepository.findById(UUID.fromString(userId))

        if (userFromDB.isPresent) {
            val userToUpdate =
                userFromDB.get().apply {
                    displayName = updateUserRequest.displayName
                    timeZoneId = updateUserRequest.timeZoneId
                }
            return userMapper.toUserResponse(userRepository.save(userToUpdate))
        }

        throw UserNotFoundException(userId)
    }

    suspend fun import(batch: MutableList<User>, row: Map<String, String>, defaultUserRole: Role) {
        val discordId = row["discord_id"].toString()
        val importId = row["member_id"].toString()
        val name = row["name"].toString()
        if (discordId.isEmpty() || importId.isEmpty() || name.isEmpty())
            throw ImportException("Required field is missing")

        val preExistingUser = userRepository.findByDiscordId(discordId)
        if (preExistingUser.isPresent) {
            val updatedUser =
                preExistingUser.get().apply {
                    this.displayName = name
                    this.importId = importId
                }
            batch.add(updatedUser)
        } else {
            throwIfImportIdAlreadyInDB(importId, discordId)
            val importedUser =
                User(
                    userId = null,
                    username = null,
                    displayName = name,
                    twelveHourClock = true,
                    discordId = discordId,
                    discordAvatarHash = null,
                    timeZoneId = null,
                    roles = setOf(defaultUserRole),
                    importId = importId,
                )
            batch.add(importedUser)
        }
    }

    suspend fun throwIfImportIdAlreadyInDB(importId: String, discordId: String) {
        val preExistingUser = userRepository.findByImportId(importId)
        if (preExistingUser.isPresent) {
            val errorMsg =
                "User has multiple discord accounts in data source (same importId, two discordIds). Skipping... "
            val userId = preExistingUser.get().userId
            log.error("$errorMsg[userId: $userId, importId: $importId, discordId: $discordId]")
            throw ImportException(errorMsg)
        }
    }

    @Transactional
    suspend fun saveBatch(batch: List<User>) {
        userRepository.saveAll(batch)
    }
}
