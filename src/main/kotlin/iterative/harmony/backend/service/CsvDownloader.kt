package iterative.harmony.backend.service

import iterative.harmony.backend.exception.DataSourceUnavailableException
import iterative.harmony.backend.util.getLogger
import java.io.File
import java.nio.file.StandardOpenOption
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException

@Component
class CsvDownloader {
    private val log = getLogger()
    private val webClient = WebClient.builder().build()

    fun downloadToTempFile(url: String, prefix: String, suffix: String): File {
        val tempFile = File.createTempFile(prefix, suffix)
        try {
            val flux =
                webClient
                    .get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .retrieve()
                    .bodyToFlux(DataBuffer::class.java)
            DataBufferUtils.write(flux, tempFile.toPath(), StandardOpenOption.CREATE).block()
        } catch (ex: WebClientRequestException) {
            tempFile.delete()
            log.error("$prefix data source is unavailable: ${ex.message}")
            throw DataSourceUnavailableException("$prefix data source is unavailable", ex)
        }

        return tempFile
    }
}
