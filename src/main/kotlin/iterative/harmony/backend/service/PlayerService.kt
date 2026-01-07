package iterative.harmony.backend.service

import iterative.harmony.backend.controller.responses.PlayerMapper
import iterative.harmony.backend.controller.responses.PlayerResponse
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.UserRepository
import java.util.Optional
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerService {
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var playerRepository: PlayerRepository
    @Autowired private lateinit var playerMapper: PlayerMapper

    val NA = "NA"
    val PEND = "Pend"
    val FA = "FA"
    val FP = "FP"
    val WAIVERS = "Waivers"
    val RFA = "RFA"

    val STAFF_POSITIONS_TO_IGNORE = listOf(NA, PEND)
    val FRANCHISES_TO_IGNORE = listOf(FA, FP, WAIVERS, PEND, RFA)

    fun getPlayersForCurrentUser(userId: String): List<PlayerResponse> {
        val userProxy = userRepository.getReferenceById(UUID.fromString(userId))
        val players = playerRepository.findAllByUser(userProxy)
        return players.map(playerMapper::toPlayerResponse)
    }

    suspend fun import(
        org: Organization,
        preExistingTeams: List<Team>,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingFranchises: List<Franchise>,
        batch: MutableList<Player>,
        row: Map<String, String>,
    ) {
        val name = row["name"].toString()
        val memberId = row["member_id"].toString()
        val skillGroupName = row["skill_group"].toString()
        val franchiseName = row["franchise"].toString()
        val staffPos = row["Franchise Staff Position"].toString()
        if (
            name.isEmpty() ||
                memberId.isEmpty() ||
                skillGroupName.isEmpty() ||
                franchiseName.isEmpty() ||
                staffPos.isEmpty()
        )
            throw ImportException("Required field is missing")

        val (skillGroup, team) =
            getTeamBy(
                franchiseName,
                skillGroupName,
                preExistingSkillGroups,
                preExistingFranchises,
                preExistingTeams,
            )
        var user: Optional<User>
        try {
            user = userRepository.findByImportId(memberId)
        } catch (ex: IncorrectResultSizeDataAccessException) {
            throw ImportException("Duplicated import IDs found. Skipping.", ex)
        }
        if (!user.isPresent)
            throw ImportException("Could not find user with import_id to link player to.")

        val teamRole = staffPos.takeIf { it !in STAFF_POSITIONS_TO_IGNORE }

        val preExistingPlayer = playerRepository.findByUserAndOrganization(user.get(), org)
        if (preExistingPlayer.isPresent) {
            val updatedPlayer =
                preExistingPlayer.get().apply {
                    this.name = name
                    this.skillGroup = skillGroup
                    this.team = team
                    this.teamRole = teamRole
                }
            batch.add(updatedPlayer)
        } else {
            val importedPlayer =
                Player(
                    name = name,
                    organization = org,
                    skillGroup = skillGroup,
                    team = team,
                    user = user.get(),
                    teamRole = teamRole,
                )
            batch.add(importedPlayer)
        }
    }

    private suspend fun getTeamBy(
        franchiseName: String,
        skillGroupName: String,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingFranchises: List<Franchise>,
        preExistingTeams: List<Team>,
    ): Pair<SkillGroup, Team?> {
        val skillGroup = preExistingSkillGroups.find { sg -> sg.name == skillGroupName }
        if (skillGroup == null)
            throw ImportException("Could not find SkillGroup with name: $skillGroupName")

        if (FRANCHISES_TO_IGNORE.contains(franchiseName)) return Pair(skillGroup, null)
        val franchise = preExistingFranchises.find { franchise -> franchise.name == franchiseName }
        if (franchise == null)
            throw ImportException("Could not find Franchise with name: $franchiseName")

        val matchingTeam =
            preExistingTeams.find { team ->
                team.skillGroup.id == skillGroup.id && team.franchise.id == franchise.id
            }
        if (matchingTeam == null)
            throw ImportException(
                "Could not find Team with name = $franchiseName and skillGroup = $skillGroupName"
            )

        return Pair(skillGroup, matchingTeam)
    }

    @Transactional
    suspend fun saveBatch(batch: List<Player>) {
        playerRepository.saveAll(batch)
    }
}
