package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.GetableResource
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.DebridFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection
import java.time.Instant
import java.util.*
import kotlin.time.measureTime

class DebridFileResource(
    val debridFile: DebridFile,
    val file: File,
    fileService: FileService
) : AbstractResource(fileService), GetableResource, DeletableResource {
    private val logger = LoggerFactory.getLogger(DebridFileResource::class.java)
    override fun getUniqueId(): String {
        return debridFile.name
    }

    override fun getName(): String {
        return debridFile.name
    }

    override fun authorise(request: Request?, method: Request.Method?, auth: Auth?): Boolean {
        return true
    }

    override fun getRealm(): String {
        return "realm"
    }

    override fun getModifiedDate(): Date {
        return Date.from(Instant.ofEpochMilli(debridFile.modified))
    }

    override fun checkRedirect(request: Request?): String? {
        return null
    }

    override fun delete() {
        file.delete()
    }

    override fun sendContent(
        out: OutputStream,
        range: Range?,
        params: MutableMap<String, String>?,
        contentType: String?
    ) {
        var connection: URLConnection? = null
        val took = measureTime {
            connection = URL(debridFile.link).openConnection()
        }
        logger.info("opened connection to ${debridFile.link} in $took ms")
        try {
            range?.let {
                if(range.start != null && range.finish == null) {
                    logger.info("Invalid range header received: $range")
                }

                val start = range.start ?: 0
                val finish = range.finish ?: debridFile.size
                val byteRange = "bytes=$start-$finish"
                logger.info("applying byterange: $byteRange from $range")
                connection!!.setRequestProperty("Range", byteRange)
            }
            logger.info("Begin streaming of ${debridFile.link}")
            connection!!.getInputStream().transferTo(out)
            logger.info("Streaming of ${debridFile.link} complete")
            connection!!.getInputStream().close()
            out.close()
        } catch (e: Exception) {
            out.close()
            connection!!.getInputStream().close()
            logger.error("error!", e)
        }
    }

    override fun getMaxAgeSeconds(auth: Auth?): Long {
        return 100
    }

    override fun getContentType(accepts: String?): String {
        return "video/mp4"
    }

    override fun getContentLength(): Long {
        return debridFile.size
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return modifiedDate
    }
}