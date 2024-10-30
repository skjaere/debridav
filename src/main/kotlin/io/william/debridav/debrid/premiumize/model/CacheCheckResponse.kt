package io.william.debridav.debrid.premiumize.model

import kotlinx.serialization.Serializable

@Serializable
data class CacheCheckResponse(
    val status: String,
    val response: List<Boolean>,
    val transcoded: List<String>,
    val filename: List<String>,
    val filesize: List<String>
) {

}