package io.william.debrid.resource

import io.milton.common.Path
import io.milton.http.HttpManager
import io.milton.http.ResourceFactory
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.resource.CollectionResource
import io.milton.resource.Resource
import io.william.debrid.fs.Directory
import io.william.debrid.fs.File
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.repository.FileRepository
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*


class StreamableResourceFactory(
    private val directoryRepository: DirectoryRepository,
    private val fileRepository: FileRepository
): ResourceFactory {
    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun getResource(host: String?, url: String): Resource? {
        //log.debug("getResource: url: $url")
        val path: Path = Path.path(url)
        val r = find(path)
        //log.debug("_found: $r for url: $url and path: $path")
        return r
    }
    private fun Directory.toResource(files: List<Resource>?): DirectoryResource = DirectoryResource(
        this.id!!,
        if(this.path == "/") "/" else this.path!!.split("/").last(),
        Date.from(Instant.now()),
        files
    )

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    private fun find(path: Path): Resource {
        val actualPath = if(path.isRoot) "/" else path.toPath()
        val directoryFromDb = directoryRepository.findByPath(actualPath).get()
        val files = fileRepository.findAllByDirectory(directoryFromDb)
        val childDirectories = directoryRepository.findAllByParent(directoryFromDb)
        childDirectories.map {  }
        val directory = directoryFromDb
            .toResource(files.map { FileResource(it)}.plus(childDirectories.map { it.toResource(emptyList()) }))

        //resources.add()
        //var r: DirectoryResource? = HttpManager.request().attributes["rootResource"] as DirectoryResource?
        /*if (r == null) {
            r = MoviesResource(movies)
            HttpManager.request().attributes["rootResource"] = r
        }*/

        return directory
       /* //}
        val rParent = find(path.parent) ?: return null
        if (rParent is CollectionResource) {
            return rParent.child(path.name)
        }
        return null*/
    }
}