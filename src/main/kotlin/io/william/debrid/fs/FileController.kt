package io.william.debrid.fs

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.william.debrid.repository.DirectoryRepository
import io.william.debrid.repository.FileRepository
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("files")
class FileController(
    private val directoryRepository: DirectoryRepository,
    private val fileRepository: FileRepository
) {

    @PostMapping("/directory/create", produces = ["application/json"])
    fun createDirectory(@RequestBody body: JsonNode): String {
        doCreateDirectory(body.get("path").asText())
        return "ok"
    }

    private fun doCreateDirectory(path: String): Directory? {
        val paths = path.split("/").filter { it.isNotEmpty() }.getPaths()
        return paths.mapIndexed { index, splitPath ->
            val existingDirectory = directoryRepository.findByPath(splitPath)
            if (existingDirectory.isEmpty) {
                val parent = if (index == 0) null else directoryRepository.findByPath(paths[index - 1]).getOrNull()
                val directory = Directory()
                directory.path = splitPath
                directory.parent = parent
                directoryRepository.save(directory)
            } else existingDirectory.get()
        }.lastOrNull()
    }

    @PostMapping("/file/create", produces = ["application/json"])
    fun createFile(@RequestBody body: JsonNode): String {
        val mapper = jacksonObjectMapper()
        val req = mapper.convertValue(body, CreateFileRequest::class.java)
        val parent = doCreateDirectory(req.path)
        val fileToCreate = File()
        fileToCreate.directory = parent
        fileToCreate.path = req.file.path
        fileToCreate.streamingLink = req.file.streamingLink
        fileToCreate.link = req.file.link
        fileToCreate.size = req.file.size
        fileRepository.save(fileToCreate)
        return "ok"
    }

    data class CreateFileRequest(
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

    private fun List<String>.getPaths(): List<String> = listOf("/").plus(
        this
            .runningReduce { acc, s ->
                "$acc/$s"
            }.map { "/$it" }
    )

}