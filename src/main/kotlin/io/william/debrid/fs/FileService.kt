package io.william.debrid.fs

import com.fasterxml.jackson.databind.ObjectMapper
import io.milton.resource.Resource
import io.william.debrid.fs.models.*
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.resource.DebridFileResource
import io.william.debrid.resource.DirectoryResource
import io.william.debrid.resource.FileResource
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.io.InvalidObjectException
import java.time.Instant

@Service
class FileService(
    private val directoryRepository: DirectoryRepository,
/*
    private val fileRepository: FileRepository,
    private val localFileRepository: LocalFileRepository,
*/
    private val localFileService: LocalStorageService,
    private val objectMapper: ObjectMapper
) {
    fun getOrCreateDirectory(path: String): Directory {
        val paths = path.split("/").filter { it.isNotEmpty() }.getPaths()
        return paths.mapIndexed { index, splitPath ->
            val existingDirectory = directoryRepository.findByPath(splitPath)
            existingDirectory ?: run {
                val parent = if (index == 0) null else directoryRepository.findByPath(paths[index - 1])
                createDirectory(parent, splitPath)
            }
        }.last()
    }

    private fun createDirectory(
        parent: Directory?,
        splitPath: String
    ): Directory {
        //val
        val directory = Directory()
        directory.path = splitPath.replace(" ", "_")
        directory.parent = parent
        directory.created = Instant.now()
        return directoryRepository.save(directory)
    }

    fun createFile(createRequest: CreateFileRequest) {
        /*val parentPath  = "${createRequest.path}/${createRequest.file.path}"
            .getDirectory()
*/
        val debridFileToCreate = DebridFile(
            getNameFromPath(createRequest.file.path),
            createRequest.file.path,
            createRequest.file.size,
            Instant.now().toEpochMilli(),
            createRequest.file.link
        )
        /*debridFileToCreate.directory = getOrCreateDirectory(parentPath)
        debridFileToCreate.name = getNameFromPath(createRequest.file.path)
        debridFileToCreate.path = createRequest.file.path.replace(" ","_")
        debridFileToCreate.streamingLink = createRequest.file.streamingLink
        debridFileToCreate.link = createRequest.file.link
        debridFileToCreate.size = createRequest.file.size
        fileRepository.save(debridFileToCreate)*/

        localFileService.createFile(
            getNameFromPath(createRequest.path),
            createRequest.file.path,
            createRequest.type,
            objectMapper.writeValueAsString(debridFileToCreate).byteInputStream()
        )
    }

    fun createLocalFile(path: String, size: Long, inputStream: InputStream, contentType: String): File {
        val parentPath  = path.getDirectory()
        val localFileToCreate = LocalFile()
        val fileName = getNameFromPath(path)

        localFileToCreate.directory = getOrCreateDirectory(parentPath)
        localFileToCreate.name = fileName
        localFileToCreate.path = path.replace(" ","_")
        localFileToCreate.size = size
        localFileToCreate.contentType = contentType

        val createdFile = localFileService.createFile(
            localFileToCreate.directory!!.path!!,
            fileName,
            CreateFileRequest.Type.NORMAL,
            inputStream
        )
        localFileToCreate.localPath = createdFile.path
        return createdFile
        //return localFileRepository.save(localFileToCreate)
    }
    fun String.getDirectory() = this.split("/")
        .filter { it.isNotBlank() }
        .dropLast(1)
        .joinToString("/")

    private fun getDirectoryAndFileFromPath(path: String): Pair<String, String>  {
        val split = path.split("/")
        val fileName = split.last()
        val directory = split.dropLast(1).joinToString("/")
        return Pair(directory, fileName)
    }

    private fun getNameFromPath(path: String): String = path.split("/").last()

    /*fun saveDebridFile(file:DebridFile): DebriDavFile {
        return fileRepository.save(file)
    }*/

    fun moveFile(src: String, dest: String, name: String) {
        localFileService.moveFile(src, dest, name)
        /*file.directory = dest
        file.name = name*/

        /*when(file) {
            is DebridFile -> return
            is LocalFile -> {
                localFileService.moveFile(file.localPath!!, dest, name)
                //localFileRepository.save(file)
            }
            is LocalDebridFile -> localFileService.moveFile(file.path!!, dest, name)
        }*/

    }

    fun moveDirectory(directory: Directory, dest: Directory, name: String) {
        directory.parent = dest
        directory.path = "${dest.path}/$name"
        saveDirectory(directory)
    }

    fun saveDirectory(directory: Directory): Directory {
        return directoryRepository.save(directory)
    }

    fun getDirectory(path: String): Directory? {
        return directoryRepository.findByPath(path)
    }

    fun createDirectory(path: String): DirectoryResource {
        //return DirectoryResource(getOrCreateDirectory(path), this)

        return DirectoryResource(localFileService.createDirectoryIfNotExist(path), this)
    }

    fun getChildren(directory: Directory): List<Resource> {
        //val debridFiles = fileRepository.findAllByDirectory(directory)
         //localFileRepository.findAllByDirectory(directory)
        val localFiles = localFileService.getFilesAtPath(directory.path!!)
        val childDirectories = localFiles
            .filter { it.isDirectory } //directoryRepository.findAllByParent(directory)
            .map { it.toDirectoryResource() }
        val parsedLocalFiles = localFiles
            .filterNot { it.isDirectory }
            .mapNotNull { it.toFileResource() }
            /*if(it.name.endsWith(".debridavfile")) {
                objectMapper.readValue(it, LocalDebridFile::class.java)
            } else {
                val lf = LocalFile()
                lf.name = it.name
                lf.localPath = it.path
                lf.size = it.length()
                lf.directory = directory
                lf
            }*/
        /*}.map {
            FileResource(it, this)
        }*/
        return parsedLocalFiles
            /*.map { FileResource(it, this) }
            .plus(parsedLocalFiles)*/
            .plus(childDirectories)
    }

    fun getResourceAtPath(path: String): Resource? {
       /*if (directoryRepository.existsByPath(path)) {
            getDirectory(path)?.let { directoryFromDb ->
                return DirectoryResource(
                    directoryFromDb,
                    this
                )
            }
        }*/
        //val file = localFileService.getFileAtPath(path)
        return localFileService.getFileAtPath(path)
            ?.let {
                if(it.isDirectory) it.toDirectoryResource()
                else it.toFileResource()
            } ?: run {
                localFileService.getFileAtPath("$path.debridfile")?.toFileResource()
        }
        /*val (directory, fileName) = getDirectoryAndFileFromPath(path)
        if(fileRepository.existsByFullPath(directory, fileName)){
            return FileResource(fileRepository.findByFullPath(directory, fileName).get(), this)
        } //else if(localFileRepository.existsByFullPath(directory, fileName)){
        else {
            localFileService.getFilesAtPath(path)
                .map {it.toFileResource() }
           //return FileResource(localFileRepository.findByFullPath(directory, fileName).get(), this)
        }
        return null*/
    }
    fun File.toFileResource(): Resource? {
        if(this.isDirectory) {
            throw RuntimeException("Provided file is a directory")
        }
        return if(this.name.endsWith(".debridfile")) {
            DebridFileResource(objectMapper.readValue(this, DebridFile::class.java), this@FileService)
        } else {
            /*val lf = LocalFile()
            lf.name = this.name
            lf.localPath = this.path
            lf.size = this.length()*/
            //lf.directory = directory
            if(this.exists())
                return FileResource(this, this@FileService)
            null
        }
        //return FileResource(file, this@FileService)
    }
    fun toResource(file: File) : Resource? {
        return if(file.isDirectory) file.toDirectoryResource() else file.toFileResource()
    }

    fun File.toDirectoryResource(): DirectoryResource {
        if(!this.isDirectory) {
            throw RuntimeException("Not a directory")
        }
        /*val directory = Directory()
        directory.path = this.path
        directory.created = Instant.ofEpochMilli(this.lastModified())*/
        return DirectoryResource(this, this@FileService)
    }

    fun moveResource(item: Resource, destination: String, name: String) {
        when(item) {
            is FileResource -> moveFile(item.file.path, destination, name)
            is DebridFileResource -> moveFile(item.debridFile.path, destination, name)
            is DirectoryResource -> moveFile(item.directory.path, destination, name)
        }
    }

    private fun List<String>.getPaths(): List<String> = listOf("/").plus(
        this
            .runningReduce { acc, s ->
                "$acc/${s.replace(" ","_")}"
            }
            .map { "/$it" }
    )

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