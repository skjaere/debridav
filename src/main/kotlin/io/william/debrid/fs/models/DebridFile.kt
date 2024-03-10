package io.william.debrid.fs.models

data class DebridFile(
    val name: String,
    val path: String,
    val size: Long,
    val modified: Long,
    val link: String
)