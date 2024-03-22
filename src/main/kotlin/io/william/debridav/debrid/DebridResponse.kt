package io.william.debridav.debrid

import io.william.debridav.fs.DebridLink
import io.william.debridav.fs.DebridProvider

data class DebridResponse(
        val path: String,
        val size: Long,
        val mimeType: String,
        val link: String
) {
    fun toDebridLink(provider: DebridProvider) = DebridLink(
            provider,
            link
    )
}