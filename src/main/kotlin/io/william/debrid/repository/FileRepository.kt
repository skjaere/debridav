package io.william.debrid.repository

import io.william.debrid.fs.Directory
import io.william.debrid.fs.File
import org.springframework.data.repository.CrudRepository

interface FileRepository: CrudRepository<File, Long> {
    fun findAllByDirectory(directory: Directory): List<File>
    fun findAllByPath(path: String): List<File>

}