package iterative.harmony.backend.config

import iterative.harmony.backend.model.dto.DiscordOAuthUser
import iterative.harmony.backend.service.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class CustomOAuth2UserService : DefaultOAuth2UserService() {

    @Autowired private lateinit var userService: UserService

    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oAuth2User = super.loadUser(userRequest)
        val discordUser = DiscordOAuthUser.fromAttributes(oAuth2User.attributes)
        val user = userService.getOrCreateUser(discordUser)

        val authorities = user.roles.map { SimpleGrantedAuthority(it.name) }

        val attributes =
            mapOf(
                "user_id" to user.userId,
                "discord_id" to user.discordId,
                "display_name" to user.displayName,
                "username" to user.username,
            )

        return DefaultOAuth2User(authorities, attributes, "user_id")
    }
}
