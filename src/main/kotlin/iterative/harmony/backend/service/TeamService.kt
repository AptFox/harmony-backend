package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.repository.TeamRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamService {
    @Autowired private lateinit var teamRepository: TeamRepository

    suspend fun import(
        org: Organization,
        batch: MutableList<Team>,
        row: Map<String, String>,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingTeams: List<Team>,
    ) {
        val importedName = row["Franchise"].toString()
        val importedAcronym = row["Code"].toString()
        val importedImageUrl = row["Photo URL"].toString()
        if (importedName.isEmpty() || importedAcronym.isEmpty() || importedImageUrl.isEmpty())
            throw ImportException("Required field is missing")
        preExistingSkillGroups.forEach { skillGroup ->
            val acronymAndName = "${skillGroup.acronym} $importedName"
            val preExistingTeam =
                preExistingTeams.find { team ->
                    team.name == acronymAndName &&
                        team.organization.id == org.id &&
                        team.skillGroup.id == skillGroup.id
                }
            if (preExistingTeam != null) {
                batch.add(
                    preExistingTeam.apply {
                        name = acronymAndName
                        acronym = importedAcronym
                        imageUrl = importedImageUrl
                    }
                )
            } else {
                batch.add(
                    Team(
                        organization = org,
                        skillGroup = skillGroup,
                        name = acronymAndName,
                        acronym = importedAcronym,
                        imageUrl = importedImageUrl,
                    )
                )
            }
        }
    }

    suspend fun getPreExistingTeamsByOrg(org: Organization): List<Team> {
        return teamRepository.findAllByOrganization(org)
    }

    @Transactional
    suspend fun saveBatch(batch: List<Team>) {
        teamRepository.saveAll(batch)
    }
}
