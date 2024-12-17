package iterative.harmony.backend.model

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(displayName: String? = null, timeZoneId: String? = null) {
    @Id @GeneratedValue(strategy = GenerationType.UUID) val userId: UUID = UUID.randomUUID()

    var displayName: String? = displayName

    var discordId: Int? = null

    var timeZoneId: String? = timeZoneId

    var playerId: Int? = null

    var roleId: Int? = null
}
