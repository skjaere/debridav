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

    override fun hashCode(): Int {
        var result = originalPath.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + modified.hashCode()
        result = 31 * result + (magnet?.hashCode() ?: 0)
        result = 31 * result + debridLinks.hashCode()
        return result
    }
}


enum class DebridProvider { REAL_DEBRID, PREMIUMIZE }