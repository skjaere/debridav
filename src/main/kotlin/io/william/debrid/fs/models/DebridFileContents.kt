package io.william.debrid.fs.models

data class DebridFileContents(
    var originalPath: String,
    var size: Long,
    var modified: Long,
    var link: String,
    var magnet: String?//,
    //val torrentFile: ByteArray?
) {
}