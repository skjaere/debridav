package io.william.debridav.fs.models

import io.william.debridav.debrid.DebridLink
import java.time.Instant

data class DebridFileContents(
        var originalPath: String,
        var size: Long,
        var modified: Long,
        var link: String,
        var magnet: String?//,
        //val torrentFile: ByteArray?
) {
    companion object {
        fun ofDebridLink(content: DebridLink, magnet: String?): DebridFileContents {
            return DebridFileContents(
                    content.path,
                    content.size,
                    Instant.now().toEpochMilli(),
                    content.link,
                    magnet
            )
        }
    }
}