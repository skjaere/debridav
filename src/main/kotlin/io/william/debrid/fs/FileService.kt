package io.william.debrid.fs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.milton.resource.Resource
import io.william.debrid.StreamingService
import io.william.debrid.fs.models.DebridFileContents
import io.william.debrid.premiumize.PremiumizeClient
import io.william.debrid.resource.DebridFileResource
import io.william.debrid.resource.DirectoryResource
import io.william.debrid.resource.FileResource
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
    private val premiumizeClient: PremiumizeClient,
    @Value("\${debriDav.local.file.path}") val localPath: String,
    @Value("\${debridav.cache.local.debridfiles.threshold.mb}") val cacheLocalFilesMbThreshold: Int,
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
            createRequest.file.path,
            createRequest.file.size,
            Instant.now().toEpochMilli(),
            createRequest.file.link,
            magnet//,
            //torrentFile
        )
        if (createRequest.file.size < cacheLocalFilesMbThreshold * 1024 * 1024) {
            createLocalFile(
                createRequest.file.path,
                URL(createRequest.file.link).openConnection().getInputStream()
            )
        } else {
            createDebridFile(
                createRequest.file.path,
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

    fun refreshDebridFile(debridFile: File): File? {
        val contents = objectMapper.readValue<DebridFileContents>(debridFile)
        val isCached = premiumizeClient.isCached(contents.magnet!!)
        if (isCached) {
            premiumizeClient.getDirectDownloadLink(contents.magnet!!)?.content
                ?.firstOrNull { it.path == contents.originalPath }
                ?.let {
                    debridFile.writeText(
                        objectMapper.writeValueAsString(
                            DebridFileContents.ofDebridResponseContents(it, contents.magnet)
                        )
                    )
                    return debridFile
                } ?: run { return null }
        }
        return null
    }

    fun handleDeadLink(
        debridFile: File,
    ): File? {
        logger.info("Found stale link for ${debridFile.path}. Attempting refresh.")
        refreshDebridFile(debridFile)?.let {
            logger.info("Found fresh link for ${debridFile.path}")
            return debridFile
            //return objectMapper.readValue<DebridFileContents>(debridFile)
            //return DebridFileContents.ofDebridContents(it, debridFile.
        } ?: run {
            logger.info("Unable to find fresh link for ${debridFile.path}. Deleting file")
            debridFile.delete()
            return null
        }
    }

    data class CreateFileRequest( // TODO: wth was I thinking?
        val path: String?,
        val type: Type,
        val file: File
    ) {
        data class File(
            val path: String,
            val size: Long,
            val link: String,
            val streamingLink: String?
        )

        enum class Type { DEBRID, NORMAL }
    }
}