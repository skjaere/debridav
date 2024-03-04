package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Request
import io.milton.resource.Resource
import io.william.debrid.fs.File
import java.time.Instant
import java.util.*

class FileResource(private val file: File) : AbstractResource(), Resource {
    override fun getUniqueId(): String {
        return file.id.toString()
    }

    override fun getName(): String {
        return file.path!!
    }

    override fun authorise(request: Request?, method: Request.Method?, auth: Auth?): Boolean {
        return true
    }

    override fun getRealm(): String {
        return "realm"
    }

    override fun getModifiedDate(): Date {
        return Date.from(Instant.now())
    }

    override fun checkRedirect(request: Request?): String? {
        return null
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return Date.from(Instant.now())
    }
}