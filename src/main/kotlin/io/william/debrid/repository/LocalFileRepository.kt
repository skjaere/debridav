package io.william.debrid.repository

import io.william.debrid.fs.models.Directory
import io.william.debrid.fs.models.DebridFile
import io.william.debrid.fs.models.LocalFile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface LocalFileRepository: CrudRepository<LocalFile, Long> {
    fun findAllByDirectory(directory: Directory): List<LocalFile>
    fun findAllByPath(path: String): List<LocalFile>
    @Query("Select lf from LocalFile lf inner join Directory d on lf.directory=d and d.path=?1 where lf.name=?2")
    fun findByFullPath(path: String, fileName: String): Optional<LocalFile>
    @Query("Select count(lf)>0 from LocalFile lf inner join Directory d on lf.directory=d and d.path=?1 where lf.name=?2")
    fun existsByFullPath(path: String, fileName: String): Boolean

}