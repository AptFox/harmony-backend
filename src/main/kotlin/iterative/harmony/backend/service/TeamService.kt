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

class TeamImportContext(
    val org: Organization,
    val preExistingTeams: List<Team>,
    val preExistingSkillGroups: List<SkillGroup>,
)

@Service
class TeamService {
    @Autowired private lateinit var teamRepository: TeamRepository
    @Autowired private lateinit var franchiseRepository: FranchiseRepository
    @Autowired private lateinit var teamMapper: TeamMapper
    @Autowired private lateinit var skillGroupService: SkillGroupService

    @Cacheable(value = [FRANCHISE_TEAMS], key = "#franchiseId")
    fun getTeamsForFranchise(franchiseId: Long): List<TeamResponse> {
        val franchise = franchiseRepository.getReferenceById(franchiseId)
        val teams = teamRepository.findAllByFranchise(franchise)
        return teamMapper.toTeamResponseList(teams)
    }

    fun buildImportContext(org: Organization): TeamImportContext {
        val preExistingTeams = getPreExistingTeamsByOrg(org)
        val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
        return TeamImportContext(org, preExistingTeams, preExistingSkillGroups)
    }

    fun import(context: TeamImportContext, batch: MutableList<Team>, row: Map<String, String>) {
        val importedName = row["Franchise"].toString()
        val importedAcronym = row["Code"].toString()
        val importedImageUrl = row["Photo URL"].toString()
        if (importedName.isEmpty() || importedAcronym.isEmpty() || importedImageUrl.isEmpty())
            throw ImportException("Required field is missing")

        val franchiseFromDb =
            franchiseRepository.findByOrganizationAndName(context.org, importedName)
        val franchise =
            if (!franchiseFromDb.isEmpty) {
                franchiseFromDb.get()
            } else {
                franchiseRepository.save(
                    Franchise(
                        organization = context.org,
                        name = importedName,
                        acronym = importedAcronym,
                        imageUrl = importedImageUrl,
                    )
                )
            }

        context.preExistingSkillGroups.forEach { skillGroup ->
            val acronymAndName = "${skillGroup.acronym} $importedName"
            val preExistingTeam =
                context.preExistingTeams.find { team ->
                    team.name == acronymAndName &&
                        team.organization.id == context.org.id &&
                        team.skillGroup.id == skillGroup.id &&
                        team.franchise.id == franchise.id
                }
            if (preExistingTeam == null) {
                batch.add(
                    Team(
                        organization = context.org,
                        skillGroup = skillGroup,
                        franchise = franchise,
                        name = acronymAndName,
                    )
                )
            }
        }
    }

    fun getPreExistingTeamsByOrg(org: Organization): List<Team> {
        return teamRepository.findAllByOrganization(org)
    }

    @Transactional
    fun saveBatch(batch: List<Team>) {
        teamRepository.saveAll(batch)
    }
}
