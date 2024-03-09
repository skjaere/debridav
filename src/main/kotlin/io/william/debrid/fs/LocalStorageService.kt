package io.william.debrid.fs

import io.william.debrid.fs.models.Directory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class LocalStorageService(
    @Value("\${debridav.local.file.path}") val localPath: String
) {
    fun createFile(directory: Directory, fileName: String, inputStream: InputStream): File {
        val file = File("$localPath${directory.path}/$fileName")
        file.absoluteFile
        if(file.exists()) {
            throw FileExistsException("${directory.path}/$fileName already exists")
        }
        if(!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        inputStream.transferTo(file.outputStream())
        return file
    }
}

class FileExistsException(message: String): RuntimeException(message) {

}