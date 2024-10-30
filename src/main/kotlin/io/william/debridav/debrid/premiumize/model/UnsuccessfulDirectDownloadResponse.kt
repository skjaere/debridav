package io.william.debridav.debrid.premiumize.model

import kotlinx.serialization.Serializable

@Serializable
class UnsuccessfulDirectDownloadResponse(override val status: String) : DirectDownloadResponse {
}