package io.william.debrid.premiumize

import com.fasterxml.jackson.annotation.JsonAlias

data class DirectDownloadResponse(
    val status: String,
    val location: String,
    val filename: String,
    val filesize: Long,
    val content: List<Content>
) {
    data class Content(
        val path: String,
        val size: Long,
        val link: String,
        @JsonAlias("stream_link")
        val streamLink: String?,
        @JsonAlias("transcode_status")
        val transcodeStatus: String?
    ) {
        fun hasSubDirectories() : Boolean {
            return path.split("/").size > 1
        }
    }
}