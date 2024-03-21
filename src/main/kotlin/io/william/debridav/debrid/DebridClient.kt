package io.william.debridav.debrid

import org.springframework.stereotype.Component

@Component
interface DebridClient {
    fun isCached(magnet: String): Boolean
    fun getDirectDownloadLink(magnet: String): List<DebridLink>
}