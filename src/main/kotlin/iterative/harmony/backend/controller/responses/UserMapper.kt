package iterative.harmony.backend.controller.responses

import iterative.harmony.backend.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
interface UserMapper {
    @Mapping(source = "userId", target = "id") fun toUserResponse(user: User): UserResponse
}
