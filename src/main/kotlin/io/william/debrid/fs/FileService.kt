package io.william.debrid.fs

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.milton.http.Range
import io.milton.resource.Resource
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
import java.io.OutputStream
import java.net.HttpURLConnection
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
            createRequest.path!!,
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
            DebridFileResource(this, this@FileService)
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

    fun streamDebridFile(
        debridFile: File,
        range: Range?,
        out: OutputStream
    ) {
        val debridFileContents: DebridFileContents = objectMapper.readValue(debridFile)
        var connection = URL(debridFileContents.link).openConnection() as HttpURLConnection

        if (connection.getResponseCode() == 404) {
            handleDeadLink(debridFile)?.let {
                connection = it
            } ?: run {
                out.close()
                return
            }
        }

        try {
            streamContents(range, debridFileContents, connection, out)
        } catch (e: Exception) {
            out.close()
            connection.inputStream.close()
            logger.error("error!", e)
        }
    }

    private fun handleDeadLink(
        debridFile: File,
    ): HttpURLConnection? {
        logger.info("Found stale link for ${debridFile.path}. Attempting refresh.")
        refreshDebridFile(debridFile)?.let {
            logger.info("Found fresh link for ${debridFile.path}")
            return URL(it).openConnection() as HttpURLConnection
        } ?: run {
            logger.info("Unable to find fresh link for ${debridFile.path}. Deleting file")
            debridFile.delete()
            return null
        }
    }

    private fun streamContents(
        range: Range?,
        debridFileContents: DebridFileContents,
        connection: HttpURLConnection,
        out: OutputStream
    ) {
        range?.let {
            val start = range.start ?: 0
            val finish = range.finish ?: debridFileContents.size
            val byteRange = "bytes=$start-$finish"
            logger.debug("applying byterange: $byteRange from $range")
            connection.setRequestProperty("Range", byteRange)
        }
        logger.info("Begin streaming of ${debridFileContents.link}")
        connection.inputStream.transferTo(out)
        logger.info("Streaming of ${debridFileContents.link} complete")
        connection.inputStream.close()
        out.close()
    }

    fun getSizeOfCachedContent(debridFile: File): Long {
        return objectMapper.readValue<DebridFileContents>(debridFile).size
    }

    fun refreshDebridFile(debridFile: File): String? {
        val contents = objectMapper.readValue<DebridFileContents>(debridFile)
        val isCached = premiumizeClient.isCached(contents.magnet!!)
        if (isCached) {
            val updatedContents = premiumizeClient.getDirectDownloadLink(contents.magnet!!)
            debridFile.writeText(objectMapper.writeValueAsString(updatedContents))
            return updatedContents
                ?.content
                ?.firstOrNull { it.path == contents.originalPath }
                ?.link
        }
        return null
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