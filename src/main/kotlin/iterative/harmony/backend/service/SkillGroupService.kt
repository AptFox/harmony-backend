package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ImportException
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
        batch: MutableList<SkillGroup>,
        row: Map<String, String>,
        preExistingSkillGroups: List<SkillGroup>,
    ) {
        val importedName = row["league_name"].toString()
        val importedAcronym = row["league_code"].toString()
        val importedImageUrl = row["league_photo_url"].toString()
        val importedColor = row["color"].toString()
        if (
            importedName.isEmpty() ||
                importedAcronym.isEmpty() ||
                importedImageUrl.isEmpty() ||
                importedColor.isEmpty()
        )
            throw ImportException("Required field is missing")
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
                    this.name = importedSq.name
                    this.acronym = importedSq.acronym
                    this.imageUrl = importedSq.imageUrl
                    this.colorHex = importedSq.colorHex
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
    suspend fun saveBatch(batch: List<SkillGroup>) {
        skillGroupRepository.saveAll(batch)
    }
}
