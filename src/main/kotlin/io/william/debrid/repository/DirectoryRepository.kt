package io.william.debrid.repository

import io.william.debrid.fs.Directory
import org.springframework.data.repository.CrudRepository
import java.util.*

interface DirectoryRepository: CrudRepository<Directory, Long> {
    fun findByPath(path: String): Optional<Directory>
    fun findAllByParent(directory: Directory): List<Directory>
}