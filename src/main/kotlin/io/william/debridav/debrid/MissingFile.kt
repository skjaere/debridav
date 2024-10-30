package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider
import kotlinx.serialization.Serializable

@Serializable
data class MissingFile(
    override val provider: DebridProvider,
    override val lastChecked: Long
): DebridFile {
}