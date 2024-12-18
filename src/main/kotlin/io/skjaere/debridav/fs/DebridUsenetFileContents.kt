package io.skjaere.debridav.fs

import io.skjaere.debridav.debrid.model.DebridFile
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class DebridUsenetFileContents(
    var type: DebridFileContentsType = DebridFileContentsType.USENET,
    override var originalPath: String,
    override var size: Long,
    override var modified: Long,
    override var debridLinks: MutableList<DebridFile>,
    var usenetDownloadId: Int,
    var nzbFileLocation: String
) : DebridFileContents {

    fun getNzb(): File {
        return File(nzbFileLocation)
    }
}

enum class DebridFileContentsType {
    TORRENT, USENET
}