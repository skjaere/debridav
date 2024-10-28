package io.william.debridav.fs


import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.springframework.stereotype.Service
import java.io.File

@Service
class FileContentsService {
    private val objectMapper = jacksonObjectMapper()
    private val cache: LoadingCache<String, DebridFileContents> = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build(CacheLoader.from { path -> loadContentsFromFile(path) })

    private fun loadContentsFromFile(path: String): DebridFileContents = objectMapper.readValue(File(path))

    fun getContentsOnPath(path: String): DebridFileContents = cache.get(path)

    fun refreshContentsOnPath(path: String, contents: DebridFileContents) = cache.put(path, contents)

    fun delete(path: String) = cache.invalidate(path)


    fun deleteContents(path: String) = cache.invalidate(path)
}