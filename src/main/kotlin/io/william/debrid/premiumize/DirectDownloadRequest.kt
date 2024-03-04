package io.william.debrid.premiumize

data class DirectDownloadRequest(
    val src: List<String>,
    val apikey: String
) {
}