package io.william.debrid.fs

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.naming.ConfigurationException
import kotlin.io.path.exists

@Service
class LocalStorageService(
    @Value("\${debridav.local.download.path}") val localPath: String
) {
    @PostConstruct
    fun postConstruct() {
        if(localPath.endsWith("/")) {
            throw ConfigurationException("debridav.local.file.path: $localPath should not contain a trailing slash")
        }
    }

    fun createDebridFile(
        directory: String,
        inputStream: InputStream
    ): File {
        val file = File("$localPath/$directory.debridfile")
        return writeFile(file, inputStream)
    }

    fun createLocalFile(
        directory: String,
        type: FileService.CreateFileRequest.Type,
        inputStream: InputStream
    ): File {
        val file = File(directory)
        return writeFile(file, inputStream)
    }

    private fun writeFile(file: File, inputStream: InputStream): File {
        if(file.exists()) {
            throw FileExistsException("${file.path} already exists")
        }
        if(!Files.exists(file.toPath().parent)) {
            Files.createDirectory(file.toPath().parent)
        }
        file.createNewFile()
        inputStream.transferTo(file.outputStream())
        return file
    }

    fun moveFile(file: String, destinationDirectory: String, name: String) {
        val src = File(file)
        val destination = File("$destinationDirectory/$name")
        if(!destination.parentFile.exists()) {
            destination.parentFile.mkdirs()
        }
        Files.move(
            src.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    fun getFileAtPath(path: String): File? {
        val file = File("$localPath/$path")
        if(file.exists()) return file
        return null
    }

    fun createDirectoryIfNotExist(path: String): File {
        val file = File(path)

        if(!Files.exists(file.toPath())) {
            Files.createDirectory(file.toPath())
        }

        return file
    }
}

class FileExistsException(message: String): RuntimeException(message) {

}