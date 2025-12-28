package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "players")
data class Player(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id") val organization: Organization,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_group_id")
    var skillGroup: SkillGroup,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "team_id") var team: Team,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") val user: User,
    var teamRole: String?,
) : LongEntity()
