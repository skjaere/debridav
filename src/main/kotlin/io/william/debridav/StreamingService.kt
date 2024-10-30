package io.william.debridav

import io.milton.http.Range
import io.william.debridav.debrid.CachedFile
import org.apache.catalina.connector.ClientAbortException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.FileNotFoundException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI

@Service
class StreamingService {
    private val logger = LoggerFactory.getLogger(StreamingService::class.java)

    fun streamDebridLink(
            debridLink: CachedFile,
            range: Range?,
            fileSize: Long,
            outputStream: OutputStream
    ): Result {
        val connection = openConnection(debridLink.link!!)
        range?.let {
            val byteRange = getByteRange(range, fileSize)
            logger.debug("applying byterange: {}  from {}", byteRange, range)
            connection.setRequestProperty("Range", byteRange)
        }
        try {
            if (connection.responseCode.isNotOk()) {
                logger.info("Got response: ${connection.responseCode} from $debridLink with body: ${connection.inputStream?.bufferedReader()?.readText() ?: ""}")
                return Result.DEAD_LINK
            }
            connection.inputStream.use { inputStream ->
                logger.debug("Begin streaming of {}", debridLink)
                inputStream.transferTo(outputStream)
                logger.debug("Streaming of {} complete", debridLink)
                return Result.OK
            }
        } catch (_: ClientAbortException) {
            return Result.OK
        } catch (_: FileNotFoundException) {
            return Result.DEAD_LINK
        } catch (e: Exception) {
            logger.error("Error reading content from server", e)
            return Result.ERROR
        }
    }

    private fun getByteRange(range: Range, fileSize: Long): String {
        val start = range.start ?: 0
        val finish = range.finish ?: fileSize
        val byteRange = "bytes=$start-$finish"
        return byteRange
    }

    fun Int.isOkResponse() = this in 200..299
    fun Int.isNotOk() = !this.isOkResponse()

    fun openConnection(link: String): HttpURLConnection {
        return URI(link).toURL().openConnection() as HttpURLConnection
    }

    enum class Result {
        DEAD_LINK, ERROR, OK
    }
}