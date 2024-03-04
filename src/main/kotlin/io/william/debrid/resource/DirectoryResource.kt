package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Request
import io.milton.resource.CollectionResource
import io.milton.resource.MakeCollectionableResource
import io.milton.resource.Resource
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

class DirectoryResource(
    private val id: Long,
    private val name: String,
    private val created: Date,
    private var children: List<Resource>?
) :AbstractResource(), MakeCollectionableResource {


    override fun getUniqueId(): String {
        return id.toString()
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

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return created
    }

    override fun child(childName: String?): Resource? {
        return children?.first { it.name == childName }
    }

    override fun getChildren(): MutableList<out Resource> {
        return children?.toMutableList() ?: emptyList<Resource>().toMutableList()
    }


    override fun createCollection(newName: String?): CollectionResource {

        TODO("not implemented")

    }
}