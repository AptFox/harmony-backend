package iterative.harmony.backend.service

import iterative.harmony.backend.exception.DataSourceUnavailableException
import iterative.harmony.backend.exception.ScheduledTaskException
import iterative.harmony.backend.model.DataSource
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.model.Role
import iterative.harmony.backend.repository.DataSourceRepository
import iterative.harmony.backend.repository.FranchiseRepository
import iterative.harmony.backend.repository.OrganizationRepository
import iterative.harmony.backend.repository.RoleRepository
import iterative.harmony.backend.util.RoleConstants
import jakarta.persistence.EntityManager
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

private val now = Instant.parse("2026-10-25T12:00:00Z")

// Lets TransactionTemplate run each block directly, without a database
private class NoOpTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus =
        SimpleTransactionStatus()

    override fun commit(status: TransactionStatus) {}

    override fun rollback(status: TransactionStatus) {}
}

@ExtendWith(MockitoExtension::class)
class ScheduledImportServiceTest {

    @Mock private lateinit var orgRepositoryMock: OrganizationRepository
    @Mock private lateinit var dataSourceRepositoryMock: DataSourceRepository
    @Mock private lateinit var roleRepositoryMock: RoleRepository
    @Mock private lateinit var csvDownloaderMock: CsvDownloader
    @Mock private lateinit var importLockMock: ImportLock
    @Mock private lateinit var csvParsingServiceMock: CsvParsingService
    @Mock private lateinit var entityManagerMock: EntityManager
    // Unstubbed, so each import step only reaches its download
    @Mock private lateinit var franchiseRepositoryMock: FranchiseRepository
    @Mock private lateinit var skillGroupServiceMock: SkillGroupService
    @Mock private lateinit var teamServiceMock: TeamService
    @Mock private lateinit var userServiceMock: UserService
    @Mock private lateinit var playerServiceMock: PlayerService
    @Spy private val clock: Clock = Clock.fixed(now, ZoneOffset.UTC)
    @Spy private val transactionManager: PlatformTransactionManager = NoOpTransactionManager()

    @InjectMocks private lateinit var scheduledImportService: ScheduledImportService

    private val org = Organization("Minor League Esports", "MLE", "America/New_York")
    private val dailyInterval = Duration.ofHours(24)

    private fun dataSource(lastImportedAt: Instant? = null, dataFormat: String = "csv") =
        DataSource(
            organization = org,
            name = "MLE Teams",
            destinationTable = "teams",
            url = "https://example.com/teams.csv",
            dataFormat = dataFormat,
            comment = null,
            enabled = true,
            lastImportedAt = lastImportedAt,
        )

    private fun stubDataSources(vararg dataSources: DataSource) {
        whenever(
                dataSourceRepositoryMock.findAllByOrganizationAndDestinationTableAndEnabled(
                    org,
                    "teams",
                    true,
                )
            )
            .thenReturn(dataSources.toList())
    }

    // One downloaded CSV: a row per name, plus how many rows the parser reports as errors
    private class Csv(vararg val names: String, val errorCount: Int = 0)

    // Each download returns `file`, and each parse feeds the next Csv's rows to the import
    private fun stubCsvs(vararg csvs: Csv, file: File = mock()) {
        whenever(csvDownloaderMock.downloadToTempFile(any(), any(), any())).thenReturn(file)
        val remaining = ArrayDeque(csvs.toList())
        whenever(csvParsingServiceMock.parseCsvStream(any(), any(), any())).doAnswer { invocation ->
            val csv = remaining.removeFirst()
            val parsingCode = invocation.getArgument<(Map<String, String>) -> Unit>(2)
            csv.names.forEach { name -> parsingCode(mapOf("name" to name)) }
            CsvParsingResult(rowsProcessed = csv.names.size, errorCount = csv.errorCount)
        }
    }

    // Each row's "name" goes into the batch
    private fun importTeams(saveBatch: (List<String>) -> Unit = {}): CsvParsingResult? =
        scheduledImportService.downloadAndImport<String>(
            org,
            "teams",
            listOf("name"),
            { batch -> saveBatch(batch.toList()) },
            dailyInterval,
        ) { batch, row ->
            batch.add(row.getValue("name"))
        }

    @Nested
    @DisplayName("downloadAndImport")
    inner class DownloadAndImport {

        @Test
        fun `skips a data source imported within the interval`() {
            stubDataSources(dataSource(lastImportedAt = now.minus(Duration.ofHours(1))))

            val result = importTeams()

            assertNull(result)
            verifyNoInteractions(csvDownloaderMock)
        }

        @Test
        fun `imports a data source last imported before the interval`() {
            stubDataSources(dataSource(lastImportedAt = now.minus(Duration.ofHours(25))))
            stubCsvs(Csv("alice"))

            val result = importTeams()

            assertEquals(CsvParsingResult(rowsProcessed = 1, errorCount = 0), result)
        }

        @Test
        fun `imports a data source that has never been imported`() {
            stubDataSources(dataSource(lastImportedAt = null))
            stubCsvs(Csv("alice"))

            val result = importTeams()

            assertEquals(CsvParsingResult(rowsProcessed = 1, errorCount = 0), result)
        }

        @Test
        fun `saves full batches of 100 and then the remainder`() {
            stubDataSources(dataSource())
            stubCsvs(Csv(*Array(205) { "player$it" }))
            val savedBatchSizes = mutableListOf<Int>()

            importTeams { batch -> savedBatchSizes.add(batch.size) }

            assertEquals(listOf(100, 100, 5), savedBatchSizes)
        }

        @Test
        fun `records the import start time on the data source`() {
            val dataSource = dataSource()
            stubDataSources(dataSource)
            stubCsvs(Csv("alice"))

            importTeams()

            assertEquals(now, dataSource.lastImportedAt)
            verify(dataSourceRepositoryMock).save(dataSource)
        }

        @Test
        fun `combines results across data sources`() {
            stubDataSources(dataSource(), dataSource())
            stubCsvs(
                Csv("alice", "bob", errorCount = 1),
                Csv("carol", "dave", "erin", errorCount = 1),
            )

            val result = importTeams()

            assertEquals(CsvParsingResult(rowsProcessed = 5, errorCount = 2), result)
        }

        @Test
        fun `returns null when no data sources are enabled`() {
            stubDataSources()

            assertNull(importTeams())
        }

        @Test
        fun `rejects data formats other than csv`() {
            stubDataSources(dataSource(dataFormat = "json"))

            assertThrows<ScheduledTaskException> { importTeams() }
            verifyNoInteractions(csvDownloaderMock)
        }

        @Test
        fun `wraps database errors and still deletes the downloaded file`() {
            stubDataSources(dataSource())
            val file = mock<File> { on { exists() } doReturn true }
            stubCsvs(Csv("alice"), file = file)

            assertThrows<ScheduledTaskException> {
                importTeams { throw DataIntegrityViolationException("duplicate key") }
            }
            verify(file).delete()
        }
    }

    private fun stubLockAcquired() {
        doAnswer { (it.arguments[0] as () -> Unit)() }.whenever(importLockMock).runExclusive(any())
    }

    @Nested
    @DisplayName("scheduledImports")
    inner class ScheduledImports {

        @Test
        fun `does nothing when another instance holds the lock`() {
            // An unstubbed lock mock never runs the import
            scheduledImportService.scheduledImports()

            verifyNoInteractions(orgRepositoryMock, roleRepositoryMock)
        }

        @Test
        fun `keeps running later steps after one fails, then reports the failure`() {
            stubLockAcquired()
            whenever(orgRepositoryMock.findAll()).thenReturn(emptyList())
            // A missing default role makes the users step fail
            whenever(roleRepositoryMock.findByName(RoleConstants.USER_ROLE))
                .thenReturn(Optional.empty<Role>())

            val ex =
                assertThrows<ScheduledTaskException> { scheduledImportService.scheduledImports() }

            assertEquals("Import steps failed: [users]", ex.message)
            // skill_groups, teams and players each looked up the orgs
            verify(orgRepositoryMock, times(3)).findAll()
        }

        @Test
        fun `skips steps whose data source is unavailable without failing the run`() {
            stubLockAcquired()
            whenever(orgRepositoryMock.findAll()).thenReturn(listOf(org))
            whenever(roleRepositoryMock.findByName(RoleConstants.USER_ROLE))
                .thenReturn(Optional.of(Role(RoleConstants.USER_ROLE, "Default role")))
            whenever(
                    dataSourceRepositoryMock.findAllByOrganizationAndDestinationTableAndEnabled(
                        any(),
                        any(),
                        any(),
                    )
                )
                .thenReturn(listOf(dataSource()))
            whenever(csvDownloaderMock.downloadToTempFile(any(), any(), any()))
                .thenThrow(
                    DataSourceUnavailableException("MLE-MLE-Teams data source is unavailable")
                )

            // No ScheduledTaskException, so nothing reaches Sentry
            assertDoesNotThrow { scheduledImportService.scheduledImports() }
            // Every step still tried its download
            verify(csvDownloaderMock, times(4)).downloadToTempFile(any(), any(), any())
        }

        @Test
        fun `clears the logging context when finished`() {
            scheduledImportService.scheduledImports()

            assertNull(MDC.getCopyOfContextMap()?.takeIf { it.isNotEmpty() })
        }
    }

    @Nested
    @DisplayName("cleanupPlayers")
    inner class CleanupPlayers {
        private val otherOrg = Organization("Other League", "OTH", "America/New_York")

        private fun stubPlayerDataSources(org: Organization, vararg lastImportedAts: Instant?) {
            whenever(
                    dataSourceRepositoryMock.findAllByOrganizationAndDestinationTableAndEnabled(
                        org,
                        "players",
                        true,
                    )
                )
                .thenReturn(lastImportedAts.map { dataSource(lastImportedAt = it) })
        }

        @Test
        fun `deletes stale players using the earliest data source import as the cutoff`() {
            val earlier = now.minus(Duration.ofHours(2))
            whenever(orgRepositoryMock.findAll()).thenReturn(listOf(org))
            stubPlayerDataSources(org, now.minus(Duration.ofHours(1)), earlier)
            whenever(playerServiceMock.findStalePlayers(org, earlier)).thenReturn(listOf(101L))

            scheduledImportService.cleanupPlayers()

            verify(playerServiceMock).deleteBatch(listOf(101L))
        }

        @Test
        fun `skips orgs whose players have never been imported`() {
            whenever(orgRepositoryMock.findAll()).thenReturn(listOf(org, otherOrg))
            stubPlayerDataSources(org, now, null)
            stubPlayerDataSources(otherOrg)

            scheduledImportService.cleanupPlayers()

            verifyNoInteractions(playerServiceMock)
        }

        @Test
        fun `does not delete when no players are stale`() {
            whenever(orgRepositoryMock.findAll()).thenReturn(listOf(org))
            stubPlayerDataSources(org, now)
            whenever(playerServiceMock.findStalePlayers(org, now)).thenReturn(emptyList())

            scheduledImportService.cleanupPlayers()

            verify(playerServiceMock, never()).deleteBatch(any())
        }

        @Test
        fun `keeps cleaning later orgs after one fails, then reports the failure`() {
            whenever(orgRepositoryMock.findAll()).thenReturn(listOf(org, otherOrg))
            stubPlayerDataSources(org, now)
            stubPlayerDataSources(otherOrg, now)
            whenever(playerServiceMock.findStalePlayers(org, now))
                .thenThrow(RuntimeException("db down"))
            whenever(playerServiceMock.findStalePlayers(otherOrg, now)).thenReturn(listOf(201L))

            val ex =
                assertThrows<ScheduledTaskException> { scheduledImportService.cleanupPlayers() }

            assertEquals("Players cleanup failed for orgs: [MLE]", ex.message)
            verify(playerServiceMock).deleteBatch(listOf(201L))
        }

        @Test
        fun `scheduled cleanup does nothing when another instance holds the lock`() {
            scheduledImportService.scheduledPlayerCleanup()

            verifyNoInteractions(orgRepositoryMock, playerServiceMock)
        }

        @Test
        fun `scheduled cleanup runs while holding the lock`() {
            stubLockAcquired()
            whenever(orgRepositoryMock.findAll()).thenReturn(emptyList())

            scheduledImportService.scheduledPlayerCleanup()

            verify(orgRepositoryMock).findAll()
        }
    }
}
