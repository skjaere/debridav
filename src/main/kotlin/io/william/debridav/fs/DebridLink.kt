package io.william.debridav.fs

import io.william.debridav.StreamingService.Result
import java.time.Instant

data class DebridLink(
        val provider: DebridProvider,
        var link: String?,
        var lastChecked: Long,
        var lastStatus: Result
)