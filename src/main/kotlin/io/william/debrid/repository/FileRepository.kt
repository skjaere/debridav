package io.william.debrid.repository

import io.william.debrid.fs.models.Directory
import io.william.debrid.fs.models.DebridFile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.Optional

/*
interface FileRepository: CrudRepository<DebridFile, Long> {
    fun findAllByDirectory(directory: Directory): List<DebridFile>
    fun findAllByPath(path: String): List<DebridFile>
    @Query("Select df from DebridFile df inner join Directory d on df.directory=d and d.path=?1 where df.name=?2")
    fun findByFullPath(directory: String, fileName: String): Optional<DebridFile>
    @Query("Select count(df)>0 from DebridFile df inner join Directory d on df.directory=d and d.path=?1 where df.name=?2")
    fun existsByFullPath(directory: String, fileName: String): Boolean

}*/
