package iterative.harmony.backend.service

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
import iterative.harmony.backend.util.getLogger
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import java.io.File
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.collections.isNotEmpty
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.reactive.function.client.WebClient

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
    private val transactionTemplate by lazy { TransactionTemplate(transactionManager) }
    private val webClient = WebClient.builder().build()
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
        )

    private fun flushHibernateCache(logPrefix: String, batchCode: suspend () -> Unit) {
        transactionTemplate.execute {
            runBlocking { batchCode() }
            log.debug("$logPrefix - Clearing hibernate cache")
            entityManager.flush()
            entityManager.clear()
        }
    }

    // Runs every hour
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun hourlyImports() {
        MDC.put("userId", "HOURLY_IMPORT")
        log.info("Hourly - Scheduled Import started")
        runBlocking {
            importUsers()
            importPlayers()
        }
        log.info("Hourly - Scheduled Import stopped")
    }

    // Runs once a day
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    fun dailyScheduleTasks() {
        MDC.put("userId", "DAILY_IMPORT")
        log.info("Daily - Scheduled Import started")
        runBlocking {
            importSkillGroups()
            importTeams()
        }
        log.info("Daily - Scheduled Import stopped")
    }

    private suspend fun downloadToTempFile(url: String, prefix: String, suffix: String): File {
        val tempFile = File.createTempFile(prefix, suffix)
        val flux =
            webClient
                .get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer::class.java)

        DataBufferUtils.write(flux, tempFile.toPath(), StandardOpenOption.CREATE)
            .awaitSingleOrNull()

        return tempFile
    }

    suspend fun <T> downloadAndImport(
        org: Organization,
        destinationTable: String,
        csvHeaders: List<String>,
        saveBatch: suspend (batch: MutableList<T>) -> Unit,
        importCode: suspend (batch: MutableList<T>, csvRow: Map<String, String>) -> Unit,
    ) {
        val dataSources =
            dataSourceRepository.findAllByOrganizationAndDestinationTableAndEnabled(
                org,
                destinationTable,
                true,
            )
        dataSources.forEach { dataSource ->
            val dataFormat = dataSource.dataFormat
            if (dataFormat != "csv")
                throw ScheduledTaskException(
                    "Only CSV import is supported, import called with $dataFormat"
                )
            val orgAcronym = org.acronym
            val logPrefix = "$orgAcronym - $destinationTable import"
            log.info("$logPrefix started")
            val dataSourceName = dataSource.name.replace(" ", "-")
            val tempFile =
                downloadToTempFile(
                    url = dataSource.url,
                    prefix = "${orgAcronym}-${dataSourceName}",
                    suffix = ".${dataFormat}",
                )
            try {
                val batch = mutableListOf<T>()
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
                log.info("$logPrefix - succeeded")
            } catch (ex: DataIntegrityViolationException) {
                log.error("$logPrefix - error saving to DB: ${ex.message}", ex)
                throw ScheduledTaskException("$logPrefix failed", ex)
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
    }

    suspend fun importSkillGroups() {
        val saveBatch: suspend (batch: List<SkillGroup>) -> Unit = { batch ->
            skillGroupService.saveBatch(batch)
        }
        orgRepository.findAll().forEach { org ->
            try {
                val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
                downloadAndImport(org, "skill_groups", MLE_LEAGUES_HEADERS, saveBatch) {
                    batch,
                    csvRow ->
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

                downloadAndImport(org, "teams", MLE_TEAMS_HEADERS, saveBatch) { batch, csvRow ->
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
                downloadAndImport(org, "players", MLE_PLAYERS_HEADERS, saveBatch) { batch, csvRow ->
                    playerService.import(
                        org,
                        preExistingTeams,
                        preExistingSkillGroups,
                        preExistingFranchises,
                        batch,
                        csvRow,
                    )
                }
            } catch (ex: Exception) {
                log.error("${org.acronym} Player import failed", ex)
                throw ex
            }
        }
    }
}
