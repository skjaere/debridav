package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider

sealed interface DebridFile {
    val provider: DebridProvider
    val lastChecked: Long
}
