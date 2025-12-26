package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.UserRepository
import java.util.Optional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PlayerService {
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var playerRepository: PlayerRepository

    val NA = "NA"
    val PEND = "Pend"
    val FA = "FA"
    val FP = "FP"
    val WAIVERS = "Waivers"

    val STAFF_POSITIONS_TO_IGNORE = listOf(NA, PEND)
    val FRANCHISES_TO_IGNORE = listOf(FA, FP, WAIVERS, PEND)

    suspend fun import(
        org: Organization,
        preExistingTeams: List<Team>,
        preExistingSkillGroups: List<SkillGroup>,
        batch: MutableList<Player>,
        row: Map<String, String>,
    ) {
        val memberId = row["member_id"].toString()
        val skillGroupName = row["skill_group"].toString()
        val franchise = row["franchise"].toString()
        val staffPos = row["Franchise Staff Position"].toString()
        if (
            memberId.isEmpty() ||
                skillGroupName.isEmpty() ||
                franchise.isEmpty() ||
                staffPos.isEmpty()
        )
            throw ImportException("Required field is missing")

        val (skillGroup, team) =
            getTeamBy(franchise, skillGroupName, preExistingSkillGroups, preExistingTeams)
        var user: Optional<User> = Optional.empty()
        try {
            user = userRepository.findByImportId(memberId)
        } catch (ex: IncorrectResultSizeDataAccessException) {
            throw ImportException("Duplicated import IDs found for: $memberId. Skipping", ex)
        }
        if (!user.isPresent)
            throw ImportException("Could not find user with import_id: $memberId to link player to")

        val teamRole = staffPos.takeIf { it !in STAFF_POSITIONS_TO_IGNORE }

        val preExistingPlayer = playerRepository.findByUser(user.get())
        if (preExistingPlayer.isPresent) {
            val updatedPlayer =
                preExistingPlayer.get().apply {
                    this.skillGroup = skillGroup
                    this.team = team
                    this.teamRole = teamRole
                }
            batch.add(updatedPlayer)
        } else {
            val importedPlayer =
                Player(
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
        franchise: String,
        skillGroupName: String,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingTeams: List<Team>,
    ): Pair<SkillGroup, Team> {
        if (FRANCHISES_TO_IGNORE.contains(franchise)) throw ImportException("Player has no team")

        val skillGroup = preExistingSkillGroups.find { sg -> sg.name == skillGroupName }

        if (skillGroup == null)
            throw ImportException("Could not find SkillGroup with name: $skillGroupName")

        val compositeName = "${skillGroup.acronym} $franchise"
        val matchingTeam = preExistingTeams.find { team -> team.name == compositeName }
        if (matchingTeam == null)
            throw ImportException("Could not find Team with name: $compositeName")

        return Pair(skillGroup, matchingTeam)
    }

    @Transactional
    suspend fun saveBatch(batch: List<Player>) {
        playerRepository.saveAll(batch)
    }
}
