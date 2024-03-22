package io.william.debridav.fs

import io.william.debridav.debrid.DebridResponse
import java.time.Instant

data class DebridFileContents(
        var originalPath: String,
        var size: Long,
        var modified: Long,
        var magnet: String?,
        var debridLinks: MutableList<DebridLink>
) {
    companion object {
        fun ofDebridResponse(
                content: DebridResponse,
                magnet: String?,
                debridProvider: DebridProvider
        ) = DebridFileContents(
                content.path,
                content.size,
                Instant.now().toEpochMilli(),
                magnet,
                mutableListOf(DebridLink(debridProvider, content.link))
        )
    }

    fun getProviderLink(provider: DebridProvider): DebridLink? = debridLinks.firstOrNull { it.provider == provider }

    override fun equals(other: Any?): Boolean {
        if (other is DebridFileContents) {
            return originalPath == other.originalPath
                    && size == other.size
                    && magnet == other.magnet
                    && debridLinks == other.debridLinks
        }

        return super.equals(other)
    }
}


enum class DebridProvider { REAL_DEBRID, PREMIUMIZE }