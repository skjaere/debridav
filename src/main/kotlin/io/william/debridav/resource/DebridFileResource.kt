package io.william.debridav.resource

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.GetableResource
import io.william.debridav.StreamingService
import io.william.debridav.fs.FileService
import io.william.debridav.fs.models.DebridFileContents
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.*

class DebridFileResource(
        val file: File,
        fileService: FileService,
        private val streamingService: StreamingService
) : AbstractResource(fileService), GetableResource, DeletableResource {
    private val debridFileContents: DebridFileContents = jacksonObjectMapper().readValue(file)

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
        val result = streamingService.streamDebridFile(debridFileContents, range, out)
        if (result == StreamingService.Result.DEAD_LINK) {
            fileService.handleDeadLink(file)?.let {
                streamingService.streamDebridFile(it, range, out)
            } ?: run {
                out.close()
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