package iterative.harmony.backend.service

import iterative.harmony.backend.exception.ImportException
import java.io.File
import java.nio.file.Path
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class CsvParsingServiceTest {

    private val csvParsingService = CsvParsingService()

    @TempDir lateinit var tempDir: Path

    private fun writeCsv(contents: String): File =
        tempDir.resolve("test.csv").toFile().apply { writeText(contents.trimIndent()) }

    private fun parse(
        file: File,
        requiredHeaders: List<String> = listOf("name", "team"),
        parsingCode: suspend (Map<String, String>) -> Unit = {},
    ) = runBlocking { csvParsingService.parseCsvStream(file, requiredHeaders, parsingCode) }

    @Nested
    @DisplayName("parseCsvStream")
    inner class ParseCsvStream() {

        @Test
        fun `passes each row to parsing code keyed by header`() {
            val file =
                writeCsv(
                    """
                    name,team
                    alice,red
                    bob,blue
                    """
                )
            val rows = mutableListOf<Map<String, String>>()

            parse(file) { rows.add(it) }

            assertEquals(
                listOf(
                    mapOf("name" to "alice", "team" to "red"),
                    mapOf("name" to "bob", "team" to "blue"),
                ),
                rows,
            )
        }

        @Test
        fun `supports reordered columns`() {
            val file =
                writeCsv(
                    """
                    team,name
                    red,alice
                    """
                )
            val rows = mutableListOf<Map<String, String>>()

            parse(file) { rows.add(it) }

            assertEquals(listOf(mapOf("name" to "alice", "team" to "red")), rows)
        }

        @Test
        fun `supports extra columns not in required headers`() {
            val file =
                writeCsv(
                    """
                    name,extra,team
                    alice,ignored,red
                    """
                )
            val rows = mutableListOf<Map<String, String>>()

            parse(file) { rows.add(it) }

            assertEquals(1, rows.size)
            assertEquals("alice", rows[0]["name"])
            assertEquals("red", rows[0]["team"])
            assertEquals("ignored", rows[0]["extra"])
        }

        @Test
        fun `handles quoted values containing commas`() {
            val file =
                writeCsv(
                    """
                    name,team
                    "smith, alice",red
                    """
                )
            val rows = mutableListOf<Map<String, String>>()

            parse(file) { rows.add(it) }

            assertEquals("smith, alice", rows.single()["name"])
        }

        @Test
        fun `throws ImportException when required headers are missing`() {
            val file =
                writeCsv(
                    """
                    name,squad
                    alice,red
                    """
                )
            var callCount = 0

            val ex = assertThrows<ImportException> { parse(file) { callCount++ } }

            assertTrue(ex.message!!.contains("[team]"))
            assertEquals(0, callCount)
        }

        @Test
        fun `does not invoke parsing code for header-only file`() {
            val file = writeCsv("name,team")
            var callCount = 0

            parse(file) { callCount++ }

            assertEquals(0, callCount)
        }

        @Test
        fun `throws RuntimeException for empty file`() {
            val file = writeCsv("")
            var callCount = 0

            val ex = assertThrows<RuntimeException> { parse(file) { callCount++ } }

            assertTrue(ex.message!!.startsWith("Error parsing CSV"))
            assertEquals(0, callCount)
        }

        @Test
        fun `continues processing after ImportException in a row`() {
            val file =
                writeCsv(
                    """
                    name,team
                    alice,red
                    bad,blue
                    carol,green
                    bad,yellow
                    """
                )
            val processed = mutableListOf<String>()

            parse(file) { row ->
                if (row["name"] == "bad") throw ImportException("bad row")
                processed.add(row["name"]!!)
            }

            assertEquals(listOf("alice", "carol"), processed)
        }

        @Test
        fun `propagates non-ImportException from parsing code`() {
            val file =
                writeCsv(
                    """
                    name,team
                    alice,red
                    bob,blue
                    """
                )
            var callCount = 0

            assertThrows<IllegalStateException> {
                parse(file) {
                    callCount++
                    throw IllegalStateException("boom")
                }
            }
            assertEquals(1, callCount)
        }

        @Test
        fun `wraps IOException in RuntimeException when file does not exist`() {
            val file = tempDir.resolve("missing.csv").toFile()

            val ex = assertThrows<RuntimeException> { parse(file) }

            assertTrue(ex.message!!.startsWith("Error parsing CSV"))
        }
    }
}
