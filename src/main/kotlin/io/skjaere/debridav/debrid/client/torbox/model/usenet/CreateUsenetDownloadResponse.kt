package io.skjaere.debridav.debrid.client.torbox.model.usenet

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateUsenetDownloadResponse(
    val success: Boolean,
    val error: String?,
    val detail: String?,
    val data: CreatedDownload
)

@Serializable
data class CreatedDownload(
    val hash: String,
    @SerialName("usenetdownload_id") val usenetDownloadId: String,
    @SerialName("auth_id") val authId: String
)
