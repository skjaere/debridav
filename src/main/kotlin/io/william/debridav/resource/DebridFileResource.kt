package io.william.debridav.resource

import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.GetableResource
import io.william.debridav.StreamingService
import io.william.debridav.debrid.DebridClient
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridProvider
import io.william.debridav.fs.FileService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.*

class DebridFileResource(
        val file: File,
        fileService: FileService,
        private val streamingService: StreamingService,
        @Value("\${debridav.debridclient}") val debridProvider: DebridProvider
) : AbstractResource(fileService), GetableResource, DeletableResource {
    private val logger = LoggerFactory.getLogger(DebridClient::class.java)
    private val debridFileContents: DebridFileContents = fileService.getDebridFileContents(file)

    override fun getUniqueId(): String {
        return file.name
    }

    override fun getName(): String {
        return file.name.replace(".debridfile", "")
    }

    override fun authorise(request: Request?, method: Request.Method?, auth: Auth?): Boolean {
        return true
    }

    override fun getRealm(): String {
        return "realm"
    }

    override fun getModifiedDate(): Date {
        return Date.from(Instant.ofEpochMilli(file.lastModified()))
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
        out.use { outputStream ->
            debridFileContents.getProviderLink(debridProvider)?.let { debridLink ->
                val result = streamingService.streamDebridLink(debridLink, range, debridFileContents.size, outputStream)
                when (result) {
                    StreamingService.Result.DEAD_LINK -> handleDeadLink(range, debridFileContents.size, outputStream)
                    StreamingService.Result.ERROR -> handleDeadLink(range, debridFileContents.size, outputStream)
                    StreamingService.Result.OK -> {}
                }
            } ?: run {
                fileService.addProviderDebridLinkToDebridFile(file)?.let { refreshedContents ->
                    streamingService.streamDebridLink(refreshedContents.getProviderLink(debridProvider)!!, range, debridFileContents.size, outputStream)
                }
            }
        }
    }

    private fun handleDeadLink(range: Range?, fileSize: Long, out: OutputStream) {
        fileService.handleDeadLink(file)?.let {
            if (streamingService.streamDebridLink(it.getProviderLink(debridProvider)!!, range, fileSize, out) != StreamingService.Result.OK) {
                logger.info("failed to refresh file ${debridFileContents.getProviderLink(debridProvider)}")
            }
        }
    }

    override fun getMaxAgeSeconds(auth: Auth?): Long {
        return 100
    }

    override fun getContentType(accepts: String?): String {
        return "video/mp4"
    }

    override fun getContentLength(): Long {
        return fileService.getSizeOfCachedContent(file)
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return modifiedDate
    }
}