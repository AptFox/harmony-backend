package iterative.harmony.backend

import jakarta.persistence.Column
import jakarta.persistence.Entity
import java.util.UUID
import jakarta.persistence.Id
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Table
import lombok.Getter
import lombok.Setter

@Entity
@Table(name = "users")
class User(displayName: String? = null, timeZoneId: String? = null) {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Getter
    val userId: UUID = UUID.randomUUID()

    @Getter @Setter @Column(name = "display_name")
    var displayName: String? = displayName

    @Getter @Setter @Column(name = "discord_id")//, nullable = false)
    val discordId: Int? = null

    @Getter @Setter @Column(name = "timezone_id")//, nullable = false)
    var timeZoneId: String? = timeZoneId

    @Getter @Setter @Column(name = "player_id")//, nullable = false)
    val playerId: Int? = null

    @Getter @Setter @Column(name = "role_id")
    val roleId: Int? = null
}