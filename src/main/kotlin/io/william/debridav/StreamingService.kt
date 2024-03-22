package io.william.debridav

import io.milton.http.Range
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

@Service
class StreamingService(
        @Value("\${debridav.debridclient}") val debridProvider: DebridProvider
) {
    private val logger = LoggerFactory.getLogger(StreamingService::class.java)

    fun streamDebridFile(
            debridFileContents: DebridFileContents,
            range: Range?,
            out: OutputStream
    ): Result {
        val providerLink = debridFileContents.getProviderLink(debridProvider)
        val connection = providerLink
                ?.link?.let {
                    openConnection(it)
                } ?: run { return Result.PROVIDER_MISSING }

        range?.let {
            val start = range.start ?: 0
            val finish = range.finish ?: debridFileContents.size
            val byteRange = "bytes=$start-$finish"
            logger.debug("applying byterange: {}  from {}", byteRange, range)
            connection.setRequestProperty("Range", byteRange)
        }
        if (connection.responseCode.isErrorResponse()) {
            return Result.DEAD_LINK
        }

        try {
            logger.info("Begin streaming of $providerLink")
            streamContents(connection, out)
            logger.info("Streaming of $providerLink complete")
        } catch (e: Exception) {
            out.close()
            connection.inputStream.close()
            connection.disconnect()
            logger.error("error!", e)
            return Result.ERROR
        }
        return Result.OK
    }

    fun Int.okayResponse() = this in 200..299
    fun Int.isErrorResponse() = !this.okayResponse()

    private fun streamContents(
            connection: HttpURLConnection,
            out: OutputStream
    ) {
        connection.inputStream.transferTo(out)
        connection.inputStream.close()
        out.close()
        connection.disconnect()
    }

    fun openConnection(link: String): HttpURLConnection = URL(link).openConnection() as HttpURLConnection

    enum class Result {
        DEAD_LINK, ERROR, OK, PROVIDER_MISSING
    }
}