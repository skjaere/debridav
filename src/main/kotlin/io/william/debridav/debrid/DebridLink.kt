package io.william.debridav.debrid

data class DebridLink(
        val path: String,
        val size: Long,
        val mimeType: String,
        val link: String
)