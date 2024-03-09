package io.william.debrid.repository

import io.william.debrid.fs.models.Directory
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DirectoryRepository: CrudRepository<Directory, Long> {
    fun findByPath(path: String): Directory?
    fun findAllByParent(directory: Directory): List<Directory>
    fun existsByPath(path: String): Boolean
}