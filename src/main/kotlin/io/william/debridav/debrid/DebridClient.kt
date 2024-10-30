package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider
import org.springframework.stereotype.Component

@Component
interface DebridClient {
    suspend fun isCached(magnet: String): Boolean
    suspend fun getCachedFiles(magnet: String): List<CachedFile> = getCachedFiles(magnet, emptyMap())
    suspend fun getCachedFiles(magnet: String, params: Map<String, String>): List<CachedFile>
    fun getProvider(): DebridProvider
}