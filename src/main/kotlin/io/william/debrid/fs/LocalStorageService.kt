package io.william.debrid.fs

import io.william.debrid.fs.models.DebridFile
import io.william.debrid.fs.models.Directory
import io.william.debrid.fs.models.LocalFile
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.naming.ConfigurationException

@Service
class LocalStorageService(
    @Value("\${debridav.local.file.path}") val localPath: String
) {
    @PostConstruct
    fun postConstruct() {
        if(localPath.endsWith("/")) {
            throw ConfigurationException("debridav.local.file.path: $localPath should not contain a trailing slash")
        }
    }

    fun createFile(
        directory: String,
        fileName: String,
        type: FileService.CreateFileRequest.Type,
        inputStream: InputStream): File {
        val file = when(type) {
            FileService.CreateFileRequest.Type.NORMAL -> File("$localPath${directory}/$fileName")
            FileService.CreateFileRequest.Type.DEBRID -> File("$localPath${directory}/$fileName.debridfile")
        }
        if(file.exists()) {
            throw FileExistsException("${directory}/$fileName already exists")
        }

        if(!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        inputStream.transferTo(file.outputStream())
        return file
    }

    fun moveFile(file: String, destinationDirectory: String, name: String) {
        val src = File(file)
        val destination = File("$localPath${destinationDirectory}/$name")
        if(!destination.parentFile.exists()) {
            destination.parentFile.mkdirs()
        }
        Files.move(
            src.toPath(),
            destination.toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    fun getFilesAtPath(path: String): List<File> {
        return File("$localPath/$path").listFiles()?.toList() ?: emptyList()
    }

    fun getFileAtPath(path: String): File? {
        return File("$localPath/$path")
    }

    fun createDirectoryIfNotExist(path: String): File {
        val file = File(path)
        if(file.exists()) {
            file.mkdirs()
        }

        return file
    }
}

class FileExistsException(message: String): RuntimeException(message) {

}