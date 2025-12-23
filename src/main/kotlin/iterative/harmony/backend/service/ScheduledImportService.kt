package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ScheduledTaskException
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.SkillGroup
import iterative.harmony.backend.repository.DataSourceRepository
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
    @Autowired private lateinit var dataSourceRepository: DataSourceRepository
    @Autowired private lateinit var skillGroupService: SkillGroupService
    @Autowired private lateinit var csvParsingService: CsvParsingService
    private val webClient = WebClient.builder().build()

    // TODO: consider storing these headers in the DB alongside data_sources somehow
    val MLE_SKILL_GROUP_HEADERS =
        listOf(
            "skill_group_id",
            "league_code",
            "league_name",
            "color",
            "league_photo_url",
            "discord_emoji",
            "max_salary",
        )
    private val BATCH_SIZE = 100

    // Runs every hour
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    fun hourlyImports() {
        log.info("Hourly - Scheduled Import started")
        //        runBlocking {
        //            importPlayers()
        //        }
        log.info("Hourly - Scheduled Import stopped")
    }

    // Runs once a day
    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.DAYS)
    fun dailyScheduleTasks() {
        log.info("Daily - Scheduled Import started")
        runBlocking {
            importSkillGroups()
            // importTeams()
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
        destinationTable: String,
        csvHeaders: List<String>,
        getPreExistingRecords: suspend (org: Organization) -> List<T>,
        saveBatch: suspend (batch: MutableList<T>) -> Unit,
        importCode:
            suspend (
                org: Organization,
                preExisting: List<T>,
                batch: MutableList<T>,
                csvRow: Map<String, String>,
            ) -> Unit,
    ) {
        val dataSources =
            dataSourceRepository.findAllByDestinationTableAndEnabled(destinationTable, true)
        dataSources.forEach { dataSource ->
            val dataFormat = dataSource.dataFormat
            if (dataFormat != "csv")
                throw ScheduledTaskException(
                    "Only CSV import is supported, import called with $dataFormat"
                )
            val org = dataSource.organization
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
                val preExisting = getPreExistingRecords(org)
                val batch = mutableListOf<T>()
                csvParsingService.parseCsvStream(tempFile, csvHeaders) { csvRow ->
                    importCode(org, preExisting, batch, csvRow)
                    if (batch.size >= BATCH_SIZE) {
                        saveBatch(batch)
                        batch.clear()
                    }
                }
                if (batch.isNotEmpty()) saveBatch(batch)
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
        val getPreExistingRecords: suspend (org: Organization) -> List<SkillGroup> = { org ->
            skillGroupService.getPreExistingSkillGroupsByOrg(org)
        }
        val saveBatch: suspend (batch: List<SkillGroup>) -> Unit = { batch ->
            skillGroupService.saveBatch(batch)
        }
        downloadAndImport(
            "skill_groups",
            MLE_SKILL_GROUP_HEADERS,
            getPreExistingRecords,
            saveBatch,
        ) { org, preExisting, batch, csvRow ->
            skillGroupService.import(org, preExisting, batch, csvRow)
        }
    }

    suspend fun importTeams() {
        throw NotImplementedError()
    }

    suspend fun importPlayers() {
        // TODO: figure out how to parse without loading all pre-existing players in memory at once
        // Maybe use BATCH_SIZE somehow?
        throw NotImplementedError()
    }
}
