package iterative.harmony.backend.service

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.util.getLogger
import java.io.File
import java.io.IOException
import org.springframework.stereotype.Service

data class CsvParsingErrorSummary(
    var count: Int = 0,
    val rowPositions: MutableList<Int> = mutableListOf(),
)

@Service
class CsvParsingService {
    private val log = getLogger()
    private val csvMapper = CsvMapper().findAndRegisterModules()

    // Parses CSV's in a stream and logs errors per row without stopping for a malformed row
    suspend fun parseCsvStream(
        file: File,
        requiredHeaders: List<String>,
        parsingCode: suspend (csvRow: Map<String, String>) -> Unit,
    ) {
        val schema = CsvSchema.emptySchema().withHeader()

        try {
            val errorCollector = mutableMapOf<String, CsvParsingErrorSummary>()
            val mappingIterator =
                csvMapper
                    .readerFor(Map::class.java)
                    .with(schema)
                    .readValues<Map<String, String>>(file)

            return mappingIterator.use { csvStream ->
                var rowIndex = 0
                var headersValidated = false
                csvStream.forEach { row ->
                    if (!headersValidated) {
                        val missing = requiredHeaders.filterNot { row.containsKey(it) }
                        if (missing.isNotEmpty()) {
                            throw ImportException(
                                "CSV is missing required column(s): $missing. Found: ${row.keys}"
                            )
                        }
                        headersValidated = true
                    }
                    try {
                        parsingCode(row)
                    } catch (ex: ImportException) {
                        val summary =
                            errorCollector.getOrPut(ex.message!!) { CsvParsingErrorSummary() }
                        summary.count++
                        summary.rowPositions.add(rowIndex)
                    } finally {
                        rowIndex++
                    }
                }
                log.info("$rowIndex rows processed")
                val totalErrorsEncountered = errorCollector.values.sumOf { it.count }
                log.info(
                    "Processing complete. Encountered ${errorCollector.size} unique errors across $totalErrorsEncountered rows."
                )
                errorCollector.forEach { (errorMsg, summary) ->
                    log.error("Error: '$errorMsg', received for rows ${summary.rowPositions}")
                }
            }
        } catch (e: IOException) {
            throw RuntimeException("Error parsing CSV: ${e.message}")
        }
    }
}
