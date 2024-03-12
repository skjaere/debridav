package io.william.debrid.fs

import com.fasterxml.jackson.databind.ObjectMapper
import io.milton.resource.Resource
import io.william.debrid.fs.models.DebridFile
import io.william.debrid.resource.DebridFileResource
import io.william.debrid.resource.DirectoryResource
import io.william.debrid.resource.FileResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.time.Instant

@Service
class FileService(
    private val localFileService: LocalStorageService,
    private val objectMapper: ObjectMapper
) {
    fun createDebridFile(createRequest: CreateFileRequest) {
        val debridFileContents = DebridFile(
            getNameFromPath(createRequest.file.path),
            createRequest.file.path,
            createRequest.file.size,
            Instant.now().toEpochMilli(),
            createRequest.file.link
        )

        localFileService.createDebridFile(
            createRequest.file.path,
            objectMapper.writeValueAsString(debridFileContents).byteInputStream()
        )
    }

    fun createLocalFile(path: String, size: Long, inputStream: InputStream, contentType: String?): File {
        return localFileService.createLocalFile(
            path,
            CreateFileRequest.Type.NORMAL,
            inputStream
        )
    }

    private fun getNameFromPath(path: String): String = path.split("/").last()

    fun moveFile(src: String, dest: String, name: String) {
        localFileService.moveFile(src, dest, name)
    }

    fun createDirectory(path: String): DirectoryResource {
        return DirectoryResource(localFileService.createDirectoryIfNotExist(path), this)
    }

    fun getResourceAtPath(path: String): Resource? {
        return localFileService.getFileAtPath(path)
            ?.let {
                if (it.isDirectory) it.toDirectoryResource()
                else it.toFileResource()
            } ?: run {
                localFileService.getFileAtPath("$path.debridfile")?.toFileResource()
        }
    }

    fun File.toFileResource(): Resource? {
        if(this.isDirectory) {
            throw RuntimeException("Provided file is a directory")
        }
        return if(this.name.endsWith(".debridfile")) {
            DebridFileResource(objectMapper.readValue(this, DebridFile::class.java), this,this@FileService)
        } else {
            if(this.exists())
                return FileResource(this, this@FileService)
            null
        }
    }

    fun toResource(file: File) : Resource? {
        return if(file.isDirectory) file.toDirectoryResource() else file.toFileResource()
    }

    fun File.toDirectoryResource(): DirectoryResource {
        if(!this.isDirectory) {
            throw RuntimeException("Not a directory")
        }
        return DirectoryResource(this, this@FileService)
    }

    fun moveResource(item: Resource, destination: String, name: String) {
        when(item) {
            is FileResource -> moveFile(item.file.path, destination, name)
            is DebridFileResource -> moveFile(item.file.path, destination, "$name.debridfile")
            is DirectoryResource -> moveFile(item.directory.path, destination, name)
        }
    }

    data class CreateFileRequest( // TODO: wth was I thinking?
        val path: String,
        val type: Type,
        val file: File
    ) {
        data class File(
            val path: String,
            val size: Long,
            val link: String,
            val streamingLink: String?
        )
        enum class Type { DEBRID, NORMAL}
    }
}