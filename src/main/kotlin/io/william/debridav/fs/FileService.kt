package io.william.debridav.fs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.milton.resource.Resource
import io.william.debridav.StreamingService
import io.william.debridav.debrid.DebridClient
import io.william.debridav.resource.DebridFileResource
import io.william.debridav.resource.DirectoryResource
import io.william.debridav.resource.FileResource
import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileExistsException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import javax.naming.ConfigurationException

@Service
class FileService(
        private val debridClient: DebridClient,
        @Value("\${debriDav.local.file.path}") val localPath: String,
        @Value("\${debridav.cache.local.debridfiles.threshold.mb}") val cacheLocalFilesMbThreshold: Int,
        @Value("\${debridav.debridclient}") val debridProvider: DebridProvider,
        private val streamingService: StreamingService
) {
    private val logger = LoggerFactory.getLogger(FileService::class.java)
    private val objectMapper = jacksonObjectMapper()

    @PostConstruct
    fun postConstruct() {
        if (localPath.endsWith("/")) {
            throw ConfigurationException("debridav.local.file.path: $localPath should not contain a trailing slash")
        }
    }

    fun createDebridFile(
            createRequest: CreateFileRequest,
            magnet: String?,
            torrentFile: ByteArray?
    ) {
        val debridFileContents = DebridFileContents(
                createRequest.path,
                createRequest.size,
                Instant.now().toEpochMilli(),
                magnet,
                mutableListOf(
                        DebridLink(
                                debridProvider,
                                createRequest.link
                        )
                )
        )
        if (createRequest.size < cacheLocalFilesMbThreshold * 1024 * 1024) {
            createLocalFile(
                    createRequest.path,
                    URL(createRequest.link).openConnection().getInputStream()
            )
        } else {
            createDebridFile(
                    createRequest.path,
                    objectMapper.writeValueAsString(debridFileContents).byteInputStream()
            )
        }
    }

    fun createDebridFile(
            directory: String,
            inputStream: InputStream
    ): File {
        return createLocalFile("$directory.debridfile", inputStream)
    }

    fun createLocalFile(
            directory: String,
            inputStream: InputStream
    ): File {
        val file = File(directory)
        return writeFile(file, inputStream)
    }

    private fun writeFile(file: File, inputStream: InputStream): File {
        if (file.exists()) {
            throw FileExistsException("${file.path} already exists")
        }
        if (!Files.exists(file.toPath().parent)) {
            Files.createDirectories(file.toPath().parent)
        }
        file.createNewFile()
        inputStream.transferTo(file.outputStream())
        return file
    }

    fun moveFile(path: String, destinationDirectory: String, name: String) {
        val src = File(path)
        val destination = File("$destinationDirectory/$name")

        if (!destination.parentFile.exists()) {
            destination.parentFile.mkdirs()
        }
        Files.move(
                src.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
        )
    }

    fun createDirectory(path: String): DirectoryResource {
        val file = File(path)

        if (!Files.exists(file.toPath())) {
            Files.createDirectory(file.toPath())
        }

        return DirectoryResource(file, this)
    }

    fun getResourceAtPath(path: String): Resource? {
        return getFileAtPath(path)
                ?.let {
                    if (it.isDirectory) it.toDirectoryResource()
                    else it.toFileResource()
                } ?: run {
            getFileAtPath("$path.debridfile")?.toFileResource()
        }
    }

    fun getFileAtPath(path: String): File? {
        val file = File("$localPath$path")
        if (file.exists()) return file
        return null
    }

    fun File.toFileResource(): Resource? {
        if (this.isDirectory) {
            throw RuntimeException("Provided file is a directory")
        }
        return if (this.name.endsWith(".debridfile")) {
            DebridFileResource(this, this@FileService, streamingService)
        } else {
            if (this.exists())
                return FileResource(this, this@FileService)
            null
        }
    }

    fun toResource(file: File): Resource? {
        return if (file.isDirectory) file.toDirectoryResource() else file.toFileResource()
    }

    fun File.toDirectoryResource(): DirectoryResource {
        if (!this.isDirectory) {
            throw RuntimeException("Not a directory")
        }
        return DirectoryResource(this, this@FileService)
    }

    fun moveResource(item: Resource, destination: String, name: String) {
        when (item) {
            is FileResource -> moveFile(item.file.path, destination, name)
            is DebridFileResource -> moveFile(item.file.path, destination, "$name.debridfile")
            is DirectoryResource -> moveFile(item.directory.path, destination, name)
        }
    }


    fun getSizeOfCachedContent(debridFile: File): Long {
        return objectMapper.readValue<DebridFileContents>(debridFile).size
    }

    fun refreshDebridFile(debridFile: File): DebridFileContents? {
        val contents = objectMapper.readValue<DebridFileContents>(debridFile)
        val isCached = debridClient.isCached(contents.magnet!!)
        if (isCached) {
            debridClient.getDirectDownloadLink(contents.magnet!!)
                    .firstOrNull { it.path == contents.originalPath }
                    ?.let {
                        val newContents = DebridFileContents.ofDebridResponse(it, contents.magnet, debridProvider)
                        debridFile.writeText(
                                objectMapper.writeValueAsString(newContents)
                        )
                        return newContents
                    } ?: run { return null }
        }
        return null
    }

    fun handleDeadLink(
            debridFile: File,
    ): DebridFileContents? {
        logger.info("Found stale link for ${debridFile.path}. Attempting refresh.")
        refreshDebridFile(debridFile)?.let {
            logger.info("Found fresh link for ${debridFile.path}")
            return it
        } ?: run {
            logger.info("Unable to find fresh link for ${debridFile.path}. Deleting file")
            debridFile.delete()
            return null
        }
    }

    fun addProviderDebridLinkToDebridFile(debridFile: File): DebridFileContents? {
        val contents = objectMapper.readValue<DebridFileContents>(debridFile)
        debridClient.getDirectDownloadLink(contents.magnet!!)
                .firstOrNull { it.path == contents.originalPath }
                ?.let {
                    contents.debridLinks.add(
                            it.toDebridLink(debridClient.getProvider())
                    )
                    debridFile.writeText(
                            objectMapper.writeValueAsString(contents)
                    )
                    return contents
                }
        return null
    }

    data class CreateFileRequest(
            val path: String,
            val size: Long,
            val link: String
    )
}