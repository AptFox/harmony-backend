package iterative.harmony.backend.service

import iterative.harmony.backend.model.DataSource
import iterative.harmony.backend.model.Organization
import iterative.harmony.backend.repository.DataSourceRepository
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.Mockito.`when` as whenever
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ScheduledImportServiceTest {

    @Mock private lateinit var dataSourceRepositoryMock: DataSourceRepository
    @Mock private lateinit var csvParsingServiceMock: CsvParsingService

    @InjectMocks private lateinit var scheduledImportService: ScheduledImportService

    private val org = Organization("Minor League Esports", "MLE", "America/New_York")
    private val dailyInterval = Duration.ofHours(24)

    private fun stubDataSource(lastImportedAt: Instant?) {
        val dataSource =
            DataSource(
                organization = org,
                name = "MLE Teams",
                destinationTable = "teams",
                // Nothing listens here, so any attempted download fails fast
                url = "http://localhost:1/teams.csv",
                dataFormat = "csv",
                comment = null,
                enabled = true,
                lastImportedAt = lastImportedAt,
            )
        whenever(
                dataSourceRepositoryMock.findAllByOrganizationAndDestinationTableAndEnabled(
                    org,
                    "teams",
                    true,
                )
            )
            .thenReturn(listOf(dataSource))
    }

    private suspend fun importTeams() =
        scheduledImportService.downloadAndImport<Any>(
            org,
            "teams",
            emptyList(),
            {},
            dailyInterval,
        ) { _, _ ->
        }

    @Nested
    @DisplayName("downloadAndImport")
    inner class DownloadAndImport {

        @Test
        fun `skips a data source imported within the interval`() {
            stubDataSource(lastImportedAt = Instant.now().minus(Duration.ofHours(1)))

            val result = runBlocking { importTeams() }

            assertNull(result)
            verifyNoInteractions(csvParsingServiceMock)
        }

        @Test
        fun `imports a data source last imported before the interval`() {
            stubDataSource(lastImportedAt = Instant.now().minus(Duration.ofHours(25)))

            // The download is attempted, which fails because nothing is listening
            assertThrows<Exception> { runBlocking { importTeams() } }
        }

        @Test
        fun `imports a data source that has never been imported`() {
            stubDataSource(lastImportedAt = null)

            assertThrows<Exception> { runBlocking { importTeams() } }
        }
    }
}
