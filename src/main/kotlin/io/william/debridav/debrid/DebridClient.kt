package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider
import org.springframework.stereotype.Component

@Component
interface DebridClient {
    fun isCached(magnet: String): Boolean
    fun getDirectDownloadLink(magnet: String): List<DebridResponse>

    fun getProvider(): DebridProvider
}