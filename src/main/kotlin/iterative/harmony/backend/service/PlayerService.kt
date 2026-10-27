package iterative.harmony.backend.service

import io.sentry.Sentry
import io.sentry.SentryLevel
import iterative.harmony.backend.controller.responses.PlayerMapper
import iterative.harmony.backend.controller.responses.PlayerResponse
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.exception.PlayerNotFoundException
import iterative.harmony.backend.model.Franchise
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.OrganizationRepository
import iterative.harmony.backend.repository.PlayerRepository
import iterative.harmony.backend.repository.UserRepository
import iterative.harmony.backend.util.getLogger
import java.time.Clock
import java.time.Instant
import java.util.Optional
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class PlayerImportContext(
    val org: Organization,
    val teams: List<Team>,
    val skillGroups: List<SkillGroup>,
    val franchises: List<Franchise>,
    val playerIdsByUserId: Map<UUID, Long>,
) {
    val newPlayersByUserId: MutableMap<UUID, Player> = mutableMapOf()
}

@Service
class PlayerService {
    private val log = getLogger()
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var organizationRepository: OrganizationRepository
    @Autowired private lateinit var playerRepository: PlayerRepository
    @Autowired private lateinit var playerMapper: PlayerMapper
    @Autowired private lateinit var clock: Clock

    val NA = "NA"
    val PEND = "Pend"
    val FA = "FA"
    val FP = "FP"
    val WAIVERS = "Waivers"
    val RFA = "RFA"
    val NO_SLOT = "NONE"
    // TODO: Store the game alongside the org, create a games table
    val IMPORTED_GAME_TITLE = "Rocket League"

    val STAFF_POSITIONS_TO_IGNORE = listOf(NA, PEND)
    val FRANCHISES_TO_IGNORE = listOf(FA, FP, WAIVERS, PEND, RFA)

    // Guards against deleting > 10% of players after a malformed import
    val MAX_STALE_PLAYER_RATE = 0.10

    fun getPlayerForCurrentUserByOrg(orgId: Long, userId: String): PlayerResponse {
        val userProxy = userRepository.getReferenceById(UUID.fromString(userId))
        val orgProxy = organizationRepository.getReferenceById(orgId)
        val player = playerRepository.findByUserAndOrganization(userProxy, orgProxy)
        if (player.isEmpty) throw PlayerNotFoundException(orgId, userId)
        return playerMapper.toPlayerResponse(player.get())
    }

    fun buildImportContext(
        org: Organization,
        preExistingTeams: List<Team>,
        preExistingSkillGroups: List<SkillGroup>,
        preExistingFranchises: List<Franchise>,
    ): PlayerImportContext {
        val playerIdsByUserId =
            playerRepository.findPlayerIdsByOrganization(org).associate { it.userId to it.playerId }
        return PlayerImportContext(
            org,
            preExistingTeams,
            preExistingSkillGroups,
            preExistingFranchises,
            playerIdsByUserId,
        )
    }

    suspend fun import(
        context: PlayerImportContext,
        batch: MutableList<Player>,
        row: Map<String, String>,
    ) {
        if (row.valueOf("game_title") != IMPORTED_GAME_TITLE) return

        val discordId = row.valueOf("discord_id")
        if (discordId.isEmpty()) throw ImportException("Required field is missing")

        var user: Optional<User>
        try {
            user = userRepository.findByDiscordId(discordId)
        } catch (ex: IncorrectResultSizeDataAccessException) {
            throw ImportException("Duplicated discordId found. Skipping.", ex)
        }
        if (!user.isPresent) {
            val errorMsg = "Could not find user with discord_id to link player to. Skipping..."
            log.error("$errorMsg[discord_id: $discordId]")
            throw ImportException(errorMsg)
        }
        val userId = user.get().userId!!

        val name = row.valueOf("name")
        val skillGroupName = row.valueOf("skill_group")
        val franchiseName = row.valueOf("franchise")
        val staffPos = row.valueOf("Franchise Staff Position")
        if (
            name.isEmpty() ||
                skillGroupName.isEmpty() ||
                franchiseName.isEmpty() ||
                staffPos.isEmpty()
        )
            throw ImportException("Required field is missing")

        val (skillGroup, team) = getTeamBy(franchiseName, skillGroupName, context)
        val teamRole = staffPos.takeIf { team != null && it !in STAFF_POSITIONS_TO_IGNORE }
        val slot = row.valueOf("slot").takeIf { team != null && it.isNotEmpty() && it != NO_SLOT }

        val existingPlayerId = context.playerIdsByUserId[userId]
        val player =
            if (existingPlayerId != null) {
                batch.find { it.id == existingPlayerId }
                    ?: playerRepository.findById(existingPlayerId).orElseThrow {
                        ImportException("Player $existingPlayerId was deleted during import")
                    }
            } else {
                context.newPlayersByUserId.getOrPut(userId) {
                    Player(
                        name = name,
                        organization = context.org,
                        skillGroup = skillGroup,
                        team = team,
                        user = user.get(),
                        teamRole = teamRole,
                    )
                }
            }
        player.apply {
            this.name = name
            this.skillGroup = skillGroup
            this.team = team
            this.teamRole = teamRole
            this.slot = slot
            this.salary = row.valueOf("salary").ifEmpty { null }
            this.currentScrimPoints = row.valueOf("current_scrim_points").toIntOrNull()
            this.eligibleThrough = row.valueOf("Eligible Through").ifEmpty { null }
            this.importedAt = clock.instant()
        }
        if (batch.none { it === player }) batch.add(player)
    }

    // Returns ids of players not imported since `lastImportedAt`. Returns nothing if > 10% of the
    // org's players are stale.
    fun findStalePlayers(org: Organization, lastImportedAt: Instant): List<Long> {
        val staleIds = playerRepository.findStalePlayerIds(org, lastImportedAt)
        if (staleIds.isEmpty()) return emptyList()

        val totalCount = playerRepository.countByOrganization(org)
        if (staleIds.size.toDouble() / totalCount > MAX_STALE_PLAYER_RATE) {
            val errorMsg =
                "${org.acronym} - Skipping delete of ${staleIds.size} of $totalCount players not imported since $lastImportedAt"
            log.error(errorMsg)
            Sentry.captureMessage(errorMsg, SentryLevel.ERROR)
            return emptyList()
        }

        log.info(
            "${org.acronym} - Deleting ${staleIds.size} players not imported since $lastImportedAt"
        )
        return staleIds
    }

    @Transactional
    suspend fun deleteBatch(playerIds: List<Long>) {
        if (playerIds.isEmpty()) return
        playerRepository.deleteAllByIdInBatch(playerIds)
    }

    // Returns a null team when no matching team found
    private fun getTeamBy(
        franchiseName: String,
        skillGroupName: String,
        context: PlayerImportContext,
    ): Pair<SkillGroup, Team?> {
        val skillGroup = context.skillGroups.find { sg -> sg.name == skillGroupName }
        if (skillGroup == null)
            throw ImportException("Could not find SkillGroup with name: $skillGroupName")

        if (FRANCHISES_TO_IGNORE.contains(franchiseName)) return Pair(skillGroup, null)
        val franchise = context.franchises.find { franchise -> franchise.name == franchiseName }
        if (franchise == null) {
            log.debug("Could not find Franchise with name: $franchiseName. removing from team.")
            return Pair(skillGroup, null)
        }

        val matchingTeam =
            context.teams.find { team ->
                team.skillGroup.id == skillGroup.id && team.franchise.id == franchise.id
            }
        if (matchingTeam == null)
            log.debug(
                "Could not find Team with name = $franchiseName and skillGroup = $skillGroupName. removing from team."
            )

        return Pair(skillGroup, matchingTeam)
    }

    private fun Map<String, String>.valueOf(key: String): String = this[key]?.trim().orEmpty()

    @Transactional
    suspend fun saveBatch(batch: List<Player>) {
        playerRepository.saveAll(batch)
    }
}
