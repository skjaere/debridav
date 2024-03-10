package io.william.debrid.resource

import io.milton.http.Auth
import io.milton.http.Request
import io.milton.resource.CollectionResource
import io.milton.resource.MakeCollectionableResource
import io.milton.resource.MoveableResource
import io.milton.resource.PutableResource
import io.milton.resource.Resource
import io.william.debrid.fs.FileService
import io.william.debrid.fs.models.Directory
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.util.*

class DirectoryResource(
    val directory: File,
    private var fileService: FileService
) : AbstractResource(fileService), MakeCollectionableResource, MoveableResource, PutableResource {

    private var children: List<Resource>? = null;
    init {
        children = directory.listFiles()
            ?.toList()
            ?.mapNotNull { fileService.toResource(it) }
            ?: emptyList()
    }

    override fun getUniqueId(): String {
        return directory.path
    }

    override fun getName(): String {
        return if(directory.path == "/") "/" else directory.path!!.split("/").last()
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

    override fun moveTo(rDest: CollectionResource, name: String) {
        fileService.moveResource(this, (rDest as DirectoryResource).directory.path, name)
    }

    override fun isDigestAllowed(): Boolean {
        return true
    }

    override fun getCreateDate(): Date {
        return Date.from(Instant.ofEpochMilli(directory.lastModified()))
    }

    override fun child(childName: String?): Resource? {
        return children?.firstOrNull { it.name == childName }
    }

    override fun getChildren(): MutableList<out Resource> {
        return children?.toMutableList() ?: emptyList<Resource>().toMutableList()
    }

    override fun createNew(newName: String, inputStream: InputStream, length: Long, contentType: String): Resource {
        val createdFile = fileService.createLocalFile(
            "${directory.path}/$newName",
                length,
                inputStream,
                contentType
                )
        return FileResource(createdFile, fileService)
    }


    override fun createCollection(newName: String?): CollectionResource {
        return fileService.createDirectory("${directory.path}/$newName")
    }

    //fun getDirectory(): Directory = directory
}