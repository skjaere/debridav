package io.william.debrid.repository

import io.william.debrid.fs.models.Directory
import io.william.debrid.fs.models.DebridFile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface FileRepository: CrudRepository<DebridFile, Long> {
    fun findAllByDirectory(directory: Directory): List<DebridFile>
    fun findAllByPath(path: String): List<DebridFile>
    @Query("Select df from DebridFile df inner join Directory d on df.directory=d where concat(d.path,'/',df.name)=?1")
    fun findByFullPath(path: String): Optional<DebridFile>
    @Query("Select count(df)>0 from DebridFile df inner join Directory d on df.directory=d where concat(d.path,'/',df.name)=?1")
    fun existsByFullPath(path: String): Boolean

}