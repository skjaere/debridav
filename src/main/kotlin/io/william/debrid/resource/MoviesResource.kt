package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Request
import io.milton.resource.CollectionResource
import io.milton.resource.MakeCollectionableResource
import io.milton.resource.Resource
import io.milton.servlet.MiltonFilter
import java.time.Instant
import java.util.*

class MoviesResource(private val movies: List<MovieResource>) : AbstractResource(), MakeCollectionableResource {

    override fun getUniqueId(): String {
        return "movies"
    }

    override fun getName(): String {
        return "movies"
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

    override fun child(childName: String?): Resource? {
        return movies.first { it.name == childName }
    }

    override fun getChildren(): MutableList<out Resource> {
        return movies.toMutableList()
    }

    override fun createCollection(newName: String?): CollectionResource {
        return MoviesResource(movies)
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return Date.from(Instant.now())
    }
}