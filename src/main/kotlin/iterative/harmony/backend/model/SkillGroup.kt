package iterative.harmony.backend.model

import iterative.harmony.backend.model.base.LongEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "skill_groups")
data class SkillGroup(
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id") val organization: Organization,
    val name: String,
    val acronym: String,
    val colorHex: String,
    val imageUrl: String,
) : LongEntity()
