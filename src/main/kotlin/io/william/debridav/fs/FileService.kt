package io.william.debridav.fs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.milton.resource.Resource
import io.william.debridav.configuration.DebridavConfiguration
import io.william.debridav.debrid.DebridFileContentsDeserializer
import io.william.debridav.resource.DebridFileResource
import io.william.debridav.resource.DirectoryResource
import io.william.debridav.resource.FileResource
import jakarta.annotation.PostConstruct
import org.apache.commons.io.FileExistsException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.naming.ConfigurationException

@Service
class FileService(
    private val debridavConfiguration: DebridavConfiguration
) {
    private val cache: LoadingCache<String, DebridFileContents?> = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(CacheLoader.from { path -> loadContentsFromFile(path) })
    private val logger = LoggerFactory.getLogger(FileService::class.java)
    private val objectMapper = jacksonObjectMapper()

    @PostConstruct
    fun postConstruct() {
        if (debridavConfiguration.filePath.endsWith("/")) {
            throw ConfigurationException("debridav.local.file.path: ${debridavConfiguration.filePath} should not contain a trailing slash")
        }
    }

    fun createDebridFile(
        path: String,
        debridFileContents: DebridFileContents
    ): File {
        return createLocalFile(
            "${debridavConfiguration.filePath}${debridavConfiguration.downloadPath}/$path.debridfile",
            objectMapper.writeValueAsString(debridFileContents).byteInputStream()
        )
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
        cache.getIfPresent(path)?.let {
            cache.invalidate(path)
            cache.put(src.path, it)
        }
    }

    fun deleteFile(file: File) {
        cache.invalidate(file.path)
        file.delete()
    }

    fun createDirectory(path: String): DirectoryResource {
        val file = File(path)

        if (!Files.exists(file.toPath())) {
            Files.createDirectory(file.toPath())
        }

        return DirectoryResource(file, listOf(),this)
    }


    fun getFileAtPath(path: String): File? {
        val file = File("${debridavConfiguration.filePath}$path")
        if (file.exists()) return file
        return null
    }

    fun moveResource(item: Resource, destination: String, name: String) {
        when (item) {
            is FileResource -> moveFile(item.file.path, destination, name)
            is DebridFileResource -> moveFile(item.file.path, destination, "$name.debridfile")
            is DirectoryResource -> moveFile(item.directory.path,  destination, name)
        }
    }


    fun getSizeOfCachedContent(debridFile: File): Long {
        return cache.get(debridFile.path).size
    }

    private fun loadContentsFromFile(path: String): DebridFileContents? {
        return if(File(path).exists()) {
            DebridFileContentsDeserializer.deserialize(File(path).readText(Charsets.UTF_8))
        }
        else null
    }

    fun writeContentsToFile(file: File, debridFileContents: DebridFileContents) {
        file.writeText(objectMapper.writeValueAsString(debridFileContents))
        cache.put(file.path, debridFileContents)
    }

    fun getDebridFileContents(file: File): DebridFileContents =  cache.get(file.path)
}