package iterative.harmony.backend.service

import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.repository.TeamRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamsService {
    @Autowired private lateinit var teamRepository: TeamRepository

    suspend fun import(
        org: Organization,
        batch: MutableList<Team>,
        row: Map<String, String>,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingTeams: List<Team>,
    ) {
        preExistingSkillGroups.forEach { skillGroup ->
            val importedTeam =
                Team(
                    organization = org,
                    skillGroup = skillGroup,
                    name = "${skillGroup.acronym} ${row["Franchise"].toString()}",
                    acronym = row["Code"].toString(),
                    imageUrl = row["Photo URL"].toString(),
                )
            val preExistingTeam =
                preExistingTeams.find { team ->
                    team.organization == importedTeam.organization &&
                        team.skillGroup == importedTeam.skillGroup
                }
            if (preExistingTeam != null) {
                val updatedTeam =
                    preExistingTeam.apply {
                        name = importedTeam.name
                        acronym = importedTeam.acronym
                        imageUrl = importedTeam.imageUrl
                    }
                batch.add(updatedTeam)
            } else {
                batch.add(importedTeam)
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
