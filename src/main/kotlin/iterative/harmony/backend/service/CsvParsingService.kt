package iterative.harmony.backend.service

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File
import org.springframework.stereotype.Service

@Service
class CsvParsingService {
    private val csvMapper = CsvMapper().findAndRegisterModules()

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
                csvStream.forEach { row -> parsingCode(row) }
            }
        } catch (e: Exception) {
            throw RuntimeException("Error parsing CSV: ${e.message}")
        }
    }
}
