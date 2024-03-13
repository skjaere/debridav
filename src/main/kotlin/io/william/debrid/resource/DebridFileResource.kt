package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.DeletableResource
import io.milton.resource.GetableResource
import io.william.debrid.fs.FileService
import java.io.File
import java.io.OutputStream
import java.time.Instant
import java.util.*

class DebridFileResource(
    val file: File,
    fileService: FileService
) : AbstractResource(fileService), GetableResource, DeletableResource {

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
        fileService.streamDebridFile(file, range, out)
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