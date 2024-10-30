package io.william.debridav.debrid.premiumize.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
data class SuccessfulDirectDownloadResponse(
    override val status: String,
    val location: String,
    val filename: String,
    val filesize: Long,
    val content: List<Content>
): DirectDownloadResponse {
}

@Serializable
data class Content @OptIn(ExperimentalSerializationApi::class) constructor(
    val path: String,
    val size: Long,
    val link: String,
    @JsonNames("stream_link") val streamLink: String?,
    @JsonNames("transcode_status") val transcodeStatus: String?
)