package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.User
import org.mapstruct.Mapper

@Mapper
interface UserMapper {
    fun toUserResponse(user: User): UserResponse
}
