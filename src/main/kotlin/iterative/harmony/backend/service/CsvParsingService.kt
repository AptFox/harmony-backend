package iterative.harmony.backend.service

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import iterative.harmony.backend.exception.ImportException
import iterative.harmony.backend.util.getLogger
import java.io.File
import java.io.IOException
import org.springframework.stereotype.Service

@Service
class CsvParsingService {
    private val log = getLogger()
    private val csvMapper = CsvMapper().findAndRegisterModules()

    // Parses CSV's in a stream and logs errors per row without stopping for a malformed row
    suspend fun parseCsvStream(
        file: File,
        headers: List<String>,
        parsingCode: suspend (csvRow: Map<String, String>) -> Unit,
    ) {
        val schema =
            CsvSchema.builder()
                .apply { headers.forEach { addColumn(it) } }
                .setUseHeader(true)
                .build()

        try {
            val mappingIterator =
                csvMapper
                    .readerFor(Map::class.java)
                    .with(schema)
                    .readValues<Map<String, String>>(file)

            return mappingIterator.use { csvStream ->
                var rowIndex = 0
                csvStream.forEach { row ->
                    try {
                        parsingCode(row)
                        rowIndex++
                    } catch (ex: ImportException) {
                        log.warn("Row $rowIndex failed: ${ex.message}")
                    } finally {
                        rowIndex++
                    }
                }
                log.info("$rowIndex rows processed")
            }
        } catch (e: IOException) {
            throw RuntimeException("Error parsing CSV: ${e.message}")
        }
    }
}
