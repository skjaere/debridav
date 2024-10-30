package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider
import kotlinx.serialization.Serializable

@Serializable
data class CachedFile(
    val path: String,
    val size: Long,
    val mimeType: String,
    val link: String?,
    override val provider: DebridProvider,
    override val lastChecked: Long,
    val params: Map<String, String> = emptyMap()
): DebridFile {
}
