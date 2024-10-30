package io.william.debridav.resource

import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.GetableResource
import io.william.debridav.StreamingService
import io.william.debridav.StreamingService.Result.*
import io.william.debridav.debrid.CachedFile
import io.william.debridav.debrid.DebridClient
import io.william.debridav.debrid.DebridService
import io.william.debridav.debrid.MissingFile
import io.william.debridav.fs.DebridFileContents
import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.FileService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.*

class DebridFileResource(
    val file: File,
    fileService: FileService,
    private val streamingService: StreamingService,
    private val debridService: DebridService
) : AbstractResource(fileService), GetableResource, DeletableResource {
    private val debridFileContents: DebridFileContents = fileService.getDebridFileContents(file)
    private val logger = LoggerFactory.getLogger(DebridClient::class.java)


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
        fileService.deleteFile(file)
    }

    override fun sendContent(
        out: OutputStream,
        range: Range?,
        params: MutableMap<String, String>?,
        contentType: String?
    ) {
        runBlocking {
            out.use { outputStream ->
                debridService.getCheckedLinks(file)
                    .firstOrNull()
                    ?.let { cachedFile ->
                        logger.info("streaming: {}",cachedFile)
                        streamingService.streamDebridLink(
                            cachedFile,
                            range,
                            debridFileContents.size,
                            outputStream
                        )
                    } ?: {
                    logger.info("No working link found for ${debridFileContents.originalPath}")
                }
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