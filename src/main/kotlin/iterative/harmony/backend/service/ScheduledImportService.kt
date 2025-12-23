package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ScheduledTaskException
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.model.Team
import iterative.harmony.backend.model.User
import iterative.harmony.backend.repository.DataSourceRepository
import iterative.harmony.backend.repository.OrganizationRepository
import iterative.harmony.backend.util.getLogger
import java.io.File
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.collections.isNotEmpty
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class ScheduledImportService {
    private val log = getLogger()
    @Autowired private lateinit var orgRepository: OrganizationRepository
    @Autowired private lateinit var dataSourceRepository: DataSourceRepository
    @Autowired private lateinit var csvParsingService: CsvParsingService
    @Autowired private lateinit var skillGroupService: SkillGroupService
    @Autowired private lateinit var teamService: TeamService
    @Autowired private lateinit var userService: UserService
    private val webClient = WebClient.builder().build()
    private val BATCH_SIZE = 100
    // TODO: Figure out how to store these headers in the DB alongside data_sources
    private val MLE_SKILL_GROUP_HEADERS =
        listOf(
            "skill_group_id",
            "league_code",
            "league_name",
            "color",
            "league_photo_url",
            "discord_emoji",
            "max_salary",
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
            "skill_group",
            "franchise",
            "Franchise Staff Position",
            "slot",
            "current_scrim_points",
            "Eligible Until",
        )

    // Runs every hour
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun hourlyImports() {
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
        saveBatch: (suspend (batch: MutableList<T>) -> Unit)? = null,
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
            val logPrefix = "$orgAcronym - $destinationTable import - "
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
                        if (saveBatch != null) saveBatch(batch)
                        batch.clear()
                    }
                }
                if (saveBatch != null && batch.isNotEmpty()) saveBatch(batch)
                log.info("$logPrefix - succeeded")
            } catch (ex: Exception) {
                log.error("$logPrefix - failed: ${ex.message}")
                throw ScheduledTaskException("$logPrefix failed", ex)
            } finally {
                if (tempFile.exists()) tempFile.delete()
            }
        }
    }

    suspend fun importSkillGroups() {
        val organizations = orgRepository.findAll()
        organizations.forEach { org ->
            val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
            val saveBatch: suspend (batch: List<SkillGroup>) -> Unit = { batch ->
                skillGroupService.saveBatch(batch)
            }
            downloadAndImport(org, "skill_groups", MLE_SKILL_GROUP_HEADERS, saveBatch) {
                batch,
                csvRow ->
                skillGroupService.import(org, batch, csvRow, preExistingSkillGroups)
            }
        }
    }

    suspend fun importTeams() {
        val organizations = orgRepository.findAll()
        organizations.forEach { org ->
            val preExistingSkillGroups = skillGroupService.getPreExistingSkillGroupsByOrg(org)
            val preExistingTeams = teamService.getPreExistingTeamsByOrg(org)

            val saveBatch: suspend (batch: List<Team>) -> Unit = { batch ->
                teamService.saveBatch(batch)
            }

            downloadAndImport(org, "teams", MLE_TEAMS_HEADERS, saveBatch) { batch, csvRow ->
                teamService.import(org, batch, csvRow, preExistingSkillGroups, preExistingTeams)
            }
        }
    }

    // Hits the DB for each row but should be fine as it's one thread
    suspend fun importUsers() {
        val organizations = orgRepository.findAll()
        organizations.forEach { org ->
            downloadAndImport<User>(org, "users", MLE_MEMBERS_HEADERS) { batch, csvRow ->
                userService.import(csvRow)
            }
        }
    }

    suspend fun importPlayers() {
        throw NotImplementedError()
        // look up exact user by import_id and save one at a time

        //        val organizations = orgRepository.findAll()
        //        organizations.forEach{ org ->
        //            // maybe don't persist users
        //        }
    }
}
