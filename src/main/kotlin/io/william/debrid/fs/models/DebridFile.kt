package io.william.debrid.fs.models

data class DebridFile(
    var name: String,
    var path: String,
    val size: Long,
    val modified: Long,
    val link: String
)