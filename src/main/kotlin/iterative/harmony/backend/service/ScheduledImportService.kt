package iterative.harmony.backend.service

import iterative.harmony.backend.exception.DataSourceUnavailableException
import iterative.harmony.backend.exception.ScheduledTaskException
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Player
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.DataSourceRepository
import iterative.harmony.backend.repository.FranchiseRepository
import iterative.harmony.backend.repository.OrganizationRepository
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.util.RoleConstants
import iterative.harmony.backend.util.clearLoggingContext
import iterative.harmony.backend.util.getLogger
import iterative.harmony.backend.util.setScheduledTaskInLogs
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.time.Clock
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.collections.isNotEmpty
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
@ConditionalOnProperty(
    prefix = "feature.scheduled.import",
    name = ["enabled"],
    havingValue = "true",
)
class ScheduledImportService {
    private val log = getLogger()
    @Autowired private lateinit var orgRepository: OrganizationRepository
    @Autowired private lateinit var dataSourceRepository: DataSourceRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var franchiseRepository: FranchiseRepository
    @Autowired private lateinit var csvParsingService: CsvParsingService
    @Autowired private lateinit var skillGroupService: SkillGroupService
    @Autowired private lateinit var teamService: TeamService
    @Autowired private lateinit var userService: UserService
    @Autowired private lateinit var playerService: PlayerService
    @PersistenceContext private lateinit var entityManager: EntityManager
    @Autowired private lateinit var transactionManager: PlatformTransactionManager
    @Autowired private lateinit var csvDownloader: CsvDownloader
    @Autowired private lateinit var importLock: ImportLock
    @Autowired private lateinit var clock: Clock
    private val transactionTemplate by lazy { TransactionTemplate(transactionManager) }
    private val BATCH_SIZE = 100
    // TODO: Figure out how to store these headers in the DB alongside data_sources
    private val MLE_LEAGUES_HEADERS =
        listOf(
            "skill_group_id",
            "league_code",
            "league_name",
            "color",
            "league_photo_url",
            "discord_emoji",
            "max_salary",
            "eligibility_requirement",
        )
    private val MLE_TEAMS_HEADERS =
        listOf(
            "Conference",
            "Super Division",
            "Division",
            "Franchise",
            "Code",
            "Primary Color",
            "Secondary Color",
            "Photo URL",
        )
    private val MLE_MEMBERS_HEADERS =
        listOf("member_id", "name", "mle_id", "mle_player_id", "discord_id")
    private val MLE_PLAYERS_HEADERS =
        listOf(
            "name",
            "salary",
            "sprocket_player_id",
            "member_id",
            "discord_id",
            "skill_group",
            "franchise",
            "Franchise Staff Position",
            "slot",
            "current_scrim_points",
            "Eligible Through",
            "game_title",
        )
    private val SCHEDULED_IMPORT = "SCHEDULED_IMPORT"
    private val DAILY_IMPORT_INTERVAL = Duration.ofHours(24).minusMinutes(5)

    private fun flushHibernateCache(logPrefix: String, batchCode: suspend () -> Unit) {
        transactionTemplate.execute {
            runBlocking { batchCode() }
            log.debug("$logPrefix - Clearing hibernate cache")
            entityManager.flush()
            entityManager.clear()
        }
    }

    private fun runScheduledTask(taskName: String, codeBlock: () -> Unit) {
        setScheduledTaskInLogs(taskName)
        log.info("$taskName started")
        try {
            codeBlock()
        } finally {
            log.info("$taskName stopped")
            clearLoggingContext()
        }
    }

    // Runs every hour. Skill groups and teams import once per DAILY_IMPORT_INTERVAL.
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun scheduledImports() {
        runScheduledTask(SCHEDULED_IMPORT) {
            importLock.runExclusive {
                val failedSteps = mutableListOf<String>()
                val steps: List<Pair<String, suspend () -> Unit>> =
                    listOf(
                        "skill_groups" to { importSkillGroups() },
                        "teams" to { importTeams() },
                        "users" to { importUsers() },
                        "players" to { importPlayers() },
                    )
                runBlocking {
                    steps.forEach { (stepName, step) ->
                        try {
                            step()
                        } catch (ex: DataSourceUnavailableException) {
                            log.error("$stepName import skipped - dataSource unavailable", ex)
                        } catch (ex: Exception) {
                            log.error("$stepName import failed", ex)
                            failedSteps.add(stepName)
                        }
                    }
                }
                if (failedSteps.isNotEmpty())
                    throw ScheduledTaskException("Import steps failed: $failedSteps")
            }
        }
    }

    suspend fun <T> downloadAndImport(
        org: Organization,
        destinationTable: String,
        csvHeaders: List<String>,
        saveBatch: suspend (batch: MutableList<T>) -> Unit,
        minInterval: Duration = Duration.ZERO,
        importCode: suspend (batch: MutableList<T>, csvRow: Map<String, String>) -> Unit,
    ): CsvParsingResult? {
        val dataSources =
            dataSourceRepository.findAllByOrganizationAndDestinationTableAndEnabled(
                org,
                destinationTable,
                true,
            )
        var combinedResult: CsvParsingResult? = null
        dataSources.forEach { dataSource ->
            val dataFormat = dataSource.dataFormat
            if (dataFormat != "csv")
                throw ScheduledTaskException(
                    "Only CSV import is supported, import called with $dataFormat"
                )
            val orgAcronym = org.acronym
            val logPrefix = "$orgAcronym - $destinationTable import"
            val importStartedAt = clock.instant()
            val lastImportedAt = dataSource.lastImportedAt
            if (
                lastImportedAt != null && lastImportedAt.plus(minInterval).isAfter(importStartedAt)
            ) {
                log.info("$logPrefix skipped, last imported at $lastImportedAt")
                return@forEach
            }
            log.info("$logPrefix started")
            val dataSourceName = dataSource.name.replace(" ", "-")
            val tempFile =
                csvDownloader.downloadToTempFile(
                    url = dataSource.url,
                    prefix = "${orgAcronym}-${dataSourceName}",
                    suffix = ".${dataFormat}",
                )
            try {
                val batch = mutableListOf<T>()
                val result =
                    csvParsingService.parseCsvStream(tempFile, csvHeaders) { csvRow ->
                        importCode(batch, csvRow)
                        if (batch.size >= BATCH_SIZE) {
                            log.debug("$logPrefix - Saving batch of $BATCH_SIZE")
                            flushHibernateCache(logPrefix) { saveBatch(batch) }
                            batch.clear()
                        }
                    }
                if (batch.isNotEmpty()) {
                    log.debug("$logPrefix - Saving batch of ${batch.size}")
                    flushHibernateCache(logPrefix) { saveBatch(batch) }
                }
                dataSource.lastImportedAt = importStartedAt
                transactionTemplate.execute { dataSourceRepository.save(dataSource) }
                combinedResult =
                    CsvParsingResult(
                        (combinedResult?.rowsProcessed ?: 0) + result.rowsProcessed,
                        (combinedResult?.errorCount ?: 0) + result.errorCount,
                    )
                log.info("$logPrefix - succeeded")
            } catch (ex: DataIntegrityViolationException) {
                log.error("$logPrefix - error saving to DB: ${ex.message}", ex)
                throw ScheduledTaskException("$logPrefix failed", ex)
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
        return combinedResult
    }

    suspend fun importSkillGroups() {
        val saveBatch: suspend (batch: List<SkillGroup>) -> Unit = { batch ->
            skillGroupService.saveBatch(batch)
        }
        orgRepository.findAll().forEach { org ->
            try {
                val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
                downloadAndImport(
                    org,
                    "skill_groups",
                    MLE_LEAGUES_HEADERS,
                    saveBatch,
                    DAILY_IMPORT_INTERVAL,
                ) { batch, csvRow ->
                    skillGroupService.import(org, batch, csvRow, preExistingSkillGroups)
                }
            } catch (ex: Exception) {
                log.error("SkillGroup import failed", ex)
                throw ex
            }
        }
    }

    suspend fun importTeams() {
        val saveBatch: suspend (batch: List<Team>) -> Unit = { batch ->
            teamService.saveBatch(batch)
        }
        orgRepository.findAll().forEach { org ->
            try {
                val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
                val preExistingTeams = teamService.getPreExistingTeamsByOrg(org)

                downloadAndImport(
                    org,
                    "teams",
                    MLE_TEAMS_HEADERS,
                    saveBatch,
                    DAILY_IMPORT_INTERVAL,
                ) { batch, csvRow ->
                    teamService.import(org, batch, csvRow, preExistingSkillGroups, preExistingTeams)
                }
            } catch (ex: Exception) {
                log.error("${org.acronym} Team import failed", ex)
                throw ex
            }
        }
    }

    suspend fun importUsers() {
        val saveBatch: suspend (batch: List<User>) -> Unit = { batch ->
            userService.saveBatch(batch)
        }
        val defaultUserRole = roleRepository.findByName(RoleConstants.USER_ROLE).get()
        orgRepository.findAll().forEach { org ->
            try {
                downloadAndImport(org, "users", MLE_MEMBERS_HEADERS, saveBatch) { batch, csvRow ->
                    userService.import(batch, csvRow, defaultUserRole)
                }
            } catch (ex: Exception) {
                log.error("${org.acronym} User import failed", ex)
                throw ex
            }
        }
    }

    suspend fun importPlayers() {
        val saveBatch: suspend (batch: List<Player>) -> Unit = { batch ->
            playerService.saveBatch(batch)
        }
        orgRepository.findAll().forEach { org ->
            try {
                val preExistingTeams = teamService.getPreExistingTeamsByOrg(org)
                val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
                val preExistingFranchises = franchiseRepository.findAllByOrganization(org)
                val context =
                    playerService.buildImportContext(
                        org,
                        preExistingTeams,
                        preExistingSkillGroups,
                        preExistingFranchises,
                    )
                val result =
                    downloadAndImport(org, "players", MLE_PLAYERS_HEADERS, saveBatch) {
                        batch,
                        csvRow ->
                        playerService.import(context, batch, csvRow)
                    }
                if (result != null) {
                    val missingPlayerIds =
                        playerService.findPlayersMissingFromImport(context, result)
                    if (missingPlayerIds.isNotEmpty())
                        flushHibernateCache("${org.acronym} - players delete") {
                            playerService.deleteBatch(missingPlayerIds)
                        }
                }
            } catch (ex: Exception) {
                log.error("${org.acronym} Player import failed", ex)
                throw ex
            }
        }
    }
}
