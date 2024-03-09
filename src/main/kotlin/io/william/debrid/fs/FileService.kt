package io.william.debrid.fs

import io.milton.resource.Resource
import io.william.debrid.fs.models.DebriDavFile
import io.william.debrid.fs.models.DebridFile
import io.william.debrid.fs.models.Directory
import io.william.debrid.fs.models.LocalFile
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.repository.FileRepository
import io.william.debrid.repository.LocalFileRepository
import io.william.debrid.resource.DirectoryResource
import io.william.debrid.resource.FileResource
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.Instant

@Service
class FileService(
    private val directoryRepository: DirectoryRepository,
    private val fileRepository: FileRepository,
    private val localFileRepository: LocalFileRepository,
    private val localFileService: LocalStorageService
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
        val parentPath  = "${createRequest.path}/${createRequest.file.path}"
            .split("/")
            .filter { it.isNotBlank() }
            .dropLast(1)
            .joinToString("/")

        val debridFileToCreate = DebridFile()
        debridFileToCreate.directory = getOrCreateDirectory(parentPath)
        debridFileToCreate.name = getNameFromPath(createRequest.file.path)
        debridFileToCreate.path = createRequest.file.path.replace(" ","_")
        debridFileToCreate.streamingLink = createRequest.file.streamingLink
        debridFileToCreate.link = createRequest.file.link
        debridFileToCreate.size = createRequest.file.size
        fileRepository.save(debridFileToCreate)
    }

    fun createLocalFile(path: String, size: Long, inputStream: InputStream, contentType: String): LocalFile {
        val parentPath  = path
            .split("/")
            .filter { it.isNotBlank() }
            .dropLast(1)
            .joinToString("/")

        val localFileToCreate = LocalFile()
        val fileName = getNameFromPath(path)
        localFileToCreate.directory = getOrCreateDirectory(parentPath)
        localFileToCreate.name = fileName
        localFileToCreate.path = path.replace(" ","_")
        localFileToCreate.size = size
        localFileToCreate.contentType = contentType

        val createdFile = localFileService.createFile(
            localFileToCreate.directory!!,
            fileName,
            inputStream
        )
        localFileToCreate.localPath = createdFile.path
        return localFileRepository.save(localFileToCreate)
    }
    private fun getDirectoryAndFileFromPath(path: String): Pair<String, String>  {
        val split = path.split("/")
        val fileName = split.last()
        val directory = split.dropLast(1).joinToString("/")
        return Pair(directory, fileName)
    }

    private fun getNameFromPath(path: String): String = path.split("/").last()

    fun saveFile(file:DebriDavFile): DebriDavFile {
        return when(file) {
            is DebridFile -> fileRepository.save(file)
            is LocalFile -> {
                // move file
                localFileRepository.save(file)
            }
        }
    }

    fun moveFile(file: DebriDavFile, dest: Directory, name: String) {
        file.directory = dest
        file.name = name
        //file.fullPath = "${dest.path}/${file.name}"
        saveFile(file)
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
        return DirectoryResource(getOrCreateDirectory(path), this)
    }

    fun getChildren(directory: Directory): List<Resource> {
        val debridFiles = fileRepository.findAllByDirectory(directory)
        val localFiles = localFileRepository.findAllByDirectory(directory)
        val childDirectories = directoryRepository.findAllByParent(directory)
        return debridFiles
            .map { FileResource(it, this) }
            .plus(localFiles.map { FileResource(it, this) })
            .plus(childDirectories.map { DirectoryResource(it, this) })
    }

    fun getResourceAtPath(path: String): Resource? {
       if (directoryRepository.existsByPath(path)) {
            getDirectory(path)?.let { directoryFromDb ->
                return DirectoryResource(
                    directoryFromDb,
                    this
                )
            }
        }
        val (directory, fileName) = getDirectoryAndFileFromPath(path)
        if(fileRepository.existsByFullPath(path)){
            return FileResource(fileRepository.findByFullPath(path).get(), this)
        } else if(localFileRepository.existsByFullPath(directory, fileName)){
           return FileResource(localFileRepository.findByFullPath(directory, fileName).get(), this)
        }
        return null
    }

    fun moveResource(item: Resource, destination: DirectoryResource, name: String) {
        when(item) {
            is FileResource -> moveFile(item.getDebriDavFile(), destination.getDirectory(), name)
            is DirectoryResource -> moveDirectory(item.getDirectory(), destination.getDirectory(), name)
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
        val file: File
    ) {
        data class File(
            val path: String,
            val size: Long,
            val link: String,
            val streamingLink: String?
        )
    }
}