package io.william.debrid

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.milton.http.Range
import io.william.debrid.fs.models.DebridFileContents
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

@Service
class StreamingService {
    private val logger = LoggerFactory.getLogger(StreamingService::class.java)
    private val objectMapper = jacksonObjectMapper()

    fun streamDebridFile(
        debridFileContents: DebridFileContents,
        range: Range?,
        out: OutputStream
    ): Result {
        //val debridFileContents: DebridFileContents = objectMapper.readValue(debridFile)
        val connection = URL(debridFileContents.link).openConnection() as HttpURLConnection
        range?.let {
            val start = range.start ?: 0
            val finish = range.finish ?: debridFileContents.size
            val byteRange = "bytes=$start-$finish"
            logger.debug("applying byterange: {}  from {}", byteRange, range)
            connection.setRequestProperty("Range", byteRange)
        }
        if (connection.getResponseCode() == 404) {
            return Result.DEAD_LINK
        }

        try {
            logger.info("Begin streaming of ${debridFileContents.link}")
            streamContents(connection, out)
            logger.info("Streaming of ${debridFileContents.link} complete")
        } catch (e: Exception) {
            out.close()
            connection.inputStream.close()
            logger.error("error!", e)
            return Result.ERROR
        }
        return Result.OK
    }

    private fun streamContents(
        connection: HttpURLConnection,
        out: OutputStream
    ) {
        connection.inputStream.transferTo(out)
        connection.inputStream.close()
        out.close()
    }

    enum class Result {
        DEAD_LINK, ERROR, OK
    }
}