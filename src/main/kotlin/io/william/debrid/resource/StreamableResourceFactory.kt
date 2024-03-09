package io.william.debrid.resource

import io.milton.common.Path
import io.milton.http.ResourceFactory
import io.milton.http.exceptions.BadRequestException
import io.milton.http.exceptions.NotAuthorizedException
import io.milton.resource.Resource
import io.william.debrid.fs.FileService
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.repository.FileRepository
import java.net.URLDecoder


class StreamableResourceFactory(
    private val fileService: FileService
): ResourceFactory {
    @Throws(NotAuthorizedException::class, BadRequestException::class)
    override fun getResource(host: String?, url: String): Resource? {
        //log.debug("getResource: url: $url")
        val path: Path = Path.path(url)

        val r = find(path)
        //log.debug("_found: $r for url: $url and path: $path")
        return r
    }

    @Throws(NotAuthorizedException::class, BadRequestException::class)
    private fun find(path: Path): Resource? {
        val actualPath = if(path.isRoot) "/" else path.toPath()
        return fileService.getResourceAtPath(actualPath)
    }
}