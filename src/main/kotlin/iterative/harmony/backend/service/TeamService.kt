package iterative.harmony.backend.service

import iterative.harmony.backend.controller.responses.TeamMapper
import iterative.harmony.backend.controller.responses.TeamResponse
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.repository.FranchiseRepository
import iterative.harmony.backend.repository.TeamRepository
import iterative.harmony.backend.util.CacheConstants.FRANCHISE_TEAMS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TeamService {
    @Autowired private lateinit var teamRepository: TeamRepository
    @Autowired private lateinit var franchiseRepository: FranchiseRepository
    @Autowired private lateinit var teamMapper: TeamMapper

    @Cacheable(value = [FRANCHISE_TEAMS], key = "#franchiseId")
    fun getTeamsForFranchise(franchiseId: Long): List<TeamResponse> {
        val franchise = franchiseRepository.getReferenceById(franchiseId)
        val teams = teamRepository.findAllByFranchise(franchise)
        return teamMapper.toTeamResponseList(teams)
    }

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

        val franchiseFromDb = franchiseRepository.findByOrganizationAndName(org, importedName)
        val franchise =
            if (!franchiseFromDb.isEmpty) {
                franchiseFromDb.get()
            } else {
                franchiseRepository.save(
                    Franchise(
                        organization = org,
                        name = importedName,
                        acronym = importedAcronym,
                        imageUrl = importedImageUrl,
                    )
                )
            }

        preExistingSkillGroups.forEach { skillGroup ->
            val acronymAndName = "${skillGroup.acronym} $importedName"
            val preExistingTeam =
                preExistingTeams.find { team ->
                    team.name == acronymAndName &&
                        team.organization.id == org.id &&
                        team.skillGroup.id == skillGroup.id &&
                        team.franchise.id == franchise.id
                }
            // TODO: this logic no longer creates new teams, fix it.
            if (preExistingTeam == null) {
                batch.add(
                    Team(
                        organization = org,
                        skillGroup = skillGroup,
                        franchise = franchise,
                        name = acronymAndName,
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
