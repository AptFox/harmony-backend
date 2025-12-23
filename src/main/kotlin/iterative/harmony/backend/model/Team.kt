package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "teams")
data class Team(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id") val organization: Organization,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_group_id")
    val skillGroup: SkillGroup,
    var name: String,
    var acronym: String,
    var imageUrl: String,
) : LongEntity()
