package iterative.harmony.backend.service

import iterative.harmony.backend.exception.DataSourceUnavailableException
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClientRequestException

class CsvDownloaderTest {

    private val csvDownloader = CsvDownloader()

    // Nothing listens on port 1, so the request fails before any response
    private val unreachableUrl = "http://localhost:1/teams.csv"
    private val prefix = "CsvDownloaderTest-unreachable"

    private fun leftoverTempFiles(): List<File> =
        File(System.getProperty("java.io.tmpdir"))
            .listFiles { file -> file.name.startsWith(prefix) }
            .orEmpty()
            .toList()

    @Nested
    @DisplayName("downloadToTempFile")
    inner class DownloadToTempFile {

        @Test
        fun `wraps a failed request in DataSourceUnavailableException`() {
            val ex =
                assertThrows<DataSourceUnavailableException> {
                    runBlocking { csvDownloader.downloadToTempFile(unreachableUrl, prefix, ".csv") }
                }

            assertEquals("$prefix data source is unavailable", ex.message)
            assertInstanceOf(WebClientRequestException::class.java, ex.cause)
        }

        @Test
        fun `deletes the temp file when the request fails`() {
            assertThrows<DataSourceUnavailableException> {
                runBlocking { csvDownloader.downloadToTempFile(unreachableUrl, prefix, ".csv") }
            }

            assertEquals(emptyList<File>(), leftoverTempFiles())
        }
    }
}
