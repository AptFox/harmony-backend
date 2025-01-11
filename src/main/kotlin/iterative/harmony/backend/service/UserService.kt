package iterative.harmony.backend.service

import iterative.harmony.backend.controller.dto.CreateUserRequest
import iterative.harmony.backend.controller.dto.UpdateUserRequest
import iterative.harmony.backend.controller.dto.UserResponse
import iterative.harmony.backend.exception.UserNotFoundException
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService @Autowired constructor(private val userRepository: UserRepository){

    fun getUser(userId: UUID): UserResponse {
        val user = userRepository
            .findById(userId)
            .orElseThrow {
                UserNotFoundException(userId)
            }
        return mapToUserResponse(user)
    }

    fun getAllUsers(): List<UserResponse> {
        val users = userRepository.findAll()
        return users.map { user -> mapToUserResponse(user)}
    }

    fun createUser(createUserRequest: CreateUserRequest): UserResponse {
        val newUser = User(
            displayName = createUserRequest.displayName,
            timeZoneId = createUserRequest.timeZoneId.toInt(),
        )
        val savedUser = userRepository.save(newUser)
        return mapToUserResponse(savedUser);
    }

    fun updateUser(updateUserRequest: UpdateUserRequest, userId: UUID): UserResponse {
        val userFromDB = userRepository.findById(userId)

        if (userFromDB.isPresent) {
            val userToUpdate = userFromDB.get().apply {
                displayName = updateUserRequest.displayName
                timeZoneId = updateUserRequest.timeZoneId.toInt()
            }
            return mapToUserResponse(userRepository.save(userToUpdate))
        }

        throw UserNotFoundException(userId)
    }

    fun deleteUser(uuid: UUID) {
        userRepository.deleteById(uuid)
    }

    private fun mapToUserResponse(user: User): UserResponse {
        return UserResponse(
            user.userId,
            user.displayName,
            user.timeZoneId
        )
    }
}