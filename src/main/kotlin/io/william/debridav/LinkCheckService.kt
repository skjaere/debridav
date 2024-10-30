package io.william.debridav

import io.william.debridav.debrid.CachedFile
import org.springframework.stereotype.Service
import java.net.HttpURLConnection
import java.net.URI

@Service
class LinkCheckService {
    fun isLinkAlive(cachedFile: CachedFile): Boolean {
        return (URI(cachedFile.link!!).toURL().openConnection() as HttpURLConnection).responseCode == 200
    }
}