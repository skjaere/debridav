package io.william.debridav.debrid

import io.william.debridav.fs.DebridProvider

sealed interface DebridResponse {
    val provider: DebridProvider
}