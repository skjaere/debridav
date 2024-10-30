package io.william.debridav.debrid.premiumize.model

import kotlinx.serialization.Serializable

@Serializable(with = DirectDownloadResponseSerializer::class)
sealed interface DirectDownloadResponse {
    val status: String
}