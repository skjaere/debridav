package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Range
import io.milton.http.Request
import io.milton.resource.GetableResource
import io.milton.resource.Resource
import io.william.debrid.premiumize.DirectDownloadResponse
import io.william.debrid.premiumize.PremiumizeClient
import java.io.OutputStream
import java.net.URL
import java.net.URLConnection
import java.time.Instant
import java.util.*

class MovieResource(
    private val name: String,
    url: String
) : AbstractResource(), GetableResource, Resource {
    private val restClient = PremiumizeClient()
    private var directDownloadResponse: DirectDownloadResponse? = null
    private var contentsWithMedia: DirectDownloadResponse.Content? = null

    init {
        directDownloadResponse =
            restClient.getDirectDownloadLink(url)

        contentsWithMedia = directDownloadResponse
            ?.content?.firstOrNull { it.path.endsWith(".mp4") }
    }

    override fun getUniqueId(): String {
        return "id"
    }

    override fun getName(): String {
        return name
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

    override fun sendContent(
        out: OutputStream?,
        range: Range?,
        params: MutableMap<String, String>?,
        contentType: String?
    ) {
        /*val restController = PremiumizeClient()
        val isCached = restController.isCached(url)
        val response = restController.getDirectDownloadLink(url)*/

        val directDlUrl = contentsWithMedia?.streamLink
        URL(directDlUrl).openStream().transferTo(out)
    }

    override fun getMaxAgeSeconds(auth: Auth?): Long {
        return 600
    }

    override fun getContentType(accepts: String?): String {
        return "text"
    }

    override fun getContentLength(): Long {
        return contentsWithMedia?.size ?: 0
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return Date.from(Instant.now())
    }
}