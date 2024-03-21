package io.william.debridav.debrid

import io.william.debridav.debrid.premiumize.DirectDownloadResponse
import org.springframework.stereotype.Component

@Component
interface DebridClient {
    fun isCached(magnet: String): Boolean
    fun getDirectDownloadLink(magnet: String): DirectDownloadResponse?
}