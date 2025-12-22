package iterative.harmony.backend.service

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.repository.SkillGroupRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SkillGroupService {
    @Autowired private lateinit var skillGroupRepository: SkillGroupRepository

    suspend fun import(
        org: Organization,
        preExistingSkillGroups: List<SkillGroup>,
        batch: MutableList<SkillGroup>,
        row: Map<String, String>,
    ) {
        val importedSq =
            SkillGroup(
                organization = org,
                name = row["league_name"].toString(),
                acronym = row["league_code"].toString(),
                imageUrl = row["league_photo_url"].toString(),
                colorHex = row["color"].toString(),
            )
        val preExistingSg = preExistingSkillGroups.find { sg -> sg.acronym == importedSq.acronym }
        if (preExistingSg != null) {
            val updatedSg =
                preExistingSg.apply {
                    name = importedSq.name
                    acronym = importedSq.acronym
                    imageUrl = importedSq.imageUrl
                    colorHex = importedSq.colorHex
                }
            batch.add(updatedSg)
        } else {
            batch.add(importedSq)
        }
    }

    suspend fun getPreExistingSkillGroupsByOrg(org: Organization): List<SkillGroup> {
        return skillGroupRepository.findAllByOrganization(org)
    }

    @Transactional
    suspend fun flushBatch(batch: MutableList<SkillGroup>) {
        skillGroupRepository.saveAll(batch)
        batch.clear()
    }
}
